package fr.flowarg.flowupdater.download;

import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;

public interface IProgressCallback
{
    /**
     * This method is called at {@link FlowUpdater} initialization.
     * @param logger {@link ILogger} of FlowUpdater instance.
     */
    void init(ILogger logger);

    /**
     * This method is called when a step started.
     * @param step Actual {@link Step}.
     */
    void step(Step step);

    /**
     * This method is called when a new file is downloaded.
     * @param downloaded Number of downloaded files.
     * @param max Total files to download.
     */
    void update(int downloaded, int max);
}
