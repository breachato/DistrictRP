package dev.breach.DistrictRP.commands.roleplay.chat;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.roleplay.profile.RPProfile;
import dev.breach.DistrictRP.commands.roleplay.profile.RPProfileManager;
import dev.breach.DistrictRP.functions.MessageUtils;
import dev.breach.DistrictRP.functions.servermode.ServerMode;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;

public class ChatModule implements Listener {

    public enum ChatType {
        AZIONE("azione", 15),
        BISBIGLIO("bisbiglio", 15),
        URLO("urlo", 25);

        private final String configKey;
        private final int defaultRadius;

        ChatType(String configKey, int defaultRadius) {
            this.configKey = configKey;
            this.defaultRadius = defaultRadius;
        }

        public String getConfigKey() { return configKey; }
        public int getDefaultRadius() { return defaultRadius; }
    }

    private final DistrictRP plugin;
    private final RPProfileManager profiles;

    public ChatModule(DistrictRP plugin, RPProfileManager profiles) {
        this.plugin = plugin;
        this.profiles = profiles;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        String message = event.getMessage();

        ServerMode mode = plugin.getServerModeManager() != null
                ? plugin.getServerModeManager().getCurrent() : ServerMode.OFF;

        if (mode == ServerMode.LOBBY || mode == ServerMode.CREATIVE) {
            handleModeChat(event, sender, message, mode);
            return;
        }

        if (mode == ServerMode.ROLEPLAY) {
            handleRoleplayChat(event, sender, message);
            return;
        }

        if (!plugin.getConfig().getBoolean("chat.proximity.enabled", true)) return;
        handleProximityChat(event, sender, message);
    }

