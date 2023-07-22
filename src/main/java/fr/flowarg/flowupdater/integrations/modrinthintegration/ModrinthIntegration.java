package fr.flowarg.flowupdater.integrations.modrinthintegration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowstringer.StringUtils;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.download.json.ModrinthModPackInfo;
import fr.flowarg.flowupdater.download.json.ModrinthVersionInfo;
import fr.flowarg.flowupdater.integrations.Integration;
import fr.flowarg.flowupdater.utils.FlowUpdaterException;
import fr.flowarg.flowupdater.utils.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ModrinthIntegration extends Integration
{
    private static final String MODRINTH_URL = "https://api.modrinth.com/v2/";
    private static final String MODRINTH_VERSION_ENDPOINT = "version/{versionId}";
    private static final String MODRINTH_PROJECT_VERSION_ENDPOINT = "project/{projectId}/version";

    /**
     * Default constructor of a basic Integration.
     *
     * @param logger the logger used.
     * @param folder the folder where the plugin can work.
     * @throws Exception if an error occurred.
     */
    public ModrinthIntegration(ILogger logger, Path folder) throws Exception
    {
        super(logger, folder);
    }

    public Mod fetchMod(@NotNull ModrinthVersionInfo versionInfo) throws Exception
    {
        if(!versionInfo.getVersionId().isEmpty())
        {
            final URL url = new URL(MODRINTH_URL + MODRINTH_VERSION_ENDPOINT
                    .replace("{versionId}", versionInfo.getVersionId()));

            return this.parseModFile(JsonParser.parseString(IOUtils.getContent(url)).getAsJsonObject());
        }

        final URL url = new URL(MODRINTH_URL + MODRINTH_PROJECT_VERSION_ENDPOINT.replace("{projectId}", versionInfo.getProjectReference()));
        final JsonArray versions = JsonParser.parseString(IOUtils.getContent(url)).getAsJsonArray();
        JsonObject version = null;
        for (JsonElement jsonElement : versions)
        {
            if(!jsonElement.getAsJsonObject().get("version_number").getAsString().equals(versionInfo.getVersionNumber()))
                continue;

            version = jsonElement.getAsJsonObject();
            break;
        }

        if(version == null)
            throw new FlowUpdaterException(
                    "No version found for " + versionInfo.getVersionNumber() +
                            " in project " + versionInfo.getProjectReference());

        return this.parseModFile(version);
    }

    public Mod parseModFile(@NotNull JsonObject version)
    {
        final JsonObject fileJson = version.getAsJsonArray("files").get(0).getAsJsonObject();
        final String fileName = fileJson.get("filename").getAsString();
        final String downloadURL = fileJson.get("url").getAsString();
        final String sha1 = fileJson.getAsJsonObject("hashes").get("sha1").getAsString();
        final long size = fileJson.get("size").getAsLong();

        return new Mod(fileName, downloadURL, sha1, size);
    }

    /**
     * Get a CurseForge's mod pack object with a project identifier and a file identifier.
     * @param info CurseForge's mod pack info.
     * @return the curse's mod pack corresponding to given parameters.
     */
    public ModrinthModPack getCurseModPack(ModrinthModPackInfo info) throws Exception
    {
        final Path modPackFile = this.checkForUpdate(info);
        if(modPackFile == null)
            throw new FlowUpdaterException("Can't find the mod pack file with the provided Modrinth mod pack info.");
        this.extractModPack(modPackFile, info.isInstallExtFiles());
        return this.parseMods();
    }

    private @Nullable Path checkForUpdate(@NotNull ModrinthModPackInfo info) throws Exception
    {
        final Mod modPackFile = this.fetchMod(info);

        if(modPackFile == null)
        {
            this.logger.err("This mod pack isn't available anymore on Modrinth (for 3rd parties maybe). ");
            return null;
        }

        final Path outPath = this.folder.resolve(modPackFile.getName());

        if(Files.notExists(outPath) || !FileUtils.getSHA1(outPath).equalsIgnoreCase(modPackFile.getSha1()))
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
            final Path flPath = dirPath.resolve(StringUtils.empty(StringUtils.empty(entry.getName(), "client-overrides/"), "overrides/"));
            final String entryName = entry.getName();

            if(entryName.equalsIgnoreCase("modrinth.index.json"))
            {
                if(Files.notExists(flPath) || entry.getCrc() != FileUtils.getCRC32(flPath))
                    this.transferAndClose(flPath, zipFile, entry);
                continue;
            }

            if(!installExtFiles || Files.exists(flPath)) continue;

            if (flPath.getFileName().toString().endsWith(flPath.getFileSystem().getSeparator()))
                Files.createDirectories(flPath);

            if (entry.isDirectory()) continue;

            this.transferAndClose(flPath, zipFile, entry);
        }
        zipFile.close();
    }

    private @NotNull ModrinthModPack parseMods() throws Exception
    {
        this.logger.info("Fetching mods...");

        final Path dirPath = this.folder.getParent();
        final String manifestJson = StringUtils.toString(Files.readAllLines(dirPath.resolve("modrinth.index.json")));
        final JsonObject manifestObj = JsonParser.parseString(manifestJson).getAsJsonObject();
        final String modPackName = manifestObj.get("name").getAsString();
        final String modPackVersion = manifestObj.get("versionId").getAsString();
        final List<Mod> mods = this.parseManifest(manifestObj);

        return new ModrinthModPack(modPackName, modPackVersion, mods);
    }

    private @NotNull List<Mod> parseManifest(@NotNull JsonObject manifestObject)
    {
        final List<Mod> mods = new ArrayList<>();

        final JsonArray files = manifestObject.getAsJsonArray("files");

        files.forEach(jsonElement -> {
            final JsonObject file = jsonElement.getAsJsonObject();

            if(file.getAsJsonObject("env").get("client").getAsString().equals("unsupported"))
                return;

            final String name = StringUtils.empty(file.get("path").getAsString(), "mods/");
            final String downloadURL = file.getAsJsonArray("downloads").get(0).getAsString();
            final String sha1 = file.getAsJsonObject("hashes").get("sha1").getAsString();
            final long size = file.get("fileSize").getAsLong();

            mods.add(new Mod(name, downloadURL, sha1, size));
        });

        return mods;
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
}
