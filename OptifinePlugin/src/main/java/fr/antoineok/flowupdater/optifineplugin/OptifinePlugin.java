package fr.antoineok.flowupdater.optifineplugin;

import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import fr.flowarg.pluginloaderapi.plugin.Plugin;

import java.io.IOException;

public class OptifinePlugin extends Plugin {

    private static OptifinePlugin instance;

    private final OkHttpClient client = new OkHttpClient();

    @Override
    public void onStart() {
        this.getLogger().info("Starting ODP (OptifinePlugin) for FlowUpdater...");
        instance = this;
    }

    public static OptifinePlugin getInstance() {
        return instance;
    }

    public Optifine getOptifineJson(String optifineVersion) throws IOException {

        String name = "OptiFine_" + optifineVersion + ".jar";

        HttpUrl.Builder urlBuilder = HttpUrl.parse("http://optifine.net/downloadx").newBuilder();
        urlBuilder.addQueryParameter("f", name);
        urlBuilder.addQueryParameter("x", getJson(optifineVersion));

        String newUrl = urlBuilder.build().toString();


        Request request = new Request.Builder()
                .url(newUrl)
                .build();
        Response response = client.newCall(request).execute();
        final String length  = response.header("Content-Length");
        response.body().close();

        shutdownOKHTTP();
        return new Optifine(name, newUrl, Integer.parseInt(length));

    }

    private void shutdownOKHTTP()
    {
        if(client.getDispatcher() != null)
            client.getDispatcher().getExecutorService().shutdown();
        if(client.getConnectionPool() != null)
            client.getConnectionPool().evictAll();
        if(client.getCache() != null)
        {
            try
            {
                client.getCache().close();
            } catch (IOException ignored) {}
        }
    }

    /**
     * @throws IOException if the version is invalid or is not found
     * @param optifineVersion
     * @return the download key
     */
    private String getJson(String optifineVersion) throws IOException {
        if(!doesVersionExist(optifineVersion))
            throw new IOException("Version de Optifine non trouvé");
        Request request = new Request.Builder()
                .url("http://optifine.net/adloadx?f=OptiFine_" + optifineVersion)
                .build();
        try
        {
            Response response = client.newCall(request).execute();
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
            String key = keyLine.replace("' onclick='onDownload()'>OptiFine 1.12 HD U F5</a>", "").replace("<a href='downloadx?f=OptiFine_" + optifineVersion + "&x=", "").replace(" ", "");

            return key;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }


        return "";
    }

    /**
     * @param optifineVersion The version of optifine
     * @return true if <code>optifineVersion</code> exist
     **/
    private boolean doesVersionExist(String optifineVersion)
    {
        Request request = new Request.Builder()
                .url("http://optifine.net/adloadx?f=OptiFine_" + optifineVersion + ".jar")
                .build();
        try
        {
            Response response = client.newCall(request).execute();
            boolean succ = response.isSuccessful();
            response.body().close();
            return succ;

        }
        catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
    }


    @Override
    public void onStop() {
        this.getLogger().info("Stopping ODP...");
    }
}
