package fr.flowarg.flowupdater.integrations.curseforgeintegration;

import fr.flowarg.flowupdater.download.json.CurseFileInfo;
import fr.flowarg.flowupdater.download.json.CurseModPackInfo;
import fr.flowarg.flowupdater.download.json.Mod;

import java.util.List;

/**
 * This class represents an object that using CurseForge features.
 */
public interface ICurseFeaturesUser
{
    /**
     * Get all curse mods to update.
     * @return all curse mods.
     */
    List<CurseFileInfo> getCurseMods();

    /**
     * Get information about the mod pack to update.
     * @return mod pack's information.
     */
    CurseModPackInfo getCurseModPackInfo();

    /**
     * Define all curse mods to update.
     * @param curseMods curse mods to define.
     */
    void setAllCurseMods(List<Mod> curseMods);
}
