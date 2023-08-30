package fr.flowarg.flowupdater.integrations.modrinthintegration;

import fr.flowarg.flowupdater.download.json.Mod;

import java.util.ArrayList;
import java.util.List;

public class ModrinthModPack
{
    private final String name;
    private final String version;
    private final List<Mod> mods;
    private final List<Mod> builtInMods;

    ModrinthModPack(String name, String version, List<Mod> mods)
    {
        this(name, version, mods, new ArrayList<>());
    }

    ModrinthModPack(String name, String version, List<Mod> mods, List<Mod> builtInMods)
    {
        this.name = name;
        this.version = version;
        this.mods = mods;
        this.builtInMods = builtInMods;
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

    /**
     * Get the built-in mods in the mod pack.
     * Built-in mods are mods directly put in the mods folder in the .mrpack file.
     * They are not downloaded from a remote server.
     * This is not a very good way to add mods because it disables some mod verification on these mods.
     * We recommend mod pack creators to use the built-in mods feature only for mods that are not available remotely.
     * @return the built-in mods in the mod pack.
     */
    public List<Mod> getBuiltInMods()
    {
        return this.builtInMods;
    }
}
