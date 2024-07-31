package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.DownloadList;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.Step;
import fr.flowarg.flowupdater.download.json.*;
import fr.flowarg.flowupdater.integrations.curseforgeintegration.ICurseForgeCompatible;
import fr.flowarg.flowupdater.integrations.modrinthintegration.IModrinthCompatible;
import fr.flowarg.flowupdater.integrations.modrinthintegration.ModrinthModPack;
import fr.flowarg.flowupdater.integrations.optifineintegration.IOptiFineCompatible;
import fr.flowarg.flowupdater.integrations.optifineintegration.OptiFine;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public abstract class AbstractModLoaderVersion implements IModLoaderVersion, ICurseForgeCompatible, IModrinthCompatible, IOptiFineCompatible
{
    protected final List<Mod> mods;
    protected final List<CurseFileInfo> curseMods;
    protected final List<ModrinthVersionInfo> modrinthMods;
    protected final ModFileDeleter fileDeleter;
    protected final CurseModPackInfo curseModPackInfo;
    protected final ModrinthModPackInfo modrinthModPackInfo;
    protected final OptiFineInfo optiFineInfo;

    protected String modLoaderVersion;
    protected ILogger logger;
    protected VanillaVersion vanilla;
    protected DownloadList downloadList;
    protected IProgressCallback callback;
    protected String javaPath;
    protected ModrinthModPack modrinthModPack;

    public AbstractModLoaderVersion(String modLoaderVersion, List<Mod> mods, List<CurseFileInfo> curseMods,
            List<ModrinthVersionInfo> modrinthMods, ModFileDeleter fileDeleter, CurseModPackInfo curseModPackInfo,
            ModrinthModPackInfo modrinthModPackInfo, OptiFineInfo optiFineInfo)
    {
        this.modLoaderVersion = modLoaderVersion;
        this.mods = mods;
        this.curseMods = curseMods;
        this.modrinthMods = modrinthMods;
        this.fileDeleter = fileDeleter;
        this.curseModPackInfo = curseModPackInfo;
        this.modrinthModPackInfo = modrinthModPackInfo;
        this.optiFineInfo = optiFineInfo;
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
    public OptiFineInfo getOptiFineInfo()
    {
        return this.optiFineInfo;
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

    @Override
    public void setModrinthModPack(ModrinthModPack modrinthModPack)
    {
        this.modrinthModPack = modrinthModPack;
    }

    @Override
    public ModrinthModPack getModrinthModPack()
    {
        return this.modrinthModPack;
    }

    @Override
    public void installMods(@NotNull Path modsDir) throws Exception
    {
        this.callback.step(Step.MODS);
        this.installAllMods(modsDir);

        final OptiFine ofObj = this.downloadList.getOptiFine();

        if(ofObj != null)
        {
            try
            {
                final Path optiFineFilePath = modsDir.resolve(ofObj.getName());

                if (Files.notExists(optiFineFilePath) || Files.size(optiFineFilePath) != ofObj.getSize())
                    IOUtils.copy(this.logger, modsDir.getParent().resolve(".op").resolve(ofObj.getName()), optiFineFilePath);
            } catch (Exception e)
            {
                this.logger.printStackTrace(e);
            }
            this.downloadList.incrementDownloaded(ofObj.getSize());
            this.callback.update(this.downloadList.getDownloadInfo());
        }

        this.fileDeleter.delete(this.logger, modsDir, this.mods, ofObj, this.modrinthModPack);
    }
}
