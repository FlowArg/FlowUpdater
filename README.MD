[version]: https://img.shields.io/maven-central/v/fr.flowarg/flowupdater.svg?label=Download
[download]: https://search.maven.org/search?q=g:%22fr.flowarg%22%20AND%20a:%22flowupdater%22

[discord-shield]: https://discordapp.com/api/guilds/730758985376071750/widget.png
[discord-invite]: https://discord.gg/dN6HWHp

[ ![version][] ][download]
[ ![discord-shield][] ][discord-invite]

# FlowUpdater
Check the [Wiki](https://github.com/FlowArg/FlowUpdater/wiki) to know how to use **FlowUpdater** and more information.
Currently, all the documentation (only on GitHub, the documentation in the code is up-to-date) isn't up-to-date because I don't have the time currently to make the tutorial.

## Legal and fork notices :warning:
The CurseForge integration works with an API Key which is mine at the moment. **You CAN'T use this key for other purposes outside FlowUpdater.**
If you wish to fork this project, **you HAVE TO use your own API Key**.


## Usage

### Vanilla

First, create a new VanillaVersion, specify arguments.
Then build the version :
```java
VanillaVersion version = new VanillaVersionBuilder().withName("1.15.2").build();
```
NOTE : Builders are all static classes except `ForgeVersionBuilder`.

You have to put the version you want as parameter, you can set a snapshot (you must set the `withSnapshot` parameter to `true`) if you want or `latest`.
The most of FlowUpdater objects are buildable:
Build a new UpdaterOptions object:
I'm not enabling the re-extracting of natives at each update (`withReExtractNatives(true)`) because `FlowUpdater` know which natives must be extracted.
```java
UpdaterOptions options = new UpdaterOptionsBuilder().build();
```
Note that currently `UpdaterOptions` is useless unless you want to use a custom `ExternalFileDeleter` or you want to disable the silent reading.

Then, instantiate a new FlowUpdater with ``FlowUpdater.Builder#withXArguments#withAnotherArgument#build``. Check the code/[JavaDoc](https://flowarg.github.io/FlowUpdater) for more information.

```java
FlowUpdater updater = new FlowUpdaterBuilder().withVanillaVersion(version).withUpdaterOptions(options).withLogger(someCustomLogger).build();
```

Don't forget to add a progress callback if you want to make a progress bar!

Finally, call the update function :
```java
updater.update("your/path/");
```

### Forge

(You need to setup a vanilla updater !)

First, in your vanilla version builder, change the version type to `VersionType.FORGE`.
Next, make a List of Mod objects (except if you have no mods to install).
```java
List<Mod> mods = new ArrayList<>();
mods.add(new Mod("OneMod.jar", "sha1ofmod", 85120, "https://link/of/mod.jar"));
mods.add(new Mod("AnotherMod.jar", "sha1ofanothermod", 86120, "https://link/of/another/mod.jar"));
```
You can also get a list of mods by providing a json link : `List<Mod> mods = Mod.getModsFromJson("https://url.com/launcher/mods.json");`. A template is available in Mod class.

You can get mods from CurseForge too:
```java
List<CurseFileInfo> modInfos = new ArrayList<>();
// project ID and file ID
modInfos.add(new CurseFileInfo(238222, 2988823));
```
You can also get a list of curse mods by providing a json link : `List<CurseFileInfo> mods = CurseFileInfo.getFilesFromJson("https://url.com/launcher/cursemods.json");`.

Then, build a forge version. For example, I will build a NewForgeVersion.
```java
AbstractForgeVersion forgeVersion = new ForgeVersionBuilder(ForgeVersionBuilder.ForgeVersionType.NEW)
            .withForgeVersion("31.2.29")
            .withCurseMods(modInfos)
            .withOptiFine("1.16.3_HD_U_G3") // installing OptiFine for 1.16.3, false = not a preview
            .withFileDeleter("jei.jar") // delete bad mods, don't remove the file jei.jar if it's present in the mods' dir.
            .build();
```

Finally, set the Forge version corresponding to the wanted Forge version :
```java
.withModLoaderVersion(forgeVersion);
```
That's all!

### MCP

(You need to setup a vanilla updater !)
In your vanilla version builder, change the version type to `VersionType.MCP`.
Finally, set to vanilla version builder a MCP version :
```java
.withMCP("clientURL", "clientSha1", 25008229);
```
If you set an empty/null string in url and sha1 and 0 in size, the updater will use the default minecraft jar.
Example on client-only mcp downloading :
```java
.withMCP("https://mighya.eu/resources/Client.jar", "f2c219e485831af2bae9464eebbe4765128c6ad6", 23005862);
```
You can get an MCP object instance by providing a json link too : `.withMCP("https://url.com/launcher/mcp.json");`.
Nothing else to add :).

### Fabric

(You need to setup a vanilla updater !)

First, in your vanilla version builder, change the version type to `VersionType.FABRIC`.
Next, make a List of Mod objects like for a ForgeVersion.

Then, build a Fabric version.
```java
FabricVersion fabricVersion = new FabricVersionBuilder()
            .withFabricVersion("0.10.8")
            .withCurseMods(modInfos)
            .withMods(mods)
            .withFileDeleter("sodium.jar") // delete bad mods ; but it won't remove the file sodium.jar if it's present in the mods' dir.
            .build();
```

Finally, set the Fabric version corresponding to the wanted Fabric version :
```java
.withModLoaderVersion(fabricVersion);
```
That's all!

## External Files

With FlowUpdater, you can download other files in your update dir!
In your FlowUpdaterBuilder, precise an array list of ExternalFile (can be got by `ExternalFile#getExternalFilesFromJson`).

### About json files...

All json files can be generated by the [FlowUpdaterJsonCreator](https://github.com/FlowArg/FlowUpdaterJsonCreator) !

## Post executions

With FlowUpdater, you can execute some actions after update, like patch a file, kill a process, launch a process, review a config etc...
In your FlowUpdaterBuilder, precise a list of Runnable.

And all it's done !
