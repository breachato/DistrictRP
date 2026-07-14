package dev.breach.DistrictRP.database;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.roleplay.appuntamenti.AppuntamentoManager;
import dev.breach.DistrictRP.commands.roleplay.chat.ChatSymManager;
import dev.breach.DistrictRP.commands.roleplay.playtime.PlaytimeData;
import dev.breach.DistrictRP.commands.roleplay.playtime.PlaytimeTracker;
import dev.breach.DistrictRP.commands.roleplay.profile.RPProfile;
import dev.breach.DistrictRP.commands.roleplay.profile.RPProfileManager;
import dev.breach.DistrictRP.commands.roleplay.ticket.Ticket;
import dev.breach.DistrictRP.commands.roleplay.ticket.TicketComment;
import dev.breach.DistrictRP.commands.roleplay.ticket.TicketManager;
import dev.breach.DistrictRP.commands.roleplay.warp.Warp;
import dev.breach.DistrictRP.commands.roleplay.warp.WarpManager;
import dev.breach.DistrictRP.functions.MessageUtils;
import dev.breach.DistrictRP.functions.VanishManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class MigrationCommand {

    private final DistrictRP plugin;

    public MigrationCommand(DistrictRP plugin) {
        this.plugin = plugin;
    }

    public void migrateAll(CommandSender sender) {
        if (plugin.getDatabaseManager() == null || !plugin.getDatabaseManager().isMariaDb()) {
            MessageUtils.sendMsg(sender, "database.mariadb-inactive");
            return;
        }

        MessageUtils.sendMsg(sender, "database.migration-start");

        migrateProfiles(sender);
        migrateWarps(sender);
        migratePlaytime(sender);
        migrateTickets(sender);
        migrateAppuntamenti(sender);
        migrateVanish(sender);
        migrateChatSym(sender);
    }

    private void mgrUnavailable(CommandSender sender, String name) {
        MessageUtils.sendMsg(sender, "database.migration-mgr-unavailable", "mgr", name);
    }

    private void repoUnavailable(CommandSender sender, String name) {
        MessageUtils.sendMsg(sender, "database.migration-repo-unavailable", "repo", name);
    }

    private void none(CommandSender sender, String type) {
        MessageUtils.sendMsg(sender, "database.migration-none", "type", type);
    }

    private void fileMissing(CommandSender sender, String file) {
        MessageUtils.sendMsg(sender, "database.migration-file-missing", "file", file);
    }

    private void inProgress(CommandSender sender, String type, int count) {
        MessageUtils.sendMsg(sender, "database.migration-in-progress",
                "type", type, "count", String.valueOf(count));
    }

    private void done(CommandSender sender, String type, int ok, int total, int fail) {
        if (fail > 0) {
            MessageUtils.sendMsg(sender, "database.migration-done-with-fails",
                    "type", type, "ok", String.valueOf(ok),
                    "total", String.valueOf(total), "fail", String.valueOf(fail));
        } else {
            MessageUtils.sendMsg(sender, "database.migration-done",
                    "type", type, "ok", String.valueOf(ok), "total", String.valueOf(total));
        }
    }

    private void migrateProfiles(CommandSender sender) {
        RPProfileManager mgr = plugin.getRoleplay() != null ? plugin.getRoleplay().getProfileManager() : null;
        if (mgr == null) { mgrUnavailable(sender, "ProfileManager"); return; }
        if (!mgr.isUsingDatabase()) { repoUnavailable(sender, "ProfileRepository"); return; }

        int total = mgr.getAll().size();
        if (total == 0) { none(sender, "profili"); return; }

        inProgress(sender, "profili", total);

        AtomicInteger ok = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);

        for (RPProfile p : mgr.getAll().values()) {
            mgr.saveProfile(p).thenAccept(success -> {
                if (Boolean.TRUE.equals(success)) ok.incrementAndGet();
                else fail.incrementAndGet();
                if (ok.get() + fail.get() == total) done(sender, "profili", ok.get(), total, fail.get());
            });
        }
    }

    private void migrateWarps(CommandSender sender) {
        WarpManager mgr = plugin.getRoleplay() != null ? plugin.getRoleplay().getWarpManager() : null;
        if (mgr == null) { mgrUnavailable(sender, "WarpManager"); return; }
        if (!mgr.isUsingDatabase()) { repoUnavailable(sender, "WarpRepository"); return; }

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("warp.data");
        if (sec == null || sec.getKeys(false).isEmpty()) { none(sender, "warp"); return; }

        int total = sec.getKeys(false).size();
        inProgress(sender, "warp", total);

        AtomicInteger ok = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);

        for (String key : sec.getKeys(false)) {
            ConfigurationSection w = sec.getConfigurationSection(key);
            if (w == null) { fail.incrementAndGet(); continue; }
            Warp warp = new Warp(
                    key,
                    w.getString("world", "world"),
                    w.getDouble("x"), w.getDouble("y"), w.getDouble("z"),
                    (float) w.getDouble("yaw", 0.0),
                    (float) w.getDouble("pitch", 0.0),
                    w.getString("permission", "")
            );
            mgr.saveWarp(warp).thenAccept(success -> {
                if (Boolean.TRUE.equals(success)) ok.incrementAndGet();
                else fail.incrementAndGet();
                if (ok.get() + fail.get() == total) done(sender, "warp", ok.get(), total, fail.get());
            });
        }
    }

    private void migratePlaytime(CommandSender sender) {
        PlaytimeTracker tracker = plugin.getRoleplay() != null ? plugin.getRoleplay().getPlaytimeTracker() : null;
        if (tracker == null) { mgrUnavailable(sender, "PlaytimeTracker"); return; }
        if (!tracker.isUsingDatabase()) { repoUnavailable(sender, "PlaytimeRepository"); return; }

        File file = new File(new File(plugin.getDataFolder(), "roleplay"), "playtime.yml");
        if (!file.exists()) { fileMissing(sender, "playtime.yml"); return; }

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = yaml.getConfigurationSection("players");
        if (sec == null || sec.getKeys(false).isEmpty()) { none(sender, "playtime"); return; }

        int total = sec.getKeys(false).size();
        inProgress(sender, "playtime", total);

        AtomicInteger ok = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);

        for (String key : sec.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String base = "players." + key + ".";
                PlaytimeData d = new PlaytimeData();
                d.setTotalSeconds(yaml.getLong(base + "total", 0));
                d.setDailySeconds(yaml.getLong(base + "daily", 0));
                d.setWeeklySeconds(yaml.getLong(base + "weekly", 0));
                d.setMonthlySeconds(yaml.getLong(base + "monthly", 0));
                d.setDailyReset(yaml.getLong(base + "daily-reset", System.currentTimeMillis()));
                d.setWeeklyReset(yaml.getLong(base + "weekly-reset", System.currentTimeMillis()));
                d.setMonthlyReset(yaml.getLong(base + "monthly-reset", System.currentTimeMillis()));

                tracker.save(uuid, d).thenAccept(success -> {
                    if (Boolean.TRUE.equals(success)) ok.incrementAndGet();
                    else fail.incrementAndGet();
                    if (ok.get() + fail.get() == total) done(sender, "playtime", ok.get(), total, fail.get());
                });
            } catch (Exception e) {
                fail.incrementAndGet();
            }
        }
    }

    private void migrateTickets(CommandSender sender) {
        TicketManager mgr = plugin.getRoleplay() != null ? plugin.getRoleplay().getTicketManager() : null;
        if (mgr == null) { mgrUnavailable(sender, "TicketManager"); return; }
        if (!mgr.isUsingDatabase()) { repoUnavailable(sender, "TicketRepository"); return; }

        File file = new File(new File(plugin.getDataFolder(), "roleplay"), "tickets.yml");
        if (!file.exists()) { fileMissing(sender, "tickets.yml"); return; }

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = yaml.getConfigurationSection("tickets");
        if (sec == null || sec.getKeys(false).isEmpty()) { none(sender, "ticket"); return; }

        int total = sec.getKeys(false).size();
        inProgress(sender, "ticket", total);

        String serverOrigin = plugin.getConfig().getString("server-id", Bukkit.getServer().getName());
        AtomicInteger ok = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);

        for (String key : sec.getKeys(false)) {
            try {
                int oldId = Integer.parseInt(key);
                String base = "tickets." + key + ".";
                UUID author = UUID.fromString(yaml.getString(base + "author"));
                String authorName = yaml.getString(base + "author-name", "?");
                String category = yaml.getString(base + "category", "generale");
                String reason = yaml.getString(base + "reason", "");
                long openedAt = yaml.getLong(base + "opened-at", System.currentTimeMillis());
                boolean open = yaml.getBoolean(base + "open", true);

                Ticket t = new Ticket(oldId, author, authorName, category, reason, openedAt);
                t.setOpen(open);
                String cb = yaml.getString(base + "closed-by", null);
                if (cb != null) t.setClosedBy(UUID.fromString(cb));
                t.setClosedByName(yaml.getString(base + "closed-by-name", null));
                t.setCloseReason(yaml.getString(base + "close-reason", null));
                t.setClosedAt(yaml.getLong(base + "closed-at", 0));
                String clb = yaml.getString(base + "claimed-by", null);
                if (clb != null) t.setClaimedBy(UUID.fromString(clb));
                t.setClaimedByName(yaml.getString(base + "claimed-by-name", null));

                List<String> commentsRaw = yaml.getStringList(base + "comments");
                for (String cs : commentsRaw) {
                    TicketComment tc = TicketComment.deserialize(cs);
                    if (tc != null) t.addComment(tc);
                }

                Ticket toMigrate = t;
                mgr.createTicket(toMigrate, serverOrigin).thenAccept(newId -> {
                    if (newId == null || newId < 0) {
                        fail.incrementAndGet();
                    } else {
                        if (toMigrate.isClaimed()) {
                            mgr.claimTicket(newId, toMigrate.getClaimedBy(), toMigrate.getClaimedByName());
                        }
                        if (!toMigrate.isOpen()) {
                            mgr.closeTicket(newId,
                                    toMigrate.getClosedBy() != null ? toMigrate.getClosedBy() : new UUID(0L, 0L),
                                    toMigrate.getClosedByName(),
                                    toMigrate.getCloseReason());
                        }
                        for (TicketComment c : toMigrate.getComments()) {
                            mgr.addComment(newId, c);
                        }
                        ok.incrementAndGet();
                    }
                    if (ok.get() + fail.get() == total) done(sender, "ticket", ok.get(), total, fail.get());
                });
            } catch (Exception e) {
                fail.incrementAndGet();
            }
        }
    }

    private void migrateAppuntamenti(CommandSender sender) {
        AppuntamentoManager mgr = plugin.getRoleplay() != null ? plugin.getRoleplay().getAppuntamentoManager() : null;
        if (mgr == null) { mgrUnavailable(sender, "AppuntamentoManager"); return; }
        if (!mgr.isUsingDatabase()) { repoUnavailable(sender, "AppuntamentoRepository"); return; }

        File file = new File(new File(plugin.getDataFolder(), "roleplay"), "appuntamenti.yml");
        if (!file.exists()) { fileMissing(sender, "appuntamenti.yml"); return; }

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = yaml.getConfigurationSection("appuntamenti");
        if (sec == null || sec.getKeys(false).isEmpty()) { none(sender, "appuntamenti"); return; }

        int total = sec.getKeys(false).size();
        inProgress(sender, "appuntamenti", total);

        AtomicInteger ok = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);

        for (String key : sec.getKeys(false)) {
            try {
                String base = "appuntamenti." + key + ".";
                UUID player = UUID.fromString(yaml.getString(base + "player"));
                String name = yaml.getString(base + "player-name", "?");
                String reparto = yaml.getString(base + "reparto", "");
                String giorno = yaml.getString(base + "giorno", "");
                String orario = yaml.getString(base + "orario", "");

                mgr.bookDb(player, name, reparto, giorno, orario).thenAccept(newId -> {
                    if (newId != null && newId >= 0) ok.incrementAndGet();
                    else fail.incrementAndGet();
                    if (ok.get() + fail.get() == total) done(sender, "appuntamenti", ok.get(), total, fail.get());
                });
            } catch (Exception e) {
                fail.incrementAndGet();
            }
        }
    }

    private void migrateVanish(CommandSender sender) {
        VanishManager mgr = plugin.getVanishManager();
        if (mgr == null) { mgrUnavailable(sender, "VanishManager"); return; }
        if (!mgr.isUsingDatabase()) { repoUnavailable(sender, "VanishRepository"); return; }

        java.util.Set<String> vanishedRaw = plugin.getDataManager().getAllVanished();
        if (vanishedRaw == null || vanishedRaw.isEmpty()) { none(sender, "vanish"); return; }

        int total = vanishedRaw.size();
        inProgress(sender, "vanish", total);

        AtomicInteger ok = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);

        for (String s : vanishedRaw) {
            try {
                UUID uuid = UUID.fromString(s);
                mgr.setVanished(uuid, true).thenAccept(success -> {
                    if (Boolean.TRUE.equals(success)) ok.incrementAndGet();
                    else fail.incrementAndGet();
                    if (ok.get() + fail.get() == total) done(sender, "vanish", ok.get(), total, fail.get());
                });
            } catch (Exception e) {
                fail.incrementAndGet();
            }
        }
    }

    private void migrateChatSym(CommandSender sender) {
        ChatSymManager mgr = plugin.getRoleplay() != null ? plugin.getRoleplay().getChatSymManager() : null;
        if (mgr == null) { mgrUnavailable(sender, "ChatSymManager"); return; }
        if (!mgr.isUsingDatabase()) { repoUnavailable(sender, "ChatSymRepository"); return; }

        Map<String, String> syms = mgr.getSymbols();
        if (syms == null || syms.isEmpty()) { none(sender, "chatsym"); return; }

        int total = syms.size();
        inProgress(sender, "chatsym", total);

        AtomicInteger ok = new AtomicInteger(0);
        AtomicInteger fail = new AtomicInteger(0);

        for (Map.Entry<String, String> e : syms.entrySet()) {
            mgr.upsert(e.getKey(), e.getValue()).thenAccept(success -> {
                if (Boolean.TRUE.equals(success)) ok.incrementAndGet();
                else fail.incrementAndGet();
                if (ok.get() + fail.get() == total) done(sender, "chatsym", ok.get(), total, fail.get());
            });
        }
    }
}