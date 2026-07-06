package dev.breach.DistrictRP.commands.roleplay.ticket;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.*;

public class TicketCommand implements CommandExecutor, TabCompleter {

    private static final List<String> PLAYER_SUBS = Arrays.asList(
            "crea", "chiudi", "info", "commenta", "lista");
    private static final List<String> STAFF_SUBS = Arrays.asList(
            "crea", "claim", "unclaim", "chiudi", "info", "categoria",
            "riapri", "commenta", "lista", "all", "aperti");

    private final DistrictRP plugin;
    private final TicketManager manager;

    public TicketCommand(DistrictRP plugin, TicketManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private SimpleDateFormat sdf() {
        return new SimpleDateFormat(manager.getDateFormat());
    }

    private boolean hasStaffPerm(CommandSender sender) {
        String perm = plugin.getConfig().getString("ticket.staff-permission", "DistrictRP.ticket.staff");
        return sender.hasPermission(perm);
    }

    private boolean canUseSub(CommandSender sender, String sub) {
        if (hasStaffPerm(sender)) return STAFF_SUBS.contains(sub);
        return PLAYER_SUBS.contains(sub);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase();

        if (!canUseSub(sender, sub)) {
            sendHelp(sender);
            return true;
        }

        switch (sub) {
            case "crea" -> handleCreate(sender, args);
            case "claim" -> handleClaim(sender, args);
            case "unclaim" -> handleUnclaim(sender, args);
            case "chiudi" -> handleClose(sender, args);
            case "info" -> handleInfo(sender, args);
            case "categoria" -> handleCategory(sender, args);
            case "riapri" -> handleReopen(sender, args);
            case "commenta" -> handleComment(sender, args);
            case "lista" -> handleList(sender, args);
            case "all" -> handleAll(sender, args);
            case "aperti" -> handleOpen(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        boolean staff = hasStaffPerm(sender);
        String helpKey = staff ? "ticket.help-staff" : "ticket.help-player";
        List<String> lines = MessageUtils.getList(helpKey);

        if (!(sender instanceof Player p)) {
            for (String l : lines) sender.sendMessage(l);
            return;
        }

        List<String> subs = staff ? STAFF_SUBS : PLAYER_SUBS;
        int subIdx = 0;

        for (String line : lines) {
            String stripped = net.md_5.bungee.api.ChatColor.stripColor(MessageUtils.color(line));
            if (stripped.trim().startsWith("»") && subIdx < subs.size()) {
                String currentSub = subs.get(subIdx++);
                String hoverKey = "ticket.help-hover." + currentSub;
                String hover = MessageUtils.get(hoverKey);
                TextComponent comp = new TextComponent(TextComponent.fromLegacyText(line));
                comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new Text(TextComponent.fromLegacyText(hover))));
                comp.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/ticket " + currentSub + " "));
                p.spigot().sendMessage(comp);
            } else {
                p.sendMessage(line);
            }
        }
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendMsg(sender, "general.only-player");
            return;
        }
        String reason = args.length >= 2 ? joinFrom(args, 1) : "";
        plugin.getRoleplay().getTicketGui().open(player, reason, TicketCategoryGUI.Mode.TICKET);
    }

    private void handleClaim(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { MessageUtils.sendMsg(sender, "general.only-player"); return; }
        if (args.length < 2) { sendHelp(sender); return; }
        int id;
        try { id = Integer.parseInt(args[1]); }
        catch (NumberFormatException e) { MessageUtils.sendMsg(sender, "general.invalid-args"); return; }
        if (!manager.claim(id, player.getUniqueId(), player.getName())) {
            MessageUtils.sendMsg(sender, "ticket.not-found");
            return;
        }
        MessageUtils.sendMsg(sender, "ticket.claimed", "id", String.valueOf(id), "staff", player.getName());
    }

    private void handleUnclaim(CommandSender sender, String[] args) {
        if (args.length < 2) { sendHelp(sender); return; }
        int id;
        try { id = Integer.parseInt(args[1]); }
        catch (NumberFormatException e) { MessageUtils.sendMsg(sender, "general.invalid-args"); return; }
        if (!manager.unclaim(id)) { MessageUtils.sendMsg(sender, "ticket.not-found"); return; }
        MessageUtils.sendMsg(sender, "ticket.unclaimed", "id", String.valueOf(id));
    }

