package dev.breach.DistrictRP.commands.roleplay.chat;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import dev.breach.DistrictRP.functions.servermode.ServerMode;
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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            MessageUtils.sendMsg(sender, "general.only-player");
            return true;
        }
        if (isChatRestricted()) {
            MessageUtils.sendMsg(p, "servermode.command-disabled",
                    "mode", plugin.getServerModeManager().getCurrentDisplay());
            return true;
        }
        if (args.length == 0) {
            MessageUtils.sendMsg(p, "chat.empty-message");
            return true;
        }
        String msg = String.join(" ", args);
        ChatModule.broadcastRp(plugin, p, msg, ChatModule.ChatType.AZIONE);
        return true;
    }

    private boolean isChatRestricted() {
        if (plugin.getServerModeManager() == null) return false;
        ServerMode mode = plugin.getServerModeManager().getCurrent();
        return mode == ServerMode.LOBBY || mode == ServerMode.CREATIVE;
    }
}