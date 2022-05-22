package fr.flowarg.flowupdater.versions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowstringer.StringUtils;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.DownloadList;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.Step;
import fr.flowarg.flowupdater.download.json.*;
import fr.flowarg.flowupdater.integrations.curseforgeintegration.ICurseFeaturesUser;
import fr.flowarg.flowupdater.integrations.modrinthintegration.IModrinthFeaturesUser;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import fr.flowarg.flowupdater.utils.builderapi.BuilderArgument;
import fr.flowarg.flowupdater.utils.builderapi.BuilderException;
import fr.flowarg.flowupdater.utils.builderapi.IBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * The object that contains Quilt's stuff.
 */
public class QuiltVersion implements ICurseFeaturesUser, IModLoaderVersion, IModrinthFeaturesUser
{
    private final List<Mod> mods;
    private final String quiltVersion;
    private final List<CurseFileInfo> curseMods;
    private final List<ModrinthVersionInfo> modrinthMods;
    private final ModFileDeleter fileDeleter;
    private final String installerVersion;
    private final CurseModPackInfo curseModPackInfo;
    private final ModrinthModPackInfo modrinthModPackInfo;

    private URL installerUrl;
    private ILogger logger;
    private VanillaVersion vanilla;
    private DownloadList downloadList;
    private IProgressCallback callback;

