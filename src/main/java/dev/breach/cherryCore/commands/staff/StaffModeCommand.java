package dev.breach.cherryCore.commands.staff;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class StaffModeCommand implements CommandExecutor {

    private final CherryCore plugin;

    public static final Map<UUID, ItemStack[]> savedInventory = new HashMap<>();
    public static final Map<UUID, ItemStack[]> savedArmor     = new HashMap<>();
    public static final Map<UUID, GameMode>    savedGamemode  = new HashMap<>();
    public static final Map<UUID, Boolean>     savedFlight    = new HashMap<>();

    public StaffModeCommand(CherryCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            MessageUtils.send(sender, "&cSolo i giocatori.");
            return true;
        }
        if (!p.hasPermission("cherrycore.staffmode")) {
            MessageUtils.send(p, "&cNon hai il permesso.");
            return true;
        }

        if (isInStaffMode(p)) {
            exitStaffMode(plugin, p);
        } else {
            enterStaffMode(plugin, p);
        }
        return true;
    }

    public static boolean isInStaffMode(Player p) {
        return savedInventory.containsKey(p.getUniqueId());
    }

    public static void enterStaffMode(CherryCore plugin, Player p) {
        UUID uuid = p.getUniqueId();
        PlayerInventory inv = p.getInventory();

        savedInventory.put(uuid, inv.getContents().clone());
        savedArmor.put(uuid, inv.getArmorContents().clone());
        savedGamemode.put(uuid, p.getGameMode());
        savedFlight.put(uuid, p.getAllowFlight());

        inv.clear();
        inv.setArmorContents(new ItemStack[4]);
        p.setGameMode(GameMode.CREATIVE);
        p.setAllowFlight(true);
        p.setFlying(true);

        if (!plugin.getVanishManager().isVanished(p)) {
            plugin.getVanishManager().enable(p);
        }

        inv.setItem(0, tool(Material.COMPASS,    "&d&l⌖ Teleport Player", "&7Click per teletrasportarti a un player"));
        inv.setItem(1, tool(Material.PLAYER_HEAD,"&d&l▦ Inventario",       "&7Click destro su un player per vedere il suo inventario"));
        inv.setItem(2, tool(Material.BOOK,       "&d&l📖 Info Player",     "&7Click destro su un player per info"));
        inv.setItem(4, tool(Material.ENDER_PEARL,"&d&l↯ Random TP",        "&7Click per andare in un luogo random"));
        inv.setItem(6, tool(Material.SPIDER_EYE, "&d&l⌐ Toggle Vanish",    "&7Click per attivare/disattivare vanish"));
        inv.setItem(7, tool(Material.ENDER_CHEST,"&d&l▣ Enderchest",       "&7Click per la tua enderchest"));
        inv.setItem(8, tool(Material.BARRIER,    "&c&l✖ Esci da Staff Mode","&7Click per uscire"));

        p.updateInventory();

        MessageUtils.sendPrefixed(p, "&7Sei entrato in &fStaff Mode&7.");
    }

    public static void exitStaffMode(CherryCore plugin, Player p) {
        UUID uuid = p.getUniqueId();
        PlayerInventory inv = p.getInventory();

        ItemStack[] backInv = savedInventory.remove(uuid);
        ItemStack[] backArm = savedArmor.remove(uuid);
        GameMode    backGm  = savedGamemode.remove(uuid);
        Boolean     backFl  = savedFlight.remove(uuid);

        if (backInv != null) inv.setContents(backInv);
        if (backArm != null) inv.setArmorContents(backArm);
        if (backGm  != null) p.setGameMode(backGm);
        if (backFl  != null) p.setAllowFlight(backFl);
        p.setFlying(false);

        if (plugin.getVanishManager().isVanished(p)) {
            plugin.getVanishManager().disable(p);
        }

        p.updateInventory();
        MessageUtils.sendPrefixed(p, "&7Sei uscito dalla &fStaff Mode&7.");
    }

    public static ItemStack tool(Material mat, String name, String... lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore.length > 0) {
                List<String> l = new ArrayList<>();
                for (String s : lore) l.add(ChatColor.translateAlternateColorCodes('&', s));
                m.setLore(l);
            }
            m.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(CherryCore.get(), "staff_tool"),
                    PersistentDataType.STRING,
                    name);
            it.setItemMeta(m);
        }
        return it;
    }

    public static boolean isStaffTool(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return false;
        ItemMeta m = it.getItemMeta();
        return m.getPersistentDataContainer().has(
                new org.bukkit.NamespacedKey(CherryCore.get(), "staff_tool"),
                PersistentDataType.STRING);
    }
}
