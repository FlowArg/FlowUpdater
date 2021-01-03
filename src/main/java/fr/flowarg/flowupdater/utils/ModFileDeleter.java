package fr.flowarg.flowupdater.utils;

import fr.antoineok.flowupdater.optifineplugin.Optifine;
import fr.flowarg.flowio.FileUtils;
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
        if (parameters.length != 6)
            return;
        if(this.isUseFileDeleter())
        {
            final File modsDir = (File)parameters[0];
            final List<Mod> mods = (List<Mod>)parameters[1];
            final boolean cursePluginLoaded = (boolean)parameters[2];
            final List<Object> allCurseMods = (List<Object>)parameters[3];
            final boolean optifinePluginLoaded = (boolean)parameters[4];
            final Object optifineParam = parameters[5];

            final Set<File> badFiles = new HashSet<>();
            final List<File> verifiedFiles = new ArrayList<>();
            Arrays.stream(this.modsToIgnore).forEach(fileName -> verifiedFiles.add(new File(modsDir, fileName)));
            for(File fileInDir : FileUtils.list(modsDir))
            {
                if(!fileInDir.isDirectory())
                {
                    if(verifiedFiles.contains(fileInDir))
                        continue;
                    if(mods.isEmpty() && allCurseMods.isEmpty() && optifineParam == null)
                    {
                        if(!verifiedFiles.contains(fileInDir))
                            badFiles.add(fileInDir);
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
                                    if(mod.getMd5().contains("-"))
                                    {
                                        badFiles.remove(fileInDir);
                                        verifiedFiles.add(fileInDir);
                                    }
                                    else if(getMD5ofFile(fileInDir).equalsIgnoreCase(mod.getMd5()) && getFileSizeBytes(fileInDir) == mod.getLength())
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

                        if(optifinePluginLoaded)
                        {
                            final Optifine optifine = (Optifine)optifineParam;
                            if(optifine != null)
                            {
                                if(optifine.getName().equalsIgnoreCase(fileInDir.getName()))
                                {
                                    if(getFileSizeBytes(fileInDir) == optifine.getSize())
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
