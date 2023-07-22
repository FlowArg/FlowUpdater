package fr.flowarg.flowupdater.utils.builderapi;

/**
 * Builder API ; Builder interface.
 * @version 1.6
 * @author flow
 *
 * @param <T> Object returned.
 */
@FunctionalInterface
public interface IBuilder<T> 
{
    /**
     * Build a {@link T} object.
     * @return a {@link T} object.
     * @throws BuilderException if an error occurred when building an object.
     */
    T build() throws BuilderException;
}
