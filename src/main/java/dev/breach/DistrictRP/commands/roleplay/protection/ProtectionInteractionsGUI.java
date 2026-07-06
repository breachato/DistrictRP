package dev.breach.DistrictRP.commands.roleplay.protection;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ProtectionInteractionsGUI implements Listener {

    private final DistrictRP plugin;
    private final ProtectionManager manager;
    private final NamespacedKey matKey;
    private final NamespacedKey worldKey;
    private final NamespacedKey navKey;

    private static final int PAGE_SIZE = 28;

    private static final List<Material> INTERACTABLE_MATERIALS = new ArrayList<>();

    static {
        for (Material m : Material.values()) {
            if (!m.isBlock()) continue;
            String n = m.name();
            if (m.isInteractable()
                    || n.contains("DOOR") || n.contains("GATE") || n.contains("BUTTON")
                    || n.contains("LEVER") || n.contains("TRAPDOOR") || n.contains("CHEST")
                    || n.contains("FURNACE") || n.contains("TABLE") || n.contains("ANVIL")
                    || n.contains("BARREL") || n.contains("SHULKER") || n.contains("HOPPER")
                    || n.contains("DISPENSER") || n.contains("DROPPER") || n.contains("BEACON")
                    || n.contains("BREWING") || n.contains("BED") || n.contains("NOTE_BLOCK")
                    || n.contains("JUKEBOX") || n.contains("BELL") || n.contains("CAMPFIRE")
                    || n.contains("LOOM") || n.contains("GRINDSTONE") || n.contains("STONECUTTER")
                    || n.contains("CARTOGRAPHY") || n.contains("SMITHING") || n.contains("COMPOSTER")
                    || n.contains("LECTERN")) {
                INTERACTABLE_MATERIALS.add(m);
            }
        }
    }

    public ProtectionInteractionsGUI(DistrictRP plugin, ProtectionManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.matKey = new NamespacedKey(plugin, "prot_material");
        this.worldKey = new NamespacedKey(plugin, "prot_world");
        this.navKey = new NamespacedKey(plugin, "prot_nav");
    }

    public void open(Player player, String world, int page) {
        int totalPages = Math.max(1, (int) Math.ceil(INTERACTABLE_MATERIALS.size() / (double) PAGE_SIZE));
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        String title = MessageUtils.get("protection.gui-title", "world", world)
                + " &7(" + (page + 1) + "/" + totalPages + ")";
        Inventory inv = Bukkit.createInventory(null, 54, MessageUtils.color(title));

        Set<Material> allowed = manager.getAllowedInteractions(world);
        int start = page * PAGE_SIZE;
        int[] slots = contentSlots();

        for (int i = start, idx = 0; i < INTERACTABLE_MATERIALS.size() && idx < PAGE_SIZE; i++, idx++) {
            Material mat = INTERACTABLE_MATERIALS.get(i);
            boolean enabled = allowed.contains(mat);

            ItemStack item;
            if (mat.isItem()) {
                item = new ItemStack(mat);
            } else {
                item = new ItemStack(Material.BARRIER);
            }
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(MessageUtils.color("&f" + formatName(mat.name())));
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(MessageUtils.color(enabled
                    ? MessageUtils.get("protection.gui-enabled")
                    : MessageUtils.get("protection.gui-disabled")));
            lore.add("");
            lore.add(MessageUtils.color(MessageUtils.get("protection.gui-click-toggle")));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(matKey, PersistentDataType.STRING, mat.name());
            meta.getPersistentDataContainer().set(worldKey, PersistentDataType.STRING, world);
            item.setItemMeta(meta);
            inv.setItem(slots[idx], item);
        }

        if (page > 0) {
            inv.setItem(45, navItem(Material.ARROW,
                    "&#FCD05C◀ Pagina precedente", "prev:" + (page - 1) + ":" + world));
        }
        if (page < totalPages - 1) {
            inv.setItem(53, navItem(Material.ARROW,
                    "&#FCD05CPagina successiva ▶", "next:" + (page + 1) + ":" + world));
        }

        player.openInventory(inv);
    }

    private int[] contentSlots() {
        int[] slots = new int[PAGE_SIZE];
        int idx = 0;
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                slots[idx++] = row * 9 + col;
            }
        }
        return slots;
    }

    private ItemStack navItem(Material mat, String name, String data) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtils.color(name));
        meta.getPersistentDataContainer().set(navKey, PersistentDataType.STRING, data);
        item.setItemMeta(meta);
        return item;
    }

    private String formatName(String name) {
        String[] parts = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String rawTitle = event.getView().getTitle();
        String prefix = MessageUtils.color(MessageUtils.get("protection.gui-title", "world", ""));
        if (!rawTitle.startsWith(prefix.substring(0, Math.min(10, prefix.length())))) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();

        String nav = meta.getPersistentDataContainer().get(navKey, PersistentDataType.STRING);
        if (nav != null) {
            String[] parts = nav.split(":", 3);
            int page = Integer.parseInt(parts[1]);
            String world = parts[2];
            open(player, world, page);
            return;
        }

        String matName = meta.getPersistentDataContainer().get(matKey, PersistentDataType.STRING);
        String world = meta.getPersistentDataContainer().get(worldKey, PersistentDataType.STRING);
        if (matName == null || world == null) return;

        Set<Material> allowed = manager.getAllowedInteractions(world);
        Material mat = Material.valueOf(matName);

        if (allowed.contains(mat)) {
            manager.removeInteraction(world, matName);
        } else {
            manager.addInteraction(world, matName);
        }

        String pageStr = rawTitle.replaceAll(".*\\(", "").replaceAll("/.*", "").trim();
        int currentPage = 0;
        try { currentPage = Integer.parseInt(pageStr) - 1; } catch (NumberFormatException ignored) {}
        open(player, world, currentPage);
    }
}