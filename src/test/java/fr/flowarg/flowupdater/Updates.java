package fr.flowarg.flowupdater;

import fr.flowarg.flowupdater.utils.UpdaterOptions;
import fr.flowarg.flowupdater.versions.VanillaVersion;
import fr.flowarg.flowupdater.versions.fabric.FabricVersion;
import fr.flowarg.flowupdater.versions.fabric.FabricVersionBuilder;
import fr.flowarg.flowupdater.versions.fabric.QuiltVersion;
import fr.flowarg.flowupdater.versions.fabric.QuiltVersionBuilder;
import fr.flowarg.flowupdater.versions.forge.ForgeVersion;
import fr.flowarg.flowupdater.versions.forge.ForgeVersionBuilder;
import fr.flowarg.flowupdater.versions.neoforge.NeoForgeVersion;
import fr.flowarg.flowupdater.versions.neoforge.NeoForgeVersionBuilder;

import java.nio.file.Path;

public class Updates
{
    public static Path UPDATE_DIR;

    public static Result vanillaUsage()
    {
        final String version = "1.18.2";
        final Path updateDir = UPDATE_DIR.resolve("vanilla-" + version);

        boolean error = false;

        try
        {
            final VanillaVersion vanillaVersion = new VanillaVersion.VanillaVersionBuilder()
                    .withName(version)
                    .build();

            final FlowUpdater updater = new FlowUpdater.FlowUpdaterBuilder()
                    .withVanillaVersion(vanillaVersion)
                    .build();

            updater.update(updateDir);
        }
        catch (Exception e)
        {
            error = true;
            e.printStackTrace();
        }

        return new Result(updateDir, error, version);
    }

    public static Result testWithNewForgeUsage()
    {
        boolean error = false;
        final String vanilla = "1.18.2";
        final String forge = "40.2.21";
        final String vanillaForge = vanilla + "-" + forge;
        final Path updateDir = UPDATE_DIR.resolve("new_forge-" + vanillaForge);

        try
        {
            final VanillaVersion version = new VanillaVersion.VanillaVersionBuilder()
                    .withName(vanilla)
                    .build();

            final ForgeVersion forgeVersion = new ForgeVersionBuilder()
                    .withForgeVersion(vanillaForge)
                    .build();

            final FlowUpdater updater = new FlowUpdater.FlowUpdaterBuilder()
                    .withVanillaVersion(version)
                    .withModLoaderVersion(forgeVersion)
                    .build();

            updater.update(updateDir);
        }
        catch (Exception e)
        {
            error = true;
            e.printStackTrace();
        }

        return new Result(updateDir, error, vanilla, forge);
    }

    public static Result testWithVeryOldForgeUsage()
    {
        boolean error = false;
        final String vanilla = "1.7.10";
        final String forge = "10.13.4.1614";
        final String full = vanilla + '-' + forge + '-' + vanilla;
        final Path updateDir = UPDATE_DIR.resolve("forge-" + vanilla);

        try
        {
            final VanillaVersion vanillaVersion = new VanillaVersion.VanillaVersionBuilder()
                    .withName(vanilla)
                    .build();

            final ForgeVersion forgeVersion = new ForgeVersionBuilder()
                    .withForgeVersion(full)
                    .build();

            final FlowUpdater updater = new FlowUpdater.FlowUpdaterBuilder()
                    .withVanillaVersion(vanillaVersion)
                    .withModLoaderVersion(forgeVersion)
                    .build();

            updater.update(updateDir);
        }
        catch (Exception e)
        {
            error = true;
            e.printStackTrace();
        }

        return new Result(updateDir, error, vanilla, forge);
    }

