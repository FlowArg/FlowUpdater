package fr.flowarg.flowupdater.utils;

import fr.flowarg.flowupdater.curseforgeplugin.CurseMod;
import fr.flowarg.flowupdater.download.json.Mod;

import java.io.File;
import java.util.*;

import static fr.flowarg.flowio.FileUtils.*;

public class ModFileDeleter implements IFileDeleter
{
    private final boolean useFileDeleter;
    private final String[] modsToIgnore;

    public ModFileDeleter(boolean useFileDeleter, String... modsToIgnore)
    {
        this.useFileDeleter = useFileDeleter;
        this.modsToIgnore = modsToIgnore;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void delete(Object... parameters) throws Exception
    {
        if(this.isUseFileDeleter())
        {
            final File modsDir = (File)parameters[0];
            final List<Mod> mods = (List<Mod>)parameters[1];
            final boolean cursePluginLoaded = (boolean)parameters[2];
            final List<Object> allCurseMods = (List<Object>)parameters[3];

            final Set<File> badFiles = new HashSet<>();
            final List<File> verifiedFiles = new ArrayList<>();
            Arrays.stream(this.modsToIgnore).forEach(fileName -> verifiedFiles.add(new File(modsDir, fileName)));
            for(File fileInDir : modsDir.listFiles())
            {
                if(!fileInDir.isDirectory())
                {
                    if(mods.isEmpty() && allCurseMods.isEmpty())
                    {
                        if(!verifiedFiles.contains(fileInDir))
                            badFiles.add(fileInDir);
                        break;
                    }
                    else
                    {
                        if(cursePluginLoaded)
                        {
                            for(Object obj : allCurseMods)
                            {
                                final CurseMod mod = (CurseMod)obj;
                                if(mod.getName().equalsIgnoreCase(fileInDir.getName()))
                                {
                                    if(getMD5ofFile(fileInDir).equalsIgnoreCase(mod.getMd5()) && getFileSizeBytes(fileInDir) == mod.getLength())
                                    {
                                        badFiles.remove(fileInDir);
                                        verifiedFiles.add(fileInDir);
                                    }
                                    else badFiles.add(fileInDir);
                                }
                                else
                                {
                                    if(!verifiedFiles.contains(fileInDir))
                                        badFiles.add(fileInDir);
                                }
                            }
                        }

                        for(Mod mod : mods)
                        {
                            if(mod.getName().equalsIgnoreCase(fileInDir.getName()))
                            {
                                if(getSHA1(fileInDir).equalsIgnoreCase(mod.getSha1()) && getFileSizeBytes(fileInDir) == mod.getSize())
                                {
                                    badFiles.remove(fileInDir);
                                    verifiedFiles.add(fileInDir);
                                }
                                else badFiles.add(fileInDir);
                            }
                            else
                            {
                                if(!verifiedFiles.contains(fileInDir))
                                    badFiles.add(fileInDir);
                            }
                        }
                    }
                }
            }

            badFiles.forEach(File::delete);
            badFiles.clear();
        }
    }

    public boolean isUseFileDeleter()
    {
        return this.useFileDeleter;
    }

    public String[] getModsToIgnore()
    {
        return this.modsToIgnore;
    }
}
