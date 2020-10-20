package fr.antoineok.flowupdater.optifineplugin;


import fr.flowarg.pluginloaderapi.plugin.Plugin;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Objects;

public class OptifinePlugin extends Plugin {

    public static OptifinePlugin instance;

    private final OkHttpClient client = new OkHttpClient();

    @Override
    public void onStart() {
        instance = this;
        this.getLogger().info("Starting ODP (OptifineDownloaderPlugin) for FlowUpdater...");
    }

    /**
     *
     * @param optifineVersion the version of Optifine
     * @return the object that defines the plugin
     * @throws IOException if the version is invalid or not found
     */
    public Optifine getOptifine(String optifineVersion) throws IOException {

        String name = "OptiFine_" + optifineVersion + ".jar";

        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse("http://optifine.net/downloadx")).newBuilder();
        urlBuilder.addQueryParameter("f", name);
        urlBuilder.addQueryParameter("x", getJson(optifineVersion));

        String newUrl = urlBuilder.build().toString();


        Request request = new Request.Builder()
                .url(newUrl)
                .build();
        Response response = client.newCall(request).execute();
        final int length  = Integer.parseInt(Objects.requireNonNull(response.header("Content-Length")));
        if(length == 16)
            throw new IOException("Given ersion of Optifine not found.");
        assert response.body() != null;
        response.body().close();

        return new Optifine(name, newUrl, length);

    }

    public void shutdownOKHTTP()
    {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
        if(client.cache() != null)
        {
            try
            {
                Objects.requireNonNull(client.cache()).close();
            } catch (IOException ignored) {}
        }
    }

    /**
     * @param optifineVersion the version of Optifine
     * @return the download key
     */
    private String getJson(String optifineVersion) {
        Request request = new Request.Builder()
                .url("http://optifine.net/adloadx?f=OptiFine_" + optifineVersion)
                .build();
        try
        {
            Response response = client.newCall(request).execute();
            assert response.body() != null;
            String resp = response.body().string();
            String[] respLine = resp.split("\n");
            response.body().close();
            String keyLine = "";
            for(String line : respLine){
                if(line.contains("downloadx?f=OptiFine")){
                    keyLine = line;
                    break;
                }
            }

            return keyLine.replace("' onclick='onDownload()'>OptiFine " + optifineVersion.replace("_", " ") +"</a>", "").replace("<a href='downloadx?f=OptiFine_" + optifineVersion + "&x=", "").replace(" ", "");
        }
        catch (IOException e)
        {
            this.getLogger().printStackTrace(e);
        }


        return "";
    }


    @Override
    public void onStop() {
        this.getLogger().info("Stopping ODP...");
    }
}
