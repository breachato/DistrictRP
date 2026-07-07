package dev.breach.DistrictRP.commands.roleplay.warp;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class WarpCommand implements CommandExecutor, TabCompleter {

    public enum Mode { WARP, SETWARP, DELWARP }

    private final DistrictRP plugin;
    private final WarpManager manager;
    private final Mode mode;

    public WarpCommand(DistrictRP plugin, WarpManager manager, Mode mode) {
        this.plugin = plugin;
        this.manager = manager;
        this.mode = mode;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        switch (mode) {
            case WARP:    return handleWarp(sender, args);
            case SETWARP: return handleSet(sender, args);
            case DELWARP: return handleDel(sender, args);
        }
        return true;
    }

    private boolean handleWarp(CommandSender sender, String[] args) {
        String usePerm = plugin.getConfig().getString("warp.permission-use", "DistrictRP.warp.use");
        if (!sender.hasPermission(usePerm)) {
            MessageUtils.sendMsg(sender, "general.no-permission");
            return true;
        }

        if (args.length == 0) {
            MessageUtils.sendList(sender, "warp.help");
            return true;
        }

        if (args[0].equalsIgnoreCase("lista") || args[0].equalsIgnoreCase("list")) {
            sendList(sender);
            return true;
        }

        if (!(sender instanceof Player player)) {
            MessageUtils.sendMsg(sender, "general.only-player");
            return true;
        }

        Warp warp = manager.get(args[0]);
        if (warp == null) {
            MessageUtils.sendMsg(player, "warp.not-found", "warp", args[0]);
            return true;
        }

        String bypass = plugin.getConfig().getString("warp.permission-bypass", "DistrictRP.warp.bypass");
        if (warp.hasPermission()
                && !player.hasPermission(warp.getPermission())
                && !player.hasPermission(bypass)) {
            MessageUtils.sendMsg(player, "warp.no-permission-warp");
            return true;
        }

        Location loc = warp.toLocation();
        if (loc == null) {
            MessageUtils.sendMsg(player, "warp.not-found", "warp", warp.getName());
            return true;
        }

        plugin.back.put(player.getUniqueId(), player.getLocation());
        player.teleport(loc);

        if (plugin.getConfig().getBoolean("warp.play-sound", true)) {
            try {
                Sound s = Sound.valueOf(plugin.getConfig().getString(
                        "warp.sound-teleport", "ENTITY_ENDERMAN_TELEPORT"));
                player.playSound(player.getLocation(), s, 1.0f, 1.0f);
            } catch (IllegalArgumentException ignored) {}
        }

        if (plugin.getConfig().getBoolean("warp.show-title", true)) {
            String title = MessageUtils.color(MessageUtils.get("warp.teleport-title"));
            String sub = MessageUtils.color(MessageUtils.get("warp.teleport-subtitle",
                    "warp", warp.getName()));
            player.sendTitle(title, sub, 10, 40, 10);
        }

        MessageUtils.sendMsg(player, "warp.teleported", "warp", warp.getName());

        if (plugin.getCoreProtectHook() != null) {
            plugin.getCoreProtectHook().logCustomAction(player, "warp use " + warp.getName());
        }
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendMsg(sender, "general.only-player");
            return true;
        }
        String managePerm = plugin.getConfig().getString("warp.permission-manage", "DistrictRP.warp.manage");
        if (!player.hasPermission(managePerm)) {
            MessageUtils.sendMsg(player, "general.no-permission");
            return true;
        }
        if (args.length < 1) {
            MessageUtils.sendMsg(player, "warp.usage-setwarp");
            return true;
        }
        String name = args[0];
        if (manager.exists(name)) {
            MessageUtils.sendMsg(player, "warp.already-exists", "warp", name);
            return true;
        }
        String perm = args.length >= 2 ? args[1] : "";

        boolean ok = manager.create(name, player.getLocation(), perm);
        if (!ok) {
            MessageUtils.sendMsg(player, "warp.already-exists", "warp", name);
            return true;
        }
        if (perm.isEmpty()) {
            MessageUtils.sendMsg(player, "warp.set", "warp", name);
        } else {
            MessageUtils.sendMsg(player, "warp.set-with-permission",
                    "warp", name, "permission", perm);
        }
        if (plugin.getCoreProtectHook() != null) {
            plugin.getCoreProtectHook().logCustomAction(player, "warp create " + name);
        }
        return true;
    }

    private boolean handleDel(CommandSender sender, String[] args) {
        String managePerm = plugin.getConfig().getString("warp.permission-manage", "DistrictRP.warp.manage");
        if (!sender.hasPermission(managePerm)) {
            MessageUtils.sendMsg(sender, "general.no-permission");
            return true;
        }
        if (args.length < 1) {
            MessageUtils.sendMsg(sender, "warp.usage-delwarp");
            return true;
        }
        if (!manager.delete(args[0])) {
            MessageUtils.sendMsg(sender, "warp.not-found", "warp", args[0]);
            return true;
        }
        MessageUtils.sendMsg(sender, "warp.removed", "warp", args[0]);
        if (sender instanceof Player p && plugin.getCoreProtectHook() != null) {
            plugin.getCoreProtectHook().logCustomAction(p, "warp delete " + args[0]);
        }
        return true;
    }

    private void sendList(CommandSender sender) {
        Collection<Warp> all = manager.all();
        if (all.isEmpty()) {
            MessageUtils.sendMsg(sender, "warp.list-empty");
            return;
        }
        String header = MessageUtils.get("warp.list-header", "count", String.valueOf(all.size()));
        sender.sendMessage(MessageUtils.color(header));

        String bypass = plugin.getConfig().getString("warp.permission-bypass", "DistrictRP.warp.bypass");
        for (Warp w : all) {
            boolean locked = w.hasPermission()
                    && sender instanceof Player p
                    && !p.hasPermission(w.getPermission())
                    && !p.hasPermission(bypass);
            String key = locked ? "warp.list-entry-locked" : "warp.list-entry";
            String line = MessageUtils.get(key,
                    "warp", w.getName(),
                    "world", w.getWorld(),
                    "x", String.valueOf((int) w.getX()),
                    "y", String.valueOf((int) w.getY()),
                    "z", String.valueOf((int) w.getZ()));
            sender.sendMessage(MessageUtils.color(line));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (mode == Mode.WARP) {
            if (args.length == 1) {
                String prefix = args[0].toLowerCase(Locale.ROOT);
                for (Warp w : manager.all()) {
                    if (w.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(w.getName());
                }
                if ("lista".startsWith(prefix)) out.add("lista");
                if ("list".startsWith(prefix)) out.add("list");
            }
        } else if (mode == Mode.DELWARP) {
            if (args.length == 1) {
                String prefix = args[0].toLowerCase(Locale.ROOT);
                for (Warp w : manager.all()) {
                    if (w.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(w.getName());
                }
            }
        } else if (mode == Mode.SETWARP) {
            if (args.length == 1) out.add("<nome>");
            else if (args.length == 2) out.add("<permesso>");
        }
        return out;
    }
}