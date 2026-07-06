package dev.breach.DistrictRP.commands.roleplay.logs;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class LogsCommand implements CommandExecutor {

    private final DistrictRP plugin;
    private final LogsAPI api;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm");

    public LogsCommand(DistrictRP plugin, LogsAPI api) {
        this.plugin = plugin;
        this.api = api;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String moduleId = args[0].toLowerCase();
        LogModule module = api.get(moduleId);

        MessageUtils.sendList(sender, "logs.loading");

        if (module == null) {
            MessageUtils.sendList(sender, "logs.no-logs");
            return true;
        }

        UUID filter = null;
        int page = 1;
        if (args.length >= 2) {
            if (args[1].matches("\\d+")) {
                page = Math.max(1, Integer.parseInt(args[1]));
            } else {
                filter = Bukkit.getOfflinePlayer(args[1]).getUniqueId();
                if (args.length >= 3) {
                    try { page = Math.max(1, Integer.parseInt(args[2])); }
                    catch (NumberFormatException ignored) {}
                }
            }
        }

        int perPage = plugin.getConfig().getInt("logs.per-page", 8);
        List<LogEntry> entries = module.fetch(filter, page, perPage);

        if (entries == null || entries.isEmpty()) {
            MessageUtils.sendList(sender, "logs.no-logs");
            return true;
        }

        sender.sendMessage(MessageUtils.get("logs.header", "module", module.getDisplayName()));
        for (LogEntry e : entries) {
            String action = e.getAction() + " &8(" + sdf.format(new Date(e.getTimestamp())) + ")";
            sender.sendMessage(MessageUtils.get("logs.entry",
                    "id", String.valueOf(e.getId()),
                    "player", e.getPlayerName(),
                    "action", action));
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MessageUtils.get("logs.help-header"));
        sender.sendMessage("");
        if (api.getAll().isEmpty()) {
            sender.sendMessage(MessageUtils.color("&7&oNessun modulo di log registrato."));
            return;
        }
        for (LogModule m : api.getAll()) {
            sender.sendMessage(MessageUtils.get("logs.help-entry", "module", m.getId()));
        }
    }
}