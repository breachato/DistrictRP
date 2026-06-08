package dev.breach.cherryCore.core;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CherryCoreDispatcher implements CommandExecutor, Listener {

    private static final String GUI_TITLE = ChatColor.translateAlternateColorCodes('&',
            "&d&l  𝐂𝐡𝐞𝐫𝐫𝐲 𝐔𝐧𝐢𝐯𝐞𝐫𝐬𝐢𝐭𝐲");

    private final CherryCore plugin;

    public CherryCoreDispatcher(CherryCore plugin) {
        this.plugin = plugin;
        // Registra come listener per gestire i click nella GUI
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("cherrycore.use")) {
            MessageUtils.send(sender, "&c✗ Non hai accesso a CherryCore.");
            return true;
        }

        if (!(sender instanceof Player p)) {
            MessageUtils.send(sender, "&cSolo i giocatori.");
            return true;
        }

        // /cc o /cc help -> help
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(p);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "gui" -> openGui(p);

            case "rtp" -> {
                if (!p.hasPermission("cherrycore.rtp")) {
                    MessageUtils.send(p, "&cNon hai il permesso.");
                    return true;
                }
                // RTP a player online random (non staff)
                List<Player> candidates = new ArrayList<>();
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.equals(p)) continue;
                    if (online.hasPermission("cherrycore.staffmode")) continue;
                    if (plugin.getVanishManager() != null && plugin.getVanishManager().isVanished(online)) continue;
                    candidates.add(online);
                }
                if (candidates.isEmpty()) {
                    MessageUtils.sendPrefixed(p, "&cNessun giocatore disponibile per il teleport.");
                    return true;
                }
                Player target = candidates.get(new Random().nextInt(candidates.size()));
                plugin.back.put(p.getUniqueId(), p.getLocation());
                p.teleport(target.getLocation());
                MessageUtils.sendPrefixed(p, "&fTeleportato a &d" + target.getName() + "&f!");
            }

            case "reload" -> {
                if (!p.hasPermission("cherrycore.reload")) {
                    MessageUtils.send(p, "&cNon hai il permesso.");
                    return true;
                }
                plugin.reloadConfig();
                MessageUtils.sendPrefixed(p, "&aConfigurazione ricaricata!");
            }

            case "info" -> {
                MessageUtils.send(p, "");
                MessageUtils.send(p, "&d&l  𝐂𝐡𝐞𝐫𝐫𝐲𝐂𝐨𝐫𝐞");
                MessageUtils.send(p, "");
                MessageUtils.send(p, "&7  Versione: &f" + plugin.getDescription().getVersion());
                MessageUtils.send(p, "&7  Build: &f" + CherryCore.BUILD_TAG);
                MessageUtils.send(p, "&7  Autore: &f" + plugin.getDescription().getAuthors());
                MessageUtils.send(p, "");
            }

            default -> sendHelp(p);
        }
        return true;
    }

    // ============================================================
    // GUI
    // ============================================================
    private void openGui(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);

        // Bordo
        ItemStack border = item(Material.PINK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, border);

        // Contenuto
        inv.setItem(10, item(Material.COMPASS, "&d&l⌖ RTP",
                "&7Click per teletrasportarti", "&7a un giocatore random."));

        inv.setItem(12, item(Material.RED_BED, "&d&l⌂ Homes",
                "&7Gestisci le tue homes."));

        inv.setItem(14, item(Material.ENDER_PEARL, "&d&l↯ Warps",
                "&7Lista dei warps disponibili."));

        inv.setItem(16, item(Material.NETHER_STAR, "&d&l✦ Spawn",
                "&7Torna allo spawn."));

        inv.setItem(28, item(Material.FEATHER, "&d&l≈ Fly",
                "&7Attiva/disattiva il volo."));

        inv.setItem(30, item(Material.GOLDEN_APPLE, "&d&l♥ God",
                "&7Attiva/disattiva godmode."));

        inv.setItem(32, item(Material.SUGAR, "&d&l» Speed",
                "&7Cambia la tua velocità."));

        inv.setItem(34, item(Material.BOOK, "&d&l✎ Info",
                "&7Info sul plugin."));

        inv.setItem(49, item(Material.BARRIER, "&c&l✖ Chiudi"));

        p.openInventory(inv);
    }

    // ============================================================
    // Click Handler
    // ============================================================
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(GUI_TITLE)) return;

        // Blocca SEMPRE il prelievo di item dalla GUI
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        switch (e.getRawSlot()) {
            case 10 -> { // RTP
                p.closeInventory();
                p.performCommand("cc rtp");
            }
            case 12 -> { // Homes
                p.closeInventory();
                p.performCommand("homes");
            }
            case 14 -> { // Warps
                p.closeInventory();
                p.performCommand("warps");
            }
            case 16 -> { // Spawn
                p.closeInventory();
                p.performCommand("spawn");
            }
            case 28 -> { // Fly
                p.closeInventory();
                p.performCommand("fly");
            }
            case 30 -> { // God
                p.closeInventory();
                p.performCommand("god");
            }
            case 32 -> { // Speed
                p.closeInventory();
                MessageUtils.sendPrefixed(p, "&7Usa &d/speed <0-10> &7per cambiare velocità.");
            }
            case 34 -> { // Info
                p.closeInventory();
                p.performCommand("cc info");
            }
            case 49 -> p.closeInventory(); // Chiudi
        }
    }

    // ============================================================
    // Help
    // ============================================================
    private void sendHelp(Player p) {
        MessageUtils.send(p, "");
        MessageUtils.send(p, "&d&l  𝐂𝐡𝐞𝐫𝐫𝐲𝐂𝐨𝐫𝐞");
        MessageUtils.send(p, "");
        MessageUtils.send(p, "&d&lGiocatore:");
        MessageUtils.send(p, "&8▸ &d/cc gmc&7, &d/cc gms&7, &d/cc gma&7, &d/cc gmsp");
        MessageUtils.send(p, "&8▸ &d/cc fly&7, &d/cc god&7, &d/cc speed <0-10>");
        MessageUtils.send(p, "&8▸ &d/cc msg&7, &d/cc reply&7, &d/cc nick, &d/rec");
        MessageUtils.send(p, "&8▸ &d/cc clearchat&7, &d/cc primo&7, &d/cc prossimo&7, &d/cc annuncio");
        MessageUtils.send(p, "");
        MessageUtils.send(p, "&d&lTeletrasporto:");
        MessageUtils.send(p, "&8▸ &d/cc home&7, &d/cc sethome&7, &d/cc delhome&7, &d/cc homes");
        MessageUtils.send(p, "&8▸ &d/cc warp&7, &d/cc setwarp&7, &d/cc delwarp&7, &d/cc warps");
        MessageUtils.send(p, "&8▸ &d/cc spawn&7, &d/cc setspawn&7, &d/cc back");
        MessageUtils.send(p, "&8▸ &d/cc tphere&7, &d/cc tpall");
        MessageUtils.send(p, "");
        MessageUtils.send(p, "&d&lAscensori:");
        MessageUtils.send(p, "&8▸ &d/el help");
        MessageUtils.send(p, "");
        MessageUtils.send(p, "&d&lMondi:");
        MessageUtils.send(p, "&8▸ &d/mondi");
        MessageUtils.send(p, "");
        MessageUtils.send(p, "&d&lStaff:");
        MessageUtils.send(p, "&8▸ &d/cc invsee&7, &d/cc enderchest, &d/staffmode");
        MessageUtils.send(p, "&8▸ &d/cc vanish&&7, &d/recskindel &7&d/recskinset");
        MessageUtils.send(p, "&8▸ &d/tpall&7, &d/tphere, &7&d/disguise&7");
        MessageUtils.send(p, "&8▸ &d/perms&7, &d/disguise&7,, &d/cc god");
        MessageUtils.send(p, "");
    }

    // ============================================================
    // Item builder
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