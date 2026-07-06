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

public class RecapScheduler {

    private final DistrictRP plugin;
    private final BotManager botManager;
    private BukkitTask discordTask;
    private BukkitTask telegramTask;

    public RecapScheduler(DistrictRP plugin, BotManager botManager) {
        this.plugin = plugin;
        this.botManager = botManager;
    }

    public void start() {
        int discordMinutes = plugin.getConfig().getInt("bots.discord.tickets.recap-interval-minutes", 30);
        int telegramMinutes = plugin.getConfig().getInt("bots.telegram.tickets.recap-interval-minutes", 30);

        if (botManager.isDiscordActive() && discordMinutes > 0) {
            long ticks = 20L * 60L * discordMinutes;
            discordTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                    this::runDiscordRecap, ticks, ticks);
        }
        if (botManager.isTelegramActive() && telegramMinutes > 0) {
            long ticks = 20L * 60L * telegramMinutes;
            telegramTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                    this::runTelegramRecap, ticks, ticks);
        }
    }

    public void stop() {
        if (discordTask != null) discordTask.cancel();
        if (telegramTask != null) telegramTask.cancel();
    }

    private void runDiscordRecap() {
        DiscordBot bot = botManager.getDiscordBot();
        if (bot == null || !bot.isReady()) return;
        TicketManager tm = plugin.getRoleplay().getTicketManager();
        AppuntamentoManager am = plugin.getRoleplay().getAppuntamentoManager();

        String recapChannel = plugin.getConfig().getString("bots.discord.tickets.recap-channel", "");
        for (TicketCategory cat : tm.getCategories()) {
            List<Ticket> open = new ArrayList<>();
            for (Ticket t : tm.list(null, cat.getId())) if (t.isOpen()) open.add(t);
            if (!recapChannel.isEmpty()) {
                bot.sendRecapTickets(cat.getName(), open, recapChannel);
            }
        }

        String appRecap = plugin.getConfig().getString("bots.discord.appuntamenti.recap-channel", "");
        ConfigurationSection reparti = plugin.getConfig().getConfigurationSection("appuntamenti.reparti");
        if (reparti != null && !appRecap.isEmpty()) {
            for (String rep : reparti.getKeys(false)) {
                List<Appuntamento> list = am.getByReparto(rep);
                String display = reparti.getString(rep + ".display", rep);
                bot.sendRecapAppuntamenti(display, list, appRecap);
            }
        }
    }

    private void runTelegramRecap() {
        TelegramBot bot = botManager.getTelegramBot();
        if (bot == null || !bot.isReady()) return;
        TicketManager tm = plugin.getRoleplay().getTicketManager();
        AppuntamentoManager am = plugin.getRoleplay().getAppuntamentoManager();

        String recapChat = plugin.getConfig().getString("bots.telegram.tickets.recap-chat-id", "");
        for (TicketCategory cat : tm.getCategories()) {
            List<Ticket> open = new ArrayList<>();
            for (Ticket t : tm.list(null, cat.getId())) if (t.isOpen()) open.add(t);
            if (!recapChat.isEmpty()) {
                bot.sendRecapTickets(stripColor(cat.getName()), open, recapChat);
            }
        }

        String appRecap = plugin.getConfig().getString("bots.telegram.appuntamenti.recap-chat-id", "");
        ConfigurationSection reparti = plugin.getConfig().getConfigurationSection("appuntamenti.reparti");
        if (reparti != null && !appRecap.isEmpty()) {
            for (String rep : reparti.getKeys(false)) {
                List<Appuntamento> list = am.getByReparto(rep);
                String display = reparti.getString(rep + ".display", rep);
                bot.sendRecapAppuntamenti(display, list, appRecap);
            }
        }
    }

    private String stripColor(String s) {
        if (s == null) return "";
        return s.replaceAll("§.", "").replaceAll("&#[A-Fa-f0-9]{6}", "").replaceAll("&.", "");
    }
}