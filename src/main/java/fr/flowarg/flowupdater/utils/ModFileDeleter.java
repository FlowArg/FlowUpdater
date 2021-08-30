package fr.flowarg.flowupdater.utils;

import fr.antoineok.flowupdater.optifineplugin.OptiFine;
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

    /**
     * Delete all bad files in the provided directory.
     * @param modsDir the mod's folder.
     * @param mods the mods list.
     * @param cursePluginLoaded is the CurseForge plugin loaded ?
     * @param allCurseMods the curse's mods list.
     * @param optiFinePluginLoaded is the OptiFine plugin loaded ?
     * @param optiFineParam the OptiFine object.
     * @throws Exception thrown if an error occurred
     */
    public void delete(Path modsDir, List<Mod> mods, boolean cursePluginLoaded, List<Object> allCurseMods, boolean optiFinePluginLoaded, Object optiFineParam) throws Exception
    {
        if(!this.isUseFileDeleter()) return;

        final Set<Path> badFiles = new HashSet<>();
        final List<Path> verifiedFiles = new ArrayList<>();
        Arrays.stream(this.modsToIgnore).forEach(fileName -> verifiedFiles.add(modsDir.resolve(fileName)));

        for(Path fileInDir : FileUtils.list(modsDir).stream().filter(path -> !Files.isDirectory(path)).collect(Collectors.toList()))
        {
            if(verifiedFiles.contains(fileInDir))
                continue;

            if(mods.isEmpty() && allCurseMods.isEmpty() && optiFineParam == null)
            {
                if (!verifiedFiles.contains(fileInDir))
                    badFiles.add(fileInDir);
            }
            else
            {
                if (cursePluginLoaded)
                {
                    this.processCurseForgeMods(allCurseMods, fileInDir, badFiles, verifiedFiles);
                }

                if (optiFinePluginLoaded)
                {
                    final OptiFine optifine = (OptiFine)optiFineParam;
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
                            if (!verifiedFiles.contains(fileInDir))
                                badFiles.add(fileInDir);
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
                        if (!verifiedFiles.contains(fileInDir))
                            badFiles.add(fileInDir);
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

    private void processCurseForgeMods(List<Object> allCurseMods, Path fileInDir, Set<Path> badFiles, List<Path> verifiedFiles) throws Exception
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
                if (!verifiedFiles.contains(fileInDir))
                    badFiles.add(fileInDir);
            }
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
