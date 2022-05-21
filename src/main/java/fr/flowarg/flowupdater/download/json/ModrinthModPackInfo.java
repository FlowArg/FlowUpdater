package fr.flowarg.flowupdater.download.json;

public class ModrinthModPackInfo extends ModrinthVersionInfo
{
    private final boolean installExtFiles;
    private final String[] excluded;

    public ModrinthModPackInfo(String projectReference, String versionNumber, boolean installExtFiles, String... excluded)
    {
        super(projectReference, versionNumber);
        this.installExtFiles = installExtFiles;
        this.excluded = excluded;
    }

    public ModrinthModPackInfo(String versionId, boolean installExtFiles, String... excluded)
    {
        super(versionId);
        this.installExtFiles = installExtFiles;
        this.excluded = excluded;
    }

    /**
     * Get the {@link #installExtFiles} option.
     * @return the {@link #installExtFiles} option.
     */
    public boolean isInstallExtFiles()
    {
        return this.installExtFiles;
    }

    /**
     * Get the excluded mods.
     * @return the excluded mods.
     */
    public String[] getExcluded()
    {
        return this.excluded;
    }
}
