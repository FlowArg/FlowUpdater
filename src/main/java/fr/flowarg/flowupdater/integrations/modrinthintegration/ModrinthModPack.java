package fr.flowarg.flowupdater.integrations.modrinthintegration;

import fr.flowarg.flowupdater.download.json.Mod;

import java.util.List;

public class ModrinthModPack
{
    private final String name;
    private final String version;
    private final List<Mod> mods;

    ModrinthModPack(String name, String version, List<Mod> mods)
    {
        this.name = name;
        this.version = version;
        this.mods = mods;
    }

    /**
     * Get the mod pack's name.
     * @return the mod pack's name.
     */
    public String getName()
    {
        return this.name;
    }


    /**
     * Get the mod pack's version.
     * @return the mod pack's version.
     */
    public String getVersion()
    {
        return this.version;
    }

    /**
     * Get the mods in the mod pack.
     * @return the mods in the mod pack.
     */
    public List<Mod> getMods()
    {
        return this.mods;
    }
}
