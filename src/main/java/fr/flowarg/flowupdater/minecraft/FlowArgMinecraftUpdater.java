package fr.flowarg.flowupdater.minecraft;

import fr.flowarg.flowlogger.Logger;
import fr.flowarg.flowupdater.minecraft.versions.IVersion;
import fr.flowarg.flowupdater.minecraft.versions.download.Downloader;
import fr.flowarg.flowupdater.minecraft.versions.download.Reader;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class FlowArgMinecraftUpdater
{
    private final IVersion version;
    private final Reader reader;
    private static File LOG_FILE = new File("updater/latest.log");
    private static Logger LOGGER = new Logger("[FlowUpdater] ", LOG_FILE);
    private boolean silentRead = false;

    public FlowArgMinecraftUpdater(@NotNull IVersion version, @NotNull Reader reader)
    {
        try
        {
            if(!LOG_FILE.exists())
            {
                LOG_FILE.getParentFile().mkdirs();
                LOG_FILE.createNewFile();
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        this.version = version;
        this.reader = reader;
    }

    public void update(File dir, boolean downloadServer, boolean readSilently) throws IOException
    {
        LOGGER.info(String.format("Reading data about %s Minecraft version...", version.getName()));
        this.reader.read(readSilently);

        final Downloader downloader = new Downloader(dir, silentUpdate);
        downloader.download(downloadServer);
    }

    public Reader getReader()
    {
        return this.reader;
    }
    public IVersion getVersion()
    {
        return this.version;
    }
    public static File getLogFile()
    {
        return LOG_FILE;
    }
    public static void setLogFile(File logFile)
    {
        LOG_FILE = logFile;
    }
    public static void setLogger(Logger logger)
    {
        LOGGER = logger;
    }
    public static Logger getLogger()
    {
        return LOGGER;
    }
}
