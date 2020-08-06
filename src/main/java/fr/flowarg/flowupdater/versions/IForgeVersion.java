package fr.flowarg.flowupdater.versions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import fr.flowarg.flowupdater.utils.ZipUtils;

/**
 * Represent a Forge version.
 * Implemented by {@link OldForgeVersion} & {@link NewForgeVersion}.
 * @author FlowArg
 */
public interface IForgeVersion
{	
	boolean isForgeAlreadyInstalled(File installDir);

	
	/**
	 * This function installs a Forge version at the specified directory.
	 * @param dirToInstall Specified directory.
	 */
	void install(final File dirToInstall);
	
	/**
	 * This function installs mods at the specified directory.
	 * @param dirToInstall Specified mods directory.
	 * @throws IOException If install fail.
	 */
	void installMods(final File dirToInstall) throws IOException;
	
	boolean isModFileDeleterEnabled();
	IForgeVersion enableModFileDeleter();
	IForgeVersion disableModFileDeleter();
	
	default void unzipJar(final File destinationDir, final File jarFile) throws IOException
	{
        final JarFile jar = new JarFile(jarFile);

        for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements(); )
        {
            final JarEntry entry = enums.nextElement();

            final String fileName = destinationDir + File.separator + entry.getName();
            final File   file     = new File(fileName);

            if (fileName.endsWith("/")) file.mkdirs();
        }

        for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements(); )
        {
            final JarEntry entry = enums.nextElement();

            final String fileName = destinationDir + File.separator + entry.getName();
            final File   file     = new File(fileName);

            if (!fileName.endsWith("/"))
            {
                if (fileName.endsWith(".lzma"))
                {
                    new File(destinationDir, "data").mkdir();
                    final InputStream stream = jar.getInputStream(entry);
                    Files.copy(stream, new File(destinationDir, entry.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    stream.close();
                }
                else
                {
                    final InputStream      is  = jar.getInputStream(entry);
                    final FileOutputStream fos = new FileOutputStream(file);

                    while (is.available() > 0)
                        fos.write(is.read());

                    fos.close();
                    is.close();
                }
                jar.getInputStream(entry).close();
            }
        }

        jar.close();
	}
	
	default void packPatchedInstaller(final File tempDir, final File tempInstallerDir) throws IOException
    {
        final File output = new File(tempDir, "forge-installer-patched.zip");
        ZipUtils.INSTANCE.compressFiles(tempInstallerDir.listFiles(), output);
        Files.move(output.toPath(), new File(output.getAbsolutePath().replace(".zip", ".jar")).toPath(), StandardCopyOption.REPLACE_EXISTING);
        tempInstallerDir.delete();
    }	
}