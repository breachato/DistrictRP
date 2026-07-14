package dev.breach.DistrictRP.commands.staff;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.tables.StaffModeTable;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class StaffModeManager implements Listener {

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

    private StaffModeTable table;
    private boolean useDb;

    public StaffModeManager(DistrictRP plugin) {
        this.plugin = plugin;
        var dbm = plugin.getDatabaseManager();
        this.table = (dbm != null && dbm.isMariaDb()) ? dbm.getTable("staffmode", StaffModeTable.class) : null;
        this.useDb = (table != null);
        if (useDb) plugin.getLogger().info("[StaffMode] Storage: MariaDB");
        else plugin.getLogger().info("[StaffMode] Storage: YAML");
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
        giveItems(plugin, p);

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

        if (useDb) setActive(uuid, true, null);

        MessageUtils.sendMsg(p, "staffmode.entered");
        if (plugin.getCoreProtectHook() != null)
            plugin.getCoreProtectHook().logCustomAction(p, "staffmode enter");
    }

    public void exit(Player p) {
        UUID uuid = p.getUniqueId();
        if (!inStaffMode.contains(uuid)) return;

        boolean keepVanish = plugin.getConfig().getBoolean("staffmode.exit-keep-vanish", true);
        boolean keepFly = plugin.getConfig().getBoolean("staffmode.exit-keep-fly", true);

        p.getInventory().clear();
        p.getInventory().setArmorContents(null);

        if (savedInventory.containsKey(uuid)) p.getInventory().setContents(savedInventory.remove(uuid));
        if (savedArmor.containsKey(uuid)) p.getInventory().setArmorContents(savedArmor.remove(uuid));
        if (savedGameMode.containsKey(uuid)) p.setGameMode(savedGameMode.remove(uuid));
        if (savedEffects.containsKey(uuid)) {
            for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
            for (PotionEffect e : savedEffects.remove(uuid)) p.addPotionEffect(e);
        }

        Boolean savedAllow = savedAllowFlight.remove(uuid);
        Boolean savedFly = savedFlying.remove(uuid);
        if (keepFly) {
            p.setAllowFlight(true);
            p.setFlying(true);
        } else {
            if (savedAllow != null) p.setAllowFlight(savedAllow);
            if (savedFly != null) p.setFlying(savedFly);
        }

        if (plugin.getConfig().getBoolean("staffmode.restore-location-on-exit", false)) {
            Location loc = savedLocation.get(uuid);
            if (loc != null) p.teleport(loc);
        }
        savedLocation.remove(uuid);

        invisibilityToggled.remove(uuid);
        inStaffMode.remove(uuid);

        if (!keepVanish
                && plugin.getVanishManager() != null
                && plugin.getVanishManager().isVanished(p)) {
            plugin.getVanishManager().disable(p);
        }

        if (plugin.godMode != null) plugin.godMode.remove(uuid);

        if (useDb) setActive(uuid, false, null);

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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!useDb) return;
        if (!plugin.getConfig().getBoolean("staffmode.persist-across-restart", true)) return;
        Player p = event.getPlayer();
        UUID uuid = p.getUniqueId();
        long delay = plugin.getConfig().getLong("staffmode.persist-restore-delay-ticks", 25L);
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            try {
                boolean active = isActive(uuid).join();
                if (!active) return;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!p.isOnline()) return;
                    if (isInStaffMode(p)) return;
                    enter(p);
                    MessageUtils.sendMsg(p, "staffmode.persisted-restored");
                });
            } catch (Throwable ignored) {}
        }, delay);
    }

    public boolean isUsingDatabase() { return useDb; }

    public CompletableFuture<Boolean> setActive(UUID uuid, boolean active, String snapshotJson) {
        if (table == null) return CompletableFuture.completedFuture(false);
        return table.setActive(uuid, active, snapshotJson);
    }

    public CompletableFuture<String> getSnapshot(UUID uuid) {
        if (table == null) return CompletableFuture.completedFuture(null);
        return table.getSnapshot(uuid);
    }

    public CompletableFuture<Boolean> isActive(UUID uuid) {
        if (table == null) return CompletableFuture.completedFuture(false);
        return table.isActive(uuid);
    }

    public static final String KEY_NAME = "staffmode_tool";
    public static final String KEY_SCALE = "staffmode_scale_value";

    public enum Tool {
        TP_RANDOM("tp-random"),
        VANISH("vanish"),
        SCALE_SMALL("scale-small"),
        SCALE_NORMAL("scale-normal"),
        PLAYER_LIST("player-list"),
        STAFF_LIST("staff-list"),
        EXIT("exit");

        private final String id;
        Tool(String id) { this.id = id; }
        public String getId() { return id; }
        public static Tool fromId(String id) {
            if (id == null) return null;
            for (Tool t : values()) if (t.id.equals(id)) return t;
            return null;
        }
    }

    private static NamespacedKey key(DistrictRP plugin) {
        return new NamespacedKey(plugin, KEY_NAME);
    }

    private static NamespacedKey keyScale(DistrictRP plugin) {
        return new NamespacedKey(plugin, KEY_SCALE);
    }

    public static void giveItems(DistrictRP plugin, Player p) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("staffmode.items");
        if (sec == null) {
            plugin.getLogger().warning("[StaffMode] Sezione staffmode.items mancante nel config!");
            return;
        }
        NamespacedKey k = key(plugin);
        NamespacedKey ks = keyScale(plugin);

        p.getInventory().clear();

        for (Tool tool : Tool.values()) {
            ConfigurationSection item = sec.getConfigurationSection(tool.id);
            if (item == null) continue;
            if (!item.getBoolean("enabled", true)) continue;

            int slot = item.getInt("slot", 0);
            String matName = item.getString("material", "STONE");
            Material mat;
            try {
                mat = Material.valueOf(matName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[StaffMode] Material invalido '" + matName + "' per " + tool.id + ", uso STONE.");
                mat = Material.STONE;
            }

            ItemStack stack = new ItemStack(mat);
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) continue;

            meta.setDisplayName(MessageUtils.color(item.getString("name", tool.name())));

            List<String> lore = new ArrayList<>();
            for (String l : item.getStringList("lore")) lore.add(MessageUtils.color(l));
            meta.setLore(lore);

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.setUnbreakable(true);

            meta.getPersistentDataContainer().set(k, PersistentDataType.STRING, tool.id);

            if (tool == Tool.SCALE_SMALL || tool == Tool.SCALE_NORMAL) {
                double val = item.getDouble("scale-value", tool == Tool.SCALE_SMALL ? 0.01 : 1.0);
                meta.getPersistentDataContainer().set(ks, PersistentDataType.DOUBLE, val);
            }

            stack.setItemMeta(meta);
            p.getInventory().setItem(slot, stack);
        }
        p.updateInventory();
    }

    public static Tool getTool(DistrictRP plugin, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        if (!item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String id = meta.getPersistentDataContainer().get(key(plugin), PersistentDataType.STRING);
        return Tool.fromId(id);
    }

    public static double getScaleValue(DistrictRP plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 1.0;
        ItemMeta meta = item.getItemMeta();
        Double v = meta.getPersistentDataContainer().get(keyScale(plugin), PersistentDataType.DOUBLE);
        return v == null ? 1.0 : v;
    }

    public static boolean isStaffModeItem(DistrictRP plugin, ItemStack item) {
        return getTool(plugin, item) != null;
    }
}