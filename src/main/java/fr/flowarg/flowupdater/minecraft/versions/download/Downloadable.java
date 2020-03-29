package fr.flowarg.flowupdater.minecraft.versions.download;

public class Downloadable
{
    private String url;
    private int size;
    private String sha1;
    private String name;

    public Downloadable(String url, int size, String sha1, String name)
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

    public int getSize()
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
