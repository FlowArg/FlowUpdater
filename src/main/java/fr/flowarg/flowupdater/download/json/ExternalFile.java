package fr.flowarg.flowupdater.download.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.flowarg.flowupdater.utils.FlowUpdaterException;
import fr.flowarg.flowupdater.utils.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents an external file object.
 */
public class ExternalFile
{
    private final String path;
    private final String downloadURL;
    private final String sha1;
    private final long size;
    private final boolean update;
    
    /**
     * Construct a new ExternalFile object.
     * @param path Path of external file.
     * @param sha1 Sha1 of external file.
     * @param size Size of external file.
     * @param downloadURL external file URL.
     */
    public ExternalFile(String path, String downloadURL, String sha1, long size)
    {
        this.path = path;
        this.downloadURL = downloadURL;
        this.sha1 = sha1;
        this.size = size;
        this.update = true;
    }

    /**
     * Construct a new ExternalFile object.
     * @param path Path of external file.
     * @param sha1 Sha1 of external file.
     * @param size Size of external file.
     * @param downloadURL external file URL.
     * @param update false: not checking if the file is valid. true: checking if the file is valid.
     */
    public ExternalFile(String path, String downloadURL, String sha1, long size, boolean update)
    {
        this.path = path;
        this.downloadURL = downloadURL;
        this.sha1 = sha1;
        this.size = size;
        this.update = update;
    }
    
    /**
     * Provide a List of external file from a JSON file.
     * Template of a JSON file :
     * <pre>
     * {
     *   "extfiles": [
     *     {
     *       "path": "other/path/AnExternalFile.binpatch",
     *       "downloadURL": "https://url.com/launcher/extern/AnExtFile.binpatch",
     *       "sha1": "40f784892989du0fc6f45c895d4l6c5db9378f48",
     *       "size": 25652
     *     },
     *     {
     *       "path": "config/config.json",
     *       "downloadURL": "https://url.com/launcher/ext/modconfig.json",
     *       "sha1": "eef74b3fbab6400cb14b02439cf092cca3c2125c",
     *       "size": 19683,
     *       "update": false
     *     }
     *   ]
     * }
     * </pre>
     * @param jsonUrl the JSON file URL.
     * @return an external file list.
     */
    public static @NotNull List<ExternalFile> getExternalFilesFromJson(URL jsonUrl)
    {
        final List<ExternalFile> result = new ArrayList<>();
        final JsonArray extfiles = IOUtils.readJson(jsonUrl).getAsJsonObject().getAsJsonArray("extfiles");
        extfiles.forEach(extFileElement -> {
            final JsonObject obj = extFileElement.getAsJsonObject();
            final String path = obj.get("path").getAsString();
            final String sha1 = obj.get("sha1").getAsString();
            final String downloadURL = obj.get("downloadURL").getAsString();
            final long size = obj.get("size").getAsLong();
            if(obj.get("update") != null)
                result.add(new ExternalFile(path, downloadURL, sha1, size, obj.get("update").getAsBoolean()));
            else result.add(new ExternalFile(path, downloadURL, sha1, size));
        });
        return result;
    }

    /**
     * Provide a List of external file from a JSON file.
     * @param jsonUrl the JSON file URL.
     * @return an external file list.
     */
    public static @NotNull List<ExternalFile> getExternalFilesFromJson(String jsonUrl)
    {
        try
        {
            return getExternalFilesFromJson(new URL(jsonUrl));
        } catch (Exception e)
        {
            throw new FlowUpdaterException(e);
        }
    }

    /**
     * Get the path of the external file.
     * @return the path of the external file.
     */
    public String getPath()
    {
        return this.path;
    }

    /**
     * Get the url of the external file.
     * @return the url of the external file.
     */
    public String getDownloadURL()
    {
        return this.downloadURL;
    }

    /**
     * Get the sha1 of the external file.
     * @return the sha1 of the external file.
     */
    public String getSha1()
    {
        return this.sha1;
    }

    /**
     * Get the size of the external file.
     * @return the size of the external file.
     */
    public long getSize()
    {
        return this.size;
    }

    /**
     * Should {@link fr.flowarg.flowupdater.utils.ExternalFileDeleter} check the file?
     * @return if the external file deleter should check and delete the file.
     */
    public boolean isUpdate()
    {
        return this.update;
    }
}
