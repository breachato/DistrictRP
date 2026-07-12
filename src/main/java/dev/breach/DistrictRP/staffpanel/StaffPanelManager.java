package dev.breach.DistrictRP.staffpanel;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.tables.*;
import dev.breach.DistrictRP.staffpanel.http.StaffPanelHttpServer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class StaffPanelManager {

    private final DistrictRP plugin;

    private StaffAccountsTable accounts;
    private StaffPanelDepartmentsTable departments;
    private StaffPanelDeptColumnsTable columns;
    private StaffPanelStaffDeptsTable staffDept;
    private StaffPanelCountersTable counters;
    private StaffPanelFlagsTable flags;

    private StaffPanelHttpServer httpServer;
    private boolean enabled = false;

    public StaffPanelManager(DistrictRP plugin) {
        this.plugin = plugin;
    }

    public boolean enable() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("staff-panel");
        if (sec == null || !sec.getBoolean("enabled", true)) {
            plugin.getLogger().info("[StaffPanel] Disabilitato in config.");
            return false;
        }

        var dbm = plugin.getDatabaseManager();
        if (dbm == null || !dbm.isMariaDb()) {
            plugin.getLogger().warning("[StaffPanel] MariaDB non disponibile: modulo NON avviato.");
            return false;
        }

        var ds = dbm.getDataStore();

        this.accounts = new StaffAccountsTable(plugin, ds);
        this.departments = new StaffPanelDepartmentsTable(plugin, ds);
        this.columns = new StaffPanelDeptColumnsTable(plugin, ds);
        this.staffDept = new StaffPanelStaffDeptsTable(plugin, ds);
        this.counters = new StaffPanelCountersTable(plugin, ds);
        this.flags = new StaffPanelFlagsTable(plugin, ds);

        dbm.registerTable("staff_accounts", accounts);
        dbm.registerTable("sp_departments", departments);
        dbm.registerTable("sp_department_columns", columns);
        dbm.registerTable("sp_staff_departments", staffDept);
        dbm.registerTable("sp_staff_counters", counters);
        dbm.registerTable("sp_staff_flags", flags);

        if (sec.getBoolean("departments.seed-on-first-run", true)) {
            seedDefaults(sec);
        }

        if (sec.getBoolean("staff-sync.auto-on-enable", true)) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this::syncStaffFromRanks, 40L);
        }

        ConfigurationSection http = sec.getConfigurationSection("http");
        if (http != null && http.getBoolean("enabled", true)) {
            String bind = http.getString("bind", "127.0.0.1");
            int port = http.getInt("port", 8787);
            String token = http.getString("api-token", "");
            try {
                this.httpServer = new StaffPanelHttpServer(this, bind, port, token);
                this.httpServer.start();
                plugin.getLogger().info("[StaffPanel] HTTP server avviato su " + bind + ":" + port);
            } catch (Throwable t) {
                plugin.getLogger().severe("[StaffPanel] Errore avvio HTTP: " + t.getMessage());
            }
        }

        this.enabled = true;
        plugin.getLogger().info("[StaffPanel] Manager abilitato.");
        return true;
    }

    public void disable() {
        if (httpServer != null) {
            try { httpServer.stop(); } catch (Throwable ignored) {}
        }
        this.enabled = false;
    }

    public boolean isEnabled() { return enabled; }

    public DistrictRP plugin() { return plugin; }
    public StaffAccountsTable accounts() { return accounts; }
    public StaffPanelDepartmentsTable departments() { return departments; }
    public StaffPanelDeptColumnsTable columns() { return columns; }
    public StaffPanelStaffDeptsTable staffDept() { return staffDept; }
    public StaffPanelCountersTable counters() { return counters; }
    public StaffPanelFlagsTable flags() { return flags; }

    private void seedDefaults(ConfigurationSection sec) {
        List<Map<?, ?>> defaults = sec.getMapList("departments.defaults");
        if (defaults == null || defaults.isEmpty()) return;
        for (Map<?, ?> raw : defaults) {
            String id = String.valueOf(raw.get("id"));
            String name = String.valueOf(raw.get("name"));
            String color = String.valueOf(raw.getOrDefault("color", "#c9a84c"));
            int order = raw.get("order") instanceof Number n ? n.intValue() : 0;
            List<String> cols = new ArrayList<>();
            Object c = raw.get("columns");
            if (c instanceof List<?> l) {
                for (Object o : l) cols.add(String.valueOf(o));
            }
            departments.createIfMissing(id, name, color, order).thenAccept(created -> {
                for (int i = 0; i < cols.size(); i++) {
                    columns.add(id, cols.get(i), i);
                }
            });
        }
    }

    public int syncStaffFromRanks() {
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("stafflist.ranks");
        if (ranks == null) return 0;

        List<String> allowed = plugin.getConfig().getStringList("staff-panel.staff-sync.from-ranks");
        String defaultDept = plugin.getConfig().getString("staff-panel.staff-sync.default-department", "generale");

        int count = 0;
        for (String key : ranks.getKeys(false)) {
            if (!allowed.isEmpty() && !allowed.contains(key.toLowerCase())) continue;
            String perm = ranks.getString(key + ".permission");
            if (perm == null || perm.isEmpty()) continue;

            for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                var p = op.getPlayer();
                if (p != null && p.hasPermission(perm)) {
                    String name = op.getName() != null ? op.getName() : op.getUniqueId().toString();
                    staffDept.assign(op.getUniqueId(), name, defaultDept).join();
                    count++;
                }
            }
        }
        return count;
    }
}