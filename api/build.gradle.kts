import ca.stellardrift.build.common.adventure
import ca.stellardrift.build.common.configurate

plugins {
    id("pex-component")
}

useCheckerFramework()
useImmutables()

dependencies {
    val configurateVersion: String by project
    val adventureVersion: String by project
    val slf4jVersion: String by project

    api("org.pcollections:pcollections:3.1.4")
    api(adventure("api", adventureVersion))
    implementation(adventure("text-serializer-plain", adventureVersion))
    api(configurate("core", configurateVersion))
    api("org.slf4j:slf4j-api:$slf4jVersion")
}
