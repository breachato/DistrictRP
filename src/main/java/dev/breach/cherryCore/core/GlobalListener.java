package dev.breach.cherryCore.core;

import dev.breach.cherryCore.CherryCore;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class GlobalListener implements Listener {

    private final CherryCore plugin;

    public GlobalListener(CherryCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        // TAB integration (via dispatch)
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "tab player " + p.getName() + " tagprefix");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "tab player " + p.getName() + " tabprefix");

        // Glowing per group.perms
        if (p.hasPermission("group.perms")) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,
                    Integer.MAX_VALUE, 0, false, false));
        } else {
            p.removePotionEffect(PotionEffectType.GLOWING);
        }

        // Rec mode persistente
        if (Boolean.TRUE.equals(plugin.recActive.get(p.getUniqueId()))) {
            String skin = plugin.recSkin.get(p.getUniqueId());
            if (skin != null) {
                Bukkit.dispatchCommand(p, "skin set " + skin);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "lp user " + p.getName() + " permission set rec.active true");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "tab player " + p.getName() + " tagprefix  ");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab reload");
                for (PotionEffect ef : p.getActivePotionEffects()) {
                    p.removePotionEffect(ef.getType());
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // pulizia eventuale (placeholder per AFK ecc.)
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        plugin.back.put(p.getUniqueId(), p.getLocation());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        Player p = e.getPlayer();
        plugin.back.put(p.getUniqueId(), e.getFrom());
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (Boolean.TRUE.equals(plugin.godMode.get(p.getUniqueId()))) {
            e.setCancelled(true);
        }
    }

    // === Protezione creative ===

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    @SuppressWarnings("deprecation")
    public void onPickup(PlayerPickupItemEvent e) {
        Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.CREATIVE) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInvClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (p.getGameMode() == GameMode.CREATIVE) {
            if (e.getClickedInventory() != null
                    && !e.getClickedInventory().equals(p.getInventory())) {
                e.setCancelled(true);
            }
        }
    }
}