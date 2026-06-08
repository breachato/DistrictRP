package dev.breach.cherryCore.functions;

import dev.breach.cherryCore.CherryCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VanishManager {

    private final CherryCore plugin;
    private final Set<UUID> vanished = new HashSet<>();

    public VanishManager(CherryCore plugin) {
        this.plugin = plugin;
        // Ricarica da disco
        for (String s : plugin.getDataManager().getAllVanished()) {
            try {
                vanished.add(UUID.fromString(s));
            } catch (Exception ignored) {}
        }
    }

    public boolean isVanished(Player p) {
        return vanished.contains(p.getUniqueId());
    }

    public boolean isVanished(UUID uuid) {
        return vanished.contains(uuid);
    }

    public void enable(Player p) {
        vanished.add(p.getUniqueId());
        plugin.getDataManager().setVanished(p.getUniqueId(), true);
        applyVanish(p);
        MessageUtils.sendPrefixed(p, "&aSei ora in &fVANISH&a.");
    }

    public void disable(Player p) {
        vanished.remove(p.getUniqueId());
        plugin.getDataManager().setVanished(p.getUniqueId(), false);
        removeVanish(p);
        MessageUtils.sendPrefixed(p, "&cNon sei piu in &fVANISH&c.");
    }

    public void toggle(Player p) {
        if (isVanished(p)) disable(p);
        else enable(p);
    }

    /**
     * Nasconde p a tutti i player che NON hanno il permesso vanish.see
     */
    public void applyVanish(Player p) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(p)) continue;
            if (!other.hasPermission("cherrycore.vanish.see")) {
                other.hidePlayer(plugin, p);
            } else {
                other.showPlayer(plugin, p);
            }
        }
        // Effetti utili per il vanish
        p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION,
                Integer.MAX_VALUE, 0, false, false, false));
        p.setAllowFlight(true);
        p.setFlying(true);
        p.setCollidable(false);
    }

    public void removeVanish(Player p) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(p)) continue;
            other.showPlayer(plugin, p);
        }
        p.removePotionEffect(PotionEffectType.NIGHT_VISION);
        p.setCollidable(true);
    }

    /**
     * Da chiamare ogni volta che un player entra:
     * - se il nuovo arrivato NON ha vanish.see, deve smettere di vedere i vanished
     * - se il nuovo arrivato e in vanish, applicalo
     */
    public void refreshFor(Player joiner) {
        if (!joiner.hasPermission("cherrycore.vanish.see")) {
            for (UUID uuid : vanished) {
                Player v = Bukkit.getPlayer(uuid);
                if (v != null && !v.equals(joiner)) {
                    joiner.hidePlayer(plugin, v);
                }
            }
        }
        if (isVanished(joiner)) {
            applyVanish(joiner);
        }
    }

    public Set<UUID> getVanished() {
        return new HashSet<>(vanished);
    }
}