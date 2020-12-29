package fr.flowarg.flowupdater.curseforgeplugin;

public class CurseMod
{
    public static final CurseMod BAD = new CurseMod("", "", "", -1);

    private final String name;
    private final String downloadURL;
    private final String md5;
    private final int length;

    CurseMod(String name, String downloadURL, String md5, int length)
    {
        this.name = name;
        this.downloadURL = downloadURL;
        this.md5 = md5;
        this.length = length;
    }

    public String getName()
    {
        return this.name;
    }

    public String getDownloadURL()
    {
        return this.downloadURL;
    }

    public String getMd5()
    {
        return this.md5;
    }

    public int getLength()
    {
        return this.length;
    }
}
