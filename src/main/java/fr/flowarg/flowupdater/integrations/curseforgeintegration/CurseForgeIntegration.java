package fr.flowarg.flowupdater.integrations.curseforgeintegration;

import com.google.gson.*;
import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowstringer.StringUtils;
import fr.flowarg.flowupdater.download.json.CurseFileInfo;
import fr.flowarg.flowupdater.download.json.CurseModPackInfo;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.integrations.Integration;
import fr.flowarg.flowupdater.utils.IOUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This integration supports all CurseForge stuff that FlowUpdater needs such as retrieve mods and mod packs from CurseForge.
 */
public class CurseForgeIntegration extends Integration
{
    private static final String CF_API_URL = "https://api.curseforge.com";
    private static final String CF_API_KEY = "JDJhJDEwJHBFZjhacXFwWE4zbVdtLm5aZ2pBMC5kdm9ibnhlV3hQZWZma2Q5ZEhCRWFid2VaUWh2cUtpJDJhJ";
    private static final String MOD_FILE_ENDPOINT = "/v1/mods/{modId}/files/{fileId}";

    private boolean changed = false;

    /**
     * Default constructor of a basic Integration.
     *
     * @param logger the logger used.
     * @param folder the folder where the plugin can work.
     * @throws Exception if an error occurred.
     */
    public CurseForgeIntegration(ILogger logger, Path folder) throws Exception
    {
        super(logger, folder);
    }

    public Mod fetchMod(CurseFileInfo curseFileInfo) throws CurseForgeException
    {
        try
        {
            return this.parseModFile(this.fetchModLink(curseFileInfo));
        } catch (Exception e)
        {
            throw new CurseForgeException(String.format("Failed to fetch mod project id: %d file id: %d", curseFileInfo.getProjectID(), curseFileInfo.getFileID()), e);
        }
    }

    public static class CurseForgeException extends Exception
    {
        public CurseForgeException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }

    public String fetchModLink(@NotNull CurseFileInfo curseFileInfo)
    {
        final String url = CF_API_URL + MOD_FILE_ENDPOINT
                .replace("{modId}", String.valueOf(curseFileInfo.getProjectID()))
                .replace("{fileId}", String.valueOf(curseFileInfo.getFileID()));

        return this.makeRequest(url);
    }

    /**
     * Make a request to the CurseForge API.
     * Oh my god, fuck Java 8 HTTP API, it's so fucking bad. Hope we drop Java 8 as soon as possible.
     *
     * @param url the url to request.
     * @return the response of the request.
     */
    private @NotNull String makeRequest(String url)
    {
        HttpURLConnection connection = null;
        try
        {
            connection = (HttpURLConnection)new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("x-api-key", this.getCurseForgeAPIKey());

            return IOUtils.getContent(connection.getInputStream());
        }
        catch (Exception e)
        {
            return "";
        }
        finally
        {
            if(connection != null)
                connection.disconnect();
        }
    }

    /**
     * Parse the CurseForge API to retrieve the mod file.
     */
    private @NotNull Mod parseModFile(String jsonResponse)
    {
        final JsonObject data = JsonParser.parseString(jsonResponse).getAsJsonObject().getAsJsonObject("data");
        final String fileName = data.get("fileName").getAsString();
        final JsonElement downloadURLElement = data.get("downloadUrl");
        String downloadURL;

        if(downloadURLElement instanceof JsonNull)
        {
            logger.warn(String.format("Mod file %s not available. The download can fail because of this! %s", data.get("displayName").getAsString(), jsonResponse));
            final String id = Integer.toString(data.get("id").getAsInt());
            downloadURL = String.format("https://mediafiles.forgecdn.net/files/%s/%s/%s", id.substring(0, 4), id.substring(4), fileName);
        }
        else downloadURL = downloadURLElement.getAsString();
        final long fileLength = data.get("fileLength").getAsLong();

        final AtomicReference<String> sha1 = new AtomicReference<>("");

        data.getAsJsonArray("hashes").forEach(hashObject -> {
            final String hash = hashObject.getAsJsonObject().get("value").getAsString();
            if(hash.length() == 40)
                sha1.set(hash);
        });

        return new Mod(fileName, downloadURL, sha1.get(), fileLength);
    }

