package dev.breach.DistrictRP.commands.roleplay.emoji;

import dev.breach.DistrictRP.DistrictRP;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

public class EmojiManager {

    private final DistrictRP plugin;
    private final Map<String, String> emojis = new LinkedHashMap<>();

    public EmojiManager(DistrictRP plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        emojis.clear();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("emoji.list");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            emojis.put(key.toLowerCase(), sec.getString(key));
        }
    }

    public Map<String, String> getEmojis() {
        return emojis;
    }

    public boolean has(String name) {
        return emojis.containsKey(name.toLowerCase());
    }

    public String getChar(String name) {
        return emojis.get(name.toLowerCase());
    }

    public String getPermission() {
        return plugin.getConfig().getString("emoji.permission", "districtrp.emojiaccess");
    }

    public String getGuiTitle() {
        return plugin.getConfig().getString("emoji.gui-title", "&8EMOJI");
    }

    public String replaceAll(String message, boolean access) {
        String result = message;
        for (Map.Entry<String, String> e : emojis.entrySet()) {
            String tag = ":" + e.getKey() + ":";
            if (result.toLowerCase().contains(tag)) {
                String replacement = access ? e.getValue() : tag;
                result = replaceIgnoreCase(result, tag, replacement);
            }
        }
        return result;
    }

    private String replaceIgnoreCase(String source, String target, String replacement) {
        StringBuilder sb = new StringBuilder();
        String lowerSource = source.toLowerCase();
        String lowerTarget = target.toLowerCase();
        int start = 0;
        int idx;
        while ((idx = lowerSource.indexOf(lowerTarget, start)) != -1) {
            sb.append(source, start, idx);
            sb.append(replacement);
            start = idx + target.length();
        }
        sb.append(source.substring(start));
        return sb.toString();
    }
}