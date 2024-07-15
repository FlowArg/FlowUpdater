package fr.flowarg.flowupdater.integrations.optifineintegration;

import fr.flowarg.flowupdater.download.json.OptiFineInfo;

public interface IOptiFineCompatible
{
    /**
     * Get information about OptiFine to update.
     * @return OptiFine's information.
     */
    OptiFineInfo getOptiFineInfo();
}
