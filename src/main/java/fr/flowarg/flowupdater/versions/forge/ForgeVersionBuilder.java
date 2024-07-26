package fr.flowarg.flowupdater.versions.forge;

import fr.flowarg.flowupdater.download.json.OptiFineInfo;
import fr.flowarg.flowupdater.utils.builderapi.BuilderArgument;
import fr.flowarg.flowupdater.utils.builderapi.BuilderException;
import fr.flowarg.flowupdater.versions.ModLoaderVersionBuilder;

/**
 * Builder for {@link ForgeVersion}
 * @author Flow Arg (FlowArg)
 */
public class ForgeVersionBuilder extends ModLoaderVersionBuilder<ForgeVersion, ForgeVersionBuilder>
{
    private final BuilderArgument<String> forgeVersionArgument = new BuilderArgument<String>("ForgeVersion").required();
    private final BuilderArgument<OptiFineInfo> optiFineArgument = new BuilderArgument<OptiFineInfo>("OptiFine").optional();

    /**
     * @param forgeVersion the Forge version you want to install. You should be very precise with the string you give.
     * For instance, "1.18.2-40.2.21", "1.12.2-14.23.5.2860", "1.8.9-11.15.1.2318-1.8.9", "1.7.10-10.13.4.1614-1.7.10" are correct.
     * Download an installer and check the name of it to get the correct version you should provide here if you are not sure.
     * @return the builder.
     */
    public ForgeVersionBuilder withForgeVersion(String forgeVersion)
    {
        this.forgeVersionArgument.set(forgeVersion);
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

    @Override
    public ForgeVersion build() throws BuilderException
    {
        return new ForgeVersion(
                this.forgeVersionArgument.get(),
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
