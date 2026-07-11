package dev.breach.DistrictRP.commands.staff;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class StaffModeItems {

    public static final String KEY_NAME = "staffmode_tool";

    public enum Tool {
        TP_RANDOM("tp-random"),
        INVISIBILITY("invisibility"),
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

    public static void giveItems(DistrictRP plugin, Player p) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("staffmode.items");
        if (sec == null) {
            plugin.getLogger().warning("[StaffMode] Sezione staffmode.items mancante nel config!");
            return;
        }
        NamespacedKey k = key(plugin);

        for (Tool tool : Tool.values()) {
            ConfigurationSection item = sec.getConfigurationSection(tool.id);
            if (item == null) {
                plugin.getLogger().warning("[StaffMode] Missing item config: staffmode.items." + tool.id);
                continue;
            }
            int slot = item.getInt("slot", 0);
            String matName = item.getString("material", "STONE");
            Material mat;
            try {
                mat = Material.valueOf(matName.toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("[StaffMode] Material invalido '" + matName + "' per " + tool.id + ", uso STONE.");
                mat = Material.STONE;
            }

            ItemStack stack = new ItemStack(mat);
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) {
                plugin.getLogger().warning("[StaffMode] Meta null per " + tool.id);
                continue;
            }

            meta.setDisplayName(MessageUtils.color(item.getString("name", tool.name())));

            List<String> lore = new ArrayList<>();
            for (String l : item.getStringList("lore")) lore.add(MessageUtils.color(l));
            meta.setLore(lore);

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.setUnbreakable(true);

            meta.getPersistentDataContainer().set(k, PersistentDataType.STRING, tool.id);
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

    public static boolean isStaffModeItem(DistrictRP plugin, ItemStack item) {
        return getTool(plugin, item) != null;
    }
}