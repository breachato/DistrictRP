package dev.breach.DistrictRP.commands.utils;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

public class ClearChatCommand implements CommandExecutor {

    private final DistrictRP plugin;

    public ClearChatCommand(DistrictRP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String perm = plugin.getConfig().getString("clearchat.permission", "DistrictRP.clearchat");
        String bypassPerm = plugin.getConfig().getString("clearchat.bypass-permission", "DistrictRP.clearchat.bypass");
        int lines = plugin.getConfig().getInt("clearchat.lines", 200);

        if (!sender.hasPermission(perm)) {
            MessageUtils.sendMsg(sender, "general.no-permission");
            return true;
        }

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(bypassPerm)) continue;
            for (int i = 0; i < lines; i++) {
                p.sendMessage(randomPadding(rnd));
            }
        }

        String staffName = sender instanceof Player ? sender.getName() : "Console";
        Bukkit.broadcastMessage(MessageUtils.get("clearchat.cleared", "staff", staffName));
        return true;
    }

    private String randomPadding(ThreadLocalRandom rnd) {
        int spaces = rnd.nextInt(1, 6);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < spaces; i++) sb.append(' ');
        int extra = rnd.nextInt(0, 4);
        for (int i = 0; i < extra; i++) {
            sb.append((char) rnd.nextInt(0x2000, 0x200B));
        }
        return sb.toString();
    }
}