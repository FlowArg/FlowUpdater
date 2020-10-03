package fr.flowarg.flowupdater;

import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowlogger.Logger;
import fr.flowarg.flowupdater.download.*;
import fr.flowarg.flowupdater.download.json.ExternalFile;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.UpdaterOptions;
import fr.flowarg.flowupdater.utils.builderapi.BuilderArgument;
import fr.flowarg.flowupdater.utils.builderapi.BuilderException;
import fr.flowarg.flowupdater.utils.builderapi.IBuilder;
import fr.flowarg.flowupdater.versions.AbstractForgeVersion;
import fr.flowarg.flowupdater.versions.VanillaVersion;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static fr.flowarg.flowio.FileUtils.getFileSizeBytes;
import static fr.flowarg.flowio.FileUtils.getSHA1;

/**
 * Represent the base class of the updater.<br>
 * You can define some parameters about your version (Forge, Vanilla, MCP (Soon fabric) etc...).
 * @author FlowArg
 */
public class FlowUpdater
{
	/** Vanilla version's object to update/install */
    private final VanillaVersion version;
    /** Vanilla version's JSON parser */
    private final VanillaReader vanillaReader;

    /** Logger object */
    private final ILogger logger;
    
    /** Forge Version to install, can be null if you want a vanilla/MCP installation */
    private final AbstractForgeVersion forgeVersion;
    /** Progress callback to notify installation progress */
    private final IProgressCallback callback;
    
    /** Informations about download status */
    private final DownloadInfos downloadInfos;
        
    /** Represent some settings for FlowUpdater */
    private final UpdaterOptions updaterOptions;
    
    /** Represent a list of ExternalFile. External files are download before post executions.*/
    private final List<ExternalFile> externalFiles;
    
    /** Represent a list of Runnable. Post Executions are called after update. */
    private final List<Runnable> postExecutions;
    
    /** Default callback */
    public static final IProgressCallback NULL_CALLBACK = new IProgressCallback()
    {
		@Override
		public void update(int downloaded, int max) {}
		@Override
		public void step(Step step) {}
		@Override
		public void init(ILogger logger)
		{
			logger.warn("You are using default callback ! It's not recommanded. IT'S NOT AN ERROR !!!");
		}
	};
	
	/** Default logger, null for file argument = no file logger */
	public static final ILogger DEFAULT_LOGGER = new Logger("[FlowUpdater]", null);

	/**
	 * Basic constructor to construct a new {@link FlowUpdater}.
	 * @param version Version to update.
	 * @param logger Logger used for log informations.
	 * @param updaterOptions options for this updater
	 * @param callback The callback. If it's null, it will automatically assigned as {@link FlowUpdater#NULL_CALLBACK}.
	 * @param externalFiles External files are download before postExecutions.
	 * @param postExecutions Post executions are called after update.
	 * @param forgeVersion ForgeVersion to install, can be null.
	 */
    private FlowUpdater(VanillaVersion version, ILogger logger, UpdaterOptions updaterOptions,
    		IProgressCallback callback, List<ExternalFile> externalFiles, List<Runnable> postExecutions, AbstractForgeVersion forgeVersion)
    {
        this.logger = logger;
        this.version = version;
        this.externalFiles = externalFiles;
        this.postExecutions = postExecutions;
        this.forgeVersion = forgeVersion;
        this.updaterOptions = updaterOptions;
        this.downloadInfos = new DownloadInfos();
        this.callback = callback;
       	this.vanillaReader = new VanillaReader(this.version, this.logger, this.updaterOptions.isSilentRead(), this.callback, this.downloadInfos);
       	this.logger.info(String.format("------------------------- FlowUpdater for Minecraft %s v%s -------------------------", this.version.getName(), "1.2.0"));
       	this.callback.init(this.logger);
    }
    
