package fr.flowarg.flowupdater;

import static fr.flowarg.flowio.FileUtils.getSHA1;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowlogger.Logger;
import fr.flowarg.flowupdater.utils.BuilderArgument;
import fr.flowarg.flowupdater.utils.BuilderArgumentException;
import fr.flowarg.flowupdater.versions.IForgeVersion;
import fr.flowarg.flowupdater.versions.IVanillaVersion;
import fr.flowarg.flowupdater.versions.download.DownloadInfos;
import fr.flowarg.flowupdater.versions.download.ExternalFile;
import fr.flowarg.flowupdater.versions.download.IProgressCallback;
import fr.flowarg.flowupdater.versions.download.Step;
import fr.flowarg.flowupdater.versions.download.VanillaDownloader;
import fr.flowarg.flowupdater.versions.download.VanillaReader;

/**
 * Represent the base class of the updater.<br>
 * You can define some parameters about your version (Forge, Vanilla, MCP etc...).
 * @author FlowArg
 */
public class FlowUpdater
{
	/** Vanilla version's object to update/install */
    private final IVanillaVersion version;
    /** Vanilla version's JSON parser */
    private final VanillaReader vanillaReader;

    /** Logger object with his {@linkFile} */
    private File logFile;
    private ILogger logger;
    
    /** Forge Version to install, can be null if you want a vanilla/MCP installation */
    private IForgeVersion forgeVersion = null;
    /** ProgressCallback to notify installation progress */
    private IProgressCallback callback;
    
    /** Informations about download status */
    private DownloadInfos downloadInfos;
    
    /** Is the update silent */
    private boolean isSilent;
    
    /** Represent a list of ExternalFile. External files are download before post executions.*/
    private List<ExternalFile> externalFiles;
    
    /** Represent a list of Runnable. Post Executions are called after update. */
    private List<Runnable> postExecutions;
    
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
	 * Basic constructor to construct a new {@link FlowUpdater}.
	 * @param version Version to update
	 * @param vanillaReader Reader to use for JSON file.
	 * @param logger Logger used for log informations.
	 * @param silentUpdate True -> reader doesn't make any log. False -> reader log all messages.
	 * @param callback The callback. If it's null, it will automatically assigned as {@link FlowUpdater#NULL_CALLBACK}.
	 * @param externalFiles External files are download before postExecutions.
	 * @param postExecutions Post executions are called after update.
	 */
    private FlowUpdater(IVanillaVersion version, ILogger logger, boolean silentUpdate, IProgressCallback callback, List<ExternalFile> externalFiles, List<Runnable> postExecutions)
    {
        this.logger = logger;
        this.externalFiles = externalFiles;
        this.postExecutions = postExecutions;
        this.isSilent = silentUpdate;
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
        this.logger.info(String.format("------------------------- FlowUpdater for Minecraft %s v%s -------------------------", version.getName(), "1.1.6"));
        this.version = version;
        this.downloadInfos = new DownloadInfos();
        this.callback = callback;
        this.callback.init();
        this.vanillaReader = new VanillaReader(this.version, this.logger, this.isSilent, this.callback, this.downloadInfos);
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
        if(!this.externalFiles.isEmpty())
        {
            this.callback.step(Step.EXTERNAL_FILES);
            this.logger.info("Downloading external files...");
    		for(ExternalFile extFile : this.externalFiles)
    		{
    	        final File file = new File(dir, extFile.getPath());

    	        if (file.exists())
    	        {
    	            if (!Objects.requireNonNull(getSHA1(file)).equals(extFile.getSha1()))
    	            {
    	                file.delete();
    	                this.download(new URL(extFile.getDownloadURL()), file);
    	            }
    	        }
    	        else this.download(new URL(extFile.getDownloadURL()), file);
    		}
        }
        if(!this.postExecutions.isEmpty())
        {
        	this.callback.step(Step.POST_EXECUTIONS);
            this.logger.info("Running post executions...");
            for(Runnable postExecution : this.postExecutions)
            	postExecution.run();
        }
        this.callback.step(Step.END);
    }
    
