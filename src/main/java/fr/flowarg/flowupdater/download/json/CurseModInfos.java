package fr.flowarg.flowupdater.download.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.flowarg.flowupdater.utils.IOUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class CurseModInfos
{
    private final int projectID;
    private final int fileID;

    public CurseModInfos(int projectID, int fileID)
    {
        this.projectID = projectID;
        this.fileID = fileID;
    }

    public static List<CurseModInfos> getModsFromJson(URL jsonUrl)
    {
        final List<CurseModInfos> result = new ArrayList<>();
        final JsonObject object = IOUtils.readJson(jsonUrl).getAsJsonObject();
        final JsonArray mods = object.getAsJsonArray("curseMods");
        mods.forEach(curseModElement -> {
            final JsonObject obj = curseModElement.getAsJsonObject();
            final int projectID = obj.get("projectID").getAsInt();
            final int fileID = obj.get("fileID").getAsInt();
            result.add(new CurseModInfos(projectID, fileID));
        });
        return result;
    }

    public static List<CurseModInfos> getModsFromJson(String jsonUrl)
    {
        try
        {
            return getModsFromJson(new URL(jsonUrl));
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
            return new ArrayList<>();
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
