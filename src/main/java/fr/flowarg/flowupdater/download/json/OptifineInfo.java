package fr.flowarg.flowupdater.download.json;

public class OptifineInfo
{
    private final String version;
    private final boolean preview;

    public OptifineInfo(String version, boolean preview)
    {
        this.version = version;
        this.preview = preview;
    }

    public String getVersion()
    {
        return this.version;
    }

    public boolean isPreview()
    {
        return this.preview;
    }
}
