package dev.breach.DistrictRP.commands.utils;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ScaleCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SCALE_VALUES = Arrays.asList(
            "0.5", "0.75", "1", "1.25", "1.5", "2", "3", "5"
    );

    private final DistrictRP plugin;

    public ScaleCommand(DistrictRP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String perm = plugin.getConfig().getString("scale.permission", "DistrictRP.scale");
        if (!sender.hasPermission(perm)) {
            MessageUtils.sendMsg(sender, "general.no-permission");
            return true;
        }

        if (args.length == 0) {
            MessageUtils.sendMsg(sender, "scale.usage");
            return true;
        }

        if (args.length == 1) {
            if (!(sender instanceof Player p)) {
                MessageUtils.sendMsg(sender, "general.only-player");
                return true;
            }
            double value;
            try {
                value = Double.parseDouble(args[0]);
            } catch (NumberFormatException e) {
                MessageUtils.sendMsg(sender, "scale.invalid-value");
                return true;
            }
            double min = plugin.getConfig().getDouble("scale.min", 0.1);
            double max = plugin.getConfig().getDouble("scale.max", 10.0);
            if (value < min || value > max) {
                MessageUtils.sendMsg(sender, "scale.out-of-range", "min", String.valueOf(min), "max", String.valueOf(max));
                return true;
            }
            applyScale(p, value);
            MessageUtils.sendMsg(p, "scale.set-self", "value", String.valueOf(value));
            return true;
        }

        String otherPerm = plugin.getConfig().getString("scale.permission-others", "DistrictRP.scale.others");
        if (!sender.hasPermission(otherPerm)) {
            MessageUtils.sendMsg(sender, "general.no-permission");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            MessageUtils.sendMsg(sender, "general.player-not-found");
            return true;
        }

        double value;
        try {
            value = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            MessageUtils.sendMsg(sender, "scale.invalid-value");
            return true;
        }
        double min = plugin.getConfig().getDouble("scale.min", 0.1);
        double max = plugin.getConfig().getDouble("scale.max", 10.0);
        if (value < min || value > max) {
            MessageUtils.sendMsg(sender, "scale.out-of-range", "min", String.valueOf(min), "max", String.valueOf(max));
            return true;
        }

        applyScale(target, value);
        MessageUtils.sendMsg(sender, "scale.set-other",
                "player", target.getName(), "value", String.valueOf(value));
        return true;
    }

    private void applyScale(Player p, double value) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "attribute " + p.getName() + " minecraft:scale base set " + value);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            String partial = args[0].toLowerCase(Locale.ROOT);
            for (String v : SCALE_VALUES) {
                if (v.startsWith(partial)) out.add(v);
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(partial)) out.add(p.getName());
            }
            return out;
        }
        if (args.length == 2) {
            String partial = args[1].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (String v : SCALE_VALUES) {
                if (v.startsWith(partial)) out.add(v);
            }
            return out;
        }
        return Collections.emptyList();
    }
}