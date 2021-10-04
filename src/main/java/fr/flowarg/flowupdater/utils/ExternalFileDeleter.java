package fr.flowarg.flowupdater.utils;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowupdater.download.DownloadList;
import fr.flowarg.flowupdater.download.json.ExternalFile;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A file deleter designed to check external files.
 */
public class ExternalFileDeleter implements IFileDeleter
{
    /**
     * Delete all bad files in the provided directory.
     * @param externalFiles the list of external files.
     * @param downloadList the download list.
     * @param dir the base dir.
     * @throws Exception thrown if an error occurred
     */
    public void delete(@NotNull List<ExternalFile> externalFiles, DownloadList downloadList, Path dir) throws Exception
    {
        if(externalFiles.isEmpty()) return;

        for(ExternalFile extFile : externalFiles)
        {
            final Path filePath = dir.resolve(extFile.getPath());

            if (Files.exists(filePath))
            {
                if(!extFile.isUpdate()) continue;

                if (FileUtils.getSHA1(filePath).equalsIgnoreCase(extFile.getSha1())) continue;

                Files.delete(filePath);
            }
            downloadList.getExtFiles().add(extFile);
        }
    }
}
