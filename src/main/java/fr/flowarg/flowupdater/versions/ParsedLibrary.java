package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.utils.IOUtils;

import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

public class ParsedLibrary
{
    private final Path path;
    private final URL url;
    private final String artifact;
    private final boolean installed;

    public ParsedLibrary(Path path, URL url, String artifact, boolean installed)
    {
        this.path = path;
        this.url = url;
        this.artifact = artifact;
        this.installed = installed;
    }

    public void download(ILogger logger)
    {
        if(this.url != null)
            IOUtils.download(logger, this.url, this.path);
    }

    public Path getPath()
    {
        return this.path;
    }

    public Optional<URL> getUrl()
    {
        return Optional.ofNullable(this.url);
    }

    public String getArtifact()
    {
        return this.artifact;
    }

    public boolean isInstalled()
    {
        return this.installed;
    }
}
