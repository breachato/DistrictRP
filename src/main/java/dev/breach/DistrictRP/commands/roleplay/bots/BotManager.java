package dev.breach.DistrictRP.commands.roleplay.bots;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.roleplay.appuntamenti.Appuntamento;
import dev.breach.DistrictRP.commands.roleplay.appuntamenti.AppuntamentoManager;
import dev.breach.DistrictRP.commands.roleplay.ticket.Ticket;
import dev.breach.DistrictRP.commands.roleplay.ticket.TicketCategory;
import dev.breach.DistrictRP.commands.roleplay.ticket.TicketManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class BotManager {

    private final DistrictRP plugin;
    private DiscordBot discordBot;
    private TelegramBot telegramBot;
    private BukkitTask discordRecapTask;
    private BukkitTask telegramRecapTask;

    public BotManager(DistrictRP plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        if (plugin.getConfig().getBoolean("bots.discord.enabled", false)) {
            try {
                discordBot = new DiscordBot(plugin);
                discordBot.start();
                plugin.getLogger().info("[Bots] DiscordBot avviato.");
            } catch (Throwable t) {
                plugin.getLogger().warning("[Bots] Errore avvio DiscordBot: " + t.getMessage());
            }
        }
        if (plugin.getConfig().getBoolean("bots.telegram.enabled", false)) {
            try {
                telegramBot = new TelegramBot(plugin);
                telegramBot.start();
                plugin.getLogger().info("[Bots] TelegramBot avviato.");
            } catch (Throwable t) {
                plugin.getLogger().warning("[Bots] Errore avvio TelegramBot: " + t.getMessage());
            }
        }
        startRecap();
    }

    public void disable() {
        if (discordRecapTask != null) discordRecapTask.cancel();
        if (telegramRecapTask != null) telegramRecapTask.cancel();
        if (discordBot != null) discordBot.shutdown();
        if (telegramBot != null) telegramBot.shutdown();
    }

    public DiscordBot getDiscordBot() { return discordBot; }
    public TelegramBot getTelegramBot() { return telegramBot; }

    public boolean isDiscordActive() { return discordBot != null && discordBot.isReady(); }
    public boolean isTelegramActive() { return telegramBot != null && telegramBot.isReady(); }

    // --- recap periodico ticket/appuntamenti verso i bot ---

    private void startRecap() {
        int discordMinutes = plugin.getConfig().getInt("bots.discord.tickets.recap-interval-minutes", 30);
        int telegramMinutes = plugin.getConfig().getInt("bots.telegram.tickets.recap-interval-minutes", 30);
        if (isDiscordActive() && discordMinutes > 0) {
            long ticks = 20L * 60L * discordMinutes;
            discordRecapTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                    this::runDiscordRecap, ticks, ticks);
        }
        if (isTelegramActive() && telegramMinutes > 0) {
            long ticks = 20L * 60L * telegramMinutes;
            telegramRecapTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                    this::runTelegramRecap, ticks, ticks);
        }
    }

    private void runDiscordRecap() {
        if (discordBot == null || !discordBot.isReady()) return;
        TicketManager tm = plugin.getRoleplay().getTicketManager();
        AppuntamentoManager am = plugin.getRoleplay().getAppuntamentoManager();

        String recapChannel = plugin.getConfig().getString("bots.discord.tickets.recap-channel", "");
        for (TicketCategory cat : tm.getCategories()) {
            List<Ticket> open = new ArrayList<>();
            for (Ticket t : tm.list(null, cat.getId())) if (t.isOpen()) open.add(t);
            if (!recapChannel.isEmpty()) discordBot.sendRecapTickets(cat.getName(), open, recapChannel);
        }

        String appRecap = plugin.getConfig().getString("bots.discord.appuntamenti.recap-channel", "");
        ConfigurationSection reparti = plugin.getConfig().getConfigurationSection("appuntamenti.reparti");
        if (reparti != null && !appRecap.isEmpty()) {
            for (String rep : reparti.getKeys(false)) {
                List<Appuntamento> list = am.getByReparto(rep);
                discordBot.sendRecapAppuntamenti(reparti.getString(rep + ".display", rep), list, appRecap);
            }
        }
    }

    private void runTelegramRecap() {
        if (telegramBot == null || !telegramBot.isReady()) return;
        TicketManager tm = plugin.getRoleplay().getTicketManager();
        AppuntamentoManager am = plugin.getRoleplay().getAppuntamentoManager();

        String recapChat = plugin.getConfig().getString("bots.telegram.tickets.recap-chat-id", "");
        for (TicketCategory cat : tm.getCategories()) {
            List<Ticket> open = new ArrayList<>();
            for (Ticket t : tm.list(null, cat.getId())) if (t.isOpen()) open.add(t);
            if (!recapChat.isEmpty()) telegramBot.sendRecapTickets(stripColor(cat.getName()), open, recapChat);
        }

        String appRecap = plugin.getConfig().getString("bots.telegram.appuntamenti.recap-chat-id", "");
        ConfigurationSection reparti = plugin.getConfig().getConfigurationSection("appuntamenti.reparti");
        if (reparti != null && !appRecap.isEmpty()) {
            for (String rep : reparti.getKeys(false)) {
                List<Appuntamento> list = am.getByReparto(rep);
                telegramBot.sendRecapAppuntamenti(reparti.getString(rep + ".display", rep), list, appRecap);
            }
        }
    }

    private String stripColor(String s) {
        if (s == null) return "";
        return s.replaceAll("§.", "").replaceAll("&#[A-Fa-f0-9]{6}", "").replaceAll("&.", "");
    }
}
