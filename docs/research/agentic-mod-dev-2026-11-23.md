---
topic: agentic-mod-dev
tags: area:pipeline, research
source_provenance: /research invocation from the farming-evolution planning session, 2026-11-23
status: answered
audit: 2026-11-23 — 170 claims: 143 SUPPORTED, 25 PARTIAL, 0 UNSUPPORTED, 2 UNCHECKABLE
---

## Question

Как ИИ-агенты разрабатывают моды и сборки Minecraft и игры вообще, и какая обратная связь им нужна. Вопросы: (1) опубликованный опыт 2025–2026: кто и как пишет моды Minecraft или целые игры кодовыми агентами, что получилось, где ломается; (2) цикл обратной связи для Java-мода: Gradle, юнит-тесты, GameTest Framework, headless-сервер, datagen, запуск клиента без человека, автоскриншоты и их проверка агентом; (3) обратная связь для скриптов и данных: проверка KubeJS-скриптов, рецептов, датапаков, квестов без ручного запуска игры; линтеры, валидаторы JSON-схем; инструменты экспорта рецептов и графа прогрессии; (4) проверка баланса и проходимости прогрессии без человека: симуляции, анализ графа рецептов, боты (Mineflayer и подобные), что реально работает; (5) инструменты: MCP-серверы и плагины для Minecraft, декомпилированные исходники и маппинги как контекст, документация загрузчиков; (6) что остаётся за человеком: плейтест, темп, визуальная оценка; (7) рекомендуемая схема конвейера для проекта farming-evolution и что строить первым. Контекст: модпак вокруг собственного Java-мода селекции растений и животных + KubeJS, датапаки, квесты; вся разработка агентами Claude Code, один автор без опыта моддинга; версия Minecraft не зафиксирована; Windows 11; термины в GLOSSARY.md.

Asked seven topics in one run, answering them as four sub-questions because the task required a single report: topics 1 and 6 are Q1, topic 2 is Q2, topics 3 and 4 are Q3, topic 5 is Q4, and topic 7 is the Summary (a synthesis, not a sub-question). The skill's rule is to split more than four sub-questions into several runs; this run folds instead, and three of the four ledgers reached the 20,000-character cap, so each sub-question is covered less deeply than a run of its own would cover it. What was cut is listed under Coverage and Open questions.

Claims are kept in English, the language of their sources, so that each claim says no more than its quote; the Summary and the notes are in Russian.

An earlier file, `docs/research-baseline.md` (2026-05-24), covers mod availability and AgriCraft on 1.20.1 Forge. This report does not re-check it; what it adds is the development-pipeline side, and one fact that bears on the baseline's implicit version choice: on 1.20.1 the maintained loader is MinecraftForge, NeoForge docs do not cover it [C452], and the JUnit path was not found documented for it [C217].

Sub-questions:
Q1 — What published experience exists from 2025–2026 of coding agents writing Minecraft mods/modpacks or whole games: what worked, where it breaks, and what the authors say must stay with a human?
Q2 — What automated, human-free feedback loop is available for a Java Minecraft mod: Gradle build, JUnit, GameTest, headless server, datagen, client launch without a human, automated screenshots?
Q3 — How can KubeJS scripts, recipes, datapacks and quests be checked without a human launching the game, and how can balance and completability of a progression be checked without a human?
Q4 — What tooling gives a coding agent accurate Minecraft-modding context and control: MCP servers and skills, decompiled sources and mappings, loader documentation?

## Summary

**Рекомендация.** Строить конвейер как лестницу проверок, каждая ступень которой заканчивается кодом выхода или разбираемым логом, и первой построить её «сквозной скелет» на пустом моде — до любого контента. Что именно ставить на ступени:

1. **Контекст до кода.** Зафиксировать версию Minecraft и загрузчика и сказать агенту, какая версия документации действует: разброс версий — названная автором специфическая трудность Minecraft для LLM [C102], вики Fabric говорит, что LLM путают версии и платформы [C418], документация NeoForge версионирована и не покрывает 1.20.1 [C448], [C452]. Вики Fabric советует указывать агенту на код Minecraft и мода до реализации; документация NeoForge называет чтение исходников необходимым для многих возможностей, и после импорта Gradle они лежат в рабочем каталоге [C420], [C415], [C413], [C422]. Исходники нельзя коммитить в публичный репозиторий [C408]. Повторяющуюся ошибку агента сразу записывать в файл правил [C106].
2. **Сборка и юнит-тесты.** Сборка ловит ошибки API уровня компиляции [C105], но не неверное поведение в игре [C147]. ModDevGradle поддерживает JUnit-тесты, которые обращаются к классам Minecraft, и тесты с живым сервером [C213], [C214]. Отсюда следствие для архитектуры: логику ядра селекции (наследование признаков, пороги) держать проверяемой юнит-тестами, без запуска мира.
3. **Datagen.** Запуск data-run поднимает мод до событий реестров, пишет файлы и выходит [C247], то есть скриптуется без человека; смотреть его выход через git diff — предложение этого отчёта, как практику его не описывает ни один прочитанный источник [C250].
4. **GameTest-сервер.** `gameTestServer` возвращает кодом выхода число упавших обязательных тестов — на NeoForge 1.21.1, в текущей документации и на Forge 1.20.x [C201], [C324], [C203]. Известные ловушки: принудительный выход в NeoGradle, пустой набор тестов, тест без явного успеха [C204], [C210]. Формат тестов меняется между 1.21.4 и 1.21.5 [C209].
5. **Загрузка всей сборки со сканированием лога.** Это прочитанная практика проверки KubeJS и данных в реальной сборке: Monifactory в CI поднимает game-test-сервер и убивает его на первой строке лога с ошибкой KubeJS [C319], [C320]; из пяти проверенных крупных сборок workflow с проверкой содержимого, судя по именам файлов, есть только у двух [C323]. Одного кода выхода мало: в KubeJS 2001 исключения рецептов и разбора JSON в обработчиках события рецептов только логируются, а обработчик пропускается [C309], [C314]; есть сводная строка с числом упавших рецептов [C310], [C311]; ошибка разбора файла датапака в одном разобранном случае — строка ERROR с префиксом «Couldn't parse data file» [C332]. Статические средства здесь слабы: типизация ProbeJS v8 не снимается без клиента с GUI и бывает неверной [C302], [C304], [C307]; в монорепозитории Spyglass нет пакета, названного lint-CLI для датапаков [C329]; знают ли инструменты ванильных схем модовые типы рецептов, не проверялось [C335].
6. **Граф прогрессии — писать самим.** Готового инструмента, который по рецептам и квестам отвечает «достижим ли каждый предмет, нет ли циклов», не найдено (запросы — в разделе Disconfirming evidence). Вход для своего есть: `/kubejs export` выгружает все рецепты после скриптов и идентификаторы реестров [C315], [C316], [C317]; работает ли команда из консоли выделенного сервера, не проверено [C318]. Для проекта это ступень первой важности, потому что взаимная зависимость (тех-уровень ↔ линия) — это свойство графа, а не отдельного рецепта.
7. **Клиент и картинка.** В прочитанной документации NeoForge и Forge не найдено официального API скриптованных клиентских тестов со скриншотами; у Fabric оно есть, но с 1.21.4 [C253], [C230], и может падать на GitHub Actions [C231], [C232]. На NeoForge/Forge это сторонние средства: HeadlessMC и dev-моды с MCP-сервером внутри клиента [C235], [C438], [C439], [C442]; путь через xvfb в этих средствах — только Linux [C244], [C228]. Автор Godogen отдаёт оценку скриншотов отдельной модели, а не агенту, писавшему код [C119]; в GameDevBench визуальная обратная связь подняла GPT-5.4 с 41.1% до 52.0% [C138], а задачи с 2D-графикой решаются хуже игровых: 33.0% против 51.4% [C137].

**Что не работает.** Внешние боты-игроки (Mineflayer, Voyager) для сборки на Forge/NeoForge: мейнтейнер Mineflayer говорит, что поддерживаются только серверы, принимающие ванильный клиент; поддержка модового рукопожатия — несмёрженный PR, который сам не обещает игровой поддержки; Voyager (2023) единственным в своём сравнении дошёл до алмазного уровня ванильного дерева [C341], [C344], [C345], [C346], [C347], [C350]. Симуляторов баланса для современных сборок не найдено, только абстрактная академическая работа [C339]. У трёх основных MCP-серверов для моддинга 49, 73 и 8 звёзд, сервера от команды загрузчика не найдено; это удобство поверх локальных исходников, не источник истины [C433].

**Что остаётся за человеком.** В отчётах о плагине Paper и о порте с Amiga неверное поведение нашёл человек, смотревший на игру или игравший в неё [C147]; ощущение — класс дефектов, который не падает и не виден в диффе [C129]; доводка ощущения заняла около трёх дней после играбельной версии за вечер [C127]. За человеком авторы оставляют игру и оценку «ощущается ли правильно», вкус и отдельные решения, необратимые действия публикации [C148]. Инструменты «глаз и рук» сужают разрыв, но не закрывают его [C149].

**Опубликованный опыт.** По Minecraft найдены только два малых мода и один плагин; ничего про контентный мод с KubeJS, датапаками и квестами в масштабе сборки [C150], поэтому перенос из опыта Godot и браузерных игр — по аналогии. В бенчмарке GameDevBench лучший агент решает 53.8% задач [C136].

**Версия.** При обязательном KubeJS самая новая пригодная версия по данным Modrinth — 26.1.2 на NeoForge (CurseForge не проверялся) [C457]; KubeJS выходит также для 1.21.1 (NeoForge) и 1.20.1 (Forge, Fabric) [C456]. Для 1.21.1 и 26.1.2 на NeoForge есть версионированная документация NeoForge, документированный game-test-сервер и JUnit-интеграция ModDevGradle; для 1.20.1 загрузчик — MinecraftForge, документация — Forge, а страница legacy-плагина ModDevGradle юнит-тесты не упоминает [C458]; на 26.x имена параметров идут от Mojang, но javadoc от Parchment нет [C412]. Выбор версии по доступности модов — вне этого отчёта.

**Что строить первым.** Одну команду-обёртку над ступенями 2–5 на пустом моде: сборка, один JUnit-тест, один GameTest, загрузка сервера со сборкой и разбор лога, итог в виде кода выхода и короткой сводки для агента [C254]. Вторым — выгрузку рецептов и проверку достижимости графа (ступень 6). Клиентские скриншоты — третьими, когда появится что рисовать.

## Findings

### Q1 — What published experience exists from 2025–2026 of coding agents writing Minecraft mods or whole games, and what stays with a human — answered with a gap

