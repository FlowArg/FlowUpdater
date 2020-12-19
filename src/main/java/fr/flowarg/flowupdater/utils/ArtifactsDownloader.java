package fr.flowarg.flowupdater.utils;

import fr.flowarg.flowlogger.ILogger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * @author antoineok <https://github.com/antoineok>
 */
public class ArtifactsDownloader
{
    public static void downloadArtifacts(File dir, String repositoryUrl, String id, ILogger logger){
        String[] parts = id.split(":");
        downloadArtifacts(dir, repositoryUrl, parts[0], parts[1], parts[2], logger);
    }

    public static void downloadArtifacts(File dir, String repositoryUrl, String group, String name, String version, ILogger logger){
        File groupDir = new File(dir, group.replace('.', File.separatorChar));
        if(!groupDir.exists())
            groupDir.mkdirs();
        String artifactLocation = repositoryUrl + group.replace('.', '/');
        File artifactDir = new File(groupDir, name);
        if(!artifactDir.exists())
            artifactDir.mkdirs();
        artifactLocation += '/' + name;
        File versionDir = new File(artifactDir, version);
        if(!versionDir.exists())
            versionDir.mkdirs();
        artifactLocation += '/' + version;
        String fileName = String.format("%s-%s.jar", name, version);
        artifactLocation += '/' + fileName;
        try {
            logger.info(String.format("Downloading %s from %s", fileName, artifactLocation));
            Files.copy(new URL(artifactLocation).openStream(), new File(versionDir, fileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
