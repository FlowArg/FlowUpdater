package fr.flowarg.flowupdater.utils;

@FunctionalInterface
public interface IFileDeleter
{
    /**
     * Delete all bad files in the provided directory.
     * @param parameters all parameters required by the FileDeleter implementation
     * @throws Exception thrown if an error occurred
     */
    void delete(Object... parameters) throws Exception;
}
