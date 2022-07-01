package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowupdater.download.json.*;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.MalformedURLException;
import java.net.URL;
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
            OptiFineInfo optiFine, CurseModPackInfo modPack, ModrinthModPackInfo modrinthModPackInfo)
    {
        super(mods, curseMods, modrinthMods, forgeVersion, fileDeleter, optiFine, modPack, modrinthModPackInfo, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void install(Path dirToInstall) throws Exception
    {
        super.install(dirToInstall);
        if(!this.installForge(dirToInstall, true))
        {
            try
            {
                this.installerUrl = new URL(
                        String.format("https://files.minecraftforge.net/maven/net/minecraftforge/forge/%s-%s/forge-%s-%s-installer.jar",
                                      this.modLoaderVersion,
                                      this.vanilla.getName(),
                                      this.modLoaderVersion,
                                      this.vanilla.getName()));
                if(!this.installForge(dirToInstall, false))
                    this.logger.err("Check the given forge version !");
            }
            catch (MalformedURLException ignored) {}
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkModLoaderEnv(@NotNull Path dirToInstall) throws Exception
    {
        if(super.checkModLoaderEnv(dirToInstall))
        {
            FileUtils.deleteDirectory(dirToInstall.resolve("libraries").resolve("net").resolve("minecraft"));
            FileUtils.deleteDirectory(dirToInstall.resolve("libraries").resolve("net").resolve("minecraftforge"));
        }

        return false;
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
            
            this.logger.info("Successfully installed Forge!");
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
