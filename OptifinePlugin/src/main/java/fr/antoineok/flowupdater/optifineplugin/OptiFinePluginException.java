package fr.antoineok.flowupdater.optifineplugin;

public class OptiFinePluginException extends RuntimeException
{
    public OptiFinePluginException() {}

    public OptiFinePluginException(String message)
    {
        super(message);
    }

    public OptiFinePluginException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public OptiFinePluginException(Throwable cause)
    {
        super(cause);
    }
}
