package fr.flowarg.flowupdater.versions;

import com.google.gson.*;
import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.curseforgeplugin.CurseMod;
import fr.flowarg.flowupdater.download.DownloadInfos;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.Step;
import fr.flowarg.flowupdater.download.json.CurseModInfos;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.utils.ArtiefactsDownloader;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
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
 * @author antoineok
 */
public class FabricVersion {
    protected final ILogger logger;
    protected final List<Mod> mods;
    protected final VanillaVersion vanilla;
    protected final String fabricVersion;
    protected final IProgressCallback callback;
    protected final ArrayList<CurseModInfos> curseMods;
    protected final ModFileDeleter fileDeleter;
    protected List<Object> allCurseMods;
    protected URL installerUrl;
    protected DownloadInfos downloadInfos;
    final String installerVersion;

    private final String[] compatibleVersions = {"1.16", "1.15", "1.14", "1.13"};

    /**
     * Use {@link ForgeVersionBuilder} to instantiate this class.
     *
     * @param logger      {@link ILogger} used for logging.
     * @param mods        {@link List<Mod>} to install.
     * @param curseMods   {@link ArrayList<CurseModInfos>} to install.
     * @param fabricVersion to install.
     * @param vanilla     {@link VanillaVersion}.
     * @param callback    {@link IProgressCallback} used for update progression.
     * @param fileDeleter {@link ModFileDeleter} used to cleanup mods dir.
     */
    protected FabricVersion(ILogger logger, List<Mod> mods, ArrayList<CurseModInfos> curseMods, String fabricVersion, VanillaVersion vanilla, IProgressCallback callback, ModFileDeleter fileDeleter) {
        this.logger = logger;
        this.mods = mods;
        this.fileDeleter = fileDeleter;
        this.curseMods = curseMods;
        this.vanilla = vanilla;
        this.fabricVersion = fabricVersion;
        this.installerVersion = getLatestInstallerVersion();
        this.callback = callback;
        try {
            this.installerUrl = new URL(String.format("https://maven.fabricmc.net/net/fabricmc/fabric-installer/%s/fabric-installer-%s.jar", installerVersion, installerVersion));
        } catch (MalformedURLException e) {
            this.logger.printStackTrace(e);
        }
    }

    protected static String getLatestFabricVersion() {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new URL("https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml").openStream());

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
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getLatestInstallerVersion() {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new URL("https://maven.fabricmc.net/net/fabricmc/fabric-installer/maven-metadata.xml").openStream());

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
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Check if forge is already installed. Used by {@link FlowUpdater} on update task.
     *
     * @param installDir the minecraft installation dir.
     * @return true if forge is already installed or not.
     */
    public boolean isFabricAlreadyInstalled(File installDir) {
        return new File(installDir, "libraries/net/fabricmc/fabric-loader/" + this.fabricVersion + "/" + "fabric-loader-" + this.fabricVersion + ".jar").exists();
    }

