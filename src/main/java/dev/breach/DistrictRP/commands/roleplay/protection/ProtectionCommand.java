package dev.breach.DistrictRP.commands.roleplay.protection;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProtectionCommand implements CommandExecutor, TabCompleter {

    private final DistrictRP plugin;
    private final ProtectionManager manager;
    private final ProtectionInteractionsGUI gui;

    public ProtectionCommand(DistrictRP plugin, ProtectionManager manager, ProtectionInteractionsGUI gui) {
        this.plugin = plugin;
        this.manager = manager;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String perm = plugin.getConfig().getString("protection.admin-permission", "DistrictRP.protection.admin");
        if (!sender.hasPermission(perm)) {
            MessageUtils.sendMsg(sender, "general.no-permission");
            return true;
        }

        if (args.length == 0) {
            MessageUtils.sendList(sender, "protection.help");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "toggle" -> handleToggle(sender, args);
            case "whitelist" -> handleWhitelist(sender, args);
            case "interactions" -> handleInteractions(sender, args);
            case "info" -> handleInfo(sender, args);
            default -> MessageUtils.sendList(sender, "protection.help");
        }
        return true;
    }

    private void handleToggle(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtils.sendList(sender, "protection.help");
            return;
        }
        String world = args[1];
        String type = args[2].toLowerCase();
        manager.ensureWorld(world);

        switch (type) {
            case "build" -> {
                boolean current = manager.isNoBuild(world);
                manager.setNoBuild(world, !current);
                String state = !current ? "&cATTIVO" : "&aDISATTIVO";
                MessageUtils.sendMsg(sender, "protection.toggled",
                        "type", "No-Build", "state", state, "world", world);
            }
            case "interact" -> {
                boolean current = manager.isNoInteract(world);
                manager.setNoInteract(world, !current);
                String state = !current ? "&cATTIVO" : "&aDISATTIVO";
                MessageUtils.sendMsg(sender, "protection.toggled",
                        "type", "No-Interact", "state", state, "world", world);
            }
            default -> MessageUtils.sendList(sender, "protection.help");
        }
    }

    private void handleWhitelist(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtils.sendList(sender, "protection.help");
            return;
        }
        String world = args[1];
        String action = args[2].toLowerCase();

        if (!manager.isWorldConfigured(world)) {
            MessageUtils.sendMsg(sender, "protection.world-not-configured", "world", world);
            return;
        }

        switch (action) {
            case "add" -> {
                if (args.length < 4) { MessageUtils.sendList(sender, "protection.help"); return; }
                String playerName = args[3];
                if (manager.addWhitelist(world, playerName)) {
                    MessageUtils.sendMsg(sender, "protection.whitelist-added",
                            "player", playerName, "world", world);
                } else {
                    MessageUtils.sendMsg(sender, "protection.whitelist-already",
                            "player", playerName);
                }
            }
            case "remove" -> {
                if (args.length < 4) { MessageUtils.sendList(sender, "protection.help"); return; }
                String playerName = args[3];
                if (manager.removeWhitelist(world, playerName)) {
                    MessageUtils.sendMsg(sender, "protection.whitelist-removed",
                            "player", playerName, "world", world);
                } else {
                    MessageUtils.sendMsg(sender, "protection.whitelist-not-found",
                            "player", playerName);
                }
            }
            case "list" -> {
                List<String> list = manager.getWhitelist(world);
                sender.sendMessage(MessageUtils.get("protection.whitelist-header", "world", world));
                if (list.isEmpty()) {
                    sender.sendMessage(MessageUtils.get("protection.whitelist-empty"));
                } else {
                    for (String p : list) {
                        sender.sendMessage(MessageUtils.get("protection.whitelist-entry", "player", p));
                    }
                }
            }
            default -> MessageUtils.sendList(sender, "protection.help");
        }
    }

    private void handleInteractions(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendMsg(sender, "general.only-player");
            return;
        }
        if (args.length < 2) {
            MessageUtils.sendList(sender, "protection.help");
            return;
        }
        String world = args[1];
        if (!manager.isWorldConfigured(world)) {
            manager.ensureWorld(world);
        }
        gui.open(player, world, 0);
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendList(sender, "protection.help");
            return;
        }
        String world = args[1];
        if (!manager.isWorldConfigured(world)) {
            MessageUtils.sendMsg(sender, "protection.world-not-configured", "world", world);
            return;
        }
        String buildState = manager.isNoBuild(world) ? "&cATTIVO" : "&aDISATTIVO";
        String interactState = manager.isNoInteract(world) ? "&cATTIVO" : "&aDISATTIVO";
        int wlCount = manager.getWhitelist(world).size();
        int intCount = manager.getAllowedInteractionsList(world).size();

        sender.sendMessage(MessageUtils.get("protection.info-header", "world", world));
        sender.sendMessage(MessageUtils.get("protection.info-build", "state", buildState));
        sender.sendMessage(MessageUtils.get("protection.info-interact", "state", interactState));
        sender.sendMessage(MessageUtils.get("protection.info-whitelist", "count", String.valueOf(wlCount)));
        sender.sendMessage(MessageUtils.get("protection.info-interactions", "count", String.valueOf(intCount)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.addAll(Arrays.asList("toggle", "whitelist", "interactions", "info"));
        } else if (args.length == 2) {
            Bukkit.getWorlds().forEach(w -> out.add(w.getName()));
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("toggle")) {
                out.addAll(Arrays.asList("build", "interact"));
            } else if (args[0].equalsIgnoreCase("whitelist")) {
                out.addAll(Arrays.asList("add", "remove", "list"));
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("whitelist")
                    && (args[2].equalsIgnoreCase("add") || args[2].equalsIgnoreCase("remove"))) {
                Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
            }
        }
        return out;
    }
}