package fr.flowarg.flowupdater.utils;

/**
 * Represent some settings for FlowUpdater
 * @author flow
 */
public class UpdaterOptions
{
	/** Is the read silent (Recommended : true) */
	private final boolean silentRead;
	
	/** Reextract natives at each updates ? (Recommended : false for old versions like 1.7.10) */
	private final boolean reextractNatives;

	/** Select some mods from CurseForge ? */
	private final boolean enableModFromCurseForge;
	
	public UpdaterOptions(boolean silentRead, boolean reextractNatives, boolean enableModFromCurseForge)
	{
		this.silentRead = silentRead;
		this.reextractNatives = reextractNatives;
		this.enableModFromCurseForge = enableModFromCurseForge;
	}
	
	public boolean isReextractNatives()
	{
		return this.reextractNatives;
	}
	
	public boolean isSilentRead()
	{
		return this.silentRead;
	}

	public boolean isEnableModFromCurseForge()
	{
		return this.enableModFromCurseForge;
	}
}
