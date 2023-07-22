package fr.flowarg.flowupdater.utils.builderapi;

/**
 * Builder API; This exception is thrown when an error occurred with Builder API.
 * @version 1.6
 * @author flow
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