- **C101** [quoted] Max Leiter (blog, 2025-06-21) made two Minecraft 1.12.2 mods using Codex CLI, Claude Code and Cursor and says he wrote no Java himself — "wrote no Java in the making of this project." — [S1], section "The Solution: more mods"
- **C102** [quoted] Leiter names version spread as the Minecraft-specific difficulty for LLMs: many versions with breaking changes, and models trained across all of them — "Minecraft is tricky because it has many versions with many breaking changes, and the LLMs are trained across all the versions." — [S1], section "Learnings for developing"
- **C103** [quoted] Leiter attributes part of his success to targeting an old, heavily modded version (1.12.2) — "I think one reason I had a lot of success is I was targeting a fairly old version of Minecraft known for its mods (1.12.2)." — [S1], section "Learnings for developing"
- **C105** [quoted] In Leiter's multiplayer mod the model missed a client-side-only annotation and corrected it itself once it ran a build — "it fixed the issue on its own after running a build." — [S1], section "Unnamed Project Management mod"
- **C106** [quoted] Leiter recommends writing rules files as soon as a recurring agent mistake is seen — "Write rules files when you see common mistakes." — [S1], section "Learnings for vibecoding"
- **C108** [quoted] In an undated write-up of a Paper 1.21.4 server plugin built with Claude Code from a spec file, the first build had recipes that did not match what the server owner wanted; this surfaced only from his photos of the in-game crafting screen — "Ben sent photos of his crafting screen — and the recipes were completely wrong." — [S2], section "Iteration in the Real World"
- **C109** [quoted] The same write-up credits the result to a loop between human judgement and agent execution speed, with the human holding the sense of how the weapons should feel — "Ben knew what he wanted the weapons to feel like." — [S2], section "The Collaborative Pattern"
- **C110** [quoted] The mcmodding-mcp server (Fabric/NeoForge docs for AI assistants) presents itself as a replacement for reliance on possibly outdated training data — "Instead of relying on potentially outdated training data, your AI assistant can search real documentation, find code examples, and explain concepts accurately." — [S3], section "What is this?"
- **C111** [quoted] The Minecraft Agent Skills bundle (13 skills for Codex and Claude Code, incl. modding, datapack, testing; README says reviewed against Minecraft 26.3 on 2026-10-29) says its validators cover file structure and common mistakes, and leaves test builds and in-game behaviour to the user's project — "Bundled validators check file structure and common mistakes. Test builds and in-game behavior in your Minecraft project." — [S4], section "Usage" (sentence is line-wrapped in the source)
- **C113** [quoted] NeoForge's 26.1 release post (2026-03-24) says Mojang removed obfuscation in 26.1 — "Mojang has removed obfuscation in 26.1." — [S5], section "Removal of obfuscation"
- **C116** [inference] The NeoForge 26.1 release (March 2026) removed obfuscation, and two tools read in this run present themselves as an answer to outdated training data or state the Minecraft version they were reviewed against; that the version drift Leiter described in 2025 was still a problem in 2026 is those tools' premise, not a measured finding — from [C102], [C110], [C111], [C113]
- **C117** [quoted] The author of Godogen (Claude Code skills that generate complete Godot games; Show HN 2026-03-16, about a year of work, four rewrites) says the model produced Python idioms in GDScript that do not compile — "LLMs barely know GDScript. It has ~850 classes and a Python-like syntax that will happily let a model hallucinate Python idioms that fail to compile." — [S6], post text, point 1
- **C118** [quoted] The Godogen author names as the core failure that Claude Code cannot see its output: code compiles while the scene is visibly broken — "The core issue is Claude Code has no way to see what it's producing — code compiles fine but assets are floating, paths lead nowhere, layouts are garbage." — [S7], comment 47404330 by htdt
- **C119** [quoted] Godogen's answer is a separate vision model as QA that sees only screenshots, on the ground that the coding agent is biased toward its own output — "A coding agent is inherently biased toward its own output." — [S6], post text, point 3
- **C120** [quoted] Counter to C117: a commenter building a Godot game with Claude Code in a human-in-the-loop way reports fine GDScript from Opus 4.5/4.6 and Sonnet 4.6 with only a CLAUDE.md and plan mode — "just a CLAUDE.md file, the project itself, and going through plan mode before each change." — [S8], comment 47407325 by Ariarule
- **C121** [quoted] The same commenter still sees occasional engine-version confusion (Godot 3 code offered for Godot 4) — "Every once in a while there will be some confusion that I get something appropriate for Godot 3 instead of Godot 4, but never Python despite the similarities of the language." — [S8], comment 47407325 by Ariarule
- **C122** [quoted] A commenter who used Claude for parts of Godot games says unsteered output degrades and non-trivial projects become intractable — "you have to steer and curate the output, otherwise non-trivial projects become intractable past a certain point, and you have a mountain of bugs to sort through." — [S9], comment 47406299 by _QrE
- **C123** [quoted] A commenter in the Godogen thread argues that reducing human effort in the loop is the wrong goal and is why the output is slop — "I think minimizing the amount of human effort in the loop is the wrong optimization, and it's the reason we end up with "slop"." — [S10], comment 47405190 by rybosworld
- **C124** [quoted] Counter to "the agent cannot play": a commenter reports the Claude agent in Claude Code Desktop can play the game he is building — "the Claude agent there can actually PLAY the game. That's a huge unlock." — [S11], comment 47415287 by gneuron
- **C125** [quoted] Rabah S. (blog, HN 2026-09-03) had Claude Code port his 1993 Amiga game to Godot, shipped on iOS/Android; he describes his own role as asking, playing every night, saying what felt wrong and making a few decisions — "I asked for it, played the result every night, said what felt wrong, and made the few decisions that needed somebody who was there in 1993." — [S12], introduction
- **C126** [quoted] In that port the agent had CLI flags to load levels, script input, dump state and take screenshots, plus headless compile and level-build checks; screenshots were judged by the human and nothing checked feel — "What wasn’t automated: there was no image comparison on the modern port (it took screenshots, I looked at them), and nothing checked whether the game felt right." — [S12], section before "Step one: 34,000 lines of C++ in an evening"
- **C127** [quoted] After a playable port appeared in one evening, getting the feel right took about three more days of human-reported fixes — "Getting it to feel right took about three days after that: jump arcs and trampoline timing, and hit detection that rewards mashing, fixed in batches on July 2nd, 3rd and 4th." — [S12], section "Step one"
- **C129** [quoted] He describes a class of defect that neither crashes nor shows in a diff, only in how the game feels — "Nothing crashes, it just feels wrong forever, and you won’t find it by reading the diff." — [S12], section "Same units, same tick"
- **C130** [quoted] In the same project the human kept logins and every submit/publish/price/release action while the agent filled store forms — "I do the login, and I press anything that submits, publishes, prices or releases. It fills in the forms." — [S12], section "Then it shipped it"
- **C131** [quoted] T. Vilas Boas (dev.to, posted Oct 4, edited Nov 21; Claude Code, one-day browser game) reports the agent ran a scripted Playwright bot through the game instead of trusting a clean compile — "Rather than treating a clean compile as proof, the agent started Playwright in headless Chromium and played" — [S13], section "Autonomous Playwright E2E Testing & Navigation"
- **C132** [quoted] He adds that this bot run was a single session, not a repeatable test suite — "That walk is one session. It is not a suite you can re-run from CI." — [S13], section "Autonomous Playwright E2E Testing & Navigation"
- **C133** [quoted] In that project a feel complaint came from the human; the model then found the cause in the sprites after checking that the physics parameters were identical — "Rather than blindly tweaking speed variables, the model verified that the physics parameters were mathematically identical." — [S13], section "Diagnosing Character Feel"
- **C135** [snippet] An itch.io jam postmortem ("four days, two people, two coding agents", Codex and Claude Code) reportedly says the agents could not judge whether a hit felt good, that this was most of the work, and that difficulty was never fixed — "the agents could not judge whether a hit felt good, and that this turned out to be most of the work" — [S14], search-tool summary; page returns HTTP 403 to curl and WebFetch
- **C136** [quoted] GameDevBench (arXiv v2, 30 Jun 2026; 333 Godot tasks from tutorials): the best agent and method solved 53.8% of tasks — "Agents struggle with game development, with the best agent and method solving only 53.8% of tasks." — [S15], abstract
- **C137** [quoted] In GameDevBench success falls with visual content: 51.4% on gameplay tasks against 33.0% on 2D graphics tasks — "with average success rate dropping from 51.4% on gameplay-oriented tasks to 33.0% on 2D graphics tasks" — [S15], abstract
- **C138** [quoted] Giving the agent image/video feedback raised GPT-5.4 from 41.1% to 52.0% in GameDevBench — "increasing GPT-5.4's performance from 41.1% to 52.0% when given visual feedback" — [S15], abstract
- **C139** [quoted] GameDevBench's error analysis names engine-pattern mistakes as a consistent failure: wrong node placement, dropped signals, resources on the wrong element — "Agents frequently add nodes to incorrect levels in the tree, drop necessary signals, or assign resources to the wrong elements." — [S16], section 4.3
- **C142** [quoted] In an Ask HN thread (2026-01-09) a commenter says none of the many AI-built games of the vibe coding jam were good — "During the vibe coding jam, there were so many games build by AI, but none of them were any good" — [S17], comment 46552524 by falloutx
- **C144** [quoted] Counter-claim: MeinMMO (2026-11-17) reports a solo developer said to have built the Steam autobattler Slotbound alone with AI and without prior knowledge; the revenue figure is the developer's own post on X — "Denn die Person hat das Spiel allein und völlig ohne Kenntnisse mithilfe von KI entwickelt." — [S18], article lead
- **C147** [inference] A build caught a compile-level API mistake in Leiter's mod, but a clean compile does not show wrong in-game behaviour; in the Paper-plugin and Amiga-port accounts the wrong behaviour was found by a human looking at or playing the running game — from [C105], [C108], [C111], [C118], [C126], [C129]
- **C148** [inference] What the authors keep with a human: judging feel and fun by playing, looking at screenshots where no visual QA exists, taste and a few design decisions, and irreversible publish actions — from [C109], [C123], [C125], [C126], [C127], [C130], [C142]
- **C149** [inference] Giving the agent eyes and hands (screenshots, scripted input, state dumps, a playing bot) narrows but does not close the gap: visual feedback raised a benchmark score, while feel complaints in these accounts still originate from the human — from [C119], [C124], [C126], [C131], [C132], [C133], [C138]
- **C150** [inference] The Minecraft accounts found in this run cover two small mods and one server plugin; nothing found covers the planned shape (custom content mod plus KubeJS, datapacks and quests at modpack scale), so transfer from the Godot and browser-game accounts is by analogy — from [C101], [C108], [C117], [C125]

### Q2 — What automated, human-free feedback loop is available for a Java Minecraft mod — answered with a gap

