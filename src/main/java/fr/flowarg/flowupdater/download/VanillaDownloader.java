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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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

        this.logger.info("All vanilla files are successfully downloaded!");
    }

    private void downloadLibraries() throws Exception
    {
        this.logger.info("Checking library files...");
        this.callback.step(Step.DL_LIBS);

        final Path vanillaJsonTarget = this.dir.resolve(this.vanillaJsonURL.substring(this.vanillaJsonURL.lastIndexOf('/') + 1));

        if(Files.notExists(vanillaJsonTarget) || !FileUtils.getSHA1(vanillaJsonTarget).equals(StringUtils.empty(StringUtils.empty(this.vanillaJsonURL, "https://launchermeta.mojang.com/v1/packages/"), this.vanillaJsonURL.substring(this.vanillaJsonURL.lastIndexOf('/')))))
            IOUtils.download(this.logger, new URL(this.vanillaJsonURL), vanillaJsonTarget);

        for (Downloadable downloadable : this.downloadList.getDownloadableFiles())
        {
            final Path filePath = this.dir.resolve(downloadable.getName());

            if(Files.notExists(filePath) || !FileUtils.getSHA1(filePath).equalsIgnoreCase(downloadable.getSha1()) || FileUtils.getFileSizeBytes(filePath) != downloadable.getSize())
            {
                IOUtils.download(this.logger, new URL(downloadable.getUrl()), filePath);
                this.callback.onFileDownloaded(filePath);
            }

            this.downloadList.incrementDownloaded(downloadable.getSize());
            this.callback.update(this.downloadList.getDownloadedBytes(), this.downloadList.getTotalToDownloadBytes());
        }
    }

    private void extractNatives() throws IOException
    {
        boolean flag = false;
        final List<Path> existingNatives = Files.list(this.natives).collect(Collectors.toList());
        if (!existingNatives.isEmpty())
        {
            for (Path minecraftNative : Files.list(this.natives).filter(path -> path.getFileName().toString().endsWith(".jar")).collect(Collectors.toList()))
            {
                final JarFile jarFile = new JarFile(minecraftNative.toFile());
                final Enumeration<? extends ZipEntry> entries = jarFile.entries();
                while (entries.hasMoreElements())
                {
                    final ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory() || entry.getName().endsWith(".git") || entry.getName().endsWith(".sha1") || entry.getName().contains("META-INF")) continue;

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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void downloadAssets()
    {
        this.logger.info("Checking assets...");
        this.callback.step(Step.DL_ASSETS);

        final ExecutorService executorService = Executors.newCachedThreadPool();

        this.downloadList.getDownloadableAssets().forEach(assetDownloadable -> executorService.submit(() -> {
            try
            {
                final Path downloadPath = this.assets.resolve(assetDownloadable.getFile());

                if (Files.notExists(downloadPath) || FileUtils.getFileSizeBytes(downloadPath) != assetDownloadable.getSize())
                {
                    final Path localAssetPath = IOUtils.getMinecraftFolder().resolve("assets").resolve(assetDownloadable.getFile());
                    if (Files.exists(localAssetPath) && FileUtils.getFileSizeBytes(localAssetPath) == assetDownloadable.getSize())
                        IOUtils.copy(this.logger, localAssetPath, downloadPath);
                    else
                    {
                        IOUtils.download(this.logger, new URL(assetDownloadable.getUrl()), downloadPath);
                        this.callback.onFileDownloaded(downloadPath);
                    }
                }

                this.downloadList.incrementDownloaded(assetDownloadable.getSize());
                this.callback.update(this.downloadList.getDownloadedBytes(), this.downloadList.getTotalToDownloadBytes());
            }
            catch (Exception e)
            {
                this.logger.printStackTrace(e);
            }
        }));

        try
        {
            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e)
        {
            this.logger.printStackTrace(e);
        }
    }
}
