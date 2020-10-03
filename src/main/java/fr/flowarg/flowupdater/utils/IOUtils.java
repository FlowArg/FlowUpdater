package fr.flowarg.flowupdater.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.flowarg.flowlogger.ILogger;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class IOUtils
{
    public static String toString(final InputStream input, final Charset encoding) throws IOException
    {
        try (final StringBuilderWriter sw = new StringBuilderWriter())
        {
            final InputStreamReader in = new InputStreamReader(input, encoding == null ? StandardCharsets.UTF_8 : encoding);
            final char[] buffer = new char[4096];
            int n;
            while ((n = in.read(buffer)) != -1)
            	sw.write(buffer, 0, n);          
            return sw.toString();
        }
    }
    
    public static void download(ILogger logger, URL in, File out)
    {
        try
        {
            logger.info(String.format("Downloading %s from %s...", out.getName(), in.toExternalForm()));
            out.getParentFile().mkdirs();
			Files.copy(in.openStream(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
        catch (IOException e)
        {
			logger.printStackTrace(e);
		}
    }

    public static JsonElement readData(URL jsonURL)
    {
        JsonElement element = JsonNull.INSTANCE;
        try(InputStream stream = new BufferedInputStream(jsonURL.openStream()))
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
}
