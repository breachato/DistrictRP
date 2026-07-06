package dev.breach.DistrictRP.functions;

import dev.breach.DistrictRP.DistrictRP;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;

import java.lang.reflect.Field;
import java.util.*;

public class CommandBlocker {

    private final DistrictRP plugin;
    private CommandMap commandMap;
    private Map<String, Command> knownCommands;

    private final List<String> blockedCommands = new ArrayList<>();
    private final List<String> overriddenCommands = new ArrayList<>();
    private final Set<String> protectedCommands = new HashSet<>();

    @SuppressWarnings("unchecked")
    public CommandBlocker(DistrictRP plugin) {
        this.plugin = plugin;
        try {
            Field cmField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            cmField.setAccessible(true);
            this.commandMap = (CommandMap) cmField.get(Bukkit.getServer());

            Field kcField = null;
            try {
                kcField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            } catch (NoSuchFieldException nsf) {
                for (Field f : this.commandMap.getClass().getDeclaredFields()) {
                    if (Map.class.isAssignableFrom(f.getType())) {
                        kcField = f;
                        break;
                    }
                }
            }
            if (kcField == null) {
                plugin.getLogger().severe("[CommandBlocker] Impossibile trovare knownCommands field.");
                return;
            }
            kcField.setAccessible(true);
            this.knownCommands = (Map<String, Command>) kcField.get(this.commandMap);

            loadProtectedCommands();

            plugin.getLogger().info("[CommandBlocker] Inizializzato correttamente. Comandi protetti: " + protectedCommands.size());
        } catch (Throwable t) {
            plugin.getLogger().severe("[CommandBlocker] Errore inizializzazione: " + t.getMessage());
        }
    }

    private void loadProtectedCommands() {
        if (plugin.getDescription() == null) return;
        Map<String, Map<String, Object>> commands = plugin.getDescription().getCommands();
        if (commands == null) return;
        for (Map.Entry<String, Map<String, Object>> entry : commands.entrySet()) {
            protectedCommands.add(entry.getKey().toLowerCase());
            Object aliases = entry.getValue().get("aliases");
            if (aliases instanceof List<?> aliasList) {
                for (Object a : aliasList) {
                    protectedCommands.add(a.toString().toLowerCase());
                }
            } else if (aliases instanceof String s) {
                protectedCommands.add(s.toLowerCase());
            }
        }
    }

    private boolean isProtected(String commandName) {
        return protectedCommands.contains(commandName.toLowerCase());
    }

    public boolean unregister(String commandName) {
        if (knownCommands == null) return false;
        commandName = commandName.toLowerCase();

        if (isProtected(commandName)) {
            plugin.getLogger().warning("[CommandBlocker] Rifiutato blocco di /" + commandName + " (comando protetto DistrictRP)");
            return false;
        }

        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Command> entry : knownCommands.entrySet()) {
            String key = entry.getKey().toLowerCase();
            Command cmd = entry.getValue();

            if (cmd instanceof PluginCommand pc && pc.getPlugin().equals(plugin)) {
                continue;
            }

            if (key.equals(commandName) || key.endsWith(":" + commandName)) {
                toRemove.add(entry.getKey());
                continue;
            }
            if (cmd.getName().equalsIgnoreCase(commandName)) {
                toRemove.add(entry.getKey());
                continue;
            }
            if (cmd.getAliases().contains(commandName)) {
                toRemove.add(entry.getKey());
            }
        }

        for (String key : toRemove) {
            knownCommands.remove(key);
            plugin.getLogger().info("[CommandBlocker] Rimosso comando: " + key);
        }

        if (!toRemove.isEmpty()) {
            blockedCommands.add(commandName);
            return true;
        }
        return false;
    }

    public int unregisterPlugin(String pluginName) {
        if (knownCommands == null) return 0;
        if (pluginName.equalsIgnoreCase(plugin.getName())) {
            plugin.getLogger().warning("[CommandBlocker] Rifiutato unregister di se stesso.");
            return 0;
        }
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, Command> entry : knownCommands.entrySet()) {
            Command cmd = entry.getValue();
            if (cmd instanceof PluginCommand pc) {
                if (pc.getPlugin().getName().equalsIgnoreCase(pluginName)) {
                    toRemove.add(entry.getKey());
                }
            }
        }

        for (String key : toRemove) {
            knownCommands.remove(key);
        }
        plugin.getLogger().info("[CommandBlocker] Rimossi " + toRemove.size()
                + " comandi del plugin " + pluginName);
        return toRemove.size();
    }

    public boolean override(String commandName) {
        if (isProtected(commandName)) {
            plugin.getLogger().warning("[CommandBlocker] Rifiutato override di /" + commandName + " (comando protetto DistrictRP)");
            return false;
        }
        boolean removed = unregister(commandName);
        overriddenCommands.add(commandName);
        return removed;
    }

    public void unregisterAll(String... commands) {
        for (String c : commands) unregister(c);
    }

    public void overrideAll(String... commands) {
        for (String c : commands) override(c);
    }

    public List<String> getBlockedCommands() {
        return Collections.unmodifiableList(blockedCommands);
    }

    public List<String> getOverriddenCommands() {
        return Collections.unmodifiableList(overriddenCommands);
    }

    public CommandMap getCommandMap() {
        return commandMap;
    }
}