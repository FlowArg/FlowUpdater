package fr.flowarg.flowupdater.download.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.flowarg.flowupdater.utils.IOUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Mod
{
    private final String name;
    private final String sha1;
    private final long size;
    private final String downloadURL;
    
    /**
     * Construct a new Mod object.
     * @param name Name of mod file.
     * @param sha1 Sha1 of mod file.
     * @param size Size of mod file.
     * @param downloadURL Mod download URL.
     */
    public Mod(String name, String sha1, long size, String downloadURL)
    {
        this.name = name;
        this.sha1 = sha1;
        this.size = size;
        this.downloadURL =  downloadURL;
    }
    
    /**
     * Provide a List of Mods from a JSON file.
     * Template of a JSON file :
     * {
     *  "mods": [
     *  {
     *     "name": "KeyStroke",
     *     "downloadURL": "https://url.com/launcher/mods/KeyStroke.jar",
     *     "sha1": "70e564892989d8bbc6f45c895df56c5db9378f48",
     *     "size": 1234
     *  },
     *  {
     *     "name": "JourneyMap",
     *     "downloadURL": "https://url.com/launcher/mods/JourneyMap.jar",
     *     "sha1": "eef74b3fbab6400cb14b02439cf092cca3c2125c",
     *     "size": 1234
     *  }
     *  ]
     * }
     * @param jsonUrl the JSON file URL.
     * @return a Mod list.
    */
    public static List<Mod> getModsFromJson(URL jsonUrl)
    {
        final List<Mod> result = new ArrayList<>();
        final JsonObject object = IOUtils.readJson(jsonUrl).getAsJsonObject();
        final JsonArray mods = object.getAsJsonArray("mods");
        mods.forEach(modElement -> {
            final JsonObject obj = modElement.getAsJsonObject();
            final String name = obj.get("name").getAsString();
            final String sha1 = obj.get("sha1").getAsString();
            final String downloadURL = obj.get("downloadURL").getAsString();
            final long size = obj.get("size").getAsLong();
            
            result.add(new Mod(name, sha1, size, downloadURL));
        });
        return result;
    }
    
    public static List<Mod> getModsFromJson(String jsonUrl)
    {
        try
        {
            return getModsFromJson(new URL(jsonUrl));
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
    
    public String getName()
    {
        return this.name;
    }
    public String getSha1()
    {
        return this.sha1;
    }
    public long getSize()
    {
        return this.size;
    }
    public String getDownloadURL()
    {
        return this.downloadURL;
    }
}
