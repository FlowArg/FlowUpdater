package fr.flowarg.flowupdater.minecraft;

import fr.flowarg.flowlogger.Logger;
import fr.flowarg.flowupdater.minecraft.versions.IForgeVersion;
import fr.flowarg.flowupdater.minecraft.versions.IVersion;
import fr.flowarg.flowupdater.minecraft.versions.download.IProgressCallback;
import fr.flowarg.flowupdater.minecraft.versions.download.Step;
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
    private IForgeVersion forgeVersion;
    private IProgressCallback callback;
    
    private static final IProgressCallback NULL_CALLBACK = new IProgressCallback()
    {
		@Override
		public void update(int downloaded, int max) {}
		@Override
		public void step(Step step) {}
		@Override
		public void init() {}
	};

    public FlowArgMinecraftUpdater(@NotNull IVersion version, @Nullable VanillaReader vanillaReader, Logger logger, boolean silentUpdate, IProgressCallback callback)
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
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        this.logger.info(String.format("------------------------- FlowUpdater for Minecraft %s v%s -------------------------", version.getName(), "1.1.0"));
        this.version       = version;
        this.callback = callback;
        this.callback.init();
        this.vanillaReader = vanillaReader == null ? new VanillaReader(this.version, this.logger, silentUpdate, this.callback) : vanillaReader;
    }

    public void update(File dir, boolean downloadServer) throws IOException
    {
        this.logger.info(String.format("Reading data about %s Minecraft version...", version.getName()));
        this.vanillaReader.read();

        if (!dir.exists())
            dir.mkdirs();
        final VanillaDownloader vanillaDownloader = new VanillaDownloader(dir, this.logger, this.callback);
        vanillaDownloader.download(downloadServer);

        if (this.getForgeVersion() != null)
            this.getForgeVersion().install(dir);
        this.callback.step(Step.END);
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

    public @Nullable IForgeVersion getForgeVersion()
    {
        return this.forgeVersion;
    }
    
    public @Nullable IProgressCallback getCallback()
    {
		return this.callback;
	}

    public void setForgeVersion(@NotNull IForgeVersion forgeVersion)
    {
        this.forgeVersion = forgeVersion;
    }
    
    public static class SlimUpdaterBuilder
    {
        public static FlowArgMinecraftUpdater build(@NotNull IVersion version, @NotNull VanillaReader vanillaReader)
        {
        	return new FlowArgMinecraftUpdater(version, vanillaReader, new Logger("[FlowUpdater]", new File("updater/latest.log")), false, NULL_CALLBACK);
        }

        public static FlowArgMinecraftUpdater build(@NotNull IVersion version)
        {
        	return new FlowArgMinecraftUpdater(version, null, new Logger("[FlowUpdater]", new File("updater/latest.log")), false, NULL_CALLBACK);
        }

        public static FlowArgMinecraftUpdater build(@NotNull IVersion version, Logger logger)
        {
        	return new FlowArgMinecraftUpdater(version, null, logger, false, NULL_CALLBACK);
        }
        
        public static FlowArgMinecraftUpdater build(@NotNull IVersion version, @NotNull VanillaReader vanillaReader, boolean silentUpdate)
        {
        	return new FlowArgMinecraftUpdater(version, vanillaReader, new Logger("[FlowUpdater]", new File("updater/latest.log")), silentUpdate, NULL_CALLBACK);
        }

        public static FlowArgMinecraftUpdater build(@NotNull IVersion version, boolean silentUpdate)
        {
        	return new FlowArgMinecraftUpdater(version, null, new Logger("[FlowUpdater]", new File("updater/latest.log")), silentUpdate, NULL_CALLBACK);
        }

        public static FlowArgMinecraftUpdater build(@NotNull IVersion version, Logger logger, boolean silentUpdate)
        {
        	return new FlowArgMinecraftUpdater(version, null, logger, silentUpdate, NULL_CALLBACK);
        }  
        
        public static FlowArgMinecraftUpdater build(@NotNull IVersion version, @NotNull VanillaReader vanillaReader, IProgressCallback callback)
        {
        	return new FlowArgMinecraftUpdater(version, vanillaReader, new Logger("[FlowUpdater]", new File("updater/latest.log")), false, callback);
        }

        public static FlowArgMinecraftUpdater build(@NotNull IVersion version, IProgressCallback callback)
        {
        	return new FlowArgMinecraftUpdater(version, null, new Logger("[FlowUpdater]", new File("updater/latest.log")), false, callback);
        }

        public static FlowArgMinecraftUpdater build(@NotNull IVersion version, Logger logger, IProgressCallback callback)
        {
        	return new FlowArgMinecraftUpdater(version, null, logger, false, callback);
        }
        
        public static FlowArgMinecraftUpdater build(@NotNull IVersion version, @NotNull VanillaReader vanillaReader, boolean silentUpdate, IProgressCallback callback)
        {
        	return new FlowArgMinecraftUpdater(version, vanillaReader, new Logger("[FlowUpdater]", new File("updater/latest.log")), silentUpdate, callback);
        }

        public static FlowArgMinecraftUpdater build(@NotNull IVersion version, boolean silentUpdate, IProgressCallback callback)
        {
        	return new FlowArgMinecraftUpdater(version, null, new Logger("[FlowUpdater]", new File("updater/latest.log")), silentUpdate, callback);
        }

        public static FlowArgMinecraftUpdater build(@NotNull IVersion version, Logger logger, boolean silentUpdate, IProgressCallback callback)
        {
        	return new FlowArgMinecraftUpdater(version, null, logger, silentUpdate, callback);
        }
    }
}
