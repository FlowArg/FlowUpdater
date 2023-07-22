package fr.flowarg.flowupdater.download.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import fr.flowarg.flowupdater.utils.FlowUpdaterException;
import fr.flowarg.flowupdater.utils.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ModrinthVersionInfo
{
    private String projectReference = "";
    private String versionNumber = "";
    private String versionId = "";

    /**
     * Construct a new ModrinthVersionInfo object.
     * @param projectReference the project reference can be slug or id.
     * @param versionNumber the version number (and NOT the version name unless they are the same).
     */
    public ModrinthVersionInfo(String projectReference, String versionNumber)
    {
        this.projectReference = projectReference.trim();
        this.versionNumber = versionNumber.trim();
    }

    /**
     * Construct a new ModrinthVersionInfo object.
     * This constructor doesn't need a project reference because
     * we can access the version without any project information.
     * @param versionId the version id.
     */
    public ModrinthVersionInfo(String versionId)
    {
        this.versionId = versionId.trim();
    }

    public static @NotNull List<ModrinthVersionInfo> getModrinthVersionsFromJson(URL jsonUrl)
    {
        final List<ModrinthVersionInfo> result = new ArrayList<>();
        final JsonObject object = IOUtils.readJson(jsonUrl).getAsJsonObject();
        final JsonArray mods = object.getAsJsonArray("modrinthMods");
        mods.forEach(modElement -> {
            final JsonObject obj = modElement.getAsJsonObject();
            final JsonElement versionIdElement = obj.get("versionId");

            if(versionIdElement instanceof JsonNull)
                result.add(new ModrinthVersionInfo(obj.get("projectReference").getAsString(), obj.get("versionNumber").getAsString()));
            else result.add(new ModrinthVersionInfo(versionIdElement.getAsString()));
        });
        return result;
    }

    public static @NotNull List<ModrinthVersionInfo> getModrinthVersionsFromJson(String jsonUrl)
    {
        try
        {
            return getModrinthVersionsFromJson(new URL(jsonUrl));
        } catch (MalformedURLException e)
        {
            throw new FlowUpdaterException(e);
        }
    }

    public String getProjectReference()
    {
        return this.projectReference;
    }

    public String getVersionNumber()
    {
        return this.versionNumber;
    }

    public String getVersionId()
    {
        return this.versionId;
    }
}
