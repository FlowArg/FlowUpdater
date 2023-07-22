package fr.flowarg.flowupdater.download.json;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class represents an asset index of a Minecraft version.
 */
public class AssetIndex
{
    private final Map<String, AssetDownloadable> objects = new LinkedHashMap<>();

    /**
     * Internal getter.
     * @return asset objects
     */
    private Map<String, AssetDownloadable> getObjects()
    {
        return this.objects;
    }

    /**
     * Get an immutable collection of asset objects.
     * @return asset objects.
     */
    public Map<String, AssetDownloadable> getUniqueObjects()
    {
        return Collections.unmodifiableMap(this.getObjects());
    }
}