    /**
     * This method update the Minecraft Installation in the given directory. If the {@link #version} is {@link VanillaVersion#NULL_VERSION}, the updater will
     * be only run external files and post executions.
     * @param dir Directory where is the Minecraft installation.
     * @param downloadServer True -> Download the server.jar.
     * @throws IOException if a I/O problem has occurred.
     */
    public void update(File dir, boolean downloadServer) throws Exception
    {
    	this.checkExtFiles(dir);
        this.updateVanillaVersion(dir, downloadServer);
    	this.updateExtFiles(dir);
    	this.runPostExecutions();
    	this.endUpdate();
    }

    private void checkExtFiles(File dir) throws Exception
	{
        if(!this.externalFiles.isEmpty())
        {
    		for(ExternalFile extFile : this.externalFiles)
    		{
    	        final File file = new File(dir, extFile.getPath());

    	        if (file.exists())
    	        {
    	            if (!Objects.requireNonNull(getSHA1(file)).equals(extFile.getSha1()))
    	            {
    	                file.delete();
    	                this.downloadInfos.getExtFiles().add(extFile);
    	            }
    	        }
    	        else this.downloadInfos.getExtFiles().add(extFile);
    		}
        }
    }
    
    private void updateVanillaVersion(File dir, boolean downloadServer) throws Exception
    {
    	if(this.version != VanillaVersion.NULL_VERSION)
    	{
            this.logger.info(String.format("Reading data about %s Minecraft version...", version.getName()));
            this.vanillaReader.read();
            
            if(this.forgeVersion != null)
            {
        		for(Mod mod : this.forgeVersion.getMods())
        		{
        	        final File file = new File(new File(dir, "mods/"), mod.getName());

        	        if (file.exists())
        	        {
        	            if (!Objects.requireNonNull(getSHA1(file)).equals(mod.getSha1()) || getFileSizeBytes(file) != mod.getSize())
        	            {
        	                file.delete();
        	                this.downloadInfos.getMods().add(mod);
        	            }
        	        }
        	        else this.downloadInfos.getMods().add(mod);
        		}
            }

            if (!dir.exists())
                dir.mkdirs();
            final VanillaDownloader vanillaDownloader = new VanillaDownloader(dir, this.logger, this.callback, this.downloadInfos, this.updaterOptions.isReextractNatives());
            vanillaDownloader.download(downloadServer);

            if (this.forgeVersion != null)
            {
            	this.forgeVersion.appendDownloadInfos(this.downloadInfos);
            	if(!this.forgeVersion.isForgeAlreadyInstalled(dir))
            	  	this.forgeVersion.install(dir);
            	else this.logger.info("Forge is already installed ! Skipping installation...");
            	this.forgeVersion.installMods(new File(dir, "mods/"));
            }
    	}
    	else this.downloadInfos.init();
    }
    
    private void updateExtFiles(File dir)
    {
    	if(!this.downloadInfos.getExtFiles().isEmpty())
    	{
            this.callback.step(Step.EXTERNAL_FILES);
            this.logger.info("Downloading external file(s)...");
        	this.downloadInfos.getExtFiles().forEach(extFile -> {
        		try
        		{
        			IOUtils.download(this.logger, new URL(extFile.getDownloadURL()), new File(dir, extFile.getPath()));
    			}
        		catch (IOException e)
        		{
    				this.logger.printStackTrace(e);
    			}
    			this.downloadInfos.incrementDownloaded();
    			this.callback.update(this.downloadInfos.getDownloaded(), this.downloadInfos.getTotalToDownload());
        	});
    	}
    }
    
    private void runPostExecutions()
    {
        if(!this.postExecutions.isEmpty())
        {
        	this.callback.step(Step.POST_EXECUTIONS);
            this.logger.info("Running post executions...");
            this.postExecutions.forEach(Runnable::run);
        }
    }
    
