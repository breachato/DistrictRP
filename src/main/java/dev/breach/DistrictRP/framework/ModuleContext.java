package dev.breach.DistrictRP.framework;

import dev.breach.DistrictRP.DistrictRP;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ModuleContext {

    private final DistrictRP core;
    private final Plugin owningPlugin;
    private final DistrictModule module;
    private final File dataFolder;

    private FileConfiguration config;
    private File configFile;
    private ModuleMessages messages;
    private File messagesFile;

    private volatile boolean enabled = false;

    public ModuleContext(DistrictRP core, Plugin owningPlugin, DistrictModule module) {
        this.core = core;
        this.owningPlugin = owningPlugin;
        this.module = module;
        this.dataFolder = new File(core.getDataFolder(), "modules" + File.separator + module.id());
        if (!dataFolder.exists()) dataFolder.mkdirs();
        loadFiles();
    }

    private void loadFiles() {
        this.configFile = new File(dataFolder, "config.yml");
        this.messagesFile = new File(dataFolder, "messages.yml");

        if (!configFile.exists()) copyFromJar("config.yml", configFile);
        if (!messagesFile.exists()) copyFromJar("messages.yml", messagesFile);

        if (!configFile.exists()) {
            try { configFile.createNewFile(); } catch (IOException ignored) {}
        }
        if (!messagesFile.exists()) {
            try { messagesFile.createNewFile(); } catch (IOException ignored) {}
        }

        this.config = YamlConfiguration.loadConfiguration(configFile);

        String prefix = core.getConfig().getString("prefix", "");
        this.messages = new ModuleMessages(messagesFile, prefix);
    }

    private void copyFromJar(String name, File dest) {
        String jarPath = "modules/" + module.id() + "/" + name;
        try (InputStream in = owningPlugin.getResource(jarPath)) {
            if (in == null) return;
            dest.getParentFile().mkdirs();
            java.nio.file.Files.copy(in, dest.toPath());
        } catch (Throwable t) {
            core.getLogger().warning("[Module " + module.id() + "] copyFromJar '" + name + "': " + t.getMessage());
        }
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(configFile);
        this.messages.reload();
    }

    public void saveConfig() {
        try { config.save(configFile); }
        catch (IOException e) { core.getLogger().warning("[Module " + module.id() + "] saveConfig: " + e.getMessage()); }
    }

    public DistrictRP core() { return core; }
    public Plugin owningPlugin() { return owningPlugin; }
    public DistrictModule module() { return module; }
    public File dataFolder() { return dataFolder; }
    public FileConfiguration config() { return config; }
    public ModuleMessages messages() { return messages; }

    public boolean isEnabled() { return enabled; }
    void setEnabled(boolean e) { this.enabled = e; }
}