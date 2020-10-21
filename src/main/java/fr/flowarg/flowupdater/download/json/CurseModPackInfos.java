package fr.flowarg.flowupdater.download.json;

public class CurseModPackInfos extends CurseFileInfos
{
    private final boolean installExtFiles;
    private final String[] excluded;

    public CurseModPackInfos(int projectID, int fileID, boolean installExtFiles, String... excluded)
    {
        super(projectID, fileID);
        this.installExtFiles = installExtFiles;
        this.excluded = excluded;
    }

    public boolean isInstallExtFiles()
    {
        return this.installExtFiles;
    }

    public String[] getExcluded()
    {
        return this.excluded;
    }
}
