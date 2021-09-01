package fr.flowarg.flowupdater.integrations.curseforgeintegration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowstringer.StringUtils;
import fr.flowarg.flowupdater.download.json.CurseFileInfo;
import fr.flowarg.flowupdater.integrations.Integration;
import fr.flowarg.flowupdater.utils.FlowUpdaterException;
import fr.flowarg.flowupdater.utils.IOUtils;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CurseForgeIntegration extends Integration
{
    public CurseForgeIntegration(ILogger logger, Path folder) throws Exception
    {
        super(logger, folder);
    }

    @NotNull
    public URL getURLOfFile(int projectID, int fileID)
    {
        try
        {
            return new URL(IOUtils.getContent(new URL(String.format("https://addons-ecs.forgesvc.net/api/v2/addon/%d/file/%d/download-url", projectID, fileID))));
        } catch (Exception e)
        {
            throw new FlowUpdaterException(e);
        }
    }

    @NotNull
    public CurseMod getCurseMod(@NotNull ProjectMod mod)
    {
        return this.getCurseMod(mod.getProjectID(), mod.getFileID());
    }

    @NotNull
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
            throw new FlowUpdaterException(e);
        } finally
        {
            if (connection != null) connection.disconnect();
        }
    }

    public CurseModPack getCurseModPack(int projectID, int fileID, boolean installExtFiles)
    {
        try
        {
            this.extractModPack(this.checkForUpdates(projectID, fileID), installExtFiles);
            return this.parseMods();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private @NotNull Path checkForUpdates(int projectID, int fileID) throws Exception
    {
        final URL link = this.getURLOfFile(projectID, fileID);
        final String linkStr = link.toExternalForm();
        final Path outPath = this.folder.resolve(linkStr.substring(linkStr.lastIndexOf('/') + 1));
        final String md5 = this.getMD5(link);

        if(Files.notExists(outPath) || (!md5.contains("-") && !FileUtils.getMD5(outPath).equalsIgnoreCase(md5)))
            IOUtils.download(this.logger, link, outPath);

        return outPath;
    }

    private @NotNull String getMD5(URL link)
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
            throw new FlowUpdaterException(e);
        }
        finally
        {
            if(connection != null)
                connection.disconnect();
        }
    }

    private void extractModPack(@NotNull Path out, boolean installExtFiles) throws Exception
    {
        this.logger.info("Extracting mod pack...");
        final ZipFile zipFile = new ZipFile(out.toFile(), ZipFile.OPEN_READ, StandardCharsets.UTF_8);
        final Path dirPath = this.folder.getParent();
        final Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements())
        {
            final ZipEntry entry = entries.nextElement();
            final Path flPath = dirPath.resolve(StringUtils.empty(entry.getName(), "overrides/"));
            final String entryName = entry.getName();

            if(entryName.equalsIgnoreCase("manifest.json"))
            {
                if(Files.notExists(flPath) || entry.getCrc() != FileUtils.getCRC32(flPath))
                    this.transferAndClose(flPath, zipFile, entry);
                continue;
            }

            if(entryName.equals("modlist.html"))
                continue;

            if(installExtFiles)
            {
                if(Files.notExists(flPath))
                {
                    if (flPath.getFileName().toString().endsWith(flPath.getFileSystem().getSeparator())) Files.createDirectories(flPath);
                    if (entry.isDirectory()) continue;

                    this.transferAndClose(flPath, zipFile, entry);
                }
            }
        }
        zipFile.close();
    }

    private void transferAndClose(@NotNull Path flPath, ZipFile zipFile, ZipEntry entry) throws Exception
    {
        if(Files.notExists(flPath.getParent()))
            Files.createDirectories(flPath.getParent());
        try(OutputStream pathStream = Files.newOutputStream(flPath);
            BufferedOutputStream fo = new BufferedOutputStream(pathStream);
            InputStream is = zipFile.getInputStream(entry)
        )
        {
            while (is.available() > 0) fo.write(is.read());
        }
    }

    private @NotNull CurseModPack parseMods() throws Exception
    {
        this.logger.info("Fetching mods...");

        final Path dirPath = Paths.get(this.folder.getParent().toString());
        final BufferedReader manifestReader = Files.newBufferedReader(Paths.get(dirPath.toString(), "manifest.json"));
        final JsonObject manifestObj = JsonParser.parseReader(manifestReader).getAsJsonObject();
        final List<ProjectMod> manifestFiles = new ArrayList<>();

        manifestObj.getAsJsonArray("files").forEach(jsonElement -> manifestFiles.add(ProjectMod.fromJsonObject(jsonElement.getAsJsonObject())));

        final Path cachePath = Paths.get(dirPath.toString(), "manifest.cache.json");
        if(Files.notExists(cachePath))
            Files.write(cachePath, Collections.singletonList("[]"), StandardCharsets.UTF_8);

        final BufferedReader cacheReader = Files.newBufferedReader(cachePath);
        final JsonArray cacheArray = JsonParser.parseReader(cacheReader).getAsJsonArray();
        final Queue<CurseModPack.CurseModPackMod> mods = new ConcurrentLinkedQueue<>();

        cacheArray.forEach(jsonElement -> {
            final JsonObject object = jsonElement.getAsJsonObject();
            final String name = object.get("name").getAsString();
            final String downloadURL = object.get("downloadURL").getAsString();
            final String md5 = object.get("md5").getAsString();
            final int length = object.get("length").getAsInt();
            final ProjectMod projectMod = ProjectMod.fromJsonObject(object);
            mods.add(new CurseModPack.CurseModPackMod(name, downloadURL, md5, length, projectMod.isRequired()));
            manifestFiles.remove(projectMod);
        });

        final ExecutorService executorService = Executors.newCachedThreadPool();

        manifestFiles.forEach(projectMod -> executorService.submit(() -> {
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
        }));

        try
        {
            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e)
        {
            throw new FlowUpdaterException(e);
        }

        manifestReader.close();
        cacheReader.close();
        Files.write(cachePath, Collections.singletonList(cacheArray.toString()), StandardCharsets.UTF_8);

        final String modPackName = manifestObj.get("name").getAsString();
        final String modPackVersion = manifestObj.get("version").getAsString();
        final String modPackAuthor = manifestObj.get("author").getAsString();

        return new CurseModPack(modPackName, modPackVersion, modPackAuthor, new ArrayList<>(mods));
    }

    private static class ProjectMod extends CurseFileInfo
    {
        private final boolean required;

        public ProjectMod(int projectID, int fileID, boolean required)
        {
            super(projectID, fileID);
            this.required = required;
        }

        private static @NotNull ProjectMod fromJsonObject(@NotNull JsonObject object)
        {
            return new ProjectMod(object.get("projectID").getAsInt(), object.get("fileID").getAsInt(), object.get("required").getAsBoolean());
        }

        public boolean isRequired()
        {
            return this.required;
        }
    }
}
