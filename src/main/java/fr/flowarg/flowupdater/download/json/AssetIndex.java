package fr.flowarg.flowupdater.download.json;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AssetIndex
{
    private final Map<String, AssetDownloadable> objects = new LinkedHashMap<>();

    public Map<String, AssetDownloadable> getObjects()
    {
        return this.objects;
    }

    public Map<String, AssetDownloadable> getUniqueObjects()
    {
        return Collections.unmodifiableMap(this.getObjects());
    }
}