    private void handleRoleplayChat(AsyncPlayerChatEvent event, Player sender, String message) {
        event.setCancelled(true);

        double radius = plugin.getConfig().getDouble("chat.proximity.radius", 15);
        RPProfile profile = profiles.get(sender.getUniqueId());

        String chatPrefix = buildRoleplayChatPrefix(sender, profile);
        String displayName = resolveDisplayName(sender, profile);
        String nameColor = resolveNameColor(sender);
        String msgColor = resolveMsgColor(sender);

        String format = plugin.getConfig().getString("chat.formats.roleplay",
                "%chat_prefix%%name_color%%display_name% &8» %msg_color%%message%");

        String legacy = format
                .replace("%chat_prefix%", chatPrefix)
                .replace("%name_color%", nameColor)
                .replace("%display_name%", displayName)
                .replace("%msg_color%", msgColor)
                .replace("%message%", message)
                .replace("%player%", sender.getName())
                .replace("%rp_name%", profile.hasRpName() ? profile.getRpName() : sender.getName())
                .replace("%rp_surname%", profile.getRpSurname() == null ? "" : profile.getRpSurname())
                .replace("%rp_fullname%", profile.getRpFullName().isEmpty() ? sender.getName() : profile.getRpFullName())
                .replace("%cf%", sender.getName());

        legacy = processPapi(sender, legacy);
        legacy = MessageUtils.color(legacy);

        BaseComponent[] component = buildHoverComponent(sender, profile, legacy, false);

        Location loc = sender.getLocation();
        boolean nobody = true;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().equals(loc.getWorld())) continue;
            if (p.equals(sender)) {
                p.spigot().sendMessage(component);
                continue;
            }
            if (p.getLocation().distance(loc) <= radius) {
                p.spigot().sendMessage(component);
                nobody = false;
            }
        }

        if (nobody && plugin.getConfig().getBoolean("chat.proximity.nobody-in-range-message", false)) {
            MessageUtils.sendMsg(sender, "chat.proximity-nobody");
        }
    }

    private void handleProximityChat(AsyncPlayerChatEvent event, Player sender, String message) {
        event.setCancelled(true);

        double radius = plugin.getConfig().getDouble("chat.proximity.radius", 15);
        RPProfile profile = profiles.get(sender.getUniqueId());
        String rpName = profile.hasRpName() ? profile.getRpName() : sender.getName();

        String formatRaw = plugin.getConfig().getString("chat.formats.normal",
                "&7%rp_name% &8» &f%message%");

        String legacy = replaceAll(formatRaw, sender.getName(), rpName,
                profile.getRpSurname() == null ? "" : profile.getRpSurname(),
                profile.getRpFullName().isEmpty() ? sender.getName() : profile.getRpFullName(),
                message);

        legacy = processPapi(sender, legacy);
        legacy = MessageUtils.color(legacy);

        BaseComponent[] component = buildHoverComponent(sender, profile, legacy, false);

        Location loc = sender.getLocation();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().equals(loc.getWorld())) continue;
            if (p.equals(sender)) {
                p.spigot().sendMessage(component);
                continue;
            }
            if (p.getLocation().distance(loc) <= radius) {
                p.spigot().sendMessage(component);
            }
        }
    }

    public static void broadcastRp(DistrictRP plugin, Player sender, String message, ChatType type) {
        RPProfileManager profiles = plugin.getRoleplay() != null
                ? plugin.getRoleplay().getProfileManager() : null;
        RPProfile profile = profiles != null ? profiles.get(sender.getUniqueId()) : null;
        String rpName = (profile != null && profile.hasRpName()) ? profile.getRpName() : sender.getName();
        String rpSurname = (profile != null && profile.getRpSurname() != null) ? profile.getRpSurname() : "";
        String rpFullName = (profile != null && !profile.getRpFullName().isEmpty()) ? profile.getRpFullName() : sender.getName();

        String base = "chat.formats." + type.getConfigKey();
        double radius = plugin.getConfig().getDouble(base + ".radius", type.getDefaultRadius());
        boolean hasRp = profile != null && profile.hasRpName();
        String pathFormat = hasRp ? base + ".format" : base + ".anonymous";

        String formatRaw = plugin.getConfig().getString(pathFormat,
                plugin.getConfig().getString(base + ".format", "&7%rp_name% &8» &f%message%"));

        String legacy = replaceAllStatic(formatRaw, sender.getName(), rpName, rpSurname, rpFullName, message);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            legacy = PlaceholderAPI.setPlaceholders(sender, legacy);
        }
        legacy = MessageUtils.color(legacy);

        Location loc = sender.getLocation();
        BaseComponent[] component = TextComponent.fromLegacyText(legacy);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().equals(loc.getWorld())) continue;
            if (p.equals(sender)) {
                p.spigot().sendMessage(component);
                continue;
            }
            if (p.getLocation().distance(loc) <= radius) {
                p.spigot().sendMessage(component);
            }
        }
    }

    private void handleModeChat(AsyncPlayerChatEvent event, Player sender, String message, ServerMode mode) {
        event.setCancelled(true);

        String rankSymbol = getRankSymbol(sender);
        String vipSuffix = getVipSuffix(sender);

        String formatRaw = plugin.getConfig().getString(
                "server-mode.modes." + mode.name() + ".chat-format",
                "%rank%&f%player%%vip% &8» &7%message%");

        if (formatRaw == null || formatRaw.equalsIgnoreCase("null") || formatRaw.isEmpty()) return;

        String legacy = formatRaw
                .replace("%rank%", rankSymbol.isEmpty() ? "" : rankSymbol + " ")
                .replace("%player%", sender.getName())
                .replace("%vip%", vipSuffix.isEmpty() ? "" : " " + vipSuffix)
                .replace("%message%", message);

        legacy = processPapi(sender, legacy);
        legacy = MessageUtils.color(legacy);

        RPProfile profile = profiles.get(sender.getUniqueId());
        BaseComponent[] component = buildHoverComponent(sender, profile, legacy, true);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.spigot().sendMessage(component);
        }
    }

    private String buildRoleplayChatPrefix(Player sender, RPProfile profile) {
        StringBuilder prefix = new StringBuilder();

        String aziendaDisplay = getAziendaDisplay(profile);
        if (!aziendaDisplay.isEmpty()) prefix.append(aziendaDisplay).append(" ");

        String ruoloDisplay = getAziendaRuoloDisplay(profile);
        if (!ruoloDisplay.isEmpty()) prefix.append(ruoloDisplay).append(" ");

        String staffRank = getStaffRank(sender);
        if (!staffRank.isEmpty()) {
            String staffSymbol = getStaffSymbol(staffRank);
            if (!staffSymbol.isEmpty()) prefix.append(staffSymbol).append(" ");
        }

        boolean highStaff = isHighStaff(staffRank);
        if (!highStaff) {
            String vipSym = getVipSymbolForChat(sender);
            if (!vipSym.isEmpty()) prefix.append("&f").append(vipSym).append(" ");
        }

        return prefix.toString();
    }

    private String resolveDisplayName(Player sender, RPProfile profile) {
        String staffRank = getStaffRank(sender);
        boolean highStaff = isHighStaff(staffRank);

        if (highStaff) {
            if (profile.hasRpName()) return profile.getRpFullName();
            return sender.getName();
        }

        if (profile.hasRpName()) return profile.getRpFullName();
        return sender.getName();
    }

    private String resolveNameColor(Player sender) {
        String staffRank = getStaffRank(sender);
        if (isHighStaff(staffRank)) {
            return plugin.getConfig().getString("chat-rules.high-staff-name-color", "&f");
        }
        return plugin.getConfig().getString("chat-rules.default-name-color", "&7");
    }

    private String resolveMsgColor(Player sender) {
        String staffRank = getStaffRank(sender);
        if (isHighStaff(staffRank)) {
            return plugin.getConfig().getString("chat-rules.high-staff-msg-color", "&f");
        }
        return plugin.getConfig().getString("chat-rules.default-msg-color", "&7");
    }

    private String getAziendaDisplay(RPProfile profile) {
        if (!profile.hasAzienda()) return "";
        String az = profile.getAzienda().toLowerCase();
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("stafflist.ranks");
        if (ranks == null) return "";
        String symbol = ranks.getString(az + ".symbol", "");
        if (!symbol.isEmpty()) return symbol;
        String color = ranks.getString(az + ".color", "&f");
        return color + profile.getAzienda();
    }

    private String getAziendaRuoloDisplay(RPProfile profile) {
        if (!profile.hasAzienda()) return "";
        String ruolo = profile.getAziendaRuolo();
        if (ruolo == null || ruolo.isEmpty()) return "";
        String az = profile.getAzienda().toLowerCase();
        String color = plugin.getConfig().getString("stafflist.ranks." + az + ".color", "&f");
        return color + "[" + ruolo + "]";
    }

    private String getStaffRank(Player player) {
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("stafflist.ranks");
        if (ranks == null) return "";
        for (String order : plugin.getConfig().getStringList("stafflist.order")) {
            String perm = ranks.getString(order + ".permission", "");
            if (!perm.isEmpty() && player.hasPermission(perm)) return order;
        }
        return "";
    }

    private String getStaffSymbol(String rank) {
        if (rank.isEmpty()) return "";
        return plugin.getConfig().getString("stafflist.ranks." + rank + ".symbol", "");
    }

    private boolean isHighStaff(String rank) {
        if (rank == null || rank.isEmpty()) return false;
        List<String> highRanks = plugin.getConfig().getStringList("chat-rules.high-staff-ranks");
        return highRanks.contains(rank.toLowerCase());
    }

    private String getVipSymbolForChat(Player player) {
        ConfigurationSection vips = plugin.getConfig().getConfigurationSection("vip-symbols");
        if (vips == null) return "";
        for (String order : plugin.getConfig().getStringList("vip-symbols.order")) {
            ConfigurationSection v = vips.getConfigurationSection(order);
            if (v == null) continue;
            String perm = v.getString("permission", "");
            String symbol = v.getString("symbol", "");
            if (perm.isEmpty() || symbol == null || symbol.isEmpty()) continue;
            boolean hasExplicit = player.getEffectivePermissions().stream()
                    .anyMatch(pai -> pai.getPermission().equalsIgnoreCase(perm) && pai.getValue());
            if (hasExplicit) return symbol;
        }
        return "";
    }

    private String getRankSymbol(Player player) {
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("stafflist.ranks");
        if (ranks == null) return "";
        for (String order : plugin.getConfig().getStringList("stafflist.order")) {
            ConfigurationSection r = ranks.getConfigurationSection(order);
            if (r == null) continue;
            String perm = r.getString("permission", "");
            String symbol = r.getString("symbol", "");
            if (!perm.isEmpty() && player.hasPermission(perm) && symbol != null && !symbol.isEmpty()) {
                return symbol;
            }
        }
        return "";
    }

    private String getVipSuffix(Player player) {
        ConfigurationSection vips = plugin.getConfig().getConfigurationSection("vip-symbols");
        if (vips == null) return "";
        for (String order : plugin.getConfig().getStringList("vip-symbols.order")) {
            ConfigurationSection v = vips.getConfigurationSection(order);
            if (v == null) continue;
            String perm = v.getString("permission", "");
            String symbol = v.getString("symbol", "");
            if (perm.isEmpty() || symbol == null || symbol.isEmpty()) continue;
            boolean hasExplicit = player.getEffectivePermissions().stream()
                    .anyMatch(pai -> pai.getPermission().equalsIgnoreCase(perm) && pai.getValue());
            if (hasExplicit) return symbol;
        }
        return "";
    }

    private BaseComponent[] buildHoverComponent(Player sender, RPProfile profile, String legacyMessage, boolean modeChat) {
        boolean hoverEnabled = plugin.getConfig().getBoolean("chat.hover.enabled", true);
        TextComponent[] parts = fromLegacy(legacyMessage);
        if (!hoverEnabled) return parts;

        String rpName = profile.hasRpName() ? profile.getRpName() : sender.getName();
        String rpSurname = profile.getRpSurname() == null ? "" : profile.getRpSurname();
        String rpFullName = profile.getRpFullName().isEmpty() ? sender.getName() : profile.getRpFullName();
        String cf = sender.getName();

        List<String> hoverLines;

        if (modeChat) {
            ServerMode mode = plugin.getServerModeManager() != null
                    ? plugin.getServerModeManager().getCurrent() : ServerMode.OFF;
            hoverLines = plugin.getConfig().getStringList("server-mode.modes." + mode.name() + ".hover-lines");
            if (hoverLines.isEmpty()) {
                hoverLines = List.of("&7Nome: &f" + cf, "", "&#FCD05Cclick per copiare");
            }
        } else if (profile.hasRpName()) {
            hoverLines = plugin.getConfig().getStringList("chat.hover.lines");
            if (hoverLines.isEmpty()) {
                hoverLines = List.of("&7Nome: &f" + rpName, "&7Cognome: &f" + rpSurname,
                        "&7CF: &f" + cf, "", "&#FCD05Cclick per copiare");
            }
        } else {
            hoverLines = plugin.getConfig().getStringList("chat.hover.lines-no-rp");
            if (hoverLines.isEmpty()) {
                hoverLines = List.of("&7Nome: &f" + cf, "", "&#FCD05Cclick per copiare");
            }
        }

        StringBuilder hoverText = new StringBuilder();
        for (int i = 0; i < hoverLines.size(); i++) {
            String line = replaceAll(hoverLines.get(i), cf, rpName, rpSurname, rpFullName, "");
            line = processPapi(sender, line);
            hoverText.append(MessageUtils.color(line));
            if (i < hoverLines.size() - 1) hoverText.append("\n");
        }

        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(TextComponent.fromLegacyText(hoverText.toString())));
        ClickEvent click = new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, cf);

        for (TextComponent part : parts) {
            part.setHoverEvent(hover);
            part.setClickEvent(click);
        }
        return parts;
    }

    private String replaceAll(String s, String cf, String rpName, String rpSurname, String rpFullName, String message) {
        return s.replace("%player%", cf)
                .replace("%cf%", cf)
                .replace("%rp_name%", rpName)
                .replace("%rp_surname%", rpSurname)
                .replace("%rp_fullname%", rpFullName)
                .replace("%message%", message);
    }

    private static String replaceAllStatic(String s, String cf, String rpName, String rpSurname, String rpFullName, String message) {
        return s.replace("%player%", cf)
                .replace("%cf%", cf)
                .replace("%rp_name%", rpName)
                .replace("%rp_surname%", rpSurname)
                .replace("%rp_fullname%", rpFullName)
                .replace("%message%", message);
    }

    private String processPapi(Player sender, String text) {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return PlaceholderAPI.setPlaceholders(sender, text);
        }
        return text;
    }

    private TextComponent[] fromLegacy(String legacy) {
        BaseComponent[] base = TextComponent.fromLegacyText(legacy);
        TextComponent[] out = new TextComponent[base.length];
        for (int i = 0; i < base.length; i++) {
            if (base[i] instanceof TextComponent tc) {
                out[i] = tc;
            } else {
                TextComponent tc = new TextComponent();
                tc.addExtra(base[i]);
                out[i] = tc;
            }
        }
        return out;
    }
}