package fr.antoineok.flowupdater.optifineplugin;

public class Optifine {
    private final String name;
    private final int size;

    public Optifine(String name, int size) {
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
