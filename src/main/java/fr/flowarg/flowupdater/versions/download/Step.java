package fr.flowarg.flowupdater.versions.download;

import fr.flowarg.flowupdater.FlowUpdater;

/**
 * Represent each step of an Minecraft Installation
 * @author flow
 */
public enum Step
{
	READ, /** JSON */
	DL_LIBS, /** Download libraries */
	DL_ASSETS, /** Download assets */
	EXTRACT_NATIVES, /** Extract natives */
	FORGE, /** Skipped if {@link FlowUpdater#getForgeVersion()} isn't null. */
	END; /** All tasks are finished */
}