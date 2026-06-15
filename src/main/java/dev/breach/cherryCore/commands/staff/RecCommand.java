package dev.breach.cherryCore.commands.staff;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.UUID;

public class RecCommand implements CommandExecutor {

    public static final String GUI_TITLE = ChatColor.translateAlternateColorCodes('&', "&8'");

    private final CherryCore plugin;

    public RecCommand(CherryCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            MessageUtils.send(sender, "&cSolo i giocatori.");
            return true;
        }
        if (!p.hasPermission("rec.use")) {
            MessageUtils.send(p, "&cNon hai il permesso.");
            return true;
        }

        UUID uuid = p.getUniqueId();
        String skin = plugin.recSkin.get(uuid);

        if (skin == null) {
            openGui(p);
            return true;
        }

        boolean active = Boolean.TRUE.equals(plugin.recActive.get(uuid));

        if (!active) {
            plugin.recActive.put(uuid, true);
            plugin.recOriginRank.put(uuid, "default");
            Bukkit.dispatchCommand(p, "skin set " + skin);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "lp user " + p.getName() + " permission set rec.active true");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "lp user " + p.getName() + " permission set rec.bypassflag true");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "tab player " + p.getName() + " tagprefix  ");
            for (PotionEffect ef : p.getActivePotionEffects()) p.removePotionEffect(ef.getType());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab reload");
            MessageUtils.sendPrefixed(p, "&aRec mode attivato.");
        } else {
            plugin.recActive.remove(uuid);
            String rank = plugin.recOriginRank.getOrDefault(uuid, "default");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "skin clear " + p.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "lp user " + p.getName() + " parent add " + rank);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "lp user " + p.getName() + " permission unset rec.active");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "lp user " + p.getName() + " permission set rec.bypassflag true");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "tab player " + p.getName() + " tagprefix");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "tab player " + p.getName() + " tabprefix");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab reload");

            if (p.hasPermission("group.perms")) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,
                        Integer.MAX_VALUE, 0, false, false));
            } else {
                p.removePotionEffect(PotionEffectType.GLOWING);
            }
            MessageUtils.sendPrefixed(p, "&cRec mode disattivato.");
        }
        return true;
    }

    public static void openGui(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);
        ItemStack pane = item(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        inv.setItem(11, item(Material.PLAYER_HEAD, "&a► Skin da Nickname",
                "&7● &7Carica una skin da un nickname."));
        inv.setItem(15, item(Material.PLAYER_HEAD, "&b► Skin da URL",
                "&7● &7Carica una skin da un link."));
        inv.setItem(22, item(Material.BARRIER, "&c✕"));

        p.openInventory(inv);
    }

    private static ItemStack item(Material mat, String name, String... lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            if (lore.length > 0) {
                m.setLore(Arrays.stream(lore)
                        .map(s -> ChatColor.translateAlternateColorCodes('&', s))
                        .toList());
            }
            it.setItemMeta(m);
        }
        return it;
    }

    // /recskinset
    public static class Set implements CommandExecutor {

        private final CherryCore plugin;

        public Set(CherryCore plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                                 @NotNull String label, @NotNull String[] args) {
            if (!sender.hasPermission("rec.admin")) {
                MessageUtils.send(sender, "&cNon hai il permesso.");
                return true;
            }
            if (args.length < 2) {
                MessageUtils.send(sender, "&cUsa: /recskinset <player> <skin>");
                return true;
            }
            OfflinePlayer op = Bukkit.getOfflinePlayer(args[0]);
            plugin.recSkin.put(op.getUniqueId(), args[1]);
            MessageUtils.send(sender, "&a✓ Skin REC impostata per " + args[0]);
            return true;
        }
    }

    // /recskindel
    public static class Del implements CommandExecutor {

        private final CherryCore plugin;

        public Del(CherryCore plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                                 @NotNull String label, @NotNull String[] args) {
            if (!sender.hasPermission("rec.admin")) {
                MessageUtils.send(sender, "&cNon hai il permesso.");
                return true;
            }
            if (args.length < 1) {
                MessageUtils.send(sender, "&cUsa: /recskindel <player>");
                return true;
            }
            OfflinePlayer op = Bukkit.getOfflinePlayer(args[0]);
            plugin.recSkin.remove(op.getUniqueId());
            MessageUtils.send(sender, "&c✗ Skin REC rimossa per " + args[0]);
            return true;
        }
    }

    public static class ChatListener implements Listener {

        private final CherryCore plugin;

        public ChatListener(CherryCore plugin) {
            this.plugin = plugin;
        }

        @EventHandler
        public void onClick(InventoryClickEvent e) {
            if (e.getView().getTitle().equals(GUI_TITLE)) {
                e.setCancelled(true);
                if (!(e.getWhoClicked() instanceof Player p)) return;

                switch (e.getRawSlot()) {
                    case 11 -> {
                        p.closeInventory();
                        plugin.recWaiting.put(p.getUniqueId(), "nick");
                        MessageUtils.send(p,
                                "&a┃ &fScrivi in chat il &anickname &fdi chi vuoi copiare la skin:");
                    }
                    case 15 -> {
                        p.closeInventory();
                        plugin.recWaiting.put(p.getUniqueId(), "url");
                        MessageUtils.send(p,
                                "&b┃ &fScrivi in chat l'indirizzo &bURL &fdella skin:");
                    }
                    case 22 -> p.closeInventory();
                }
            }
        }

        @EventHandler
        public void onChat(AsyncPlayerChatEvent e) {
            Player p = e.getPlayer();
            String mode = plugin.recWaiting.get(p.getUniqueId());
            if (mode == null) return;

            e.setCancelled(true);
            String msg = e.getMessage();

            if (mode.equals("nick")) {
                plugin.recSkin.put(p.getUniqueId(), msg);
                plugin.recWaiting.remove(p.getUniqueId());
                MessageUtils.send(p, "&a✓ Skin impostata da nickname: &f" + msg);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            "tab player " + p.getName() + " tagprefix  ");
                    Bukkit.dispatchCommand(p, "rec");
                });
            } else if (mode.equals("url")) {
                if (!msg.contains("http")) {
                    plugin.recWaiting.remove(p.getUniqueId());
                    MessageUtils.send(p, "&c✗ URL non valido!");
                    return;
                }
                plugin.recSkin.put(p.getUniqueId(), msg);
                plugin.recWaiting.remove(p.getUniqueId());
                MessageUtils.send(p, "&a✓ Skin URL impostata");
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            "tab player " + p.getName() + " tagprefix  ");
                    Bukkit.dispatchCommand(p, "rec");
                });
            }
        }
    }
}
