package dev.breach.cherryCore.functions;

import dev.breach.cherryCore.CherryCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;

import java.lang.reflect.Field;
import java.util.*;

public class CommandBlocker {

    private final CherryCore plugin;
    private CommandMap commandMap;
    private Map<String, Command> knownCommands;

    private final List<String> blockedCommands = new ArrayList<>();
    private final List<String> overriddenCommands = new ArrayList<>();

    public CommandBlocker(CherryCore plugin) {
        this.plugin = plugin;
        try {
            Field cmField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            cmField.setAccessible(true);
            this.commandMap = (CommandMap) cmField.get(Bukkit.getServer());

            Field kcField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            kcField.setAccessible(true);
            
            this.knownCommands = (Map<String, Command>) kcField.get(this.commandMap);

            plugin.getLogger().info("[CommandBlocker] Inizializzato correttamente.");
        } catch (Throwable t) {
            plugin.getLogger().severe("[CommandBlocker] Errore inizializzazione: " + t.getMessage());
        }
    }

    public boolean unregister(String commandName) {
        if (knownCommands == null) return false;
        commandName = commandName.toLowerCase();

        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Command> entry : knownCommands.entrySet()) {
            String key = entry.getKey().toLowerCase();
            Command cmd = entry.getValue();

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
