package dev.breach.DistrictRP.functions;

import dev.breach.DistrictRP.DistrictRP;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VanishManager {

    public static final String VANISH_SUFFIX = ChatColor.translateAlternateColorCodes('&', " &7[ &cVANISH &7]");
    public static final String SEE_PERMISSION = "DistrictRP.vanish.see";

    private final DistrictRP plugin;
    private final Set<UUID> vanished = new HashSet<>();
    private BukkitTask equipmentHideTask;

    public VanishManager(DistrictRP plugin) {
        this.plugin = plugin;

        for (String s : plugin.getDataManager().getAllVanished()) {
            try {
                vanished.add(UUID.fromString(s));
            } catch (Exception ignored) {}
        }

        startEquipmentHideTask();
    }

    private void startEquipmentHideTask() {
        equipmentHideTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : vanished) {
                Player vanishedPlayer = Bukkit.getPlayer(uuid);
                if (vanishedPlayer == null) continue;
                hideEquipmentFromNonStaff(vanishedPlayer);
            }
        }, 20L, 20L);
    }

    private void hideEquipmentFromNonStaff(Player vanishedPlayer) {
        ItemStack air = new ItemStack(org.bukkit.Material.AIR);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(vanishedPlayer)) continue;
            if (viewer.hasPermission(SEE_PERMISSION)) continue;
            try {
                viewer.sendEquipmentChange(vanishedPlayer, EquipmentSlot.HAND, air);
                viewer.sendEquipmentChange(vanishedPlayer, EquipmentSlot.OFF_HAND, air);
                viewer.sendEquipmentChange(vanishedPlayer, EquipmentSlot.HEAD, air);
                viewer.sendEquipmentChange(vanishedPlayer, EquipmentSlot.CHEST, air);
                viewer.sendEquipmentChange(vanishedPlayer, EquipmentSlot.LEGS, air);
                viewer.sendEquipmentChange(vanishedPlayer, EquipmentSlot.FEET, air);
            } catch (Throwable ignored) {}
        }
    }

    private void restoreEquipmentForAll(Player vanishedPlayer) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(vanishedPlayer)) continue;
            try {
                viewer.sendEquipmentChange(vanishedPlayer, EquipmentSlot.HAND,
                        vanishedPlayer.getInventory().getItemInMainHand());
                viewer.sendEquipmentChange(vanishedPlayer, EquipmentSlot.OFF_HAND,
                        vanishedPlayer.getInventory().getItemInOffHand());
                viewer.sendEquipmentChange(vanishedPlayer, EquipmentSlot.HEAD,
                        vanishedPlayer.getInventory().getHelmet() != null
                                ? vanishedPlayer.getInventory().getHelmet()
                                : new ItemStack(org.bukkit.Material.AIR));
                viewer.sendEquipmentChange(vanishedPlayer, EquipmentSlot.CHEST,
                        vanishedPlayer.getInventory().getChestplate() != null
                                ? vanishedPlayer.getInventory().getChestplate()
                                : new ItemStack(org.bukkit.Material.AIR));
                viewer.sendEquipmentChange(vanishedPlayer, EquipmentSlot.LEGS,
                        vanishedPlayer.getInventory().getLeggings() != null
                                ? vanishedPlayer.getInventory().getLeggings()
                                : new ItemStack(org.bukkit.Material.AIR));
                viewer.sendEquipmentChange(vanishedPlayer, EquipmentSlot.FEET,
                        vanishedPlayer.getInventory().getBoots() != null
                                ? vanishedPlayer.getInventory().getBoots()
                                : new ItemStack(org.bukkit.Material.AIR));
            } catch (Throwable ignored) {}
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
        refreshTabSuffix(p);
        hideEquipmentFromNonStaff(p);

        MessageUtils.sendPrefixed(p, "&7Sei ora in &fVanish&#FCD05C.");
    }

    public void disable(Player p) {
        vanished.remove(p.getUniqueId());
        plugin.getDataManager().setVanished(p.getUniqueId(), false);

        removeVanish(p);
        refreshTabSuffix(p);
        restoreEquipmentForAll(p);

        MessageUtils.sendPrefixed(p, "&7Non sei più in &fVanish&c.");
    }

    public void toggle(Player p) {
        if (isVanished(p)) disable(p);
        else enable(p);
    }

    public void applyVanish(Player p) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(p)) continue;

            if (other.hasPermission(SEE_PERMISSION)) {
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

    public void removeVanish(Player p) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(p)) continue;
            other.showPlayer(plugin, p);
        }

        p.removePotionEffect(PotionEffectType.NIGHT_VISION);
        p.setCollidable(true);
    }

    public void refreshTabSuffix(Player target) {
        if (plugin.getRoleplay() != null && plugin.getRoleplay().getVanishTabHandler() != null) {
            plugin.getRoleplay().getVanishTabHandler().refreshTarget(target);
        }
    }

    public void refreshAllTabSuffixes() {
        if (plugin.getRoleplay() != null && plugin.getRoleplay().getVanishTabHandler() != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                plugin.getRoleplay().getVanishTabHandler().refreshTarget(p);
            }
        }
    }

    public void refreshFor(Player joiner) {
        if (!joiner.hasPermission(SEE_PERMISSION)) {
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

        refreshAllTabSuffixes();
    }

    public void cleanupTeam(Player p) {
    }

    public Set<UUID> getVanished() {
        return new HashSet<>(vanished);
    }

    public void shutdown() {
        if (equipmentHideTask != null) equipmentHideTask.cancel();
    }
}