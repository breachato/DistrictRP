package dev.breach.DistrictRP.functions.servermode;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.tables.ServerModeTable;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class ServerModeManager implements Listener {

    private final DistrictRP plugin;
    private ServerMode currentMode = ServerMode.OFF;

    private ServerModeTable table;
    private boolean useDb;

    public ServerModeManager(DistrictRP plugin) {
        this.plugin = plugin;
        var dbm = plugin.getDatabaseManager();
        this.table = (dbm != null && dbm.isMariaDb()) ? dbm.getTable("server_mode", ServerModeTable.class) : null;
        this.useDb = (table != null);
        loadFromConfig();
    }

    public void loadFromConfig() {
        if (useDb) {
            try {
                String fromDb = getMode(getServerId()).join();
                if (fromDb != null && !fromDb.isEmpty()) {
                    this.currentMode = ServerMode.fromString(fromDb);
                    plugin.getLogger().info("[ServerMode] Modalità caricata dal DB: " + currentMode.name());
                    return;
                }
            } catch (Exception ignored) {}
        }
        String raw = plugin.getConfig().getString("server-mode.current", "OFF");
        this.currentMode = ServerMode.fromString(raw);
        plugin.getLogger().info("[ServerMode] Modalità caricata da config: " + currentMode.name());
    }

    public ServerMode getCurrent() {
        return currentMode;
    }

    public String getCurrentDisplay() {
        String path = "server-mode.modes." + currentMode.name() + ".display";
        return MessageUtils.color(plugin.getConfig().getString(path, currentMode.name()));
    }

    private String getServerId() {
        return plugin.getConfig().getString("server-id", Bukkit.getServer().getName());
    }

    public boolean setMode(ServerMode mode) {
        return setMode(mode, "console");
    }

    public boolean setMode(ServerMode mode, String by) {
        if (mode == null) return false;
        if (mode == currentMode) return false;

        ServerMode old = this.currentMode;
        this.currentMode = mode;

        if (useDb) {
            setMode(getServerId(), mode.name(), by);
        } else {
            plugin.getConfig().set("server-mode.current", mode.name());
            plugin.saveConfig();
        }

        applyToAll(old, mode);

        if (plugin.getConfig().getBoolean("server-mode.broadcast-change", true)) {
            String msg = MessageUtils.get("servermode.broadcast", "mode", getCurrentDisplay());
            Bukkit.broadcastMessage(msg);
        }

        return true;
    }

    private void applyToAll(ServerMode oldMode, ServerMode newMode) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            applyToPlayer(p);
            showModeTitle(p, newMode);
        }
    }

    public void applyToPlayer(Player p) {
        if (canBypass(p)) return;

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection(
                "server-mode.modes." + currentMode.name());
        if (sec == null) return;

        String gm = sec.getString("force-gamemode");
        if (gm != null && !gm.isEmpty()) {
            try {
                GameMode g = GameMode.valueOf(gm.toUpperCase(Locale.ROOT));
                if (p.getGameMode() != g) p.setGameMode(g);
            } catch (IllegalArgumentException ignored) {}
        }

        if (sec.isSet("allow-fly")) {
            boolean allowFly = sec.getBoolean("allow-fly");
            if (!allowFly && !p.hasPermission("DistrictRP.fly")) {
                if (p.getAllowFlight()) p.setAllowFlight(false);
                if (p.isFlying()) p.setFlying(false);
            }
        }

        String targetWorld = sec.getString("auto-teleport-world");
        if (targetWorld != null && !targetWorld.isEmpty()
                && !p.getWorld().getName().equalsIgnoreCase(targetWorld)) {
            teleportToSpawn(p, targetWorld);
        }
    }

    public void handleJoin(Player p) {
        if (canBypass(p)) return;

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection(
                "server-mode.modes." + currentMode.name());
        if (sec == null) return;

        String targetWorld = sec.getString("auto-teleport-world");
        if (targetWorld != null && !targetWorld.isEmpty()
                && !p.getWorld().getName().equalsIgnoreCase(targetWorld)) {
            teleportToSpawn(p, targetWorld);
        }

        String gm = sec.getString("force-gamemode");
        if (gm != null && !gm.isEmpty()) {
            try {
                GameMode g = GameMode.valueOf(gm.toUpperCase(Locale.ROOT));
                if (p.getGameMode() != g) p.setGameMode(g);
            } catch (IllegalArgumentException ignored) {}
        }

        if (plugin.getConfig().getBoolean("server-mode.show-title-on-join", true)) {
            showModeTitle(p, currentMode);
        }
    }

    private void teleportToSpawn(Player p, String worldName) {
        World w = Bukkit.getWorld(worldName);
        if (w == null) {
            plugin.getLogger().warning("[ServerMode] Mondo '" + worldName + "' non caricato, teleport saltato.");
            return;
        }
        Location spawn = null;
        if (plugin.getDataManager() != null) {
            try {
                spawn = plugin.getDataManager().getSpawn(worldName);
            } catch (Throwable ignored) {}
        }
        if (spawn == null) spawn = w.getSpawnLocation();
        p.teleport(spawn);
    }

    private void showModeTitle(Player p, ServerMode mode) {
        String basePath = "servermode.title-" + mode.name().toLowerCase(Locale.ROOT);
        String title = MessageUtils.get(basePath + ".title");
        String subtitle = MessageUtils.get(basePath + ".subtitle");
        if (title == null || title.isEmpty()) return;
        p.sendTitle(MessageUtils.color(title), MessageUtils.color(subtitle), 10, 60, 20);
    }

    public boolean isCommandDisabled(String command) {
        if (command == null) return false;
        String base = command.toLowerCase(Locale.ROOT);
        if (base.startsWith("/")) base = base.substring(1);
        int spaceIdx = base.indexOf(' ');
        if (spaceIdx >= 0) base = base.substring(0, spaceIdx);

        int colonIdx = base.indexOf(':');
        if (colonIdx >= 0) base = base.substring(colonIdx + 1);

        List<String> disabled = plugin.getConfig().getStringList(
                "server-mode.modes." + currentMode.name() + ".disabled-commands");
        for (String d : disabled) {
            if (d.equalsIgnoreCase(base)) return true;
        }
        return false;
    }

    public String getChatFormat() {
        return plugin.getConfig().getString(
                "server-mode.modes." + currentMode.name() + ".chat-format");
    }

    public boolean canBypass(Player p) {
        String perm = plugin.getConfig().getString(
                "server-mode.bypass-permission", "DistrictRP.servermode.bypass");
        return p.hasPermission(perm);
    }

    public List<String> availableModes() {
        List<String> out = new ArrayList<>();
        for (ServerMode m : ServerMode.values()) out.add(m.name().toLowerCase(Locale.ROOT));
        return out;
    }

    public boolean isUsingDatabase() { return useDb; }

    public CompletableFuture<Boolean> setMode(String serverId, String mode, String by) {
        if (table == null) return CompletableFuture.completedFuture(false);
        return table.set(serverId, mode, by);
    }

    public CompletableFuture<String> getMode(String serverId) {
        if (table == null) return CompletableFuture.completedFuture("OFF");
        return table.get(serverId);
    }

    // --- listener: applica il mode ai join e blocca i comandi non consentiti ---

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (event.getPlayer().isOnline()) handleJoin(event.getPlayer());
        }, 10L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCmd(PlayerCommandPreprocessEvent event) {
        if (canBypass(event.getPlayer())) return;
        if (isCommandDisabled(event.getMessage())) {
            event.setCancelled(true);
            MessageUtils.sendMsg(event.getPlayer(), "servermode.command-disabled",
                    "mode", getCurrentDisplay());
        }
    }
}