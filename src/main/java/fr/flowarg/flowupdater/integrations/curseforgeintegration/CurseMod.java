package fr.flowarg.flowupdater.integrations.curseforgeintegration;

/**
 * Basic object that represents a CurseForge's mod.
 */
public class CurseMod
{
    private final String name;
    private final String downloadURL;
    private final String sha1;
    private final long length;

    CurseMod(String name, String downloadURL, String sha1, long length)
    {
        this.name = name;
        this.downloadURL = downloadURL;
        this.sha1 = sha1;
        this.length = length;
    }

    /**
     * Get the mod's name.
     * @return the mod's name.
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Get the mod's download url.
     * @return the mod's download url.
     */
    public String getDownloadURL()
    {
        return this.downloadURL;
    }

    /**
     * Get the mod's sha1.
     * @return the mod's sha1.
     */
    public String getSha1()
    {
        return this.sha1;
    }

    /**
     * Get the mod's length.
     * @return the mod's length.
     */
    public long getLength()
    {
        return this.length;
    }
}
