# README

## PermissionsEx

PermissionsEx \(PEX \(not the pipe\)\) is a full-service permissions plugin giving in-depth control of permissions for a server. PEX is available for various server and proxy platforms:

* [Bukkit/Spigot/Paper](https://dev.bukkit.org/projects/permissionsex) -- [source](https://github.com/PEXPlugins/PermissionsEx/tree/a49a017bb9108f556df1644084308c18bb150dbd/permissionsex-bukkit/README.md)
* [Sponge](https://ore.spongepowered.org/zml/PermissionsEx) -- [source](https://github.com/PEXPlugins/PermissionsEx/tree/a49a017bb9108f556df1644084308c18bb150dbd/permissionsex-sponge/README.md)
* BungeeCord/Waterfall -- [source](https://github.com/PEXPlugins/PermissionsEx/tree/a49a017bb9108f556df1644084308c18bb150dbd/permissionsex-bungee/README.md)
* \[Velocity\] -- [source](https://github.com/PEXPlugins/PermissionsEx/tree/a49a017bb9108f556df1644084308c18bb150dbd/permissionsex-velocity/README.md)
* Fabric -- [source](https://github.com/PEXPlugins/PermissionsEx/tree/a49a017bb9108f556df1644084308c18bb150dbd/permissionsex-fabric/README.md)

💬 Having an issue setting up PEX? Check out our [Discord](https://discord.gg/PHpuzZS)

🐞 Found a bug? File a [bug report](https://github.com/PEXPlugins/PermissionsEx/tree/a49a017bb9108f556df1644084308c18bb150dbd/PEXPlugins/PermissionsEx/issues/README.md)

⛏ [Development Builds](https://jenkins.addstar.com.au/job/PermissionsEx/lastSuccessfulBuild/)

## Development

Want to access permissions in your plugin? PermissionsEx tries to provide extensive compatibility with native APIs, but sometimes more direct access is needed. In that case, PEX has an extensive API that allows querying any sort of information.

PEX can also be extended to support new platforms or implement new data store formats using just the implementation-agnostic `core` API.

### On Maven

PEX is available in a format that can be retrieved in Maven. Its repository:

```markup
<repository>
    <id>pex-repo</id>
    <url>https://repo.glaremasters.me/repository/permissionsex/</url>
</repository>
```

and its dependency specification is:

```markup
<dependency>
    <groupId>ca.stellardrift.permissionsex</groupId>
    <artifactId>permissionsex-core</artifactId> <!-- replace with -sponge or -bukkit depending on which platform you're using -->
    <version>2.0-SNAPSHOT</version>
</dependency>
```

### On Gradle

We work in the Kotlin DSL, but Groovy should be similar.

```kotlin
repositories {
    maven(url = "https://repo.glaremasters.me/repository/permissionsex/") {
        name = "pex-repo"
    }
}

dependencies {
    implementation("ca.stellardrift.permissionsex:permissionsex-core:2.0-SNAPSHOT")
}
```

## Contributing

PermissionsEx always appreciates well thought-out pull requests for code changes, documentation improvements, and translations. All contributions must be released under the terms of the Apache 2.0 license.

We build with Gradle -- a wrapper is provided, to build the project simply run `./gradlew build`. Feel free to run ideas by me in the discord before spending time implementing something that doesn't match my vision for the plugin.

