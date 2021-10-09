package fr.flowarg.flowupdater.versions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowstringer.StringUtils;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.DownloadList;
import fr.flowarg.flowupdater.download.ICurseFeaturesUser;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.Step;
import fr.flowarg.flowupdater.download.json.CurseFileInfo;
import fr.flowarg.flowupdater.download.json.CurseModPackInfo;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.integrations.IntegrationManager;
import fr.flowarg.flowupdater.integrations.curseforgeintegration.CurseMod;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import fr.flowarg.flowupdater.utils.builderapi.BuilderArgument;
import fr.flowarg.flowupdater.utils.builderapi.BuilderException;
import fr.flowarg.flowupdater.utils.builderapi.IBuilder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * The object that contains Fabric's stuff.
 * @author antoineok https://github.com/antoineok
 */
public class FabricVersion implements ICurseFeaturesUser, IModLoaderVersion
{
    private final List<Mod> mods;
    private final String fabricVersion;
    private final List<CurseFileInfo> curseMods;
    private final ModFileDeleter fileDeleter;
    private List<CurseMod> allCurseMods;
    private final String installerVersion;
    private final CurseModPackInfo modPackInfo;

    private URL installerUrl;
    private ILogger logger;
    private VanillaVersion vanilla;
    private DownloadList downloadList;
    private IProgressCallback callback;

    /**
     * Use {@link FabricVersionBuilder} to instantiate this class.
     * @param mods        {@link List<Mod>} to install.
     * @param curseMods   {@link List<CurseFileInfo>} to install.
     * @param fabricVersion to install.
     * @param fileDeleter {@link ModFileDeleter} used to clean up mods' dir.
     * @param modPackInfo {@link CurseModPackInfo} the mod pack you want to install.
     */
    private FabricVersion(List<Mod> mods, List<CurseFileInfo> curseMods, String fabricVersion, ModFileDeleter fileDeleter, CurseModPackInfo modPackInfo) {
        this.mods = mods;
        this.fileDeleter = fileDeleter;
        this.curseMods = curseMods;
        this.fabricVersion = fabricVersion;
        this.modPackInfo = modPackInfo;
        this.installerVersion = this.getLatestInstallerVersion();
    }

