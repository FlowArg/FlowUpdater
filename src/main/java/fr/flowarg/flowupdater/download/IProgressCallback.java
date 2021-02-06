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
     * @param downloaded Number of downloaded/checked bytes.
     * @param max Total bytes to download/check.
     */
    void update(long downloaded, long max);
}
