package dev.breach.DistrictRP.commands.roleplay.chat;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.roleplay.profile.RPProfile;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AzioneCommand implements CommandExecutor {

    private final DistrictRP plugin;

    public AzioneCommand(DistrictRP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendMsg(sender, "general.only-player");
            return true;
        }
        if (plugin.getLobbyModeManager() != null && plugin.getLobbyModeManager().isEnabled()) {
            MessageUtils.sendMsg(sender, "lobby-mode.rp-disabled");
            return true;
        }
        if (args.length == 0) {
            MessageUtils.sendMsg(sender, "chat.empty-message");
            return true;
        }

        String message = String.join(" ", args);
        double radius = plugin.getConfig().getDouble("chat.formats.azione.radius", 15);

        boolean anonymous = ChatFlags.isAnonymous(player);
        String formatKey = anonymous
                ? "chat.formats.azione.anonymous"
                : "chat.formats.azione.format";

        RPProfile profile = plugin.getRoleplay().getProfileManager().get(player.getUniqueId());
        String rpName = profile.hasRpName() ? profile.getRpName() : player.getName();

        String formatted = MessageUtils.color(
                plugin.getConfig().getString(formatKey, "")
                        .replace("%player%", rpName)
                        .replace("%rp_name%", rpName)
                        .replace("%message%", message)
        );

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().equals(player.getWorld())) continue;
            if (p.getLocation().distance(player.getLocation()) <= radius) {
                p.sendMessage(formatted);
            }
        }
        return true;
    }
}