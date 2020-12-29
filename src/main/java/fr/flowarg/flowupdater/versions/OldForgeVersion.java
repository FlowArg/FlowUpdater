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
import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Represent an old Forge version (1.7 -> 1.12.2) (No support for versions older than 1.7)
 * @author FlowArg
 */
public class OldForgeVersion extends AbstractForgeVersion
{
    OldForgeVersion(String forgeVersion, VanillaVersion vanilla, ILogger logger, IProgressCallback callback, List<Mod> mods, List<CurseFileInfos> curseMods, ModFileDeleter fileDeleter, OptifineInfo optifine, CurseModPackInfos modPack)
    {
        super(logger, mods, curseMods, forgeVersion, vanilla, callback, fileDeleter, optifine, modPack, true);
    }
    
    @Override
    public void install(File dirToInstall)
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

    private boolean installForge(File dirToInstall, boolean first)
    {
        try (BufferedInputStream stream = new BufferedInputStream(this.installerUrl.openStream()))
        {
            final ReadyToLaunchResult readyToLaunchResult = this.prepareForgePatches(dirToInstall, stream);
            final ProcessBuilder processBuilder = new ProcessBuilder(readyToLaunchResult.getCommand());
            
            processBuilder.redirectOutput(Redirect.INHERIT);
            processBuilder.directory(dirToInstall);
            final Process process = processBuilder.start();
            process.waitFor();
            
            this.logger.info("Successfully installed Forge !");
            FileUtils.deleteDirectory(readyToLaunchResult.getTempDir());
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
    protected void cleaningInstaller(File tempInstallerDir)
    {
        FileUtils.deleteDirectory(new File(tempInstallerDir, "net"));
        FileUtils.deleteDirectory(new File(tempInstallerDir, "joptisimple"));
        FileUtils.deleteDirectory(new File(tempInstallerDir, "META-INF"));
        FileUtils.deleteDirectory(new File(tempInstallerDir, "com"));
        new File(tempInstallerDir, "big_logo.png").delete();
        new File(tempInstallerDir, "url.png").delete();
    }
}
