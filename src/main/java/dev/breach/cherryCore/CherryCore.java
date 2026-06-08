package dev.breach.cherryCore;

import dev.breach.cherryCore.commands.elevators.*;
import dev.breach.cherryCore.commands.staff.*;
import dev.breach.cherryCore.commands.utils.*;
import dev.breach.cherryCore.core.*;
import dev.breach.cherryCore.functions.*;
import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class CherryCore extends JavaPlugin {

    // ============================================================
    // VERSION TAG
    // ============================================================
    public static final String BUILD_TAG = "BUILD-v6-NO-MULTIVERSE-MANAGER";
    // ============================================================

    private static CherryCore instance;

    // ===== CAMPI PUBBLICI =====
    public final Map<UUID, UUID> reply = new HashMap<>();
    public final Map<UUID, String> recSkin = new HashMap<>();
    public final Map<UUID, Boolean> recActive = new HashMap<>();
    public final Map<UUID, String> recOriginRank = new HashMap<>();
    public final Map<UUID, String> recWaiting = new HashMap<>();
    public final Map<UUID, Boolean> godMode = new HashMap<>();
    public final Map<UUID, Location> back = new HashMap<>();
    public final Map<UUID, Boolean> spawnInvis = new HashMap<>();

    // ===== MANAGER =====
    private DataManager dataManager;
    private WorldManager worldManager;
    private VanishManager vanishManager;
    private WhitelistManager whitelistManager;
    private CherryTabManager tabManager;
    private ElevatorManager elevatorManager;
    private WorldDownloader worldDownloader;

    @Override
    public void onEnable() {
        instance = this;

        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        getLogger().info("============================================");
        getLogger().info("       FluentCore v1.0.0");
        getLogger().info(" TAG : " + BUILD_TAG);
        getLogger().info(" ORA : " + time);
        getLogger().info("============================================");

        try {
            saveDefaultConfig();
        } catch (Throwable t) {
            getLogger().warning("Errore saveDefaultConfig: " + t.getMessage());
        }

        getLogger().info("[INIT] DataManager...");
        this.dataManager       = safeInit("DataManager",       () -> new DataManager(this));

        getLogger().info("[INIT] WorldManager...");
        this.worldManager      = safeInit("WorldManager",      () -> new WorldManager(this));

        getLogger().info("[INIT] VanishManager...");
        this.vanishManager     = safeInit("VanishManager",     () -> new VanishManager(this));

        getLogger().info("[INIT] WhitelistManager...");
        this.whitelistManager  = safeInit("WhitelistManager",  () -> new WhitelistManager(this));

        getLogger().info("[INIT] FluentTabManager...");
        this.tabManager        = safeInit("FluentTabManager",  () -> new CherryTabManager(this));

        getLogger().info("[INIT] ElevatorManager...");
        this.elevatorManager   = safeInit("ElevatorManager",   () -> new ElevatorManager(this));

        getLogger().info("[INIT] WorldDownloader...");
        this.worldDownloader   = safeInit("WorldDownloader",   () -> new WorldDownloader(this));

        getLogger().info("[LISTENER] Registrazione listener...");
        safeRegisterListener("GlobalListener",     () -> new GlobalListener(this));
        safeRegisterListener("ElevatorListener",   () -> new ElevatorListener(this));
        safeRegisterListener("StaffModeListener",  () -> new StaffModeListener(this));
        safeRegisterListener("SpawnWorldListener", () -> new SpawnWorldListener(this));
        safeRegisterListener("Rec ChatListener",   () -> new RecCommand.ChatListener(this));
        safeRegisterListener("ElevatorGUI",       () -> new ElevatorGUI(this));

        getLogger().info("[COMMANDS] Registrazione comandi...");
        try {
            registerCommands();
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "[FluentCore] Errore registrazione comandi:", t);
        }

        getLogger().info("============================================");
        getLogger().info("  FluentCore ABILITATO con successo!");
        getLogger().info("  TAG: " + BUILD_TAG);
        getLogger().info("============================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("[FluentCore] Plugin disabilitato. (Tag: " + BUILD_TAG + ")");
    }

    // ============================================================
    // Helper safe
    // ============================================================
    @FunctionalInterface
    private interface ThrowingSupplier<T> { T get() throws Throwable; }

    private <T> T safeInit(String name, ThrowingSupplier<T> s) {
        try {
            T result = s.get();
            if (result != null) {
                getLogger().info("  [OK] " + name + " caricato");
            } else {
                getLogger().warning("  [!!] " + name + " restituito null");
            }
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
        } else {
            getLogger().warning("  [!!] Comando /" + name + " non trovato nel plugin.yml!");
        }
    }

    private void registerCommands() {
        // CORE
        safeRegister("cherrycore", new CherryCoreDispatcher(this));

        // STAFF
        safeRegister("perms", new PermsCommand(this));

        // REC
        safeRegister("rec", new RecCommand(this));
        safeRegister("recskinset", new RecCommand.Set(this));
        safeRegister("recskindel", new RecCommand.Del(this));

        safeRegister("vanish", new VanishCommand(this));
        safeRegister("staffmode", new StaffModeCommand(this));
        safeRegister("disguise", new DisguiseCommand(this));
        safeRegister("fly", new FlyCommand(this));
        safeRegister("god", new GodCommand(this));
        safeRegister("speed", new SpeedCommand(this));
        safeRegister("invsee", new InvseeCommand(this));
        safeRegister("enderchest", new EnderchestCommand(this));
        safeRegister("clear", new ClearCommand(this));
        safeRegister("tpall", new TpAllCommand(this));
        safeRegister("tphere", new TpHereCommand(this));
        safeRegister("nick", new NickCommand(this));

        // GAMEMODES
        GamemodeCommands gmCmds = new GamemodeCommands(this);
        safeRegister("gms", gmCmds.gms());
        safeRegister("gmc", gmCmds.gmc());
        safeRegister("gma", gmCmds.gma());
        safeRegister("gmsp", gmCmds.gmsp());

        // HOMES
        HomeCommands homeCmds = new HomeCommands(this);
        safeRegister("sethome", homeCmds.sethome());
        safeRegister("home", homeCmds.home());
        safeRegister("delhome", homeCmds.delhome());
        safeRegister("homes", homeCmds.homes());

        // WARPS
        WarpCommands warpCmds = new WarpCommands(this);
        safeRegister("setwarp", warpCmds.setwarp());
        safeRegister("warp", warpCmds.warp());
        safeRegister("delwarp", warpCmds.delwarp());
        safeRegister("warps", warpCmds.warps());

        // SPAWN
        SpawnCommands spawnCmds = new SpawnCommands(this);
        safeRegister("spawn", spawnCmds.spawn());
        safeRegister("setspawn", spawnCmds.setspawn());

        // MSG
        MsgCommand msgCmd = new MsgCommand(this);
        safeRegister("msg", msgCmd);
        safeRegister("reply", msgCmd.reply());

        // ALTRI
        safeRegister("back", new BackCommand(this));
        safeRegister("clearchat", new ClearChatCommand(this));
        safeRegister("build", new BuildCommand(this));
        safeRegister("inizia", new IniziaCommand(this));
        safeRegister("mondo", new MondoCommand(this));

        // ANNUNCI
        AnnunciCommands annCmds = new AnnunciCommands(this);
        safeRegister("avantiilprimo", annCmds.primo());
        safeRegister("avantiilprossimo", annCmds.prossimo());
        safeRegister("annuncio", annCmds.annuncio());
        safeRegister("annunciofull", annCmds.annfull());

        // ELEVATORS
        safeRegister("elevator", new ElevatorCommand(this));
    }

    // ===== GETTERS =====
    public static CherryCore getInstance() { return instance; }
    public static CherryCore get() { return instance; }

    public DataManager getDataManager() { return dataManager; }

    public WorldManager getWorldManager() { return worldManager; }
    public WorldManager getMultiverseManager() { return worldManager; } // alias
    public WorldManager getMultiverse() { return worldManager; }        // alias

    public VanishManager getVanishManager() { return vanishManager; }

    public WhitelistManager getWhitelistManager() { return whitelistManager; }
    public WhitelistManager getWhitelist() { return whitelistManager; }

    public CherryTabManager getTabManager() { return tabManager; }

    public ElevatorManager getElevatorManager() { return elevatorManager; }
    public ElevatorManager getElevators() { return elevatorManager; }

    public WorldDownloader getWorldDownloader() { return worldDownloader; }
}