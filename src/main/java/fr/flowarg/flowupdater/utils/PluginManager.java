package fr.flowarg.flowupdater.utils;

import fr.antoineok.flowupdater.optifineplugin.Optifine;
import fr.antoineok.flowupdater.optifineplugin.OptifinePlugin;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowlogger.Logger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.curseforgeplugin.CurseForgePlugin;
import fr.flowarg.flowupdater.curseforgeplugin.CurseMod;
import fr.flowarg.flowupdater.curseforgeplugin.CurseModPack;
import fr.flowarg.flowupdater.download.DownloadInfos;
import fr.flowarg.flowupdater.download.ICurseFeaturesUser;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.Step;
import fr.flowarg.flowupdater.download.json.CurseFileInfos;
import fr.flowarg.flowupdater.download.json.CurseModPackInfos;
import fr.flowarg.flowupdater.versions.AbstractForgeVersion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static fr.flowarg.flowio.FileUtils.getFileSizeBytes;
import static fr.flowarg.flowio.FileUtils.getMD5ofFile;

public class PluginManager
{
    private final IProgressCallback progressCallback;
    private final ILogger logger;
    private final DownloadInfos downloadInfos;

    private boolean cursePluginLoaded = false;
    private boolean optifinePluginLoaded = false;

    public PluginManager(FlowUpdater updater)
    {
        this.progressCallback = updater.getCallback();
        this.logger = updater.getLogger();
        this.downloadInfos = updater.getDownloadInfos();
    }

    public void loadCurseForgePlugin(Path dir, ICurseFeaturesUser curseFeaturesUser)
    {
        if (!this.cursePluginLoaded)
        {
            try
            {
                Class.forName("fr.flowarg.flowupdater.curseforgeplugin.CurseForgePlugin");
                this.cursePluginLoaded = true;
                CurseForgePlugin.INSTANCE.setLogger(new Logger("[CurseForgePlugin]", this.logger.getLogFile(), true));
                CurseForgePlugin.INSTANCE.setFolder(Paths.get(dir.getParent().toString(), ".cfp"));
            } catch (ClassNotFoundException e)
            {
                this.cursePluginLoaded = false;
                this.logger.err("Cannot install mods from CurseForge: CurseAPI is not loaded. Please, enable the 'enableCurseForgePlugin' updater option !");
                return;
            }
        }

        final List<Object> allCurseMods = new ArrayList<>(curseFeaturesUser.getCurseMods().size());

        for (CurseFileInfos infos : curseFeaturesUser.getCurseMods())
        {
            try
            {
                final CurseForgePlugin curseForgePlugin = CurseForgePlugin.INSTANCE;
                final CurseMod mod = curseForgePlugin.getCurseMod(infos.getProjectID(), infos.getFileID());
                allCurseMods.add(mod);

                final Path filePath = Paths.get(dir.toString(), mod.getName());
                if(Files.notExists(filePath) || !getMD5ofFile(filePath.toFile()).equals(mod.getMd5()) || getFileSizeBytes(filePath) != mod.getLength())
                {
                    if (!mod.getMd5().contains("-"))
                    {
                        Files.deleteIfExists(filePath);
                        this.downloadInfos.getCurseMods().add(mod);
                    }
                }
            } catch (Exception e)
            {
                this.logger.printStackTrace(e);
            }
        }
        final CurseModPackInfos modPackInfos = curseFeaturesUser.getModPackInfos();
        if (modPackInfos != null)
        {
            this.progressCallback.step(Step.MOD_PACK);
            final CurseForgePlugin plugin = CurseForgePlugin.INSTANCE;
            final CurseModPack modPack = plugin.getCurseModPack(modPackInfos.getProjectID(), modPackInfos.getFileID(), modPackInfos.isInstallExtFiles());
            this.logger.info("Loading mod pack: " + modPack.getName() + " (" + modPack.getVersion() + ") by " + modPack.getAuthor() + '.');
            modPack.getMods().forEach(mod -> {
                allCurseMods.add(mod);
                try
                {
                    final Path filePath = Paths.get(dir.toString(), mod.getName());
                    boolean flag = false;
                    for (String exclude : modPackInfos.getExcluded())
                    {
                        if (mod.getName().equalsIgnoreCase(exclude))
                        {
                            flag = !mod.isRequired();
                            break;
                        }
                    }
                    if(!flag && (Files.notExists(filePath) || !getMD5ofFile(filePath.toFile()).equals(mod.getMd5()) || getFileSizeBytes(filePath) != mod.getLength()))
                    {
                        if (!mod.getMd5().contains("-"))
                        {
                            Files.deleteIfExists(filePath);
                            this.downloadInfos.getCurseMods().add(mod);
                        }
                    }
                } catch (NoSuchAlgorithmException | IOException e)
                {
                    this.logger.printStackTrace(e);
                }
            });
        }

        curseFeaturesUser.setAllCurseMods(allCurseMods);
    }

    public void loadOptifinePlugin(Path dir, AbstractForgeVersion forgeVersion)
    {
        try
        {
            Class.forName("fr.antoineok.flowupdater.optifineplugin.OptifinePlugin");
            this.optifinePluginLoaded = true;
            try
            {
                final OptifinePlugin optifinePlugin = OptifinePlugin.INSTANCE;
                optifinePlugin.setLogger(new Logger("[OptifinePlugin]", this.logger.getLogFile(), true));
                optifinePlugin.setFolder(Paths.get(dir.getParent().toString(), ".op"));
                final Optifine optifine = optifinePlugin.getOptifine(forgeVersion.getOptifine().getVersion(), forgeVersion.getOptifine().isPreview());
                this.downloadInfos.setOptifine(optifine);
            } catch (Exception e)
            {
                this.logger.printStackTrace(e);
            }
        } catch (ClassNotFoundException e)
        {
            this.optifinePluginLoaded = false;
            this.logger.err("Cannot install optifine: OptifinePlugin is not loaded. Please, enable the 'enableOptifineDownloaderPlugin' updater option !");
        }
    }

    public void shutdown()
    {
        if (this.cursePluginLoaded) CurseForgePlugin.INSTANCE.shutdownOKHTTP();
        if (this.optifinePluginLoaded) OptifinePlugin.INSTANCE.shutdownOKHTTP();
        this.cursePluginLoaded = false;
        this.optifinePluginLoaded = false;
    }

    public ILogger getLogger()
    {
        return this.logger;
    }

    public boolean isCursePluginLoaded()
    {
        return this.cursePluginLoaded;
    }

    public boolean isOptifinePluginLoaded()
    {
        return this.optifinePluginLoaded;
    }
}
