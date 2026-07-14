package dev.breach.DistrictRP.commands.roleplay;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.roleplay.appuntamenti.AppuntamentoGUI;
import dev.breach.DistrictRP.commands.roleplay.appuntamenti.AppuntamentoManager;
import dev.breach.DistrictRP.commands.roleplay.bots.BotManager;
import dev.breach.DistrictRP.commands.roleplay.chat.ChatModule;
import dev.breach.DistrictRP.commands.roleplay.chat.ChatSymManager;
import dev.breach.DistrictRP.commands.roleplay.chat.SpeechCommand;
import dev.breach.DistrictRP.commands.roleplay.chat.StaffListCommand;
import dev.breach.DistrictRP.commands.roleplay.emoji.EmojiGUI;
import dev.breach.DistrictRP.commands.roleplay.emoji.EmojiManager;
import dev.breach.DistrictRP.commands.roleplay.playtime.PlaytimeTracker;
import dev.breach.DistrictRP.commands.roleplay.plot.PlotAddon;
import dev.breach.DistrictRP.commands.roleplay.profile.ProfileCommand;
import dev.breach.DistrictRP.commands.roleplay.profile.RPProfileManager;
import dev.breach.DistrictRP.commands.roleplay.protection.ProtectionCommand;
import dev.breach.DistrictRP.commands.roleplay.stuck.StuckCommand;
import dev.breach.DistrictRP.commands.roleplay.supporto.SupportoCommand;
import dev.breach.DistrictRP.commands.roleplay.ticket.TicketCategoryGUI;
import dev.breach.DistrictRP.commands.roleplay.ticket.TicketCommand;
import dev.breach.DistrictRP.commands.roleplay.ticket.TicketManager;
import dev.breach.DistrictRP.commands.roleplay.ticket.TicketQuickRepliesGUI;
import dev.breach.DistrictRP.commands.roleplay.vanish.VanishTabHandler;
import dev.breach.DistrictRP.commands.roleplay.warp.WarpCommand;
import dev.breach.DistrictRP.commands.roleplay.warp.WarpManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;

public class RoleplayModule {

    private final DistrictRP plugin;

    private RPProfileManager profileManager;
    private PlaytimeTracker playtimeTracker;
    private TicketManager ticketManager;
    private TicketCategoryGUI ticketGui;
    private TicketQuickRepliesGUI ticketQuickRepliesGUI;
    private AppuntamentoManager appuntamentoManager;
    private ChatSymManager chatSymManager;
    private EmojiManager emojiManager;
    private BotManager botManager;
    private VanishTabHandler vanishTabHandler;
    private WarpManager warpManager;
    private PlotAddon plotAddon;
    private dev.breach.DistrictRP.commands.ChatGate.ChatGate chatGate;

    public RoleplayModule(DistrictRP plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        profileManager = new RPProfileManager(plugin);
        playtimeTracker = new PlaytimeTracker(plugin);
        ticketManager = new TicketManager(plugin);
        appuntamentoManager = new AppuntamentoManager(plugin);
        chatSymManager = new ChatSymManager(plugin);
        emojiManager = new EmojiManager(plugin);
        warpManager = new WarpManager(plugin);

        chatGate = new dev.breach.DistrictRP.commands.ChatGate.ChatGate(plugin);
        chatGate.enable();

        botManager = new BotManager(plugin);
        botManager.enable();

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new RoleplayPlaceholders(plugin, profileManager, playtimeTracker, ticketManager).register();
            plugin.getLogger().info("PlaceholderAPI hookato -> %districtrp_...%");
        }

        Bukkit.getPluginManager().registerEvents(new ChatModule(plugin, profileManager), plugin);
        bindExecutor("azione", new SpeechCommand(plugin, ChatModule.ChatType.AZIONE));
        bindExecutor("bisbiglio", new SpeechCommand(plugin, ChatModule.ChatType.BISBIGLIO));
        bindExecutor("urlo", new SpeechCommand(plugin, ChatModule.ChatType.URLO));
        bindExecutor("chatsym", chatSymManager);
        Bukkit.getPluginManager().registerEvents(chatSymManager, plugin);

        ProfileCommand profileCmd = new ProfileCommand(plugin, profileManager);
        bindFull("profilo", profileCmd, profileCmd);

        EmojiGUI emojiGUI = new EmojiGUI(plugin, emojiManager);
        bindExecutor("emoji", emojiGUI);
        Bukkit.getPluginManager().registerEvents(emojiGUI, plugin);

        StaffListCommand slExecutor = new StaffListCommand(plugin);
        bindFull("stafflist", slExecutor, slExecutor);
        bindFull("sl", slExecutor, slExecutor);

