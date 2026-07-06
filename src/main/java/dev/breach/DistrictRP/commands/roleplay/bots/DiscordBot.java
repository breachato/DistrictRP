package dev.breach.DistrictRP.commands.roleplay.bots;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.roleplay.appuntamenti.Appuntamento;
import dev.breach.DistrictRP.commands.roleplay.ticket.Ticket;
import dev.breach.DistrictRP.functions.MessageUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DiscordBot extends ListenerAdapter {

    private final DistrictRP plugin;
    private JDA jda;
    private volatile boolean ready = false;

    private final Map<UUID, String> supportoChannels = new HashMap<>();

    public DiscordBot(DistrictRP plugin) {
        this.plugin = plugin;
    }

    public void start() throws Exception {
        String token = plugin.getConfig().getString("bots.discord.token", "");
        if (token.isEmpty()) {
            plugin.getLogger().warning("[DiscordBot] Token vuoto, avvio saltato.");
            return;
        }
        jda = JDABuilder.createDefault(token,
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.GUILD_VOICE_STATES,
                        GatewayIntent.GUILD_MESSAGES)
                .addEventListeners(this)
                .build()
                .awaitReady();
        ready = true;
    }

    public void shutdown() {
        if (jda != null) {
            try { jda.shutdownNow(); } catch (Throwable ignored) {}
        }
        ready = false;
    }

    public boolean isReady() { return ready; }

    private Guild getGuild() {
        if (jda == null) return null;
        String gid = plugin.getConfig().getString("bots.discord.guild-id", "");
        return gid.isEmpty() ? null : jda.getGuildById(gid);
    }

    public void sendTicketCreated(Ticket t) {
        if (!ready) return;
        Guild g = getGuild();
        if (g == null) return;
        String channelId = plugin.getConfig().getString(
                "bots.discord.tickets.channels-by-category." + t.getCategory(), "");
        TextChannel channel = channelId.isEmpty() ? null : g.getTextChannelById(channelId);
        if (channel == null) return;

        String title = MessageUtils.get("bots.discord.ticket-title", "id", String.valueOf(t.getId()));
        String desc = MessageUtils.get("bots.discord.ticket-desc",
                "player", t.getAuthorName(),
                "category", t.getCategory(),
                "reason", t.getReason());
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(stripColor(title))
                .setDescription(stripColor(desc))
                .setColor(new Color(0xFCD05C))
                .setTimestamp(java.time.Instant.now());
        channel.sendMessageEmbeds(eb.build()).queue();
    }

    public void sendTicketClosed(Ticket t, String staffName, String reason) {
        if (!ready) return;
        Guild g = getGuild();
        if (g == null) return;
        String channelId = plugin.getConfig().getString(
                "bots.discord.tickets.channels-by-category." + t.getCategory(), "");
        TextChannel channel = channelId.isEmpty() ? null : g.getTextChannelById(channelId);
        if (channel == null) return;

        String title = MessageUtils.get("bots.discord.ticket-closed-title", "id", String.valueOf(t.getId()));
        String desc = MessageUtils.get("bots.discord.ticket-closed-desc",
                "staff", staffName == null ? "-" : staffName,
                "reason", reason == null ? "-" : reason);
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(stripColor(title))
                .setDescription(stripColor(desc))
                .setColor(new Color(0xE74C3C))
                .setTimestamp(java.time.Instant.now());
        channel.sendMessageEmbeds(eb.build()).queue();
    }

    public void sendAppuntamento(Appuntamento a) {
        if (!ready) return;
        Guild g = getGuild();
        if (g == null) return;
        String channelId = plugin.getConfig().getString(
                "bots.discord.appuntamenti.channels-by-reparto." + a.getReparto(), "");
        TextChannel channel = channelId.isEmpty() ? null : g.getTextChannelById(channelId);
        if (channel == null) return;

        String title = MessageUtils.get("bots.discord.appuntamento-title");
        String desc = MessageUtils.get("bots.discord.appuntamento-desc",
                "player", a.getPlayerName(),
                "reparto", a.getReparto(),
                "day", a.getGiorno(),
                "time", a.getOrario());
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(stripColor(title))
                .setDescription(stripColor(desc))
                .setColor(new Color(0x3498DB))
                .setTimestamp(java.time.Instant.now());
        channel.sendMessageEmbeds(eb.build()).queue();
    }

    public void sendSupportoRequest(String playerName, String category) {
        if (!ready) return;
        Guild g = getGuild();
        if (g == null) return;
        String channelId = plugin.getConfig().getString(
                "bots.discord.tickets.channels-by-category." + category, "");
        TextChannel channel = channelId.isEmpty() ? null : g.getTextChannelById(channelId);
        if (channel == null) return;

        String title = MessageUtils.get("bots.discord.supporto-title");
        String desc = MessageUtils.get("bots.discord.supporto-desc",
                "player", playerName, "category", category);
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(stripColor(title))
                .setDescription(stripColor(desc))
                .setColor(new Color(0xE67E22))
                .setTimestamp(java.time.Instant.now());
        channel.sendMessageEmbeds(eb.build()).queue();
    }

    public void createSupportoVoice(UUID playerUuid, String playerName) {
        if (!ready) return;
        Guild g = getGuild();
        if (g == null) return;
        String catId = plugin.getConfig().getString("bots.discord.supporto.voice-category-id", "");
        Category category = catId.isEmpty() ? null : g.getCategoryById(catId);
        if (category == null) return;
        String nameTemplate = plugin.getConfig().getString(
                "bots.discord.supporto.voice-channel-name", "supporto-%player%");
        String name = nameTemplate.replace("%player%", playerName.toLowerCase());

        category.createVoiceChannel(name).queue(vc -> {
            supportoChannels.put(playerUuid, vc.getId());
            plugin.getLogger().info("[DiscordBot] Voice supporto creata: " + vc.getName());
        });
    }

    public void deleteSupportoVoice(UUID playerUuid) {
        if (!ready) return;
        Guild g = getGuild();
        if (g == null) return;
        String vcId = supportoChannels.remove(playerUuid);
        if (vcId == null) return;
        VoiceChannel vc = g.getVoiceChannelById(vcId);
        if (vc != null) vc.delete().queue();
    }

    public boolean isSupportoActive(UUID playerUuid) {
        return supportoChannels.containsKey(playerUuid);
    }

    public void sendRecapTickets(String categoryName, java.util.List<Ticket> tickets, String channelId) {
        if (!ready || tickets.isEmpty()) return;
        Guild g = getGuild();
        if (g == null) return;
        TextChannel channel = channelId == null || channelId.isEmpty() ? null : g.getTextChannelById(channelId);
        if (channel == null) return;

        StringBuilder sb = new StringBuilder();
        for (Ticket t : tickets) {
            sb.append(MessageUtils.get("bots.discord.recap-tickets-entry",
                    "id", String.valueOf(t.getId()),
                    "player", t.getAuthorName())).append("\n");
        }
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(stripColor(MessageUtils.get("bots.discord.recap-tickets-title",
                        "category", categoryName)))
                .setDescription(stripColor(sb.toString()))
                .setColor(new Color(0xFCD05C))
                .setTimestamp(java.time.Instant.now());
        channel.sendMessageEmbeds(eb.build()).queue();
    }

    public void sendRecapAppuntamenti(String repartoName, java.util.List<Appuntamento> list, String channelId) {
        if (!ready || list.isEmpty()) return;
        Guild g = getGuild();
        if (g == null) return;
        TextChannel channel = channelId == null || channelId.isEmpty() ? null : g.getTextChannelById(channelId);
        if (channel == null) return;

        StringBuilder sb = new StringBuilder();
        for (Appuntamento a : list) {
            sb.append(MessageUtils.get("bots.discord.recap-appuntamenti-entry",
                    "id", String.valueOf(a.getId()),
                    "player", a.getPlayerName(),
                    "day", a.getGiorno(),
                    "time", a.getOrario())).append("\n");
        }
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(stripColor(MessageUtils.get("bots.discord.recap-appuntamenti-title",
                        "reparto", repartoName)))
                .setDescription(stripColor(sb.toString()))
                .setColor(new Color(0x3498DB))
                .setTimestamp(java.time.Instant.now());
        channel.sendMessageEmbeds(eb.build()).queue();
    }

    @Override
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        if (!plugin.getConfig().getBoolean("bots.discord.supporto.auto-delete-on-leave", true)) return;
        if (event.getChannelLeft() == null) return;
        VoiceChannel vc = event.getChannelLeft().asVoiceChannel();
        String catId = plugin.getConfig().getString("bots.discord.supporto.voice-category-id", "");
        if (vc.getParentCategoryId() == null || !vc.getParentCategoryId().equals(catId)) return;
        if (!vc.getMembers().isEmpty()) return;

        UUID toRemove = null;
        for (Map.Entry<UUID, String> e : supportoChannels.entrySet()) {
            if (e.getValue().equals(vc.getId())) { toRemove = e.getKey(); break; }
        }
        if (toRemove != null) supportoChannels.remove(toRemove);
        vc.delete().queue();
    }

    private String stripColor(String s) {
        if (s == null) return "";
        return s.replaceAll("§.", "").replaceAll("&#[A-Fa-f0-9]{6}", "").replaceAll("&.", "");
    }
}