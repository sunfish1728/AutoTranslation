# Delivery status

Only artifacts that pass every release gate may be copied to the final delivery
directory. A successful Gradle build by itself is not a candidate verdict.

| Target | Native toolchain | Compile | Feature parity | Cold start | Release status |
|---|---|---:|---:|---:|---|
| 1.20.1 Fabric | Loom 1.6.12, Java 17 | yes | candidate scope | yes | candidate |
| 1.20.1 Forge | ForgeGradle 6.0.24, Java 17 | yes | candidate scope | yes | candidate |
| 1.21.1 NeoForge | ModDevGradle 2.0.143, NeoForge 21.1.248, Java 21 | yes | candidate scope | yes | candidate |
| 26.1.2 NeoForge | ModDevGradle 2.0.143, NeoForge 26.1.2.95, Gradle 9.4.1, Java 25 | yes | candidate scope | yes | candidate |

Candidate means the production-shaped JAR passed the documented build, archive,
core-test, and cold-start gates. It does not claim a final release or untested
deep Patchouli body/FTB compatibility.
