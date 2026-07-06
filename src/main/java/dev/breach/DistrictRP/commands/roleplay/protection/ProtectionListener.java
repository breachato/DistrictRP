package dev.breach.DistrictRP.commands.roleplay.protection;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Set;

public class ProtectionListener implements Listener {

    private final DistrictRP plugin;
    private final ProtectionManager manager;

    public ProtectionListener(DistrictRP plugin, ProtectionManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String world = player.getWorld().getName();
        if (!manager.isNoBuild(world)) return;
        if (manager.canBypass(player)) return;
        if (manager.isWhitelisted(world, player)) return;

        event.setCancelled(true);
        denyFeedback(player, event.getBlock().getLocation(), "protection.no-build");
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        String world = player.getWorld().getName();
        if (!manager.isNoBuild(world)) return;
        if (manager.canBypass(player)) return;
        if (manager.isWhitelisted(world, player)) return;

        event.setCancelled(true);
        denyFeedback(player, event.getBlock().getLocation(), "protection.no-build");
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        String world = player.getWorld().getName();
        if (!manager.isNoBuild(world)) return;
        if (manager.canBypass(player)) return;
        if (manager.isWhitelisted(world, player)) return;

        event.setCancelled(true);
        denyFeedback(player, event.getBlock().getLocation(), "protection.no-build");
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        String world = player.getWorld().getName();
        if (!manager.isNoBuild(world)) return;
        if (manager.canBypass(player)) return;
        if (manager.isWhitelisted(world, player)) return;

        event.setCancelled(true);
        denyFeedback(player, event.getBlock().getLocation(), "protection.no-build");
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        String world = player.getWorld().getName();
        if (!manager.isNoInteract(world)) return;
        if (manager.canBypass(player)) return;
        if (manager.isWhitelisted(world, player)) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Set<Material> allowed = manager.getAllowedInteractions(world);
        if (allowed.contains(block.getType())) return;

        switch (event.getAction()) {
            case RIGHT_CLICK_BLOCK, LEFT_CLICK_BLOCK -> {
                if (isInteractable(block.getType())) {
                    event.setCancelled(true);
                    denyFeedback(player, block.getLocation(), "protection.no-interact");
                }
            }
        }
    }

    private boolean isInteractable(Material mat) {
        String name = mat.name();
        return mat.isInteractable()
                || name.contains("DOOR")
                || name.contains("GATE")
                || name.contains("BUTTON")
                || name.contains("LEVER")
                || name.contains("TRAPDOOR")
                || name.contains("CHEST")
                || name.contains("FURNACE")
                || name.contains("TABLE")
                || name.contains("ANVIL")
                || name.contains("BARREL")
                || name.contains("SHULKER")
                || name.contains("HOPPER")
                || name.contains("DISPENSER")
                || name.contains("DROPPER")
                || name.contains("BEACON")
                || name.contains("BREWING")
                || name.contains("BED")
                || name.contains("NOTE_BLOCK")
                || name.contains("JUKEBOX")
                || name.contains("BELL")
                || name.contains("CAMPFIRE")
                || name.contains("LOOM")
                || name.contains("GRINDSTONE")
                || name.contains("STONECUTTER")
                || name.contains("CARTOGRAPHY")
                || name.contains("SMITHING")
                || name.contains("COMPOSTER")
                || name.contains("LECTERN");
    }

    private void denyFeedback(Player player, Location location, String msgKey) {
        MessageUtils.actionBar(player, MessageUtils.get(msgKey));

        if (manager.showParticle()) {
            try {
                Particle particle = Particle.valueOf(manager.getParticleType());
                Location particleLoc = location.clone().add(0.5, 1.0, 0.5);
                player.getWorld().spawnParticle(
                        particle,
                        particleLoc,
                        manager.getParticleCount(),
                        0.1, 0.3, 0.1,
                        manager.getParticleSpeed()
                );
            } catch (IllegalArgumentException ignored) {}
        }
    }
}