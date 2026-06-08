package dev.breach.cherryCore.core;

import dev.breach.cherryCore.CherryCore;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Gestisce il mondo "spawn":
 *   - giocatori invisibili + adventure
 *   - quando lasciano il mondo => survival + clear invisibility
 *   - check periodico ogni secondo
 */
public class SpawnWorldListener implements Listener {

    public static final String SPAWN_WORLD = "spawn";

    private final CherryCore plugin;

    public SpawnWorldListener(CherryCore plugin) {
        this.plugin = plugin;
        startTask();
    }

    private void startTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getWorld().getName().equalsIgnoreCase(SPAWN_WORLD)) {
                        applySpawnState(p);
                    } else if (plugin.spawnInvis.containsKey(p.getUniqueId())) {
                        clearSpawnState(p);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public static void applySpawnState(Player p) {
        CherryCore.get().spawnInvis.put(p.getUniqueId(), true);
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,
                80, 0, true, false, false));
        if (p.getGameMode() != GameMode.ADVENTURE) {
            p.setGameMode(GameMode.ADVENTURE);
        }
    }

    public static void clearSpawnState(Player p) {
        CherryCore.get().spawnInvis.remove(p.getUniqueId());
        p.removePotionEffect(PotionEffectType.INVISIBILITY);
        if (p.getGameMode() != GameMode.SURVIVAL) {
            p.setGameMode(GameMode.SURVIVAL);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (p.getWorld().getName().equalsIgnoreCase(SPAWN_WORLD)) {
            applySpawnState(p);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.getWorld().getName().equalsIgnoreCase(SPAWN_WORLD)) {
                applySpawnState(p);
            }
        }, 2L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.spawnInvis.remove(e.getPlayer().getUniqueId());
    }
}