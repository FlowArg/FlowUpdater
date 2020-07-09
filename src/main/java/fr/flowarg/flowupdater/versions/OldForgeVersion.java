package fr.flowarg.flowupdater.versions;

import static fr.flowarg.flowio.FileUtils.getFileSizeBytes;
import static fr.flowarg.flowio.FileUtils.getSHA1;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

import fr.flowarg.flowlogger.Logger;
import fr.flowarg.flowupdater.versions.download.IProgressCallback;
import fr.flowarg.flowupdater.versions.download.Mod;
import fr.flowarg.flowupdater.versions.download.Step;

/**
 * Represent an old Forge version (1.7 -> 1.12.2)
 * @author FlowArg
 */
public class OldForgeVersion implements IForgeVersion
{
	private final Logger logger;
	private final String forgeVersion;
	private final IVanillaVersion vanilla;
	private final IProgressCallback callback;
	private List<Mod> mods;
	
	public OldForgeVersion(String forgeVersion, IVanillaVersion vanilla, Logger logger, IProgressCallback callback, List<Mod> mods)
	{
		this.forgeVersion = forgeVersion;
		this.logger = logger;
		this.vanilla = vanilla;
		this.callback = callback;
		this.mods = mods;
	}
	
	@Override
	public void install(File dirToInstall)
	{
		this.callback.step(Step.FORGE);
		this.logger.info("Installing old forge version : " + this.forgeVersion + "...");
		// TODO make this.
	}
	
	public String getForgeVersion()
	{
		return this.forgeVersion;
	}
	
	public Logger getLogger()
	{
		return this.logger;
	}
	
	public IVanillaVersion getVanilla()
	{
		return this.vanilla;
	}
	
	public List<Mod> getMods()
	{
		return this.mods;
	}

	@Override
	public void installMods(File modsDir) throws IOException
	{
		for(Mod mod : this.mods)
		{
	        final File file = new File(modsDir, mod.getName());

	        if (file.exists())
	        {
	            if (!Objects.requireNonNull(getSHA1(file)).equals(mod.getSha1()) || getFileSizeBytes(file) != mod.getSize())
	            {
	                file.delete();
	                this.download(new URL(mod.getDownloadURL()), file);
	            }
	        }
	        else this.download(new URL(mod.getDownloadURL()), file);
		}
	}
	
    private void download(URL in, File out) throws IOException
    {
        this.logger.info(String.format("[Downloader] Downloading %s from %s...", out.getName(), in.toExternalForm()));
        out.getParentFile().mkdirs();
        Files.copy(in.openStream(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
