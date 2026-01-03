package fr.flowarg.flowupdater.versions.neoforge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.flowarg.flowupdater.download.json.*;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import fr.flowarg.flowupdater.utils.Version;
import fr.flowarg.flowupdater.versions.AbstractModLoaderVersion;
import fr.flowarg.flowupdater.versions.ModLoaderUtils;
import fr.flowarg.flowupdater.versions.ParsedLibrary;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class NeoForgeVersion extends AbstractModLoaderVersion
{
    private final boolean isOldNeoForge; // 1.20.1 neo forge versions only are "old"
    private final String versionId;

    NeoForgeVersion(String modLoaderVersion, List<Mod> mods, List<CurseFileInfo> curseMods,
            List<ModrinthVersionInfo> modrinthMods, ModFileDeleter fileDeleter,
            CurseModPackInfo curseModPackInfo, ModrinthModPackInfo modrinthModPackInfo, OptiFineInfo optiFineInfo)
    {
        super(modLoaderVersion, mods, curseMods, modrinthMods, fileDeleter, curseModPackInfo, modrinthModPackInfo, optiFineInfo);
        this.isOldNeoForge = this.modLoaderVersion.startsWith("1.");

        if(this.isOldNeoForge)
        {
            final String[] oldNeoForgeVersionData = this.modLoaderVersion.split("-");
            final String vanillaVersion = oldNeoForgeVersionData[0];
            final String oldNeoForgeVersion = oldNeoForgeVersionData[1];

            this.versionId = String.format("%s-forge-%s", vanillaVersion, oldNeoForgeVersion);
        }
        else this.versionId = String.format("neoforge-%s", this.modLoaderVersion);
    }

    @Override
    public boolean isModLoaderAlreadyInstalled(@NotNull Path installDir)
    {
        final Path versionJsonFile = installDir.resolve(this.versionId + ".json");

        if(Files.notExists(versionJsonFile))
            return false;

        try {
            final JsonObject object = JsonParser.parseReader(Files.newBufferedReader(versionJsonFile))
                    .getAsJsonObject();

            final boolean firstPass = ModLoaderUtils.parseNewVersionInfo(installDir, object).stream().allMatch(ParsedLibrary::isInstalled);

            if(!firstPass)
                return false;
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
        final Path minecraftClientPatchedJar = installDir.resolve("libraries")
                .resolve("net")
                .resolve("neoforged")
                .resolve("minecraft-client-patched")
                .resolve(this.modLoaderVersion)
                .resolve("minecraft-client-patched-" + this.modLoaderVersion + ".jar"); // starting from 21.10.37-beta
        final Path clientNeoForgeJar = neoForgeDirectory.resolve(this.versionId + "-client.jar");

        final Version modLoaderVer = Version.gen(this.modLoaderVersion.split("-")[0]); // skip -beta/alpha etc strings

        return Files.exists(universalNeoForgeJar) && (
                Files.exists(
                        modLoaderVer.isNewerOrEqualTo(Version.gen("21.10.37")) ? minecraftClientPatchedJar : clientNeoForgeJar
                )
        );
    }

    @Override
    public void install(@NotNull Path installDir) throws Exception
    {
        super.install(installDir);

        final String installerUrl = String.format(
                "https://maven.neoforged.net/net/neoforged/%s/%s/%s-installer.jar",
                this.isOldNeoForge ? "forge" : "neoforge",
                this.modLoaderVersion,
                this.isOldNeoForge ? "forge-" + this.modLoaderVersion : this.versionId
        );

        ModLoaderUtils.fakeContext(installDir, this.vanilla.getName());

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
        ModLoaderUtils.removeFakeContext(installDir);
    }

    @Override
    public String name()
    {
        return "NeoForge";
    }
}
