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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class IOUtils
{
    private static File cachedMinecraftDir = null;

    public static void download(ILogger logger, URL in, File out)
    {
        try
        {
            logger.info(String.format("Downloading %s from %s...", out.getName(), in.toExternalForm()));
            out.getParentFile().mkdirs();
            Files.copy(catchForbidden(in), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            logger.printStackTrace(e);
        }
    }

    public static void copy(ILogger logger, File in, File out)
    {
        try
        {
            logger.info(String.format("Copying %s to %s...", in.getAbsolutePath(), out.getAbsolutePath()));
            out.getParentFile().mkdirs();
            Files.copy(in.toPath(), out.toPath());
        }
        catch (IOException e)
        {
            logger.printStackTrace(e);
        }
    }

    public static String getContent(URL url)
    {
        try(InputStream stream = new BufferedInputStream(catchForbidden(url)))
        {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            final StringBuilder sb = new StringBuilder();

            int character;
            while ((character = reader.read()) != -1) sb.append((char)character);

            return sb.toString();
        } catch (IOException e)
        {
            FlowUpdater.DEFAULT_LOGGER.printStackTrace(e);
        }
        return "";
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
            final Reader reader = new BufferedReader(new InputStreamReader(stream));
            final StringBuilder sb = new StringBuilder();

            int character;
            while ((character = reader.read()) != -1) sb.append((char)character);

            element =  JsonParser.parseString(sb.toString());
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

    public static File getMinecraftFolder()
    {
        if(cachedMinecraftDir == null) cachedMinecraftDir = new File(Platform.isOnWindows() ? System.getenv("APPDATA") : (Platform.isOnMac() ? System.getProperty("user.home") + "/Library/Application Support/" : System.getProperty("user.home")), ".minecraft/");
        return cachedMinecraftDir;
    }

    public static String consumeStringList(List<String> stringList)
    {
        final StringBuilder sb = new StringBuilder();
        stringList.forEach(sb::append);
        return sb.toString();
    }
}
