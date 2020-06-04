package fr.flowarg.flowupdater.minecraft;

import fr.flowarg.flowupdater.minecraft.versions.ForgeVersion;
import fr.flowarg.flowupdater.minecraft.versions.IVersion;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class FlowArgMinecraftUpdaterTest
{
    @Test
    public void test()
    {
        try
        {
            final IVersion.Builder builder = new IVersion.Builder("1.15.2");
            final IVersion version = builder.build(false);
            final FlowArgMinecraftUpdater updater = new FlowArgMinecraftUpdater(version, true);
            updater.setForgeVersion(new ForgeVersion("1.15.2-31.1.0", version, updater.getLogger()));
            updater.update(new File("/home/flow/Bureau/test/"), false);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
