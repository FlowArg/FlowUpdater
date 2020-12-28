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

public class VanillaReader
{
    private final VanillaVersion version;
    private final ILogger logger;
    private final boolean isSilent;
    private final IProgressCallback callback;
    private final DownloadInfos infos;
    private final boolean downloadServer;

    public VanillaReader(FlowUpdater flowUpdater)
    {
        this.version = flowUpdater.getVersion();
        this.logger = flowUpdater.getLogger();
        this.isSilent = flowUpdater.getUpdaterOptions().isSilentRead();
        this.callback = flowUpdater.getCallback();
        this.infos = flowUpdater.getDownloadInfos();
        this.downloadServer = flowUpdater.getUpdaterOptions().isDownloadServer();
    }

    public void read() throws IOException
    {
        this.callback.step(Step.READ);
        if(!this.isSilent)
            this.logger.debug("Reading libraries information...");
        long start = System.currentTimeMillis();
        this.getLibraries();

        if(!this.isSilent)
            this.logger.debug("Reading assets information...");
        this.getAssetsIndex();

        if(!this.isSilent)
            this.logger.debug("Reading jars for client/server game...");
        this.getClientServerJars();

        if(!this.isSilent)
            this.logger.debug("Reading natives...");
        this.getNatives();

        if(!this.isSilent)
            this.logger.debug("Reading assets...");
        this.getAssets();

        if(!this.isSilent)
        {
            final long end = System.currentTimeMillis();
            this.logger.debug("Parsing of the json file took " + (end - start) + " milliseconds...");
        }
    }

    private void getLibraries()
    {
        this.version.getMinecraftLibrariesJson().forEach(jsonElement ->
        {
            final boolean canDownload;
            final JsonObject element = jsonElement.getAsJsonObject();
            if (element != null)
            {
                canDownload = this.checkRules(element);

                if (canDownload)
                {
                    if (element.getAsJsonObject("downloads").getAsJsonObject("artifact") != null)
                    {
                        final JsonObject obj = element.getAsJsonObject("downloads").getAsJsonObject("artifact");
                        final String url = obj.get("url").getAsString();
                        final int size = obj.get("size").getAsInt();
                        final String path = "/libraries/" + obj.get("path").getAsString();
                        final String sha1 = obj.get("sha1").getAsString();

                        if(!this.isSilent)
                            this.logger.debug("Reading " + path + " from " + url + "... SHA1 is : " + sha1);
                        this.infos.getLibraryDownloadables().add(new Downloadable(url, size, sha1, path));
                    }
                }
            }
        });
    }

    private void getAssetsIndex()
    {
        final JsonObject assetIndex = this.version.getMinecraftAssetsIndex();
        final String url = assetIndex.get("url").getAsString();
        final int size = assetIndex.get("size").getAsInt();
        final String name = "/assets/indexes/" + url.substring(url.lastIndexOf('/') + 1);
        final String sha1 = assetIndex.get("sha1").getAsString();

        if(!this.isSilent)
            this.logger.debug("Reading assets index from " + url + "... SHA1 is : " + sha1);
        this.infos.getLibraryDownloadables().add(new Downloadable(url, size, sha1, name));
    }

    private void getClientServerJars()
    {
        final JsonObject client = this.version.getMinecraftClient();
        final String clientURL = client.get("url").getAsString();
        final int clientSize = client.get("size").getAsInt();
        final String clientName = clientURL.substring(clientURL.lastIndexOf('/') + 1);
        final String clientSha1 = client.get("sha1").getAsString();

        final JsonObject server = this.version.getMinecraftServer();
        final String serverURL = server.get("url").getAsString();
        final int serverSize = server.get("size").getAsInt();
        final String serverName = serverURL.substring(serverURL.lastIndexOf('/') + 1);
        final String serverSha1 = server.get("sha1").getAsString();

        if(!this.isSilent)
        {
            this.logger.debug("Reading client jar from " + clientURL + "... SHA1 is : " + this.version.getMinecraftClient().get("sha1").getAsString());
            this.logger.debug("Reading server jar from " + serverURL + "... SHA1 is : " + this.version.getMinecraftServer().get("sha1").getAsString());
        }
        
        this.infos.getLibraryDownloadables().add(new Downloadable(clientURL, clientSize, clientSha1, clientName));
        if(this.downloadServer)
            this.infos.getLibraryDownloadables().add(new Downloadable(serverURL, serverSize, serverSha1, serverName));
    }

    private void getNatives()
    {
        this.version.getMinecraftLibrariesJson().forEach(jsonElement ->
        {
            final JsonObject obj = jsonElement.getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("classifiers");
            if (obj != null)
            {
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
            }
        });
    }
    
    private void getNativeForOS(String os, JsonObject obj)
    {
        final String url = obj.get("url").getAsString();
        final int size = obj.get("size").getAsInt();
        final String path = obj.get("path").getAsString();
        final String name = "/natives/" + path.substring(path.lastIndexOf('/') + 1);
        final String sha1 = obj.get("sha1").getAsString();

        if(!os.equals("mac"))
        {
            if (name.contains("-3.2.1-") && name.contains("lwjgl")) return;
            if (name.contains("-2.9.2-") && name.contains("lwjgl")) return;
        }

        if(!this.isSilent)
            this.logger.debug("Reading " + name + " from " + url + "... SHA1 is : " + sha1);
        this.infos.getLibraryDownloadables().add(new Downloadable(url, size, sha1, name));
    }

    private void getAssets() throws IOException
    {
        final Set<AssetDownloadable> toDownload = new HashSet<>();
        final URL url = new URL(this.version.getMinecraftAssetsIndex().get("url").getAsString());
        final String json = IOUtils.getContent(url);

        final AssetIndex index = new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .setPrettyPrinting()
                .create()
                .fromJson(json, AssetIndex.class);

        for (final Map.Entry<String, AssetDownloadable> entry : index.getUniqueObjects().entrySet())
            toDownload.add(new AssetDownloadable(entry.getValue().getHash(), entry.getValue().getSize()));
        this.infos.getAssetDownloadables().addAll(toDownload);
    }

    private boolean checkRules(JsonObject obj)
    {
        final AtomicBoolean canDownload = new AtomicBoolean(true);

        if (obj.get("rules") != null)
        {
            obj.get("rules").getAsJsonArray().forEach(jsonElement ->
            {
                if (jsonElement.getAsJsonObject().get("action").getAsString().equals("allow"))
                {
                    if (jsonElement.getAsJsonObject().getAsJsonObject("os") != null)
                    {
                        final String os = jsonElement.getAsJsonObject().getAsJsonObject("os").get("name").getAsString();
                        canDownload.set(this.check(os));
                    }
                }
                else if (jsonElement.getAsJsonObject().get("action").getAsString().equals("disallow"))
                {
                    final String os = jsonElement.getAsJsonObject().getAsJsonObject("os").get("name").getAsString();
                    if(this.check(os))
                        canDownload.set(false);
                }
            });
        }

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
