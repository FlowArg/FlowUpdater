package fr.flowarg.flowupdater.utils.builderapi;

import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.versions.AbstractForgeVersion;
import fr.flowarg.flowupdater.versions.VanillaVersion;

/**
 * Builder API
 * 
 * @author flow
 * @version 1.6
 * 
 * Used for {@link FlowUpdater} and {@link VanillaVersion} and {@link AbstractForgeVersion}
 * 
 * This exception is thrown when an error occurred with Builder API.
 */
public class BuilderException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public BuilderException()
    {
        super();
    }

    public BuilderException(String reason)
    {
        super(reason);
    }

    public BuilderException(String reason, Throwable cause)
    {
        super(reason, cause);
    }

    public BuilderException(Throwable cause)
    {
        super(cause);
    }
}
