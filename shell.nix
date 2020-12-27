{pkgs ? import <nixpkgs> {}, jdk ? pkgs.jdk11 }:
let 
  gradle6 = pkgs.callPackage gradle/gradle.nix { java = jdk; };
in
pkgs.mkShell {
  buildInputs = with pkgs; [ jdk gradle6 gitAndTools.gitFull ];
}
