package fr.flowarg.flowupdater.integrations;

import fr.flowarg.flowlogger.ILogger;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The new Integration system replaces an old plugin system
 * which had some problems such as unavailability to communicate directly with FlowUpdater.
 * This new system is easier to use: no more annoying updater's options, no more extra-dependencies.
 * Polymorphism and inheritance can now be used to avoid code duplication.
 */
public abstract class Integration
{
    protected final ILogger logger;
    protected final Path folder;

    /**
     * Default constructor of a basic Integration.
     * @param logger the logger used.
     * @param folder the folder where the plugin can work.
     * @throws Exception if an error occurred.
     */
    public Integration(ILogger logger, Path folder) throws Exception
    {
        this.logger = logger;
        this.folder = folder;
        Files.createDirectories(this.folder);
    }
}
