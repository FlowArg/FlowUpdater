package fr.flowarg.flowupdater;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import fr.flowarg.flowupdater.FlowUpdater.FlowUpdaterBuilder;
import fr.flowarg.flowupdater.utils.BuilderArgumentException;
import fr.flowarg.flowupdater.versions.IVanillaVersion;
import fr.flowarg.flowupdater.versions.OldForgeVersion;
import fr.flowarg.flowupdater.versions.VersionType;
import fr.flowarg.flowupdater.versions.download.Mod;

public class FlowUpdaterTest
{
    @Test
    public void test()
    {
        try
        {
            final IVanillaVersion.Builder builder = new IVanillaVersion.Builder("1.7.10");
            final IVanillaVersion version = builder.build(false, VersionType.FORGE);
            final FlowUpdater updater = new FlowUpdaterBuilder().withVersion(version).withSilentUpdate(true).build();
            final List<Mod> mods = new ArrayList<>();
            mods.add(new Mod("ironchest-1.12.2-7.0.72.847.jar", "adc4c785a484c5d5a4ab9a29e1937faeed4312dd", 956692, "https://media.forgecdn.net/files/2747/935/ironchest-1.12.2-7.0.72.847.jar"));
            updater.setForgeVersion(new OldForgeVersion("1.7.10-10.13.4.1614-1.7.10", version, updater.getLogger(), updater.getCallback(), mods).enableModFileDeleter());
            updater.update(new File("/home/flow/Bureau/test/"), false);
        }
        catch (IOException | BuilderArgumentException e)
        {
            e.printStackTrace();
        }
    }
}
