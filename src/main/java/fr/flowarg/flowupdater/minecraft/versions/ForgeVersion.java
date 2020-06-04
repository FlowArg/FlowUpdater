package fr.flowarg.flowupdater.minecraft.versions;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.Logger;

import java.io.*;
import java.lang.ProcessBuilder.Redirect;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ForgeVersion
{
    private Logger   logger;
    private String   forgeVersion;
    private IVersion vanilla;
    private URL      installerUrl;

    public ForgeVersion(String forgeVersion, IVersion vanilla, Logger logger)
    {
        try
        {
            this.logger = logger;
            if (!forgeVersion.contains("-"))
                this.forgeVersion = vanilla.getName() + '-' + forgeVersion;
            else this.forgeVersion = forgeVersion;
            this.installerUrl = new URL(String.format("https://files.minecraftforge.net/maven/net/minecraftforge/forge/%s/forge-%s-installer.jar", forgeVersion, forgeVersion));
            this.vanilla      = vanilla;
        } catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
    }

    public void install(final File dirToInstall)
    {
        if (this.forgeVersion.startsWith("1.15") ||
                this.forgeVersion.startsWith("1.14") ||
                this.forgeVersion.startsWith("1.13") || this.forgeVersion.equalsIgnoreCase("1.12.2-14.23.5.2854"))
        {
            try (BufferedInputStream stream = new BufferedInputStream(this.installerUrl.openStream()))
            {
                this.logger.info("Downloading new forge installer...");
                final File tempDir          = new File(dirToInstall + File.separator + ".flowupdater");
                final File tempInstallerDir = new File(tempDir, "installer/");
                final File install          = new File(tempDir, "forge-installer.jar");
                final File patches          = new File(tempDir, "patches.jar");
                final File patchedInstaller = new File(tempDir, "forge-installer-patched.jar");
                FileUtils.deleteDirectory(tempInstallerDir);
                install.delete();
                patchedInstaller.delete();
                patches.delete();
                tempDir.mkdirs();
                tempInstallerDir.mkdirs();

                Files.copy(stream, install.toPath(), StandardCopyOption.REPLACE_EXISTING);
                this.logger.info("Downloading patches...");
                Files.copy(new URL("https://flowarg.github.io/minecraft/launcher/patches.jar").openStream(), patches.toPath(), StandardCopyOption.REPLACE_EXISTING);

                this.logger.info("Applying patches...");
                this.unzipJar(tempInstallerDir, install);
                this.cleaningInstaller(tempInstallerDir);
                this.unzipJar(tempInstallerDir, patches);
                this.packPatchedInstaller(tempDir, tempInstallerDir);
                patches.delete();
                this.logger.info("Launching forge installer...");
                
                final ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", patchedInstaller.getAbsolutePath(), "--installClient", dirToInstall.getAbsolutePath());
                
                processBuilder.redirectOutput(Redirect.INHERIT);
                final Process process = processBuilder.start();
                process.waitFor();
                
                
                this.logger.info("Successfully installed Forge !");
                FileUtils.deleteDirectory(tempDir);
            }
            catch (IOException | InterruptedException e)
            {
                this.logger.printStackTrace(e);
            }
        }
    }

    private void unzipJar(final File destinationDir, final File jarFile) throws IOException
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

    private void cleaningInstaller(File tempInstallerDir)
    {
        FileUtils.deleteDirectory(new File(tempInstallerDir, "net"));
        FileUtils.deleteDirectory(new File(tempInstallerDir, "joptisimple"));
        new File(tempInstallerDir, "META-INF/MANIFEST.MF").delete();
        new File(tempInstallerDir, "META-INF/FORGE.DSA").delete();
        new File(tempInstallerDir, "META-INF/FORGE.SF").delete();
    }

    private void packPatchedInstaller(final File tempDir, final File tempInstallerDir) throws IOException
    {
        final File output = new File(tempDir, "forge-installer-patched.zip");
        PatcherUtils.INSTANCE.compressFiles(tempInstallerDir.listFiles(), output);
        Files.move(output.toPath(), new File(output.getAbsolutePath().replace(".zip", ".jar")).toPath(), StandardCopyOption.REPLACE_EXISTING);
        tempInstallerDir.delete();
    }

    public Logger getLogger()
    {
        return this.logger;
    }

    public String getForgeVersion()
    {
        return this.forgeVersion;
    }

    public URL getInstallerUrl()
    {
        return this.installerUrl;
    }

    public IVersion getVanilla()
    {
        return this.vanilla;
    }
}
