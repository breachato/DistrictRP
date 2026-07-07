package dev.breach.DistrictRP.commands.roleplay;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.roleplay.appuntamenti.AppuntamentoCommand;
import dev.breach.DistrictRP.commands.roleplay.appuntamenti.AppuntamentoGUI;
import dev.breach.DistrictRP.commands.roleplay.appuntamenti.AppuntamentoManager;
import dev.breach.DistrictRP.commands.roleplay.bots.BotManager;
import dev.breach.DistrictRP.commands.roleplay.chat.*;
import dev.breach.DistrictRP.commands.roleplay.emoji.EmojiCommand;
import dev.breach.DistrictRP.commands.roleplay.emoji.EmojiGUI;
import dev.breach.DistrictRP.commands.roleplay.emoji.EmojiListener;
import dev.breach.DistrictRP.commands.roleplay.emoji.EmojiManager;
import dev.breach.DistrictRP.commands.roleplay.logs.LogsAPI;
import dev.breach.DistrictRP.commands.roleplay.logs.LogsCommand;
import dev.breach.DistrictRP.commands.roleplay.playtime.PlaytimeCommand;
import dev.breach.DistrictRP.commands.roleplay.playtime.PlaytimeTracker;
import dev.breach.DistrictRP.commands.roleplay.plot.PlotAddon;
import dev.breach.DistrictRP.commands.roleplay.profile.JobService;
import dev.breach.DistrictRP.commands.roleplay.profile.ProfileCommand;
import dev.breach.DistrictRP.commands.roleplay.profile.RPProfileManager;
import dev.breach.DistrictRP.commands.roleplay.protection.ProtectionCommand;
import dev.breach.DistrictRP.commands.roleplay.protection.ProtectionInteractionsGUI;
import dev.breach.DistrictRP.commands.roleplay.protection.ProtectionListener;
import dev.breach.DistrictRP.commands.roleplay.protection.ProtectionManager;
import dev.breach.DistrictRP.commands.roleplay.chat.StaffListCommand;
import dev.breach.DistrictRP.commands.roleplay.stuck.StuckCommand;
import dev.breach.DistrictRP.commands.roleplay.supporto.SupportoCommand;
import dev.breach.DistrictRP.commands.roleplay.supporto.SupportoListener;
import dev.breach.DistrictRP.commands.roleplay.ticket.TicketCategoryGUI;
import dev.breach.DistrictRP.commands.roleplay.ticket.TicketCommand;
import dev.breach.DistrictRP.commands.roleplay.ticket.TicketManager;
import dev.breach.DistrictRP.commands.roleplay.ticket.TicketQuickRepliesGUI;
import dev.breach.DistrictRP.commands.roleplay.vanish.VanishTabHandler;
import dev.breach.DistrictRP.commands.roleplay.warp.WarpCommand;
import dev.breach.DistrictRP.commands.roleplay.warp.WarpManager;
import org.bukkit.Bukkit;
import org.bukkit.command.TabCompleter;

public class RoleplayModule {

    private final DistrictRP plugin;

    private RPProfileManager profileManager;
    private JobService jobService;
    private PlaytimeTracker playtimeTracker;
    private TicketManager ticketManager;
    private TicketCategoryGUI ticketGui;
    private TicketQuickRepliesGUI ticketQuickRepliesGUI;
    private AppuntamentoManager appuntamentoManager;
    private ChatSymManager chatSymManager;
    private EmojiManager emojiManager;
    private LogsAPI logsAPI;
    private BotManager botManager;
    private ProtectionManager protectionManager;
    private VanishTabHandler vanishTabHandler;
    private WarpManager warpManager;
    private PlotAddon plotAddon;
    private dev.breach.DistrictRP.commands.ChatGate.ChatGate chatGate;

