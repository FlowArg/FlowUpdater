package fr.flowarg.flowupdater.versions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.flowarg.flowio.FileUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class ModLoaderUtils
{
    @Contract(pure = true)
    public static @NotNull String buildJarUrl(String baseUrl, @NotNull String group, String artifact, String version)
    {
        return buildJarUrl(baseUrl, group, artifact, version, "");
    }

    @Contract(pure = true)
    public static @NotNull String buildJarUrl(String baseUrl, @NotNull String group, String artifact, String version, String classifier)
    {
        return baseUrl + group.replace(".", "/") + "/" + artifact + "/" + version + "/" + artifact + "-" + version + classifier + ".jar";
    }

    public static @NotNull Path buildLibraryPath(@NotNull Path installDir, @NotNull String group, String artifact, String version)
    {
        return installDir.resolve("libraries")
                .resolve(group.replace(".", installDir.getFileSystem().getSeparator()))
                .resolve(artifact)
                .resolve(version)
                .resolve(artifact + "-" + version + ".jar");
    }

    public static void fakeContext(@NotNull Path dirToInstall, String vanilla) throws Exception
    {
        final Path fakeProfiles = dirToInstall.resolve("launcher_profiles.json");

        Files.write(fakeProfiles, "{}".getBytes(StandardCharsets.UTF_8));

        final Path versions = dirToInstall.resolve("versions");
        if(Files.notExists(versions))
            Files.createDirectories(versions);

        final Path vanillaVersion = versions.resolve(vanilla);
        if(Files.notExists(vanillaVersion))
            Files.createDirectories(vanillaVersion);

        Files.copy(
                dirToInstall.resolve("client.jar"),
                vanillaVersion.resolve(vanilla + ".jar"),
                StandardCopyOption.REPLACE_EXISTING
        );
    }

    public static void removeFakeContext(@NotNull Path dirToInstall) throws Exception
    {
        FileUtils.deleteDirectory(dirToInstall.resolve("versions"));
        Files.deleteIfExists(dirToInstall.resolve("launcher_profiles.json"));
    }

    public static @NotNull List<ParsedLibrary> parseNewVersionInfo(Path installDir, @NotNull JsonObject versionInfo) throws Exception
    {
        final List<ParsedLibrary> parsedLibraries = new ArrayList<>();

        final JsonArray libraries = versionInfo.getAsJsonArray("libraries");

        for (final JsonElement libraryElement : libraries)
        {
            final JsonObject library = libraryElement.getAsJsonObject();
            final String name = library.get("name").getAsString();
            final JsonObject downloads = library.getAsJsonObject("downloads");
            final JsonObject artifact = downloads.getAsJsonObject("artifact");

            final String path = artifact.get("path").getAsString();
            final String sha1 = artifact.get("sha1").getAsString();
            final String url = artifact.get("url").getAsString();

            final Path libraryPath = installDir.resolve("libraries")
                    .resolve(path.replace("/", installDir.getFileSystem().getSeparator()));

            final boolean installed = Files.exists(libraryPath) &&
                    FileUtils.getSHA1(libraryPath).equalsIgnoreCase(sha1);

            parsedLibraries.add(new ParsedLibrary(libraryPath, url.isEmpty() ? null : new URL(url), name, installed));
        }

        return parsedLibraries;
    }
}
