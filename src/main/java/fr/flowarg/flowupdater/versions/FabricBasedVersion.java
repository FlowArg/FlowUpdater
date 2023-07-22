package fr.flowarg.flowupdater.versions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.flowarg.flowstringer.StringUtils;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.json.*;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public abstract class FabricBasedVersion extends AbstractModLoaderVersion
{
    protected final String installerVersion;
    protected URL installerUrl;

    private final String baseInstallerUrl;

    public FabricBasedVersion(List<Mod> mods, String modLoaderVersion, List<CurseFileInfo> curseMods,
            List<ModrinthVersionInfo> modrinthMods, ModFileDeleter fileDeleter, CurseModPackInfo curseModPackInfo,
            ModrinthModPackInfo modrinthModPackInfo, String installerVersion, String baseInstallerUrl)
    {
        super(mods, modLoaderVersion, curseMods, modrinthMods, fileDeleter, curseModPackInfo, modrinthModPackInfo);
        this.installerVersion = installerVersion;
        this.baseInstallerUrl = baseInstallerUrl;
    }

    protected void parseAndMoveJson(@NotNull Path dirToInstall, @NotNull Path versionDir) throws Exception
    {
        final Path jsonFilePath = versionDir.resolve(versionDir.getFileName().toString() + ".json");

        final JsonObject obj = JsonParser.parseString(
                        StringUtils.toString(Files.readAllLines(jsonFilePath, StandardCharsets.UTF_8)))
                .getAsJsonObject();

        final JsonArray libraryArray = obj.getAsJsonArray("libraries");
        final Path libraries = dirToInstall.resolve("libraries");

        libraryArray.forEach(el -> {
            final JsonObject artifact = el.getAsJsonObject();
            final String[] parts = artifact.get("name").getAsString().split(":");
            IOUtils.downloadArtifacts(this.logger, libraries, artifact.get("url").getAsString(), parts);
        });

        final Path newJsonFilePath = dirToInstall.resolve(jsonFilePath.getFileName());

        if(Files.notExists(newJsonFilePath))
            Files.move(jsonFilePath, newJsonFilePath);
        else if(Files.size(newJsonFilePath) != Files.size(jsonFilePath))
        {
            Files.delete(newJsonFilePath);
            Files.move(jsonFilePath, newJsonFilePath);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void attachFlowUpdater(@NotNull FlowUpdater flowUpdater)
    {
        super.attachFlowUpdater(flowUpdater);
        try
        {
            this.installerUrl = new URL(String.format(this.baseInstallerUrl, this.installerVersion, this.installerVersion));
        }
        catch (Exception e)
        {
            this.logger.printStackTrace(e);
        }
    }

    protected class FabricBasedLauncherEnvironment extends ModLoaderLauncherEnvironment
    {
        private final Path modLoaderDir;

        public FabricBasedLauncherEnvironment(List<String> command, Path tempDir, Path modLoaderDir)
        {
            super(command, tempDir);
            this.modLoaderDir = modLoaderDir;
        }

        public Path getModLoaderDir()
        {
            return this.modLoaderDir;
        }

        public void launchInstaller() throws Exception
        {
            final ProcessBuilder processBuilder = new ProcessBuilder(this.getCommand());

            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            final Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) FabricBasedVersion.this.logger.info(line);

            reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = reader.readLine()) != null) FabricBasedVersion.this.logger.info(line);

            process.waitFor();

            reader.close();
        }
    }
}
