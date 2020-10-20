package fr.flowarg.flowupdater.utils;

import fr.antoineok.flowupdater.optifineplugin.Optifine;
import fr.antoineok.flowupdater.optifineplugin.OptifinePlugin;
import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.curseforgeplugin.CurseForgePlugin;
import fr.flowarg.flowupdater.curseforgeplugin.CurseMod;
import fr.flowarg.flowupdater.download.DownloadInfos;
import fr.flowarg.flowupdater.download.json.CurseModInfos;
import fr.flowarg.flowupdater.versions.AbstractForgeVersion;
import fr.flowarg.pluginloaderapi.PluginLoaderAPI;
import fr.flowarg.pluginloaderapi.plugin.PluginLoader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static fr.flowarg.flowio.FileUtils.getFileSizeBytes;
import static fr.flowarg.flowio.FileUtils.getMD5ofFile;

public class PluginManager
{
    private final FlowUpdater updater;
    private final UpdaterOptions options;
    private final ILogger logger;
    private final DownloadInfos downloadInfos;

    private boolean cursePluginLoaded = false;
    private boolean optifinePluginLoaded = false;

    public PluginManager(FlowUpdater updater)
    {
        this.updater = updater;
        this.options = this.updater.getUpdaterOptions();
        this.logger = this.updater.getLogger();
        this.downloadInfos = this.updater.getDownloadInfos();
    }

    public void loadPlugins(File dir) throws Exception
    {
        if(this.options.isEnableModsFromCurseForge())
            this.updatePlugin(new File(dir, "FUPlugins/CurseForgePlugin.jar"), "CurseForgePlugin", "CFP");

        if(this.options.isInstallOptifineAsMod())
            this.updatePlugin(new File(dir, "FUPlugins/OptifinePlugin.jar"), "OptifinePlugin", "OP");

        this.logger.debug("Configuring PLA...");
        this.configurePLA(dir);
    }

    public void updatePlugin(File out, String name, String alias) throws Exception
    {
        boolean flag = true;
        if (out.exists())
        {
            final String crc32 = IOUtils.getContent(new URL(String.format("https://flowarg.github.io/minecraft/launcher/%s.info", name)));
            if(FileUtils.getCRC32(out) == Long.parseLong(crc32))
                flag = false;
        }

        if(flag)
        {
            this.logger.debug(String.format("Downloading %s...", alias));
            IOUtils.download(this.logger, new URL(String.format("https://flowarg.github.io/minecraft/launcher/%s.jar", name)), out);
        }
    }

    public void loadCurseMods(File dir, AbstractForgeVersion forgeVersion) throws Exception
    {
        final List<Object> allCurseMods = new ArrayList<>(forgeVersion.getCurseMods().size());
        for (CurseModInfos infos : forgeVersion.getCurseMods())
        {
            try
            {
                Class.forName("fr.flowarg.flowupdater.curseforgeplugin.CurseForgePlugin");
                this.cursePluginLoaded = true;
                final CurseForgePlugin curseForgePlugin = CurseForgePlugin.instance;
                final CurseMod mod = curseForgePlugin.getCurseMod(infos.getProjectID(), infos.getFileID());
                allCurseMods.add(mod);
                final File file = new File(dir, mod.getName());
                if (file.exists())
                {
                    if (!Objects.requireNonNull(getMD5ofFile(file)).equals(mod.getMd5()) || getFileSizeBytes(file) != mod.getLength())
                    {
                        file.delete();
                        this.downloadInfos.getCurseMods().add(mod);
                    }
                }
                else this.downloadInfos.getCurseMods().add(mod);
            }
            catch (ClassNotFoundException e)
            {
                this.cursePluginLoaded = false;
                this.logger.err("Cannot install mods from CurseForge: CurseAPI is not loaded. Please, enable the 'enableModsFromCurseForge' updater option !");
                break;
            }
        }

        forgeVersion.setAllCurseMods(allCurseMods);
    }

    public void loadOptifine(File dir, AbstractForgeVersion forgeVersion)
    {
        try
        {
            Class.forName("fr.antoineok.flowupdater.optifineplugin.OptifinePlugin");
            this.optifinePluginLoaded = true;
        }
        catch (ClassNotFoundException e)
        {
            this.optifinePluginLoaded = false;
            this.logger.err("Cannot install optifine: OptifinePlugin is not loaded. Please, enable the 'installOptifineAsMod' updater option !");
        }
        try
        {
            final OptifinePlugin optifinePlugin = OptifinePlugin.instance;
            final Optifine optifine = optifinePlugin.getOptifine(forgeVersion.getOptifine());
            this.downloadInfos.setOptifine(optifine);
        }
        catch (IOException e)
        {
            this.logger.printStackTrace(e);
        }
    }

    public void configurePLA(File dir)
    {
        PluginLoaderAPI.setLogger(this.logger);
        PluginLoaderAPI.registerPluginLoader(new PluginLoader("FlowUpdater", new File(dir, "FUPlugins/"), PluginManager.class)).complete();
        PluginLoaderAPI.removeDefaultShutdownTrigger().complete();
        PluginLoaderAPI.ready(PluginManager.class).complete();
    }

    public void shutdown()
    {
        if(this.cursePluginLoaded)
            CurseForgePlugin.instance.shutdownOKHTTP();
        if(this.optifinePluginLoaded)
            OptifinePlugin.instance.shutdownOKHTTP();
        this.cursePluginLoaded = false;
        this.optifinePluginLoaded = false;
    }

    public FlowUpdater getUpdater()
    {
        return this.updater;
    }

    public UpdaterOptions getOptions()
    {
        return this.options;
    }

    public ILogger getLogger()
    {
        return this.logger;
    }

    public DownloadInfos getDownloadInfos()
    {
        return this.downloadInfos;
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