    public RoleplayModule(DistrictRP plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        profileManager = new RPProfileManager(plugin);
        jobService = new JobService(plugin, profileManager);
        playtimeTracker = new PlaytimeTracker(plugin);
        ticketManager = new TicketManager(plugin);
        appuntamentoManager = new AppuntamentoManager(plugin);
        chatSymManager = new ChatSymManager(plugin);
        emojiManager = new EmojiManager(plugin);
        logsAPI = new LogsAPI(plugin);
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
        plugin.getCommand("azione").setExecutor(new AzioneCommand(plugin));
        plugin.getCommand("bisbiglio").setExecutor(new BisbiglioCommand(plugin));
        plugin.getCommand("urlo").setExecutor(new UrloCommand(plugin));
        plugin.getCommand("chatsym").setExecutor(new ChatSymCommand(plugin, chatSymManager));
        Bukkit.getPluginManager().registerEvents(new ChatSymListener(plugin, chatSymManager), plugin);

        ProfileCommand profileCmd = new ProfileCommand(plugin, profileManager);
        if (plugin.getCommand("profilo") != null) {
            plugin.getCommand("profilo").setExecutor(profileCmd);
            plugin.getCommand("profilo").setTabCompleter(profileCmd);
        }

        EmojiGUI emojiGUI = new EmojiGUI(plugin, emojiManager);
        EmojiCommand emojiCmd = new EmojiCommand(emojiManager);
        emojiCmd.setGui(emojiGUI);
        plugin.getCommand("emoji").setExecutor(emojiCmd);
        Bukkit.getPluginManager().registerEvents(new EmojiListener(plugin, emojiManager), plugin);
        Bukkit.getPluginManager().registerEvents(emojiGUI, plugin);

        StaffListCommand slExecutor = new StaffListCommand(plugin);
        if (plugin.getCommand("stafflist") != null) {
            plugin.getCommand("stafflist").setExecutor(slExecutor);
            plugin.getCommand("stafflist").setTabCompleter((TabCompleter) slExecutor);
        }

        ticketGui = new TicketCategoryGUI(plugin, ticketManager);
        Bukkit.getPluginManager().registerEvents(ticketGui, plugin);
        TicketCommand tc = new TicketCommand(plugin, ticketManager);
        plugin.getCommand("ticket").setExecutor(tc);
        plugin.getCommand("ticket").setTabCompleter(tc);

        ticketQuickRepliesGUI = new TicketQuickRepliesGUI(plugin, ticketManager);
        Bukkit.getPluginManager().registerEvents(ticketQuickRepliesGUI, plugin);

        SupportoListener supportoListener = new SupportoListener(plugin, ticketGui);
        plugin.getCommand("supporto").setExecutor(new SupportoCommand(plugin, ticketGui));
        Bukkit.getPluginManager().registerEvents(supportoListener, plugin);

        plugin.getCommand("appuntamento").setExecutor(new AppuntamentoCommand(plugin, appuntamentoManager));
        Bukkit.getPluginManager().registerEvents(new AppuntamentoGUI(plugin, appuntamentoManager), plugin);

        StuckCommand stuckCmd = new StuckCommand(plugin);
        plugin.getCommand("stuck").setExecutor(stuckCmd);
        if (plugin.getCommand("stuck") != null) plugin.getCommand("stuck").setTabCompleter(stuckCmd);

        plugin.getCommand("playtime").setExecutor(new PlaytimeCommand(plugin, playtimeTracker, profileManager));
        Bukkit.getPluginManager().registerEvents(playtimeTracker, plugin);
        playtimeTracker.start();

        plugin.getCommand("logs").setExecutor(new LogsCommand(plugin, logsAPI));

        protectionManager = new ProtectionManager(plugin);
        ProtectionInteractionsGUI protGui = new ProtectionInteractionsGUI(plugin, protectionManager);
        ProtectionCommand protCmd = new ProtectionCommand(plugin, protectionManager, protGui);
        plugin.getCommand("protection").setExecutor(protCmd);
        plugin.getCommand("protection").setTabCompleter(protCmd);
        Bukkit.getPluginManager().registerEvents(new ProtectionListener(plugin, protectionManager), plugin);
        Bukkit.getPluginManager().registerEvents(protGui, plugin);

        WarpCommand warpExec = new WarpCommand(plugin, warpManager, WarpCommand.Mode.WARP);
        WarpCommand setwarpExec = new WarpCommand(plugin, warpManager, WarpCommand.Mode.SETWARP);
        WarpCommand delwarpExec = new WarpCommand(plugin, warpManager, WarpCommand.Mode.DELWARP);
        if (plugin.getCommand("warp") != null) {
            plugin.getCommand("warp").setExecutor(warpExec);
            plugin.getCommand("warp").setTabCompleter(warpExec);
        }
        if (plugin.getCommand("setwarp") != null) {
            plugin.getCommand("setwarp").setExecutor(setwarpExec);
            plugin.getCommand("setwarp").setTabCompleter(setwarpExec);
        }
        if (plugin.getCommand("delwarp") != null) {
            plugin.getCommand("delwarp").setExecutor(delwarpExec);
            plugin.getCommand("delwarp").setTabCompleter(delwarpExec);
        }

        plotAddon = new PlotAddon(plugin, plugin.getServerModeManager());
        plotAddon.enable();

        vanishTabHandler = new VanishTabHandler(plugin);
        Bukkit.getPluginManager().registerEvents(vanishTabHandler, plugin);
        vanishTabHandler.start();

        plugin.getLogger().info("RoleplayModule abilitato.");
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

    public ProtectionManager getProtectionManager() { return protectionManager; }
    public dev.breach.DistrictRP.commands.ChatGate.ChatGate getChatGate() { return chatGate; }
    public RPProfileManager getProfileManager() { return profileManager; }
    public JobService getJobService() { return jobService; }
    public PlaytimeTracker getPlaytimeTracker() { return playtimeTracker; }
    public TicketManager getTicketManager() { return ticketManager; }
    public TicketCategoryGUI getTicketGui() { return ticketGui; }
    public TicketQuickRepliesGUI getTicketQuickRepliesGUI() { return ticketQuickRepliesGUI; }
    public AppuntamentoManager getAppuntamentoManager() { return appuntamentoManager; }
    public ChatSymManager getChatSymManager() { return chatSymManager; }
    public EmojiManager getEmojiManager() { return emojiManager; }
    public LogsAPI getLogsAPI() { return logsAPI; }
    public BotManager getBotManager() { return botManager; }
    public VanishTabHandler getVanishTabHandler() { return vanishTabHandler; }
    public WarpManager getWarpManager() { return warpManager; }
    public PlotAddon getPlotAddon() { return plotAddon; }
}