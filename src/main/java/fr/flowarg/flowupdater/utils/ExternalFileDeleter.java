package fr.flowarg.flowupdater.utils;

import fr.flowarg.flowupdater.download.DownloadInfos;
import fr.flowarg.flowupdater.download.json.ExternalFile;

import java.io.File;
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
        final File dir = (File)parameters[2];

        if(!externalFiles.isEmpty())
        {
            for(ExternalFile extFile : externalFiles)
            {
                final File file = new File(dir, extFile.getPath());

                if (file.exists())
                {
                    if(extFile.isUpdate())
                    {
                        if (!getSHA1(file).equals(extFile.getSha1()))
                        {
                            file.delete();
                            downloadInfos.getExtFiles().add(extFile);
                        }
                    }
                }
                else downloadInfos.getExtFiles().add(extFile);
            }
        }
    }
}
