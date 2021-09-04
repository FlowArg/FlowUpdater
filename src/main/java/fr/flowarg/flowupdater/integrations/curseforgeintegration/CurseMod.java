package fr.flowarg.flowupdater.integrations.curseforgeintegration;

/**
 * Basic object that represents a CurseForge's mod.
 */
public class CurseMod
{
    private final String name;
    private final String downloadURL;
    private final String md5;
    private final int length;

    CurseMod(String name, String downloadURL, String md5, int length)
    {
        this.name = name;
        this.downloadURL = downloadURL;
        this.md5 = md5;
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
     * Get the mod's md5.
     * @return the mod's md5.
     */
    public String getMd5()
    {
        return this.md5;
    }

    /**
     * Get the mod's length.
     * @return the mod's length.
     */
    public int getLength()
    {
        return this.length;
    }
}