    public static Result testWithOldForgeUsage()
    {
        boolean error = false;
        final String vanilla = "1.8.9";
        final String forge = "11.15.1.2318-" + vanilla;
        final Path updateDir = UPDATE_DIR.resolve("forge-" + vanilla);

        try
        {
            final VanillaVersion vanillaVersion = new VanillaVersion.VanillaVersionBuilder()
                    .withName(vanilla)
                    .build();

            final ForgeVersion forgeVersion = new ForgeVersionBuilder()
                    .withForgeVersion(vanilla + '-' + forge)
                    .build();

            final FlowUpdater updater = new FlowUpdater.FlowUpdaterBuilder()
                    .withVanillaVersion(vanillaVersion)
                    .withModLoaderVersion(forgeVersion)
                    .build();

            updater.update(updateDir);
        }
        catch (Exception e)
        {
            error = true;
            e.printStackTrace();
        }

        return new Result(updateDir, error, vanilla, forge);
    }

    public static Result testWithFabric()
    {
        boolean error = false;
        final String version = "1.18.2";
        final Path updateDir = UPDATE_DIR.resolve("fabric-" + version);

        String fabric = "";

        try
        {
            final VanillaVersion vanillaVersion = new VanillaVersion.VanillaVersionBuilder()
                    .withName(version)
                    .build();

            final FabricVersion fabricVersion = new FabricVersionBuilder()
                    .build();

            fabric = fabricVersion.getModLoaderVersion();

            final FlowUpdater updater = new FlowUpdater.FlowUpdaterBuilder()
                    .withVanillaVersion(vanillaVersion)
                    .withModLoaderVersion(fabricVersion)
                    .build();

            updater.update(updateDir);
        }
        catch (Exception e)
        {
            error = true;
            e.printStackTrace();
        }

        return new Result(updateDir, error, version, fabric);
    }

    public static Result testWithQuilt(UpdaterOptions opts)
    {
        boolean error = false;
        String version = "1.18.2";
        final Path updateDir = UPDATE_DIR.resolve("quilt-" + version);

        String quilt = "";

        try
        {
            final VanillaVersion vanillaVersion = new VanillaVersion.VanillaVersionBuilder()
                    .withName(version)
                    .build();

            final QuiltVersion quiltVersion = new QuiltVersionBuilder()
                    .build();

            quilt = quiltVersion.getModLoaderVersion();

            final FlowUpdater updater = new FlowUpdater.FlowUpdaterBuilder()
                    .withVanillaVersion(vanillaVersion)
                    .withModLoaderVersion(quiltVersion)
                    .withUpdaterOptions(opts)
                    .build();

            updater.update(updateDir);
        }
        catch (Exception e)
        {
            error = true;
            e.printStackTrace();
        }

        return new Result(updateDir, error, version, quilt);
    }

    public static Result testWithFabric119()
    {
        boolean error = false;
        String version = "1.19.4";
        final Path updateDir = UPDATE_DIR.resolve("fabric-" + version);

        String fabric = "";

        try
        {
            final VanillaVersion vanillaVersion = new VanillaVersion.VanillaVersionBuilder()
                    .withName(version)
                    .build();

            final FabricVersion fabricVersion = new FabricVersionBuilder()
                    .build();

            fabric = fabricVersion.getModLoaderVersion();

            final FlowUpdater updater = new FlowUpdater.FlowUpdaterBuilder()
                    .withVanillaVersion(vanillaVersion)
                    .withModLoaderVersion(fabricVersion)
                    .build();

            updater.update(updateDir);
        }
        catch (Exception e)
        {
            error = true;
            e.printStackTrace();
        }

        return new Result(updateDir, error, version, fabric);
    }

