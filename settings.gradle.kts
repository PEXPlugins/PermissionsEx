rootProject.name = "PermissionsEx"

include("api")
include("core")

listOf("sponge", "sponge7", "bukkit", "fabric", "bungee", "velocity").forEach {
    include(":platform:$it")
}

listOf("proxy-common", "hikari-config", "minecraft", "glob").forEach {
    include("impl-blocks:$it")
}

listOf("file", "sql", "conversion").forEach {
    include(":datastore:$it")
}
