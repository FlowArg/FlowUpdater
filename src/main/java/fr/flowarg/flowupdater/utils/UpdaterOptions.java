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
    public static final UpdaterOptions DEFAULT = new UpdaterOptions(true, false, false);

    /** Is the read silent */
    private final boolean silentRead;
    
    /** Re-extract natives at each updates ? */
    private final boolean reextractNatives;

    /** Select some mods from CurseForge ?
     * WARNING: IF THIS FIELD IS THE TO TRUE, IT WILL DOWNLOAD AND LOAD A PLUGIN ; DISABLE THIS OPTION IF YOU DON'T USE CURSEFORGE !!
     */
    private final boolean enableModsFromCurseForge;
    
    private UpdaterOptions(boolean silentRead, boolean reextractNatives, boolean enableModsFromCurseForge)
    {
        this.silentRead = silentRead;
        this.reextractNatives = reextractNatives;
        this.enableModsFromCurseForge = enableModsFromCurseForge;
    }
    
    public boolean isReextractNatives()
    {
        return this.reextractNatives;
    }
    
    public boolean isSilentRead()
    {
        return this.silentRead;
    }
    
    public boolean isEnableModsFromCurseForge()
    {
        return this.enableModsFromCurseForge;
    }

    public static class UpdaterOptionsBuilder implements IBuilder<UpdaterOptions>
    {
        private final BuilderArgument<Boolean> silentRead = new BuilderArgument<>("SilentRead", () -> true).optional();
        private final BuilderArgument<Boolean> reExtractNatives = new BuilderArgument<>("ReExtractingNatives", () -> false).optional();
        private final BuilderArgument<Boolean> enableModsFromCurseForge = new BuilderArgument<>("EnableModsFromCurseForge", () -> false).optional();

        public UpdaterOptionsBuilder withSilentRead(boolean silentRead)
        {
            this.silentRead.set(silentRead);
            return this;
        }

        public UpdaterOptionsBuilder withReExtractNatives(boolean reExtractNatives)
        {
            this.reExtractNatives.set(reExtractNatives);
            return this;
        }

        public UpdaterOptionsBuilder withEnableModsFromCurseForge(boolean enableModsFromCurseForge)
        {
            this.enableModsFromCurseForge.set(enableModsFromCurseForge);
            return this;
        }

        @Override
        public UpdaterOptions build() throws BuilderException
        {
            return new UpdaterOptions(this.silentRead.get(), this.reExtractNatives.get(), this.enableModsFromCurseForge.get());
        }
    }
}
