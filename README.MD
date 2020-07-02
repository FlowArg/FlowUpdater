# FlowUpdater

### How it's use ?
This is very simple !

#### Vanilla

First, declare the version builder :
```java
IVanillaVersion.Builder versionBuilder = new IVanillaVersion.Builder("1.9.1");
```
You have to put the version you want as parameter, you can set a snapshot if you want or `latest`.

Then, build the version :
```java
IVanillaVersion version = versionBuilder.build(false);
```
If you have specified "latest" to the version name, and that the version is a snapshot, replace false by true.

Then, instantiate a new FlowArgMinecraftUpdater, you can use ``SlimUpdaterBuilder#build`` too.
```java
FlowArgMinecraftUpdater updater = SlimUpdaterBuilder.build(version, new VanillaReader(version), true);
```

Finally, call the update function :
```java
updater.update(new File("/your/path/"), false);
```
Replace false by true if you want to download the server Jar.

### Forge
(You need to setup a vanilla updater)
First, make a List of Mod objects.
```
List<Mod> mods = new ArrayList<>();
mods.add(new Mod("OneMod.jar", "sha1ofmod", 85120, "https://link/of/mod.jar"));
mods.add(new Mod("AnotherMod.jar", "sha1ofanothermod", 86120, "https://link/of/another/mod.jar"));
```


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
    compile 'fr.flowarg:flowupdater:1.1.0'
}
```

And all it's done !

### Informations
This library use content of another library : [FlowMultitools](https://github.com/FlowArg/FlowMultitools).
