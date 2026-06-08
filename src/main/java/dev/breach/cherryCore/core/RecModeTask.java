package dev.breach.cherryCore.core;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Equivalente di:
 *   every 1 tick: action bar AB_PREFIX + AB_MESSAGE se non in rec
 *   every 6 ticks: cancella l'action bar
 *
 * Implementato come singolo runnable per non duplicare il task.
 */
public class RecModeTask extends BukkitRunnable {

    private final CherryCore plugin;
    private int counter = 0;

    public RecModeTask(CherryCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        counter++;
        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean recActive = Boolean.TRUE.equals(plugin.recActive.get(p.getUniqueId()));
            boolean bypass    = p.hasPermission("rec.bypassflag");

            if (!recActive && !bypass) {
                // ogni 6 tick mando uno spazio per evitare flicker (come nello Skript)
                if (counter % 6 == 0) {
                    MessageUtils.actionBar(p, " ");
                } else {
                    MessageUtils.actionBar(p, MessageUtils.AB_PREFIX + " " + MessageUtils.AB_MESSAGE);
                }
            }
        }
    }
}