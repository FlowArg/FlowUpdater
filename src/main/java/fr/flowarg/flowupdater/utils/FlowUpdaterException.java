package fr.flowarg.flowupdater.utils;

public class FlowUpdaterException extends RuntimeException
{
    public FlowUpdaterException()
    {
        super();
    }

    public FlowUpdaterException(String message)
    {
        super(message);
    }

    public FlowUpdaterException(String message, Throwable cause)
    {
        super(message, cause);
        cause.printStackTrace();
    }

    public FlowUpdaterException(Throwable cause)
    {
        super(cause);
    }
}
