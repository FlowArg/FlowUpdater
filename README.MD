# FlowUpdater

### How it's use ?
This is very simple !

#### Vanilla

First, declare the version builder :
```java
IVanillaVersion.Builder versionBuilder = new IVanillaVersion.Builder("1.15.2");
```
You have to put the version you want as parameter, you can set a snapshot if you want or `latest`.

Then, build the version :
```java
IVanillaVersion version = versionBuilder.build(false, VersionType.VANILLA);
```
If you have specified "latest" to the version name, and that the version is a snapshot, replace false by true.

Then, instantiate a new FlowArgMinecraftUpdater, you can use ``SlimUpdaterBuilder#build`` too.
```java
FlowUpdater updater = SlimUpdaterBuilder.build(version, true);
```

Finally, call the update function :
```java
updater.update(new File("/your/path/"), false);
```
Replace false by true if you want to download the server Jar.

### Forge
(You need to setup a vanilla updater !)

(Before calling update function!)

First, in your versionBuilder instance, change the version type to `VersionType.FORGE`.
Next, make a List of Mod objects.
```java
List<Mod> mods = new ArrayList<>();
mods.add(new Mod("OneMod.jar", "sha1ofmod", 85120, "https://link/of/mod.jar"));
mods.add(new Mod("AnotherMod.jar", "sha1ofanothermod", 86120, "https://link/of/another/mod.jar"));
```
You can get a list of mods by providing a json link : `List<Mod> mods = Mod.getModsFromJson("https://url.com/launcher/mods.json");` too.

Finally, set the Forge version corresponding to the wanted Forge version :
```java
updater.setForgeVersion(new NewForgeVersion("31.2.30", version, updater.getLogger(), updater.getCallback(), mods)); // NewForgeVersion -> 1.13 1.16 ; OldForgeVersion -> 1.7 1.12
```
That's all!

### MCP
(You need to setup a vanilla updater !)
In your versionBuilder instance, change the version type to `VersionType.MCP`.
Finally, set to versionBuilder a MCP version before building the version :
```java
versionBuilder.setMcp(new MCP("clientDownloadURL", "name", "clientSha1", "author", "serverDownloadURL", "serverSha1", 1215, 20525));
```
You can get a MCP object instance by providing a json link : `versionBuilder.setMcp(MCP.fromJson("https://url.com/launcher/mcp.json"));` too.
Nothing else to add :).

### Add to dependencies
It's very simple ago !
In your block `repositories`, add this lines :
```groovy
repositories {
    maven {
        url = "https://flowarg.github.io/maven/"
        name = "FlowArg's Maven repository"
    }   
}
```

and in your `dependencies` block :
```groovy
dependencies {
    compile 'fr.flowarg:flowupdater:1.1.1'
}
```

And all it's done !

### Informations
This library use content of another library : [FlowMultitools](https://github.com/FlowArg/FlowMultitools).
