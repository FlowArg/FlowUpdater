package fr.flowarg.flowupdater.versions.forge;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowupdater.download.json.*;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Represent an old Forge version (1.7 to 1.12.2) (No support for versions older than 1.7)
 * @author FlowArg
 */
public class OldForgeVersion extends AbstractForgeVersion
{
    OldForgeVersion(String forgeVersion, List<Mod> mods,
            List<CurseFileInfo> curseMods, List<ModrinthVersionInfo> modrinthMods, ModFileDeleter fileDeleter,
            CurseModPackInfo modPack, ModrinthModPackInfo modrinthModPackInfo, OptiFineInfo optiFine)
    {
        super(forgeVersion, mods, curseMods, modrinthMods, fileDeleter, modPack, modrinthModPackInfo, optiFine, ForgeVersionType.OLD);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void cleanInstaller(@NotNull Path tempInstallerDir) throws Exception
    {
        FileUtils.deleteDirectory(tempInstallerDir.resolve("net"));
        FileUtils.deleteDirectory(tempInstallerDir.resolve("com"));
        FileUtils.deleteDirectory(tempInstallerDir.resolve("joptsimple"));
        FileUtils.deleteDirectory(tempInstallerDir.resolve("META-INF"));
        Files.deleteIfExists(tempInstallerDir.resolve("big_logo.png"));
        Files.deleteIfExists(tempInstallerDir.resolve("url.png"));
    }
}
