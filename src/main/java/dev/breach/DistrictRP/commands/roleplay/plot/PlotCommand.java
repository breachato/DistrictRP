package dev.breach.DistrictRP.commands.roleplay.plot;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PlotCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = Arrays.asList(
            "rivendica", "casa", "info", "ospite", "rimuoviospite",
            "ban", "sban", "lista", "elimina", "visita", "auto",
            "flag", "fidati", "sfidati", "aiuto"
    );

    private final DistrictRP plugin;
    private final PlotSquaredHook hook;

    public PlotCommand(DistrictRP plugin, PlotSquaredHook hook) {
        this.plugin = plugin;
        this.hook = hook;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            MessageUtils.sendMsg(sender, "general.only-player");
            return true;
        }
        String perm = plugin.getConfig().getString("plot.permissions.use", "DistrictRP.plot.use");
        if (!p.hasPermission(perm)) {
            MessageUtils.sendMsg(p, "general.no-permission");
            return true;
        }
        if (!hook.isAvailable()) {
            MessageUtils.sendMsg(p, "plot.not-installed");
            return true;
        }

        if (args.length == 0) {
            MessageUtils.sendList(p, "plot.help");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        String rest = args.length > 1
                ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                : "";

        switch (sub) {
            case "rivendica": return dispatch(p, "plot claim " + rest);
            case "casa":      return dispatch(p, "plot home " + rest);
            case "info":      return dispatch(p, "plot info " + rest);
            case "ospite":    return dispatch(p, "plot add " + rest);
            case "rimuoviospite": return dispatch(p, "plot remove " + rest);
            case "fidati":    return dispatch(p, "plot trust " + rest);
            case "sfidati":   return dispatch(p, "plot untrust " + rest);
            case "ban":       return dispatch(p, "plot deny " + rest);
            case "sban":      return dispatch(p, "plot undeny " + rest);
            case "lista":     return dispatch(p, "plot list " + rest);
            case "elimina":   return dispatch(p, "plot delete " + rest);
            case "visita":    return dispatch(p, "plot visit " + rest);
            case "auto":      return dispatch(p, "plot auto " + rest);
            case "flag":      return dispatch(p, "plot flag " + rest);
            case "aiuto":
            default:
                MessageUtils.sendList(p, "plot.help");
                return true;
        }
    }

    private boolean dispatch(Player p, String cmd) {
        boolean ok = hook.runPlotSquaredCommand(p, cmd.trim());
        if (!ok) MessageUtils.sendMsg(p, "plot.command-failed");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            for (String s : SUBS) if (s.startsWith(partial)) out.add(s);
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("ospite") || sub.equals("rimuoviospite")
                    || sub.equals("fidati") || sub.equals("sfidati")
                    || sub.equals("ban") || sub.equals("sban")
                    || sub.equals("visita")) {
                for (Player pl : plugin.getServer().getOnlinePlayers()) out.add(pl.getName());
            }
        }
        return out;
    }
}