package fr.flowarg.flowupdater.download;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import fr.flowarg.flowcompat.Platform;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.json.AssetDownloadable;
import fr.flowarg.flowupdater.download.json.AssetIndex;
import fr.flowarg.flowupdater.download.json.Downloadable;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.versions.VanillaVersion;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This method handles all parsing stuff about vanilla files.
 */
public class VanillaReader
{
    private final VanillaVersion version;
    private final ILogger logger;
    private final boolean shouldLog;
    private final IProgressCallback callback;
    private final DownloadList info;
    private final boolean downloadServer;

    /**
     * Construct a new VanillaReader.
     * @param flowUpdater the flow updater object.
     */
    public VanillaReader(FlowUpdater flowUpdater)
    {
        this.version = flowUpdater.getVersion();
        this.logger = flowUpdater.getLogger();
        this.shouldLog = !flowUpdater.getUpdaterOptions().isSilentRead();
        this.callback = flowUpdater.getCallback();
        this.info = flowUpdater.getDownloadList();
        this.downloadServer = flowUpdater.getUpdaterOptions().isDownloadServer();
    }

    /**
     * This method calls others methods to parse each part of the given Minecraft Version.
     * @throws IOException if an I/O error occurred.
     */
    public void read() throws IOException
    {
        this.callback.step(Step.READ);
        if(this.shouldLog)
            this.logger.debug("Reading libraries information...");
        long start = System.currentTimeMillis();
        this.parseLibraries();

        if(this.shouldLog)
            this.logger.debug("Reading assets information...");
        this.parseAssetIndex();

        if(this.shouldLog)
            this.logger.debug("Reading jars for client/server game...");
        this.parseClientServerJars();

        if(this.shouldLog)
            this.logger.debug("Reading natives...");
        this.parseNatives();

        if(this.shouldLog)
            this.logger.debug("Reading assets...");
        this.parseAssets();

        if(!this.shouldLog) return;

        final long end = System.currentTimeMillis();
        this.logger.debug("Parsing of the json file took " + (end - start) + " milliseconds...");
    }

    private void parseLibraries()
    {
        this.version.getMinecraftLibrariesJson().forEach(jsonElement -> {
            final boolean canDownload;
            final JsonObject element = jsonElement.getAsJsonObject();

            if (element == null) return;

            canDownload = this.checkRules(element);

            if (!canDownload) return;
            if (element.getAsJsonObject("downloads").getAsJsonObject("artifact") == null) return;

            final JsonObject obj = element.getAsJsonObject("downloads").getAsJsonObject("artifact");
            final String url = obj.getAsJsonPrimitive("url").getAsString();
            final int size = obj.getAsJsonPrimitive("size").getAsInt();
            final String path = "libraries/" + obj.getAsJsonPrimitive("path").getAsString();
            final String sha1 = obj.getAsJsonPrimitive("sha1").getAsString();

            if(this.shouldLog)
                this.logger.debug("Reading " + path + " from " + url + "... SHA1 is : " + sha1);
            this.info.getDownloadableFiles().add(new Downloadable(url, size, sha1, path));
        });
        this.info.getDownloadableFiles().addAll(this.version.getAnotherLibraries());
    }

    private void parseAssetIndex()
    {
        if(this.version.getCustomAssetIndex() != null) return;

        final JsonObject assetIndex = this.version.getMinecraftAssetsIndex();
        final String url = assetIndex.getAsJsonPrimitive("url").getAsString();
        final int size = assetIndex.getAsJsonPrimitive("size").getAsInt();
        final String name = "assets/indexes/" + url.substring(url.lastIndexOf('/') + 1);
        final String sha1 = assetIndex.getAsJsonPrimitive("sha1").getAsString();

        if(this.shouldLog)
            this.logger.debug("Reading assets index from " + url + "... SHA1 is : " + sha1);
        this.info.getDownloadableFiles().add(new Downloadable(url, size, sha1, name));
    }

