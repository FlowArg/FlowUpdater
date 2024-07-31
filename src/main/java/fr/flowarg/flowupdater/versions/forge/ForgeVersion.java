package fr.flowarg.flowupdater.versions.forge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowstringer.StringUtils;
import fr.flowarg.flowupdater.download.json.*;
import fr.flowarg.flowupdater.integrations.optifineintegration.IOptiFineCompatible;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import fr.flowarg.flowupdater.utils.Version;
import fr.flowarg.flowupdater.versions.AbstractModLoaderVersion;
import fr.flowarg.flowupdater.versions.ModLoaderUtils;
import fr.flowarg.flowupdater.versions.ParsedLibrary;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class ForgeVersion extends AbstractModLoaderVersion implements IOptiFineCompatible
{
    private final OptiFineInfo optiFineInfo;
    private final String versionId;
    private final boolean shouldUseInstaller;
    private final boolean newInstallerJsonSpec;

    public ForgeVersion(String modLoaderVersion, List<Mod> mods, List<CurseFileInfo> curseMods,
            List<ModrinthVersionInfo> modrinthMods, ModFileDeleter fileDeleter, CurseModPackInfo curseModPackInfo,
            ModrinthModPackInfo modrinthModPackInfo, OptiFineInfo optiFineInfo)
    {
        super(modLoaderVersion, mods, curseMods, modrinthMods, fileDeleter, curseModPackInfo, modrinthModPackInfo, optiFineInfo);
        this.optiFineInfo = optiFineInfo;

        final String[] data = this.modLoaderVersion.split("-");
        final String vanilla = data[0];
        final String forge = data[1];
        final Version vanillaVersion = Version.gen(vanilla);
        if(vanillaVersion.isEqualTo(Version.gen("1.20.3")))
            throw new IllegalArgumentException("Forge 1.20.3 is not supported. (can't even launch through official launcher!).");
        final Version forgeVersion = Version.gen(forge);

        if (data.length == 2)
        {
            if(forgeVersion.isNewerOrEqualTo(Version.gen("14.23.5.2851")))
            {
                this.versionId = vanilla + "-forge-" + forge;
                this.shouldUseInstaller = vanillaVersion.isNewerThan(Version.gen("1.12.2"));
                this.newInstallerJsonSpec = true;
            }
            else
            {
                this.versionId = vanilla + "-forge" + this.modLoaderVersion;
                this.shouldUseInstaller = false;
                this.newInstallerJsonSpec = false;
            }
        }
        else
        {
            if(vanillaVersion.isOlderOrEqualTo(Version.gen("1.7.10")))
                this.versionId = vanilla + "-Forge" + forge + "-" + data[2];
            else this.versionId = vanilla + "-forge" + this.modLoaderVersion;
            this.shouldUseInstaller = false;
            this.newInstallerJsonSpec = false;
        }
    }

    @Override
    public boolean isModLoaderAlreadyInstalled(@NotNull Path installDir)
    {
        final Path versionJsonFile = installDir.resolve(this.versionId + ".json");

        if(Files.notExists(versionJsonFile))
            return false;

        try {
            final JsonObject object = JsonParser.parseReader(Files.newBufferedReader(versionJsonFile))
                    .getAsJsonObject();

            if(this.newInstallerJsonSpec)
            {
                final String vanillaVersionStr = this.vanilla.getName();
                final Version vanillaVersion = Version.gen(vanillaVersionStr);
                final boolean firstPass = ModLoaderUtils.parseNewVersionInfo(installDir, object).stream().allMatch(ParsedLibrary::isInstalled);

                if(vanillaVersion.isEqualTo(Version.gen("1.12.2")))
                    return firstPass;

                if(!firstPass)
                    return false;

                final Path librariesDir = installDir.resolve("libraries");

                // 1.13.2 --> 1.15.2 = minecraft (vanilla : slim + extra) + (vanilla-mcp : srg)
                // 1.16.1 --> 1.20.2 = minecraft (vanilla-mcp : slim + extra + srg)

                if(vanillaVersion.isBetweenOrEqual(Version.gen("1.13.2"), Version.gen("1.15.2")))
                {
                    final String mcpVersion = this.getMcpVersion(object);
                    final Path minecraftDir = librariesDir
                            .resolve("net")
                            .resolve("minecraft")
                            .resolve("client");

                    final Path vanillaDir = minecraftDir.resolve(vanillaVersionStr);
                    final Path vanillaMcpDir = minecraftDir.resolve(vanillaVersionStr + "-" + mcpVersion);
                    final Path extraJar = vanillaDir.resolve("client-" + vanillaVersionStr + "-extra.jar");
                    final Path extraJarCache = vanillaDir.resolve("client-" + vanillaVersionStr + "-extra.jar.cache");
                    final Path slimJar = vanillaDir.resolve("client-" + vanillaVersionStr + "-slim.jar");
                    final Path slimJarCache = vanillaDir.resolve("client-" + vanillaVersionStr + "-slim.jar.cache");
                    final Path srgJar = vanillaMcpDir.resolve("client-" + vanillaVersionStr + "-" + mcpVersion + "-srg.jar");

                    if (this.isSlimOrExtraSha1Wrong(extraJar, extraJarCache, slimJar, slimJarCache, srgJar))
                        return false;
                }
                else if(vanillaVersion.isBetweenOrEqual(Version.gen("1.16.1"), Version.gen("1.20.2")))
                {
                    final String mcpVersion = this.getMcpVersion(object);
                    final String clientId = "client-" + vanillaVersionStr + "-" + mcpVersion;
                    final Path vanillaMcpDir = librariesDir
                            .resolve("net")
                            .resolve("minecraft")
                            .resolve("client")
                            .resolve(vanillaVersionStr + "-" + mcpVersion);
                    final Path extraJar = vanillaMcpDir.resolve(clientId + "-extra.jar");
                    final Path extraJarCache = vanillaMcpDir.resolve(clientId + "-extra.jar.cache");
                    final Path slimJar = vanillaMcpDir.resolve(clientId + "-slim.jar");
                    final Path slimJarCache = vanillaMcpDir.resolve(clientId + "-slim.jar.cache");
                    final Path srgJar = vanillaMcpDir.resolve(clientId + "-srg.jar");

                    if (this.isSlimOrExtraSha1Wrong(extraJar, extraJarCache, slimJar, slimJarCache, srgJar))
                        return false;
                }

                // 1.12.2 = libs
                // 1.13.2 --> 1.20.2 = libs + client + universal
                // 1.20.4 --> 1.21 = libs + shim

                if(vanillaVersion.isBetweenOrEqual(Version.gen("1.13.2"), Version.gen("1.20.2")))
                {
                    final Path forgeDir = librariesDir
                            .resolve("net")
                            .resolve("minecraftforge")
                            .resolve("forge")
                            .resolve(this.modLoaderVersion);

                    final Path universalJar = forgeDir.resolve("forge-" + this.modLoaderVersion + "-universal.jar");
                    final Path clientJar = forgeDir.resolve("forge-" + this.modLoaderVersion + "-client.jar");

                    if(Files.notExists(universalJar) || Files.notExists(clientJar))
                        return false;
                }
                else if(vanillaVersion.isNewerOrEqualTo(Version.gen("1.20.4")))
                {
                    final Path shimJar = librariesDir
                            .resolve("net")
                            .resolve("minecraftforge")
                            .resolve("forge")
                            .resolve(this.modLoaderVersion)
                            .resolve("forge-" + this.modLoaderVersion + "-shim.jar");

                    if(Files.notExists(shimJar))
                        return false;
                }
            }
            else return this.parseOldVersionInfo(installDir, object).stream().allMatch(ParsedLibrary::isInstalled);
        }
        catch (Exception e)
        {
            this.logger.err("An error occurred while checking if the mod loader is already installed.");
            return false;
        }

        return true;
    }

    private String getMcpVersion(@NotNull JsonObject object)
    {
        final List<String> gameArguments = object
                .getAsJsonObject("arguments")
                .getAsJsonArray("game")
                .asList()
                .stream()
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsString)
                .collect(Collectors.toList());
        return gameArguments.get(gameArguments.indexOf("--fml.mcpVersion") + 1);
    }

    private boolean isSlimOrExtraSha1Wrong(Path extraJar, Path extraJarCache, Path slimJar, Path slimJarCache, Path srgJar) throws IOException
    {
        if(Files.notExists(extraJar) ||
                Files.notExists(extraJarCache) ||
                Files.notExists(slimJar) ||
                Files.notExists(slimJarCache) ||
                Files.notExists(srgJar)) return true;

        final String extraJarSha1 = FileUtils.getSHA1(extraJar);
        final String slimJarSha1 = FileUtils.getSHA1(slimJar);

        String slimJarCacheSha1 = "";
        for (final String line : Files.readAllLines(slimJarCache))
        {
            if(line.contains("Output: "))
            {
                slimJarCacheSha1 = StringUtils.empty(line, "Output: ");
                break;
            }
        }

        String extraJarCacheSha1 = "";
        for (final String line : Files.readAllLines(extraJarCache))
        {
            if(line.contains("Output: "))
            {
                extraJarCacheSha1 = StringUtils.empty(line, "Output: ");
                break;
            }
        }

        return !extraJarSha1.equalsIgnoreCase(extraJarCacheSha1) || !slimJarSha1.equalsIgnoreCase(slimJarCacheSha1);
    }

    private @NotNull Callable<String> getSha1FromLibrary(@NotNull JsonObject library, String builtJarUrl)
    {
        final JsonElement checksumsElem = library.get("checksums");
        if (checksumsElem != null)
        {
            final JsonElement checksums = checksumsElem.getAsJsonArray().get(0);

            if(checksums != null)
                return checksums::getAsString;
        }

        return () -> IOUtils.getContent(new URL(builtJarUrl + ".sha1"));
    }

    @Override
    public void install(@NotNull Path installDir) throws Exception
    {
        super.install(installDir);

        final String installerUrl = String.format("https://maven.minecraftforge.net/net/minecraftforge/forge/%s/forge-%s-installer.jar",
                                                  this.modLoaderVersion, this.modLoaderVersion);
        final String[] installerUrlParts = installerUrl.split("/");
        final Path installerFile = installDir.resolve(installerUrlParts[installerUrlParts.length - 1]);
        IOUtils.download(
                this.logger,
                new URL(installerUrl),
                installerFile
        );

        if(this.newInstallerJsonSpec)
        {
            if(this.shouldUseInstaller)
                this.useInstaller(installDir, installerFile);
            else
            {
                this.logger.info("Installing libraries...");
                final URI uri = URI.create("jar:file:" + installerFile.toAbsolutePath());
                try (final FileSystem zipFs = FileSystems.newFileSystem(uri, new HashMap<>()))
                {
                    final Path versionFile = zipFs.getPath("version.json");
                    final Path versionJsonFile = installDir.resolve(this.versionId + ".json");
                    Files.copy(versionFile, versionJsonFile, StandardCopyOption.REPLACE_EXISTING);

                    ModLoaderUtils.parseNewVersionInfo(installDir, JsonParser.parseReader(Files.newBufferedReader(versionFile)).getAsJsonObject())
                            .stream()
                            .filter(parsedLibrary -> !parsedLibrary.isInstalled())
                            .forEach(parsedLibrary -> {
                                if(parsedLibrary.getUrl().isPresent())
                                    parsedLibrary.download(this.logger);
                                else
                                {
                                    try
                                    {
                                        final String[] name = parsedLibrary.getArtifact().split(":");
                                        final String group = name[0].replace('.', '/');
                                        final String artifact = name[1];
                                        final boolean hasExtension = name[2].contains("@");
                                        final String version = name[2].contains("@") ? name[2].split("@")[0] : name[2];
                                        final String extension = hasExtension ? name[2].split("@")[1] : "jar";
                                        String classifier = "";
                                        if(name.length == 4)
                                            classifier = "-" + name[3];
                                        Files.createDirectories(parsedLibrary.getPath().getParent());
                                        Files.copy(zipFs.getPath("maven/" + group + '/' + artifact + '/' + version + '/' + artifact + "-" + version + classifier + "." + extension), parsedLibrary.getPath(), StandardCopyOption.REPLACE_EXISTING);
                                    } catch (IOException e)
                                    {
                                        this.logger.printStackTrace(e);
                                    }
                                }
                            });
                } catch (Exception e)
                {
                    this.logger.printStackTrace(e);
                }
            }
        }
        else
        {
            this.logger.info("Installing libraries...");
            final URI uri = URI.create("jar:file:" + installerFile.toAbsolutePath());
            try (final FileSystem zipFs = FileSystems.newFileSystem(uri, new HashMap<>()))
            {
                final Path installProfileFile = zipFs.getPath("install_profile.json");
                final JsonObject versionInfo = JsonParser.parseReader(Files.newBufferedReader(installProfileFile)).getAsJsonObject().getAsJsonObject("versionInfo");
                final Path versionJsonFile = installDir.resolve(this.versionId + ".json");
                Files.write(versionJsonFile, versionInfo.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);

                this.parseOldVersionInfo(installDir, versionInfo)
                        .stream()
                        .filter(parsedLibrary -> !parsedLibrary.isInstalled())
                        .forEach(parsedLibrary -> parsedLibrary.download(this.logger));
            } catch (Exception e)
            {
                this.logger.printStackTrace(e);
            }
        }
        Files.deleteIfExists(installerFile);
    }

    private void useInstaller(Path installDir, @NotNull Path installerFile) throws Exception
    {
        this.logger.info("Launching installer...");
        ModLoaderUtils.fakeContext(installDir, this.vanilla.getName());

        final List<String> command = new ArrayList<>();
        command.add(this.javaPath);
        command.add("-jar");
        command.add(installerFile.toAbsolutePath().toString());
        command.add("--installClient");
        command.add(installDir.toAbsolutePath().toString());

        final ProcessBuilder processBuilder = new ProcessBuilder(command);

        processBuilder.directory(installDir.toFile());
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        final Process process = processBuilder.start();
        process.waitFor();

        Files.copy(
                installDir.resolve("versions")
                        .resolve(this.versionId)
                        .resolve(this.versionId + ".json"),
                installDir.resolve(this.versionId + ".json"),
                StandardCopyOption.REPLACE_EXISTING
        );

        ModLoaderUtils.removeFakeContext(installDir);
    }

    private @NotNull List<ParsedLibrary> parseOldVersionInfo(Path installDir, @NotNull JsonObject versionInfo) throws Exception
    {
        final List<ParsedLibrary> parsedLibraries = new ArrayList<>();
        final JsonArray libraries = versionInfo.getAsJsonArray("libraries");

        for (final JsonElement libraryElement : libraries)
        {
            final JsonObject library = libraryElement.getAsJsonObject();
            final JsonElement clientreqElem = library.get("clientreq");
            final boolean shouldInstall = clientreqElem == null || clientreqElem.getAsBoolean();

            if(!shouldInstall)
                continue;

            final JsonElement urlElem = library.get("url");
            final String baseUrl = urlElem == null ? "https://libraries.minecraft.net/" : urlElem.getAsString();
            final String completeArtifact = library.get("name").getAsString();
            final String[] name = completeArtifact.split(":");
            final String group = name[0];
            final String artifact = name[1];
            final String version = name[2];
            final String classifier = artifact.equals("forge") ? "-universal" : "";
            final Path libraryPath = ModLoaderUtils.buildLibraryPath(installDir, group, artifact, version);
            final String builtJarUrl = ModLoaderUtils.buildJarUrl(baseUrl, group, artifact, version, classifier);
            final Callable<String> sha1 = this.getSha1FromLibrary(library, builtJarUrl);
            final boolean installed = Files.exists(libraryPath) &&
                    FileUtils.getSHA1(libraryPath).equals(sha1.call());

            parsedLibraries.add(new ParsedLibrary(libraryPath, new URL(builtJarUrl), completeArtifact, installed));
        }

        return parsedLibraries;
    }

    @Override
    public OptiFineInfo getOptiFineInfo()
    {
        return this.optiFineInfo;
    }

    @Override
    public String name()
    {
        return "Forge";
    }
}
