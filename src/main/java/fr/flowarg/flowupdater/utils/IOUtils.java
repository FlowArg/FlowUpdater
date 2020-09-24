package fr.flowarg.flowupdater.utils;

import fr.flowarg.flowlogger.ILogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
}
