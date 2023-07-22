package fr.flowarg.flowupdater.download.json;

/**
 * This class represents an OptiFineInfo object.
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
     * Construct a new OptiFineInfo object, use {@link OptiFineInfo#OptiFineInfo(String, boolean)} .
     * @param version the OptiFine's version.
     */
    public OptiFineInfo(String version)
    {
        this(version, false);
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
