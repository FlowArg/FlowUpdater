package fr.flowarg.flowupdater.minecraft.versions;

import java.io.File;

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
}