    private void endUpdate()
    {
        this.callback.step(Step.END);
        if(this.downloadInfos.getTotalToDownload() == this.downloadInfos.getDownloaded() + 1)
        {
        	this.downloadInfos.incrementDownloaded();
        	this.callback.update(this.downloadInfos.getDownloaded(), this.downloadInfos.getTotalToDownload());
        }
        this.downloadInfos.clear();
    }
    
    
    /**
     * Builder of {@link FlowUpdater}.
     * @author FlowArg
     */
    public static class FlowUpdaterBuilder implements IBuilder<FlowUpdater>
    {
    	private final BuilderArgument<VanillaVersion> versionArgument = new BuilderArgument<>("VanillaVersion", VanillaVersion.NULL_VERSION, VanillaVersion.NULL_VERSION).optional();
    	private final BuilderArgument<ILogger> loggerArgument = new BuilderArgument<>("Logger", DEFAULT_LOGGER).optional();
    	private final BuilderArgument<UpdaterOptions> updaterOptionsArgument = new BuilderArgument<UpdaterOptions>("UpdaterOptions").required();
    	private final BuilderArgument<IProgressCallback> progressCallbackArgument = new BuilderArgument<>("Callback", NULL_CALLBACK).optional();
    	private final BuilderArgument<List<ExternalFile>> externalFilesArgument = new BuilderArgument<List<ExternalFile>>("External Files", new ArrayList<>()).optional();
    	private final BuilderArgument<List<Runnable>> postExecutionsArgument = new BuilderArgument<List<Runnable>>("Post Executions", new ArrayList<>()).optional();
    	private final BuilderArgument<AbstractForgeVersion> forgeVersionArgument = new BuilderArgument<AbstractForgeVersion>("ForgeVersion").optional().require(this.versionArgument);
    	
    	public FlowUpdaterBuilder withVersion(VanillaVersion version)
    	{
    		this.versionArgument.set(version);
    		return this;
    	}
    	
    	public FlowUpdaterBuilder withLogger(ILogger logger)
    	{
    		this.loggerArgument.set(logger);
    		return this;
    	}
    	
    	public FlowUpdaterBuilder withUpdaterOptions(UpdaterOptions updaterOptions)
    	{
    		this.updaterOptionsArgument.set(updaterOptions);
    		return this;
    	}
    	
    	public FlowUpdaterBuilder withProgressCallback(IProgressCallback callback)
    	{
    		this.progressCallbackArgument.set(callback);
    		return this;
    	}
    	
    	public FlowUpdaterBuilder withExternaFiles(List<ExternalFile> externalFiles)
    	{
    		this.externalFilesArgument.set(externalFiles);
    		return this;
    	}
    	
    	public FlowUpdaterBuilder withPostExecutions(List<Runnable> postExecutions)
    	{
    		this.postExecutionsArgument.set(postExecutions);
    		return this;
    	}
    	
        /**
         * Necessary if you want install a Forge version.
         * @param forgeVersion Forge version to install.
         */
    	public FlowUpdaterBuilder withForgeVersion(AbstractForgeVersion forgeVersion)
    	{
    		this.forgeVersionArgument.set(forgeVersion);
    		return this;
    	}
    	
    	@Override
    	public FlowUpdater build() throws BuilderException
    	{
    		return new FlowUpdater(this.versionArgument.get(),
    				this.loggerArgument.get(),
    				this.updaterOptionsArgument.get(),
    				this.progressCallbackArgument.get(),
    				this.externalFilesArgument.get(),
    				this.postExecutionsArgument.get(),
    				this.forgeVersionArgument.get());
    	}
    }
    
    // Some getters
    
    public VanillaReader getVanillaReader()
    {
        return this.vanillaReader;
    }

    public VanillaVersion getVersion()
    {
        return this.version;
    }

    public ILogger getLogger()
    {
        return this.logger;
    }

    public AbstractForgeVersion getForgeVersion()
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
      
    public DownloadInfos getDownloadInfos()
    {
		return this.downloadInfos;
	}
    
    public UpdaterOptions getUpdaterOptions()
    {
		return this.updaterOptions;
	}
}
