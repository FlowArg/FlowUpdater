package fr.flowarg.flowupdater.versions;

/**
 * Hello developers! You SHOULD (and MUST) use this class to create your own ModLoaderVersion class.
 * Why? Because it's the only way to make sure that your mod loader version is compatible with FlowUpdater.
 */
public interface OtherModLoaderVersion extends IModLoaderVersion
{
    /**
     * Just the display name of your mod loader version.
     * @return The display name of your mod loader version.
     */
    String name();
}
