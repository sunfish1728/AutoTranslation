# Mixin migration inventory

The fixed upstream commit declares 17 entries in the common configuration and
one loader-specific `GameRendererMixin`, for 18 active declarations per loader
JAR. `PatchouliBookTextRendererMixin` exists in source but is not declared,
bringing the complete inventory to 19. This count is taken from the fixed Git
tree, not from an estimate.

| Upstream mixin | Original target and purpose | 1.3.0 replacement | Load condition | Status |
|---|---|---|---|---|
| `EntityMixin` | `Entity`; suppress or alter names/messages | No server/game object mutation; display-copy renderer | client display only | Removed from production |
| `MinecraftServerMixin` | `MinecraftServer`; suppress server/chat logging translation | None | never | Removed |
| `PlayerChatMessageMixin` | signed/decorated chat contents | Client display copy; signed content remains untouched | client only | Removed |
| `PlayerMixin` | player name/display name | Client display copy | client only | Removed |
| `PlayerTeamMixin` | team display/prefix/suffix | Client display copy | client only | Removed |
| `ServerPlayerMixin` | server system messages | None | never | Removed |
| `ChatComponentMixin` | mark chat components non-translatable | No source-chat translation; only immutable display copies at safe render boundaries | client only | Obsolete/covered: signed chat and chat source Components stay untouched |
| `ClientLanguageMixin` | replaced `loadFrom` loop and cleared language list | Native client reload listener reads English/target stacks without changing vanilla control flow | client reload | Implemented in native 1.20.1 Fabric/Forge, 1.21.1 NeoForge, and 26.1.2 NeoForge |
| `EditBoxMixin` | temporarily blacklisted editable text during render | Screen class blacklist and display boundary | client only | Implemented: editable, command-block, sign, chat and FTB config/quest screens are denied |
| `FontMixin` | rewrote raw render string | Immutable-cache lookup returning a display string | whitelisted screen only | Implemented by narrow `GuiGraphics` String/safe-literal hooks |
| common `GameRendererMixin` | toggled per-frame screen translation readiness | Native keybind plus per-screen state | client only | Replaced; no per-frame mutable state |
| `MutableComponentMixin` | shadowed and mutated component contents | Never mutate the source component; create translated display copy | client only | Removed; `GuiGraphics`/tooltip/BookEntry return only safe literal copies |
| `ScreenMixin` | key toggle and screen flags | Native key event plus weak screen-status registry | client only | Implemented; icon and keybind use canonical `ScreenTranslationState.screenId(Object)` |
| `TooltipMixin` | mutated final tooltip message off-thread | Build translated tooltip display list from immutable cache | client only | Replaced by safe render-boundary copy; no mutation |
| `compat.patchouli.BookEntryMixin` | Patchouli entry name | Isolated `required:false` config plus class-presence plugin; `getName` returns a new safe literal display copy | Patchouli present | Implemented; see version-specific evidence below |
| `compat.patchouli.SpanMixin` | Patchouli styled substring | None: its old role was to support mutable-component translation | never | Obsolete: new architecture neither mutates the span nor needs a suppression hook |
| `compat.patchouli.WordMixin` | Patchouli hover word | None: preserve Patchouli style/click/hover semantics rather than changing render state | never | Obsolete by design; safe BookEntry title bridge is the supported scope |
| loader `GameRendererMixin` | wrapped loader-specific tooltip rendering calls | Narrow native `GuiGraphics`/tooltip render hooks | client only | Replaced in native 1.20.1, 1.21.1, and 26.1.2 |
| `compat.patchouli.PatchouliBookTextRendererMixin` | legacy renderer translation | Do not enable unless required by a tested upstream version | Patchouli present | Deprecated/inactive upstream |

No production mixin configuration may contain a server target. Core mixins must
remain `required: true`; optional compatibility must use a separate
`required: false` configuration with a class-presence plugin.

## Render and chat scope

The 1.3.0 architecture deliberately does not translate signed chat, packets, or
the source `Component` owned by Minecraft or another mod.  Chat therefore has
no direct replacement mixin.  Translation is an immutable String/Component
*display copy* only at the narrow `GuiGraphics` and tooltip render paths, and
only for a root literal without siblings, click event, hover event, or
insertion.  This is why `ChatComponentMixin`, `MutableComponentMixin`, and the
old Patchouli body hooks are obsolete rather than pending reimplementations.

## Patchouli verification scope and evidence

The inspected official 1.20.1 and 1.21.1 artifacts expose
`BookEntry#getName()` as `MutableComponent`, while `Span` returns styled
substrings and `Word#render` receives a style with hover/click semantics.  The
compatibility bridge therefore only replaces a BookEntry return value when it
is a root literal with no siblings, click event, hover event, or insertion.  It
preserves the safe style on a new Component and never edits Patchouli-owned
state.  `Span` and `Word` are intentionally obsolete: the new architecture has
no mutable-component translation to suppress and does not claim deep body
translation.

| Native loader/version | Optional artifact and result | Evidence / limitation |
|---|---|---|
| Fabric 1.20.1 | `Patchouli-1.20.1-85-FABRIC`: PASS | Cold start loaded `patchouli`, reached ResourceManager, OpenAL, atlases, and Patchouli's `BookContentResourceListenerLoader`. |
| Forge 1.20.1 / Forge 47.4.10 userdev | `Patchouli-1.20.1-85-FORGE`: PASS (test-only harness) | The initial userdev launch stopped in Patchouli's `AccessorScreen` because its refmap was not remapped.  The test-only harness sets `mixin.env.remapRefMap=true` and supplies a generated SRG remapping file; the log then reports `Remapping refMap patchouli.refmap.json`, reaches Forge startup, ResourceManager, OpenAL/atlases, and Patchouli's resource preloader without a fatal error.  The harness and dependency are enabled only by `-PcompatSmoke`; production metadata/JAR remains dependency-free. |
| NeoForge 1.21.1 / NeoForge 21.1.248 | `Patchouli-1.21.1-92-NEOFORGE`: PASS | Cold start listed `patchouli`, reloaded `mod/patchouli`, reached OpenAL/atlases, and logged Patchouli's resource preloader. |
| NeoForge 26.1.2 / NeoForge 26.1.2.95 | `patchouli-neoforge-26.1-94`: PASS | Test-only `-PcompatSmoke` cold start listed `patchouli`, reloaded `mod/patchouli`, reached OpenAL/atlases, and logged Patchouli's resource preloader. Production metadata/JAR remains dependency-free. |

The FTB integration remains reflection-only (`ScreenWrapper#getGui`) and has no
hard optional class reference.  No compatible formal FTB dependency chain was
available in the local test environment, so no FTB cold-start claim is made.

The four loader/version cold-start gates validate conditional mixin discovery and loader mapping;
they do **not** claim that a Patchouli title was translated during those runs.
The test environment preloaded no authored Patchouli book JSON and has no
repeatable UI automation fixture that constructs a `BookEntry` and opens it.
Accordingly, `BookEntry#getName` is limited to its independently-auditable
safe-copy return bridge, while deep Patchouli body (`Span`/`Word`) translation
remains explicitly unsupported.
