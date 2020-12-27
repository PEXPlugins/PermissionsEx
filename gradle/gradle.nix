{fetchurl, gradleGen, jdk, java ? jdk}:
# Create a custom gradle with our chosen JDK
(gradleGen.override { java = java; }).gradleGen rec {
  name = "gradle-6.7.1";
  nativeVersion = "0.22-milestone-9";

  src = fetchurl {
    url = "https://services.gradle.org/distributions/${name}-bin.zip";
    sha256 = "0789xjsjk5azza4nrsz1v0ziyk7nfc2i1b43v4vqm0y3hvnvaf9j";
  };
}
