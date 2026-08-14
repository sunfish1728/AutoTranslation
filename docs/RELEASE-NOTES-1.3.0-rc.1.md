# AutoTranslation 1.3.0 RC1

This is the first release candidate of the client-safe native-loader refactor.
Choose exactly one JAR matching your Minecraft version and loader.

| Minecraft | Loader | Java | Required dependencies | Asset SHA-256 |
|---|---|---:|---|---|
| 1.20.1 | Fabric | 17 | Fabric API 0.92.2+, Cloth Config 11.1.118+ | `B700AA6C0A2D38A8A8324DFB534DCB68D8B26AF1E611709A1D73CA0DF24B0F76` |
| 1.20.1 | Forge | 17 | Forge 47+, Cloth Config `[11.1.118,12)` | `4E5D450A3E77A005FB35A0176C7849157430BDA16F3C7333DFABBED2E2379909` |
| 1.21.1 | NeoForge | 21 | NeoForge 21.1.248+, Cloth Config `[15.0.140,16)` | `DA46C310245E3A801210A445866BB8233D821E216DB42715ECB6363DFC0D3AD6` |
| 26.1.2 | NeoForge | 25 | NeoForge 26.1.2.95+, Cloth Config `[26.1.154,27)` | `7E60A4B756D15642E2C2A2015CB8EC1301E602E770550AF08FF5316465BC5C32` |

## Highlights

- Native Fabric, Forge, and NeoForge entry points; no Architectury runtime in production JARs.
- Client-only commands, lifecycle, config screen, key binding, translation icon, and resource-pack creation.
- Bounded and deduplicated translation queue, batching, retry limits, placeholder validation, and atomic storage.
- JVM-default TLS trust and hostname verification. A configured IP changes only DNS routing; Host, SNI, and certificate verification continue to use the domain.
- Immutable render-facing translation snapshots; signed chat and caller-owned Components are not modified.
- Optional, fail-closed Patchouli title compatibility without a required Patchouli dependency.

## Validation

- Pure core: 24 tests, 0 failures/errors/skips.
- All four production JARs passed archive metadata, duplicate-class, Java class-version, license, API, and forbidden-entry audits.
- Client cold-start evidence exists for each loader/version target; the 1.20.1 Forge client-only boundary also passed a dedicated-server startup gate.
- Patchouli conditional cold-start passed on all four loader/version targets using the documented test harness where required.

## Known limitations

- Patchouli compatibility covers conditional loading and a conservative safe-copy `BookEntry` title bridge. Deep book-body translation is not claimed.
- FTB wrapper integration is reflection-only and has not completed a formal dependency cold-start matrix.
- This is a pre-release candidate. Back up existing `AutoTranslation/` and `config/autotranslation.json5` data before broad deployment, although migration and writes are designed to be atomic and backward compatible.

Source baseline: Moirstral/AutoTranslation commit `425fd65f62fbb55434edc985cb9d4e2c18cf6b78`.
Port source: tag `v1.3.0-rc.1` in this fork.
