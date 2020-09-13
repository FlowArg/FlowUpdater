package fr.flowarg.flowupdater.utils.builderapi;

import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.versions.VanillaVersion;

/**
 * Builder API
 * 
 * @author flow
 * @version 1.3
 * 
 * Used for {@link FlowUpdater} & {@link VanillaVersion}
 * @param <T> Object Argument
 * 
 * This exception is throwed when an error occured with Builder API.
 */
public class BuilderArgumentException extends Exception
{
	private static final long serialVersionUID = 1L;

    public BuilderArgumentException()
    {
        super();
    }

    public BuilderArgumentException(String reason)
    {
        super(reason);
    }

    public BuilderArgumentException(String reason, Throwable cause)
    {
        super(reason, cause);
    }

    public BuilderArgumentException(Throwable cause)
    {
        super(cause);
    }
}
