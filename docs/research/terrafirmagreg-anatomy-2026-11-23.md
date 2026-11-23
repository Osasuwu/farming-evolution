---
topic: terrafirmagreg-anatomy
tags: area:modpack-architecture, research
source_provenance: /research invocation from the farming-evolution planning session, 2026-11-23
status: open
audit: 2026-11-23 — 170 claims: 126 SUPPORTED, 44 PARTIAL, 0 UNSUPPORTED, 0 UNCHECKABLE
---

## Question

Как устроена сборка TerraFirmaGreg-Modern и что из её устройства можно взять для farming-evolution. Источники первой очереди: репозитории TerraFirmaGreg-Team на GitHub (Modpack-Modern, Core-Modern, Tools-Modern, QuestBook-Modern, Modpack-Template, Mod-Template), вики сборки, документация TerraFirmaCraft. Вопросы: (1) разделение работы KubeJS / датапаки / Java-мод Core-Modern и почему; (2) генерация мира и климат TFC: зависимость климата от координат, привязка растений, животных, руд; можно ли использовать генерацию без остального TFC, лицензия; (3) сцепка TFC и GregTech: где ранняя часть гейтит позднюю, что с ранними механиками в поздней игре; (4) блокировка ванильной еды и рецептов, удаление/замена мобов; (5) процесс: сборка релизов, тесты, квесты, локализация, сколько людей и времени; (6) типичные жалобы игроков.

Asked six sub-questions, answering the first four because the skill caps one run at four sub-questions (LEDGER.md § Splitting). Sub-questions (5) process and (6) player complaints are not answered here; they are written out under Open questions as the next run.

Sub-questions: Q1 — how work is divided between KubeJS scripts, datapacks/resource packs and the team's Java mod, and why; Q2 — TFC world generation and climate, how biology and ores are tied to it, whether it can be used without the rest of TFC, and the licence; Q3 — how TFC and GregTech are coupled, where the early part gates the late one and what happens to early mechanics later; Q4 — how vanilla food, recipes and mobs are blocked or replaced.

## Summary

Версии: сборка стоит на Minecraft 1.20.1 / Forge 47.4.13 и закрепляет 279 проектов [C117], [C240]; у TFC есть ветка 1.21.x под NeoForge 1.21.1 [C201].

**Q1. Слои.** Правило команды записано в гайде для контрибьюторов: свои машины и мультиблоки GregTech — в Java-мод, а рецепты, простые предметы и блоки, материалы, данные, ассеты и лут — через KubeJS, «when most convenient» [C102], [C103]. Репозиторий сборки не содержит Java и папки mods; почти всё лежит в `kubejs/` [C107]: 12 770 файлов ресурс-пака, 2 917 файлов датапака и около 487 скриптов на ~3,09 МБ [C108], [C109]. Датапак — в основном генерация мира и структуры [C114]. Квесты — обычные SNBT-файлы FTB Quests в `config`, тексты — в lang-файлах [C118]. Java-мод Core-Modern — 1 070 своих файлов, из них 376 — миксины в чужие моды (GregTech, Minecraft, Create, TFC) [C119], [C120], [C121]; он же регистрирует плагин KubeJS; в пакете плагина лежат классы билдеров блоков и схем рецептов (по именам файлов) [C123]. На практике граница проницаема в обе стороны: в Java ушли блок, которому понадобился настоящий класс CarpetBlock [C132], и рыболовные сети (в том же PR кэширование цвета чинило производительность) [C134], обратно в KubeJS — файл обработки руд [C137]; при этом часть машин GregTech всё ещё описана в KubeJS вопреки правилу [C113]. Итоговая картина — [C139]. С 12.09.2026 Core-Modern содержит внутри себя полный форк TerraFirmaCraft под исходным mod id `tfc`, вместе с форками TFC Ambiental и TFC Textile [C119], [C125], [C131]; названная причина форка TFC — «long-term stability» разработки сборки [C130], причины двух других — оптимизация, расширяемость и контроль [C128], [C129].

**Q2. Климат и генерация.** Базовый климат TFC — треугольная волна по одной оси на параметр: осадки вдоль X, температура вдоль Z [C203]; диапазоны на уровне моря — от −20 до 30 °C и 0–500 мм плюс шум [C204]. Масштаб задаётся в JSON пресета мира; по умолчанию 20 000 блоков между экстремумами [C205], [C206], [C208]. Побережья и высота поправляют базу [C209], [C210]. В 1.20.x сезонная амплитуда не меняет знак, полушарий нет [C212]; в 1.21.x знак меняется с Z, появляются южные полушария и сезонность осадков [C213], [C214], [C215]. В Core-Modern (1.20.1) есть миксин с формулой полушарий из 1.21; он срабатывает только при включённом собственном пайплайне генерации TFG (`useTfgOverworldPipeline()`) [C240]. Привязка биологии: диапазон, в котором блок может расти, — через climate range JSON [C218], размещение диких растений — декоратор `tfc:climate` на placed feature [C220], [C221], спавн животных — fauna JSON с тем же климатическим блоком [C223]. И climate ranges, и fauna датапаком можно только менять, но не добавлять [C218], [C223]. Руды к климату не привязаны: жила связана с породой через карту замены блоков и имеет только необязательный фильтр по тегу биома [C226], [C227]; слои пород задаются пресетом по типу участка [C228]. Отсюда: «материал только в одном климате» естественен для растений и животных и требует обходного пути для руд [C229]. Отдельной сборки «только генерация» не найдено: генерация — это пресет мира `tfc:overworld` внутри мода, который сам называет себя total conversion [C230], [C234]; координатную модель климата обработчик TFC события `SelectClimateModelEvent` ставит только при TFC-генераторе [C232], а модель по умолчанию читает климат из ванильного биома [C233]; общего выключателя survival-механик в серверном конфиге нет, только отдельные тумблеры [C235]. Вывод о трёх вариантах (зависеть от TFC и гасить механики; свой ClimateModel через событие; форк кода) — [C236]. Лицензия: EUPL 1.2, если в файле не сказано иное [C244]; она разрешает изменение и распространение [C247], но производная работа распространяется под EUPL либо, если она объединена с работой под лицензией из списка совместимых (GPL, AGPL, MPL, LGPL, EPL), под той лицензией [C248], [C249], с сохранением уведомлений, пометкой изменений и исходниками [C250]. Часть кода происходит от TFC 1.7.10 по отдельному разрешению, условий которого README не приводит [C246]. Что это значит для своего мода — [C251] (это вывод, не юридическая консультация).

**Q3. Сцепка TFC и GregTech.** Сцепка сделана на одной системе материалов: материалам GregTech скриптом навешивается свойство TFC (температуры ковки, сварки и плавления, тир наковальни) [C303], [C304], [C305], рецепты наковальни генерируются циклом по материалам [C306], а рецепты обоих модов переопределяются [C307], [C313]. Вход в пар высокого давления и LV требует всей металлургической цепочки TFC: кричное железо из горна, сталь из доменной печи TFC, чёрная сталь для котлов и пяти других паровых машин высокого давления, синяя сталь для катушек первой электропечи [C310], [C311], [C312], [C314], [C316], [C318]; собственная примитивная доменная печь GregTech внесена в конфиг удаления предметов [C309]. Дальше металлургия TFC вытесняется по шагам: паровой мультиблок автоматизирует горн, кузнечный молот получает рецепты шагов наковальни, на LV квест велит разобрать доменную печь TFC, экструдер делает головки инструментов [C323], [C325], [C326], [C327], [C349]. Биологическая сторона не выводится из игры: порча еды остаётся со ставкой по умолчанию и действует внутри автоматики [C329], [C331], питание расширяется [C345], под еду, культуры, скот и рыбу есть машины GregTech [C328], [C332], [C335], [C336], причём теплица намеренно замедлена до темпа TFC [C333], а разведение и кормление машина для скота не делает [C335]. Найденные в этом прогоне поздние био-входы — шерсть марсианских животных на EV [C340], `tfg:glacian_wool` в рецепте напряжения IV [C341], масла и биомасса в венерианской биохимии [C342], [C343]; полный перечень не составлялся. Для топливной ветки от фермы на LV квест называет альтернативы без фермы [C337], [C338]. Обобщение — [C350]. Глава IV в квестах — заглушка под будущую переработку [C321], так что самая поздняя игра не завершена. Заявления авторов о замысле относительно судьбы ранних механик не найдено; есть только общие формулировки README [C351], [C352].

**Q4. Ванильная еда, рецепты, мобы.** TFC убирает ванильный рецепт файлом с тем же id и условием `forge:false` [C401]; таких файлов 230, и в серверном конфиге нет ключа со словом recipe [C403], [C406]. Еду TFC делает бесполезной не удалением: объект питания игрока заменён, и предмет без определения еды TFC не даёт эффекта [C416], [C418], [C419], [C420]; часть ванильных рецептов еды при этом не отключена [C404], [C405], а тыквенный пирог — настоящая еда TFC [C422]. TFG добавляет жёсткий слой: мод Reliable Remover со списками по одному JSON на мод [C407], [C410], куда входят 36 из 44 проверенных ванильных предметов еды [C425]; второй список в KubeJS — для предметов, которые должны остаться зарегистрированными: у них удаляются все рецепты и они прячутся из просмотрщика рецептов [C411], [C412], [C413]; лут ванильных животных обнуляется скриптом [C427]. В глобальном конфиге Reliable Remover вычистка из инвентарей, торговли и творческих вкладок включена, а из сундуков с лутом, выпавших предметов и хранилищ — выключена [C409]. Мобы: в JSON биома plains у TFC в списке существ только сущности `tfc:` [C431]; поверхностный спавн монстров отменяется слушателем `FinalizeSpawn` по тегу `tfc:vanilla_monsters` и двум опциям конфига [C432], [C433], [C434], [C435], только в мирах с генератором TFC и только для естественного спавна [C437]; мейнтейнер TFC подтверждает, что это намеренно [C442]. TFG оставляет эти опции как есть [C443], добавляет в тег enderman, husk, drowned, slime и вариант из Enderman Overhaul [C444], добавляет похожий слушатель для других измерений в Java [C446], гасит спавн-модификаторы добавленных модов файлами `forge:none` / `remove_spawns` [C447], [C448] и перечисляет мобов в собственном JSON биома (прочитан earth/plains) [C445], [C449]; в прочитанном скрипте событие KubeJS `EntityEvents.spawned` используется для перебалансировки характеристик [C450], отдельного мода контроля спавна по поиску имён не найдено [C451].

**Что взять для farming-evolution (рекомендации; выводы автора отчёта из перечисленных утверждений):**

1. Разделить слои так же: рецепты, теги, лут, простая регистрация, JSON генерации мира, ассеты и тексты — в папке `kubejs` сборки; поведение (классы блоков и сущностей, типы рецептов, миксины, код генерации) — в Java-ядре селекции, которое отдаёт скриптам свои билдеры и схемы рецептов через плагин KubeJS — from [C102], [C103], [C123], [C139].
2. Не рассчитывать на «генерацию TFC без TFC»: выбрать одно из трёх — зависеть от TFC целиком и гасить лишнее, написать свою модель климата, либо форкнуть код под копилефтом — from [C236], [C251]. Если зависеть от TFC, климатическую привязку растений и животных делать его же механизмами (`tfc:climate`, fauna JSON), учитывая, что новые climate ranges и fauna датапаком не добавляются — from [C218], [C220], [C223].
3. Климатический уникум закладывать как растение или животное, а не как руду: руды в TFC к климату не привязаны — from [C229].
4. Сцеплять слои через одну общую систему материалов и переопределение рецептов, а не параллельные наборы предметов — from [C303], [C306], [C318]. Схему «ранняя металлургия гейтит вход и затем уходит» не копировать; для взаимной зависимости брать приём с биологической стороны TFG — механика остаётся включённой, получает машины, поздние рецепты требуют её продукцию — и закладывать такие обязательные входы намеренно (в TFG в этом прогоне их найдено четыре) — from [C349], [C350].
5. Запрет ванильной еды строить в два слоя: еда без определения в своей системе питания не работает (в Java-ядре), плюс явное удаление предметов и лута конфигом и скриптом; отдельно решить, вычищать ли сундуки с лутом (в конфиге TFG эта опция выключена) — from [C409], [C420], [C425], [C427].
6. Мобов контролировать тегом и слушателем спавна в Java-ядре плюс собственными списками спавна в JSON биомов; спавн чужих модов гасить переопределением их biome modifier — from [C435], [C447], [C448], [C449].

## Findings

### Q1 — how work is divided between KubeJS, datapacks and the Java mod, and why — answered with a gap

