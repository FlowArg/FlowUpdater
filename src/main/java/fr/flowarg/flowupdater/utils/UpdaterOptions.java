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

    /** Select some mods from CurseForge ?
     * WARNING: IF THIS FIELD IS THE TO TRUE, IT WILL DOWNLOAD AND LOAD A PLUGIN ; DISABLE THIS OPTION IF YOU DON'T USE CURSEFORGE !!
     */
    private final boolean enableModsFromCurseForge;
    
    public UpdaterOptions(boolean silentRead, boolean reextractNatives, boolean enableModsFromCurseForge)
    {
        this.silentRead = silentRead;
        this.reextractNatives = reextractNatives;
        this.enableModsFromCurseForge = enableModsFromCurseForge;
    }
    
    public boolean isReextractNatives()
    {
        return this.reextractNatives;
    }
    
    public boolean isSilentRead()
    {
        return this.silentRead;
    }
    
    public boolean isEnableModsFromCurseForge()
    {
        return this.enableModsFromCurseForge;
    }
}
