[version]: https://img.shields.io/maven-central/v/fr.flowarg/flowupdater.svg?label=Download
[download]: https://search.maven.org/search?q=g:%22fr.flowarg%22%20AND%20a:%22flowupdater%22

[discord-shield]: https://discordapp.com/api/guilds/730758985376071750/widget.png
[discord-invite]: https://discord.gg/dN6HWHp

[ ![version][] ][download]
[ ![discord-shield][] ][discord-invite]

# FlowUpdater
Welcome on FlowUpdater's repository. FlowUpdater is a free and open source solution to update Minecraft in Java.
It was mainly designed for launcher's purposes but can be used for other usages as well. FlowUpdater focuses on customization and reliability.
The best documentation is the JavaDoc included in FlowUpdater's source code. The rest of the documentation (for instance this readme or the wiki tab on GitHub) has a chance of not being updated.

## Legal and fork notices :warning:
The CurseForge integration works with an API Key which is mine at the moment. **You CAN'T use this key for other purposes outside FlowUpdater.**
If you wish to fork this project, **you HAVE TO use your own API Key**.


## Usage

### Vanilla

First, create a new VanillaVersion, specify arguments.
Then build the version :
```java
VanillaVersion version = new VanillaVersion.VanillaVersionBuilder().withName("1.20.4").build();
```
`VanillaVersion` accepts some arguments to add more libraries, assets or to reach snapshots or custom version of the game.
All accepted arguments are available in the `VanillaVersionBuilder` class.

Add the version to a new `FlowUpdater` instance and build it:
```java
FlowUpdater updater = new FlowUpdater.FlowUpdaterBuilder()
        .withVanillaVersion(version)
        .build();
```

In the same way, `FlowUpdater` accepts many arguments that you can use as you want.
The more important ones to know about are: the logger, the progress callback, the vanilla version, possibly a mod loader version. The full list is available in the `FlowUpdaterBuilder` class.


Finally, call the update function:
```java
updater.update(Paths.get("your/path/"));
```
This `update` method will start the whole checks-and-download pipeline and will return when all the work is done.
You usually need to put this method in a new `Thread` / `ExecutorService` because apart from the assets part, all actions are run on the same thread.


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
            .withOptiFine(new OptiFineInfo("1.16.3_HD_U_G3")) // installing OptiFine for 1.16.3, false = not a preview
            .withFileDeleter(new ModFileDeleter("jei.jar")) // delete bad mods, don't remove the file jei.jar if it's present in the mods' dir.
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
.withMCP(new MCP("clientURL", "clientSha1", 25008229));
```
If you set an empty/null string in url and sha1 and 0 in size, the updater will use the default minecraft jar.
Example on client-only mcp downloading :
```java
.withMCP(new MCP("https://mighya.eu/resources/Client.jar", "f2c219e485831af2bae9464eebbe4765128c6ad6", 23005862));
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
            .withFileDeleter(new ModFileDeleter("sodium.jar")) // delete bad mods ; but it won't remove the file sodium.jar if it's present in the mods' dir.
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
