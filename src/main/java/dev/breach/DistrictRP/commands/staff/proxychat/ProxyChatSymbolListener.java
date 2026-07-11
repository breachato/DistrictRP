package dev.breach.DistrictRP.commands.staff.proxychat;

import dev.breach.DistrictRP.DistrictRP;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ProxyChatSymbolListener implements Listener {

    private final DistrictRP plugin;

    public ProxyChatSymbolListener(DistrictRP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfig().getBoolean("proxy-chat.enabled", true)) return;

        String raw = event.getMessage();
        if (raw == null || raw.isEmpty()) return;

        ConfigurationSection channels = plugin.getConfig().getConfigurationSection("proxy-chat.channels");
        if (channels == null) return;

        Player p = event.getPlayer();

        for (String channelId : channels.getKeys(false)) {
            ConfigurationSection ch = channels.getConfigurationSection(channelId);
            if (ch == null) continue;

            String prefix = ch.getString("prefix-symbol", "");
            if (prefix.isEmpty()) continue;

            if (!raw.startsWith(prefix)) continue;

            String permission = ch.getString("permission", "");
            if (!permission.isEmpty() && !p.hasPermission(permission)) continue;

            String msg = raw.substring(prefix.length()).trim();
            if (msg.isEmpty()) return;

            event.setCancelled(true);
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> ProxyChatBridge.sendToProxy(plugin, p, channelId, msg));
            return;
        }
    }
}