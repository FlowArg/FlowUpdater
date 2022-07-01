package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowupdater.download.Step;
import fr.flowarg.flowupdater.download.json.*;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import fr.flowarg.flowupdater.utils.builderapi.BuilderArgument;
import fr.flowarg.flowupdater.utils.builderapi.BuilderException;
import fr.flowarg.flowupdater.utils.builderapi.IBuilder;
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
 * The object that contains Quilt's stuff.
 */
public class QuiltVersion extends FabricBasedVersion
{
    private static final String QUILT_INSTALLER_METADATA =
            "https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-installer/maven-metadata.xml";
    private static final String QUILT_VERSION_METADATA =
            "https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-loader/maven-metadata.xml";
    private static final String QUILT_BASE_INSTALLER = "https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-installer/%s/quilt-installer-%s.jar";

    /**
     * Use {@link QuiltVersionBuilder} to instantiate this class.
     * @param mods        {@link List<Mod>} to install.
     * @param curseMods   {@link List<CurseFileInfo>} to install.
     * @param quiltVersion to install.
     * @param fileDeleter {@link ModFileDeleter} used to clean up mods' dir.
     * @param curseModPackInfo {@link CurseModPackInfo} the mod pack you want to install.
     */
    private QuiltVersion(List<Mod> mods, List<CurseFileInfo> curseMods, List<ModrinthVersionInfo> modrinthMods,
            String quiltVersion, ModFileDeleter fileDeleter, CurseModPackInfo curseModPackInfo,
            ModrinthModPackInfo modrinthModPackInfo)
    {
        super(mods, quiltVersion, curseMods,
              modrinthMods, fileDeleter, curseModPackInfo,
              modrinthModPackInfo, IOUtils.getLatestArtifactVersion(QUILT_INSTALLER_METADATA), QUILT_BASE_INSTALLER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isModLoaderAlreadyInstalled(@NotNull Path installDir)
    {
        return Files.exists(
                installDir.resolve("libraries")
                        .resolve("org")
                        .resolve("quiltmc")
                        .resolve("quilt-loader")
                        .resolve(this.modLoaderVersion)
                        .resolve("quilt-loader-" + this.modLoaderVersion + ".jar"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FabricBasedLauncherEnvironment prepareModLoaderLauncher(@NotNull Path dirToInstall, InputStream stream) throws IOException
    {
        this.logger.info("Downloading quilt installer...");

        final Path tempDirPath = dirToInstall.resolve(".flowupdater");
        FileUtils.deleteDirectory(tempDirPath);
        final Path quiltPath = tempDirPath.resolve("tempquilt");
        final Path installPath = tempDirPath.resolve(String.format("quilt-installer-%s.jar", installerVersion));

        Files.createDirectories(tempDirPath);
        Files.createDirectories(quiltPath);

        Files.copy(stream, installPath, StandardCopyOption.REPLACE_EXISTING);
        return this.makeCommand(tempDirPath, installPath, quiltPath);
    }

    @Contract("_, _, _ -> new")
    private @NotNull FabricBasedLauncherEnvironment makeCommand(Path tempDir, @NotNull Path install, @NotNull Path quilt)
    {
        final List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-Xmx256M");
        command.add("-jar");
        command.add(install.toString());
        command.add("install");
        command.add("client");
        command.add(this.vanilla.getName());
        command.add(this.modLoaderVersion);
        command.add("--no-profile");
        command.add("--install-dir=" + quilt);
        return new FabricBasedLauncherEnvironment(command, tempDir, quilt);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void install(final Path dirToInstall) throws Exception
    {
        this.callback.step(Step.MOD_LOADER);
        this.logger.info("Installing Quilt, version: " + this.modLoaderVersion + "...");
        this.checkModLoaderEnv(dirToInstall);

        try (BufferedInputStream stream = new BufferedInputStream(this.installerUrl.openStream()))
        {
            final FabricBasedLauncherEnvironment quiltLauncherEnvironment = this.prepareModLoaderLauncher(dirToInstall, stream);
            this.logger.info("Launching quilt installer...");
            quiltLauncherEnvironment.launchInstaller();

            final Path versionDir = quiltLauncherEnvironment.getModLoaderDir()
                    .resolve("versions")
                    .resolve(String.format("quilt-loader-%s-%s", this.modLoaderVersion, this.vanilla.getName()));

            this.parseAndMoveJson(dirToInstall, versionDir);

            this.logger.info("Successfully installed Quilt!");
            FileUtils.deleteDirectory(quiltLauncherEnvironment.getTempDir());
        } catch (Exception e)
        {
            this.logger.printStackTrace(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkModLoaderEnv(@NotNull Path dirToInstall) throws Exception
    {
        boolean result= false;
        final Path quiltDirPath = dirToInstall
                .resolve("libraries")
                .resolve("org")
                .resolve("quiltmc")
                .resolve("quilt-loader");
        if (Files.exists(quiltDirPath))
        {
            for (Path contained : FileUtils.list(quiltDirPath))
            {
                if (!contained.getFileName().toString().contains(this.modLoaderVersion))
                {
                    FileUtils.deleteDirectory(contained);
                    result = true;
                }
            }
        }
        return result;
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
     * Builder for {@link QuiltVersion}.
     */
    public static class QuiltVersionBuilder implements IBuilder<QuiltVersion>
    {
        private final BuilderArgument<String> quiltVersionArgument =
                new BuilderArgument<>("QuiltVersion", () -> IOUtils.getLatestArtifactVersion(QUILT_VERSION_METADATA)).optional();
        private final BuilderArgument<List<Mod>> modsArgument =
                new BuilderArgument<List<Mod>>("Mods", ArrayList::new).optional();
        private final BuilderArgument<List<CurseFileInfo>> curseModsArgument =
                new BuilderArgument<List<CurseFileInfo>>("CurseMods", ArrayList::new).optional();
        private final BuilderArgument<List<ModrinthVersionInfo>> modrinthModsArgument =
                new BuilderArgument<List<ModrinthVersionInfo>>("ModrinthMods", ArrayList::new).optional();
        private final BuilderArgument<ModFileDeleter> fileDeleterArgument =
                new BuilderArgument<>("ModFileDeleter", () -> new ModFileDeleter(false)).optional();
        private final BuilderArgument<CurseModPackInfo> curseModPackArgument =
                new BuilderArgument<CurseModPackInfo>("CurseModPack").optional();
        private final BuilderArgument<ModrinthModPackInfo> modrinthPackArgument =
                new BuilderArgument<ModrinthModPackInfo>("ModrinthModPack").optional();

        /**
         * @param quiltVersion the Quilt version you want to install
         * (don't use this function if you want to use the latest Quilt version).
         * @return the builder.
         */
        public QuiltVersionBuilder withQuiltVersion(String quiltVersion)
        {
            this.quiltVersionArgument.set(quiltVersion);
            return this;
        }

        /**
         * Append a mods list to the version.
         * @param mods mods to append.
         * @return the builder.
         */
        public QuiltVersionBuilder withMods(List<Mod> mods)
        {
            this.modsArgument.set(mods);
            return this;
        }

        /**
         * Append a mods list to the version.
         * @param curseMods CurseForge's mods to append.
         * @return the builder.
         */
        public QuiltVersionBuilder withCurseMods(List<CurseFileInfo> curseMods)
        {
            this.curseModsArgument.set(curseMods);
            return this;
        }

        /**
         * Append a mods list to the version.
         * @param modrinthMods Modrinth's mods to append.
         * @return the builder.
         */
        public QuiltVersionBuilder withModrinthMods(List<ModrinthVersionInfo> modrinthMods)
        {
            this.modrinthModsArgument.set(modrinthMods);
            return this;
        }

        /**
         * Assign to the future forge version a mod pack.
         * @param modPackInfo the mod pack information to assign.
         * @return the current builder.
         */
        public QuiltVersionBuilder withCurseModPack(CurseModPackInfo modPackInfo)
        {
            this.curseModPackArgument.set(modPackInfo);
            return this;
        }


        /**
         * Assign to the future forge version a mod pack.
         * @param modPackInfo the mod pack information to assign.
         * @return the builder.
         */
        public QuiltVersionBuilder withModrinthModPack(ModrinthModPackInfo modPackInfo)
        {
            this.modrinthPackArgument.set(modPackInfo);
            return this;
        }

        /**
         * Append a file deleter to the version.
         * @param fileDeleter the file deleter to append.
         * @return the builder.
         */
        public QuiltVersionBuilder withFileDeleter(ModFileDeleter fileDeleter)
        {
            this.fileDeleterArgument.set(fileDeleter);
            return this;
        }

        /**
         * Build a new {@link QuiltVersion} instance with provided arguments.
         * @return the freshly created instance.
         * @throws BuilderException if an error occurred.
         */
        @Override
        public QuiltVersion build() throws BuilderException
        {
            return new QuiltVersion(
                    this.modsArgument.get(),
                    this.curseModsArgument.get(),
                    this.modrinthModsArgument.get(),
                    this.quiltVersionArgument.get(),
                    this.fileDeleterArgument.get(),
                    this.curseModPackArgument.get(),
                    this.modrinthPackArgument.get()
           );
        }
    }
}
