package dev.breach.DistrictRP.commands.staff;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class StaffModeManager {

    private final DistrictRP plugin;
    private final Set<UUID> inStaffMode = new HashSet<>();
    private final Map<UUID, ItemStack[]> savedInventory = new HashMap<>();
    private final Map<UUID, ItemStack[]> savedArmor = new HashMap<>();
    private final Map<UUID, GameMode> savedGameMode = new HashMap<>();
    private final Map<UUID, Location> savedLocation = new HashMap<>();
    private final Map<UUID, Collection<PotionEffect>> savedEffects = new HashMap<>();
    private final Map<UUID, Boolean> savedFlying = new HashMap<>();
    private final Map<UUID, Boolean> savedAllowFlight = new HashMap<>();
    private final Set<UUID> invisibilityToggled = new HashSet<>();

    public StaffModeManager(DistrictRP plugin) {
        this.plugin = plugin;
    }

    public boolean isInStaffMode(UUID uuid) { return inStaffMode.contains(uuid); }
    public boolean isInStaffMode(Player p) { return inStaffMode.contains(p.getUniqueId()); }
    public Set<UUID> getAllInStaffMode() { return new HashSet<>(inStaffMode); }
    public boolean isInvisibilityOn(UUID uuid) { return invisibilityToggled.contains(uuid); }

    public void enter(Player p) {
        UUID uuid = p.getUniqueId();
        boolean saveInv = plugin.getConfig().getBoolean("staffmode.save-inventory", true);

        if (saveInv) {
            savedInventory.put(uuid, p.getInventory().getContents().clone());
            savedArmor.put(uuid, p.getInventory().getArmorContents().clone());
        }
        savedGameMode.put(uuid, p.getGameMode());
        savedLocation.put(uuid, p.getLocation().clone());
        savedEffects.put(uuid, p.getActivePotionEffects());
        savedFlying.put(uuid, p.isFlying());
        savedAllowFlight.put(uuid, p.getAllowFlight());

        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        p.setGameMode(GameMode.CREATIVE);

        for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());

        inStaffMode.add(uuid);
        StaffModeItems.giveItems(plugin, p);

        if (plugin.getConfig().getBoolean("staffmode.auto-vanish", true)
                && plugin.getVanishManager() != null
                && !plugin.getVanishManager().isVanished(p)) {
            plugin.getVanishManager().enable(p);
        }

        if (plugin.getConfig().getBoolean("staffmode.auto-fly", true)) {
            p.setAllowFlight(true);
            p.setFlying(true);
        }

        if (plugin.getConfig().getBoolean("staffmode.auto-godmode", true)) {
            if (plugin.godMode != null) plugin.godMode.put(uuid, true);
        }

        if (plugin.getConfig().getBoolean("staffmode.auto-invisibility", false)) {
            toggleInvisibility(p);
        }

        MessageUtils.sendMsg(p, "staffmode.entered");
        if (plugin.getCoreProtectHook() != null)
            plugin.getCoreProtectHook().logCustomAction(p, "staffmode enter");
    }

    public void exit(Player p) {
        UUID uuid = p.getUniqueId();
        if (!inStaffMode.contains(uuid)) return;

        p.getInventory().clear();
        p.getInventory().setArmorContents(null);

        if (savedInventory.containsKey(uuid)) p.getInventory().setContents(savedInventory.remove(uuid));
        if (savedArmor.containsKey(uuid)) p.getInventory().setArmorContents(savedArmor.remove(uuid));
        if (savedGameMode.containsKey(uuid)) p.setGameMode(savedGameMode.remove(uuid));
        if (savedEffects.containsKey(uuid)) {
            for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
            for (PotionEffect e : savedEffects.remove(uuid)) p.addPotionEffect(e);
        }
        if (savedAllowFlight.containsKey(uuid)) p.setAllowFlight(savedAllowFlight.remove(uuid));
        if (savedFlying.containsKey(uuid)) p.setFlying(savedFlying.remove(uuid));

        if (plugin.getConfig().getBoolean("staffmode.restore-location-on-exit", false)) {
            Location loc = savedLocation.get(uuid);
            if (loc != null) p.teleport(loc);
        }
        savedLocation.remove(uuid);

        invisibilityToggled.remove(uuid);
        inStaffMode.remove(uuid);

        if (plugin.getVanishManager() != null && plugin.getVanishManager().isVanished(p)) {
            plugin.getVanishManager().disable(p);
        }

        if (plugin.godMode != null) plugin.godMode.remove(uuid);

        MessageUtils.sendMsg(p, "staffmode.exited");
        if (plugin.getCoreProtectHook() != null)
            plugin.getCoreProtectHook().logCustomAction(p, "staffmode exit");
    }

    public void toggle(Player p) {
        if (isInStaffMode(p)) exit(p);
        else enter(p);
    }

    public void toggleInvisibility(Player p) {
        UUID uuid = p.getUniqueId();
        if (invisibilityToggled.contains(uuid)) {
            invisibilityToggled.remove(uuid);
            if (plugin.getVanishManager() != null && plugin.getVanishManager().isVanished(p)) {
                if (!plugin.getConfig().getBoolean("staffmode.auto-vanish", true)) {
                    plugin.getVanishManager().disable(p);
                }
            }
            MessageUtils.sendMsg(p, "staffmode.invisibility-off");
        } else {
            invisibilityToggled.add(uuid);
            if (plugin.getVanishManager() != null && !plugin.getVanishManager().isVanished(p)) {
                plugin.getVanishManager().enable(p);
            }
            MessageUtils.sendMsg(p, "staffmode.invisibility-on");
        }
    }
}