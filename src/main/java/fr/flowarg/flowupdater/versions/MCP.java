package fr.flowarg.flowupdater.versions;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MCP
{
	private String clientDownloadURL;
	private String clientSha1;
	private int clientSize;
	private int serverSize;
	private String serverDownloadURL;
	private String serverSha1;
	
	/**
	 * Construct a new MCP.
	 * @param clientDownloadURL URL of client.jar
	 * @param clientSha1 SHA1 of client.jar
	 * @param serverDownloadURL URL of server.jar
	 * @param serverSha1 SHA1 of server.jar
	 * @param clientSize Size (bytes) of client.jar
	 * @param serverSize Size (bytes) of server.jar
	 */
	public MCP(String clientDownloadURL, String clientSha1, String serverDownloadURL, String serverSha1, int clientSize, int serverSize)
	{
		this.clientDownloadURL = clientDownloadURL;
		this.clientSha1 = clientSha1;
		this.serverDownloadURL = serverDownloadURL;
		this.serverSha1 = serverSha1;
		this.clientSize = clientSize;
		this.serverSize = serverSize;
	}
	
	/**
	 * Provide a MCP instance from a JSON file.
	 * Template of a JSON file :
	 * {
	 * 	"clientURL": "https://url.com/launcher/client.jar",
	 * 	"clientSha1": "9b0a9d70320811e7af2e8741653f029151a6719a",
	 * 	"serverURL": "https://url.com/launcher/server.jar",
	 * 	"serverSha1": "777039aab46578247b8954e2f7d482826315fca8",
	 * 	"clientSize": 1234,
	 * 	"serverSize": 1234
	 * }
	 * @param url the JSON file URL.
	 * @return the MCP instance.
	 */
	public static MCP fromJson(URL url)
	{
		JsonElement element = JsonNull.INSTANCE;
        try(InputStream stream = new BufferedInputStream(url.openStream()))
        {
            final Reader reader = new BufferedReader(new InputStreamReader(stream));
            final StringBuilder sb = new StringBuilder();

            int character;
            while ((character = reader.read()) != -1) sb.append((char)character);

            element =  JsonParser.parseString(sb.toString());
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
        final JsonObject object = element.getAsJsonObject();
        
        return new MCP(object.get("clientURL").getAsString(),
        		object.get("clientSha1").getAsString(),
        		object.get("serverURL").getAsString(),
        		object.get("serverSha1").getAsString(),
        		object.get("clientSize").getAsInt(),
        		object.get("serverSize").getAsInt());
	}
	
	public String getClientDownloadURL()
	{
		return this.clientDownloadURL;
	}
	
	public String getClientSha1()
	{
		return this.clientSha1;
	}
	
	public String getServerDownloadURL()
	{
		return this.serverDownloadURL;
	}
	
	public String getServerSha1()
	{
		return this.serverSha1;
	}
	
	public int getClientSize()
	{
		return this.clientSize;
	}
	
	public int getServerSize()
	{
		return this.serverSize;
	}
}
