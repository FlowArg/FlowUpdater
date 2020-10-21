package fr.flowarg.flowupdater.download.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.flowarg.flowupdater.utils.IOUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CurseFileInfos
{
    private final int projectID;
    private final int fileID;

    public CurseFileInfos(int projectID, int fileID)
    {
        this.projectID = projectID;
        this.fileID = fileID;
    }

    public static List<CurseFileInfos> getFilesFromJson(URL jsonUrl)
    {
        final List<CurseFileInfos> result = new ArrayList<>();
        final JsonObject object = IOUtils.readJson(jsonUrl).getAsJsonObject();
        final JsonArray mods = object.getAsJsonArray("curseFiles");
        mods.forEach(curseModElement -> {
            final JsonObject obj = curseModElement.getAsJsonObject();
            final int projectID = obj.get("projectID").getAsInt();
            final int fileID = obj.get("fileID").getAsInt();
            result.add(new CurseFileInfos(projectID, fileID));
        });
        return result;
    }

    public static List<CurseFileInfos> getFilesFromJson(String jsonUrl)
    {
        try
        {
            return getFilesFromJson(new URL(jsonUrl));
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
            return Collections.emptyList();
        }
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
