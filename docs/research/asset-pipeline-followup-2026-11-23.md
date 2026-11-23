---
topic: asset-pipeline-followup
tags: area:assets, research
source_provenance: /research follow-up requested by the farming-evolution planning session, 2026-11-23
status: open
audit: 2026-11-23 — 129 claims: 107 SUPPORTED, 21 PARTIAL, 0 UNSUPPORTED, 1 UNCHECKABLE
---

## Question

Дозапуск исследования по текстурам: пользователь решил закрыть открытые вопросы сейчас, а не перед этапом графики.
Сделай следующий прогон /research по разделу Open questions отчёта docs/research/asset-pipeline-2026-11-23.md и обнови сам отчёт (тот же файл или новый рядом, как требует скилл), чтобы он проходил check_artifact.py --gate со статусом answered или с честно названным остатком.
Что изменилось с первого прогона: платформа зафиксирована — Minecraft 1.21.1 + NeoForge (не 1.20.1 Forge), хребет технологий — Create; публикация — CurseForge и GitHub с раскрытием ИИ, Modrinth пересматривается к релизу; текстуры — программная генерация из шаблонов и палитр, генеративные сервисы только для черновиков.
Приоритет: (1) Terms of Use и Mod Author Terms CurseForge про ИИ-контент; (2) непрочитанные источники Q2 — Resourceful Bees, моды Chickens, документация NeoForge 1.21.1 по тинту и paletted_permutations, Dynamic Asset Generator; (3) автоматический клиентский скриншот на NeoForge 1.21.1; (4) лицензия на вывод Retro Diffusion.
Пункты, которые решаются только собственным пилотом (замер сервисов на 16×16, пилот модели животного), не запускай: оставь их в отчёте как пилоты с описанием, что именно мерить. Ничего не коммить.

Asked весь раздел Open questions первого отчёта, answering четыре приоритетных подвопроса, because правило скилла — не больше четырёх подвопросов на прогон, а заказчик сам назвал четыре приоритета. Вопросы, которые решает только собственный замер, по указанию заказчика не исследовались и записаны в Open questions как пилоты с тем, что мерить. Остальные пункты первого отчёта, не вошедшие в приоритеты (авторское право вне США, открытые коллекции ассетов, практика модерации Modrinth, реакция сообщества в моддинге Minecraft), перечислены там же как следующие прогоны.

Предыдущий отчёт: `docs/research/asset-pipeline-2026-11-23.md` (статус open). Он использован как указатель, не как источник. Что этот отчёт к нему добавляет и что отменяет:
- отменяет рабочее допущение «Minecraft 1.20.1 Forge»: всё про тинт, атласы и datagen здесь перечитано по документации NeoForge 1.21.1 и ванильным файлам 1.21.1;
- закрывает пробел «Terms of Use CurseForge не читались»: юридические документы Overwolf прочитаны;
- закрывает пробел «лицензия на вывод Retro Diffusion не прочитана»;
- заменяет вывод первого отчёта о клиентском скриншоте на Forge 1.20.1 выводом для NeoForge 1.21.1;
- дочитывает Resourceful Bees, моды линии Chickens и Dynamic Asset Generator.
Выводы первого отчёта о Modrinth, Mojang EULA, генеративных сервисах и моделях Blockbench/GeckoLib здесь не перепроверялись.

Sub-questions: Q1 — что юридические документы CurseForge и Overwolf говорят об ИИ-контенте, о гарантиях автора и о лицензии, и есть ли заявления Overwolf об ИИ-модах; Q2 — как на Minecraft 1.21.1 / NeoForge работают тинт и `paletted_permutations` и как Resourceful Bees, моды Chickens и Dynamic Asset Generator получают много вариантов из малого числа основ; Q3 — как на NeoForge 1.21.1 получить клиентский скриншот или экспорт иконок без человека; Q4 — на каких условиях Retro Diffusion отдаёт сгенерированный вывод.

## Summary

**CurseForge (Q1).** В документах Overwolf, которые связывают загрузку на CurseForge, — Mod Authors Terms, Platforms Terms of Use, Community Guidelines, условия Rewards Program — упоминаний ИИ нет [C107], [C108], [C109]; ни один из прочитанных документов прямо не запрещает ИИ-код или ИИ-ассеты и не требует их раскрытия [C112]. Единственное правило об ИИ для обычных загрузок — дисклеймер на витринных изображениях, изменённых ИИ, если они могут вводить в заблуждение [C132]; оно появилось между июнем и декабрём 2025 и с января 2026 не менялось [C133], [C134]. Автор гарантирует наличие прав и отсутствие нарушений чужих прав [C113], [C114], [C115], нарушение гарантии ведёт к удалению мода [C117] и возмещению претензий третьих лиц [C118]; само происхождение от ИИ нарушением не является [C119]. Лицензия Overwolf включает право изменять мод и делать производные работы [C121], исключительность сформулирована «на Платформе», и публикация на GitHub текстом не запрещена [C122], [C123], [C125]. Задеть проект могут только общие нормы: правило Fair Play о множестве похожих или малозатратных проектов и право Overwolf удалить мод за репутационный ущерб [C126], [C130], [C131]. Куда движется позиция Overwolf, видно по конкурсным правилам 2026 года: они запрещают ИИ-визуал и действуют только для конкурсных заявок [C140], [C142], [C143]; запрос на ИИ-метку висит в статусе «Future consideration» [C137], [C139]. Рекомендация: публиковать на CurseForge с добровольным раскрытием ИИ, изображения страницы — настоящие скриншоты из игры, в конкурсах Overwolf с ИИ-графикой не участвовать. Как модераторы поступают с ИИ-модами на практике, из документов не видно; общего заявления Overwolf об ИИ-модах не найдено.

**Тинт и палитры на 1.21.1 NeoForge (Q2).** Тинт работает как раньше: `tintindex` на гранях и обработчики `RegisterColorHandlersEvent.Block` и `.Item` [C201], [C202], [C203], у `item/generated` свой индекс на слой [C204], постоянный цвет можно задать прямо в JSON модели [C205]. В 1.21.4 тинт предметов переехал в `tints` клиентского JSON предмета, так что этот код при переносе придётся переписать [C206], [C208]. `paletted_permutations` есть в ванильном `atlases/blocks.json` 1.21.1 с полями `textures`, `palette_key`, `permutations` [C211]; поля `separator` в 1.21.1 нет, оно добавлено в 1.21.5 [C213]. Файлы атласов сливаются между паками [C215], [C216], и источник, добавленный паком в атлас `blocks`, текстуры генерирует, но анимацию `.png.mcmeta` основы не переносит [C217]. Что мод может поставлять собственную запись — вывод из этих фактов, готового мода с ванильным типом источника не найдено [C220]; в 1.21.11 источник переехал из `blocks.json` в `items.json` [C219]. Рекомендация: основной путь — генерировать готовые PNG скриптом при сборке, а не при загрузке игры: он не зависит от этого дрейфа версий и ограничения по анимации [C217], [C219], а автор Dynamic Asset Generator в ответе пользователю пишет, что для его задачи мод не сделает ничего, чего не может datagen [C234]. Dynamic Asset Generator для 1.21.1 NeoForge существует, но последний релиз — август 2024 [C233], [C242]; Moonlight Lib под 1.21.1 NeoForge обновляется [C241]. Тинт оставить для вариантов, число которых заранее не известно.

**Прецеденты (Q2).** Resourceful Bees множит варианты тинтом: модель блока соты с одной текстурой и `tintindex`, цвет соты в JSON, и до шести слоёв пчелы, каждый умножен на цвет; рядом в моде лежат и другие текстуры сущностей [C222], [C223], [C225], [C226], [C227]. Линия Chickens — обратный пример: в Modern Chickens под NeoForge 1.21.1 сотни отдельных PNG на породы, а умножение текстуры на цвет включается для пород с `generated_texture` или когда текстура породы не загрузилась [C229], [C230], [C231], [C232]. В обоих модах есть и отдельные текстуры на вариант, и тинт.

**Клиентский скриншот (Q3).** В самом NeoForge 1.21.1 клиентского теста со скриншотом нет: GameTest идёт на сервере, в документации и модуле testframework скриншотов нет [C302], [C305], [C306], [C307]; у ModDevGradle четыре типа запуска и ни одного клиентского тестового [C308]. Клиентский gametest Fabric в ветке Fabric API 1.21.1 отсутствует [C335], [C336], HeadlessMC в безголовом режиме ничего не рендерит [C337]. Скриншот предположительно можно собрать из частей, документированных по отдельности: запуск `client` с аргументами `--quickPlaySingleplayer`, `--width`, `--height` [C309], [C314], [C315] и собственный dev-код, который вызывает `Screenshot.takeScreenshot` на основном буфере и пишет PNG, как это делает Icon Exporter [C312]; что аргумент quick-play загружает названный мир и как этот код завершает клиент, из прочитанных источников не следует; рецепт никем не показан работающим от запуска до выхода и требует пилота [C316], [C345]. Готовое для иконок: CyclopsMC Icon Exporter под 1.21.1 NeoForge с командой `/iconexporter export` [C320], [C321]; команду всё равно должен кто-то подать, режима «запустить и выйти» нет [C323]. Для сущностей найден только Isometric Renders - Forked: экспорт кнопкой в GUI, без пакетного режима [C324], [C325], [C326].

**Retro Diffusion (Q4).** По условиям от 28 сентября 2026 вывод, полученный любым путём, включая API и MCP, принадлежит тому, кто его сгенерировал [C401], [C402]; автоматический доступ разрешён только через API, MCP и официальные приложения [C405]. Из процитированных условий использование вывода ограничивает запрет на обучение ИИ-моделей [C406], [C411]; атрибуция не требуется [C410]. Оговорки: вывод с бесплатного лимита Retro Diffusion вправе публично показывать [C407], поэтому черновики стоит делать на платном балансе; сам сервис пишет, что ИИ-контент может не охраняться авторским правом [C403], не обещает уникальности [C404] и не гарантирует отсутствия нарушений чужих прав [C409].

## Findings

### Q1 — что документы CurseForge и Overwolf говорят об ИИ-контенте, гарантиях автора и лицензии — answered

