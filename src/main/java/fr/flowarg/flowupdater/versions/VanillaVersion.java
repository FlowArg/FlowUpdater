package fr.flowarg.flowupdater.versions;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.utils.BuilderArgument;
import fr.flowarg.flowupdater.utils.BuilderArgumentException;
import fr.flowarg.flowupdater.utils.IBuilder;

public class VanillaVersion
{
	public static final VanillaVersion NULL_VERSION = new VanillaVersion("no", null, false, null);
	
	private final String name;
	private final MCP mcp;
	private final boolean snapshot;
	private final VersionType versionType;
	
	private JsonElement json;
	
	private VanillaVersion(String name, MCP mcp, boolean snapshot, VersionType versionType)
	{
		this.name = name;
		this.mcp = mcp;
		this.snapshot = snapshot;
		this.versionType = versionType;
		if(!this.name.equals("no"))
			this.json = this.readData(this.getJsonVersion());
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public MCP getMcp()
	{
		return this.mcp;
	}
	
	public boolean isSnapshot()
	{
		return this.snapshot;
	}
	
	public VersionType getVersionType()
	{
		return this.versionType;
	}
	
    public JsonArray getMinecraftLibrariesJson() 
    {
    	return this.json.getAsJsonObject().getAsJsonArray("libraries");
    }
    
    public JsonObject getMinecraftClient() 
    {
    	if(versionType == VersionType.MCP)
    	{
    		final JsonObject result = new JsonObject();
    		final String sha1 = this.mcp.getClientSha1();
    		final String url = this.mcp.getClientDownloadURL();
    		final int size =this.mcp.getClientSize();
    		if(this.checkString(sha1) && this.checkString(url) && size > 0)
    		{
        		result.addProperty("sha1", this.mcp.getClientSha1());
        		result.addProperty("size", this.mcp.getClientSize());
        		result.addProperty("url", this.mcp.getClientDownloadURL());
        		return result;
    		}
    		else FlowUpdater.DEFAULT_LOGGER.warn("Skipped MCP Client");
    	}
    	return this.json.getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("client");
    }
    
    public JsonObject getMinecraftServer() 
    {
    	if(versionType == VersionType.MCP)
    	{
    		final JsonObject result = new JsonObject();
    		final String sha1 = this.mcp.getServerSha1();
    		final String url = this.mcp.getServerDownloadURL();
    		final int size = this.mcp.getServerSize();
    		if(this.checkString(url) && this.checkString(sha1) && size > 0)
    		{
        		result.addProperty("sha1", this.mcp.getServerSha1());
        		result.addProperty("size", this.mcp.getServerSize());
        		result.addProperty("url", this.mcp.getServerDownloadURL());
        		return result;
    		}
    		else FlowUpdater.DEFAULT_LOGGER.warn("Skipped MCP Server");
    	}
    	return this.json.getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("server");
    }

    public JsonObject getMinecraftAssetsIndex() 
    {
    	return this.json.getAsJsonObject().getAsJsonObject("assetIndex");
    }
    
    private boolean checkString(String str)
    {
    	return str != null && !str.trim().equals("");
    }
    
    private InputStream getJsonVersion()
    {
        final AtomicReference<String> version = new AtomicReference<>(null);
        final AtomicReference<InputStream> result = new AtomicReference<>(null);

        try
        {
            final JsonObject launcherMeta = this.readData(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json").openStream()).getAsJsonObject();

            if (this.getName().equals("latest"))
            {
                if (this.snapshot)
                    version.set(launcherMeta.getAsJsonObject("latest").get("snapshot").getAsString());
                else version.set(launcherMeta.getAsJsonObject("latest").get("release").getAsString());
            }
            else version.set(this.getName());
            launcherMeta.getAsJsonArray("versions").forEach(jsonElement ->
            {
                if (jsonElement.getAsJsonObject().get("id").getAsString().equals(version.get()))
                {
                    try
                    {
                        result.set(new URL(jsonElement.getAsJsonObject().get("url").getAsString()).openStream());
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        return result.get();
    }
    
    private JsonElement readData(InputStream input)
    {
        try(InputStream stream = new BufferedInputStream(input))
        {
            final Reader reader = new BufferedReader(new InputStreamReader(stream));
            final StringBuilder sb = new StringBuilder();

            int character;
            while ((character = reader.read()) != -1) sb.append((char)character);

            return JsonParser.parseString(sb.toString());
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return JsonNull.INSTANCE;
    }
	
	public static class VanillaVersionBuilder implements IBuilder<VanillaVersion>
	{
		private final BuilderArgument<String> nameArgument = new BuilderArgument<String>(null).required();
		private final BuilderArgument<MCP> mcpArgument = new BuilderArgument<MCP>(null).optional();
		private final BuilderArgument<Boolean> snapshotArgument = new BuilderArgument<Boolean>(false).optional();
		private final BuilderArgument<VersionType> versionTypeArgument = new BuilderArgument<VersionType>(null).required();
		
		public VanillaVersionBuilder withName(String name)
		{
			this.nameArgument.set(name);
			return this;
		}
		
		public VanillaVersionBuilder withMCP(MCP mcp)
		{
			this.mcpArgument.set(mcp);
			return this;
		}
		
		public VanillaVersionBuilder withSnapshot(boolean snapshot)
		{
			this.snapshotArgument.set(snapshot);
			return this;
		}
		
		public VanillaVersionBuilder withVersionType(VersionType versionType)
		{
			this.versionTypeArgument.set(versionType);
			return this;
		}

		@Override
		public VanillaVersion build() throws BuilderArgumentException
		{
			return new VanillaVersion(this.nameArgument.get(), this.mcpArgument.get(), this.snapshotArgument.get(), this.versionTypeArgument.get());
		}
	}
}