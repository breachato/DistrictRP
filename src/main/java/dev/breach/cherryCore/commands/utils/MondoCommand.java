package dev.breach.cherryCore.commands.utils;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.UUID;

public class MondoCommand implements CommandExecutor {

    private final CherryCore plugin;

    public MondoCommand(CherryCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("cherrycore.mondo")) {
            MessageUtils.send(sender, "&c✗ Non hai il permesso.");
            return true;
        }

        if (!plugin.getMultiverse().isReady()) {
            MessageUtils.sendPrefixed(sender, "&cMultiverse-Core non è installato!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "aggiungi":
            case "crea":
            case "add":
                handleAdd(sender, args);
                break;

            case "rimuovi":
            case "elimina":
            case "remove":
                handleRemove(sender, args);
                break;

            case "visita":
            case "tp":
                handleVisit(sender, args);
                break;

            case "lista":
            case "list":
                handleList(sender);
                break;

            case "whitelist":
                handleWhitelist(sender, args);
                break;

            case "blacklist":
                handleBlacklist(sender, args);
                break;

            default:
                sendHelp(sender);
        }
        return true;
    }

    private void handleAdd(CommandSender s, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendPrefixed(s, "&cUsa: /mondo aggiungi <cartella|url> [nome]");
            MessageUtils.sendPrefixed(s, "&7Esempi:");
            MessageUtils.sendPrefixed(s, "&7 /mondo aggiungi survival_world");
            MessageUtils.sendPrefixed(s, "&7 /mondo aggiungi C:/Users/me/myworld nuovo_nome");
            MessageUtils.sendPrefixed(s, "&7 /mondo aggiungi https://site.com/mondo.zip parkour");
            return;
        }

        String source = args[1];
        String targetName = args.length >= 3 ? args[2] : null;

        if (source.startsWith("http://") || source.startsWith("https://")) {
            if (targetName == null) {
                String fileName = source.substring(source.lastIndexOf('/') + 1);
                fileName = fileName.replaceAll("\\.zip$|\\.tar\\.gz$|\\.tgz$|\\.rar$", "");
                targetName = fileName;
            }
            final String finalName = targetName;

            if (plugin.getMultiverse().worldExists(finalName)) {
                MessageUtils.sendPrefixed(s, "&cIl mondo &f" + finalName + " &cesiste già in Multiverse!");
                return;
            }
            File existing = new File(Bukkit.getWorldContainer(), finalName);
            if (existing.exists()) {
                MessageUtils.sendPrefixed(s, "&cLa cartella &f" + finalName + " &cesiste già nel server!");
                return;
            }

            MessageUtils.sendPrefixed(s, "&7Download e estrazione in corso (potrebbe richiedere tempo)...");
            plugin.getWorldDownloader().downloadAndExtract(source, finalName,
                    ok -> {
                        MessageUtils.sendPrefixed(s, "&aDownload completato. Importazione in Multiverse...");
                        importToMultiverse(s, finalName);
                    },
                    err -> MessageUtils.sendPrefixed(s, "&cErrore download: " + err)
            );
            return;
        }

        if (source.contains(":") || source.startsWith("/")) {
            if (targetName == null) {
                File src = new File(source);
                targetName = src.getName();
            }
            final String finalName = targetName;

            if (plugin.getMultiverse().worldExists(finalName)) {
                MessageUtils.sendPrefixed(s, "&cIl mondo &f" + finalName + " &cesiste già in Multiverse!");
                return;
            }
            File existing = new File(Bukkit.getWorldContainer(), finalName);
            if (existing.exists()) {
                MessageUtils.sendPrefixed(s, "&cLa cartella &f" + finalName + " &cesiste già nel server!");
                return;
            }

            MessageUtils.sendPrefixed(s, "&7Copia della cartella in corso...");
            plugin.getWorldDownloader().importLocalFolder(source, finalName,
                    ok -> {
                        MessageUtils.sendPrefixed(s, "&aCopia completata. Importazione in Multiverse...");
                        importToMultiverse(s, finalName);
                    },
                    err -> MessageUtils.sendPrefixed(s, "&cErrore: " + err)
            );
            return;
        }

