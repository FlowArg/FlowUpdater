package fr.flowarg.flowupdater.versions.fabric;

import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.utils.IOUtils;

import java.net.URL;
import java.nio.file.Path;

public class ParsedLibrary
{
    private final Path path;
    private final URL url;
    private final boolean installed;

    public ParsedLibrary(Path path, URL url, boolean installed)
    {
        this.path = path;
        this.url = url;
        this.installed = installed;
    }

    public void download(ILogger logger)
    {
        IOUtils.download(logger, this.url, this.path);
    }

    public Path getPath()
    {
        return this.path;
    }

    public URL getUrl()
    {
        return this.url;
    }

    public boolean isInstalled()
    {
        return this.installed;
    }
}
