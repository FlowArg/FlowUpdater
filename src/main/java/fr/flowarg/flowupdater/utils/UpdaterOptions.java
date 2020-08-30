package fr.flowarg.flowupdater.utils;

public class UpdaterOptions
{
	/** Is the update silent (Recommended : true) */
	private final boolean silentUpdate;
	
	/** Reextract natives at each updates ? (Recommended : false for old versions like 1.7.10) */
	private final boolean reextractNatives;
	
	/** Enable/Disable forge fixes (Recommended : false for new Forge Versions like 1.14.4) */
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
