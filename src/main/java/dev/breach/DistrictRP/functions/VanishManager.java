package dev.breach.DistrictRP.functions;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.tables.VanishTable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VanishManager {

    public static final String SEE_PERMISSION = "DistrictRP.vanish.see";
    public static final String VANISH_SUFFIX = ChatColor.translateAlternateColorCodes('&', " &fᴠᴀɴɪꜱʜ");

    private final DistrictRP plugin;
    private final Set<UUID> vanished = new HashSet<>();
    private BukkitTask equipmentHideTask;

    private VanishTable table;
    private boolean useDb;

    public VanishManager(DistrictRP plugin) {
        this.plugin = plugin;

        var dbm = plugin.getDatabaseManager();
        this.table = (dbm != null && dbm.isMariaDb()) ? dbm.getTable("vanish", VanishTable.class) : null;
        this.useDb = (table != null);

        if (useDb) {
            plugin.getLogger().info("[Vanish] Storage: MariaDB (con cache locale)");
            loadFromDb();
        } else {
            plugin.getLogger().info("[Vanish] Storage: YAML");
            for (String s : plugin.getDataManager().getAllVanished()) {
                try { vanished.add(UUID.fromString(s)); } catch (Exception ignored) {}
            }
        }

        startEquipmentHideTask();
    }

    private void loadFromDb() {
        loadAllVanished().thenAccept(list -> {
            vanished.clear();
            vanished.addAll(list);
            plugin.getLogger().info("[Vanish] Caricati " + vanished.size() + " vanish dal database.");
        }).exceptionally(t -> {
            plugin.getLogger().warning("[Vanish] Errore caricamento DB: " + t.getMessage());
            return null;
        });
    }

    public static String getVanishSuffix() {
        try {
            String raw = DistrictRP.get().getConfig().getString("vanish.tab-suffix", " &fᴠᴀɴɪꜱʜ");
            return ChatColor.translateAlternateColorCodes('&', raw);
        } catch (Throwable t) {
            return VANISH_SUFFIX;
        }
    }

    private void startEquipmentHideTask() {
        equipmentHideTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : vanished) {
                Player vp = Bukkit.getPlayer(uuid);
                if (vp == null) continue;
                hideEquipmentFromNonStaff(vp);
            }
        }, 20L, 20L);
    }

    private void hideEquipmentFromNonStaff(Player vp) {
        ItemStack air = new ItemStack(org.bukkit.Material.AIR);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(vp)) continue;
            if (viewer.hasPermission(SEE_PERMISSION)) continue;
            try {
                viewer.sendEquipmentChange(vp, EquipmentSlot.HAND, air);
                viewer.sendEquipmentChange(vp, EquipmentSlot.OFF_HAND, air);
                viewer.sendEquipmentChange(vp, EquipmentSlot.HEAD, air);
                viewer.sendEquipmentChange(vp, EquipmentSlot.CHEST, air);
                viewer.sendEquipmentChange(vp, EquipmentSlot.LEGS, air);
                viewer.sendEquipmentChange(vp, EquipmentSlot.FEET, air);
            } catch (Throwable ignored) {}
        }
    }

    private void restoreEquipmentForAll(Player vp) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(vp)) continue;
            try {
                viewer.sendEquipmentChange(vp, EquipmentSlot.HAND, vp.getInventory().getItemInMainHand());
                viewer.sendEquipmentChange(vp, EquipmentSlot.OFF_HAND, vp.getInventory().getItemInOffHand());
                viewer.sendEquipmentChange(vp, EquipmentSlot.HEAD, safe(vp.getInventory().getHelmet()));
                viewer.sendEquipmentChange(vp, EquipmentSlot.CHEST, safe(vp.getInventory().getChestplate()));
                viewer.sendEquipmentChange(vp, EquipmentSlot.LEGS, safe(vp.getInventory().getLeggings()));
                viewer.sendEquipmentChange(vp, EquipmentSlot.FEET, safe(vp.getInventory().getBoots()));
            } catch (Throwable ignored) {}
        }
    }

    private ItemStack safe(ItemStack it) {
        return it != null ? it : new ItemStack(org.bukkit.Material.AIR);
    }

    public boolean isVanished(Player p) { return vanished.contains(p.getUniqueId()); }
    public boolean isVanished(UUID uuid) { return vanished.contains(uuid); }

    public void enable(Player p) {
        vanished.add(p.getUniqueId());
        if (useDb) setVanished(p.getUniqueId(), true);
        else plugin.getDataManager().setVanished(p.getUniqueId(), true);
        applyVanish(p);
        refreshTabSuffix(p);
        hideEquipmentFromNonStaff(p);
        MessageUtils.sendMsg(p, "vanish.enabled-self");
        if (plugin.getCoreProtectHook() != null)
            plugin.getCoreProtectHook().logCustomAction(p, "vanish enable");
    }

    public void disable(Player p) {
        vanished.remove(p.getUniqueId());
        if (useDb) setVanished(p.getUniqueId(), false);
        else plugin.getDataManager().setVanished(p.getUniqueId(), false);
        removeVanish(p);
        refreshTabSuffix(p);
        restoreEquipmentForAll(p);
        MessageUtils.sendMsg(p, "vanish.disabled-self");
        if (plugin.getCoreProtectHook() != null)
            plugin.getCoreProtectHook().logCustomAction(p, "vanish disable");
    }

    public void toggle(Player p) {
        if (isVanished(p)) disable(p);
        else enable(p);
    }

    public void applyVanish(Player p) {
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(p)) continue;
            if (other.hasPermission(SEE_PERMISSION)) other.showPlayer(plugin, p);
            else other.hidePlayer(plugin, p);
        }
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

    public void refreshTabSuffix(Player target) {
        if (plugin.getRoleplay() != null && plugin.getRoleplay().getVanishTabHandler() != null) {
            plugin.getRoleplay().getVanishTabHandler().refreshTarget(target);
        }
    }

    public void refreshAllTabSuffixes() {
        if (plugin.getRoleplay() != null && plugin.getRoleplay().getVanishTabHandler() != null) {
            for (Player p : Bukkit.getOnlinePlayers())
                plugin.getRoleplay().getVanishTabHandler().refreshTarget(p);
        }
    }

    public void refreshFor(Player joiner) {
        if (!joiner.hasPermission(SEE_PERMISSION)) {
            for (UUID uuid : vanished) {
                Player v = Bukkit.getPlayer(uuid);
                if (v != null && !v.equals(joiner)) joiner.hidePlayer(plugin, v);
            }
        } else {
            for (UUID uuid : vanished) {
                Player v = Bukkit.getPlayer(uuid);
                if (v != null) joiner.showPlayer(plugin, v);
            }
        }
        refreshAllTabSuffixes();
    }

    public void cleanupTeam(Player p) {}
    public Set<UUID> getVanished() { return new HashSet<>(vanished); }

    public boolean isUsingDatabase() { return useDb; }

    public CompletableFuture<Boolean> setVanished(UUID uuid, boolean v) {
        if (table == null) return CompletableFuture.completedFuture(false);
        return table.set(uuid, v);
    }

    public CompletableFuture<List<UUID>> loadAllVanished() {
        if (table == null) return CompletableFuture.completedFuture(new java.util.ArrayList<>());
        return table.allVanished();
    }

    public void shutdown() {
        if (equipmentHideTask != null) equipmentHideTask.cancel();
    }
}