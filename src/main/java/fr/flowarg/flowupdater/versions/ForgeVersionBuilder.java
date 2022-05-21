package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowupdater.download.json.*;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import fr.flowarg.flowupdater.utils.builderapi.BuilderArgument;
import fr.flowarg.flowupdater.utils.builderapi.BuilderException;
import fr.flowarg.flowupdater.utils.builderapi.IBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for {@link AbstractForgeVersion}
 * @author Flow Arg (FlowArg)
 */
public class ForgeVersionBuilder implements IBuilder<AbstractForgeVersion>
{
    private final ForgeVersionType type;

    public ForgeVersionBuilder(ForgeVersionType type)
    {
        this.type = type;
    }

    private final BuilderArgument<String> forgeVersionArgument = new BuilderArgument<String>("ForgeVersion").required();
    private final BuilderArgument<OptiFineInfo> optiFineArgument = new BuilderArgument<OptiFineInfo>("OptiFine").optional();
    private final BuilderArgument<List<Mod>> modsArgument = new BuilderArgument<List<Mod>>("Mods", ArrayList::new).optional();
    private final BuilderArgument<List<CurseFileInfo>> curseModsArgument = new BuilderArgument<List<CurseFileInfo>>("CurseMods", ArrayList::new).optional();
    private final BuilderArgument<List<ModrinthVersionInfo>> modrinthModsArgument = new BuilderArgument<List<ModrinthVersionInfo>>("ModrinthMods", ArrayList::new).optional();
    private final BuilderArgument<ModFileDeleter> fileDeleterArgument = new BuilderArgument<>("ModFileDeleter", () -> new ModFileDeleter(false)).optional();
    private final BuilderArgument<CurseModPackInfo> curseModPackArgument = new BuilderArgument<CurseModPackInfo>("CurseModPack").optional();
    private final BuilderArgument<ModrinthModPackInfo> modrinthPackArgument = new BuilderArgument<ModrinthModPackInfo>("ModrinthModPack").optional();

    /**
     * @param forgeVersion the Forge version you want to install.
     * @return the builder.
     */
    public ForgeVersionBuilder withForgeVersion(String forgeVersion)
    {
        this.forgeVersionArgument.set(forgeVersion);
        return this;
    }

    /**
     * Append a mods list to the version.
     * @param mods mods to append.
     * @return the builder.
     */
    public ForgeVersionBuilder withMods(List<Mod> mods)
    {
        this.modsArgument.set(mods);
        return this;
    }

    /**
     * Append a mods list to the version.
     * @param curseMods CurseForge's mods to append.
     * @return the builder.
     */
    public ForgeVersionBuilder withCurseMods(List<CurseFileInfo> curseMods)
    {
        this.curseModsArgument.set(curseMods);
        return this;
    }

    /**
     * Append a mods list to the version.
     * @param modrinthMods Modrinth's mods to append.
     * @return the builder.
     */
    public ForgeVersionBuilder withModrinthMods(List<ModrinthVersionInfo> modrinthMods)
    {
        this.modrinthModsArgument.set(modrinthMods);
        return this;
    }

    /**
     * Assign to the future forge version a mod pack.
     * @param modPackInfo the mod pack information to assign.
     * @return the builder.
     */
    public ForgeVersionBuilder withCurseModPack(CurseModPackInfo modPackInfo)
    {
        this.curseModPackArgument.set(modPackInfo);
        return this;
    }

    /**
     * Assign to the future forge version a mod pack.
     * @param modPackInfo the mod pack information to assign.
     * @return the builder.
     */
    public ForgeVersionBuilder withModrinthModPack(ModrinthModPackInfo modPackInfo)
    {
        this.modrinthPackArgument.set(modPackInfo);
        return this;
    }

    /**
     * Append a file deleter to the version.
     * @param fileDeleter the file deleter to append.
     * @return the builder.
     */
    public ForgeVersionBuilder withFileDeleter(ModFileDeleter fileDeleter)
    {
        this.fileDeleterArgument.set(fileDeleter);
        return this;
    }

    /**
     * Append some OptiFine download's information.
     * @param optiFineInfo provided information.
     * @return the builder.
     */
    public ForgeVersionBuilder withOptiFine(OptiFineInfo optiFineInfo)
    {
        this.optiFineArgument.set(optiFineInfo);
        return this;
    }

    /**
     * Build a new {@link AbstractForgeVersion} instance with provided arguments.
     * @return the freshly created instance.
     * @throws BuilderException if an error occurred.
     */
    @Override
    public AbstractForgeVersion build() throws BuilderException
    {
        switch (this.type)
        {
            case NEW:
                return new NewForgeVersion(
                        this.forgeVersionArgument.get(),
                        this.modsArgument.get(),
                        this.curseModsArgument.get(),
                        this.modrinthModsArgument.get(),
                        this.fileDeleterArgument.get(),
                        this.optiFineArgument.get(),
                        this.curseModPackArgument.get(),
                        this.modrinthPackArgument.get()
                );
            case OLD:
                return new OldForgeVersion(
                        this.forgeVersionArgument.get(),
                        this.modsArgument.get(),
                        this.curseModsArgument.get(),
                        this.modrinthModsArgument.get(),
                        this.fileDeleterArgument.get(),
                        this.optiFineArgument.get(),
                        this.curseModPackArgument.get(),
                        this.modrinthPackArgument.get()
                );
            default:
                return null;
        }
    }

    public enum ForgeVersionType
    {
        /** 1.12.2-14.23.5.2851 to 1.19 */
        NEW,
        /** 1.7 to 1.12.2 */
        OLD
    }
}
