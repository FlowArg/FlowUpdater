package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowupdater.download.json.CurseFileInfos;
import fr.flowarg.flowupdater.download.json.CurseModPackInfo;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.download.json.OptifineInfo;
import fr.flowarg.flowupdater.utils.ModFileDeleter;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Represent a new Forge version (1.12.2-14.23.5.2851 to 1.17)
 * @author FlowArg
 */
public class NewForgeVersion extends AbstractForgeVersion
{
    private final String[] compatibleVersions = {"1.17", "1.16", "1.15", "1.14", "1.13", "1.12.2-14.23.5.285"};

    NewForgeVersion(String forgeVersion, List<Mod> mods,
            List<CurseFileInfos> curseMods, ModFileDeleter fileDeleter,
            OptifineInfo optifine, CurseModPackInfo modPack)
    {
        super(mods, curseMods, forgeVersion, fileDeleter, optifine, modPack, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void install(final Path dirToInstall) throws Exception
    {
        super.install(dirToInstall);
        if (!this.isCompatible()) return;

        try (BufferedInputStream stream = new BufferedInputStream(this.installerUrl.openStream()))
        {
            final ModLoaderLauncherEnvironment forgeLauncherEnvironment = this.prepareModLoaderLauncher(dirToInstall, stream);
            final ProcessBuilder processBuilder = new ProcessBuilder(forgeLauncherEnvironment.getCommand());

            processBuilder.redirectOutput(Redirect.INHERIT);
            final Process process = processBuilder.start();
            process.waitFor();

            this.logger.info("Successfully installed Forge !");
            FileUtils.deleteDirectory(forgeLauncherEnvironment.getTempDir());
        }
        catch (IOException | InterruptedException e)
        {
            this.logger.printStackTrace(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkModLoaderEnv(Path dirToInstall) throws Exception
    {
        if(this.isCompatible() && super.checkModLoaderEnv(dirToInstall))
        {
            FileUtils.deleteDirectory(dirToInstall.resolve("libraries").resolve("net").resolve("minecraft"));
            FileUtils.deleteDirectory(dirToInstall.resolve("libraries").resolve("net").resolve("minecraftforge"));
            FileUtils.deleteDirectory(dirToInstall.resolve("libraries").resolve("de").resolve("oceanlabs"));
            FileUtils.deleteDirectory(dirToInstall.resolve("libraries").resolve("cpw"));
        }

        return false;
    }

    public boolean isCompatible()
    {
        for (String str : this.compatibleVersions)
        {
            if (this.forgeVersion.startsWith(str))
                return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void cleanInstaller(Path tempInstallerDir) throws Exception
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
