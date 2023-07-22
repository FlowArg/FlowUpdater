package fr.flowarg.flowupdater.integrations.optifineintegration;

import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.integrations.Integration;
import fr.flowarg.flowupdater.utils.FlowUpdaterException;
import fr.flowarg.flowupdater.utils.IOUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.HttpsURLConnection;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Optional;

/**
 * This integration supports the download of OptiFine in any version from the official site
 * (<a href="https://optifine.net">OptiFine</a>).
 */
public class OptiFineIntegration extends Integration
{
    public OptiFineIntegration(ILogger logger, Path folder) throws Exception
    {
        super(logger, folder);
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
            final String fixedVersion = preview ? (optiFineVersion.startsWith("preview_OptiFine_") ?
                            optiFineVersion : optiFineVersion.startsWith("OptiFine_") ?
                    "preview_" + optiFineVersion : "preview_OptiFine_" + optiFineVersion) :
                    optiFineVersion.startsWith("OptiFine_") ? optiFineVersion : "OptiFine_" + optiFineVersion;
            final String name = fixedVersion + ".jar";
            final String newUrl = this.getNewURL(name, preview, fixedVersion);
            final GetResponse getResponse = this.getResponse(new URL(newUrl));
            final int length = getResponse.contentLength;

            this.checkForUpdates(name, getResponse.byteStream, length, newUrl);
            getResponse.byteStream.close();

            if(length <= 40)
                throw new FlowUpdaterException("Given version of OptiFine not found.");

            return new OptiFine(name, length);
        }
        catch (FlowUpdaterException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new FlowUpdaterException(e);
        }
    }

    private @NotNull String getNewURL(String name, boolean preview, String optiFineVersion)
    {
        return "https://optifine.net/downloadx?f=" +
                name +
                "&x=" +
                (preview ? this.getJsonPreview(optiFineVersion) : this.getJson(optiFineVersion));
    }

    private void checkForUpdates(String name, InputStream byteStream, int length, String newUrl) throws Exception
    {
        final Path outputPath = this.folder.resolve(name);
        if(Files.notExists(outputPath) || Files.size(outputPath) != length)
        {
            this.logger.info(String.format("Downloading %s from %s...", outputPath.getFileName().toString(), newUrl));
            Files.copy(byteStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
        }
        byteStream.close();
    }

    private @NotNull String getJson(String optiFineVersion)
    {
        try
        {
            final String[] respLine = IOUtils.getContent(new URL("https://optifine.net/adloadx?f=OptiFine_" + optiFineVersion))
                    .split("\n");
            final Optional<String> result = Arrays.stream(respLine).filter(s -> s.contains("downloadx?f=OptiFine")).findFirst();
            if(result.isPresent())
                return result.get()
                        .replace("' onclick='onDownload()'>OptiFine " + optiFineVersion.replace("_", " ") +
                                         "</a>", "")
                        .replace("<a href='downloadx?f=OptiFine_" + optiFineVersion + "&x=", "")
                        .replace(" ", "");
            else throw new FlowUpdaterException("No line found in: " + Arrays.toString(respLine));
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
            final String[] respLine = IOUtils.getContent(new URL("https://optifine.net/adloadx?f=" + optiFineVersion))
                    .split("\n");
            final Optional<String> result = Arrays.stream(respLine).filter(s -> s.contains("downloadx?f=preview")).findFirst();
            if(result.isPresent())
                return result.get()
                        .replace("' onclick='onDownload()'>" + optiFineVersion.replace("_", " ") +
                                         "</a>", "")
                        .replace("<a href='downloadx?f=" + optiFineVersion + "&x=", "")
                        .replace(" ", "");
            else throw new FlowUpdaterException("No line found in: " + Arrays.toString(respLine));
        }
        catch (Exception e)
        {
            throw new FlowUpdaterException(e);
        }
    }

    @Contract("_ -> new")
    private @NotNull GetResponse getResponse(URL url)
    {
        HttpsURLConnection connection;
        try
        {
            connection = (HttpsURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setUseCaches(false);
            connection.addRequestProperty("User-Agent",
                                          "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.124 Safari/537.36");
            connection.setInstanceFollowRedirects(true);
            return new GetResponse(Integer.parseInt(connection.getHeaderField("Content-Length")), connection.getInputStream());
        } catch (Exception e)
        {
            throw new FlowUpdaterException(e);
        }
    }

    private static class GetResponse
    {
        public final int contentLength;
        public final InputStream byteStream;

        public GetResponse(int contentLength, InputStream byteStream)
        {
            this.contentLength = contentLength;
            this.byteStream = byteStream;
        }
    }
}
