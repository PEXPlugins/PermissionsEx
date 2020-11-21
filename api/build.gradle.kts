import ca.stellardrift.build.common.adventure
import ca.stellardrift.build.common.configurate

plugins {
    id("pex-component")
}

useCheckerFramework()
useImmutables()

val configurateVersion: String by project
val adventureVersion: String by project
dependencies {
    api("io.projectreactor:reactor-core:3.4.0")
    api("org.pcollections:pcollections:3.1.4")
    api(adventure("api", adventureVersion))
    implementation(adventure("text-serializer-plain", adventureVersion))
    api(configurate("core", configurateVersion))
}