        File serverFolder = new File(Bukkit.getWorldContainer(), source);
        if (!serverFolder.exists() || !serverFolder.isDirectory()) {
            MessageUtils.sendPrefixed(s, "&cCartella &f" + source + " &cnon trovata nella root del server.");
            return;
        }
        if (!new File(serverFolder, "level.dat").exists()) {
            MessageUtils.sendPrefixed(s, "&cLa cartella &f" + source + " &cnon è un mondo Minecraft valido (manca level.dat).");
            return;
        }

        if (targetName != null && !targetName.equals(source)) {
            File newFolder = new File(Bukkit.getWorldContainer(), targetName);
            if (newFolder.exists()) {
                MessageUtils.sendPrefixed(s, "&cIl nome &f" + targetName + " &cesiste già.");
                return;
            }
            if (!serverFolder.renameTo(newFolder)) {
                MessageUtils.sendPrefixed(s, "&cImpossibile rinominare la cartella.");
                return;
            }
            source = targetName;
        }

        if (plugin.getMultiverse().worldExists(source)) {
            MessageUtils.sendPrefixed(s, "&cIl mondo &f" + source + " &cè già caricato in Multiverse!");
            return;
        }

        importToMultiverse(s, source);
    }

    private void importToMultiverse(CommandSender s, String name) {
        boolean ok = plugin.getMultiverse().importWorld(name, World.Environment.NORMAL);
        if (ok) {
            MessageUtils.sendPrefixed(s, "&aMondo &f" + name + " &aimportato con successo!");
            MessageUtils.sendPrefixed(s, "&7Usa &f/mondo visita " + name + " &7per teletrasportarti.");
        } else {
            MessageUtils.sendPrefixed(s, "&cErrore nell'importazione in Multiverse.");
        }
    }

    private void handleRemove(CommandSender s, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendPrefixed(s, "&cUsa: /mondo rimuovi <nome>");
            return;
        }
        String name = args[1];
        if (!plugin.getMultiverse().worldExists(name)) {
            MessageUtils.sendPrefixed(s, "&cMondo non trovato in Multiverse.");
            return;
        }
        boolean ok = plugin.getMultiverse().removeWorld(name);
        if (ok) {
            MessageUtils.sendPrefixed(s, "&aMondo &f" + name + " &arimosso da Multiverse.");
            MessageUtils.sendPrefixed(s, "&7I file della cartella NON sono stati eliminati.");
        } else {
            MessageUtils.sendPrefixed(s, "&cErrore nella rimozione.");
        }
    }

    private void handleVisit(CommandSender s, String[] args) {
        if (!(s instanceof Player p)) {
            MessageUtils.send(s, "&cSolo i giocatori.");
            return;
        }
        if (args.length < 2) {
            MessageUtils.sendPrefixed(p, "&cUsa: /mondo visita <nome>");
            return;
        }
        String name = args[1];
        if (!plugin.getMultiverse().worldExists(name)) {
            MessageUtils.sendPrefixed(p, "&cMondo non trovato.");
            return;
        }

        UUID uuid = p.getUniqueId();
        if (plugin.getDataManager().isBlacklisted(name, uuid)
                && !p.hasPermission("cherrycore.mondo.bypass")) {
            MessageUtils.sendPrefixed(p, "&cSei nella blacklist di questo mondo!");
            return;
        }
        if (plugin.getDataManager().isWhitelistEnabled(name)
                && !plugin.getDataManager().isWhitelisted(name, uuid)
                && !p.hasPermission("cherrycore.mondo.bypass")) {
            MessageUtils.sendPrefixed(p, "&cQuesto mondo è in whitelist e tu non sei autorizzato!");
            return;
        }

        boolean ok = plugin.getMultiverse().teleport(p, name);
        if (ok) {
            MessageUtils.sendPrefixed(p, "&aTeletrasportato nel mondo &f" + name + "&a.");
        } else {
            MessageUtils.sendPrefixed(p, "&cErrore nel teletrasporto.");
        }
    }

    private void handleList(CommandSender s) {
        List<String> worlds = plugin.getMultiverse().listWorlds();
        MessageUtils.send(s, "");
        MessageUtils.send(s, "&d&l━━━━━━━━━━━━━━━━━━━━━━━━");
        MessageUtils.send(s, "&d&l  ❀ MONDI REGISTRATI ❀");
        MessageUtils.send(s, "&d&l━━━━━━━━━━━━━━━━━━━━━━━━");
        if (worlds.isEmpty()) {
            MessageUtils.send(s, "&7  Nessun mondo registrato.");
        } else {
            for (String w : worlds) {
                int wl = plugin.getDataManager().getWhitelist(w).size();
                int bl = plugin.getDataManager().getBlacklist(w).size();
                MessageUtils.send(s, "&d  ▸ &f" + w
                        + " &8(WL: &d" + wl + " &8| BL: &d" + bl + "&8)");
            }
        }
        MessageUtils.send(s, "&d&l━━━━━━━━━━━━━━━━━━━━━━━━");
        MessageUtils.send(s, "");
    }

    private void handleWhitelist(CommandSender s, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendPrefixed(s, "&cUsa: /mondo whitelist <aggiungi|rimuovi|lista> <mondo> [player]");
            return;
        }
        String action = args[1].toLowerCase();

        if (action.equals("aggiungi") || action.equals("add")) {
            if (args.length < 4) {
                MessageUtils.sendPrefixed(s, "&cUsa: /mondo whitelist aggiungi <mondo> <player>");
                return;
            }
            String world = args[2];
            if (!plugin.getMultiverse().worldExists(world)) {
                MessageUtils.sendPrefixed(s, "&cMondo non trovato.");
                return;
            }
            OfflinePlayer op = Bukkit.getOfflinePlayer(args[3]);
            plugin.getDataManager().addWhitelist(world, op.getUniqueId());
            MessageUtils.sendPrefixed(s, "&a" + args[3] + " &7aggiunto alla whitelist di &f" + world + "&7.");
        }
        else if (action.equals("rimuovi") || action.equals("remove")) {
            if (args.length < 4) {
                MessageUtils.sendPrefixed(s, "&cUsa: /mondo whitelist rimuovi <mondo> <player>");
                return;
            }
            String world = args[2];
            OfflinePlayer op = Bukkit.getOfflinePlayer(args[3]);
            plugin.getDataManager().removeWhitelist(world, op.getUniqueId());
            MessageUtils.sendPrefixed(s, "&c" + args[3] + " &7rimosso dalla whitelist di &f" + world + "&7.");
        }
        else if (action.equals("lista") || action.equals("list")) {
            if (args.length < 3) {
                MessageUtils.sendPrefixed(s, "&cUsa: /mondo whitelist lista <mondo>");
                return;
            }
            String world = args[2];
            List<String> list = plugin.getDataManager().getWhitelist(world);
            MessageUtils.send(s, "");
            MessageUtils.send(s, "&d&l━━━━━━━━━━━━━━━━━━━━━━━━");
            MessageUtils.send(s, "&d&l  ✔ WHITELIST &f" + world);
            MessageUtils.send(s, "&d&l━━━━━━━━━━━━━━━━━━━━━━━━");
            if (list.isEmpty()) {
                MessageUtils.send(s, "&7  Whitelist vuota.");
            } else {
                for (String uuid : list) {
                    String name;
                    try {
                        name = Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName();
                    } catch (Exception ex) {
                        name = uuid;
                    }
                    if (name == null) name = uuid;
                    MessageUtils.send(s, "&d  ▸ &f" + name);
                }
            }
            MessageUtils.send(s, "&d&l━━━━━━━━━━━━━━━━━━━━━━━━");
            MessageUtils.send(s, "");
        }
        else {
            MessageUtils.sendPrefixed(s, "&cAzione sconosciuta. Usa: aggiungi, rimuovi, lista");
        }
    }

    private void handleBlacklist(CommandSender s, String[] args) {
        if (args.length < 2) {
            MessageUtils.sendPrefixed(s, "&cUsa: /mondo blacklist <aggiungi|rimuovi|lista> <mondo> [player]");
            return;
        }
        String action = args[1].toLowerCase();

        if (action.equals("aggiungi") || action.equals("add")) {
            if (args.length < 4) {
                MessageUtils.sendPrefixed(s, "&cUsa: /mondo blacklist aggiungi <mondo> <player>");
                return;
            }
            String world = args[2];
            if (!plugin.getMultiverse().worldExists(world)) {
                MessageUtils.sendPrefixed(s, "&cMondo non trovato.");
                return;
            }
            OfflinePlayer op = Bukkit.getOfflinePlayer(args[3]);
            plugin.getDataManager().addBlacklist(world, op.getUniqueId());
            MessageUtils.sendPrefixed(s, "&a" + args[3] + " &7aggiunto alla blacklist di &f" + world + "&7.");
        }
        else if (action.equals("rimuovi") || action.equals("remove")) {
            if (args.length < 4) {
                MessageUtils.sendPrefixed(s, "&cUsa: /mondo blacklist rimuovi <mondo> <player>");
                return;
            }
            String world = args[2];
            OfflinePlayer op = Bukkit.getOfflinePlayer(args[3]);
            plugin.getDataManager().removeBlacklist(world, op.getUniqueId());
            MessageUtils.sendPrefixed(s, "&c" + args[3] + " &7rimosso dalla blacklist di &f" + world + "&7.");
        }
        else if (action.equals("lista") || action.equals("list")) {
            if (args.length < 3) {
                MessageUtils.sendPrefixed(s, "&cUsa: /mondo blacklist lista <mondo>");
                return;
            }
            String world = args[2];
            List<String> list = plugin.getDataManager().getBlacklist(world);
            MessageUtils.send(s, "");
            MessageUtils.send(s, "&d&l━━━━━━━━━━━━━━━━━━━━━━━━");
            MessageUtils.send(s, "&d&l  ✖ BLACKLIST &f" + world);
            MessageUtils.send(s, "&d&l━━━━━━━━━━━━━━━━━━━━━━━━");
            if (list.isEmpty()) {
                MessageUtils.send(s, "&7  Blacklist vuota.");
            } else {
                for (String uuid : list) {
                    String name;
                    try {
                        name = Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName();
                    } catch (Exception ex) {
                        name = uuid;
                    }
                    if (name == null) name = uuid;
                    MessageUtils.send(s, "&d  ▸ &f" + name);
                }
            }
            MessageUtils.send(s, "&d&l━━━━━━━━━━━━━━━━━━━━━━━━");
            MessageUtils.send(s, "");
        }
        else {
            MessageUtils.sendPrefixed(s, "&cAzione sconosciuta. Usa: aggiungi, rimuovi, lista");
        }
    }

    private void sendHelp(CommandSender s) {
        MessageUtils.send(s, "");
        MessageUtils.send(s, "&d&l   Cherry University");
        MessageUtils.send(s, "");
        MessageUtils.send(s, "&d  /mondo aggiungi <cartella|url> [nome]");
        MessageUtils.send(s, "&7    Importa un mondo da:");
        MessageUtils.send(s, "&7     • cartella nella root server");
        MessageUtils.send(s, "&7     • percorso assoluto (C:/.../world)");
        MessageUtils.send(s, "&7     • URL zip");
        MessageUtils.send(s, "");
        MessageUtils.send(s, "&d  /mondo rimuovi <nome>");
        MessageUtils.send(s, "&d  /mondo visita <nome>");
        MessageUtils.send(s, "&d  /mondo lista");
        MessageUtils.send(s, "");
        MessageUtils.send(s, "&d  /mondo whitelist <aggiungi|rimuovi|lista> <mondo> [player]");
        MessageUtils.send(s, "&d  /mondo blacklist <aggiungi|rimuovi|lista> <mondo> [player]");
        MessageUtils.send(s, "");
    }
}