    private void download(URL in, File out) throws IOException
    {
        this.logger.info(String.format("[Downloader] Downloading %s from %s...", out.getName(), in.toExternalForm()));
        out.getParentFile().mkdirs();
        Files.copy(in.openStream(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    
    // GETTERS

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

    public void setLogger(ILogger logger)
    {
        this.logger = logger;
    }

    public ILogger getLogger()
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
    
    public List<ExternalFile> getExternalFiles()
    {
		return this.externalFiles;
	}
    
    public List<Runnable> getPostExecutions()
    {
		return this.postExecutions;
	}
    
    public boolean isSilent()
    {
		return this.isSilent;
	}
    
    
    public DownloadInfos getDownloadInfos()
    {
		return this.downloadInfos;
	}

    /**
     * Necessary if you want install a Forge version.
     * @param forgeVersion Forge version to install.
     */
    public void setForgeVersion(IForgeVersion forgeVersion)
    {
        this.forgeVersion = forgeVersion;
    }
    
    /**
     * Builder to build a {@link FlowUpdater} with less argument.
     * @author FlowArg
     */
    public static class FlowUpdaterBuilder
    {    	
    	private BuilderArgument<IVanillaVersion> versionArgument = new BuilderArgument<IVanillaVersion>(null).required();
    	private BuilderArgument<ILogger> loggerArgument = new BuilderArgument<ILogger>(null).optional();
    	private BuilderArgument<Boolean> silentUpdateArgument = new BuilderArgument<Boolean>(null).optional();
    	private BuilderArgument<IProgressCallback> progressCallbackArgument = new BuilderArgument<IProgressCallback>(null).optional();
    	private BuilderArgument<List<ExternalFile>> externalFilesArgument = new BuilderArgument<List<ExternalFile>>(null).optional();
    	private BuilderArgument<List<Runnable>> postExecutionsArgument = new BuilderArgument<List<Runnable>>(null).optional();
    	
    	public FlowUpdaterBuilder withVersion(IVanillaVersion version)
    	{
    		this.versionArgument.set(version);
    		return this;
    	}
    	
    	public FlowUpdaterBuilder withLogger(ILogger logger)
    	{
    		this.loggerArgument.set(logger);
    		return this;
    	}
    	
    	public FlowUpdaterBuilder withSilentUpdate(boolean silentUpdate)
    	{
    		this.silentUpdateArgument.set(Boolean.valueOf(silentUpdate));
    		return this;
    	}
    	
    	public FlowUpdaterBuilder withProgressCallback(IProgressCallback callback)
    	{
    		this.progressCallbackArgument.set(callback);
    		return this;
    	}
    	
    	public FlowUpdaterBuilder withExternaFiles(ArrayList<ExternalFile> externalFiles)
    	{
    		this.externalFilesArgument.set(externalFiles);
    		return this;
    	}
    	
    	public FlowUpdaterBuilder witPostExecutions(ArrayList<Runnable> postExecutions)
    	{
    		this.postExecutionsArgument.set(postExecutions);
    		return this;
    	}
    	
    	public FlowUpdater build() throws BuilderArgumentException
    	{
    		assert this.versionArgument.get() != null;
    		final ILogger logger = this.loggerArgument.get() != null ? this.loggerArgument.get() : new Logger("[FlowUpdater]", new File(".", "updater/latest.log"));
    		return new FlowUpdater(this.versionArgument.get(),
    				logger,
    				this.silentUpdateArgument.get(),
    				this.progressCallbackArgument.get() != null ? this.progressCallbackArgument.get() : NULL_CALLBACK,
    				this.externalFilesArgument.get() != null ? this.externalFilesArgument.get() : new ArrayList<>(),
    				this.postExecutionsArgument.get() != null ? this.postExecutionsArgument.get() : new ArrayList<>());
    	}
    }
}
