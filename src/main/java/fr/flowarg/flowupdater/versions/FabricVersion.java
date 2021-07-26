package fr.flowarg.flowupdater.versions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowstringer.StringUtils;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.DownloadInfos;
import fr.flowarg.flowupdater.download.ICurseFeaturesUser;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.Step;
import fr.flowarg.flowupdater.download.json.CurseFileInfos;
import fr.flowarg.flowupdater.download.json.CurseModPackInfo;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.utils.ArtifactsDownloader;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import fr.flowarg.flowupdater.utils.PluginManager;
import fr.flowarg.flowupdater.utils.builderapi.BuilderArgument;
import fr.flowarg.flowupdater.utils.builderapi.BuilderException;
import fr.flowarg.flowupdater.utils.builderapi.IBuilder;
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
 * @author antoineok https://github.com/antoineok
 */
public class FabricVersion implements ICurseFeaturesUser, IModLoaderVersion
{
    private final List<Mod> mods;
    private final String fabricVersion;
    private final List<CurseFileInfos> curseMods;
    private final ModFileDeleter fileDeleter;
    private List<Object> allCurseMods;
    private final String installerVersion;
    private final CurseModPackInfo modPackInfo;

    private URL installerUrl;
    private ILogger logger;
    private VanillaVersion vanilla;
    private DownloadInfos downloadInfos;
    private IProgressCallback callback;

    /**
     * Use {@link FabricVersionBuilder} to instantiate this class.
     * @param mods        {@link List<Mod>} to install.
     * @param curseMods   {@link List<CurseFileInfos>} to install.
     * @param fabricVersion to install.
     * @param fileDeleter {@link ModFileDeleter} used to cleanup mods dir.
     * @param modPackInfo {@link CurseModPackInfo} the modpack you want to install.
     */
    private FabricVersion(List<Mod> mods, List<CurseFileInfos> curseMods, String fabricVersion, ModFileDeleter fileDeleter, CurseModPackInfo modPackInfo) {
        this.mods = mods;
        this.fileDeleter = fileDeleter;
        this.curseMods = curseMods;
        this.fabricVersion = fabricVersion;
        this.modPackInfo = modPackInfo;
        this.installerVersion = this.getLatestInstallerVersion();
    }

    private static String getLatestFabricVersion() {
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

    private String getLatestInstallerVersion() {
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

    private static String getLatestVersionOfArtifact(Document doc)
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
    public boolean isModLoaderAlreadyInstalled(Path installDir)
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
    public FabricLauncherEnvironment prepareModLoaderLauncher(Path dirToInstall, InputStream stream) throws IOException
    {
        this.logger.info("Downloading fabric installer...");

        final Path tempDirPath = dirToInstall.resolve(".flowupdater");
        FileUtils.deleteDirectory(tempDirPath);
        final Path fabricPath = tempDirPath.resolve("zeWorld");
        final Path installPath = tempDirPath.resolve(String.format("fabric-installer-%s.jar", installerVersion));

        Files.createDirectories(tempDirPath);
        Files.createDirectories(fabricPath);

        Files.copy(stream, installPath, StandardCopyOption.REPLACE_EXISTING);
        return this.makeCommand(tempDirPath, installPath, fabricPath);
    }

    private FabricLauncherEnvironment makeCommand(Path tempDir, Path install, Path fabric)
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
                ArtifactsDownloader.downloadArtifacts(libraries, artifact.get("url").getAsString(), artifact.get("name").getAsString(), this.logger);
            });

            this.logger.info("Successfully installed Fabric !");
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
    public boolean checkModLoaderEnv(Path dirToInstall) throws Exception
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
    public void installMods(Path modsDir, PluginManager pluginManager) throws Exception
    {
        this.callback.step(Step.MODS);
        final boolean cursePluginLoaded = pluginManager.isCursePluginLoaded();

        this.installAllMods(modsDir, cursePluginLoaded);
        this.fileDeleter.delete(modsDir, this.mods, cursePluginLoaded, this.allCurseMods, false, null);
    }

