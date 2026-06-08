package dev.breach.cherryCore.commands.elevators;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ElevatorGUI implements Listener {

    // ============================================================
    // Titoli GUI (diversi da CherryCore!)
    // ============================================================
    public static final String MAIN_TITLE = ChatColor.translateAlternateColorCodes('&',
            "&8&l  ⬆ Ascensori ⬇");
    public static final String FLOOR_TITLE = ChatColor.translateAlternateColorCodes('&',
            "&8&l  ⬆ Piano Info ⬇");

    // Link selection per /el link
    public static final Map<UUID, String[]> linkSel = new HashMap<>();

    private final CherryCore plugin;

    public ElevatorGUI(CherryCore plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // ============================================================
    // GUI principale ascensori
    // ============================================================
    public static void openMain(Player p) {
        CherryCore plugin = CherryCore.get();
        Inventory inv = Bukkit.createInventory(null, 54, MAIN_TITLE);

        // Bordo scuro
        ItemStack border = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, border);

        // Tipi di ascensore disponibili
        inv.setItem(10, elevatorTypeItem("classic", Material.IRON_BLOCK,
                "&f&lClassic",
                "&7Ascensore standard.",
                "&7Cooldown: &f2s"));

        inv.setItem(12, elevatorTypeItem("express", Material.GOLD_BLOCK,
                "&6&lExpress",
                "&7Nessun cooldown!",
                "&7Velocità istantanea."));

        inv.setItem(14, elevatorTypeItem("vip", Material.DIAMOND_BLOCK,
                "&b&lVIP",
                "&7Ascensore esclusivo.",
                "&7Cooldown: &f1s"));

        inv.setItem(16, elevatorTypeItem("freight", Material.NETHERITE_BLOCK,
                "&8&lFreight",
                "&7Ascensore cargo.",
                "&7Cooldown: &f3s"));

        inv.setItem(28, elevatorTypeItem("glass", Material.GLASS,
                "&a&lGlass",
                "&7Ascensore trasparente.",
                "&7Cooldown: &f2s"));

        // Info
        inv.setItem(31, item(Material.BOOK, "&e&lHelp",
                "&7Usa &f/el help &7per i comandi.",
                "&7Piazza un blocco ascensore",
                "&7e salta/sneaka per usarlo."));

        // Chiudi
        inv.setItem(49, item(Material.BARRIER, "&c&l✖ Chiudi"));

        p.openInventory(inv);
    }

    // ============================================================
    // GUI pannello piano (info ascensore specifico)
    // ============================================================
    public static void openFloorPanel(Player p, Location loc) {
        CherryCore plugin = CherryCore.get();
        String id = ElevatorManager.locToId(loc);

        if (!plugin.getElevators().exists(loc)) {
            MessageUtils.send(p, "&cNessun ascensore trovato.");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, FLOOR_TITLE);

        ItemStack border = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        String fname = plugin.getElevators().getFloorName(id);
        String etype = plugin.getElevators().getEtype(id);
        String group = plugin.getElevators().getGroup(id);
        int cd = plugin.getElevators().getCooldown(id);
        List<String> links = plugin.getElevators().getLinks(id);

        inv.setItem(4, item(Material.NAME_TAG, "&d&l" + fname,
                "&7Tipo: &f" + etype,
                "&7Gruppo: &f" + group,
                "&7Cooldown: &f" + cd + "s",
                "&7Link: &f" + links.size(),
                "",
                "&7Posizione:",
                "&f" + loc.getWorld().getName() + " "
                        + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()));

        inv.setItem(11, item(Material.OAK_SIGN, "&e&lRinomina",
                "&7Usa &f/el setname <nome>"));

        inv.setItem(13, item(Material.CHAIN, "&a&lCollega",
                "&7Usa &f/el link &7su due ascensori."));

        inv.setItem(15, item(Material.CLOCK, "&6&lCooldown",
                "&7Usa &f/el setcooldown <sec>"));

        inv.setItem(22, item(Material.BARRIER, "&c&l✖ Chiudi"));

        p.openInventory(inv);
    }

    // ============================================================
    // Click Handler
    // ============================================================
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();

        if (!title.equals(MAIN_TITLE) && !title.equals(FLOOR_TITLE)) return;

        // Blocca SEMPRE il prelievo
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        // === MAIN GUI ===
        if (title.equals(MAIN_TITLE)) {
            switch (e.getRawSlot()) {
                case 10 -> { // Classic
                    p.closeInventory();
                    giveElevator(p, "classic");
                }
                case 12 -> { // Express
                    p.closeInventory();
                    giveElevator(p, "express");
                }
                case 14 -> { // VIP
                    p.closeInventory();
                    giveElevator(p, "vip");
                }
                case 16 -> { // Freight
                    p.closeInventory();
                    giveElevator(p, "freight");
                }
                case 28 -> { // Glass
                    p.closeInventory();
                    giveElevator(p, "glass");
                }
                case 49 -> p.closeInventory();
            }
        }

        // === FLOOR PANEL ===
        if (title.equals(FLOOR_TITLE)) {
            if (e.getRawSlot() == 22) p.closeInventory();
        }
    }

    // ============================================================
    // Give elevator item
    // ============================================================
    private void giveElevator(Player p, String type) {
        if (!p.hasPermission("elevators.give")) {
            MessageUtils.send(p, "&cNon hai il permesso di ottenere ascensori!");
            return;
        }
        p.getInventory().addItem(getElevatorItem(type));
        MessageUtils.sendPrefixed(p, "&aHai ricevuto un ascensore &f(" + type + ")&a!");
    }

    // ============================================================
    // Elevator item per tipo
    // ============================================================
    public static ItemStack getElevatorItem(String type) {
        Material mat;
        String name;
        String loreType;

        switch (type.toLowerCase()) {
            case "express" -> {
                mat = Material.GOLD_BLOCK;
                name = "&6&lAscensore Express";
                loreType = "Express";
            }
            case "vip" -> {
                mat = Material.DIAMOND_BLOCK;
                name = "&b&lAscensore VIP";
                loreType = "VIP";
            }
            case "freight" -> {
                mat = Material.NETHERITE_BLOCK;
                name = "&8&lAscensore Freight";
                loreType = "Freight";
            }
            case "glass" -> {
                mat = Material.GLASS;
                name = "&a&lAscensore Glass";
                loreType = "Glass";
            }
            default -> {
                mat = Material.IRON_BLOCK;
                name = "&f&lAscensore Classic";
                loreType = "Classic";
            }
        }

        return item(mat, name,
                "&7Piazza questo blocco per",
                "&7creare un ascensore.",
                "",
                "&7Tipo: " + loreType,
                "",
                "&dSalta &7per salire",
                "&dSneaka &7per scendere");
    }

    // ============================================================
    // Item builder per tipo ascensore (GUI)
    // ============================================================
    private static ItemStack elevatorTypeItem(String type, Material mat, String name, String... lore) {
        List<String> fullLore = new ArrayList<>();
        for (String s : lore) fullLore.add(ChatColor.translateAlternateColorCodes('&', s));
        fullLore.add("");
        fullLore.add(ChatColor.translateAlternateColorCodes('&', "&dClick per ottenere!"));

        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            m.setLore(fullLore);
            it.setItemMeta(m);
        }
        return it;
    }

    // ============================================================
    // Item builder generico
    // ============================================================
    private static ItemStack item(Material mat, String name, String... lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore.length > 0) {
                List<String> l = new ArrayList<>();
                for (String s : lore) l.add(ChatColor.translateAlternateColorCodes('&', s));
                m.setLore(l);
            }
            it.setItemMeta(m);
        }
        return it;
    }
}