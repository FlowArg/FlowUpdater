package fr.flowarg.flowupdater.download.json;

/**
 * This class represents a mod pack file in the CurseForge API.
 */
public class CurseModPackInfo extends CurseFileInfo
{
    private final boolean installExtFiles;
    private final String[] excluded;

    private String url = "";

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
     * Construct a new CurseModPackInfo object.
     * @param url the url of the custom mod pack endpoint.
     * @param installExtFiles should install external files like config and resource packs.
     * @param excluded mods to exclude.
     */
    public CurseModPackInfo(String url, boolean installExtFiles, String... excluded)
    {
        super(0, 0);
        this.url = url;
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

    /**
     * Get the url of the mod pack endpoint.
     * Should be of the form:
     * {
     *     "data": {
     *         "fileName": "modpack.zip",
     *         "downloadUrl": "https://site.com/modpack.zip",
     *         "fileLength": 123456789,
     *         "hashes": [
     *             {
     *                 "value": "a02b0499589bc6982fced96dcc85c3b3e33af119",
     *                 "algo": 1
     *             }
     *         ]
     *     }
     * }
     * @return the url of the mod pack endpoint if it's not from CurseForge's servers.
     */
    public String getUrl()
    {
        return this.url;
    }
}
