package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowupdater.download.json.*;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Represent a new Forge version (1.12.2-14.23.5.2851 to 1.19)
 * @author FlowArg
 */
public class NewForgeVersion extends AbstractForgeVersion
{
    protected String[] compatibleVersions = {"1.20", "1.19", "1.18", "1.17",
            "1.16", "1.15", "1.14",
            "1.13", "1.12.2-14.23.5.285", "1.12.2-14.23.5.286"};

    NewForgeVersion(String forgeVersion, List<Mod> mods,
            List<CurseFileInfo> curseMods, List<ModrinthVersionInfo> modrinthMods, ModFileDeleter fileDeleter,
            OptiFineInfo optiFine, CurseModPackInfo modPack, ModrinthModPackInfo modrinthModPackInfo, ForgeVersionType forgeVersionType)
    {
        super(mods, curseMods, modrinthMods, forgeVersion, fileDeleter, optiFine, modPack, modrinthModPackInfo, forgeVersionType);
    }

    @Override
    protected boolean isCompatible()
    {
        for (String str : this.compatibleVersions)
        {
            if (this.modLoaderVersion.startsWith(str))
                return true;
        }

        // with this line, 1.21, 1.22 etc... will be marked as compatible without adding them to the array
        return Integer.parseInt(this.modLoaderVersion.split("\\.")[1]) >=
                Integer.parseInt(this.compatibleVersions[0].split("\\.")[1]);
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
        Files.deleteIfExists(tempInstallerDir.resolve("META-INF").resolve("MANIFEST.MF"));
        Files.deleteIfExists(tempInstallerDir.resolve("lekeystore.jks"));
        Files.deleteIfExists(tempInstallerDir.resolve("big_logo.png"));
        Files.deleteIfExists(tempInstallerDir.resolve("META-INF").resolve("FORGE.DSA"));
        Files.deleteIfExists(tempInstallerDir.resolve("META-INF").resolve("FORGE.SF"));
    }
}
