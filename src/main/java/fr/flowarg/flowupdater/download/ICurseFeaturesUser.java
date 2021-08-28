package fr.flowarg.flowupdater.download;

import fr.flowarg.flowupdater.download.json.CurseFileInfo;
import fr.flowarg.flowupdater.download.json.CurseModPackInfo;

import java.util.List;

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
    CurseModPackInfo getModPackInfo();

    /**
     * Define all curse mods to update.
     * @param curseMods curse mods to define.
     */
    void setAllCurseMods(List<Object> curseMods);
}
