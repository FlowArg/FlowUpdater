package fr.flowarg.flowupdater.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;
import fr.flowarg.flowcompat.Platform;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;

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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IOUtils
{
    private static Path cachedMinecraftPath = null;

    public static void download(ILogger logger, URL in, Path out)
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

    public static void copy(ILogger logger, Path in, Path out)
    {
        try
        {
            logger.info(String.format("Copying %s to %s...", in.toString(), out.toString()));
            Files.createDirectories(out.getParent());
            Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            logger.printStackTrace(e);
        }
    }

    public static String getContent(URL url)
    {
        final StringBuilder sb = new StringBuilder();

        try(InputStream stream = new BufferedInputStream(catchForbidden(url)))
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
            e.printStackTrace();
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
            e.printStackTrace();
        }

        return element.getAsJsonObject();
    }

    public static InputStream catchForbidden(URL url) throws IOException
    {
        final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.addRequestProperty("User-Agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.124 Safari/537.36");
        connection.setInstanceFollowRedirects(true);
        return connection.getInputStream();
    }

    public static Path getMinecraftFolder()
    {
        if(cachedMinecraftPath == null)
            cachedMinecraftPath = Paths.get(
                    Platform.isOnWindows() ? System.getenv("APPDATA")
                    : (Platform.isOnMac() ? System.getProperty("user.home") + "/Library/Application Support/" : System.getProperty("user.home")),
                    ".minecraft"
            );
        return cachedMinecraftPath;
    }

    public static String consumeStringList(List<String> stringList)
    {
        final StringBuilder sb = new StringBuilder();
        stringList.forEach(sb::append);
        return sb.toString();
    }

    public static void deleteDirectory(final Path folder) throws IOException
    {
        if (Files.exists(folder) && Files.isDirectory(folder))
        {
            final List<Path> files = listRecursive(folder);
            for (final Path f : files)
                Files.delete(f);

            Files.delete(folder);
        }
    }

    public static List<Path> listRecursive(final Path directory) throws IOException
    {
        final List<Path> files = new ArrayList<>();
        final List<Path> fs = list(directory).collect(Collectors.toList());

        for (final Path f : fs)
        {
            if (Files.isDirectory(f)) files.addAll(listRecursive(f));
            files.add(f);
        }
        return files;
    }

    public static Stream<Path> list(final Path dir) throws IOException
    {
        return Files.exists(dir) ? Files.list(dir) : Stream.empty();
    }
}
