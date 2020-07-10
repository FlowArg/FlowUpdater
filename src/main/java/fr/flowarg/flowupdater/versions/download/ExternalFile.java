package fr.flowarg.flowupdater.versions.download;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ExternalFile
{
	private String path;
	private String downloadURL;
	private String sha1;
	
	/**
	 * Construct a new ExternalFile object.
	 * @param path
	 * @param downloadURL
	 * @param sha1
	 */
	public ExternalFile(String path, String downloadURL, String sha1)
	{
		this.path = path;
		this.downloadURL = downloadURL;
		this.sha1 = sha1;
	}
	
	/**
	 * Provide a List of external file from a JSON file.
	 * Template of a JSON file :
	 * {
	 * 	"extfiles": [
	 * 	{
	 * 		"path": "other/path/AnExternalFile.binpatch",
	 * 		"downloadURL": "https://url.com/launcher/extern/AnExtFile.binpatch",
	 * 		"sha1": "40f784892989du0fc6f45c895d4l6c5db9378f48"
	 * 	},
	 * 	{
	 * 		"path": "config/config.json",
	 * 		"downloadURL": "https://url.com/launcher/ext/modconfig.json",
	 * 		"sha1": "eef74b3fbab6400cb14b02439cf092cca3c2125c"
	 * 	}
	 * 	]
	 * }
	 * @param url the JSON file URL.
	 * @return an external file list.
	 */
	public static ArrayList<ExternalFile> fromJson(URL json)
	{
		final ArrayList<ExternalFile> result = new ArrayList<>();
		JsonElement element = JsonNull.INSTANCE;
        try(InputStream stream = new BufferedInputStream(json.openStream()))
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
        final JsonArray extfiles = object.getAsJsonArray("extfiles");
        extfiles.forEach(extFileElement -> {
        	final JsonObject obj = extFileElement.getAsJsonObject();
        	final String path = obj.get("path").getAsString();
        	final String sha1 = obj.get("sha1").getAsString();
        	final String downloadURL = obj.get("downloadURL").getAsString();
        	
        	result.add(new ExternalFile(path, downloadURL, sha1));
        });
        return result;
	}
	
	public String getPath()
	{
		return this.path;
	}
	
	public String getDownloadURL()
	{
		return this.downloadURL;
	}
	
	public String getSha1()
	{
		return this.sha1;
	}
}
