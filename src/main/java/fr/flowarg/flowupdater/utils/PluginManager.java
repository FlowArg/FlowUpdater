package fr.flowarg.flowupdater.utils;

import fr.antoineok.flowupdater.optifineplugin.OptiFine;
import fr.antoineok.flowupdater.optifineplugin.OptiFinePlugin;
import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.curseforgeplugin.CurseForgePlugin;
import fr.flowarg.flowupdater.curseforgeplugin.CurseMod;
import fr.flowarg.flowupdater.curseforgeplugin.CurseModPack;
import fr.flowarg.flowupdater.download.DownloadList;
import fr.flowarg.flowupdater.download.ICurseFeaturesUser;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.Step;
import fr.flowarg.flowupdater.download.json.CurseFileInfo;
import fr.flowarg.flowupdater.download.json.CurseModPackInfo;
import fr.flowarg.flowupdater.versions.AbstractForgeVersion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PluginManager
{
    private final IProgressCallback progressCallback;
    private final ILogger logger;
    private final DownloadList downloadList;

    private boolean cursePluginLoaded = false;
    private boolean optiFinePluginLoaded = false;

    public PluginManager(FlowUpdater updater)
    {
        this.progressCallback = updater.getCallback();
        this.logger = updater.getLogger();
        this.downloadList = updater.getDownloadList();
    }

    public void loadCurseForgePlugin(Path dir, ICurseFeaturesUser curseFeaturesUser)
    {
        if (this.cursePluginLoaded) return;

        try
        {
            Class.forName("fr.flowarg.flowupdater.curseforgeplugin.CurseForgePlugin");
            this.cursePluginLoaded = true;
            CurseForgePlugin.INSTANCE.setLogger(this.logger);
            CurseForgePlugin.INSTANCE.setFolder(dir.getParent().resolve(".cfp"));
        }
        catch (ClassNotFoundException e)
        {
            this.cursePluginLoaded = false;
            this.logger.err("Cannot install mods from CurseForge: CurseAPI is not loaded. Please, enable the 'enableCurseForgePlugin' updater option !");
            return;
        }

        final List<Object> allCurseMods = new ArrayList<>(curseFeaturesUser.getCurseMods().size());

        for (CurseFileInfo info : curseFeaturesUser.getCurseMods())
        {
            try
            {
                final CurseForgePlugin curseForgePlugin = CurseForgePlugin.INSTANCE;
                final CurseMod mod = curseForgePlugin.getCurseMod(info.getProjectID(), info.getFileID());
                allCurseMods.add(mod);

                final Path filePath = dir.resolve(mod.getName());

                if(Files.exists(filePath) && FileUtils.getMD5(filePath).equals(mod.getMd5()) && FileUtils.getFileSizeBytes(filePath) == mod.getLength()) continue;

                if (mod.getMd5().contains("-")) continue;

                Files.deleteIfExists(filePath);
                this.downloadList.getCurseMods().add(mod);
            } catch (Exception e)
            {
                this.logger.printStackTrace(e);
            }
        }

        final CurseModPackInfo modPackInfo = curseFeaturesUser.getModPackInfo();

        if (modPackInfo == null)
        {
            curseFeaturesUser.setAllCurseMods(allCurseMods);
            return;
        }

        this.progressCallback.step(Step.MOD_PACK);
        final CurseForgePlugin plugin = CurseForgePlugin.INSTANCE;
        final CurseModPack modPack = plugin.getCurseModPack(modPackInfo.getProjectID(), modPackInfo.getFileID(), modPackInfo.isInstallExtFiles());
        this.logger.info("Loading mod pack: " + modPack.getName() + " (" + modPack.getVersion() + ") by " + modPack.getAuthor() + '.');
        modPack.getMods().forEach(mod -> {
            allCurseMods.add(mod);
            try
            {
                final Path filePath = dir.resolve(mod.getName());
                boolean flag = false;
                for (String exclude : modPackInfo.getExcluded())
                {
                    if (!mod.getName().equalsIgnoreCase(exclude)) continue;

                    flag = !mod.isRequired();
                    break;
                }
                if(!flag && (Files.notExists(filePath) || !FileUtils.getMD5(filePath).equalsIgnoreCase(mod.getMd5()) || FileUtils.getFileSizeBytes(filePath) != mod.getLength()))
                {
                    if (mod.getMd5().contains("-")) return;

                    Files.deleteIfExists(filePath);
                    this.downloadList.getCurseMods().add(mod);
                }
            } catch (IOException e)
            {
                this.logger.printStackTrace(e);
            }
        });

        curseFeaturesUser.setAllCurseMods(allCurseMods);
    }

    public void loadOptiFinePlugin(Path dir, AbstractForgeVersion forgeVersion)
    {
        if(forgeVersion.getOptiFineInfo() == null) return;

        try
        {
            Class.forName("fr.antoineok.flowupdater.optifineplugin.OptiFinePlugin");
            this.optiFinePluginLoaded = true;
            try
            {
                final OptiFinePlugin optifinePlugin = OptiFinePlugin.INSTANCE;
                optifinePlugin.setLogger(this.logger);
                optifinePlugin.setFolder(dir.getParent().resolve(".op"));
                final OptiFine optifine = optifinePlugin.getOptiFine(forgeVersion.getOptiFineInfo().getVersion(), forgeVersion.getOptiFineInfo().isPreview());
                this.downloadList.setOptiFine(optifine);
            } catch (Exception e)
            {
                this.logger.printStackTrace(e);
            }
        } catch (ClassNotFoundException e)
        {
            this.optiFinePluginLoaded = false;
            this.logger.err("Cannot install OptiFine: OptiFinePlugin is not loaded. Please, enable the 'enableOptiFineDownloaderPlugin' updater option !");
        }
    }

    public void shutdown()
    {
        if (this.cursePluginLoaded) CurseForgePlugin.INSTANCE.shutdownOKHTTP();
        if (this.optiFinePluginLoaded) OptiFinePlugin.INSTANCE.shutdownOKHTTP();
        this.cursePluginLoaded = false;
        this.optiFinePluginLoaded = false;
    }

    public ILogger getLogger()
    {
        return this.logger;
    }

    public boolean isCursePluginLoaded()
    {
        return this.cursePluginLoaded;
    }

    public boolean isOptiFinePluginLoaded()
    {
        return this.optiFinePluginLoaded;
    }
}
