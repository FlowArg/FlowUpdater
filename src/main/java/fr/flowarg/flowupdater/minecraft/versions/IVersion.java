package fr.flowarg.flowupdater.minecraft.versions;

import com.google.gson.*;

import java.io.*;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

public interface IVersion
{
    String getName();

    JsonArray getMinecraftLibrariesJson();
    JsonObject getMinecraftClient();
    JsonObject getMinecraftServer();

    JsonObject getMinecraftAssetsIndex();

    class Builder
    {
        private String name;

        public Builder(String name)
        {
            this.name = name;
        }

        public IVersion build(boolean isSnapshot)
        {
            return new IVersion()
            {
                private final JsonElement JSON = this.readData(this.getJsonVersion());

                @Override
                public String getName()
                {
                    return Builder.this.name;
                }

                private InputStream getJsonVersion()
                {
                    final AtomicReference<String> version = new AtomicReference<>(null);
                    final AtomicReference<InputStream> result = new AtomicReference<>(null);

                    try
                    {
                        final JsonObject launcherMeta = this.readData(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json").openStream()).getAsJsonObject();

                        if (this.getName().equals("latest"))
                        {
                            if (isSnapshot)
                                version.set(launcherMeta.getAsJsonObject("latest").get("snapshot").getAsString());
                            else version.set(launcherMeta.getAsJsonObject("latest").get("release").getAsString());
                        }
                        else version.set(this.getName());
                        launcherMeta.getAsJsonArray("versions").forEach(jsonElement ->
                        {
                            if (jsonElement.getAsJsonObject().get("id").getAsString().equals(version.get()))
                            {
                                try
                                {
                                    result.set(new URL(jsonElement.getAsJsonObject().get("url").getAsString()).openStream());
                                } catch (IOException e)
                                {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }

                    return result.get();
                }

                @Override
                public JsonArray getMinecraftLibrariesJson()
                {
                    return JSON.getAsJsonObject().getAsJsonArray("libraries");
                }

                @Override
                public JsonObject getMinecraftClient()
                {
                    return JSON.getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("client");
                }

                @Override
                public JsonObject getMinecraftServer()
                {
                    return JSON.getAsJsonObject().getAsJsonObject("downloads").getAsJsonObject("server");
                }

                @Override
                public JsonObject getMinecraftAssetsIndex()
                {
                    return JSON.getAsJsonObject().getAsJsonObject("assetIndex");
                }

                private JsonElement readData(InputStream input)
                {
                    try(InputStream stream = new BufferedInputStream(input))
                    {
                        final Reader reader = new BufferedReader(new InputStreamReader(stream));
                        final StringBuilder sb = new StringBuilder();

                        int character;
                        while ((character = reader.read()) != -1) sb.append((char)character);

                        return JsonParser.parseString(sb.toString());
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    return JsonNull.INSTANCE;
                }
            };
        }
    }
}
