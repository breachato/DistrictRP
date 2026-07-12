package dev.breach.DistrictRP.staffpanel;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class StaffAuthCommand implements CommandExecutor, TabCompleter {

    private final StaffPanelManager mgr;

    public StaffAuthCommand(StaffPanelManager mgr) {
        this.mgr = mgr;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("DistrictRP.staffpanel.command.staffauth")) {
            sender.sendMessage("§cNon hai il permesso.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("§6Uso: §f/staffauth <register|password> <email> <password> [player]");
            return true;
        }
        String sub = args[0].toLowerCase();
        String email = args[1];
        String password = args[2];
        String playerArg = args.length >= 4 ? args[3] : (sender instanceof Player p ? p.getName() : null);

        if (!PasswordUtil.isValidEmail(email)) {
            sender.sendMessage("§cEmail non valida.");
            return true;
        }

        if (sub.equals("register")) {
            if (playerArg == null) { sender.sendMessage("§cSpecifica il player."); return true; }
            OfflinePlayer op = Bukkit.getOfflinePlayer(playerArg);
            if (op.getUniqueId() == null) { sender.sendMessage("§cPlayer non trovato: " + playerArg); return true; }
            UUID uuid = op.getUniqueId();
            String name = op.getName() != null ? op.getName() : playerArg;
            String hash = PasswordUtil.hash(password);
            mgr.accounts().upsert(uuid, name, email, hash).thenAccept(ok ->
                    sender.sendMessage("§aAccount §6" + email + " §acreato per §f" + name));
            return true;
        }

        if (sub.equals("password")) {
            String hash = PasswordUtil.hash(password);
            mgr.accounts().updatePasswordByEmail(email, hash).thenAccept(updated -> {
                if (updated) sender.sendMessage("§aPassword aggiornata per §6" + email);
                else sender.sendMessage("§cAccount " + email + " non trovato.");
            });
            return true;
        }

        sender.sendMessage("§6Uso: §f/staffauth <register|password> <email> <password> [player]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return Arrays.asList("register", "password");
        if (args.length == 4) {
            List<String> out = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
            return out;
        }
        return Collections.emptyList();
    }
}