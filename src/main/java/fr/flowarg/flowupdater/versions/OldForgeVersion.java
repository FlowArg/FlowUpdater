package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.json.Mod;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Represent an old Forge version (1.7 -> 1.12.2) (No support for versions older than 1.7)
 * @author FlowArg
 */
public class OldForgeVersion extends AbstractForgeVersion
{
    public OldForgeVersion(String forgeVersion, VanillaVersion vanilla, ILogger logger, IProgressCallback callback, List<Mod> mods)
    {
        super(logger, mods, forgeVersion, vanilla, callback);
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
            this.logger.info("Downloading old forge installer...");
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
            Files.copy(new URL("https://flowarg.github.io/minecraft/launcher/oldpatches.jar").openStream(), patches.toPath(), StandardCopyOption.REPLACE_EXISTING);

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
            final ProcessBuilder processBuilder = new ProcessBuilder(command);
            
            processBuilder.redirectOutput(Redirect.INHERIT);
            processBuilder.directory(dirToInstall);
            final Process process = processBuilder.start();
            process.waitFor();
            
            this.logger.info("Successfully installed Forge !");
            FileUtils.deleteDirectory(tempDir);
            return true;
        }
        catch (IOException | InterruptedException e)
        {
            if(!first)
                this.logger.printStackTrace(e);
            return false;
        }
    }
    
    private void cleaningInstaller(File tempInstallerDir)
    {
        FileUtils.deleteDirectory(new File(tempInstallerDir, "net"));
        FileUtils.deleteDirectory(new File(tempInstallerDir, "joptisimple"));
        FileUtils.deleteDirectory(new File(tempInstallerDir, "META-INF"));
        FileUtils.deleteDirectory(new File(tempInstallerDir, "com"));
        new File(tempInstallerDir, "big_logo.png").delete();
        new File(tempInstallerDir, "url.png").delete();
    }

    public VanillaVersion getVanilla()
    {
        return this.vanilla;
    }
}
