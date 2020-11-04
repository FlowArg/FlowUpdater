package fr.flowarg.flowupdater.versions;

import fr.antoineok.flowupdater.optifineplugin.Optifine;
import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.curseforgeplugin.CurseMod;
import fr.flowarg.flowupdater.download.DownloadInfos;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.Step;
import fr.flowarg.flowupdater.download.json.CurseFileInfos;
import fr.flowarg.flowupdater.download.json.CurseModPackInfos;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.download.json.OptifineInfo;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import fr.flowarg.flowupdater.utils.PluginManager;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * The base object of a forge version.
 * Implemented by {@link OldForgeVersion} & {@link NewForgeVersion}
 * @author flow
 */
public abstract class AbstractForgeVersion
{
    protected final ILogger logger;
    protected final List<Mod> mods;
    protected final VanillaVersion vanilla;
    protected final String forgeVersion;
    protected final IProgressCallback callback;
    protected final List<CurseFileInfos> curseMods;
    protected final ModFileDeleter fileDeleter;
    protected final OptifineInfo optifine;
    protected final CurseModPackInfos modPackInfos;
    protected List<Object> allCurseMods;
    protected URL installerUrl;
    protected DownloadInfos downloadInfos;

    /**
     * Use {@link ForgeVersionBuilder} to instantiate this class.
     * @param logger {@link ILogger} used for logging.
     * @param mods {@link List<Mod>} to install.
     * @param curseMods {@link List<CurseFileInfos>} to install.
     * @param forgeVersion to install.
     * @param vanilla {@link VanillaVersion}.
     * @param callback {@link IProgressCallback} used for update progression.
     * @param fileDeleter {@link ModFileDeleter} used to cleanup mods dir.
     * @param optifine Optifine version to install.
     */
    protected AbstractForgeVersion(ILogger logger, List<Mod> mods, List<CurseFileInfos> curseMods, String forgeVersion, VanillaVersion vanilla, IProgressCallback callback, ModFileDeleter fileDeleter, OptifineInfo optifine, CurseModPackInfos modPackInfos)
    {
        this.logger = logger;
        this.mods = mods;
        this.fileDeleter = fileDeleter;
        this.curseMods = curseMods;
        this.vanilla = vanilla;
        this.optifine = optifine;
        this.modPackInfos = modPackInfos;
        if (!forgeVersion.contains("-"))
            this.forgeVersion = this.vanilla.getName() + '-' + forgeVersion;
        else this.forgeVersion = forgeVersion.trim();
        this.callback = callback;
        try
        {
            this.installerUrl = new URL(String.format("https://files.minecraftforge.net/maven/net/minecraftforge/forge/%s/forge-%s-installer.jar", this.forgeVersion, this.forgeVersion));
        } catch (MalformedURLException e)
        {
            this.logger.printStackTrace(e);
        }
    }
    
    /**
     * Check if forge is already installed. Used by {@link FlowUpdater} on update task.
     * @param installDir the minecraft installation dir.
     * @return true if forge is already installed or not.
     */
    public boolean isForgeAlreadyInstalled(File installDir)
    {
        return new File(installDir, "libraries/net/minecraftforge/forge/" + this.forgeVersion + "/" + "forge-" + this.forgeVersion + ".jar").exists();
    }
    
    /**
     * This function installs a Forge version at the specified directory.
     * @param dirToInstall Specified directory.
     */
    public void install(final File dirToInstall)
    {
        this.callback.step(Step.FORGE);
        this.logger.info("Installing forge, version: " + this.forgeVersion + "...");
        this.checkForgeEnv(dirToInstall);
    }

    /**
     * Check if the minecraft installation already contains another forge installation not corresponding to this version.
     * @param dirToInstall Forge installation directory.
     */
    protected boolean checkForgeEnv(File dirToInstall)
    {
        boolean result = false;
        final File forgeDir = new File(dirToInstall, "libraries/net/minecraftforge/forge/");
        if(forgeDir.exists())
        {
            if(forgeDir.listFiles() != null)
            {
                for (File contained : forgeDir.listFiles())
                {
                    if(!contained.getName().contains(this.forgeVersion))
                    {
                        if (contained.isDirectory()) FileUtils.deleteDirectory(contained);
                        else contained.delete();
                        result = true;
                    }
                }
            }
        }

        return result;
    }
    
