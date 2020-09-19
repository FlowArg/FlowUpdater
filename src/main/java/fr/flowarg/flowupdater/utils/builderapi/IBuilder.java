package fr.flowarg.flowupdater.utils.builderapi;

import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.versions.VanillaVersion;

/**
 * Builder API
 * @version 1.4
 * @author flow
 * Used for {@link FlowUpdater} & {@link VanillaVersion}
 * @param <T> Object returned.
 * 
 * Represent an argument for a Builder implementation.
 */
public interface IBuilder<T> 
{
	/**
	 * Build a {@link T} object.
	 * @return a {@link T} object.
	 * @throws BuilderArgumentException
	 */
	T build() throws BuilderException;
}