- **C201** [quoted] NeoForge 1.21.1 docs: the game test server returns an exit code equal to the number of required, failed Game Tests — "The build server returns an exit code of the number of required, failed Game Tests." — [S19], Game Test Server Run Configuration
- **C203** [quoted] Forge 1.20.x docs describe the same Game Test Server and exit code; other run configs need `forge.enableGameTest` — "If another run configuration should run Game Tests, then the `forge.enableGameTest` property must be set to `true`." — [S20], Enabling Game Tests in Other Run Configurations
- **C204** [quoted] Counter to C201 (NeoForge 1.21.1 docs, NeoGradle only): with the default forced exit the Game Test Server task always fails until `setForceExit false` is set — "NeoGradle sets by default a force exit on run tasks such that any subprojects are not executed in sequence. However, as such, the Game Test Server will always fail." — [S19], Important information on NeoGradle
- **C205** [quoted] NeoForge 1.21.1: tests are Java methods annotated `@GameTest`, registered by `@GameTestHolder` or `RegisterGameTestsEvent` — "There are two methods of doing so: via the `@GameTestHolder` annotation or `RegisterGameTestsEvent`." — [S19], Registering a Game Test
- **C206** [quoted] NeoForge 1.21.1: every Game Test needs a structure template stored as an `.nbt` file — "The template must be stored as an `.nbt` file within `data/<namespace>/structure`." — [S19], Structure Templates
- **C207** [quoted] Current NeoForge docs: a test is a test instance (data file) linking test data, environment and a registered function — "All test instances are located within `data/<namespace>/test_instance/<path>.json`." — [S21], Registering The Test Instance
- **C209** [measured] The NeoForge docs switch from annotation tests to test instances between 1.21.4 and 1.21.5: the 1.21.4 page has 19 `@GameTest` and 0 `test_instance`, the 1.21.5 page 0 and 7 — Own measurements, M201
- **C210** [quoted] Forge 1.18+ community guide (maintainer gist): a Game Test with no explicit success times out and fails; a run with no registered tests crashes — "In fact, running `gameTestServer` with no registered game tests will exit with a crash." — [S22], Running Game Tests
- **C212** [quoted] ModDevGradle (NeoForge) supports four run types: client, data, gameTestServer and server — "Every run must have a type. Currently, the supported types are `client`, `data`, `gameTestServer`, `server`." — [S23], Runs
- **C213** [quoted] ModDevGradle supports JUnit tests that reference Minecraft classes, enabled by `neoForge { unitTest { enable() ... } }` — "On top of gametests, this plugin supports unit testing mods with JUnit." — [S23], Unit testing with JUnit
- **C214** [quoted] With the NeoForge `testframework` dependency a JUnit test can receive a running `MinecraftServer` via `@ExtendWith(EphemeralTestServerProvider.class)` — "With the NeoForge test framework, you can run your unit tests in the context of a Minecraft server:" — [S23], Loading a server
- **C215** [quoted] Counter to C213: in vanilla/NeoForm mode (no NeoForge) `unitTest` failed for a user in 2024-10 (issue closed) — "The unit testing plugin requires a 'junit' run-type to be made available by NeoForge. Available run types: [client, data, server]" — [S24], issue body
- **C216** [quoted] ModDevGradle has a legacy plugin for MinecraftForge 1.17–1.20.1 — "that adds support for developing mods against MinecraftForge and Vanilla Minecraft versions 1.17 up to 1.20.1." — [S25], intro
- **C217** [measured] LEGACY.md (Forge 1.20.1 path) does not mention JUnit or unit tests: the only match for test/junit is the `testproject` example name — Own measurements, M202
- **C218** [quoted] Fabric docs: plain JUnit does not work for mods, Fabric Loader JUnit is the supported plugin — "Since Minecraft modding relies on runtime byte-code modification tools such as Mixin, simply adding and using JUnit normally would not work." — [S26], Unit Testing
- **C219** [quoted] Fabric docs: registry-dependent classes need Minecraft bootstrapped in `beforeAll` (the reference test calls `SharedConstants.tryDetectVersion()` then `Bootstrap.bootStrap()`) — "We just need to initialize it a little bit to have registries working." — [S26], Setting Up Registries
- **C221** [quoted] Fabric docs (latest): server game tests run in `build`; client game tests have their own task — "Server game tests will be run automatically with the `build` Gradle task. You can run client game tests with the `runClientGameTest` Gradle task." — [S26], Running Game Tests
- **C222** [quoted] Fabric adds client game tests on top of the vanilla server-side framework — "Minecraft provides the game test framework for testing server-side features. Fabric additionally provides client game tests for testing client-side features, similar to an end-to-end test." — [S26], Game Tests
- **C223** [quoted] Client game tests run without a human: sequential, then the game closes — "When all gametests have been run, the game will be closed." — [S27], Lifecycle
- **C224** [quoted] The client test context saves a screenshot file and returns its path — "Takes a screenshot and saves it in the screenshots directory." — [S28], takeScreenshot(String)
- **C225** [quoted] The Fabric client gametest screenshot options recommend non-exact matching unless an exact match is specifically required, because the exact screenshot can vary slightly — "matching unless you specifically require an exact match, because the exact screenshot can vary slightly on different" — [S29], class javadoc
- **C227** [quoted] A missing template image is written on first run, so goldens can be bootstrapped by running the test — "If the template image is not found then the screenshot will be" — [S29], class javadoc
- **C228** [quoted] Loom's production client run task uses XVFB by default only on Linux CI — "Defaults to true only on Linux and when the" — [S30], Client
- **C229** [quoted] Fabric API's own CI runs client game tests on ubuntu-24.04 (opengl and vulkan matrix) and uploads `run/screenshots` as an artifact — "- run: ./gradlew runClientGametest --stacktrace --warning-mode=fail" — [S31], job client_test
- **C230** [measured] The `fabric-client-gametest-api-v1` module exists on fabric-api branch 1.21.4 but not on 1.21.1 or 1.20.1; its first commit is 2024-12-16 "Add experimental Client Game Test API (#4292)" — Own measurements, M203
- **C231** [quoted] Counter (Fabric docs): client game tests may fail on GitHub Actions; workaround is a JVM flag — "Currently, game test may fail on GitHub Actions due to an error in the network synchronizer." — [S26], Run Game Tests on GitHub Actions
- **C232** [quoted] Counter: open issue since 2025-02 — client game test crashes in CI on xvfb with a network synchronizer assertion — "Client game test crashes in CI on xvfb due to" — [S32], issue body
- **C233** [quoted] Counter: intermittent hang reported on Minecraft 26.2 / 26.1.2 when closing a singleplayer world with several clients on a busy machine (closed 2026-10-18) — "Now and then a client gametest hangs forever while it closes a singleplayer world with `TestSingleplayerContext.close()`." — [S33], issue body
- **C234** [quoted] Counter: open issue — screenshots larger than the window render GUIs wrongly — "the opened inventory will only take up the size of the actual window in the top-right corner" — [S34], issue body
- **C235** [quoted] HeadlessMC `--headless` launches the client with LWJGL patched so nothing is rendered — "This is achieved by patching the LWJGL library to not render anything" — [S35], Headless Launch
- **C237** [quoted] HeadlessMC offline accounts are restricted to headless CI/CD use — "Offline accounts can only be used to run the game headlessly in CI/CD pipelines." — [S36], warning box
- **C238** [quoted] hmc-specifics mods let the command line drive a running client (click, gui dump, text) and output all text rendered on the screen — "render       Outputs all text rendered on the screen." — [S37], HMC-Specifics
- **C239** [quoted] hmc-specifics can press keys, the docs show F2 producing a screenshot file — "To simulate a key press use the `key` command." — [S37], Keys and Screenshots
- **C240** [quoted] mc-runtime-test (GitHub Action) runs the production client in CI and lists 1.20.1–1.20.6 and 1.21–1.21.11 as supported for Forge, Fabric and NeoForge — "MC-Runtime-Test enables you to run the Minecraft client within your CI/CD pipelines, simplifying the testing of runtime" — [S38], intro and versions table
- **C241** [quoted] Its bundled mod only smoke-tests: join a singleplayer world, wait for chunks, quit; plus `/test runall` — "Join a single-player world." — [S38], Features; Running Your Own Tests
- **C242** [quoted] Counter: in mc-runtime-test, Forge and NeoForge GameTest discovery needs extra setup or hacks — "> Currently, Forge and NeoForge GameTest discovery may require additional" — [S38], Running Your Own Tests, tip
- **C243** [quoted] Counter: Forge and NeoForge normally disable the GameTest ticker in production clients — "Those loaders normally disable their GameTest ticker" — [S38], Running Your Own Tests
- **C244** [quoted] The action's xvfb path is Linux-only (apt-get, xvfb-run); without xvfb it launches with the `-lwjgl` stub; the action also has a Windows (pwsh) bootstrap step — "-jar headlessmc.jar launch -lwjgl --offline ${{ inputs.headlessmc-command }} ${{ inputs.modloader }} ${{ inputs.mc }}" — [S39], steps "Run game with xvfb" / "Run game"
- **C245** [inference] HeadlessMC's LWJGL patch renders nothing, so screenshots of real pixels need a run without it; mc-runtime-test offers that only through xvfb, on Linux — from [C235], [C244]
- **C246** [prior] Xvfb does not exist on Windows; a client started locally on Windows 11 opens a real window unless run under WSL2 or a container — not fetched in this run
- **C247** [quoted] NeoForge (current docs): the data run boots the mod up to registry events, writes generated files and exits; Gradle tasks are `runClientData` / `runServerData` — "writes said objects to disk, and ends the process." — [S40], Data Generation
- **C249** [quoted] Fabric Loom: datagen has a strict validation switch, off by default — "// Contains a boolean indicating whether strict validation is enabled." — [S41], Data Generation
- **C250** [inference] The data run ends on its own, so it can be scripted without a human; reading its generated files through git diff as a signal for the agent is this report's suggestion — no source read here describes it as a practice — from [C247], [C249]
- **C251** [quoted] A PyPI package (v0.1.0, one release, 2026-03-14) wraps Gradle and GameTest log parsing in a JSON CLI for coding agents; its author calls the Java library and the generated templates lightweight starting points, not a complete integration — "The Java library and generated templates are intentionally lightweight starting points rather than a complete production-ready Minecraft integration." — [S42], info.description, Status
- **C252** [quoted] Community mods exist that expose a running client to an agent over MCP with logs and screenshots (BlockPilot: NeoForge and Fabric, 1.21.1 and 26.1.2) — "Agents connect over MCP and get short text replies, logs and screenshots" — [S43], project description, "What you can do with it"
- **C253** [inference] No official NeoForge or Forge API for scripted client tests with screenshots appeared in the docs read; on those loaders client-side visual checks go through third-party tools (HeadlessMC + xvfb, hmc-specifics, community MCP mods), on Fabric 1.21.4+ through the official client gametest API — from [C212], [C222], [C230], [C239], [C252]
- **C254** [inference] The checks documented as ending without a human are the JUnit run, the data run and the game-test server (whose exit code counts failed tests); a pack-level boot check additionally needs a log scan, as Monifactory's CI does — from [C201], [C213], [C247], [C319], [C320]

### Q3 — How KubeJS scripts, recipes, datapacks, quests, balance and completability can be checked without a human — answered with a gap

