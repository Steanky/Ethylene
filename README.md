# Ethylene

[![standard-readme compliant](https://img.shields.io/badge/readme%20style-standard-brightgreen.svg?style=flat-square)](https://github.com/RichardLitt/standard-readme)

Ethylene is a open-source, lightweight, general-purpose compatibility layer standing between the developer and the
chaotic world of configuration file formats. The purpose of this library is simple: decouple any specific configuration
format (TOML, JSON, YML, etc) from the code that uses it. Using Ethylene, you can mix and match formats such as these,
and more, with minimal effort.

Ethylene does not implement any parsers; rather, its various modules each depend on a particular parser, and provide a
compatibility layer for it.

_Ethylene is currently in early development; the public API should be considered unstable and liable to change over
time. Version 1.0.0 will mark the first stable release._

## Table of Contents

- [Background](#background)
- [Install](#install)
- [Usage](#usage)
    - [Core](#core)
        - [ConfigElement](#configelement)
        - [Configuration class](#configuration)
        - [Codecs](#codecs)
    - [Mapper](#mapper)
- [Maintainers](#maintainers)
- [Contributing](#contributing)
- [License](#license)
- [Hosting](#hosting)

## Background

Ethylene started out of a need to avoid depending on a particular configuration file format while working on a personal
project. Over time, it has evolved to act as a common interface for reading and writing configuration to and from
various sources.

## Install

[![Latest version of 'ethylene-core' @ Cloudsmith](https://api-prd.cloudsmith.io/v1/badges/version/steank-f1g/ethylene/maven/ethylene-core/latest/a=noarch;xg=com.github.steanky/?render=true&show_latest=true)](https://cloudsmith.io/~steank-f1g/repos/ethylene/packages/detail/maven/ethylene-core/latest/a=noarch;xg=com.github.steanky/)

Pre-built artifacts are hosted on a Cloudsmith Maven repository.

For Gradle, add the repository URL like this:

```groovy
repositories {
    maven {
        url 'https://dl.cloudsmith.io/public/steanky/ethylene/maven/'
    }
}
```

And in your dependencies section:

```groovy
dependencies {
    implementation 'com.github.steanky:ethylene-core:0.12.4'
}
```

You can also build the latest version directly from source:

```shell
git clone https://github.com/Steanky/Ethylene.git
cd ./Ethylene
./gradlew build
```

There are separate artifacts published for each configuration format natively supported by Ethylene, plus an artifact
for `ethylene-core` which contains all core functionality and is a dependency of all other modules. Finally, there
is `ethylene-mapper` which provides support for converting arbitrary configuration directly into POJOs and vice-versa.

All module names start with `ethylene-`. Configuration format modules follow the format `ethylene-[format]`,
where `[format]` is replaced with the lowercase format name (ex. `toml`, `yaml`).

## Usage

Using Ethylene starts with picking the correct modules. Users will always need `ethylene-core`, at the very least, and
usually at least one format module which adds support for a particular configuration format.

For more specific information on individual classes not covered here, please see the relevant Javadoc.

### Core

Ethylene Core contains most of the API intended for use by developers. It provides convenient entrypoints
like `com.github.steanky.ethylene.core.bridge.Configuration`, which contains a selection of static utility methods.

#### ConfigElement

ConfigElement is the common supertype of all configuration data objects. It may represent _scalar_ data, like a string,
or it may represent a list of other ConfigElements, or a map of named (string) keys to other ConfigElements.

Lists of ConfigElements are represented by ConfigList, whereas maps of strings to ConfigElements are represented by
ConfigNode.

#### Configuration

```java
//from the ethylene-json module
ConfigCodec jsonCodec = new JsonCodec();

//some json data, in string form
String json = """
{
    "key": "test"
}
""";

//synchronously read the data
//equivalent methods to read asynchronously are also provided
ConfigElement element = Configuration.read(json, jsonCodec);

//equal to "test"
String value = element.getStringOrThrow("key");
```

`Configuration` contains many more methods for reading from and writing to various sources, both synchronously and
asynchronously.

#### Codecs

Codecs are a fundamental component of Ethylene. Each codec is an object representing a particular configuration format.
They communicate with a parser directly.

For example, `JsonCodec`, from `ethylene-json`, uses Google's Gson internally to read and write JSON data from various
sources. This codec allows you to configure the underlying parser directly by passing in a `Gson` instance to the
constructor. It also has a parameterless constructor that uses a default `Gson`, for convenience. Codecs for other
parsers provide similar functionality.

### Mapper

`ethylene-mapper` provides support for the mapping of arbitrary configuration data.

*Documentation for this section is a work-in-progress.*

## Maintainers

[Steanky](https://github.com/Steanky)

## Contributing

See [the contribution guide](CONTRIBUTING.md).

## License

[GNU General Public License v3.0](LICENSE)

## Hosting

[![Hosted By: Cloudsmith](https://img.shields.io/badge/OSS%20hosting%20by-cloudsmith-blue?logo=cloudsmith&style=for-the-badge)](https://cloudsmith.com)

Package repository hosting is graciously provided by  [Cloudsmith](https://cloudsmith.com).
Cloudsmith is the only fully hosted, cloud-native, universal package management solution, that enables your organization
to create, store and share packages in any format, to any place, with total confidence.

