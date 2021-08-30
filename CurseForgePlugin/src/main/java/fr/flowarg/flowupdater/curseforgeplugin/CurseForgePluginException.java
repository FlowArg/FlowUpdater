package fr.flowarg.flowupdater.curseforgeplugin;

public class CurseForgePluginException extends RuntimeException
{
    public CurseForgePluginException() {}

    public CurseForgePluginException(String message)
    {
        super(message);
    }

    public CurseForgePluginException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public CurseForgePluginException(Throwable cause)
    {
        super(cause);
    }
}