- **C102** [quoted] Stated rule: custom GregTech machines and multiblocks belong in the Java core mod, not in KubeJS — "Custom GT machines/multiblocks should be submitted in our [Core Mod](https://github.com/TerraFirmaGreg-Team/Core-Modern) instead of done through KubeJS." — [S1], Quality Guidelines, b. Organization
- **C103** [quoted] Stated rule: recipes, basic items and blocks, materials, data, assets and loot go through KubeJS rather than the core mod, with the qualifier "when most convenient" — "Recipes, basic items/blocks, materials, data, assets, loot, etc. Should be submitted through [Kubejs](https://github.com/TerraFirmaGreg-Team/Modpack-Modern/tree/dev/kubejs) instead of in our core mod when most convenient." — [S1], Quality Guidelines, b. Organization
- **C104** [quoted] The guide describes the modpack side as handled by KubeJS — "The modpack side of TerraFirmaGreg is typically handled by KubeJS." — [S1], Modpack Development, 3. Feature Creation, Step 2: Navigating the Repository
- **C105** [quoted] Core-Modern's README defines the Java mod as the integration layer between TerraFirmaCraft and GregTech Modern plus custom features — "Developed in Java, this core mod is designed to bridge **TerraFirmaCraft** and **GregTech Modern**, delivering seamless compatibility and new custom features that enrich the overall gameplay experience." — [S2], section "The Core of TerraFirmaGreg-Modern"
- **C107** [measured] Modpack-Modern (dev) holds 19,004 tree entries; by blob count the top-level split is kubejs 16,183, config 569, .pakku 87, defaultconfigs 61, .github 60, tacz 6. The repo contains no Java source and no mods folder — Own measurements, M1
- **C108** [measured] Inside kubejs the static packs dwarf the scripts by file count: assets (resource pack) 12,770 files, data (datapack) 2,917, server_scripts 353, startup_scripts 130, client_scripts 4 — Own measurements, M2
- **C109** [measured] JavaScript volume: server_scripts 2,486,469 bytes, startup_scripts 547,044 bytes, client_scripts 56,038 bytes (about 3.09 MB of .js in ~487 script files) — Own measurements, M3
- **C110** [measured] Scripts are foldered per mod: server_scripts has 96 top-level entries and startup_scripts 31; the `tfg` folder is the largest in both (156 of 353 server files, 85 of 130 startup files), the rest are small per-mod folders (gregtech 8, tfc 8, tacz 8, minecraft 6, greate 5, ...) — Own measurements, M4
- **C112** [quoted] One startup entry script wires the item, block, fluid, sound and GTCEu registries and the block/item modifications, including GT recipe types and GT machines registered from KubeJS — "GTCEuStartupEvents.registry('gtceu:machine', event => {" — [S3], lines ~17-60 (registry and modification handlers)
- **C113** [measured] Despite rule C102 some GT machines are still defined in KubeJS: `kubejs/startup_scripts/gtceu/machines.js` is 54,091 bytes with 9 `event.create(` calls, next to `gtceu/recipe_types.js` (2,738 bytes) and `tfg/nuclear/machine.js` (325 bytes) — Own measurements, M5
- **C114** [measured] The datapack layer is mostly worldgen and structures: of 2,081 files in kubejs/data/tfg, configured_feature 599, placed_feature 597, template_pool 86, processor_list 57, density_function 54, biome 45, plus structure NBT folders (mineshaft 94, trees 57, moon 37, ...) and block loot tables 91 — Own measurements, M6
- **C117** [measured] pakku-lock.json pins 279 projects (268 mods, 9 resource packs, 2 shaders) on Forge 47.4.13; it includes the team's own TerraFirmaGreg-Core, Jellies, Splice and TerraFirmaGreg-Shaders, the script stack KubeJS, KubeJS TFC, KubeJS Create, Vintage KubeJS, LootJS and ProbeJS, and no project with slug terrafirmacraft — Own measurements, M8
- **C118** [measured] Quests are plain FTB Quests SNBT under config (17 chapter files, 1.9-85 KB each) with their text in kubejs/assets/ftbquests/lang (16 locale files); they are neither script nor Java — Own measurements, M9
- **C119** [measured] Core-Modern (dev) has 2,565 .java files, 11,980,221 bytes; 1,467 files (6.48 MB) are a vendored TerraFirmaCraft under `net/dries007/tfc`, 1,070 files (5.34 MB) are the team's `su/terrafirmagreg/core`, 23 are `tfcambiental`, 4 are `tfc_textile` — Own measurements, M10
- **C120** [measured] Subsystems of the team's own package by file count and bytes: common 391 (2.94 MB; block 73, tfgt 64, entity 63, data 56, environment 23, particle 22, command 12), mixins 376 (0.79 MB), world 138 (0.68 MB; new_ow_wg 70, feature 37), compat 71 (0.45 MB), client 54, network 18, utils 13, config 7 — Own measurements, M11
- **C121** [measured] Over a third of the team's Java files are mixins patching other mods: 374 mixin classes across 49 target folders, led by gtceu 54, minecraft 53, create 46, tfc 45, firmalife 20, ad_astra 19, species 17 — Own measurements, M12
- **C123** [quoted] Core registers a KubeJS plugin; the plugin's package holds classes named as block builders (plants, particle emitters, GT active blocks), recipe schemas (item repair, gas burner fuel, TFG recipe bindings) and TFG startup/server events (file names only; the code was not read) — "su.terrafirmagreg.core.compat.kjs.TFGKubeJSPlugin" — [S4], whole file; class list in src/main/java/su/terrafirmagreg/core/compat/kjs/ (19 files)
- **C125** [quoted] Core's mods.toml declares four mods in one jar: the TFC fork under the original mod id `tfc`, `tfcambiental`, `tfc_textile` and the core mod itself — "description = '''TerraFirmaCraft fork for TerraFirmaGreg.'''" — [S5], line 16 (modId "tfc" at line 11; "tfcambiental" line 44; "tfc_textile" line 83)
- **C126** [measured] Core also carries static resources and Java datagen output: src/main/resources has assets/tfc 21,937 and data/tfc 17,002 files (from the TFC fork), assets/tfg 1,767 and data/tfg 687; src/generated/resources has assets/tfg 2,768 and data/tfg 664 — Own measurements, M14
- **C128** [quoted] Reason given for forking TFC Ambiental into Core: optimization and the ability to extend it — "We've forked this mod into TFG-Core, so we can better optimize it and extend it @ashleney" — [S6], [0.13.0] - 21-06-2026, "TFC Ambiental" (line 879); code: Core-Modern PR #496
- **C129** [quoted] Reason given for forking TFC Textile into Core: control — "Forked TFC Textile into TFG Core, so we have more control over it @ashleney" — [S6], [0.13.5] - 25-07-2026, "Ambiental" (line 640); code: Core-Modern PR #536
- **C130** [quoted] Reason given in the changelog for forking TerraFirmaCraft itself: long-term stability of pack development; the fuller explanation is linked to Reddit and was not read — "Has been forked! This shouldn't affect you at all, but it ensures long-term stability for the future development of the modpack." — [S6], [0.13.11] - 05-10-2026, Mods, "TerraFirmaCraft" (line 337)
- **C131** [measured] The TFC fork entered Core in one PR, "Tfc fork" (#580, by Pyritie, created 2026-09-12): 896,858 additions, 3,311 deletions, 40,974 changed files, empty description, no comments — Own measurements, M15
- **C132** [quoted] Concrete KubeJS-to-Java move with its reason: the pale moss carpet was added to Core so that it can be registered as a CarpetBlock and stop surviving when the block under it is broken — "This PR adds the pale moss carpet to Core so that it can be registered as a CarpetBlock. This prevents the pale moss carpet from surviving when the block beneath it is broken." — [S7], PR body, "What is the new behavior?"; companion Modpack-Modern PR #4438 removes the KubeJS registration
- **C133** [quoted] For that bug the maintainer offered two routes: move the block into Core, or keep it in KubeJS and extend the Java-side KubeJS builder — "remove the [pale moss carpet](https://github.com/TerraFirmaGreg-Team/Modpack-Modern/blob/dev/kubejs/startup_scripts/tfg/beneath/blocks.beneath.js#L48) from the modpack and put it into core, and either register it as a type of carpet (or however a normal moss carpet is implemented) or copy how we do sand piles" — [S8], comment by Pyritie, 2026-06-25
- **C134** [quoted] Another move to Java: fishing nets are now handled in Core (no reason stated); the same PR adds colour caching to dynamic colour handling to fix performance issues — "Dynamic color handling now caches the color to fix the performance issues." — [S9], PR body by Redeix, 2026-05-15 (same body: "Fishing nets are now handled in core."); companion Modpack-Modern PR #4034
- **C137** [quoted] Counter-direction: an ore processing file was moved out of the Java mod into KubeJS, with the stated outcome being fewer files in the core mod — "Fewer files in TFG-Core" — [S10], PR body, "Outcome" (title "Moved ore processing file out of TFG-Core and into KJS", 2025-04-17)
- **C139** [inference] The practised split matches the stated one with exceptions on both sides: recipes, tags, loot, simple registration, worldgen JSON, assets and lang sit in the pack's kubejs folder; block/entity classes with behaviour, machines, mixins into other mods, worldgen code, recipe types and KubeJS bindings sit in Java; the crossings seen are a script-registered block that needed a real block class and fishing nets (to Java) and an ore processing file (to KubeJS) — from [C102], [C103], [C108], [C113], [C120], [C121], [C123], [C132], [C134], [C137]
- **C140** [measured] The org has 25 repos; only one is a GitHub fork (AutoPack-Director, "Runtime mod&file processing"). Forks of gameplay mods are not separate repos: TFC, TFC Ambiental and TFC Textile are vendored inside Core-Modern (C119, C125). The pack has 11 files under config/mod-director — Own measurements, M16
- **C141** [quoted] Tools-Modern is a tools repo (mostly C#) for automated and centralized pack files such as translations and field guide pages — "Developed mostly in C#, this tools repo is designed to centrilized translations, field guide pages and more." — [S11], "Tools for TerraFirmaGreg-Modern"; root dirs incl. LanguageMerger, LanguageSync, OresToFieldGuide, PakkuLockChecker, PatchouliToEmi, EMICategories
- **C142** [measured] QuestBook-Modern does not hold the quests: it contains only a README title, `language.json` and a `ci/` export pipeline (build-json.mjs, run.sh, export-*.toml, share/site-meta scripts); quest data stays in Modpack-Modern (C118) — Own measurements, M17
- **C144** [measured] Modpack-Template and Mod-Template are skeletons with title-only READMEs: the first has .pakku, config, pakku.json, pakku-lock.json, pakku.jar, CHANGELOG.md; the second a Gradle project with a `modules` dir — Own measurements, M18
- **C145** [quoted] Splice is the team's own client-side mod that merges split lang folders; its README names Modpack-Modern's kubejs/assets/tfg/lang as an authoring location — "Client-side mod that merges split lang folders at reload." — [S12], first paragraph; "Authoring" section names `kubejs/assets/tfg/lang/`

### Q2 — TFC world generation and climate, ties to biology and ores, standalone use, licence — answered with a gap

- **C201** [quoted] TFC 1.21.x exists and targets Minecraft 1.21.1 on NeoForge; 1.20.x targets 1.20.1 — "val minecraftVersion: String = "1.21.1"" — [S13], lines 11-12 (neoForgeVersion 21.1.234); 1.20.x build.gradle.kts line 27 has "1.20.1"
- **C203** [quoted] Base climate is a triangle wave along one axis each: rainfall along X, temperature along Z; a scale of 0 gives a constant instead (same code in 1.20.x and 1.21.x) — "return scale == 0 ? (x, z) -> constant : axisIsX ? (x, z) -> triangle(frequency, x) : (x, z) -> triangle(frequency, z);" — [S14], baseNoise(), lines 46-53 (quote joins 4 source lines); temperatureNoise = baseNoise(false, ...) line 95, rainfallNoise = baseNoise(true, ...) line 105
- **C204** [quoted] Sea-level average temperature spans -20..30 C from the wave plus +-3 C simplex noise; rainfall spans 0..500 mm plus -80..+40 noise (identical constants in 1.20.x lines 76-88) — "this.temperatureNoise = baseNoise(false, settings.temperatureScale(), settings.temperatureConstant()) .scaled(-20f, 30f)" — [S14], lines 95-110; rainfall: ".scaled(0f, 500f)" then ".scaled(-80f, 40f)"
- **C205** [quoted] The scale is a world-preset setting, documented as distance between extremes in blocks — "`temperature_scale` (Integer): The distance between two temperature extremes, in blocks. If zero, the temperature will be a constant based on `temperature_constant`" — [S15], tfc_settings list; same sentence in 1.21.x/worldgen/world-preset.md line 21; rainfall_scale worded the same
- **C206** [quoted] Default scale is 20000 blocks for both axes in the shipped preset (extreme to extreme 20 km, full period 40 km; Settings.java javadoc: "Default 20km") — ""temperature_scale": 20000" — [S16], tfc_settings; also "rainfall_scale": 20000, "spawn_distance": 4000, "finite_continents": false; 1.20.x file (src/main/resources/...) has the same two scales
- **C208** [quoted] Climate scale is a field of the TFC world generator settings codec, i.e. of the world preset JSON — "Codec.INT.fieldOf("temperature_scale").forGetter(c -> c.temperatureScale)," — [S17], CODEC, lines 36-49
- **C209** [quoted] Coasts modify the base climate: near ocean the temperature is pulled toward 5 C and rainfall raised by up to 350 mm (present in both versions) — "final float biasTargetRainfall = Mth.lerp(bias, Math.min(point.rainfall + 350f, 500f), point.rainfall);" — [S18], lines 35-52; "final float biasTargetTemperature = Mth.lerp(bias, 5f, point.temperature);"
- **C210** [quoted] Altitude: above sea level (y 63) average temperature drops 0.16225 C per block, capped at 17.822 C (same constants in 1.20.x OverworldClimateModel line 89) — "return averageTemperature - Mth.clamp((y - seaLevel) * 0.16225f, 0, 17.822f);" — [S19], adjustAverageTemperatureByElevation, line 1704; SEA_LEVEL_Y = 63 (TFCChunkGenerator line 118); below y 0 temperature tends to LAVA_LEVEL_TEMPERATURE = 15f; daily swing within +-4 C
- **C212** [quoted] 1.20.x seasons: the monthly swing amplitude depends on Z between 12 and 18 C and never changes sign, so there are no hemispheres — "return monthTemperatureModifier * (temperatureScale == 0 ? 0 : Helpers.triangle(-3f, 15f, 1f / (2f * temperatureScale), z));" — [S20], calculateMonthlyTemperature, line 410
- **C213** [quoted] 1.21.x seasons: the swing runs from -18 to +18 with midpoint 0 and period 4x temperatureScale, so its sign flips with Z = southern hemispheres with inverted seasons — "return monthTemperatureModifier * (temperatureScale == 0 ? 0 : Helpers.triangle(-18f, 0f, 1f / (4f * temperatureScale), z - temperatureScale / 2));" — [S21], calculateMonthlyTemperature, line 435; javadoc: "The hemispheral impact on seasonal temperatures is calculated here as well"
- **C214** [quoted] 1.21.x adds a per-position rainfall variance in -1..1 set by distance from the west coast plus noise; 1.20.x AnnotateClimate has no such field — "point.rainfallVariance = Mth.clampedMap(point.distanceToWestCoast + (float) context.generator().rainfallVarianceNoise.noise(x, z), 0f, 80f, -1, 1);" — [S18], line 48; 1.21.x also adds groundwater (OverworldClimateModel lines 173-193) and a finite_continents preset option
- **C215** [quoted] In 1.21.x that variance makes rainfall seasonal (wet summer or wet winter) — "For positive values of variance, drought in January/rain in July, for negative values drought in summer, the opposite" — [S21], getInstantRainfall javadoc, lines 159-171
- **C218** [quoted] Climate range JSON (hydration and temperature min/max) gives the range of values a block can grow in; climate ranges can be modified by datapack but not added — "**Note:** Climate ranges cannot be added through datapacks, only modified." — [S22], "## Climate Ranges", lines 35-48; same note in 1.21.x/custom.md line 38
- **C220** [quoted] Worldgen placement by climate is the tfc:climate decorator (placement modifier) on a placed feature: min/max temperature, rainfall, forest, fuzzy — "This decorator will conditionally place a feature based on specific climate properties. All config values are optional, and if omitted will allow all positions." — [S23], "### Climate", lines 46-60; "The typical range for yearly average temperatures is -25 to +30 C. Rainfall values can vary between 0 mm and 500 mm."
- **C221** [quoted] Example: wild rice patches are a placed feature gated by tfc:climate 15..30 C and 100..500 mm — ""type": "tfc:climate"," — [S24], placement[2]: min_temperature 15, max_temperature 30, min_rainfall 100, max_rainfall 500, min_forest normal
- **C222** [quoted] 1.21.x tfc:climate uses groundwater and rain variance fields instead of the 1.20 min/max rainfall, and forest as integer 0..4 — "`min_groundwater`: An optional float representing the minimum allowed groundwater level." — [S25], "### Climate", lines 40-56; no min_rainfall line in that list
- **C223** [quoted] Animal spawning uses fauna JSON per entity whose climate field is the same climate decorator config; fauna can be modified but not added by datapack — "`climate`: A [Climate Decorator](../worldgen/decorators/#climate) configuration for the climate requirements of the fauna." — [S22], "## Fauna", lines 132-144; "**Note:** Fauna cannot be added with datapacks, only modified."
- **C225** [quoted] Ore veins are configured features (tfc:cluster_vein, tfc:disc_vein, tfc:pipe_vein) listed in the placed feature tag #tfc:in_biome/veins — "To add a new vein, simply add it to this tag and TFC will generate it in the world." — [S26], intro
- **C226** [quoted] A vein is tied to rock type only through its blocks replacement map: each listed raw rock is replaced by an ore block; rocks not listed get no ore — ""replace": [ "tfc:rock/raw/granite" ]," — [S27], config.blocks[0] (quote joins 3 lines); with: tfc:ore/poor_cassiterite/granite weight 35 etc.
- **C227** [quoted] Veins have an optional biome tag filter but no climate field; climate gating of ore is not a documented vein option — "`biomes`: An optional biome tag. If used, the vein will be restricted to the given biomes." — [S26], common options list
- **C228** [quoted] Rock layers are set in the preset: a layer type per area (ocean_floor, volcanic, uplift, land) selects layers, each a map of rock IDs to layer IDs — "First, TFC picks the overall **Layer Type** of the area. This may be one of `ocean_floor` (Oceans), `volcanic` (Volcanic islands, and volcanic mountains), `uplift` (Inland mountains), or `land` (All other non-ocean areas)." — [S15], "### Rock Layer Settings"; rocks may be custom block sets
- **C229** [inference] Geology follows tectonic layer type, not climate; biology (crops, wild plants, fauna) follows climate; so "material only in one climate" is native for plants and animals and needs a workaround for ores, whose only documented placement filter is a biome tag — from [C220], [C223], [C227], [C228]
- **C230** [quoted] TFC worldgen is a world preset (tfc:overworld) that can be selected on client and server — "TFC adds the `tfc:overworld` [World Preset](https://minecraft.wiki/w/World_preset_definition) which generates TFC terrain, and can be selected both in client and on servers." — [S28], intro
- **C231** [quoted] A common-config option sets which preset is the default for new worlds — "If the TFC world preset 'tfc:overworld' should be set as the default world generation when creating a new world." — [S29], lines 36-38, define("defaultWorldPreset", "tfc:overworld")
- **C232** [quoted] TFC's own handler of SelectClimateModelEvent sets the coordinate-based OverworldClimateModel only when the overworld runs a TFC-enabled chunk generator — "// TFC decides to select the climate model for the overworld, if we're using a TFC enabled chunk generator" — [S30], onSelectClimateModel, lines 1400-1408; 1.20.x lines 1552-1560
- **C233** [quoted] Without the TFC generator the default model reads climate from the vanilla biome (rainfall is 300 or 0) — "A default climate model, for dimensions that are entirely biome determined (i.e. vanilla)." — [S31], class javadoc; "getPrecipitationAt(pos) != Biome.Precipitation.NONE ? 300f : 0f"
- **C234** [quoted] TFC describes itself as a total conversion mod; the same README paragraph covers both survival and world generation — "TerraFirmaCraft (TFC) is a total conversion mod that overhauls and reinvents survival mode." — [S32], "### About"
- **C235** [measured] TFC 1.20.x ServerConfig has 169 config keys; the `enable*` keys are per-mechanic toggles (enableThirstOverheating, enableOverburdening, enableVanillaMonsters and others); no key name matches worldgen, survival or disableAll, so the config has no single switch that turns the survival mechanics off as a whole — Own measurements, M23
- **C236** [inference] No standalone or worldgen-only build was found: TFC worldgen cannot run without the TFC mod installed; options are (1) depend on TFC and tune or disable mechanics by config, datapack and KubeJS, (2) supply an own ClimateModel through SelectClimateModelEvent, the hook TFC itself uses (use by another mod not tested; it replaces the climate model, not the terrain), (3) fork the worldgen code under EUPL — from [C230], [C231], [C232], [C234], [C235]
- **C237** [quoted] The one third-party worldgen project cited (TFC-Real-World) is an addon that mixins into TFC, not a replacement for it — "**Non-Intrusive:** No new blocks, items, or mobs. Uses Mixins to feed TFC's own world generation with map data." — [S33], line 43; line 24: "**It is still TFC.**"
- **C239** [quoted] The forked files keep the EUPL header while the Core-Modern repo licence is LGPL-3.0 (a licence listed as compatible in the EUPL appendix) — " * Licensed under the EUPL, Version 1.2." — [S34], header lines 1-5; diff vs TFC 1.20.x file: only a lombok @Getter added; repo LICENSE is LGPL v3
- **C240** [quoted] Core-Modern (MC 1.20.1) carries a mixin that applies the 1.21 hemisphere formula to the 1.20 climate model only when `TfgClientPreviewState.useTfgOverworldPipeline()` is true, as part of its own "new_ow_wg" overworld pipeline (mixins on RegionGenerator, TFCChunkGenerator, TFCBiomes, TFCLayers) — "cir.setReturnValue(monthTemperatureModifier * (temperatureScale == 0 ? 0 : Helpers.triangle(-18f, 0f, 1f / (4f * temperatureScale), z - temperatureScale / 2)));" — [S35], tfg$calculateMonthlyTemperature; gradle.properties: "minecraft_version                   = 1.20.1"
- **C241** [quoted] TFG defines its own veins as datapack JSON using TFC's vein feature types, replacing TFC rock with GregTech ore blocks — ""block": "gtceu:granite_apatite_ore"," — [S36], "type": "tfc:cluster_vein", replace "tfc:rock/raw/granite"; "__credits__": "This vein was automatically generated by OresToFieldGuide."
- **C242** [measured] TFG adds its own dimensions and worldgen data: kubejs/data/tfg/worldgen has 599 configured and 597 placed features grouped earth, nether, mars, moon, venus (76 configured features under earth/vein); dimension_type files glacio, mars, mercury, moon, the_nether, venus; 19 files override tfc worldgen data and 5 files are firmalife climate ranges — Own measurements, M27
- **C244** [quoted] TFC is under EUPL 1.2 unless a file says otherwise; there is one LICENSE.txt, no separate asset licence file — "Licensed under the [EUPL, Version 1.2](LICENSE.txt) unless otherwise noted." — [S32], "### Legal"; root has only LICENSE.txt and HEADER.txt; listed exceptions: "Sounds are used under [CC0](https://creativecommons.org/publicdomain/zero/1.0/) unless otherwise noted.", FastNoiseLite.java MIT, star catalogue CC-BY 4.0, one sound CC-BY-4.0
- **C246** [quoted] Part of the code derives from TFC 1.7.10 under a different licence, with permission from the original author; the README does not state its terms — "Parts of this project are edited source code from the original TerraFirmaCraft for 1.7.10 mod. They are used under a different license with permission from the original author (Bioxx)." — [S32], "### Acknowledgments"; music credited with no licence stated
- **C247** [quoted] EUPL grants use, modification, derivative works and distribution, royalty-free — "— modify the Work, and make Derivative Works based upon the Work," — [S37], Article 2
- **C248** [quoted] Copyleft: distributing a derivative (a fork) must be under EUPL (or a later version) — "Copyleft clause: If the Licensee distributes or communicates copies of the Original Works or Derivative Works, this Distribution or Communication will be done under the terms of this Licence or of a later version of this Licence" — [S37], Article 5, lines 103-110
- **C249** [quoted] A derivative combined with work under a listed compatible licence may go out under that licence; the list includes GPL 2/3, AGPL 3, MPL 2, LGPL 2.1/3, EPL 1.0 — "— GNU Lesser General Public Licence (LGPL) v. 2.1, v. 3" — [S37], Appendix; Article 5 "Compatibility clause"
- **C250** [quoted] A fork must keep notices, ship the licence, mark modifications with date, and provide source — "The Licensee must cause any Derivative Work to carry prominent notices stating that the Work has been modified and the date of modification." — [S37], Article 5 "Attribution right"; "Provision of Source Code" lines 121-124
- **C251** [inference] For a public pack: bundling the unmodified TFC jar as a dependency is distribution of the Work (keep licence and notices); forking worldgen code into an own mod is allowed but that mod's derived part must be EUPL, or a listed compatible licence when it is combined with a work under that licence, with source published; MIT or closed is not an option for the derived part. Whether an addon that only calls TFC's API is a Derivative Work is left by the licence to applicable copyright law — from [C247], [C248], [C249], [C250]

### Q3 — how TFC and GregTech are coupled; early gates and the fate of early mechanics — answered with a gap

- **C301** [quoted] The pack's README presents the two mods as tightly integrated, with TFC and GregTech Modern as the core — "The heart of the modpack is a tight integration between the two namesake mods:" — [S38], intro, before mod list
- **C302** [quoted] A separate Java core mod exists whose stated purpose is to bridge TFC and GregTech Modern — "this core mod is designed to bridge **TerraFirmaCraft** and **GregTech Modern**, delivering seamless compatibility and new custom features" — [S39], section "The Core of TerraFirmaGreg-Modern"
- **C303** [quoted] Unification runs on GregTech's material system: a custom TFC property (forging, welding and melting temperature plus anvil tier) is attached to GT materials in a startup script — "// TFC_PROPERTY = (forging temp, welding temp, melt temp, material, tier, percent of material)" — [S40], line 9
- **C304** [quoted] GT Black Steel is registered as a TFC-workable metal of tier 5 — "GTMaterials.BlackSteel.setProperty(TFGPropertyKey.TFC_PROPERTY, new $TFC_PROPERTY(891, 1188, 1485, 5));" — [S40], line 21
- **C305** [quoted] GT ore materials get the same property pointing at the metal they melt into (Hematite to Iron, tier 3, 90 percent) — "GTMaterials.Hematite.setProperty(TFGPropertyKey.TFC_PROPERTY, new $TFC_PROPERTY(921, 1228, 1535, GTMaterials.Iron, 3, 90));" — [S40], line 37
- **C306** [quoted] TFC anvil recipes are generated by script per GT material (helper used inside a material loop gated on the TFC property) — "event.recipes.tfc.anvil(outputItem, inputItem, steps)" — [S41], line 59; property check at lines 89 and 379
- **C307** [quoted] The TFC bloomery recipe is replaced by one that consumes GregTech's iron fluid — "event.recipes.tfc.bloomery('tfc:raw_iron_bloom', 'minecraft:charcoal', Fluid.of('gtceu:iron', 144), 15000)" — [S42], line 298, id tfc:bloomery/raw_iron_bloom
- **C308** [quoted] TFC alloy ratios were changed to GregTech's — "Changed the TFC ratios of bronze and brass to match GregTech's (3:1 instead of 9:1)" — [S43], line 43
- **C309** [quoted] GregTech's own primitive blast furnace is listed in the pack's item remover config for gtceu — "gtceu:primitive_blast_furnace" — [S44], line 52
- **C310** [quoted] The quest book makes the TFC bloomery the required route to wrought iron — "The Bloomery is a Multiblock from TerraFirmaCraft that allows you to create Wrought Iron." — [S45], quests.metal_age.bloomery.desc; subtitle "Required if you seek the Iron Age."
- **C311** [quoted] Steel comes from the TFC blast furnace via pig iron — "inside the Blast Furnace produces Pig Iron, which can be worked into Steel." — [S45], quests.metal_age.blast_furnace.desc
- **C312** [quoted] The Metallurgy Age chapter is ordered by TFC anvil tiers (T2 bronze to T6 red or blue steel); the T5 black steel anvil is the point where the high pressure steam section opens — "You've also now opened up a massive new section in the Early Automation chapter related to high pressure Steam Power!" — [S45], quests.metal_age.black_steel_anvil.desc; titles "T5 Anvil: Black Steel", "T6 Anvil: Red or Blue Steel"
- **C313** [quoted] The GregTech high-pressure solid boiler recipe is overridden to need a TFC crucible — "D: 'tfc:crucible'," — [S46], line 295, recipe gtceu:shaped/steam_boiler_coal_steel
- **C314** [quoted] The same boiler recipe needs a Black Steel plate, and seven more HP steam recipes (liquid and solar boilers, macerator, compressor, forge hammer, furnace, alloy smelter) need Black Steel plates, rods or ingots; the HP rock crusher and miner recipes in the same file do not — "E: ChemicalHelper.get(TagPrefix.plate, GTMaterials.BlackSteel, 1)," — [S46], lines 296, 310, 322, 338, 354, 367, 381, 394
- **C315** [quoted] The LV electric forge hammer recipe consumes a TFC steel anvil (the MV one a black steel anvil) — "E: 'tfc:metal/anvil/steel'," — [S46], line 145; black steel anvil at line 157
- **C316** [quoted] The first Electric Blast Furnace (LV) still needs TFC's top-tier alloy: its coils require Blue Steel — "Prepare &a128 Cupronickel&r, &a32 Blue Steel&r and &a16 Tin Alloy&r ingots" — [S47], quests.low_voltage.cupronickel_coil.desc
- **C317** [quoted] The Steam Age chapter has a quest for the LV circuit whose text expects its inputs to be automatable after that age — "if you've spent an appropriate amount of time in the Steam Age, everything should be easily automatable" — [S48], quests.steam_age.lv_circuit.desc
- **C318** [inference] Entering high pressure steam and LV requires the whole TFC metal chain: bloomery wrought iron, blast-furnace steel, black steel for the HP boilers and five other HP steam machines, blue steel for the first EBF; implementation is recipe override plus GT-material unification, not parallel item sets — from [C303], [C306], [C309], [C310], [C311], [C313], [C314], [C316]
- **C319** [quoted] Chapter order by file order_index: Stone Age, Metallurgy Age, Steam Age, then LV, MV, HV, then EV, IV; the HV subtitle points to space — "Upgrade your power, learn about the Cleanroom, make more chemicals, and get ready for Space" — [S49], key quests.high_voltage.subtitle; order_index in config/ftbquests/quests/chapters/*.snbt
- **C320** [quoted] EV is the Mars and fission tier — "Reach Mars, discover Nuclear Fission, and build new massive multiblocks" — [S50], key quests.extreme_voltage.subtitle
- **C321** [quoted] IV (Venus) content is not finished: the IV chapter is a placeholder for a future rework — "will be the primary planet for the upcoming &1IV&r rework in TFG 0.14. Stay tuned!" — [S51], quests.insane_voltage.venus.desc
- **C322** [quoted] The Beneath is positioned as a mid-early dimension spanning wrought iron age to LV/MV, with its own crops — "with the different layers providing challenges for different stages of the game, from wrought iron age to LV/MV" — [S52], line 14
- **C323** [quoted] In the Steam Age a new multiblock, the Steam Bloomery, automates the TFC bloomery — "is a new multiblock that'll automate your old TFC bloomery, and do it faster too!" — [S48], quests.steam_age.steam_bloomery.desc.1
- **C324** [quoted] In the Steam Age the quest book advises automating the TFC blast furnace (Mechanical Pump into a Mold Table) rather than replacing it — "so automating your blast furnace is definitely a worthwhile investment" — [S48], quests.steam_age.mold_table.desc.1; subtitle "The Electric Blast Furnace is a long way off"
- **C325** [quoted] GT forge hammers get recipes for the TFC anvil steps for blooms and pig iron — "event.recipes.gtceu.forge_hammer('tfg/high_carbon_steel')" — [S42], lines 64-88 (raw bloom, refined bloom, pig iron, high carbon steel)
- **C326** [quoted] At LV the quest book tells the player to dismantle the TFC blast furnace — "It's almost time to tear down your old TFC blast furnace setup!" — [S47], quests.low_voltage.refractory_brick.desc
- **C327** [quoted] Tool heads, anvil work in TFC, get an LV extruder recipe — "event.recipes.gtceu.extruder(`tfg:extrude_${materialName}_ingot_to_${tagPrefixName}`)" — [S53], lines 126-131, EUt LV
- **C328** [quoted] Food is not dropped at LV: custom GT machines are built around TFC's food system — "The Food Processor and Electric Oven are two custom machines designed specifically to work with TerraFirmaCraft's food system." — [S47], quests.low_voltage.food_machines.desc.1
- **C329** [quoted] Food decay keeps acting inside automation; a cover exists to void rotten food — "The Rotten Voiding Cover can be placed on any machine or Input Bus to automatically void any food items that become rotten." — [S47], quests.low_voltage.food_machines.desc.1
- **C330** [quoted] Maintainers closed a hole where machines bypassed decay — "so you can't turn rotten grains into fresh bread any more" — [S43], line 24
- **C331** [quoted] The shipped TFC server config leaves food decay at the default rate — "foodDecayModifier = 1.0" — [S54], line 753
- **C332** [quoted] Crops get a GT machine at LV, the Electric Greenhouse, offered as an alternative to Create farms — "can be your source of &aLogs&r and other plants if you prefer making GregTech machines over Create contraptions" — [S47], quests.low_voltage.lv_greenhouse.desc.1
- **C333** [quoted] The Electric Greenhouse was deliberately slowed to TFC growth speed — "It's significantly slower than default to keep in line with TFC's slower tree growing speeds" — [S47], quests.low_voltage.lv_greenhouse.desc.3
- **C334** [quoted] Greenhouse recipes are defined per dimension, including Moon and Mars, with their own fluids and voltage — "{id: 'ad_astra:moon', fluid: 'gtceu:helium_3', fluid_tier2: null, fluid_chance: 2, fertilizer: null, eut: GTValues.VHA[GTValues.MV], oxygenated: null}," — [S55], line 40; Mars at line 41
- **C335** [quoted] Husbandry gets an LV multiblock that milks and shears but leaves breeding and feeding manual — "Speaking of which, this machine will not breed, feed, or cull any of your animals." — [S47], quests.low_voltage.rancher.desc (Pastoral Rancher)
- **C336** [quoted] At HV a fishery multiblock forms a closed loop with the greenhouse — "A circular loop of recipes between the Pisciculture fishery and the &2Electric Greenhouse&r which boosts each others yields" — [S49], quests.high_voltage.pisciculture.desc.1
- **C337** [quoted] Farm output feeds the tech tree as fuel and chemistry feedstock at LV: biodiesel from seed oil and TFC alcohol — "Simply mix your &aSeed Oil&r with any &eAlcohol&r from &6TFC&r." — [S47], quests.low_voltage.lv_biodiesel.desc
- **C338** [quoted] That farm route is optional; the quest book names non-farming fuels for players who dislike farming — "If you really hate farming and prefer adventuring in the &6desert&r or mining massive amounts of &7Oilsands&r" — [S47], quests.low_voltage.lv_naphtha_light.desc
- **C339** [quoted] Any TFC food or seed can be turned into GT biomass in a brewery — ".itemInputs('#tfc:foods')" — [S56], line 15; seeds line 8; MV quest quests.medium_voltage.mv_biomass
- **C340** [quoted] On Mars (EV) animal husbandry yields a required late component: sheared Sniffer or Wraptor wool — "are also a required component of &6Bio-AES Reinforced R-Polyurethane Foam." — [S50], quests.space_survival.mineral_rich_wool.desc
- **C341** [quoted] An IV-voltage space suit fabric recipe takes `tfg:glacian_wool` as an input — "'tfg:glacian_wool'" — [S57], lines 65-74, forming_press better_space_suit_fabric, EUt IV
- **C342** [quoted] The Venus biochemistry line consumes plant oils (EV chemical reactor) — "'#firmalife:oils 1000'," — [S58], line 363, recipe tfg:lauryl_alcohol
- **C343** [quoted] The same line's IV bioreactor step on Venus takes biomass or bio chaff as its organic input — "{ type: 'fluid', id: 'gtceu:biomass', amount: 1000 }" — [S58], line 420; .dimension('ad_astra:venus') line 448
- **C344** [quoted] Mars adds new tamable, farmable fauna, framed as a break from the factory — "some of which is tamable and farmable, and can be used to produce more husbandry products. Perfect for a break from your factory!" — [S59], line 20
- **C345** [quoted] Nutrition is being extended, not retired: 0.13 gives each nutrient a threshold buff — "now each nutrient type has a special buff on the player when a certain threshold is crossed, such as >85% fruit nutrition granting a passive 30% mining speed buff" — [S52], line 31, section "Food rework"
- **C346** [quoted] Late food items were added for the machine era — "Added many new advanced food items including freeze-dried fruit, calorie paste, and meal bags." — [S43], line 27
- **C348** [quoted] An early TFC resource is called back at HV — "Kaolinite will be used again in some upcoming recipes for industrial ceramics and heat shielding." — [S49], quests.high_voltage.kaolinite_powder.desc
- **C349** [inference] TFC metalworking is superseded in steps (steam bloomery and forge hammer in Steam Age, EBF and extruder at LV); it gates entry and is then replaced step by step by machines; that every TFC metalworking step disappears is not shown — from [C323], [C324], [C325], [C326], [C327]
- **C350** [inference] The biological side is not retired: decay and nutrition stay on, each activity gets a GT machine (food processor, greenhouse, rancher, fishery), and Mars adds farmable fauna; the late inputs from it found in this run are Mars wool, glacian wool, and oils and biomass on Venus, and the LV farm fuel route has non-farm alternatives; no complete list was made — from [C328], [C329], [C331], [C332], [C335], [C336], [C338], [C340], [C341], [C342], [C343], [C344], [C345]
- **C351** [quoted] The README states the intended feel: each stage builds on earlier technology — "you're constantly building upon previous technologies in a way that feels seamless and natural" — [S38], intro paragraph 2
- **C352** [quoted] The README sets integration of every mod as a goal, against loosely bundled packs — "Do you want a modpack where every mod is carefully integrated instead of just thrown in?" — [S38], intro paragraph 2
- **C354** [quoted] Quest text recommends a farm and nutritious food as preparation before steel — "Wrought Iron is a fantastic tech level to \"rest\" for a little bit and catch up with the rest of your base." — [S45], quests.metal_age.wrought_iron_goodies.desc; list includes a farm and nutritious food

### Q4 — how vanilla food, recipes and mobs are blocked or replaced — answered with a gap

- **C401** [quoted] TFC removes a vanilla recipe by shipping a same-id file under data/minecraft/recipes whose only content is a never-true Forge condition (forge:false); e.g. minecraft:furnace — "\"type\": \"forge:false\"" — [S60], whole file (conditions array)
- **C402** [quoted] These override files are generated by a helper in TFC's Python data generator that writes a recipe with no type and the forge:false condition — "rm.recipe(name_parts, None, {}, conditions='forge:false')" — [S61], def disable_recipe, line ~1361
- **C403** [measured] TFC 1.20.x ships 230 override files under src/main/resources/data/minecraft/recipes — Own measurements, M21
- **C404** [measured] Among food-related vanilla recipes these have a TFC override file: beetroot_soup, hay_block, melon, melon_seeds, mushroom_stew, pumpkin_pie, rabbit_stew_from_brown_mushroom, rabbit_stew_from_red_mushroom, suspicious_stew, wheat; bread, cake, cookie, golden_apple, golden_carrot, cooked_beef, cooked_porkchop, cooked_chicken, baked_potato and dried_kelp have none — Own measurements, M22
- **C405** [inference] TFC does not disable every vanilla food recipe: bread, cake, cookie, golden apple, golden carrot and the cooked-meat recipes have no override file, while the furnace's own crafting recipe is disabled; what eating does is decided separately by the food capability — from [C401], [C404], [C416]
- **C406** [measured] ServerConfig.java on 1.20.x has no config key whose name contains 'recipe'; the enableVanilla* keys cover bonemeal, weather effects, skeleton horses, mob enchantments and equipment, golems, monsters, monsters on surface, egg throwing and milk — Own measurements, M23
- **C407** [quoted] TFG ships the item-removal mod Reliable Remover (in pakku-lock.json as reliable_remover-3.9.3+1.20.1-forge.jar), described by its author as a JSON-config utility for removing items — "A lightweight utility designed for completely removing items or blocking interactions" — [S62], project body, first paragraph (fetched via api.modrinth.com/v2/project/reliable-remover)
- **C408** [quoted] Reliable Remover advertises automatic removal of tags and recipes of removed items plus loot/trade handling — "Automatic item/block tag and recipe removal." — [S62], "Why Reliable Remover?" list
- **C409** [quoted] TFG's global Reliable Remover config strips removed items from creative tabs, EMI, inventories and trades, but NOT from dropped items, loot chests or storage — "\"removeItemsFromLootChests\": false," — [S63], whole file; also "removeItemsFromCreativeTabs": true, "removeItemsFromEmi": true, "removeItemsFromInventories": true, "removeDroppedItems": false, "removeItemsFromStorage": false, "removeItemsFromTrades": true
- **C410** [quoted] Removal lists are one JSON file per mod under config/reliable_remover/ (minecraft.json, create.json, ae2.json, beneath.json …), each with an action and an item id list — "\"action\": \"remove\"," — [S64], line 2; file is 639 lines
- **C411** [quoted] A second, KubeJS-side list MINECRAFT_HIDED_ITEMS covers vanilla items that must stay registered; for each, every recipe using or producing it is removed — "// Everything in this array is stuff that we don't want, but needs to exist" — [S65], function removeMinecraftRecipes, lines 6-11; next lines: "event.remove({ input: item })" / "event.remove({ output: item })"
- **C412** [quoted] The same list is hidden from recipe viewers by adding the common tag c:hidden_from_recipe_viewers (also applied to all vanilla potions by regex) — "event.add('c:hidden_from_recipe_viewers', item)" — [S66], registerMinecraftItemTags, lines 15-20
- **C413** [quoted] MINECRAFT_HIDED_ITEMS groups its entries by reason: spawn eggs (regex), creative blocks, items unobtainable but used in worldgen (torch, lantern, golden_carrot …), items used for recipe displays (grass_block, furnace), mob heads — "// unobtainable but used in worldgen" — [S67], global.MINECRAFT_HIDED_ITEMS, lines 4-36
- **C414** [quoted] In the tfc folder's recipes.removes.js, recipe removals are by exact id, by regex id, or by mod+type filter — "event.remove({ mod: 'tfc', type: 'tfc:welding' })" — [S68], removeTFCRecipes, lines 6-8; also "event.remove({ id: /tfc:heating\/metal\/.*/ })"
- **C416** [quoted] TFC replaces the player's food stats object; eating an item only has an effect if the stack has TFC's food capability, otherwise the eat call does nothing — "final @Nullable IFood food = FoodCapability.get(stack);" — [S69], eat(Item, ItemStack, LivingEntity), lines 121-129; followed by "if (food != null)" / "eat(food);"
- **C418** [quoted] The food capability is attached only to stacks that match a food definition loaded from data — "FoodDefinition food = FoodCapability.getDefinition(stack);" — [S70], attach-capabilities handler, lines 395-398
- **C419** [quoted] Food definitions are datapack JSON under <namespace>/tfc/food_items, matched by an ingredient, with hunger/saturation/water/decay/nutrient fields — "A food item definition defines a food, and applies TFC style stats to it including decay, hunger, water, and nutrition. It is a file located under the subfolder `food_items`." — [S71], section "Food Items" (text from Documentation repo 1.20.x/custom.md line 187)
- **C420** [inference] A vanilla food item that has no food_items definition gets no food capability, so eating it adds no hunger, saturation or nutrition through TFC's food data even if it is obtained; which vanilla items lack a definition was not listed — from [C416], [C418], [C419]
- **C421** [quoted] TFC explicitly defines vanilla rotten flesh as a zero-value food — "\"hunger\": 0," — [S72], whole file: ingredient minecraft:rotten_flesh, hunger 0, saturation 0, decay_modifier 99999
- **C422** [quoted] Counter-example: at least one vanilla item is a real TFC food — minecraft:pumpkin_pie has a definition with hunger 4 (its vanilla recipe is disabled per C404) — "\"item\": \"minecraft:pumpkin_pie\"" — [S73], ingredient field; hunger 4, saturation 2, water 5, fruit 1.5, grain 1
- **C423** [quoted] TFC overrides the vanilla zombie loot table with a table whose only entry is rotten flesh, i.e. no carrot/potato rare drops — "\"name\": \"minecraft:rotten_flesh\"," — [S74], single pool, single entry
- **C425** [measured] TFG removes vanilla food items outright through Reliable Remover: config/reliable_remover/minecraft.json lists raw and cooked porkchop, chicken, mutton, rabbit, beef, cod, salmon, plus tropical_fish, bread, wheat, cookie, cake, carrot, potato, baked_potato, beetroot, beetroot_soup, mushroom_stew, rabbit_stew, sweet_berries, melon_slice, apple, golden_apple, enchanted_golden_apple, dried_kelp, honey_bottle, wheat_seeds and both mushrooms; it does not list pumpkin_pie, rotten_flesh, golden_carrot, chorus_fruit, glow_berries, pufferfish or spider_eye — Own measurements, M24
- **C427** [quoted] TFG wipes the loot of vanilla animal/mob entity types with a KubeJS loot modifier per entity (cow, pig, chicken, sheep, rabbit, cod, salmon …), closing the drop path for vanilla meat — "event.addEntityLootModifier('minecraft:cow').removeLoot(ItemFilter.ALWAYS_TRUE)" — [S75], registerMinecraftLoots, line 9 (41 such lines at file start)
- **C430** [quoted] TFG gives `minecraft:popped_chorus_fruit` food properties (hunger 4, saturation 1) by item modification in a startup script — "event.modify('minecraft:popped_chorus_fruit', item => {" — [S76], registerMinecraftItemModifications, lines 5-10
- **C431** [quoted] TFC's plains biome JSON carries the spawn lists: its creature list holds only tfc: entities (vanilla animals are absent), its monster list holds five vanilla monsters — "\"type\": \"tfc:pig\"," — [S77], spawners.creature (line 73); spawners.monster lists minecraft:spider, zombie, skeleton, creeper, slime (lines 33-60)
- **C432** [quoted] The tag tfc:vanilla_monsters defines which entities the surface-spawn rule applies to — "`tfc:vanilla_monsters` | Monsters that are prevented from spawning on the surface." — [S78], entity tags table (Documentation repo 1.20.x/tags.md line 150); tag file lists minecraft:spider, zombie, skeleton, creeper, slime
- **C433** [quoted] Config enableVanillaMonsters (default true): if false all spawns of tagged monsters are denied — "If true, vanilla monsters are able to spawn. If false, the 'enableVanillaMonstersOnSurface' config option is not used, and all spawns are denied." — [S79], line 651
- **C434** [quoted] Config enableVanillaMonstersOnSurface (default false): monsters only underground — "If true, vanilla monsters will spawn on the surface instead of just underground. If false, vanilla monsters will not spawn on the surface." — [S79], line 652
- **C435** [quoted] Enforcement is a Forge MobSpawnEvent.FinalizeSpawn listener that cancels natural/chunk-generation/reinforcement spawns of tagged entities — "if (spawn == MobSpawnType.NATURAL || spawn == MobSpawnType.CHUNK_GENERATION || spawn == MobSpawnType.REINFORCEMENT)" — [S70], onLivingSpawnCheck, lines 1007-1052; cancel = "event.setSpawnCancelled(true);" + "event.setCanceled(true);"
- **C436** [quoted] The "surface" test has three conditions: any light at the position (slimes exempt), position at/above the WORLD_SURFACE heightmap, or the block below not in tag tfc:monster_spawns_on — "else if (!Helpers.isBlock(level.getBlockState(pos.below()), TFCTags.Blocks.MONSTER_SPAWNS_ON))" — [S70], lines 1027-1043; first branch: "if (entity.getType() != EntityType.SLIME && level.getRawBrightness(pos, 0) != 0)"
- **C437** [quoted] The spawn rule applies only in worlds using TFC's chunk generator; spawners, eggs and commands are not affected (only the three spawn types in C435) — "if (!(event.getLevel().getLevel().getChunkSource().getGenerator() instanceof ChunkGeneratorExtension))" — [S70], lines 1013-1016
- **C438** [quoted] Some entities are cancelled in EntityJoinLevelEvent instead, because FinalizeSpawn is not always fired: skeleton trap horses, iron/snow golems, chicken jockeys (each behind its own config, default off) — "- Prevents some categories of mobs from spawning. Some can't be done in {@link MobSpawnEvent.FinalizeSpawn} because Forge does not always fire it." — [S70], onEntityJoinLevel javadoc line 1060; body lines 1114-1147
- **C440** [quoted] Phantoms, wandering traders and pillager patrols are switched off through forced game rules, not spawn events — "  doInsomnia = false (No phantoms)" — [S79], enableForcedTFCGameRules comment lines 284-290; applied in ForgeEventHandler lines 559-566
- **C441** [quoted] Fauna JSON only tunes climate/ground/brightness conditions of existing spawn entries; spawn weights live in the biome JSON — "**Note:** Fauna cannot be added with datapacks, only modified." — [S71], section "Fauna" (custom.md line 136)
- **C442** [quoted] TFC maintainer on the design: surface-monster blocking is intentional and config-controlled, and other mods' mobs need datapack compatibility to spawn in TFC biomes — "Hostile mobs not spawning on the surface is intentional behavior, this can be disabled in the config, via `enableVanillaMonsters` and `enableVanillaMonstersOnSurface`." — [S80], comment by alcatrazEscapee (member)
- **C443** [quoted] TFG ships TFC server config unchanged on the monster options: monsters on, surface off, golems/jockeys/skeleton horses off — "enableVanillaMonstersOnSurface = false" — [S81], line 839; also "enableVanillaMonsters = true" line 836, "enableNetherPortals = true" line 9
- **C444** [quoted] TFG extends TFC's underground-only rule to more mobs by adding them to tfc:vanilla_monsters in KubeJS (enderman, husk, drowned, slime, an Enderman Overhaul variant) — "// Tag used by TFC to control monsters spawning on the surface" — [S82], registerTFGOverworldEntityTypeTags, lines 686-691; e.g. "event.add('tfc:vanilla_monsters', 'minecraft:husk')"
- **C445** [measured] TFG's overworld plains biome JSON in Core-Modern (data/tfg/worldgen/biome/earth/plains.json) lists as spawner types minecraft:spider, zombie, skeleton, creeper, slime, enderman, husk, drowned and bat, endermanoverhaul:flower_fields_enderman, tfc:* animals, tfg:* animals (bison, camels, jerboa, lemming, mongoose) and fowlplay:* birds; no minecraft: farm animal is in the list — Own measurements, M25
- **C446** [quoted] Core-Modern adds its own FinalizeSpawn listener for non-overworld dimensions: natural spawns are cancelled unless the block below is in tfc:monster_spawns_on — "// This prevents mobs from spawning on \"non-natural blocks\" in other dimensions." — [S83], class comment lines 17-18; "// Let TFC handle the overworld" line 27
- **C447** [quoted] TFG neutralises the default spawns of added mob mods by overriding their Forge biome modifiers in kubejs/data: either a remove_spawns modifier over all biomes or replacing the mod's add-spawn file with forge:none — "\"type\": \"forge:remove_spawns\"," — [S84], line 2, with "biomes": {"type": "forge:any"}; same in kubejs/data/arthropocolypse/forge/biome_modifier/ap_mob_spawns.json
- **C448** [quoted] The forge:none variant (file with the same path as the mod's own modifier) — "\"type\": \"forge:none\"" — [S85], whole file; same for kubejs/data/fowlplay/forge/biome_modifier/biome_modifier.json
- **C449** [inference] Besides neutralising mod defaults, TFG lists mobs in its own biome JSON; only earth/plains was read, the other dimensions' biome files were not — from [C445], [C447], [C448]
- **C450** [quoted] In events.entities.js the KubeJS entity event is EntityEvents.spawned, used for rebalancing (max HP, armor, fire resistance per entity type and dimension); that file cancels no spawns — "// Easier to just keep all entities in here for balancing instead of spreading them around" — [S86], line 123; "EntityEvents.spawned((event) => {" line 125
- **C451** [measured] A name search of pakku-lock.json project slugs for spawn, mob, control or blacklist returns only Let Me Despawn and Controlling; no dedicated spawn-control mod turns up by that search — Own measurements, M26

## Disconfirming evidence

- [C139] — queries (`gh search prs`, 2026-11-22): `--repo TerraFirmaGreg-Team/Modpack-Modern "core in:title"`, `"\"to core\""`, `--repo TerraFirmaGreg-Team/Core-Modern "\"to kubejs\""`, `"kubejs in:title"`, `"\"instead of kubejs\" OR \"from kubejs\" OR \"out of kubejs\""` — found against a one-way reading "everything migrates to Java": moves go in both directions; the counter-direction is its own claim [C137]. No issue or PR was found where the team regrets the KubeJS-heavy split or plans to leave KubeJS.
- [C236] — queries: web search `TerraFirmaCraft world generation only without survival mechanics standalone mod worldgen TFC terrain climate separate mod`; `gh search issues` in TerraFirmaCraft/TerraFirmaCraft for `worldgen standalone`, `"world gen" "separate mod"`, `worldgen only vanilla` (0 hits each); `gh search repos` `tfc worldgen`, `terrafirmacraft terrain` — nothing found against. Hits were addons that require TFC [C237]. No maintainer statement either way; the TFC Discord was not searched.
- [C350] — queries (`gh search issues`, Modpack-Modern): `TFC irrelevant late game`, `food late game pointless`, `anvil late game obsolete`, `nutrition late game`, `late game food`, `skip TFC` — zero hits each. A direct check for the opposite (decay switched off by config) found the default rate instead [C331]. Nothing found against; no play data was read.
- [C420], [C435] — queries (`gh search issues`): `vanilla food` (Modpack-Modern 0 hits; TerraFirmaCraft hits unrelated to leaks), `edible` (Modpack-Modern hits unrelated), `surface monsters` (Modpack-Modern 0 hits), `enableVanillaMonsters` (TerraFirmaCraft #2796, the maintainer confirms the behaviour, [C442]); a fifth query hit the search rate limit and returned nothing. Nothing found against in issue trackers. Paths that stay open were found in code and are claims of their own: [C404], [C405], [C409], [C422], [C430], [C437].
- [C318] — queries (`gh search issues --repo TerraFirmaGreg-Team/Modpack-Modern`, 2026-11-23): `bypass black steel`, `skip bloomery`, `skip progression`, `exploit steel early` — zero hits each. Nothing found against.
- [C251] — queries (`gh search issues --repo TerraFirmaCraft/TerraFirmaCraft`, 2026-11-23): `EUPL` (one hit, #1813, whose title is about ingot placement), `license fork`, `relicense` — zero relevant hits. Nothing found against.
- [C203] — queries (`gh search issues --repo TerraFirmaCraft/TerraFirmaCraft`, 2026-11-23): `temperature east west`, `rainfall north south`, `climate rotate axis` — zero hits each. Nothing found against.

## Coverage

| Channel | Read | Gap |
|---|---|---|
| Users | | not searched: player experience is sub-question (6), left to the next run; issue trackers were searched only for the disconfirming queries above |
| Specialists | the team's own repositories and wiki (Modpack-Modern, Core-Modern, Wiki, Tools-Modern, QuestBook-Modern, Splice, templates), TerraFirmaCraft source and its Documentation repo, one TFC maintainer comment, the Reliable Remover page — see Sources | Discord of both teams not readable; the Reddit post that explains the TFC fork is not a source; Java sources of Core-Modern counted, not read; the TFC field guide not read |
| Data | own measurements M1–M27 over the repositories' git trees, lock file and configs | no external dataset; none needed for a question about one pack's structure |
| Adversarial | issue and PR searches listed under Disconfirming evidence | nothing found; no post-mortem or "why we moved off" text exists in the sources read |

Declared overruns of the ceiling (four searches, three page reads per sub-question): Q1 about 14 `gh search` calls and more than three page reads; Q2 about nine primary reads; Q3 about 25 raw files; Q4 five code searches and six issue searches. Reason in each case: the sources are many small files in large repositories, each the primary source for one fact. Two ledgers were trimmed to the 20,000-character cap; entries cut for the cap are named in Open questions where they matter.

Tools: the built-in floor only (`gh`, `curl`, web search). One GitHub search call in Q1 and one in Q4 hit the search rate limit and returned nothing.

The Q2 gatherer's count of the vendored TFC files (1572 files under `net/dries007/tfc`, all file types) differed from the Q1 count (1,467 `.java` files, [C119]); only the Q1 measurement, which has its command and output recorded, is kept.

## Own measurements

### M1 — Q1 gathering, run 2026-11-22/23

Command and output: `gh api "repos/TerraFirmaGreg-Team/Modpack-Modern/git/trees/dev?recursive=1" > mp-tree.json; python -I count.py` -> `[('kubejs', 16183), ('config', 569), ('.pakku', 87), ('defaultconfigs', 61), ('.github', 60), ('tacz', 6), ...]`; `.js` 488 files; `java bytes 0`.

### M2 — Q1 gathering, run 2026-11-22/23

Command and output: same script, depth-2 counter -> `[('kubejs/assets', 12770), ('kubejs/data', 2917), ('kubejs/server_scripts', 353), ('kubejs/startup_scripts', 130), ('kubejs/client_scripts', 4), ...]`.

### M3 — Q1 gathering, run 2026-11-22/23

Command and output: sum of blob `size` for `kubejs/**.js` per second-level dir -> `Counter({'kubejs/server_scripts': 2486469, 'kubejs/startup_scripts': 547044, 'kubejs/client_scripts': 56038, 'kubejs/eslint.config.js': 1991})`.

### M4 — Q1 gathering, run 2026-11-22/23

Command and output: `python -I count2.py` -> `kubejs/server_scripts 96 [('tfg', 156), ('gregtech', 8), ('tacz', 8), ('tfc', 8), ('minecraft', 6), ('greate', 5), ...]`, `kubejs/startup_scripts 31 [('tfg', 85), ('gtceu', 5), ...]`.

### M5 — Q1 gathering, run 2026-11-22/23

Command and output: tree filter on `startup_scripts` paths containing machine/recipe_type -> the three paths with sizes above; `curl -sL .../dev/kubejs/startup_scripts/gtceu/machines.js | grep -c "event.create("` -> `9`.

### M6 — Q1 gathering, run 2026-11-22/23

Command and output: `python -I count2.py` depth-5 counter on `kubejs/data/tfg` -> `[('kubejs/data/tfg/worldgen/configured_feature', 599), ('kubejs/data/tfg/worldgen/placed_feature', 597), ('kubejs/data/tfg/structures/mineshaft', 94), ('kubejs/data/tfg/loot_tables/blocks', 91), ('kubejs/data/tfg/worldgen/template_pool', 86), ...]`.

### M8 — Q1 gathering, run 2026-11-22/23

Command and output: `curl -sL .../dev/pakku-lock.json -o pakku-lock.json; python -I lock.py` -> `projects 279`, `Counter({'MOD': 268, 'RESOURCE_PACK': 9, 'SHADER': 2})`, `'loaders': {'forge': '47.4.13'}`, names listed by keyword filter; `grep -c -i '"terrafirmacraft"' pakku-lock.json` -> `0`. Unresolved anomaly: the lock still has a `tfc-ambiental-refreshed` slug (line 14910).

### M9 — Q1 gathering, run 2026-11-22/23

Command and output: tree filter on `ftbquests` -> `config/ftbquests/quests/chapters/*.snbt` (17 files, e.g. `questsmetallurgy.snbt 85327`, `iv__insane_voltage.snbt 1898`), `kubejs/assets/ftbquests/lang/en_us.json 39162` and 15 other locales.

### M10 — Q1 gathering, run 2026-11-22/23

Command and output: `gh api "repos/TerraFirmaGreg-Team/Core-Modern/git/trees/dev?recursive=1" > core-tree.json; python -I count2.py` -> `6 1467 6484171 src/main/java/net/dries007/tfc`, `6 1070 5340636 src/main/java/su/terrafirmagreg/core`, `6 4 42742 .../tfc_textile`, `6 23 110301 .../tfcambiental`; `('.java', 2565)`, `java bytes 11980221`.

### M11 — Q1 gathering, run 2026-11-22/23

Command and output: `count2.py` depth 7 -> `391 2940228 .../core/common`, `71 445564 .../core/compat`, `376 786448 .../core/mixins`, `138 679602 .../core/world`, `54 333668 .../core/client`, network 18, utils 13, config 7; `count.py` depth 8 -> `73 .../common/block`, `64 .../common/tfgt`, `63 .../common/entity`, `56 .../common/data`, `70 .../world/new_ow_wg`, `37 .../world/feature`.

### M12 — Q1 gathering, run 2026-11-22/23

Command and output: counter over `core/mixins/{client,common}/<target>` -> `49 374 [('gtceu', 54), ('minecraft', 53), ('create', 46), ('tfc', 45), ('firmalife', 20), ('ad_astra', 19), ('species', 17), ('rpg_skeleton', 15), ('wab', 10), ('beneath', 9), ...]`; resources hold `tfc.mixins.json`, `tfg.mixins.json`, `tfg.optional.mixins.json`.

### M14 — Q1 gathering, run 2026-11-22/23

Command and output: `count.py` depth-5 counter -> `[('src/main/resources/assets/tfc', 21937), ('src/main/resources/data/tfc', 17002), ('src/generated/resources/assets/tfg', 2768), ('src/main/resources/assets/tfg', 1767), ('src/main/resources/data/tfg', 687), ('src/generated/resources/data/tfg', 664), ...]`.

### M15 — Q1 gathering, run 2026-11-22/23

Command and output: `gh pr view 580 --repo TerraFirmaGreg-Team/Core-Modern --json title,body,author,comments,createdAt,additions,deletions,changedFiles` -> `{"additions":896858,"author":"Pyritie","body":"","changedFiles":40974,"comments":[],"createdAt":"2026-09-12T12:11:42Z","deletions":3311,"title":"Tfc fork"}`; first commit on `net/dries007/tfc/TerraFirmaCraft.java`: `b4ad76cd 2026-09-12 Pyritie Tfc fork (#580)`.

### M16 — Q1 gathering, run 2026-11-22/23

Command and output: `gh api orgs/TerraFirmaGreg-Team/repos --paginate --jq '.[]|[.name,.description,.fork,.pushed_at,.default_branch,.archived]|@tsv'` -> 25 rows, `fork=true` only on `AutoPack-Director	Runtime mod&file processing	true	2026-09-13...`; non-forks include Modpack-Modern, Core-Modern, Tools-Modern, Jellies-Modern, QuestBook-Modern, RecipeBook-Modern, Field-Guide-Modern, Wiki, Actions, Splice, Konf, Modularity, Game-Exporter, Modpack-Template, Mod-Template. `count2.py` -> `('config/mod-director', 11)`.

### M17 — Q1 gathering, run 2026-11-22/23

Command and output: `gh api "repos/TerraFirmaGreg-Team/QuestBook-Modern/git/trees/main?recursive=1" --jq '.tree[].path'` -> `.github/workflows/build.yml`, `README.md`, `ci/build.env`, `ci/config/export-*.toml`, `ci/lib/build-json.mjs`, `ci/run.sh`, `ci/scripts/*.mjs`, `language.json`; README 18 bytes.

### M18 — Q1 gathering, run 2026-11-22/23

Command and output: `gh api "repos/TerraFirmaGreg-Team/<repo>/contents?ref=main" --jq '[.[].name]|join(" ")'` -> Modpack-Template: `.github .gitignore .pakku CHANGELOG.md README.md config pakku-lock.json pakku.jar pakku.json`; Mod-Template: `.gitattributes .github .gitignore CHANGELOG.md README.md build.gradle gradle.properties gradle gradlew gradlew.bat modules settings.gradle`; READMEs 18 and 14 bytes.


### M21–M27 — re-measurement for Q2 and Q4, run 2026-11-23

Command: `gh api "repos/TerraFirmaCraft/TerraFirmaCraft/git/trees/1.20.x?recursive=1" > tfc-tree.json; python measure.py`. The script reads three git-tree dumps (`mp-tree.json` = Modpack-Modern `dev`, `core-tree.json` = Core-Modern `dev`, `tfc-tree.json` = TerraFirmaCraft `1.20.x`), the pack's `pakku-lock.json`, and fetches three raw files: TFC `1.20.x` `src/main/java/net/dries007/tfc/config/ServerConfig.java`, Modpack-Modern `dev` `config/reliable_remover/minecraft.json`, Core-Modern `dev` `src/main/resources/data/tfg/worldgen/biome/earth/plains.json`. Output sections map to measurement IDs: MA = M21 (count of blobs under `src/main/resources/data/minecraft/recipes/`), MB = M22 (whether a recipe override file of that name exists), MC = M23 (config keys from `define*("<key>"` calls), MD = M24 (whether `"minecraft:<item>"` occurs in the remover list), ME = M25 (distinct `"type"` values in plains.json), MF = M26 (lock-file slugs matching `spawn|mob|control|blacklist`), MG = M27 (file counts under `kubejs/data/tfg/worldgen/`).

Output:

```
== trees
mp-tree.json truncated: False entries: 19004
core-tree.json truncated: False entries: 51013
tfc-tree.json truncated: False entries: 43944
== MA tfc 1.20.x blobs under data/minecraft/recipes: 230
== MB food-related override present?: {'bread': False, 'cake': False, 'cookie': False, 'golden_apple': False, 'golden_carrot': False, 'beetroot_soup': True, 'hay_block': True, 'melon': True, 'melon_seeds': True, 'mushroom_stew': True, 'pumpkin_pie': True, 'rabbit_stew_from_brown_mushroom': True, 'rabbit_stew_from_red_mushroom': True, 'suspicious_stew': True, 'wheat': True, 'cooked_beef': False, 'cooked_porkchop': False, 'baked_potato': False, 'cooked_chicken': False, 'dried_kelp': False, 'pumpkin_seeds': False, 'sugar': False}
   names containing cooked/smoking/campfire: ['campfire', 'soul_campfire']
== MC ServerConfig keys total: 169
   keys starting enable: ['enableNetherPortals', 'enableForcedTFCGameRules', 'enableTimeStopWhenServerEmpty', 'enableFireArrowSpreading', 'enableInfestations', 'enableCalendarSensitiveMoonPhases', 'enableLightning', 'enableLightningStrippingLogs', 'enableFarmlandCreation', 'enableGrassPathCreation', 'enableRootedDirtToDirtCreation', 'enableDirtToMudCreation', 'enableSnowSlowEntities', 'enableMossyRockSpreading', 'enablePumpkinCarving', 'enableThatchBedSpawnSetting', 'enableThatchBedSleeping', 'enableLeavesDecaySlowly', 'enablePlacingItems', 'enableSmallVesselInventoryInteraction', 'enableBlockCollapsing', 'enableExplosionCollapsing', 'enableBlockLandslides', 'enableChiselsStartCollapses', 'enablePeacefulDifficultyPassiveRegeneration', 'enableThirstOverheating', 'enableOverburdening', 'enableVanillaBonemeal', 'enableVanillaWeatherEffects', 'enableVanillaSkeletonHorseSpawning', 'enableVanillaMobsSpawningWithEnchantments', 'enableVanillaMobsSpawningWithVanillaEquipment', 'enableVanillaGolems', 'enableVanillaMonsters', 'enableVanillaMonstersOnSurface', 'enableChickenJockies', 'enableVanillaEggThrowing', 'enableVanillaDrinkingMilkClearsPotionEffects']
   keys matching recipe: []
   keys matching worldgen|survival|disableAll: []
== MD reliable_remover/minecraft.json listed: {'porkchop': True, 'cooked_porkchop': True, 'chicken': True, 'cooked_chicken': True, 'mutton': True, 'cooked_mutton': True, 'rabbit': True, 'cooked_rabbit': True, 'beef': True, 'cooked_beef': True, 'cod': True, 'cooked_cod': True, 'salmon': True, 'cooked_salmon': True, 'tropical_fish': True, 'bread': True, 'wheat': True, 'cookie': True, 'cake': True, 'carrot': True, 'potato': True, 'baked_potato': True, 'beetroot': True, 'beetroot_soup': True, 'mushroom_stew': True, 'rabbit_stew': True, 'sweet_berries': True, 'melon_slice': True, 'apple': True, 'golden_apple': True, 'enchanted_golden_apple': True, 'dried_kelp': True, 'honey_bottle': True, 'wheat_seeds': True, 'brown_mushroom': True, 'red_mushroom': True, 'pumpkin_pie': False, 'rotten_flesh': False, 'golden_carrot': False, 'chorus_fruit': False, 'popped_chorus_fruit': False, 'glow_berries': False, 'pufferfish': False, 'spider_eye': False}
== ME plains.json spawner types: ['endermanoverhaul:flower_fields_enderman', 'fowlplay:blue_jay', 'fowlplay:cardinal', 'fowlplay:chickadee', 'fowlplay:crow', 'fowlplay:goose', 'fowlplay:gull', 'fowlplay:hawk', 'fowlplay:pigeon', 'fowlplay:raven', 'fowlplay:robin', 'fowlplay:sparrow', 'minecraft:bat', 'minecraft:creeper', 'minecraft:drowned', 'minecraft:enderman', 'minecraft:husk', 'minecraft:skeleton', 'minecraft:slime', 'minecraft:spider', 'minecraft:zombie', 'tfc:alpaca', 'tfc:black_bear', 'tfc:boar', 'tfc:bongo', 'tfc:caribou', 'tfc:chicken', 'tfc:cougar', 'tfc:cow', 'tfc:crocodile', 'tfc:deer', 'tfc:direwolf', 'tfc:donkey', 'tfc:duck', 'tfc:fox', 'tfc:frog', 'tfc:gazelle', 'tfc:goat', 'tfc:grizzly_bear', 'tfc:grouse', 'tfc:horse', 'tfc:hyena', 'tfc:lion', 'tfc:moose', 'tfc:musk_ox', 'tfc:ocelot', 'tfc:panda', 'tfc:panther', 'tfc:peafowl', 'tfc:pheasant', 'tfc:pig', 'tfc:polar_bear', 'tfc:quail', 'tfc:rabbit', 'tfc:sabertooth', 'tfc:sheep', 'tfc:tiger', 'tfc:turkey', 'tfc:wildebeest', 'tfc:wolf', 'tfc:yak', 'tfg:bactrian_camel', 'tfg:bison', 'tfg:dromedary_camel', 'tfg:jerboa', 'tfg:lemming', 'tfg:mongoose']
== MF lock slugs matching spawn|mob|control|blacklist|despawn: ['Let Me Despawn', 'controlling', 'let-me-despawn']
== MG kubejs/data/tfg/worldgen by kind: [('configured_feature', 599), ('placed_feature', 597), ('template_pool', 86), ('processor_list', 57), ('density_function', 54), ('biome', 45), ('noise', 20), ('structure', 17), ('structure_set', 16), ('configured_carver', 5), ('noise_settings', 5)]
    configured_feature groups: [('nether', 184), ('earth', 156), ('mars', 132), ('moon', 84), ('venus', 42)]
    placed_feature groups: [('nether', 180), ('earth', 160), ('mars', 125), ('moon', 87), ('venus', 44)]
   earth/vein configured: 76
   dimension_type: ['glacio.json', 'mars.json', 'mercury.json', 'moon.json', 'the_nether.json', 'venus.json']
   overrides in kubejs/data/tfc/worldgen: 19 firmalife climate_ranges: 5

```

## Open questions

- Next run, sub-question (5) — process: how releases are built (pakku, CI workflows in `.github`, the `Actions` and `AutoPack-Director` repos), what tests exist, how quests are authored and exported (QuestBook-Modern `ci/`), how localisation is organised (Tools-Modern, 16 locale files), how many people contributed and over what time (contributor and commit history of Modpack-Modern and Core-Modern). Not gathered in this run.
- Next run, sub-question (6) — typical player complaints about the pack: issue tracker themes, GitHub Discussions, CurseForge/Modrinth comments, long-form reviews. Not gathered in this run.
- Q1 — the full rationale for forking TerraFirmaCraft is linked from the changelog to Reddit, which cannot be read; the fork PR has an empty body. A maintainer statement on GitHub or the wiki would settle it.
- Q1 — Java sources of Core-Modern were counted by package, not read; what `common/tfgt` holds is unverified. Reading the packages would settle it.
- Q2 — no maintainer statement on using TFC worldgen standalone; `VeinConfig.java` was not read to confirm that veins have no undocumented climate field; per-file asset licences are not itemised in the repo.
- Q2 — the terms of the permission covering code derived from TFC 1.7.10 are not published; asking the TFC maintainers would settle whether it restricts a fork.
- Q3 — no maintainer statement on what should happen to TFC mechanics in the late game, and no play data on whether players ignore food late; IV content is a placeholder, so late-game findings rest on EV content.
- Q4 — not verified in code whether an undefined vanilla food item is still consumed on use; the contents of the tag `tfc:monster_spawns_on` were not read; TFC 1.21.x was not checked for Q4.
- Label `open` — what is missing: the terms of the TFC 1.7.10 permission and a reading of the vein code (both under Q2), and sub-questions (5) and (6), which were not gathered. A reply from the TFC maintainers on the terms, reading `VeinConfig.java`, and a second run on (5) and (6) would settle it.
- Q2 — whether TFG's own overworld pipeline (`useTfgOverworldPipeline()`), and with it the hemisphere mixin, is switched on in the released pack was not checked; reading where the flag is set would settle it.
- Q4 — which vanilla food items have no TFC `food_items` definition was not listed; listing TFC's `data/tfc/tfc/food_items` against the vanilla food items would settle it.

## Sources

- [S1] TerraFirmaGreg-Team/Wiki: docs/modern/en_us/developer/contributing.md — https://github.com/TerraFirmaGreg-Team/Wiki/blob/main/docs/modern/en_us/developer/contributing.md — passage
- [S2] TerraFirmaGreg-Team/Core-Modern: README.md — https://github.com/TerraFirmaGreg-Team/Core-Modern/blob/dev/README.md — passage
- [S3] TerraFirmaGreg-Team/Modpack-Modern: kubejs/startup_scripts/main_startup_script.js — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/blob/dev/kubejs/startup_scripts/main_startup_script.js — passage
- [S4] TerraFirmaGreg-Team/Core-Modern: src/main/resources/kubejs.plugins.txt — https://github.com/TerraFirmaGreg-Team/Core-Modern/blob/dev/src/main/resources/kubejs.plugins.txt — passage
- [S5] TerraFirmaGreg-Team/Core-Modern: src/main/resources/META-INF/mods.toml — https://github.com/TerraFirmaGreg-Team/Core-Modern/blob/dev/src/main/resources/META-INF/mods.toml — passage
- [S6] TerraFirmaGreg-Team/Modpack-Modern: CHANGELOG.md — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/blob/dev/CHANGELOG.md — passage
- [S7] TerraFirmaGreg-Team/Core-Modern: 518 — https://github.com/TerraFirmaGreg-Team/Core-Modern/pull/518 — passage
- [S8] TerraFirmaGreg-Team/Modpack-Modern: 4290#issuecomment-4798649683 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/4290#issuecomment-4798649683 — passage
- [S9] TerraFirmaGreg-Team/Core-Modern: 453 — https://github.com/TerraFirmaGreg-Team/Core-Modern/pull/453 — passage
- [S10] TerraFirmaGreg-Team/Core-Modern: 25 — https://github.com/TerraFirmaGreg-Team/Core-Modern/pull/25 — passage
- [S11] TerraFirmaGreg-Team/Tools-Modern: README.md — https://github.com/TerraFirmaGreg-Team/Tools-Modern/blob/dev/README.md — passage
- [S12] TerraFirmaGreg-Team/Splice: README.md — https://github.com/TerraFirmaGreg-Team/Splice/blob/main/README.md — passage
- [S13] TerraFirmaCraft/TerraFirmaCraft: build.gradle.kts — https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/build.gradle.kts — passage
- [S14] TerraFirmaCraft/TerraFirmaCraft: src/main/java/net/dries007/tfc/world/region/RegionGenerator.java — https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/region/RegionGenerator.java — passage
- [S15] TerraFirmaCraft/Documentation: 1.20.x/worldgen/world-preset.md — https://github.com/TerraFirmaCraft/Documentation/blob/main/1.20.x/worldgen/world-preset.md — passage
- [S16] TerraFirmaCraft/TerraFirmaCraft: src/generated/resources/data/tfc/worldgen/world_preset/overworld.json — https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/generated/resources/data/tfc/worldgen/world_preset/overworld.json — passage
- [S17] TerraFirmaCraft/TerraFirmaCraft: src/main/java/net/dries007/tfc/world/settings/Settings.java — https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/settings/Settings.java — passage
- [S18] TerraFirmaCraft/TerraFirmaCraft: src/main/java/net/dries007/tfc/world/region/AnnotateClimate.java — https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/world/region/AnnotateClimate.java — passage
- [S19] TerraFirmaCraft/TerraFirmaCraft: src/main/java/net/dries007/tfc/util/Helpers.java — https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/util/Helpers.java — passage
- [S20] TerraFirmaCraft/TerraFirmaCraft: src/main/java/net/dries007/tfc/util/climate/OverworldClimateModel.java — https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/src/main/java/net/dries007/tfc/util/climate/OverworldClimateModel.java — passage
- [S21] TerraFirmaCraft/TerraFirmaCraft: src/main/java/net/dries007/tfc/util/climate/OverworldClimateModel.java — https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/util/climate/OverworldClimateModel.java — passage
- [S22] TerraFirmaCraft/Documentation: 1.20.x/custom.md — https://github.com/TerraFirmaCraft/Documentation/blob/main/1.20.x/custom.md — passage
- [S23] TerraFirmaCraft/Documentation: 1.20.x/worldgen/decorators.md — https://github.com/TerraFirmaCraft/Documentation/blob/main/1.20.x/worldgen/decorators.md — passage
- [S24] TerraFirmaCraft/TerraFirmaCraft: src/main/resources/data/tfc/worldgen/placed_feature/plant/wild_crop/rice_patch.json — https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/src/main/resources/data/tfc/worldgen/placed_feature/plant/wild_crop/rice_patch.json — passage
- [S25] TerraFirmaCraft/Documentation: 1.21.x/worldgen/placement_modifiers.md — https://github.com/TerraFirmaCraft/Documentation/blob/main/1.21.x/worldgen/placement_modifiers.md — passage
- [S26] TerraFirmaCraft/Documentation: 1.20.x/worldgen/features/veins.md — https://github.com/TerraFirmaCraft/Documentation/blob/main/1.20.x/worldgen/features/veins.md — passage
- [S27] TerraFirmaCraft/TerraFirmaCraft: src/main/resources/data/tfc/worldgen/configured_feature/vein/surface_cassiterite.json — https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/src/main/resources/data/tfc/worldgen/configured_feature/vein/surface_cassiterite.json — passage
- [S28] TerraFirmaCraft/Documentation: 1.20.x/worldgen/index.md — https://github.com/TerraFirmaCraft/Documentation/blob/main/1.20.x/worldgen/index.md — passage
- [S29] TerraFirmaCraft/TerraFirmaCraft: src/main/java/net/dries007/tfc/config/CommonConfig.java — https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/config/CommonConfig.java — passage
- [S30] TerraFirmaCraft/TerraFirmaCraft: src/main/java/net/dries007/tfc/ForgeEventHandler.java — https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/src/main/java/net/dries007/tfc/ForgeEventHandler.java — passage
- [S31] TerraFirmaCraft/TerraFirmaCraft: src/main/java/net/dries007/tfc/util/climate/BiomeBasedClimateModel.java — https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/src/main/java/net/dries007/tfc/util/climate/BiomeBasedClimateModel.java — passage
- [S32] TerraFirmaCraft/TerraFirmaCraft: README.md — https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/README.md — passage
- [S33] yazloysasha/TFC-Real-World: README.md — https://github.com/yazloysasha/TFC-Real-World/blob/HEAD/README.md — passage
- [S34] TerraFirmaGreg-Team/Core-Modern: src/main/java/net/dries007/tfc/util/climate/OverworldClimateModel.java — https://github.com/TerraFirmaGreg-Team/Core-Modern/blob/dev/src/main/java/net/dries007/tfc/util/climate/OverworldClimateModel.java — passage
- [S35] TerraFirmaGreg-Team/Core-Modern: src/main/java/su/terrafirmagreg/core/mixins/common/tfc/new_ow_wg/OverworldClimateModelMixin.java — https://github.com/TerraFirmaGreg-Team/Core-Modern/blob/dev/src/main/java/su/terrafirmagreg/core/mixins/common/tfc/new_ow_wg/OverworldClimateModelMixin.java — passage
- [S36] TerraFirmaGreg-Team/Modpack-Modern: kubejs/data/tfg/worldgen/configured_feature/earth/vein/deep_apatite.json — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/blob/dev/kubejs/data/tfg/worldgen/configured_feature/earth/vein/deep_apatite.json — passage
- [S37] TerraFirmaCraft/TerraFirmaCraft: LICENSE.txt — https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.21.x/LICENSE.txt — passage
- [S38] TerraFirmaGreg-Team/Modpack-Modern: .github/README.md — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/.github/README.md — passage
- [S39] TerraFirmaGreg-Team/Core-Modern: README.md — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Core-Modern/dev/README.md — passage
- [S40] TerraFirmaGreg-Team/Modpack-Modern: kubejs/startup_scripts/tfg/materials/material_modification.tfc.js — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/kubejs/startup_scripts/tfg/materials/material_modification.tfc.js — passage
- [S41] TerraFirmaGreg-Team/Modpack-Modern: kubejs/server_scripts/tfg/ores_and_materials/recipes.materials.js — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/kubejs/server_scripts/tfg/ores_and_materials/recipes.materials.js — passage
- [S42] TerraFirmaGreg-Team/Modpack-Modern: kubejs/server_scripts/tfc/recipes.metals.js — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/kubejs/server_scripts/tfc/recipes.metals.js — passage
- [S43] TerraFirmaGreg-Team/Wiki: docs/modern/en_us/major-changelogs/0.10.md — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Wiki/main/docs/modern/en_us/major-changelogs/0.10.md — passage
- [S44] TerraFirmaGreg-Team/Modpack-Modern: config/reliable_remover/gtceu.json — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/config/reliable_remover/gtceu.json — passage
- [S45] TerraFirmaGreg-Team/Modpack-Modern: kubejs/assets/tfg/lang/en_us/quests/metal_age.json — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/kubejs/assets/tfg/lang/en_us/quests/metal_age.json — passage
- [S46] TerraFirmaGreg-Team/Modpack-Modern: kubejs/server_scripts/gregtech/recipes.machines.js — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/kubejs/server_scripts/gregtech/recipes.machines.js — passage
- [S47] TerraFirmaGreg-Team/Modpack-Modern: kubejs/assets/tfg/lang/en_us/quests/low_voltage.json — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/kubejs/assets/tfg/lang/en_us/quests/low_voltage.json — passage
- [S48] TerraFirmaGreg-Team/Modpack-Modern: kubejs/assets/tfg/lang/en_us/quests/steam_age.json — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/kubejs/assets/tfg/lang/en_us/quests/steam_age.json — passage
- [S49] TerraFirmaGreg-Team/Modpack-Modern: kubejs/assets/tfg/lang/en_us/quests/high_voltage.json — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/kubejs/assets/tfg/lang/en_us/quests/high_voltage.json — passage
- [S50] TerraFirmaGreg-Team/Modpack-Modern: kubejs/assets/tfg/lang/en_us/quests/extreme_voltage.json — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/kubejs/assets/tfg/lang/en_us/quests/extreme_voltage.json — passage
- [S51] TerraFirmaGreg-Team/Modpack-Modern: kubejs/assets/tfg/lang/en_us/quests/insane_voltage.json — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/kubejs/assets/tfg/lang/en_us/quests/insane_voltage.json — passage
- [S52] TerraFirmaGreg-Team/Wiki: docs/modern/en_us/major-changelogs/0.13.md — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Wiki/main/docs/modern/en_us/major-changelogs/0.13.md — passage
- [S53] TerraFirmaGreg-Team/Modpack-Modern: kubejs/server_scripts/tfg/ores_and_materials/recipes.material_tools.js — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/kubejs/server_scripts/tfg/ores_and_materials/recipes.material_tools.js — passage
- [S54] TerraFirmaGreg-Team/Modpack-Modern: defaultconfigs/tfc-server.toml — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/defaultconfigs/tfc-server.toml — passage
- [S55] TerraFirmaGreg-Team/Modpack-Modern: kubejs/server_scripts/tfg/aquaponics/recipes.greenhouse.js — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/kubejs/server_scripts/tfg/aquaponics/recipes.greenhouse.js — passage
- [S56] TerraFirmaGreg-Team/Modpack-Modern: kubejs/server_scripts/tfg/food/recipes.biomass.js — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/kubejs/server_scripts/tfg/food/recipes.biomass.js — passage
- [S57] TerraFirmaGreg-Team/Modpack-Modern: kubejs/server_scripts/tfg/space_general/recipes.space_suits.js — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/kubejs/server_scripts/tfg/space_general/recipes.space_suits.js — passage
- [S58] TerraFirmaGreg-Team/Modpack-Modern: kubejs/server_scripts/tfg/venus/recipes.biochem.js — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/kubejs/server_scripts/tfg/venus/recipes.biochem.js — passage
- [S59] TerraFirmaGreg-Team/Wiki: docs/modern/en_us/major-changelogs/0.11.md — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Wiki/main/docs/modern/en_us/major-changelogs/0.11.md — passage
- [S60] TerraFirmaCraft/TerraFirmaCraft: src/main/resources/data/minecraft/recipes/furnace.json — https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/src/main/resources/data/minecraft/recipes/furnace.json — passage
- [S61] TerraFirmaCraft/TerraFirmaCraft: resources/recipes.py — https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/resources/recipes.py — passage
- [S62] modrinth.com/mod/reliable-remover — https://modrinth.com/mod/reliable-remover — passage
- [S63] TerraFirmaGreg-Team/Modpack-Modern: config/reliable_remover.json — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/blob/dev/config/reliable_remover.json — passage
- [S64] TerraFirmaGreg-Team/Modpack-Modern: config/reliable_remover/minecraft.json — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/blob/dev/config/reliable_remover/minecraft.json — passage
- [S65] TerraFirmaGreg-Team/Modpack-Modern: kubejs/server_scripts/minecraft/recipes.removes.js — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/blob/dev/kubejs/server_scripts/minecraft/recipes.removes.js — passage
- [S66] TerraFirmaGreg-Team/Modpack-Modern: kubejs/server_scripts/minecraft/tags.js — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/blob/dev/kubejs/server_scripts/minecraft/tags.js — passage
- [S67] TerraFirmaGreg-Team/Modpack-Modern: kubejs/startup_scripts/minecraft/constants.js — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/blob/dev/kubejs/startup_scripts/minecraft/constants.js — passage
- [S68] TerraFirmaGreg-Team/Modpack-Modern: kubejs/server_scripts/tfc/recipes.removes.js — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/blob/dev/kubejs/server_scripts/tfc/recipes.removes.js — passage
- [S69] TerraFirmaCraft/TerraFirmaCraft: src/main/java/net/dries007/tfc/common/capabilities/food/TFCFoodData.java — https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/src/main/java/net/dries007/tfc/common/capabilities/food/TFCFoodData.java — passage
- [S70] TerraFirmaCraft/TerraFirmaCraft: src/main/java/net/dries007/tfc/ForgeEventHandler.java — https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/src/main/java/net/dries007/tfc/ForgeEventHandler.java — passage
- [S71] terrafirmacraft.github.io/Documentation/1.20.x/custom/ — https://terrafirmacraft.github.io/Documentation/1.20.x/custom/ — passage
- [S72] TerraFirmaCraft/TerraFirmaCraft: src/main/resources/data/tfc/tfc/food_items/rotten_flesh.json — https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/src/main/resources/data/tfc/tfc/food_items/rotten_flesh.json — passage
- [S73] TerraFirmaCraft/TerraFirmaCraft: src/main/resources/data/tfc/tfc/food_items/pumpkin_pie.json — https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/src/main/resources/data/tfc/tfc/food_items/pumpkin_pie.json — passage
- [S74] TerraFirmaCraft/TerraFirmaCraft: src/main/resources/data/minecraft/loot_tables/entities/zombie.json — https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/src/main/resources/data/minecraft/loot_tables/entities/zombie.json — passage
- [S75] TerraFirmaGreg-Team/Modpack-Modern: kubejs/server_scripts/minecraft/loot.js — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/blob/dev/kubejs/server_scripts/minecraft/loot.js — passage
- [S76] TerraFirmaGreg-Team/Modpack-Modern: kubejs/startup_scripts/minecraft/modifications.js — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/blob/dev/kubejs/startup_scripts/minecraft/modifications.js — passage
- [S77] TerraFirmaCraft/TerraFirmaCraft: src/main/resources/data/tfc/worldgen/biome/plains.json — https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/src/main/resources/data/tfc/worldgen/biome/plains.json — passage
- [S78] terrafirmacraft.github.io/Documentation/1.20.x/tags/ — https://terrafirmacraft.github.io/Documentation/1.20.x/tags/ — passage
- [S79] TerraFirmaCraft/TerraFirmaCraft: src/main/java/net/dries007/tfc/config/ServerConfig.java — https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/src/main/java/net/dries007/tfc/config/ServerConfig.java — passage
- [S80] TerraFirmaCraft/TerraFirmaCraft: 2796 — https://github.com/TerraFirmaCraft/TerraFirmaCraft/issues/2796 — passage
- [S81] TerraFirmaGreg-Team/Modpack-Modern: defaultconfigs/tfc-server.toml — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/blob/dev/defaultconfigs/tfc-server.toml — passage
- [S82] TerraFirmaGreg-Team/Modpack-Modern: kubejs/server_scripts/tfg/overworld/tags.overworld.js — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/blob/dev/kubejs/server_scripts/tfg/overworld/tags.overworld.js — passage
- [S83] TerraFirmaGreg-Team/Core-Modern: src/main/java/su/terrafirmagreg/core/common/event/MobSpawnEvent.java — https://github.com/TerraFirmaGreg-Team/Core-Modern/blob/dev/src/main/java/su/terrafirmagreg/core/common/event/MobSpawnEvent.java — passage
- [S84] TerraFirmaGreg-Team/Modpack-Modern: kubejs/data/primitive_creatures/forge/biome_modifier/all_biome_modifier.json — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/blob/dev/kubejs/data/primitive_creatures/forge/biome_modifier/all_biome_modifier.json — passage
- [S85] TerraFirmaGreg-Team/Modpack-Modern: kubejs/data/species/forge/biome_modifier/add_ghoul.json — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/blob/dev/kubejs/data/species/forge/biome_modifier/add_ghoul.json — passage
- [S86] TerraFirmaGreg-Team/Modpack-Modern: kubejs/server_scripts/tfg/worldgen/events.entities.js — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/blob/dev/kubejs/server_scripts/tfg/worldgen/events.entities.js — passage

## Audit

| Claim | Verdict | Note |
|---|---|---|
| C102 | SUPPORTED |  |
| C103 | SUPPORTED |  |
| C104 | SUPPORTED |  |
| C105 | SUPPORTED |  |
| C107 | SUPPORTED | re-counted from the recorded tree dump: no `mods` path, 0 .java files |
| C108 | SUPPORTED |  |
| C109 | SUPPORTED |  |
| C110 | SUPPORTED |  |
| C112 | PARTIAL | "all registries" not shown by one file; narrowed to the registries the file lists |
| C113 | SUPPORTED | re-ran the grep on machines.js: 9 |
| C114 | SUPPORTED | 2,081 and the structure folders re-counted from the recorded tree dump |
| C117 | SUPPORTED | project names re-read from the recorded lock file |
| C118 | SUPPORTED |  |
| C119 | SUPPORTED |  |
| C120 | SUPPORTED | environment, particle, command counts re-counted from the recorded tree dump |
| C121 | SUPPORTED |  |
| C123 | PARTIAL | quote shows only the plugin class; 19 file names confirmed in the git tree; "exposes to scripts" narrowed to what the names show; the quote check could not fetch this file, the auditor read the raw file and the quote is there |
| C125 | SUPPORTED |  |
| C126 | SUPPORTED |  |
| C128 | SUPPORTED |  |
| C129 | SUPPORTED |  |
| C130 | SUPPORTED |  |
| C131 | SUPPORTED |  |
| C132 | PARTIAL | "could not" goes past the PR text (the maintainer also offered a KubeJS route, C133); narrowed |
| C133 | SUPPORTED | second route read in the same comment |
| C134 | PARTIAL | the performance reason belongs to the caching change, not to the move of fishing nets; narrowed |
| C137 | PARTIAL | PR says "ore processing file" (the deleted file is OreRecipeHandlerMixin.java), not "recipes"; narrowed |
| C139 | PARTIAL | narrowed after C134 and C137: performance as a reason for moving and "plain recipe data" are not in the sources |
| C140 | SUPPORTED |  |
| C141 | PARTIAL | "not loaded by the game" is not in the README; removed |
| C142 | SUPPORTED |  |
| C144 | SUPPORTED |  |
| C145 | PARTIAL | "small", "Java" and "written to support the pack's layout" are not in the README; narrowed |
| C201 | SUPPORTED |  |
| C203 | SUPPORTED |  |
| C204 | SUPPORTED |  |
| C205 | SUPPORTED |  |
| C206 | SUPPORTED |  |
| C208 | PARTIAL | the quote shows the codec field only; the negative about the toml config is not in the source and was removed |
| C209 | SUPPORTED |  |
| C210 | SUPPORTED |  |
| C212 | SUPPORTED |  |
| C213 | SUPPORTED |  |
| C214 | SUPPORTED |  |
| C215 | SUPPORTED |  |
| C218 | PARTIAL | source says "a block can grow in", not "crop and fruit"; narrowed |
| C220 | SUPPORTED |  |
| C221 | SUPPORTED |  |
| C222 | SUPPORTED |  |
| C223 | SUPPORTED |  |
| C225 | SUPPORTED |  |
| C226 | SUPPORTED |  |
| C227 | SUPPORTED |  |
| C228 | PARTIAL | source says "a map of Rock IDs to Layer IDs"; "the layer below" not in the passage read |
| C229 | PARTIAL | that a biome tag can express climate is an unstated premise; narrowed |
| C230 | PARTIAL | generator and biome source are not in the S28 quote (S15 lists `type` and `biome_source` `tfc:overworld`); narrowed to the quote |
| C231 | PARTIAL | "only" is not shown by the quote; narrowed |
| C232 | PARTIAL | "an API hook addons can use" is not in the source; narrowed to what the handler does |
| C233 | SUPPORTED |  |
| C234 | PARTIAL | "ship in the same jar" is not in the README; narrowed |
| C235 | SUPPORTED |  |
| C236 | PARTIAL | option 2 rested on "addons can use the event", removed from C232; narrowed |
| C237 | PARTIAL | one project presented as a class; narrowed |
| C239 | SUPPORTED |  |
| C240 | PARTIAL | the mixin is conditional on useTfgOverworldPipeline(); condition added |
| C241 | SUPPORTED |  |
| C242 | SUPPORTED |  |
| C244 | SUPPORTED | repo root listed: LICENSE.txt and HEADER.txt only |
| C246 | PARTIAL | "not published in the repo" was not checked beyond the README; narrowed |
| C247 | SUPPORTED |  |
| C248 | SUPPORTED |  |
| C249 | SUPPORTED |  |
| C250 | SUPPORTED |  |
| C251 | PARTIAL | the compatibility clause applies only to a derivative combined with a work under the compatible licence; condition added |
| C301 | SUPPORTED |  |
| C302 | SUPPORTED |  |
| C303 | SUPPORTED |  |
| C304 | SUPPORTED |  |
| C305 | SUPPORTED |  |
| C306 | SUPPORTED |  |
| C307 | SUPPORTED |  |
| C308 | SUPPORTED |  |
| C309 | SUPPORTED |  |
| C310 | SUPPORTED |  |
| C311 | SUPPORTED |  |
| C312 | PARTIAL | quote says "high pressure Steam Power"; narrowed |
| C313 | SUPPORTED |  |
| C314 | PARTIAL | the HP rock crusher and miner recipes have no Black Steel; "the other HP steam machines" narrowed |
| C315 | SUPPORTED |  |
| C316 | SUPPORTED |  |
| C317 | PARTIAL | "ends in" is not shown by the quote; narrowed |
| C318 | PARTIAL | narrowed after C312 and C314: high pressure steam only, and not every HP machine |
| C319 | SUPPORTED | order_index read in the chapter .snbt files: 0,1,2 / 0,1,2 / 0,1 within their groups |
| C320 | SUPPORTED |  |
| C321 | SUPPORTED |  |
| C322 | SUPPORTED |  |
| C323 | PARTIAL | quote says "automate your old TFC bloomery"; "GregTech" and "replaced" not in it; narrowed |
| C324 | PARTIAL | "Create parts" not named in the quest text; narrowed to what it says |
| C325 | PARTIAL | the file adds forge hammer recipes; that the anvil route is taken away is not shown; narrowed |
| C326 | SUPPORTED |  |
| C327 | SUPPORTED |  |
| C328 | SUPPORTED |  |
| C329 | SUPPORTED |  |
| C330 | SUPPORTED |  |
| C331 | SUPPORTED |  |
| C332 | SUPPORTED |  |
| C333 | SUPPORTED |  |
| C334 | SUPPORTED |  |
| C335 | SUPPORTED |  |
| C336 | SUPPORTED |  |
| C337 | SUPPORTED |  |
| C338 | SUPPORTED |  |
| C339 | PARTIAL | the MV ethylene path is not in the cited file; removed |
| C340 | SUPPORTED | Mars confirmed by quests.extreme_voltage.aes_insulation.desc in the same file |
| C341 | PARTIAL | the source does not say glacian wool comes from a Mars animal; narrowed |
| C342 | SUPPORTED |  |
| C343 | SUPPORTED |  |
| C344 | SUPPORTED |  |
| C345 | SUPPORTED |  |
| C346 | SUPPORTED |  |
| C348 | SUPPORTED |  |
| C349 | PARTIAL | "then leaves" goes past the named claims (C324 keeps the blast furnace through the Steam Age); narrowed |
| C350 | PARTIAL | "few" and "most uses are optional" assume an exhaustive survey that was not done; narrowed, and C341 no longer says Mars |
| C351 | SUPPORTED |  |
| C352 | SUPPORTED |  |
| C354 | PARTIAL | "tying survival to progression pace" is interpretation; removed |
| C401 | SUPPORTED | the quote check could not fetch this file; the auditor read the raw file and the quote is there |
| C402 | SUPPORTED |  |
| C403 | SUPPORTED |  |
| C404 | SUPPORTED |  |
| C405 | PARTIAL | "vanilla inputs are not produced by TFC worldgen" is a premise no claim supports; narrowed to what C401, C404, C416 show |
| C406 | SUPPORTED |  |
| C407 | PARTIAL | "main" is not shown by the source; narrowed |
| C408 | SUPPORTED |  |
| C409 | SUPPORTED | same file also has removeItemsOnInventoryOpen: true |
| C410 | SUPPORTED |  |
| C411 | SUPPORTED |  |
| C412 | SUPPORTED |  |
| C413 | SUPPORTED |  |
| C414 | PARTIAL | one file read; "one recipes.removes.js per mod folder" not shown; narrowed |
| C416 | SUPPORTED | replaceFoodStats call is in S70, line 1282 |
| C418 | SUPPORTED |  |
| C419 | SUPPORTED |  |
| C420 | PARTIAL | that bread, cooked_beef and apple have no definition is not in any named claim; narrowed |
| C421 | SUPPORTED | the quote check could not fetch this file; the auditor read the raw file and the quote is there |
| C422 | PARTIAL | "reachable only through TFC's own recipe" not checked; removed; the quote check could not fetch this file, the auditor read the raw file and the quote is there |
| C423 | PARTIAL | only the zombie file is cited; "(and drowned)" removed |
| C425 | SUPPORTED |  |
| C427 | SUPPORTED |  |
| C430 | PARTIAL | "re-enables" assumes the item then works as food under TFC, which the source does not show; narrowed |
| C431 | PARTIAL | one biome file presented as all biomes; narrowed to plains |
| C432 | SUPPORTED |  |
| C433 | SUPPORTED |  |
| C434 | SUPPORTED |  |
| C435 | SUPPORTED |  |
| C436 | SUPPORTED |  |
| C437 | SUPPORTED |  |
| C438 | SUPPORTED |  |
| C440 | SUPPORTED |  |
| C441 | SUPPORTED |  |
| C442 | SUPPORTED |  |
| C443 | SUPPORTED |  |
| C444 | PARTIAL | no jellies mob is added to the tag in the cited file; removed |
| C445 | SUPPORTED |  |
| C446 | SUPPORTED | the listener checks only the block below, for any living entity, not the monster tag |
| C447 | SUPPORTED |  |
| C448 | SUPPORTED | the quote check could not fetch this file; the auditor read the raw file and the quote is there |
| C449 | PARTIAL | C445 covers one overworld biome; per-dimension placement and "rather than KubeJS events" do not follow from the named claims; narrowed |
| C450 | PARTIAL | one file read out of 353 server scripts; "the only ... in TFG server scripts" narrowed to that file |
| C451 | SUPPORTED |  |

Label verdict: open — two gaps under Q2 would change the Summary if they resolved the unfavourable way (the unpublished terms covering the TFC 1.7.10-derived code decide whether the fork option exists; vein code was not read, and the claim that ores have no climate gate carries recommendation 3), and sub-questions (5) and (6) of the question as asked are not gathered.
Quote check: Quote check terrafirmagreg-anatomy-2026-11-23.md: 136 quoted claim(s): 130 found, 0 NOT FOUND, 6 unfetchable, 0 no source

## Disposition

| Recommendation | State | Where |
|---|---|---|
| Разделить слои так же: данные и рецепты — в папке kubejs сборки, поведение — в Java-ядре селекции с плагином KubeJS | deferred | unfiled: this run was told to touch only the report file and file nothing; the planning session that ordered it decides whether it becomes an issue or a design decision |
| Не рассчитывать на «генерацию TFC без TFC»: зависеть от TFC целиком, написать свою модель климата либо форкнуть код под копилефтом; при зависимости от TFC привязывать растения и животных его механизмами | deferred | unfiled: this run was told to touch only the report file and file nothing; the planning session that ordered it decides whether it becomes an issue or a design decision |
| Климатический уникум закладывать как растение или животное, а не как руду | deferred | unfiled: this run was told to touch only the report file and file nothing; the planning session that ordered it decides whether it becomes an issue or a design decision |
| Сцеплять слои через одну общую систему материалов и переопределение рецептов; схему «ранняя металлургия уходит» не копировать, обязательные поздние био-входы закладывать намеренно | deferred | unfiled: this run was told to touch only the report file and file nothing; the planning session that ordered it decides whether it becomes an issue or a design decision |
| Запрет ванильной еды строить в два слоя: неработающая еда без определения плюс явное удаление предметов и лута; отдельно решить про сундуки с лутом | deferred | unfiled: this run was told to touch only the report file and file nothing; the planning session that ordered it decides whether it becomes an issue or a design decision |
| Мобов контролировать тегом и слушателем спавна в Java-ядре плюс своими списками спавна в JSON биомов; спавн чужих модов гасить переопределением biome modifier | deferred | unfiled: this run was told to touch only the report file and file nothing; the planning session that ordered it decides whether it becomes an issue or a design decision |
