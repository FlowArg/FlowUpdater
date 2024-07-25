package fr.flowarg.flowupdater.versions.fabric;

import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.json.*;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The object that contains Fabric's stuff.
 */
public class FabricVersion extends FabricBasedVersion
{
    private static final String FABRIC_META_API = "https://meta.fabricmc.net/v2/versions/loader/%s/%s/profile/json";

    /**
     * Use {@link FabricVersionBuilder} to instantiate this class.
     * @param mods        {@link List<Mod>} to install.
     * @param curseMods   {@link List<CurseFileInfo>} to install.
     * @param fabricVersion to install.
     * @param fileDeleter {@link ModFileDeleter} used to clean up mods' dir.
     * @param curseModPackInfo {@link CurseModPackInfo} the mod pack you want to install.
     */
    FabricVersion(String fabricVersion, List<Mod> mods, List<CurseFileInfo> curseMods,
            List<ModrinthVersionInfo> modrinthMods, ModFileDeleter fileDeleter, CurseModPackInfo curseModPackInfo,
            ModrinthModPackInfo modrinthModPackInfo)
    {
        super(fabricVersion, mods, curseMods, modrinthMods, fileDeleter, curseModPackInfo, modrinthModPackInfo, FABRIC_META_API);
    }

    @Override
    public void attachFlowUpdater(@NotNull FlowUpdater flowUpdater)
    {
        super.attachFlowUpdater(flowUpdater);
        this.versionId = "fabric-loader-" + this.modLoaderVersion + "-" + this.vanilla.getName();
    }

    @Override
    public String name()
    {
        return "Fabric";
    }
}
