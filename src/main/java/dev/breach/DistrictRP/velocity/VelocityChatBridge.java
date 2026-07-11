package dev.breach.DistrictRP.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;

public class VelocityChatBridge {

    private final DistrictRPVelocity plugin;
    private final LegacyComponentSerializer serializer =
            LegacyComponentSerializer.builder().hexColors().character('&').build();

    public VelocityChatBridge(DistrictRPVelocity plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(DistrictRPVelocity.CHANNEL_STAFFCHAT)) return;
        if (!(event.getSource() instanceof ServerConnection)) return;

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(event.getData()))) {
            String action = in.readUTF();
            if (!"CHAT".equals(action)) return;

            String channelId = in.readUTF();
            String serverName = in.readUTF();
            String playerName = in.readUTF();
            String rankSymbol = in.readUTF();
            String message = in.readUTF();

            broadcast(channelId, serverName, playerName, rankSymbol, message);
        } catch (IOException e) {
            plugin.getLogger().warn("[Bridge] Errore parse messaggio: " + e.getMessage());
        }
    }

    public void broadcast(String channelId, String serverName,
                          String playerName, String rankSymbol, String message) {
        Map<String, Object> channel = channelConfig(channelId);
        if (channel == null) return;

        String permission = str(channel.get("permission"));
        String format = str(channel.get("format"));
        String hover = str(channel.get("hover"));

        String displayServer = resolveServerDisplay(serverName);

        String legacy = format
                .replace("%server%", displayServer)
                .replace("%player%", playerName)
                .replace("%rank%", rankSymbol == null ? "" : rankSymbol)
                .replace("%message%", message);

        Component base = serializer.deserialize(legacy);

        if (hover != null && !hover.isEmpty()) {
            String hoverProcessed = hover
                    .replace("%server%", displayServer)
                    .replace("%player%", playerName)
                    .replace("%rank%", rankSymbol == null ? "" : rankSymbol);
            Component hoverComp = serializer.deserialize(hoverProcessed);
            base = base.hoverEvent(HoverEvent.showText(hoverComp));
            base = base.clickEvent(ClickEvent.suggestCommand("/" + resolveCommand(channelId) + " "));
        }

        for (Player p : plugin.getProxy().getAllPlayers()) {
            if (!p.hasPermission(permission)) continue;
            p.sendMessage(base);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> channelConfig(String channelId) {
        Object raw = plugin.getConfig().getChannels().get(channelId);
        if (raw instanceof Map) return (Map<String, Object>) raw;
        return null;
    }

    private String resolveCommand(String channelId) {
        Map<String, Object> ch = channelConfig(channelId);
        if (ch == null) return channelId;
        Object cmd = ch.get("command");
        return cmd == null ? channelId : cmd.toString();
    }

    private String resolveServerDisplay(String serverName) {
        Map<String, Object> map = plugin.getConfig().getSection("server-display-names");
        Object v = map.get(serverName);
        if (v == null) v = map.get(serverName.toLowerCase());
        if (v != null) return v.toString();
        return serverName;
    }

    private String str(Object o) { return o == null ? "" : o.toString(); }
}