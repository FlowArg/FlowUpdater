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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Represent a new Forge version (1.12.2-14.23.5.2851 -> 1.16.3)
 * @author FlowArg
 */
public class NewForgeVersion extends AbstractForgeVersion
{
    private final String[] compatibleVersions = {"1.16", "1.15", "1.14", "1.13", "1.12.2-14.23.5.285"};
    private final boolean noGui;

    NewForgeVersion(String forgeVersion, VanillaVersion vanilla, ILogger logger, IProgressCallback callback, List<Mod> mods, List<CurseFileInfos> curseMods, boolean noGui, ModFileDeleter fileDeleter, OptifineInfo optifine, CurseModPackInfos modPack)
    {
        super(logger, mods, curseMods, forgeVersion, vanilla, callback, fileDeleter, optifine, modPack);
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
                this.logger.info("Downloading new forge installer...");
                final File tempDir = new File(dirToInstall, ".flowupdater");
                final File tempInstallerDir = new File(tempDir, "installer/");
                final File install = new File(tempDir, "forge-installer.jar");
                final File patches = new File(tempDir, "patches.jar");
                final File patchedInstaller = new File(tempDir, "forge-installer-patched.jar");
                FileUtils.deleteDirectory(tempInstallerDir);
                install.delete();
                patchedInstaller.delete();
                patches.delete();
                tempDir.mkdirs();
                tempInstallerDir.mkdirs();

                Files.copy(stream, install.toPath(), StandardCopyOption.REPLACE_EXISTING);
                this.logger.info("Downloading patches...");
                Files.copy(new URL("https://flowarg.github.io/minecraft/launcher/patches.jar").openStream(), patches.toPath(), StandardCopyOption.REPLACE_EXISTING);

                this.logger.info("Applying patches...");
                FileUtils.unzipJarWithLZMACompat(tempInstallerDir, install);
                this.cleaningInstaller(tempInstallerDir);
                FileUtils.unzipJarWithLZMACompat(tempInstallerDir, patches);
                this.logger.info("Repack installer...");
                this.packPatchedInstaller(tempDir, tempInstallerDir);
                patches.delete();
                this.logger.info("Launching forge installer...");
                
                final ArrayList<String> command = new ArrayList<>();
                command.add("java");
                command.add("-Xmx256M");
                command.add("-jar");
                command.add(patchedInstaller.getAbsolutePath());
                command.add("--installClient");
                command.add(dirToInstall.getAbsolutePath());
                if(this.noGui)
                	command.add("--nogui");
                final ProcessBuilder processBuilder = new ProcessBuilder(command);
                
                processBuilder.redirectOutput(Redirect.INHERIT);
                final Process process = processBuilder.start();
                process.waitFor();
                
                this.logger.info("Successfully installed Forge !");
                FileUtils.deleteDirectory(tempDir);
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

    private void cleaningInstaller(File tempInstallerDir)
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
