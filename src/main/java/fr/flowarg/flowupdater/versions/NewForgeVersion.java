package fr.flowarg.flowupdater.versions;

import static fr.flowarg.flowio.FileUtils.getFileSizeBytes;
import static fr.flowarg.flowio.FileUtils.getSHA1;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import fr.flowarg.flowio.FileUtils;
import fr.flowarg.flowlogger.Logger;
import fr.flowarg.flowupdater.utils.ZipUtils;
import fr.flowarg.flowupdater.versions.download.IProgressCallback;
import fr.flowarg.flowupdater.versions.download.Mod;
import fr.flowarg.flowupdater.versions.download.Step;

/**
 * Represent a new Forge version (1.13 -> 1.15.2)
 * @author FlowArg
 */
public class NewForgeVersion implements IForgeVersion
{
    private Logger   logger;
    private String   forgeVersion;
    private IVanillaVersion vanilla;
    private URL      installerUrl;
    private IProgressCallback callback;
    private List<Mod> mods;

    public NewForgeVersion(String forgeVersion, IVanillaVersion vanilla, Logger logger, IProgressCallback callback, List<Mod> mods)
    {
        try
        {
            this.logger = logger;
            if (!forgeVersion.contains("-"))
                this.forgeVersion = vanilla.getName() + '-' + forgeVersion;
            else this.forgeVersion = forgeVersion;
            this.installerUrl = new URL(String.format("https://files.minecraftforge.net/maven/net/minecraftforge/forge/%s/forge-%s-installer.jar", this.forgeVersion, this.forgeVersion));
            this.vanilla      = vanilla;
            this.callback = callback;
            this.mods = mods;
        } catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void install(final File dirToInstall)
    {
    	this.callback.step(Step.FORGE);
    	this.logger.info("Installing new forge version : " + this.forgeVersion + "...");
    	if (this.forgeVersion.startsWith("1.15") ||
                this.forgeVersion.startsWith("1.14") ||
                this.forgeVersion.startsWith("1.13") || this.forgeVersion.equalsIgnoreCase("1.12.2-14.23.5.2854"))
        {
            try (BufferedInputStream stream = new BufferedInputStream(this.installerUrl.openStream()))
            {
                this.logger.info("Downloading new forge installer...");
                final File tempDir          = new File(dirToInstall, ".flowupdater");
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
                this.logger.info("Repack installer...");
                this.packPatchedInstaller(tempDir, tempInstallerDir);
                patches.delete();
                this.logger.info("Launching forge installer...");
                
                final ProcessBuilder processBuilder = new ProcessBuilder("java", "-Xmx256M", "-jar", patchedInstaller.getAbsolutePath(), "--installClient", dirToInstall.getAbsolutePath());
                
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

    private void cleaningInstaller(File tempInstallerDir)
    {
        FileUtils.deleteDirectory(new File(tempInstallerDir, "net"));
        FileUtils.deleteDirectory(new File(tempInstallerDir, "com"));
        FileUtils.deleteDirectory(new File(tempInstallerDir, "joptisimple"));
        FileUtils.deleteDirectory(new File(tempInstallerDir, "net"));
        new File(tempInstallerDir, "META-INF/MANIFEST.MF").delete();
        new File(tempInstallerDir, "META-INF/FORGE.DSA").delete();
        new File(tempInstallerDir, "META-INF/FORGE.SF").delete();
    }

    private void packPatchedInstaller(final File tempDir, final File tempInstallerDir) throws IOException
    {
        final File output = new File(tempDir, "forge-installer-patched.zip");
        ZipUtils.INSTANCE.compressFiles(tempInstallerDir.listFiles(), output);
        Files.move(output.toPath(), new File(output.getAbsolutePath().replace(".zip", ".jar")).toPath(), StandardCopyOption.REPLACE_EXISTING);
        tempInstallerDir.delete();
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

    public IVanillaVersion getVanilla()
    {
        return this.vanilla;
    }
    
    public List<Mod> getMods()
    {
		return this.mods;
	}
    
    public void setMods(List<Mod> mods)
    {
		this.mods = mods;
	}

	@Override
	public void installMods(File modsDir) throws IOException
	{
		for(Mod mod : this.mods)
		{
	        final File file = new File(modsDir, mod.getName().endsWith(".jar") ? mod.getName() : mod.getName() + ".jar");

	        if (file.exists())
	        {
	            if (!Objects.requireNonNull(getSHA1(file)).equals(mod.getSha1()) || getFileSizeBytes(file) != mod.getSize())
	            {
	                file.delete();
	                this.download(new URL(mod.getDownloadURL()), file);
	            }
	        }
	        else this.download(new URL(mod.getDownloadURL()), file);
		}
	}
	
    private void download(URL in, File out) throws IOException
    {
        this.logger.info(String.format("[Downloader] Downloading %s from %s...", out.getName(), in.toExternalForm()));
        out.getParentFile().mkdirs();
        Files.copy(in.openStream(), out.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
