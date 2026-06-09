package dev.breach.cherryCore.functions;

import dev.breach.cherryCore.CherryCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VanishManager {

    private final CherryCore plugin;
    private final Set<UUID> vanished = new HashSet<>();

    public VanishManager(CherryCore plugin) {
        this.plugin = plugin;

        for (String s : plugin.getDataManager().getAllVanished()) {
            try {
                vanished.add(UUID.fromString(s));
            } catch (Exception ignored) {}
        }
    }

    // ============================================================
    // CHECK
    // ============================================================
    public boolean isVanished(Player p) {
        return vanished.contains(p.getUniqueId());
    }

    public boolean isVanished(UUID uuid) {
        return vanished.contains(uuid);
    }

    // ============================================================
    // ENABLE
    // ============================================================
    public void enable(Player p) {
        vanished.add(p.getUniqueId());
        plugin.getDataManager().setVanished(p.getUniqueId(), true);

        applyVanish(p);
        setVanishTag(p, true);

        MessageUtils.sendPrefixed(p, "&7Sei ora in &fVanish&a.");
    }

    // ============================================================
    // DISABLE
    // ============================================================
    public void disable(Player p) {
        vanished.remove(p.getUniqueId());
        plugin.getDataManager().setVanished(p.getUniqueId(), false);

        removeVanish(p);
        setVanishTag(p, false);

        MessageUtils.sendPrefixed(p, "&7Non sei più in &fVanish&c.");
    }

    public void toggle(Player p) {
        if (isVanished(p)) disable(p);
        else enable(p);
    }

    // ============================================================
    // VANISH APPLY
    // ============================================================
    public void applyVanish(Player p) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(p)) continue;

            if (other.hasPermission("cherrycore.vanish.see")) {
                other.showPlayer(plugin, p);
            } else {
                other.hidePlayer(plugin, p);
            }
        }

        p.addPotionEffect(new PotionEffect(
                PotionEffectType.NIGHT_VISION,
                Integer.MAX_VALUE, 0, false, false, false
        ));

        p.setAllowFlight(true);
        p.setFlying(true);
        p.setCollidable(false);
    }

    // ============================================================
    // REMOVE VANISH
    // ============================================================
    public void removeVanish(Player p) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(p)) continue;
            other.showPlayer(plugin, p);
        }

        p.removePotionEffect(PotionEffectType.NIGHT_VISION);
        p.setCollidable(true);
    }

    // ============================================================
    // TAB TAG VANISH
    // ============================================================
    private void setVanishTag(Player p, boolean state) {
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();

        Team team = sb.getTeam("vanish");
        if (team == null) {
            team = sb.registerNewTeam("vanish");
            team.setSuffix(ChatColor.translateAlternateColorCodes('&', " &7[VANISH]"));
        }

        if (state) {
            team.addEntry(p.getName());
        } else {
            team.removeEntry(p.getName());
        }
    }

    // ============================================================
    // JOIN REFRESH
    // ============================================================
    public void refreshFor(Player joiner) {
        if (!joiner.hasPermission("cherrycore.vanish.see")) {
            for (UUID uuid : vanished) {
                Player v = Bukkit.getPlayer(uuid);
                if (v != null && !v.equals(joiner)) {
                    joiner.hidePlayer(plugin, v);
                }
            }
        } else {
            for (UUID uuid : vanished) {
                Player v = Bukkit.getPlayer(uuid);
                if (v != null) {
                    joiner.showPlayer(plugin, v);
                }
            }
        }
    }

    public Set<UUID> getVanished() {
        return new HashSet<>(vanished);
    }
}