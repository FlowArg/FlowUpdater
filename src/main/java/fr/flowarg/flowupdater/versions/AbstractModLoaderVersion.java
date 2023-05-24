package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.DownloadList;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.json.*;
import fr.flowarg.flowupdater.integrations.curseforgeintegration.ICurseFeaturesUser;
import fr.flowarg.flowupdater.integrations.modrinthintegration.IModrinthFeaturesUser;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class AbstractModLoaderVersion implements ICurseFeaturesUser, IModLoaderVersion, IModrinthFeaturesUser
{
    protected final List<Mod> mods;
    protected final List<CurseFileInfo> curseMods;
    protected final List<ModrinthVersionInfo> modrinthMods;
    protected final ModFileDeleter fileDeleter;
    protected final CurseModPackInfo curseModPackInfo;
    protected final ModrinthModPackInfo modrinthModPackInfo;

    protected String modLoaderVersion;
    protected ILogger logger;
    protected VanillaVersion vanilla;
    protected DownloadList downloadList;
    protected IProgressCallback callback;
    protected String javaPath;

    public AbstractModLoaderVersion(List<Mod> mods, String modLoaderVersion, List<CurseFileInfo> curseMods,
            List<ModrinthVersionInfo> modrinthMods, ModFileDeleter fileDeleter, CurseModPackInfo curseModPackInfo,
            ModrinthModPackInfo modrinthModPackInfo)
    {
        this.mods = mods;
        this.modLoaderVersion = modLoaderVersion;
        this.curseMods = curseMods;
        this.modrinthMods = modrinthMods;
        this.fileDeleter = fileDeleter;
        this.curseModPackInfo = curseModPackInfo;
        this.modrinthModPackInfo = modrinthModPackInfo;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void attachFlowUpdater(@NotNull FlowUpdater flowUpdater)
    {
        this.logger = flowUpdater.getLogger();
        this.vanilla = flowUpdater.getVanillaVersion();
        this.downloadList = flowUpdater.getDownloadList();
        this.callback = flowUpdater.getCallback();
        this.javaPath = flowUpdater.getUpdaterOptions().getJavaPath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Mod> getMods()
    {
        return this.mods;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DownloadList getDownloadList()
    {
        return this.downloadList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IProgressCallback getCallback()
    {
        return this.callback;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CurseFileInfo> getCurseMods()
    {
        return this.curseMods;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ModrinthVersionInfo> getModrinthMods()
    {
        return this.modrinthMods;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAllCurseMods(List<Mod> allCurseMods)
    {
        this.mods.addAll(allCurseMods);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CurseModPackInfo getCurseModPackInfo()
    {
        return this.curseModPackInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModrinthModPackInfo getModrinthModPackInfo()
    {
        return this.modrinthModPackInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAllModrinthMods(List<Mod> modrinthMods)
    {
        this.mods.addAll(modrinthMods);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ILogger getLogger()
    {
        return this.logger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getModLoaderVersion()
    {
        return this.modLoaderVersion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModFileDeleter getFileDeleter()
    {
        return this.fileDeleter;
    }
}
