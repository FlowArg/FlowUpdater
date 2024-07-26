package fr.flowarg.flowupdater.versions.forge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowupdater.download.Step;
import fr.flowarg.flowupdater.download.json.*;
import fr.flowarg.flowupdater.integrations.optifineintegration.IOptiFineCompatible;
import fr.flowarg.flowupdater.integrations.optifineintegration.OptiFine;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import fr.flowarg.flowupdater.utils.Version;
import fr.flowarg.flowupdater.versions.AbstractModLoaderVersion;
import fr.flowarg.flowupdater.versions.ParsedLibrary;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

public class ForgeVersion extends AbstractModLoaderVersion implements IOptiFineCompatible
{
    private final OptiFineInfo optiFineInfo;
    private final String versionId;
    private final boolean shouldUseInstaller;
    private final boolean newInstallerJsonSpec;

    public ForgeVersion(String modLoaderVersion, List<Mod> mods, List<CurseFileInfo> curseMods,
            List<ModrinthVersionInfo> modrinthMods, ModFileDeleter fileDeleter, CurseModPackInfo curseModPackInfo,
            ModrinthModPackInfo modrinthModPackInfo,  OptiFineInfo optiFineInfo)
    {
        super(modLoaderVersion, mods, curseMods, modrinthMods, fileDeleter, curseModPackInfo, modrinthModPackInfo);
        this.optiFineInfo = optiFineInfo;

        final String[] data = this.modLoaderVersion.split("-");
        final String vanilla = data[0];
        final String forge = data[1];
        final Version vanillaVersion = Version.gen(vanilla);
        final Version forgeVersion = Version.gen(forge);

        if (data.length == 2)
        {
            if(forgeVersion.isNewerOrEqualTo(Version.gen("14.23.5.2851")))
            {
                this.versionId = vanilla + "-forge-" + forge;
                this.shouldUseInstaller = vanillaVersion.isNewerThan(Version.gen("1.12.2"));
                this.newInstallerJsonSpec = true;
            }
            else
            {
                this.versionId = vanilla + "-forge" + this.modLoaderVersion;
                this.shouldUseInstaller = false;
                this.newInstallerJsonSpec = false;
            }
        }
        else
        {
            if(vanillaVersion.isOlderOrEqualTo(Version.gen("1.7.10")))
                this.versionId = vanilla + "-Forge" + forge + "-" + data[2];
            else this.versionId = vanilla + "-forge" + this.modLoaderVersion;
            this.shouldUseInstaller = false;
            this.newInstallerJsonSpec = false;
        }
    }

    @Override
    public boolean isModLoaderAlreadyInstalled(Path installDir)
    {
        final Path versionJsonFile = installDir.resolve(this.versionId + ".json");

        if(Files.notExists(versionJsonFile))
            return false;

        try {
            final JsonObject object = JsonParser.parseReader(Files.newBufferedReader(versionJsonFile))
                    .getAsJsonObject();

            if(this.newInstallerJsonSpec)
            {

            }
            else return this.parseVersionInfo(installDir, object).stream().allMatch(ParsedLibrary::isInstalled);
        }
        catch (Exception e)
        {
            this.logger.err("An error occurred while checking if the mod loader is already installed.");
            return false;
        }

        return true;
    }

    protected Path buildLibraryPath(@NotNull Path installDir, @NotNull String group, String artifact, String version)
    {
        return installDir.resolve("libraries")
                .resolve(group.replace(".", installDir.getFileSystem().getSeparator()))
                .resolve(artifact)
                .resolve(version)
                .resolve(artifact + "-" + version + ".jar");
    }

    @Contract(pure = true)
    protected @NotNull String buildJarUrl(String baseUrl, @NotNull String group, String artifact, String version, String classifier)
    {
        return baseUrl + group.replace(".", "/") + "/" + artifact + "/" + version + "/" + artifact + "-" + version + classifier + ".jar";
    }

    protected Callable<String> getSha1FromLibrary(@NotNull JsonObject library, String builtJarUrl)
    {
        final JsonElement checksumsElem = library.get("checksums");
        if (checksumsElem != null)
        {
            final JsonElement checksums = checksumsElem.getAsJsonArray().get(0);

            if(checksums != null)
                return checksums::getAsString;
        }

        return () -> IOUtils.getContent(new URL(builtJarUrl + ".sha1"));
    }

