package dev.breach.DistrictRP;

import dev.breach.DistrictRP.commands.roleplay.RoleplayModule;
import dev.breach.DistrictRP.commands.staff.*;
import dev.breach.DistrictRP.commands.staff.proxychat.ProxyChatBridge;
import dev.breach.DistrictRP.commands.staff.proxychat.ProxyChatCommand;
import dev.breach.DistrictRP.commands.staff.proxychat.ProxyChatSymbolListener;
import dev.breach.DistrictRP.commands.utils.*;
import dev.breach.DistrictRP.core.*;
import dev.breach.DistrictRP.framework.ModuleManager;
import dev.breach.DistrictRP.functions.*;
import dev.breach.DistrictRP.functions.servermode.ServerModeListener;
import dev.breach.DistrictRP.functions.servermode.ServerModeManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class DistrictRP extends JavaPlugin {

    public static final String BUILD_TAG = "Private";

    private static DistrictRP instance;

    public final Map<UUID, UUID> reply = new HashMap<>();
    public final Map<UUID, Boolean> godMode = new HashMap<>();
    public final Map<UUID, Location> back = new HashMap<>();
    public final Map<UUID, Integer> stuckActive = new HashMap<>();

    private DataManager dataManager;
    private WorldManager worldManager;
    private VanishManager vanishManager;
    private DistrictTabManager tabManager;
    private WorldDownloader worldDownloader;
    private CommandBlocker commandBlocker;
    private StaffModeManager staffModeManager;
    private StaffModeGUI staffModeGUI;
    private ServerModeManager serverModeManager;
    private CoreProtectHook coreProtectHook;
    private WorldGuardHook worldGuardHook;
    private ModuleManager moduleManager;
    private dev.breach.DistrictRP.database.DatabaseManager databaseManager;

    private RoleplayModule roleplayModule;

    @Override
    public void onEnable() {
        instance = this;

        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        getLogger().info(" DistrictRP");
        getLogger().info("");
        getLogger().info(" Versione : " + BUILD_TAG);
        getLogger().info(" Ora : " + time);
        getLogger().info("");

        try {
            saveDefaultConfig();
            reloadConfig();
        } catch (Throwable t) {
            getLogger().warning("Errore saveDefaultConfig: " + t.getMessage());
            try {
                File cfgFile = new File(getDataFolder(), "config.yml");
                if (cfgFile.exists()) {
                    File backup = new File(getDataFolder(), "config.yml.broken." + System.currentTimeMillis());
                    if (cfgFile.renameTo(backup)) {
                        getLogger().warning("Config corrotto rinominato in: " + backup.getName());
                    }
                }
                saveDefaultConfig();
                reloadConfig();
                getLogger().info("Config rigenerato correttamente.");
            } catch (Throwable t2) {
                getLogger().severe("Impossibile rigenerare il config: " + t2.getMessage());
            }
        }

        try {
            MessageUtils.load(this);
        } catch (Throwable t) {
            getLogger().warning("Errore load messages.yml: " + t.getMessage());
        }

        registerRankPermissions();

        this.commandBlocker = safeInit("CommandBlocker", () -> new CommandBlocker(this));

        boolean blockerEnabled = getConfig().getBoolean("command-blocker.enabled", false);
        if (blockerEnabled) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (commandBlocker != null) {
                    List<String> override = getConfig().getStringList("command-blocker.override");
                    List<String> unregister = getConfig().getStringList("command-blocker.unregister");
                    if (!override.isEmpty()) commandBlocker.overrideAll(override.toArray(new String[0]));
                    if (!unregister.isEmpty()) commandBlocker.unregisterAll(unregister.toArray(new String[0]));
                    getLogger().info("[CommandBlocker] Override completato!");
                }
            }, 20L);
        } else {
            getLogger().info("[CommandBlocker] Disabilitato in config.");
        }

        getLogger().info("[INIT] DataManager...");
        this.dataManager = safeInit("DataManager", () -> new DataManager(this));

        getLogger().info("[INIT] DatabaseManager...");
        this.databaseManager = safeInit("DatabaseManager", () -> {
            dev.breach.DistrictRP.database.DatabaseManager dm =
                    new dev.breach.DistrictRP.database.DatabaseManager(this);
            dm.initialize();
            return dm;
        });

        if (databaseManager != null && databaseManager.isMariaDb()) {
            getLogger().info("[Database] Registrazione tabelle...");
            var ds = databaseManager.getDataStore();
            databaseManager.registerTable("tickets",
                    new dev.breach.DistrictRP.database.tables.TicketsTable(this, ds));
            databaseManager.registerTable("ticket_comments",
                    new dev.breach.DistrictRP.database.tables.TicketCommentsTable(this, ds));
            databaseManager.registerTable("profiles",
                    new dev.breach.DistrictRP.database.tables.ProfilesTable(this, ds));
            databaseManager.registerTable("warps",
                    new dev.breach.DistrictRP.database.tables.WarpsTable(this, ds));
            databaseManager.registerTable("playtime",
                    new dev.breach.DistrictRP.database.tables.PlaytimeTable(this, ds));
            databaseManager.registerTable("appuntamenti",
                    new dev.breach.DistrictRP.database.tables.AppuntamentiTable(this, ds));
            databaseManager.registerTable("vanish",
                    new dev.breach.DistrictRP.database.tables.VanishTable(this, ds));
            databaseManager.registerTable("staffmode",
                    new dev.breach.DistrictRP.database.tables.StaffModeTable(this, ds));
            databaseManager.registerTable("server_mode",
                    new dev.breach.DistrictRP.database.tables.ServerModeTable(this, ds));
            databaseManager.registerTable("chatsym",
                    new dev.breach.DistrictRP.database.tables.ChatSymTable(this, ds));
            databaseManager.registerTable("logs",
                    new dev.breach.DistrictRP.database.tables.LogsTable(this, ds));
            databaseManager.registerTable("homes",
                    new dev.breach.DistrictRP.database.tables.HomesTable(this, ds));
            getLogger().info("[Database] Registrate " + databaseManager.getAllTables().size() + " tabelle.");
        }

        getLogger().info("[INIT] ModuleManager...");
        this.moduleManager = safeInit("ModuleManager", () -> new ModuleManager(this));

        getLogger().info("[INIT] WorldManager...");
        this.worldManager = safeInit("WorldManager", () -> new WorldManager(this));

        getLogger().info("[INIT] VanishManager...");
        this.vanishManager = safeInit("VanishManager", () -> new VanishManager(this));

        getLogger().info("[INIT] StaffModeManager...");
        this.staffModeManager = safeInit("StaffModeManager", () -> new StaffModeManager(this));

        getLogger().info("[INIT] StaffModeGUI...");
        this.staffModeGUI = safeInit("StaffModeGUI", () -> new StaffModeGUI(this));

        getLogger().info("[INIT] ServerModeManager...");
        this.serverModeManager = safeInit("ServerModeManager", () -> new ServerModeManager(this));

        getLogger().info("[INIT] DistrictTabManager...");
        this.tabManager = safeInit("DistrictTabManager", () -> new DistrictTabManager(this));

        getLogger().info("[INIT] WorldDownloader...");
        this.worldDownloader = safeInit("WorldDownloader", () -> new WorldDownloader(this));

        getLogger().info("[INIT] CoreProtectHook...");
        this.coreProtectHook = safeInit("CoreProtectHook", () -> new CoreProtectHook(this));

        getLogger().info("[INIT] WorldGuardHook...");
        this.worldGuardHook = safeInit("WorldGuardHook", () -> new WorldGuardHook(this));

        if (coreProtectHook != null && coreProtectHook.isAvailable()) {
            safeRegisterListener("CoreProtectAutoLogger",
                    () -> new CoreProtectAutoLogger(this, coreProtectHook));
        }

        getLogger().info("[LISTENER] Registrazione listener...");
        safeRegisterListener("GlobalListener", () -> new GlobalListener(this));
        safeRegisterListener("StaffModeListener", () -> new StaffModeListener(this, staffModeManager, staffModeGUI));
        safeRegisterListener("ServerModeListener", () -> new ServerModeListener(this, serverModeManager));
        safeRegisterListener("AutoStaffModeListener", () -> new AutoStaffModeListener(this));
        safeRegisterListener("ProxyChatSymbolListener", () -> new ProxyChatSymbolListener(this));

        if (staffModeManager != null) {
            getServer().getPluginManager().registerEvents(staffModeManager, this);
            getLogger().info("  [OK] StaffModeManager auto-registered as Listener (persist)");
        }

        try {
            getServer().getMessenger().registerOutgoingPluginChannel(this, ProxyChatBridge.CHANNEL);
            getLogger().info("[ProxyChat] Canale plugin-message registrato: " + ProxyChatBridge.CHANNEL);
        } catch (Throwable t) {
            getLogger().warning("[ProxyChat] Errore registrazione canale: " + t.getMessage());
        }

        getLogger().info("[COMMANDS] Registrazione comandi...");
        try {
            registerCommands();
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "[DistrictRP] Errore registrazione comandi:", t);
        }

        getLogger().info("[INIT] RoleplayModule...");
        try {
            this.roleplayModule = new RoleplayModule(this);
            this.roleplayModule.enable();
            getLogger().info("  [OK] RoleplayModule abilitato");
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "  [ERR] Errore RoleplayModule: " + t.getMessage(), t);
        }

        getLogger().info("");
        getLogger().info("  DistrictRP abilitato con successo!");
        getLogger().info("  Versione: " + BUILD_TAG);
        getLogger().info("");
    }

    private void registerRankPermissions() {
        try {
            int count = 0;
            ConfigurationSection ranks = getConfig().getConfigurationSection("stafflist.ranks");
            if (ranks != null) {
                for (String key : ranks.getKeys(false)) {
                    String perm = ranks.getString(key + ".permission");
                    if (perm != null && !perm.isEmpty()
                            && Bukkit.getPluginManager().getPermission(perm) == null) {
                        Bukkit.getPluginManager().addPermission(
                                new Permission(perm, "DistrictRP rank " + key, PermissionDefault.FALSE));
                        count++;
                    }
                }
            }
            ConfigurationSection vip = getConfig().getConfigurationSection("vip-symbols");
            if (vip != null) {
                for (String key : vip.getKeys(false)) {
                    if (key.equalsIgnoreCase("order")) continue;
                    String perm = vip.getString(key + ".permission");
                    if (perm != null && !perm.isEmpty()
                            && Bukkit.getPluginManager().getPermission(perm) == null) {
                        Bukkit.getPluginManager().addPermission(
                                new Permission(perm, "DistrictRP vip " + key, PermissionDefault.FALSE));
                        count++;
                    }
                }
            }
            String staffNotify = getConfig().getString("staff-notify.permission", "DistrictRP.staff.notify");
            if (staffNotify != null && Bukkit.getPluginManager().getPermission(staffNotify) == null) {
                Bukkit.getPluginManager().addPermission(
                        new Permission(staffNotify, "DistrictRP staff notify", PermissionDefault.OP));
                count++;
            }
            getLogger().info("[Perms] Registrati " + count + " permessi rank/vip in Bukkit PermissionManager.");
        } catch (Throwable t) {
            getLogger().warning("[Perms] Errore registrazione permessi: " + t.getMessage());
        }
    }

    @Override
    public void onDisable() {
        if (moduleManager != null) {
            try { moduleManager.disableAll(); }
            catch (Throwable t) { getLogger().warning("Errore disable ModuleManager: " + t.getMessage()); }
        }
        if (roleplayModule != null) {
            try { roleplayModule.disable(); }
            catch (Throwable t) { getLogger().warning("Errore disable RoleplayModule: " + t.getMessage()); }
        }
        if (vanishManager != null) vanishManager.shutdown();
        if (databaseManager != null) {
            try { databaseManager.shutdown(); }
            catch (Throwable t) { getLogger().warning("Errore shutdown DB: " + t.getMessage()); }
        }
        try {
            getServer().getMessenger().unregisterOutgoingPluginChannel(this, ProxyChatBridge.CHANNEL);
        } catch (Throwable ignored) {}
        getLogger().info("[DistrictRP] Plugin disabilitato. (Tag: " + BUILD_TAG + ")");
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> { T get() throws Throwable; }

    private <T> T safeInit(String name, ThrowingSupplier<T> s) {
        try {
            T result = s.get();
            if (result != null) getLogger().info("  [OK] " + name + " caricato");
            else getLogger().warning("  [!!] " + name + " restituito null");
            return result;
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "  [ERR] Errore init " + name + ": " + t.getMessage(), t);
            return null;
        }
    }

    private void safeRegisterListener(String name, ThrowingSupplier<Listener> s) {
        try {
            Listener l = s.get();
            if (l != null) {
                getServer().getPluginManager().registerEvents(l, this);
                getLogger().info("  [OK] Listener " + name + " registrato");
            }
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "  [ERR] Errore listener " + name + ": " + t.getMessage(), t);
        }
    }

    private void safeRegister(String name, CommandExecutor executor) {
        if (executor == null) {
            getLogger().warning("  [!!] Executor null per /" + name);
            return;
        }
        if (getCommand(name) != null) {
            getCommand(name).setExecutor(executor);
            if (executor instanceof TabCompleter tc) {
                getCommand(name).setTabCompleter(tc);
            } else if (tabManager != null) {
                getCommand(name).setTabCompleter(tabManager);
            }
        } else {
            getLogger().warning("  [!!] Comando /" + name + " non trovato nel plugin.yml!");
        }
    }

    private void registerCommands() {
        safeRegister("districtrp", new DistrictRPDispatcher(this));

        safeRegister("vanish", new VanishCommand(this));
        safeRegister("staffmode", new StaffModeCommand(this));
        safeRegister("fly", new FlyCommand(this));
        safeRegister("god", new GodCommand(this));
        safeRegister("speed", new SpeedCommand(this));
        safeRegister("invsee", new InvseeCommand(this));
        safeRegister("enderchest", new EnderchestCommand(this));
        safeRegister("clear", new ClearCommand(this));
        safeRegister("tpall", new TpAllCommand(this));
        safeRegister("tphere", new TpHereCommand(this));
        safeRegister("scale", new ScaleCommand(this));

        GamemodeCommands gmCmds = new GamemodeCommands(this);
        safeRegister("gms", gmCmds.gms());
        safeRegister("gmc", gmCmds.gmc());
        safeRegister("gma", gmCmds.gma());
        safeRegister("gmsp", gmCmds.gmsp());

        SpawnCommands spawnCmds = new SpawnCommands(this);
        safeRegister("spawn", spawnCmds.spawn());
        safeRegister("setspawn", spawnCmds.setSpawn());

        MsgCommand msgCmd = new MsgCommand(this);
        safeRegister("msg", msgCmd);
        safeRegister("reply", msgCmd.reply());

        safeRegister("back", new BackCommand(this));
        safeRegister("clearchat", new ClearChatCommand(this));
        safeRegister("build", new BuildCommand(this));
        safeRegister("mondo", new MondoCommand(this));
        safeRegister("wipe", new WipeCommand(this));

        registerProxyChatCommands();

        CommandExecutor noop = (s, c, l, a) -> true;
        safeRegister("appuntamento_select_reparto", noop);
        safeRegister("appuntamento_select_giorno", noop);
        safeRegister("appuntamento_fascia_page", noop);
        safeRegister("appuntamento_select_orario", noop);
        safeRegister("ticket_quickreplies", noop);
        safeRegister("ticket_cancel_comment", noop);
        safeRegister("supporto_continua", noop);
    }

    private void registerProxyChatCommands() {
        ConfigurationSection chSec = getConfig().getConfigurationSection("proxy-chat.channels");
        if (chSec == null) return;
        for (String channelId : chSec.getKeys(false)) {
            ConfigurationSection ch = chSec.getConfigurationSection(channelId);
            if (ch == null) continue;
            String cmdName = ch.getString("command", channelId);
            ProxyChatCommand exec = new ProxyChatCommand(this, channelId);
            safeRegister(cmdName, exec);
            List<String> aliases = ch.getStringList("aliases");
            for (String alias : aliases) {
                safeRegister(alias, exec);
            }
        }
    }

    public static DistrictRP getInstance() { return instance; }
    public static DistrictRP get() { return instance; }

    public DataManager getDataManager() { return dataManager; }
    public WorldManager getWorldManager() { return worldManager; }
    public WorldManager getMultiverseManager() { return worldManager; }
    public WorldManager getMultiverse() { return worldManager; }
    public VanishManager getVanishManager() { return vanishManager; }
    public StaffModeManager getStaffModeManager() { return staffModeManager; }
    public StaffModeGUI getStaffModeGUI() { return staffModeGUI; }
    public ServerModeManager getServerModeManager() { return serverModeManager; }
    public DistrictTabManager getTabManager() { return tabManager; }
    public WorldDownloader getWorldDownloader() { return worldDownloader; }
    public CoreProtectHook getCoreProtectHook() { return coreProtectHook; }
    public WorldGuardHook getWorldGuardHook() { return worldGuardHook; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public dev.breach.DistrictRP.database.DatabaseManager getDatabaseManager() { return databaseManager; }
    public RoleplayModule getRoleplay() { return roleplayModule; }
}