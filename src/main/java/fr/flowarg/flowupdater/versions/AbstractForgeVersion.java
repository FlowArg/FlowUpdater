package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.curseforgeplugin.CurseMod;
import fr.flowarg.flowupdater.download.DownloadInfos;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.Step;
import fr.flowarg.flowupdater.download.json.CurseModInfos;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.utils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static fr.flowarg.flowio.FileUtils.*;

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
    protected final ArrayList<CurseModInfos> curseMods;
    protected final boolean useFileDeleter;
    protected List<Object> allCurseMods;
    protected URL installerUrl;
    protected DownloadInfos downloadInfos;

    protected AbstractForgeVersion(ILogger logger, List<Mod> mods, ArrayList<CurseModInfos> curseMods, String forgeVersion, VanillaVersion vanilla, IProgressCallback callback, boolean useFileDeleter)
    {
        this.logger = logger;
        this.mods = mods;
        this.useFileDeleter = useFileDeleter;
        this.curseMods = curseMods;
        this.vanilla = vanilla;
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
     * @param cursePluginLoaded if FlowUpdater has loaded CurseForge plugin
     * @throws IOException If install fail.
     */
    public void installMods(File modsDir, boolean cursePluginLoaded) throws Exception
    {
        this.callback.step(Step.MODS);
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
        
        if(this.useFileDeleter)
        {
            final List<File> badFiles = new ArrayList<>();
            final List<File> verifiedFiles = new ArrayList<>();
            for(File fileInDir : modsDir.listFiles())
            {
                if(!fileInDir.isDirectory())
                {
                    if(this.mods.isEmpty() && this.allCurseMods.isEmpty())
                    {
                        badFiles.add(fileInDir);
                    }
                    else
                    {
                        if(cursePluginLoaded)
                        {
                            for(Object obj : this.getAllCurseMods())
                            {
                                final CurseMod mod = (CurseMod)obj;
                                final File file = new File(modsDir, mod.getName().endsWith(".jar") ? mod.getName() : mod.getName() + ".jar");
                                if(file.getName().equalsIgnoreCase(fileInDir.getName()))
                                {
                                    if(getMD5ofFile(fileInDir).equals(mod.getMd5()) && getFileSizeBytes(fileInDir) == mod.getLength())
                                    {
                                        badFiles.remove(fileInDir);
                                        verifiedFiles.add(fileInDir);
                                    }
                                    else badFiles.add(fileInDir);
                                }
                                else
                                {
                                    if(!verifiedFiles.contains(fileInDir))
                                        badFiles.add(fileInDir);
                                }
                            }
                        }

                        for(Mod mod : this.mods)
                        {
                            final File file = new File(modsDir, mod.getName().endsWith(".jar") ? mod.getName() : mod.getName() + ".jar");
                            if(file.getName().equalsIgnoreCase(fileInDir.getName()))
                            {
                                if(getSHA1(fileInDir).equals(mod.getSha1()) && getFileSizeBytes(fileInDir) == mod.getSize())
                                {
                                    badFiles.remove(fileInDir);
                                    verifiedFiles.add(fileInDir);
                                }
                                else badFiles.add(fileInDir);
                            }
                            else
                            {
                                if(!verifiedFiles.contains(fileInDir))
                                    badFiles.add(fileInDir);
                            }
                        }
                    }
                }
            }
            
            badFiles.forEach(File::delete);
            badFiles.clear();
        }
    }
    
    public boolean isModFileDeleterEnabled()
    {
        return this.useFileDeleter;
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

    public ArrayList<CurseModInfos> getCurseMods()
    {
        return this.curseMods;
    }
}
