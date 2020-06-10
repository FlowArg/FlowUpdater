package fr.flowarg.flowupdater.minecraft.versions.download;

public interface IProgressCallback
{
	/**
	 * This method is called at {@link FlowArgMinecraftUpdater} initialization.
	 */
	void init();
	/**
	 * This method is called when a step started.
	 * @param step Actual step.
	 */
	void step(Step step);
	/**
	 * This method is called when a new file is downloaded.
	 * @param downloaded Number of downloaded files.
	 * @param max Total files to download.
	 */
	void update(int downloaded, int max);
}
