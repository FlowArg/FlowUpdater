package fr.flowarg.flowupdater.utils;

public class UpdaterOptions
{
	/** Is the update silent */
	private final boolean silentUpdate;
	
	/** Reextract natives at each updates ? */
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
