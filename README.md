# AutoTranslation

The following is a machine translation.

由于很多 Mod 只有英文，非英语母语玩家玩起来可能有些吃力，Mod 作者接受翻译 PR 的时间不定，且不一定有人翻译。为了解决这些痛点，开发了这个
Mod。

Since many mods are only in English, it may be difficult for non-English native players to play. The time it takes for
mod authors to accept translation PRs is uncertain, and there may not necessarily be someone to translate them. In order
to solve these pain points, I developed this Mod.

[更新日志](CHANGELOG.md)

[CHANGELOG](CHANGELOG_en.md)

## 1.3.0 重构移植版 / Refactored port

本 fork 的 1.3.0 版本以客户端安全、原生加载器适配和可重现构建为目标，提供以下四个独立产物：

| Minecraft | Loader | Java | Required dependencies |
|---|---|---:|---|
| 1.20.1 | Fabric | 17 | Fabric API, Cloth Config |
| 1.20.1 | Forge | 17 | Forge 47+, Cloth Config |
| 1.21.1 | NeoForge | 21 | NeoForge 21.1.248+, Cloth Config |
| 26.1.2 | NeoForge | 25 | NeoForge 26.1.2.95+, Cloth Config |

下载 / Downloads: [GitHub Releases](https://github.com/sunfish1728/AutoTranslation/releases)

安装时只需选择与 Minecraft 和加载器相符的一个 JAR，并安装表中必要依赖。Patchouli 与 FTB 均不是必要依赖；Patchouli 相容层只提供安全的标题显示副本，不宣称翻译深层书本正文。

The 1.3.0 fork ships four loader/version-specific JARs. Install exactly one JAR matching the game and loader, together with the required dependencies above. Patchouli and FTB remain optional; deep Patchouli body translation is not claimed.

安全重构包括默认 JVM TLS/hostname 验证、有限队列与重试、不可变显示快照、客户端指令、原子快取写入，以及不修改签名聊天或调用方持有的 Component。

# 功能

## 自动翻译无当前语言翻译的语言文件

自动翻译后加载，并不是官方加载资源包形式，在游戏目录下有个 AutoTranslation 文件夹，未翻译部分的原版文件和翻译文件都有，可以润色后打包成资源包;

## 屏幕翻译（实验性，谨慎开启）

这个功能需要在快捷键设置里设置快捷键后，打开要需要翻译的界面，然后按下快捷键，该功能应该能翻译大多数使用了原版 Screen
渲染机制的模组。其快捷键默认为无，需要自行指定。

> + 如需翻译的屏幕界面内有编辑框，请勿开启，目前通过测试的帕秋莉，FTB 任务的非编辑模式可以开启；
> + 此功能因每帧渲染均需运算，比较耗性能，非必要不要开启。

## 丰富的配置项

通过 Mod 配置菜单，可以配置 Mod 所需参数，具体可在游戏中查看。

## 游戏内指令

```
/auto_translation reload                    重载资源
/auto_translation confirm                   确认执行指令
/auto_translation pack_resource full        全量打包资源包
/auto_translation pack_resource increment   增量打包资源包
```

- - -

## Automatically translate language files that do not have the current language translation

load after automatic translation. It is not an official resource package. There is an Auto Translation folder in the
game directory. There are original files and translated files for the untranslated parts, which can be polished and
packaged into a resource package;

## Screen translation (Experimental, open with caution)

This function requires setting the shortcut key in the shortcut key settings, opening the interface that needs to be
translated, and then pressing the shortcut key. This function should be able to translate most Mods that use the
original Screen rendering mechanism;

## Rich configuration items

Through the Mod configuration menu, you can configure the parameters required by the Mod, which can be viewed in the
game.

## In-game commands

```
/auto_translation reload                    Reload resources
/auto_translation confirm                   Confirm execution of commands
/auto_translation pack_resource full        Fully packaging resource
/auto_translation pack_resource increment   Incremental packaging resource
```

# 翻译 API (Translation API)

## Google

Mod 默认翻译引擎为 Google。

The default translation engine of the Mod is Google.

## 其他 (Other)

目前 Mod 仅集成了 Google 翻译，若需其他翻译 API，需等待开发，或者自行开发，仅需实现接口 `ITranslator`
，然后调用 `TranslatorManager.registerTranslator(String name, Supplier<ITranslator> getInstance)` 即可，然后通过配置项可切换翻译
API。

Currently, Mod only integrates Google Translator. If you need other translation APIs, you need to wait for development
or develop it yourself. You only need to implement the interface `ITranslator`, and then
call `TranslatorManager.registerTranslator(String name, Supplier<ITranslator> getInstance)`, and then The translation
API can be switched through configuration items.
