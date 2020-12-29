package fr.flowarg.flowupdater.versions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.*;
import fr.flowarg.flowupdater.download.json.CurseFileInfos;
import fr.flowarg.flowupdater.download.json.CurseModPackInfos;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * @author antoineok <https://github.com/antoineok>
 */
public class FabricVersion implements ICurseFeaturesUser, IModLoaderVersion
{
    private final ILogger logger;
    private final List<Mod> mods;
    private final VanillaVersion vanilla;
    private final String fabricVersion;
    private final IProgressCallback callback;
    private final ArrayList<CurseFileInfos> curseMods;
    private final ModFileDeleter fileDeleter;
    private List<Object> allCurseMods;
    private URL installerUrl;
    private DownloadInfos downloadInfos;
    private final String installerVersion;
    private final CurseModPackInfos modPackInfos;

    private final String[] compatibleVersions = {"1.16", "1.15", "1.14", "1.13"};

    /**
     * Use {@link FabricVersionBuilder} to instantiate this class.
     *  @param logger      {@link ILogger} used for logging.
     * @param mods        {@link List<Mod>} to install.
     * @param curseMods   {@link ArrayList<CurseFileInfos>} to install.
     * @param fabricVersion to install.
     * @param vanilla     {@link VanillaVersion}.
     * @param callback    {@link IProgressCallback} used for update progression.
     * @param fileDeleter {@link ModFileDeleter} used to cleanup mods dir.
     * @param modPackInfos {@link CurseModPackInfos} the modpack you want to install.
     */
    private FabricVersion(ILogger logger, List<Mod> mods, ArrayList<CurseFileInfos> curseMods, String fabricVersion, VanillaVersion vanilla, IProgressCallback callback, ModFileDeleter fileDeleter, CurseModPackInfos modPackInfos) {
        this.logger = logger;
        this.mods = mods;
        this.fileDeleter = fileDeleter;
        this.curseMods = curseMods;
        this.vanilla = vanilla;
        this.fabricVersion = fabricVersion;
        this.modPackInfos = modPackInfos;
        this.installerVersion = this.getLatestInstallerVersion();
        this.callback = callback;
        try {
            this.installerUrl = new URL(String.format("https://maven.fabricmc.net/net/fabricmc/fabric-installer/%s/fabric-installer-%s.jar", installerVersion, installerVersion));
        } catch (MalformedURLException e) {
            this.logger.printStackTrace(e);
        }
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

        Element root = doc.getDocumentElement();
        NodeList nList = root.getElementsByTagName("versioning");
        String version = "";
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node node = nList.item(temp);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                version = ((Element) node).getElementsByTagName("release").item(0).getTextContent();
            }
        }
        return version;
    }

    /**
     * Check if fabric is already installed. Used by {@link FlowUpdater} on update task.
     *
     * @param installDir the minecraft installation dir.
     * @return true if fabric is already installed or not.
     */
    @Override
    public boolean isModLoaderAlreadyInstalled(File installDir) {
        return new File(installDir, "libraries/net/fabricmc/fabric-loader/" + this.fabricVersion + "/" + "fabric-loader-" + this.fabricVersion + ".jar").exists();
    }

    /**
     * This function installs a Fabric version at the specified directory.
     * @param dirToInstall Specified directory.
     */
    // TODO optimize this
    @Override
    public void install(final File dirToInstall) {
        this.callback.step(Step.FABRIC);
        this.logger.info("Installing fabric, version: " + this.fabricVersion + "...");
        this.checkFabricEnv(dirToInstall);
        if(this.isCompatible())
        {
            try (BufferedInputStream stream = new BufferedInputStream(this.installerUrl.openStream()))
            {
                this.logger.info("Downloading fabric installer...");

                final File tempDir = new File(dirToInstall, ".flowupdater");
                final File fabric = new File(tempDir, "zeWorld");
                final File install = new File(tempDir, String.format("fabric-installer-%s.jar", installerVersion));
                final File libraries = new File(dirToInstall, "libraries");

                install.delete();
                fabric.mkdirs();
                tempDir.mkdirs();

                Files.copy(stream, install.toPath(), StandardCopyOption.REPLACE_EXISTING);
                this.logger.info("Launching fabric installer...");
                final List<String> command = new ArrayList<>();
                command.add("java");
                command.add("-Xmx256M");
                command.add("-jar");
                command.add(install.getAbsolutePath());
                command.add("client");
                command.add("-dir");
                command.add(fabric.getAbsolutePath());
                command.add("-mcversion");
                command.add(this.vanilla.getName());
                command.add("-loader");
                command.add(this.fabricVersion);
                command.add("-noprofile");
                final ProcessBuilder processBuilder = new ProcessBuilder(command);

                processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                final Process process = processBuilder.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;

                while ((line = reader.readLine()) != null)
                    System.out.println(line);

                reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                while ((line = reader.readLine()) != null)
                    System.out.println(line);

                process.waitFor();

                final File json = new File(fabric, String.format("versions" + File.separatorChar + "fabric-loader-%s-%s" , fabricVersion, this.vanilla.getName()));
                final File jsonFile = new File(json, json.getName() + ".json");
                final String jsonString = FileUtils.loadFile(jsonFile);
                final JsonObject obj = JsonParser.parseString(jsonString).getAsJsonObject();
                final JsonArray libs = obj.getAsJsonArray("libraries");

                for(JsonElement el : libs)
                {
                    final JsonObject artifact = el.getAsJsonObject();
                    ArtifactsDownloader.downloadArtifacts(libraries, artifact.get("url").getAsString(), artifact.get("name").getAsString(), this.logger);
                }

                this.logger.info("Successfully installed Fabric !");
                FileUtils.deleteDirectory(tempDir);
            } catch (IOException | InterruptedException e) {
                this.logger.printStackTrace(e);
            }
        }
    }

    /**
     * Check if the minecraft installation already contains another fabric installation not corresponding to this version.
     *
     * @param dirToInstall Fabric installation directory.
     */
    private void checkFabricEnv(File dirToInstall)
    {
        final File fabricDir = new File(dirToInstall, "libraries/net/fabricmc/fabric-loader/");
        if (fabricDir.exists())
        {
            for (File contained : FileUtils.list(fabricDir))
            {
                if (!contained.getName().contains(this.fabricVersion))
                {
                    if (contained.isDirectory()) FileUtils.deleteDirectory(contained);
                    else contained.delete();
                }
            }
        }
    }

    public boolean isCompatible()
    {
        for(String str : this.compatibleVersions)
        {
            if(this.vanilla.getName().startsWith(str))
                return true;
        }
        return false;
    }

    /**
     * This function installs mods at the specified directory.
     * @param modsDir Specified mods directory.
     * @param pluginManager PluginManager of FlowUpdater
     * @throws IOException If the install fail.
     */
    @Override
    public void installMods(File modsDir, PluginManager pluginManager) throws Exception
    {
        this.callback.step(Step.MODS);
        final boolean cursePluginLoaded = pluginManager.isCursePluginLoaded();

        ModCommons.installAllMods(this.downloadInfos, this.logger, modsDir, this.callback, cursePluginLoaded);
        this.fileDeleter.delete(modsDir, this.mods, cursePluginLoaded, this.allCurseMods, false, null);
    }

    public ModFileDeleter getFileDeleter() {
        return this.fileDeleter;
    }

    @Override
    public void appendDownloadInfos(DownloadInfos infos)
    {
        this.downloadInfos = infos;
    }

    @Override
    public List<Mod> getMods()
    {
        return this.mods;
    }

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

    @Override
    public void setAllCurseMods(List<Object> allCurseMods) {
        this.allCurseMods = allCurseMods;
    }

    @Override
    public List<CurseFileInfos> getCurseMods() {
        return this.curseMods;
    }

    @Override
    public CurseModPackInfos getModPackInfos() { return modPackInfos; }

    public static class FabricVersionBuilder implements IBuilder<FabricVersion> {

        private final BuilderArgument<String> fabricVersionArgument = new BuilderArgument<>("FabricVersion", FabricVersion::getLatestFabricVersion).optional();
        private final BuilderArgument<VanillaVersion> vanillaVersionArgument = new BuilderArgument<>(() -> VanillaVersion.NULL_VERSION, "VanillaVersion").required();
        private final BuilderArgument<ILogger> loggerArgument = new BuilderArgument<>("Logger", () -> FlowUpdater.DEFAULT_LOGGER).optional();
        private final BuilderArgument<IProgressCallback> progressCallbackArgument = new BuilderArgument<>("ProgressCallback", () -> FlowUpdater.NULL_CALLBACK).optional();
        private final BuilderArgument<List<Mod>> modsArgument = new BuilderArgument<List<Mod>>("Mods", ArrayList::new).optional();
        private final BuilderArgument<ArrayList<CurseFileInfos>> curseModsArgument = new BuilderArgument<ArrayList<CurseFileInfos>>("CurseMods", ArrayList::new).optional();
        private final BuilderArgument<ModFileDeleter> fileDeleterArgument = new BuilderArgument<>("ModFileDeleter", () -> new ModFileDeleter(false)).optional();
        private final BuilderArgument<CurseModPackInfos> modPackArgument = new BuilderArgument<CurseModPackInfos>("ModPack").optional();

        /**
         *
         * @param fabricVersion the fabric version you want to install (don't use this function if you want to use the latest fabric version automatically).
         */
        public FabricVersionBuilder withFabricVersion(String fabricVersion)
        {
            this.fabricVersionArgument.set(fabricVersion);
            return this;
        }

        public FabricVersionBuilder withVanillaVersion(VanillaVersion vanillaVersion)
        {
            this.vanillaVersionArgument.set(vanillaVersion);
            return this;
        }

        public FabricVersionBuilder withLogger(ILogger logger)
        {
            this.loggerArgument.set(logger);
            return this;
        }

        public FabricVersionBuilder withProgressCallback(IProgressCallback progressCallback)
        {
            this.progressCallbackArgument.set(progressCallback);
            return this;
        }

        public FabricVersionBuilder withMods(List<Mod> mods)
        {
            this.modsArgument.set(mods);
            return this;
        }

        public FabricVersionBuilder withCurseMods(ArrayList<CurseFileInfos> curseMods)
        {
            this.curseModsArgument.set(curseMods);
            return this;
        }

        public FabricVersionBuilder withCurseModPack(CurseModPackInfos modpack)
        {
            this.modPackArgument.set(modpack);
            return this;
        }

        public FabricVersionBuilder withFileDeleter(ModFileDeleter fileDeleter)
        {
            this.fileDeleterArgument.set(fileDeleter);
            return this;
        }

        @Override
        public FabricVersion build() throws BuilderException {
            if(this.progressCallbackArgument.get() == FlowUpdater.NULL_CALLBACK)
                this.loggerArgument.get().warn("You are using default callback for fabric installation. If you're using a custom callback for vanilla files, it will not updated when fabric and mods will be installed.");

            return new FabricVersion(
                    this.loggerArgument.get(),
                    this.modsArgument.get(),
                    this.curseModsArgument.get(),
                    this.fabricVersionArgument.get(),
                    this.vanillaVersionArgument.get(),
                    this.progressCallbackArgument.get(),
                    this.fileDeleterArgument.get(),
                    this.modPackArgument.get()
           );
        }
    }
}
