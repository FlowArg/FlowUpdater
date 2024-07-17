package fr.flowarg.flowupdater.versions.fabric;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.flowarg.flowstringer.StringUtils;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.json.*;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import fr.flowarg.flowupdater.versions.AbstractModLoaderVersion;
import org.jetbrains.annotations.NotNull;

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

    public FabricBasedVersion(String modLoaderVersion, List<Mod> mods, List<CurseFileInfo> curseMods,
            List<ModrinthVersionInfo> modrinthMods, ModFileDeleter fileDeleter, CurseModPackInfo curseModPackInfo,
            ModrinthModPackInfo modrinthModPackInfo, String installerVersion, String baseInstallerUrl)
    {
        super(modLoaderVersion, mods, curseMods, modrinthMods, fileDeleter, curseModPackInfo, modrinthModPackInfo);
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
}
