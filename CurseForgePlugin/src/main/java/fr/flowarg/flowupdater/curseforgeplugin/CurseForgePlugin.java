package fr.flowarg.flowupdater.curseforgeplugin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.util.OkHttpUtils;
import fr.flowarg.flowio.FileUtils;
import fr.flowarg.pluginloaderapi.plugin.Plugin;
import okhttp3.HttpUrl;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CurseForgePlugin extends Plugin
{
    public static CurseForgePlugin instance;

    @Override
    public void onStart()
    {
        instance = this;
        this.getLogger().info("Starting CFP (CurseForgePlugin) for FlowUpdater...");
    }

    public URL getURLOfFile(int projectID, int fileID)
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
        final URL url = this.getURLOfFile(projectID, fileID);
        HttpsURLConnection connection = null;

        try
        {
            connection = (HttpsURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);
            connection.setUseCaches(false);
            final String md5 = connection.getHeaderField("ETag").replace("\"", "");
            final int length = Integer.parseInt(connection.getHeaderField("Content-Length"));
            return new CurseMod(url.getFile().substring(url.getFile().lastIndexOf('/') + 1), url.toExternalForm(), md5, length);
        } catch (Exception e)
        {
            this.getLogger().printStackTrace(e);
        } finally
        {
            if (connection != null) connection.disconnect();
        }

        return new CurseMod("", "", "", -1);
    }

    public CurseModPack getCurseModPack(int projectID, int fileID, boolean installExtFiles)
    {
        final URL link = this.getURLOfFile(projectID, fileID);
        final String linkStr = link.toExternalForm();

        HttpsURLConnection connection = null;

        try
        {
            connection = (HttpsURLConnection)link.openConnection();
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);
            connection.setUseCaches(false);
            final String md5 = connection.getHeaderField("ETag").replace("\"", "");
            final File out = new File(this.getDataPluginFolder(), linkStr.substring(linkStr.lastIndexOf('/') + 1));

            if(!out.exists() || !FileUtils.getMD5ofFile(out).equalsIgnoreCase(md5))
            {
                this.getLogger().info(String.format("Downloading %s from %s...", out.getName(), link.toExternalForm()));
                out.getParentFile().mkdirs();
                Files.copy(this.catchForbidden(link), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            this.getLogger().info("Extracting mod pack...");
            final ZipFile zipFile = new ZipFile(out, ZipFile.OPEN_READ, StandardCharsets.UTF_8);
            final Enumeration<? extends ZipEntry> entries = zipFile.entries();
            final File dir = this.getDataPluginFolder().getParentFile().getParentFile();
            while (entries.hasMoreElements())
            {
                final ZipEntry entry = entries.nextElement();
                final File fl = new File(dir, entry.getName().replace("overrides/", ""));
                if(entry.getName().equalsIgnoreCase("manifest.json") && fl.exists() && entry.getCrc() == FileUtils.getCRC32(fl))
                    break;
                if(installExtFiles && !entry.getName().equals("modlist.html"))
                {
                    if(!fl.exists())
                    {
                        if (fl.getName().endsWith(File.separator)) fl.mkdirs();
                        if (!fl.exists()) fl.getParentFile().mkdirs();
                        if (entry.isDirectory()) continue;

                        final InputStream is = zipFile.getInputStream(entry);
                        final BufferedOutputStream fo = new BufferedOutputStream(new FileOutputStream(fl));
                        while (is.available() > 0) fo.write(is.read());
                        fo.close();
                        is.close();
                    }
                }
                else if(entry.getName().equals("manifest.json"))
                {
                    final InputStream is = zipFile.getInputStream(entry);
                    final BufferedOutputStream fo = new BufferedOutputStream(new FileOutputStream(fl));
                    while (is.available() > 0) fo.write(is.read());
                    fo.close();
                    is.close();
                }
            }
            zipFile.close();

            final BufferedReader reader = new BufferedReader(new FileReader(new File(dir, "manifest.json")));
            final JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            final String name = obj.get("name").getAsString();
            final String version = obj.get("version").getAsString();
            final String author = obj.get("author").getAsString();
            final List<CurseModPack.CurseModPackMod> mods = new ArrayList<>();

            this.getLogger().info("Fetching mods...");
            obj.getAsJsonArray("files").forEach(jsonElement -> {
                final JsonObject object = jsonElement.getAsJsonObject();
                mods.add(new CurseModPack.CurseModPackMod(this.getCurseMod(object.get("projectID").getAsInt(), object.get("fileID").getAsInt()), object.get("required").getAsBoolean()));
            });
            reader.close();
            return new CurseModPack(name, version, author, mods, installExtFiles);
        } catch (Exception e)
        {
            this.getLogger().printStackTrace(e);
        }
        finally
        {
            if(connection != null)
                connection.disconnect();
        }
        return new CurseModPack("", "", "", Collections.emptyList(), false);
    }

    public void shutdownOKHTTP()
    {
        if(OkHttpUtils.getClient() != null)
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

    public InputStream catchForbidden(URL url) throws IOException
    {
        final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.addRequestProperty("User-Agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.124 Safari/537.36");
        connection.setInstanceFollowRedirects(true);
        return connection.getInputStream();
    }

    @Override
    public void onStop()
    {
        this.getLogger().info("Stopping CFP...");
    }
}
