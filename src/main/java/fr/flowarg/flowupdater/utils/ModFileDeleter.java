package fr.flowarg.flowupdater.utils;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.integrations.curseforgeintegration.CurseMod;
import fr.flowarg.flowupdater.integrations.optifineintegration.OptiFine;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A file deleter designed to check mods.
 */
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
     * @param allCurseMods the curse's mods list.
     * @param optiFine the OptiFine object.
     * @throws Exception thrown if an error occurred
     */
    public void delete(Path modsDir, List<Mod> mods, List<CurseMod> allCurseMods, OptiFine optiFine) throws Exception
    {
        if(!this.isUseFileDeleter()) return;

        final Set<Path> badFiles = new HashSet<>();
        final List<Path> verifiedFiles = new ArrayList<>();
        Arrays.stream(this.modsToIgnore).forEach(fileName -> verifiedFiles.add(modsDir.resolve(fileName)));

        for(Path fileInDir : FileUtils.list(modsDir).stream().filter(path -> !Files.isDirectory(path)).collect(Collectors.toList()))
        {
            if(verifiedFiles.contains(fileInDir))
                continue;

            if(mods.isEmpty() && allCurseMods.isEmpty() && optiFine == null)
            {
                if (!verifiedFiles.contains(fileInDir))
                    badFiles.add(fileInDir);
            }
            else
            {
                this.processCurseForgeMods(allCurseMods, fileInDir, badFiles, verifiedFiles);

                if (optiFine != null)
                {
                    if (optiFine.getName().equalsIgnoreCase(fileInDir.getFileName().toString()))
                    {
                        if (Files.size(fileInDir) == optiFine.getSize())
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

                for (Mod mod : mods)
                {
                    if (mod.getName().equalsIgnoreCase(fileInDir.getFileName().toString()))
                    {
                        if (FileUtils.getSHA1(fileInDir).equalsIgnoreCase(mod.getSha1()) &&
                                Files.size(fileInDir) == mod.getSize())
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

    private void processCurseForgeMods(@NotNull List<CurseMod> allCurseMods, Path fileInDir,
            Set<Path> badFiles, List<Path> verifiedFiles) throws Exception
    {
        for (CurseMod mod : allCurseMods)
        {
            if (mod.getName().equalsIgnoreCase(fileInDir.getFileName().toString()))
            {
                if (Files.size(fileInDir) == mod.getLength() && (mod.getSha1().isEmpty() || FileUtils.getSHA1(fileInDir).equalsIgnoreCase(mod.getSha1())))
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
