package fr.flowarg.flowupdater.integrations.modrinthintegration;

import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.download.json.ModrinthModPackInfo;
import fr.flowarg.flowupdater.download.json.ModrinthVersionInfo;

import java.util.List;

public interface IModrinthFeaturesUser
{
    /**
     * Get all modrinth mods to update.
     * @return all modrinth mods.
     */
    List<ModrinthVersionInfo> getModrinthMods();

    /**
     * Get information about the mod pack to update.
     * @return mod pack's information.
     */
    ModrinthModPackInfo getModrinthModPackInfo();

    /**
     * Get the modrinth mod pack.
     * @return the modrinth mod pack.
     */
    ModrinthModPack getModrinthModPack();

    /**
     * Define the modrinth mod pack.
     * @param modrinthModPack the modrinth mod pack.
     */
    void setModrinthModPack(ModrinthModPack modrinthModPack);

    /**
     * Define all modrinth mods to update.
     * @param modrinthMods modrinth mods to define.
     */
    void setAllModrinthMods(List<Mod> modrinthMods);
}
