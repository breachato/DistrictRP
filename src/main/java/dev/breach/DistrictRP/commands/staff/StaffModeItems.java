package dev.breach.DistrictRP.commands.staff;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class StaffModeItems {

    public static final String KEY = "staffmode_tool";

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
            for (Tool t : values()) if (t.id.equals(id)) return t;
            return null;
        }
    }

    public static void giveItems(DistrictRP plugin, Player p) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("staffmode.items");
        if (sec == null) return;
        NamespacedKey key = new NamespacedKey(plugin, KEY);

        for (Tool tool : Tool.values()) {
            ConfigurationSection item = sec.getConfigurationSection(tool.id);
            if (item == null) continue;
            int slot = item.getInt("slot", 0);
            String matName = item.getString("material", "STONE");
            Material mat;
            try { mat = Material.valueOf(matName.toUpperCase()); }
            catch (IllegalArgumentException e) { mat = Material.STONE; }
            ItemStack stack = new ItemStack(mat);
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName(MessageUtils.color(item.getString("name", tool.name())));
            List<String> lore = new ArrayList<>();
            for (String l : item.getStringList("lore")) lore.add(MessageUtils.color(l));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, tool.id);
            stack.setItemMeta(meta);
            p.getInventory().setItem(slot, stack);
        }
    }

    public static Tool getTool(DistrictRP plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        NamespacedKey key = new NamespacedKey(plugin, KEY);
        String id = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (id == null) return null;
        return Tool.fromId(id);
    }
}