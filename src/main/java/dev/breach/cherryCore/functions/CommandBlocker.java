package dev.breach.cherryCore.functions;

import dev.breach.cherryCore.CherryCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Permette di:
 *  - Rimuovere completamente comandi registrati da altri plugin
 *  - "Rubare" un comando per assegnarlo al nostro plugin
 *  - Bloccare comandi specifici (ritornano "comando sconosciuto")
 *
 * Va chiamato in onEnable() DOPO che tutti i plugin sono caricati.
 */
public class CommandBlocker {

    private final CherryCore plugin;
    private CommandMap commandMap;
    private Map<String, Command> knownCommands;

    // Lista di comandi che vogliamo bloccare o sovrascrivere
    private final List<String> blockedCommands = new ArrayList<>();
    private final List<String> overriddenCommands = new ArrayList<>();

    public CommandBlocker(CherryCore plugin) {
        this.plugin = plugin;
        try {
            // Ottieni la CommandMap del server via reflection
            Field cmField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            cmField.setAccessible(true);
            this.commandMap = (CommandMap) cmField.get(Bukkit.getServer());

            // Ottieni la mappa interna dei comandi
            Field kcField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            kcField.setAccessible(true);
            //noinspection unchecked
            this.knownCommands = (Map<String, Command>) kcField.get(this.commandMap);

            plugin.getLogger().info("[CommandBlocker] Inizializzato correttamente.");
        } catch (Throwable t) {
            plugin.getLogger().severe("[CommandBlocker] Errore inizializzazione: " + t.getMessage());
        }
    }

    /**
     * Rimuove COMPLETAMENTE un comando dal server.
     * Se altri plugin lo avevano registrato, viene cancellato.
     * Da chiamare PRIMA di registrare il proprio.
     *
     * @param commandName nome del comando (senza /)
     * @return true se rimosso almeno una variante
     */
    public boolean unregister(String commandName) {
        if (knownCommands == null) return false;
        commandName = commandName.toLowerCase();

        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Command> entry : knownCommands.entrySet()) {
            String key = entry.getKey().toLowerCase();
            Command cmd = entry.getValue();

            // Match diretto sul nome o sugli alias
            if (key.equals(commandName) || key.endsWith(":" + commandName)) {
                toRemove.add(entry.getKey());
                continue;
            }
            // Match sul nome reale del comando
            if (cmd.getName().equalsIgnoreCase(commandName)) {
                toRemove.add(entry.getKey());
                continue;
            }
            // Match sugli alias
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

    /**
     * Rimuove tutti i comandi di un plugin specifico.
     * Es: blockPlugin("Mohist") rimuove ogni cosa registrata da Mohist.
     */
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

    /**
     * Sovrascrive un comando di un altro plugin con la nostra versione.
     * Usalo PRIMA di registrare il TUO comando con setExecutor.
     * Esempio:
     *   blocker.override("warp");  // rimuove /warp di altri plugin
     *   getCommand("warp").setExecutor(new WarpCommand(this));
     */
    public boolean override(String commandName) {
        boolean removed = unregister(commandName);
        overriddenCommands.add(commandName);
        return removed;
    }

    /**
     * Rimuove più comandi in un colpo solo.
     */
    public void unregisterAll(String... commands) {
        for (String c : commands) unregister(c);
    }

    /**
     * Sovrascrive più comandi in un colpo solo.
     */
    public void overrideAll(String... commands) {
        for (String c : commands) override(c);
    }

    // ============================================================
    // Getters
    // ============================================================
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