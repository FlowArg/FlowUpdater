package fr.flowarg.flowupdater.minecraft.versions;

import java.io.File;

import fr.flowarg.flowlogger.Logger;

/**
 * Represent an old Forge version (1.7 -> 1.12.2)
 * @author FlowArg
 */
public class OldForgeVersion implements IForgeVersion
{
	private final Logger logger;
	private final String forgeVersion;
	private final IVersion vanilla;
	
	public OldForgeVersion(String forgeVersion, IVersion vanilla, Logger logger)
	{
		this.forgeVersion = forgeVersion;
		this.logger = logger;
		this.vanilla = vanilla;
	}
	
	@Override
	public void install(File dirToInstall)
	{
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
	
	public IVersion getVanilla()
	{
		return this.vanilla;
	}
}
