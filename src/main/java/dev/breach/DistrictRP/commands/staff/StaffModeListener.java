package dev.breach.DistrictRP.commands.staff;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import dev.breach.DistrictRP.functions.servermode.ServerMode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (!manager.isInStaffMode(p)) return;

        Action action = event.getAction();
        boolean isRight = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
        boolean isLeft = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        if (!isRight && !isLeft) return;

        ItemStack held = event.getItem();
        if (held == null) return;

        StaffModeManager.Tool tool = StaffModeManager.getTool(plugin, held);
        if (tool == null) return;

        event.setCancelled(true);
        event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);

        switch (tool) {
            case TP_RANDOM -> handleTpRandom(p);
            case VANISH -> {
                if (plugin.getVanishManager() != null) plugin.getVanishManager().toggle(p);
            }
            case SCALE_SMALL, SCALE_NORMAL -> applyScale(p, StaffModeManager.getScaleValue(plugin, held));
            case PLAYER_LIST -> gui.openPlayerList(p);
            case STAFF_LIST -> gui.openStaffList(p);
            case EXIT -> manager.exit(p);
        }
    }

    private void handleTpRandom(Player p) {
        String perm = plugin.getConfig().getString("staffmode.tp-random-permission", "DistrictRP.staffmode.tprandom");
        if (!p.hasPermission(perm)) {
            MessageUtils.sendMsg(p, "general.no-permission");
            return;
        }
        List<Player> candidates = new ArrayList<>();
        for (Player o : Bukkit.getOnlinePlayers()) {
            if (!o.equals(p) && !manager.isInStaffMode(o)) candidates.add(o);
        }
        if (candidates.isEmpty()) {
            MessageUtils.sendMsg(p, "staffmode.tp-random-no-players");
            return;
        }
        Player target = candidates.get(random.nextInt(candidates.size()));
        p.teleport(target.getLocation());
        MessageUtils.sendMsg(p, "staffmode.tp-random-success", "player", target.getName());
    }

    private void applyScale(Player p, double value) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "attribute " + p.getName() + " minecraft:scale base set " + value);
        MessageUtils.sendMsg(p, "staffmode.scale-applied", "value", String.valueOf(value));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        if (!manager.isInStaffMode(event.getPlayer())) return;
        if (StaffModeManager.isStaffModeItem(plugin, event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            return;
        }
        if (plugin.getConfig().getBoolean("staffmode.block-drop", true)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInvClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (!manager.isInStaffMode(p)) return;

        if (StaffModeManager.isStaffModeItem(plugin, event.getCurrentItem())
                || StaffModeManager.isStaffModeItem(plugin, event.getCursor())) {
            event.setCancelled(true);
            return;
        }

        if (plugin.getConfig().getBoolean("staffmode.block-inventory-change", true)) {
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(p.getInventory())) {
                event.setCancelled(true);
            } else if (event.isShiftClick()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInvDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (!manager.isInStaffMode(p)) return;
        if (StaffModeManager.isStaffModeItem(plugin, event.getOldCursor())) {
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
        if (StaffModeManager.isStaffModeItem(plugin, event.getMainHandItem())
                || StaffModeManager.isStaffModeItem(plugin, event.getOffHandItem())) {
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("staffmode.auto-staffmode-enabled", true)) return;
        Player p = event.getPlayer();

        String perm = plugin.getConfig().getString(
                "staffmode.auto-staffmode-permission", "DistrictRP.autostaffmode");
        if (!p.hasPermission(perm)) return;

        if (plugin.getServerModeManager() == null) return;
        if (plugin.getServerModeManager().getCurrent() != ServerMode.ROLEPLAY) return;

        long delay = plugin.getConfig().getLong("staffmode.auto-staffmode-delay-ticks", 20L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            if (plugin.getStaffModeManager() == null) return;
            if (plugin.getStaffModeManager().isInStaffMode(p)) return;
            plugin.getStaffModeManager().enter(p);
            MessageUtils.sendMsg(p, "staffmode.auto-entered");
        }, delay);
    }
}