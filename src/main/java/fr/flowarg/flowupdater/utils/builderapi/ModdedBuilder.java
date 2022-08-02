package fr.flowarg.flowupdater.utils.builderapi;

import fr.flowarg.flowupdater.download.json.*;
import fr.flowarg.flowupdater.utils.ModFileDeleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class ModdedBuilder<T, B extends ModdedBuilder<T, B>> implements IBuilder<T>
{
    protected final BuilderArgument<List<Mod>> modsArgument = new BuilderArgument<List<Mod>>("Mods", ArrayList::new).optional();
    protected final BuilderArgument<List<CurseFileInfo>> curseModsArgument = new BuilderArgument<List<CurseFileInfo>>("CurseMods", ArrayList::new).optional();
    protected final BuilderArgument<List<ModrinthVersionInfo>> modrinthModsArgument = new BuilderArgument<List<ModrinthVersionInfo>>("ModrinthMods", ArrayList::new).optional();
    protected final BuilderArgument<ModFileDeleter> fileDeleterArgument = new BuilderArgument<>("ModFileDeleter", () -> new ModFileDeleter(false)).optional();
    protected final BuilderArgument<CurseModPackInfo> curseModPackArgument = new BuilderArgument<CurseModPackInfo>("CurseModPack").optional();
    protected final BuilderArgument<ModrinthModPackInfo> modrinthPackArgument = new BuilderArgument<ModrinthModPackInfo>("ModrinthModPack").optional();

    protected abstract B getThis();

    /**
     * Append a mods list to the version.
     * @param mods mods to append.
     * @return the builder.
     */
    public B withMods(List<Mod> mods)
    {
        this.modsArgument.get().addAll(mods);
        return getThis();
    }

    /**
     * Append a mods list to the version.
     * @param mods mods to append.
     * @return the builder.
     */
    public B withMods(Mod... mods)
    {
        return withMods(Arrays.asList(mods));
    }

    /**
     * Append a mods list to the version.
     * @param curseMods CurseForge's mods to append.
     * @return the builder.
     */
    public B withCurseMods(List<CurseFileInfo> curseMods)
    {
        this.curseModsArgument.get().addAll(curseMods);
        return getThis();
    }

    /**
     * Append a mods list to the version.
     * @param curseMods CurseForge's mods to append.
     * @return the builder.
     */
    public B withCurseMods(CurseFileInfo... curseMods)
    {
        return withCurseMods(Arrays.asList(curseMods));
    }

    /**
     * Append a mods list to the version.
     * @param modrinthMods Modrinth's mods to append.
     * @return the builder.
     */
    public B withModrinthMods(List<ModrinthVersionInfo> modrinthMods)
    {
        this.modrinthModsArgument.get().addAll(modrinthMods);
        return getThis();
    }

    /**
     * Append a mods list to the version.
     * @param modrinthMods Modrinth's mods to append.
     * @return the builder.
     */
    public B withModrinthMods(ModrinthVersionInfo... modrinthMods)
    {
        return withModrinthMods(Arrays.asList(modrinthMods));
    }

    /**
     * Assign to the future forge version a mod pack.
     * @param modPackInfo the mod pack information to assign.
     * @return the builder.
     */
    public B withCurseModPack(CurseModPackInfo modPackInfo)
    {
        this.curseModPackArgument.set(modPackInfo);
        return getThis();
    }

    /**
     * Assign to the future forge version a mod pack.
     * @param modPackInfo the mod pack information to assign.
     * @return the builder.
     */
    public B withModrinthModPack(ModrinthModPackInfo modPackInfo)
    {
        this.modrinthPackArgument.set(modPackInfo);
        return getThis();
    }

    /**
     * Append a file deleter to the version.
     * @param fileDeleter the file deleter to append.
     * @return the builder.
     */
    public B withFileDeleter(ModFileDeleter fileDeleter)
    {
        this.fileDeleterArgument.set(fileDeleter);
        return getThis();
    }
}
