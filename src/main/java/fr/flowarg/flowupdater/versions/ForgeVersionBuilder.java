package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.json.CurseFileInfos;
import fr.flowarg.flowupdater.download.json.CurseModPackInfos;
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
    private final BuilderArgument<VanillaVersion> vanillaVersionArgument = new BuilderArgument<>(() -> VanillaVersion.NULL_VERSION, "VanillaVersion").required();
    private final BuilderArgument<ILogger> loggerArgument = new BuilderArgument<>("Logger", () -> FlowUpdater.DEFAULT_LOGGER).optional();
    private final BuilderArgument<IProgressCallback> progressCallbackArgument = new BuilderArgument<>("ProgressCallback", () -> FlowUpdater.NULL_CALLBACK).optional();
    private final BuilderArgument<List<Mod>> modsArgument = new BuilderArgument<List<Mod>>("Mods", ArrayList::new).optional();
    private final BuilderArgument<List<CurseFileInfos>> curseModsArgument = new BuilderArgument<List<CurseFileInfos>>("CurseMods", ArrayList::new).optional();
    private final BuilderArgument<Boolean> nogGuiArgument = new BuilderArgument<>("NoGui", () -> true).optional();
    private final BuilderArgument<ModFileDeleter> fileDeleterArgument = new BuilderArgument<>("ModFileDeleter", () -> new ModFileDeleter(false)).optional();
    private final BuilderArgument<OptifineInfo> optifineArgument = new BuilderArgument<OptifineInfo>("Optifine").optional();
    private final BuilderArgument<CurseModPackInfos> modPackArgument = new BuilderArgument<CurseModPackInfos>("ModPack").optional();

    public ForgeVersionBuilder withForgeVersion(String forgeVersion)
    {
        this.forgeVersionArgument.set(forgeVersion);
        return this;
    }

    public ForgeVersionBuilder withVanillaVersion(VanillaVersion vanillaVersion)
    {
        this.vanillaVersionArgument.set(vanillaVersion);
        return this;
    }

    public ForgeVersionBuilder withLogger(ILogger logger)
    {
        this.loggerArgument.set(logger);
        return this;
    }

    public ForgeVersionBuilder withProgressCallback(IProgressCallback progressCallback)
    {
        this.progressCallbackArgument.set(progressCallback);
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

    public ForgeVersionBuilder withNoGui(boolean noGui)
    {
        this.nogGuiArgument.set(noGui);
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

    public ForgeVersionBuilder withModPack(CurseModPackInfos modPackInfos)
    {
        this.modPackArgument.set(modPackInfos);
        return this;
    }

    @Override
    public AbstractForgeVersion build() throws BuilderException
    {
        if(this.progressCallbackArgument.get() == FlowUpdater.NULL_CALLBACK)
            this.loggerArgument.get().warn("You are using default callback for forge installation. If you're using a custom callback for vanilla files, it will not updated when forge and mods will be installed.");
        switch (this.type)
        {
            case NEW:
                return new NewForgeVersion(
                        this.forgeVersionArgument.get(),
                        this.vanillaVersionArgument.get(),
                        this.loggerArgument.get(),
                        this.progressCallbackArgument.get(),
                        this.modsArgument.get(),
                        this.curseModsArgument.get(),
                        this.nogGuiArgument.get(),
                        this.fileDeleterArgument.get(),
                        this.optifineArgument.get(),
                        this.modPackArgument.get()
                );
            case OLD:
                return new OldForgeVersion(
                        this.forgeVersionArgument.get(),
                        this.vanillaVersionArgument.get(),
                        this.loggerArgument.get(),
                        this.progressCallbackArgument.get(),
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
        /** 1.12.2-14.23.5.2851 -> 1.16.3 */
        NEW,
        /** 1.7 -> 1.12.2 */
        OLD
    }
}
