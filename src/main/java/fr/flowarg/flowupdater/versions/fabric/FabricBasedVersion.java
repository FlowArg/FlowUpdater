package fr.flowarg.flowupdater.versions.fabric;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowupdater.download.json.*;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import fr.flowarg.flowupdater.versions.AbstractModLoaderVersion;
import fr.flowarg.flowupdater.versions.ModLoaderUtils;
import fr.flowarg.flowupdater.versions.ParsedLibrary;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public abstract class FabricBasedVersion extends AbstractModLoaderVersion
{
    protected final String metaApi;
    protected String versionId;

    public FabricBasedVersion(String modLoaderVersion, List<Mod> mods, List<CurseFileInfo> curseMods,
            List<ModrinthVersionInfo> modrinthMods, ModFileDeleter fileDeleter, CurseModPackInfo curseModPackInfo,
            ModrinthModPackInfo modrinthModPackInfo, OptiFineInfo optiFineInfo, String metaApi)
    {
        super(modLoaderVersion, mods, curseMods, modrinthMods, fileDeleter, curseModPackInfo, modrinthModPackInfo, optiFineInfo);
        this.metaApi = metaApi;
    }

    @Override
    public boolean isModLoaderAlreadyInstalled(@NotNull Path installDir)
    {
        final Path versionJsonFile = installDir.resolve(this.versionId + ".json");

        if(Files.notExists(versionJsonFile))
            return false;

        try {
            return this.parseLibraries(versionJsonFile, installDir).stream().allMatch(ParsedLibrary::isInstalled);
        }
        catch (Exception e)
        {
            this.logger.err("An error occurred while checking if the mod loader is already installed.");
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void install(final @NotNull Path installDir) throws Exception
    {
        super.install(installDir);

        final Path versionJsonFile = installDir.resolve(this.versionId + ".json");

        IOUtils.download(this.logger, new URL(String.format(this.metaApi, this.vanilla.getName(), this.modLoaderVersion)), versionJsonFile);

        try {
            final List<ParsedLibrary> parsedLibraries = this.parseLibraries(versionJsonFile, installDir);
            parsedLibraries.stream()
                    .filter(parsedLibrary -> !parsedLibrary.isInstalled())
                    .forEach(parsedLibrary -> parsedLibrary.download(this.logger));
        }
        catch (Exception e)
        {
            this.logger.err("An error occurred while installing the mod loader.");
        }
    }

    protected List<ParsedLibrary> parseLibraries(Path versionJsonFile, Path installDir) throws Exception
    {
        final List<ParsedLibrary> parsedLibraries = new ArrayList<>();
        final JsonObject object = JsonParser.parseReader(Files.newBufferedReader(versionJsonFile))
                .getAsJsonObject();
        final JsonArray libraries = object.getAsJsonArray("libraries");

        for (final JsonElement libraryElement : libraries)
        {
            final JsonObject library = libraryElement.getAsJsonObject();
            final String url = library.get("url").getAsString();
            final String completeArtifact = library.get("name").getAsString();
            final String[] name = completeArtifact.split(":");
            final String group = name[0];
            final String artifact = name[1];
            final String version = name[2];

            final String builtJarUrl = ModLoaderUtils.buildJarUrl(url, group, artifact, version);
            final Path builtLibaryPath = ModLoaderUtils.buildLibraryPath(installDir, group, artifact, version);
            final Callable<String> sha1 = this.getSha1FromLibrary(library, builtJarUrl);
            final boolean installed = Files.exists(builtLibaryPath) &&
                    FileUtils.getSHA1(builtLibaryPath).equalsIgnoreCase(sha1.call());

            parsedLibraries.add(new ParsedLibrary(builtLibaryPath, new URL(builtJarUrl), completeArtifact, installed));
        }
        return parsedLibraries;
    }

    protected Callable<String> getSha1FromLibrary(@NotNull JsonObject library, String builtJarUrl)
    {
        final JsonElement sha1Elem = library.get("sha1");
        if (sha1Elem != null)
            return sha1Elem::getAsString;

        return () -> IOUtils.getContent(new URL(builtJarUrl + ".sha1"));
    }
}
