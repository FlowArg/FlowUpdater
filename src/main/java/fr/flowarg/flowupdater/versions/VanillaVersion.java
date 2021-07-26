package fr.flowarg.flowupdater.versions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.flowarg.flowstringer.StringUtils;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.json.AssetDownloadable;
import fr.flowarg.flowupdater.download.json.AssetIndex;
import fr.flowarg.flowupdater.download.json.Downloadable;
import fr.flowarg.flowupdater.download.json.MCP;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.builderapi.BuilderArgument;
import fr.flowarg.flowupdater.utils.builderapi.BuilderException;
import fr.flowarg.flowupdater.utils.builderapi.IBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class VanillaVersion
{
    /**
     * Default version used for no minecraft updates.
     */
    public static final VanillaVersion NULL_VERSION = new VanillaVersion("no", null, false, null, null, new ArrayList<>(), new ArrayList<>(), null);
    
    private final String name;
    private final MCP mcp;
    private final boolean snapshot;
    private final VersionType versionType;
    private final AssetIndex customAssetIndex;
    private final List<AssetDownloadable> anotherAssets;
    private final List<Downloadable> anotherLibraries;
    
    private JsonElement json = null;
    
    private VanillaVersion(String name, MCP mcp,
            boolean snapshot, VersionType versionType,
            AssetIndex customAssetIndex, List<AssetDownloadable> anotherAssets,
            List<Downloadable> anotherLibraries, JsonObject customVersionJson)
    {
        this.name = name;
        this.mcp = mcp;
        this.snapshot = snapshot;
        this.versionType = versionType;
        this.customAssetIndex = customAssetIndex;
        this.anotherAssets = anotherAssets;
        this.anotherLibraries = anotherLibraries;
        if(!this.name.equals("no"))
            this.json = (customVersionJson == null ? IOUtils.readJson(this.getJsonVersion()) : customVersionJson);
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
            final String url = this.mcp.getClientURL();
            final long size = this.mcp.getClientSize();
            if(StringUtils.checkString(sha1) && StringUtils.checkString(url) && size > 0)
            {
                result.addProperty("sha1", this.mcp.getClientSha1());
                result.addProperty("size", this.mcp.getClientSize());
                result.addProperty("url", this.mcp.getClientURL());
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
            final String url = this.mcp.getServerURL();
            final long size = this.mcp.getServerSize();
            if(StringUtils.checkString(url) && StringUtils.checkString(sha1) && size > 0)
            {
                result.addProperty("sha1", this.mcp.getServerSha1());
                result.addProperty("size", this.mcp.getServerSize());
                result.addProperty("url", this.mcp.getServerURL());
                return result;
            }
        }
        return this.json.getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("server");
    }

    public JsonObject getMinecraftAssetsIndex() 
    {
        return this.json.getAsJsonObject().getAsJsonObject("assetIndex");
    }
    
    /**
     * Get the input stream of the wanted version json.
     */
    private InputStream getJsonVersion()
    {
        final AtomicReference<String> version = new AtomicReference<>(null);
        final AtomicReference<InputStream> result = new AtomicReference<>(null);

        try
        {
            final JsonObject launcherMeta = IOUtils.readJson(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json").openStream()).getAsJsonObject();

            if (this.getName().equals("latest"))
            {
                if (this.snapshot)
                    version.set(launcherMeta.getAsJsonObject("latest").get("snapshot").getAsString());
                else version.set(launcherMeta.getAsJsonObject("latest").get("release").getAsString());
            }
            else version.set(this.getName());
            launcherMeta.getAsJsonArray("versions").forEach(jsonElement ->
            {
                if (!jsonElement.getAsJsonObject().get("id").getAsString().equals(version.get())) return;
                try
                {
                    result.set(new URL(jsonElement.getAsJsonObject().get("url").getAsString()).openStream());
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            });
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        return result.get();
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

    public AssetIndex getCustomAssetIndex()
    {
        return this.customAssetIndex;
    }

    public List<AssetDownloadable> getAnotherAssets()
    {
        return this.anotherAssets;
    }

    public List<Downloadable> getAnotherLibraries()
    {
        return this.anotherLibraries;
    }

    /**
     * A builder for building a vanilla version like {@link fr.flowarg.flowupdater.FlowUpdater.FlowUpdaterBuilder}
     * @author FlowArg
     */
    public static class VanillaVersionBuilder implements IBuilder<VanillaVersion>
    {
        private final BuilderArgument<String> nameArgument = new BuilderArgument<String>("Name").required();
        private final BuilderArgument<MCP> mcpArgument = new BuilderArgument<MCP>("MCP").optional();
        private final BuilderArgument<Boolean> snapshotArgument = new BuilderArgument<>("Snapshot", () -> false).optional();
        private final BuilderArgument<VersionType> versionTypeArgument = new BuilderArgument<VersionType>("VersionType").required();
        private final BuilderArgument<AssetIndex> customAssetIndex = new BuilderArgument<AssetIndex>("CustomAssetIndex").optional();
        private final BuilderArgument<List<AssetDownloadable>> anotherAssets = new BuilderArgument<List<AssetDownloadable>>("AnotherAssets", ArrayList::new).optional();
        private final BuilderArgument<List<Downloadable>> anotherLibraries = new BuilderArgument<List<Downloadable>>("AnotherLibraries", ArrayList::new).optional();
        private final BuilderArgument<JsonObject> customVersionJson = new BuilderArgument<JsonObject>("CustomVersionJson").optional();

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

        public VanillaVersionBuilder withCustomAssetIndex(AssetIndex versionType)
        {
            this.customAssetIndex.set(versionType);
            return this;
        }

        public VanillaVersionBuilder withAnotherAssets(List<AssetDownloadable> anotherAssets)
        {
            this.anotherAssets.set(anotherAssets);
            return this;
        }

        public VanillaVersionBuilder withAnotherLibraries(List<Downloadable> anotherLibraries)
        {
            this.anotherLibraries.set(anotherLibraries);
            return this;
        }

        public VanillaVersionBuilder withCustomVersionJson(JsonObject customVersionJson)
        {
            this.customVersionJson.set(customVersionJson);
            return this;
        }

        @Override
        public VanillaVersion build() throws BuilderException
        {
            return new VanillaVersion(this.nameArgument.get(), this.mcpArgument.get(),
                                      this.snapshotArgument.get(), this.versionTypeArgument.get(),
                                      this.customAssetIndex.get(), this.anotherAssets.get(),
                                      this.anotherLibraries.get(), this.customVersionJson.get());
        }
    }
}
