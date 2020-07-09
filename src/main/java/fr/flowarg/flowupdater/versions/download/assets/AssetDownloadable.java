package fr.flowarg.flowupdater.versions.download.assets;

import java.net.MalformedURLException;
import java.net.URL;

public class AssetDownloadable
{
    private String hash;
    private int size;
    private String urlString;
    private URL url;
    private String file;

    public AssetDownloadable(String hash, int size) throws MalformedURLException
    {
        this.hash = hash;
        this.size = size;
        this.urlString = "http://resources.download.minecraft.net/" + this.hash.substring(0, 2) + "/" + this.hash;
        this.url = new URL(this.urlString);
        this.file = "/objects/" + this.hash.substring(0, 2) + "/" + this.hash;
    }

    public String getHash() { return this.hash; }
    public int getSize() { return this.size; }
    public String getUrlString() { return this.urlString; }
    public URL getUrl() { return this.url; }
    public String getFile() { return this.file; }
}
