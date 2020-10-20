package fr.antoineok.flowupdater.optifineplugin;

import com.google.gson.JsonObject;

public class Optifine {

    private String name;
    private String url;
    private int size;

    public Optifine(String name, String url, int size) {
        this.name = name;
        this.url = url;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public int getSize() {
        return size;
    }

    @Override
    public String toString() {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", name);
        obj.addProperty("url", url);
        obj.addProperty("size", size);
        return obj.toString();
    }
}
