package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowupdater.download.Step;
import fr.flowarg.flowupdater.download.json.*;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import fr.flowarg.flowupdater.utils.builderapi.BuilderArgument;
import fr.flowarg.flowupdater.utils.builderapi.BuilderException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * The object that contains Fabric's stuff.
 * @author antoineok <a href="https://github.com/antoineok">antoineok's GitHub</a>
 */
public class FabricVersion extends FabricBasedVersion
{
    private static final String FABRIC_INSTALLER_METADATA =
            "https://maven.fabricmc.net/net/fabricmc/fabric-installer/maven-metadata.xml";
    private static final String FABRIC_VERSION_METADATA =
            "https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml";
    private static final String FABRIC_BASE_INSTALLER = "https://maven.fabricmc.net/net/fabricmc/fabric-installer/%s/fabric-installer-%s.jar";

    /**
     * Use {@link FabricVersionBuilder} to instantiate this class.
     * @param mods        {@link List<Mod>} to install.
     * @param curseMods   {@link List<CurseFileInfo>} to install.
     * @param fabricVersion to install.
     * @param fileDeleter {@link ModFileDeleter} used to clean up mods' dir.
     * @param curseModPackInfo {@link CurseModPackInfo} the mod pack you want to install.
     */
    private FabricVersion(List<Mod> mods, List<CurseFileInfo> curseMods, List<ModrinthVersionInfo> modrinthMods,
            String fabricVersion, ModFileDeleter fileDeleter, CurseModPackInfo curseModPackInfo,
            ModrinthModPackInfo modrinthModPackInfo)
    {
        super(mods, fabricVersion, curseMods,
              modrinthMods, fileDeleter, curseModPackInfo,
              modrinthModPackInfo, IOUtils.getLatestArtifactVersion(FABRIC_INSTALLER_METADATA), FABRIC_BASE_INSTALLER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isModLoaderAlreadyInstalled(@NotNull Path installDir)
    {
        return Files.exists(
                installDir.resolve("libraries")
                        .resolve("net")
                        .resolve("fabricmc")
                        .resolve("fabric-loader")
                        .resolve(this.modLoaderVersion)
                        .resolve("fabric-loader-" + this.modLoaderVersion + ".jar"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FabricBasedLauncherEnvironment prepareModLoaderLauncher(@NotNull Path dirToInstall, InputStream stream) throws IOException
    {
        this.logger.info("Downloading fabric installer...");

        final Path tempDirPath = dirToInstall.resolve(".flowupdater");
        FileUtils.deleteDirectory(tempDirPath);
        final Path fabricPath = tempDirPath.resolve("tempfabric");
        final Path installPath = tempDirPath.resolve(String.format("fabric-installer-%s.jar", installerVersion));

        Files.createDirectories(tempDirPath);
        Files.createDirectories(fabricPath);

        Files.copy(stream, installPath, StandardCopyOption.REPLACE_EXISTING);
        return this.makeCommand(tempDirPath, installPath, fabricPath);
    }

    @Contract("_, _, _ -> new")
    private @NotNull FabricBasedLauncherEnvironment makeCommand(Path tempDir, @NotNull Path install, @NotNull Path fabric)
    {
        final List<String> command = new ArrayList<>();
        command.add(this.javaPath);
        command.add("-Xmx256M");
        command.add("-jar");
        command.add(install.toString());
        command.add("client");
        command.add("-dir");
        command.add(fabric.toString());
        command.add("-mcversion");
        command.add(this.vanilla.getName());
        command.add("-loader");
        command.add(this.modLoaderVersion);
        command.add("-noprofile");
        return new FabricBasedLauncherEnvironment(command, tempDir, fabric);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void install(final Path dirToInstall) throws Exception
    {
        this.callback.step(Step.MOD_LOADER);
        this.logger.info("Installing Fabric, version: " + this.modLoaderVersion + "...");
        this.checkModLoaderEnv(dirToInstall);

        try (BufferedInputStream stream = new BufferedInputStream(this.installerUrl.openStream()))
        {
            final FabricBasedLauncherEnvironment fabricLauncherEnvironment = this.prepareModLoaderLauncher(dirToInstall, stream);
            this.logger.info("Launching fabric installer...");
            fabricLauncherEnvironment.launchInstaller();

            final Path versionDir = fabricLauncherEnvironment.getModLoaderDir()
                    .resolve("versions")
                    .resolve(String.format("fabric-loader-%s-%s", this.modLoaderVersion, this.vanilla.getName()));

            this.parseAndMoveJson(dirToInstall, versionDir);

            this.logger.info("Successfully installed Fabric!");
            FileUtils.deleteDirectory(fabricLauncherEnvironment.getTempDir());
        } catch (Exception e)
        {
            this.logger.printStackTrace(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkModLoaderEnv(@NotNull Path dirToInstall) throws Exception
    {
        final Path fabricDirPath = dirToInstall
                .resolve("libraries")
                .resolve("net")
                .resolve("fabricmc")
                .resolve("fabric-loader");

        if (Files.exists(fabricDirPath))
            for (Path contained : FileUtils.list(fabricDirPath))
                if (!contained.getFileName().toString().contains(this.modLoaderVersion))
                    FileUtils.deleteDirectory(contained);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void installMods(Path modsDir) throws Exception
    {
        this.callback.step(Step.MODS);

        this.installAllMods(modsDir);
        this.fileDeleter.delete(modsDir, this.mods, null);
    }

    /**
     * Builder for {@link FabricVersion}.
     */
    public static class FabricVersionBuilder extends ModLoaderVersionBuilder<FabricVersion, FabricVersionBuilder>
    {
        private final BuilderArgument<String> fabricVersionArgument =
                new BuilderArgument<>("FabricVersion", () ->
                        IOUtils.getLatestArtifactVersion(FABRIC_VERSION_METADATA))
                        .optional();

        /**
         * @param fabricVersion the Fabric version you want to install
         * (don't use this function if you want to use the latest fabric version).
         * @return the builder.
         */
        public FabricVersionBuilder withFabricVersion(String fabricVersion)
        {
            this.fabricVersionArgument.set(fabricVersion);
            return this;
        }

        /**
         * Build a new {@link FabricVersion} instance with provided arguments.
         * @return the freshly created instance.
         * @throws BuilderException if an error occurred.
         */
        @Override
        public FabricVersion build() throws BuilderException
        {
            return new FabricVersion(
                    this.modsArgument.get(),
                    this.curseModsArgument.get(),
                    this.modrinthModsArgument.get(),
                    this.fabricVersionArgument.get(),
                    this.fileDeleterArgument.get(),
                    this.curseModPackArgument.get(),
                    this.modrinthPackArgument.get()
           );
        }
    }
}
