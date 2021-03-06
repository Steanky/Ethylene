# Ethylene

Ethylene is a open-source, lightweight, general-purpose compatibility layer standing between the developer and the chaotic world of configuration file formats. The purpose of this library is simple: decouple any specific configuration format (TOML, JSON, YML, ...) from the code that actually uses it. Ideally, with minimal work by the developer, you should even be able to change or mix different formats without needing to modify lots of code. 

**Ethylene is currently in early development; the public API should be considered unstable and liable to change over time. Version 1.0.0 will mark the first "stable" release.**

# Get Ethylene

Ethylene binaries are available from a publicly hosted [Cloudsmith repository](https://cloudsmith.io/~steank-f1g/repos/ethylene/packages/).

<details>
  <summary>Latest Versions</summary>
  <ul>
    <li>
      <b>
        ethylene-core <br/> <a href="https://cloudsmith.io/~steank-f1g/repos/ethylene/packages/detail/maven/ethylene-core/latest/a=noarch;xg=com.github.steanky/"><img src="https://api-prd.cloudsmith.io/v1/badges/version/steank-f1g/ethylene/maven/ethylene-core/latest/a=noarch;xg=com.github.steanky/?render=true&show_latest=true" alt="Latest version of 'ethylene-core' @ Cloudsmith" /></a>
      </b>
    </li>
    <li>
      <b>
        ethylene-json <br/> <a href="https://cloudsmith.io/~steank-f1g/repos/ethylene/packages/detail/maven/ethylene-json/latest/a=noarch;xg=com.github.steanky/"><img src="https://api-prd.cloudsmith.io/v1/badges/version/steank-f1g/ethylene/maven/ethylene-json/latest/a=noarch;xg=com.github.steanky/?render=true&show_latest=true" alt="Latest version of 'ethylene-json' @ Cloudsmith" /></a>
      </b>
    </li>
    <li>
      <b>
        ethylene-toml <br/> <a href="https://cloudsmith.io/~steank-f1g/repos/ethylene/packages/detail/maven/ethylene-toml/latest/a=noarch;xg=com.github.steanky/"><img src="https://api-prd.cloudsmith.io/v1/badges/version/steank-f1g/ethylene/maven/ethylene-toml/latest/a=noarch;xg=com.github.steanky/?render=true&show_latest=true" alt="Latest version of 'ethylene-toml' @ Cloudsmith" /></a>
      </b>
    </li>
    <li>
      <b>
        ethylene-hjson <br/> <a href="https://cloudsmith.io/~steank-f1g/repos/ethylene/packages/detail/maven/ethylene-hjson/latest/a=noarch;xg=com.github.steanky/"><img src="https://api-prd.cloudsmith.io/v1/badges/version/steank-f1g/ethylene/maven/ethylene-hjson/latest/a=noarch;xg=com.github.steanky/?render=true&show_latest=true" alt="Latest version of 'ethylene-hjson' @ Cloudsmith" /></a>
      </b>
    </li>
    <li>
      <b>
        ethylene-yaml <br/> <a href="https://cloudsmith.io/~steank-f1g/repos/ethylene/packages/detail/maven/ethylene-yaml/latest/a=noarch;xg=com.github.steanky/"><img src="https://api-prd.cloudsmith.io/v1/badges/version/steank-f1g/ethylene/maven/ethylene-yaml/latest/a=noarch;xg=com.github.steanky/?render=true&show_latest=true" alt="Latest version of 'ethylene-yaml' @ Cloudsmith" /></a>
      </b>
    </li>
  </ul>
</details>

For Gradle, add the Cloudsmith URL to the repositories block like this:

```groovy
repositories {
    maven {
        url 'https://dl.cloudsmith.io/public/steank-f1g/ethylene/maven/'
    }
}
```

Then, in your dependencies section (this example assumes version 1.0.0; check the repository for the latest):

```
dependencies {
    implementation 'com.github.steanky:ethylene-core:1.0.0'
}
```

For Maven, you'd add the repository like this:
```xml
<repositories>
  <repository>
    <id>steank-f1g-ethylene</id>
    <url>https://dl.cloudsmith.io/public/steank-f1g/ethylene/maven/</url>
  </repository>
</repositories>
```

And the dependency like _this_:
```xml
<dependency>
  <groupId>com.github.steanky</groupId>
  <artifactId>ethylene-core</artifactId>
  <version>1.0.0</version>
</dependency>
```

Note that in most cases you'll actually want to depend on a bunch of different Ethylene _modules_, such as `ethylene-json` or `ethylene-toml`, as without at least one of these you won't actually be able to read or write configuration data.

# Examples

To read some configuration data from a source (let's say a file, `/tmp/config.json`), you can do something like this:

```java
ConfigNode node = ConfigBridges.read(new File("/tmp/config.json"), new JsonCodec()).asNode();
```

The `node` object will contain the data stored in the file we just read from. Ethylene knew how to interpret that data because the codec we supplied, `JsonCodec`, is designed for that purpose. If the file contained some other kind of format ??? say, YAML ??? you'd want to use a `YamlCodec` instance from the `ethylene-yaml` module.

Now, let's assume that our file contains the following json:

```json
{
  "string" : "This is a string!",
  "number" : 100,
  "developer" : {
    "name" : "Steanky",
    "repositories" : [ "Ethylene", "Polymer", "RegularCommands" ]
  }
}
```

In order to access the value associated with `string`, we can do the following:

```java
String string = node.get("string").asString();
```

`string`, as expected, will be equal to "This is a string!"

Here are some of the other ways you can access data:

```java
//number == 100
int number = node.get("number").asNumber().intValue();

//nested objects, like developer, are themselves ConfigNode objects
ConfigNode developer = node.get("developer").asNode();

//name.equals("Steanky")
String name = developer.get("name");

//support for lists
ConfigList list = developer.get("repositories").asList();

//repository.equals("Polymer")
String repository = list.get(1).asString();

//you can also directly access nested elements using getElement and providing a "path array" representing member names:
//name.equals("Steanky")
String name = node.getElement("developer", "name").asString();

//you can freely mix string keys and indices to access elements in lists using a path array:
//polymer.equals("Polymer")
String polymer = node.getElement("developer", "repositories", 1).asString();

//ConfigNode and ConfigList objects are fully integrated into Java's type system. they implement Map<String, ConfigElement> and List<ConfigElement>, respectively:
Map<String, ConfigElement> exampleMap = developer;
List<ConfigElement> exampleList = list;

//they're also mutable:
list.remove(0); //removes "Ethylene"
developer.put("age", new ConfigPrimitive(69)); //adds an age field
developer.clear(); //clears all entries from developer
```

For additional examples, check out the `example-project` module.

# Build Ethylene

Building Ethylene is simple! Just clone this repository and run `gradlew build` in the root directory. Build outputs are located in the `[module]/build` directory, replacing `[module]` with the name of the Ethylene module (such as `ethylene-core`) for which you want to find the build output for.

Ethylene uses a few custom Gradle plugins to simplify build logic. These can be found in the `buildSrc/src/main/groovy` folder. If you're creating addons, like support for a specific configuration format, take a look at the `build.gradle` file for an existing module, and use the same structure. Generally, you'll want to apply `ethylene.library-conventions` for most cases.

# Contributing

See `CONTRIBUTING.md` for more information on making contributions.

# Hosting

[![Hosted By: Cloudsmith](https://img.shields.io/badge/OSS%20hosting%20by-cloudsmith-blue?logo=cloudsmith&style=for-the-badge)](https://cloudsmith.com)

Package repository hosting is graciously provided by  [Cloudsmith](https://cloudsmith.com). 
Cloudsmith is the only fully hosted, cloud-native, universal package management solution, that enables your organization to create, store and share packages in any format, to any place, with total confidence.
