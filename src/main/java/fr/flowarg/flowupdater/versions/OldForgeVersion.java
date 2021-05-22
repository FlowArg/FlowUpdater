package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.json.CurseFileInfos;
import fr.flowarg.flowupdater.download.json.CurseModPackInfos;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.download.json.OptifineInfo;
import fr.flowarg.flowupdater.utils.ModFileDeleter;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Represent an old Forge version (1.7 to 1.12.2) (No support for versions older than 1.7)
 * @author FlowArg
 */
public class OldForgeVersion extends AbstractForgeVersion
{
    OldForgeVersion(String forgeVersion, VanillaVersion vanilla, ILogger logger, IProgressCallback callback, List<Mod> mods, List<CurseFileInfos> curseMods, ModFileDeleter fileDeleter, OptifineInfo optifine, CurseModPackInfos modPack)
    {
        super(logger, mods, curseMods, forgeVersion, vanilla, callback, fileDeleter, optifine, modPack, true);
    }
    
    @Override
    public void install(Path dirToInstall) throws Exception
    {
        super.install(dirToInstall);
        if(!this.installForge(dirToInstall, true))
        {
            try
            {
                this.installerUrl = new URL(String.format("https://files.minecraftforge.net/maven/net/minecraftforge/forge/%s-%s/forge-%s-%s-installer.jar", this.forgeVersion, this.vanilla.getName(), this.forgeVersion, this.vanilla.getName()));
                if(!this.installForge(dirToInstall, false))
                    this.logger.err("Check the given forge version !");
            }
            catch (MalformedURLException ignored) {}
        }
    }

    private boolean installForge(Path dirToInstall, boolean first) throws Exception
    {
        try (BufferedInputStream stream = new BufferedInputStream(this.installerUrl.openStream()))
        {
            final ModLoaderLauncherEnvironment forgeLauncherEnvironment = this.prepareModLoaderLauncher(dirToInstall, stream);
            final ProcessBuilder processBuilder = new ProcessBuilder(forgeLauncherEnvironment.getCommand());
            
            processBuilder.redirectOutput(Redirect.INHERIT);
            processBuilder.directory(dirToInstall.toFile());
            final Process process = processBuilder.start();
            process.waitFor();
            
            this.logger.info("Successfully installed Forge !");
            FileUtils.deleteDirectory(forgeLauncherEnvironment.getTempDir());
            return true;
        }
        catch (IOException | InterruptedException e)
        {
            if(!first)
                this.logger.printStackTrace(e);
            return false;
        }
    }

    @Override
    protected void cleanInstaller(Path tempInstallerDir) throws Exception
    {
        final String path = tempInstallerDir.toString();
        FileUtils.deleteDirectory(Paths.get(path, "net"));
        FileUtils.deleteDirectory(Paths.get(path, "com"));
        FileUtils.deleteDirectory(Paths.get(path, "joptsimple"));
        FileUtils.deleteDirectory(Paths.get(path, "META-INF"));
        Files.deleteIfExists(Paths.get(path, "big_logo.png"));
        Files.deleteIfExists(Paths.get(path, "url.png"));
    }
}
