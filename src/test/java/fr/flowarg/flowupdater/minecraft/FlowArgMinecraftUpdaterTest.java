package fr.flowarg.flowupdater.minecraft;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import fr.flowarg.flowupdater.minecraft.FlowUpdater.SlimUpdaterBuilder;
import fr.flowarg.flowupdater.minecraft.versions.IVanillaVersion;
import fr.flowarg.flowupdater.minecraft.versions.OldForgeVersion;
import fr.flowarg.flowupdater.minecraft.versions.VersionType;
import fr.flowarg.flowupdater.minecraft.versions.download.Mod;

public class FlowArgMinecraftUpdaterTest
{
    @Test
    public void test()
    {
        try
        {
            final IVanillaVersion.Builder builder = new IVanillaVersion.Builder("1.7.10");
            final IVanillaVersion version = builder.build(false, VersionType.FORGE);
            final FlowUpdater updater = SlimUpdaterBuilder.build(version, true);
            final List<Mod> mods = new ArrayList<>();
            mods.add(new Mod("name", "sha1", 0, "https://example.org/"));
            updater.setForgeVersion(new OldForgeVersion("1.7.10-10.13.4.1614", version, updater.getLogger(), updater.getCallback(), mods));
            updater.update(new File("/home/flow/Bureau/test/"), false);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
