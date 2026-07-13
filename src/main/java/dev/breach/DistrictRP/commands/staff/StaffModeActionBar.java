package dev.breach.DistrictRP.commands.staff;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public class StaffModeActionBar {

    private final DistrictRP plugin;
    private final StaffModeManager manager;
    private BukkitTask task;

    public StaffModeActionBar(DistrictRP plugin, StaffModeManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("staffmode.actionbar.enabled", true)) return;
        long interval = plugin.getConfig().getLong("staffmode.actionbar.refresh-ticks", 20L);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, interval, interval);
    }

    public void stop() {
        if (task != null) task.cancel();
    }

    private void tick() {
        for (UUID uuid : manager.getAllInStaffMode()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            sendActionBar(p);
        }
    }

    public void sendActionBar(Player p) {
        boolean vanished = plugin.getVanishManager() != null && plugin.getVanishManager().isVanished(p);
        String vanishOn = plugin.getConfig().getString("staffmode.actionbar.vanish-on", "&a(✔)");
        String vanishOff = plugin.getConfig().getString("staffmode.actionbar.vanish-off", "&c(✗)");
        String vanishState = MessageUtils.color(vanished ? vanishOn : vanishOff);
        String tps = String.format("%.0f", Math.min(20.0, getTps()));

        String headerTemplate = plugin.getConfig().getString("staffmode.actionbar.header",
                "&aVANISHED %vanish_state% &fTPS: &a%tps%");
        String header = MessageUtils.color(headerTemplate
                .replace("%vanish_state%", vanishState)
                .replace("%tps%", tps));

        String sub = MessageUtils.color(getSubline(p));
        String finalMsg = header + (sub.isEmpty() ? "" : "\n" + sub);

        p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(finalMsg));
    }

    private double getTps() {
        try {
            Object server = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
            double[] tps = (double[]) server.getClass().getField("recentTps").get(server);
            return tps[0];
        } catch (Throwable t) {
            return 20.0;
        }
    }

    private String getSubline(Player p) {
        ItemStack held = p.getInventory().getItemInMainHand();
        StaffModeItems.Tool tool = StaffModeItems.getTool(plugin, held);
        String path;
        if (tool == null) {
            path = "staffmode.actionbar.subline-none";
        } else {
            path = switch (tool) {
                case TP_RANDOM -> "staffmode.actionbar.subline-tp-random";
                case VANISH -> "staffmode.actionbar.subline-vanish";
                case PLAYER_LIST -> "staffmode.actionbar.subline-player-list";
                case STAFF_LIST -> "staffmode.actionbar.subline-staff-list";
                case EXIT -> "staffmode.actionbar.subline-exit";
                default -> "staffmode.actionbar.subline-none";
            };
        }
        return plugin.getConfig().getString(path, "");
    }
}