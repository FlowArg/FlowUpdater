package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowupdater.download.json.CurseFileInfo;
import fr.flowarg.flowupdater.download.json.CurseModPackInfo;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.download.json.OptiFineInfo;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import fr.flowarg.flowupdater.utils.builderapi.BuilderArgument;
import fr.flowarg.flowupdater.utils.builderapi.BuilderException;
import fr.flowarg.flowupdater.utils.builderapi.IBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder of {@link AbstractForgeVersion}
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
    private final BuilderArgument<ModFileDeleter> fileDeleterArgument = new BuilderArgument<>("ModFileDeleter", () -> new ModFileDeleter(false)).optional();
    private final BuilderArgument<CurseModPackInfo> modPackArgument = new BuilderArgument<CurseModPackInfo>("ModPack").optional();

    public ForgeVersionBuilder withForgeVersion(String forgeVersion)
    {
        this.forgeVersionArgument.set(forgeVersion);
        return this;
    }

    public ForgeVersionBuilder withMods(List<Mod> mods)
    {
        this.modsArgument.set(mods);
        return this;
    }

    public ForgeVersionBuilder withCurseMods(List<CurseFileInfo> curseMods)
    {
        this.curseModsArgument.set(curseMods);
        return this;
    }

    /**
     * Assign to the future forge version a mod pack.
     * @param modPackInfo the mod pack information to assign.
     * @return the current builder.
     */
    public ForgeVersionBuilder withCurseModPack(CurseModPackInfo modPackInfo)
    {
        this.modPackArgument.set(modPackInfo);
        return this;
    }

    public ForgeVersionBuilder withFileDeleter(ModFileDeleter fileDeleter)
    {
        this.fileDeleterArgument.set(fileDeleter);
        return this;
    }

    /**
     * Assign to the future forge version a mod pack.
     * @param modPackInfo the mod pack information to assign.
     * @return the current builder.
     * @deprecated use {@link #withCurseModPack(CurseModPackInfo)} instead.
     */
    @Deprecated
    public ForgeVersionBuilder withModPack(CurseModPackInfo modPackInfo)
    {
        this.modPackArgument.set(modPackInfo);
        return this;
    }


    public ForgeVersionBuilder withOptiFine(OptiFineInfo optiFine)
    {
        this.optiFineArgument.set(optiFine);
        return this;
    }

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
                        this.fileDeleterArgument.get(),
                        this.optiFineArgument.get(),
                        this.modPackArgument.get()
                );
            case OLD:
                return new OldForgeVersion(
                        this.forgeVersionArgument.get(),
                        this.modsArgument.get(),
                        this.curseModsArgument.get(),
                        this.fileDeleterArgument.get(),
                        this.optiFineArgument.get(),
                        this.modPackArgument.get()
                );
            default:
                return null;
        }
    }

    public enum ForgeVersionType
    {
        /** 1.12.2-14.23.5.2851 to 1.17.1 */
        NEW,
        /** 1.7 to 1.12.2 */
        OLD
    }
}
