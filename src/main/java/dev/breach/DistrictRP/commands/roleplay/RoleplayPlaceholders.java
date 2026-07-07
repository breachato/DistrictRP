package dev.breach.DistrictRP.commands.roleplay;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.roleplay.appuntamenti.AppuntamentoManager;
import dev.breach.DistrictRP.commands.roleplay.playtime.PlaytimeTracker;
import dev.breach.DistrictRP.commands.roleplay.plot.PlotSquaredHook;
import dev.breach.DistrictRP.commands.roleplay.profile.RPProfile;
import dev.breach.DistrictRP.commands.roleplay.profile.RPProfileManager;
import dev.breach.DistrictRP.commands.roleplay.protection.ProtectionManager;
import dev.breach.DistrictRP.commands.roleplay.ticket.Ticket;
import dev.breach.DistrictRP.commands.roleplay.ticket.TicketManager;
import dev.breach.DistrictRP.functions.VanishManager;
import dev.breach.DistrictRP.functions.servermode.ServerMode;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;

public class RoleplayPlaceholders extends PlaceholderExpansion {

    private final DistrictRP plugin;
    private final RPProfileManager profiles;
    private final PlaytimeTracker playtime;
    private final TicketManager tickets;

    private final SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy");
    private final SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");
    private final SimpleDateFormat sdfFull = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private final boolean worldGuardAvailable;

    public RoleplayPlaceholders(DistrictRP plugin,
                                RPProfileManager profiles,
                                PlaytimeTracker playtime,
                                TicketManager tickets) {
        this.plugin = plugin;
        this.profiles = profiles;
        this.playtime = playtime;
        this.tickets = tickets;
        this.worldGuardAvailable = Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
    }

    @Override public @NotNull String getIdentifier() { return "districtrp"; }
    @Override public @NotNull String getAuthor() { return "Breach"; }
    @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    private String getDefaultJob() {
        return plugin.getConfig().getString("profile.default-job", "DISOCCUPATO");
    }

