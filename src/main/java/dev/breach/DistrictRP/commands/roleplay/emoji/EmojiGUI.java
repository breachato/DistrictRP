package dev.breach.DistrictRP.commands.roleplay.emoji;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EmojiGUI implements Listener, CommandExecutor {

    private final DistrictRP plugin;
    private final EmojiManager manager;
    private final NamespacedKey emojiKey;
    private final NamespacedKey navKey;

    private static final int PAGE_SIZE = 21;

    public EmojiGUI(DistrictRP plugin, EmojiManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.emojiKey = new NamespacedKey(plugin, "emoji_name");
        this.navKey = new NamespacedKey(plugin, "emoji_nav");
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        boolean access = event.getPlayer().hasPermission(manager.getPermission());
        String replaced = manager.replaceAll(event.getMessage(), access);
        if (!replaced.equals(event.getMessage())) event.setMessage(replaced);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            MessageUtils.sendMsg(sender, "general.only-player");
            return true;
        }
        open(p, 0);
        return true;
    }

    public void open(Player player, int page) {
        List<Map.Entry<String, String>> list = new ArrayList<>(manager.getEmojis().entrySet());
        int totalPages = Math.max(1, (int) Math.ceil(list.size() / (double) PAGE_SIZE));
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        Inventory inv = Bukkit.createInventory(null, 54,
                MessageUtils.color(manager.getGuiTitle()));

        int start = page * PAGE_SIZE;
        int slot = 0;
        int[] contentSlots = contentSlots();

        for (int i = start; i < list.size() && slot < PAGE_SIZE; i++) {
            Map.Entry<String, String> entry = list.get(i);
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            String name = entry.getKey();

            meta.setDisplayName(MessageUtils.color("&e" + capitalize(name) + " &8» &7:" + name + ":"));
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add(MessageUtils.color("&e&lCLICK PER INVIARE IN CHAT"));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(emojiKey, PersistentDataType.STRING, name);
            item.setItemMeta(meta);

            inv.setItem(contentSlots[slot], item);
            slot++;
        }

        if (page > 0) inv.setItem(45, navItem(Material.ARROW, "&e◀ Pagina precedente", "prev:" + (page - 1)));
        if (page < totalPages - 1) inv.setItem(53, navItem(Material.ARROW, "&ePagina successiva ▶", "next:" + (page + 1)));

        player.openInventory(inv);
    }

    private int[] contentSlots() {
        int[] slots = new int[PAGE_SIZE];
        int idx = 0;
        for (int row = 1; row <= 3; row++) {
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

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = MessageUtils.color(manager.getGuiTitle());
        if (!event.getView().getTitle().equals(title)) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();

        String nav = meta.getPersistentDataContainer().get(navKey, PersistentDataType.STRING);
        if (nav != null) {
            String[] parts = nav.split(":");
            open(player, Integer.parseInt(parts[1]));
            return;
        }

        String emojiName = meta.getPersistentDataContainer().get(emojiKey, PersistentDataType.STRING);
        if (emojiName != null) {
            player.closeInventory();
            boolean access = player.hasPermission(manager.getPermission());
            String out = access ? manager.getChar(emojiName) : ":" + emojiName + ":";
            Bukkit.getScheduler().runTask(plugin, () -> player.chat(out));
        }
    }
}