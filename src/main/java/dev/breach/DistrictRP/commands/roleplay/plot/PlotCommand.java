package dev.breach.DistrictRP.commands.roleplay.plot;

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
import java.util.List;
import java.util.Locale;

public class PlotCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS_PLAYER = Arrays.asList(
            "rivendica", "casa", "info", "ospite", "rimuoviospite",
            "ban", "sban", "lista", "elimina", "visita", "auto",
            "flag", "fidati", "sfidati", "aiuto", "help"
    );

    private static final List<String> SUBS_ADMIN = Arrays.asList(
            "setup", "admin", "debug", "regen", "reload",
            "area", "cluster", "database", "purge", "backup",
            "condense", "clear"
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
        if (!hook.isAvailable()) {
            MessageUtils.sendMsg(sender, "plot.not-installed");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        String rest = args.length > 1
                ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                : "";

        if (sub.equals("aiuto") || sub.equals("help")) {
            sendHelp(sender);
            return true;
        }

        if (SUBS_ADMIN.contains(sub)) {
            String adminPerm = plugin.getConfig().getString("plot.permissions.admin", "DistrictRP.plot.admin");
            if (!sender.hasPermission(adminPerm)) {
                MessageUtils.sendMsg(sender, "general.no-permission");
                return true;
            }
            return dispatchConsole(sender, "plot " + sub + " " + rest);
        }

        if (!(sender instanceof Player p)) {
            MessageUtils.sendMsg(sender, "general.only-player");
            return true;
        }

        String usePerm = plugin.getConfig().getString("plot.permissions.use", "DistrictRP.plot.use");
        if (!p.hasPermission(usePerm)) {
            MessageUtils.sendMsg(p, "general.no-permission");
            return true;
        }

        return switch (sub) {
            case "rivendica" -> dispatch(p, "plot claim " + rest);
            case "casa" -> dispatch(p, "plot home " + rest);
            case "info" -> dispatch(p, "plot info " + rest);
            case "ospite" -> dispatch(p, "plot add " + rest);
            case "rimuoviospite" -> dispatch(p, "plot remove " + rest);
            case "fidati" -> dispatch(p, "plot trust " + rest);
            case "sfidati" -> dispatch(p, "plot untrust " + rest);
            case "ban" -> dispatch(p, "plot deny " + rest);
            case "sban" -> dispatch(p, "plot undeny " + rest);
            case "lista" -> dispatch(p, "plot list " + rest);
            case "elimina" -> dispatch(p, "plot delete " + rest);
            case "visita" -> dispatch(p, "plot visit " + rest);
            case "auto" -> dispatch(p, "plot auto " + rest);
            case "flag" -> dispatch(p, "plot flag " + rest);
            default -> {
                sendHelp(p);
                yield true;
            }
        };
    }

    private void sendHelp(CommandSender sender) {
        String adminPerm = plugin.getConfig().getString("plot.permissions.admin", "DistrictRP.plot.admin");
        boolean isAdmin = sender.hasPermission(adminPerm);
        String key = isAdmin ? "plot.help-admin" : "plot.help";
        MessageUtils.sendList(sender, key);
    }

    private boolean dispatch(Player p, String cmd) {
        boolean ok = hook.runPlotSquaredCommand(p, cmd.trim());
        if (!ok) MessageUtils.sendMsg(p, "plot.command-failed");
        return true;
    }

    private boolean dispatchConsole(CommandSender sender, String cmd) {
        if (sender instanceof Player p) {
            boolean ok = hook.runPlotSquaredCommand(p, cmd.trim());
            if (!ok) MessageUtils.sendMsg(sender, "plot.command-failed");
        } else {
            boolean ok = hook.runConsoleCommand(cmd.trim());
            if (!ok) MessageUtils.sendMsg(sender, "plot.command-failed");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        String adminPerm = plugin.getConfig().getString("plot.permissions.admin", "DistrictRP.plot.admin");
        boolean isAdmin = sender.hasPermission(adminPerm);

        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            for (String s : SUBS_PLAYER) if (s.startsWith(partial)) out.add(s);
            if (isAdmin) {
                for (String s : SUBS_ADMIN) if (s.startsWith(partial)) out.add(s);
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (Arrays.asList("ospite", "rimuoviospite", "fidati", "sfidati",
                    "ban", "sban", "visita").contains(sub)) {
                for (Player pl : Bukkit.getOnlinePlayers()) out.add(pl.getName());
            }
        }
        return out;
    }
}