    private String resolveJob(RPProfile p) {
        String j = p.getJob();
        return (j == null || j.isEmpty()) ? getDefaultJob() : j;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        RPProfile p = profiles.get(player.getUniqueId());
        String key = params.toLowerCase();

        switch (key) {
            case "rpname": return p.hasRpName() ? p.getRpName() : player.getName();
            case "rpsurname": return p.getRpSurname() == null ? "" : p.getRpSurname();
            case "rpfullname": return p.getRpFullName().isEmpty() ? player.getName() : p.getRpFullName();
            case "cf": return player.getName();
            case "cf_short": {
                String n = player.getName();
                if (n == null) return "";
                return n.length() > 8 ? n.substring(0, 8) + "..." : n;
            }
            case "job": return resolveJob(p);
            case "job_prefix": return "&8[&e" + resolveJob(p) + "&8]";
            case "job_short": {
                String j = resolveJob(p);
                return j.length() > 6 ? j.substring(0, 6) : j;
            }
            case "ic_age": return String.valueOf(p.getIcAge());
            case "ic_gender": return p.getIcGender();
            case "ic_birthday": return p.getIcBirthday();
            case "ic_nationality": return p.getIcNationality();
            case "ic_bio": return p.getIcBio();
            case "phone": return p.getPhone();
            case "discord_id": return p.getDiscordId();
            case "telegram_id": return p.getTelegramId();
            case "address": return p.getLastKnownAddress();
            case "vehicle": return p.getVehicle();
            case "vehicle_plate": return p.getVehiclePlate();
            case "azienda": return p.getAzienda() == null ? "" : p.getAzienda();
            case "azienda_ruolo": return p.getAziendaRuolo() == null ? "" : p.getAziendaRuolo();
            case "has_azienda": return Boolean.toString(p.hasAzienda());
        }

        switch (key) {
            case "playtime_total": return playtime.formatTotal(player.getUniqueId());
            case "playtime_daily": return playtime.formatDaily(player.getUniqueId());
            case "playtime_weekly": return playtime.formatWeekly(player.getUniqueId());
            case "playtime_monthly": return playtime.formatMonthly(player.getUniqueId());
            case "playtime_status": return player.isOnline() ? "Online" : "Offline";
            case "playtime_total_seconds": return String.valueOf(playtime.get(player.getUniqueId()).getTotalSeconds());
            case "playtime_daily_seconds": return String.valueOf(playtime.get(player.getUniqueId()).getDailySeconds());
            case "playtime_weekly_seconds": return String.valueOf(playtime.get(player.getUniqueId()).getWeeklySeconds());
            case "playtime_monthly_seconds": return String.valueOf(playtime.get(player.getUniqueId()).getMonthlySeconds());
            case "playtime_total_hours": return String.valueOf(playtime.get(player.getUniqueId()).getTotalSeconds() / 3600);
            case "playtime_total_minutes": return String.valueOf(playtime.get(player.getUniqueId()).getTotalSeconds() / 60);
        }

        switch (key) {
            case "rank": return getRankFromOrder(player);
            case "rank_symbol": return getRankSymbolFromOrder(player);
            case "rank_any": return getRankAny(player);
            case "rank_symbol_any": return getRankSymbolAny(player);
            case "rank_symbol_or_none": {
                String s = getRankSymbolAny(player);
                return s.isEmpty() ? "&7[UTENTE]" : s;
            }
            case "staff_rank": return getRankFromOrder(player);
            case "staff_symbol": return getRankSymbolFromOrder(player);
            case "is_staff": return Boolean.toString(isStaff(player));
            case "effective_symbol": return getEffectiveSymbol(player);
            case "effective_prefix": return getEffectivePrefix(player);
        }

        switch (key) {
            case "servermode": {
                if (plugin.getServerModeManager() == null) return "OFF";
                return plugin.getServerModeManager().getCurrent().name();
            }
            case "servermode_display": {
                if (plugin.getServerModeManager() == null) return "OFF";
                return plugin.getServerModeManager().getCurrentDisplay();
            }
            case "is_servermode_lobby":
                return Boolean.toString(isServerMode(ServerMode.LOBBY));
            case "is_servermode_creative":
                return Boolean.toString(isServerMode(ServerMode.CREATIVE));
            case "is_servermode_roleplay":
                return Boolean.toString(isServerMode(ServerMode.ROLEPLAY));
            case "is_servermode_off":
                return Boolean.toString(isServerMode(ServerMode.OFF));
        }

        switch (key) {
            case "current_plot": return getCurrentPlot(player);
            case "current_plot_owner": return getCurrentPlotOwner(player);
            case "is_plot_owner": return Boolean.toString(isPlotOwner(player));
            case "is_plot_trusted": return Boolean.toString(isPlotTrusted(player));
            case "is_in_plot": return Boolean.toString(isInPlot(player));
            case "is_in_own_plot": return Boolean.toString(isPlotOwner(player));
            case "owned_plots": return String.valueOf(getOwnedPlotsCount(player));
        }

        switch (key) {
            case "ticket_open": return String.valueOf(tickets.countOpenByAuthor(player.getUniqueId()));
            case "ticket_total": return String.valueOf(countTotalTickets(player));
            case "ticket_closed": return String.valueOf(countClosedTickets(player));
            case "ticket_last_id": return getLastTicketId(player);
            case "ticket_categories_count": return String.valueOf(tickets.getCategories().size());
        }

        switch (key) {
            case "appuntamenti_count": return String.valueOf(countAppuntamenti(player));
            case "appuntamento_next": return getNextAppuntamento(player);
            case "appuntamento_next_day": return getNextAppuntamentoDay(player);
            case "appuntamento_next_time": return getNextAppuntamentoTime(player);
            case "appuntamento_next_reparto": return getNextAppuntamentoReparto(player);
        }

        switch (key) {
            case "vanish_status": return Boolean.toString(isVanished(player));
            case "vanish_suffix": return getVanishSuffix(player);
            case "vanish_count": return String.valueOf(countVanished());
            case "staffmode_status": return Boolean.toString(isInStaffMode(player));
            case "staffmode_suffix": return isInStaffMode(player) ? " §7[ §5STAFFMODE §7]" : "";
            case "staffmode_count": return String.valueOf(countStaffMode());
            case "stuck_active": return Boolean.toString(isStuckActive(player));
            case "stuck_seconds": return String.valueOf(getStuckSeconds(player));
        }

        switch (key) {
            case "current_world": return player.isOnline() && player.getPlayer() != null
                    ? player.getPlayer().getWorld().getName() : "";
            case "current_world_protected": return Boolean.toString(isCurrentWorldProtected(player));
            case "current_world_no_build": return Boolean.toString(isCurrentWorldNoBuild(player));
            case "current_world_no_interact": return Boolean.toString(isCurrentWorldNoInteract(player));
            case "current_world_whitelisted": return Boolean.toString(isCurrentWorldWhitelisted(player));
            case "region": return getWorldGuardRegion(player);
            case "region_or_none": {
                String r = getWorldGuardRegion(player);
                return r.isEmpty() ? "wilderness" : r;
            }
        }

        if (player.isOnline()) {
            Player pl = player.getPlayer();
            if (pl != null) {
                Location loc = pl.getLocation();
                switch (key) {
                    case "pos_x": return String.valueOf(loc.getBlockX());
                    case "pos_y": return String.valueOf(loc.getBlockY());
                    case "pos_z": return String.valueOf(loc.getBlockZ());
                    case "pos_full": return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
                    case "pos_biome": return loc.getBlock().getBiome().name();
                    case "yaw": return String.format("%.0f", loc.getYaw());
                    case "pitch": return String.format("%.0f", loc.getPitch());
                    case "gamemode": return pl.getGameMode().name();
                    case "health": return String.format("%.1f", pl.getHealth());
                    case "max_health": return String.format("%.1f", pl.getMaxHealth());
                    case "food": return String.valueOf(pl.getFoodLevel());
                    case "level": return String.valueOf(pl.getLevel());
                    case "exp": return String.format("%.2f", pl.getExp());
                    case "ping": return String.valueOf(pl.getPing());
                    case "flying": return String.valueOf(pl.isFlying());
                    case "sneaking": return String.valueOf(pl.isSneaking());
                    case "sprinting": return String.valueOf(pl.isSprinting());
                }
            }
        }

        switch (key) {
            case "deaths": return String.valueOf(p.getDeaths());
            case "kills": return String.valueOf(p.getKills());
            case "kd_ratio":
                if (p.getDeaths() == 0) return String.valueOf(p.getKills());
                return String.format("%.2f", (double) p.getKills() / p.getDeaths());
            case "first_join": return sdfFull.format(new Date(p.getFirstJoin()));
            case "first_join_date": return sdfDate.format(new Date(p.getFirstJoin()));
            case "last_join": return sdfFull.format(new Date(p.getLastJoin()));
            case "last_quit": return p.getLastQuit() == 0 ? "-" : sdfFull.format(new Date(p.getLastQuit()));
            case "days_registered": return String.valueOf((System.currentTimeMillis() - p.getFirstJoin()) / (86400000L));
        }

        if (key.startsWith("has_license_")) {
            String lic = key.substring("has_license_".length());
            return Boolean.toString(p.hasLicense(lic));
        }
        if (key.startsWith("has_permesso_")) {
            String perm = key.substring("has_permesso_".length());
            return Boolean.toString(p.hasPermesso(perm));
        }
        if (key.startsWith("has_rank_")) {
            String rank = key.substring("has_rank_".length());
            return Boolean.toString(hasRank(player, rank));
        }
        if (key.startsWith("symbol_")) {
            String rank = key.substring("symbol_".length());
            return getSymbolFor(rank);
        }

        switch (key) {
            case "licenses_count": return String.valueOf(p.getLicenses().size());
            case "licenses_list": return String.join(", ", p.getLicenses());
            case "permessi_count": return String.valueOf(p.getPermessi().size());
            case "permessi_list": return String.join(", ", p.getPermessi());
        }

        switch (key) {
            case "server_name": return plugin.getConfig().getString("bots.discord.server-name", "DistrictRP");
            case "server_online": return String.valueOf(Bukkit.getOnlinePlayers().size());
            case "server_max": return String.valueOf(Bukkit.getMaxPlayers());
            case "server_tps": return String.format("%.1f", Math.min(20.0, getTps()));
            case "server_tps_short": return String.format("%.0f", Math.min(20.0, getTps()));
            case "server_worlds_count": return String.valueOf(Bukkit.getWorlds().size());
            case "server_staff_online": return String.valueOf(countStaffOnline());
            case "server_players_online": return String.valueOf(countNonStaffOnline());
            case "server_date": return sdfDate.format(new Date());
            case "server_time": return sdfTime.format(new Date());
            case "server_datetime": return sdfFull.format(new Date());
        }

        switch (key) {
            case "is_online": return Boolean.toString(player.isOnline());
            case "is_op": return Boolean.toString(player.isOp());
            case "is_vanished": return Boolean.toString(isVanished(player));
            case "is_in_staffmode": return Boolean.toString(isInStaffMode(player));
            case "is_in_azienda": return Boolean.toString(p.hasAzienda());
            case "is_disoccupato": return Boolean.toString(getDefaultJob().equalsIgnoreCase(resolveJob(p)));
        }

        return null;
    }

