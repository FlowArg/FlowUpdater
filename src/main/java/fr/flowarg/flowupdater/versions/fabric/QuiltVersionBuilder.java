package fr.flowarg.flowupdater.versions.fabric;

import fr.flowarg.flowupdater.utils.IOUtils;
import fr.flowarg.flowupdater.utils.builderapi.BuilderArgument;
import fr.flowarg.flowupdater.utils.builderapi.BuilderException;
import fr.flowarg.flowupdater.versions.ModLoaderVersionBuilder;

public class QuiltVersionBuilder extends ModLoaderVersionBuilder<QuiltVersion, QuiltVersionBuilder>
{
    private static final String QUILT_VERSION_METADATA =
            "https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-loader/maven-metadata.xml";

    private final BuilderArgument<String> quiltVersionArgument =
            new BuilderArgument<>("QuiltVersion", () -> IOUtils.getLatestArtifactVersion(QUILT_VERSION_METADATA)).optional();

    /**
     * @param quiltVersion the Quilt version you want to install
     * (don't use this function if you want to use the latest Quilt version).
     * @return the builder.
     */
    public QuiltVersionBuilder withQuiltVersion(String quiltVersion)
    {
        this.quiltVersionArgument.set(quiltVersion);
        return this;
    }

    /**
     * Build a new {@link QuiltVersion} instance with provided arguments.
     * @return the freshly created instance.
     * @throws BuilderException if an error occurred.
     */
    @Override
    public QuiltVersion build() throws BuilderException
    {
        return new QuiltVersion(
                this.quiltVersionArgument.get(),
                this.modsArgument.get(),
                this.curseModsArgument.get(),
                this.modrinthModsArgument.get(),
                this.fileDeleterArgument.get(),
                this.curseModPackArgument.get(),
                this.modrinthPackArgument.get()
        );
    }
}
