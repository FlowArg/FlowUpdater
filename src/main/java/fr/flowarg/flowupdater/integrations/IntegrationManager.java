package fr.flowarg.flowupdater.integrations;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.DownloadList;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.Step;
import fr.flowarg.flowupdater.download.json.CurseFileInfo;
import fr.flowarg.flowupdater.download.json.CurseModPackInfo;
import fr.flowarg.flowupdater.download.json.OptiFineInfo;
import fr.flowarg.flowupdater.integrations.curseforgeintegration.CurseForgeIntegration;
import fr.flowarg.flowupdater.integrations.curseforgeintegration.CurseMod;
import fr.flowarg.flowupdater.integrations.curseforgeintegration.CurseModPack;
import fr.flowarg.flowupdater.integrations.curseforgeintegration.ICurseFeaturesUser;
import fr.flowarg.flowupdater.integrations.optifineintegration.OptiFine;
import fr.flowarg.flowupdater.integrations.optifineintegration.OptiFineIntegration;
import fr.flowarg.flowupdater.versions.AbstractForgeVersion;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The integration manager loads integrations stuff at the startup of FlowUpdater.
 */
public class IntegrationManager
{
    private final IProgressCallback progressCallback;
    private final ILogger logger;
    private final DownloadList downloadList;

    /**
     * Construct a new Integration Manager.
     * @param updater a {@link FlowUpdater} instance.
     */
    public IntegrationManager(@NotNull FlowUpdater updater)
    {
        this.progressCallback = updater.getCallback();
        this.logger = updater.getLogger();
        this.downloadList = updater.getDownloadList();
    }

    /**
     * This method loads the CurseForge integration and fetches some data.
     * @param dir the installation directory.
     * @param curseFeaturesUser a version that accepts CurseForge's features stuff.
     */
    public void loadCurseForgeIntegration(Path dir, ICurseFeaturesUser curseFeaturesUser)
    {
        this.progressCallback.step(Step.INTEGRATION);
        try
        {
            final CurseModPackInfo modPackInfo = curseFeaturesUser.getModPackInfo();
            final List<CurseMod> allCurseMods = new ArrayList<>();

            if(curseFeaturesUser.getCurseMods().isEmpty() && modPackInfo == null)
            {
                curseFeaturesUser.setAllCurseMods(allCurseMods);
                return;
            }

            final CurseForgeIntegration curseForgeIntegration = new CurseForgeIntegration(this.logger, dir.getParent().resolve(".cfp"));

            for (CurseFileInfo info : curseFeaturesUser.getCurseMods())
            {
                final CurseMod mod = curseForgeIntegration.getCurseMod(info.getProjectID(), info.getFileID());
                allCurseMods.add(mod);

                final Path filePath = dir.resolve(mod.getName());

                if(Files.exists(filePath)
                        && FileUtils.getFileSizeBytes(filePath) == mod.getLength()
                        && (FileUtils.getMD5(filePath).equalsIgnoreCase(mod.getMd5())
                        || mod.getMd5().contains("-")))
                    continue;

                Files.deleteIfExists(filePath);
                this.downloadList.getCurseMods().add(mod);
            }

            if (modPackInfo == null)
            {
                curseFeaturesUser.setAllCurseMods(allCurseMods);
                return;
            }

            this.progressCallback.step(Step.MOD_PACK);
            final CurseModPack modPack = curseForgeIntegration.getCurseModPack(modPackInfo);
            this.logger.info(String.format("Loading mod pack: %s (%s) by %s.", modPack.getName(), modPack.getVersion(), modPack.getAuthor()));
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

                    if(flag) return;

                    if(Files.exists(filePath)
                            && FileUtils.getFileSizeBytes(filePath) == mod.getLength()
                            && (FileUtils.getMD5(filePath).equalsIgnoreCase(mod.getMd5())
                            || mod.getMd5().contains("-")))
                        return;

                    Files.deleteIfExists(filePath);
                    this.downloadList.getCurseMods().add(mod);
                } catch (Exception e)
                {
                    this.logger.printStackTrace(e);
                }
            });

            curseFeaturesUser.setAllCurseMods(allCurseMods);
        }
        catch (Exception e)
        {
            this.logger.printStackTrace(e);
        }
    }

    /**
     * This method loads the OptiFine integration and fetches OptiFine data.
     * @param dir the installation directory.
     * @param forgeVersion the current Forge version.
     */
    public void loadOptiFineIntegration(Path dir, @NotNull AbstractForgeVersion forgeVersion)
    {
        final OptiFineInfo info = forgeVersion.getOptiFineInfo();
        if(info == null) return;

        try
        {
            final OptiFineIntegration optifineIntegration = new OptiFineIntegration(this.logger, dir.getParent().resolve(".op"));
            final OptiFine optifine = optifineIntegration.getOptiFine(info.getVersion(), info.isPreview());
            this.downloadList.setOptiFine(optifine);
        } catch (Exception e)
        {
            this.logger.printStackTrace(e);
        }
    }
}