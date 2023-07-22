package fr.flowarg.flowupdater.download.json;

/**
 * This class represents an asset.
 */
public class AssetDownloadable
{
    private final String hash;
    private final long size;
    private final String url;
    private final String file;

    /**
     * Construct a new asset object.
     * @param hash the sha1 of the asset.
     * @param size the size of the asset.
     */
    public AssetDownloadable(String hash, long size)
    {
        this.hash = hash;
        this.size = size;
        final String assetsPath = "/" + this.hash.substring(0, 2) + "/" + this.hash;
        this.url = "https://resources.download.minecraft.net" + assetsPath;
        this.file = "objects" + assetsPath;
    }

    /**
     * Get the hash of the asset.
     * @return the sha1 of the asset.
     */
    public String getHash()
    {
        return this.hash;
    }

    /**
     * Get the length of the asset.
     * @return the size of the asset.
     */
    public long getSize()
    {
        return this.size;
    }

    /**
     * Get the remote url of the asset.
     * @return the url of the asset.
     */
    public String getUrl()
    {
        return this.url;
    }

    /**
     * Get the file path of the asset.
     * @return the relative local path of this asset.
     */
    public String getFile()
    {
        return this.file;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final AssetDownloadable that = (AssetDownloadable)o;
        return this.file.equals(that.file) && this.size == that.size && this.hash.equals(that.hash) && this.url.equals(that.url);
    }

    @Override
    public int hashCode()
    {
        int result = this.hash.hashCode();
        result = 31 * result + (int)(this.size ^ (this.size >>> 32));
        result = 31 * result + this.url.hashCode();
        return result;
    }
}
