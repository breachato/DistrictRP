package dev.breach.DistrictRP.commands.roleplay.emoji;

import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EmojiCommand implements CommandExecutor {

    private final EmojiManager manager;
    private EmojiGUI gui;

    public EmojiCommand(EmojiManager manager) {
        this.manager = manager;
    }

    public void setGui(EmojiGUI gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendMsg(sender, "general.only-player");
            return true;
        }
        if (gui != null) {
            gui.open(player, 0);
        }
        return true;
    }
}