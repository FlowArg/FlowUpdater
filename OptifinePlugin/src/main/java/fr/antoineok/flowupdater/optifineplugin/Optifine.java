package fr.antoineok.flowupdater.optifineplugin;

import com.google.gson.JsonObject;
import fr.flowarg.pluginloaderapi.api.JsonSerializable;

public class Optifine implements JsonSerializable {
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

    @Override
    public String toString() {
        return this.toJson();
    }

    @Override
    public String toJson()
    {
        final JsonObject obj = new JsonObject();
        obj.addProperty("name", this.name);
        obj.addProperty("size", this.size);
        return obj.toString();
    }
}
