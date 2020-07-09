package fr.flowarg.flowupdater.versions.download.assets;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class AssetIndex
{
    private Map<String, AssetDownloadable> objects = new LinkedHashMap<>();

    public Map<String, AssetDownloadable> getObjects()
    {
        return this.objects;
    }

    public Map<AssetDownloadable, String> getUniqueObjects()
    {
        final Map<AssetDownloadable, String> result = new HashMap<>();
        for (Map.Entry<String, AssetDownloadable> entry : this.objects.entrySet())
            result.put(entry.getValue(), entry.getKey());
        return result;
    }
}
