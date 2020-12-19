package fr.flowarg.flowupdater.download;

import fr.flowarg.flowupdater.FlowUpdater;

/**
 * Represent each step of a Minecraft Installation
 * @author flow
 */
public enum Step
{
    /** Prerequisites (like plugins loading) */
    PREREQUISITES,
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
    /** Install a forge version. Skipped if {@link FlowUpdater#getForgeVersion()} is null. */
    FORGE,
    /** Install a fabric version. Skipped if {@link FlowUpdater#getFabricVersion()} is null. */
    FABRIC,
    /** Download mods. Skipped if {@link FlowUpdater#getForgeVersion()} or {@link FlowUpdater#getFabricVersion()} is null. */
    MODS,
    /** Download other files. */
    EXTERNAL_FILES,
    /** Runs a list of runnable at the end of update. */
    POST_EXECUTIONS,
    /** All tasks are finished */
    END;
}
