package dev.breach.DistrictRP.commands.roleplay.chat;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.tables.ChatSymTable;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ChatSymManager implements Listener, CommandExecutor, TabCompleter {

    private final DistrictRP plugin;
    private final File file;
    private FileConfiguration config;
    private final Map<String, String> symbols = new LinkedHashMap<>();

    private ChatSymTable table;
    private boolean useDb;

    public ChatSymManager(DistrictRP plugin) {
        this.plugin = plugin;
        File dir = new File(plugin.getDataFolder(), "roleplay");
        if (!dir.exists()) dir.mkdirs();
        this.file = new File(dir, "chatsym.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.config = YamlConfiguration.loadConfiguration(file);

        var dbm = plugin.getDatabaseManager();
        this.table = (dbm != null && dbm.isMariaDb()) ? dbm.getTable("chatsym", ChatSymTable.class) : null;
        this.useDb = (table != null);

        if (useDb) {
            plugin.getLogger().info("[ChatSym] Storage: MariaDB");
            loadFromDb();
        } else {
            plugin.getLogger().info("[ChatSym] Storage: YAML");
            loadYaml();
        }
    }

    private void loadYaml() {
        symbols.clear();
        List<Map<?, ?>> savedList = config.getMapList("symbols");
        for (Map<?, ?> entry : savedList) {
            Object sym = entry.get("symbol");
            Object cmd = entry.get("command");
            if (sym != null && cmd != null) {
                String s = sym.toString();
                String c = cmd.toString();
                if (!s.isEmpty()) symbols.put(s, c);
            }
        }
        if (symbols.isEmpty()) applyDefaults();
        plugin.getLogger().info("[ChatSym] Caricati " + symbols.size() + " simboli.");
    }

    private void loadFromDb() {
        fetchAll().thenAccept(map -> {
            symbols.clear();
            symbols.putAll(map);
            if (symbols.isEmpty()) {
                applyDefaults();
                for (Map.Entry<String, String> e : symbols.entrySet()) {
                    upsert(e.getKey(), e.getValue());
                }
            }
            plugin.getLogger().info("[ChatSym] Caricati " + symbols.size() + " simboli dal DB.");
        }).exceptionally(t -> {
            plugin.getLogger().warning("[ChatSym] Errore load DB: " + t.getMessage());
            return null;
        });
    }

    private void applyDefaults() {
        List<Map<?, ?>> defList = plugin.getConfig().getMapList("chatsym.default");
        for (Map<?, ?> entry : defList) {
            Object sym = entry.get("symbol");
            Object cmd = entry.get("command");
            if (sym != null && cmd != null) {
                String s = sym.toString();
                String c = cmd.toString();
                if (!s.isEmpty()) symbols.put(s, c);
            }
        }
        if (symbols.isEmpty()) {
            symbols.put("!", "staffchat");
            symbols.put(".", "modchat");
            symbols.put("?", "builderchat");
            symbols.put("@", "adminchat");
        }
    }

    public Map<String, String> getSymbols() { return symbols; }
    public String getCommand(String symbol) { return symbols.get(symbol); }
    public boolean exists(String symbol) { return symbols.containsKey(symbol); }

    public boolean add(String symbol, String command) {
        if (symbol == null || symbol.isEmpty()) return false;
        if (symbols.containsKey(symbol)) return false;
        symbols.put(symbol, command);
        if (useDb) upsert(symbol, command);
        else save();
        return true;
    }

    public boolean remove(String symbol) {
        if (!symbols.containsKey(symbol)) return false;
        symbols.remove(symbol);
        if (useDb) delete(symbol);
        else save();
        return true;
    }

    public void save() {
        if (useDb) {
            for (Map.Entry<String, String> e : symbols.entrySet()) {
                upsert(e.getKey(), e.getValue());
            }
            return;
        }
        try {
            config = new YamlConfiguration();
            List<Map<String, String>> list = new ArrayList<>();
            for (Map.Entry<String, String> e : symbols.entrySet()) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("symbol", e.getKey());
                m.put("command", e.getValue());
                list.add(m);
            }
            config.set("symbols", list);
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Errore salvataggio chatsym.yml: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Errore path chatsym.yml: " + e.getMessage());
        }
    }

    public boolean isUsingDatabase() { return useDb; }

    public CompletableFuture<Boolean> upsert(String symbol, String command) {
        if (table == null) return CompletableFuture.completedFuture(false);
        return table.upsert(symbol, command);
    }

    public CompletableFuture<Boolean> delete(String symbol) {
        if (table == null) return CompletableFuture.completedFuture(false);
        return table.delete(symbol);
    }

    public CompletableFuture<Map<String, String>> fetchAll() {
        if (table == null) return CompletableFuture.completedFuture(new java.util.HashMap<>());
        return table.all();
    }

    // --- listener: intercetta i prefissi-simbolo e li converte in comandi ---

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String perm = plugin.getConfig().getString("chatsym.permission", "DistrictRP.staffchatsym");
        if (!player.hasPermission(perm)) return;

        String message = event.getMessage();
        if (message == null || message.isEmpty()) return;

        List<Map.Entry<String, String>> sorted = new ArrayList<>(symbols.entrySet());
        sorted.sort(Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length()).reversed());

        for (Map.Entry<String, String> entry : sorted) {
            String symbol = entry.getKey();
            if (message.startsWith(symbol)) {
                String rest = message.substring(symbol.length()).trim();
                String fullCommand = entry.getValue() + (rest.isEmpty() ? "" : " " + rest);
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!Bukkit.dispatchCommand(player, fullCommand)) {
                        plugin.getLogger().warning("[ChatSym] Comando /" + fullCommand + " non eseguito (unknown/errore).");
                    }
                });
                return;
            }
        }
    }

    // --- comando /chatsym ---

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String perm = plugin.getConfig().getString("chatsym.permission", "DistrictRP.staffchatsym");
        if (!sender.hasPermission(perm)) {
            MessageUtils.sendMsg(sender, "general.no-permission");
            return true;
        }
        if (args.length == 0) {
            MessageUtils.sendList(sender, "chatsym.help");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "aggiungi" -> {
                if (args.length < 3) {
                    MessageUtils.sendList(sender, "chatsym.help");
                    return true;
                }
                String name = args[1].startsWith("/") ? args[1].substring(1) : args[1];
                String symbol = args[2];
                if (!add(symbol, name)) {
                    MessageUtils.sendMsg(sender, "chatsym.already-exists");
                    return true;
                }
                MessageUtils.sendMsg(sender, "chatsym.added", "symbol", symbol, "command", name);
            }
            case "rimuovi" -> {
                if (args.length < 2) {
                    MessageUtils.sendList(sender, "chatsym.help");
                    return true;
                }
                String symbol = args[1];
                if (!remove(symbol)) {
                    MessageUtils.sendMsg(sender, "chatsym.not-found");
                    return true;
                }
                MessageUtils.sendMsg(sender, "chatsym.removed", "symbol", symbol);
            }
            default -> MessageUtils.sendList(sender, "chatsym.help");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return Arrays.asList("aggiungi", "rimuovi");
        if (args.length == 2 && args[0].equalsIgnoreCase("rimuovi")) {
            return new ArrayList<>(symbols.keySet());
        }
        return new ArrayList<>();
    }
}