    private boolean isServerMode(ServerMode mode) {
        if (plugin.getServerModeManager() == null) return mode == ServerMode.OFF;
        return plugin.getServerModeManager().getCurrent() == mode;
    }

    private PlotSquaredHook getPlotHook() {
        if (plugin.getRoleplay() == null || plugin.getRoleplay().getPlotAddon() == null) return null;
        return plugin.getRoleplay().getPlotAddon().getHook();
    }

    private String getCurrentPlot(OfflinePlayer player) {
        if (!player.isOnline() || player.getPlayer() == null) return "";
        PlotSquaredHook h = getPlotHook();
        if (h == null || !h.isAvailable()) return "";
        return h.getCurrentPlotId(player.getPlayer());
    }

    private String getCurrentPlotOwner(OfflinePlayer player) {
        if (!player.isOnline() || player.getPlayer() == null) return "";
        PlotSquaredHook h = getPlotHook();
        if (h == null || !h.isAvailable()) return "";
        return h.getCurrentPlotOwnerName(player.getPlayer());
    }

    private boolean isPlotOwner(OfflinePlayer player) {
        if (!player.isOnline() || player.getPlayer() == null) return false;
        PlotSquaredHook h = getPlotHook();
        if (h == null || !h.isAvailable()) return false;
        return h.isPlotOwner(player.getPlayer());
    }

