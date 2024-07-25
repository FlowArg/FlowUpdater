package fr.flowarg.flowupdater.versions.neoforge;

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
import fr.flowarg.flowupdater.versions.AbstractModLoaderVersion;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class NeoForgeVersion extends AbstractModLoaderVersion implements IOptiFineCompatible
{
    private final boolean isOldNeoForge; // 1.20.1 neo forge versions only are "old"
    private final String versionId;
    private final OptiFineInfo optiFineInfo;

    NeoForgeVersion(String modLoaderVersion, List<Mod> mods, List<CurseFileInfo> curseMods,
            List<ModrinthVersionInfo> modrinthMods, ModFileDeleter fileDeleter,
            CurseModPackInfo curseModPackInfo, ModrinthModPackInfo modrinthModPackInfo, OptiFineInfo optiFineInfo)
    {
        super(modLoaderVersion, mods, curseMods, modrinthMods, fileDeleter, curseModPackInfo, modrinthModPackInfo);
        this.isOldNeoForge = this.modLoaderVersion.startsWith("1.");

        if(this.isOldNeoForge)
        {
            final String[] oldNeoForgeVersionData = this.modLoaderVersion.split("-");
            final String vanillaVersion = oldNeoForgeVersionData[0];
            final String oldNeoForgeVersion = oldNeoForgeVersionData[1];

            this.versionId = String.format("%s-forge-%s", vanillaVersion, oldNeoForgeVersion);
        }
        else this.versionId = String.format("neoforge-%s", this.modLoaderVersion);
        this.optiFineInfo = optiFineInfo;
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
            final JsonArray libraries = object.getAsJsonArray("libraries");

            for (final JsonElement libraryElement : libraries)
            {
                final JsonObject library = libraryElement.getAsJsonObject();
                final JsonObject downloads = library.getAsJsonObject("downloads");
                final JsonObject artifact = downloads.getAsJsonObject("artifact");

                final String path = artifact.get("path").getAsString();
                final String sha1 = artifact.get("sha1").getAsString();
                final long size = artifact.get("size").getAsLong();

                final Path libraryPath = installDir.resolve("libraries").resolve(path);

                if(Files.notExists(libraryPath))
                    return false;

                if(Files.size(libraryPath) != size)
                    return false;

                if(!FileUtils.getSHA1(libraryPath).equalsIgnoreCase(sha1))
                    return false;
            }
        }
        catch (Exception e)
        {
            this.logger.warn("An error occurred while checking if the mod loader is already installed.");
            return false;
        }

        final Path neoForgeDirectory = installDir.resolve("libraries")
                .resolve("net")
                .resolve("neoforged")
                .resolve(this.isOldNeoForge ? "forge" : "neoforge")
                .resolve(this.modLoaderVersion);

        final Path universalNeoForgeJar = neoForgeDirectory.resolve(this.versionId + "-universal.jar");
        final Path clientNeoForgeJar = neoForgeDirectory.resolve(this.versionId + "-client.jar");

        return Files.exists(universalNeoForgeJar) && Files.exists(clientNeoForgeJar);
    }

    @Override
    public void install(Path installDir) throws Exception
    {
        super.install(installDir);

        final String installerUrl = String.format("https://maven.neoforged.net/net/neoforged/%s/%s/%s-installer.jar",
                this.isOldNeoForge ? "forge" : "neoforge", this.modLoaderVersion, this.versionId);

        this.fakeContext(installDir);

        final String[] installerUrlParts = installerUrl.split("/");
        final Path installerFile = installDir.resolve(installerUrlParts[installerUrlParts.length - 1]);

        IOUtils.download(this.logger, new URL(installerUrl), installerFile);

        final List<String> command = new ArrayList<>();
        command.add(this.javaPath);
        command.add("-jar");
        command.add(installerFile.toAbsolutePath().toString());
        command.add("--installClient");
        command.add(installDir.toAbsolutePath().toString());

        final ProcessBuilder processBuilder = new ProcessBuilder(command);

        processBuilder.directory(installDir.toFile());
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        final Process process = processBuilder.start();
        process.waitFor();

        Files.copy(
                installDir.resolve("versions")
                        .resolve(this.versionId)
                        .resolve(this.versionId + ".json"),
                installDir.resolve(this.versionId + ".json"),
                StandardCopyOption.REPLACE_EXISTING
        );

        Files.deleteIfExists(installerFile);
        this.removeFakeContext(installDir);
    }

    private void fakeContext(@NotNull Path dirToInstall) throws Exception
    {
        final Path fakeProfiles = dirToInstall.resolve("launcher_profiles.json");

        Files.write(fakeProfiles, "{}".getBytes(StandardCharsets.UTF_8));

        final Path versions = dirToInstall.resolve("versions");
        if(Files.notExists(versions))
            Files.createDirectories(versions);

        final Path vanillaVersion = versions.resolve(this.vanilla.getName());
        if(Files.notExists(vanillaVersion))
            Files.createDirectories(vanillaVersion);

        Files.copy(
                dirToInstall.resolve("client.jar"),
                vanillaVersion.resolve(this.vanilla.getName() + ".jar"),
                StandardCopyOption.REPLACE_EXISTING
        );
    }

    private void removeFakeContext(@NotNull Path dirToInstall) throws Exception
    {
        FileUtils.deleteDirectory(dirToInstall.resolve("versions"));
        Files.deleteIfExists(dirToInstall.resolve("launcher_profiles.json"));
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
    public String name()
    {
        return "NeoForge";
    }

    @Override
    public OptiFineInfo getOptiFineInfo()
    {
        return this.optiFineInfo;
    }
}
