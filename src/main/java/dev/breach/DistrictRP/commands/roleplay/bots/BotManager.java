package dev.breach.DistrictRP.commands.roleplay.bots;

import dev.breach.DistrictRP.DistrictRP;

public class BotManager {

    private final DistrictRP plugin;
    private DiscordBot discordBot;
    private TelegramBot telegramBot;
    private RecapScheduler recapScheduler;

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
        recapScheduler = new RecapScheduler(plugin, this);
        recapScheduler.start();
    }

    public void disable() {
        if (recapScheduler != null) recapScheduler.stop();
        if (discordBot != null) discordBot.shutdown();
        if (telegramBot != null) telegramBot.shutdown();
    }

    public DiscordBot getDiscordBot() { return discordBot; }
    public TelegramBot getTelegramBot() { return telegramBot; }

    public boolean isDiscordActive() { return discordBot != null && discordBot.isReady(); }
    public boolean isTelegramActive() { return telegramBot != null && telegramBot.isReady(); }
}