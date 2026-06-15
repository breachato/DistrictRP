package dev.breach.cherryCore.core;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

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
                if (counter % 6 == 0) {
                    MessageUtils.actionBar(p, " ");
                } else {
                    MessageUtils.actionBar(p, MessageUtils.AB_PREFIX + " " + MessageUtils.AB_MESSAGE);
                }
            }
        }
    }
}
