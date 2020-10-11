package fr.flowarg.flowupdater.utils.builderapi;

import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.versions.AbstractForgeVersion;
import fr.flowarg.flowupdater.versions.VanillaVersion;

/**
 * Builder API
 *
 * @version 1.5
 * @author flow
 *
 * Used for {@link FlowUpdater} & {@link VanillaVersion} & {@link AbstractForgeVersion}
 * @param <T> Object returned.
 * 
 * Builder interface.
 */
public interface IBuilder<T> 
{
    /**
     * Build a {@link T} object.
     * @return a {@link T} object.
     * @throws BuilderException if an error occurred when building object.
     */
    T build() throws BuilderException;
}
