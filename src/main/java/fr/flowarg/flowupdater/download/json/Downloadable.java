package fr.flowarg.flowupdater.download.json;

public class Downloadable
{
    private final String url;
    private final long size;
    private final String sha1;
    private final String name;

    public Downloadable(String url, long size, String sha1, String name)
    {
        this.url = url;
        this.size = size;
        this.sha1 = sha1;
        this.name = name;
    }

    public String getUrl()
    {
        return this.url;
    }
    public long getSize()
    {
        return this.size;
    }
    public String getSha1()
    {
        return this.sha1;
    }
    public String getName()
    {
        return this.name;
    }
}
