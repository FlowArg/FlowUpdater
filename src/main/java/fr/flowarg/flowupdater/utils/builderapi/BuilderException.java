package fr.flowarg.flowupdater.utils.builderapi;

import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.versions.AbstractForgeVersion;
import fr.flowarg.flowupdater.versions.VanillaVersion;

/**
 * Builder API
 * 
 * @author flow
 * @version 1.5
 * 
 * Used for {@link FlowUpdater} & {@link VanillaVersion} & {@link AbstractForgeVersion}
 * 
 * This exception is threw when an error occurred with Builder API.
 */
public class BuilderException extends Exception
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
