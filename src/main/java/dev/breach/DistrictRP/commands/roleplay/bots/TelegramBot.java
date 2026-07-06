package dev.breach.DistrictRP.commands.roleplay.bots;

import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.roleplay.appuntamenti.Appuntamento;
import dev.breach.DistrictRP.commands.roleplay.ticket.Ticket;
import dev.breach.DistrictRP.functions.MessageUtils;

import java.util.List;

public class TelegramBot {

    private final DistrictRP plugin;
    private com.pengrad.telegrambot.TelegramBot bot;
    private volatile boolean ready = false;

    public TelegramBot(DistrictRP plugin) {
        this.plugin = plugin;
    }

    public void start() {
        String token = plugin.getConfig().getString("bots.telegram.bot-token", "");
        if (token.isEmpty()) {
            plugin.getLogger().warning("[TelegramBot] Token vuoto, avvio saltato.");
            return;
        }
        bot = new com.pengrad.telegrambot.TelegramBot(token);
        ready = true;
    }

    public void shutdown() {
        if (bot != null) {
            try { bot.shutdown(); } catch (Throwable ignored) {}
        }
        ready = false;
    }

    public boolean isReady() { return ready; }

    private void send(String chatId, String html) {
        if (!ready || chatId == null || chatId.isEmpty()) return;
        try {
            bot.execute(new SendMessage(chatId, html).parseMode(ParseMode.HTML));
        } catch (Throwable t) {
            plugin.getLogger().warning("[TelegramBot] Errore invio: " + t.getMessage());
        }
    }

    public void sendTicketCreated(Ticket t) {
        String chatId = plugin.getConfig().getString(
                "bots.telegram.tickets.chat-id-by-category." + t.getCategory(), "");
        String msg = MessageUtils.get("bots.telegram.ticket-new",
                "id", String.valueOf(t.getId()),
                "player", t.getAuthorName(),
                "category", t.getCategory(),
                "reason", t.getReason());
        send(chatId, stripColor(msg));
    }

    public void sendTicketClosed(Ticket t, String staffName, String reason) {
        String chatId = plugin.getConfig().getString(
                "bots.telegram.tickets.chat-id-by-category." + t.getCategory(), "");
        String msg = MessageUtils.get("bots.telegram.ticket-closed",
                "id", String.valueOf(t.getId()),
                "staff", staffName == null ? "-" : staffName,
                "reason", reason == null ? "-" : reason);
        send(chatId, stripColor(msg));
    }

    public void sendAppuntamento(Appuntamento a) {
        String chatId = plugin.getConfig().getString(
                "bots.telegram.appuntamenti.chat-id-by-reparto." + a.getReparto(), "");
        String msg = MessageUtils.get("bots.telegram.appuntamento-new",
                "player", a.getPlayerName(),
                "reparto", a.getReparto(),
                "day", a.getGiorno(),
                "time", a.getOrario());
        send(chatId, stripColor(msg));
    }

    public void sendSupporto(String playerName, String category) {
        String chatId = plugin.getConfig().getString("bots.telegram.supporto.chat-id", "");
        String msg = MessageUtils.get("bots.telegram.supporto-new",
                "player", playerName, "category", category);
        send(chatId, stripColor(msg));
    }

    public void sendRecapTickets(String categoryName, List<Ticket> tickets, String chatId) {
        if (tickets.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        sb.append(MessageUtils.get("bots.telegram.recap-tickets-title", "category", categoryName));
        for (Ticket t : tickets) {
            sb.append(MessageUtils.get("bots.telegram.recap-tickets-entry",
                    "id", String.valueOf(t.getId()),
                    "player", t.getAuthorName()));
        }
        send(chatId, stripColor(sb.toString()));
    }

    public void sendRecapAppuntamenti(String repartoName, List<Appuntamento> list, String chatId) {
        if (list.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        sb.append(MessageUtils.get("bots.telegram.recap-appuntamenti-title", "reparto", repartoName));
        for (Appuntamento a : list) {
            sb.append(MessageUtils.get("bots.telegram.recap-appuntamenti-entry",
                    "id", String.valueOf(a.getId()),
                    "player", a.getPlayerName(),
                    "day", a.getGiorno(),
                    "time", a.getOrario()));
        }
        send(chatId, stripColor(sb.toString()));
    }

    private String stripColor(String s) {
        if (s == null) return "";
        return s.replaceAll("§.", "").replaceAll("&#[A-Fa-f0-9]{6}", "").replaceAll("&.", "");
    }
}