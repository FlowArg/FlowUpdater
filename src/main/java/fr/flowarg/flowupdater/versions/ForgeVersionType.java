package fr.flowarg.flowupdater.versions;

public enum ForgeVersionType
{
    /** 1.20.1 to ?? */
    NEO_FORGE("NeoForge", ""),
    /** 1.12.2-14.23.5.2851 to 1.20 */
    NEW("new Forge", ""),
    /** 1.7 to 1.12.2 */
    OLD("old Forge", "old");

    private final String displayName;
    private final String patches;

    ForgeVersionType(String displayName, String patches)
    {
        this.displayName = displayName;
        this.patches = patches;
    }

    public String getDisplayName()
    {
        return this.displayName;
    }

    public String getPatches()
    {
        return this.patches;
    }
}
