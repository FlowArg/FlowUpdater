# FlowUpdater

### How it's use ?
This is very simple !

First, declare the version builder :
```java
IVersion.Builder versionBuilder = new IVersion.Builder("1.9.1");
```
You have to put the version you want as parameter, you can set a snapshot if you want.

Then, build the version :
```java
IVersion version = versionBuilder.build(false);
```
If you have specified "latest" to the version name, and that the version is a snapshot, replace false by true.

Then, instantiate a new FlowArgMinecraftUpdater :
```java
FlowArgMinecraftUpdater updater = new FlowArgMinecraftUpdater(version, new Reader(version));
```

Define the Updater with your version and instantiate a new Reader with your version as parameter. Finally, call the update function :
```java
updater.update(new File("/your/path/"), false);
```
Replace false by true if you want to download the server Jar.

### Add to dependencies
It's very simple !
in your block `repositories`, add this lines :
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
    compile 'fr.flowarg:flowupdater:1.0.0'
}
```

And all it's done !
