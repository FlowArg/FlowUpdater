package fr.flowarg.flowupdater.utils;

import fr.flowarg.flowlogger.ILogger;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author antoineok https://github.com/antoineok
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
            final Path groupDirPath = Paths.get(dir.toString(), group.replace(".", dir.getFileSystem().getSeparator()));
            final Path artifactDirPath = Paths.get(groupDirPath.toString(), name);
            final Path versionDirPath = Paths.get(artifactDirPath.toString(), version);
            final String fileName = String.format("%s-%s.jar", name, version);
            IOUtils.download(logger, new URL(repositoryUrl + group.replace('.', '/') + '/' + name + '/' + version + '/' + String.format("%s-%s.jar", name, version)), Paths.get(versionDirPath.toString(), fileName));
        } catch (IOException e)
        {
            logger.printStackTrace(e);
        }
    }
}
