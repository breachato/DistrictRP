package dev.breach.cherryCore;

import dev.breach.cherryCore.commands.staff.*;
import dev.breach.cherryCore.commands.utils.*;
import dev.breach.cherryCore.core.*;
import dev.breach.cherryCore.functions.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class CherryCore extends JavaPlugin {

    public static final String BUILD_TAG = "Private";

    private static CherryCore instance;

    public final Map<UUID, UUID> reply = new HashMap<>();
    public final Map<UUID, String> recSkin = new HashMap<>();
    public final Map<UUID, Boolean> recActive = new HashMap<>();
    public final Map<UUID, String> recOriginRank = new HashMap<>();
    public final Map<UUID, String> recWaiting = new HashMap<>();
    public final Map<UUID, Boolean> godMode = new HashMap<>();
    public final Map<UUID, Location> back = new HashMap<>();
    public final Map<UUID, Boolean> spawnInvis = new HashMap<>();

    private DataManager dataManager;
    private WorldManager worldManager;
    private VanishManager vanishManager;
    private CherryTabManager tabManager;
    private WorldDownloader worldDownloader;
    private CommandBlocker commandBlocker;

    @Override
    public void onEnable() {
        instance = this;

        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        getLogger().info(" CherryCore v1.0.0");
        getLogger().info("");
        getLogger().info(" Versione : " + BUILD_TAG);
        getLogger().info(" Ora : " + time);
        getLogger().info("");

        try {
            saveDefaultConfig();
        } catch (Throwable t) {
            getLogger().warning("Errore saveDefaultConfig: " + t.getMessage());
        }

        this.commandBlocker = safeInit("CommandBlocker", () -> new CommandBlocker(this));

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (commandBlocker != null) {
                commandBlocker.overrideAll(
                        "warp", "warps", "setwarp", "delwarp",
                        "home", "homes", "sethome", "delhome", "spawn",
                        "setspawn", "tphere", "tpall", "fly",
                        "god", "vanish", "msg", "reply", "r"
                );

                commandBlocker.unregisterAll(
                        "plugins", "pl",
                        "version", "ver", "about",
                        "help", "?"
                );

                getLogger().info("[CommandBlocker] Override completato!");
            }
        }, 20L);

        getLogger().info("[INIT] DataManager...");
        this.dataManager       = safeInit("DataManager",       () -> new DataManager(this));

        getLogger().info("[INIT] WorldManager...");
        this.worldManager      = safeInit("WorldManager",      () -> new WorldManager(this));

        getLogger().info("[INIT] VanishManager...");
        this.vanishManager     = safeInit("VanishManager",     () -> new VanishManager(this));

        getLogger().info("[INIT] CherryTabManager...");
        this.tabManager        = safeInit("CherryTabManager",  () -> new CherryTabManager(this));

        getLogger().info("[INIT] WorldDownloader...");
        this.worldDownloader   = safeInit("WorldDownloader",   () -> new WorldDownloader(this));

        getLogger().info("[LISTENER] Registrazione listener...");
        safeRegisterListener("GlobalListener",     () -> new GlobalListener(this));
        safeRegisterListener("StaffModeListener",  () -> new StaffModeListener(this));
        safeRegisterListener("RecChatListener",   () -> new RecCommand.ChatListener(this));

        getLogger().info("[COMMANDS] Registrazione comandi...");
        try {
            registerCommands();
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "[CherryCore] Errore registrazione comandi:", t);
        }

        getLogger().info("");
        getLogger().info("  CherryCore abilitato con successo!");
        getLogger().info("  Versione: " + BUILD_TAG);
        getLogger().info("");
    }

    @Override
    public void onDisable() {
        getLogger().info("[CherryCore] Plugin disabilitato. (Tag: " + BUILD_TAG + ")");
    }

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
            if (executor instanceof TabCompleter tc) {
                getCommand(name).setTabCompleter(tc);
            }
        } else {
            getLogger().warning("  [!!] Comando /" + name + " non trovato nel plugin.yml!");
        }
    }

    private void registerCommands() {
        safeRegister("cherrycore", new CherryCoreDispatcher(this));

        safeRegister("rec", new RecCommand(this));
        safeRegister("recskinset", new RecCommand.Set(this));
        safeRegister("recskindel", new RecCommand.Del(this));

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

        GamemodeCommands gmCmds = new GamemodeCommands(this);
        safeRegister("gms", gmCmds.gms());
        safeRegister("gmc", gmCmds.gmc());
        safeRegister("gma", gmCmds.gma());
        safeRegister("gmsp", gmCmds.gmsp());

        HomeCommands homeCmds = new HomeCommands(this);
        safeRegister("sethome", homeCmds.sethome());
        safeRegister("home", homeCmds.home());
        safeRegister("delhome", homeCmds.delhome());
        safeRegister("homes", homeCmds.homes());

        WarpCommands warpCmds = new WarpCommands(this);
        safeRegister("setwarp", warpCmds.setwarp());
        safeRegister("warp", warpCmds.warp());
        safeRegister("delwarp", warpCmds.delwarp());
        safeRegister("warps", warpCmds.warps());

        MsgCommand msgCmd = new MsgCommand(this);
        safeRegister("msg", msgCmd);
        safeRegister("reply", msgCmd.reply());

        safeRegister("back", new BackCommand(this));
        safeRegister("clearchat", new ClearChatCommand(this));
        safeRegister("build", new BuildCommand(this));
        safeRegister("inizia", new IniziaCommand(this));
        safeRegister("mondo", new MondoCommand(this));

        AnnunciCommands annCmds = new AnnunciCommands(this);
        safeRegister("avantiilprimo", annCmds.primo());
        safeRegister("avantiunaltro", annCmds.prossimo());
        safeRegister("annuncio", annCmds.annuncio());
        safeRegister("annunciofull", annCmds.annfull());

    }

    public static CherryCore getInstance() { return instance; }
    public static CherryCore get() { return instance; }

    public DataManager getDataManager() { return dataManager; }

    public WorldManager getWorldManager() { return worldManager; }
    public WorldManager getMultiverseManager() { return worldManager; }
    public WorldManager getMultiverse() { return worldManager; }

    public VanishManager getVanishManager() { return vanishManager; }

    public CherryTabManager getTabManager() { return tabManager; }

    public WorldDownloader getWorldDownloader() { return worldDownloader; }
}
