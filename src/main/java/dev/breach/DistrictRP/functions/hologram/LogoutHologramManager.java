package dev.breach.DistrictRP.functions.hologram;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import dev.breach.DistrictRP.functions.servermode.ServerMode;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LogoutHologramManager implements Listener {

    private final DistrictRP plugin;
    private final Map<UUID, TextDisplay> activeHolograms = new HashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public LogoutHologramManager(DistrictRP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (!plugin.getConfig().getBoolean("logout-hologram.enabled", true)) return;
        if (plugin.getServerModeManager() == null) return;
        if (plugin.getServerModeManager().getCurrent() != ServerMode.ROLEPLAY) return;

        Player p = event.getPlayer();

        if (plugin.getVanishManager() != null && plugin.getVanishManager().isVanished(p)) return;
        if (plugin.getStaffModeManager() != null && plugin.getStaffModeManager().isInStaffMode(p)) return;

        Location loc = p.getLocation().clone().add(0, 2.2, 0);
        String playerName = p.getName();
        String timestamp = dateFormat.format(new Date());

        int duration = plugin.getConfig().getInt("logout-hologram.duration-seconds", 15);

        String line1 = MessageUtils.color(plugin.getConfig().getString(
                        "logout-hologram.line-1", "&c✖ %player% ꜱɪ è ᴅɪꜱᴄᴏɴɴᴇꜱꜱᴏ")
                .replace("%player%", playerName));
        String line2 = MessageUtils.color(plugin.getConfig().getString(
                        "logout-hologram.line-2", "&7%date%")
                .replace("%date%", timestamp));

        String fullText = line1 + "\n" + line2;

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (loc.getWorld() == null) return;

            TextDisplay existing = activeHolograms.remove(p.getUniqueId());
            if (existing != null && !existing.isDead()) existing.remove();

            TextDisplay display = (TextDisplay) loc.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
            display.setText(fullText);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(false);
            display.setShadowed(true);
            display.setDefaultBackground(false);

            org.bukkit.Color bgColor = org.bukkit.Color.fromARGB(
                    plugin.getConfig().getInt("logout-hologram.background-alpha", 128),
                    plugin.getConfig().getInt("logout-hologram.background-red", 0),
                    plugin.getConfig().getInt("logout-hologram.background-green", 0),
                    plugin.getConfig().getInt("logout-hologram.background-blue", 0));
            display.setBackgroundColor(bgColor);

            activeHolograms.put(p.getUniqueId(), display);

            new BukkitRunnable() {
                @Override
                public void run() {
                    TextDisplay holo = activeHolograms.remove(p.getUniqueId());
                    if (holo != null && !holo.isDead()) holo.remove();
                }
            }.runTaskLater(plugin, duration * 20L);
        });
    }

    public void cleanupAll() {
        for (TextDisplay display : activeHolograms.values()) {
            if (display != null && !display.isDead()) display.remove();
        }
        activeHolograms.clear();
    }
}