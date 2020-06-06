package fr.flowarg.flowupdater.minecraft;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import fr.flowarg.flowupdater.minecraft.FlowArgMinecraftUpdater.SlimUpdaterBuilder;
import fr.flowarg.flowupdater.minecraft.versions.IVersion;
import fr.flowarg.flowupdater.minecraft.versions.OldForgeVersion;

public class FlowArgMinecraftUpdaterTest
{
    @Test
    public void test()
    {
        try
        {
            final IVersion.Builder builder = new IVersion.Builder("1.6.4");
            final IVersion version = builder.build(false);
            final FlowArgMinecraftUpdater updater = SlimUpdaterBuilder.build(version, true);
            updater.setForgeVersion(new OldForgeVersion("1.6.4-9.11.1.1345", version, updater.getLogger()));
            updater.update(new File("/home/flow/Bureau/test/"), false);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
