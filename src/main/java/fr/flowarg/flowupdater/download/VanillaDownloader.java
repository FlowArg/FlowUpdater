package fr.flowarg.flowupdater.download;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.json.AssetDownloadable;
import fr.flowarg.flowupdater.download.json.Downloadable;
import fr.flowarg.flowupdater.utils.IOUtils;

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

    public VanillaDownloader(File dir, FlowUpdater flowUpdater)
    {
        this.dir = dir;
        this.logger = flowUpdater.getLogger();
        this.callback = flowUpdater.getCallback();
        this.downloadInfos = flowUpdater.getDownloadInfos();
        this.reExtractNatives = flowUpdater.getUpdaterOptions().isReExtractNatives();
        this.threadsForAssets = flowUpdater.getUpdaterOptions().getNmbrThreadsForAssets();

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

        for (File files : FileUtils.list(this.natives))
        {
            if (files.isDirectory()) FileUtils.deleteDirectory(files);
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
        boolean flag = true;
        for (File minecraftNative : FileUtils.list(this.natives))
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
            for (File minecraftNative : FileUtils.list(this.natives))
            {
                if (!minecraftNative.isDirectory() && minecraftNative.getName().endsWith(".jar"))
                    unzipJar(this.natives.getAbsolutePath(), minecraftNative.getAbsolutePath(), "ignoreMetaInf");
            }
        }

        for (File toDelete : FileUtils.list(this.natives))
            if (toDelete.getName().endsWith(".git") || toDelete.getName().endsWith(".sha1")) toDelete.delete();
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
                            final File localAsset = new File(IOUtils.getMinecraftFolder(), "assets/" + assetDownloadable.getFile());
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
