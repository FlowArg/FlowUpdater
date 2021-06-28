package fr.flowarg.flowupdater.utils;

import fr.flowarg.flowlogger.ILogger;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

/**
 * @author antoineok https://github.com/antoineok Optimization by FlowArg https://github.com/FlowArg
 */
public class ArtifactsDownloader
{
    public static void downloadArtifacts(Path dir, String repositoryUrl, String id, ILogger logger)
    {
        final String[] parts = id.split(":");
        downloadArtifacts(dir, repositoryUrl, parts[0], parts[1], parts[2], logger);
    }

    public static void downloadArtifacts(Path dir, String repositoryUrl, String group, String name, String version, ILogger logger)
    {
        try
        {
            IOUtils.download(logger,
                             new URL(repositoryUrl + group.replace('.', '/') + '/' + name + '/' + version + '/' + String.format("%s-%s.jar", name, version)),
                             dir.resolve(group.replace(".", dir.getFileSystem().getSeparator())).resolve(name).resolve(version).resolve(String.format("%s-%s.jar", name, version)));
        } catch (IOException e)
        {
            logger.printStackTrace(e);
        }
    }
}
