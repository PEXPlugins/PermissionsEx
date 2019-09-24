package ca.stellardrift.permissionsex.sponge;

class PomData {
	public static final String ARTIFACT_ID = "${project.rootProject.name.toLowerCase()}";
	public static final String NAME = "${project.rootProject.name}";
	public static final String VERSION = "${project.version}${project.ext.pexSuffix}";
	public static final String DESCRIPTION = "${project.ext.pexDescription}";
}
