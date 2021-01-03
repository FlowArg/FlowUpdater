package fr.flowarg.flowupdater.utils;

import fr.antoineok.flowupdater.optifineplugin.Optifine;
import fr.antoineok.flowupdater.optifineplugin.OptifinePlugin;
import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
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
import fr.flowarg.pluginloaderapi.PluginLoaderAPI;
import fr.flowarg.pluginloaderapi.plugin.PluginLoader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static fr.flowarg.flowio.FileUtils.getFileSizeBytes;
import static fr.flowarg.flowio.FileUtils.getMD5ofFile;

public class PluginManager
{
    private final IProgressCallback progressCallback;
    private final UpdaterOptions options;
    private final ILogger logger;
    private final DownloadInfos downloadInfos;

    private boolean cursePluginLoaded = false;
    private boolean optifinePluginLoaded = false;

    public PluginManager(FlowUpdater updater)
    {
        this.progressCallback = updater.getCallback();
        this.options = updater.getUpdaterOptions();
        this.logger = updater.getLogger();
        this.downloadInfos = updater.getDownloadInfos();
    }

    public void loadPlugins(File dir) throws Exception
    {
        if (this.options.isEnableModsFromCurseForge())
            this.updatePlugin(new File(dir, "FUPlugins/CurseForgePlugin.jar"), "CurseForgePlugin", "CFP");

        if (this.options.isInstallOptifineAsMod())
            this.updatePlugin(new File(dir, "FUPlugins/OptifinePlugin.jar"), "OptifinePlugin", "OP");

        this.logger.debug("Configuring PLA...");
        this.configurePLA(dir);
    }

    public void updatePlugin(File out, String name, String alias) throws Exception
    {
        boolean flag = true;
        if (out.exists())
        {
            final String crc32 = IOUtils.getContent(new URL(String.format("https://flowarg.github.io/minecraft/launcher/%s.info", name))).trim();
            if (FileUtils.getCRC32(out) == Long.parseLong(crc32)) flag = false;
        }

        if (flag)
        {
            this.logger.debug(String.format("Downloading %s...", alias));
            IOUtils.download(this.logger, new URL(String.format("https://flowarg.github.io/minecraft/launcher/%s.jar", name)), out);
        }
    }

    public void loadCurseForgePlugin(File dir, ICurseFeaturesUser curseFeaturesUser)
    {
        final List<Object> allCurseMods = new ArrayList<>(curseFeaturesUser.getCurseMods().size());
        for (CurseFileInfos infos : curseFeaturesUser.getCurseMods())
        {
            if (!this.cursePluginLoaded)
            {
                try
                {
                    Class.forName("fr.flowarg.flowupdater.curseforgeplugin.CurseForgePlugin");
                    this.cursePluginLoaded = true;
                } catch (ClassNotFoundException e)
                {
                    this.cursePluginLoaded = false;
                    this.logger.err("Cannot install mods from CurseForge: CurseAPI is not loaded. Please, enable the 'enableModsFromCurseForge' updater option !");
                    break;
                }
            }

            try
            {
                final CurseForgePlugin curseForgePlugin = CurseForgePlugin.instance;
                final CurseMod mod = curseForgePlugin.getCurseMod(infos.getProjectID(), infos.getFileID());
                allCurseMods.add(mod);
                final File file = new File(dir, mod.getName());
                if(!file.exists() || !getMD5ofFile(file).equals(mod.getMd5()) || getFileSizeBytes(file) != mod.getLength())
                {
                    if (!mod.getMd5().contains("-"))
                    {
                        file.delete();
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
            try
            {
                Class.forName("fr.flowarg.flowupdater.curseforgeplugin.CurseForgePlugin");
                this.cursePluginLoaded = true;
                final CurseForgePlugin plugin = CurseForgePlugin.instance;
                final CurseModPack modPack = plugin.getCurseModPack(modPackInfos.getProjectID(), modPackInfos.getFileID(), modPackInfos.isInstallExtFiles());
                this.logger.info("Loading mod pack: " + modPack.getName() + " (" + modPack.getVersion() + ") by " + modPack.getAuthor() + '.');
                modPack.getMods().forEach(mod -> {
                    allCurseMods.add(mod);
                    try
                    {
                        final File file = new File(dir, mod.getName());
                        boolean flag = false;
                        for (String exclude : modPackInfos.getExcluded())
                        {
                            if (mod.getName().equalsIgnoreCase(exclude))
                            {
                                flag = !mod.isRequired();
                                break;
                            }
                        }
                        if(!flag && (!file.exists() || !getMD5ofFile(file).equals(mod.getMd5()) || getFileSizeBytes(file) != mod.getLength()))
                        {
                            if (!mod.getMd5().contains("-"))
                            {
                                file.delete();
                                this.downloadInfos.getCurseMods().add(mod);
                            }
                        }
                    } catch (NoSuchAlgorithmException | IOException e)
                    {
                        this.logger.printStackTrace(e);
                    }
                });
            } catch (ClassNotFoundException e)
            {
                this.cursePluginLoaded = false;
                this.logger.err("Cannot install mod pack from CurseForge: CurseAPI is not loaded. Please, enable the 'enableModsFromCurseForge' updater option !");
            }
        }

        curseFeaturesUser.setAllCurseMods(allCurseMods);
    }

    public void loadOptifinePlugin(File dir, AbstractForgeVersion forgeVersion)
    {
        try
        {
            Class.forName("fr.antoineok.flowupdater.optifineplugin.OptifinePlugin");
            this.optifinePluginLoaded = true;
            try
            {
                final OptifinePlugin optifinePlugin = OptifinePlugin.instance;
                final Optifine optifine = optifinePlugin.getOptifine(forgeVersion.getOptifine().getVersion(), forgeVersion.getOptifine().isPreview());
                this.downloadInfos.setOptifine(optifine);
            } catch (Exception e)
            {
                this.logger.printStackTrace(e);
            }
        } catch (ClassNotFoundException e)
        {
            this.optifinePluginLoaded = false;
            this.logger.err("Cannot install optifine: OptifinePlugin is not loaded. Please, enable the 'installOptifineAsMod' updater option !");
        }
    }

    public void configurePLA(File dir)
    {
        PluginLoaderAPI.setLogger(this.logger);
        PluginLoaderAPI.registerPluginLoader(new PluginLoader("FlowUpdater", new File(dir, "FUPlugins/"), PluginManager.class)).complete();
        PluginLoaderAPI.removeDefaultShutdownTrigger().complete();
        PluginLoaderAPI.ready(PluginManager.class).complete();
        while (!PluginLoaderAPI.finishedLoading()) ;
    }

    public void shutdown()
    {
        if (this.cursePluginLoaded) CurseForgePlugin.instance.shutdownOKHTTP();
        if (this.optifinePluginLoaded) OptifinePlugin.instance.shutdownOKHTTP();
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
