package fr.antoineok.flowupdater.optifineplugin;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public class OptifinePlugin
{
    public static final OptifinePlugin INSTANCE = new OptifinePlugin();

    private final OkHttpClient client = new OkHttpClient();

    private ILogger logger;
    private Path folder;

    /**
     * Get an OptiFine object from the official website.
     * @param optifineVersion the version of Optifine
     * @param preview if the optifine version is a preview.
     * @return the object that defines the plugin
     * @throws IOException if the version is invalid or not found
     */
    public Optifine getOptifine(String optifineVersion, boolean preview) throws IOException
    {
        final String name = preview ? (optifineVersion.contains("preview_") && optifineVersion.contains("OptiFine_") ? optifineVersion + ".jar" : "preview_OptiFine_" + optifineVersion + ".jar") : "OptiFine_" + optifineVersion + ".jar";
        final String newUrl = this.getNewURL(name, preview, optifineVersion);
        final Request request = new Request.Builder()
                .url(newUrl)
                .build();

        final Response response = this.client.newCall(request).execute();
        final int length = Integer.parseInt(Objects.requireNonNull(response.header("Content-Length")));

        assert response.body() != null;
        this.checkForUpdates(name, response.body().byteStream(), length, newUrl);

        response.body().close();

        if(length <= 40)
            throw new IOException("Given version of Optifine not found.");

        return new Optifine(name, length);
    }

    private String getNewURL(String name, boolean preview, String optifineVersion)
    {
        final HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse("https://optifine.net/downloadx")).newBuilder();
        urlBuilder.addQueryParameter("f", name);
        urlBuilder.addQueryParameter("x", preview ? this.getJsonPreview(optifineVersion) : this.getJson(optifineVersion));

        return urlBuilder.build().toString();
    }

    private void checkForUpdates(String name, InputStream byteStream, int length, String newUrl) throws IOException
    {
        final Path outputPath = Paths.get(this.getFolder().toString(), name);
        if(Files.notExists(outputPath) || FileUtils.getFileSizeBytes(outputPath) != length)
        {
            this.getLogger().info(String.format("Downloading %s from %s...", outputPath.getFileName().toString(), newUrl));
            Files.copy(byteStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
        }
        byteStream.close();
    }

    public void shutdownOKHTTP()
    {
        this.client.dispatcher().executorService().shutdown();
        this.client.connectionPool().evictAll();
        if(this.client.cache() != null)
        {
            try
            {
                Objects.requireNonNull(this.client.cache()).close();
            } catch (IOException ignored) {}
        }
    }

    /**
     * @param optifineVersion the version of Optifine
     * @return the download key
     */
    private String getJson(String optifineVersion) {
        final Request request = new Request.Builder()
                .url("https://optifine.net/adloadx?f=OptiFine_" + optifineVersion)
                .build();
        try
        {
            final Response response = client.newCall(request).execute();
            assert response.body() != null;
            final String resp = response.body().string();
            final String[] respLine = resp.split("\n");
            response.body().close();
            String keyLine = "";
            for(String line : respLine) {
                if(line.contains("downloadx?f=OptiFine")) {
                    keyLine = line;
                    break;
                }
            }

            return keyLine.replace("' onclick='onDownload()'>OptiFine " + optifineVersion.replace("_", " ") +"</a>", "").replace("<a href='downloadx?f=OptiFine_" + optifineVersion + "&x=", "").replace(" ", "");
        }
        catch (IOException e)
        {
            this.getLogger().printStackTrace(e);
        }

        return "";
    }

    private String getJsonPreview(String optifineVersion) {
        final Request request = new Request.Builder()
                .url("https://optifine.net/adloadx?f=" + optifineVersion)
                .build();
        try
        {
            final Response response = client.newCall(request).execute();
            assert response.body() != null;
            final String resp = response.body().string();
            final String[] respLine = resp.split("\n");
            response.body().close();
            String keyLine = "";
            for(String line : respLine) {
                if(line.contains("downloadx?f=preview")) {
                    keyLine = line;
                    break;
                }
            }

            return keyLine.replace("' onclick='onDownload()'>" + optifineVersion.replace("_", " ") +"</a>", "").replace("<a href='downloadx?f=" + optifineVersion + "&x=", "").replace(" ", "");
        }
        catch (IOException e)
        {
            this.getLogger().printStackTrace(e);
        }

        return "";
    }

    public ILogger getLogger()
    {
        return this.logger;
    }

    public void setLogger(ILogger logger)
    {
        this.logger = logger;
    }

    public Path getFolder()
    {
        return this.folder;
    }

    public void setFolder(Path folder)
    {
        this.folder = folder;
        try
        {
            Files.createDirectories(this.folder);
        } catch (IOException e)
        {
            this.logger.printStackTrace(e);
        }
    }
}