    private static @Nullable String getLatestFabricVersion() {
        try
        {
            final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            final Document doc = dBuilder.parse(new URL("https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml").openStream());

            return getLatestVersionOfArtifact(doc);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private @Nullable String getLatestInstallerVersion() {
        try {
            final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            final Document doc = dBuilder.parse(new URL("https://maven.fabricmc.net/net/fabricmc/fabric-installer/maven-metadata.xml").openStream());

            return getLatestVersionOfArtifact(doc);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private static String getLatestVersionOfArtifact(@NotNull Document doc)
    {
        doc.getDocumentElement().normalize();

        final Element root = doc.getDocumentElement();
        final NodeList nList = root.getElementsByTagName("versioning");
        String version = "";

        for (int temp = 0; temp < nList.getLength(); temp++)
        {
            final Node node = nList.item(temp);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            version = ((Element) node).getElementsByTagName("release").item(0).getTextContent();
        }
        return version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isModLoaderAlreadyInstalled(@NotNull Path installDir)
    {
        return Files.exists(installDir.resolve("libraries").resolve("net").resolve("fabricmc").resolve("fabric-loader").resolve(this.fabricVersion).resolve("fabric-loader-" + this.fabricVersion + ".jar"));
    }

    private class FabricLauncherEnvironment extends ModLoaderLauncherEnvironment
    {
        private final Path fabric;

        public FabricLauncherEnvironment(List<String> command, Path tempDir, Path fabric)
        {
            super(command, tempDir);
            this.fabric = fabric;
        }

        public Path getFabric()
        {
            return this.fabric;
        }

        public void launchFabricInstaller() throws Exception
        {
            final ProcessBuilder processBuilder = new ProcessBuilder(this.getCommand());

            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            final Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) FabricVersion.this.logger.info(line);

            reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = reader.readLine()) != null) FabricVersion.this.logger.info(line);

            process.waitFor();

            reader.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FabricLauncherEnvironment prepareModLoaderLauncher(@NotNull Path dirToInstall, InputStream stream) throws IOException
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
    private @NotNull FabricLauncherEnvironment makeCommand(Path tempDir, @NotNull Path install, @NotNull Path fabric)
    {
        final List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-Xmx256M");
        command.add("-jar");
        command.add(install.toString());
        command.add("client");
        command.add("-dir");
        command.add(fabric.toString());
        command.add("-mcversion");
        command.add(this.vanilla.getName());
        command.add("-loader");
        command.add(this.fabricVersion);
        command.add("-noprofile");
        return new FabricLauncherEnvironment(command, tempDir, fabric);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void install(final Path dirToInstall) throws Exception
    {
        this.callback.step(Step.FABRIC);
        this.logger.info("Installing fabric, version: " + this.fabricVersion + "...");
        this.checkModLoaderEnv(dirToInstall);

        try (BufferedInputStream stream = new BufferedInputStream(this.installerUrl.openStream()))
        {
            final FabricLauncherEnvironment fabricLauncherEnvironment = this.prepareModLoaderLauncher(dirToInstall, stream);
            this.logger.info("Launching fabric installer...");
            fabricLauncherEnvironment.launchFabricInstaller();

            final Path jsonPath = fabricLauncherEnvironment.getFabric().resolve("versions").resolve(String.format("fabric-loader-%s-%s", this.fabricVersion, this.vanilla.getName()));
            final Path jsonFilePath = jsonPath.resolve(jsonPath.getFileName().toString() + ".json");

            final JsonObject obj = JsonParser.parseString(StringUtils.toString(Files.readAllLines(jsonFilePath, StandardCharsets.UTF_8))).getAsJsonObject();
            final JsonArray libs = obj.getAsJsonArray("libraries");

            final Path libraries = dirToInstall.resolve("libraries");
            libs.forEach(el -> {
                final JsonObject artifact = el.getAsJsonObject();
                final String[] parts = artifact.get("name").getAsString().split(":");
                this.downloadArtifacts(libraries, artifact.get("url").getAsString(), parts[0], parts[1], parts[2]);
            });

            this.logger.info("Successfully installed Fabric !");
            FileUtils.deleteDirectory(fabricLauncherEnvironment.getTempDir());
        } catch (Exception e)
        {
            this.logger.printStackTrace(e);
        }
    }

    private void downloadArtifacts(Path dir, String repositoryUrl, String group, String name, String version)
    {
        try
        {
            IOUtils.download(this.logger,
                             new URL(repositoryUrl + group.replace('.', '/') + '/' + name + '/' + version + '/' + String.format("%s-%s.jar", name, version)),
                             dir.resolve(group.replace(".", dir.getFileSystem().getSeparator())).resolve(name).resolve(version).resolve(String.format("%s-%s.jar", name, version)));
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
        final Path fabricDirPath = dirToInstall.resolve("libraries").resolve("net").resolve("fabricmc").resolve("fabric-loader");
        if (Files.exists(fabricDirPath))
        {
            for (Path contained : FileUtils.list(fabricDirPath))
            {
                if (!contained.getFileName().toString().contains(this.fabricVersion))
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
    public void installMods(Path modsDir, IntegrationManager integrationManager) throws Exception
    {
        this.callback.step(Step.MODS);

        this.installAllMods(modsDir);
        this.fileDeleter.delete(modsDir, this.mods, this.allCurseMods, null);
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
            this.installerUrl = new URL(String.format("https://maven.fabricmc.net/net/fabricmc/fabric-installer/%s/fabric-installer-%s.jar", installerVersion, installerVersion));
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
    public ILogger getLogger() {
        return this.logger;
    }

    /**
     * Get the Fabric's version.
     * @return the Fabric's version.
     */
    public String getFabricVersion() {
        return this.fabricVersion;
    }

    /**
     * Get the Fabric's installer's url.
     * @return the Fabric's installer's url.
     */
    public URL getInstallerUrl() {
        return this.installerUrl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CurseMod> getAllCurseMods() {
        return this.allCurseMods;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAllCurseMods(List<CurseMod> allCurseMods) {
        this.allCurseMods = allCurseMods;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CurseFileInfo> getCurseMods() {
        return this.curseMods;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CurseModPackInfo getModPackInfo() { return modPackInfo; }

    /**
     * Builder for {@link FabricVersion}.
     */
    public static class FabricVersionBuilder implements IBuilder<FabricVersion>
    {
        private final BuilderArgument<String> fabricVersionArgument = new BuilderArgument<>("FabricVersion", FabricVersion::getLatestFabricVersion).optional();
        private final BuilderArgument<List<Mod>> modsArgument = new BuilderArgument<List<Mod>>("Mods", ArrayList::new).optional();
        private final BuilderArgument<List<CurseFileInfo>> curseModsArgument = new BuilderArgument<List<CurseFileInfo>>("CurseMods", ArrayList::new).optional();
        private final BuilderArgument<ModFileDeleter> fileDeleterArgument = new BuilderArgument<>("ModFileDeleter", () -> new ModFileDeleter(false)).optional();
        private final BuilderArgument<CurseModPackInfo> modPackArgument = new BuilderArgument<CurseModPackInfo>("ModPack").optional();

        /**
         * @param fabricVersion the Fabric version you want to install (don't use this function if you want to use the latest fabric version automatically).
         * @return the builder.
         */
        public FabricVersionBuilder withFabricVersion(String fabricVersion)
        {
            this.fabricVersionArgument.set(fabricVersion);
            return this;
        }

        /**
         * Append a mods list to the version.
         * @param mods mods to append.
         * @return the builder.
         */
        public FabricVersionBuilder withMods(List<Mod> mods)
        {
            this.modsArgument.set(mods);
            return this;
        }

        /**
         * Append a mods list to the version.
         * @param curseMods CurseForge's mods to append.
         * @return the builder.
         */
        public FabricVersionBuilder withCurseMods(List<CurseFileInfo> curseMods)
        {
            this.curseModsArgument.set(curseMods);
            return this;
        }

        /**
         * Assign to the future forge version a mod pack.
         * @param modPackInfo the mod pack information to assign.
         * @return the current builder.
         */
        public FabricVersionBuilder withCurseModPack(CurseModPackInfo modPackInfo)
        {
            this.modPackArgument.set(modPackInfo);
            return this;
        }

        /**
         * Append a file deleter to the version.
         * @param fileDeleter the file deleter to append.
         * @return the builder.
         */
        public FabricVersionBuilder withFileDeleter(ModFileDeleter fileDeleter)
        {
            this.fileDeleterArgument.set(fileDeleter);
            return this;
        }

        /**
         * Assign to the future forge version a mod pack.
         * @param modPackInfo the mod pack information to assign.
         * @return the current builder.
         * @deprecated use {@link #withCurseModPack(CurseModPackInfo)} instead.
         */
        @Deprecated
        @ApiStatus.ScheduledForRemoval(inVersion = "1.6.0")
        public FabricVersionBuilder withModPack(CurseModPackInfo modPackInfo)
        {
            this.modPackArgument.set(modPackInfo);
            return this;
        }

        /**
         * Build a new {@link FabricVersion} instance with provided arguments.
         * @return the freshly created instance.
         * @throws BuilderException if an error occurred.
         */
        @Override
        public FabricVersion build() throws BuilderException {
            return new FabricVersion(
                    this.modsArgument.get(),
                    this.curseModsArgument.get(),
                    this.fabricVersionArgument.get(),
                    this.fileDeleterArgument.get(),
                    this.modPackArgument.get()
           );
        }
    }
}
