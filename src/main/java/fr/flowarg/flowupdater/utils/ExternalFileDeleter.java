package fr.flowarg.flowupdater.utils;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowupdater.download.DownloadInfos;
import fr.flowarg.flowupdater.download.json.ExternalFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ExternalFileDeleter implements IFileDeleter
{
    @SuppressWarnings("unchecked")
    @Override
    public void delete(Object... parameters) throws Exception
    {
        if(parameters.length != 3)
            return;
        final List<ExternalFile> externalFiles = (List<ExternalFile>)parameters[0];
        final DownloadInfos downloadInfos = (DownloadInfos)parameters[1];
        final Path dir = (Path)parameters[2];

        if(!externalFiles.isEmpty())
        {
            for(ExternalFile extFile : externalFiles)
            {
                final Path filePath = dir.resolve(extFile.getPath());

                if (Files.exists(filePath))
                {
                    if(extFile.isUpdate())
                    {
                        if (!FileUtils.getSHA1(filePath).equalsIgnoreCase(extFile.getSha1()))
                        {
                            Files.delete(filePath);
                            downloadInfos.getExtFiles().add(extFile);
                        }
                    }
                }
                else downloadInfos.getExtFiles().add(extFile);
            }
        }
    }
}
