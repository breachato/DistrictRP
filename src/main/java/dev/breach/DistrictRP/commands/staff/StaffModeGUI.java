package dev.breach.DistrictRP.commands.staff;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StaffModeGUI implements Listener {

    private final DistrictRP plugin;
    private final NamespacedKey targetKey;
    private final NamespacedKey guiKey;

    public StaffModeGUI(DistrictRP plugin) {
        this.plugin = plugin;
        this.targetKey = new NamespacedKey(plugin, "staffmode_target");
        this.guiKey = new NamespacedKey(plugin, "staffmode_gui_type");
    }

    public void openPlayerList(Player viewer) {
        String title = MessageUtils.color(plugin.getConfig().getString(
                "staffmode.gui.player-list-title", "&8Lista Giocatori"));
        int rows = plugin.getConfig().getInt("staffmode.gui.player-list-rows", 6);
        openList(viewer, title, rows,
                Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).toList(), "players");
    }

    public void openStaffList(Player viewer) {
        String staffPerm = plugin.getConfig().getString("staffmode.permission", "DistrictRP.staffmode");
        List<UUID> staff = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(staffPerm) || isRankedStaff(p)) staff.add(p.getUniqueId());
        }
        String title = MessageUtils.color(plugin.getConfig().getString(
                "staffmode.gui.staff-list-title", "&8Lista Staff"));
        int rows = plugin.getConfig().getInt("staffmode.gui.staff-list-rows", 6);
        openList(viewer, title, rows, staff, "staff");
    }

    private boolean isRankedStaff(Player p) {
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("stafflist.ranks");
        if (ranks == null) return false;
        for (String r : plugin.getConfig().getStringList("stafflist.order")) {
            String perm = ranks.getString(r + ".permission", "");
            if (!perm.isEmpty() && p.hasPermission(perm)) return true;
        }
        return false;
    }

    private void openList(Player viewer, String title, int rows, List<UUID> players, String type) {
        int size = Math.min(54, Math.max(9, rows * 9));
        Inventory inv = Bukkit.createInventory(null, size, title);

        int i = 0;
        for (UUID uuid : players) {
            if (i >= size) break;
            Player target = Bukkit.getPlayer(uuid);
            if (target == null) continue;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta == null) continue;

            meta.setOwningPlayer(target);
            meta.setDisplayName(MessageUtils.color("&#FCD05C" + target.getName()));

            List<String> lore = MessageUtils.getList("staffmode.gui-player-hover",
                    "x", String.valueOf(target.getLocation().getBlockX()),
                    "y", String.valueOf(target.getLocation().getBlockY()),
                    "z", String.valueOf(target.getLocation().getBlockZ()),
                    "world", target.getWorld().getName(),
                    "health", String.format("%.1f", target.getHealth()),
                    "max_health", String.format("%.1f", target.getMaxHealth()),
                    "food", String.valueOf(target.getFoodLevel()),
                    "exp", String.format("%.2f", target.getExp()),
                    "exp_next", String.valueOf(target.getExpToLevel()),
                    "level", String.valueOf(target.getLevel()),
                    "gamemode", target.getGameMode().name(),
                    "ping", String.valueOf(target.getPing()),
                    "uuid", target.getUniqueId().toString());
            meta.setLore(lore);

            meta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING, target.getName());
            meta.getPersistentDataContainer().set(guiKey, PersistentDataType.STRING, type);

            head.setItemMeta(meta);
            inv.setItem(i, head);
            i++;
        }

        viewer.openInventory(inv);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            if (isStaffModeGui(event)) {
                event.setCancelled(true);
            }
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) {
            if (isStaffModeGui(event)) {
                event.setCancelled(true);
            }
            return;
        }

        String guiType = meta.getPersistentDataContainer().get(guiKey, PersistentDataType.STRING);
        if (guiType == null) {
            if (isStaffModeGui(event)) {
                event.setCancelled(true);
            }
            return;
        }

        event.setCancelled(true);

        String targetName = meta.getPersistentDataContainer().get(targetKey, PersistentDataType.STRING);
        if (targetName == null) return;

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) return;

        if (event.getClick() == ClickType.LEFT) {
            String perm = plugin.getConfig().getString("staffmode.tp-target-permission", "DistrictRP.staffmode.tptarget");
            if (!viewer.hasPermission(perm)) {
                MessageUtils.sendMsg(viewer, "general.no-permission");
                return;
            }
            viewer.closeInventory();
            viewer.teleport(target.getLocation());
            MessageUtils.sendMsg(viewer, "staffmode.tp-target-success", "player", target.getName());
        } else if (event.getClick() == ClickType.RIGHT) {
            String perm = plugin.getConfig().getString("staffmode.invsee-permission", "DistrictRP.staffmode.invsee");
            if (!viewer.hasPermission(perm)) {
                MessageUtils.sendMsg(viewer, "general.no-permission");
                return;
            }
            viewer.closeInventory();
            viewer.openInventory(target.getInventory());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (isStaffModeGuiDrag(event)) {
            event.setCancelled(true);
        }
    }

    private boolean isStaffModeGui(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        String tPl = MessageUtils.color(plugin.getConfig().getString(
                "staffmode.gui.player-list-title", "&8Lista Giocatori"));
        String tSt = MessageUtils.color(plugin.getConfig().getString(
                "staffmode.gui.staff-list-title", "&8Lista Staff"));
        return title.equals(tPl) || title.equals(tSt);
    }

    private boolean isStaffModeGuiDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        String tPl = MessageUtils.color(plugin.getConfig().getString(
                "staffmode.gui.player-list-title", "&8Lista Giocatori"));
        String tSt = MessageUtils.color(plugin.getConfig().getString(
                "staffmode.gui.staff-list-title", "&8Lista Staff"));
        return title.equals(tPl) || title.equals(tSt);
    }
}