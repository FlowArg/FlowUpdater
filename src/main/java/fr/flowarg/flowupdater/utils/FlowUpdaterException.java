package fr.flowarg.flowupdater.utils;

/**
 * A simple exception class that represents a FlowUpdater error.
 */
public class FlowUpdaterException extends RuntimeException
{
    /**
     * Initialize the exception.
     */
    public FlowUpdaterException()
    {
        super();
    }

    /**
     * Initialize the exception with an error message.
     * @param message error message.
     */
    public FlowUpdaterException(String message)
    {
        super(message);
    }

    /**
     * Initialize the exception with an error message and a cause.
     * @param message error message.
     * @param cause cause.
     */
    public FlowUpdaterException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Initialize the exception with a cause.
     * @param cause cause.
     */
    public FlowUpdaterException(Throwable cause)
    {
        super(cause);
    }
}
