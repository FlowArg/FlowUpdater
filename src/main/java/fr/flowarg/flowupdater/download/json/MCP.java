package fr.flowarg.flowupdater.download.json;

import com.google.gson.JsonObject;
import fr.flowarg.flowupdater.utils.FlowUpdaterException;
import fr.flowarg.flowupdater.utils.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class represents an MCP object.
 */
public class MCP
{
    private final String clientURL;
    private final String clientSha1;
    private final long clientSize;

    /**
     * Construct a new MCP object.
     * @param clientURL URL of client.jar
     * @param clientSha1 SHA1 of client.jar
     * @param clientSize Size (bytes) of client.jar
     */
    public MCP(String clientURL, String clientSha1, long clientSize)
    {
        this.clientURL = clientURL;
        this.clientSha1 = clientSha1;
        this.clientSize = clientSize;
    }
    
    /**
     * Provide an MCP instance from a JSON file.
     * Template of a JSON file :
     * <pre>
     * {
     *   "clientURL": "https://url.com/launcher/client.jar",
     *   "clientSha1": "9b0a9d70320811e7af2e8741653f029151a6719a",
     *   "clientSize": 1234
     * }
     * </pre>
     * @param jsonUrl the JSON file URL.
     * @return the MCP instance.
     */
    public static @NotNull MCP getMCPFromJson(URL jsonUrl)
    {
        final JsonObject object = IOUtils.readJson(jsonUrl).getAsJsonObject();
        return new MCP(object.get("clientURL").getAsString(), object.get("clientSha1").getAsString(), object.get("clientSize").getAsLong());
    }

    /**
     * Provide an MCP instance from a JSON file.
     * @param jsonUrl the JSON file URL.
     * @return the MCP instance.
     */
    public static @NotNull MCP getMCPFromJson(String jsonUrl)
    {
        try
        {
            return getMCPFromJson(new URL(jsonUrl));
        } catch (MalformedURLException e)
        {
            throw new FlowUpdaterException(e);
        }
    }

    /**
     * Return the client url.
     * @return the client url.
     */
    public String getClientURL()
    {
        return this.clientURL;
    }

    /**
     * Return the client sha1.
     * @return the client sha1.
     */
    public String getClientSha1()
    {
        return this.clientSha1;
    }

    /**
     * Return the client size.
     * @return the client size.
     */
    public long getClientSize()
    {
        return this.clientSize;
    }
}
