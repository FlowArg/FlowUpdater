package fr.flowarg.flowupdater.download;

import fr.flowarg.flowupdater.download.json.CurseFileInfos;
import fr.flowarg.flowupdater.download.json.CurseModPackInfos;

import java.util.List;

public interface ICurseFeaturesUser
{
    List<CurseFileInfos> getCurseMods();
    CurseModPackInfos getModPackInfos();
    void setAllCurseMods(List<Object> curseMods);
}
