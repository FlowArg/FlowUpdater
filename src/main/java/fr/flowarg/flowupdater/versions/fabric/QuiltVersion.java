package fr.flowarg.flowupdater.versions.fabric;

import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.json.*;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The object that contains Quilt's stuff.
 */
public class QuiltVersion extends FabricBasedVersion
{
    private static final String QUILT_META_API = "https://meta.quiltmc.org/v3/versions/loader/%s/%s/profile/json";

    /**
     * Use {@link QuiltVersionBuilder} to instantiate this class.
     * @param mods        {@link List<Mod>} to install.
     * @param curseMods   {@link List<CurseFileInfo>} to install.
     * @param quiltVersion to install.
     * @param fileDeleter {@link ModFileDeleter} used to clean up mods' dir.
     * @param curseModPackInfo {@link CurseModPackInfo} the mod pack you want to install.
     */
    QuiltVersion(String quiltVersion, List<Mod> mods, List<CurseFileInfo> curseMods,
            List<ModrinthVersionInfo> modrinthMods, ModFileDeleter fileDeleter, CurseModPackInfo curseModPackInfo,
            ModrinthModPackInfo modrinthModPackInfo, OptiFineInfo optiFineInfo)
    {
        super(quiltVersion, mods, curseMods, modrinthMods, fileDeleter, curseModPackInfo, modrinthModPackInfo, optiFineInfo, QUILT_META_API);
    }

    @Override
    public void attachFlowUpdater(@NotNull FlowUpdater flowUpdater)
    {
        super.attachFlowUpdater(flowUpdater);
        this.versionId = "quilt-loader-" + this.modLoaderVersion + "-" + this.vanilla.getName();
    }

    @Override
    public String name()
    {
        return "Quilt";
    }
}
