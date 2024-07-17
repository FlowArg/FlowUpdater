package fr.flowarg.flowupdater.versions.forge;

import fr.flowarg.flowupdater.download.json.OptiFineInfo;
import fr.flowarg.flowupdater.utils.builderapi.BuilderArgument;
import fr.flowarg.flowupdater.utils.builderapi.BuilderException;
import fr.flowarg.flowupdater.versions.ModLoaderVersionBuilder;

/**
 * Builder for {@link AbstractForgeVersion}
 * @author Flow Arg (FlowArg)
 */
public class ForgeVersionBuilder extends ModLoaderVersionBuilder<AbstractForgeVersion, ForgeVersionBuilder>
{
    private final ForgeVersionType type;

    public ForgeVersionBuilder(ForgeVersionType type)
    {
        this.type = type;
    }

    private final BuilderArgument<String> forgeVersionArgument = new BuilderArgument<String>("ForgeVersion").required();
    private final BuilderArgument<OptiFineInfo> optiFineArgument = new BuilderArgument<OptiFineInfo>("OptiFine").optional();

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
                        this.curseModPackArgument.get(),
                        this.modrinthPackArgument.get(),
                        this.optiFineArgument.get()
                );
            case OLD:
                return new OldForgeVersion(
                        this.forgeVersionArgument.get(),
                        this.modsArgument.get(),
                        this.curseModsArgument.get(),
                        this.modrinthModsArgument.get(),
                        this.fileDeleterArgument.get(),
                        this.curseModPackArgument.get(),
                        this.modrinthPackArgument.get(),
                        this.optiFineArgument.get()
                );
            default:
                return null;
        }
    }
}
