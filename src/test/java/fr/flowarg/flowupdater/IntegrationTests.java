package fr.flowarg.flowupdater;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowupdater.versions.*;
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

    @BeforeEach
    public void setup() throws Exception
    {
        FileUtils.deleteDirectory(UPDATE_DIR);
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
        boolean error = false;
        try
        {
            final VanillaVersion version = new VanillaVersion.VanillaVersionBuilder()
                    .withName("1.18.1")
                    .withVersionType(VersionType.VANILLA)
                    .build();

            final FlowUpdater updater = new FlowUpdater.FlowUpdaterBuilder()
                    .withVanillaVersion(version)
                    .build();

            updater.update(UPDATE_DIR);
        }
        catch (Exception e)
        {
            error = true;
            e.printStackTrace();
        }

        this.basicAssertions(error, "1.18.1");
    }

    @Order(2)
    @Test
    public void testWithNewForgeUsage() throws Exception
    {
        boolean error = false;
        final String vanilla = "1.18.1";
        final String forge = "39.0.0";
        final String vanillaForge = vanilla + "-" + forge;

        try
        {
            final VanillaVersion version = new VanillaVersion.VanillaVersionBuilder()
                    .withName(vanilla)
                    .withVersionType(VersionType.FORGE)
                    .build();

            final AbstractForgeVersion forgeVersion = new ForgeVersionBuilder(ForgeVersionBuilder.ForgeVersionType.NEW)
                    .withForgeVersion(vanillaForge)
                    .build();

            final FlowUpdater updater = new FlowUpdater.FlowUpdaterBuilder()
                    .withVanillaVersion(version)
                    .withForgeVersion(forgeVersion)
                    .build();

            updater.update(UPDATE_DIR);
        }
        catch (Exception e)
        {
            error = true;
            e.printStackTrace();
        }

        this.basicAssertions(error, vanilla);
        assertTrue(Files.exists(UPDATE_DIR.resolve(String.format("%s-forge-%s.json", vanilla, forge))));
        assertTrue(Files.exists(UPDATE_DIR.resolve("libraries").resolve("net").resolve("minecraftforge").resolve("forge").resolve(vanillaForge).resolve("forge-" + vanillaForge + "-universal.jar")));
    }

    @Order(3)
    @Test
    public void testWithOldForgeUsage() throws Exception
    {
        boolean error = false;
        final String vanilla = "1.7.10";

        try
        {
            final VanillaVersion version = new VanillaVersion.VanillaVersionBuilder()
                    .withName(vanilla)
                    .withVersionType(VersionType.FORGE)
                    .build();

            final AbstractForgeVersion forgeVersion = new ForgeVersionBuilder(ForgeVersionBuilder.ForgeVersionType.OLD)
                    .withForgeVersion("1.7.10-10.13.4.1614-1.7.10")
                    .build();

            final FlowUpdater updater = new FlowUpdater.FlowUpdaterBuilder()
                    .withVanillaVersion(version)
                    .withForgeVersion(forgeVersion)
                    .build();

            updater.update(UPDATE_DIR);
        }
        catch (Exception e)
        {
            error = true;
            e.printStackTrace();
        }

        this.basicAssertions(error, vanilla);
        assertTrue(Files.exists(UPDATE_DIR.resolve("1.7.10-Forge10.13.4.1614-1.7.10.json")));
        assertTrue(Files.exists(UPDATE_DIR.resolve("libraries").resolve("net").resolve("minecraftforge").resolve("forge").resolve("1.7.10-10.13.4.1614-1.7.10").resolve("forge-" + "1.7.10-10.13.4.1614-1.7.10.jar")));
    }

    @Order(4)
    @Test
    public void testWithFabric() throws Exception
    {
        boolean error = false;
        try
        {
            final VanillaVersion version = new VanillaVersion.VanillaVersionBuilder()
                    .withName("1.18.1")
                    .withVersionType(VersionType.FABRIC)
                    .build();

            final FabricVersion fabricVersion = new FabricVersion.FabricVersionBuilder().build();

            final FlowUpdater updater = new FlowUpdater.FlowUpdaterBuilder()
                    .withVanillaVersion(version)
                    .withFabricVersion(fabricVersion)
                    .build();

            updater.update(UPDATE_DIR);
        }
        catch (Exception e)
        {
            error = true;
            e.printStackTrace();
        }

        this.basicAssertions(error, "1.18.1");
        assertTrue(Files.exists(UPDATE_DIR.resolve("libraries").resolve("net").resolve("fabricmc").resolve("fabric-loader")));
    }

    private void basicAssertions(boolean error, String version) throws Exception
    {
        assertFalse(error);
        assertTrue(Files.exists(UPDATE_DIR.resolve(version + ".json")));
        assertTrue(Files.exists(UPDATE_DIR.resolve("client.jar")));
        final Path nativesDir = UPDATE_DIR.resolve("natives");
        assertTrue(Files.exists(nativesDir));
        assertTrue(Files.isDirectory(nativesDir));
        assertTrue(FileUtils.list(nativesDir).size() > 0);
        final Path librariesDir = UPDATE_DIR.resolve("libraries");
        assertTrue(Files.exists(librariesDir));
        assertTrue(Files.isDirectory(librariesDir));
        assertTrue(FileUtils.list(librariesDir).size() > 0);
        FileUtils.list(librariesDir).forEach(path -> assertTrue(Files.isDirectory(path)));
        assertTrue(FileUtils.list(UPDATE_DIR.resolve("assets").resolve("objects")).size() > 200);
    }
}
