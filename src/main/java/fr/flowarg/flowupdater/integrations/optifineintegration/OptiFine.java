package fr.flowarg.flowupdater.integrations.optifineintegration;

/**
 * This class represents a basic OptiFine object.
 */
public class OptiFine
{
    private final String name;
    private final long size;

    OptiFine(String name, long size)
    {
        this.name = name;
        this.size = size;
    }

    /**
     * Get the OptiFine filename.
     * @return the OptiFine filename.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the OptiFine file size.
     * @return the OptiFine file size.
     */
    public long getSize() {
        return this.size;
    }
}
