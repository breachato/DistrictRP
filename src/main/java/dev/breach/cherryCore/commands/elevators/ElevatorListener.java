package dev.breach.cherryCore.commands.elevators;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ElevatorListener implements Listener {

    private final CherryCore plugin;
    private final Set<UUID> recentJump = new HashSet<>();

    public ElevatorListener(CherryCore plugin) {
        this.plugin = plugin;
    }

    // ===== Posa =====
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        ItemStack item = e.getItemInHand();
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getLore() == null) return;

        String loreStr = String.join("\n", meta.getLore());
        if (!loreStr.contains("Tipo:")) return;

        if (!p.hasPermission("elevators.use")) {
            e.setCancelled(true);
            MessageUtils.send(p, "&cNon hai il permesso di creare ascensori!");
            return;
        }

        String world = e.getBlock().getWorld().getName();
        if (plugin.getElevators().isWorldDisabled(world)) {
            e.setCancelled(true);
            MessageUtils.send(p, "&cGli ascensori sono disabilitati in questo mondo!");
            return;
        }

        Location loc = e.getBlock().getLocation();
        if (plugin.getElevators().exists(loc)) return;

        String etype = "classic";
        if (loreStr.contains("Tipo: Express"))  etype = "express";
        else if (loreStr.contains("Tipo: VIP")) etype = "vip";
        else if (loreStr.contains("Tipo: Freight")) etype = "freight";
        else if (loreStr.contains("Tipo: Glass"))   etype = "glass";

        plugin.getElevators().create(loc, p, etype, e.getBlock().getType().name());
        String id = ElevatorManager.locToId(loc);
        MessageUtils.sendPrefixed(p, "&aAscensore piazzato! Piano: &f"
                + plugin.getElevators().getAutoFloorName(id));
    }

    // ===== Rottura =====
    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Location loc = e.getBlock().getLocation();
        if (!plugin.getElevators().exists(loc)) return;

        String id = ElevatorManager.locToId(loc);
        if (!plugin.getElevators().canEdit(p, id)) {
            e.setCancelled(true);
            MessageUtils.send(p, "&cNon hai il permesso di rimuovere questo ascensore!");
            return;
        }
        plugin.getElevators().delete(id);
        MessageUtils.sendPrefixed(p, "&cAscensore rimosso.");
    }

    // ===== Right click =====
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;
        Player p = e.getPlayer();
        Block b = e.getClickedBlock();
        if (!plugin.getElevators().exists(b.getLocation())) return;

        // Sneak + right click => Floor Panel
        if (p.isSneaking()) {
            ElevatorGUI.openFloorPanel(p, b.getLocation());
            e.setCancelled(true);
            return;
        }

        // Mano vuota => sali
        if (p.getInventory().getItemInMainHand().getType() == Material.AIR) {
            if (plugin.getElevators().isWorldDisabled(b.getWorld().getName())) {
                MessageUtils.send(p, "&cGli ascensori sono disabilitati in questo mondo!");
                return;
            }
            if (!p.hasPermission("elevators.use")) {
                MessageUtils.send(p, "&cNon hai il permesso di usare gli ascensori!");
                return;
            }
            move(p, b.getLocation(), true);
        }
    }

    // ===== Jump (sostituto di "on jump" Skript) =====
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        // Detect salto: y aumenta e player non in volo
        if (e.getFrom().getY() >= e.getTo().getY()) return;
        if (p.isFlying()) return;
        if (!p.isOnGround() && (e.getTo().getY() - e.getFrom().getY()) < 0.4) return;

        UUID id = p.getUniqueId();
        if (recentJump.contains(id)) return;

        Location below = p.getLocation().clone().subtract(0, 1, 0);
        if (!plugin.getElevators().exists(below)) return;
        if (plugin.getElevators().isWorldDisabled(below.getWorld().getName())) return;
        if (!p.hasPermission("elevators.use")) return;

        recentJump.add(id);
        Bukkit.getScheduler().runTaskLater(plugin, () -> recentJump.remove(id), 10L);

        move(p, below, true);
    }

    // ===== Sneak (scendi) =====
    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) return;
        Player p = e.getPlayer();
        Location below = p.getLocation().clone().subtract(0, 1, 0);
        if (!plugin.getElevators().exists(below)) return;
        if (plugin.getElevators().isWorldDisabled(below.getWorld().getName())) return;
        if (!p.hasPermission("elevators.use")) return;

        move(p, below, false);
    }

    // ===== Logica movimento =====
    private void move(Player p, Location originBlockLoc, boolean up) {
        String originId = ElevatorManager.locToId(originBlockLoc);
        String etype    = plugin.getElevators().getEtype(originId);

        if (!"express".equals(etype) && !p.hasPermission("elevators.bypass")) {
            if (plugin.getElevators().isOnCooldown(p)) return;
        }

        String destId = plugin.getElevators().findDestination(originId, up);
        if (destId == null) return;

        Location dest = plugin.getElevators().getLocation(destId);
        if (dest == null) return;
        String fname = plugin.getElevators().getFloorName(destId);

        if (!"express".equals(etype) && !p.hasPermission("elevators.bypass")) {
            int cd = plugin.getElevators().getCooldown(originId);
            plugin.getElevators().setCooldownFor(p, cd);
            MessageUtils.actionBar(p, "&8· &fIn arrivo: &d" + fname + " &8·");
            Bukkit.getScheduler().runTaskLater(plugin, () -> teleport(p, originBlockLoc, dest, fname),
                    cd * 20L);
        } else {
            teleport(p, originBlockLoc, dest, fname);
        }
    }

    private void teleport(Player p, Location origin, Location dest, String fname) {
        double offX = p.getLocation().getX() - origin.getX();
        double offZ = p.getLocation().getZ() - origin.getZ();
        Location finalLoc = new Location(
                dest.getWorld(),
                dest.getX() + offX,
                dest.getY() + 1,
                dest.getZ() + offZ,
                dest.getYaw(),      // ← usa lo yaw del blocco destinazione
                dest.getPitch()     // ← usa il pitch del blocco destinazione
        );
        p.teleport(finalLoc);
        MessageUtils.actionBar(p, "&8· &f" + fname + " &8·");
    }
}