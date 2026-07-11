package dev.breach.DistrictRP.core;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import dev.breach.DistrictRP.functions.servermode.ServerMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DistrictRPDispatcher implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("reload", "info", "mode", "migrate");
    private static final List<String> MODES = Arrays.asList("lobby", "creative", "roleplay", "off");

    private final DistrictRP plugin;

    public DistrictRPDispatcher(DistrictRP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {


        if (!sender.hasPermission("DistrictRP.use")) {
            MessageUtils.send(sender, "&c✗ Non hai accesso a DistrictRP.");
            return true;
        }

        if (args.length == 0) {
            sendInfo(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("DistrictRP.reload")) {
                    MessageUtils.send(sender, "&c✗ Non hai il permesso.");
                    return true;
                }
                doReload(sender);
            }
            case "info" -> sendInfo(sender);
            case "mode" -> handleMode(sender, args);
            case "migrate" -> {
                if (!sender.hasPermission("DistrictRP.admin")) {
                    MessageUtils.send(sender, "&c✗ Non hai il permesso.");
                    return true;
                }
                new dev.breach.DistrictRP.database.MigrationCommand(plugin).migrateAll(sender);
            }
            default -> sendInfo(sender);
        }
        return true;
    }

    private void handleMode(CommandSender sender, String[] args) {
        String adminPerm = plugin.getConfig().getString(
                "server-mode.admin-permission", "DistrictRP.servermode.admin");
        if (!sender.hasPermission(adminPerm)) {
            MessageUtils.sendMsg(sender, "general.no-permission");
            return;
        }

        if (plugin.getServerModeManager() == null) {
            MessageUtils.send(sender, "&cServerModeManager non inizializzato.");
            return;
        }

        if (args.length < 2) {
            MessageUtils.sendMsg(sender, "servermode.current",
                    "mode", plugin.getServerModeManager().getCurrentDisplay());
            MessageUtils.sendMsg(sender, "servermode.usage");
            return;
        }

        String target = args[1].toLowerCase(Locale.ROOT);
        if (!MODES.contains(target)) {
            MessageUtils.sendMsg(sender, "servermode.invalid");
            return;
        }

        ServerMode mode = ServerMode.fromString(target);
        ServerMode current = plugin.getServerModeManager().getCurrent();
        if (mode == current) {
            MessageUtils.sendMsg(sender, "servermode.same-mode",
                    "mode", plugin.getServerModeManager().getCurrentDisplay());
            return;
        }

        plugin.getServerModeManager().setMode(mode);
        MessageUtils.sendMsg(sender, "servermode.changed",
                "mode", plugin.getServerModeManager().getCurrentDisplay());
    }

    private void doReload(CommandSender sender) {
        try {
            plugin.reloadConfig();
            MessageUtils.reload();

            if (plugin.getServerModeManager() != null) {
                plugin.getServerModeManager().loadFromConfig();
            }

            if (plugin.getRoleplay() != null) {
                if (plugin.getRoleplay().getTicketManager() != null) {
                    plugin.getRoleplay().getTicketManager().loadCategories();
                }
                if (plugin.getRoleplay().getChatGate() != null) {
                    plugin.getRoleplay().getChatGate().loadChats();
                }
                if (plugin.getRoleplay().getWarpManager() != null) {
                    plugin.getRoleplay().getWarpManager().load();
                }
            }

            MessageUtils.sendPrefixed(sender, "&#FCD05Cᴄᴏɴꜰɪɢᴜʀᴀᴢɪᴏɴᴇ ᴇ ᴍᴇꜱꜱᴀɢɢɪ ʀɪᴄᴀʀɪᴄᴀᴛɪ ᴄᴏɴ ꜱᴜᴄᴄᴇꜱꜱᴏ.");
        } catch (Throwable t) {
            MessageUtils.send(sender, "&cErrore durante il reload: " + t.getMessage());
            plugin.getLogger().warning("[Reload] " + t.getMessage());
        }
    }

    private void sendInfo(CommandSender sender) {
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, "&#FCD05C&lᴅɪꜱᴛʀɪᴄᴛʀᴘ");
        MessageUtils.send(sender, "");
        MessageUtils.send(sender, "&7ᴠᴇʀꜱɪᴏɴᴇ &8» &f" + DistrictRP.BUILD_TAG);
        MessageUtils.send(sender, "&7ᴀᴜᴛᴏʀᴇ &8» &fvisualizzazione");
        if (plugin.getServerModeManager() != null) {
            MessageUtils.send(sender, "&7ᴍᴏᴅᴀʟɪᴛà &8» " + plugin.getServerModeManager().getCurrentDisplay());
        }
        MessageUtils.send(sender, "");
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("DistrictRP.use")) return Collections.emptyList();

        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            List<String> result = new ArrayList<>();
            for (String sub : SUBCOMMANDS) if (sub.startsWith(partial)) result.add(sub);
            return result;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("mode")) {
            String partial = args[1].toLowerCase(Locale.ROOT);
            List<String> result = new ArrayList<>();
            for (String m : MODES) if (m.startsWith(partial)) result.add(m);
            return result;
        }

        return Collections.emptyList();
    }
}