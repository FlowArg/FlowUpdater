package fr.flowarg.flowupdater.download.json;

import java.util.Objects;

/**
 * This class represents a classic downloadable file such as a library, the client/server or natives.
 */
public class Downloadable
{
    private final String url;
    private final long size;
    private final String sha1;
    private final String name;

    /**
     * Construct a new Downloadable object.
     * @param url the url where to download the file.
     * @param size the size of the file.
     * @param sha1 the sha1 of the file.
     * @param name the name (path) of the file.
     */
    public Downloadable(String url, long size, String sha1, String name)
    {
        this.url = url;
        this.size = size;
        this.sha1 = sha1;
        this.name = name;
    }

    /**
     * Get the url of the file.
     * @return the url of the file.
     */
    public String getUrl()
    {
        return this.url;
    }

    /**
     * Get the size of the file.
     * @return the size of the file.
     */
    public long getSize()
    {
        return this.size;
    }

    /**
     * Get the sha1 of the file.
     * @return the sha1 of the file.
     */
    public String getSha1()
    {
        return this.sha1;
    }

    /**
     * Get the relative path of the file.
     * @return the relative path of the file.
     */
    public String getName()
    {
        return this.name;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Downloadable that = (Downloadable)o;
        return this.size == that.size &&
                Objects.equals(this.url, that.url) &&
                Objects.equals(this.sha1, that.sha1) &&
                Objects.equals(this.name, that.name);
    }
}
