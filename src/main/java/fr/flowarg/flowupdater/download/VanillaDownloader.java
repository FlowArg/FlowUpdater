package fr.flowarg.flowupdater.download;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.download.json.AssetDownloadable;
import fr.flowarg.flowupdater.download.json.Downloadable;
import fr.flowarg.flowupdater.utils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static fr.flowarg.flowio.FileUtils.*;

public class VanillaDownloader
{
    private final File dir;
    private final ILogger logger;
    private final IProgressCallback callback;
    private final DownloadInfos downloadInfos;
    private final boolean reExtractNatives;

    private final File natives;
    private final File assets;

    public VanillaDownloader(File dir, ILogger logger, IProgressCallback callback, DownloadInfos infos, boolean reExtractNatives)
    {
        this.dir = dir;
        this.logger = logger;
        this.callback = callback;
        this.downloadInfos = infos;
        this.reExtractNatives = reExtractNatives;

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
            {
                if (toDelete.getName().endsWith(".git") || toDelete.getName().endsWith(".sha1")) toDelete.delete();
            }
        }
    }

    private void downloadAssets()
    {
        this.logger.info("Checking assets...");
        this.callback.step(Step.DL_ASSETS);

        for (AssetDownloadable assetDownloadable : this.downloadInfos.getAssetDownloadables())
        {
            final File download = new File(this.assets, assetDownloadable.getFile());

            if (!download.exists() || getFileSizeBytes(download) != assetDownloadable.getSize())
                IOUtils.download(this.logger, assetDownloadable.getUrl(), download);

            this.downloadInfos.incrementDownloaded();
            this.callback.update(this.downloadInfos.getDownloaded(), this.downloadInfos.getTotalToDownload());
        }
    }
}
