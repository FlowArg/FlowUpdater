package fr.flowarg.flowupdater.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;
import fr.flowarg.flowcompat.Platform;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A basic I/O utility class.
 */
public class IOUtils
{
    private static Path cachedMinecraftPath = null;

    /**
     * Download a remote file to a destination file.
     * @param logger a valid logger instance.
     * @param in the input url.
     * @param out the output file.
     */
    public static void download(@NotNull ILogger logger, @NotNull URL in, @NotNull Path out)
    {
        try
        {
            logger.info(String.format("Downloading %s from %s...", out.getFileName().toString(), in.toExternalForm()));
            Files.createDirectories(out.getParent());
            Files.copy(catchForbidden(in), out, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            logger.printStackTrace(e);
        }
    }

    /**
     * Copy a local file to a destination file.
     * @param logger a valid logger instance.
     * @param in the input file.
     * @param out the output file.
     */
    public static void copy(@NotNull ILogger logger, @NotNull Path in, @NotNull Path out)
    {
        try
        {
            logger.info(String.format("Copying %s to %s...", in, out));
            Files.createDirectories(out.getParent());
            Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            logger.printStackTrace(e);
        }
    }

    /**
     * Get the content from a remote url.
     * @param url the destination url
     * @return the content.
     */
    public static @NotNull String getContent(URL url)
    {
        try
        {
            return getContent(catchForbidden(url));
        } catch (IOException e)
        {
            FlowUpdater.DEFAULT_LOGGER.printStackTrace(e);
            return "";
        }
    }

    /**
     * Get the content from a remote stream.
     * @param remote the remote stream
     * @return the content.
     */
    public static @NotNull String getContent(InputStream remote)
    {
        final StringBuilder sb = new StringBuilder();

        try(InputStream stream = new BufferedInputStream(remote))
        {
            final ReadableByteChannel rbc = Channels.newChannel(stream);
            final Reader enclosedReader = Channels.newReader(rbc, StandardCharsets.UTF_8.newDecoder(), -1);
            final BufferedReader reader = new BufferedReader(enclosedReader);

            int character;
            while ((character = reader.read()) != -1) sb.append((char)character);

            reader.close();
            enclosedReader.close();
            rbc.close();

        } catch (IOException e)
        {
            FlowUpdater.DEFAULT_LOGGER.printStackTrace(e);
        }
        return sb.toString();
    }

    /**
     * Reading an url in a json element
     * @param jsonURL json input
     * @return a json element
     */
    public static JsonElement readJson(URL jsonURL)
    {
        try
        {
            return readJson(catchForbidden(jsonURL));
        } catch (IOException e)
        {
            FlowUpdater.DEFAULT_LOGGER.printStackTrace(e);
        }
        return JsonNull.INSTANCE;
    }

    /**
     * Reading an inputStream in a json element
     * @param inputStream json input
     * @return a json element
     */
    public static JsonElement readJson(InputStream inputStream)
    {
        JsonElement element = JsonNull.INSTANCE;
        try(InputStream stream = new BufferedInputStream(inputStream))
        {
            final ReadableByteChannel rbc = Channels.newChannel(stream);
            final Reader enclosedReader = Channels.newReader(rbc, StandardCharsets.UTF_8.newDecoder(), -1);
            final BufferedReader reader = new BufferedReader(enclosedReader);
            final StringBuilder sb = new StringBuilder();

            int character;
            while ((character = reader.read()) != -1) sb.append((char)character);

            element = JsonParser.parseString(sb.toString());

            reader.close();
            enclosedReader.close();
            rbc.close();
        } catch (IOException e)
        {
            FlowUpdater.DEFAULT_LOGGER.printStackTrace(e);
        }

        return element.getAsJsonObject();
    }

    /**
     * A trick to avoid some forbidden response.
     * @param url the destination url.
     * @return the opened connection.
     * @throws IOException if an I/O error occurred.
     */
    public static InputStream catchForbidden(@NotNull URL url) throws IOException
    {
        final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.addRequestProperty("User-Agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.124 Safari/537.36");
        connection.setInstanceFollowRedirects(true);
        return connection.getInputStream();
    }

    /**
     * Execute asynchronously a task for a collection of items.
     * @param iterable the collection of items.
     * @param service the executor service.
     * @param runnable the task to execute.
     * @param <T> the type of the items.
     */
    public static <T> void executeAsyncForEach(Iterable<T> iterable, ExecutorService service, Consumer<T> runnable)
    {
        try
        {
            iterable.forEach(t -> service.submit(() -> runnable.accept(t)));
            service.shutdown();
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e)
        {
            throw new FlowUpdaterException(e);
        }
    }

    public static void downloadArtifacts(ILogger logger, Path dir, String repositoryUrl, String[] metadata)
    {
        final String group = metadata[0];
        final String name = metadata[1];
        final String version = metadata[2];

        final String groupSlash = group.replace('.', '/');
        final String groupDir = group.replace(".", dir.getFileSystem().getSeparator());

        try
        {
            IOUtils.download(logger,
                             new URL(repositoryUrl + groupSlash + '/' + name + '/' + version + '/' +
                                             String.format("%s-%s.jar", name, version)),
                             dir.resolve(groupDir)
                                     .resolve(name)
                                     .resolve(version)
                                     .resolve(String.format("%s-%s.jar", name, version)));
        } catch (Exception e)
        {
            logger.printStackTrace(e);
        }
    }

    public static @Nullable String getLatestArtifactVersion(String mavenMetadataUrl)
    {
        try
        {
            final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            final Document doc = dBuilder.parse(new URL(mavenMetadataUrl).openStream());

            return getLatestArtifactVersion(doc);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public static String getLatestArtifactVersion(@NotNull Document doc)
    {
        doc.getDocumentElement().normalize();

        final Element root = doc.getDocumentElement();
        final NodeList nList = root.getElementsByTagName("versioning");
        String version = "";

        for (int temp = 0; temp < nList.getLength(); temp++)
        {
            final Node node = nList.item(temp);
            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;
            version = ((Element) node).getElementsByTagName("release").item(0).getTextContent();
        }
        return version;
    }

    /**
     * Retrieve the local Minecraft folder path.
     * @return the Minecraft folder path.
     */
    public static Path getMinecraftFolder()
    {
        return cachedMinecraftPath == null ?
            cachedMinecraftPath = Paths.get(
                    Platform.isOnWindows() ? System.getenv("APPDATA")
                    : (Platform.isOnMac() ? System.getProperty("user.home") + "/Library/Application Support/"
                            : System.getProperty("user.home")), ".minecraft")
                : cachedMinecraftPath;
    }
}