    /**
     * Get a CurseForge's mod pack object with a project identifier and a file identifier.
     * @param info CurseForge's mod pack info.
     * @return the curse's mod pack corresponding to given parameters.
     */
    public CurseModPack getCurseModPack(CurseModPackInfo info) throws Exception
    {
        final Path modPackFile = this.checkForUpdate(info);
        this.extractModPack(modPackFile, info.isInstallExtFiles());
        return this.parseMods();
    }

    private @NotNull Path checkForUpdate(@NotNull CurseModPackInfo info) throws Exception
    {
        final String responseData = info.getUrl().isEmpty() ? this.fetchModLink(info) : this.makeRequest(info.getUrl());
        final Mod modPackFile = this.parseModFile(responseData);

        final Path outPath = this.folder.resolve(modPackFile.getName());
        if(Files.notExists(outPath) || (!modPackFile.getSha1().isEmpty() && !FileUtils.getSHA1(outPath).equalsIgnoreCase(modPackFile.getSha1())) || Files.size(outPath) != modPackFile.getSize())
            IOUtils.download(this.logger, new URL(modPackFile.getDownloadURL()), outPath);

        return outPath;
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
                {
                    this.changed = true;
                    this.transferAndClose(flPath, zipFile, entry);
                }
                continue;
            }

            if(entryName.equals("modlist.html"))
                continue;

            if(!installExtFiles || Files.exists(flPath)) continue;

            if (flPath.getFileName().toString().endsWith(flPath.getFileSystem().getSeparator()))
                Files.createDirectories(flPath);

            if (entry.isDirectory()) continue;

