package dev.breach.DistrictRP.commands.staff;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Comandi staff a singola azione (fly, god, vanish, speed, clear, invsee,
 * enderchest, tphere, tpall). Ognuno è un piccolo executor che condivide
 * lo stesso preambolo di permessi/target; raccolti qui per evitare una
 * classe-file per comando.
 */
public class StaffCommands {

    private final DistrictRP plugin;

    public StaffCommands(DistrictRP plugin) {
        this.plugin = plugin;
    }

    // --- toggle su self/altro ---

    @FunctionalInterface
    private interface Toggle {
        void apply(CommandSender sender, Player target, boolean other);
    }

    public CommandExecutor fly() {
        return toggle("fly", (sender, target, other) -> {
            boolean on = !target.getAllowFlight();
            target.setAllowFlight(on);
            target.setFlying(on);
            MessageUtils.sendPrefixed(target, on ? "&fVolo attivato." : "&cVolo disattivato.");
            if (other) MessageUtils.sendPrefixed(sender,
                    (on ? "&fVolo attivato per &e" : "&cVolo disattivato per &e") + target.getName() + "&f.");
        });
    }

    public CommandExecutor god() {
        return toggle("god", (sender, target, other) -> {
            UUID id = target.getUniqueId();
            boolean on = !Boolean.TRUE.equals(plugin.godMode.get(id));
            if (on) plugin.godMode.put(id, true); else plugin.godMode.remove(id);
            MessageUtils.sendPrefixed(target, on ? "&fGodmode attivato." : "&cGodmode disattivato.");
            if (other) MessageUtils.sendPrefixed(sender,
                    (on ? "&fGodmode attivato per &e" : "&cGodmode disattivato per &e") + target.getName() + "&f.");
        });
    }

    public CommandExecutor vanish() {
        return toggle("vanish", (sender, target, other) -> {
            plugin.getVanishManager().toggle(target);
            if (other) MessageUtils.sendPrefixed(sender, "&fVanish di &e" + target.getName() + " &ftoggleato.");
        });
    }

    // --- velocità ---

    public CommandExecutor speed() {
        return (sender, cmd, label, args) -> {
            if (denied(sender, "speed")) return true;
            if (args.length == 0) {
                MessageUtils.sendPrefixed(sender, "&cUsa: /speed <0-10> [player]");
                return true;
            }
            int spd;
            try {
                spd = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                MessageUtils.sendPrefixed(sender, "&cVelocità non valida.");
                return true;
            }
            if (spd < 0 || spd > 10) {
                MessageUtils.sendPrefixed(sender, "&cVelocità tra 0 e 10.");
                return true;
            }
            Player target;
            if (args.length > 1) {
                if (denied(sender, "speed.others")) return true;
                target = online(sender, args[1]);
                if (target == null) return true;
            } else {
                target = self(sender, "/speed <0-10> <player>");
                if (target == null) return true;
            }
            float val = spd / 10f;
            target.setWalkSpeed(val);
            target.setFlySpeed(val);
            MessageUtils.sendPrefixed(target, "&fVelocità impostata a &e" + spd + "&f.");
            if (!target.equals(sender)) {
                MessageUtils.sendPrefixed(sender,
                        "&fVelocità di &e" + target.getName() + " &fimpostata a &e" + spd + "&f.");
            }
            return true;
        };
    }

    // --- inventari ---

    public CommandExecutor clear() {
        return (sender, cmd, label, args) -> {
            if (denied(sender, "clear")) return true;
            if (args.length == 0) {
                Player p = self(sender, "/clear <player>");
                if (p == null) return true;
                p.getInventory().clear();
                MessageUtils.sendPrefixed(p, "&fInventario svuotato.");
                return true;
            }
            if (denied(sender, "clear.others")) return true;
            Player target = online(sender, args[0]);
            if (target == null) return true;
            target.getInventory().clear();
            MessageUtils.sendPrefixed(sender, "&fInventario di &e" + target.getName() + " &fsvuotato.");
            MessageUtils.sendPrefixed(target, "&fInventario svuotato da &e" + sender.getName() + "&f.");
            return true;
        };
    }

