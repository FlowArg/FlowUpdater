package fr.flowarg.flowupdater;

import static fr.flowarg.flowio.FileUtils.getFileSizeBytes;
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
import fr.flowarg.flowupdater.download.DownloadInfos;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.Step;
import fr.flowarg.flowupdater.download.VanillaDownloader;
import fr.flowarg.flowupdater.download.VanillaReader;
import fr.flowarg.flowupdater.download.json.ExternalFile;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.utils.BuilderArgument;
import fr.flowarg.flowupdater.utils.BuilderArgumentException;
import fr.flowarg.flowupdater.utils.ForgeHacks;
import fr.flowarg.flowupdater.utils.UpdaterOptions;
import fr.flowarg.flowupdater.versions.IForgeVersion;
import fr.flowarg.flowupdater.versions.IVanillaVersion;

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

    /** Logger object */
    private final ILogger logger;
    
    /** Forge Version to install, can be null if you want a vanilla/MCP installation */
    private final IForgeVersion forgeVersion;
    /** ProgressCallback to notify installation progress */
    private final IProgressCallback callback;
    
    /** Informations about download status */
    private final DownloadInfos downloadInfos;
        
    /** Represent somme settings for FlowUpdater */
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
		public void init() {}
	};
	
	/** Default logger */
	public static final ILogger DEFAULT_LOGGER = new Logger("[FlowUpdater]", null);

	/**
	 * Basic constructor to construct a new {@link FlowUpdater}.
	 * @param version Version to update.
	 * @param logger Logger used for log informations.
	 * @param silentUpdate True -> reader doesn't make any log. False -> reader log all messages.
	 * @param callback The callback. If it's null, it will automatically assigned as {@link FlowUpdater#NULL_CALLBACK}.
	 * @param externalFiles External files are download before postExecutions.
	 * @param postExecutions Post executions are called after update.
	 */
    private FlowUpdater(IVanillaVersion version, ILogger logger, UpdaterOptions updaterOptions,
    		IProgressCallback callback, List<ExternalFile> externalFiles, List<Runnable> postExecutions, IForgeVersion forgeVersion)
    {
        this.logger = logger;
        this.version = version;
        this.externalFiles = externalFiles;
        this.postExecutions = postExecutions;
        this.forgeVersion = forgeVersion;
        this.updaterOptions = updaterOptions;
        this.downloadInfos = new DownloadInfos();
        this.callback = callback;
        this.callback.init();
       	this.vanillaReader = new VanillaReader(this.version, this.logger, this.updaterOptions.isSilentUpdate(), this.callback, this.downloadInfos);
       	this.logger.info(String.format("------------------------- FlowUpdater for Minecraft %s v%s -------------------------", this.version.getName(), "1.1.11"));
    }

    /**
     * This method update the Minecraft Installation in the given directory. If the {@link #version} is {@link IVanillaVersion#NULL_VERSION}, the updater will
     * be only run external files and post executions.
     * @param dir Directory where is the Minecraft installation.
     * @param downloadServer True -> Download the server.jar.
     * @throws IOException if a I/O problem has occurred.
     */
    public void update(File dir, boolean downloadServer) throws IOException
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
        
    	if(this.version != IVanillaVersion.NULL_VERSION)
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
            	else this.logger.info("Detected forge ! Skipping installation...");
            	this.forgeVersion.installMods(new File(dir, "mods/"));
            	
            	if(!this.updaterOptions.disableForgeHacks())
            		ForgeHacks.fix(this.callback, dir);
            }
    	}
    	else this.downloadInfos.init();
    	
    	if(!this.downloadInfos.getExtFiles().isEmpty())
    	{
            this.callback.step(Step.EXTERNAL_FILES);
            this.logger.info("Downloading external file(s)...");
        	this.downloadInfos.getExtFiles().forEach(extFile -> {
        		try
        		{
    				this.download(new URL(extFile.getDownloadURL()), new File(dir, extFile.getPath()));
    			}
        		catch (IOException e)
        		{
    				this.logger.printStackTrace(e);
    			}
    			this.downloadInfos.incrementDownloaded();
    			this.callback.update(this.downloadInfos.getDownloaded(), this.downloadInfos.getTotalToDownload());
        	});
    	}
    	
        if(!this.postExecutions.isEmpty())
        {
        	this.callback.step(Step.POST_EXECUTIONS);
            this.logger.info("Running post executions...");
            this.postExecutions.forEach(Runnable::run);
        }
        this.callback.step(Step.END);
        this.downloadInfos.clear();
    }
    
    private void download(URL in, File out) throws IOException
    {
        try
        {
            this.logger.info(String.format("[Downloader] Downloading %s from %s...", out.getName(), in.toExternalForm()));
            out.getParentFile().mkdirs();
			Files.copy(in.openStream(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
        catch (IOException e)
        {
			this.logger.printStackTrace(e);
		}
    }
    
    /**
     * Builder of {@link FlowUpdater}.
     * @author FlowArg
     */
    public static class FlowUpdaterBuilder
    {
    	private final BuilderArgument<IVanillaVersion> versionArgument = new BuilderArgument<IVanillaVersion>(IVanillaVersion.NULL_VERSION).optional();
    	private final BuilderArgument<ILogger> loggerArgument = new BuilderArgument<ILogger>(DEFAULT_LOGGER).optional();
    	private final BuilderArgument<UpdaterOptions> updaterOptionsArgument = new BuilderArgument<UpdaterOptions>(null).required();
    	private final BuilderArgument<IProgressCallback> progressCallbackArgument = new BuilderArgument<IProgressCallback>(NULL_CALLBACK).optional();
    	private final BuilderArgument<List<ExternalFile>> externalFilesArgument = new BuilderArgument<List<ExternalFile>>(new ArrayList<>()).optional();
    	private final BuilderArgument<List<Runnable>> postExecutionsArgument = new BuilderArgument<List<Runnable>>(new ArrayList<>()).optional();
    	private final BuilderArgument<IForgeVersion> forgeVersionArgument = new BuilderArgument<IForgeVersion>(null).optional();
    	
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
    	public FlowUpdaterBuilder withForgeVersion(IForgeVersion forgeVersion)
    	{
    		this.forgeVersionArgument.set(forgeVersion);
    		return this;
    	}
    	
    	public FlowUpdater build() throws BuilderArgumentException
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
    
    public VanillaReader getVanillaReader()
    {
        return this.vanillaReader;
    }

    public IVanillaVersion getVersion()
    {
        return this.version;
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
      
    public DownloadInfos getDownloadInfos()
    {
		return this.downloadInfos;
	}
    
    public UpdaterOptions getUpdaterOptions()
    {
		return this.updaterOptions;
	}
}
