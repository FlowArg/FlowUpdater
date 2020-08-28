package fr.flowarg.flowupdater.utils;

public class UpdaterOptions
{
	/** Is the update silent */
	private final boolean silentUpdate;
	
	/** Reextract natives at each updates ? */
	private final boolean reextractNatives;
	
	/** Enable/Disable forge fixes */
	private final boolean disableForgeHacks;
	
	public UpdaterOptions(boolean silentUpdate, boolean reextractNatives, boolean disableForgeHacks)
	{
		this.silentUpdate = silentUpdate;
		this.reextractNatives = reextractNatives;
		this.disableForgeHacks = disableForgeHacks;
	}
	
	public boolean isReextractNatives()
	{
		return this.reextractNatives;
	}
	
	public boolean isSilentUpdate()
	{
		return this.silentUpdate;
	}
	
	public boolean disableForgeHacks()
	{
		return this.disableForgeHacks;
	}
}
