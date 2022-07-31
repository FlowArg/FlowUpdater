package fr.flowarg.flowupdater.download;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowstringer.StringUtils;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.json.Downloadable;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowzipper.ZipUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/**
 * This class handles the downloading of vanilla files (client, assets, natives...).
 */
public class VanillaDownloader
{
    private final Path dir;
    private final ILogger logger;
    private final IProgressCallback callback;
    private final DownloadList downloadList;
    private final Path natives;
    private final Path assets;
    private final String vanillaJsonURL;

    /**
     * Construct a new VanillaDownloader object.
     * @param dir the installation directory.
     * @param flowUpdater the flow updater object.
     * @throws IOException if an I/O error occurred.
     */
    public VanillaDownloader(Path dir, @NotNull FlowUpdater flowUpdater) throws IOException
    {
        this.dir = dir;
        this.logger = flowUpdater.getLogger();
        this.callback = flowUpdater.getCallback();
        this.downloadList = flowUpdater.getDownloadList();

        this.natives = this.dir.resolve("natives");
        this.assets = this.dir.resolve("assets");
        this.vanillaJsonURL = flowUpdater.getVanillaVersion().getJsonURL();

        Files.createDirectories(this.dir.resolve("libraries"));
        Files.createDirectories(this.assets);
        Files.createDirectories(this.natives);

        this.downloadList.init();
    }

    /**
     * This method downloads calls other methods to download and verify all files.
     * @throws Exception if an I/O error occurred.
     */
    public void download() throws Exception
    {
        this.downloadLibraries();
        this.downloadAssets();
        this.extractNatives();

        this.logger.info("All vanilla files were successfully downloaded!");
    }

    private void downloadLibraries() throws Exception
    {
        this.logger.info("Checking library files...");
        this.callback.step(Step.DL_LIBS);

        if(this.vanillaJsonURL != null)
            this.downloadVanillaJson();

        for (Downloadable downloadable : this.downloadList.getDownloadableFiles())
        {
            final Path filePath = this.dir.resolve(downloadable.getName());

            if(Files.notExists(filePath) ||
                    !FileUtils.getSHA1(filePath).equalsIgnoreCase(downloadable.getSha1()) ||
                    Files.size(filePath) != downloadable.getSize())
            {
                IOUtils.download(this.logger, new URL(downloadable.getUrl()), filePath);
                this.callback.onFileDownloaded(filePath);
            }

            this.downloadList.incrementDownloaded(downloadable.getSize());
            this.callback.update(this.downloadList.getDownloadInfo());
        }
    }

    private void downloadVanillaJson() throws Exception
    {
        final Path vanillaJsonTarget = this.dir.resolve(this.vanillaJsonURL.substring(this.vanillaJsonURL.lastIndexOf('/') + 1));
        final String vanillaJsonResourceName = this.vanillaJsonURL.substring(this.vanillaJsonURL.lastIndexOf('/'));
        final String vanillaJsonPathUrl = StringUtils.empty(StringUtils.empty(this.vanillaJsonURL, "https://launchermeta.mojang.com/v1/packages/"), "https://piston-meta.mojang.com/v1/packages/");

        if(Files.notExists(vanillaJsonTarget) || !FileUtils.getSHA1(vanillaJsonTarget)
                .equals(StringUtils.empty(vanillaJsonPathUrl, vanillaJsonResourceName)))
            IOUtils.download(this.logger, new URL(this.vanillaJsonURL), vanillaJsonTarget);
    }

    private void extractNatives() throws IOException
    {
        boolean flag = false;
        final List<Path> existingNatives = FileUtils.list(this.natives);
        if (!existingNatives.isEmpty())
        {
            for (Path minecraftNative : FileUtils.list(this.natives)
                    .stream()
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .collect(Collectors.toList()))
            {
                final JarFile jarFile = new JarFile(minecraftNative.toFile());
                final Enumeration<? extends ZipEntry> entries = jarFile.entries();
                while (entries.hasMoreElements())
                {
                    final ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory() ||
                            entry.getName().endsWith(".git") ||
                            entry.getName().endsWith(".sha1") ||
                            entry.getName().contains("META-INF")) continue;

                    final Path flPath = this.natives.resolve(entry.getName());

                    if(Files.exists(flPath) && entry.getCrc() == FileUtils.getCRC32(flPath)) continue;

                    flag = true;
                    break;
                }
                jarFile.close();
                if (flag) break;
            }
        }

        if (flag)
        {
            this.logger.info("Extracting natives...");
            this.callback.step(Step.EXTRACT_NATIVES);

            final Stream<Path> natives = FileUtils.list(this.natives).stream();
            natives.filter(file -> !Files.isDirectory(file) && file.getFileName().toString().endsWith(".jar"))
                    .forEach(file -> {
                        try
                        {
                            ZipUtils.unzipJar(this.natives, file, "ignoreMetaInf");
                        } catch (IOException e)
                        {
                            this.logger.printStackTrace(e);
                        }
                    });
            natives.close();
        }

        final Stream<Path> natives = FileUtils.list(this.natives).stream();
        natives.forEach(path -> {
            try {
                if (path.getFileName().toString().endsWith(".git") || path.getFileName().toString().endsWith(".sha1")) Files.delete(path);
                else if(Files.isDirectory(path)) FileUtils.deleteDirectory(path);
            } catch (IOException e)
            {
                this.logger.printStackTrace(e);
            }
        });

        natives.close();
    }

    private void downloadAssets()
    {
        this.logger.info("Checking assets...");
        this.callback.step(Step.DL_ASSETS);

        IOUtils.executeAsyncForEach(this.downloadList.getDownloadableAssets(), Executors.newWorkStealingPool(), assetDownloadable -> {
            try
            {
                final Path downloadPath = this.assets.resolve(assetDownloadable.getFile());

                if (Files.notExists(downloadPath) || Files.size(downloadPath) != assetDownloadable.getSize())
                {
                    final Path localAssetPath = IOUtils.getMinecraftFolder().resolve("assets").resolve(assetDownloadable.getFile());
                    if (Files.exists(localAssetPath) && Files.size(localAssetPath) == assetDownloadable.getSize())
                        IOUtils.copy(this.logger, localAssetPath, downloadPath);
                    else
                    {
                        IOUtils.download(this.logger, new URL(assetDownloadable.getUrl()), downloadPath);
                        this.callback.onFileDownloaded(downloadPath);
                    }
                }

                this.downloadList.incrementDownloaded(assetDownloadable.getSize());
                this.callback.update(this.downloadList.getDownloadInfo());
            }
            catch (Exception e)
            {
                this.logger.printStackTrace(e);
            }
        });
    }
}
