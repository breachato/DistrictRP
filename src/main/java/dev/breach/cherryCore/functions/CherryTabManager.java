package dev.breach.cherryCore.functions;

import dev.breach.cherryCore.CherryCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class CherryTabManager implements TabCompleter {

    private final CherryCore plugin;

    private static final List<String> CC_SUBS = Arrays.asList(
            "gmc", "gms", "gma", "gmsp",
            "fly", "god", "speed",
            "home", "sethome", "delhome", "homes",
            "warp", "setwarp", "delwarp", "warps",
            "spawn", "setspawn", "back",
            "tphere", "tpall",
            "msg", "reply", "nick",
            "invsee", "enderchest", "clear",
            "clearchat",
            "primo", "prossimo", "annuncio", "annunciofull",
            "build", "inizia", "rec", "mondo",
            "help", "vanish", "disguise", "staffmode"
    );

    private static final List<String> EL_SUBS = Arrays.asList(
            "gui", "get", "give", "setname", "resetname", "setgroup",
            "setcooldown", "link", "unlink", "info", "list", "reload", "deleteall"
    );

    private static final List<String> EL_TYPES = Arrays.asList(
            "classic", "express", "vip", "freight", "glass"
    );

    private static final List<String> PERMS_SUBS = Arrays.asList("list", "add", "remove");
    private static final List<String> ANN_SUBS = Arrays.asList("Benvenuti!", "Attenzione!", "Evento", "Test");
    private static final List<String> SPEED_NUMS = Arrays.asList("0","1","2","3","4","5","6","7","8","9","10");

    public CherryTabManager(CherryCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {

        String cmd = command.getName().toLowerCase();

        switch (cmd) {
            case "cherrycore":
                return handleCherryCore(sender, args);
            case "elevator":
                return handleElevator(sender, args);
            case "perms":
                return handlePerms(sender, args);
            case "home":
            case "delhome":
                return handleHomes(sender, args);
            case "warp":
            case "delwarp":
                return handleWarps(sender, args);
            case "speed":
                return handleSpeed(sender, args);
            case "mondo":
            case "mondi":
                return handleMondo(sender, args);
            case "fly":
            case "god":
            case "gms":
            case "gmc":
            case "gma":
            case "gmsp":
            case "tphere":
            case "invsee":
            case "clear":
            case "tpall":
            case "enderchest":
            case "msg":
                if (args.length == 1) return filterPlayers(args[0]);
                return empty();
            case "nick":
                if (args.length == 2) return filterPlayers(args[1]);
                return empty();
            case "recskinset":
            case "recskindel":
                if (args.length == 1) return filterPlayers(args[0]);
                return empty();
            case "annuncio":
            case "annunciofull":
                if (args.length == 1) return filter(ANN_SUBS, args[0]);
                return empty();
            case "vanish":
            case "v":
                if (args.length == 1 && sender.hasPermission("cherrycore.vanish.others"))
                    return filterPlayers(args[0]);
                return empty();

            case "disguise":
            case "d":
            case "dis":
                if (args.length == 1) {
                    List<String> opts = new ArrayList<>();
                    opts.add("off");
                    opts.addAll(filterPlayers(args[0]));
                    return filter(opts, args[0]);
                }
                return empty();

            case "staffmode":
            case "sm":
            case "staff":
                return empty();
            default:
                return empty();
        }
    }

    // ============================================================
    // HANDLERS
    // ============================================================

    private List<String> handleCherryCore(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filter(CC_SUBS, args[0]);
        }
        if (args.length >= 2) {
            String sub = args[0].toLowerCase();
            if (Arrays.asList("fly", "god", "tphere", "invsee", "enderchest", "clear",
                    "gms", "gmc", "gma", "gmsp", "msg", "nick").contains(sub)) {
                if (args.length == 2) return filterPlayers(args[1]);
            }
            if (sub.equals("speed")) {
                if (args.length == 2) return filter(SPEED_NUMS, args[1]);
                if (args.length == 3) return filterPlayers(args[2]);
            }
            if (sub.equals("home") || sub.equals("delhome")) {
                if (args.length == 2 && sender instanceof Player p) {
                    return filter(new ArrayList<>(plugin.getDataManager().listHomes(p.getUniqueId())), args[1]);
                }
            }
            if (sub.equals("warp") || sub.equals("delwarp")) {
                if (args.length == 2) {
                    return filter(new ArrayList<>(plugin.getDataManager().listWarps()), args[1]);
                }
            }
        }
        return empty();
    }

    private List<String> handleElevator(CommandSender sender, String[] args) {
        if (args.length == 1) return filter(EL_SUBS, args[0]);
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "get":         return filter(EL_TYPES, args[1]);
                case "give":        return filterPlayers(args[1]);
                case "setcooldown": return filter(SPEED_NUMS, args[1]);
                default:            return empty();
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return filter(EL_TYPES, args[2]);
        }
        return empty();
    }

    private List<String> handlePerms(CommandSender sender, String[] args) {
        if (args.length == 1) return filter(PERMS_SUBS, args[0]);
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("add") || sub.equals("remove")) {
                return filterPlayers(args[1]);
            }
        }
        return empty();
    }

    private List<String> handleHomes(CommandSender sender, String[] args) {
        if (args.length == 1 && sender instanceof Player p) {
            return filter(new ArrayList<>(plugin.getDataManager().listHomes(p.getUniqueId())), args[0]);
        }
        return empty();
    }

    private List<String> handleWarps(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return filter(new ArrayList<>(plugin.getDataManager().listWarps()), args[0]);
        }
        return empty();
    }

    private List<String> handleSpeed(CommandSender sender, String[] args) {
        if (args.length == 1) return filter(SPEED_NUMS, args[0]);
        if (args.length == 2) return filterPlayers(args[1]);
        return empty();
    }

    private List<String> handleMondo(CommandSender sender, String[] args) {
        List<String> SUBS = Arrays.asList(
                "aggiungi", "rimuovi", "visita", "lista", "whitelist", "blacklist");
        List<String> WL_ACTIONS = Arrays.asList("aggiungi", "rimuovi", "lista");

        if (args.length == 1) return filter(SUBS, args[0]);

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            switch (sub) {
                case "rimuovi":
                case "visita":
                    if (plugin.getMultiverse() != null && plugin.getMultiverse().isReady())
                        return filter(plugin.getMultiverse().listWorlds(), args[1]);
                    return empty();
                case "whitelist":
                case "blacklist":
                    return filter(WL_ACTIONS, args[1]);
                case "aggiungi":
                    return empty();
            }
        }
        if (args.length == 3) {
            if (sub.equals("whitelist") || sub.equals("blacklist")) {
                if (plugin.getMultiverse() != null && plugin.getMultiverse().isReady())
                    return filter(plugin.getMultiverse().listWorlds(), args[2]);
                return empty();
            }
        }
        if (args.length == 4) {
            if (sub.equals("whitelist") || sub.equals("blacklist")) {
                String action = args[1].toLowerCase();
                if (action.equals("aggiungi") || action.equals("rimuovi")) {
                    return filterPlayers(args[3]);
                }
            }
        }
        return empty();
    }

    // ============================================================
    // UTILITY
    // ============================================================

    private List<String> filter(List<String> options, String prefix) {
        if (prefix == null || prefix.isEmpty()) return new ArrayList<>(options);
        String low = prefix.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(low))
                .sorted()
                .collect(Collectors.toList());
    }

    private List<String> filterPlayers(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> prefix == null || prefix.isEmpty()
                        || n.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT)))
                .sorted()
                .collect(Collectors.toList());
    }

    private List<String> empty() {
        return Collections.emptyList();
    }
}