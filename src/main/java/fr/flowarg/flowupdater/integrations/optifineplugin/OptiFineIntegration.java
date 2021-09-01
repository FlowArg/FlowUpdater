package fr.flowarg.flowupdater.integrations.optifineplugin;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.utils.FlowUpdaterException;
import fr.flowarg.flowupdater.utils.IOUtils;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.HttpsURLConnection;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class OptiFineIntegration
{
    private final ILogger logger;
    private final Path folder;

    public OptiFineIntegration(ILogger logger, Path folder) throws Exception
    {
        this.logger = logger;
        this.folder = folder;
        Files.createDirectories(this.folder);
    }

    /**
     * Get an OptiFine object from the official website.
     * @param optiFineVersion the version of OptiFine
     * @param preview if the OptiFine version is a preview.
     * @return the object that defines the plugin
     */
    public OptiFine getOptiFine(String optiFineVersion, boolean preview)
    {
        try
        {
            final String name = preview ? (optiFineVersion.contains("preview_") && optiFineVersion.contains("OptiFine_") ? optiFineVersion + ".jar" : "preview_OptiFine_" + optiFineVersion + ".jar") : "OptiFine_" + optiFineVersion + ".jar";
            final String newUrl = this.getNewURL(name, preview, optiFineVersion);
            final GetResponse getResponse = this.getResponse(new URL(newUrl), "Content-Length");
            final int length = Integer.parseInt(getResponse.header);

            this.checkForUpdates(name, getResponse.byteStream, length, newUrl);
            getResponse.byteStream.close();

            if(length <= 40)
                throw new FlowUpdaterException("Given version of OptiFine not found.");

            return new OptiFine(name, length);
        }
        catch (Exception e)
        {
            throw new FlowUpdaterException(e);
        }
    }

    private @NotNull String getNewURL(String name, boolean preview, String optiFineVersion)
    {
        return "https://optifine.net/downloadx?f=" + name + "&x=" + (preview ? this.getJsonPreview(optiFineVersion) : this.getJson(optiFineVersion));
    }

    private void checkForUpdates(String name, InputStream byteStream, int length, String newUrl) throws Exception
    {
        final Path outputPath = this.folder.resolve(name);
        if(Files.notExists(outputPath) || FileUtils.getFileSizeBytes(outputPath) != length)
        {
            this.logger.info(String.format("Downloading %s from %s...", outputPath.getFileName().toString(), newUrl));
            Files.copy(byteStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
        }
        byteStream.close();
    }

    /**
     * @param optiFineVersion the version of OptiFine
     * @return the download key
     */
    private @NotNull String getJson(String optiFineVersion)
    {
        try
        {
            final String[] respLine = IOUtils.getContent(new URL("https://optifine.net/adloadx?f=OptiFine_" + optiFineVersion)).split("\n");
            String keyLine = "";
            for(String line : respLine)
            {
                if(line.contains("downloadx?f=OptiFine"))
                {
                    keyLine = line;
                    break;
                }
            }

            return keyLine.replace("' onclick='onDownload()'>OptiFine " + optiFineVersion.replace("_", " ") +"</a>", "").replace("<a href='downloadx?f=OptiFine_" + optiFineVersion + "&x=", "").replace(" ", "");
        }
        catch (Exception e)
        {
            throw new FlowUpdaterException(e);
        }
    }

    private @NotNull String getJsonPreview(String optiFineVersion)
    {
        try
        {
            final String[] respLine = IOUtils.getContent(new URL("https://optifine.net/adloadx?f=" + optiFineVersion)).split("\n");
            String keyLine = "";
            for(String line : respLine)
            {
                if(line.contains("downloadx?f=preview"))
                {
                    keyLine = line;
                    break;
                }
            }

            return keyLine.replace("' onclick='onDownload()'>" + optiFineVersion.replace("_", " ") +"</a>", "").replace("<a href='downloadx?f=" + optiFineVersion + "&x=", "").replace(" ", "");
        }
        catch (Exception e)
        {
            throw new FlowUpdaterException(e);
        }
    }

    private GetResponse getResponse(URL url, String header)
    {
        HttpsURLConnection connection;
        try
        {
            connection = (HttpsURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            connection.addRequestProperty("User-Agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.124 Safari/537.36");
            connection.setInstanceFollowRedirects(true);
            return new GetResponse(connection.getHeaderField(header), connection.getInputStream());
        } catch (Exception e)
        {
            throw new FlowUpdaterException(e);
        }
    }

    private static class GetResponse
    {
        public final String header;
        public final InputStream byteStream;

        public GetResponse(String header, InputStream byteStream)
        {
            this.header = header;
            this.byteStream = byteStream;
        }
    }
}
