package fr.flowarg.flowupdater.download;

import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;

import java.io.File;
import java.nio.file.Path;

public interface IProgressCallback
{
    /**
     * This method is called at {@link FlowUpdater} initialization.
     * @param logger {@link ILogger} of FlowUpdater instance.
     */
    default void init(ILogger logger) {}

    /**
     * This method is called when a step started.
     * @param step Actual {@link Step}.
     */
    default void step(Step step) {}

    /**
     * This method is called when a new file is downloaded.
     * @param downloaded Number of downloaded/checked bytes.
     * @param max Total bytes to download/check.
     */
    default void update(long downloaded, long max) {}

    /**
     * This method is called before {@link #update(long, long)} when a file is downloaded.
     * @param path the file downloaded.
     */
    default void onFileDownloaded(Path path) {}
}
