package fr.flowarg.flowupdater.utils.builderapi;

/**
 * Builder API ;
 * Used for {@link fr.flowarg.flowupdater.FlowUpdater}, {@link fr.flowarg.flowupdater.versions.VanillaVersion}, {@link fr.flowarg.flowupdater.versions.AbstractForgeVersion}, {@link fr.flowarg.flowupdater.versions.FabricVersion}
 * @version 1.6
 * @author flow
 *
 * @param <T> Object returned.
 * 
 * Builder interface.
 */
@FunctionalInterface
public interface IBuilder<T> 
{
    /**
     * Build a {@link T} object.
     * @return a {@link T} object.
     * @throws BuilderException if an error occurred when building object.
     */
    T build() throws BuilderException;
}
