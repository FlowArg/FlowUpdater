package fr.flowarg.flowupdater.utils;

import fr.flowarg.flowupdater.download.DownloadInfos;
import fr.flowarg.flowupdater.download.json.ExternalFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static fr.flowarg.flowio.FileUtils.getSHA1;

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
                final Path filePath = Paths.get(dir.toString(), extFile.getPath());

                if (Files.exists(filePath))
                {
                    if(extFile.isUpdate())
                    {
                        if (!getSHA1(filePath.toFile()).equals(extFile.getSha1()))
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
