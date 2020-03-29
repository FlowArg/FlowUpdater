package fr.flowarg.flowupdater.minecraft.versions.download;

import com.google.common.collect.Sets;
import fr.flowarg.flowupdater.minecraft.FlowArgMinecraftUpdater;
import fr.flowarg.flowupdater.minecraft.versions.download.assets.AssetDownloadable;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Set;

import static fr.flowarg.flowio.FileUtils.*;

@SuppressWarnings("ALL")
public class Downloader
{
    private static final Set<Downloadable> LIBRARY_DOWNLOADABLE = Sets.newHashSet();
    private static final Set<AssetDownloadable> ASSET_DOWNLOADABLES = Sets.newHashSet();
    private final File libraries;
    private final File natives;
    private final File assets;
    private File dir;

    public Downloader(File dir)
    {
        this.dir       = dir;
        this.libraries = new File(this.dir, "/libraries/");
        this.natives   = new File(this.dir, "/natives/");
        this.assets    = new File(this.dir, "/assets/");
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
        FlowArgMinecraftUpdater.getLogger().info("[Downloader] Checking library files...");
        this.checkAllLibraries(downloadServer);

        this.extractNatives();

        FlowArgMinecraftUpdater.getLogger().info("[Downloader] Checking assets...");
        this.downloadAssets();

        LIBRARY_DOWNLOADABLE.clear();
        ASSET_DOWNLOADABLES.clear();
        FlowArgMinecraftUpdater.getLogger().info("[Downloader] All files are successfully downloaded !");
    }

    private void checkAllLibraries(boolean downloadServer) throws IOException
    {
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
                } else
                {
                    this.download(new URL(downloadable.getUrl()), file);
                }
            }
        }
    }

    private void download(@NotNull URL in, @NotNull File out) throws IOException
    {
        FlowArgMinecraftUpdater.getLogger().info(String.format("[Downloader] Downloading %s from %s...", out.getName(), in.toExternalForm()));
        FileUtils.copyURLToFile(in, out);
    }

    private void extractNatives() throws IOException
    {
        FlowArgMinecraftUpdater.getLogger().info("[Downloader] Extracting natives...");
        for (File minecraftNative : Objects.requireNonNull(this.natives.listFiles())) unzipJar(this.natives.getAbsolutePath(), minecraftNative.getAbsolutePath());

        for (File toDelete : Objects.requireNonNull(this.natives.listFiles()))
        {
            String name = toDelete.getName().substring(toDelete.getName().lastIndexOf('.') + 1);
            if (name.equals("git") || name.equals("sha1")) toDelete.delete();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() ->
        {
            try
            {
                FileUtils.cleanDirectory(this.natives);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }));
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
