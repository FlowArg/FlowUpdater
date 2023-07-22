package fr.flowarg.flowupdater.versions;

import fr.flowarg.flowupdater.download.json.*;
import fr.flowarg.flowupdater.utils.ModFileDeleter;
import fr.flowarg.flowupdater.utils.builderapi.BuilderArgument;
import fr.flowarg.flowupdater.utils.builderapi.BuilderException;
import fr.flowarg.flowupdater.utils.builderapi.IBuilder;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("unchecked")
public abstract class ModLoaderVersionBuilder<T extends IModLoaderVersion, B extends ModLoaderVersionBuilder<T, B>> implements IBuilder<T>
{
    protected final BuilderArgument<List<Mod>> modsArgument = new BuilderArgument<List<Mod>>("Mods", ArrayList::new).optional();
    protected final BuilderArgument<List<CurseFileInfo>> curseModsArgument = new BuilderArgument<List<CurseFileInfo>>("CurseMods", ArrayList::new).optional();
    protected final BuilderArgument<List<ModrinthVersionInfo>> modrinthModsArgument = new BuilderArgument<List<ModrinthVersionInfo>>("ModrinthMods", ArrayList::new).optional();
    protected final BuilderArgument<ModFileDeleter> fileDeleterArgument = new BuilderArgument<>("ModFileDeleter", () -> new ModFileDeleter(false)).optional();
    protected final BuilderArgument<CurseModPackInfo> curseModPackArgument = new BuilderArgument<CurseModPackInfo>("CurseModPack").optional();
    protected final BuilderArgument<ModrinthModPackInfo> modrinthPackArgument = new BuilderArgument<ModrinthModPackInfo>("ModrinthModPack").optional();

    /**
     * Append a mod list to the version.
     * @param mods mods to append.
     * @return the builder.
     */
    public B withMods(List<Mod> mods)
    {
        this.modsArgument.get().addAll(mods);
        return (B) this;
    }

    /**
     * Append a single mod or a mod array to the version.
     * @param mods mods to append.
     * @return the builder.
     */
    public B withMods(Mod... mods)
    {
        return withMods(Arrays.asList(mods));
    }

    /**
     * Append mods contained in the provided JSON url.
     * @param jsonUrl The json URL of mods to append.
     * @return the builder.
     */
    public B withMods(URL jsonUrl)
    {
        return withMods(Mod.getModsFromJson(jsonUrl));
    }

    /**
     * Append mods contained in the provided JSON url.
     * @param jsonUrl The json URL of mods to append.
     * @return the builder.
     */
    public B withMods(String jsonUrl)
    {
        return withMods(Mod.getModsFromJson(jsonUrl));
    }

    /**
     * Append a mod list to the version.
     * @param curseMods CurseForge's mods to append.
     * @return the builder.
     */
    public B withCurseMods(Collection<CurseFileInfo> curseMods)
    {
        this.curseModsArgument.get().addAll(curseMods);
        return (B) this;
    }

    /**
     * Append a single mod or a mod array to the version.
     * @param curseMods CurseForge's mods to append.
     * @return the builder.
     */
    public B withCurseMods(CurseFileInfo... curseMods)
    {
        return withCurseMods(Arrays.asList(curseMods));
    }

    /**
     * Append mods contained in the provided JSON url.
     * @param jsonUrl The json URL of mods to append.
     * @return the builder.
     */
    public B withCurseMods(URL jsonUrl)
    {
        return withCurseMods(CurseFileInfo.getFilesFromJson(jsonUrl));
    }

    /**
     * Append mods contained in the provided JSON url.
     * @param jsonUrl The json URL of mods to append.
     * @return the builder.
     */
    public B withCurseMods(String jsonUrl)
    {
        return withCurseMods(CurseFileInfo.getFilesFromJson(jsonUrl));
    }

    /**
     * Append a mod list to the version.
     * @param modrinthMods Modrinth's mods to append.
     * @return the builder.
     */
    public B withModrinthMods(Collection<ModrinthVersionInfo> modrinthMods)
    {
        this.modrinthModsArgument.get().addAll(modrinthMods);
        return (B) this;
    }

    /**
     * Append a single mod or a mod array to the version.
     * @param modrinthMods Modrinth's mods to append.
     * @return the builder.
     */
    public B withModrinthMods(ModrinthVersionInfo... modrinthMods)
    {
        return withModrinthMods(Arrays.asList(modrinthMods));
    }

    /**
     * Append mods contained in the provided JSON url.
     * @param jsonUrl The json URL of mods to append.
     * @return the builder.
     */
    public B withModrinthMods(URL jsonUrl)
    {
        return withModrinthMods(ModrinthVersionInfo.getModrinthVersionsFromJson(jsonUrl));
    }

    /**
     * Append mods contained in the provided JSON url.
     * @param jsonUrl The json URL of mods to append.
     * @return the builder.
     */
    public B withModrinthMods(String jsonUrl)
    {
        return withModrinthMods(ModrinthVersionInfo.getModrinthVersionsFromJson(jsonUrl));
    }

    /**
     * Assign to the future forge version a mod pack.
     * @param modPackInfo the mod pack information to assign.
     * @return the builder.
     */
    public B withCurseModPack(CurseModPackInfo modPackInfo)
    {
        this.curseModPackArgument.set(modPackInfo);
        return (B) this;
    }

    /**
     * Assign to the future forge version a mod pack.
     * @param modPackInfo the mod pack information to assign.
     * @return the builder.
     */
    public B withModrinthModPack(ModrinthModPackInfo modPackInfo)
    {
        this.modrinthPackArgument.set(modPackInfo);
        return (B) this;
    }

    /**
     * Append a file deleter to the version.
     * @param fileDeleter the file deleter to append.
     * @return the builder.
     */
    public B withFileDeleter(ModFileDeleter fileDeleter)
    {
        this.fileDeleterArgument.set(fileDeleter);
        return (B) this;
    }

    @Override
    public abstract T build() throws BuilderException;
}
