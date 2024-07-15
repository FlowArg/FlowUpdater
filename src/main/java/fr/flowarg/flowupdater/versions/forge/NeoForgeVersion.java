package fr.flowarg.flowupdater.versions.forge;

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
        this.compatibleVersions = new String[]{"20.", "1.20.1"};
    }

    @Override
    protected boolean isCompatible()
    {
        for (String str : this.compatibleVersions)
        {
            if (this.modLoaderVersion.startsWith(str))
                return true;
        }

        return Integer.parseInt(this.modLoaderVersion.split("\\.")[0]) >=
                Integer.parseInt(this.compatibleVersions[0].substring(0, 2));
    }

    @Override
    public void attachFlowUpdater(@NotNull FlowUpdater flowUpdater)
    {
        this.logger = flowUpdater.getLogger();
        this.vanilla = flowUpdater.getVanillaVersion();
        this.downloadList = flowUpdater.getDownloadList();
        this.callback = flowUpdater.getCallback();
        this.javaPath = flowUpdater.getUpdaterOptions().getJavaPath();
        try
        {
            String forge = this.modLoaderVersion.startsWith("1.") ? "forge" : "neoforge";
            this.installerUrl = new URL(
                    String.format("https://maven.neoforged.net/net/neoforged/%s/%s/%s-%s-installer.jar",
                                  forge, this.modLoaderVersion, forge, this.modLoaderVersion));
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

    @Override
    public String name()
    {
        return "NeoForge";
    }
}
