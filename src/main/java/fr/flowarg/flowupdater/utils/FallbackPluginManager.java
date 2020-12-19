package fr.flowarg.flowupdater.utils;

import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.ICurseFeaturesUser;
import fr.flowarg.flowupdater.versions.AbstractForgeVersion;

import java.io.File;

public class FallbackPluginManager extends PluginManager
{
    public FallbackPluginManager(FlowUpdater updater)
    {
        super(updater);
    }

    @Override
    public void loadPlugins(File dir) {}

    @Override
    public void updatePlugin(File out, String name, String alias) {}

    @Override
    public void loadCurseForgePlugin(File dir, ICurseFeaturesUser curseFeaturesUser) {}

    @Override
    public void loadOptifinePlugin(File dir, AbstractForgeVersion forgeVersion) {}

    @Override
    public void configurePLA(File dir) {}

    @Override
    public void shutdown() {}

    @Override
    public boolean isCursePluginLoaded()
    {
        return false;
    }

    @Override
    public boolean isOptifinePluginLoaded()
    {
        return false;
    }
}
