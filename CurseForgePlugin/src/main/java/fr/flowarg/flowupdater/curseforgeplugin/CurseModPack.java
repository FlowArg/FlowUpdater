package fr.flowarg.flowupdater.curseforgeplugin;

import java.util.Collections;
import java.util.List;

public class CurseModPack
{
    public static final CurseModPack BAD = new CurseModPack("", "", "", Collections.emptyList(), false);

    private final String name;
    private final String version;
    private final String author;
    private final List<CurseModPackMod> mods;
    private final boolean installExtFiles;

    public CurseModPack(String name, String version, String author, List<CurseModPackMod> mods, boolean installExtFiles)
    {
        this.name = name;
        this.version = version;
        this.author = author;
        this.mods = mods;
        this.installExtFiles = installExtFiles;
    }

    public String getName()
    {
        return this.name;
    }

    public String getVersion()
    {
        return this.version;
    }

    public String getAuthor()
    {
        return this.author;
    }

    public List<CurseModPackMod> getMods()
    {
        return this.mods;
    }

    public boolean isInstallExtFiles()
    {
        return this.installExtFiles;
    }

    public static class CurseModPackMod extends CurseMod
    {
        private final boolean required;

        CurseModPackMod(String name, String downloadURL, String md5, int length, boolean required)
        {
            super(name, downloadURL, md5, length);
            this.required = required;
        }

        CurseModPackMod(CurseMod base, boolean required)
        {
            this(base.getName(), base.getDownloadURL(), base.getMd5(), base.getLength(), required);
        }

        public boolean isRequired()
        {
            return this.required;
        }
    }
}
