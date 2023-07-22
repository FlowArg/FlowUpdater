package fr.flowarg.flowupdater.download;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.flowarg.flowcompat.Platform;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.json.AssetDownloadable;
import fr.flowarg.flowupdater.download.json.AssetIndex;
import fr.flowarg.flowupdater.download.json.Downloadable;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.versions.VanillaVersion;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class handles all parsing stuff about vanilla files.
 */
public class VanillaReader
{
    private final VanillaVersion version;
    private final ILogger logger;
    private final boolean shouldLog;
    private final IProgressCallback callback;
    private final DownloadList downloadList;

    /**
     * Construct a new VanillaReader.
     * @param flowUpdater the flow updater object.
     */
    public VanillaReader(@NotNull FlowUpdater flowUpdater)
    {
        this.version = flowUpdater.getVanillaVersion();
        this.logger = flowUpdater.getLogger();
        this.shouldLog = !flowUpdater.getUpdaterOptions().isSilentRead();
        this.callback = flowUpdater.getCallback();
        this.downloadList = flowUpdater.getDownloadList();
    }

    private void silentDebug(String message)
    {
        if (this.shouldLog)
            this.logger.debug(message);
    }

    /**
     * This method calls other methods to parse each part of the given Minecraft Version.
     * @throws IOException if an I/O error occurred.
     */
    public void read() throws IOException
    {
        this.callback.step(Step.READ);
        this.silentDebug("Parsing libraries information...");
        long start = System.currentTimeMillis();
        this.parseLibraries();

        this.silentDebug("Parsing asset index information...");
        this.parseAssetIndex();

        this.silentDebug("Parsing the information of client's jar...");
        this.parseClient();

        this.silentDebug("Parsing natives information...");
        this.parseNatives();

        this.silentDebug("Parsing assets information...");
        this.parseAssets();

        this.silentDebug("Parsing of the json file took " + (System.currentTimeMillis() - start) + " milliseconds...");
    }

    private void parseLibraries()
    {
        this.version.getMinecraftLibrariesJson().forEach(jsonElement -> {
            final JsonObject element = jsonElement.getAsJsonObject();

            if (element == null) return;
            if (!this.checkRules(element)) return;
            final JsonObject downloads = element.getAsJsonObject("downloads");

            if(downloads == null) return;

            block: {
                final String name = element.getAsJsonPrimitive("name").getAsString();

                if(!name.contains("lwjgl") || !name.contains("natives") || !name.contains("macos"))
                    break block;

                boolean platformCheck = (Platform.isOnMac() &&
                        Platform.getArch().equals("64") &&
                        System.getProperty("os.arch").equals("aarch64"));

                if(platformCheck != name.contains("arm64"))
                    return;
            }

            final JsonObject artifact = downloads.getAsJsonObject("artifact");

            if (artifact == null) return;

            final String url = artifact.getAsJsonPrimitive("url").getAsString();
            final int size = artifact.getAsJsonPrimitive("size").getAsInt();
            final String path = "libraries/" + artifact.getAsJsonPrimitive("path").getAsString();
            final String sha1 = artifact.getAsJsonPrimitive("sha1").getAsString();

            this.silentDebug("Reading " + path + " from " + url + "... SHA1 is : " + sha1);

            final Downloadable downloadable = new Downloadable(url, size, sha1, path);

            if(!this.downloadList.getDownloadableFiles().contains(downloadable))
                this.downloadList.getDownloadableFiles().add(downloadable);
        });
        this.downloadList.getDownloadableFiles().addAll(this.version.getAnotherLibraries());
    }

    private void parseAssetIndex()
    {
        if(this.version.getCustomAssetIndex() != null) return;

        final JsonObject assetIndex = this.version.getMinecraftAssetIndex();
        final String url = assetIndex.getAsJsonPrimitive("url").getAsString();
        final int size = assetIndex.getAsJsonPrimitive("size").getAsInt();
        final String name = "assets/indexes/" + url.substring(url.lastIndexOf('/') + 1);
        final String sha1 = assetIndex.getAsJsonPrimitive("sha1").getAsString();

        this.silentDebug("Reading assets index from " + url + "... SHA1 is : " + sha1);
        this.downloadList.getDownloadableFiles().add(new Downloadable(url, size, sha1, name));
    }

