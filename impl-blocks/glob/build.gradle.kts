plugins {
    id("pex-component")
    antlr
}

configurations.compile {
    exclude("org.antlr", "antlr4")
}

useCheckerFramework()
dependencies {
    val antlrVersion: String by project
    antlr("org.antlr:antlr4:$antlrVersion")
    implementation("org.antlr:antlr4-runtime:$antlrVersion")
}

tasks.generateGrammarSource {
    this.arguments.addAll(listOf("-visitor", "-no-listener"))
}
