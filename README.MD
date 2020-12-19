[version]: https://api.bintray.com/packages/flowarg/maven/FlowUpdater/images/download.svg
[download]: https://bintray.com/flowarg/maven/FlowUpdater/_latestVersion
[discord-shield]: https://discordapp.com/api/guilds/730758985376071750/widget.png
[discord-invite]: https://discord.gg/dN6HWHp

[ ![version][] ][download]
[ ![discord-shield][] ][discord-invite]

# FlowUpdater

### Installation

In your block `repositories`, add this lines :
```groovy
repositories {
    jcenter()
    maven {
        url "https://jitpack.io/"
        name = "JitPack"
    } 
}
```

and in your `dependencies` block :
```groovy
dependencies {
    implementation 'fr.flowarg:flowupdater:VERSION'
}
```

## Support, Help, Contribution
Join the Discord server for support, help and contribution : https://discord.gg/yvv78kV

## Usage

### Vanilla

First, create a new VanillaVersion, specify arguments.
Then build the version :
```java
VanillaVersion version = new VanillaVersionBuilder().withName("1.15.2").withSnapshot(false).withVersionType(VersionType.VANILLA).build();
```
You have to put the version you want as parameter, you can set a snapshot if you want or `latest`.
If you have specified "latest" to the version name, and that the version is a snapshot, replace false by true.

Then, instantiate a new FlowUpdater with ``FlowUpdaterBuilder#withXArguments#withAnotherArguemtn#build``. Check the code for more information.
The most of FlowUpdater objects are buildable:
Build a new UpdaterOptions object:
I'm enabling the re-extracting of natives at each update.
```java
UpdaterOptions options = new UpdaterOptionsBuilder().withReExtractNatives(true).build();
```
```java
FlowUpdater updater = new FlowUpdaterBuilder().withVersion(version).withUpdaterOptions(options).withLogger(someCustomLogger).build();
```

Don't forget to add a progress callback if you want to make a progress bar !

Finally, call the update function :
```java
updater.update(new File("/your/path/"));
```
Replace false by true if you want to download the server Jar (useful for server installation program).

### Forge

(You need to setup a vanilla updater !)

First, in your vanilla version builder, change the version type to `VersionType.FORGE`.
Next, make a List of Mod objects.
```java
List<Mod> mods = new ArrayList<>();
mods.add(new Mod("OneMod.jar", "sha1ofmod", 85120, "https://link/of/mod.jar"));
mods.add(new Mod("AnotherMod.jar", "sha1ofanothermod", 86120, "https://link/of/another/mod.jar"));
```
You can get a list of mods by providing a json link too : `List<Mod> mods = Mod.getModsFromJson("https://url.com/launcher/mods.json");`.

You can get mods from CurseForge too:
```java
List<CurseFileInfos> modInfos = new ArrayList<>();
// project ID and file ID
modInfos.add(new CurseFileInfos(238222, 2988823));
```
You can get a list of curse mods by providing a json link too : `List<CurseFileInfos> mods = CurseFileInfos.getFilesFromJson("https://url.com/launcher/cursemods.json");`.

Then, build a forge version. For example, I will build a NewForgeVersion.
```java
AbstractForgeVersion forgeVersion = new ForgeVersionBuilder(ForgeVersionBuilder.ForgeVersionType.NEW)
            .withForgeVersion("31.2.29")
            .withVanillaVersion(version)
            .withLogger(logger)
            .withProgressCallback(callback)
            .withCurseMods(modInfos)
            .withOptifine(new OptifineInfo("1.16.3_HD_U_G3", false)) // installing optifine for 1.16.3, false = not a preview
            .withUseFileDeleter(new ModFileDeleter(true, "jei.jar")) // delete bad mods, don't remove the file jei.jar if it's present in the dir.
            .withNoGui(true) // only for new forge version: true -> don't show the forge installer gui. false -> show the forge installer gui.
            .build();
```

Finally, set the Forge version corresponding to the wanted Forge version :
```java
.withForgeVersion(forgeVersion);
```
That's all!

### MCP

(You need to setup a vanilla updater !)
In your vanilla version builder, change the version type to `VersionType.MCP`.
Finally, set to vanilla version builder a MCP version :
```java
.withMCP(new MCP("clientURL", "name", "clientSha1", "author", "serverURL", "serverSha1", 1215, 20525));
```
If you set an empty/null string in url and sha1 and 0 in size, the updater will use the default minecraft jar.
Example on client-only mcp downloading :
```java
.withMCP(new MCP("https://mighya.eu/resources/Client.jar", "f2c219e485831af2bae9464eebbe4765128c6ad6", "", "", 23005862, 210052));
```
You can get a MCP object instance by providing a json link too : `.withMCP(MCP.getMCPFromJson("https://url.com/launcher/mcp.json"));`.
Nothing else to add :).

### Fabric

(You need to setup a vanilla updater !)

First, in your vanilla version builder, change the version type to `VersionType.FABRIC`.
Next, make a List of Mod objects.
```java
List<Mod> mods = new ArrayList<>();
mods.add(new Mod("OneMod.jar", "sha1ofmod", 85120, "https://link/of/mod.jar"));
mods.add(new Mod("AnotherMod.jar", "sha1ofanothermod", 86120, "https://link/of/another/mod.jar"));
```
You can get a list of mods by providing a json link too : `List<Mod> mods = Mod.getModsFromJson("https://url.com/launcher/mods.json");`.

You can get mods from CurseForge too:
```java
List<CurseFileInfos> modInfos = new ArrayList<>();
// project ID and file ID
modInfos.add(new CurseFileInfos(238222, 2988823));
```
You can get a list of curse mods by providing a json link too : `List<CurseFileInfos> mods = CurseFileInfos.getFilesFromJson("https://url.com/launcher/cursemods.json");`.

Then, build a Fabric version.
```java
FabricVersion fabricVersion = new FabricVersionBuilder()
            .withFabricVersion("0.10.8")
            .withVanillaVersion(version)
            .withLogger(logger)
            .withProgressCallback(callback)
            .withCurseMods(modInfos)
            .withUseFileDeleter(new ModFileDeleter(true, "sodium.jar")) // delete bad mods ; doesn't remove the file sodium.jar if it's present in the dir.
            .build();
```

Finally, set the Fabric version corresponding to the wanted Fabric version :
```java
.withFabricVersion(forgeVersion);
```
That's all!

### External Files

With FlowUpdater, you can download other files in your update dir!
In your FlowUpdaterBuilder, precise an array list of ExternalFile (can be get by `ExternalFile#getExternalFilesFromJson`).

### About json files...

All json files can be generated by the [FlowUpdaterJsonCreator](https://github.com/FlowArg/FlowUpdaterJsonCreator) !

### Post executions

With FlowUpdater, you can execute some actions after update, like patch a file, kill a process, launch a process, review a config etc...
In your FlowUpdaterBuilder, precise a list of Runnable.

And all it's done !

### Informations

This library has some dependencies :
- [FlowMultiTools](https://github.com/FlowArg/FlowMultitools).
- [PluginLoaderAPI](https://github.com/FlowArg/PluginLoaderAPI).
- [Gson](https://github.com/Google/Gson).


### Donating

If you want to contribute to the project, please consider providing me with some support:
https://paypal.me/Philouse
