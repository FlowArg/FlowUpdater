package fr.flowarg.flowupdater.utils;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.integrations.modrinthintegration.ModrinthModPack;
import fr.flowarg.flowupdater.integrations.optifineintegration.OptiFine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A file deleter designed to check mods.
 */
public class ModFileDeleter implements IFileDeleter
{
    private final boolean useFileDeleter;
    private final String[] modsToIgnore;
    private final Pattern modsToIgnorePattern;

    public ModFileDeleter(boolean useFileDeleter, String... modsToIgnore)
    {
        this.useFileDeleter = useFileDeleter;
        this.modsToIgnore = modsToIgnore;
        this.modsToIgnorePattern = null;
    }

    public ModFileDeleter(String... modsToIgnore)
    {
        this(true, modsToIgnore);
    }

    public ModFileDeleter(boolean useFileDeleter, Pattern modsToIgnorePattern)
    {
        this.useFileDeleter = useFileDeleter;
        this.modsToIgnore = null;
        this.modsToIgnorePattern = modsToIgnorePattern;
    }

    public ModFileDeleter(Pattern modsToIgnorePattern)
    {
        this(true, modsToIgnorePattern);
    }

    /**
     * Delete all bad files in the provided directory.
     * @param logger the logger.
     * @param modsDir the mod's folder.
     * @param mods the mods list.
     * @param optiFine the OptiFine object. (SPECIFIC USE CASE)
     * @param modrinthModPack the modrinth mod pack. (SPECIFIC USE CASE)
     * @throws Exception thrown if an error occurred
     */
    public void delete(ILogger logger, Path modsDir, List<Mod> mods, OptiFine optiFine, ModrinthModPack modrinthModPack) throws Exception
    {
        if(!this.isUseFileDeleter())
            return;

        final Set<Path> badFiles = new HashSet<>();
        final List<Path> verifiedFiles = new ArrayList<>();

        if(this.modsToIgnore != null)
            Arrays.stream(this.modsToIgnore).forEach(fileName -> verifiedFiles.add(modsDir.resolve(fileName)));
        else if(this.modsToIgnorePattern != null)
        {
            FileUtils.list(modsDir).stream().filter(path -> !Files.isDirectory(path)).forEach(path -> {
                if(this.modsToIgnorePattern.matcher(path.getFileName().toString()).matches())
                    verifiedFiles.add(path);
            });
        }

        if(modrinthModPack != null)
            modrinthModPack.getBuiltInMods().forEach(mod -> verifiedFiles.add(modsDir.resolve(mod.getName())));

        for(Path fileInDir : FileUtils.list(modsDir).stream().filter(path -> !Files.isDirectory(path)).collect(Collectors.toList()))
        {
            if(verifiedFiles.contains(fileInDir))
                continue;

            if(mods.isEmpty() && optiFine == null)
            {
                if (!verifiedFiles.contains(fileInDir))
                    badFiles.add(fileInDir);
            }
            else
            {
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
                        if (Files.size(fileInDir) == mod.getSize() && (mod.getSha1().isEmpty() || FileUtils.getSHA1(fileInDir).equalsIgnoreCase(mod.getSha1())))
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
                Files.deleteIfExists(path);
            } catch (Exception e)
            {
                logger.printStackTrace(e);
            }
        });
        badFiles.clear();
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
