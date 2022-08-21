package fr.flowarg.flowupdater.utils;

import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import org.jetbrains.annotations.NotNull;

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

    private static class Version implements Comparable<Version>
    {
        private final int major;
        private final int minor;
        private final int patch;

        private Version(int major, int minor, int patch)
        {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }

        private static @NotNull Version gen(@NotNull String version)
        {
            final String[] parts = version.split("\\.");

            return new Version(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        }

        @Override
        public int compareTo(@NotNull VersionChecker.Version o)
        {
            final int majorCompare = Integer.compare(this.major, o.major);
            final int minorCompare = Integer.compare(this.minor, o.minor);
            return majorCompare != 0 ? majorCompare : minorCompare != 0 ? minorCompare : Integer.compare(this.patch, o.patch);
        }
    }
}