- **C101** [quoted] The Mod Authors Terms are accepted by using the platform or submitting a mod, not only by clicking accept — "By using the Platform, submitting your Mod or clicking "accept", you are entering into a legally binding agreement with Overwolf" — [S1], intro, 3rd paragraph
- **C102** [quoted] The Mod Authors Terms incorporate the Overwolf Community Guidelines and the CurseForge Moderation Guidelines by reference — "Your use of the Platform and the Platform API are further subject and governed by Overwolf Community Guidelines and Moderation Guidelines which are incorporated into the Terms by reference." — [S1], intro
- **C103** [quoted] The Platforms Terms of Use say a CurseForge mod author's relationship with Overwolf is governed by the Mod Authors Terms, not by the Platforms Terms — "a Mod Author of Curseforge Mods, the engagement with Overwolf and the usage of the Mod Author services provided by Overwolf, shall be subject to the Overwolf Mod Authors and Conditions" — [S2], intro, "Please note that these Terms govern…"
- **C104** [quoted] A modpack is a Mod under the Mod Authors Terms, and the definition covers code, digital items and any custom content — ""Mod(s)" means any code, software, a digital item (such as maps, skins and similar) or any custom content which is created by a Mod Author for a third party game." — [S1], §1 Definitions (the next sentence lists "modpacks")
- **C105** [measured] The Moderation Guidelines link inside the Mod Authors Terms (article 9000198907) is a stub dated 20 Mar 2024 that says the article moved; the live rules are the Moderation Policies article — Own measurements, M4
- **C106** [measured] www.curseforge.com/terms and www.overwolf.com/legal/author-terms/ return HTTP 404; www.overwolf.com/legal/terms/ redirects to the Overwolf website Terms of Use, which is about overwolf.com — Own measurements, M5
- **C107** [measured] The current Mod Authors Terms (last updated August 5, 2025) contain no mention of AI, artificial intelligence, generative or machine-generated content — Own measurements, M1
- **C108** [measured] The Platforms Terms of Use (last updated November 4, 2026) and the Community Guidelines (last updated March 01, 2024) contain no mention of AI, artificial intelligence or generative content — Own measurements, M1
- **C109** [measured] The Rewards Program Terms of Service (modified 4 Sep 2024), the CurseForge Website Rules (modified 9 Dec 2024) and the Overwolf website Terms of Use (June 23, 2024) contain no mention of AI apart from the support site's "ASK THE AI ASSISTANT" widget label — Own measurements, M2
- **C110** [quoted] The Community Guidelines' closest clause is on authenticity: they encourage original content and prohibit deceptive content including deep-fakes — "We do not tolerate any content that misrepresents the identity of other Users or is designed to deceive our Users. This includes deep-fakes, faked, impersonating, manipulated, or edited content that alters the original context of the material." — [S3], Authenticity
- **C112** [inference] None of the Overwolf documents read in this run (Mod Authors Terms, Platforms Terms of Use, Community Guidelines, Rewards Program Terms of Service, CurseForge Website Rules, Overwolf website Terms of Use) mentions AI, so none of them prohibits, restricts or requires disclosure of AI-generated code or assets by name; the one AI-specific rule found for ordinary uploads is the showcase-image disclaimer in the Moderation Policies — from [C107], [C108], [C109], [C119], [C132]
- **C113** [quoted] The author warrants having obtained, before upload, the rights needed for everything the Terms grant — "Before providing Overwolf or any End User the Mod, you will have obtained the rights necessary for the exercise of all rights granted under these Terms," — [S1], §4 Representations and Warranties, item ii
- **C114** [quoted] The author warrants the right to distribute the mod and that its use by Overwolf and end users will not violate third-party rights or any law — "You represent and warrant to Overwolf that you have the right to use, store, reproduce, transmit, display, perform, and distribute your Mod on the Platform, and that use of your Mod by Overwolf and its End Users, in accordance with Overwolf terms and policies, will not violate the rights of any third party or any applicable regulation or law" — [S1], §4
- **C115** [quoted] The author warrants that no mod infringes any intellectual-property or other right of anyone — "violate or infringe any intellectual property, proprietary, or other rights of any person or entity (including contractual rights, copyrights, trademarks, patents, trade dress, trade secret, common law rights, rights of publicity or privacy, or moral rights)" — [S1], §4, "None of your Mods or the sale, distribution, or promotion thereof will either:"
- **C117** [quoted] Breach of a warranty lets Overwolf remove the mod (the next sentence adds reduced visibility and withheld payments) — "If you violate any of the above representations, Overwolf reserves the right to remove your Mod from the Platform, in its sole discretion, for any reason or no reason at all, at any time." — [S1], §4, last paragraph
- **C118** [quoted] The author indemnifies Overwolf for third-party claims arising from a breach of warranties or from distribution of the mod — "(ii) Your breach of your representations, warranties, or obligations in these Terms; or (iii) the performance, promotion, sale, or distribution of your Mod." — [S1], §9 Indemnification
- **C119** [inference] The warranties are of having the necessary rights and of non-infringement; none of them requires that the author hold copyright in the mod or that a human authored it, so AI origin is not itself a breach, while an AI output that reproduces someone's protected work would be — from [C113], [C114], [C115], [C107]
- **C120** [quoted] The Terms leave ownership of the mod with the author and the author's licensors — "Mod Author and their licensors shall have all right, title and interest to all Mods they develop, subject to Overwolf’s and their licensors’ rights in the underlying Platform." — [S1], §5.3
- **C121** [quoted] The author grants Overwolf an unrestricted, worldwide, royalty-free licence that includes modifying the mod and making derivative works, limited to six listed purposes — "You grant Overwolf an unrestricted, worldwide, royalty-free license to use, store, reproduce, distribute, display, perform, operate, modify, adapt, and create derivative works of your Mod for the purpose of (a) operating and maintaining our Platform; (b) marketing, advertising and distributing of the Mods on the Platform" — [S1], §2 "License to Overwolf"
- **C122** [quoted] Two of the purposes — marketing and distributing the mod on the Platform, and using and sharing end-user usage data — are granted to Overwolf exclusively, and Overwolf is appointed the author's agent — "Without derogating from the foregoing, the rights set forth in subsections (b) and (e) are hereby granted exclusively to Overwolf. You hereby appoint Overwolf to be your agent for the above mentioned purposes." — [S1], §2 "License to Overwolf"
- **C123** [quoted] The author may withdraw mods at any time, and termination does not bar distributing them outside the platform — "provided that nothing herein shall preclude Mod Author from distributing Mods, or allowing End Users to access Mods, outside of the Platform" — [S1], §12.3 Effect of Termination
- **C125** [inference] The licence grant is not conditioned on the mod being copyrightable or human-authored; the text does not say the licence is perpetual or irrevocable, and exclusivity is worded as "on the Platform", which the termination clause's carve-out for off-platform distribution is consistent with (GitHub publication is not barred by the text) — from [C121], [C122], [C123]
- **C126** [quoted] Overwolf may reject, remove, suspend or limit visibility of a mod that violates law, terms or policies or that has an adverse reputational impact, without notice — "then Overwolf may reject, remove, suspend or limit the visibility of your Mod on the Platform, at any time and without notice to you" — [S1], §3 (paragraph "Unless agreed upon otherwise by Overwolf…"; ground (c) includes "an adverse economic, reputational or security-related impact")
- **C127** [quoted] Rewards Program eligibility is an individual aged 18+ with an account who opts in — "You must be an individual person at least 18 years of age to register for the Rewards Program." — [S4], §3 Eligibility
- **C129** [quoted] The Rewards "Fair Play" section bans gaming the system and misleading players about what a project is, with a platform ban as sanction — "anything that could take advantage of or mislead players into downloading your project for anything other than what it actually is" — [S4], §9 Fair Play
- **C130** [quoted] The Moderation Policies' Fair Play rule bans multiple similar or low-effort projects uploaded to game the system; it does not name AI — "It is not allowed to upload multiple similar and/or low effort projects in an attempt to game the system." — [S5], General CurseForge Moderation Policies, "Fair Play"
- **C131** [inference] The clauses that could reach an AI-built project are generic: the low-effort/multiple-projects Fair Play rule, the misleading-players rule and Overwolf's reputational-impact removal discretion; a single substantive mod plus one modpack is outside the wording of the Fair Play rule, which targets multiple similar or low-effort uploads — from [C126], [C129], [C130]
- **C132** [quoted] The only AI rule in the Moderation Policies is a disclaimer requirement for AI-modified showcase images that may misrepresent the mod; it is conditional on possible misrepresentation and covers images, not code or in-mod assets — "Any AI-modified (generated, edited, or enhanced) showcase image that may misrepresent the mod’s actual content must include a clear and visible disclaimer (e.g., on the image or in the description)." — [S5], General CurseForge Moderation Policies, "AI Misleading Content Disclosure"
- **C133** [measured] The Moderation Policies page was last modified Thu, 22 Oct, 2026; the Wayback snapshot of 2026-01-18 shows "Modified on: Wed, 31 Dec, 2025" and already has the same AI rule; the snapshot of 2025-06-20 (modified 10 Jun 2025) does not have it, so the AI rule was added between June and December 2025 — Own measurements, M3
- **C134** [measured] Compared with the 2026-01-18 snapshot the current page has a new "Project Variants - Merge projects" rule; the AI rule's wording is unchanged and no other AI text was added — Own measurements, M3
- **C137** [quoted] CF-I-7917 (created Feb 10 2026) carries the status "Future consideration" and 24 votes; the page shows three guest comments and two merged ideas, and no staff or official reply text — "Future consideration" — [S6], status badge under the idea body; comments section
- **C138** [quoted] Two later requests for an AI tag on mods (created 07 Nov and 19 Nov) were merged into CF-I-7917 — "(AI tag) should be added to any mods made using AI or made with the help of AI" — [S6], Merged ideas (2)
- **C139** [inference] As of 2026-11-23 the only official answer to the AI-labelling request is the portal status "Future consideration"; no AI tag or labelling feature has been announced on that page — from [C137], [C138]
- **C140** [quoted] Overwolf's Hytale New Worlds contest rules (contest March 3 – April 28) prohibit AI-generated visual assets on pain of disqualification — "The use of artificial intelligence to generate or create visual assets, artwork, logos, project icons, or other graphical elements for a Contest entry is strictly prohibited and will result in disqualification." — [S7], Official Rules, "AI Usage - Reserved"
- **C141** [quoted] The same rules treat other AI use as permitted but subject to disclosure, with disqualification for non-disclosure or misrepresentation — "Failure to disclose permitted AI usage, or any misrepresentation regarding the nature or extent of AI involvement, may result in disqualification." — [S7], Official Rules, "AI Usage - Reserved"
- **C142** [quoted] The Minecraft ModJam 2026 rules (start July 21st, 2026) prohibit AI-generated project avatars and gallery images and say nothing on AI code — "AI-generated project avatars or gallery images are prohibited." — [S8], Submission requirements; repeated in §4 How to Enter
- **C143** [inference] Overwolf's 2026 written positions on AI are contest-scoped and aimed at AI-made visuals (avatars, gallery images, artwork, in the Hytale contest all visual assets); in none of the documents read is AI-written code prohibited, and the Hytale rules imply it is permitted with disclosure. These rules bind contest entries only, not ordinary uploads — from [C140], [C141], [C142], [C112]

### Q2 — тинт и paletted_permutations на 1.21.1 NeoForge; Resourceful Bees, Chickens, Dynamic Asset Generator — answered with a gap

