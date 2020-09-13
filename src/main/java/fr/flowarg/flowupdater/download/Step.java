package fr.flowarg.flowupdater.download;

import fr.flowarg.flowupdater.FlowUpdater;

/**
 * Represent each step of an Minecraft Installation
 * @author flow
 */
public enum Step
{
	READ, /** JSON reading */
	DL_LIBS, /** Download libraries */
	DL_ASSETS, /** Download assets */
	EXTRACT_NATIVES, /** Extract natives */
	FORGE, /** Install a forge version. Skipped if {@link FlowUpdater#getForgeVersion()} is null. */
	MODS, /** Download mods. Skipped if {@link FlowUpdater#getForgeVersion()} is null. */
	EXTERNAL_FILES, /** Download other files. */
	POST_EXECUTIONS, /** Runs a list of runnable at the end of update. */
	END; /** All tasks are finished */
}