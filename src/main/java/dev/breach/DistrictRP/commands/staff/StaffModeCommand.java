package dev.breach.DistrictRP.commands.staff;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StaffModeCommand implements CommandExecutor {

    private final DistrictRP plugin;

    public StaffModeCommand(DistrictRP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendMsg(sender, "general.only-player");
            return true;
        }
        String perm = plugin.getConfig().getString("staffmode.permission", "DistrictRP.staffmode");
        if (!player.hasPermission(perm)) {
            MessageUtils.sendMsg(sender, "general.no-permission");
            return true;
        }
        if (plugin.getStaffModeManager() == null) {
            MessageUtils.sendMsg(sender, "general.invalid-args");
            return true;
        }
        plugin.getStaffModeManager().toggle(player);
        return true;
    }
}