package dev.breach.cherryCore.commands.staff;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Random;

public class StaffModeListener implements Listener {

    public static final String TP_GUI_TITLE = ChatColor.translateAlternateColorCodes(
            '&', "&d❀ Teleport Player ❀");

    private final CherryCore plugin;
    private final Random random = new Random();

    public StaffModeListener(CherryCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (plugin.getVanishManager().isVanished(p) || StaffModeCommand.isInStaffMode(p)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageBy(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p) {
            if (StaffModeCommand.isInStaffMode(p) && !(e.getEntity() instanceof Player)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent e) {
        if (e.getTarget() instanceof Player p) {
            if (plugin.getVanishManager().isVanished(p)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p) {
            if (plugin.getVanishManager().isVanished(p) || StaffModeCommand.isInStaffMode(p)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        if (StaffModeCommand.isInStaffMode(p)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInvMove(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!StaffModeCommand.isInStaffMode(p)) return;
        ItemStack it = e.getCurrentItem();
        if (StaffModeCommand.isStaffTool(it)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInvDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (StaffModeCommand.isInStaffMode(p)) {
            for (ItemStack it : e.getNewItems().values()) {
                if (StaffModeCommand.isStaffTool(it)) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onUseTool(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!StaffModeCommand.isInStaffMode(p)) return;
        ItemStack it = e.getItem();
        if (!StaffModeCommand.isStaffTool(it)) return;

        e.setCancelled(true);

        if (e.getAction() != Action.RIGHT_CLICK_AIR
                && e.getAction() != Action.RIGHT_CLICK_BLOCK
                && e.getAction() != Action.LEFT_CLICK_AIR
                && e.getAction() != Action.LEFT_CLICK_BLOCK) return;

        ItemMeta meta = it.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) return;
        String name = ChatColor.stripColor(meta.getDisplayName());

        if (name.contains("Teleport Player")) {
            openPlayerTpGui(p);
        }
        else if (name.contains("Random TP")) {
            randomTeleport(p);
        }
        else if (name.contains("Toggle Vanish")) {
            plugin.getVanishManager().toggle(p);
        }
        else if (name.contains("Enderchest")) {
            p.openInventory(p.getEnderChest());
        }
        else if (name.contains("Esci da Staff Mode")) {
            StaffModeCommand.exitStaffMode(plugin, p);
        }
    }

    @EventHandler
    public void onInteractPlayer(PlayerInteractAtEntityEvent e) {
        Player p = e.getPlayer();
        if (!StaffModeCommand.isInStaffMode(p)) return;
        Entity target = e.getRightClicked();
        if (!(target instanceof Player tp)) return;

        ItemStack it = p.getInventory().getItemInMainHand();
        if (!StaffModeCommand.isStaffTool(it)) return;

        e.setCancelled(true);
        ItemMeta meta = it.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) return;
        String name = ChatColor.stripColor(meta.getDisplayName());

        if (name.contains("Inventario")) {
            p.openInventory(tp.getInventory());
            MessageUtils.sendPrefixed(p, "&fInventario di &d" + tp.getName() + "&f.");
        }
        else if (name.contains("Info Player")) {
            sendPlayerInfo(p, tp);
        }
    }

    private void sendPlayerInfo(Player viewer, Player target) {
        MessageUtils.send(viewer, "");
        MessageUtils.send(viewer, "");
        MessageUtils.send(viewer, "&d&l   INFO &f" + target.getName());
        MessageUtils.send(viewer, "");
        MessageUtils.send(viewer, "&d  ▸ IP: &7" + (target.getAddress() != null
                ? target.getAddress().getAddress().getHostAddress() : "?"));
        MessageUtils.send(viewer, "&d  ▸ Mondo: &7" + target.getWorld().getName());
        MessageUtils.send(viewer, "&d  ▸ Gamemode: &7" + target.getGameMode());
        MessageUtils.send(viewer, "&d  ▸ Health: &7" + Math.round(target.getHealth()));
        MessageUtils.send(viewer, "&d  ▸ Ping: &7" + target.getPing() + "ms");
        MessageUtils.send(viewer, "&d  ▸ Posizione: &7"
                + target.getLocation().getBlockX() + ", "
                + target.getLocation().getBlockY() + ", "
                + target.getLocation().getBlockZ());
        MessageUtils.send(viewer, "");
    }

    private void randomTeleport(Player p) {
        if (Bukkit.getOnlinePlayers().size() <= 1) {
            MessageUtils.sendPrefixed(p, "&cNessun player disponibile per il teletrasporto.");
            return;
        }

        Player[] players = Bukkit.getOnlinePlayers().toArray(new Player[0]);

        Player target;
        do {
            target = players[random.nextInt(players.length)];
        } while (target.getUniqueId().equals(p.getUniqueId()));

        Location loc = target.getLocation().clone();

        p.teleport(loc);
        MessageUtils.sendPrefixed(p, "&aTeletrasportato su &f" + target.getName() + "&a.");
    }

    private void openPlayerTpGui(Player viewer) {
        int size = (int) Math.ceil(Bukkit.getOnlinePlayers().size() / 9.0) * 9;
        if (size < 9) size = 9;
        if (size > 54) size = 54;

        Inventory gui = Bukkit.createInventory(null, size, TP_GUI_TITLE);
        int slot = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (slot >= size) break;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta m = head.getItemMeta();
            if (m instanceof org.bukkit.inventory.meta.SkullMeta sm) {
                sm.setOwningPlayer(online);
                sm.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&d" + online.getName()));
                sm.setLore(java.util.Arrays.asList(
                        ChatColor.translateAlternateColorCodes('&', "&7Mondo: &f" + online.getWorld().getName()),
                        ChatColor.translateAlternateColorCodes('&', "&7Health: &f" + Math.round(online.getHealth())),
                        ChatColor.translateAlternateColorCodes('&', "&7Ping: &f" + online.getPing() + "ms"),
                        "",
                        ChatColor.translateAlternateColorCodes('&', "&d▸ &fClick per teletrasportarti")
                ));
                head.setItemMeta(sm);
            }
            gui.setItem(slot++, head);
        }
        viewer.openInventory(gui);
    }

    @EventHandler
    public void onTpGuiClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(TP_GUI_TITLE)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player viewer)) return;
        ItemStack it = e.getCurrentItem();
        if (it == null || !it.hasItemMeta()) return;
        ItemMeta m = it.getItemMeta();
        if (!(m instanceof org.bukkit.inventory.meta.SkullMeta sm)) return;
        if (sm.getOwningPlayer() == null) return;

        Player target = Bukkit.getPlayer(sm.getOwningPlayer().getUniqueId());
        if (target == null) {
            MessageUtils.sendPrefixed(viewer, "&cGiocatore non piu online.");
            return;
        }
        viewer.closeInventory();
        viewer.teleport(target.getLocation());
        MessageUtils.sendPrefixed(viewer, "&aTeletrasportato da &f" + target.getName() + "&a.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        plugin.getVanishManager().refreshFor(p);

    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (StaffModeCommand.isInStaffMode(p)) {
            StaffModeCommand.exitStaffMode(plugin, p);
        }
    }
}
