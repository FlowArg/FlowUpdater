package fr.flowarg.flowupdater.versions;

import java.io.File;
import java.io.IOException;

/**
 * Represent a Forge version.
 * Implemented by {@link OldForgeVersion} & {@link NewForgeVersion}.
 * @author FlowArg
 */
public interface IForgeVersion
{
	/**
	 * This function install a Forge version at the specified directory.
	 * @param dirToInstall Specified directory.
	 */
	void install(final File dirToInstall);
	
	/**
	 * This function install mods at the specified directory.
	 * @param dirToInstall Specified mods directory.
	 * @throws IOException If install fail.
	 */
	void installMods(final File dirToInstall) throws IOException;
}