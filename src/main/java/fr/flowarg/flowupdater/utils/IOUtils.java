package fr.flowarg.flowupdater.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class IOUtils
{
    public static String toString(final InputStream input, final Charset encoding) throws IOException
    {
        try (final StringBuilderWriter sw = new StringBuilderWriter())
        {
            final InputStreamReader in = new InputStreamReader(input, encoding == null ? Charset.forName("UTF-8") : encoding);
            final char[] buffer = new char[4096];
            int n;
            while ((n = in.read(buffer)) != -1)
            	sw.write(buffer, 0, n);          
            return sw.toString();
        }
    }
}