            this.transferAndClose(flPath, zipFile, entry);
        }
        zipFile.close();
    }

    private @NotNull CurseModPack parseMods() throws Exception
    {
        this.logger.info("Fetching mods...");

        final Path dirPath = this.folder.getParent();
        final String manifestJson = StringUtils.toString(Files.readAllLines(dirPath.resolve("manifest.json")));
        final JsonObject manifestObj = JsonParser.parseString(manifestJson).getAsJsonObject();
        final String modPackName = manifestObj.get("name").getAsString();
        final String modPackVersion = manifestObj.get("version").getAsString();
        final String modPackAuthor = manifestObj.get("author").getAsString();
        final List<CurseModPack.CurseModPackMod> mods = this.processCacheFile(dirPath, this.populateManifest(manifestObj));

        return new CurseModPack(modPackName, modPackVersion, modPackAuthor, mods);
    }

    private @NotNull List<ProjectMod> populateManifest(@NotNull JsonObject manifestObj)
    {
        final List<ProjectMod> manifestFiles = new ArrayList<>();

        manifestObj.getAsJsonArray("files")
                .forEach(jsonElement -> manifestFiles.add(ProjectMod.fromJson(jsonElement.getAsJsonObject())));

        return manifestFiles;
    }

    private @NotNull List<CurseModPack.CurseModPackMod> processCacheFile(@NotNull Path dirPath, List<ProjectMod> manifestFiles) throws Exception
    {
        final Path cachePath = dirPath.resolve("manifest.cache.json");

        if(Files.notExists(cachePath))
        {
            Files.createFile(cachePath);
            Files.write(cachePath, Collections.singletonList("[]"), StandardCharsets.UTF_8);
        }

        String json = StringUtils.toString(Files.readAllLines(cachePath, StandardCharsets.UTF_8));

        if(this.changed || json.contains("\"md5\"") || json.contains("\"length\""))
        {
            Files.delete(cachePath);
            Files.createFile(cachePath);
            Files.write(cachePath, Collections.singletonList("[]"), StandardCharsets.UTF_8);
            json = StringUtils.toString(Files.readAllLines(cachePath, StandardCharsets.UTF_8));
        }

        return this.deserializeWriteCache(json, manifestFiles, cachePath);
    }

    @Contract("_, _, _ -> new")
    private @NotNull List<CurseModPack.CurseModPackMod> deserializeWriteCache(String json,
            List<ProjectMod> manifestFiles, Path cachePath) throws Exception
    {
        final JsonArray cacheArray = JsonParser.parseString(json).getAsJsonArray();
        final Queue<CurseModPack.CurseModPackMod> mods = new ConcurrentLinkedQueue<>();

        cacheArray.forEach(jsonElement -> {
            final JsonObject object = jsonElement.getAsJsonObject();
            final Mod mod = Mod.fromJson(jsonElement);
            final ProjectMod projectMod = ProjectMod.fromJson(object);

            mods.add(new CurseModPack.CurseModPackMod(mod, projectMod.isRequired()));
            manifestFiles.remove(projectMod);
        });

        IOUtils.executeAsyncForEach(manifestFiles, Executors.newWorkStealingPool(), projectMod -> this.fetchAndSerializeProjectMod(projectMod, cacheArray, mods));
        Files.write(cachePath, Collections.singletonList(cacheArray.toString()), StandardCharsets.UTF_8);

        return new ArrayList<>(mods);
    }

    private void fetchAndSerializeProjectMod(@NotNull ProjectMod projectMod, JsonArray cacheArray,
            Queue<CurseModPack.CurseModPackMod> mods)
    {
        final boolean required = projectMod.isRequired();

        try
        {
            final Mod retrievedMod = this.fetchMod(projectMod);

            if(retrievedMod == null)
                return;

            final CurseModPack.CurseModPackMod mod = new CurseModPack.CurseModPackMod(retrievedMod, required);
            final JsonObject inCache = new JsonObject();

            inCache.addProperty("name", mod.getName());
            inCache.addProperty("downloadURL", mod.getDownloadURL());
            inCache.addProperty("sha1", mod.getSha1());
            inCache.addProperty("size", mod.getSize());
            inCache.addProperty("required", required);
            inCache.addProperty("projectID", projectMod.getProjectID());
            inCache.addProperty("fileID", projectMod.getFileID());

            cacheArray.add(inCache);
            mods.add(mod);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void transferAndClose(@NotNull Path flPath, ZipFile zipFile, ZipEntry entry) throws Exception
    {
        if(Files.notExists(flPath.getParent()))
            Files.createDirectories(flPath.getParent());

        try(OutputStream pathStream = Files.newOutputStream(flPath);
            BufferedOutputStream fo = new BufferedOutputStream(pathStream);
            InputStream is = zipFile.getInputStream(entry)) {

            while (is.available() > 0)
                fo.write(is.read());
        }
    }

    private static class ProjectMod extends CurseFileInfo
    {
        private final boolean required;

        public ProjectMod(int projectID, int fileID, boolean required)
        {
            super(projectID, fileID);
            this.required = required;
        }

        private static @NotNull ProjectMod fromJson(@NotNull JsonObject object)
        {
            return new ProjectMod(object.get("projectID").getAsInt(),
                                  object.get("fileID").getAsInt(),
                                  object.get("required").getAsBoolean());
        }

        public boolean isRequired()
        {
            return this.required;
        }
    }

    /**
     * Get the CurseForge API Key.
     */
    private static String cacheKey = "";

    private String getCurseForgeAPIKey()
    {
        return cacheKey.isEmpty() ? cacheKey = StringUtils.toString(Base64.getDecoder().decode(CF_API_KEY.substring(0, CF_API_KEY.length() - 5))) : cacheKey;
    }
}
