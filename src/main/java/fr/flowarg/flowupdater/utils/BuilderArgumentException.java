package fr.flowarg.flowupdater.utils;

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
