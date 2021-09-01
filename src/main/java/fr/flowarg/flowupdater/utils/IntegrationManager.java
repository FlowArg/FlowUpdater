package fr.flowarg.flowupdater.utils;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.DownloadList;
import fr.flowarg.flowupdater.download.ICurseFeaturesUser;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.Step;
import fr.flowarg.flowupdater.download.json.CurseFileInfo;
import fr.flowarg.flowupdater.download.json.CurseModPackInfo;
import fr.flowarg.flowupdater.integrations.curseforgeintegration.CurseForgeIntegration;
import fr.flowarg.flowupdater.integrations.curseforgeintegration.CurseMod;
import fr.flowarg.flowupdater.integrations.curseforgeintegration.CurseModPack;
import fr.flowarg.flowupdater.integrations.optifineintegration.OptiFine;
import fr.flowarg.flowupdater.integrations.optifineintegration.OptiFineIntegration;
import fr.flowarg.flowupdater.versions.AbstractForgeVersion;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class IntegrationManager
{
    private final IProgressCallback progressCallback;
    private final ILogger logger;
    private final DownloadList downloadList;

    public IntegrationManager(FlowUpdater updater)
    {
        this.progressCallback = updater.getCallback();
        this.logger = updater.getLogger();
        this.downloadList = updater.getDownloadList();
    }

    public void loadCurseForgeIntegration(Path dir, ICurseFeaturesUser curseFeaturesUser)
    {
        try
        {
            final CurseForgeIntegration curseForgeIntegration = new CurseForgeIntegration(this.logger, dir.getParent().resolve(".cfp"));

            final List<CurseMod> allCurseMods = new ArrayList<>(curseFeaturesUser.getCurseMods().size());

            for (CurseFileInfo info : curseFeaturesUser.getCurseMods())
            {
                final CurseMod mod = curseForgeIntegration.getCurseMod(info.getProjectID(), info.getFileID());
                allCurseMods.add(mod);

                final Path filePath = dir.resolve(mod.getName());

                if(Files.exists(filePath) && FileUtils.getMD5(filePath).equals(mod.getMd5()) && FileUtils.getFileSizeBytes(filePath) == mod.getLength()) continue;

                if (mod.getMd5().contains("-")) continue;

                Files.deleteIfExists(filePath);
                this.downloadList.getCurseMods().add(mod);
            }

            final CurseModPackInfo modPackInfo = curseFeaturesUser.getModPackInfo();

            if (modPackInfo == null)
            {
                curseFeaturesUser.setAllCurseMods(allCurseMods);
                return;
            }

            this.progressCallback.step(Step.MOD_PACK);
            final CurseModPack modPack = curseForgeIntegration.getCurseModPack(modPackInfo.getProjectID(), modPackInfo.getFileID(), modPackInfo.isInstallExtFiles());
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

    public void loadOptiFineIntegration(Path dir, AbstractForgeVersion forgeVersion)
    {
        if(forgeVersion.getOptiFineInfo() == null) return;

        try
        {
            final OptiFineIntegration optifineIntegration = new OptiFineIntegration(this.logger, dir.getParent().resolve(".op"));
            final OptiFine optifine = optifineIntegration.getOptiFine(forgeVersion.getOptiFineInfo().getVersion(), forgeVersion.getOptiFineInfo().isPreview());
            this.downloadList.setOptiFine(optifine);
        } catch (Exception e)
        {
            this.logger.printStackTrace(e);
        }
    }

    public ILogger getLogger()
    {
        return this.logger;
    }
}
