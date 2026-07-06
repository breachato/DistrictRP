package dev.breach.DistrictRP.commands.roleplay.vanish;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class VanishTabHandler implements Listener {

    public static final String STAFF_VIEW_PERMISSION = "DistrictRP.vanish.see";

    private final DistrictRP plugin;
    private BukkitTask suffixTask;
    private BukkitTask actionBarTask;

    public VanishTabHandler(DistrictRP plugin) {
        this.plugin = plugin;
    }

    public void start() {
        suffixTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAll, 40L, 20L);
        long refresh = plugin.getConfig().getLong("vanish.actionbar-refresh-ticks", 20L);
        actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickActionBars, refresh, refresh);
    }

    public void stop() {
        if (suffixTask != null) suffixTask.cancel();
        if (actionBarTask != null) actionBarTask.cancel();
    }

    private void refreshAll() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                applySuffix(viewer, target);
            }
        }
    }

    public void refreshFor(Player viewer) {
        for (Player target : Bukkit.getOnlinePlayers()) {
            applySuffix(viewer, target);
        }
    }

    public void refreshTarget(Player target) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            applySuffix(viewer, target);
        }
    }

    private void applySuffix(Player viewer, Player target) {
        try {
            boolean isVanished = plugin.getVanishManager() != null
                    && plugin.getVanishManager().isVanished(target.getUniqueId());
            boolean canSeeVanish = viewer.hasPermission(STAFF_VIEW_PERMISSION);

            Scoreboard sb = viewer.getScoreboard();
            if (sb == null || sb == Bukkit.getScoreboardManager().getMainScoreboard()) {
                sb = Bukkit.getScoreboardManager().getMainScoreboard();
            }

            String teamName = "drp_" + target.getName();
            if (teamName.length() > 16) teamName = teamName.substring(0, 16);

            Team team = sb.getTeam(teamName);
            if (team == null) {
                team = sb.registerNewTeam(teamName);
            }

            if (!team.hasEntry(target.getName())) {
                team.addEntry(target.getName());
            }

            String suffixRaw = plugin.getConfig().getString("vanish.tab-suffix", " &7[ &cVANISH &7]");
            String suffix = (isVanished && canSeeVanish) ? MessageUtils.color(suffixRaw) : "";

            if (suffix.length() > 40) suffix = suffix.substring(0, 40);

            if (!team.getSuffix().equals(suffix)) {
                team.setSuffix(suffix);
            }
        } catch (Throwable ignored) {}
    }

    private void tickActionBars() {
        boolean enabled = plugin.getConfig().getBoolean("vanish.actionbar-enabled", true);
        if (!enabled) return;

        String vanishMsg = plugin.getConfig().getString("vanish.actionbar-vanished", "&#FCD05Cᴠᴀɴɪꜱʜ ᴀᴛᴛɪᴠᴏ");
        String flyMsg = plugin.getConfig().getString("vanish.actionbar-flying", "&#FCD05Cᴠᴏʟᴏ ᴀᴛᴛɪᴠᴏ");
        String vanishFlyMsg = plugin.getConfig().getString("vanish.actionbar-vanish-fly", "&#FCD05Cᴠᴀɴɪꜱʜ &7+ &#FCD05Cᴠᴏʟᴏ");

        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean vanished = plugin.getVanishManager() != null && plugin.getVanishManager().isVanished(p);
            boolean flying = p.isFlying();

            String msg = null;
            if (vanished && flying) msg = vanishFlyMsg;
            else if (vanished) msg = vanishMsg;
            else if (flying && p.hasPermission("DistrictRP.fly")) msg = flyMsg;

            if (msg == null || msg.isEmpty()) continue;

            p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(MessageUtils.color(msg)));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, this::refreshAll, 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
                String teamName = "drp_" + event.getPlayer().getName();
                if (teamName.length() > 16) teamName = teamName.substring(0, 16);
                Team team = sb.getTeam(teamName);
                if (team != null) team.unregister();
            } catch (Throwable ignored) {}
        }, 5L);
    }
}