package dev.breach.cherryCore.core;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CherryCoreDispatcher implements CommandExecutor, TabCompleter, Listener {

    private static final String GUI_TITLE = ChatColor.translateAlternateColorCodes('&',
            "&d&l  𝐂𝐡𝐞𝐫𝐫𝐲 𝐔𝐧𝐢𝐯𝐞𝐫𝐬𝐢𝐭𝐲");

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "help", "gui", "rtp", "reload", "info"
    );

    private final CherryCore plugin;

    public CherryCoreDispatcher(CherryCore plugin) {
        this.plugin = plugin;
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
                MessageUtils.send(p, "&d&l  CherryCore");
                MessageUtils.send(p, "");
                MessageUtils.send(p, "&7  Versione: &f" + CherryCore.BUILD_TAG);
                MessageUtils.send(p, "&7  Autore: &fvisualizzazione");
                MessageUtils.send(p, "");
            }

            default -> sendHelp(p);
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("cherrycore.use")) return Collections.emptyList();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> result = new ArrayList<>();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(partial)) result.add(sub);
            }
            return result;
        }

        return Collections.emptyList();
    }

    private void openGui(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);

        ItemStack border = item(Material.PINK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, border);

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

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(GUI_TITLE)) return;
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        switch (e.getRawSlot()) {
            case 10 -> { p.closeInventory(); p.performCommand("cc rtp"); }
            case 12 -> { p.closeInventory(); p.performCommand("homes"); }
            case 14 -> { p.closeInventory(); p.performCommand("warps"); }
            case 16 -> { p.closeInventory(); p.performCommand("spawn"); }
            case 28 -> { p.closeInventory(); p.performCommand("fly"); }
            case 30 -> { p.closeInventory(); p.performCommand("god"); }
            case 32 -> {
                p.closeInventory();
                MessageUtils.sendPrefixed(p, "&7Usa &d/speed <0-10> &7per cambiare velocità.");
            }
            case 34 -> { p.closeInventory(); p.performCommand("cc info"); }
            case 49 -> p.closeInventory();
        }
    }

    private void sendHelp(Player p) {
        MessageUtils.send(p, "");
        MessageUtils.send(p, "&d&lCherryCore &7- &fHelp");
        MessageUtils.send(p, "");
        MessageUtils.send(p, "&8▸ &dGM&7: /gmc, /gms, /gma, /gmsp");
        MessageUtils.send(p, "&8▸ &dUtilità&7: /fly, /god, /speed <0-10>");
        MessageUtils.send(p, "&8▸ &dChat&7: /msg, /reply, /nick, /rec");
        MessageUtils.send(p, "&8▸ &dChat tools&7: /clearchat, /annuncio");
        MessageUtils.send(p, "&8▸ &dQueue&7: /primo, /prossimo");
        MessageUtils.send(p, "&8▸ &dHome&7: /home, /sethome, /delhome, /homes");
        MessageUtils.send(p, "&8▸ &dWarp&7: /warp, /setwarp, /delwarp, /warps");
        MessageUtils.send(p, "&8▸ &dTeleport&7: /spawn, /setspawn, /back");
        MessageUtils.send(p, "&8▸ &dTP utils&7: /tphere, /tpall");
        MessageUtils.send(p, "&8▸ &dStaff&7: /invsee, /enderchest, /staffmode");
        MessageUtils.send(p, "&8▸ &dStaff tools&7: /vanish, /perms");
        MessageUtils.send(p, "&8▸ &dExtra&7: /disguise, /mondi");
        MessageUtils.send(p, "&8▸ &dSkin&7: /recskinset, /recskindel");

        MessageUtils.send(p, "");
    }

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
