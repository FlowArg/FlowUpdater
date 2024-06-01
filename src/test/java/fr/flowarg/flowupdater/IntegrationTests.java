package fr.flowarg.flowupdater;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowupdater.utils.UpdaterOptions;
import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IntegrationTests
{
    private static final Path UPDATE_DIR = Paths.get("testing_directory");

    @BeforeAll
    public static void setup() throws Exception
    {
        Updates.UPDATE_DIR = UPDATE_DIR;
        Files.createDirectory(UPDATE_DIR);
    }

    @AfterAll
    public static void cleanup() throws Exception
    {
        FileUtils.deleteDirectory(UPDATE_DIR);
    }

    @Order(1)
    @Test
    public void testWithVanillaUsage() throws Exception
    {
        final Updates.Result result = Updates.vanillaUsage();
        this.basicAssertions(result.updateDir, result.error, result.version);
    }

    @Order(2)
    @Test
    public void testWithNewForgeUsage() throws Exception
    {
        final Updates.Result result = Updates.testWithNewForgeUsage();
        final String vanillaForge = result.version + "-" + result.modLoaderVersion;

        this.basicAssertions(result.updateDir, result.error, result.version);

        assertTrue(Files.exists(result.updateDir.resolve(String.format("%s-forge-%s.json", result.version, result.modLoaderVersion))));
        assertTrue(Files.exists(result.updateDir.resolve("libraries").resolve("net").resolve("minecraftforge").resolve("forge").resolve(vanillaForge).resolve("forge-" + vanillaForge + "-universal.jar")));
    }

    @Order(3)
    @Test
    public void testWithVeryOldForgeUsage() throws Exception
    {
        final Updates.Result result = Updates.testWithVeryOldForgeUsage();
        final String full = result.version + '-' + result.modLoaderVersion + '-' + result.version;

        this.basicAssertions(result.updateDir, result.error, result.version);

        assertTrue(Files.exists(result.updateDir.resolve(result.version + "-Forge" + result.modLoaderVersion + "-" + result.version + ".json")));
        assertTrue(Files.exists(result.updateDir.resolve("libraries").resolve("net").resolve("minecraftforge").resolve("forge").resolve(full).resolve("forge-" + full + ".jar")));
    }

    @Order(4)
    @Test
    public void testWithOldForgeUsage() throws Exception
    {
        final Updates.Result result = Updates.testWithOldForgeUsage();
        final String full = result.version + '-' + result.modLoaderVersion;

        this.basicAssertions(result.updateDir, result.error, result.version);

        assertTrue(Files.exists(result.updateDir.resolve(result.version + "-forge" + full + ".json")));
        assertTrue(Files.exists(result.updateDir.resolve("libraries").resolve("net").resolve("minecraftforge").resolve("forge").resolve(full).resolve("forge-" + full + ".jar")));
    }

    @Order(5)
    @Test
    public void testWithFabric() throws Exception
    {
        final Updates.Result result = Updates.testWithFabric();

        this.basicAssertions(result.updateDir, result.error, result.version);

        assertTrue(Files.exists(result.updateDir.resolve("libraries").resolve("net").resolve("fabricmc").resolve("fabric-loader")));
    }

    @Order(6)
    @Test
    public void testWithQuilt() throws Exception
    {
        if(Integer.parseInt(System.getProperty("java.version").split("\\.")[0]) < 17)
        {
            System.out.println("Skipping test with Quilt because Java version is < 17");
            return;
        }

        final Updates.Result result = Updates.testWithQuilt(new UpdaterOptions.UpdaterOptionsBuilder().build());

        this.basicAssertions(result.updateDir, result.error, result.version);

        assertTrue(Files.exists(result.updateDir.resolve("libraries").resolve("org").resolve("quiltmc").resolve("quilt-loader")));
    }

    @Order(6)
    @Test
    public void testWithFabric119() throws Exception
    {
        final Updates.Result result = Updates.testWithFabric119();

        this.basicAssertions(result.updateDir, result.error, result.version, false);
        assertTrue(Files.exists(result.updateDir.resolve("libraries").resolve("net").resolve("fabricmc").resolve("fabric-loader")));
    }

    @Order(8)
    @Test
    public void testWithNeoForgeUsage() throws Exception
    {
        final Updates.Result result = Updates.testWithNeoForgeUsage();

        this.basicAssertions(result.updateDir, result.error, result.version, false);

        assertTrue(Files.exists(result.updateDir.resolve(String.format("neoforge-%s.json", result.modLoaderVersion))));
        assertTrue(Files.exists(result.updateDir.resolve("libraries").resolve("net").resolve("neoforged").resolve("neoforge").resolve(result.modLoaderVersion).resolve("neoforge-" + result.modLoaderVersion + "-universal.jar")));
    }

    private void basicAssertions(Path updateDir, boolean error, String version) throws Exception
    {
        this.basicAssertions(updateDir, error, version, true);
    }

    private void basicAssertions(Path updateDir, boolean error, String version, boolean natives) throws Exception
    {
        assertFalse(error);
        assertTrue(Files.exists(updateDir.resolve(version + ".json")));
        assertTrue(Files.exists(updateDir.resolve("client.jar")));

        if(natives)
        {
            final Path nativesDir = updateDir.resolve("natives");
            assertTrue(Files.exists(nativesDir));
            assertTrue(Files.isDirectory(nativesDir));
            assertFalse(FileUtils.list(nativesDir).isEmpty());
        }

        final Path librariesDir = updateDir.resolve("libraries");
        assertTrue(Files.exists(librariesDir));
        assertTrue(Files.isDirectory(librariesDir));
        assertFalse(FileUtils.list(librariesDir).isEmpty());
        FileUtils.list(librariesDir).forEach(path -> assertTrue(Files.isDirectory(path)));
        assertTrue(FileUtils.list(updateDir.resolve("assets").resolve("objects")).size() > 200);
    }
}
