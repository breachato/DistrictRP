package dev.breach.DistrictRP.commands.roleplay.chat;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.repository.ChatSymRepository;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChatSymManager {

    private final DistrictRP plugin;
    private final File file;
    private FileConfiguration config;
    private final Map<String, String> symbols = new LinkedHashMap<>();

    private ChatSymRepository repo;
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

        this.repo = new ChatSymRepository(plugin);
        this.useDb = repo.isAvailable();

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
        repo.fetchAll().thenAccept(map -> {
            symbols.clear();
            symbols.putAll(map);
            if (symbols.isEmpty()) {
                applyDefaults();
                for (Map.Entry<String, String> e : symbols.entrySet()) {
                    repo.upsert(e.getKey(), e.getValue());
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
        if (useDb) repo.upsert(symbol, command);
        else save();
        return true;
    }

    public boolean remove(String symbol) {
        if (!symbols.containsKey(symbol)) return false;
        symbols.remove(symbol);
        if (useDb) repo.delete(symbol);
        else save();
        return true;
    }

    public void save() {
        if (useDb) {
            for (Map.Entry<String, String> e : symbols.entrySet()) {
                repo.upsert(e.getKey(), e.getValue());
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
    public ChatSymRepository getRepository() { return repo; }
}