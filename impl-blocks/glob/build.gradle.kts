import ca.stellardrift.permissionsex.gradle.Versions

plugins {
    antlr
}

configurations.compile {
    exclude("org.antlr", "antlr4")
}

dependencies {
    antlr("org.antlr:antlr4:${Versions.ANTLR}")
    implementation("org.antlr:antlr4-runtime:${Versions.ANTLR}")
    compileOnlyApi("org.checkerframework:checker-qual:3.7.1")
}

tasks.generateGrammarSource {
    this.arguments.addAll(listOf("-visitor", "-no-listener"))
}

opinionated {
    useJUnit5()
}
