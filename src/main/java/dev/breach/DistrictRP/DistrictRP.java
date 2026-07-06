package dev.breach.DistrictRP;

import dev.breach.DistrictRP.commands.roleplay.RoleplayModule;
import dev.breach.DistrictRP.commands.staff.*;
import dev.breach.DistrictRP.commands.utils.*;
import dev.breach.DistrictRP.core.*;
import dev.breach.DistrictRP.functions.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
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
    private LobbyModeManager lobbyModeManager;

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
            getLogger().warning("Provo a rigenerare il config corrotto...");
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

        this.commandBlocker = safeInit("CommandBlocker", () -> new CommandBlocker(this));

        boolean blockerEnabled = getConfig().getBoolean("command-blocker.enabled", false);
        if (blockerEnabled) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (commandBlocker != null) {
                    java.util.List<String> override = getConfig().getStringList("command-blocker.override");
                    java.util.List<String> unregister = getConfig().getStringList("command-blocker.unregister");
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

        getLogger().info("[INIT] WorldManager...");
        this.worldManager = safeInit("WorldManager", () -> new WorldManager(this));

        getLogger().info("[INIT] VanishManager...");
        this.vanishManager = safeInit("VanishManager", () -> new VanishManager(this));

        getLogger().info("[INIT] StaffModeManager...");
        this.staffModeManager = safeInit("StaffModeManager", () -> new StaffModeManager(this));

        getLogger().info("[INIT] StaffModeGUI...");
        this.staffModeGUI = safeInit("StaffModeGUI", () -> new StaffModeGUI(this));

        getLogger().info("[INIT] LobbyModeManager...");
        this.lobbyModeManager = safeInit("LobbyModeManager", () -> new LobbyModeManager(this));

        getLogger().info("[INIT] DistrictTabManager...");
        this.tabManager = safeInit("DistrictTabManager", () -> new DistrictTabManager(this));

        getLogger().info("[INIT] WorldDownloader...");
        this.worldDownloader = safeInit("WorldDownloader", () -> new WorldDownloader(this));

        getLogger().info("[LISTENER] Registrazione listener...");
        safeRegisterListener("GlobalListener", () -> new GlobalListener(this));
        safeRegisterListener("StaffModeListener", () -> new StaffModeListener(this, staffModeManager, staffModeGUI));

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

    @Override
    public void onDisable() {
        if (roleplayModule != null) {
            try {
                roleplayModule.disable();
            } catch (Throwable t) {
                getLogger().warning("Errore disable RoleplayModule: " + t.getMessage());
            }
        }
        getLogger().info("[DistrictRP] Plugin disabilitato. (Tag: " + BUILD_TAG + ")");
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Throwable;
    }

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

        CommandExecutor noop = (s, c, l, a) -> true;
        safeRegister("appuntamento_select_reparto", noop);
        safeRegister("appuntamento_select_giorno", noop);
        safeRegister("appuntamento_fascia_page", noop);
        safeRegister("appuntamento_select_orario", noop);
        safeRegister("ticket_quickreplies", noop);
        safeRegister("ticket_cancel_comment", noop);
        safeRegister("supporto_continua", noop);
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

    public LobbyModeManager getLobbyModeManager() { return lobbyModeManager; }

    public DistrictTabManager getTabManager() { return tabManager; }

    public WorldDownloader getWorldDownloader() { return worldDownloader; }

    public RoleplayModule getRoleplay() { return roleplayModule; }
}