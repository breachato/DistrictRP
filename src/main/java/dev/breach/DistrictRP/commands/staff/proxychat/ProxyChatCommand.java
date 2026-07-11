package dev.breach.DistrictRP.commands.staff.proxychat;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ProxyChatCommand implements CommandExecutor {

    private final DistrictRP plugin;
    private final String channelId;

    public ProxyChatCommand(DistrictRP plugin, String channelId) {
        this.plugin = plugin;
        this.channelId = channelId;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            MessageUtils.sendMsg(sender, "general.only-player");
            return true;
        }

        ConfigurationSection ch = plugin.getConfig().getConfigurationSection(
                "proxy-chat.channels." + channelId);
        if (ch == null) {
            MessageUtils.sendMsg(p, "proxychat.channel-unknown");
            return true;
        }

        String permission = ch.getString("permission", "");
        if (!permission.isEmpty() && !p.hasPermission(permission)) {
            MessageUtils.sendMsg(p, "proxychat.no-permission");
            return true;
        }

        if (args.length == 0) {
            MessageUtils.sendMsg(p, "proxychat.empty-message");
            return true;
        }

        String message = String.join(" ", args);
        ProxyChatBridge.sendToProxy(plugin, p, channelId, message);
        return true;
    }
}