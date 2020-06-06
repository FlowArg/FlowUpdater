package fr.flowarg.flowupdater.minecraft.versions.download;

import static fr.flowarg.flowio.FileUtils.getFileSizeBytes;
import static fr.flowarg.flowio.FileUtils.getSHA1;
import static fr.flowarg.flowio.FileUtils.unzipJar;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.Sets;

import fr.flowarg.flowlogger.Logger;
import fr.flowarg.flowupdater.minecraft.versions.download.assets.AssetDownloadable;

public class VanillaDownloader
{
    private static final Set<Downloadable> LIBRARY_DOWNLOADABLE = Sets.newHashSet();
    private static final Set<AssetDownloadable> ASSET_DOWNLOADABLES = Sets.newHashSet();
    private final File natives;
    private final File assets;
    private final File libraries;
    private final Logger logger;
    private File dir;
    private IProgressCallback callback;

    public VanillaDownloader(File dir, Logger logger, IProgressCallback callback)
    {
        this.dir       = dir;
        this.natives   = new File(this.dir, "/natives/");
        this.assets    = new File(this.dir, "/assets/");
        this.libraries = new File(this.dir, "/libraries/");
        this.logger    = logger;
        this.callback  = callback;
        
        this.dir.mkdirs();
        this.assets.mkdirs();
        this.natives.mkdirs();
        this.libraries.mkdirs();
    }

    public static Set<Downloadable> getLibraryDownloadable()
    {
        return LIBRARY_DOWNLOADABLE;
    }
    public static Set<AssetDownloadable> getAssetDownloadables()
    {
        return ASSET_DOWNLOADABLES;
    }

    public void download(boolean downloadServer) throws IOException
    {
        this.logger.info("[Downloader] Checking library files...");
        this.callback.step(Step.DL_LIBS);
        this.checkAllLibraries(downloadServer);

        this.logger.info("[Downloader] Extracting natives...");
        this.callback.step(Step.EXTRACT_NATIVES);
        this.extractNatives();

        this.logger.info("[Downloader] Checking assets...");
        this.callback.step(Step.DL_ASSETS);
        this.downloadAssets();

        LIBRARY_DOWNLOADABLE.clear();
        ASSET_DOWNLOADABLES.clear();
        this.logger.info("[Downloader] All files are successfully downloaded !");
    }

    private void checkAllLibraries(boolean downloadServer) throws IOException
    {
        if (this.natives.listFiles() != null)
        {
            for (File files : this.natives.listFiles())
            {
                if (files.getName().endsWith(".dll") || files.getName().endsWith(".so") || files.getName().endsWith(".dylib"))
                {
                    if (!files.delete())
                        files.deleteOnExit();
                }
                if (files.isDirectory())
                    files.delete();
            }
        }

        for (Downloadable downloadable : LIBRARY_DOWNLOADABLE)
        {
            if (downloadable.getName().equals("server.jar") && !downloadServer) continue;
            else
            {
                final File file = new File(this.dir, downloadable.getName());

                if (file.exists())
                {
                    if (!Objects.requireNonNull(getSHA1(file)).equals(downloadable.getSha1()) || getFileSizeBytes(file) != downloadable.getSize())
                    {
                        file.delete();
                        this.download(new URL(downloadable.getUrl()), file);
                    }
                }
                else
                {
                    this.download(new URL(downloadable.getUrl()), file);
                }
            }
        }
    }

    private void download(@NotNull URL in, @NotNull File out) throws IOException
    {
        this.logger.info(String.format("[Downloader] Downloading %s from %s...", out.getName(), in.toExternalForm()));
        out.getParentFile().mkdirs();
        Files.copy(in.openStream(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private void extractNatives() throws IOException
    {
        for (File minecraftNative : Objects.requireNonNull(this.natives.listFiles()))
        {
            if (!minecraftNative.isDirectory() && minecraftNative.getName().endsWith(".jar"))
                unzipJar(this.natives.getAbsolutePath(), minecraftNative.getAbsolutePath());
        }

        for (File toDelete : Objects.requireNonNull(this.natives.listFiles()))
        {
            String name = toDelete.getName().substring(toDelete.getName().lastIndexOf('.') + 1);
            if (name.equals("git") || name.equals("sha1")) toDelete.delete();
        }
    }

    private void downloadAssets()
    {
        for (AssetDownloadable assetDownloadable : ASSET_DOWNLOADABLES)
        {
            try
            {
                final File download = new File(this.assets, assetDownloadable.getFile());

                if (download.exists())
                {
                    if (getFileSizeBytes(download) != assetDownloadable.getSize())
                    {
                        download.delete();
                        this.download(assetDownloadable.getUrl(), download);
                    }
                } else this.download(assetDownloadable.getUrl(), download);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