    @Override
    public void install(Path installDir) throws Exception
    {
        super.install(installDir);

        final String installerUrl = String.format("https://maven.minecraftforge.net/net/minecraftforge/forge/%s/forge-%s-installer.jar",
                                                  this.modLoaderVersion, this.modLoaderVersion);
        final String[] installerUrlParts = installerUrl.split("/");
        final Path installerFile = installDir.resolve(installerUrlParts[installerUrlParts.length - 1]);
        IOUtils.download(
                this.logger,
                new URL(installerUrl),
                installerFile
        );

        if(this.newInstallerJsonSpec)
        {

        }
        else
        {
            final URI uri = URI.create("jar:file:" + installerFile.toAbsolutePath());
            try (final FileSystem zipFs = FileSystems.newFileSystem(uri, new HashMap<>()))
            {
                final Path installProfileFile = zipFs.getPath("install_profile.json");
                final JsonObject versionInfo = JsonParser.parseReader(Files.newBufferedReader(installProfileFile)).getAsJsonObject().getAsJsonObject("versionInfo");
                final Path versionJsonFile = installDir.resolve(this.versionId + ".json");
                Files.write(versionJsonFile, versionInfo.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);

                this.parseVersionInfo(installDir, versionInfo)
                        .stream()
                        .filter(parsedLibrary -> !parsedLibrary.isInstalled())
                        .forEach(parsedLibrary -> parsedLibrary.download(this.logger));
            } catch (Exception e)
            {
                this.logger.printStackTrace(e);
            }
        }
    }

    private @NotNull List<ParsedLibrary> parseVersionInfo(Path installDir, @NotNull JsonObject versionInfo) throws Exception
    {
        final List<ParsedLibrary> parsedLibraries = new ArrayList<>();
        final JsonArray libraries = versionInfo.getAsJsonArray("libraries");

        for (final JsonElement libraryElement : libraries)
        {
            final JsonObject library = libraryElement.getAsJsonObject();
            final JsonElement clientreqElem = library.get("clientreq");
            final boolean shouldInstall = clientreqElem == null || clientreqElem.getAsBoolean();

            if(!shouldInstall)
                continue;

            final JsonElement urlElem = library.get("url");
            final String baseUrl = urlElem == null ? "https://libraries.minecraft.net/" : urlElem.getAsString();
            final String[] name = library.get("name").getAsString().split(":");
            final String group = name[0];
            final String artifact = name[1];
            final String version = name[2];
            final String classifier = artifact.equals("forge") ? "-universal" : "";
            final Path libraryPath = this.buildLibraryPath(installDir, group, artifact, version);
            final String builtJarUrl = this.buildJarUrl(baseUrl, group, artifact, version, classifier);
            final Callable<String> sha1 = this.getSha1FromLibrary(library, builtJarUrl);
            final boolean installed = Files.exists(libraryPath) &&
                    FileUtils.getSHA1(libraryPath).equals(sha1.call());

            parsedLibraries.add(new ParsedLibrary(libraryPath, new URL(builtJarUrl), installed));
        }

        return parsedLibraries;
    }

    @Override
    public void installMods(Path modsDir) throws Exception
    {
        this.callback.step(Step.MODS);
        this.installAllMods(modsDir);

        final OptiFine ofObj = this.downloadList.getOptiFine();

        if(ofObj != null)
        {
            try
            {
                final Path optiFineFilePath = modsDir.resolve(ofObj.getName());

                if (Files.notExists(optiFineFilePath) || Files.size(optiFineFilePath) != ofObj.getSize())
                    IOUtils.copy(this.logger, modsDir.getParent().resolve(".op").resolve(ofObj.getName()), optiFineFilePath);
            } catch (Exception e)
            {
                this.logger.printStackTrace(e);
            }
            this.downloadList.incrementDownloaded(ofObj.getSize());
            this.callback.update(this.downloadList.getDownloadInfo());
        }

        this.fileDeleter.delete(this.logger, modsDir, this.mods, ofObj, this.modrinthModPack);
    }

    @Override
    public OptiFineInfo getOptiFineInfo()
    {
        return this.optiFineInfo;
    }

    @Override
    public String name()
    {
        return "Forge";
    }
}
