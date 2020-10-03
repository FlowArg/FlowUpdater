package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.DownloadInfos;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.Step;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.utils.CurseModInfos;
import fr.flowarg.flowupdater.utils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static fr.flowarg.flowio.FileUtils.getFileSizeBytes;
import static fr.flowarg.flowio.FileUtils.getSHA1;

/**
 * The base object of a forge version.
 * Implemented by {@link OldForgeVersion} & {@link NewForgeVersion}
 * @author flow
 */
public abstract class AbstractForgeVersion
{
	protected final ILogger logger;
	protected final List<Mod> mods;
	protected final VanillaVersion vanilla;
	protected final String forgeVersion;
	protected final IProgressCallback callback;
	protected URL installerUrl;
	protected DownloadInfos downloadInfos;
	protected boolean useFileDeleter = false;
	protected List<CurseModInfos> curseMods;
	
	protected AbstractForgeVersion(ILogger logger, List<Mod> mods, String forgeVersion, VanillaVersion vanilla, IProgressCallback callback)
	{
		this.logger = logger;
		this.mods = mods;
		this.vanilla = vanilla;
        if (!forgeVersion.contains("-"))
            this.forgeVersion = this.vanilla.getName() + '-' + forgeVersion;
        else this.forgeVersion = forgeVersion.trim();
        this.callback = callback;
        try
        {
            this.installerUrl = new URL(String.format("https://files.minecraftforge.net/maven/net/minecraftforge/forge/%s/forge-%s-installer.jar", this.forgeVersion, this.forgeVersion));
        } catch (MalformedURLException e)
        {
            this.logger.printStackTrace(e);
        }
	}
	
	/**
	 * Check if forge is already installed. Used by {@link FlowUpdater} on update task.
	 * @param installDir the minecraft installation dir.
	 * @return true if forge is already installed or not.
	 */
	public boolean isForgeAlreadyInstalled(File installDir)
	{
		return new File(installDir, "libraries/net/minecraftforge/forge/" + this.forgeVersion + "/" + "forge-" + this.forgeVersion + ".jar").exists();
	}
	
	/**
	 * This function installs a Forge version at the specified directory.
	 * @param dirToInstall Specified directory.
	 */
	public void install(final File dirToInstall)
	{
    	this.callback.step(Step.FORGE);
    	this.logger.info("Installing forge, version: " + this.forgeVersion + "...");
	}
	
	/**
	 * This function installs mods at the specified directory.
	 * @param modsDir Specified mods directory.
	 * @throws IOException If install fail.
	 */
	public void installMods(File modsDir) throws Exception
	{
		this.callback.step(Step.MODS);
		this.downloadInfos.getMods().forEach(mod -> {
			try
			{
				IOUtils.download(this.logger, new URL(mod.getDownloadURL()), new File(modsDir, mod.getName()));
			}
			catch (MalformedURLException e)
			{
				this.logger.printStackTrace(e);
			}
			this.downloadInfos.incrementDownloaded();
			this.callback.update(this.downloadInfos.getDownloaded(), this.downloadInfos.getTotalToDownload());
		});
		
		if(this.useFileDeleter)
		{
			final List<File> badFiles = new ArrayList<>();
			final List<File> verifiedFiles = new ArrayList<>();
			for(File fileInDir : modsDir.listFiles())
			{
				if(!fileInDir.isDirectory())
				{
					for(Mod mod : this.mods)
					{
						final File file = new File(modsDir, mod.getName().endsWith(".jar") ? mod.getName() : mod.getName() + ".jar");
						if(file.getName().equalsIgnoreCase(fileInDir.getName()))
						{
							if(getSHA1(fileInDir).equals(mod.getSha1()) && getFileSizeBytes(fileInDir) == mod.getSize())
							{
								if(badFiles.contains(fileInDir))
									badFiles.remove(fileInDir);
								verifiedFiles.add(fileInDir);
							}
							else badFiles.add(fileInDir);
						}
						else
						{
							if(!verifiedFiles.contains(fileInDir))
								badFiles.add(fileInDir);
						}
					}
				}
			}
			
			badFiles.forEach(File::delete);
			badFiles.clear();
		}
	}
	
	public boolean isModFileDeleterEnabled()
	{
		return this.useFileDeleter;
	}

	public AbstractForgeVersion enableModFileDeleter()
	{
		this.useFileDeleter = true;
		return this;
	}

	public AbstractForgeVersion disableModFileDeleter()
	{
		this.useFileDeleter = false;
		return this;
	}
	
	public void appendDownloadInfos(DownloadInfos infos)
	{
		this.downloadInfos = infos;
	}
	
	protected void packPatchedInstaller(final File tempDir, final File tempInstallerDir) throws IOException
    {
        final File output = new File(tempDir, "forge-installer-patched.zip");
        FileUtils.compressFiles(tempInstallerDir.listFiles(), output);
        Files.move(output.toPath(), new File(output.getAbsolutePath().replace(".zip", ".jar")).toPath(), StandardCopyOption.REPLACE_EXISTING);
        tempInstallerDir.delete();
    }

    public AbstractForgeVersion withCurseMods(List<CurseModInfos> curseMods)
	{
		this.curseMods = curseMods;
		return this;
	}
	
	public List<Mod> getMods()
	{
		return this.mods;
	}
	
    public ILogger getLogger()
    {
        return this.logger;
    }

    public String getForgeVersion()
    {
        return this.forgeVersion;
    }

    public URL getInstallerUrl()
    {
        return this.installerUrl;
    }

	public List<CurseModInfos> getCurseMods()
	{
		return this.curseMods;
	}
}
