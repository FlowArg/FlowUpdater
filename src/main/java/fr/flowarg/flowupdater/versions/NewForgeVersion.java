package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.json.CurseFileInfos;
import fr.flowarg.flowupdater.download.json.CurseModPackInfos;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.download.json.OptifineInfo;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.ModFileDeleter;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Represent a new Forge version (1.12.2-14.23.5.2851 to 1.16.5)
 * @author FlowArg
 */
public class NewForgeVersion extends AbstractForgeVersion
{
    private final String[] compatibleVersions = {"1.17", "1.16", "1.15", "1.14", "1.13", "1.12.2-14.23.5.285"};

    NewForgeVersion(String forgeVersion, VanillaVersion vanilla, ILogger logger, IProgressCallback callback, List<Mod> mods, List<CurseFileInfos> curseMods, ModFileDeleter fileDeleter, OptifineInfo optifine, CurseModPackInfos modPack)
    {
        super(logger, mods, curseMods, forgeVersion, vanilla, callback, fileDeleter, optifine, modPack, false);
    }

    @Override
    public void install(final Path dirToInstall) throws Exception
    {
        super.install(dirToInstall);
        if (this.isCompatible())
        {
            try (BufferedInputStream stream = new BufferedInputStream(this.installerUrl.openStream()))
            {
                final ModLoaderLauncherEnvironment forgeLauncherEnvironment = this.prepareModLoaderLauncher(dirToInstall, stream);
                forgeLauncherEnvironment.getCommand().add("--nogui");
                final ProcessBuilder processBuilder = new ProcessBuilder(forgeLauncherEnvironment.getCommand());
                
                processBuilder.redirectOutput(Redirect.INHERIT);
                final Process process = processBuilder.start();
                process.waitFor();
                
                this.logger.info("Successfully installed Forge !");
                IOUtils.deleteDirectory(forgeLauncherEnvironment.getTempDir());
            }
            catch (IOException | InterruptedException e)
            {
                this.logger.printStackTrace(e);
            }
        }
    }

    @Override
    protected boolean checkForgeEnv(Path dirToInstall) throws Exception
    {
        if(this.isCompatible() && !this.forgeVersion.contains("1.12.2") && super.checkForgeEnv(dirToInstall))
        {
            final Path minecraftDirPath = Paths.get(dirToInstall.toString(), "libraries", "net", "minecraft");
            final Path minecraftForgeDirPath = Paths.get(dirToInstall.toString(), "libraries", "net", "minecraftforge");
            final Path mappingsDirPath = Paths.get(dirToInstall.toString(), "libraries", "de", "oceanlabs");

            IOUtils.deleteDirectory(minecraftDirPath);
            IOUtils.deleteDirectory(minecraftForgeDirPath);
            IOUtils.deleteDirectory(mappingsDirPath);
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

    @Override
    protected void cleanInstaller(Path tempInstallerDir) throws Exception
    {
        final String path = tempInstallerDir.toString();
        IOUtils.deleteDirectory(Paths.get(path, "net"));
        IOUtils.deleteDirectory(Paths.get(path, "com"));
        IOUtils.deleteDirectory(Paths.get(path, "joptsimple"));
        Files.deleteIfExists(Paths.get(path, "META-INF", "MANIFEST.MF"));
        Files.deleteIfExists(Paths.get(path, "lekeystore.jks"));
        Files.deleteIfExists(Paths.get(path, "big_logo.png"));
        Files.deleteIfExists(Paths.get(path, "META-INF", "FORGE.DSA"));
        Files.deleteIfExists(Paths.get(path, "META-INF", "FORGE.SF"));
    }
}
