# Upstream baseline

- Source: <https://github.com/Moirstral/AutoTranslation>
- Fixed commit: `425fd65f62fbb55434edc985cb9d4e2c18cf6b78`
- Upstream license: GNU Affero General Public License v3.0
- Upstream build baseline: Minecraft 1.20.2, not 1.20.1
- Port release: `1.3.0`
- Integration branch: `integration/1.3.0`

The retained `common`, `fabric`, and `forge` directories are upstream-reference
sources. Production artifacts are built only from `core`,
`client-1201-shared`, `fabric-1.20.1`, `forge-1.20.1`,
`neoforge-1.21.1`, and `neoforge-26.1.2`.
