package fr.flowarg.flowupdater.versions;

import java.nio.file.Path;
import java.util.List;

/**
 * This class represents a process' environment with a working directory and the launch command.
 */
public class ModLoaderLauncherEnvironment
{
    private final List<String> command;
    private final Path tempDir;

    /**
     * Construct a new {@link ModLoaderLauncherEnvironment} object.
     * @param command the process' command.
     * @param tempDir the working directory.
     */
    public ModLoaderLauncherEnvironment(List<String> command, Path tempDir)
    {
        this.command = command;
        this.tempDir = tempDir;
    }

    /**
     * Get the process' command.
     * @return the process' command.
     */
    public List<String> getCommand()
    {
        return this.command;
    }

    /**
     * Get the working directory.
     * @return the working directory.
     */
    public Path getTempDir()
    {
        return this.tempDir;
    }
}
