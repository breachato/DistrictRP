package dev.breach.DistrictRP.commands.roleplay.protection;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import dev.breach.DistrictRP.functions.WorldGuardHook;
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

public class ProtectionCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = Arrays.asList(
            "crea", "elimina", "flag", "info", "lista",
            "aggiungimembro", "rimuovimembro",
            "aggiungiowner", "rimuoviowner",
            "wand", "select", "expand",
            "reload", "load", "save"
    );

    private static final List<String> FLAGS = Arrays.asList(
            "build", "interact", "pvp", "chest-access", "damage-animals",
            "mob-spawning", "creeper-explosion", "tnt", "fire-spread",
            "greeting", "farewell", "entry", "exit", "use", "enderpearl"
    );

    private final DistrictRP plugin;
    private final WorldGuardHook wg;

    public ProtectionCommand(DistrictRP plugin, WorldGuardHook wg) {
        this.plugin = plugin;
        this.wg = wg;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String perm = plugin.getConfig().getString("protection.admin-permission", "DistrictRP.protection.admin");
        if (!sender.hasPermission(perm)) {
            MessageUtils.sendMsg(sender, "general.no-permission");
            return true;
        }
        if (!wg.isAvailable()) {
            MessageUtils.sendMsg(sender, "protection.wg-not-installed");
            return true;
        }
        if (args.length == 0) {
            MessageUtils.sendList(sender, "protection.help");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        String rest = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "";

        return switch (sub) {
            case "crea" -> dispatch(sender, "rg define " + rest);
            case "elimina" -> dispatch(sender, "rg remove " + rest);
            case "flag" -> dispatch(sender, "rg flag " + rest);
            case "info" -> dispatch(sender, "rg info " + rest);
            case "lista" -> dispatch(sender, "rg list " + rest);
            case "aggiungimembro" -> dispatch(sender, "rg addmember " + rest);
            case "rimuovimembro" -> dispatch(sender, "rg removemember " + rest);
            case "aggiungiowner" -> dispatch(sender, "rg addowner " + rest);
            case "rimuoviowner" -> dispatch(sender, "rg removeowner " + rest);
            case "wand" -> dispatch(sender, "wand");
            case "select" -> dispatch(sender, "sel " + rest);
            case "expand" -> dispatch(sender, "expand " + rest);
            case "reload" -> dispatch(sender, "wg reload");
            case "load" -> dispatch(sender, "rg load " + rest);
            case "save" -> dispatch(sender, "rg save " + rest);
            default -> {
                MessageUtils.sendList(sender, "protection.help");
                yield true;
            }
        };
    }

    private boolean dispatch(CommandSender sender, String cmd) {
        boolean ok;
        if (sender instanceof Player p) ok = wg.runCommand(p, cmd.trim());
        else ok = wg.runConsoleCommand(cmd.trim());
        if (!ok) MessageUtils.sendMsg(sender, "protection.command-failed");
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
            if (Arrays.asList("elimina", "info", "flag", "aggiungimembro",
                    "rimuovimembro", "aggiungiowner", "rimuoviowner",
                    "load", "save").contains(sub)) {
                if (sender instanceof Player p) {
                    out.addAll(wg.listRegions(p.getWorld()));
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("flag")) {
            String partial = args[2].toLowerCase(Locale.ROOT);
            for (String f : FLAGS) if (f.startsWith(partial)) out.add(f);
        } else if (args.length >= 3 && (
                args[0].equalsIgnoreCase("aggiungimembro") ||
                        args[0].equalsIgnoreCase("rimuovimembro") ||
                        args[0].equalsIgnoreCase("aggiungiowner") ||
                        args[0].equalsIgnoreCase("rimuoviowner"))) {
            for (Player pl : Bukkit.getOnlinePlayers()) out.add(pl.getName());
        }
        return out;
    }
}