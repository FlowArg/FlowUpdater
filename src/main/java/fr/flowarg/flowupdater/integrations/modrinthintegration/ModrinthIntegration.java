package fr.flowarg.flowupdater.integrations.modrinthintegration;

import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.integrations.Integration;

import java.nio.file.Path;

public class ModrinthIntegration extends Integration
{
    /**
     * Default constructor of a basic Integration.
     *
     * @param logger the logger used.
     * @param folder the folder where the plugin can work.
     * @throws Exception if an error occurred.
     */
    public ModrinthIntegration(ILogger logger, Path folder) throws Exception
    {
        super(logger, folder);
    }
}