    private void parseClient()
    {
        final JsonObject client = this.version.getMinecraftClient();
        final String clientURL = client.getAsJsonPrimitive("url").getAsString();
        final int clientSize = client.getAsJsonPrimitive("size").getAsInt();
        final String clientName = clientURL.substring(clientURL.lastIndexOf('/') + 1);
        final String clientSha1 = client.getAsJsonPrimitive("sha1").getAsString();

        this.silentDebug("Reading client jar from " + clientURL + "... SHA1 is : " + clientSha1);
        this.downloadList.getDownloadableFiles().add(new Downloadable(clientURL, clientSize, clientSha1, clientName));
    }

    private void parseNatives()
    {
        this.version.getMinecraftLibrariesJson().forEach(jsonElement -> {
            final JsonObject obj = jsonElement.getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("classifiers");

            if (obj == null) return;

            final JsonObject macObj = obj.getAsJsonObject("natives-macos");
            final JsonObject osxObj = obj.getAsJsonObject("natives-osx");
            JsonObject windowsObj = obj.getAsJsonObject(String.format("natives-windows-%s", Platform.getArch()));
            if (windowsObj == null) windowsObj = obj.getAsJsonObject("natives-windows");
            final JsonObject linuxObj = obj.getAsJsonObject("natives-linux");

            if (macObj != null && Platform.isOnMac())
                this.getNativeForOS("mac", macObj);
            else if (osxObj != null && Platform.isOnMac())
                this.getNativeForOS("mac", osxObj);
            else if (windowsObj != null && Platform.isOnWindows())
                this.getNativeForOS("win", windowsObj);
            else if (linuxObj != null && Platform.isOnLinux())
                this.getNativeForOS("linux", linuxObj);
        });
    }
    
    private void getNativeForOS(@NotNull String os, @NotNull JsonObject obj)
    {
        final String url = obj.getAsJsonPrimitive("url").getAsString();
        final int size = obj.getAsJsonPrimitive("size").getAsInt();
        final String path = obj.getAsJsonPrimitive("path").getAsString();
        final String name = "natives/" + path.substring(path.lastIndexOf('/') + 1);
        final String sha1 = obj.getAsJsonPrimitive("sha1").getAsString();

        if(!os.equals("mac"))
        {
            if (name.contains("-3.2.1-") && name.contains("lwjgl")) return;
            if (name.contains("-2.9.2-") && name.contains("lwjgl")) return;
        }
        else if(name.contains("-3.2.2-") && name.contains("lwjgl")) return;

        this.silentDebug("Reading " + name + " from " + url + "... SHA1 is : " + sha1);
        this.downloadList.getDownloadableFiles().add(new Downloadable(url, size, sha1, name));
    }

    private void parseAssets() throws IOException
    {
        final Set<AssetDownloadable> toDownload = new HashSet<>(this.version.getAnotherAssets());
        final AssetIndex assetIndex;

        if(this.version.getCustomAssetIndex() == null)
            assetIndex = new GsonBuilder()
                    .disableHtmlEscaping()
                    .create()
                    .fromJson(IOUtils.getContent(new URL(this.version.getMinecraftAssetIndex().get("url").getAsString())), AssetIndex.class);
        else assetIndex = this.version.getCustomAssetIndex();

        assetIndex.getUniqueObjects()
                .values()
                .forEach(assetDownloadable ->
                                 toDownload.add(new AssetDownloadable(assetDownloadable.getHash(), assetDownloadable.getSize())));
        this.downloadList.getDownloadableAssets().addAll(toDownload);
    }

    private boolean checkRules(@NotNull JsonObject obj)
    {
        final JsonElement rulesElement = obj.get("rules");
        if (rulesElement == null) return true;

        final AtomicBoolean canDownload = new AtomicBoolean(true);

        rulesElement.getAsJsonArray().forEach(jsonElement -> {
            final JsonObject object = jsonElement.getAsJsonObject();
            final String actionValue = object.getAsJsonPrimitive("action").getAsString();
            final JsonObject osObject = object.getAsJsonObject("os");

            if (actionValue.equals("allow"))
            {
                if (osObject == null) return;

                final String os = osObject.getAsJsonPrimitive("name").getAsString();
                canDownload.set(this.check(os));
            }
            else if (actionValue.equals("disallow"))
            {
                final String os = osObject.getAsJsonPrimitive("name").getAsString();
                canDownload.set(!this.check(os));
            }
        });

        return canDownload.get();
    }
    
    private boolean check(@NotNull String os)
    {
        return (os.equalsIgnoreCase("osx") && Platform.isOnMac()) ||
                (os.equalsIgnoreCase("macos") && Platform.isOnMac()) ||
                (os.equalsIgnoreCase("windows") && Platform.isOnWindows()) ||
                (os.equalsIgnoreCase("linux") && Platform.isOnLinux());
    }
}
