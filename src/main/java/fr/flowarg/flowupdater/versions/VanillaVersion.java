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
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class VanillaVersion
{
    /**
     * Default version. It used when an update doesn't need a Minecraft installation.
     */
    public static final VanillaVersion NULL_VERSION = new VanillaVersion("no", null, false,
                                                                         null, new ArrayList<>(),
                                                                         new ArrayList<>(), null);
    
    private final String name;
    private final MCP mcp;
    private final boolean snapshot;
    private final AssetIndex customAssetIndex;
    private final List<AssetDownloadable> anotherAssets;
    private final List<Downloadable> anotherLibraries;
    private final boolean custom;
    
    private JsonElement json = null;
    private String jsonURL = null;
    
    private VanillaVersion(String name, MCP mcp,
            boolean snapshot,
            AssetIndex customAssetIndex, List<AssetDownloadable> anotherAssets,
            List<Downloadable> anotherLibraries, JsonObject customVersionJson)
    {
        this.name = name;
        this.mcp = mcp;
        this.snapshot = snapshot;
        this.customAssetIndex = customAssetIndex;
        this.anotherAssets = anotherAssets;
        this.anotherLibraries = anotherLibraries;
        this.custom = customVersionJson != null;
        if(!this.name.equals("no"))
            this.json = (customVersionJson == null ? IOUtils.readJson(this.getJsonVersion()) : customVersionJson);
    }

    /**
     * Get the JSON array representing all Minecraft's libraries.
     * @return the libraries in JSON format.
     */
    public JsonArray getMinecraftLibrariesJson() 
    {
        return this.json.getAsJsonObject().getAsJsonArray("libraries");
    }

    /**
     * Get the JSON object representing Minecraft's client.
     * @return the client in JSON format.
     */
    public JsonObject getMinecraftClient() 
    {
        if(!this.custom && this.mcp != null)
        {
            final JsonObject result = new JsonObject();
            final String sha1 = this.mcp.getClientSha1();
            final String url = this.mcp.getClientURL();
            final long size = this.mcp.getClientSize();
            if(StringUtils.checkString(sha1) && StringUtils.checkString(url) && size > 0)
            {
                result.addProperty("sha1", sha1);
                result.addProperty("size", size);
                result.addProperty("url", url);
                return result;
            }
            else FlowUpdater.DEFAULT_LOGGER.warn("Skipped MCP Client");
        }
        return this.json.getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("client");
    }

    /**
     * Get the JSON object representing Minecraft's asset index.
     * @return the asset index in JSON format.
     */
    public JsonObject getMinecraftAssetIndex()
    {
        return this.json.getAsJsonObject().getAsJsonObject("assetIndex");
    }
    
    /**
     * Get the input stream of the wanted version json.
     */
    private InputStream getJsonVersion()
    {
        final AtomicReference<String> version = new AtomicReference<>(this.getName());
        final AtomicReference<InputStream> result = new AtomicReference<>(null);

        try
        {
            final JsonObject launcherMeta = IOUtils.readJson(
                    new URL("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json")
                            .openStream())
                    .getAsJsonObject();

            if (this.getName().equals("latest"))
            {
                final JsonObject latest = launcherMeta.getAsJsonObject("latest");
                if (this.snapshot)
                    version.set(latest.get("snapshot").getAsString());
                else version.set(latest.get("release").getAsString());
            }

            launcherMeta.getAsJsonArray("versions").forEach(jsonElement ->
            {
                if (!jsonElement.getAsJsonObject().get("id").getAsString().equals(version.get())) return;
                try
                {
                    this.jsonURL = jsonElement.getAsJsonObject().get("url").getAsString();
                    result.set(new URL(this.jsonURL).openStream());
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            });
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        return result.get();
    }

    /**
     * Get the name of the version.
     * @return the name of the version.
     */
    public @NotNull String getName()
    {
        return this.name;
    }

    /**
     * Get the MCP object of the version.
     * @return the MCP object of the version.
     */
    public MCP getMCP()
    {
        return this.mcp;
    }

    /**
     * Is the current version a snapshot?
     * @return if the current version is a snapshot.
     */
    public boolean isSnapshot()
    {
        return this.snapshot;
    }

    /**
     * The custom asset index.
     * @return the custom asset index.
     */
    public AssetIndex getCustomAssetIndex()
    {
        return this.customAssetIndex;
    }

    /**
     * The list of custom assets.
     * @return The list of custom assets.
     */
    public List<AssetDownloadable> getAnotherAssets()
    {
        return this.anotherAssets;
    }

    /**
     * The list of custom libraries.
     * @return The list of custom libraries.
     */
    public List<Downloadable> getAnotherLibraries()
    {
        return this.anotherLibraries;
    }

    /**
     * Get the url of the JSON version.
     * @return the url of the JSON version.
     */
    public String getJsonURL()
    {
        return this.jsonURL;
    }

    /**
     * A builder for building a vanilla version like {@link FlowUpdater.FlowUpdaterBuilder}
     * @author FlowArg
     */
    public static class VanillaVersionBuilder implements IBuilder<VanillaVersion>
    {
        private final BuilderArgument<String> nameArgument = new BuilderArgument<String>("Name").required();
        private final BuilderArgument<MCP> mcpArgument = new BuilderArgument<MCP>("MCP").optional();
        private final BuilderArgument<Boolean> snapshotArgument = new BuilderArgument<>("Snapshot", () -> false).optional();
        private final BuilderArgument<AssetIndex> customAssetIndexArgument = new BuilderArgument<AssetIndex>("CustomAssetIndex").optional();
        private final BuilderArgument<List<AssetDownloadable>> anotherAssetsArgument = new BuilderArgument<List<AssetDownloadable>>("AnotherAssets", ArrayList::new).optional();
        private final BuilderArgument<List<Downloadable>> anotherLibrariesArgument = new BuilderArgument<List<Downloadable>>("AnotherLibraries", ArrayList::new).optional();
        private final BuilderArgument<JsonObject> customVersionJsonArgument = new BuilderArgument<JsonObject>("CustomVersionJson").optional();

        /**
         * Define the name of the wanted Minecraft version.
         * @param name wanted Minecraft's version.
         * @return the builder.
         */
        public VanillaVersionBuilder withName(String name)
        {
            this.nameArgument.set(name);
            return this;
        }

        /**
         * Append a mcp object to the version
         * @param mcp the mcp object to append.
         * @return the builder.
         */
        public VanillaVersionBuilder withMCP(MCP mcp)
        {
            this.mcpArgument.set(mcp);
            return this;
        }

        /**
         * Append a mcp object to the version
         * @param mcpJsonUrl the mcp json url of mcp object to append.
         * @return the builder.
         */
        public VanillaVersionBuilder withMCP(URL mcpJsonUrl)
        {
            return withMCP(MCP.getMCPFromJson(mcpJsonUrl));
        }

        /**
         * Append a mcp object to the version
         * @param mcpJsonUrl the mcp json url of mcp object to append.
         * @return the builder.
         */
        public VanillaVersionBuilder withMCP(String mcpJsonUrl)
        {
            return withMCP(MCP.getMCPFromJson(mcpJsonUrl));
        }

        /**
         * Is the version a snapshot?
         * @param snapshot if the version is a snapshot.
         * @return the builder.
         */
        public VanillaVersionBuilder withSnapshot(boolean snapshot)
        {
            this.snapshotArgument.set(snapshot);
            return this;
        }

        /**
         * Add custom asset index to the version.
         * @param assetIndex the custom asset index to add.
         * @return the builder.
         */
        public VanillaVersionBuilder withCustomAssetIndex(AssetIndex assetIndex)
        {
            this.customAssetIndexArgument.set(assetIndex);
            return this;
        }

        /**
         * Add custom assets to the version.
         * @param anotherAssets custom assets to add.
         * @return the builder.
         */
        public VanillaVersionBuilder withAnotherAssets(Collection<AssetDownloadable> anotherAssets)
        {
            this.anotherAssetsArgument.get().addAll(anotherAssets);
            return this;
        }

        /**
         * Add custom assets to the version.
         * @param anotherAssets custom assets to add.
         * @return the builder.
         */
        public VanillaVersionBuilder withAnotherAssets(AssetDownloadable... anotherAssets)
        {
            return withAnotherAssets(Arrays.asList(anotherAssets));
        }

        /**
         * Add custom libraries to the version.
         * @param anotherLibraries custom libraries to add.
         * @return the builder.
         */
        public VanillaVersionBuilder withAnotherLibraries(Collection<Downloadable> anotherLibraries)
        {
            this.anotherLibrariesArgument.get().addAll(anotherLibraries);
            return this;
        }

        /**
         * Add custom libraries to the version.
         * @param anotherLibraries custom libraries to add.
         * @return the builder.
         */
        public VanillaVersionBuilder withAnotherLibraries(Downloadable... anotherLibraries)
        {
            return withAnotherLibraries(Arrays.asList(anotherLibraries));
        }

        /**
         * Define the version's json.
         * @param customVersionJson the custom version's json to set.
         * @return the builder.
         */
        public VanillaVersionBuilder withCustomVersionJson(JsonObject customVersionJson)
        {
            this.customVersionJsonArgument.set(customVersionJson);
            return this;
        }

        /**
         * Build a new {@link VanillaVersion} instance with provided arguments.
         * @return the freshly created instance.
         * @throws BuilderException if an error occurred.
         */
        @Override
        public VanillaVersion build() throws BuilderException
        {
            return new VanillaVersion(this.nameArgument.get(), this.mcpArgument.get(),
                                      this.snapshotArgument.get(),
                                      this.customAssetIndexArgument.get(), this.anotherAssetsArgument.get(),
                                      this.anotherLibrariesArgument.get(), this.customVersionJsonArgument.get());
        }
    }
}
