package fr.flowarg.flowupdater.utils;

public class CurseModInfos
{
    private final int projectID;
    private final int fileID;

    public CurseModInfos(int projectID, int fileID)
    {
        this.projectID = projectID;
        this.fileID = fileID;
    }

    public int getProjectID()
    {
        return this.projectID;
    }

    public int getFileID()
    {
        return this.fileID;
    }
}