    public static Result testWithNeoForgeUsage()
    {
        boolean error = false;
        final String vanilla = "1.20.4";
        final String neoForge = "20.4.235";
        final Path updateDir = UPDATE_DIR.resolve("neo_forge-" + vanilla);

        try
        {
            final VanillaVersion version = new VanillaVersion.VanillaVersionBuilder()
                    .withName(vanilla)
                    .build();

            final NeoForgeVersion neoForgeVersion = new NeoForgeVersionBuilder()
                    .withNeoForgeVersion(neoForge)
                    .build();

            final FlowUpdater updater = new FlowUpdater.FlowUpdaterBuilder()
                    .withVanillaVersion(version)
                    .withModLoaderVersion(neoForgeVersion)
                    .build();

            updater.update(updateDir);
        }
        catch (Exception e)
        {
            error = true;
            e.printStackTrace();
        }

        return new Result(updateDir, error, vanilla, neoForge);
    }

    public static Result testWithNeoForgeUsage2()
    {
        boolean error = false;
        final String vanilla = "1.21.1";
        final String neoForge = "21.1.18";
        final Path updateDir = UPDATE_DIR.resolve("neo_forge-" + vanilla);

        try
        {
            final VanillaVersion version = new VanillaVersion.VanillaVersionBuilder()
                    .withName(vanilla)
                    .build();

            final NeoForgeVersion neoForgeVersion = new NeoForgeVersionBuilder()
                    .withNeoForgeVersion(neoForge)
                    .build();

            final FlowUpdater updater = new FlowUpdater.FlowUpdaterBuilder()
                    .withVanillaVersion(version)
                    .withModLoaderVersion(neoForgeVersion)
                    .build();

            updater.update(updateDir);
        }
        catch (Exception e)
        {
            error = true;
            e.printStackTrace();
        }

        return new Result(updateDir, error, vanilla, neoForge);
    }

    public static class Result
    {
        public final Path updateDir;
        public final boolean error;
        public final String version;
        public final String modLoaderVersion;

        public Result(Path updateDir, boolean error, String version, String modLoaderVersion)
        {
            this.updateDir = updateDir;
            this.error = error;
            this.version = version;
            this.modLoaderVersion = modLoaderVersion;
        }

        public Result(Path updateDir, boolean error, String version)
        {
            this.updateDir = updateDir;
            this.error = error;
            this.version = version;
            this.modLoaderVersion = null;
        }
    }

    public static Result testWithLast1122Forge()
    {
        boolean error = false;
        final String vanilla = "1.12.2";
        final String forge = "14.23.5.2860";
        final String vanillaForge = vanilla + "-" + forge;
        final Path updateDir = UPDATE_DIR.resolve("last_1122_forge-" + vanillaForge);

        try
        {
            final VanillaVersion version = new VanillaVersion.VanillaVersionBuilder()
                    .withName(vanilla)
                    .build();

            final ForgeVersion forgeVersion = new ForgeVersionBuilder()
                    .withForgeVersion(vanillaForge)
                    .build();

            final FlowUpdater updater = new FlowUpdater.FlowUpdaterBuilder()
                    .withVanillaVersion(version)
                    .withModLoaderVersion(forgeVersion)
                    .build();

            updater.update(updateDir);
        }
        catch (Exception e)
        {
            error = true;
            e.printStackTrace();
        }

        return new Result(updateDir, error, vanilla, forge);
    }

    public static Result testWith121Forge()
    {
        boolean error = false;
        final String vanilla = "1.21";
        final String forge = "51.0.29";
        final String vanillaForge = vanilla + "-" + forge;
        final Path updateDir = UPDATE_DIR.resolve("121_forge-" + vanillaForge);

        try
        {
            final VanillaVersion version = new VanillaVersion.VanillaVersionBuilder()
                    .withName(vanilla)
                    .build();

            final ForgeVersion forgeVersion = new ForgeVersionBuilder()
                    .withForgeVersion(vanillaForge)
                    .build();

            final FlowUpdater updater = new FlowUpdater.FlowUpdaterBuilder()
                    .withVanillaVersion(version)
                    .withModLoaderVersion(forgeVersion)
                    .build();

            updater.update(updateDir);
        }
        catch (Exception e)
        {
            error = true;
            e.printStackTrace();
        }

        return new Result(updateDir, error, vanilla, forge);
    }
}
