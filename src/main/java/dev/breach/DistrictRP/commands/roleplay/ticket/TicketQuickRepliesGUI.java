package dev.breach.DistrictRP.commands.roleplay.ticket;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TicketQuickRepliesGUI implements Listener {

    private static final String CMD_QUICK = "/ticket_quickreplies ";
    private static final String CMD_CANCEL = "/ticket_cancel_comment ";

    private final DistrictRP plugin;
    private final TicketManager manager;
    private final NamespacedKey ticketIdKey;
    private final NamespacedKey replyIdxKey;
    private final NamespacedKey categoryKey;
    private final NamespacedKey typeKey;

    public TicketQuickRepliesGUI(DistrictRP plugin, TicketManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.ticketIdKey = new NamespacedKey(plugin, "quick_ticket_id");
        this.replyIdxKey = new NamespacedKey(plugin, "quick_reply_idx");
        this.categoryKey = new NamespacedKey(plugin, "quick_category");
        this.typeKey = new NamespacedKey(plugin, "quick_type");
    }

    public void openCategorySelection(Player player, int ticketId) {
        Set<String> qrCategories = manager.getQuickReplyCategories();
        String title = MessageUtils.color(MessageUtils.get("ticket.quick-replies-select-gui-title"));
        int size = Math.max(9, ((qrCategories.size() / 9) + 1) * 9);
        if (size > 54) size = 54;
        Inventory inv = Bukkit.createInventory(null, size, title);

        int slot = 0;
        for (String catId : qrCategories) {
            TicketCategory cat = manager.getCategory(catId);
            Material mat = cat != null ? cat.getMaterial() : Material.PAPER;
            String catName = cat != null ? cat.getName() : catId;

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            String displayName = MessageUtils.color(MessageUtils.get("ticket.quick-replies-category.name",
                    "category", catName));
            meta.setDisplayName(displayName);

            List<String> loreRaw = MessageUtils.getList("ticket.quick-replies-category.lore",
                    "category", catName);
            List<String> lore = new ArrayList<>();
            for (String l : loreRaw) lore.add(MessageUtils.color(l));
            meta.setLore(lore);

            meta.getPersistentDataContainer().set(ticketIdKey, PersistentDataType.INTEGER, ticketId);
            meta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, catId);
            meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "SELECT");
            item.setItemMeta(meta);

            inv.setItem(slot++, item);
            if (slot >= size) break;
        }
        player.openInventory(inv);
    }

    public void openReplies(Player player, int ticketId, String category) {
        List<String> replies = manager.getQuickReplies(category);
        int rows = Math.max(1, (int) Math.ceil(replies.size() / 9.0));
        int size = Math.min(54, rows * 9);
        String title = MessageUtils.color(MessageUtils.get("ticket.quick-replies-gui-title",
                "category", category));
        Inventory inv = Bukkit.createInventory(null, size, title);

        for (int i = 0; i < replies.size() && i < size; i++) {
            String reply = replies.get(i);
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(MessageUtils.color("&#FCD05C" + shorten(reply, 40)));
            List<String> lore = new ArrayList<>();
            for (String l : wrap(reply, 40)) lore.add(MessageUtils.color("&7" + l));
            lore.add("");
            lore.add(MessageUtils.color(MessageUtils.get("ticket.quick-replies-hover")));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(ticketIdKey, PersistentDataType.INTEGER, ticketId);
            meta.getPersistentDataContainer().set(replyIdxKey, PersistentDataType.INTEGER, i);
            meta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, category);
            meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "REPLY");
            item.setItemMeta(meta);
            inv.setItem(i, item);
        }
        player.openInventory(inv);
    }

    private String shorten(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, max - 3) + "...";
    }

    private List<String> wrap(String s, int width) {
        List<String> out = new ArrayList<>();
        String[] words = s.split(" ");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            if (cur.length() + w.length() + 1 > width) {
                out.add(cur.toString());
                cur = new StringBuilder();
            }
            if (cur.length() > 0) cur.append(" ");
            cur.append(w);
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;

        String selectTitle = MessageUtils.color(MessageUtils.get("ticket.quick-replies-select-gui-title"));
        String repliesTitleTest = MessageUtils.color(MessageUtils.get("ticket.quick-replies-gui-title", "category", ""));

        String viewTitle = event.getView().getTitle();
        boolean isSelect = viewTitle.equals(selectTitle);
        boolean isReplies = viewTitle.startsWith(repliesTitleTest.substring(0,
                Math.min(repliesTitleTest.length(), Math.max(1, repliesTitleTest.indexOf('-') > 0 ? repliesTitleTest.indexOf('-') : repliesTitleTest.length()))));

        if (!isSelect && !isReplies) return;
        event.setCancelled(true);

        ItemStack it = event.getCurrentItem();
        if (it == null || !it.hasItemMeta()) return;
        ItemMeta meta = it.getItemMeta();

        String type = meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        Integer tId = meta.getPersistentDataContainer().get(ticketIdKey, PersistentDataType.INTEGER);
        if (type == null || tId == null) return;

        if (type.equals("SELECT")) {
            String cat = meta.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
            if (cat == null) return;
            p.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> openReplies(p, tId, cat));
            return;
        }

        if (type.equals("REPLY")) {
            Integer idx = meta.getPersistentDataContainer().get(replyIdxKey, PersistentDataType.INTEGER);
            String cat = meta.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
            if (idx == null || cat == null) return;
            List<String> replies = manager.getQuickReplies(cat);
            if (idx < 0 || idx >= replies.size()) return;
            String reply = replies.get(idx);
            p.closeInventory();
            manager.comment(tId, p.getUniqueId(), p.getName(), reply, true);
            MessageUtils.sendMsg(p, "ticket.comment-added", "id", String.valueOf(tId));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();
        String msg = event.getMessage();

        if (msg.startsWith(CMD_QUICK)) {
            event.setCancelled(true);
            event.setMessage("/dummyinternalticket");
            String perm = plugin.getConfig().getString("ticket.staff-permission", "DistrictRP.ticket.staff");
            if (!p.hasPermission(perm)) {
                MessageUtils.sendMsg(p, "general.no-permission");
                return;
            }
            try {
                int id = Integer.parseInt(msg.substring(CMD_QUICK.length()).trim());
                openCategorySelection(p, id);
            } catch (NumberFormatException ignored) {
            }
            return;
        }

        if (msg.startsWith(CMD_CANCEL)) {
            event.setCancelled(true);
            event.setMessage("/dummyinternalticket");
            String perm = plugin.getConfig().getString("ticket.staff-permission", "DistrictRP.ticket.staff");
            if (!p.hasPermission(perm)) {
                MessageUtils.sendMsg(p, "general.no-permission");
                return;
            }
            String rest = msg.substring(CMD_CANCEL.length()).trim();
            String[] parts = rest.split(" ");
            if (parts.length < 2) return;
            try {
                int tId = Integer.parseInt(parts[0]);
                int cIdx = Integer.parseInt(parts[1]);
                if (manager.cancelComment(tId, cIdx)) {
                    MessageUtils.sendMsg(p, "ticket.comment-cancelled-by-staff");
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }
}