package dev.breach.DistrictRP.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "districtrp", name = "DistrictRP", version = "2.1.8",
        description = "DistrictRP Proxy", authors = {"breachato"})
public class DistrictRPVelocity {

    public static final MinecraftChannelIdentifier CHANNEL_STAFFCHAT =
            MinecraftChannelIdentifier.from("districtrp:staffchat");

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    private VelocityConfig config;
    private VelocityChatBridge chatBridge;
    private VelocityChatCommands chatCommands;

    @Inject
    public DistrictRPVelocity(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        logger.info(" ");
        logger.info("  DistrictRP Proxy avviato");
        logger.info(" ");

        this.config = new VelocityConfig(this);
        config.load();

        proxy.getChannelRegistrar().register(CHANNEL_STAFFCHAT);

        this.chatBridge = new VelocityChatBridge(this);
        proxy.getEventManager().register(this, chatBridge);

        this.chatCommands = new VelocityChatCommands(this);
        chatCommands.registerAll();

        logger.info("  DistrictRP Velocity pronto. Canali chat: "
                + config.getChannels().size());
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("[DistrictRP-Velocity] Shutdown.");
    }

    public ProxyServer getProxy() { return proxy; }
    public Logger getLogger() { return logger; }
    public Path getDataDirectory() { return dataDirectory; }
    public VelocityConfig getConfig() { return config; }
    public VelocityChatBridge getChatBridge() { return chatBridge; }
}