    /**
     * This function installs a Fabric version at the specified directory.
     *
     * @param dirToInstall Specified directory.
     */
    //TODO: optimize this
    public void install(final File dirToInstall) {
        this.callback.step(Step.FABRIC);
        this.logger.info("Installing fabric, version: " + this.fabricVersion + "...");
        this.checkFabricEnv(dirToInstall);
        if(this.isCompatible()){
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
                final ArrayList<String> command = new ArrayList<>();
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
                BufferedReader reader = new BufferedReader (new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine ()) != null) {
                    System.out.println(line);
                }
                reader = new BufferedReader (new InputStreamReader(process.getErrorStream()));
                while ((line = reader.readLine ()) != null) {
                    System.out.println(line);
                }
                process.waitFor();
                File json = new File(fabric, String.format("versions" + File.separatorChar + "fabric-loader-%s-%s" , fabricVersion, this.vanilla.getName()));

                File jsonFile = new File(json, json.getName() + ".json");
                System.out.println(json.exists());
                String jsonString = FileUtils.loadFile(jsonFile);
                JsonObject obj = JsonParser.parseString(jsonString).getAsJsonObject();
                JsonArray libs = obj.getAsJsonArray("libraries");
                for(JsonElement el : libs){
                    JsonObject artifact = el.getAsJsonObject();
                    String id = artifact.get("name").getAsString();
                    String url = artifact.get("url").getAsString();
                    logger.info(String.format("Téléchargement de %s depuis %s", id, url));
                    ArtiefactsDownloader.donwloadArtifacts(libraries, url, id);
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
    protected boolean checkFabricEnv(File dirToInstall) {
        boolean result = false;
        final File forgeDir = new File(dirToInstall, "libraries/net/fabricmc/fabric-loader/");
        if (forgeDir.exists()) {
            if (forgeDir.listFiles() != null) {
                for (File contained : forgeDir.listFiles()) {
                    if (!contained.getName().contains(this.fabricVersion)) {
                        if (contained.isDirectory()) FileUtils.deleteDirectory(contained);
                        else contained.delete();
                        result = true;
                    }
                }
            }
        }

        return result;
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
     *
     * @param modsDir           Specified mods directory.
     * @param cursePluginLoaded if FlowUpdater has loaded CurseForge plugin
     * @throws IOException If the install fail.
     */
    public void installMods(File modsDir, boolean cursePluginLoaded) throws Exception {
        this.callback.step(Step.MODS);
        this.downloadInfos.getMods().forEach(mod -> {
            try {
                IOUtils.download(this.logger, new URL(mod.getDownloadURL()), new File(modsDir, mod.getName()));
            } catch (MalformedURLException e) {
                this.logger.printStackTrace(e);
            }
            this.downloadInfos.incrementDownloaded();
            this.callback.update(this.downloadInfos.getDownloaded(), this.downloadInfos.getTotalToDownload());
        });

        if (cursePluginLoaded) {
            this.downloadInfos.getCurseMods().forEach(obj -> {
                try {
                    final CurseMod curseMod = (CurseMod) obj;
                    IOUtils.download(this.logger, new URL(curseMod.getDownloadURL()), new File(modsDir, curseMod.getName()));
                } catch (MalformedURLException e) {
                    this.logger.printStackTrace(e);
                }
                this.downloadInfos.incrementDownloaded();
                this.callback.update(this.downloadInfos.getDownloaded(), this.downloadInfos.getTotalToDownload());
            });
        }

        this.fileDeleter.delete(modsDir, this.mods, cursePluginLoaded, this.allCurseMods);
    }

    public ModFileDeleter getFileDeleter() {
        return this.fileDeleter;
    }

    public void appendDownloadInfos(DownloadInfos infos) {
        this.downloadInfos = infos;
    }

    public List<Mod> getMods() {
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

    public void setAllCurseMods(List<Object> allCurseMods) {
        this.allCurseMods = allCurseMods;
    }

    public ArrayList<CurseModInfos> getCurseMods() {
        return this.curseMods;
    }


    public static class FabricVersionBuilder implements IBuilder<FabricVersion> {

        private final BuilderArgument<String> fabricVersionArgument = new BuilderArgument<String>("fabricVerion", FabricVersion::getLatestFabricVersion).required();
        private final BuilderArgument<VanillaVersion> vanillaVersionArgument = new BuilderArgument<>(() -> VanillaVersion.NULL_VERSION, "VanillaVersion").required();
        private final BuilderArgument<ILogger> loggerArgument = new BuilderArgument<>("Logger", () -> FlowUpdater.DEFAULT_LOGGER).optional();
        private final BuilderArgument<IProgressCallback> progressCallbackArgument = new BuilderArgument<>("ProgressCallback", () -> FlowUpdater.NULL_CALLBACK).optional();
        private final BuilderArgument<List<Mod>> modsArgument = new BuilderArgument<List<Mod>>("Mods", ArrayList::new).optional();
        private final BuilderArgument<ArrayList<CurseModInfos>> curseModsArgument = new BuilderArgument<ArrayList<CurseModInfos>>("CurseMods", ArrayList::new).optional();
        private final BuilderArgument<ModFileDeleter> fileDeleterArgument = new BuilderArgument<>("ModFileDeleter", () -> new ModFileDeleter(false)).optional();

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

        public FabricVersionBuilder withCurseMods(ArrayList<CurseModInfos> curseMods)
        {
            this.curseModsArgument.set(curseMods);
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
                this.loggerArgument.get().warn("You are using default callback for forge installation. If you're using a custom callback for vanilla files, it will not updated when forge and mods will be installed.");

            return new FabricVersion(this.loggerArgument.get(), this.modsArgument.get(), this.curseModsArgument.get(), this.fabricVersionArgument.get(), this.vanillaVersionArgument.get(), this.progressCallbackArgument.get(), this.fileDeleterArgument.get());
        }
    }
}
