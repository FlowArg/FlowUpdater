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
	
	public UpdaterOptions(boolean silentRead, boolean reextractNatives)
	{
		this.silentRead = silentRead;
		this.reextractNatives = reextractNatives;
	}
	
	public boolean isReextractNatives()
	{
		return this.reextractNatives;
	}
	
	public boolean isSilentRead()
	{
		return this.silentRead;
	}
}