    /**
     * Use {@link QuiltVersionBuilder} to instantiate this class.
     * @param mods        {@link List<Mod>} to install.
     * @param curseMods   {@link List<CurseFileInfo>} to install.
     * @param quiltVersion to install.
     * @param fileDeleter {@link ModFileDeleter} used to clean up mods' dir.
     * @param curseModPackInfo {@link CurseModPackInfo} the mod pack you want to install.
     */
    private QuiltVersion(List<Mod> mods, List<CurseFileInfo> curseMods, List<ModrinthVersionInfo> modrinthMods, String quiltVersion,
            ModFileDeleter fileDeleter, CurseModPackInfo curseModPackInfo, ModrinthModPackInfo modrinthModPackInfo)
    {
        this.mods = mods;
        this.fileDeleter = fileDeleter;
        this.curseMods = curseMods;
        this.modrinthMods = modrinthMods;
        this.quiltVersion = quiltVersion;
        this.curseModPackInfo = curseModPackInfo;
        this.modrinthModPackInfo = modrinthModPackInfo;
        this.installerVersion = IOUtils.getLatestArtifactVersion("https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-installer/maven-metadata.xml");
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
                        .resolve(this.quiltVersion)
                        .resolve("quilt-loader-" + this.quiltVersion + ".jar"));
    }

    private class QuiltLauncherEnvironment extends ModLoaderLauncherEnvironment
    {
        private final Path quilt;

        public QuiltLauncherEnvironment(List<String> command, Path tempDir, Path quilt)
        {
            super(command, tempDir);
            this.quilt = quilt;
        }

        public Path getQuilt()
        {
            return this.quilt;
        }

        public void launchQuiltInstaller() throws Exception
        {
            final ProcessBuilder processBuilder = new ProcessBuilder(this.getCommand());

            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            final Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) QuiltVersion.this.logger.info(line);

            reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = reader.readLine()) != null) QuiltVersion.this.logger.info(line);

            process.waitFor();

            reader.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QuiltLauncherEnvironment prepareModLoaderLauncher(@NotNull Path dirToInstall, InputStream stream) throws IOException
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
    private @NotNull QuiltLauncherEnvironment makeCommand(Path tempDir, @NotNull Path install, @NotNull Path quilt)
    {
        final List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-Xmx256M");
        command.add("-jar");
        command.add(install.toString());
        command.add("install");
        command.add("client");
        command.add(this.vanilla.getName());
        command.add(this.quiltVersion);
        command.add("--no-profile");
        command.add("--install-dir=" + quilt);
        return new QuiltLauncherEnvironment(command, tempDir, quilt);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void install(final Path dirToInstall) throws Exception
    {
        this.callback.step(Step.MOD_LOADER);
        this.logger.info("Installing Quilt, version: " + this.quiltVersion + "...");
        this.checkModLoaderEnv(dirToInstall);

        try (BufferedInputStream stream = new BufferedInputStream(this.installerUrl.openStream()))
        {
            final QuiltLauncherEnvironment quiltLauncherEnvironment = this.prepareModLoaderLauncher(dirToInstall, stream);
            this.logger.info("Launching quilt installer...");
            quiltLauncherEnvironment.launchQuiltInstaller();

            final Path versionDir = quiltLauncherEnvironment.getQuilt()
                    .resolve("versions")
                    .resolve(String.format("quilt-loader-%s-%s", this.quiltVersion, this.vanilla.getName()));

            final Path jsonFilePath = versionDir.resolve(versionDir.getFileName().toString() + ".json");

            final JsonObject obj = JsonParser.parseString(
                    StringUtils.toString(Files.readAllLines(jsonFilePath, StandardCharsets.UTF_8)))
                    .getAsJsonObject();

            final JsonArray libs = obj.getAsJsonArray("libraries");
            final Path libraries = dirToInstall.resolve("libraries");

            libs.forEach(el -> {
                final JsonObject artifact = el.getAsJsonObject();
                final String[] parts = artifact.get("name").getAsString().split(":");
                IOUtils.downloadArtifacts(this.logger, libraries, artifact.get("url").getAsString(), parts);
            });

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
        final Path quiltDirPath = dirToInstall.resolve("libraries").resolve("org").resolve("quiltmc").resolve("quilt-loader");
        if (Files.exists(quiltDirPath))
        {
            for (Path contained : FileUtils.list(quiltDirPath))
            {
                if (!contained.getFileName().toString().contains(this.quiltVersion))
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

    public ModFileDeleter getFileDeleter() {
        return this.fileDeleter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void attachFlowUpdater(@NotNull FlowUpdater flowUpdater)
    {
        this.callback = flowUpdater.getCallback();
        this.logger = flowUpdater.getLogger();
        this.downloadList = flowUpdater.getDownloadList();
        this.vanilla = flowUpdater.getVanillaVersion();
        try {
            this.installerUrl = new URL(
                    String.format("https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-installer/%s/quilt-installer-%s.jar",
                                  installerVersion, installerVersion));
        } catch (Exception e) {
            this.logger.printStackTrace(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DownloadList getDownloadList()
    {
        return this.downloadList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IProgressCallback getCallback()
    {
        return this.callback;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Mod> getMods()
    {
        return this.mods;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ILogger getLogger() {
        return this.logger;
    }

    /**
     * Get the Quilt's version.
     * @return the Quilt's version.
     */
    public String getQuiltVersion() {
        return this.quiltVersion;
    }

    /**
     * Get the Quilt's installer's url.
     * @return the Quilt's installer's url.
     */
    public URL getInstallerUrl() {
        return this.installerUrl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAllCurseMods(List<Mod> allCurseMods)
    {
        this.mods.addAll(allCurseMods);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CurseFileInfo> getCurseMods() {
        return this.curseMods;
    }

    @Override
    public List<ModrinthVersionInfo> getModrinthMods()
    {
        return this.modrinthMods;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CurseModPackInfo getCurseModPackInfo() { return curseModPackInfo; }

    @Override
    public ModrinthModPackInfo getModrinthModPackInfo()
    {
        return this.modrinthModPackInfo;
    }

    @Override
    public void setAllModrinthMods(List<Mod> modrinthMods)
    {
        this.mods.addAll(modrinthMods);
    }

    /**
     * Builder for {@link QuiltVersion}.
     */
    public static class QuiltVersionBuilder implements IBuilder<QuiltVersion>
    {
        private final BuilderArgument<String> quiltVersionArgument = new BuilderArgument<>("QuiltVersion", () -> IOUtils.getLatestArtifactVersion("https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-loader/maven-metadata.xml")).optional();
        private final BuilderArgument<List<Mod>> modsArgument = new BuilderArgument<List<Mod>>("Mods", ArrayList::new).optional();
        private final BuilderArgument<List<CurseFileInfo>> curseModsArgument = new BuilderArgument<List<CurseFileInfo>>("CurseMods", ArrayList::new).optional();
        private final BuilderArgument<List<ModrinthVersionInfo>> modrinthModsArgument = new BuilderArgument<List<ModrinthVersionInfo>>("ModrinthMods", ArrayList::new).optional();
        private final BuilderArgument<ModFileDeleter> fileDeleterArgument = new BuilderArgument<>("ModFileDeleter", () -> new ModFileDeleter(false)).optional();
        private final BuilderArgument<CurseModPackInfo> curseModPackArgument = new BuilderArgument<CurseModPackInfo>("CurseModPack").optional();
        private final BuilderArgument<ModrinthModPackInfo> modrinthPackArgument = new BuilderArgument<ModrinthModPackInfo>("ModrinthModPack").optional();

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
        public QuiltVersion build() throws BuilderException {
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
