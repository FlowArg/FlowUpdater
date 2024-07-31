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

First, create a new VanillaVersion, and build the version:
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

(You need to setup a vanilla version like above!)

Next, make a List of Mod objects (except if you don't need some).
```java
List<Mod> mods = new ArrayList<>();
mods.add(new Mod("OneMod.jar", "sha1ofmod", 85120, "https://link.com/of/mod.jar"));
mods.add(new Mod("AnotherMod.jar", "sha1ofanothermod", 86120, "https://link.com/of/another/mod.jar"));
```
You can also get a list of mods by providing a json link: `List<Mod> mods = Mod.getModsFromJson("https://url.com/launcher/mods.json");`. A template is available in Mod class.

You can get mods from CurseForge too:
```java
List<CurseFileInfo> modInfos = new ArrayList<>();
// project ID and file ID
modInfos.add(new CurseFileInfo(238222, 2988823));
```
You can also get a list of curse mods by providing a json link: `List<CurseFileInfo> mods = CurseFileInfo.getFilesFromJson("https://url.com/launcher/cursemods.json");`.

On the same pattern, you can get mods from Modrinth.

Then, build a forge version. For example, I will build a NewForgeVersion.
```java
ForgeVersion forgeVersion = new ForgeVersionBuilder()
            .withForgeVersion("1.20.6-50.1.12") // mandatory
            .withCurseMods(modInfos) // optional
            .withOptiFine(new OptiFineInfo("preview_OptiFine_1.20.6_HD_U_I9_pre1")) // installing OptiFine (optional)
            .withFileDeleter(new ModFileDeleter("jei.jar")) // (optional, but recommended) delete bad mods, don't remove the file jei.jar if it's present in mods directory.
            .build();
```

Finally, set the Forge version object to your `FlowUpdaterBuilder`:
```java
.withModLoaderVersion(forgeVersion);
```

### MCP

(You need to setup a vanilla updater!)

There are two ways to setup an MCP version. You can either (1) provide an MCP object (for a simple client for example) or (2) a JSON link to a custom json version which can contains custom assets, custom libraries etc...

(1) set to vanilla version builder a MCP version:
```java
.withMCP(new MCP("clientURL", "clientSha1", 25008229));
```
If you set an empty/null string in url and sha1 and 0 in size, the updater will use the default minecraft jar.
Example for a client-only mcp downloading:
```java
.withMCP(new MCP("https://mighya.eu/resources/Client.jar", "f2c219e485831af2bae9464eebbe4765128c6ad6", 23005862));
```
You can get an MCP object instance by providing a json link too: `.withMCP("https://url.com/launcher/mcp.json");`.

(2)
Still in the vanilla version builder, set a json link to a custom MCP version:
```java
.withCustomVersionJson(new URL("https://url.com/launcher/mcp.json"));
```

You can also provide some more additional libraries or assets with all methods in the `VanillaVersionBuilder` class
(`withAnotherLibraries`, `withAnotherAssets`, `withCustomAssetIndex`).

### Fabric

(You need to setup a vanilla updater!)

Next, make a List of Mod objects like for a ForgeVersion if you need some.

Then, build a Fabric version.
```java
FabricVersion fabricVersion = new FabricVersionBuilder()
            .withFabricVersion("0.10.8") // optional, if you don't set one, it will take the latest fabric loader version available.
            .withCurseMods(modInfos) // optional
            .withMods(mods) // optional
            .withFileDeleter(new ModFileDeleter("sodium.jar")) // (optional but recommended) delete bad mods ; but it won't remove the file sodium.jar if it's present in the mods' dir.
            .build();
```

Finally, set the Fabric version to your `FlowUpdaterBuilder`:
```java
.withModLoaderVersion(fabricVersion);
```

## External Files

With FlowUpdater, you can download other files in your update dir! This system is designed mainly for configs, resource packs.
You can also configure a keep-policy for these files (should the updater download the file again if it is modified?).
In your FlowUpdaterBuilder, define an array list of ExternalFile (by `ExternalFile#getExternalFilesFromJson` for more convenience).

### About json files...

**Deprecated**: All json files can be generated by the [FlowUpdaterJsonCreator](https://github.com/FlowArg/FlowUpdaterJsonCreator)!

There are new tools made by the community that can help you generate some JSON files:
- [FlowJsonCreator by Paulem79](https://github.com/Paulem79/FlowJsonCreator) (Java)
- [FUJC-API by Zuygui](https://github.com/zuygui/flowupdater-json-creator) (Rust)

## Post executions

With FlowUpdater, you can execute some actions after update, like patch a file, kill a process, launch a process, review a config etc...
In your FlowUpdaterBuilder, you have to set a list of Runnable.
It's not always relevant to use this feature, but it can be useful in some specific cases.
