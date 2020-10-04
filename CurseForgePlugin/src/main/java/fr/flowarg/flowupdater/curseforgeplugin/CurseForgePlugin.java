package fr.flowarg.flowupdater.curseforgeplugin;

import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.util.OkHttpUtils;
import fr.flowarg.pluginloaderapi.plugin.Plugin;
import okhttp3.HttpUrl;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;

public class CurseForgePlugin extends Plugin
{
    public static CurseForgePlugin instance;

    @Override
    public void onStart()
    {
        instance = this;
        this.getLogger().info("Starting CFP (CurseForgePlugin) for FlowUpdater...");
    }

    public URL getURLOfMod(int projectID, int fileID)
    {
        try
        {
            return CurseAPI.fileDownloadURL(projectID, fileID).map(HttpUrl::url).orElse(null);
        } catch (CurseException e)
        {
            this.getLogger().printStackTrace(e);
        }
        return null;
    }

    public CurseMod getCurseMod(int projectID, int fileID)
    {
        final URL url = this.getURLOfMod(projectID, fileID);
        HttpsURLConnection connection = null;

        try
        {
            connection = (HttpsURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);
            connection.setUseCaches(false);
            final String md5 = connection.getHeaderField("ETag").replace("\"", "");
            final int length  = Integer.parseInt(connection.getHeaderField("Content-Length"));
            return new CurseMod(url.getFile().substring(url.getFile().lastIndexOf('/') + 1), url.toExternalForm(), md5, length);
        }
        catch (Exception e)
        {
            this.getLogger().printStackTrace(e);
        }
        finally
        {
            if(connection != null)
                connection.disconnect();
        }

        return new CurseMod("", "", "", -1);
    }

    public void shutdownOKHTTP()
    {
        if (OkHttpUtils.getClient() != null)
        {
            OkHttpUtils.getClient().dispatcher().executorService().shutdown();
            OkHttpUtils.getClient().connectionPool().evictAll();
            if(OkHttpUtils.getClient().cache() != null)
            {
                try
                {
                    OkHttpUtils.getClient().cache().close();
                } catch (IOException ignored) {}
            }
        }
    }

    @Override
    public void onStop()
    {
        this.getLogger().info("Stopping CFP...");
    }
}