- **C301** [quoted] ProbeJS describes itself as a dumper and TypeScript-typing generator for KubeJS — "A data dumper and typing generator for the KubeJS functions, constants and classes." — [S44], README, first line
- **C302** [quoted] In ProbeJS v8.0 the typings are produced from inside a running game, by a command plus a GUI button — "In game, use `/probejs` and click the dump button." — [S44], README § 1. Installation, step 3
- **C304** [quoted] Counter to "typings are correct": ProbeJS 8.0.3 on NeoForge 1.21.1 / KubeJS 2101.7.2 generated recipe-shortcut names that do not exist at runtime (open issue, 2026-10-08) — "The generated `RecipesKubeEvent` typings declare `smithing_transform()` and `smithing_trim()`, but the actual shortcuts are `smithing()` and `smithingTrim()`" — [S45], issue body
- **C305** [quoted] A shipping 1.20.1 Forge pack (Monifactory) points its kubejs jsconfig at ProbeJS-generated typings — "./probe/generated" — [S46], compilerOptions.typeRoots
- **C307** [inference] ProbeJS v8 typings cannot be regenerated fully headless today (needs a client with GUI; open issue #171 of 2026-05-21 asks for a command-only dump); a type-check step (tsc/tsgo over jsconfig) can run headless only against typings dumped earlier and committed or cached, and it can report false errors and miss real ones — from [C302], [C304], [C305]
- **C308** [quoted] KubeJS branch 2001 has a code path that throws a RuntimeException when startup scripts have errors; the line above it (L225) guards it with `Platform.getEnvironment() == Env.SERVER` or startupErrorGUI disabled — "throw new RuntimeException("There were KubeJS startup script syntax errors! See logs/kubejs/startup.log for more info");" — [S47], L225-226
- **C309** [quoted] In branch 2001 a `RecipeExceptionJS` or `JsonParseException` thrown inside a recipe event handler (server script) is logged to the SERVER console and that handler is skipped; any other exception from the handler is rethrown — "ConsoleJS.SERVER.error("Error while processing recipe event handler: " + handler, ex);" — [S48], L83
- **C310** [quoted] Branch 2001 writes one summary log line per recipe load with a failed-recipe count — "", modified " + modifiedCount + " recipes, with " + failedCount.get() + " failed recipes in " + timer.stop());" — [S48], L411
- **C311** [quoted] Branch 2101 (1.21.1) keeps the same summary line in a fixed format that a log check can parse — "Added %d recipes, removed %d recipes, modified %d recipes, with %d failed recipes taking %s in total" — [S49], L350
- **C312** [quoted] `/kubejs errors startup/server/client` exists in branch 2001; with no errors it prints a fixed message and returns 0 — "source.sendSystemMessage(Component.literal("No errors or warnings found!").withStyle(ChatFormatting.GREEN));" — [S50], L113-126, L599-611
- **C313** [inference] On a dedicated server the 2001 errors command, when errors exist, calls `source.getPlayerOrException()` and sends a network message to that player, so from the console/RCON it yields only the no-errors message or a failure; the log files (logs/kubejs/startup.log is the one named in the code read) are the machine-readable channel — from [C312], [C308]
- **C314** [inference] A dedicated-server boot is a two-part check for KubeJS 2001: process exit covers startup-script errors, while recipe and JSON exceptions in recipe event handlers and failed recipes are only logged and need a log scan; what other server-script errors do to the server process was not read — from [C308], [C309], [C310]
- **C315** [quoted] With an export active, KubeJS 2001 writes every non-removed recipe after script modification as a JSON file under recipes/ (same call exists in 2101 RecipesKubeEvent.java L386) — "DataExport.export.addJson("recipes/%s.json".formatted(path), r.json);" — [S48], L449-454
- **C316** [quoted] The same export writes each registry's ids to registries/<name>.json — "addJson("registries/" + key.location().getPath() + ".json", j);" — [S51], L110
- **C317** [quoted] The export is triggered by the `/kubejs export` command (needs singleplayer or OP) and lands in local/kubejs/export — "source.sendSuccess(() -> Component.literal("Done! Export in local/kubejs/export"), false);" — [S51], L202; command at KubeJSCommands.java L153-155
- **C318** [inference] A post-script recipe dump plus the item registry is obtainable from a running server by one command, which is the input a self-written reachability/cycle check needs; whether the command works from a dedicated-server console was not run here — from [C315], [C316], [C317]
- **C319** [quoted] Monifactory (1.20.1 Forge, KubeJS-heavy, 487 stars, pushed 2026-11-20) boots a Forge game-test server in CI — "["-Dforge.enableGameTest=true", "-Dforge.gameTestServer=true"]," — [S52], RunGametestServerTarget, L55
- **C320** [quoted] That runner reads server stdout line by line and kills the server on the first line matching its KubeJS ERROR/FATAL regex (RX_ERRORS, L19), then fails on a null or non-zero exit code — "if (!RX_ERRORS.test(line)) return" — [S52], L19, L64-81
- **C321** [quoted] The job runs on ubuntu-latest for each of three pack modes on PRs touching kubejs/, config/ and manifest.json — "node index.ts run-gametest-server --pack-mode=${{ matrix.pack-mode }}" — [S53], job gametest
- **C323** [measured] Judged by workflow file names, of five large packs checked only Monifactory (auto-lint-kubejs, eslint, pr-check-gametests) and Nomi-CEu (checks, testbuildpack) have workflows that check content; TerraFirmaGreg-Modern has build, release and housekeeping workflows, ATM-10 and Enigmatica10 have one non-content workflow each (stale-filter, support) — Own measurements, M302
- **C324** [quoted] NeoForge documents a game-test server run whose exit code is the number of failed required tests — "The Game Test Server is a special configuration which runs a build server. The build server returns an exit code of the number of required, failed Game Tests." — [S21], § Running Game Tests (file line 1043; current-version docs)
- **C325** [quoted] misode's PackTest lets tests be written as mcfunction files in a data pack and run automatically with an exit code; its CI example uses a Fabric server on 1.20.4 — "When `-Dpacktest.auto` is set, the game test server will start automatically with the loaded tests. The process will exit when all tests have finished with the exist code set to the number of failed tests." — [S54], § Auto test server
- **C326** [quoted] MC-Runtime-Test (HeadlessMC + Xvfb GitHub Action) launches the real client in CI; its table marks Forge and NeoForge supported for 1.20.1-1.20.6 and 1.21-1.21.11 — "MC-Runtime-Test enables you to run the Minecraft client within your CI/CD pipelines, simplifying the testing of runtime" — [S38], intro; table "Supported Minecraft Versions and Modloaders"
- **C328** [quoted] Spyglass presents itself as an editor-experience project — "Spyglass aims at improving users' editing experience of Minecraft data packs by providing IntelliSense features like" — [S55], README intro
- **C329** [measured] The Spyglass monorepo has packages language-server, mcdoc-cli, java-edition, vscode-extension etc. but no package named as a data-pack lint CLI — Own measurements, M301
- **C330** [quoted] A third-party CLI for datapack validation that uses SpyglassMC's mcdoc format exists (jmoiron/mcheck `tsmc`, 0 stars, pushed 2025-09), written with LLM tools for exactly the agent-guardrail use; its author warns against relying on it — "`tsmc` has not known the love of human touch. Its code is probably terrible. It might not really work properly." — [S56], § Slop Warning
- **C332** [quoted] In one reported case (loot table in a NeoForge mod, issue dated 2026-07-14) a datapack file that failed to parse appeared in the game log as an ERROR line starting "Couldn't parse data file" — "Couldn't parse data file 'sfm:blocks/buffer' from 'sfm:loot_table/blocks/buffer.json':" — [S57], issue body, log excerpt
- **C335** [inference] For a modded pack, loading the data in a server and scanning the log catches parse errors (one case seen), and registry dumps ([C316]) supply ids for offline cross-reference; whether vanilla-schema tools handle modded recipe types or item ids was not checked — from [C328], [C332], [C316]
- **C336** [quoted] The GTNH-scale exporter (NESQL, MC 1.7.10) dumps items, recipes and BetterQuesting quests to a database but needs a client in a world and about an hour — "For GTNH, the exported database file is ~600 MB, and the export process takes my computer ~60 min." — [S58], README intro; steps 3 and 6 ("Start Minecraft and join a world", "Run the command `/nesql`")
- **C338** [quoted] One small in-game mod advertising a recipe-graph export was read: RecipeGraph (client-side, Fabric, Minecraft 1.21.1, 84 downloads on Modrinth) scans JEI recipes through an in-game command and exports them as a JSON graph — "It can scan all recipe data from JEI in game through commands, then export the data as a JSON graph" — [S59], project description
- **C339** [quoted] Academic work on simulation-based economy balancing exists but is game-agnostic and abstract (graph economies tuned by evolutionary search), not a Minecraft tool — "We propose GEEvo (Game Economy Evolution), a framework to generate graph-based game economies and balancing both, newly generated or existing economies." — [S60], abstract (v1)
- **C341** [quoted] A mineflayer maintainer answered "neoforge support?" (2025-03) with a flat no — "No, mineflayer supports servers that accept vanilla clients only" — [S61], comment by extremeheat (member)
- **C344** [quoted] Counter-claim: an open, unmerged PR (opened 2026-09-11, updated 2026-10-07) claims modern Forge/NeoForge handshake support tested on NeoForge 21.1.250 / MC 1.21.1 and Forge 47.4.10 (1.20.1) — "The client completed channel negotiation, registry synchronization, command-tree decoding, and the clean-server recipe exchange." — [S62], PR body, "This was tested against real servers"
- **C345** [quoted] The same PR limits itself to the handshake and refuses to imply gameplay support for modded content — "fail explicitly for unsupported channels, codecs, recipes, flags, and versions instead of treating an advertised mod list as gameplay support" — [S62], PR body, bullet 5
- **C346** [quoted] A user pointed at that branch still reported an incompatibility disconnect from a NeoForge 21.1.250 server (issue open, 2026-09) — ""translate":{"type":"string","value":"multiplayer.disconnect.incompatible"}" — [S63], comment by ironweb10
- **C347** [quoted] Voyager (GPT-4, mineflayer) was the only compared agent to reach the diamond level of the vanilla tech tree, within a 160-iteration budget over three trials — "Voyager is the only one to unlock the diamond level of the tech tree" — [S64], § 3.2 Evaluation Results; Table 1 caption
- **C349** [quoted] Voyager's README names fabric-loader 1.19 as the version it is tested on and requires helper Fabric mods; it does not mention content mods — ""version": "fabric-loader-0.14.18-1.19", # the version Voyager is tested on" — [S65], § Getting Started (line 86)
- **C350** [inference] External protocol bots (mineflayer, Voyager) are not a working route for playtesting a Forge/NeoForge content pack as of 2026-11: a mineflayer maintainer says only servers that accept vanilla clients are supported, modded handshake support is an unmerged PR that disclaims modded-gameplay support, and Voyager (2023) was the only agent in its own comparison to reach the vanilla diamond level; the one pack whose CI was read (Monifactory) runs an in-process game-test server ([C319], [C324]) — from [C341], [C344], [C345], [C346], [C347], [C349], [C319], [C324]

### Q4 — What tooling gives a coding agent accurate Minecraft-modding context and control — answered with a gap

- **C401** [quoted] Mojang announced on 2025-10-29 that Java Edition stops being obfuscated, starting with the first snapshot after the Mounts of Mayhem launch — "Starting with the first snapshot following the complete Mounts of Mayhem launch, we will no longer obfuscate Minecraft: Java Edition." — [S66], section "Removing obfuscation in Java Edition"; datePublished 2025-10-29
- **C402** [quoted] The same announcement says the EULA and Usage Guidelines are unchanged and each jar now carries a LICENSE file linking to the EULA — "For extra transparency, you’ll now find a LICENSE file inside the jar that links directly to the EULA." — [S66], section "No changes to EULA"
- **C403** [quoted] Per Fabric docs for 26.1.2, Minecraft 26.1 is unobfuscated, ships parameter names and needs no obfuscation mappings — "Minecraft 26.1 is unobfuscated and includes parameter names, so there is no need for any obfuscation mappings." — [S67], section "What Are Mappings?"
- **C404** [quoted] Fabric stopped maintaining third-party mappings (Yarn) from the first unobfuscated release onward; mods updating to it must be on Mojang names — "from this version onward. If you plan to update your mod to this version, you will need to switch to Mojang's mappings first before updating." — [S67], intro paragraph; the page also carries a "Tribute to Fabric Yarn (2016-2025)" heading
- **C405** [measured] Fabric's meta API lists no Yarn build for any 26.x game version; the highest game versions with Yarn are 1.21.11 builds — Own measurements, M405
- **C406** [quoted] Official Mojang mapping files for obfuscated versions (checked: 1.20.1 client.txt) allow copying and development use but forbid redistributing them complete and unmodified — "You may copy and use the mappings for development purposes, but you may not redistribute the mappings complete and unmodified." — [S68], line 1 header, 1.20.1
- **C407** [quoted] The Minecraft EULA forbids distributing Mojang's code and defines a Mod as original work without a substantial part of Mojang's copyrightable code — "By "Mods," we mean something original that you or someone else created that doesn't contain a substantial part of our copyrightable code or content." — [S69], section "USING mods"; nearby: "you must not distribute anything we've made unless we specifically agree to it"
- **C408** [inference] The EULA's ban on distributing Mojang's code means decompiled Minecraft sources should not be committed to a public repo or shipped; that keeping them on the developer's disk as agent context is allowed is inferred from the "development purposes" wording of the 1.20.1 mappings licence, not stated by Mojang — from [C402], [C406], [C407]
- **C409** [quoted] NeoForge 26.1 release post (2026-03-24): Parchment can be dropped because Mojang's parameter names are available — "Finally, Parchment can be removed since Mojang’s parameter names are now available thanks to the removal of obfuscation." — [S5], section on updating build scripts; published_time 2026-03-24
- **C410** [quoted] NeoForge's 26.1 snapshots post adds that Parchment may be kept for its extra javadocs — "You can also keep it to benefit from additional javadocs." — [S70], same Parchment paragraph; published_time 2025-12-26
- **C411** [measured] Parchment's maven has data folders only up to parchment-1.21.11 (latest there: 2025.12.21 nightly); none for 26.x; parchment-1.21.1 latest is 2024.11.17 — Own measurements, M411
- **C412** [inference] On 26.x an agent gets real class, method and parameter names from the game jar but no javadocs from Parchment; on 1.20.1 and 1.21.1 names come from Mojang mappings plus optional Parchment parameter names/javadocs — from [C403], [C409], [C410], [C411]
- **C413** [quoted] NeoForge docs (current, 26.1): after Gradle import the workspace contains the patched Minecraft and NeoForge sources for reference — "your workspace will also contain the (patched) sources and [resource files][resources] of Minecraft and NeoForge for you to reference." — [S71], section on setting up the workspace
- **C415** [quoted] NeoForge docs call reading the source code vital for implementing many features — "If you want to look at the source code - which is vital for implementing many features -, we only support IntelliJ IDEA or Eclipse." — [S72], workspace setup note
- **C417** [quoted] Fabric: Minecraft source is generated locally with the genSources Gradle task — "The Fabric toolchain lets you access the Minecraft source code by generating it locally" — [S73], first paragraph; command `./gradlew genSources`
- **C418** [quoted] The Fabric wiki's LLM page says LLMs mix up versions and loaders and invent APIs — "**LLMs often mix up Minecraft versions and modding platforms**, or hallucinate APIs that don't exist." — [S74], section "If You're Learning"
- **C420** [quoted] It advises pointing an AI agent at the Minecraft and mod codebase before it implements a feature — "Consider giving it pointers to the right places to look to learn about the Minecraft and mod codebase before implementing the feature you are asking of it" — [S74], section "Context"
- **C422** [inference] Pointing the agent at locally generated sources is supported by loader-community guidance (Fabric wiki, NeoForge "vital"), but no official page read here prescribes an agent workflow or gives a filesystem path for the sources jar — from [C413], [C415], [C420]
- **C423** [measured] MCDxAI/minecraft-dev-mcp: 49 stars, MIT, created 2025-12-06, last push 2026-10-21, 0 open issues — Own measurements, M423
- **C424** [quoted] minecraft-dev-mcp decompiles any Minecraft version from 1.14 on demand and caches it locally — "Download, remap, and decompile any Minecraft version (1.14+) on first use — cached for instant access afterward" — [S75], features table
- **C425** [quoted] Its README lists tested versions ending at 26.1 snapshots; nothing newer is listed as tested — "**Tested versions:** 1.19.4 · 1.20.1 · 1.21.10 · 1.21.11 · 26.1-snapshot-8 · 26.1-snapshot-9" — [S75], version support section
- **C427** [measured] OGMatrix/mcmodding-mcp: 73 stars, MIT, last push 2026-08-28 (about three months before this run) — Own measurements, M423
- **C428** [quoted] mcmodding-mcp is a docs-search server: its core database is Fabric and NeoForge documentation (about 520 MB), with optional Parchment-mappings and mod-example databases — "Core Fabric & NeoForge documentation (installed by default)" — [S76], table "Available Databases"; README examples use minecraft_version "1.21.10"
- **C429** [measured] adhi-jp/minecraft-modding-mcp: 8 stars, MIT, created 2026-03-01, last push 2026-11-16 — Own measurements, M423
- **C430** [quoted] Its README states the project is entirely vibe-coded without formal specs — "This project is entirely vibe-coded — built with AI-assisted development without formal specs." — [S77], note under title
- **C432** [measured] Other mod-dev finds are tiny: GRAINALCOHOL/linkie-mcp 4 stars (last push 2026-08-24), neoforge-dev/neoforge-mcp-server 0 stars (2025-04), Zhou-Shilin/mcp-server-minecraft-mod-devdoc 6 stars (2025-05); these searches surfaced no MCP server published by Fabric, NeoForged or Forge — Own measurements, M432
- **C433** [inference] Mod-dev MCP servers exist: the three main ones have 49, 73 and 8 stars, and the two with a recorded creation date are from Dec 2025 and Mar 2026; no server published by a loader team surfaced; treat them as conveniences over the local sources, not as the source of truth — from [C423], [C427], [C429], [C430], [C432]
- **C434** [measured] yuniko-software/minecraft-mcp-server: 830 stars, Apache-2.0, last push 2026-04-04, 20 open issues — Own measurements, M423
- **C435** [quoted] It is a Mineflayer bot (a separate player joining the world) and its README says it supports only 1.21.11 — "Currently supports Minecraft version 1.21.11. Newer versions may not work with this MCP server, but we will add support as soon as possible." — [S78], IMPORTANT box at top
- **C438** [quoted] minecraft-mod-mcp runs inside the client on Forge, Fabric or NeoForge and claims 1.7.2 through 26.3; aimed at testing mods (screenshots, GUI clicks, commands) — "It exposes Minecraft's internals to AI tools via the MCP protocol, letting your AI agent inspect screens, click through GUIs, and run commands" — [S79], section "What is Minecraft Mod MCP"; versions line: "Works with Minecraft **1.7.2** through **26.3**"
- **C439** [quoted] CoolerProYT/MC-Test is a dev-only client mod plus MCP server for 1.20.1 (Fabric, Forge), 1.21.1 and 26.1.2–26.3 (Fabric, NeoForge), added as a dev-runtime dependency — "A **dev-only** Minecraft client mod (Fabric + NeoForge, Minecraft 1.21.1, 26.1.2, 26.2 and 26.3; Fabric + Forge for 1.20.1) plus an **MCP server**" — [S80], first paragraph
- **C441** [measured] Five RCON MCP repos were found, all with 0 stars — Own measurements, M432
- **C442** [inference] For testing a custom mod in a modded dev client, the in-game-mod servers (C438, C439) are the fitting class; the popular Mineflayer-based server is pinned to 1.21.11 and its behaviour on Forge/NeoForge servers was not established in this run — from [C435], [C438], [C439]
- **C443** [measured] Jahrome907/minecraft-agent-skills (redirect from minecraft-codex-skills): 211 stars, MIT, last push 2026-10-30 — Own measurements, M423
- **C444** [quoted] That bundle has 13 skills and states support for 26.x with examples for 1.21.x and Forge 1.20.1; installable as a Claude Code plugin or by copying into .claude/skills — "Supports Minecraft 26.x, with examples for 1.21.x and Forge 1.20.1." — [S81], intro
- **C446** [measured] On 2026-11-23 /llms.txt returns HTTP 404 on docs.neoforged.net, docs.fabricmc.net, docs.minecraftforge.net and kubejs.com — Own measurements, M446
- **C447** [measured] Context7 serves an llms.txt (HTTP 200) for neoforged/documentation and fabricmc/fabric-docs; content quality not assessed — Own measurements, M446
- **C448** [measured] NeoForge docs are versioned: current docs are 26.1, with versioned copies for 1.20.4, 1.20.6, 1.21.1, 1.21.3, 1.21.4, 1.21.5, 1.21.8, 1.21.10, 1.21.11; there is no 1.20.1 version and no 26.2/26.3 version yet — Own measurements, M448
- **C449** [measured] Forge docs have branches 1.20.1, 1.20.x and 1.21.x; Forge still ships 1.20.1 (Recommended 47.4.28, Latest 47.4.29; branch commit 2026-11-08) and lists builds for 26.3 — Own measurements, M448
- **C450** [measured] NeoForge's own 1.20.1 artifact (net.neoforged:forge) last updated 2024-05-28 at 1.20.1-47.1.106; NeoForge 21.1.x for 1.21.1 is still published (21.1.267; branch commit 2026-11-17); the repo default branch is 26.3.x — Own measurements, M448
- **C452** [inference] On 1.20.1 the maintained loader is MinecraftForge (NeoForge's 1.20.1 fork is dormant) and the docs are Forge's; NeoForge docs exist per version from 1.20.4 to 26.1, so an agent must be told which docs version applies — from [C448], [C449], [C450]
- **C453** [quoted] Mojang switched to year-based version numbers (announced 2025-12-02) — "Going forwards, we’ll be numbering our versions based on the year." — [S82], body; datePublished 2025-12-02
- **C454** [measured] On 2026-11-23 Mojang's manifest reports latest release 26.3 and latest snapshot 26.4-snapshot-10 — Own measurements, M454
- **C455** [inference] 2026 had three releases (26.1 in March, then 26.2, 26.3) with a fourth in snapshots, so "latest" moves about quarterly and each tool's supported version must be checked per drop — from [C409], [C453], [C454]
- **C456** [measured] KubeJS on Modrinth: newest files 2026-11-01 for 26.1.2 and for 1.21/1.21.1 (NeoForge only); 1.20.1 last file 2026-05-01 (Forge and Fabric); game versions listed stop at 26.1.2 although the repo has a 2603 branch — Own measurements, M456
- **C457** [inference] With KubeJS required, the newest usable Minecraft version on 2026-11-23 is, per Modrinth (CurseForge not checked), 26.1.2 on NeoForge, two drops behind the latest release — from [C454], [C456]
- **C458** [inference] For both KubeJS-capable NeoForge versions (1.21.1 and 26.1.2) there are versioned NeoForge docs, a documented game-test server and the ModDevGradle JUnit integration; for 1.20.1 the loader is MinecraftForge, NeoForge docs have no 1.20.1 version, and the ModDevGradle legacy page does not mention unit tests — from [C448], [C449], [C450], [C456], [C201], [C213], [C217]

## Disconfirming evidence

- [C102] — query (HN Algolia comments): `minecraft mod claude code neoforge`; `minecraft mod LLM version mappings`; `kubejs claude` — zero hits on all three; nothing found against. Partial counter already in the ledger: C103 (old version worked well), C120 (current models write fine code for a niche engine language with only a rules file), C121 (version confusion persists but is occasional).
- [C118] and [C148] — query: `LLM agent automated playtesting can evaluate game fun OR balance OR pacing study 2026 limitations "human playtesters"` — found balance/difficulty work only (RuleSmith, arXiv 2602.06232, snippet); no study of agents judging fun or pacing. Counter-claims on "agent cannot see or play": C124, C131, C138.
- [C123] and [C142] — query: `vibe coded game commercial success revenue players 2026 made entirely with AI agents Steam hit counterexample "not slop"` — found counter-claim C144 (Slotbound, self-reported revenue, outlet did not verify) and a snippet-only report of a profitable vibe-coded browser flight sim (see sources).
- [C136] — no separate search run; only one benchmark of this kind was found (search 2), so there is no competing number. Gap.
- [C135] — query: `"What Are We Gonna Do?!" postmortem "four days, two people, two coding agents" Codex Claude Code agents "felt good"` — no second copy of the text found; claim stays snippet. blocked: itch.io devlog returns 403 to curl and WebFetch.
- [C201] — query (WebSearch): `NeoForge runGameTestServer CI hangs OR "exit code" OR "always fails" github actions game tests` — no report of a hang found; found against: C204 (forced exit makes the task always fail under NeoGradle), C210 (zero tests = crash). `gh search issues --repo neoforged/NeoForge gameTestServer`: title-only hits (#2311, a test with a chance to fail, closed 2025-06), not read.
- [C213] — query (WebSearch): `Forge 1.20.1 JUnit unit test mod "Bootstrap.bootStrap" registry ForgeGradle test not supported` — nothing found for or against official JUnit support on Forge 1.20.1 / ForgeGradle; only Fabric pages came back. Stated gap. `gh search issues --repo neoforged/ModDevGradle "unit test"` gave C215.
- [C223], [C224] — query: `gh search issues --repo FabricMC/fabric-api "client gametest"` — found against: C231–C234; title-only hits not read: #5277 hang on disconnect in 26.1 (closed), #5623 deadlock on world close (closed), #5466 Sodium crashes screenshots in 26.1.2 (closed).
- [C235], [C240] — query: `gh search issues --repo headlesshq/headlessmc windows` and `--repo headlesshq/mc-runtime-test "neoforge OR timeout OR hang"` — blocked: GitHub search API rate limit (HTTP 403) — not retried. WebSearch `NeoForge automated client test screenshot CI "runClient" headless xvfb mod test startup time seconds gametest` — summary only: no startup-time figures found; counters are C242, C243.
- [C253] — query (WebSearch): `Minecraft mod development AI coding agent automated screenshot client test feedback loop Claude Code NeoForge OR Fabric "game test"` — nothing found against (no official NeoForge client-test API surfaced); result is a search summary of small community projects, none read except C251.
- [C301/C302] — query: gh issue list Prunoideae/ProbeJS (all states, 15 newest) + gh search issues "wrong type" — found against: [C304]; also open #177 "[1.21.1|NeoForge] Missing `$DefaultedRegistry` declaration breaks member autocomplete", #173 "Generated @special/types/index.d.ts contains invalid empty type", closed #169 (8.0.1 dump files have ts errors). Titles only read for these three.
- [C308/C314] — query: `KubeJS dedicated server does not stop on server script error recipes silently fail CI "kubejs" "server.log" errors continue` — nothing found against; a secondary wiki (deepwiki, snippet only) says failed recipes are logged and do not crash the server, consistent with [C309].
- [gap above] — query: the FTB Quests query above — found only small in-game mods; one of them (RecipeGraph) is [C338], the others were not opened; nothing established.
- [C341] — query: `mineflayer bot connect Forge 1.20.1 modded server "minecraft-protocol-forge" FML3 works custom blocks items unknown` + gh search issues PrismarineJS/mineflayer "forge"/"neoforge" — found against: [C344] (handshake only, unmerged); no report of a bot playing modded content.
- [C350] — query: `LLM agent automated playtesting modded Minecraft modpack bot quest progression completability "modpack"` — nothing found: no report of anyone bot-playtesting a modpack. Baritone (client mod, default branch 1.21.4) and Mindcraft (mineflayer-based, 6k stars) READMEs were only grepped; no modded-server claims seen.
- [C403] — query: `Parchment mappings 26.1 unobfuscated Minecraft still needed javadoc parameter names NeoForge 26.1 release blog` — partial counter: javadocs are not covered ([C410], [C411]); nothing found saying 26.x still needs a mapping file.
- [C404] — query: `curl https://meta.fabricmc.net/v2/versions/yarn` filtered for 26.x — nothing found against: no Yarn for 26.x ([C405]).
- [C418] — query: `LLM hallucinates Minecraft modding API wrong mappings version Forge Fabric NeoForge Claude ChatGPT mod development outdated methods` — nothing found against; also no measurement of how often it happens (no benchmark found).
- [C408] — query: `Minecraft decompiled source code cannot redistribute EULA mod repository "decompiled" sources committing to git modding legal` — no Mojang statement on git repos of decompiled source found; EULA read directly ([C407]).
- [C433] — query: gh repo metadata (pushed_at, stars) for each server — staleness found for mcmodding-mcp (2026-08-28) and tested-version lag for minecraft-dev-mcp ([C425]); no archived repos.
- [C442] — query: `gh search issues "forge OR modded OR neoforge" --repo yuniko-software/minecraft-mcp-server` and grep of the Mineflayer FAQ — no hits either way. A gap, not a confirmation.
- [C457] — query: Modrinth API `project/kubejs/version` game_versions union, and KubeJS repo branches — branch 2603 exists (work in progress) but no released file for 26.2/26.3. CurseForge not checked.

## Coverage

| Channel | Read | Gap |
|---|---|---|
| Users | Q1: two HN threads and four first-hand write-ups (Leiter, ben-weapons, Amiga port, PWA game). Q3: Monifactory and Nomi-CEu CI, ProbeJS and mineflayer issue threads. Q2: one PyPI harness. | Nothing first-hand at modpack scale, on KubeJS, quests or a content mod on NeoForge 1.21+/26.x. No practitioner write-up of an agent feedback loop for a Java mod. Q4: no issue threads read. |
| Specialists | Official docs and sources of NeoForge, Forge, Fabric, ModDevGradle, HeadlessMC; KubeJS source (branches 2001, 2101); Mojang announcements and EULA; a mineflayer maintainer; tool READMEs. | No official KubeJS docs page on error reporting found. No statement from loader maintainers or known mod authors about agent-written mods. No NeoForge/Forge support-policy statement. |
| Data | GameDevBench (arXiv 2602.11103v2), Voyager (arXiv 2305.16291v2), GEEvo abstract (arXiv 2404.18574), own measurements of repo and version facts. | No boot-time or flakiness numbers for server or client launches. No Minecraft-specific benchmark of agent modding. No data on bot cost per run. |
| Adversarial | HN dissent, Fabric client-gametest issues, ProbeJS wrong-typing issue, mineflayer incompatibility issue, mcheck's self-warning, Fabric wiki LLM page. | The itch.io jam postmortem is blocked (HTTP 403), so C135 is snippet-only. HeadlessMC issue search was blocked by the GitHub search rate limit. |

Declared overruns of the ceiling (four searches, three full reads per sub-question): Q1 — six searches, about five page reads; Q2 — four web searches plus three `gh` issue searches, about twelve short raw-doc reads (four toolchains, each with its own official page); Q3 — eight searches, about six substantial reads; Q4 — six web searches plus about ten `gh` repo searches. Disconfirming searches are not counted. Q1, Q2 and Q3 ledgers reached the 20,000-character cap; entries cut for the cap: packwiz, mcmeta, datapack-linter, mc-craft-tree, Voyager's limitations section, smaller Claude Code skills repos.

Tools: gathering was done by four subagents with the built-in web search and fetch, `curl` and `gh`; no connector was used. Quotes were copied from page text fetched over plain HTTP. The GitHub API rate limit was hit near the end of gathering.

Nothing was installed or run: every capability of an MCP server, mod or CLI named here is its README's self-description. Two READMEs contained text addressed to AI agents (langyo/minecraft-mod-mcp: a self-configure link; mcmodding-mcp: a suggested system prompt); both were treated as data.

No local experiment was run: the claims that could be tested here (a `gameTestServer` exit code, `/kubejs export` from a dedicated-server console, HeadlessMC on Windows 11) need a mod workspace that does not exist yet.

## Own measurements

### M201 — recorded during gathering for Q2

Command and output, as recorded: `grep -c "@GameTest"` and `grep -c "test_instance"` on raw.githubusercontent.com/neoforged/Documentation/main/versioned_docs/version-{1.21.4,1.21.5}/misc/gametest.md → 1.21.4: 19 / 0; 1.21.5: 0 / 7.

### M202 — recorded during gathering for Q2

Command and output, as recorded: `grep -n -i -E "test|junit" LEGACY.md` (raw.githubusercontent.com/neoforged/ModDevGradle/main/LEGACY.md) → only line 37 `testproject {`.

### M203 — recorded during gathering for Q2

Command and output, as recorded: `gh api "repos/FabricMC/fabric-api/contents/fabric-client-gametest-api-v1?ref=<branch>"` → 1.21.4: 2 entries; 1.21.1: 404; 1.20.1: 404. Oldest commit on that path: b47eab6b 2024-12-16 "Add experimental Client Game Test API (#4292)". fabric-docs versions/1.21.1 automatic-testing.md has no game test section; versions/1.21.4 has.

### M301 — recorded during gathering for Q3

Command and output, as recorded: command: `gh api repos/SpyglassMC/Spyglass/contents/packages --jq '.[].name'` — output: benchmarks core discord-bot java-edition json language-server locales mcdoc-cli mcdoc mcfunction nbt playground vscode-extension web-api-server (+ tsconfig files).

### M302 — recorded during gathering for Q3

Command and output, as recorded: command: `gh api repos/<repo>/contents/.github/workflows --jq '.[].name'` for five repos — output: Monifactory: auto-lint-kubejs, eslint, pr-check-gametests + build/release files; Nomi-CEu: checks (has a step `npm run gulp checkQB`, contents not read), testbuildpack + build/release files; TerraFirmaGreg-Team/Modpack-Modern: automerge, build, crowdin, giscus-rename, server (a Discord notice); ATM-10: stale-filter; Enigmatica10: support.

### M405 — recorded during gathering for Q4

Command and output, as recorded: `curl -sL https://meta.fabricmc.net/v2/versions/yarn | jq` game versions matching `^(26|1.21.1[01])`, last five → `1.21.11-pre4 1.21.11-pre5 1.21.11-rc1 1.21.11-rc2 1.21.11-rc3`; none starts with 26.

### M411 — recorded during gathering for Q4

Command and output, as recorded: listing of https://maven.parchmentmc.org/org/parchmentmc/data/ → last folders `parchment-1.21.9 parchment-1.21.10 parchment-1.21.11`; maven-metadata.xml: 1.21.11 `<latest>2025.12.21-nightly-SNAPSHOT</latest>`, 1.21.1 `<latest>2024.11.17</latest>`.

### M423 — recorded during gathering for Q4

Command and output, as recorded: `gh api repos/<r> --jq` → MCDxAI/minecraft-dev-mcp `stars=49 pushed_at=2026-10-21T01:45:13Z created_at=2025-12-06 license=MIT open_issues=0`; OGMatrix/mcmodding-mcp `stars=73 pushed_at=2026-08-28T22:23:28Z license=MIT`; adhi-jp/minecraft-modding-mcp `stars=8 pushed_at=2026-11-16T23:17:47Z created_at=2026-03-01 license=MIT`; yuniko-software/minecraft-mcp-server `stars=830 pushed_at=2026-04-04T20:59:29Z license=Apache-2.0 open_issues=20`; langyo/minecraft-mod-mcp `stars=44 pushed_at=2026-11-21T22:07:26Z created_at=2026-05-24`; CoolerProYT/MC-Test `stars=2 pushed_at=2026-11-20 created_at=2026-10-08`; Jahrome907/minecraft-agent-skills `stars=211 pushed_at=2026-10-30T00:19:45Z license=MIT`. All archived=false.

### M432 — recorded during gathering for Q4

Command and output, as recorded: `gh search repos "<q>" --sort stars --limit 8` (name, stars, pushed) → `GRAINALCOHOL/linkie-mcp | 4 | 2026-08-24`; `neoforge-dev/neoforge-mcp-server | 0 | 2025-04-17`; `Zhou-Shilin/mcp-server-minecraft-mod-devdoc | 6 | 2025-05-04`; "minecraft rcon mcp" → five repos (deemkeen, Faneraiy14, Arkteus, SkyToti, scotteratigan), each `| 0 |`; "fabric mcp minecraft" → no results.

### M446 — recorded during gathering for Q4

Command and output, as recorded: `curl -o /dev/null -w "%{http_code}"` → docs.neoforged.net/llms.txt 404; docs.fabricmc.net/llms.txt 404 (llms-full.txt 404); docs.minecraftforge.net/llms.txt 404; kubejs.com/llms.txt 404; context7.com/neoforged/documentation/llms.txt 200; context7.com/fabricmc/fabric-docs/llms.txt 200.

### M448 — recorded during gathering for Q4

Command and output, as recorded: `gh api repos/neoforged/Documentation/contents/versioned_docs` → `version-1.20.4 version-1.20.6 version-1.21.1 version-1.21.10 version-1.21.11 version-1.21.3 version-1.21.4 version-1.21.5 version-1.21.8`; docs site shows `Version: 26.1`. Forge Documentation branches end `1.19.2 1.20.x 1.20.1 1.21.x`. files.minecraftforge.net list starts `26.3 26.2 26.1.2 26.1.1 26.1 1.21.11`; index_1.20.1 → `Recommended: 47.4.28 Latest: 47.4.29`; MinecraftForge branch 1.20.1 last commit `2026-11-08T17:39:33Z Update MDK to use FG7 (#10923)`. maven.neoforged.net: net/neoforged/forge `<latest>1.20.1-47.1.106</latest> <lastUpdated>20240528174429`; net/neoforged/neoforge last 21.1 entry `21.1.267`; NeoForge repo `default=26.3.x`, branch 1.21.1 last commit 2026-11-17.

### M454 — recorded during gathering for Q4

Command and output, as recorded: `curl https://piston-meta.mojang.com/mc/game/version_manifest_v2.json` → `{'release': '26.3', 'snapshot': '26.4-snapshot-10'}`.

### M456 — recorded during gathering for Q4

Command and output, as recorded: `curl https://api.modrinth.com/v2/project/kubejs/version | jq` → newest: `2026-11-01 26.1.2-8.0.8+neoforge mc=26.1.2 loaders=neoforge`; `2026-11-01 2101.7.2-build.381 mc=1.21,1.21.1 loaders=neoforge`; game versions union `1.18.2 1.19 1.19.2 1.20.1 1.20.4 1.21 1.21.1 26.1.2`; 1.20.1 filter → `2026-05-01 2001.6.5-build.26+forge` and `+fabric`. KubeJS repo: `default_branch=2601`, branches `… 2001 2101 2601 2603`.

## Open questions

- Q1 — no new information — no first-hand account exists, in what was found, of agents building a content mod plus KubeJS, datapacks and quests at modpack scale; the project's own first slices are what would settle how well the Godot and browser-game lessons transfer.
- Q1 — blocked: itch.io returns 403 — the jam postmortem behind C135 could not be read; reading it in a browser would settle whether it says what the snippet reports.
- Q2 — no new information — boot time of one `gameTestServer` run and of one client launch, with a real modpack loaded: no figures found; measure on the skeleton (it decides how often an agent can afford each rung).
- Q1 — no new information — whether version drift still measurably hurts 2026 models on Minecraft modding: only tool authors' framing was found (C116), no benchmark; the project's first slices on a pinned version would show it.
- Q2 — ceiling — JUnit on Forge 1.20.1 (ForgeGradle or the ModDevGradle legacy plugin): not found documented either way; a ten-minute trial in a 1.20.1 workspace settles it.
- Q2 — no new information — whether HeadlessMC or an in-client MCP mod gives usable screenshots on a local Windows 11 machine (C246 is a prior); a trial settles it.
- Q2 — no new information — golden images versus a vision model reading screenshots: no source compares them.
- Q3 — ceiling — whether a KubeJS 2101 (1.21.1) dedicated server exits on startup-script errors: the throw seen in branch 2001 was not located in 2101; reading the 2101 startup path or one trial boot settles it.
- Q3 — ceiling — whether `/kubejs export` runs from a dedicated-server console or RCON; one trial settles it.
- Q3 — ceiling — what KubeJS does with server-script errors outside recipe event handlers (2001 and 2101), and whether they stop the server: not read; a trial boot with a deliberately broken server script settles it.
- Q3 — ceiling — whether vanilla-schema tools (Spyglass, mcdoc-based checkers) accept modded recipe types and item ids: not checked; running one against a modded recipe JSON settles it.
- Q3 — ceiling — FTB Quests SNBT format and parsers, JEI/EMI/REI headless export, the contents of Nomi-CEu's `checkQB` step: not checked; a next run on "quest book validation and recipe export" would cover them.
- Q3 — no new information — no off-the-shelf progression-reachability checker and no crafting-cost simulator for 1.20+/1.21 packs were found; of the small in-game mods the search returned, only RecipeGraph's page was read (C338); installing it in a pack would show whether its graph export is usable.
- Q4 — ceiling — the filesystem path of the decompiled sources jar under ModDevGradle and Loom is not in the docs read; `gradlew` in the skeleton workspace shows it.
- Q4 — no new information — no Mojang statement was found on keeping decompiled sources locally as agent context (C408 infers it from the 1.20.1 mappings licence); the LICENSE file in a 26.x jar or a Mojang statement would settle it.
- Q4 — ceiling — the LICENSE file inside 26.x jars was not read (the mapping-licence quote is from 1.20.1); KubeJS on CurseForge and any 26.2/26.3 release date were not checked.
- Next runs, by the splitting rule: (a) quest-book and recipe-export tooling (FTB Quests SNBT, EMI/JEI export, checkQB); (b) agent-driven client testing on NeoForge/Forge (the in-client MCP mods, HeadlessMC on Windows, vision QA of screenshots); (c) version choice for the pack as a whole, joining this report's pipeline facts with mod availability.

## Sources

- [S1] maxleiter.com/blog/vibecoding-minecraft-mods — https://maxleiter.com/blog/vibecoding-minecraft-mods — passage
- [S2] hypo321.github.io/ben-weapons/article.html — https://hypo321.github.io/ben-weapons/article.html — passage
- [S3] raw.githubusercontent.com/OGMatrix/mcmodding-mcp/main/README.md — https://raw.githubusercontent.com/OGMatrix/mcmodding-mcp/main/README.md — passage
- [S4] raw.githubusercontent.com/Jahrome907/minecraft-agent-skills/main/README.md — https://raw.githubusercontent.com/Jahrome907/minecraft-agent-skills/main/README.md — passage
- [S5] neoforged.net/news/26.1release/ — https://neoforged.net/news/26.1release/ — passage
- [S6] news.ycombinator.com/item?id=47400868 — https://news.ycombinator.com/item?id=47400868 — passage
- [S7] news.ycombinator.com/item?id=47404330 — https://news.ycombinator.com/item?id=47404330 — passage
- [S8] news.ycombinator.com/item?id=47407325 — https://news.ycombinator.com/item?id=47407325 — passage
- [S9] news.ycombinator.com/item?id=47406299 — https://news.ycombinator.com/item?id=47406299 — passage
- [S10] news.ycombinator.com/item?id=47405190 — https://news.ycombinator.com/item?id=47405190 — passage
- [S11] news.ycombinator.com/item?id=47415287 — https://news.ycombinator.com/item?id=47415287 — passage
- [S12] babyloniantwins.com/blog/porting-a-1993-amiga-game-to-godot/ — https://babyloniantwins.com/blog/porting-a-1993-amiga-game-to-godot/ — passage
- [S13] dev.to/tiagovilasboas/i-built-a-full-pwa-game-in-one-day-with-claude-opus-55-and-claude-code-here-is-the-real-token-41p7 — https://dev.to/tiagovilasboas/i-built-a-full-pwa-game-in-one-day-with-claude-opus-55-and-claude-code-here-is-the-real-token-41p7 — passage
- [S14] rodrigopiston.itch.io/what-are-we-gonna-do/devlog/1782563/postmortem-four-days-two-people-two-coding-agents — https://rodrigopiston.itch.io/what-are-we-gonna-do/devlog/1782563/postmortem-four-days-two-people-two-coding-agents — snippet only
- [S15] arxiv.org/abs/2602.11103v2 — https://arxiv.org/abs/2602.11103v2 — passage
- [S16] arxiv.org/html/2602.11103v2 — https://arxiv.org/html/2602.11103v2 — passage
- [S17] news.ycombinator.com/item?id=46552524 — https://news.ycombinator.com/item?id=46552524 — passage
- [S18] mein-mmo.de/euro-in-2-tagen-vibecoder-entwickelt-solo-game-steam-hit/ — https://mein-mmo.de/euro-in-2-tagen-vibecoder-entwickelt-solo-game-steam-hit/ — passage
- [S19] raw.githubusercontent.com/neoforged/Documentation/main/versioned_docs/version-1.21.1/misc/gametest.md — https://raw.githubusercontent.com/neoforged/Documentation/main/versioned_docs/version-1.21.1/misc/gametest.md — passage
- [S20] raw.githubusercontent.com/MinecraftForge/Documentation/1.20.x/docs/misc/gametest.md — https://raw.githubusercontent.com/MinecraftForge/Documentation/1.20.x/docs/misc/gametest.md — passage
- [S21] raw.githubusercontent.com/neoforged/Documentation/main/docs/misc/gametest.md — https://raw.githubusercontent.com/neoforged/Documentation/main/docs/misc/gametest.md — passage
- [S22] gist.githubusercontent.com/SizableShrimp/60ad4109e3d0a23107a546b3bc0d9752/raw/README.md — https://gist.githubusercontent.com/SizableShrimp/60ad4109e3d0a23107a546b3bc0d9752/raw/README.md — passage
- [S23] raw.githubusercontent.com/neoforged/ModDevGradle/main/README.md — https://raw.githubusercontent.com/neoforged/ModDevGradle/main/README.md — passage
- [S24] github.com/neoforged/ModDevGradle/issues/171 — https://github.com/neoforged/ModDevGradle/issues/171 — passage
- [S25] raw.githubusercontent.com/neoforged/ModDevGradle/main/LEGACY.md — https://raw.githubusercontent.com/neoforged/ModDevGradle/main/LEGACY.md — passage
- [S26] raw.githubusercontent.com/FabricMC/fabric-docs/main/develop/automatic-testing.md — https://raw.githubusercontent.com/FabricMC/fabric-docs/main/develop/automatic-testing.md — passage
- [S27] raw.githubusercontent.com/FabricMC/fabric-api/HEAD/fabric-client-gametest-api-v1/src/client/java/net/fabricmc/fabric/api/client/gametest/v1/package-info.java — https://raw.githubusercontent.com/FabricMC/fabric-api/HEAD/fabric-client-gametest-api-v1/src/client/java/net/fabricmc/fabric/api/client/gametest/v1/package-info.java — passage
- [S28] raw.githubusercontent.com/FabricMC/fabric-api/HEAD/fabric-client-gametest-api-v1/src/client/java/net/fabricmc/fabric/api/client/gametest/v1/context/ClientGameTestContext.java — https://raw.githubusercontent.com/FabricMC/fabric-api/HEAD/fabric-client-gametest-api-v1/src/client/java/net/fabricmc/fabric/api/client/gametest/v1/context/ClientGameTestContext.java — passage
- [S29] raw.githubusercontent.com/FabricMC/fabric-api/HEAD/fabric-client-gametest-api-v1/src/client/java/net/fabricmc/fabric/api/client/gametest/v1/screenshot/TestScreenshotComparisonOptions.java — https://raw.githubusercontent.com/FabricMC/fabric-api/HEAD/fabric-client-gametest-api-v1/src/client/java/net/fabricmc/fabric/api/client/gametest/v1/screenshot/TestScreenshotComparisonOptions.java — passage
- [S30] raw.githubusercontent.com/FabricMC/fabric-docs/main/develop/loom/production-run-tasks.md — https://raw.githubusercontent.com/FabricMC/fabric-docs/main/develop/loom/production-run-tasks.md — passage
- [S31] raw.githubusercontent.com/FabricMC/fabric-api/HEAD/.github/workflows/build.yml — https://raw.githubusercontent.com/FabricMC/fabric-api/HEAD/.github/workflows/build.yml — passage
- [S32] github.com/FabricMC/fabric-api/issues/4415 — https://github.com/FabricMC/fabric-api/issues/4415 — passage
- [S33] github.com/FabricMC/fabric-api/issues/5655 — https://github.com/FabricMC/fabric-api/issues/5655 — passage
- [S34] github.com/FabricMC/fabric-api/issues/4411 — https://github.com/FabricMC/fabric-api/issues/4411 — passage
- [S35] raw.githubusercontent.com/headlesshq/headlessmc/HEAD/docs/launch.md — https://raw.githubusercontent.com/headlesshq/headlessmc/HEAD/docs/launch.md — passage
- [S36] raw.githubusercontent.com/headlesshq/headlessmc/HEAD/README.md — https://raw.githubusercontent.com/headlesshq/headlessmc/HEAD/README.md — passage
- [S37] raw.githubusercontent.com/headlesshq/headlessmc/HEAD/docs/specifics.md — https://raw.githubusercontent.com/headlesshq/headlessmc/HEAD/docs/specifics.md — passage
- [S38] raw.githubusercontent.com/headlesshq/mc-runtime-test/main/README.md — https://raw.githubusercontent.com/headlesshq/mc-runtime-test/main/README.md — passage
- [S39] raw.githubusercontent.com/headlesshq/mc-runtime-test/main/action.yml — https://raw.githubusercontent.com/headlesshq/mc-runtime-test/main/action.yml — passage
- [S40] raw.githubusercontent.com/neoforged/Documentation/main/docs/resources/index.md — https://raw.githubusercontent.com/neoforged/Documentation/main/docs/resources/index.md — passage
- [S41] raw.githubusercontent.com/FabricMC/fabric-docs/main/develop/loom/fabric-api.md — https://raw.githubusercontent.com/FabricMC/fabric-docs/main/develop/loom/fabric-api.md — passage
- [S42] pypi.org/pypi/irongolem/json — https://pypi.org/pypi/irongolem/json — passage
- [S43] www.curseforge.com/minecraft/mc-mods/blockpilot — https://www.curseforge.com/minecraft/mc-mods/blockpilot — passage
- [S44] raw.githubusercontent.com/Prunoideae/ProbeJS/HEAD/README.md — https://raw.githubusercontent.com/Prunoideae/ProbeJS/HEAD/README.md — passage
- [S45] github.com/Prunoideae/ProbeJS/issues/175 — https://github.com/Prunoideae/ProbeJS/issues/175 — passage
- [S46] raw.githubusercontent.com/ThePansmith/Monifactory/main/kubejs/jsconfig.json — https://raw.githubusercontent.com/ThePansmith/Monifactory/main/kubejs/jsconfig.json — passage
- [S47] raw.githubusercontent.com/KubeJS-Mods/KubeJS/2001/common/src/main/java/dev/latvian/mods/kubejs/KubeJS.java — https://raw.githubusercontent.com/KubeJS-Mods/KubeJS/2001/common/src/main/java/dev/latvian/mods/kubejs/KubeJS.java — passage
- [S48] raw.githubusercontent.com/KubeJS-Mods/KubeJS/2001/common/src/main/java/dev/latvian/mods/kubejs/recipe/RecipesEventJS.java — https://raw.githubusercontent.com/KubeJS-Mods/KubeJS/2001/common/src/main/java/dev/latvian/mods/kubejs/recipe/RecipesEventJS.java — passage
- [S49] raw.githubusercontent.com/KubeJS-Mods/KubeJS/2101/src/main/java/dev/latvian/mods/kubejs/recipe/RecipesKubeEvent.java — https://raw.githubusercontent.com/KubeJS-Mods/KubeJS/2101/src/main/java/dev/latvian/mods/kubejs/recipe/RecipesKubeEvent.java — passage
- [S50] raw.githubusercontent.com/KubeJS-Mods/KubeJS/2001/common/src/main/java/dev/latvian/mods/kubejs/command/KubeJSCommands.java — https://raw.githubusercontent.com/KubeJS-Mods/KubeJS/2001/common/src/main/java/dev/latvian/mods/kubejs/command/KubeJSCommands.java — passage
- [S51] raw.githubusercontent.com/KubeJS-Mods/KubeJS/2001/common/src/main/java/dev/latvian/mods/kubejs/server/DataExport.java — https://raw.githubusercontent.com/KubeJS-Mods/KubeJS/2001/common/src/main/java/dev/latvian/mods/kubejs/server/DataExport.java — passage
- [S52] raw.githubusercontent.com/ThePansmith/Monifactory/main/build/launcher/server.ts — https://raw.githubusercontent.com/ThePansmith/Monifactory/main/build/launcher/server.ts — passage
- [S53] raw.githubusercontent.com/ThePansmith/Monifactory/HEAD/.github/workflows/pr-check-gametests.yml — https://raw.githubusercontent.com/ThePansmith/Monifactory/HEAD/.github/workflows/pr-check-gametests.yml — passage
- [S54] raw.githubusercontent.com/misode/packtest/HEAD/README.md — https://raw.githubusercontent.com/misode/packtest/HEAD/README.md — passage
- [S55] raw.githubusercontent.com/SpyglassMC/Spyglass/main/README.md — https://raw.githubusercontent.com/SpyglassMC/Spyglass/main/README.md — passage
- [S56] raw.githubusercontent.com/jmoiron/mcheck/HEAD/README.md — https://raw.githubusercontent.com/jmoiron/mcheck/HEAD/README.md — passage
- [S57] github.com/TeamDman/SuperFactoryManager/issues/568 — https://github.com/TeamDman/SuperFactoryManager/issues/568 — passage
- [S58] raw.githubusercontent.com/D-Cysteine/nesql-exporter/HEAD/README.md — https://raw.githubusercontent.com/D-Cysteine/nesql-exporter/HEAD/README.md — passage
- [S59] modrinth.com/mod/recipegraph — https://modrinth.com/mod/recipegraph — passage
- [S60] arxiv.org/abs/2404.18574 — https://arxiv.org/abs/2404.18574 — passage
- [S61] github.com/PrismarineJS/mineflayer/issues/3606 — https://github.com/PrismarineJS/mineflayer/issues/3606 — passage
- [S62] github.com/PrismarineJS/node-minecraft-protocol-forge/pull/51 — https://github.com/PrismarineJS/node-minecraft-protocol-forge/pull/51 — passage
- [S63] github.com/PrismarineJS/mineflayer/issues/4097 — https://github.com/PrismarineJS/mineflayer/issues/4097 — passage
- [S64] arxiv.org/html/2305.16291v2 — https://arxiv.org/html/2305.16291v2 — passage
- [S65] raw.githubusercontent.com/MineDojo/Voyager/main/README.md — https://raw.githubusercontent.com/MineDojo/Voyager/main/README.md — passage
- [S66] www.minecraft.net/en-us/article/removing-obfuscation-in-java-edition — https://www.minecraft.net/en-us/article/removing-obfuscation-in-java-edition — passage
- [S67] docs.fabricmc.net/26.1.2/develop/porting/mappings/ — https://docs.fabricmc.net/26.1.2/develop/porting/mappings/ — passage
- [S68] piston-data.mojang.com/v1/objects/6c48521eed01fe2e8ecdadbd5ae348415f3c47da/client.txt — https://piston-data.mojang.com/v1/objects/6c48521eed01fe2e8ecdadbd5ae348415f3c47da/client.txt — passage
- [S69] www.minecraft.net/en-us/eula — https://www.minecraft.net/en-us/eula — passage
- [S70] neoforged.net/news/26.1snapshots/ — https://neoforged.net/news/26.1snapshots/ — passage
- [S71] raw.githubusercontent.com/neoforged/Documentation/main/docs/gettingstarted/index.md — https://raw.githubusercontent.com/neoforged/Documentation/main/docs/gettingstarted/index.md — passage
- [S72] docs.neoforged.net/docs/gettingstarted/ — https://docs.neoforged.net/docs/gettingstarted/ — passage
- [S73] raw.githubusercontent.com/FabricMC/fabric-docs/main/develop/getting-started/vscode/generating-sources.md — https://raw.githubusercontent.com/FabricMC/fabric-docs/main/develop/getting-started/vscode/generating-sources.md — passage
- [S74] wiki.fabricmc.net/_export/raw/tutorial:using_llms — https://wiki.fabricmc.net/_export/raw/tutorial:using_llms — passage
- [S75] raw.githubusercontent.com/MCDxAI/minecraft-dev-mcp/main/README.md — https://raw.githubusercontent.com/MCDxAI/minecraft-dev-mcp/main/README.md — passage
- [S76] raw.githubusercontent.com/OGMatrix/mcmodding-mcp/dev/README.md — https://raw.githubusercontent.com/OGMatrix/mcmodding-mcp/dev/README.md — passage
- [S77] raw.githubusercontent.com/adhi-jp/minecraft-modding-mcp/main/README.md — https://raw.githubusercontent.com/adhi-jp/minecraft-modding-mcp/main/README.md — passage
- [S78] raw.githubusercontent.com/yuniko-software/minecraft-mcp-server/main/README.md — https://raw.githubusercontent.com/yuniko-software/minecraft-mcp-server/main/README.md — passage
- [S79] raw.githubusercontent.com/langyo/minecraft-mod-mcp/master/README.md — https://raw.githubusercontent.com/langyo/minecraft-mod-mcp/master/README.md — passage
- [S80] raw.githubusercontent.com/CoolerProYT/MC-Test/26.3/README.md — https://raw.githubusercontent.com/CoolerProYT/MC-Test/26.3/README.md — passage
- [S81] raw.githubusercontent.com/jahrome907/minecraft-codex-skills/main/README.md — https://raw.githubusercontent.com/jahrome907/minecraft-codex-skills/main/README.md — passage
- [S82] www.minecraft.net/en-us/article/minecraft-new-version-numbering-system — https://www.minecraft.net/en-us/article/minecraft-new-version-numbering-system — passage

## Audit

| Claim | Verdict | Note |
|---|---|---|
| C101 | SUPPORTED |  |
| C102 | SUPPORTED |  |
| C103 | SUPPORTED |  |
| C105 | SUPPORTED |  |
| C106 | SUPPORTED |  |
| C108 | SUPPORTED |  |
| C109 | SUPPORTED |  |
| C110 | SUPPORTED |  |
| C111 | SUPPORTED |  |
| C113 | SUPPORTED |  |
| C116 | PARTIAL | the named claims show a toolchain change and tools' self-description, not that drift persists or that tools were built to pin a version; narrowed |
| C117 | SUPPORTED |  |
| C118 | SUPPORTED |  |
| C119 | SUPPORTED |  |
| C120 | SUPPORTED |  |
| C121 | SUPPORTED |  |
| C122 | SUPPORTED |  |
| C123 | SUPPORTED |  |
| C124 | SUPPORTED |  |
| C125 | SUPPORTED |  |
| C126 | SUPPORTED |  |
| C127 | SUPPORTED |  |
| C129 | SUPPORTED |  |
| C130 | SUPPORTED |  |
| C131 | SUPPORTED |  |
| C132 | SUPPORTED |  |
| C133 | SUPPORTED |  |
| C135 | UNCHECKABLE | itch.io returns HTTP 403 to curl with and without a browser user agent |
| C136 | SUPPORTED |  |
| C137 | SUPPORTED |  |
| C138 | SUPPORTED |  |
| C139 | SUPPORTED |  |
| C142 | PARTIAL | the comment does not say its author took part in the jam; "participant" changed to "commenter" |
| C144 | SUPPORTED |  |
| C147 | PARTIAL | "in every first-hand account" contradicted by C119 and C131; narrowed to the two accounts where a human found it |
| C148 | PARTIAL | no named claim speaks of scope decisions; changed to "a few design decisions" (C125) |
| C149 | PARTIAL | "finds reachability and collision bugs" is in none of the named claims; removed |
| C150 | PARTIAL | "the published evidence covers only" narrowed to what this run found: two mods, one plugin |
| C201 | SUPPORTED |  |
| C203 | SUPPORTED |  |
| C204 | SUPPORTED |  |
| C205 | SUPPORTED |  |
| C206 | SUPPORTED |  |
| C207 | SUPPORTED |  |
| C209 | SUPPORTED |  |
| C210 | SUPPORTED |  |
| C212 | SUPPORTED |  |
| C213 | SUPPORTED |  |
| C214 | SUPPORTED |  |
| C215 | SUPPORTED |  |
| C216 | SUPPORTED |  |
| C217 | SUPPORTED |  |
| C218 | SUPPORTED |  |
| C219 | SUPPORTED |  |
| C221 | SUPPORTED |  |
| C222 | SUPPORTED |  |
| C223 | SUPPORTED |  |
| C224 | SUPPORTED |  |
| C225 | SUPPORTED |  |
| C227 | SUPPORTED |  |
| C228 | SUPPORTED |  |
| C229 | SUPPORTED |  |
| C230 | SUPPORTED |  |
| C231 | SUPPORTED |  |
| C232 | SUPPORTED |  |
| C233 | SUPPORTED |  |
| C234 | SUPPORTED |  |
| C235 | SUPPORTED |  |
| C237 | SUPPORTED |  |
| C238 | PARTIAL | the docs line says text rendered on the screen; "without a screen" removed |
| C239 | SUPPORTED |  |
| C240 | SUPPORTED |  |
| C241 | SUPPORTED |  |
| C242 | SUPPORTED |  |
| C243 | PARTIAL | source says "normally"; the README's own 26.3 harness re-enables the ticker; word restored |
| C244 | SUPPORTED |  |
| C245 | PARTIAL | "a real window on Windows" rests on prior C246 and "text/GUI dumps only" on nothing named; narrowed |
| C246 | UNCHECKABLE | prior |
| C247 | SUPPORTED |  |
| C249 | SUPPORTED |  |
| C250 | PARTIAL | the claims show the run ends on its own; the git-diff signal is the report's suggestion; reworded to say so |
| C251 | PARTIAL | the quote calls the Java library and templates starting points, not the package; the same section says the Python CLI is usable today |
| C252 | SUPPORTED | page read (curl with a browser user agent; plain urllib gets HTTP 403); label changed from snippet to quoted |
| C253 | SUPPORTED |  |
| C254 | SUPPORTED |  |
| C301 | SUPPORTED |  |
| C302 | SUPPORTED |  |
| C304 | SUPPORTED |  |
| C305 | SUPPORTED |  |
| C307 | SUPPORTED |  |
| C308 | SUPPORTED |  |
| C309 | PARTIAL | the surrounding code logs and skips only RecipeExceptionJS and JsonParseException; other exceptions are rethrown; narrowed |
| C310 | SUPPORTED |  |
| C311 | SUPPORTED |  |
| C312 | SUPPORTED |  |
| C313 | PARTIAL | the file name server.log is not in the code read; removed |
| C314 | PARTIAL | "the server keeps running" and "server-script errors" in general go beyond C309; narrowed to recipe-handler exceptions and failed recipes |
| C315 | SUPPORTED |  |
| C316 | SUPPORTED |  |
| C317 | SUPPORTED |  |
| C318 | SUPPORTED |  |
| C319 | SUPPORTED |  |
| C320 | SUPPORTED |  |
| C321 | SUPPORTED |  |
| C323 | PARTIAL | recorded output is file names only; ATM-10 and Enigmatica10 have one workflow each, not none; reworded |
| C324 | SUPPORTED |  |
| C325 | SUPPORTED |  |
| C326 | SUPPORTED |  |
| C328 | SUPPORTED |  |
| C329 | SUPPORTED |  |
| C330 | PARTIAL | README says it uses SpyglassMC's mcdoc format, not that it wraps Spyglass; reworded |
| C332 | PARTIAL | one issue's log excerpt stated as a general rule; narrowed to the one case |
| C335 | PARTIAL | that vanilla-schema tools cannot know modded types is in no named claim; narrowed and marked unchecked |
| C336 | SUPPORTED |  |
| C338 | PARTIAL | page read; only RecipeGraph is on it; the four other mods and the GitHub-search remark removed; label changed from snippet to quoted |
| C339 | SUPPORTED |  |
| C341 | SUPPORTED |  |
| C344 | SUPPORTED |  |
| C345 | SUPPORTED |  |
| C346 | SUPPORTED |  |
| C347 | SUPPORTED |  |
| C349 | PARTIAL | README does not mention content mods; reworded to what it says |
| C350 | PARTIAL | Mindcraft is in no claim; "best published LLM agent" and "what packs actually run" narrowed to Voyager's own comparison and to Monifactory |
| C401 | SUPPORTED |  |
| C402 | SUPPORTED |  |
| C403 | SUPPORTED |  |
| C404 | SUPPORTED |  |
| C405 | SUPPORTED |  |
| C406 | SUPPORTED |  |
| C407 | SUPPORTED |  |
| C408 | PARTIAL | no named claim says local use as agent context is allowed; marked as inferred from the mappings licence |
| C409 | SUPPORTED |  |
| C410 | SUPPORTED |  |
| C411 | SUPPORTED |  |
| C412 | SUPPORTED |  |
| C413 | SUPPORTED |  |
| C415 | SUPPORTED |  |
| C417 | SUPPORTED |  |
| C418 | SUPPORTED |  |
| C420 | SUPPORTED |  |
| C422 | SUPPORTED |  |
| C423 | SUPPORTED |  |
| C424 | SUPPORTED |  |
| C425 | SUPPORTED |  |
| C427 | SUPPORTED |  |
| C428 | SUPPORTED |  |
| C429 | SUPPORTED |  |
| C430 | SUPPORTED |  |
| C432 | SUPPORTED |  |
| C433 | PARTIAL | no author-count data, C432 repos date from 2025-04/05 and one server has no creation date; narrowed to recorded stars and dates |
| C434 | SUPPORTED |  |
| C435 | SUPPORTED |  |
| C438 | SUPPORTED |  |
| C439 | SUPPORTED |  |
| C441 | SUPPORTED |  |
| C442 | SUPPORTED |  |
| C443 | SUPPORTED |  |
| C444 | SUPPORTED |  |
| C446 | SUPPORTED |  |
| C447 | SUPPORTED |  |
| C448 | SUPPORTED |  |
| C449 | PARTIAL | the site-menu list is not in the recorded output M448; removed |
| C450 | SUPPORTED |  |
| C452 | SUPPORTED |  |
| C453 | SUPPORTED |  |
| C454 | SUPPORTED |  |
| C455 | SUPPORTED |  |
| C456 | SUPPORTED |  |
| C457 | PARTIAL | rests on Modrinth alone (M456); condition added |
| C458 | SUPPORTED |  |

Label verdict: answered — the Summary gives a concrete ladder and a first build step, every claim it cites is SUPPORTED or a repaired PARTIAL, and the gapped items (dedicated-console export, boot times, Windows screenshots, CurseForge) are stated in it as unchecked, so an unfavourable result changes details of a rung, not the recommendation.
Quote check: agentic-mod-dev-2026-11-23.md: 123 quoted claim(s): 121 found, 0 NOT FOUND, 2 unfetchable, 0 no source

## Disposition

| Recommendation | State | Where |
|---|---|---|
| Строить конвейер как лестницу проверок, каждая ступень которой заканчивается кодом выхода или разбираемым логом | deferred | unfiled: research-сессии поручено только сохранить отчёт, без коммитов и задач; решение о задачах принимает запустившая её сессия планирования |
| Зафиксировать версию Minecraft и загрузчика и сказать агенту, какая версия документации действует | deferred | unfiled: версия выбирается в сессии планирования с учётом соседних отчётов; носитель правила — файл правил репозитория (AGENTS.md), потому что правило нужно в каждой сессии и дешевле носителя, который читается всегда, нет |
| Локальные исходники игры как контекст агента, не коммитить их в публичный репозиторий | deferred | unfiled: рабочего каталога мода ещё нет; носитель — запись в .gitignore плюс строка в файле правил, отдельная проверка не предлагается (инцидента не было) |
| Логику ядра селекции держать проверяемой юнит-тестами, без запуска мира | deferred | unfiled: архитектурное решение для сессии планирования, кода ещё нет |
| Первой построить одну команду-обёртку над сборкой, JUnit, GameTest и загрузкой сервера с разбором лога на пустом моде | deferred | unfiled: первая задача конвейера, её заводит сессия планирования после выбора версии |
| Вторым построить выгрузку рецептов и свою проверку достижимости графа прогрессии | deferred | unfiled: зависит от скелета и от непроверенного запуска `/kubejs export` из консоли сервера |
| Клиентские скриншоты третьими, оценку скриншотов отдавать не тому агенту, который писал код | deferred | unfiled: нужен отдельный research-прогон по клиентскому тестированию на NeoForge/Forge (см. Open questions) |
| Не закладывать внешних ботов-игроков (Mineflayer, Voyager) и симуляторы баланса в конвейер | dropped | это отказ от работы, а не работа: делать нечего, достаточно этой записи |
| Плейтест, оценку ощущения и темпа и необратимые действия публикации оставить за человеком | deferred | unfiled: войдёт в описание процесса в сессии планирования; носитель — файл правил репозитория |
