package fr.flowarg.flowupdater.integrations.optifineintegration;

public class OptiFine
{
    private final String name;
    private final int size;

    OptiFine(String name, int size)
    {
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return this.name;
    }

    public int getSize() {
        return this.size;
    }
}
