package fr.flowarg.flowupdater.download;

import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;

import java.nio.file.Path;

/**
 * This interface provides useful methods to implement to access to download and update status.
 */
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
     * @param info The {@link DownloadList.DownloadInfo} instance that contains all wanted information.
     */
    default void update(DownloadList.DownloadInfo info) {}

    /**
     * This method is called before {@link #update(DownloadList.DownloadInfo)} when a file is downloaded.
     * @param path the file downloaded.
     */
    default void onFileDownloaded(Path path) {}
}