    private void handleClose(CommandSender sender, String[] args) {
        if (args.length < 2) { sendHelp(sender); return; }
        int id;
        try { id = Integer.parseInt(args[1]); }
        catch (NumberFormatException e) { MessageUtils.sendMsg(sender, "general.invalid-args"); return; }
        Ticket t = manager.get(id);
        if (t == null) { MessageUtils.sendMsg(sender, "ticket.not-found"); return; }

        boolean isStaff = hasStaffPerm(sender);
        boolean isAuthor = sender instanceof Player p && p.getUniqueId().equals(t.getAuthor());
        if (!isStaff && !isAuthor) { MessageUtils.sendMsg(sender, "ticket.not-yours"); return; }

        String reason = args.length >= 3 ? joinFrom(args, 2) : "Nessun motivo specificato";
        UUID closer = sender instanceof Player p ? p.getUniqueId() : null;
        if (!manager.close(id, closer, sender.getName(), reason)) {
            MessageUtils.sendMsg(sender, "ticket.already-closed");
            return;
        }
        MessageUtils.sendMsg(sender, "ticket.closed", "id", String.valueOf(id));
    }

    private void handleReopen(CommandSender sender, String[] args) {
        if (args.length < 2) { sendHelp(sender); return; }
        int id;
        try { id = Integer.parseInt(args[1]); }
        catch (NumberFormatException e) { MessageUtils.sendMsg(sender, "general.invalid-args"); return; }
        if (!manager.reopen(id)) { MessageUtils.sendMsg(sender, "ticket.not-found"); return; }
        MessageUtils.sendMsg(sender, "ticket.reopened", "id", String.valueOf(id));
    }

    private void handleCategory(CommandSender sender, String[] args) {
        if (args.length < 3) { sendHelp(sender); return; }
        int id;
        try { id = Integer.parseInt(args[1]); }
        catch (NumberFormatException e) { MessageUtils.sendMsg(sender, "general.invalid-args"); return; }
        String cat = args[2].toLowerCase();
        if (!manager.changeCategory(id, cat)) { MessageUtils.sendMsg(sender, "ticket.not-found"); return; }
        MessageUtils.sendMsg(sender, "ticket.category-changed",
                "id", String.valueOf(id), "category", cat);
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) { sendHelp(sender); return; }
        int id;
        try { id = Integer.parseInt(args[1]); }
        catch (NumberFormatException e) { MessageUtils.sendMsg(sender, "general.invalid-args"); return; }
        Ticket t = manager.get(id);
        if (t == null) { MessageUtils.sendMsg(sender, "ticket.not-found"); return; }

        boolean isStaff = hasStaffPerm(sender);
        boolean isAuthor = sender instanceof Player p && p.getUniqueId().equals(t.getAuthor());
        if (!isStaff && !isAuthor) { MessageUtils.sendMsg(sender, "ticket.not-yours"); return; }

