package fr.flowarg.flowupdater.download;

import fr.flowarg.flowupdater.FlowUpdater;

/**
 * Represent each step of a Minecraft Installation
 * @author flow
 */
public enum Step
{
    /** Integration loading */
    INTEGRATION,
    /** ModPack preparation */
    MOD_PACK,
    /** JSON reading */
    READ,
    /** Download libraries */
    DL_LIBS,
    /** Download assets */
    DL_ASSETS,
    /** Extract natives */
    EXTRACT_NATIVES,
    /** Install a mod loader version. Skipped if {@link FlowUpdater#getModLoaderVersion()} is null. */
    MOD_LOADER,
    /** Download mods. Skipped if {@link FlowUpdater#getModLoaderVersion()} is null. */
    MODS,
    /** Download other files. */
    EXTERNAL_FILES,
    /** Runs a list of runnable at the end of update. */
    POST_EXECUTIONS,
    /** All tasks are finished */
    END
}