    private boolean isPlotTrusted(OfflinePlayer player) {
        if (!player.isOnline() || player.getPlayer() == null) return false;
        PlotSquaredHook h = getPlotHook();
        if (h == null || !h.isAvailable()) return false;
        return h.isPlotTrusted(player.getPlayer());
    }

    private boolean isInPlot(OfflinePlayer player) {
        if (!player.isOnline() || player.getPlayer() == null) return false;
        PlotSquaredHook h = getPlotHook();
        if (h == null || !h.isAvailable()) return false;
        return h.isInPlot(player.getPlayer());
    }

    private int getOwnedPlotsCount(OfflinePlayer player) {
        if (!player.isOnline() || player.getPlayer() == null) return 0;
        PlotSquaredHook h = getPlotHook();
        if (h == null || !h.isAvailable()) return 0;
        return h.getOwnedPlotsCount(player.getPlayer());
    }

    private String getWorldGuardRegion(OfflinePlayer player) {
        if (!worldGuardAvailable) return "";
        if (!player.isOnline() || player.getPlayer() == null) return "";
        try {
            Player pl = player.getPlayer();
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(pl.getWorld()));
            if (regions == null) return "";
            ApplicableRegionSet set = regions.getApplicableRegions(
                    BukkitAdapter.adapt(pl.getLocation()).toVector().toBlockPoint());
            if (set.size() == 0) return "";
            ProtectedRegion highest = null;
            for (ProtectedRegion r : set) {
                if (highest == null || r.getPriority() > highest.getPriority()) highest = r;
            }
            return highest != null ? highest.getId() : "";
        } catch (Throwable t) {
            return "";
        }
    }

    private String getRankFromOrder(OfflinePlayer player) {
        if (!player.isOnline() || player.getPlayer() == null) return "";
        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection ranks = cfg.getConfigurationSection("stafflist.ranks");
        if (ranks == null) return "";
        for (String order : cfg.getStringList("stafflist.order")) {
            String perm = ranks.getString(order + ".permission", "");
            if (!perm.isEmpty() && player.getPlayer().hasPermission(perm)) return order;
        }
        return "";
    }

    private String getRankSymbolFromOrder(OfflinePlayer player) {
        String rank = getRankFromOrder(player);
        if (rank.isEmpty()) return "";
        return plugin.getConfig().getString("stafflist.ranks." + rank + ".symbol", "");
    }

    private String getRankAny(OfflinePlayer player) {
        if (!player.isOnline() || player.getPlayer() == null) return "";
        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection ranks = cfg.getConfigurationSection("stafflist.ranks");
        if (ranks == null) return "";
        for (String order : cfg.getStringList("stafflist.order")) {
            String perm = ranks.getString(order + ".permission", "");
            if (!perm.isEmpty() && player.getPlayer().hasPermission(perm)) return order;
        }
        for (String rk : ranks.getKeys(false)) {
            String perm = ranks.getString(rk + ".permission", "");
            if (!perm.isEmpty() && player.getPlayer().hasPermission(perm)) return rk;
        }
        return "";
    }

    private String getRankSymbolAny(OfflinePlayer player) {
        String rank = getRankAny(player);
        if (rank.isEmpty()) return "";
        return plugin.getConfig().getString("stafflist.ranks." + rank + ".symbol", "");
    }

    private String getSymbolFor(String rank) {
        return plugin.getConfig().getString("stafflist.ranks." + rank + ".symbol", "");
    }

    private boolean hasRank(OfflinePlayer player, String rank) {
        if (!player.isOnline() || player.getPlayer() == null) return false;
        String perm = plugin.getConfig().getString("stafflist.ranks." + rank + ".permission", "");
        return !perm.isEmpty() && player.getPlayer().hasPermission(perm);
    }

    private boolean isStaff(OfflinePlayer player) {
        return !getRankFromOrder(player).isEmpty();
    }

    private String getEffectiveSymbol(OfflinePlayer player) {
        return getRankSymbolAny(player);
    }

    private String getEffectivePrefix(OfflinePlayer player) {
        String symbol = getEffectiveSymbol(player);
        if (symbol.isEmpty()) return "";
        return symbol + " ";
    }

    private boolean isVanished(OfflinePlayer player) {
        if (plugin.getVanishManager() == null) return false;
        return plugin.getVanishManager().isVanished(player.getUniqueId());
    }

    private String getVanishSuffix(OfflinePlayer player) {
        if (!isVanished(player)) return "";
        return VanishManager.getVanishSuffix();
    }

    private boolean isInStaffMode(OfflinePlayer player) {
        try {
            if (plugin.getStaffModeManager() == null) return false;
            return plugin.getStaffModeManager().isInStaffMode(player.getUniqueId());
        } catch (Throwable t) { return false; }
    }

    private boolean isStuckActive(OfflinePlayer player) {
        try { return plugin.stuckActive.containsKey(player.getUniqueId()); }
        catch (Throwable t) { return false; }
    }

    private int getStuckSeconds(OfflinePlayer player) {
        try {
            Integer s = plugin.stuckActive.get(player.getUniqueId());
            return s != null ? s : 0;
        } catch (Throwable t) { return 0; }
    }

    private int countAppuntamenti(OfflinePlayer player) {
        try {
            AppuntamentoManager am = plugin.getRoleplay().getAppuntamentoManager();
            if (am == null) return 0;
            int count = 0;
            for (var a : am.getAll()) if (a.getPlayer().equals(player.getUniqueId())) count++;
            return count;
        } catch (Throwable t) { return 0; }
    }

    private String getNextAppuntamento(OfflinePlayer player) {
        try {
            AppuntamentoManager am = plugin.getRoleplay().getAppuntamentoManager();
            if (am == null) return "";
            for (var a : am.getAll()) if (a.getPlayer().equals(player.getUniqueId()))
                return a.getGiorno() + " " + a.getOrario() + " (" + a.getReparto() + ")";
            return "";
        } catch (Throwable t) { return ""; }
    }

    private String getNextAppuntamentoDay(OfflinePlayer player) {
        try {
            AppuntamentoManager am = plugin.getRoleplay().getAppuntamentoManager();
            if (am == null) return "";
            for (var a : am.getAll()) if (a.getPlayer().equals(player.getUniqueId())) return a.getGiorno();
            return "";
        } catch (Throwable t) { return ""; }
    }

    private String getNextAppuntamentoTime(OfflinePlayer player) {
        try {
            AppuntamentoManager am = plugin.getRoleplay().getAppuntamentoManager();
            if (am == null) return "";
            for (var a : am.getAll()) if (a.getPlayer().equals(player.getUniqueId())) return a.getOrario();
            return "";
        } catch (Throwable t) { return ""; }
    }

    private String getNextAppuntamentoReparto(OfflinePlayer player) {
        try {
            AppuntamentoManager am = plugin.getRoleplay().getAppuntamentoManager();
            if (am == null) return "";
            for (var a : am.getAll()) if (a.getPlayer().equals(player.getUniqueId())) return a.getReparto();
            return "";
        } catch (Throwable t) { return ""; }
    }

    private boolean isCurrentWorldProtected(OfflinePlayer player) {
        if (!player.isOnline() || player.getPlayer() == null) return false;
        try {
            ProtectionManager pm = plugin.getRoleplay().getProtectionManager();
            if (pm == null) return false;
            String w = player.getPlayer().getWorld().getName();
            return pm.isNoBuild(w) || pm.isNoInteract(w);
        } catch (Throwable t) { return false; }
    }

    private boolean isCurrentWorldNoBuild(OfflinePlayer player) {
        if (!player.isOnline() || player.getPlayer() == null) return false;
        try {
            ProtectionManager pm = plugin.getRoleplay().getProtectionManager();
            if (pm == null) return false;
            return pm.isNoBuild(player.getPlayer().getWorld().getName());
        } catch (Throwable t) { return false; }
    }

    private boolean isCurrentWorldNoInteract(OfflinePlayer player) {
        if (!player.isOnline() || player.getPlayer() == null) return false;
        try {
            ProtectionManager pm = plugin.getRoleplay().getProtectionManager();
            if (pm == null) return false;
            return pm.isNoInteract(player.getPlayer().getWorld().getName());
        } catch (Throwable t) { return false; }
    }

    private boolean isCurrentWorldWhitelisted(OfflinePlayer player) {
        if (!player.isOnline() || player.getPlayer() == null) return false;
        try {
            ProtectionManager pm = plugin.getRoleplay().getProtectionManager();
            if (pm == null) return false;
            return pm.isWhitelisted(player.getPlayer().getWorld().getName(), player.getPlayer());
        } catch (Throwable t) { return false; }
    }

    private int countTotalTickets(OfflinePlayer player) {
        return tickets.listByAuthor(player.getUniqueId()).size();
    }

    private int countClosedTickets(OfflinePlayer player) {
        int c = 0;
        for (Ticket t : tickets.listByAuthor(player.getUniqueId())) if (!t.isOpen()) c++;
        return c;
    }

    private String getLastTicketId(OfflinePlayer player) {
        var list = tickets.listByAuthor(player.getUniqueId());
        if (list.isEmpty()) return "-";
        return String.valueOf(list.get(0).getId());
    }

    private int countVanished() {
        if (plugin.getVanishManager() == null) return 0;
        return plugin.getVanishManager().getVanished().size();
    }

    private int countStaffMode() {
        try {
            if (plugin.getStaffModeManager() == null) return 0;
            return plugin.getStaffModeManager().getAllInStaffMode().size();
        } catch (Throwable t) { return 0; }
    }

    private int countStaffOnline() {
        int c = 0;
        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection ranks = cfg.getConfigurationSection("stafflist.ranks");
        if (ranks == null) return 0;
        for (Player pl : Bukkit.getOnlinePlayers()) {
            for (String r : cfg.getStringList("stafflist.order")) {
                String perm = ranks.getString(r + ".permission", "");
                if (!perm.isEmpty() && pl.hasPermission(perm)) { c++; break; }
            }
        }
        return c;
    }

    private int countNonStaffOnline() {
        return Bukkit.getOnlinePlayers().size() - countStaffOnline();
    }

    private double getTps() {
        try {
            Object server = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
            double[] tps = (double[]) server.getClass().getField("recentTps").get(server);
            return tps[0];
        } catch (Throwable t) { return 20.0; }
    }
}