    public CommandExecutor enderchest() {
        return (sender, cmd, label, args) -> {
            Player p = self(sender, "/enderchest <player>");
            if (p == null) return true;
            if (denied(p, "enderchest")) return true;
            if (args.length == 0) {
                p.openInventory(p.getEnderChest());
                return true;
            }
            if (denied(p, "enderchest.others")) return true;
            Player target = online(p, args[0]);
            if (target == null) return true;
            p.openInventory(target.getEnderChest());
            MessageUtils.sendPrefixed(p, "&fEnderchest di &e" + target.getName() + "&f.");
            return true;
        };
    }

    public CommandExecutor invsee() {
        return (sender, cmd, label, args) -> {
            Player p = self(sender, "/invsee <player>");
            if (p == null) return true;
            if (denied(p, "invsee")) return true;
            if (args.length == 0) {
                MessageUtils.sendPrefixed(p, "&cUsa: /invsee <player>");
                return true;
            }
            Player target = online(p, args[0]);
            if (target == null) return true;
            if (target.equals(p)) {
                MessageUtils.sendPrefixed(p, "&cNon puoi vedere il tuo stesso inventario.");
                return true;
            }
            p.openInventory(target.getInventory());
            MessageUtils.sendPrefixed(p, "&fInventario di &e" + target.getName() + "&f.");
            return true;
        };
    }

    // --- teleport ---

    public CommandExecutor tphere() {
        return (sender, cmd, label, args) -> {
            Player p = self(sender, "/tphere <player>");
            if (p == null) return true;
            if (denied(p, "tphere")) return true;
            if (args.length == 0) {
                MessageUtils.sendPrefixed(p, "&cUsa: /tphere <player>");
                return true;
            }
            Player target = online(p, args[0]);
            if (target == null) return true;
            if (target.equals(p)) {
                MessageUtils.sendPrefixed(p, "&cNon puoi teleportare te stesso.");
                return true;
            }
            target.teleport(p.getLocation());
            MessageUtils.sendPrefixed(p, "&e" + target.getName() + " &fteleportato da te.");
            MessageUtils.sendPrefixed(target, "&fTeleportato da &e" + p.getName() + "&f.");
            return true;
        };
    }

    public CommandExecutor tpall() {
        return (sender, cmd, label, args) -> {
            Player p = self(sender, "/tpall");
            if (p == null) return true;
            if (denied(p, "tpall")) return true;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(p)) continue;
                online.teleport(p.getLocation());
                MessageUtils.sendPrefixed(online, "&fTeleportato da &e" + p.getName() + "&f.");
            }
            MessageUtils.sendPrefixed(p, "&fTutti teleportati da te.");
            return true;
        };
    }

    // --- helper condivisi ---

    private CommandExecutor toggle(String node, Toggle action) {
        return (sender, cmd, label, args) -> {
            if (denied(sender, node)) return true;
            Player target;
            boolean other = false;
            if (args.length > 0) {
                if (denied(sender, node + ".others")) return true;
                target = online(sender, args[0]);
                if (target == null) return true;
                other = true;
            } else {
                target = self(sender, "/" + node + " <player>");
                if (target == null) return true;
            }
            action.apply(sender, target, other);
            return true;
        };
    }

    private boolean denied(CommandSender sender, String node) {
        if (sender.hasPermission("DistrictRP." + node)) return false;
        MessageUtils.send(sender, "&cNon hai il permesso.");
        return true;
    }

    private Player self(CommandSender sender, String usage) {
        if (sender instanceof Player p) return p;
        MessageUtils.send(sender, "&cUsa: " + usage);
        return null;
    }

    private Player online(CommandSender sender, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) MessageUtils.send(sender, "&cGiocatore non trovato.");
        return target;
    }
}
