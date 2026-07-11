package dev.breach.DistrictRP.framework;

import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ModuleMessages {

    private final File file;
    private FileConfiguration config;
    private final String prefix;

    public ModuleMessages(File file, String prefix) {
        this.file = file;
        this.prefix = prefix == null ? "" : prefix;
        reload();
    }

    public void reload() {
        if (!file.exists()) {
            this.config = new YamlConfiguration();
            return;
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public String getRaw(String key) {
        String raw = config.getString(key);
        if (raw == null) return "&c[missing: " + key + "]";
        return raw.replace("%prefix%", prefix).replace("{prefix}", prefix);
    }

    public String get(String key, Object... placeholders) {
        String raw = getRaw(key);
        return MessageUtils.color(applyPlaceholders(raw, placeholders));
    }

    public List<String> getList(String key, Object... placeholders) {
        List<String> raw = config.getStringList(key);
        List<String> out = new ArrayList<>();
        for (String line : raw) {
            String r = line.replace("%prefix%", prefix).replace("{prefix}", prefix);
            out.add(MessageUtils.color(applyPlaceholders(r, placeholders)));
        }
        return out;
    }

    private String applyPlaceholders(String raw, Object... placeholders) {
        if (placeholders == null || placeholders.length == 0) return raw;
        String out = raw;
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            String key = String.valueOf(placeholders[i]);
            String val = String.valueOf(placeholders[i + 1]);
            out = out.replace("%" + key + "%", val)
                    .replace("<" + key + ">", val)
                    .replace("{" + key + "}", val);
        }
        return out;
    }

    public FileConfiguration raw() { return config; }
}