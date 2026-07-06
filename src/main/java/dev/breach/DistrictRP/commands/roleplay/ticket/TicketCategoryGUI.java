package dev.breach.DistrictRP.commands.roleplay.ticket;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TicketCategoryGUI implements Listener {

    public enum Mode { TICKET, SUPPORTO }

    private final DistrictRP plugin;
    private final TicketManager manager;
    private final NamespacedKey categoryKey;
    private final NamespacedKey modeKey;
    private final Map<UUID, String> pendingReasons = new HashMap<>();
    private final Map<UUID, Mode> pendingModes = new HashMap<>();

    public TicketCategoryGUI(DistrictRP plugin, TicketManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.categoryKey = new NamespacedKey(plugin, "ticket_category");
        this.modeKey = new NamespacedKey(plugin, "ticket_mode");
    }

    public void open(Player player, String reason, Mode mode) {
        pendingReasons.put(player.getUniqueId(), reason == null ? "" : reason);
        pendingModes.put(player.getUniqueId(), mode);

        String title = plugin.getConfig().getString("ticket.gui-title", "&8Seleziona categoria");
        Inventory inv = Bukkit.createInventory(null, 27, MessageUtils.color(title));

        for (TicketCategory cat : manager.getCategories()) {
            ItemStack item = new ItemStack(cat.getMaterial() == null ? Material.PAPER : cat.getMaterial());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(MessageUtils.color(cat.getName()));
            java.util.List<String> lore = new ArrayList<>();
            for (String l : cat.getLore()) lore.add(MessageUtils.color(l));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(categoryKey, PersistentDataType.STRING, cat.getId());
            meta.getPersistentDataContainer().set(modeKey, PersistentDataType.STRING, mode.name());
            item.setItemMeta(meta);
            inv.setItem(cat.getSlot(), item);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = MessageUtils.color(plugin.getConfig().getString("ticket.gui-title", "&8Seleziona categoria"));
        if (!event.getView().getTitle().equals(title)) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        String catId = meta.getPersistentDataContainer().get(categoryKey, PersistentDataType.STRING);
        String modeStr = meta.getPersistentDataContainer().get(modeKey, PersistentDataType.STRING);
        if (catId == null || modeStr == null) return;

        Mode mode = Mode.valueOf(modeStr);
        String reason = pendingReasons.getOrDefault(player.getUniqueId(), "");
        pendingReasons.remove(player.getUniqueId());
        pendingModes.remove(player.getUniqueId());
        player.closeInventory();

        if (mode == Mode.TICKET) {
            Ticket t = manager.create(player.getUniqueId(), player.getName(), catId,
                    reason.isEmpty() ? "(Nessun motivo)" : reason);
            MessageUtils.sendMsg(player, "ticket.created", "id", String.valueOf(t.getId()));
        } else {
            String supportReason = reason.isEmpty() ? "Richiesta supporto rapido" : reason;
            Ticket t = manager.create(player.getUniqueId(), player.getName(), catId, supportReason);
            MessageUtils.sendMsg(player, "supporto.requested");

            TicketCategory cat = manager.getCategory(catId);
            String permNotify = cat != null ? cat.getPermissionNotify() : "";
            String staffPerm = plugin.getConfig().getString("ticket.staff-permission", "districtrp.ticket.staff");
            String notify = MessageUtils.get("supporto.staff-notify",
                    "player", player.getName(), "category", catId);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission(staffPerm) || (!permNotify.isEmpty() && p.hasPermission(permNotify))) {
                    p.sendMessage(notify);
                }
            }
        }
    }
}