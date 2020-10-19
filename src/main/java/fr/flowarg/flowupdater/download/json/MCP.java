package fr.flowarg.flowupdater.download.json;

import com.google.gson.JsonObject;
import fr.flowarg.flowupdater.utils.IOUtils;

import java.net.MalformedURLException;
import java.net.URL;

public class MCP
{
    private final String clientURL;
    private final String clientSha1;
    private final long clientSize;
    private final long serverSize;
    private final String serverURL;
    private final String serverSha1;
    
    /**
     * Construct a new MCP.
     * @param clientURL URL of client.jar
     * @param clientSha1 SHA1 of client.jar
     * @param serverURL URL of server.jar
     * @param serverSha1 SHA1 of server.jar
     * @param clientSize Size (bytes) of client.jar
     * @param serverSize Size (bytes) of server.jar
     */
    public MCP(String clientURL, String clientSha1, String serverURL, String serverSha1, long clientSize, long serverSize)
    {
        this.clientURL = clientURL;
        this.clientSha1 = clientSha1;
        this.serverURL = serverURL;
        this.serverSha1 = serverSha1;
        this.clientSize = clientSize;
        this.serverSize = serverSize;
    }
    
    /**
     * Provide a MCP instance from a JSON file.
     * Template of a JSON file :
     * {
     *     "clientURL": "https://url.com/launcher/client.jar",
     *     "clientSha1": "9b0a9d70320811e7af2e8741653f029151a6719a",
     *     "serverURL": "https://url.com/launcher/server.jar",
     *     "serverSha1": "777039aab46578247b8954e2f7d482826315fca8",
     *     "clientSize": 1234,
     *     "serverSize": 1234
     * }
     * @param jsonUrl the JSON file URL.
     * @return the MCP instance.
     */
    public static MCP getMCPFromJson(URL jsonUrl)
    {
        final JsonObject object = IOUtils.readJson(jsonUrl).getAsJsonObject();
        
        return new MCP(object.get("clientURL").getAsString(),
                object.get("clientSha1").getAsString(),
                object.get("serverURL").getAsString(),
                object.get("serverSha1").getAsString(),
                object.get("clientSize").getAsLong(),
                object.get("serverSize").getAsLong());
    }

    public static MCP getMCPFromJson(String jsonUrl)
    {
        try
        {
            return getMCPFromJson(new URL(jsonUrl));
        } catch (MalformedURLException e)
        {
            e.printStackTrace();
            return new MCP("", "", "", "", -1, -1);
        }
    }
    
    public String getClientURL()
    {
        return this.clientURL;
    }
    public String getClientSha1()
    {
        return this.clientSha1;
    }
    public String getServerURL()
    {
        return this.serverURL;
    }
    public String getServerSha1()
    {
        return this.serverSha1;
    }
    public long getClientSize()
    {
        return this.clientSize;
    }
    public long getServerSize()
    {
        return this.serverSize;
    }
}
