package fr.flowarg.flowupdater.minecraft;

import java.io.File;
import java.io.IOException;

import fr.flowarg.flowlogger.Logger;
import fr.flowarg.flowupdater.minecraft.versions.IForgeVersion;
import fr.flowarg.flowupdater.minecraft.versions.IVanillaVersion;
import fr.flowarg.flowupdater.minecraft.versions.download.DownloadInfos;
import fr.flowarg.flowupdater.minecraft.versions.download.IProgressCallback;
import fr.flowarg.flowupdater.minecraft.versions.download.Step;
import fr.flowarg.flowupdater.minecraft.versions.download.VanillaDownloader;
import fr.flowarg.flowupdater.minecraft.versions.download.VanillaReader;

/**
 * Represent the base class of the updater.<br>
 * You can define some parameters about your version (Forge, Vanilla, MCP etc...).
 * @author FlowArg
 */
public class FlowArgMinecraftUpdater
{
	/** Vanilla version's object to update/install */
    private final IVanillaVersion      version;
    /** Vanilla version's JSON parser */
    private final VanillaReader vanillaReader;

    /** Logger object with his {@linkFile} */
    private File         logFile;
    private Logger       logger;
    
    /** Forge Version to install, can be null if you want a vanilla/MCP installation */
    private IForgeVersion forgeVersion = null;
    /** ProgressCallback to notify installation progress */
    private IProgressCallback callback;
    
    /** Informations about download status */
    private DownloadInfos downloadInfos;
    
    /** Default callback */
    public static final IProgressCallback NULL_CALLBACK = new IProgressCallback()
    {
		@Override
		public void update(int downloaded, int max) {}
		@Override
		public void step(Step step) {}
		@Override
		public void init() {}
	};

