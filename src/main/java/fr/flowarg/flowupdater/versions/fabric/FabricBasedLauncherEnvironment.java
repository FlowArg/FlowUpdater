package fr.flowarg.flowupdater.versions.fabric;

import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.versions.ModLoaderLauncherEnvironment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;

public class FabricBasedLauncherEnvironment extends ModLoaderLauncherEnvironment
{
    private final ILogger logger;
    private final Path modLoaderDir;

    public FabricBasedLauncherEnvironment(List<String> command, Path tempDir, ILogger logger, Path modLoaderDir)
    {
        super(command, tempDir);
        this.logger = logger;
        this.modLoaderDir = modLoaderDir;
    }

    public Path getModLoaderDir()
    {
        return this.modLoaderDir;
    }

    public void launchInstaller() throws Exception
    {
        final ProcessBuilder processBuilder = new ProcessBuilder(this.getCommand());

        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        final Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        while ((line = reader.readLine()) != null) this.logger.info(line);

        reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        while ((line = reader.readLine()) != null) this.logger.info(line);

        process.waitFor();

        reader.close();
    }
}
