package fr.flowarg.flowupdater.utils;

/**
 * Represent some settings for FlowUpdater
 * @author flow
 */
public class UpdaterOptions
{
	/** Is the update silent (Recommended : true) */
	private final boolean silentUpdate;
	
	/** Reextract natives at each updates ? (Recommended : false for old versions like 1.7.10) */
	private final boolean reextractNatives;
	
	public UpdaterOptions(boolean silentUpdate, boolean reextractNatives)
	{
		this.silentUpdate = silentUpdate;
		this.reextractNatives = reextractNatives;
	}
	
	public boolean isReextractNatives()
	{
		return this.reextractNatives;
	}
	
	public boolean isSilentUpdate()
	{
		return this.silentUpdate;
	}
}
