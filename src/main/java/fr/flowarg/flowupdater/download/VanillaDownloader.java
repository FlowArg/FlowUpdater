package fr.flowarg.flowupdater.download;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.download.json.AssetDownloadable;
import fr.flowarg.flowupdater.download.json.Downloadable;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.UpdaterOptions;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static fr.flowarg.flowio.FileUtils.*;

public class VanillaDownloader
{
    private final File dir;
    private final ILogger logger;
    private final IProgressCallback callback;
    private final DownloadInfos downloadInfos;
    private final boolean reExtractNatives;
    private final int threadsForAssets;

    private final File natives;
    private final File assets;

    public VanillaDownloader(File dir, ILogger logger, IProgressCallback callback, DownloadInfos infos, UpdaterOptions options)
    {
        this.dir = dir;
        this.logger = logger;
        this.callback = callback;
        this.downloadInfos = infos;
        this.reExtractNatives = options.isReExtractNatives();
        this.threadsForAssets = options.getNmbrThreadsForAssets();

        this.natives = new File(this.dir, "natives/");
        this.assets = new File(this.dir, "assets/");

        new File(this.dir, "libraries/").mkdirs();
        this.dir.mkdirs();
        this.assets.mkdirs();
        this.natives.mkdirs();
        this.downloadInfos.init();
    }

    public void download() throws Exception
    {
        this.checkAllLibraries();
        this.downloadAssets();
        this.extractNatives();

        this.logger.info("All vanilla files are successfully downloaded !");
    }

    private void checkAllLibraries() throws Exception
    {
        this.logger.info("Checking library files...");
        this.callback.step(Step.DL_LIBS);

        if (this.natives.listFiles() != null)
        {
            for (File files : this.natives.listFiles())
            {
                if (files.isDirectory()) FileUtils.deleteDirectory(files);
            }
        }

        for (Downloadable downloadable : this.downloadInfos.getLibraryDownloadables())
        {
            final File file = new File(this.dir, downloadable.getName());

            if(!file.exists() || !getSHA1(file).equals(downloadable.getSha1()) || getFileSizeBytes(file) != downloadable.getSize())
                IOUtils.download(this.logger, new URL(downloadable.getUrl()), file);

            this.downloadInfos.incrementDownloaded();
            this.callback.update(this.downloadInfos.getDownloaded(), this.downloadInfos.getTotalToDownload());
        }
    }

    private void extractNatives() throws IOException
    {
        if (this.natives.listFiles() != null)
        {
            boolean flag = true;
            for (File minecraftNative : this.natives.listFiles())
            {
                if (minecraftNative.getName().endsWith(".so") || minecraftNative.getName().endsWith(".dylib") || minecraftNative.getName().endsWith(".dll"))
                {
                    flag = false;
                    break;
                }
            }
            if (this.reExtractNatives || flag)
            {
                this.logger.info("Extracting natives...");
                this.callback.step(Step.EXTRACT_NATIVES);
                for (File minecraftNative : this.natives.listFiles())
                {
                    if (!minecraftNative.isDirectory() && minecraftNative.getName().endsWith(".jar"))
                        unzipJar(this.natives.getAbsolutePath(), minecraftNative.getAbsolutePath(), "ignoreMetaInf");
                }
            }

            for (File toDelete : this.natives.listFiles())
                if (toDelete.getName().endsWith(".git") || toDelete.getName().endsWith(".sha1")) toDelete.delete();
        }
    }

    private void downloadAssets()
    {
        this.logger.info("Checking assets...");
        this.callback.step(Step.DL_ASSETS);
        final ThreadPoolExecutor threadPool = (ThreadPoolExecutor)Executors.newFixedThreadPool(this.threadsForAssets);
        for (int i = 0; i < threadPool.getMaximumPoolSize(); i++)
        {
            threadPool.submit(() -> {
                try {
                    AssetDownloadable assetDownloadable;
                    while ((assetDownloadable = this.downloadInfos.getAssetDownloadables().poll()) != null)
                    {
                        final File download = new File(this.assets, assetDownloadable.getFile());

                        if (!download.exists() || getFileSizeBytes(download) != assetDownloadable.getSize())
                        {
                            final File localAsset = new File(IOUtils.getMinecraftFolder(), assetDownloadable.getFile());
                            if(localAsset.exists() && getFileSizeBytes(localAsset) == assetDownloadable.getSize()) IOUtils.copy(this.logger, localAsset, download);
                            else IOUtils.download(this.logger, assetDownloadable.getUrl(), download);
                        }

                        this.downloadInfos.incrementDownloaded();
                        this.callback.update(this.downloadInfos.getDownloaded(), this.downloadInfos.getTotalToDownload());
                    }
                } catch (Exception e)
                {
                    this.logger.printStackTrace(e);
                }
            });
        }
        try
        {
            threadPool.shutdown();
            threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e)
        {
            this.logger.printStackTrace(e);
        }
    }
}
