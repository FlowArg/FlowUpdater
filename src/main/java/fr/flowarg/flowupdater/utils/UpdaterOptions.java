package fr.flowarg.flowupdater.utils;

import fr.flowarg.flowupdater.utils.builderapi.BuilderArgument;
import fr.flowarg.flowupdater.utils.builderapi.BuilderException;
import fr.flowarg.flowupdater.utils.builderapi.IBuilder;

/**
 * Represent some settings for FlowUpdater
 * @author flow
 */
public class UpdaterOptions
{
    public static final UpdaterOptions DEFAULT = new UpdaterOptions(true, false, false, false);

    /** Is the read silent */
    private final boolean silentRead;
    
    /** Re-extract natives at each updates ? */
    private final boolean reExtractNatives;

    /** Select some mods from CurseForge ?
     * WARNING: IF THIS FIELD IS THE TO TRUE, IT WILL DOWNLOAD AND LOAD A PLUGIN ; DISABLE THIS OPTION IF YOU DON'T USE CURSE_FORGE !!
     */
    private final boolean enableModsFromCurseForge;

    /** Install optifine from the official Website (mod) ?
     * WARNING: IF THIS FIELD IS THE TO TRUE, IT WILL DOWNLOAD AND LOAD A PLUGIN ; DISABLE THIS OPTION IF YOU DON'T USE OPTIFINE !!
     */
    private final boolean installOptifineAsMod;
    
    private UpdaterOptions(boolean silentRead, boolean reExtractNatives, boolean enableModsFromCurseForge, boolean installOptifineAsMod)
    {
        this.silentRead = silentRead;
        this.reExtractNatives = reExtractNatives;
        this.enableModsFromCurseForge = enableModsFromCurseForge;
        this.installOptifineAsMod = installOptifineAsMod;
    }
    
    public boolean isReExtractNatives()
    {
        return this.reExtractNatives;
    }
    
    public boolean isSilentRead()
    {
        return this.silentRead;
    }
    
    public boolean isEnableModsFromCurseForge()
    {
        return this.enableModsFromCurseForge;
    }

    public boolean isInstallOptifineAsMod()
    {
        return this.installOptifineAsMod;
    }

    public static class UpdaterOptionsBuilder implements IBuilder<UpdaterOptions>
    {
        private final BuilderArgument<Boolean> silentReadArgument = new BuilderArgument<>("SilentRead", () -> true).optional();
        private final BuilderArgument<Boolean> reExtractNativesArgument = new BuilderArgument<>("ReExtractingNatives", () -> false).optional();
        private final BuilderArgument<Boolean> enableModsFromCurseForgeArgument = new BuilderArgument<>("EnableModsFromCurseForge", () -> false).optional();
        private final BuilderArgument<Boolean> installOptifineAsModArgument = new BuilderArgument<>("InstallOptifineAsMod", () -> false).optional();

        public UpdaterOptionsBuilder withSilentRead(boolean silentRead)
        {
            this.silentReadArgument.set(silentRead);
            return this;
        }

        public UpdaterOptionsBuilder withReExtractNatives(boolean reExtractNatives)
        {
            this.reExtractNativesArgument.set(reExtractNatives);
            return this;
        }

        public UpdaterOptionsBuilder withEnableModsFromCurseForge(boolean enableModsFromCurseForge)
        {
            this.enableModsFromCurseForgeArgument.set(enableModsFromCurseForge);
            return this;
        }

        public UpdaterOptionsBuilder withInstallOptifineAsMod(boolean installOptifineAsMod)
        {
            this.installOptifineAsModArgument.set(installOptifineAsMod);
            return this;
        }

        @Override
        public UpdaterOptions build() throws BuilderException
        {
            return new UpdaterOptions(this.silentReadArgument.get(), this.reExtractNativesArgument.get(), this.enableModsFromCurseForgeArgument.get(), this.installOptifineAsModArgument.get());
        }
    }
}
