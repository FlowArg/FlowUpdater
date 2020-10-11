package fr.flowarg.flowupdater.download.json;

import java.util.HashMap;
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
        final Map<String, AssetDownloadable> result = new HashMap<>();
        for (Map.Entry<String, AssetDownloadable> entry : this.getObjects().entrySet())
            result.put(entry.getKey(), entry.getValue());
        return result;
    }
}