	/**
	 * Basic constructor to construct a new {@link FlowArgMinecraftUpdater}.
	 * @param version Version to update
	 * @param vanillaReader Reader to use for JSON file.
	 * @param logger Logger used for log informations.
	 * @param silentUpdate True -> reader doesn't make any log. False -> reader log all messages.
	 * @param callback The callback. If it's null, it will automatically assigned as {@link FlowArgMinecraftUpdater#NULL_CALLBACK}.
	 */
    public FlowArgMinecraftUpdater(IVanillaVersion version, VanillaReader vanillaReader, Logger logger, boolean silentUpdate, IProgressCallback callback)
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
        this.downloadInfos = new DownloadInfos();
        this.callback = callback != null ? callback : NULL_CALLBACK;
        this.callback.init();
        this.vanillaReader = vanillaReader == null ? new VanillaReader(this.version, this.logger, silentUpdate, this.callback, this.downloadInfos) : vanillaReader;
    }

    /**
     * This method update the Minecraft Installation in the given directory.
     * @param dir Directory where is the Minecraft installation.
     * @param downloadServer True -> Download the server.jar.
     * @throws IOException if a problem has occurred.
     */
    public void update(File dir, boolean downloadServer) throws IOException
    {
        this.logger.info(String.format("Reading data about %s Minecraft version...", version.getName()));
        this.vanillaReader.read();

        if (!dir.exists())
            dir.mkdirs();
        final VanillaDownloader vanillaDownloader = new VanillaDownloader(dir, this.logger, this.callback, this.downloadInfos);
        vanillaDownloader.download(downloadServer);

        if (this.getForgeVersion() != null)
        {
        	this.forgeVersion.install(dir);
        	this.forgeVersion.installMods(new File(dir, "mods/"));
        }    
        this.callback.step(Step.END);
    }

    public VanillaReader getVanillaReader()
    {
        return this.vanillaReader;
    }

    public IVanillaVersion getVersion()
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

    public IForgeVersion getForgeVersion()
    {
        return this.forgeVersion;
    }
    
    public IProgressCallback getCallback()
    {
		return this.callback;
	}

    /**
     * Necessary if you want install a Forge version.
     * @param forgeVersion Forge version to install.
     */
    public void setForgeVersion(IForgeVersion forgeVersion)
    {
        this.forgeVersion = forgeVersion;
    }
    
    public DownloadInfos getDownloadInfos()
    {
		return this.downloadInfos;
	}
    
    /**
     * Builder to build a {@link FlowArgMinecraftUpdater} with less argument.
     * @author FlowArg
     */
    public static class SlimUpdaterBuilder
    {
        public static FlowArgMinecraftUpdater build(
        		IVanillaVersion version,
        		VanillaReader vanillaReader)
        {
        	return new FlowArgMinecraftUpdater(version, vanillaReader, new Logger("[FlowUpdater]", new File("updater/latest.log")), false, NULL_CALLBACK);
        }

        public static FlowArgMinecraftUpdater build(
        		IVanillaVersion version)
        {
        	return new FlowArgMinecraftUpdater(version, null, new Logger("[FlowUpdater]", new File("updater/latest.log")), false, NULL_CALLBACK);
        }

        public static FlowArgMinecraftUpdater build(
        		IVanillaVersion version,
        		Logger logger)
        {
        	return new FlowArgMinecraftUpdater(version, null, logger, false, NULL_CALLBACK);
        }
        
        public static FlowArgMinecraftUpdater build(
        		IVanillaVersion version,
        		VanillaReader vanillaReader,
        		boolean silentUpdate)
        {
        	return new FlowArgMinecraftUpdater(version, vanillaReader, new Logger("[FlowUpdater]", new File("updater/latest.log")), silentUpdate, NULL_CALLBACK);
        }

        public static FlowArgMinecraftUpdater build(
        		IVanillaVersion version,
        		boolean silentUpdate)
        {
        	return new FlowArgMinecraftUpdater(version, null, new Logger("[FlowUpdater]", new File("updater/latest.log")), silentUpdate, NULL_CALLBACK);
        }

        public static FlowArgMinecraftUpdater build(
        		IVanillaVersion version,
        		Logger logger,
        		boolean silentUpdate)
        {
        	return new FlowArgMinecraftUpdater(version, null, logger, silentUpdate, NULL_CALLBACK);
        }  
        
        public static FlowArgMinecraftUpdater build(
        		IVanillaVersion version,
        		VanillaReader vanillaReader,
        		IProgressCallback callback)
        {
        	return new FlowArgMinecraftUpdater(version, vanillaReader, new Logger("[FlowUpdater]", new File("updater/latest.log")), false, callback);
        }

        public static FlowArgMinecraftUpdater build(
        		IVanillaVersion version,
        		IProgressCallback callback)
        {
        	return new FlowArgMinecraftUpdater(version, null, new Logger("[FlowUpdater]", new File("updater/latest.log")), false, callback);
        }

        public static FlowArgMinecraftUpdater build(
        		IVanillaVersion version,
        		Logger logger,
        		IProgressCallback callback)
        {
        	return new FlowArgMinecraftUpdater(version, null, logger, false, callback);
        }
        
        public static FlowArgMinecraftUpdater build(
        		IVanillaVersion version,
        		VanillaReader vanillaReader,
        		boolean silentUpdate,
        		IProgressCallback callback)
        {
        	return new FlowArgMinecraftUpdater(version, vanillaReader, new Logger("[FlowUpdater]", new File("updater/latest.log")), silentUpdate, callback);
        }

        public static FlowArgMinecraftUpdater build(
        		IVanillaVersion version,
        		boolean silentUpdate,
        		IProgressCallback callback)
        {
        	return new FlowArgMinecraftUpdater(version, null, new Logger("[FlowUpdater]", new File("updater/latest.log")), silentUpdate, callback);
        }

        public static FlowArgMinecraftUpdater build(
        		IVanillaVersion version,
        		Logger logger,
        		boolean silentUpdate,
        		IProgressCallback callback)
        {
        	return new FlowArgMinecraftUpdater(version, null, logger, silentUpdate, callback);
        }
    }
}
