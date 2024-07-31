package fr.flowarg.flowupdater.integrations;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.DownloadList;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.Step;
import fr.flowarg.flowupdater.download.json.*;
import fr.flowarg.flowupdater.integrations.curseforgeintegration.CurseForgeIntegration;
import fr.flowarg.flowupdater.integrations.curseforgeintegration.CurseModPack;
import fr.flowarg.flowupdater.integrations.curseforgeintegration.ICurseForgeCompatible;
import fr.flowarg.flowupdater.integrations.modrinthintegration.IModrinthCompatible;
import fr.flowarg.flowupdater.integrations.modrinthintegration.ModrinthIntegration;
import fr.flowarg.flowupdater.integrations.modrinthintegration.ModrinthModPack;
import fr.flowarg.flowupdater.integrations.optifineintegration.IOptiFineCompatible;
import fr.flowarg.flowupdater.integrations.optifineintegration.OptiFine;
import fr.flowarg.flowupdater.integrations.optifineintegration.OptiFineIntegration;
import fr.flowarg.flowupdater.utils.FlowUpdaterException;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The integration manager loads integration's stuff at the startup of FlowUpdater.
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
     * @param curseForgeCompatible a version that accepts CurseForge's feature stuff.
     */
    public void loadCurseForgeIntegration(Path dir, ICurseForgeCompatible curseForgeCompatible)
    {
        this.progressCallback.step(Step.INTEGRATION);
        try
        {
            final CurseModPackInfo modPackInfo = curseForgeCompatible.getCurseModPackInfo();
            final List<Mod> allCurseMods = new ArrayList<>();

            if(curseForgeCompatible.getCurseMods().isEmpty() && modPackInfo == null)
            {
                curseForgeCompatible.setAllCurseMods(allCurseMods);
                return;
            }

            final CurseForgeIntegration curseForgeIntegration = new CurseForgeIntegration(this.logger, dir.getParent().resolve(".cfp"));

            for (CurseFileInfo info : curseForgeCompatible.getCurseMods())
            {
                try {
                    final Mod mod = curseForgeIntegration.fetchMod(info);

                    if(mod == null)
                        break;

                    this.checkMod(mod, allCurseMods, dir);
                }
                catch (Exception e)
                {
                    this.logger.printStackTrace(e);
                }
            }

            if (modPackInfo == null)
            {
                curseForgeCompatible.setAllCurseMods(allCurseMods);
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
                            && Files.size(filePath) == mod.getSize()
                            && (mod.getSha1().isEmpty() || FileUtils.getSHA1(filePath).equalsIgnoreCase(mod.getSha1())))
                        return;

                    Files.deleteIfExists(filePath);
                    this.downloadList.getMods().add(mod);
                } catch (Exception e)
                {
                    this.logger.printStackTrace(e);
                }
            });

            curseForgeCompatible.setAllCurseMods(allCurseMods);
        }
        catch (Exception e)
        {
            throw new FlowUpdaterException(e);
        }
    }

    public void loadModrinthIntegration(Path dir, IModrinthCompatible modrinthCompatible)
    {
        try
        {
            final ModrinthModPackInfo modPackInfo = modrinthCompatible.getModrinthModPackInfo();
            final List<Mod> allModrinthMods = new ArrayList<>();

            if(modrinthCompatible.getModrinthMods().isEmpty() && modPackInfo == null)
            {
                modrinthCompatible.setAllModrinthMods(allModrinthMods);
                return;
            }

            final ModrinthIntegration modrinthIntegration = new ModrinthIntegration(this.logger, dir.getParent().resolve(".modrinth"));

            for (ModrinthVersionInfo info : modrinthCompatible.getModrinthMods())
            {
                final Mod mod = modrinthIntegration.fetchMod(info);
                this.checkMod(mod, allModrinthMods, dir);
            }

            if (modPackInfo == null)
            {
                modrinthCompatible.setAllModrinthMods(allModrinthMods);
                return;
            }

            this.progressCallback.step(Step.MOD_PACK);
            final ModrinthModPack modPack = modrinthIntegration.getCurseModPack(modPackInfo);
            this.logger.info(String.format("Loading mod pack: %s (%s).", modPack.getName(), modPack.getVersion()));
            modrinthCompatible.setModrinthModPack(modPack);

            for (Mod mod : modPack.getMods())
                this.checkMod(mod, allModrinthMods, dir);

            modrinthCompatible.setAllModrinthMods(allModrinthMods);
        }
        catch (Exception e)
        {
            throw new FlowUpdaterException(e);
        }
    }

    /**
     * This method loads the OptiFine integration and fetches OptiFine data.
     * @param dir the installation directory.
     * @param optiFineCompatible the current Forge version.
     */
    public void loadOptiFineIntegration(Path dir, @NotNull IOptiFineCompatible optiFineCompatible)
    {
        final OptiFineInfo info = optiFineCompatible.getOptiFineInfo();

        if(info == null)
            return;

        try
        {
            final OptiFineIntegration optifineIntegration = new OptiFineIntegration(this.logger, dir.getParent().resolve(".op"));
            final OptiFine optifine = optifineIntegration.getOptiFine(info.getVersion(), info.isPreview());
            this.downloadList.setOptiFine(optifine);
        } catch (Exception e)
        {
            throw new FlowUpdaterException(e);
        }
    }

    private void checkMod(Mod mod, @NotNull List<Mod> allMods, @NotNull Path dir) throws Exception
    {
        allMods.add(mod);

        final Path filePath = dir.resolve(mod.getName());

        if(Files.exists(filePath)
                && Files.size(filePath) == mod.getSize()
                && (mod.getSha1().isEmpty() || FileUtils.getSHA1(filePath).equalsIgnoreCase(mod.getSha1())))
            return;

        Files.deleteIfExists(filePath);
        this.downloadList.getMods().add(mod);
    }
}
