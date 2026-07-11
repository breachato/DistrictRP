package dev.breach.DistrictRP.velocity;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class VelocityChatCommands {

    private final DistrictRPVelocity plugin;
    private final LegacyComponentSerializer serializer =
            LegacyComponentSerializer.builder().hexColors().character('&').build();

    public VelocityChatCommands(DistrictRPVelocity plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unchecked")
    public void registerAll() {
        CommandManager cm = plugin.getProxy().getCommandManager();
        Map<String, Object> channels = plugin.getConfig().getChannels();

        for (Map.Entry<String, Object> e : channels.entrySet()) {
            String id = e.getKey();
            Object raw = e.getValue();
            if (!(raw instanceof Map)) continue;
            Map<String, Object> cfg = (Map<String, Object>) raw;

            String mainCmd = str(cfg.get("command"), id);
            List<String> aliases = plugin.getConfig().getStringList("channels." + id + ".aliases");

            String[] all = new String[aliases.size() + 1];
            all[0] = mainCmd;
            for (int i = 0; i < aliases.size(); i++) all[i + 1] = aliases.get(i);

            CommandMeta meta = cm.metaBuilder(mainCmd).aliases(aliases.toArray(new String[0])).build();
            cm.register(meta, new ChannelCommand(id, cfg));

            plugin.getLogger().info("[Chat] Registrato /" + mainCmd + " (canale " + id + ")");
        }
    }

    private String str(Object o, String def) { return o == null ? def : o.toString(); }

    private class ChannelCommand implements SimpleCommand {
        private final String channelId;
        private final Map<String, Object> cfg;

        ChannelCommand(String channelId, Map<String, Object> cfg) {
            this.channelId = channelId;
            this.cfg = cfg;
        }

        @Override
        public void execute(Invocation invocation) {
            CommandSource src = invocation.source();
            String[] args = invocation.arguments();

            String permission = String.valueOf(cfg.get("permission"));
            if (permission != null && !permission.isEmpty() && !src.hasPermission(permission)) {
                send(src, plugin.getConfig().getMessage("no-permission"));
                return;
            }
            if (!(src instanceof Player p)) {
                send(src, plugin.getConfig().getMessage("only-player"));
                return;
            }
            if (args.length == 0) {
                send(src, plugin.getConfig().getMessage("empty-message"));
                return;
            }
            String message = String.join(" ", args);

            ServerConnection conn = p.getCurrentServer().orElse(null);
            if (conn == null) return;

            plugin.getChatBridge().broadcast(
                    channelId,
                    conn.getServerInfo().getName(),
                    p.getUsername(),
                    "",
                    message
            );
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            String permission = String.valueOf(cfg.get("permission"));
            if (permission == null || permission.isEmpty()) return true;
            return invocation.source().hasPermission(permission);
        }
    }

    private void send(CommandSource src, String legacy) {
        if (legacy == null || legacy.isEmpty()) return;
        Component comp = serializer.deserialize(legacy);
        src.sendMessage(comp);
    }
}