    public ModFileDeleter getFileDeleter() {
        return this.fileDeleter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void attachFlowUpdater(FlowUpdater flowUpdater)
    {
        this.callback = flowUpdater.getCallback();
        this.logger = flowUpdater.getLogger();
        this.downloadInfos = flowUpdater.getDownloadInfos();
        this.vanilla = flowUpdater.getVersion();
        try {
            this.installerUrl = new URL(String.format("https://maven.fabricmc.net/net/fabricmc/fabric-installer/%s/fabric-installer-%s.jar", installerVersion, installerVersion));
        } catch (Exception e) {
            this.logger.printStackTrace(e);
        }

        if(!this.curseMods.isEmpty() && !flowUpdater.getUpdaterOptions().isEnableCurseForgePlugin())
            this.logger.warn("You must enable the enableCurseForgePlugin option to use curse forge features!");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DownloadInfos getDownloadInfos()
    {
        return this.downloadInfos;
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

    public String getFabricVersion() {
        return this.fabricVersion;
    }

    public URL getInstallerUrl() {
        return this.installerUrl;
    }

    public List<Object> getAllCurseMods() {
        return this.allCurseMods;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAllCurseMods(List<Object> allCurseMods) {
        this.allCurseMods = allCurseMods;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CurseFileInfos> getCurseMods() {
        return this.curseMods;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CurseModPackInfo getModPackInfo() { return modPackInfo; }

    public static class FabricVersionBuilder implements IBuilder<FabricVersion>
    {
        private final BuilderArgument<String> fabricVersionArgument = new BuilderArgument<>("FabricVersion", FabricVersion::getLatestFabricVersion).optional();
        private final BuilderArgument<List<Mod>> modsArgument = new BuilderArgument<List<Mod>>("Mods", ArrayList::new).optional();
        private final BuilderArgument<List<CurseFileInfos>> curseModsArgument = new BuilderArgument<List<CurseFileInfos>>("CurseMods", ArrayList::new).optional();
        private final BuilderArgument<ModFileDeleter> fileDeleterArgument = new BuilderArgument<>("ModFileDeleter", () -> new ModFileDeleter(false)).optional();
        private final BuilderArgument<CurseModPackInfo> modPackArgument = new BuilderArgument<CurseModPackInfo>("ModPack").optional();

        /**
         * @param fabricVersion the fabric version you want to install (don't use this function if you want to use the latest fabric version automatically).
         * @return the builder.
         */
        public FabricVersionBuilder withFabricVersion(String fabricVersion)
        {
            this.fabricVersionArgument.set(fabricVersion);
            return this;
        }

        public FabricVersionBuilder withMods(List<Mod> mods)
        {
            this.modsArgument.set(mods);
            return this;
        }

        public FabricVersionBuilder withCurseMods(List<CurseFileInfos> curseMods)
        {
            this.curseModsArgument.set(curseMods);
            return this;
        }

        public FabricVersionBuilder withCurseModPack(CurseModPackInfo modpack)
        {
            this.modPackArgument.set(modpack);
            return this;
        }

        public FabricVersionBuilder withFileDeleter(ModFileDeleter fileDeleter)
        {
            this.fileDeleterArgument.set(fileDeleter);
            return this;
        }

        public FabricVersionBuilder withModPack(CurseModPackInfo modPackInfo)
        {
            this.modPackArgument.set(modPackInfo);
            return this;
        }

        @Deprecated
        public FabricVersionBuilder withVanillaVersion(VanillaVersion vanillaVersion)
        {
            return this;
        }

        @Deprecated
        public FabricVersionBuilder withLogger(ILogger logger)
        {
            return this;
        }

        @Deprecated
        public FabricVersionBuilder withProgressCallback(IProgressCallback progressCallback)
        {
            return this;
        }

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
