package dev.breach.DistrictRP.commands.utils;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class WipeCommand implements CommandExecutor, TabCompleter {

    private static final List<String> TYPES = Arrays.asList("ruoli", "progressi", "playtime", "all");

    private final DistrictRP plugin;

    public WipeCommand(DistrictRP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("DistrictRP.wipe")) {
            MessageUtils.sendMsg(sender, "general.no-permission");
            return true;
        }

        if (args.length < 2) {
            MessageUtils.sendList(sender, "wipe.help");
            return true;
        }

        String targetName = args[0];
        String type = args[1].toLowerCase();
        boolean confirm = args.length >= 3 && args[2].equalsIgnoreCase("confirm");

        if (!TYPES.contains(type)) {
            MessageUtils.sendMsg(sender, "wipe.invalid-type");
            return true;
        }

        OfflinePlayer target = resolveTarget(targetName);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            MessageUtils.sendMsg(sender, "general.player-not-found");
            return true;
        }

        if (!confirm) {
            MessageUtils.sendMsg(sender, "wipe.confirm",
                    "player", targetName, "type", type);
            return true;
        }

        UUID uuid = target.getUniqueId();

        switch (type) {
            case "playtime" -> wipePlaytime(sender, uuid, targetName);
            case "progressi" -> wipeProgressi(sender, uuid, targetName);
            case "ruoli" -> wipeRuoli(sender, uuid, targetName);
            case "all" -> {
                wipePlaytime(sender, uuid, targetName);
                wipeProgressi(sender, uuid, targetName);
                wipeRuoli(sender, uuid, targetName);
                MessageUtils.sendMsg(sender, "wipe.completed-all", "player", targetName);
            }
        }

        return true;
    }

    @SuppressWarnings("deprecation")
    private OfflinePlayer resolveTarget(String name) {
        org.bukkit.entity.Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        return Bukkit.getOfflinePlayer(name);
    }

    private void wipePlaytime(CommandSender sender, UUID uuid, String name) {
        try {
            if (plugin.getRoleplay() != null && plugin.getRoleplay().getPlaytimeTracker() != null) {
                plugin.getRoleplay().getPlaytimeTracker().reset(uuid);
                MessageUtils.sendMsg(sender, "wipe.playtime-done", "player", name);
            } else {
                MessageUtils.sendMsg(sender, "wipe.module-unavailable", "module", "playtime");
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[Wipe] Errore reset playtime: " + t.getMessage());
            MessageUtils.sendMsg(sender, "wipe.error", "type", "playtime");
        }
    }

    private void wipeProgressi(CommandSender sender, UUID uuid, String name) {
        try {
            if (plugin.getRoleplay() != null && plugin.getRoleplay().getProfileManager() != null) {
                plugin.getRoleplay().getProfileManager().reset(uuid);
                MessageUtils.sendMsg(sender, "wipe.progressi-done", "player", name);
            } else {
                MessageUtils.sendMsg(sender, "wipe.module-unavailable", "module", "progressi");
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[Wipe] Errore reset progressi: " + t.getMessage());
            MessageUtils.sendMsg(sender, "wipe.error", "type", "progressi");
        }
    }

    private void wipeRuoli(CommandSender sender, UUID uuid, String name) {
        if (!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            MessageUtils.sendMsg(sender, "wipe.luckperms-missing");
            return;
        }

        try {
            LuckPerms lp = LuckPermsProvider.get();
            CompletableFuture<User> future = lp.getUserManager().loadUser(uuid);
            future.thenAcceptAsync(user -> {
                if (user == null) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            MessageUtils.sendMsg(sender, "general.player-not-found"));
                    return;
                }

                Collection<Node> nodes = user.data().toCollection();
                int removed = 0;
                for (Node node : new ArrayList<>(nodes)) {
                    if (node instanceof InheritanceNode) {
                        DataMutateResult res = user.data().remove(node);
                        if (res.wasSuccessful()) removed++;
                    }
                }

                InheritanceNode defaultNode = InheritanceNode.builder("default").build();
                user.data().add(defaultNode);

                lp.getUserManager().saveUser(user);

                int finalRemoved = removed;
                Bukkit.getScheduler().runTask(plugin, () ->
                        MessageUtils.sendMsg(sender, "wipe.ruoli-done",
                                "player", name, "count", String.valueOf(finalRemoved)));
            });
        } catch (Throwable t) {
            plugin.getLogger().warning("[Wipe] Errore reset ruoli LuckPerms: " + t.getMessage());
            MessageUtils.sendMsg(sender, "wipe.error", "type", "ruoli");
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("DistrictRP.wipe")) return out;

        if (args.length == 1) {
            String p = args[0].toLowerCase();
            for (org.bukkit.entity.Player pl : Bukkit.getOnlinePlayers()) {
                if (pl.getName().toLowerCase().startsWith(p)) out.add(pl.getName());
            }
        } else if (args.length == 2) {
            String p = args[1].toLowerCase();
            for (String t : TYPES) if (t.startsWith(p)) out.add(t);
        } else if (args.length == 3) {
            out.add("confirm");
        }
        return out;
    }
}