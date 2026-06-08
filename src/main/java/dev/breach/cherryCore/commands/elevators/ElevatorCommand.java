package dev.breach.cherryCore.commands.elevators;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ElevatorCommand implements CommandExecutor {

    private final CherryCore plugin;

    public ElevatorCommand(CherryCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("elevators.admin")) {
            MessageUtils.send(sender, "&cNon hai il permesso di usare questo comando!");
            return true;
        }

        if (!(sender instanceof Player p)) {
            MessageUtils.send(sender, "&cSolo i giocatori.");
            return true;
        }

        // /el o /el help -> help
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(p);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "gui" -> ElevatorGUI.openMain(p);

            case "get" -> {
                if (!p.hasPermission("elevators.give")) {
                    MessageUtils.send(p, "&cNon hai il permesso!");
                    return true;
                }
                String type = "classic";
                if (args.length > 1) {
                    String t = args[1].toLowerCase();
                    if (Set.of("classic","express","vip","freight","glass").contains(t)) type = t;
                }
                p.getInventory().addItem(ElevatorGUI.getElevatorItem(type));
                MessageUtils.sendPrefixed(p, "&aHai ricevuto un ascensore &f(" + type + ")&a!");
            }

            case "give" -> {
                if (!p.hasPermission("elevators.give")) {
                    MessageUtils.send(p, "&cNon hai il permesso!");
                    return true;
                }
                if (args.length < 2) {
                    MessageUtils.send(p, "&cUso: /el give <giocatore> [tipo]");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    MessageUtils.send(p, "&cGiocatore non trovato.");
                    return true;
                }
                String type = "classic";
                if (args.length > 2) {
                    String t = args[2].toLowerCase();
                    if (Set.of("classic","express","vip","freight","glass").contains(t)) type = t;
                }
                target.getInventory().addItem(ElevatorGUI.getElevatorItem(type));
                MessageUtils.sendPrefixed(target, "&aTi è stato dato un ascensore &f(" + type + ")&a!");
                MessageUtils.sendPrefixed(p,      "&aAscensore dato a &f" + target.getName() + "&a.");
            }

            case "setname" -> {
                if (args.length < 2) { MessageUtils.send(p, "&cUso: /el setname <nome>"); return true; }
                Location below = p.getLocation().clone().subtract(0,1,0);
                if (!plugin.getElevators().exists(below)) {
                    MessageUtils.send(p, "&cNessun ascensore sotto di te."); return true;
                }
                String id = ElevatorManager.locToId(below);
                if (!plugin.getElevators().canEdit(p, id)) {
                    MessageUtils.send(p, "&cNon sei il proprietario!"); return true;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < args.length; i++) { if (i>1) sb.append(" "); sb.append(args[i]); }
                plugin.getElevators().setCustomFloor(id, sb.toString());
                MessageUtils.sendPrefixed(p, "&aNome piano impostato a: &f" + sb);
            }

            case "resetname" -> {
                Location below = p.getLocation().clone().subtract(0,1,0);
                if (!plugin.getElevators().exists(below)) {
                    MessageUtils.send(p, "&cNessun ascensore sotto di te."); return true;
                }
                String id = ElevatorManager.locToId(below);
                if (!plugin.getElevators().canEdit(p, id)) {
                    MessageUtils.send(p, "&cNon sei il proprietario!"); return true;
                }
                plugin.getElevators().resetCustomFloor(id);
                MessageUtils.sendPrefixed(p, "&aNome reset a: &f" + plugin.getElevators().getAutoFloorName(id));
            }

            case "setgroup" -> {
                if (args.length < 2) { MessageUtils.send(p, "&cUso: /el setgroup <nome>"); return true; }
                Location below = p.getLocation().clone().subtract(0,1,0);
                if (!plugin.getElevators().exists(below)) {
                    MessageUtils.send(p, "&cNessun ascensore sotto di te."); return true;
                }
                String id = ElevatorManager.locToId(below);
                if (!plugin.getElevators().canEdit(p, id)) {
                    MessageUtils.send(p, "&cNon sei il proprietario!"); return true;
                }
                plugin.getElevators().setGroup(id, args[1]);
                MessageUtils.sendPrefixed(p, "&aGruppo impostato a: &f" + args[1]);
            }

            case "setcooldown" -> {
                if (args.length < 2) { MessageUtils.send(p, "&cUso: /el setcooldown <secondi>"); return true; }
                Location below = p.getLocation().clone().subtract(0,1,0);
                if (!plugin.getElevators().exists(below)) {
                    MessageUtils.send(p, "&cNessun ascensore sotto di te."); return true;
                }
                String id = ElevatorManager.locToId(below);
                if (!plugin.getElevators().canEdit(p, id)) {
                    MessageUtils.send(p, "&cNon sei il proprietario!"); return true;
                }
                try {
                    int cd = Integer.parseInt(args[1]);
                    plugin.getElevators().setCooldown(id, cd);
                    MessageUtils.sendPrefixed(p, "&aCooldown impostato a: &f" + cd + "s");
                } catch (NumberFormatException ex) {
                    MessageUtils.send(p, "&cInserisci un numero valido.");
                }
            }

            case "link" -> {
                Location below = p.getLocation().clone().subtract(0,1,0);
                if (!plugin.getElevators().exists(below)) {
                    MessageUtils.send(p, "&cNessun ascensore sotto di te."); return true;
                }
                String id = ElevatorManager.locToId(below);
                String[] sel = ElevatorGUI.linkSel.computeIfAbsent(p.getUniqueId(), k -> new String[2]);
                if (sel[0] == null) {
                    sel[0] = id;
                    MessageUtils.sendPrefixed(p, "&aPrimo ascensore selezionato! Vai sul secondo e ripeti.");
                } else {
                    if (sel[0].equals(id)) {
                        MessageUtils.send(p, "&cNon puoi collegare un ascensore a se stesso!");
                        return true;
                    }
                    plugin.getElevators().linkBoth(sel[0], id);
                    MessageUtils.sendPrefixed(p, "&aAscensori collegati!");
                    sel[0] = null;
                }
            }

            case "unlink" -> {
                Location below = p.getLocation().clone().subtract(0,1,0);
                if (!plugin.getElevators().exists(below)) {
                    MessageUtils.send(p, "&cNessun ascensore sotto di te."); return true;
                }
                String id = ElevatorManager.locToId(below);
                int removed = 0;
                for (String other : plugin.getElevators().getLinks(id)) {
                    plugin.getElevators().unlinkBoth(id, other);
                    removed++;
                }
                MessageUtils.sendPrefixed(p, "&cRimossi &f" + removed + " &ccollegamenti.");
            }

            case "info" -> {
                Location below = p.getLocation().clone().subtract(0,1,0);
                if (!plugin.getElevators().exists(below)) {
                    MessageUtils.send(p, "&cNessun ascensore sotto di te."); return true;
                }
                ElevatorGUI.openFloorPanel(p, below);
            }

            case "list" -> {
                MessageUtils.send(p, "");
                MessageUtils.send(p, "&d&l ━━━━━━━━━━━━━━━━━━━━━━━━");
                MessageUtils.send(p, "&d&l  ❀ ASCENSORI REGISTRATI ❀");
                MessageUtils.send(p, "&d&l ━━━━━━━━━━━━━━━━━━━━━━━━");
                int count = 0;
                for (String id : plugin.getElevators().allIds()) {
                    count++;
                    Location l = plugin.getElevators().getLocation(id);
                    if (l == null) continue;
                    String fname = plugin.getElevators().getFloorName(id);
                    String et    = plugin.getElevators().getEtype(id);
                    String gr    = plugin.getElevators().getGroup(id);
                    MessageUtils.send(p, "&d  " + count + ". &f" + fname
                            + " &8| &d" + et + " &8| &d" + gr
                            + " &8| &7" + l.getWorld().getName()
                            + " " + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ());
                }
                if (count == 0) MessageUtils.send(p, "&7  Nessun ascensore registrato.");
                MessageUtils.send(p, "&d&l ━━━━━━━━━━━━━━━━━━━━━━━━");
                MessageUtils.send(p, "");
            }

            case "reload" -> {
                if (!p.hasPermission("elevators.reload")) {
                    MessageUtils.send(p, "&cNon hai il permesso!"); return true;
                }
                plugin.getElevators().save();
                MessageUtils.sendPrefixed(p, "&aDati ricaricati!");
            }

            case "deleteall" -> {
                plugin.getElevators().deleteAll();
                MessageUtils.sendPrefixed(p, "&c&lTutti gli ascensori eliminati!");
            }

            default -> sendHelp(p);
        }
        return true;
    }

    private void sendHelp(Player p) {
        MessageUtils.send(p, "");
        MessageUtils.send(p, "&d&l  𝐂𝐡𝐞𝐫𝐫𝐲 𝐔𝐧𝐢𝐯𝐞𝐫𝐬𝐢𝐭𝐲 &8- &7Ascensori");
        MessageUtils.send(p, "");
        MessageUtils.send(p, "&d  /el &8- &fMostra questo help");
        MessageUtils.send(p, "&d  /el gui &8- &fApri la GUI ascensori");
        MessageUtils.send(p, "&d  /el get [tipo] &8- &fOttieni un ascensore");
        MessageUtils.send(p, "&d  /el give <player> [tipo] &8- &fDai un ascensore");
        MessageUtils.send(p, "&d  /el setname <nome> &8- &fRinomina il piano");
        MessageUtils.send(p, "&d  /el resetname &8- &fReset nome automatico");
        MessageUtils.send(p, "&d  /el setgroup <nome> &8- &fImposta gruppo");
        MessageUtils.send(p, "&d  /el setcooldown <sec> &8- &fImposta cooldown");
        MessageUtils.send(p, "&d  /el link &8- &fCollega ascensori");
        MessageUtils.send(p, "&d  /el unlink &8- &fScollega ascensori");
        MessageUtils.send(p, "&d  /el info &8- &fPannello ascensore");
        MessageUtils.send(p, "&d  /el list &8- &fLista ascensori");
        MessageUtils.send(p, "&d  /el reload &8- &fRicarica");
        MessageUtils.send(p, "&d  /el deleteall &8- &fElimina tutti");
        MessageUtils.send(p, "");
        MessageUtils.send(p, "&d  Tipi: &fclassic, express, vip, freight, glass");
        MessageUtils.send(p, "");
    }
}