package fr.flowarg.flowupdater.utils;

import fr.antoineok.flowupdater.optifineplugin.Optifine;
import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowupdater.curseforgeplugin.CurseMod;
import fr.flowarg.flowupdater.download.json.Mod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

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
            final Path modsDir = (Path)parameters[0];
            final List<Mod> mods = (List<Mod>)parameters[1];
            final boolean cursePluginLoaded = (boolean)parameters[2];
            final List<Object> allCurseMods = (List<Object>)parameters[3];
            final boolean optifinePluginLoaded = (boolean)parameters[4];
            final Object optifineParam = parameters[5];

            final Set<Path> badFiles = new HashSet<>();
            final List<Path> verifiedFiles = new ArrayList<>();
            Arrays.stream(this.modsToIgnore).forEach(fileName -> verifiedFiles.add(modsDir.resolve(fileName)));

            for(Path fileInDir : FileUtils.list(modsDir).stream().filter(path -> !Files.isDirectory(path)).collect(Collectors.toList()))
            {
                if(verifiedFiles.contains(fileInDir))
                    continue;

                if((mods == null || mods.isEmpty()) && (allCurseMods == null || allCurseMods.isEmpty()) && optifineParam == null)
                {
                    if (!verifiedFiles.contains(fileInDir))
                        badFiles.add(fileInDir);
                }
                else
                {
                    if (cursePluginLoaded)
                    {
                        for (Object obj : allCurseMods)
                        {
                            final CurseMod mod = (CurseMod)obj;
                            if (mod.getName().equalsIgnoreCase(fileInDir.getFileName().toString()))
                            {
                                if (mod.getMd5().contains("-"))
                                {
                                    badFiles.remove(fileInDir);
                                    verifiedFiles.add(fileInDir);
                                }
                                else if (FileUtils.getMD5(fileInDir).equalsIgnoreCase(mod.getMd5()) && FileUtils.getFileSizeBytes(fileInDir) == mod.getLength())
                                {
                                    badFiles.remove(fileInDir);
                                    verifiedFiles.add(fileInDir);
                                }
                                else badFiles.add(fileInDir);
                            }
                            else
                            {
                                if (!verifiedFiles.contains(fileInDir)) badFiles.add(fileInDir);
                            }
                        }
                    }

                    if (optifinePluginLoaded)
                    {
                        final Optifine optifine = (Optifine)optifineParam;
                        if (optifine != null)
                        {
                            if (optifine.getName().equalsIgnoreCase(fileInDir.getFileName().toString()))
                            {
                                if (FileUtils.getFileSizeBytes(fileInDir) == optifine.getSize())
                                {
                                    badFiles.remove(fileInDir);
                                    verifiedFiles.add(fileInDir);
                                }
                                else badFiles.add(fileInDir);
                            }
                            else
                            {
                                if (!verifiedFiles.contains(fileInDir)) badFiles.add(fileInDir);
                            }
                        }
                    }

                    for (Mod mod : mods)
                    {
                        if (mod.getName().equalsIgnoreCase(fileInDir.getFileName().toString()))
                        {
                            if (FileUtils.getSHA1(fileInDir).equalsIgnoreCase(mod.getSha1()) && FileUtils.getFileSizeBytes(fileInDir) == mod.getSize())
                            {
                                badFiles.remove(fileInDir);
                                verifiedFiles.add(fileInDir);
                            }
                            else badFiles.add(fileInDir);
                        }
                        else
                        {
                            if (!verifiedFiles.contains(fileInDir)) badFiles.add(fileInDir);
                        }
                    }
                }
            }

            badFiles.forEach(path -> {
                try
                {
                    Files.delete(path);
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            });
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