        renderInfo(sender, t);
    }

    public void renderInfo(CommandSender sender, Ticket t) {
        boolean isStaff = hasStaffPerm(sender);
        SimpleDateFormat sdf = sdf();

        sender.sendMessage(MessageUtils.get("ticket.info.separator"));

        String titleLine = MessageUtils.get("ticket.info.title", "id", String.valueOf(t.getId()));
        if (sender instanceof Player p) {
            List<String> hover;
            if (!t.isOpen()) {
                hover = MessageUtils.getList("ticket.info-hover.title-closed",
                        "date", sdf.format(new Date(t.getOpenedAt())),
                        "staff", t.getClosedByName() != null ? t.getClosedByName() : "-");
            } else if (t.isClaimed()) {
                hover = MessageUtils.getList("ticket.info-hover.title-open",
                        "date", sdf.format(new Date(t.getOpenedAt())),
                        "staff", t.getClaimedByName());
            } else {
                hover = MessageUtils.getList("ticket.info-hover.title-open-unclaimed",
                        "date", sdf.format(new Date(t.getOpenedAt())));
            }
            TextComponent comp = new TextComponent(TextComponent.fromLegacyText(titleLine));
            comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text(TextComponent.fromLegacyText(String.join("\n", hover)))));
            p.spigot().sendMessage(comp);
        } else {
            sender.sendMessage(titleLine);
        }

        String creatorLine = MessageUtils.get("ticket.info.creator", "author", t.getAuthorName());
        if (sender instanceof Player p) {
            TextComponent comp = new TextComponent(TextComponent.fromLegacyText(creatorLine));
            comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text(TextComponent.fromLegacyText(MessageUtils.get("ticket.info-hover.creator")))));
            comp.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, t.getAuthorName()));
            p.spigot().sendMessage(comp);
        } else {
            sender.sendMessage(creatorLine);
        }

        TicketCategory cat = manager.getCategory(t.getCategory());
        String catName = cat != null ? cat.getName() : t.getCategory();
        sender.sendMessage(MessageUtils.get("ticket.info.category", "category", catName));
        sender.sendMessage(MessageUtils.get("ticket.info.description", "reason", t.getReason()));
        sender.sendMessage("");
        sender.sendMessage(MessageUtils.get("ticket.info.comments-title"));

        if (t.getComments().isEmpty()) {
            sender.sendMessage(MessageUtils.get("ticket.info.no-comments"));
        } else {
            int idx = 0;
            for (TicketComment c : t.getComments()) {
                sendCommentLine(sender, t, c, idx, isStaff);
                idx++;
            }
        }

        sender.sendMessage("");
        if (sender instanceof Player p) {
            if (isStaff) {
                sendStaffActions(p, t);
            } else if (p.getUniqueId().equals(t.getAuthor())) {
                sendPlayerActions(p, t);
            }
        }
        sender.sendMessage(MessageUtils.get("ticket.info.separator"));
    }

    private void sendCommentLine(CommandSender sender, Ticket t, TicketComment c, int index, boolean viewerIsStaff) {
        SimpleDateFormat sdf = sdf();
        String line = MessageUtils.get("ticket.info.comment-entry",
                "commenter", c.getCommenterName(), "comment", c.getText());
        if (!(sender instanceof Player p)) { sender.sendMessage(line); return; }

        List<String> hover;
        if (c.isStaff()) {
            hover = MessageUtils.getList("ticket.info-hover.comment-staff",
                    "date", sdf.format(new Date(c.getTimestamp())));
        } else {
            hover = MessageUtils.getList("ticket.info-hover.comment-user",
                    "date", sdf.format(new Date(c.getTimestamp())));
        }

        TextComponent comp = new TextComponent(TextComponent.fromLegacyText(line));
        comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(TextComponent.fromLegacyText(String.join("\n", hover)))));
        if (viewerIsStaff && !c.isCancelled()) {
            comp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    "/ticket_cancel_comment " + t.getId() + " " + index));
        }
        p.spigot().sendMessage(comp);
    }

    private void sendStaffActions(Player player, Ticket t) {
        TextComponent line = new TextComponent(TextComponent.fromLegacyText(
                MessageUtils.color(MessageUtils.get("ticket.actions.label"))));

        TextComponent commenta = makeAction(
                MessageUtils.get("ticket.actions.btn-commenta"),
                "/ticket commenta " + t.getId() + " ",
                MessageUtils.get("ticket.info-hover.action-commenta"),
                ClickEvent.Action.SUGGEST_COMMAND);

        TextComponent claimOrUnclaim;
        if (t.isClaimed()) {
            claimOrUnclaim = makeAction(
                    MessageUtils.get("ticket.actions.btn-unclaim"),
                    "/ticket unclaim " + t.getId(),
                    MessageUtils.get("ticket.info-hover.action-unclaim"),
                    ClickEvent.Action.RUN_COMMAND);
        } else {
            claimOrUnclaim = makeAction(
                    MessageUtils.get("ticket.actions.btn-claim"),
                    "/ticket claim " + t.getId(),
                    MessageUtils.get("ticket.info-hover.action-claim"),
                    ClickEvent.Action.RUN_COMMAND);
        }

        TextComponent chiudiOrRiapri;
        if (t.isOpen()) {
            chiudiOrRiapri = makeAction(
                    MessageUtils.get("ticket.actions.btn-chiudi"),
                    "/ticket chiudi " + t.getId(),
                    MessageUtils.get("ticket.info-hover.action-chiudi"),
                    ClickEvent.Action.RUN_COMMAND);
        } else {
            chiudiOrRiapri = makeAction(
                    MessageUtils.get("ticket.actions.btn-riapri"),
                    "/ticket riapri " + t.getId(),
                    MessageUtils.get("ticket.info-hover.action-riapri"),
                    ClickEvent.Action.RUN_COMMAND);
        }

        TextComponent rapide = makeAction(
                MessageUtils.get("ticket.actions.btn-rapide"),
                "/ticket_quickreplies " + t.getId(),
                MessageUtils.get("ticket.info-hover.action-rapide"),
                ClickEvent.Action.RUN_COMMAND);

        line.addExtra(commenta);
        line.addExtra(new TextComponent(" "));
        line.addExtra(claimOrUnclaim);
        line.addExtra(new TextComponent(" "));
        line.addExtra(chiudiOrRiapri);
        line.addExtra(new TextComponent(" "));
        line.addExtra(rapide);
        player.spigot().sendMessage(line);
    }

    private void sendPlayerActions(Player player, Ticket t) {
        TextComponent line = new TextComponent(TextComponent.fromLegacyText(
                MessageUtils.color(MessageUtils.get("ticket.actions.label"))));

        TextComponent commenta = makeAction(
                MessageUtils.get("ticket.actions.btn-commenta"),
                "/ticket commenta " + t.getId() + " ",
                MessageUtils.get("ticket.info-hover.action-commenta"),
                ClickEvent.Action.SUGGEST_COMMAND);

        TextComponent chiudi = makeAction(
                MessageUtils.get("ticket.actions.btn-chiudi"),
                "/ticket chiudi " + t.getId(),
                MessageUtils.get("ticket.info-hover.action-chiudi"),
                ClickEvent.Action.RUN_COMMAND);

        line.addExtra(commenta);
        line.addExtra(new TextComponent(" "));
        line.addExtra(chiudi);
        player.spigot().sendMessage(line);
    }

    private TextComponent makeAction(String display, String command, String hover, ClickEvent.Action action) {
        TextComponent comp = new TextComponent(TextComponent.fromLegacyText(MessageUtils.color(display)));
        comp.setClickEvent(new ClickEvent(action, command));
        comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(TextComponent.fromLegacyText(hover))));
        return comp;
    }

    private void handleComment(CommandSender sender, String[] args) {
        if (args.length < 3) { sendHelp(sender); return; }
        int id;
        try { id = Integer.parseInt(args[1]); }
        catch (NumberFormatException e) { MessageUtils.sendMsg(sender, "general.invalid-args"); return; }
        Ticket t = manager.get(id);
        if (t == null) { MessageUtils.sendMsg(sender, "ticket.not-found"); return; }

        boolean isStaff = hasStaffPerm(sender);
        boolean isAuthor = sender instanceof Player p && p.getUniqueId().equals(t.getAuthor());
        if (!isStaff && !isAuthor) { MessageUtils.sendMsg(sender, "ticket.not-yours"); return; }

        String text = joinFrom(args, 2);
        UUID commenter = sender instanceof Player p ? p.getUniqueId() : new UUID(0, 0);
        if (!manager.comment(id, commenter, sender.getName(), text, isStaff)) {
            MessageUtils.sendMsg(sender, "ticket.not-found");
            return;
        }
        MessageUtils.sendMsg(sender, "ticket.comment-added", "id", String.valueOf(id));
    }

    private void handleList(CommandSender sender, String[] args) {
        boolean isStaff = hasStaffPerm(sender);

        UUID authorFilter;
        int page = 1;
        String categoryFilter = null;

        if (!isStaff) {
            if (!(sender instanceof Player p)) { MessageUtils.sendMsg(sender, "general.only-player"); return; }
            authorFilter = p.getUniqueId();
            if (args.length >= 2) {
                try { page = Math.max(1, Integer.parseInt(args[1])); }
                catch (NumberFormatException ignored) {}
            }
        } else {
            authorFilter = null;
            if (args.length >= 2) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
                authorFilter = op.getUniqueId();
            }
            if (args.length >= 3) {
                try { page = Math.max(1, Integer.parseInt(args[2])); }
                catch (NumberFormatException ignored) {}
            }
            if (args.length >= 4) categoryFilter = args[3];
        }

        List<Ticket> list = manager.list(authorFilter, categoryFilter);
        String headerName = authorFilter != null
                ? (Bukkit.getOfflinePlayer(authorFilter).getName() != null
                   ? Bukkit.getOfflinePlayer(authorFilter).getName()
                   : "?")
                : "Tutti";
        String header = MessageUtils.get("ticket.list.header-player",
                "player", headerName,
                "count", String.valueOf(list.size()));
        String navCmd = "lista " + (isStaff && args.length >= 2 ? args[1] : "");
        renderList(sender, list, page, header, navCmd.trim());
    }

    private void handleAll(CommandSender sender, String[] args) {
        int page = 1;
        String categoryFilter = null;
        if (args.length >= 2) {
            try { page = Math.max(1, Integer.parseInt(args[1])); }
            catch (NumberFormatException ignored) {}
        }
        if (args.length >= 3) categoryFilter = args[2];

        List<Ticket> list = categoryFilter == null ? manager.listAll() : manager.list(null, categoryFilter);
        String header = MessageUtils.get("ticket.list.header-all", "count", String.valueOf(list.size()));
        renderList(sender, list, page, header, "all");
    }

    private void handleOpen(CommandSender sender, String[] args) {
        int page = 1;
        String categoryFilter = null;
        if (args.length >= 2) {
            try { page = Math.max(1, Integer.parseInt(args[1])); }
            catch (NumberFormatException ignored) {}
        }
        if (args.length >= 3) categoryFilter = args[2];

        List<Ticket> list = new ArrayList<>();
        for (Ticket t : (categoryFilter == null ? manager.listAll() : manager.list(null, categoryFilter))) {
            if (t.isOpen()) list.add(t);
        }
        String header = MessageUtils.get("ticket.list.header-open", "count", String.valueOf(list.size()));
        renderList(sender, list, page, header, "aperti");
    }

    private void renderList(CommandSender sender, List<Ticket> list, int page, String header, String navCmd) {
        int perPage = manager.getPerPage();
        int totalPages = Math.max(1, (int) Math.ceil(list.size() / (double) perPage));
        if (page > totalPages) page = totalPages;
        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, list.size());

        sender.sendMessage(header);
        sender.sendMessage("");

        for (int i = start; i < end; i++) {
            Ticket t = list.get(i);
            String extra = t.isClaimed()
                    ? MessageUtils.get("ticket.list.extra-waiting")
                    : MessageUtils.get("ticket.list.extra-open");
            String entryTemplate = t.isOpen() ? "ticket.list.entry-open" : "ticket.list.entry-closed";
            String entry = MessageUtils.get(entryTemplate,
                    "id", String.valueOf(t.getId()),
                    "author", t.getAuthorName(),
                    "comments", String.valueOf(t.getCommentCount()),
                    "extra", extra);

            if (sender instanceof Player p) {
                TextComponent comp = new TextComponent(TextComponent.fromLegacyText(entry));
                List<String> hover = t.isClaimed()
                        ? MessageUtils.getList("ticket.list.entry-hover-claimed", "staff", t.getClaimedByName())
                        : MessageUtils.getList("ticket.list.entry-hover-open");
                comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new Text(TextComponent.fromLegacyText(String.join("\n", hover)))));
                comp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/ticket info " + t.getId()));
                p.spigot().sendMessage(comp);
            } else {
                sender.sendMessage(entry);
            }
        }

        if (sender instanceof Player p) {
            sender.sendMessage("");
            TextComponent nav = new TextComponent();
            if (page > 1) {
                TextComponent prev = new TextComponent(TextComponent.fromLegacyText(
                        MessageUtils.get("ticket.list.prev-page")));
                prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/ticket " + navCmd + " " + (page - 1)));
                prev.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new Text(TextComponent.fromLegacyText(MessageUtils.get("ticket.list.prev-hover")))));
                nav.addExtra(prev);
                nav.addExtra(new TextComponent(" "));
            }
            if (page < totalPages) {
                TextComponent next = new TextComponent(TextComponent.fromLegacyText(
                        MessageUtils.get("ticket.list.next-page")));
                next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/ticket " + navCmd + " " + (page + 1)));
                next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new Text(TextComponent.fromLegacyText(MessageUtils.get("ticket.list.next-hover")))));
                nav.addExtra(next);
            }
            p.spigot().sendMessage(nav);
        }
    }

    private String joinFrom(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) sb.append(" ");
            sb.append(args[i]);
        }
        return sb.toString();
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        boolean staff = hasStaffPerm(sender);
        List<String> subs = staff ? STAFF_SUBS : PLAYER_SUBS;

        if (args.length == 1) {
            String p = args[0].toLowerCase();
            for (String s : subs) if (s.startsWith(p)) out.add(s);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("lista") && staff) {
            for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("categoria") && staff) {
            for (TicketCategory c : manager.getCategories()) out.add(c.getId());
        }
        return out;
    }
}