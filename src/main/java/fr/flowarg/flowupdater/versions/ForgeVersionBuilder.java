package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.json.CurseFileInfos;
import fr.flowarg.flowupdater.download.json.CurseModPackInfo;
import fr.flowarg.flowupdater.download.json.Mod;
import fr.flowarg.flowupdater.download.json.OptifineInfo;
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
    private final BuilderArgument<List<Mod>> modsArgument = new BuilderArgument<List<Mod>>("Mods", ArrayList::new).optional();
    private final BuilderArgument<List<CurseFileInfos>> curseModsArgument = new BuilderArgument<List<CurseFileInfos>>("CurseMods", ArrayList::new).optional();
    private final BuilderArgument<ModFileDeleter> fileDeleterArgument = new BuilderArgument<>("ModFileDeleter", () -> new ModFileDeleter(false)).optional();
    private final BuilderArgument<OptifineInfo> optifineArgument = new BuilderArgument<OptifineInfo>("Optifine").optional();
    private final BuilderArgument<CurseModPackInfo> modPackArgument = new BuilderArgument<CurseModPackInfo>("ModPack").optional();

    public ForgeVersionBuilder withForgeVersion(String forgeVersion)
    {
        this.forgeVersionArgument.set(forgeVersion);
        return this;
    }

    @Deprecated
    public ForgeVersionBuilder withVanillaVersion(VanillaVersion vanillaVersion)
    {
        return this;
    }

    @Deprecated
    public ForgeVersionBuilder withLogger(ILogger logger)
    {
        return this;
    }

    @Deprecated
    public ForgeVersionBuilder withProgressCallback(IProgressCallback progressCallback)
    {
        return this;
    }

    public ForgeVersionBuilder withMods(List<Mod> mods)
    {
        this.modsArgument.set(mods);
        return this;
    }

    public ForgeVersionBuilder withCurseMods(List<CurseFileInfos> curseMods)
    {
        this.curseModsArgument.set(curseMods);
        return this;
    }

    /**
     * A useless function. Will be removed soon.
     * @param noGui true/false.
     * @return the builder.
     * @deprecated Useless, don't use that.
     */
    @Deprecated
    public ForgeVersionBuilder withNoGui(boolean noGui)
    {
        return this;
    }

    public ForgeVersionBuilder withFileDeleter(ModFileDeleter fileDeleter)
    {
        this.fileDeleterArgument.set(fileDeleter);
        return this;
    }

    public ForgeVersionBuilder withOptifine(OptifineInfo optifine)
    {
        this.optifineArgument.set(optifine);
        return this;
    }

    public ForgeVersionBuilder withModPack(CurseModPackInfo modPackInfos)
    {
        this.modPackArgument.set(modPackInfos);
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
                        this.optifineArgument.get(),
                        this.modPackArgument.get()
                );
            case OLD:
                return new OldForgeVersion(
                        this.forgeVersionArgument.get(),
                        this.modsArgument.get(),
                        this.curseModsArgument.get(),
                        this.fileDeleterArgument.get(),
                        this.optifineArgument.get(),
                        this.modPackArgument.get()
                );
            default:
                return null;
        }
    }

    public enum ForgeVersionType
    {
        /** 1.12.2-14.23.5.2851 to 1.17 */
        NEW,
        /** 1.7 to 1.12.2 */
        OLD
    }
}