- **C201** [quoted] NeoForge 1.21.1 docs: a model element face may carry `tintindex`, used by a color handler; default -1 = no tinting — "`tintindex`: Optional. Specifies a tint index that may be used by a color handler, see [Tinting][tinting] for more information. Defaults to -1, which means no tinting." — [S9], doc 1.21.1, Elements
- **C202** [quoted] NeoForge 1.21.1 docs: tinting works through two events, block color handlers (`RegisterColorHandlersEvent.Block`, lambda state/level/pos/tintIndex, ARGB result) and item color handlers — "The code side of things works through two events, one for block color handlers and one for item color handlers." — [S9], doc 1.21.1, Tinting; code below it: "event.register((state, level, pos, tintIndex) -> {"
- **C203** [quoted] NeoForge 1.21.1 docs: item handlers register in `RegisterColorHandlersEvent.Item` with a lambda (stack, tintIndex) — 1.21.1 still uses the ItemColor-style handler — "public static void registerItemColorHandlers(RegisterColorHandlersEvent.Item event) {" — [S9], doc 1.21.1, Tinting; comment there: "// Parameters are the item stack and the tint index."
- **C204** [quoted] NeoForge 1.21.1 docs: `item/generated` gives each layer its own tint index (layer0 → 0, layer1 → 1); a block item needs an item color handler of its own — "Be aware that the `item/generated` model specifies tint indices for its various layers - `layer0` has tint index 0, `layer1` has tint index 1, etc. Also, remember that block items are items, not blocks, and require an item color handler to be colored." — [S9], doc 1.21.1, Tinting
- **C205** [quoted] NeoForge 1.21.1 docs: a constant ARGB `color` can be set in model JSON (extra face data; per layer of `item/generated` via the `neoforge:item_layers` loader) instead of a handler — "This can be used as a replacement for tinting if the color values are constant." — [S9], doc 1.21.1, Elements, extra face data; next paragraph: "Using the custom `neoforge:item_layers` loader, you can also specify extra face data to apply to all the geometry in an `item/generated` model."
- **C206** [quoted] NeoForge 1.21.4 docs (not 1.21.1): item tinting moved to a `tints` list of `ItemTintSource`s in the client item JSON; list position = tintindex — "These are known as `ItemTintSource`s, which are defined in `ItemTintSources`. They also have a `type` field to define which source to use. The `tintindex` they are applied to is specified by their index within the list." — [S10], doc 1.21.4, Tinting
- **C208** [inference] Item tint code written for 1.21.1 (`RegisterColorHandlersEvent.Item`) must become tint sources in item JSON when porting to 1.21.4+; block tinting (`RegisterColorHandlersEvent.Block` + `tintindex`) is documented for 1.21.1 — from [C202], [C203], [C206]
- **C209** [quoted] NeoForge 1.21.1 docs: client model datagen = `BlockStateProvider` (with internal `BlockModelProvider`/`ItemModelProvider`) and `ItemModelProvider`; a `ModelBuilder` has `#element()` for faces — "While item model datagen directly extends `ItemModelProvider`, block model datagen uses the `BlockStateProvider` base class, which has an internal `BlockModelProvider` that can be accessed via `BlockStateProvider#models()`." — [S11], doc 1.21.1, ModelProvider
- **C210** [measured] The NeoForge 1.21.1 docs have no page on texture atlases or atlas sources: `textures.md` covers PNG location, `.mcmeta` and animation only — Own measurements, M6
- **C211** [quoted] Vanilla 1.21.1 `atlases/blocks.json` itself contains a `paletted_permutations` source (item armour-trim overlays) with three fields besides `type`: `textures`, `palette_key`, `permutations` — "\"palette_key\": \"trims/color_palettes/trim_palette\"," — [S12], last entry of `sources`
- **C212** [measured] Vanilla 1.21.1 derives 4×14 = 56 item sprites (blocks.json) and 36×14 = 504 armour sprites (armor_trims.json) from 40 grey base textures, 14 palette files and one key — Own measurements, M7
- **C213** [quoted] `separator` does not exist in 1.21.1: Mojang lists it as a new field in the 1.21.5 release notes — "separator - optional string (default: _) - value to be used when joining texture and permutation names to produce sprite name" — [S13], "minecraft:paletted_permutations Sprite Source", under "New field:"
- **C214** [inference] In 1.21.1 a generated sprite is always named `<texture>_<permutation>`; the joiner is fixed until 1.21.5 — from [C211], [C213]
- **C215** [quoted] Official statement that atlas files merge across packs (Mojang 1.19.3 notes, where `atlases/` was introduced) — "If multiple packs include those files, they are merged by their order within the pack (same as tags in data packs)" — [S14], Texture loading changes, Atlas configuration files
- **C216** [quoted] Same notes: every `sources` entry runs in order, adding or removing files, so an extra file extends the list; `blocks` is the atlas of block and item models — "Every entry in sources runs in during load, in order of definition, adding or removing new files to the texture list; to be later referenced by block models, particles, etc" — [S14], Atlas configuration files; also "blocks - textures used by block and item models"
- **C217** [quoted] A user report (affects 1.20.4–1.21.3) shows a pack-added `paletted_permutations` source in the `blocks` atlas does generate textures; the limit: `.png.mcmeta` (animation) of the base is not copied — "The dynamically generated texture gets generated, but it does not take on the resource metadata of the base texture." — [S15], description, read via the tracker's JSON API; steps: "Add a \"paletted_permutations\" atlas source to an existing atlas (blocks is used in the example)"; resolution field "Invalid"
- **C218** [quoted] Dynamic Asset Generator's docs describe placing a source of its own custom type in `assets/minecraft/atlases/blocks.json`, and its repo holds such a file in a test resource pack on branch 1.21.x; this is a docs example and a dev test pack, not a released mod shipping the file — "For instance, if the following were placed at `assets/minecraft/atlases/blocks.json`:" — [S16], Sprite Sources; same file on branch 1.21.x at `neoforge/run/resourcepacks/test/assets/minecraft/atlases/blocks.json`
- **C219** [measured] Version drift: `blocks.json` holds the `paletted_permutations` source in 1.21.1–1.21.9; in 1.21.11 it is gone from `blocks.json` and sits in a new `items.json` — Own measurements, M8
- **C220** [inference] Mojang's merge rule and two pack-level uses (a user's resource pack with the vanilla source type, Dynamic Asset Generator's docs example and test pack with a custom type) imply that on 1.21.1 a mod should be able to ship `assets/minecraft/atlases/blocks.json` with its own `paletted_permutations` entry (grey template, palette key, one palette PNG per line) and use sprites `<texture>_<permutation>`; this is untested: no NeoForge doc page states it and no source read shows a mod doing it; animated bases lose animation — from [C210], [C215], [C216], [C217], [C218]
- **C221** [measured] Repo `Team-Resourceful/ResourcefulBees` is alive (default branch `26.3`, pushed 2026-10-26) and has a `1.21.x` branch (last commit 2025-03-31), but Modrinth has no 1.21.1 release: 11 versions, for 1.16.5/1.19.2/26.2/26.3 — Own measurements, M10
- **C222** [quoted] Bees are JSON-defined; rendering is a list of layers, each a colour plus a texture path, up to 6 — "\"texture\": \"path to texture, can have multiple layers up to 6\"," — [S17], RenderData.layers[0]; sibling: "\"color\": \"either a color name or a hex color, can also be named rainbow\","
- **C223** [quoted] The layer colour is a render-time vertex colour multiplier on the layer texture; no new texture is baked — "LivingEntityRenderer.getOverlayCoords(bee, 0.0F), layerData.color().getFloatRed(), layerData.color().getFloatGreen(), layerData.color().getFloatBlue(), 1.0F);" — [S18], method renderNone
- **C224** [measured] The mod ships generic layer textures (`entity/custom/`: bee, primary_layer, secondary_layer, emissive_layer + angry variants, 6 files; 6 more in `entity/ores/custom/`) beside other entity textures: 146 PNGs under `textures/entity/`, 433 in the branch — Own measurements, M11
- **C225** [quoted] Honeycombs are data: one JSON per comb with a hex colour and no texture name — "\"color\": \"#00ffff\"," — [S19], honeycomb object
- **C226** [quoted] The honeycomb block model uses one texture (`resourcefulbees:block/honeycomb_block`) with `tintindex: 0` on every face — "\"north\": {\"uv\": [0, 0, 16, 16], \"texture\": \"#0\", \"tintindex\": 0, \"cullface\": \"north\"}," — [S20], elements[0].faces
- **C227** [inference] Resourceful Bees multiplies variants by tint, not palette swap: the honeycomb block model is tinted through `tintindex` 0 and each comb's JSON carries a colour and no texture name; bees = up to 6 stacked layers each multiplied by a JSON colour, shipped beside generic layer textures and many other entity textures — from [C222], [C223], [C224], [C225], [C226]
- **C228** [measured] Original `setycz/ChickensMod` (branches 1.9–1.12.2): 46 PNGs in `textures/entity` — a separate entity texture file per chicken; 5 PNGs in `textures/items` — Own measurements, M12
- **C229** [measured] `Modern-Mods/ModernChickens` (port of Chickens + Roost, branch `Neo/1.21.1`, pushed 2026-11-13, 22 Modrinth versions for 1.21.1, all NeoForge): 371 PNGs directly in `textures/entity`, 286 in `textures/item/chicken`, 167 in `textures/entity/megachicken` — per-breed files are the main mechanism — Own measurements, M13
- **C230** [quoted] Modern Chickens has a tint fallback for config-defined or generated breeds — "When `generated_texture` is enabled, the renderer tints the referenced texture so in-world chickens match the colours used for the item and spawn egg. If the texture cannot be loaded, the base bone chicken sprite is used instead, and tinted." — [S21], custom chickens, notes after the JSON example
- **C231** [quoted] The fallback is runtime image generation: per breed a `NativeImage` is built by multiplying a grey template's shade by the colour per pixel and registered as a `DynamicTexture`, outside any atlas — "int tintedRed = red * shade / 255;" — [S22], method tintLayer; earlier: "Minecraft.getInstance().getTextureManager().register(id, texture);"
- **C232** [inference] The Chickens lineage is the counter-example to template-plus-tint: a separate PNG per breed for entity and for item (hundreds of files in Modern Chickens); multiply-tint is used for breeds with `generated_texture` enabled or whose texture cannot be loaded — from [C228], [C229], [C230], [C231]
- **C233** [measured] DynAssetGen has a 1.21.1 NeoForge build, and it is the last release for any version: `6.1.2`, 2024-08-13 (NeoForge and Fabric/Quilt); repo `lukebemishprojects/DynamicAssetGenerator` (redirect from `lukebemish/dynamic_asset_generator`) not archived, last push 2024-09-09 — Own measurements, M14
- **C234** [quoted] Maintainer, 2026-03, on the issue "Is this mod discontinued in later updates": no update date given, and for what the asker needs the mod can do nothing datagen cannot, it only generates at runtime instead — "I'll get to updating it eventually as life is busy. But in terms of what you need -- it's not going to be able to do anything datagen can't, it just does generation at runtime instead." — [S23], comment by lukebemish (member), read with `gh issue view`
- **C235** [quoted] Purpose per its docs: generate assets at runtime instead of at build time (listed uses: match the player's resource packs; build on vanilla assets without redistributing them) — "Dynamic Asset Generator (or DynAssetGen) is a minecraft mod meant to help dynamically generate assets and data at runtime, as opposed to creating them manually or when the mod is compiled." — [S24], first paragraph
- **C237** [quoted] `palette_combined`: a palette is extracted from `background`, a grey `paletted` image maps into it (average RGB picks the colour), then background, recoloured image and `overlay` are stacked — "The `paletted` image is then used as a sort of \"map\" for this palette, with the average RGB value being used to select a color from the palette." — [S25], body; preceding sentence: "First, a palette of colors is extracted from `background`."
- **C239** [quoted] Its texture sources can also run as an atlas sprite source (`dynamic_asset_generator:tex_sources`), compared by the docs to vanilla paletted permutations — "Dynamic Asset Generator's texture sources can also be used as a sprite source in a texture atlas, similar to vanilla's paletted" — [S16], Sprite Sources (sentence continues on the next line: "permutations.")
- **C240** [measured] The docs list these texture sources: channel_route, color, crop, error, fallback, file, foreground_transfer, overlay (layers a list, first at the back), palette_combined, palette_spread (texture → evenly spaced greyscale), shadowed, splitter, spread, transform, mask + 7 mask generators (add, channel, cutoff, edge, grow, invert, multiply) — Own measurements, M15
- **C241** [measured] Moonlight Lib has 1.21.1 NeoForge builds and is actively updated: 431 Modrinth versions for 1.21.1 (215 NeoForge, 216 Fabric), newest NeoForge `1.21.1-3.8.3` of 2026-11-09 — Own measurements, M16
- **C242** [inference] For a mod that owns its templates and knows its palettes at build time, DynAssetGen adds a runtime dependency frozen at an 2024-08 build and gives nothing a build-time script cannot; its value is matching textures the mod does not own — from [C233], [C234], [C235]

### Q3 — клиентский скриншот или экспорт иконок без человека на NeoForge 1.21.1 — answered with a gap

- **C301** [quoted] NeoForge 1.21.1 docs describe Game Tests as in-game unit tests — "Game Tests are a way to run in-game unit tests. The system was designed to be scalable and in parallel to run large numbers of different tests efficiently." — [S26], intro
- **C302** [quoted] The dedicated automated run is the Game Test Server, a build server whose exit code is the number of failed required tests; it is started with gradlew runGameTestServer — "The Game Test Server is a special configuration which runs a build server. The build server returns an exit code of the number of required, failed Game Tests" — [S26], Running Game Tests / The Game Test Server
- **C303** [quoted] Game Tests are also enabled inside the client run configuration, not only on the game test server — "By default, only the client, server, and gameTestServer run configurations have Game Tests enabled." — [S26], Enabling Other Run Configurations
- **C304** [quoted] Which test namespaces load is controlled by the system property neoforge.enabledGameTestNamespaces — "enabledGameTestNamespaces to a string specifying each namespace separated by a comma. If the property is empty or not set, then all namespaces will be loaded" — [S26], Registering Game Tests / namespaces
- **C305** [measured] The NeoForge 1.21.1 GameTest docs page contains no occurrence of the word "screenshot" (0 matches), i.e. it documents no screenshot facility — Own measurements, M17
- **C306** [measured] NeoForge branch 1.21.1 `testframework` module has a `client` package (AbstractTestScreen, TestScreen, TestsOverlay, ClientUtils, FrameworkClientImpl, CommitBasedList) but no file with "screenshot" in its path; the only screenshot-named files in the repo are the vanilla patch Screenshot.java.patch and client/event/ScreenshotEvent.java — Own measurements, M18
- **C307** [inference] NeoForge 1.21.1 itself ships no client-side game test or screenshot-test facility; its test tooling is server-side GameTest plus an in-game test UI overlay — from [C302], [C305], [C306], [C310]
- **C308** [quoted] ModDevGradle supports exactly four run types; none is a client test run — "Every run must have a type. Currently, the supported types are `client`, `data`, `gameTestServer`, `server`." — [S27], More Configuration / Runs
- **C309** [quoted] A run can be given arbitrary program arguments, JVM arguments and system properties from the build script (the route for passing --quickPlaySingleplayer or a dev-only -D flag to runClient) — "programArguments = ["--arg"]" — [S27], More Configuration / Runs
- **C310** [quoted] ModDevGradle's JUnit integration (neoForge.unitTest.enable()) can load a Minecraft server via the NeoForge test framework; the README names only a server context — "With the NeoForge test framework, you can run your unit tests in the context of a Minecraft server:" — [S27], Unit testing with JUnit / Loading a server
- **C311** [measured] The ModDevGradle README (main, fetched 2026-11-23) has 0 lines matching "quickplay" or "screenshot" (case-insensitive): no built-in quick-play or client-screenshot feature is documented there — Own measurements, M19
- **C312** [quoted] In 1.21.1 (mojmap) mod code can call `Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())` to get a NativeImage of the main framebuffer and `image.writeToFile(file)` to save it as PNG; CyclopsMC IconExporter's 1.21.1 branch does exactly this — "NativeImage imageFull = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget());" — [S28], exportImageFromScreenshot (branch gradle.properties: minecraft_version=1.21.1, neoforge_version=21.1.2)
- **C313** [quoted] NeoForge 1.21.1 patches vanilla Screenshot to fire a ScreenshotEvent that can cancel the save or redirect the target file, then writes the PNG on the IO pool — "+        net.neoforged.neoforge.client.event.ScreenshotEvent event = net.neoforged.neoforge.client.ClientHooks.onScreenshot(nativeimage, file2);" — [S29], hunk @@ -54,15
- **C314** [quoted] Mojang's own 1.21.1 version manifest lists a `--quickPlaySingleplayer <value>` game argument for the vanilla client, under the launcher feature rule `is_quick_play_singleplayer` — ""value": ["--quickPlaySingleplayer", "${quickPlaySingleplayer}"]" — [S30], arguments.game, rule is_quick_play_singleplayer
- **C315** [quoted] The same manifest lists `--width` / `--height` program arguments for a fixed window resolution — ""value": ["--width", "${resolution_width}", "--height", "${resolution_height}"]" — [S30], arguments.game, rule has_custom_resolution
- **C316** [inference] A human-free dev screenshot on NeoForge 1.21.1 could be assembled from parts that the sources document separately: a `client` run in ModDevGradle with programArguments `--quickPlaySingleplayer <world>` (and --width/--height), plus own dev-only client code (gated by a system property) that calls Screenshot.takeScreenshot on the main render target and writes the PNG. That the quick-play argument loads the named world, and how that code then stops the client, are not from a source read in this run. No source read shows this whole chain working on NeoForge 1.21.1; untested — from [C308], [C309], [C312], [C314], [C315]
- **C317** [prior] The 1.21.1 mojmap signatures `Screenshot.grab(File gameDirectory, RenderTarget, Consumer<Component>)` and `Minecraft#stop()` / `Minecraft#execute` for scheduling on the render thread; vanilla GUI item rendering into an off-screen RenderTarget (TextureTarget) — not fetched in this run
- **C320** [measured] CyclopsMC Icon Exporter (Modrinth slug `icon-exporter`) has 33 versions for 1.21.1 + NeoForge, newest 1.21.1-1.4.2-220 (2026-09-01); its required library Cyclops Core has 124 (newest 1.21.1-1.30.2, 2026-11-15) — Own measurements, M20
- **C321** [quoted] Icon Exporter exports every item/block icon (vanilla and modded) to transparent PNGs via the client command `/iconexporter export [size]`, output in `icon-exports-x64` in the game directory; export can be filtered by mod — "Run the <code>/iconexporter export</code> command. <em>(Optionally, the image size can be passed, defaults to <code>64</code>)</em>" — [S31], body, "How to use"
- **C323** [inference] Icon Exporter's PNGs are real client renders: it reads the main framebuffer (C312), then keys out the background colour; it needs a rendering client and something to issue the command — its description offers no launch-and-exit mode — from [C312], [C321]
- **C324** [measured] "Isometric Renders - Forked" (slug `isometric-renders-forked`, loaders: neoforge only) has 2 versions for 1.21.1 + NeoForge (0.3.0 2026-07-21, 0.2.0 2026-05-05); the original glisco "Isometric Renders" (slug `isometric-renders`) lists loaders fabric,quilt and 0 versions for 1.21.1 + NeoForge — Own measurements, M20
- **C325** [quoted] Isometric Renders - Forked renders items, tooltips, blocks, block entities, entities and world areas to PNG via client `/isorender` commands, but export is a GUI button — "After opening the render preview GUI, use the **Export** button to save a PNG." — [S32], body, Exporting
- **C326** [quoted] The fork lacks batch rendering and has known render defects — "Batch rendering, item atlases, animation export, and the original owo-ui interface are not currently included. Tweaks to rendering also need to be made to remove odd shadows and issues with tile entities." — [S32], body, note
- **C327** [measured] "Item Icon Exporter" (slug `item-icon-exporter`, loaders forge,neoforge, client-only, no source URL listed) has 4 versions for 1.21.1 + NeoForge, newest 1.0.3-NeoForge-1.21.1 (2026-05-21) — Own measurements, M20
- **C328** [quoted] Item Icon Exporter exports the held item or inventory items to transparent PNGs by keypress — "(Default Keys: `Numpad 4` for Held Item, `Numpad 5` for Inventory)" — [S33], body, Key Features
- **C329** [quoted] Item Icon Exporter 1.0.3 adds an `/itemexport` command so export does not depend on keybindings — "Introduced the /itemexport command system, allowing users to export items without relying on keybindings." — [S34], What's New, changelog of file item_icon_exporter-1.0.3-NeoForge-1.21.1.jar
- **C330** [measured] No 1.21.1 + NeoForge build on Modrinth for: BlockExporter (`blockexporter`, 0 versions; has neoforge builds for other MC versions), Block Renderer (`block-renderer`, loaders fabric,forge, 0 versions). Slugs `iconexporter`, `blockrenderer`, `mob-renders`, `itemrender`, `mineclient-bridge`, `hmc-specifics` return HTTP 404 on Modrinth — Own measurements, M20
- **C331** [measured] KubeJS (29 versions) and ProbeJS (2 versions, newest 8.0.3) exist for 1.21.1 + NeoForge; no source read in this run says either can dump item/block renders to PNG — Own measurements, M20
- **C332** [quoted] "MineClient Bridge" (CurseForge) describes itself as a client-side NeoForge 1.21.1 mod, intended among other things for automated testing, that captures the presented framebuffer as PNG; it is enabled by default on a local port (127.0.0.1:38121) — "Captures the current presented framebuffer as PNG." — [S35], Features; first paragraph: "MineClient Bridge is a client-side NeoForge 1.21.1 mod"
- **C333** [quoted] "Client Runtime Interface Toolkit" (CurseForge) describes itself as a client-side test kit where automated tests drive a real client, operate GUIs, send input and take screenshots; loader/version support not confirmed — "Client-side test kit for Minecraft: automated tests drive a real client, operate GUIs, send input, take screenshots and check the client state." — [S36], project summary line
- **C334** [quoted] Fabric (current docs) provides client game tests as an end-to-end facility, run with the runClientGameTest Gradle task, with a takeScreenshot call and screenshots under build/run/clientGameTest/screenshots — "Fabric additionally provides client game tests for testing client-side features, similar to an end-to-end test." — [S37], intro (page also contains `takeScreenshot("example-mod-singleplayer-test");`)
- **C335** [measured] The FabricMC/fabric repo branch `1.21.1` contains only `fabric-gametest-api-v1`; `fabric-client-gametest-api-v1` appears on branch `1.21.4`. So Fabric's client gametest API is not part of Fabric API for 1.21.1 — Own measurements, M21
- **C336** [inference] Fabric's client gametest screenshot API is not available to a 1.21.1 project even on the Fabric side of a multi-loader setup, so neither a multi-loader build nor Sinytra Connector can bring it to NeoForge 1.21.1; no source found in this run claims otherwise — from [C335]
- **C337** [quoted] HeadlessMC's headless mode works by patching LWJGL so that nothing is rendered — "This is achieved by patching the LWJGL library to not render anything" — [S38], Headless Launch
- **C338** [quoted] With a virtual display (xvfb) the LWJGL patch flag is not needed — "If you run the game with xvfb the -lwjgl flag is not needed." — [S39], configuration section
- **C339** [quoted] mc-runtime-test's mod joins a singleplayer world, optionally runs registered gametests and exits with a status code; it supports NeoForge for 1.21–1.21.11 — "When the game is ready, they join a SinglePlayer world, possibly execute all registered" — [S39], intro; support table row "1.21 - 1.21.11" has a check mark in the Forge, Fabric and NeoForge columns
- **C340** [quoted] mc-runtime-test states it is not an integration-test framework — "MC-Runtime-Test does not provide a framework for full integration tests." — [S39], Running your own tests
- **C341** [quoted] On NeoForge, gametest discovery in a production (non-dev) client does not work out of the box with mc-runtime-test — "On Neoforge/Lexforge gametest discovery does really not work in production, you might need to register" — [S39], Running your own tests
- **C342** [measured] mc-runtime-test has one Modrinth version for 1.21.1 + NeoForge: 5.0.1 (2026-10-21, mc-runtime-test-1.21.1-5.0.1-neoforge-release.jar) — Own measurements, M20
- **C343** [quoted] HeadlessMC's companion mod hmc-specifics lets the launcher's command line press keys in the running game, including F2 for a screenshot — "This way you can e.g. take a screenshot:" — [S41], Keys and Screenshots (preceded by "To simulate a key press use the key command.", followed by `> key f2`)
- **C345** [inference] For a Windows 11 desktop session, the sources read name the LWJGL patch and a virtual display only as means of running the client without a screen; documented automation inputs are quick-play and resolution args (C314, C315) passed via the run's programArguments (C309). Nothing read in this run documents a blocker specific to launching the client from a CLI agent; that `gradlew runClient` then renders normally in a window is not from a source — from [C309], [C314], [C315], [C337], [C338]
- **C346** [quoted] Reported failure mode for unattended captures: a window that is minimized or fully covered may not be rendered by the compositor (Mojang bug MC-270184, GNOME Mutter, i.e. Linux), and minimizing while paused logs "Invalid framebuffer operation" (MC-99770). No Windows-specific report of black in-game screenshots found — "Mutter (the GNOME compositor) seems to have an optimization, where if the window is fully covered by another window or the it is minimized" — [S42], description, Notes; read via the tracker's JSON API

### Q4 — условия Retro Diffusion на сгенерированный вывод — answered with a gap

- **C401** [quoted] Retro Diffusion's Terms of Service were last updated on 2026-09-28 and cover the website, the API and the official MCP server — "These Terms cover the website at retrodiffusion.ai, the Retro Diffusion API, the official MCP server, our official apps and extensions, and related services (together, the "Services")." — [S43], section 1, Agreement; header "Last updated September 28, 2026"
- **C402** [quoted] Under Retro Diffusion's terms, outputs generated by any route (website, API, MCP, plan, balance or free usage) are owned by the person who generated them as between that person and the company, which assigns its own rights to them — "As between you and us, outputs are owned by the person who generated them, and we assign to you any rights we have in them." — [S43], section 6, Outputs are yours
- **C403** [quoted] The same terms warn that AI-generated content may not be protected by copyright depending on jurisdiction — "Depending on where you live, AI-generated content may not be protected by copyright." — [S43], section 6, Outputs are yours
- **C404** [quoted] The same terms give no promise that an output is unique or exclusive to the user — "Other users may generate similar content, and we don't promise that your outputs are unique or exclusive to you." — [S43], section 6, Outputs are yours
- **C405** [quoted] Automated use is allowed only through the official API, MCP server and official apps — "Automated use through the Approved Access Points is allowed, subject to these Terms and any limits we set." — [S43], section 4, Automated access, the API and MCP
- **C406** [quoted] Outputs may not be used to train or improve any AI model without written permission, and this holds although the user owns the outputs — "Unless we've given you explicit written permission, you may not use outputs, or any other content from the Services, to train, fine-tune, distill, evaluate or otherwise improve any AI or machine-learning model." — [S43], section 7, No training on outputs
- **C407** [quoted] Outputs generated with free usage are licensed to Retro Diffusion for public display and promotion — "You grant us a worldwide, non-exclusive, royalty-free license to display, reproduce and distribute the following publicly, in the Services and in promotion of the Services (for example our website, social media and ads):" — [S43], section 6, Public content; first list item "outputs generated using free usage"
- **C408** [quoted] Retro Diffusion keeps a perpetual licence to store and process the user's outputs and to train its own models on them — "For outputs, this license also covers using them to train, evaluate and improve our models and Services (see section 7)." — [S43], section 6, Our license to store and access your content
- **C409** [quoted] Retro Diffusion disclaims the non-infringement warranty and says outputs may be similar to others' outputs; checking them is the user's responsibility — "Outputs are generated by AI and may be inaccurate, similar to others' outputs, or unsuitable for your purpose. You're responsible for checking them before you use them." — [S43], section 15, Disclaimers
- **C410** [quoted] Attribution to Retro Diffusion is not required for products built on it — "Attribution to Retro Diffusion isn't required." — [S43], section 5, Building on Retro Diffusion
- **C411** [inference] Of the clauses quoted here, the one that restricts what a user does with outputs is the ban on AI training; ownership sits with the person who generated them and attribution is not required, and none of the quoted clauses restricts commercial use or redistribution of outputs inside a published game or mod — from [C402], [C406], [C410]

## Disconfirming evidence

- [C112] — query: `CurseForge bans AI generated mods removed mod "AI generated" moderation` — nothing found against: в выдаче страница Moderation Policies, запрос CF-I-7917 и политики Modrinth и Nexus Mods; запрета, объявления об удалении или заявления сотрудников CurseForge нет.
- [C112], [C119] — query: `"CurseForge" "AI" mod rejected OR banned OR "not allowed" "AI-generated code" Overwolf policy announcement blog` — против общего вывода ничего; найдены только конкурсные правила, записанные как [C140], [C141], [C142].
- [C220] — query: `"paletted_permutations" mod "atlases/blocks.json" custom item textures neoforge OR fabric issue not working` — сообщений о том, что моду не удалось добавить источник, нет; найдены ограничение по анимации [C217] и переезд источника в другой атлас в поздних версиях [C219].
- [C242] — query: `"Dynamic Asset Generator" mod slow load time OR discontinued OR "no longer" 1.21 runtime texture generation problems` — разбора причин отказа нет; найден ответ сопровождающего [C234] и закрытый без установленной причины отчёт о долгой загрузке (issue 85, не процитирован).
- [C227], [C232] — отдельного поиска не было; противоположные примеры найдены чтением репозиториев и стоят друг против друга: тинт как основной механизм [C227] и отдельный файл на породу [C232].
- [C307] — query: `NeoForge client game test screenshot automated 1.21.1 "client" gametest issue neoforged` — клиентских gametest у NeoForge не найдено; всплыли сторонние моды [C332], [C333]. Поиски по issue `neoforged` (`client gametest`, `client test screenshot`, `client game tests`, `screenshot test`) вернули пустой список — слабое свидетельство.
- [C316], [C345] — query: `Minecraft screenshot black OR blank when window minimized OR unfocused framebuffer automated test mod "Screenshot.grab" OR "takeScreenshot" issue` — найдено только [C346] (свёрнутое или перекрытое окно на Linux); про Windows ничего.
- [C402], [C411] — поиска не было; оговорки взяты из самого документа: [C403], [C404], [C407], [C409].

## Coverage

| Channel | Read | Gap |
|---|---|---|
| Users | [S6], [S15], [S23] | тредов пользователей о клиентских скриншотах на NeoForge не найдено; Reddit, Discord и X не источники |
| Specialists | [S1], [S2], [S5], [S9], [S14], [S26], [S27], [S43] | в документации NeoForge 1.21.1 нет страницы об атласах; minecraft.wiki закрыт для curl |
| Data | Own measurements M1–M21 (API Modrinth, деревья репозиториев, подсчёт упоминаний в документах) | измерений времени загрузки при рантайм-генерации текстур нет |
| Adversarial | [S7], [S32], [S38], [S39] | случаев отказа модерации CurseForge из-за ИИ не найдено |

Перерасход потолка (четыре поиска и три полных чтения на подвопрос): Q1 — 3 поиска и 2 опровергающих, три документа прочитаны целиком и пять — отрывками с поиском по всему тексту; Q2 — 3 веб-поиска и 1 поиск по коду GitHub, сверх трёх полных чтений просмотрены поиском четыре набора заметок Mojang, четыре страницы NeoForge и короткие исходники; Q3 — 4 веб-поиска и 4 поиска по issue, около 18 страниц получены curl и просмотрены поиском; Q4 — одна страница, без поисков. Причина перерасхода одна: каждый подвопрос называет несколько документов, у каждого своя первичная страница. Ledger'ы Q1–Q3 остановлены у предела 20 000 знаков, часть утверждений ради него вырезана.

Инструменты: встроенный поиск, `curl`, `gh`; для Q4 — встроенный браузер, потому что страница условий Retro Diffusion строится скриптом и по простому HTTP текста не отдаёт (проверка цитат скриптом для C401–C410 поэтому, скорее всего, покажет «не найдено»). Не открылись: minecraft.wiki (HTTP 403), поиск по blog.curseforge.com (строится скриптом), снимок Wayback от 2025-12-12; страницы CurseForge для C329, C332, C333 автором не загружались; на аудите они и отчёты трекера для C346 прочитаны (встроенный браузер, JSON API трекера), метки изменены на quoted. Текста, обращённого к ИИ-ассистенту, на прочитанных страницах не было.

## Own measurements

### M1 — замер подвопроса Q1

Command and output, as recorded at gathering: command: `for u in <Mod Authors Terms URL> <Platforms Terms of Use URL> <Community Guidelines URL>; do curl -sL "$u" | grep -i -c "artificial\|generative\| AI "; done` — output: `0`, `0`, `0`. Same result on tag-stripped text with the wider pattern of M2.

### M2 — замер подвопроса Q1

Command and output, as recorded at gathering: command: `grep -E -i "artificial intelligence|generative|machine[- ]generated|\bAI\b|A\.I\.|LLM" <file>.txt | grep -v -c "ASK THE AI ASSISTANT"` on the tag-stripped text of the Rewards Program Terms of Service, the Website Rules (https://support.curseforge.com/support/solutions/articles/9000197998--curseforge-website-rules), the Overwolf website Terms of Use and the 2023 Medium guidelines post — output: rewards.txt 0, rules.txt 0, tos2.txt 0, medium.txt 0.

### M3 — замер подвопроса Q1

Command and output, as recorded at gathering: command: `grep -n -E "Modified on|AI Misleading|Project Variants" <snapshot>.txt` on current the Moderation Policies page and Wayback snapshots (`https://web.archive.org/web/<ts>id_/…/9000197279`) — output: current: "Modified on: Thu, 22 Oct, 2026 at 4:07 PM", AI Misleading 1, Project Variants 1; ts 20260118072130: "Modified on: Wed, 31 Dec, 2025 at 2:33 PM", AI Misleading 1, Project Variants 0; ts 20250620065342: "Modified on: Tue, 10 Jun, 2025 at 1:41 PM", AI Misleading 0; ts 20251212205558: no article text returned (inconclusive).

### M4 — замер подвопроса Q1

Command and output, as recorded at gathering: command: `curl -sL https://support.curseforge.com/en/support/solutions/articles/9000198907-moderation-guidelines` then strip tags — output: "Moderation Guidelines / Modified on: Wed, 20 Mar, 2024 at 2:18 PM / This article moved to a new location - View our updated article here".

### M5 — замер подвопроса Q1

Command and output, as recorded at gathering: command: `curl -sL -o /dev/null -w "%{http_code} %{url_effective}" <url>` — output: `404 https://www.curseforge.com/terms`; `404 https://www.overwolf.com/legal/author-terms/`; `200 https://legal.overwolf.com/docs/overwolf/website/terms-of-use/` (from https://www.overwolf.com/legal/terms/).

### M6 — замер подвопроса Q2

Command and output, as recorded at gathering: `curl -sL <NeoForge 1.21.1>/resources/client/textures.md | grep -n -i -E "atlas|palett|tint"` → no output (headings: Textures, Texture Metadata, Animated Textures); the 1.21.1 `resources/client` tree has no atlas page.

### M7 — замер подвопроса Q2

Command and output, as recorded at gathering: counted in the mcmeta 1.21.1 files: blocks.json `textures` 4, `permutations` 14; armor_trims.json `textures` 36, `permutations` 14, same three fields.

### M8 — замер подвопроса Q2

Command and output, as recorded at gathering: `curl -sL https://raw.githubusercontent.com/misode/mcmeta/<v>-assets/assets/minecraft/atlases/{blocks,items}.json | grep -c paletted_permutations` → blocks: 1.21.2=1, 1.21.4=1, 1.21.5=1, 1.21.9=1, 1.21.11=0; items: 0, 0, 0, 0, 1.21.11=1.

### M9 — замер подвопроса Q2

Command and output, as recorded at gathering: `gh search code "paletted_permutations" --filename blocks.json --limit 30` → 30 hits, 27 under `assets/minecraft/atlases` (vanilla copies, resource packs); none is a mod shipping a vanilla-type entry.

### M10 — замер подвопроса Q2

Command and output, as recorded at gathering: `gh api repos/Team-Resourceful/ResourcefulBees` → default_branch 26.3, pushed 2026-10-26, archived false; `…/commits/1.21.x` → 2025-03-31; Modrinth `/v2/project/resourceful-bees/version?game_versions=["1.21.1"]` → 0; unfiltered → 11 (neoforge 4, forge 7), newest `2.0.0-beta` for 26.3.

### M11 — замер подвопроса Q2

Command and output, as recorded at gathering: `gh api …/ResourcefulBees/git/trees/1.21.x?recursive=1` → 1,481 blobs; `grep -c "\.png$"` 433; `grep -c "textures/entity/.*\.png$"` 146.

### M12 — замер подвопроса Q2

Command and output, as recorded at gathering: `gh api repos/setycz/ChickensMod/git/trees/main?recursive=1`, PNGs per directory: textures/entity 46, blocks 18, gui 10, items 5.

### M13 — замер подвопроса Q2

Command and output, as recorded at gathering: `gh api "repos/Modern-Mods/ModernChickens/git/trees/Neo%2F1.21.1?recursive=1"` → 1,758 blobs; PNGs: textures/entity 371, item/chicken 286, entity/megachicken 167, block 83. Modrinth `modern-chickens`, 1.21.1 → 22 versions, all neoforge, newest 2.6.1 (2026-08-17).

### M14 — замер подвопроса Q2

Command and output, as recorded at gathering: Modrinth `/v2/project/dynamic_asset_generator/version?game_versions=["1.21.1"]` → 2: `6.1.2 [fabric, quilt] 2024-08-13`, `6.1.2 [neoforge] 2024-08-13`; unfiltered → 136, newest 6.1.2. `gh api repos/lukebemish/dynamic_asset_generator` → lukebemishprojects/DynamicAssetGenerator, archived false, pushed 2024-09-09; last 1.21.x commit 2024-08-13.

### M15 — замер подвопроса Q2

Command and output, as recorded at gathering: `gh api …/DynamicAssetGenerator/git/trees/docs?recursive=1` → pages under `docs/json/texsources/`; overlay.md and palette_spread.md read for the two glosses.

### M16 — замер подвопроса Q2

Command and output, as recorded at gathering: Modrinth `/v2/project/moonlight/version?game_versions=["1.21.1"]` → 431 (fabric 216, neoforge 215); first rows `1.21.1-3.8.4 fabric 2026-11-22`, `1.21.1-3.8.3 neoforge 2026-11-09`.

### M17 — замер подвопроса Q3

Command and output, as recorded at gathering: NeoForge gametest docs mention of screenshots `curl -sL https://docs.neoforged.net/docs/1.21.1/misc/gametest/ | sed 's/<[^>]*>//g' | grep -c -iE "screenshot"` → `0` (HTTP 200, 82808 bytes).

### M18 — замер подвопроса Q3

Command and output, as recorded at gathering: NeoForge 1.21.1 repo tree `curl -sL "https://api.github.com/repos/neoforged/NeoForge/git/trees/1.21.1?recursive=1"` → 4945 paths, truncated=False. Paths with "testframework" and "client": testframework/.../client/{AbstractTestScreen,ClientUtils,CommitBasedList,FrameworkClientImpl,TestScreen,TestsOverlay,package-info}.java, conf/ClientConfiguration.java, impl/FrameworkClient.java. Paths with "screenshot" (whole repo): patches/net/minecraft/client/Screenshot.java.patch, src/main/java/net/neoforged/neoforge/client/event/ScreenshotEvent.java.

### M19 — замер подвопроса Q3

Command and output, as recorded at gathering: ModDevGradle README `grep -c -iE "quickplay|screenshot" README.md` (raw.githubusercontent.com/neoforged/ModDevGradle/main/README.md, 23906 bytes) → `0`.

### M20 — замер подвопроса Q3

Command and output, as recorded at gathering: Modrinth API, 1.21.1 + NeoForge versions Command per slug (script q3/modrinth.sh): `curl -s "https://api.modrinth.com/v2/project/<slug>"` then `curl -s "https://api.modrinth.com/v2/project/<slug>/version?game_versions=%5B%221.21.1%22%5D&loaders=%5B%22neoforge%22%5D"`. Output (count; two newest): - icon-exporter (loaders fabric,forge,neoforge,quilt): 33; 1.21.1-1.4.2-220 2026-09-01 iconexporter-1.21.1-neoforge-1.4.2-220.jar - cyclops-core: 124; 1.21.1-1.30.2 2026-11-15 - isometric-renders (fabric,quilt): 0 — isometric-renders-forked (neoforge): 2; 0.3.0 2026-07-21; 0.2.0 2026-05-05 - item-icon-exporter (forge,neoforge): 4; 1.0.3-NeoForge-1.21.1 2026-05-21 - blockexporter (fabric,forge,modloader,neoforge): 0 — block-renderer (fabric,forge): 0 - mc-runtime-test: 1; 5.0.1 2026-10-21 - probejs: 2; 8.0.3 2026-06-22 — kubejs: 29; 2101.7.2-build.381 2026-11-01 - HTTP 404 (no such slug): iconexporter, blockrenderer, mob-renders, itemrender, mineclient-bridge, hmc-specifics Modrinth `/v2/search` (facets categories:neoforge, versions:1.21.1): "icon export" → icon-exporter, item-icon-exporter; "isometric render" → isometric-renders-forked; "export item png", "render export" → no hits.

### M21 — замер подвопроса Q3

Command and output, as recorded at gathering: Fabric API modules per branch `curl -sL "https://api.github.com/repos/FabricMC/fabric/contents/?ref=1.21.1" | grep -oE '"name": "fabric-[a-z-]*gametest[^"]*"'` → `fabric-gametest-api-v1` only. Same with `ref=1.21.4` → `fabric-client-gametest-api-v1`, `fabric-gametest-api-v1`.

## Open questions

Пилоты — решаются только собственным замером, в этом прогоне по указанию заказчика не запускались:

- Пилот A — генеративные сервисы на 16×16. Мерить: по 10 предметных спрайтов и по 4 блочные текстуры проекта через API PixelLab и Retro Diffusion при одной входной палитре; для каждого результата — настоящий ли размер сетки (16×16 без пересчёта), число цветов и доля пикселей вне палитры, для блоков — шов при укладке 3×3; затем контактный лист и доля спрайтов, которые агент принимает без правки. Для Retro Diffusion отдельно проверить, сочетаются ли стили Minecraft с тайлингом и размером меньше 32 px на одной модели.
- Пилот B — LLM рисует спрайт сеткой индексов. Мерить: 10–20 спрайтов проекта как сетка индексов палитры; доля прошедших числовые тесты (число цветов, границы, принадлежность палитре) и доля принятых по контактному листу.
- Пилот C — модель животного. Мерить: одно животное через безголовый MCP-сервер Blockbench — проходит ли экспорт в `.geo.json` и анимацию, загружается ли результат в GeckoLib под NeoForge 1.21.1, сколько итераций «рендер — правка» до приемлемого вида, какие ошибки ловит проверка геометрии.
- Пилот D — клиентский скриншот на NeoForge 1.21.1 (Q3). Мерить: работает ли от запуска до выхода связка «`runClient` с `--quickPlaySingleplayer` и dev-хуком на `Screenshot.takeScreenshot`» на Windows 11; не чёрный ли кадр при окне без фокуса; работает ли команда `/iconexporter export` из того же хука; чем снимать сущности.
- Пилот E — `paletted_permutations` из мода (Q2). Мерить: подхватывает ли NeoForge 1.21.1 файл `assets/minecraft/atlases/blocks.json` из мода и появляются ли спрайты `<текстура>_<палитра>`; сравнить с PNG, сгенерированными при сборке.
- Пилот F — инструменты проверки вне игры. Мерить: запускаются ли `block-model-renderer` и рендер `.bbmodel` на Windows 11 и совпадает ли их картинка со скриншотом из игры.

Остаток по подвопросам:

- Почему отчёт open: ответ на Q3 держится на цепочке «`runClient` с `--quickPlaySingleplayer` — dev-хук на `Screenshot.takeScreenshot` — выход», которую ни один прочитанный источник не показывает работающей; если пилот D её не подтвердит, ответ на Q3 придётся менять. Решает пилот D.
- Q1 — no new information — как модераторы CurseForge на практике поступают с ИИ-модами и общее заявление Overwolf об ИИ-модах: не найдено; решает вопрос в поддержку CurseForge с описанием проекта. Архивная версия Mod Authors Terms от 13 января 2025 с текущей не сравнивалась.
- Q2 — для выводов о Resourceful Bees и линии Chickens (C227, C232) опровергающего поиска не было; что текстуры этих модов нарисованы вручную и что в Resourceful Bees предмет соты тоже тинтуется, из процитированных источников не следует — решает чтение моделей предметов и обработчика цвета в репозитории. Мода, поставляющего ванильный источник `paletted_permutations` в атлас `blocks`, не найдено (решает пилот E); как мод регистрирует собственный тип источника спрайтов в коде, не исследовалось; для Resourceful Bees не подтверждено, под какую версию Minecraft собирается ветка `1.21.x`; Roost Ultimate и датапак Resource Chickens не открывались.
- Q3 — no new information — сквозного примера «запуск — скриншот — выход» на NeoForge 1.21.1 нет (решает пилот D); страницы MineClient Bridge и Client Runtime Interface Toolkit на CurseForge прочитаны только на аудите, сами моды не проверялись: закрывает ли MineClient Bridge (NeoForge 1.21.1, снимок кадра в PNG, команды, запрос на завершение клиента) цепочку «запуск — скриншот — выход» и поддерживает ли Toolkit NeoForge 1.21.1 — решает чтение их репозиториев и пилот D; README NeoGradle, экспортёры на базе JEI/EMI и визуальные тесты крупных модов не проверялись.
- Q4 — опровергающего поиска для C402, C411 не было: решает поиск других заявлений Retro Diffusion о правах на вывод (FAQ, документация API) и разбор §5 условий (запрет перепродавать генерации как собственное предложение) применительно к моду с ассетами. Предыдущая версия условий Retro Diffusion (19 августа 2025) не читалась.

Следующие прогоны (пункты первого отчёта вне четырёх приоритетов этого прогона): (1) как Modrinth применяет правило «преимущественно ИИ» после 2026-09-27 — к пересмотру Modrinth перед релизом; (2) реакция сообщества на ИИ-ассеты именно в моддинге Minecraft; (3) авторское право на ИИ-вывод вне США и лицензии открытых коллекций ассетов; (4) условия вывода PixelLab и Scenario в части, не прочитанной первым прогоном.

## Sources

- [S1] legal.overwolf.com — curseforge/mod-authors-terms — https://legal.overwolf.com/docs/curseforge/mod-authors-terms/ — passage
- [S2] legal.overwolf.com — platform/platform-terms-of-use — https://legal.overwolf.com/docs/overwolf/platform/platform-terms-of-use/ — passage
- [S3] legal.overwolf.com — overwolf/community-guidelines — https://legal.overwolf.com/docs/overwolf/community-guidelines/ — passage
- [S4] support.curseforge.com — articles/9000197898-rewards-program-terms-of-service — https://support.curseforge.com/support/solutions/articles/9000197898-rewards-program-terms-of-service — passage
- [S5] support.curseforge.com — articles/9000197279 — https://support.curseforge.com/en/support/solutions/articles/9000197279 — passage
- [S6] curseforge-ideas.overwolf.com — ideas/CF-I-7917 — https://curseforge-ideas.overwolf.com/ideas/CF-I-7917 — passage
- [S7] hytale.curseforge.com — newworldscontest/terms — https://hytale.curseforge.com/newworldscontest/terms/ — passage
- [S8] mod.curseforge.com — modjam2026/terms-and-conditions — https://mod.curseforge.com/minecraft/modjam2026/terms-and-conditions/ — passage
- [S9] raw.githubusercontent.com — models/index.md — https://raw.githubusercontent.com/neoforged/Documentation/main/versioned_docs/version-1.21.1/resources/client/models/index.md — passage
- [S10] raw.githubusercontent.com — models/items.md — https://raw.githubusercontent.com/neoforged/Documentation/main/versioned_docs/version-1.21.4/resources/client/models/items.md — passage
- [S11] raw.githubusercontent.com — models/datagen.md — https://raw.githubusercontent.com/neoforged/Documentation/main/versioned_docs/version-1.21.1/resources/client/models/datagen.md — passage
- [S12] raw.githubusercontent.com — atlases/blocks.json — https://raw.githubusercontent.com/misode/mcmeta/1.21.1-assets/assets/minecraft/atlases/blocks.json — passage
- [S13] www.minecraft.net — article/minecraft-java-edition-1-21-5 — https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-5 — passage
- [S14] www.minecraft.net — article/minecraft-java-edition-1-19-3 — https://www.minecraft.net/en-us/article/minecraft-java-edition-1-19-3 — passage
- [S15] bugs.mojang.com — browse/MC-268543 — https://bugs.mojang.com/browse/MC-268543 — passage
- [S16] raw.githubusercontent.com — json/category.md — https://raw.githubusercontent.com/lukebemishprojects/DynamicAssetGenerator/docs/docs/json/category.md — passage
- [S17] raw.githubusercontent.com — json_templates/CodecBeeJsonTemplate.json — https://raw.githubusercontent.com/Team-Resourceful/ResourcefulBees/1.21.x/dev_resources/json_templates/CodecBeeJsonTemplate.json — passage
- [S18] raw.githubusercontent.com — layers/CustomBeeLayer.java — https://raw.githubusercontent.com/Team-Resourceful/ResourcefulBees/1.21.x/src/main/java/com/teamresourceful/resourcefulbees/client/rendering/entities/layers/CustomBeeLayer.java — passage
- [S19] raw.githubusercontent.com — default_honeycombs/Diamond.json — https://raw.githubusercontent.com/Team-Resourceful/ResourcefulBees/1.21.x/forge/src/main/resources/data/resourcefulbees/defaults/default_honeycombs/Diamond.json — passage
- [S20] raw.githubusercontent.com — block/honeycomb_block.json — https://raw.githubusercontent.com/Team-Resourceful/ResourcefulBees/1.21.x/common/src/main/resources/assets/resourcefulbees/models/block/honeycomb_block.json — passage
- [S21] raw.githubusercontent.com — 1.21.1/README.md — https://raw.githubusercontent.com/Modern-Mods/ModernChickens/Neo/1.21.1/README.md — passage
- [S22] raw.githubusercontent.com — render/DynamicChickenTextures.java — https://raw.githubusercontent.com/Modern-Mods/ModernChickens/Neo/1.21.1/src/main/java/strhercules/strhercules/chickens/client/render/DynamicChickenTextures.java — passage
- [S23] github.com — issues/166 — https://github.com/lukebemishprojects/DynamicAssetGenerator/issues/166 — passage
- [S24] raw.githubusercontent.com — docs/root.md — https://raw.githubusercontent.com/lukebemishprojects/DynamicAssetGenerator/docs/docs/root.md — passage
- [S25] raw.githubusercontent.com — texsources/palette_combined.md — https://raw.githubusercontent.com/lukebemishprojects/DynamicAssetGenerator/docs/docs/json/texsources/palette_combined.md — passage
- [S26] docs.neoforged.net — misc/gametest — https://docs.neoforged.net/docs/1.21.1/misc/gametest/ — passage
- [S27] raw.githubusercontent.com — main/README.md — https://raw.githubusercontent.com/neoforged/ModDevGradle/main/README.md — passage
- [S28] raw.githubusercontent.com — gui/ImageExportUtil.java — https://raw.githubusercontent.com/CyclopsMC/IconExporter/master-1.21-lts/loader-common/src/main/java/org/cyclops/iconexporter/client/gui/ImageExportUtil.java — passage
- [S29] raw.githubusercontent.com — client/Screenshot.java.patch — https://raw.githubusercontent.com/neoforged/NeoForge/1.21.1/patches/net/minecraft/client/Screenshot.java.patch — passage
- [S30] piston-meta.mojang.com — ff7ef54488da4a711f3c8a781a9f79105bbe801a/1.21.1.json — https://piston-meta.mojang.com/v1/packages/ff7ef54488da4a711f3c8a781a9f79105bbe801a/1.21.1.json — passage
- [S31] api.modrinth.com — project/icon-exporter — https://api.modrinth.com/v2/project/icon-exporter — passage
- [S32] api.modrinth.com — project/isometric-renders-forked — https://api.modrinth.com/v2/project/isometric-renders-forked — passage
- [S33] api.modrinth.com — project/item-icon-exporter — https://api.modrinth.com/v2/project/item-icon-exporter — passage
- [S34] www.curseforge.com — files/8122914 — https://www.curseforge.com/minecraft/mc-mods/item-icon-exporter/files/8122914 — passage
- [S35] www.curseforge.com — mc-mods/mineclient-bridge — https://www.curseforge.com/minecraft/mc-mods/mineclient-bridge — passage
- [S36] www.curseforge.com — mc-mods/client-runtime-interface-toolkit — https://www.curseforge.com/minecraft/mc-mods/client-runtime-interface-toolkit — passage
- [S37] docs.fabricmc.net — develop/automatic-testing — https://docs.fabricmc.net/develop/automatic-testing — passage
- [S38] headlesshq.github.io — headlessmc/launch — https://headlesshq.github.io/headlessmc/launch/ — passage
- [S39] raw.githubusercontent.com — main/README.md — https://raw.githubusercontent.com/headlesshq/mc-runtime-test-mod/main/README.md — passage
- [S41] headlesshq.github.io — headlessmc/specifics — https://headlesshq.github.io/headlessmc/specifics/ — passage
- [S42] bugs-legacy.mojang.com — browse/MC-270184 — https://bugs-legacy.mojang.com/browse/MC-270184 — passage
- [S43] retrodiffusion.ai — terms — https://retrodiffusion.ai/terms — passage

## Audit

| Claim | Verdict | Note |
|---|---|---|
| C101 | SUPPORTED |  |
| C102 | SUPPORTED |  |
| C103 | SUPPORTED |  |
| C104 | SUPPORTED |  |
| C105 | SUPPORTED |  |
| C106 | SUPPORTED | recorded output shows 404; on re-run today www.curseforge.com/terms returned 403 (bot check), the other two as recorded |
| C107 | SUPPORTED |  |
| C108 | SUPPORTED |  |
| C109 | SUPPORTED |  |
| C110 | SUPPORTED |  |
| C112 | PARTIAL | "no Overwolf legal document that binds an upload" went beyond the documents read, and the second half rests on C132, which was not named; narrowed to the documents read, C132 added to `from` |
| C113 | SUPPORTED |  |
| C114 | SUPPORTED |  |
| C115 | SUPPORTED |  |
| C117 | SUPPORTED |  |
| C118 | SUPPORTED |  |
| C119 | SUPPORTED | §4.1 i–ix read in full: no warranty names authorship or copyright ownership |
| C120 | SUPPORTED |  |
| C121 | SUPPORTED |  |
| C122 | SUPPORTED |  |
| C123 | SUPPORTED |  |
| C125 | SUPPORTED |  |
| C126 | SUPPORTED |  |
| C127 | PARTIAL | "sets no condition on how a project was made" is not in the quote or §3; removed |
| C129 | SUPPORTED |  |
| C130 | SUPPORTED |  |
| C131 | SUPPORTED |  |
| C132 | SUPPORTED |  |
| C133 | SUPPORTED |  |
| C134 | SUPPORTED | re-run: the AI sentence in the 2026-01-18 snapshot is word for word the current one |
| C137 | SUPPORTED |  |
| C138 | SUPPORTED |  |
| C139 | SUPPORTED |  |
| C140 | SUPPORTED |  |
| C141 | SUPPORTED |  |
| C142 | SUPPORTED |  |
| C143 | SUPPORTED |  |
| C201 | SUPPORTED |  |
| C202 | SUPPORTED |  |
| C203 | SUPPORTED |  |
| C204 | SUPPORTED |  |
| C205 | SUPPORTED |  |
| C206 | SUPPORTED |  |
| C208 | PARTIAL | the quoted 1.21.4 sentence about block tinting is in models/index.md of 1.21.4, which is not a listed source and not in C202/C203/C206; claim narrowed to what the named claims give |
| C209 | SUPPORTED |  |
| C210 | SUPPORTED |  |
| C211 | SUPPORTED |  |
| C212 | SUPPORTED |  |
| C213 | SUPPORTED |  |
| C214 | SUPPORTED |  |
| C215 | SUPPORTED |  |
| C216 | SUPPORTED |  |
| C217 | SUPPORTED | bugs.mojang.com is script-rendered; read via its JSON API (POST /api/jql-search-post): quote, steps, versions 1.20.4–1.21.3 and resolution Invalid are there |
| C218 | PARTIAL | the quote is a docs example ("if the following were placed"), and the 1.21.x file is a dev test resource pack; "a mod does add its own source" narrowed |
| C219 | SUPPORTED |  |
| C220 | PARTIAL | the named claims show pack-level uses and a docs example, not a mod shipping the vanilla source type; "a mod can ship" narrowed to an untested implication |
| C221 | SUPPORTED |  |
| C222 | SUPPORTED |  |
| C223 | SUPPORTED |  |
| C224 | PARTIAL | the recorded output gives file names and counts, not that the layers are tintable or the other textures hand-drawn; narrowed |
| C225 | SUPPORTED |  |
| C226 | PARTIAL | S20 is the block model only; "all combs share" and the item model are not in it; narrowed to the block model |
| C227 | PARTIAL | "one grey texture + colour handler" and "hand-drawn textures for showpiece bees" are not in C222–C226; narrowed |
| C228 | PARTIAL | "hand-drawn" is not in the recorded output; removed (re-run: file names are one per chicken) |
| C229 | SUPPORTED | re-run of the tree listing: file names are one per breed, counts as recorded |
| C230 | SUPPORTED |  |
| C231 | SUPPORTED |  |
| C232 | PARTIAL | "hand-made" and "breeds nobody drew" are not in C228–C231; narrowed to what C230 says triggers the tint |
| C233 | SUPPORTED |  |
| C234 | PARTIAL | the maintainer's sentence is scoped by "in terms of what you need"; the claim stated it as general; narrowed |
| C235 | SUPPORTED |  |
| C237 | SUPPORTED |  |
| C239 | SUPPORTED |  |
| C240 | SUPPORTED |  |
| C241 | SUPPORTED |  |
| C242 | SUPPORTED |  |
| C301 | PARTIAL | the quote and page do not say tests are run through /test (the page mentions only /test pos); removed |
| C302 | SUPPORTED |  |
| C303 | PARTIAL | "so /test works in a dev client" is not in the source; removed |
| C304 | SUPPORTED |  |
| C305 | SUPPORTED |  |
| C306 | SUPPORTED |  |
| C307 | SUPPORTED |  |
| C308 | SUPPORTED |  |
| C309 | SUPPORTED |  |
| C310 | SUPPORTED |  |
| C311 | SUPPORTED |  |
| C312 | SUPPORTED |  |
| C313 | SUPPORTED |  |
| C314 | PARTIAL | the manifest lists the argument; that it loads straight into a world is not in the source; removed |
| C315 | SUPPORTED |  |
| C316 | PARTIAL | depends on what the quick-play argument does and on stopping the client, neither from a read source (C317 is prior); narrowed from "can be assembled" to "could be" |
| C317 | UNCHECKABLE | label is prior |
| C320 | SUPPORTED |  |
| C321 | SUPPORTED |  |
| C323 | PARTIAL | "draws each item in a Screen" is not in C312 or C321; removed |
| C324 | SUPPORTED |  |
| C325 | SUPPORTED |  |
| C326 | SUPPORTED |  |
| C327 | SUPPORTED |  |
| C328 | SUPPORTED |  |
| C329 | SUPPORTED | was snippet; file page read in the built-in browser (CurseForge refuses plain HTTP), passage found; label changed to quoted |
| C330 | SUPPORTED |  |
| C331 | SUPPORTED |  |
| C332 | SUPPORTED | was snippet; project page read in the built-in browser, passages found; label changed to quoted, "page not read" removed |
| C333 | SUPPORTED | was snippet; project page read in the built-in browser, passage found; label changed to quoted |
| C334 | SUPPORTED |  |
| C335 | SUPPORTED |  |
| C336 | SUPPORTED |  |
| C337 | PARTIAL | "a screenshot taken in that mode has no rendered content" is the author's inference, not the page; removed |
| C338 | PARTIAL | "the client renders normally into the virtual framebuffer" is not in the source; removed |
| C339 | SUPPORTED | claim line and its source entry were mangled at build (a stray S40 entry); the quote and the table row are in S39; citation repaired, S40 entry removed |
| C340 | PARTIAL | "no screenshot API of its own" is not in the source; removed |
| C341 | SUPPORTED |  |
| C342 | SUPPORTED |  |
| C343 | SUPPORTED |  |
| C345 | PARTIAL | "runClient opens a real window" and C338's rendering half were unstated premises; narrowed |
| C346 | SUPPORTED | was snippet; MC-270184 and MC-99770 read via the tracker's JSON API, quote found in MC-270184 notes; label changed to quoted |
| C401 | SUPPORTED | page is script-rendered; read in the built-in browser, quote found |
| C402 | SUPPORTED | page is script-rendered; read in the built-in browser, quote found |
| C403 | SUPPORTED | page is script-rendered; read in the built-in browser, quote found |
| C404 | SUPPORTED | page is script-rendered; read in the built-in browser, quote found |
| C405 | SUPPORTED | page is script-rendered; read in the built-in browser, quote found |
| C406 | SUPPORTED | page is script-rendered; read in the built-in browser, quote found |
| C407 | SUPPORTED | page is script-rendered; read in the built-in browser, quote found |
| C408 | SUPPORTED | page is script-rendered; read in the built-in browser, quote found |
| C409 | SUPPORTED | page is script-rendered; read in the built-in browser, quote found |
| C410 | SUPPORTED | page is script-rendered; read in the built-in browser, quote found |
| C411 | PARTIAL | "the only use restriction" does not follow from the three named claims, and §5 of the terms also bars reselling generations as one's offering for products built on the API; narrowed to the quoted clauses |

Label verdict: open — the Q3 answer rests on an untested chain (quick-play launch, dev screenshot hook, exit); if pilot D fails, the Summary's Q3 answer has to change.
Quote check: asset-pipeline-followup-2026-11-23.md: 83 quoted claim(s): 67 found, 0 NOT FOUND, 16 unfetchable, 0 no source

## Disposition

| Recommendation | State | Where |
|---|---|---|
| Публиковать на CurseForge с добровольным раскрытием ИИ; изображения страницы — настоящие скриншоты из игры; в конкурсах Overwolf с ИИ-графикой не участвовать | deferred | unfiled: задание прогона разрешает менять только файл отчёта; решение о публикации принимает планирующая сессия проекта, issue предлагается в итоговом сообщении |
| Основной путь текстур — генерировать готовые PNG скриптом при сборке, а не при загрузке игры | deferred | unfiled: задание прогона разрешает менять только файл отчёта; кода мода и сборки в репозитории ещё нет, issue предлагается в итоговом сообщении |
| Тинт оставить для вариантов, число которых заранее не известно | deferred | unfiled: то же ограничение задания; входит в то же решение о конвейере текстур |
| Черновики в Retro Diffusion делать на платном балансе, а не на бесплатном лимите | deferred | unfiled: то же ограничение задания; применимо, только если пилот A выберет Retro Diffusion |