    /**
     * This function installs mods at the specified directory.
     * @param modsDir Specified mods directory.
     * @param pluginManager PluginManager of FlowUpdater
     * @throws IOException If the install fail.
     */
    public void installMods(File modsDir, PluginManager pluginManager) throws Exception
    {
        this.callback.step(Step.MODS);
        final boolean cursePluginLoaded = pluginManager.isCursePluginLoaded();
        final boolean optifinePluginLoaded = pluginManager.isOptifinePluginLoaded();
        this.downloadInfos.getMods().forEach(mod -> {
            try
            {
                IOUtils.download(this.logger, new URL(mod.getDownloadURL()), new File(modsDir, mod.getName()));
            }
            catch (MalformedURLException e)
            {
                this.logger.printStackTrace(e);
            }
            this.downloadInfos.incrementDownloaded();
            this.callback.update(this.downloadInfos.getDownloaded(), this.downloadInfos.getTotalToDownload());
        });

        if(cursePluginLoaded)
        {
            this.downloadInfos.getCurseMods().forEach(obj -> {
                try
                {
                    final CurseMod curseMod = (CurseMod)obj;
                    IOUtils.download(this.logger, new URL(curseMod.getDownloadURL()), new File(modsDir, curseMod.getName()));
                } catch (MalformedURLException e)
                {
                    this.logger.printStackTrace(e);
                }
                this.downloadInfos.incrementDownloaded();
                this.callback.update(this.downloadInfos.getDownloaded(), this.downloadInfos.getTotalToDownload());
            });
        }

        Object ofObj = null;
        if(optifinePluginLoaded)
        {
            if(this.downloadInfos.getOptifine() != null)
            {
                try
                {
                    final Optifine optifine = (Optifine)this.downloadInfos.getOptifine();
                    ofObj = optifine;
                    final File pluginDir = new File(modsDir.getParentFile(), "FUPlugins/OptifinePlugin/");
                    final File optifineFile = new File(modsDir, optifine.getName());
                    if(!optifineFile.exists())
                        Files.copy(new File(pluginDir, optifine.getName()).toPath(), optifineFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (MalformedURLException e)
                {
                    this.logger.printStackTrace(e);
                }
                this.downloadInfos.incrementDownloaded();
                this.callback.update(this.downloadInfos.getDownloaded(), this.downloadInfos.getTotalToDownload());
            }
        }

        this.fileDeleter.delete(modsDir, this.mods, cursePluginLoaded, this.allCurseMods, optifinePluginLoaded, ofObj);
    }

    public ModFileDeleter getFileDeleter()
    {
        return this.fileDeleter;
    }

    public void appendDownloadInfos(DownloadInfos infos)
    {
        this.downloadInfos = infos;
    }
    
    protected void packPatchedInstaller(final File tempDir, final File tempInstallerDir) throws IOException
    {
        final File output = new File(tempDir, "forge-installer-patched.zip");
        FileUtils.compressFiles(tempInstallerDir.listFiles(), output);
        Files.move(output.toPath(), new File(output.getAbsolutePath().replace(".zip", ".jar")).toPath(), StandardCopyOption.REPLACE_EXISTING);
        tempInstallerDir.delete();
    }
    
    public List<Mod> getMods()
    {
        return this.mods;
    }
    public ILogger getLogger()
    {
        return this.logger;
    }
    public String getForgeVersion()
    {
        return this.forgeVersion;
    }
    public URL getInstallerUrl()
    {
        return this.installerUrl;
    }
    public List<Object> getAllCurseMods()
    {
        return this.allCurseMods;
    }
    public void setAllCurseMods(List<Object> allCurseMods)
    {
        this.allCurseMods = allCurseMods;
    }
    public List<CurseFileInfos> getCurseMods()
    {
        return this.curseMods;
    }
    public OptifineInfo getOptifine()
    {
        return this.optifine;
    }
    public CurseModPackInfos getModPackInfos()
    {
        return this.modPackInfos;
    }
}
