package fr.flowarg.flowupdater.download.json;

/**
 * This class represent an OptiFineInfo object.
 */
public class OptiFineInfo
{
    private final String version;
    private final boolean preview;

    /**
     * Construct a new OptiFineInfo object.
     * @param version the OptiFine's version.
     * @param preview if the version is a preview.
     */
    public OptiFineInfo(String version, boolean preview)
    {
        this.version = version;
        this.preview = preview;
    }

    /**
     * Get the OptiFine's version.
     * @return the OptiFine's version.
     */
    public String getVersion()
    {
        return this.version;
    }

    /**
     * Is the version a preview?
     * @return if the version is a preview or not.
     */
    public boolean isPreview()
    {
        return this.preview;
    }
}
