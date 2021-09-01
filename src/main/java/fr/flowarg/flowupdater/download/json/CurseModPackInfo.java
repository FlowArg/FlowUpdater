package fr.flowarg.flowupdater.download.json;

/**
 * This class represent a mod pack file in the CurseForge API.
 */
public class CurseModPackInfo extends CurseFileInfo
{
    private final boolean installExtFiles;
    private final String[] excluded;

    /**
     * Construct a new CurseModPackInfo object.
     * @param projectID the ID of the project.
     * @param fileID the ID of the file.
     * @param installExtFiles should install external files like config and resource packs.
     * @param excluded mods to exclude.
     */
    public CurseModPackInfo(int projectID, int fileID, boolean installExtFiles, String... excluded)
    {
        super(projectID, fileID);
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
