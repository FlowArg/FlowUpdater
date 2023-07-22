package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.json.*;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class NeoForgeVersion extends NewForgeVersion
{
    NeoForgeVersion(String forgeVersion, List<Mod> mods,
            List<CurseFileInfo> curseMods, List<ModrinthVersionInfo> modrinthMods, ModFileDeleter fileDeleter,
            OptiFineInfo optiFine, CurseModPackInfo modPack, ModrinthModPackInfo modrinthModPackInfo)
    {
        super(forgeVersion, mods, curseMods, modrinthMods, fileDeleter, optiFine, modPack, modrinthModPackInfo, ForgeVersionType.NEO_FORGE);
        this.compatibleVersions = new String[]{"1.20"};
    }

    @Override
    public void attachFlowUpdater(@NotNull FlowUpdater flowUpdater)
    {
        super.attachFlowUpdater(flowUpdater);
        try
        {
            this.installerUrl = new URL(
                    String.format("https://maven.neoforged.net/net/neoforged/forge/%s/forge-%s-installer.jar",
                                  this.modLoaderVersion, this.modLoaderVersion));
        } catch (Exception e)
        {
            this.logger.printStackTrace(e);
        }
    }

    @Override
    protected void cleanInstaller(@NotNull Path tempInstallerDir) throws Exception
    {
        super.cleanInstaller(tempInstallerDir);
        Files.deleteIfExists(tempInstallerDir.resolve("META-INF").resolve("NEOFORGE.DSA"));
        Files.deleteIfExists(tempInstallerDir.resolve("META-INF").resolve("NEOFORGE.SF"));
    }
}
