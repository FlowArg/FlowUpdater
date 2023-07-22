package fr.flowarg.flowupdater.download.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.flowarg.flowupdater.utils.FlowUpdaterException;
import fr.flowarg.flowupdater.utils.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class represents a file in the CurseForge API.
 */
public class CurseFileInfo
{
    private final int projectID;
    private final int fileID;

    /**
     * Construct a new CurseFileInfo object.
     * @param projectID the ID of the project.
     * @param fileID the ID of the file.
     */
    public CurseFileInfo(int projectID, int fileID)
    {
        this.projectID = projectID;
        this.fileID = fileID;
    }

    /**
     * Retrieve a collection of {@link CurseFileInfo} by parsing a remote JSON file.
     * @param jsonUrl the url of the remote JSON file.
     * @return a collection of {@link CurseFileInfo}.
     */
    public static @NotNull List<CurseFileInfo> getFilesFromJson(URL jsonUrl)
    {
        final List<CurseFileInfo> result = new ArrayList<>();
        final JsonObject object = IOUtils.readJson(jsonUrl).getAsJsonObject();
        final JsonArray mods = object.getAsJsonArray("curseFiles");
        mods.forEach(curseModElement -> {
            final JsonObject obj = curseModElement.getAsJsonObject();
            final int projectID = obj.get("projectID").getAsInt();
            final int fileID = obj.get("fileID").getAsInt();
            result.add(new CurseFileInfo(projectID, fileID));
        });
        return result;
    }

    /**
     * Retrieve a collection of {@link CurseFileInfo} by parsing a remote JSON file.
     * @param jsonUrl the url of the remote JSON file.
     * @return a collection of {@link CurseFileInfo}.
     */
    public static @NotNull List<CurseFileInfo> getFilesFromJson(String jsonUrl)
    {
        try
        {
            return getFilesFromJson(new URL(jsonUrl));
        }
        catch (Exception e)
        {
            throw new FlowUpdaterException(e);
        }
    }

    /**
     * Get the project ID.
     * @return the project ID.
     */
    public int getProjectID()
    {
        return this.projectID;
    }

    /**
     * Get the file ID.
     * @return the file ID.
     */
    public int getFileID()
    {
        return this.fileID;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        final CurseFileInfo that = (CurseFileInfo)o;
        return this.projectID == that.projectID && this.fileID == that.fileID;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.projectID, this.fileID);
    }
}
