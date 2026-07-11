package dev.breach.DistrictRP.functions;

import dev.breach.DistrictRP.DistrictRP;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class DistrictTabManager implements TabCompleter {

    private final DistrictRP plugin;

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
            "build", "mondo",
            "vanish", "staffmode",
            "ticket", "supporto", "appuntamento",
            "stuck", "playtime", "stafflist",
            "emoji", "chatsym", "logs",
            "protection",
            "azione", "bisbiglio", "urlo"
    );

    private static final List<String> PERMS_SUBS = Arrays.asList("list", "add", "remove");
    private static final List<String> SPEED_NUMS = Arrays.asList("0","1","2","3","4","5","6","7","8","9","10");

    private static final List<String> TICKET_SUBS = Arrays.asList(
            "crea", "claim", "unclaim", "chiudi", "info",
            "categoria", "riapri", "commenta", "lista", "all", "aperti"
    );

    private static final List<String> CHATSYM_SUBS = Arrays.asList("aggiungi", "rimuovi");
    private static final List<String> LOGS_SUBS = Arrays.asList("chat","comandi","morti","bank","police","crime","hit","job","smanetta","borghese","blitz");
    private static final List<String> PROTECTION_SUBS = Arrays.asList("toggle", "whitelist", "interactions", "info");
    private static final List<String> PROTECTION_TOGGLE = Arrays.asList("build", "interact");
    private static final List<String> PROTECTION_WL = Arrays.asList("add", "remove", "list");

    private static final List<String> MONDO_SUBS = Arrays.asList(
            "aggiungi", "crea", "add",
            "rimuovi", "elimina", "remove",
            "visita", "tp",
            "lista", "list",
            "whitelist", "blacklist"
    );
    private static final List<String> MONDO_WL_ACTIONS = Arrays.asList(
            "aggiungi", "rimuovi", "lista", "add", "remove", "list"
    );

    private static final List<String> WIPE_TYPES = Arrays.asList("ruoli", "progressi", "playtime", "all");
    private static final List<String> PROFILO_SUBS = Arrays.asList(
            "nome","cognome","eta","nascita","sesso","bio","nazionalita","job","vedi"
    );

    public DistrictTabManager(DistrictRP plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {

        String cmd = command.getName().toLowerCase();

        switch (cmd) {
            case "districtrp": return handleDistrictRP(sender, args);
            case "perms":      return handlePerms(sender, args);
            case "home":
            case "delhome":    return handleHomes(sender, args);
            case "warp":
            case "delwarp":    return handleWarps(sender, args);
            case "speed":      return handleSpeed(sender, args);
            case "mondo":
            case "mondi":      return handleMondo(sender, args);
            case "ticket":
            case "aiuto":      return handleTicket(sender, args);
            case "chatsym":    return handleChatSym(sender, args);
            case "logs":       return handleLogs(sender, args);
            case "protection":
            case "prot":       return handleProtection(sender, args);
            case "wipe":       return handleWipe(sender, args);
            case "profilo":
            case "profile":    return handleProfilo(sender, args);
            case "playtime":
            case "info":
                if (args.length == 1) return filterPlayers(args[0]);
                return empty();
            case "fly": case "god": case "gms": case "gmc": case "gma": case "gmsp":
            case "tphere": case "invsee": case "clear": case "tpall": case "enderchest": case "msg":
                if (args.length == 1) return filterPlayers(args[0]);
                return empty();
            case "nick":
                if (args.length == 2) return filterPlayers(args[1]);
                return empty();
            case "vanish": case "v":
                if (args.length == 1 && sender.hasPermission("DistrictRP.vanish.others"))
                    return filterPlayers(args[0]);
                return empty();
            case "spawn": case "setspawn":
                if (args.length == 1) return filterWorlds(args[0]);
                return empty();
            default:
                return empty();
        }
    }

    private List<String> handleDistrictRP(CommandSender sender, String[] args) {
        if (args.length == 1) return filter(CC_SUBS, args[0]);
        if (args.length >= 2) {
            String sub = args[0].toLowerCase();
            if (Arrays.asList("fly","god","tphere","invsee","enderchest","clear",
                    "gms","gmc","gma","gmsp","msg","nick","playtime").contains(sub)) {
                if (args.length == 2) return filterPlayers(args[1]);
            }
            if (sub.equals("speed")) {
                if (args.length == 2) return filter(SPEED_NUMS, args[1]);
                if (args.length == 3) return filterPlayers(args[2]);
            }
            if (sub.equals("mondo")) return handleMondo(sender, shiftArgs(args));
            if (sub.equals("ticket")) return handleTicket(sender, shiftArgs(args));
            if (sub.equals("chatsym")) return handleChatSym(sender, shiftArgs(args));
            if (sub.equals("logs")) return handleLogs(sender, shiftArgs(args));
            if (sub.equals("protection")) return handleProtection(sender, shiftArgs(args));
            if (sub.equals("spawn") || sub.equals("setspawn")) {
                if (args.length == 2) return filterWorlds(args[1]);
            }
        }
        return empty();
    }

    private List<String> handleTicket(CommandSender sender, String[] args) {
        if (args.length == 1) return filter(TICKET_SUBS, args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("lista")) return filterPlayers(args[1]);
        if (args.length == 3 && args[0].equalsIgnoreCase("categoria")) {
            if (plugin.getRoleplay() != null && plugin.getRoleplay().getTicketManager() != null) {
                List<String> cats = new ArrayList<>();
                plugin.getRoleplay().getTicketManager().getCategories().forEach(c -> cats.add(c.getId()));
                return filter(cats, args[2]);
            }
        }
        return empty();
    }

    private List<String> handleChatSym(CommandSender sender, String[] args) {
        if (args.length == 1) return filter(CHATSYM_SUBS, args[0]);
        if (args.length == 2 && args[0].equalsIgnoreCase("rimuovi")) {
            if (plugin.getRoleplay() != null && plugin.getRoleplay().getChatSymManager() != null) {
                return filter(new ArrayList<>(plugin.getRoleplay().getChatSymManager().getSymbols().keySet()), args[1]);
            }
        }
        return empty();
    }

    private List<String> handleLogs(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (plugin.getRoleplay() != null && plugin.getRoleplay().getLogsAPI() != null) {
                List<String> mods = new ArrayList<>();
                plugin.getRoleplay().getLogsAPI().getAll().forEach(m -> mods.add(m.getId()));
                if (!mods.isEmpty()) return filter(mods, args[0]);
            }
            return filter(LOGS_SUBS, args[0]);
        }
        if (args.length == 2) return filterPlayers(args[1]);
        if (args.length == 3) return filter(Arrays.asList("1","2","3","4","5"), args[2]);
        return empty();
    }

    private List<String> handleProtection(CommandSender sender, String[] args) {
        List<String> subs = Arrays.asList(
                "crea", "elimina", "flag", "info", "lista",
                "aggiungimembro", "rimuovimembro",
                "aggiungiowner", "rimuoviowner",
                "wand", "select", "expand", "reload"
        );
        if (args.length == 1) return filter(subs, args[0]);
        return empty();
    }

    private List<String> handlePerms(CommandSender sender, String[] args) {
        if (args.length == 1) return filter(PERMS_SUBS, args[0]);
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("add") || sub.equals("remove")) return filterPlayers(args[1]);
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
        if (args.length == 1) return filter(MONDO_SUBS, args[0]);

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            switch (sub) {
                case "rimuovi": case "elimina": case "remove":
                case "visita":  case "tp":
                    return filter(getAvailableWorlds(), args[1]);
                case "whitelist": case "blacklist":
                    return filter(MONDO_WL_ACTIONS, args[1]);
                case "aggiungi": case "crea": case "add":
                case "lista":    case "list":
                    return empty();
            }
        }
        if (args.length == 3) {
            if (sub.equals("whitelist") || sub.equals("blacklist")) {
                return filter(getAvailableWorlds(), args[2]);
            }
        }
        if (args.length == 4) {
            if (sub.equals("whitelist") || sub.equals("blacklist")) {
                String action = args[1].toLowerCase();
                if (action.equals("aggiungi") || action.equals("rimuovi")
                        || action.equals("add") || action.equals("remove")) {
                    return filterPlayers(args[3]);
                }
            }
        }
        return empty();
    }

    private List<String> handleWipe(CommandSender sender, String[] args) {
        if (args.length == 1) return filterPlayers(args[0]);
        if (args.length == 2) return filter(WIPE_TYPES, args[1]);
        if (args.length == 3) return filter(Collections.singletonList("confirm"), args[2]);
        return empty();
    }

    private List<String> handleProfilo(CommandSender sender, String[] args) {
        if (args.length == 1) return filter(PROFILO_SUBS, args[0]);
        if (args.length == 2) return filterPlayers(args[1]);
        return empty();
    }

    private List<String> getAvailableWorlds() {
        List<String> list = new ArrayList<>();
        if (plugin.getMultiverse() != null && plugin.getMultiverse().isReady()) {
            try { list.addAll(plugin.getMultiverse().listWorlds()); } catch (Throwable ignored) {}
        }
        if (list.isEmpty()) {
            for (var w : Bukkit.getWorlds()) list.add(w.getName());
        }
        return list;
    }

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

    private List<String> filterWorlds(String prefix) {
        return Bukkit.getWorlds().stream()
                .map(w -> w.getName())
                .filter(n -> prefix == null || prefix.isEmpty()
                        || n.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT)))
                .sorted()
                .collect(Collectors.toList());
    }

    private String[] shiftArgs(String[] args) {
        if (args.length <= 1) return new String[0];
        return Arrays.copyOfRange(args, 1, args.length);
    }

    private List<String> empty() { return Collections.emptyList(); }
}