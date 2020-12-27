import ca.stellardrift.build.common.adventure
import ca.stellardrift.build.common.configurate

plugins {
    id("pex-component")
}

useCheckerFramework()
useImmutables()

dependencies {
    val adventureVersion: String by project
    val configurateVersion: String by project
    val pCollectionsVersion: String by project
    val slf4jVersion: String by project

    api("org.pcollections:pcollections:$pCollectionsVersion")
    api(adventure("api", adventureVersion))
    implementation(adventure("text-serializer-plain", adventureVersion))
    api(configurate("core", configurateVersion))
    api("org.slf4j:slf4j-api:$slf4jVersion")
}
