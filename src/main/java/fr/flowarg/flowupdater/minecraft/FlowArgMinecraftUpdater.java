package fr.flowarg.flowupdater.minecraft;

import fr.flowarg.flowlogger.Logger;
import fr.flowarg.flowupdater.minecraft.versions.ForgeVersion;
import fr.flowarg.flowupdater.minecraft.versions.IVersion;
import fr.flowarg.flowupdater.minecraft.versions.download.VanillaDownloader;
import fr.flowarg.flowupdater.minecraft.versions.download.VanillaReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public class FlowArgMinecraftUpdater
{
    private final IVersion      version;
    private final VanillaReader vanillaReader;

    private File         logFile;
    private Logger       logger;
    @Nullable
    private ForgeVersion forgeVersion;

    public FlowArgMinecraftUpdater(@NotNull IVersion version, @Nullable VanillaReader vanillaReader, Logger logger)
    {
        this.logger  = logger;
        this.logFile = this.logger.getLogFile();
        try
        {
            if (!this.logFile.exists())
            {
                this.logFile.getParentFile().mkdirs();
                this.logFile.createNewFile();
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        this.logger.info(String.format("------------------------- FlowUpdater for Minecraft %s v%s -------------------------", version.getName(), "1.1.0"));
        this.version       = version;
        this.vanillaReader = vanillaReader == null ? new VanillaReader(this.version, this.logger) : vanillaReader;
    }

    public FlowArgMinecraftUpdater(@NotNull IVersion version, @NotNull VanillaReader vanillaReader)
    {
        this(version, vanillaReader, new Logger("[FlowUpdater]", new File("updater/latest.log")));
    }

    public FlowArgMinecraftUpdater(@NotNull IVersion version)
    {
        this(version, null, new Logger("[FlowUpdater]", new File("updater/latest.log")));
    }

    public FlowArgMinecraftUpdater(@NotNull IVersion version, Logger logger)
    {
        this(version, null, logger);
    }

    public void update(File dir, boolean downloadServer) throws IOException
    {
        this.logger.info(String.format("Reading data about %s Minecraft version...", version.getName()));
        this.vanillaReader.read();

        if (!dir.exists())
            dir.mkdirs();
        final VanillaDownloader vanillaDownloader = new VanillaDownloader(dir, this.logger);
        vanillaDownloader.download(downloadServer);

        if (this.getForgeVersion() != null)
            this.getForgeVersion().install(dir);
    }

    public VanillaReader getVanillaReader()
    {
        return this.vanillaReader;
    }

    public IVersion getVersion()
    {
        return this.version;
    }

    public File getLogFile()
    {
        return this.logFile;
    }

    public void setLogFile(File logFile)
    {
        this.logFile = logFile;
    }

    public void setLogger(Logger logger)
    {
        this.logger = logger;
    }

    public Logger getLogger()
    {
        return this.logger;
    }

    public @Nullable ForgeVersion getForgeVersion()
    {
        return this.forgeVersion;
    }

    public void setForgeVersion(@NotNull ForgeVersion forgeVersion)
    {
        this.forgeVersion = forgeVersion;
    }
}
