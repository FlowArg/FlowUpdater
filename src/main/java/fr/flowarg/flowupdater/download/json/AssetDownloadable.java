package fr.flowarg.flowupdater.download.json;

public class AssetDownloadable
{
    private final String hash;
    private final long size;
    private final String url;
    private final String file;

    public AssetDownloadable(String hash, long size)
    {
        this.hash = hash;
        this.size = size;
        final String assetsPath = "/" + this.hash.substring(0, 2) + "/" + this.hash;
        this.url = "https://resources.download.minecraft.net" + assetsPath;
        this.file = "objects" + assetsPath;
    }

    public String getHash() { return this.hash; }
    public long getSize() { return this.size; }
    public String getUrl() { return this.url; }
    public String getFile() { return this.file; }
}
