package dev.breach.DistrictRP.commands.roleplay.appuntamenti;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AppuntamentoCommand implements CommandExecutor {

    private final DistrictRP plugin;
    private final AppuntamentoManager manager;

    public AppuntamentoCommand(DistrictRP plugin, AppuntamentoManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendMsg(sender, "general.only-player");
            return true;
        }
        AppuntamentoGUI.startFlow(plugin, manager, player);
        return true;
    }
}