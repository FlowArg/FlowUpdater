package fr.flowarg.flowupdater.utils;

import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;

public class VersionChecker
{
    public static void run(ILogger logger)
    {
        new Thread(() -> {
            final String version = IOUtils.getLatestArtifactVersion("https://repo1.maven.org/maven2/fr/flowarg/flowupdater/maven-metadata.xml");

            if (version == null)
            {
                logger.err("Couldn't get the latest version of FlowUpdater.");
                logger.err("Maybe the maven repository is down? Or your internet connection sucks?");
                return;
            }

            final int compare = Version.gen(FlowUpdater.FU_VERSION).compareTo(Version.gen(version));

            if(compare > 0)
            {
                logger.info("You're running on an unpublished version of FlowUpdater. Are you in a dev environment?");
                return;
            }

            if(compare < 0)
                logger.warn(String.format("Detected a new version of FlowUpdater (%s). You should update!", version));
        }).start();
    }
}
