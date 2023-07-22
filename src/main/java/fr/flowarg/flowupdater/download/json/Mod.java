package fr.flowarg.flowupdater.download.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.flowarg.flowupdater.utils.FlowUpdaterException;
import fr.flowarg.flowupdater.utils.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a Mod object.
 */
public class Mod
{
    private final String name;
    private final String sha1;
    private final long size;
    private final String downloadURL;
    
    /**
     * Construct a new Mod object.
     * @param name Name of mod file.
     * @param downloadURL Mod download URL.
     * @param sha1 Sha1 of mod file.
     * @param size Size of mod file.
     */
    public Mod(String name, String downloadURL, String sha1, long size)
    {
        this.name = name;
        this.downloadURL = downloadURL;
        this.sha1 = sha1;
        this.size = size;
    }
    
    /**
     * Provide a List of Mods from a JSON file.
     * Template of a JSON file :
     * <pre>
     * {
     *   "mods": [
     *     {
     *       "name": "KeyStroke",
     *       "downloadURL": "https://url.com/launcher/mods/KeyStroke.jar",
     *       "sha1": "70e564892989d8bbc6f45c895df56c5db9378f48",
     *       "size": 1234
     *     },
     *     {
     *       "name": "JourneyMap",
     *       "downloadURL": "https://url.com/launcher/mods/JourneyMap.jar",
     *       "sha1": "eef74b3fbab6400cb14b02439cf092cca3c2125c",
     *       "size": 1234
     *     }
     *   ]
     * }
     * </pre>
     * @param jsonUrl the JSON file URL.
     * @return a Mod list.
    */
    public static @NotNull List<Mod> getModsFromJson(URL jsonUrl)
    {
        final List<Mod> result = new ArrayList<>();
        final JsonObject object = IOUtils.readJson(jsonUrl).getAsJsonObject();
        final JsonArray mods = object.getAsJsonArray("mods");
        mods.forEach(modElement -> result.add(fromJson(modElement)));
        return result;
    }

    public static Mod fromJson(JsonElement modElement)
    {
        final JsonObject obj = modElement.getAsJsonObject();

        return new Mod(
                obj.get("name").getAsString(),
                obj.get("downloadURL").getAsString(),
                obj.get("sha1").getAsString(),
                obj.get("size").getAsLong()
        );
    }

    /**
     * Provide a List of Mods from a JSON file.
     * Template of a JSON file :
     * @param jsonUrl the JSON file URL.
     * @return a Mod list.
     */
    public static @NotNull List<Mod> getModsFromJson(String jsonUrl)
    {
        try
        {
            return getModsFromJson(new URL(jsonUrl));
        }
        catch (Exception e)
        {
            throw new FlowUpdaterException(e);
        }
    }

    /**
     * Get the mod name.
     * @return the mod name.
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Get the sha1 of the mod.
     * @return the sha1 of the mod.
     */
    public String getSha1()
    {
        return this.sha1;
    }

    /**
     * Get the mod size.
     * @return the mod size.
     */
    public long getSize()
    {
        return this.size;
    }

    /**
     * Get the mod url.
     * @return the mod url.
     */
    public String getDownloadURL()
    {
        return this.downloadURL;
    }
}
