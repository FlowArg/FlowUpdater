package fr.flowarg.flowupdater.integrations;

import fr.flowarg.flowlogger.ILogger;

import java.nio.file.Files;
import java.nio.file.Path;

public abstract class Integration
{
    protected final ILogger logger;
    protected final Path folder;

    public Integration(ILogger logger, Path folder) throws Exception
    {
        this.logger = logger;
        this.folder = folder;
        Files.createDirectories(this.folder);
    }
}
