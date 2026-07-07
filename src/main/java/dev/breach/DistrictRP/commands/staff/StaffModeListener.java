package dev.breach.DistrictRP.commands.staff;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StaffModeListener implements Listener {

    private final DistrictRP plugin;
    private final StaffModeManager manager;
    private final StaffModeGUI gui;
    private final Random random = new Random();

    public StaffModeListener(DistrictRP plugin, StaffModeManager manager, StaffModeGUI gui) {
        this.plugin = plugin;
        this.manager = manager;
        this.gui = gui;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (!manager.isInStaffMode(p)) return;
        StaffModeItems.Tool tool = StaffModeItems.getTool(plugin, event.getItem());
        if (tool == null) return;
        event.setCancelled(true);

        switch (tool) {
            case TP_RANDOM: handleTpRandom(p); break;
            case INVISIBILITY: manager.toggleInvisibility(p); break;
            case PLAYER_LIST: gui.openPlayerList(p); break;
            case STAFF_LIST: gui.openStaffList(p); break;
            case EXIT: manager.exit(p); break;
        }
    }

    private void handleTpRandom(Player p) {
        String perm = plugin.getConfig().getString("staffmode.tp-random-permission", "DistrictRP.staffmode.tprandom");
        if (!p.hasPermission(perm)) {
            MessageUtils.sendMsg(p, "general.no-permission");
            return;
        }
        List<Player> candidates = new ArrayList<>();
        for (Player o : Bukkit.getOnlinePlayers()) if (!o.equals(p)) candidates.add(o);
        if (candidates.isEmpty()) { MessageUtils.sendMsg(p, "staffmode.tp-random-no-players"); return; }
        Player target = candidates.get(random.nextInt(candidates.size()));
        p.teleport(target.getLocation());
        MessageUtils.sendMsg(p, "staffmode.tp-random-success", "player", target.getName());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        if (!manager.isInStaffMode(event.getPlayer())) return;
        if (plugin.getConfig().getBoolean("staffmode.block-drop", true)) {
            event.setCancelled(true);
            return;
        }
        if (StaffModeItems.getTool(plugin, event.getItemDrop().getItemStack()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInvClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (!manager.isInStaffMode(p)) return;

        boolean isStaffToolCurrent = StaffModeItems.getTool(plugin, event.getCurrentItem()) != null;
        boolean isStaffToolCursor  = StaffModeItems.getTool(plugin, event.getCursor()) != null;
        if (isStaffToolCurrent || isStaffToolCursor) {
            event.setCancelled(true);
            return;
        }

        String title = event.getView().getTitle();
        String tPl = MessageUtils.color(plugin.getConfig().getString(
                "staffmode.gui.player-list-title", "&8Lista Giocatori"));
        String tSt = MessageUtils.color(plugin.getConfig().getString(
                "staffmode.gui.staff-list-title", "&8Lista Staff"));
        if (title.equals(tPl) || title.equals(tSt)) {
            event.setCancelled(true);
            return;
        }

        if (!plugin.getConfig().getBoolean("staffmode.block-inventory-change", true)) return;

        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory().equals(p.getInventory())) {
            event.setCancelled(true);
            return;
        }
        if (event.isShiftClick()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInvDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (!manager.isInStaffMode(p)) return;
        if (StaffModeItems.getTool(plugin, event.getOldCursor()) != null) {
            event.setCancelled(true);
            return;
        }
        if (plugin.getConfig().getBoolean("staffmode.block-inventory-change", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (!manager.isInStaffMode(event.getPlayer())) return;
        if (StaffModeItems.getTool(plugin, event.getMainHandItem()) != null
                || StaffModeItems.getTool(plugin, event.getOffHandItem()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (!manager.isInStaffMode(p)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (!plugin.getConfig().getBoolean("staffmode.block-block-break", true)) return;
        if (manager.isInStaffMode(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (!plugin.getConfig().getBoolean("staffmode.block-block-place", true)) return;
        if (manager.isInStaffMode(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!plugin.getConfig().getBoolean("staffmode.block-damage", true)) return;
        if (event.getEntity() instanceof Player p && manager.isInStaffMode(p)) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (manager.isInStaffMode(event.getPlayer())) manager.exit(event.getPlayer());
    }
}