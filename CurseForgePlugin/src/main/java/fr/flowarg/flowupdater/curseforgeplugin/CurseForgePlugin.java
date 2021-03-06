package fr.flowarg.flowupdater.curseforgeplugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.therandomlabs.curseapi.CurseAPI;
import com.therandomlabs.curseapi.CurseException;
import com.therandomlabs.curseapi.util.OkHttpUtils;
import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowstringer.StringUtils;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CurseForgePlugin
{
    public static final CurseForgePlugin INSTANCE = new CurseForgePlugin();

    private ILogger logger;
    private Path folder;

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

    public CurseMod getCurseMod(ProjectMod mod)
    {
        return this.getCurseMod(mod.getProjectID(), mod.getFileID());
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

        return CurseMod.BAD;
    }

    public CurseModPack getCurseModPack(int projectID, int fileID, boolean installExtFiles)
    {
        try
        {
            this.extractModPack(this.checkForUpdates(projectID, fileID), installExtFiles);
            return this.parseMods(installExtFiles);
        }
        catch (Exception e)
        {
            this.getLogger().printStackTrace(e);
        }
        return CurseModPack.BAD;
    }

    private Path checkForUpdates(int projectID, int fileID) throws Exception
    {
        final URL link = this.getURLOfFile(projectID, fileID);
        final String linkStr = link.toExternalForm();
        final Path outPath = Paths.get(this.getFolder().toString(), linkStr.substring(linkStr.lastIndexOf('/') + 1));

        if(Files.notExists(outPath) || !FileUtils.getMD5(outPath).equalsIgnoreCase(this.getMD5(link)))
        {
            this.getLogger().info(String.format("Downloading %s from %s...", outPath.getFileName().toString(), linkStr));
            Files.createDirectories(outPath.getParent());
            Files.copy(this.catchForbidden(link), outPath, StandardCopyOption.REPLACE_EXISTING);
        }
        return outPath;
    }

    private String getMD5(URL link)
    {
        HttpsURLConnection connection = null;
        try
        {
            connection = (HttpsURLConnection)link.openConnection();
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);
            connection.setUseCaches(false);

            return connection.getHeaderField("ETag").replace("\"", "");
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
        return "";
    }

    private void extractModPack(Path out, boolean installExtFiles) throws Exception
    {
        this.getLogger().info("Extracting mod pack...");
        final ZipFile zipFile = new ZipFile(out.toFile(), ZipFile.OPEN_READ, StandardCharsets.UTF_8);
        final Path dirPath = this.getFolder().getParent();
        final Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements())
        {
            final ZipEntry entry = entries.nextElement();
            final Path flPath = Paths.get(dirPath.toString(), StringUtils.empty(entry.getName(), "overrides/"));
            if(entry.getName().equalsIgnoreCase("manifest.json") && Files.exists(flPath) && entry.getCrc() == FileUtils.getCRC32(flPath))
                break;
            if(installExtFiles && !entry.getName().equals("modlist.html"))
            {
                if(Files.notExists(flPath))
                {
                    if (flPath.getFileName().toString().endsWith(flPath.getFileSystem().getSeparator())) Files.createDirectories(flPath);
                    if (entry.isDirectory()) continue;

                    final NioZipObject nioZipObject = new NioZipObject(flPath, zipFile.getInputStream(entry));
                    nioZipObject.transfer();
                    nioZipObject.close();
                }
            }
            else if(entry.getName().equals("manifest.json"))
            {
                final NioZipObject nioZipObject = new NioZipObject(flPath, zipFile.getInputStream(entry));
                nioZipObject.transfer();
                nioZipObject.close();
            }
        }
        zipFile.close();
    }

    private static class NioZipObject
    {
        private final OutputStream pathStream;
        private final BufferedOutputStream fo;
        private final InputStream is;

        public NioZipObject(Path path, InputStream is) throws IOException
        {
            this.pathStream = Files.newOutputStream(path);
            this.fo = new BufferedOutputStream(this.pathStream);
            this.is = is;
        }

        public void transfer() throws IOException
        {
            while (this.is.available() > 0) this.fo.write(this.is.read());
        }

        public void close() throws IOException
        {
            this.fo.close();
            this.pathStream.close();
            this.is.close();
        }
    }

    private CurseModPack parseMods(boolean installExtFiles) throws Exception
    {
        this.getLogger().info("Fetching mods...");

        final Path dirPath = Paths.get(this.getFolder().getParent().toString());
        final BufferedReader manifestReader = Files.newBufferedReader(Paths.get(dirPath.toString(), "manifest.json"));
        final JsonObject manifestObj = JsonParser.parseReader(manifestReader).getAsJsonObject();
        final List<ProjectMod> manifestFiles = new ArrayList<>();

        manifestObj.getAsJsonArray("files").forEach(jsonElement -> manifestFiles.add(ProjectMod.fromJsonObject(jsonElement.getAsJsonObject())));

        final Path cachePath = Paths.get(dirPath.toString(), "manifest.cache.json");
        if(Files.notExists(cachePath))
            Files.write(cachePath, Collections.singletonList("[]"), StandardCharsets.UTF_8);

        final BufferedReader cacheReader = Files.newBufferedReader(cachePath);
        final JsonArray cacheArray = JsonParser.parseReader(cacheReader).getAsJsonArray();
        final List<CurseModPack.CurseModPackMod> mods = new ArrayList<>();

        cacheArray.forEach(jsonElement -> {
            final JsonObject object = jsonElement.getAsJsonObject();
            final String name = object.get("name").getAsString();
            final String downloadURL = object.get("downloadURL").getAsString();
            final String md5 = object.get("md5").getAsString();
            final int length = object.get("length").getAsInt();
            final ProjectMod projectMod = ProjectMod.fromJsonObject(object);
            final boolean required = projectMod.isRequired();
            mods.add(new CurseModPack.CurseModPackMod(name, downloadURL, md5, length, required));
            manifestFiles.remove(projectMod);
        });

        manifestFiles.forEach(projectMod -> {
            final boolean required = projectMod.isRequired();
            final CurseModPack.CurseModPackMod mod = new CurseModPack.CurseModPackMod(this.getCurseMod(projectMod), required);
            final JsonObject inCache = new JsonObject();

            inCache.addProperty("name", mod.getName());
            inCache.addProperty("downloadURL", mod.getDownloadURL());
            inCache.addProperty("md5", mod.getMd5());
            inCache.addProperty("length", mod.getLength());
            inCache.addProperty("required", required);
            inCache.addProperty("projectID", projectMod.getProjectID());
            inCache.addProperty("fileID", projectMod.getFileID());

            cacheArray.add(inCache);
            mods.add(mod);
        });

        manifestReader.close();
        cacheReader.close();
        Files.write(cachePath, Collections.singletonList(cacheArray.toString()), StandardCharsets.UTF_8);

        final String modPackName = manifestObj.get("name").getAsString();
        final String modPackVersion = manifestObj.get("version").getAsString();
        final String modPackAuthor = manifestObj.get("author").getAsString();

        return new CurseModPack(modPackName, modPackVersion, modPackAuthor, mods, installExtFiles);
    }

    public void shutdownOKHTTP()
    {
        final OkHttpClient client = OkHttpUtils.getClient();
        if(client != null)
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
    }

    public InputStream catchForbidden(URL url) throws IOException
    {
        final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.addRequestProperty("User-Agent", "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.124 Safari/537.36");
        connection.setInstanceFollowRedirects(true);
        return connection.getInputStream();
    }

    private static class ProjectMod
    {
        private final int projectID;
        private final int fileID;
        private final boolean required;

        public ProjectMod(int projectID, int fileID, boolean required)
        {
            this.projectID = projectID;
            this.fileID = fileID;
            this.required = required;
        }

        private static ProjectMod fromJsonObject(JsonObject object)
        {
            return new ProjectMod(object.get("projectID").getAsInt(), object.get("fileID").getAsInt(), object.get("required").getAsBoolean());
        }

        public int getProjectID()
        {
            return this.projectID;
        }

        public int getFileID()
        {
            return this.fileID;
        }

        public boolean isRequired()
        {
            return this.required;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final ProjectMod that = (ProjectMod)o;

            if (this.projectID != that.projectID) return false;
            if (this.fileID != that.fileID) return false;
            return this.required == that.required;
        }

        @Override
        public int hashCode()
        {
            int result = this.projectID;
            result = 31 * result + this.fileID;
            result = 31 * result + (this.required ? 1 : 0);
            return result;
        }
    }

    public ILogger getLogger()
    {
        return this.logger;
    }

    public void setLogger(ILogger logger)
    {
        this.logger = logger;
    }

    public Path getFolder()
    {
        return this.folder;
    }

    public void setFolder(Path folder)
    {
        this.folder = folder;
        try
        {
            Files.createDirectories(this.folder);
        } catch (IOException e)
        {
            this.logger.printStackTrace(e);
        }
    }
}
