package dev.breach.DistrictRP.functions.installer;

import dev.breach.DistrictRP.DistrictRP;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AutoInstaller {

    private final DistrictRP plugin;
    private final File pluginsFolder;

    public AutoInstaller(DistrictRP plugin) {
        this.plugin = plugin;
        this.pluginsFolder = plugin.getDataFolder().getParentFile();
    }

    public boolean runChecks() {
        if (!plugin.getConfig().getBoolean("auto-installer.enabled", true)) {
            plugin.getLogger().info("[AutoInstaller] Disabilitato in config.");
            return false;
        }
        if (!plugin.getConfig().getBoolean("auto-installer.check-on-startup", true)) {
            return false;
        }

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("auto-installer.plugins");
        if (sec == null) {
            plugin.getLogger().info("[AutoInstaller] Nessun plugin da controllare.");
            return false;
        }

        List<String> installed = new ArrayList<>();
        List<String> alreadyPresent = new ArrayList<>();
        List<String> failed = new ArrayList<>();

        for (String key : sec.getKeys(false)) {
            ConfigurationSection p = sec.getConfigurationSection(key);
            if (p == null) continue;
            if (!p.getBoolean("enabled", true)) continue;

            String pluginName = p.getString("plugin-name", key);
            String url = p.getString("url", "");
            String fileName = p.getString("file-name", key + ".jar");
            boolean required = p.getBoolean("required", false);
            List<String> aliases = p.getStringList("file-aliases");

            if (isPluginPresent(pluginName, fileName, aliases)) {
                alreadyPresent.add(pluginName);
                plugin.getLogger().info("[AutoInstaller] ✔ " + pluginName + " già presente, skip.");
                continue;
            }

            if (url.isEmpty()) {
                plugin.getLogger().warning("[AutoInstaller] ✖ " + pluginName + " manca URL, skip.");
                if (required) failed.add(pluginName);
                continue;
            }

            plugin.getLogger().info("[AutoInstaller] ⬇ Download " + pluginName + " da " + url);
            File dest = new File(pluginsFolder, fileName);

            boolean ok = downloadFile(url, dest);
            if (ok) {
                installed.add(pluginName);
                plugin.getLogger().info("[AutoInstaller] ✔ " + pluginName + " scaricato (" + dest.length() / 1024 + " KB)");
            } else {
                failed.add(pluginName);
                plugin.getLogger().warning("[AutoInstaller] ✖ Download fallito: " + pluginName);
            }
        }

        if (!alreadyPresent.isEmpty()) {
            plugin.getLogger().info("[AutoInstaller] Plugin già presenti (" + alreadyPresent.size() + "): "
                    + String.join(", ", alreadyPresent));
        }

        if (!installed.isEmpty()) {
            plugin.getLogger().warning("");
            plugin.getLogger().warning("  [AutoInstaller] " + installed.size() + " plugin installati:");
            for (String p : installed) plugin.getLogger().warning("   ➜ " + p);
            plugin.getLogger().warning(" ");
            plugin.getLogger().warning("  RIAVVIA IL SERVER per completare l'installazione!");
            plugin.getLogger().warning(" ");
            plugin.getLogger().warning("");

            if (!failed.isEmpty()) {
                plugin.getLogger().severe("[AutoInstaller] Fallimenti: " + String.join(", ", failed));
            }

            if (plugin.getConfig().getBoolean("auto-installer.shutdown-after-install", true)) {
                long delay = plugin.getConfig().getLong("auto-installer.shutdown-delay-seconds", 10L);
                plugin.getLogger().warning("[AutoInstaller] Shutdown automatico in " + delay + " secondi...");
                Bukkit.getScheduler().runTaskLater(plugin, Bukkit::shutdown, 20L * delay);
                return true;
            }
        }

        if (installed.isEmpty() && !failed.isEmpty()) {
            plugin.getLogger().severe("[AutoInstaller] Fallimenti: " + String.join(", ", failed));
        }

        return false;
    }

    private boolean isPluginPresent(String pluginName, String fileName, List<String> aliases) {
        if (Bukkit.getPluginManager().getPlugin(pluginName) != null) return true;
        if (!pluginsFolder.exists() || !pluginsFolder.isDirectory()) return false;

        File[] files = pluginsFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".jar"));
        if (files == null) return false;

        String pluginNameLower = pluginName.toLowerCase(Locale.ROOT);
        String fileNameLower = fileName.toLowerCase(Locale.ROOT);

        for (File f : files) {
            String n = f.getName().toLowerCase(Locale.ROOT);
            if (n.equals(fileNameLower)) return true;
            if (n.contains(pluginNameLower)) return true;
            if (aliases != null) {
                for (String alias : aliases) {
                    if (alias == null || alias.isEmpty()) continue;
                    if (n.contains(alias.toLowerCase(Locale.ROOT))) return true;
                }
            }
        }
        return false;
    }

    private boolean downloadFile(String urlStr, File dest) {
        return downloadFile(urlStr, dest, 0);
    }

    private boolean downloadFile(String urlStr, File dest, int redirectCount) {
        if (redirectCount > 5) {
            plugin.getLogger().warning("[AutoInstaller] Troppi redirect per " + urlStr);
            return false;
        }
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Linux; DistrictRP-AutoInstaller/1.0)");

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP
                    || code == 303 || code == 307 || code == 308) {
                String newUrl = conn.getHeaderField("Location");
                conn.disconnect();
                if (newUrl == null) return false;
                if (!newUrl.startsWith("http")) {
                    URL base = new URL(urlStr);
                    newUrl = base.getProtocol() + "://" + base.getHost() + newUrl;
                }
                return downloadFile(newUrl, dest, redirectCount + 1);
            }

            if (code >= 400) {
                plugin.getLogger().warning("[AutoInstaller] HTTP " + code + " per " + urlStr);
                return false;
            }

            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[8192];
                int n;
                long total = 0;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    total += n;
                }
                if (total < 1024) {
                    plugin.getLogger().warning("[AutoInstaller] File sospetto (troppo piccolo): " + total + " bytes");
                    dest.delete();
                    return false;
                }
                if (!isValidJar(dest)) {
                    plugin.getLogger().warning("[AutoInstaller] File scaricato non è un JAR valido: " + dest.getName());
                    dest.delete();
                    return false;
                }
                return true;
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[AutoInstaller] Errore download " + urlStr + ": " + t.getMessage());
            if (dest.exists()) dest.delete();
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private boolean isValidJar(File file) {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
            byte[] magic = new byte[4];
            int read = fis.read(magic);
            if (read < 4) return false;
            return magic[0] == 0x50 && magic[1] == 0x4B;
        } catch (Throwable t) {
            return false;
        }
    }
}