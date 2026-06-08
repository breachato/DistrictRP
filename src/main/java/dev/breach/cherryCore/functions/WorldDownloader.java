package dev.breach.cherryCore.functions;

import dev.breach.cherryCore.CherryCore;
import org.bukkit.Bukkit;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Scarica un archivio (zip/tar.gz/rar) da un URL, lo estrae nella cartella del server,
 * rinominandolo come specificato.
 *
 * Supporto:
 *   - .zip  (nativo)
 *   - .tar.gz / .tgz (richiede Apache Commons Compress - opzionale)
 *   - .rar  (richiede junrar - opzionale)
 *
 * Per semplicita, di base supportiamo solo ZIP. tar.gz e rar daranno errore se non
 * sono presenti le librerie.
 */
public class WorldDownloader {

    private final CherryCore plugin;

    public WorldDownloader(CherryCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Esegue il download e l'estrazione in modo asincrono.
     * @param url        URL dell'archivio
     * @param targetName nome finale della cartella mondo nel server
     * @param onSuccess  callback su success (riceve true se OK)
     * @param onError    callback su errore (riceve il messaggio)
     */
    public void downloadAndExtract(String url, String targetName,
                                   Consumer<Boolean> onSuccess, Consumer<String> onError) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                File serverRoot = Bukkit.getWorldContainer();
                File target = new File(serverRoot, targetName);
                if (target.exists()) {
                    runSync(() -> onError.accept("La cartella '" + targetName + "' esiste gia nel server."));
                    return;
                }

                // Scarica in temp
                File tmpDir = new File(plugin.getDataFolder(), "tmp");
                if (!tmpDir.exists()) tmpDir.mkdirs();

                String lower = url.toLowerCase();
                String ext;
                if (lower.endsWith(".zip")) ext = "zip";
                else if (lower.endsWith(".tar.gz") || lower.endsWith(".tgz")) ext = "targz";
                else if (lower.endsWith(".rar")) ext = "rar";
                else {
                    runSync(() -> onError.accept("Formato non riconosciuto. Usa .zip, .tar.gz o .rar"));
                    return;
                }

                File downloaded = new File(tmpDir, "download_" + System.currentTimeMillis() + "." + ext);
                plugin.getLogger().info("[Mondo] Download da " + url);

                URLConnection conn = new URL(url).openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(60000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 FluentCore");

                try (InputStream in = conn.getInputStream();
                     FileOutputStream out = new FileOutputStream(downloaded)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                }

                plugin.getLogger().info("[Mondo] Download completato. Estrazione in corso...");

                // Estrazione
                File extractDir = new File(tmpDir, "extract_" + System.currentTimeMillis());
                extractDir.mkdirs();

                switch (ext) {
                    case "zip":
                        unzip(downloaded, extractDir);
                        break;
                    case "targz":
                        runSync(() -> onError.accept("Formato .tar.gz richiede libreria aggiuntiva non installata. Usa .zip"));
                        return;
                    case "rar":
                        runSync(() -> onError.accept("Formato .rar richiede libreria aggiuntiva non installata. Usa .zip"));
                        return;
                }

                // Trova la cartella con level.dat dentro l'estrazione
                File worldFolder = findWorldFolder(extractDir);
                if (worldFolder == null) {
                    runSync(() -> onError.accept("Nessun mondo Minecraft trovato nell'archivio (manca level.dat)."));
                    return;
                }

                // Sposta nella root del server con il nome corretto
                moveDirectory(worldFolder, target);

                // Pulisci temp
                deleteRecursive(downloaded);
                deleteRecursive(extractDir);

                plugin.getLogger().info("[Mondo] Mondo '" + targetName + "' importato con successo!");
                runSync(() -> onSuccess.accept(true));

            } catch (Exception ex) {
                plugin.getLogger().severe("[Mondo] Errore: " + ex.getMessage());
                ex.printStackTrace();
                runSync(() -> onError.accept("Errore: " + ex.getMessage()));
            }
        });
    }

    /**
     * Importa una cartella locale dal disco copiandola nella server root.
     */
    public void importLocalFolder(String absolutePath, String targetName,
                                  Consumer<Boolean> onSuccess, Consumer<String> onError) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                File src = new File(absolutePath);
                if (!src.exists() || !src.isDirectory()) {
                    runSync(() -> onError.accept("Cartella sorgente non trovata: " + absolutePath));
                    return;
                }
                if (!new File(src, "level.dat").exists()) {
                    runSync(() -> onError.accept("La cartella non contiene level.dat (non e un mondo Minecraft)."));
                    return;
                }
                File target = new File(Bukkit.getWorldContainer(), targetName);
                if (target.exists()) {
                    runSync(() -> onError.accept("La cartella '" + targetName + "' esiste gia."));
                    return;
                }
                copyDirectory(src, target);
                runSync(() -> onSuccess.accept(true));
            } catch (Exception ex) {
                ex.printStackTrace();
                runSync(() -> onError.accept("Errore: " + ex.getMessage()));
            }
        });
    }

    // ============================================================
    // UTILITY INTERNE
    // ============================================================
    private void runSync(Runnable r) {
        Bukkit.getScheduler().runTask(plugin, r);
    }

    private void unzip(File zipFile, File destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buf = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                File out = new File(destDir, entry.getName());
                // Prevent zip slip
                if (!out.getCanonicalPath().startsWith(destDir.getCanonicalPath())) {
                    throw new IOException("Zip slip rilevato: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    out.mkdirs();
                } else {
                    out.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        int n;
                        while ((n = zis.read(buf)) > 0) fos.write(buf, 0, n);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * Cerca ricorsivamente una cartella che contenga level.dat.
     */
    private File findWorldFolder(File dir) {
        if (dir == null || !dir.isDirectory()) return null;
        if (new File(dir, "level.dat").exists()) return dir;
        File[] children = dir.listFiles();
        if (children == null) return null;
        for (File c : children) {
            if (c.isDirectory()) {
                File found = findWorldFolder(c);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void moveDirectory(File source, File dest) throws IOException {
        if (!source.renameTo(dest)) {
            // fallback: copia + cancella
            copyDirectory(source, dest);
            deleteRecursive(source);
        }
    }

    private void copyDirectory(File source, File dest) throws IOException {
        if (source.isDirectory()) {
            if (!dest.exists()) dest.mkdirs();
            String[] children = source.list();
            if (children != null) {
                for (String c : children) {
                    copyDirectory(new File(source, c), new File(dest, c));
                }
            }
        } else {
            Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteRecursive(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File c : children) deleteRecursive(c);
        }
        f.delete();
    }
}