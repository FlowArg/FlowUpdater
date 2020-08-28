package fr.flowarg.flowupdater.download.json;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Mod
{
	private String name;
	private String sha1;
	private int size;
	private String downloadURL;
	
	/**
	 * Construct a new Mod object.
	 * @param name Name of mod file.
	 * @param sha1 Sha1 of mod file.
	 * @param size Size of mod file.
	 * @param downloadURL Mod download URL.
	 */
	public Mod(String name, String sha1, int size, String downloadURL)
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
	 * 	"mods": [
	 * 	{
	 * 		"name": "KeyStroke",
	 * 		"downloadURL": "https://url.com/launcher/mods/KeyStroke.jar",
	 * 		"sha1": "70e564892989d8bbc6f45c895df56c5db9378f48",
	 * 		"size": 1234
	 * 	},
	 * 	{
	 * 		"name": "JourneyMap",
	 * 		"downloadURL": "https://url.com/launcher/mods/JourneyMap.jar",
	 * 		"sha1": "eef74b3fbab6400cb14b02439cf092cca3c2125c",
	 * 		"size": 1234
	 * 	}
	 * 	]
	 * }
	 * @param url the JSON file URL.
	 * @return a Mod list.
	 */
	public static List<Mod> getModsFromJson(URL jsonUrl)
	{
		final List<Mod> result = new ArrayList<>();
		JsonElement element = JsonNull.INSTANCE;
        try(InputStream stream = new BufferedInputStream(jsonUrl.openStream()))
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
        final JsonArray mods = object.getAsJsonArray("mods");
        mods.forEach(modElement -> {
        	final JsonObject obj = modElement.getAsJsonObject();
        	final String name = obj.get("name").getAsString();
        	final String sha1 = obj.get("sha1").getAsString();
        	final String downloadURL = obj.get("downloadURL").getAsString();
        	final int size = obj.get("size").getAsInt();
        	
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
			return new ArrayList<>();
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
	
	public int getSize()
	{
		return this.size;
	}
	
	public String getDownloadURL()
	{
		return this.downloadURL;
	}
}
