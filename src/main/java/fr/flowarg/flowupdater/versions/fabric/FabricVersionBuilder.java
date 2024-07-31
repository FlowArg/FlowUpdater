package fr.flowarg.flowupdater.versions.fabric;

import fr.flowarg.flowupdater.download.json.OptiFineInfo;
import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.builderapi.BuilderArgument;
import fr.flowarg.flowupdater.utils.builderapi.BuilderException;
import fr.flowarg.flowupdater.versions.ModLoaderVersionBuilder;

public class FabricVersionBuilder extends ModLoaderVersionBuilder<FabricVersion, FabricVersionBuilder>
{
    private static final String FABRIC_VERSION_METADATA =
            "https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml";

    private final BuilderArgument<String> fabricVersionArgument =
            new BuilderArgument<>("FabricVersion", () ->
                    IOUtils.getLatestArtifactVersion(FABRIC_VERSION_METADATA))
                    .optional();
    private final BuilderArgument<OptiFineInfo> optiFineArgument = new BuilderArgument<OptiFineInfo>("OptiFine").optional();


    /**
     * @param fabricVersion the Fabric version you want to install
     * (don't use this function if you want to use the latest fabric version).
     * @return the builder.
     */
    public FabricVersionBuilder withFabricVersion(String fabricVersion)
    {
        this.fabricVersionArgument.set(fabricVersion);
        return this;
    }

    /**
     * Append some OptiFine download's information.
     * @param optiFineInfo OptiFine info.
     * @return the builder.
     */
    public FabricVersionBuilder withOptiFine(OptiFineInfo optiFineInfo)
    {
        this.optiFineArgument.set(optiFineInfo);
        return this;
    }

    /**
     * Build a new {@link FabricVersion} instance with provided arguments.
     * @return the freshly created instance.
     * @throws BuilderException if an error occurred.
     */
    @Override
    public FabricVersion build() throws BuilderException
    {
        return new FabricVersion(
                this.fabricVersionArgument.get(),
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
