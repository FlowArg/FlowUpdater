package fr.flowarg.flowupdater.utils;

import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.ICurseFeaturesUser;
import fr.flowarg.flowupdater.versions.AbstractForgeVersion;

import java.nio.file.Path;

public class FallbackPluginManager extends PluginManager
{
    public FallbackPluginManager(FlowUpdater updater)
    {
        super(updater);
    }

    @Override
    public void loadCurseForgePlugin(Path dir, ICurseFeaturesUser curseFeaturesUser) {}

    @Override
    public void loadOptiFinePlugin(Path dir, AbstractForgeVersion forgeVersion) {}

    @Override
    public void shutdown() {}

    @Override
    public boolean isCursePluginLoaded()
    {
        return false;
    }

    @Override
    public boolean isOptiFinePluginLoaded()
    {
        return false;
    }
}
