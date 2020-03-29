package fr.flowarg.flowupdater.minecraft.versions.download;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import fr.flowarg.flowupdater.minecraft.FlowArgMinecraftUpdater;
import fr.flowarg.flowcompat.Platform;
import fr.flowarg.flowupdater.minecraft.versions.IVersion;
import fr.flowarg.flowupdater.minecraft.versions.download.assets.AssetDownloadable;
import fr.flowarg.flowupdater.minecraft.versions.download.assets.AssetIndex;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class Reader
{
    private IVersion version;

    public Reader(IVersion version)
    {
        this.version = version;
    }

    public void read() throws IOException
    {
        FlowArgMinecraftUpdater.getLogger().info("Reading libraries information...");
        long start = System.currentTimeMillis();
        this.getLibraries();

        FlowArgMinecraftUpdater.getLogger().info("Reading assets information...");
        this.getAssetsIndex();

        FlowArgMinecraftUpdater.getLogger().info("Reading jars for client/server game...");
        this.getClientServerJars();

        FlowArgMinecraftUpdater.getLogger().info("Reading natives...");
        this.getNatives();

        FlowArgMinecraftUpdater.getLogger().info("Reading assets...");
        this.getAssets();

        long end = System.currentTimeMillis();
        FlowArgMinecraftUpdater.getLogger().warn("Parsing of the json file took " + (end - start) + " milliseconds...");
    }

    private void getLibraries()
    {
        this.version.getLibrariesJson().forEach(jsonElement ->
        {
            boolean canDownload;
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
                        final String path = obj.get("path").getAsString();
                        final String name = path.replace(path, "/libraries/" + path.substring(path.lastIndexOf('/') + 1));
                        final String sha1 = obj.get("sha1").getAsString();

                        FlowArgMinecraftUpdater.getLogger().info("Reading " + name + " from " + url + "... SHA1 is : " + sha1);
                        Downloader.getLibraryDownloadable().add(new Downloadable(url, size, sha1, name));
                    }
                }
            }
        });
    }

    private void getAssetsIndex()
    {
        final JsonObject assetIndex = this.version.getAssetsIndex();
        final String url = assetIndex.get("url").getAsString();
        final int size = assetIndex.get("size").getAsInt();
        final String name = url.replace(url, "/assets/indexes/" + url.substring(url.lastIndexOf('/') + 1));
        final String sha1 = assetIndex.get("sha1").getAsString();

        FlowArgMinecraftUpdater.getLogger().info("Reading assets index from " + url + "... SHA1 is : " + sha1);
        Downloader.getLibraryDownloadable().add(new Downloadable(url, size, sha1, name));
    }

    private void getClientServerJars()
    {
        final JsonObject client = this.version.getClient();
        final String clientURL = client.get("url").getAsString();
        final int clientSize = client.get("size").getAsInt();
        final String clientName = clientURL.replace(clientURL, clientURL.substring(clientURL.lastIndexOf('/') + 1));
        final String clientSha1 = client.get("sha1").getAsString();

        final JsonObject server = this.version.getServer();
        final String serverURL = server.get("url").getAsString();
        final int serverSize = server.get("size").getAsInt();
        final String serverName = serverURL.replace(serverURL, serverURL.substring(serverURL.lastIndexOf('/') + 1));
        final String serverSha1 = server.get("sha1").getAsString();

        FlowArgMinecraftUpdater.getLogger().info("Reading client jar from " + clientURL + "... SHA1 is : " + this.version.getClient().get("sha1").getAsString());
        FlowArgMinecraftUpdater.getLogger().info("Reading server jar from " + serverURL + "... SHA1 is : " + this.version.getServer().get("sha1").getAsString());

        Downloader.getLibraryDownloadable().add(new Downloadable(clientURL, clientSize, clientSha1, clientName));
        Downloader.getLibraryDownloadable().add(new Downloadable(serverURL, serverSize, serverSha1, serverName));
    }

    private void getNatives()
    {
        this.version.getLibrariesJson().forEach(jsonElement ->
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
                {
                    final String url = macObj.get("url").getAsString();
                    final int size = macObj.get("size").getAsInt();
                    final String path = macObj.get("path").getAsString();
                    final String name = path.replace(path, "/natives/" + path.substring(path.lastIndexOf('/') + 1));
                    final String sha1 = macObj.get("sha1").getAsString();

                    FlowArgMinecraftUpdater.getLogger().info("Reading " + name + " from " + url + "... SHA1 is : " + sha1);
                    Downloader.getLibraryDownloadable().add(new Downloadable(url, size, sha1, name));
                } else if (osxObj != null && Platform.isOnMac())
                {
                    final String url = osxObj.get("url").getAsString();
                    final int size = osxObj.get("size").getAsInt();
                    final String path = osxObj.get("path").getAsString();
                    final String name = path.replace(path, "/natives/" + path.substring(path.lastIndexOf('/') + 1));
                    final String sha1 = osxObj.get("sha1").getAsString();

                    FlowArgMinecraftUpdater.getLogger().info("Reading " + name + " from " + url + "... SHA1 is : " + sha1);
                    Downloader.getLibraryDownloadable().add(new Downloadable(url, size, sha1, name));
                } else if (windowsObj != null && Platform.isOnWindows())
                {
                    final String url = windowsObj.get("url").getAsString();
                    final int size = windowsObj.get("size").getAsInt();
                    final String path = windowsObj.get("path").getAsString();
                    final String name = path.replace(path, "/natives/" + path.substring(path.lastIndexOf('/') + 1));
                    final String sha1 = windowsObj.get("sha1").getAsString();

                    if (name.contains("-3.2.1-") && name.contains("lwjgl")) return;
                    if (name.contains("-2.9.2-") && name.contains("lwjgl")) return;

                    FlowArgMinecraftUpdater.getLogger().info("Reading " + name + " from " + url + "... SHA1 is : " + sha1);
                    Downloader.getLibraryDownloadable().add(new Downloadable(url, size, sha1, name));
                } else if (linuxObj != null && Platform.isOnLinux())
                {
                    final String url = linuxObj.get("url").getAsString();
                    final int size = linuxObj.get("size").getAsInt();
                    final String path = linuxObj.get("path").getAsString();
                    final String name = path.replace(path, "/natives/" + path.substring(path.lastIndexOf('/') + 1));
                    final String sha1 = linuxObj.get("sha1").getAsString();

                    if (name.contains("-3.2.1-") && name.contains("lwjgl")) return;
                    if (name.contains("-2.9.2-") && name.contains("lwjgl")) return;

                    FlowArgMinecraftUpdater.getLogger().info("Reading " + name + " from " + url + "... SHA1 is : " + sha1);
                    Downloader.getLibraryDownloadable().add(new Downloadable(url, size, sha1, name));
                }
            }
        });
    }

    private void getAssets() throws IOException
    {
        final Set<AssetDownloadable> toDownload = new HashSet<>();
        final URL url = new URL(this.version.getAssetsIndex().get("url").getAsString());
        final String json = IOUtils.toString(url.openStream(), StandardCharsets.UTF_8);

        final AssetIndex index = new GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .setPrettyPrinting()
                .create()
                .fromJson(json, AssetIndex.class);

        for (final Map.Entry<AssetDownloadable, String> entry : index.getUniqueObjects().entrySet())
            toDownload.add(new AssetDownloadable(entry.getKey().getHash(), entry.getKey().getSize()));
        Downloader.getAssetDownloadables().addAll(toDownload);
    }

    private boolean checkRules(@NotNull JsonObject obj)
    {
        AtomicBoolean canDownload = new AtomicBoolean(true);

        if (obj.get("rules") != null)
        {
            obj.get("rules").getAsJsonArray().forEach(jsonElement ->
            {
                if (jsonElement.getAsJsonObject().get("action").getAsString().equals("allow"))
                {
                    if (jsonElement.getAsJsonObject().getAsJsonObject("os") != null)
                    {
                        final String os = jsonElement.getAsJsonObject().getAsJsonObject("os").get("name").getAsString();
                        if (os.equalsIgnoreCase("osx") && Platform.isOnMac())
                            canDownload.set(true);
                        else if (os.equalsIgnoreCase("macos") && Platform.isOnMac())
                            canDownload.set(true);
                        else if(os.equalsIgnoreCase("linux") && Platform.isOnLinux())
                            canDownload.set(true);
                        else if (os.equalsIgnoreCase("windows") && Platform.isOnWindows())
                            canDownload.set(true);
                        else canDownload.set(false);
                    }
                }
                else if (jsonElement.getAsJsonObject().get("action").getAsString().equals("disallow"))
                {
                    final String os = jsonElement.getAsJsonObject().getAsJsonObject("os").get("name").getAsString();
                    if (os.equalsIgnoreCase("osx") && Platform.isOnMac())
                        canDownload.set(false);
                    else if (os.equalsIgnoreCase("macos") && Platform.isOnMac())
                        canDownload.set(false);
                    else if (os.equalsIgnoreCase("windows") && Platform.isOnWindows())
                        canDownload.set(false);
                    else if (os.equalsIgnoreCase("linux") && Platform.isOnLinux())
                        canDownload.set(false);
                }
            });
        }

        return canDownload.get();
    }
}