    private void parseClientServerJars()
    {
        final JsonObject client = this.version.getMinecraftClient();
        final String clientURL = client.getAsJsonPrimitive("url").getAsString();
        final int clientSize = client.getAsJsonPrimitive("size").getAsInt();
        final String clientName = clientURL.substring(clientURL.lastIndexOf('/') + 1);
        final String clientSha1 = client.getAsJsonPrimitive("sha1").getAsString();

        if(this.shouldLog)
            this.logger.debug("Reading client jar from " + clientURL + "... SHA1 is : " + clientSha1);
        this.info.getDownloadableFiles().add(new Downloadable(clientURL, clientSize, clientSha1, clientName));

        if(!this.downloadServer) return;

        final JsonObject server = this.version.getMinecraftServer();
        final String serverURL = server.getAsJsonPrimitive("url").getAsString();
        final int serverSize = server.getAsJsonPrimitive("size").getAsInt();
        final String serverName = serverURL.substring(serverURL.lastIndexOf('/') + 1);
        final String serverSha1 = server.getAsJsonPrimitive("sha1").getAsString();

        if(this.shouldLog)
            this.logger.debug("Reading server jar from " + serverURL + "... SHA1 is : " + serverSha1);

        this.info.getDownloadableFiles().add(new Downloadable(serverURL, serverSize, serverSha1, serverName));
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
    
    private void getNativeForOS(String os, JsonObject obj)
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

        if(this.shouldLog)
            this.logger.debug("Reading " + name + " from " + url + "... SHA1 is : " + sha1);
        this.info.getDownloadableFiles().add(new Downloadable(url, size, sha1, name));
    }

    private void parseAssets() throws IOException
    {
        final Set<AssetDownloadable> toDownload = new HashSet<>(this.version.getAnotherAssets());
        final AssetIndex assetIndex;

        if(this.version.getCustomAssetIndex() == null) assetIndex = new GsonBuilder().disableHtmlEscaping().create().fromJson(IOUtils.getContent(new URL(this.version.getMinecraftAssetsIndex().get("url").getAsString())), AssetIndex.class);
        else assetIndex = this.version.getCustomAssetIndex();

        for (final Map.Entry<String, AssetDownloadable> entry : assetIndex.getUniqueObjects().entrySet())
            toDownload.add(new AssetDownloadable(entry.getValue().getHash(), entry.getValue().getSize()));

        this.info.getDownloadableAssets().addAll(toDownload);
    }

    private boolean checkRules(JsonObject obj)
    {
        if (obj.get("rules") == null) return true;

        final AtomicBoolean canDownload = new AtomicBoolean(true);

        obj.get("rules").getAsJsonArray().forEach(jsonElement -> {
            if (jsonElement.getAsJsonObject().getAsJsonPrimitive("action").getAsString().equals("allow"))
            {
                if (jsonElement.getAsJsonObject().getAsJsonObject("os") == null) return;

                final String os = jsonElement.getAsJsonObject().getAsJsonObject("os").getAsJsonPrimitive("name").getAsString();
                canDownload.set(this.check(os));
            }
            else if (jsonElement.getAsJsonObject().getAsJsonPrimitive("action").getAsString().equals("disallow"))
            {
                final String os = jsonElement.getAsJsonObject().getAsJsonObject("os").getAsJsonPrimitive("name").getAsString();
                if(this.check(os))
                    canDownload.set(false);
            }
        });

        return canDownload.get();
    }
    
    private boolean check(String str, String str2)
    {
        return str.equalsIgnoreCase(str2);
    }
    
    private boolean check(String os)
    {
        return (this.check(os, "osx") && Platform.isOnMac()) || (this.check(os, "macos") && Platform.isOnMac()) || (this.check(os, "windows") && Platform.isOnWindows()) || (this.check(os, "linux") && Platform.isOnLinux());
    }
}
