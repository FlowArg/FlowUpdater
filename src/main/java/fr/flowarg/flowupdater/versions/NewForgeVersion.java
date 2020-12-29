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
import java.util.List;

/**
 * Represent a new Forge version (1.12.2-14.23.5.2851 -> 1.16.4)
 * @author FlowArg
 */
public class NewForgeVersion extends AbstractForgeVersion
{
    private final String[] compatibleVersions = {"1.16", "1.15", "1.14", "1.13", "1.12.2-14.23.5.285"};
    private final boolean noGui;

    NewForgeVersion(String forgeVersion, VanillaVersion vanilla, ILogger logger, IProgressCallback callback, List<Mod> mods, List<CurseFileInfos> curseMods, boolean noGui, ModFileDeleter fileDeleter, OptifineInfo optifine, CurseModPackInfos modPack)
    {
        super(logger, mods, curseMods, forgeVersion, vanilla, callback, fileDeleter, optifine, modPack, false);
        this.noGui = noGui;
    }

    @Override
    public void install(final File dirToInstall)
    {
        super.install(dirToInstall);
        if (this.isCompatible())
        {
            try (BufferedInputStream stream = new BufferedInputStream(this.installerUrl.openStream()))
            {
                final ReadyToLaunchResult readyToLaunchResult = this.prepareForgePatches(dirToInstall, stream);

                if(this.noGui)
                	readyToLaunchResult.getCommand().add("--nogui");
                final ProcessBuilder processBuilder = new ProcessBuilder(readyToLaunchResult.getCommand());
                
                processBuilder.redirectOutput(Redirect.INHERIT);
                final Process process = processBuilder.start();
                process.waitFor();
                
                this.logger.info("Successfully installed Forge !");
                FileUtils.deleteDirectory(readyToLaunchResult.getTempDir());
            }
            catch (IOException | InterruptedException e)
            {
                this.logger.printStackTrace(e);
            }
        }
    }

    @Override
    protected boolean checkForgeEnv(File dirToInstall)
    {
        final boolean hasAnotherVersions = super.checkForgeEnv(dirToInstall);
        if(this.isCompatible() && !this.forgeVersion.contains("1.12.2") && hasAnotherVersions)
        {
            final File minecraftForgeDir = new File(dirToInstall, "libraries/net/minecraft/");
            final File mappingsDir = new File(dirToInstall, "libraries/de/");
            FileUtils.deleteDirectory(minecraftForgeDir);
            FileUtils.deleteDirectory(mappingsDir);
        }

        return false;
    }

    public boolean isCompatible()
    {
        for(String str : this.compatibleVersions)
        {
            if(this.forgeVersion.startsWith(str))
                return true;
        }
        return false;
    }

    @Override
    protected void cleaningInstaller(File tempInstallerDir)
    {
        FileUtils.deleteDirectory(new File(tempInstallerDir, "net"));
        FileUtils.deleteDirectory(new File(tempInstallerDir, "com"));
        FileUtils.deleteDirectory(new File(tempInstallerDir, "joptisimple"));
        new File(tempInstallerDir, "META-INF/MANIFEST.MF").delete();
        new File(tempInstallerDir, "lekeystore.jks").delete();
        new File(tempInstallerDir, "big_logo.png").delete();
        new File(tempInstallerDir, "META-INF/FORGE.DSA").delete();
        new File(tempInstallerDir, "META-INF/FORGE.SF").delete();
    }
    
    public boolean isNoGui()
    {
        return this.noGui;
    }
}