        ticketGui = new TicketCategoryGUI(plugin, ticketManager);
        Bukkit.getPluginManager().registerEvents(ticketGui, plugin);
        TicketCommand tc = new TicketCommand(plugin, ticketManager);
        bindFull("ticket", tc, tc);

        ticketQuickRepliesGUI = new TicketQuickRepliesGUI(plugin, ticketManager);
        Bukkit.getPluginManager().registerEvents(ticketQuickRepliesGUI, plugin);

        SupportoCommand supporto = new SupportoCommand(plugin, ticketGui);
        bindExecutor("supporto", supporto);
        Bukkit.getPluginManager().registerEvents(supporto, plugin);

        bindExecutor("appuntamento", appuntamentoManager);
        Bukkit.getPluginManager().registerEvents(new AppuntamentoGUI(plugin, appuntamentoManager), plugin);

        StuckCommand stuckCmd = new StuckCommand(plugin);
        bindFull("stuck", stuckCmd, stuckCmd);

        bindExecutor("playtime", playtimeTracker);
        Bukkit.getPluginManager().registerEvents(playtimeTracker, plugin);
        playtimeTracker.start();

        if (plugin.getWorldGuardHook() != null && plugin.getWorldGuardHook().isAvailable()) {
            ProtectionCommand protCmd = new ProtectionCommand(plugin, plugin.getWorldGuardHook());
            bindFull("protection", protCmd, protCmd);
            plugin.getLogger().info("[Protection] Comando /protection registrato (WorldGuard wrapper).");
        } else {
            plugin.getLogger().warning("[Protection] WorldGuard non disponibile, /protection disabilitato.");
        }

        WarpCommand warpExec = new WarpCommand(plugin, warpManager, WarpCommand.Mode.WARP);
        WarpCommand setwarpExec = new WarpCommand(plugin, warpManager, WarpCommand.Mode.SETWARP);
        WarpCommand delwarpExec = new WarpCommand(plugin, warpManager, WarpCommand.Mode.DELWARP);
        bindFull("warp", warpExec, warpExec);
        bindFull("setwarp", setwarpExec, setwarpExec);
        bindFull("delwarp", delwarpExec, delwarpExec);

        plotAddon = new PlotAddon(plugin, plugin.getServerModeManager());
        plotAddon.enable();

        vanishTabHandler = new VanishTabHandler(plugin);
        Bukkit.getPluginManager().registerEvents(vanishTabHandler, plugin);
        vanishTabHandler.start();

        plugin.getLogger().info("RoleplayModule abilitato.");
    }

    private void bindExecutor(String name, org.bukkit.command.CommandExecutor exec) {
        PluginCommand cmd = plugin.getCommand(name);
        if (cmd == null) {
            plugin.getLogger().warning("[Roleplay] Comando /" + name + " non trovato nel plugin.yml!");
            return;
        }
        cmd.setExecutor(exec);
    }

    private void bindFull(String name, org.bukkit.command.CommandExecutor exec, org.bukkit.command.TabCompleter tab) {
        PluginCommand cmd = plugin.getCommand(name);
        if (cmd == null) {
            plugin.getLogger().warning("[Roleplay] Comando /" + name + " non trovato nel plugin.yml!");
            return;
        }
        cmd.setExecutor(exec);
        cmd.setTabCompleter(tab);
    }

    public void disable() {
        if (vanishTabHandler != null) vanishTabHandler.stop();
        if (botManager != null) botManager.disable();
        if (playtimeTracker != null) playtimeTracker.stop();
        if (profileManager != null) profileManager.saveAll();
        if (ticketManager != null) ticketManager.saveAll();
        if (appuntamentoManager != null) appuntamentoManager.saveAll();
        if (chatSymManager != null) chatSymManager.save();
        if (warpManager != null) warpManager.saveAll();
        if (plotAddon != null) plotAddon.disable();
        if (chatGate != null) chatGate.disable();
    }

    public dev.breach.DistrictRP.commands.ChatGate.ChatGate getChatGate() { return chatGate; }
    public RPProfileManager getProfileManager() { return profileManager; }
    public PlaytimeTracker getPlaytimeTracker() { return playtimeTracker; }
    public TicketManager getTicketManager() { return ticketManager; }
    public TicketCategoryGUI getTicketGui() { return ticketGui; }
    public TicketQuickRepliesGUI getTicketQuickRepliesGUI() { return ticketQuickRepliesGUI; }
    public AppuntamentoManager getAppuntamentoManager() { return appuntamentoManager; }
    public ChatSymManager getChatSymManager() { return chatSymManager; }
    public EmojiManager getEmojiManager() { return emojiManager; }
    public BotManager getBotManager() { return botManager; }
    public VanishTabHandler getVanishTabHandler() { return vanishTabHandler; }
    public WarpManager getWarpManager() { return warpManager; }
    public PlotAddon getPlotAddon() { return plotAddon; }
}