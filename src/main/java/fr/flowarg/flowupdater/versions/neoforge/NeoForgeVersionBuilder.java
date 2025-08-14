package fr.flowarg.flowupdater.versions.neoforge;

import fr.flowarg.flowupdater.download.json.OptiFineInfo;
import fr.flowarg.flowupdater.utils.builderapi.BuilderArgument;
import fr.flowarg.flowupdater.utils.builderapi.BuilderException;
import fr.flowarg.flowupdater.versions.ModLoaderVersionBuilder;

public class NeoForgeVersionBuilder extends ModLoaderVersionBuilder<NeoForgeVersion, NeoForgeVersionBuilder>
{
    private final BuilderArgument<String> neoForgeVersionArgument = new BuilderArgument<String>("NeoForgeVersion").required();
    private final BuilderArgument<OptiFineInfo> optiFineArgument = new BuilderArgument<OptiFineInfo>("OptiFine").optional();

    /**
     * @param neoForgeVersion the NeoForge version you want to install.
     * For 1.20.1, it should be in the format "1.20.1-47.1.x" (vanilla version-NeoForge version). (forge format)
     * For 1.21 and above, it should only be the NeoForge version (for example: 21.8.31) (NeoForge version only).
     * @return the builder.
     */
    public NeoForgeVersionBuilder withNeoForgeVersion(String neoForgeVersion)
    {
        this.neoForgeVersionArgument.set(neoForgeVersion);
        return this;
    }

    /**
     * Append some OptiFine download's information.
     * @param optiFineInfo OptiFine info.
     * @return the builder.
     */
    public NeoForgeVersionBuilder withOptiFine(OptiFineInfo optiFineInfo)
    {
        this.optiFineArgument.set(optiFineInfo);
        return this;
    }

    @Override
    public NeoForgeVersion build() throws BuilderException
    {
        return new NeoForgeVersion(
                this.neoForgeVersionArgument.get(),
                this.modsArgument.get(),
                this.curseModsArgument.get(),
                this.modrinthModsArgument.get(),
                this.fileDeleterArgument.get(),
                this.curseModPackArgument.get(),
                this.modrinthPackArgument.get(),
                this.optiFineArgument.get()
        );
    }
}
