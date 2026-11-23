---
topic: platform-version
tags: area:platform, research
source_provenance: /research invocation from the farming-evolution planning session, 2026-11-23
status: answered
audit: 2026-11-23 — 164 claims: 141 SUPPORTED, 21 PARTIAL, 0 UNSUPPORTED, 2 UNCHECKABLE
---

## Question

Какую версию Minecraft и какой загрузчик (Forge, NeoForge, Fabric) выбрать для проекта на конец 2026 года.

Нужно ответить:
- На каких версиях и загрузчиках сейчас живут и активно поддерживаются TerraFirmaCraft, GregTech CEu Modern, Create, KubeJS, FTB Quests, AgriCraft, Serene Seasons; где есть порты, где нет.
- На какой версии сидят действующие сложные сборки (TerraFirmaGreg-Modern, GTNH и другие) и планируют ли переезд.
- Цена написания своего Java-мода на каждой платформе: стабильность API, инструменты сборки, документация, частота ломающих изменений.
- Цена портирования чужого мода между версиями, если нужного мода на выбранной версии нет.
- Рекомендация с явными условиями, при которых она меняется.

Sub-questions: Q1 — версии и загрузчики семи модов; Q2 — версии действующих сложных сборок и планы переезда; Q3 — цена своего Java-мода на каждой платформе; Q4 — цена портирования чужого мода. Пятый пункт (рекомендация) — не отдельный сбор фактов, а вывод из Q1–Q4; он дан в Summary.

Прежних отчётов в `docs/research/` нет. `docs/research-baseline.md` (2026-05-24) использован только как наводка; этот отчёт отменяет его строку «Version: 1.20.1 Forge, максимальная мод-поддержка» как безусловную: на ноябрь 2026 это один из двух живых вариантов, а не единственный.

## Summary

**Рекомендация: Minecraft 1.21.1 + NeoForge — если GregTech не обязан быть в сборке с первого играбельного релиза. Если обязан — Minecraft 1.20.1 + Forge. Версии 26.x и Fabric не подходят при любом из этих двух условий.** Выбор между 1.21.1 и 1.20.1 упирается в один факт, которого в источниках нет: когда GregTech CEu Modern выпустит стабильную версию под 1.21.1.

Почему кандидатов только два. Всё, что новее 1.21.1, отпадает: на 26.x сборки есть только у KubeJS, FTB Quests и Serene Seasons, а у TerraFirmaCraft, GregTech, Create и AgriCraft их нет [C141]. Fabric отпадает: TerraFirmaCraft под него не выходит [C105], сборки GregTech под Fabric остановились в январе 2024 [C141], Create Fabric пропускает 1.21.1 [C119], и Monifactory прямо отказывается от Fabric из-за отсутствия нужных модов [C232]. Экосистема NeoForge на 26.x пока не переехала: 75 сборок на 26.1.2 против 2328 на 1.21.1 [C360], [C362].

Что есть на 1.20.1 Forge. Это единственная версия, где опубликованы все семь модов [C140]. На ней сидят пять из семи проверенных сборок с GregTech в центре — TerraFirmaGreg-Modern, Monifactory, Star Technology, GregTech Community Pack и ATM9 Expert, репозиторий которого открыт в октябре 2026 [C244], [C240] (остальные две, Nomi-CEu и GTNH, — на 1.12.2 и 1.7.10); участник команды TerraFirmaGreg в апреле 2026 написал, что переезжать пока не собираются [C205], и объяснил это размером экосистемы 1.20 и умением команды дописывать недостающее самим [C206]. Forge держит 1.20.1 в уровне полной поддержки [C308]. Минусы: Create на 1.20.1 не обновлялся с ноября 2025 [C117], AgriCraft — тоже с ноября 2025 [C134]; майское исправление сетевой уязвимости NeoForge выпустил только для 26.1.2 и 1.21.1 [C358]; документация NeoForge начинается с 1.20.3 и 1.20.1 не покрывает [C346]; шаблон мода Forge 1.20.1 в ноябре 2026 перевели на ForgeGradle 7 [C307], [C320]. Monifactory уже убрал часть Ender IO, потому что поддержка мода на 1.20.1 прекратилась [C233].

Что есть на 1.21.1 NeoForge. Сборки есть у шести модов из семи [C141]: TerraFirmaCraft вышел из беты в апреле 2026 и выпускается параллельно с линией 1.20.1 [C405], [C103]; Create развивается только здесь [C116]; KubeJS, FTB Quests и Serene Seasons обновлялись осенью 2026 [C122], [C128], [C136]. NeoForge продолжает выпускать сборки 21.1 [C302]; суффикс -beta, означающий, что ломающие изменения ещё допускаются, снимается при стабилизации линии [C334]. Больше всего новых модов с июня 2026 выходит именно под 1.21.1 NeoForge [C361]. Две дыры: у GregTech CEu Modern нет стабильного релиза под 1.21.1 уже 16 месяцев — новейшая сборка там бета 7.0.2 от июля 2025, плюс скользящие снапшоты 8.0.0 [C113], [C417], с открытыми сообщениями о поломках [C219]; у AgriCraft сборки под 1.21.x нет вовсе [C134]. В репозиториях проверенных GregTech-сборок релиза под 1.21.1 нет [C244]; движение туда есть, но оно невыпущенное или неофициальное [C245].

Цена своего мода. На закреплённой линии (1.21.1 или 1.20.1) мод не сталкивается с миграциями, а на свежей версии — примерно раз в три месяца с руководством по миграции на тысячи строк [C344]. Для 1.20.1 NeoForge рекомендует свой плагин сборки ModDevGradle в legacy-режиме вместо ForgeGradle [C316]; сам ModDevGradle — новый плагин NeoForge, прежний NeoGradle поддерживается рядом с ним [C315]. Java: 17 на 1.20.1 [C321], 21 начиная с 1.20.5 [C324], 25 на 26.1 [C325]. Между 1.20.1 и 1.21.1 для мода с данными на предметах лежит замена NBT на компоненты данных в 1.20.5 [C337]; плюс переписанные реестры, capabilities, сеть и события [C338]. То есть мод, написанный под 1.20.1, при позднем переезде на 1.21.1 переписывается именно в этих местах.

Цена портирования чужого мода. Команды, перескочившие с 1.20.1 сразу на 1.21, шли долго: TerraFirmaCraft — около 14 месяцев до первой беты и 21 до стабильного релиза [C407], при 806 коммитах в ветке порта [C406]; GregTech — 16 месяцев без стабильного релиза [C417]; Create — около 8,5 месяцев от первой сборки NeoForge под 1.21 [C421]. Команды, портировавшие на каждую промежуточную версию (Mekanism, AE2), выпустили 1.21 за дни [C457]. Слоя, который запускал бы моды 1.20.1 Forge на 1.21.1 без порта, по сути нет: Sinytra Connector работает только с модами Fabric [C445], а Retromod сам называет свою совместимость частичной и исключает Create и AE2 [C448], [C449]. Лицензии разрешают распространять собственный порт TerraFirmaCraft, GregTech, KubeJS и AgriCraft, но не Serene Seasons и FTB Quests, а у Create — только код без ассетов [C458].

Условия, при которых рекомендация меняется:

1. **GregTech обязателен, и к началу работ над тех-веткой стабильного GregTech 8.0.0 под 1.21.1 всё ещё нет → 1.20.1 Forge.** Сейчас 8.0.0 не выпущен ни на одной из двух версий [C220]; заявления мейнтейнеров о сроке не найдено.
2. **Проект стартует с кода или скриптов TerraFirmaGreg-Modern либо с AgriCraft как есть → 1.20.1 Forge**: обе базы существуют только там [C202], [C134].
3. **GregTech не нужен (тех-ветка на Create и других модах) → 1.21.1 NeoForge без оговорок**: все остальные моды списка, кроме AgriCraft, там выпускаются [C141], а AgriCraft, по `GLOSSARY.md` проекта, — возможная стартовая база, не ядро.
4. **К 26.x возвращаться только когда там появятся релизы TerraFirmaCraft и Create**; сейчас у Create нет даже ветки [C420], у TerraFirmaCraft — тоже [C103]. Прогноз мейнтейнера NeoForge, что 26.1 заменит 1.21.1 как стабильную версию [C355], сам автор называет личной догадкой [C356].

## Findings

### Q1 — На каких версиях и загрузчиках живут TerraFirmaCraft, GregTech CEu Modern, Create, KubeJS, FTB Quests, AgriCraft, Serene Seasons — answered with a gap

- **C101** [quoted] TerraFirmaCraft's newest Modrinth release line is 4.2.16 for Minecraft 1.21.1 on NeoForge only, published 2026-11-12 — ""game_versions":["1.21.1"],"loaders":["neoforge"]" — [S1], also in response: "version_number":"4.2.16" and "date_published":"2026-11-12T04:11:02.903466Z"
- **C102** [quoted] TerraFirmaCraft still releases for 1.20.1: version 3.2.30 for 1.20.1 (Forge, also tagged NeoForge) was published 2026-11-08 — ""game_versions":["1.20.1"],"loaders":["forge","neoforge"]" — [S2], also in response: "version_number":"3.2.30" and "date_published":"2026-11-08T14:14:29.381319Z"
- **C103** [measured] TerraFirmaCraft GitHub releases alternate between the 1.20.1 line (3.2.26 on 2026-09-28, 3.2.27 on 10-10, 3.2.28 on 10-20, 3.2.29 on 11-07, 3.2.30 on 11-08) and the 1.21.1 line (4.2.10 on 2026-09-07 … 4.2.16 on 11-12); the repo default branch is 1.21.x and branches are 1.12.x, 1.16.x, 1.17.x, 1.18.x, 1.20.x, 1.21.x (no branch above 1.21) — Own measurements, M1
- **C105** [measured] On Modrinth the latest TerraFirmaCraft version per (game version, loader) is: 1.7.10 forge 2023-09-24; 1.12.2 forge 2023-09-24; 1.18.2 forge 2.2.33 2024-01-28; 1.20.1 forge/neoforge 3.2.30 2026-11-08; 1.21.1 neoforge 4.2.16 2026-11-12 — no Fabric build and nothing above 1.21.1 — Own measurements, M2
- **C107** [quoted] The newest stable GregTech CEu Modern release is 7.5.3 for Minecraft 1.20.1 Forge, published 2026-04-12 (typed beta on Modrinth) — ""game_versions":["1.20.1"],"loaders":["forge"]" — [S3], also in response: "version_number":"mc1.20.1-7.5.3" and "date_published":"2026-04-12T20:48:08.842356Z" and "version_type":"beta"
- **C108** [quoted] The newest GTCEu Modern build published on Modrinth for 1.21/1.21.1 (NeoForge) is 7.0.2 from 2025-07-20, typed beta — ""game_versions":["1.21","1.21.1"],"loaders":["neoforge"]" — [S4], also in response: "version_number":"mc1.21.1-7.0.2" and "date_published":"2025-07-20T21:30:14.880506Z"
- **C109** [quoted] GTCEu Modern publishes rolling pre-release snapshot builds of 8.0.0 for Minecraft 1.21 on GitHub; the latest is dated 2026-11-22 and is flagged prerelease — "name": "1.21-8.0.0 SNAPSHOT 2918b17" — [S5], also in response: "prerelease": true and "published_at": "2026-11-22T05:22:19Z"
- **C110** [measured] GTCEu Modern GitHub: default branch is 1.20.1; the last 10 tagged non-prerelease releases are all 1.20.1 (v7.1.4 2025-08-08 … v7.4.1 2026-01-06, v7.5.0 02-08, v7.5.1 02-09, v7.5.2 02-14, v7.5.3 2026-04-12); a parallel snapshot `latest-1.20.1` named "1.20.1-8.0.0 SNAPSHOT 5a6aff9" is dated 2026-11-21; branch heads: 1.20.1 committed 2026-11-22, 1.21 committed 2026-11-22, a branch named 26.1 last committed 2026-04-28; gradle.properties on both 1.20.1 and 1.21 say mod_version = 8.0.0 — Own measurements, M1
- **C111** [quoted] The GTCEu Modern README states its targets as Forge for 1.20.1 and NeoForge for 1.21.1+ — "GregTech:CEu for modern Minecraft versions MinecraftForge (1.20.1) & NeoForge (1.21.1+)." — [S6], line 3
- **C113** [inference] GTCEu Modern is actively developed on both 1.20.1 and 1.21.1 as of 2026-11, but there has been no stable (non-snapshot) 1.21.1 release for 16 months and no stable release on any version since 2026-04-12; 8.0.0 is unreleased on both — from [C107], [C108], [C109], [C110]
- **C114** [quoted] Create's newest release is 6.0.10 for Minecraft 1.21.1 NeoForge, published 2026-04-21 — ""game_versions":["1.21.1"],"loaders":["neoforge"]" — [S7], also in response: "version_number":"6.0.10+mc1.21.1" and "date_published":"2026-04-21T22:20:03.579201Z"
- **C115** [quoted] Create's last release for 1.20.1 (Forge, also tagged NeoForge) is 6.0.8 from 2025-11-02 — ""game_versions":["1.20.1"],"loaders":["forge","neoforge"]" — [S8], also in response: "version_number":"mc1.20.1-6.0.8" and "date_published":"2025-11-02T16:10:25.364809Z"
- **C116** [measured] Create GitHub releases: 6.0.7 and 6.0.8 shipped for both 1.20.1 and 1.21.1 (2025-10-25, 2025-11-02); 6.0.9 (2026-01-02) and 6.0.10 (2026-04-21) shipped for 1.21.1 only; branch mc1.20.1/dev last commit is 2025-11-30, mc1.21.1/dev (default) last commit 2026-11-17; there is no 26.x branch in Creators-of-Create/Create — Own measurements, M1
- **C117** [inference] Create on 1.20.1 Forge looks frozen: no release for over 12 months and no commit on its dev branch since 2025-11-30, while 1.21.1 kept receiving releases and commits — from [C115], [C116]
- **C118** [quoted] Create Fabric's newest release is 6.0.8.1 for Minecraft 1.20.1 (Fabric/Quilt), published 2025-12-02; there is no Create Fabric release for 1.21.x — ""game_versions":["1.20.1"],"loaders":["fabric","quilt"]" — [S9], also in response: "version_number":"6.0.8.1+build.1744-mc1.20.1" and "date_published":"2025-12-02T17:58:03.504148Z"; absence of 1.21 from mr.py over create-fabric (67 versions)
- **C119** [quoted] A Create Fabric team member said (2026-06-04) there will be no Fabric 1.21.1 port and that Create Fabric will go straight to Minecraft 26.1 after the Create NeoForge port is finished — "There is no real point in working on a 1.21.1 port. Hence why Create Fabric is going to be jumping straight to 26.1 once we finish up porting Create NeoForge." — [S10], issue #1905, comment by IThundxr (author_association MEMBER)
- **C121** [quoted] KubeJS's newest build is 8.0.8 for Minecraft 26.1.2 on NeoForge, published 2026-11-01, typed beta — ""game_versions":["26.1.2"],"loaders":["neoforge"]" — [S11], also in response: "version_number":"26.1.2-8.0.8+neoforge" and "date_published":"2026-11-01T15:39:33.652244Z" and "version_type":"beta"
- **C122** [quoted] KubeJS for 1.21/1.21.1 NeoForge is still released: 2101.7.2-build.381 on 2026-11-01, typed release — ""game_versions":["1.21","1.21.1"],"loaders":["neoforge"]" — [S12], also in response: "version_number":"2101.7.2-build.381" and "date_published":"2026-11-01T15:40:10.497679Z"
- **C123** [quoted] KubeJS's most recent 1.20.1 Forge release is 2001.6.5-build.26 from 2026-05-01 — ""game_versions":["1.20.1"],"loaders":["forge"]" — [S13], also in response: "version_number":"2001.6.5-build.26+forge" and "date_published":"2026-05-01T10:43:18.671562Z"
- **C124** [measured] KubeJS: on Modrinth the 1.20.1 line has Forge and Fabric/Quilt builds (both 2001.6.5-build.26, 2026-05-01), while 1.20.4, 1.21/1.21.1 and 26.1.2 have NeoForge builds only; GitHub default branch is 2601, branch heads: 2001 last commit 2026-05-01, 2101 2026-11-15, 2601 2026-11-15, 2603 2026-10-05 (no 2603 release seen) — Own measurements, M1, M2
- **C125** [quoted] FTB Quests's latest 1.20.1 Forge artifact is 2001.4.22, last published 2026-05-12 — "<release>2001.4.22</release>" — [S14], also in response: <lastUpdated>20260512155913</lastUpdated>
- **C126** [quoted] FTB Quests's NeoForge artifact list includes 1.21.1 (2101.1.39), 1.21.11 (2111.1.5) and 26.1.2 (26.1.2.10) versions; the maven "latest" is 2101.1.39, last updated 2026-11-16 — "<version>26.1.2.10</version>" — [S15], also in response: <release>2101.1.39</release> and <lastUpdated>20261116163040</lastUpdated> and <version>2111.1.5</version>
- **C127** [measured] FTB Quests git tags and their commit dates: v2001.4.22 (1.20.1) 2026-05-12; v2111.1.5 (1.21.11) 2026-04-11; v26.1.2.10 (26.1.2) 2026-11-05; v2101.1.39 (1.21.1) 2026-11-16. Maven artifact counts: ftb-quests-forge has 2001.x only among modern lines (no 2101/2111/26.x); ftb-quests-neoforge and ftb-quests-fabric each have 40 2101.x, 5 2111.x and 10 26.x versions; ftb-quests-fabric also has 2001.x — Own measurements, M1
- **C128** [inference] FTB Quests is actively maintained on 1.21.1 (NeoForge + Fabric) and 26.1.2 (NeoForge + Fabric) as of 2026-11; its 1.20.1 line (Forge + Fabric) last shipped 2026-05-12, i.e. slowed but updated within 2026; there is no Forge build above 1.20.1 — from [C125], [C126], [C127]
- **C129** [quoted] AgriCraft's newest release is 4.0.6 for Minecraft 1.20.1 (Forge; a Fabric 4.0.6 build has the same date), published 2025-11-24 — ""game_versions":["1.20.1"],"loaders":["forge"]" — [S16], also in response: "version_number":"4.0.6" and "date_published":"2025-11-24T14:03:09.193831Z"
- **C130** [quoted] The highest Minecraft version with any published AgriCraft build is 1.20.4 (NeoForge/Forge/Fabric), version 4.0.2-beta from 2024-06-23 — ""game_versions":["1.20.4"],"loaders":["neoforge"]" — [S17], also in response: "version_number":"4.0.2-beta" and "date_published":"2024-06-23T18:58:57.031353Z"
- **C131** [measured] AgriCraft has no published build for 1.21.x on Modrinth (28 versions; newest lines 1.20.1 4.0.6 from 2025-11-24 and 1.20.4 4.0.2-beta from 2024-06-23); the GitHub repo has a `1.21` branch whose last commit is 2025-04-06, and the repo's last push overall is 2025-11-24 (branch 1.20.1 head) — Own measurements, M1, M2
- **C133** [quoted] A community pull request to bring AgriCraft's 1.21 branch to 1.21.1 / NeoForge 21.1.255 was opened 2026-10-04 by a non-maintainer and is still open and unmerged with no comments — "title": "[1.21.1] Update to NeoForge 21.1.255 and port fixes from 1.20.1" — [S18], also in response: "state": "open" and "created_at": "2026-10-04T16:26:35Z" and "merged": false
- **C134** [inference] AgriCraft is effectively alive only on 1.20.1 (Forge + Fabric), with its last release and last maintainer commit a year ago (2025-11-24); a 1.21 port exists only as an unreleased branch stalled since 2025-04 plus an unmerged community PR — from [C129], [C130], [C131], [C133]
- **C135** [quoted] Serene Seasons's newest build targets Minecraft 26.3 (Forge, NeoForge and Fabric builds), published 2026-09-21, typed beta — ""game_versions":["26.3"],"loaders":["neoforge"]" — [S19], also in response: "version_number":"26.1.2.0.7" and "date_published":"2026-09-21T10:20:23.431363Z"
- **C136** [quoted] Serene Seasons for 1.21.1 (NeoForge; Forge and Fabric builds share the version and date) was last updated 2026-09-05 with 10.1.0.9 — ""game_versions":["1.21.1"],"loaders":["neoforge"]" — [S20], also in response: "version_number":"10.1.0.9" and "date_published":"2026-09-05T11:46:04.208678Z"
- **C137** [quoted] Serene Seasons for 1.20.1 Forge (and Fabric, same version/date) was last updated 2026-06-14 with 9.1.0.3 — ""game_versions":["1.20.1"],"loaders":["forge"]" — [S21], also in response: "version_number":"9.1.0.3" and "date_published":"2026-06-14T11:04:51.638824Z"
- **C138** [measured] Serene Seasons ships Forge + NeoForge + Fabric builds on Modrinth for 1.20.4, 1.21, 1.21.1, 1.21.3 to 1.21.11, 26.1.1, 26.1.2, 26.2 and 26.3 (the recorded output has none tagged 1.21.2 or 26.1); intermediate versions stop getting builds once the next one ships (e.g. 1.21.11 last built 2026-03-18, 26.1.1 last built 2026-04-13), while 1.20.1 and 1.21.1 kept getting builds into mid/late 2026 — Own measurements, M2
- **C140** [inference] 1.20.1 Forge is the only version where all seven mods have a published build (TFC 3.2.30, GTCEu 7.5.3, Create 6.0.8, KubeJS 2001.6.5, FTB Quests 2001.4.22, AgriCraft 4.0.6, Serene Seasons 9.1.0.3); by last-release date on 1.20.1 only TFC is current to 2026-11, KubeJS/FTB Quests/Serene Seasons/GTCEu stopped at 2026-04..06 (GTCEu still has rolling 1.20.1 snapshots), Create and AgriCraft stopped in 2025-11 — from [C102], [C105], [C107], [C110], [C115], [C123], [C125], [C129], [C130], [C137]
- **C141** [inference] On 1.21.1 NeoForge six of seven have builds (TFC, Create, KubeJS, FTB Quests, Serene Seasons released; for GTCEu the newest is the 7.0.2 beta from 2025-07 plus 8.0.0 snapshots); AgriCraft has none. Above 1.21.1 only KubeJS (26.1.2 beta), FTB Quests (26.1.2) and Serene Seasons (26.3) have builds; TFC, GTCEu, Create and AgriCraft do not. Fabric coverage of the set is only possible on 1.20.1 and lacks TFC and (last Fabric build 2024-01-08, per M2) GTCEu — from [C101], [C105], [C108], [C109], [C114], [C118], [C121], [C122], [C124], [C126], [C131], [C135], [C136], Own measurements M2

### Q2 — На какой версии сидят действующие сложные сборки и планируют ли переезд — answered with a gap

- **C202** [quoted] The TFG-Modern dev branch lockfile pins Minecraft 1.20.1 on Forge 47.4.13 (classic Forge, not NeoForge) — ""forge": "47.4.13"" — [S22], top of file, mc_versions + loaders
- **C203** [quoted] The latest TFG-Modern release is 0.13.19, published 2026-11-19 — "2026-11-19T15:22:00Z" — [S23], first release object, tag_name 0.13.19
- **C205** [quoted] A TFG team member (Pyritie, association MEMBER) answered on 2026-04-01 that the team does not intend to move off 1.20 at this time — "we have no intent of moving at this time" — [S24], comment id 4170379213
- **C206** [quoted] The reason given for staying is the size of the 1.20 mod ecosystem plus the team's ability to write whatever is missing themselves — "our own team's skills are at the point where we can just make our own things for anything missing" — [S24], comment id 4170379213
- **C207** [measured] TFG-Modern has no 1.21 or NeoForge branch: the 16 branches include 0.14 and gtm8, and the lockfiles of dev, 0.14 and gtm8 all pin 1.20.1 + Forge 47.4.13 — Own measurements, M3
- **C208** [quoted] The TFG-Modern branch gtm8 exists and was committed to on 2026-11-22 — ""name": "gtm8"" — [S25], branch list
- **C209** [inference] TFG has active branches named 0.14 and gtm8 (the name suggests GregTech Modern 8, which exists as 8.0.0 snapshots), all pinned to 1.20.1 Forge; no TFG branch targets another Minecraft version as of Nov 2026 — from [C207], [C208], [C216]
- **C211** [quoted] An unofficial third-party port of TFG-Modern to 1.21.1 NeoForge exists as a small GitHub repo (5 stars, created 2026-07-18, last push 2026-07-19) — "Hand-made port of TerraFirmaGreg" — [S26], description field
- **C212** [snippet] A modpack listing describes Gregnautics Continued as a hand-made port of TerraFirmaGreg to 1.21.1 NeoForge on KubeJS 7 / GregTech CEu 8 / TerraFirmaCraft 4 — "all adapted by hand to the new KubeJS 7 / GregTech CEu 8 / TerraFirmaCraft 4 APIs" — [S27], search snippet
- **C214** [quoted] On Modrinth the newest GregTech Modern build for 1.21.1 NeoForge is 7.0.2 (beta channel, 2025-07-20) — ""version_number":"mc1.21.1-7.0.2"" — [S28], version list
- **C215** [quoted] On Modrinth the newest GregTech Modern build for 1.20.1 Forge is 7.5.3 (2026-04-12), i.e. the 1.20.1 line got four 7.5.x releases in 2026 while 1.21.1 got none there — ""version_number":"mc1.20.1-7.5.3"" — [S28], version list
- **C216** [quoted] GregTech Modern publishes rolling 8.0.0 snapshot prereleases for both 1.21 and 1.20.1 (seen 2026-11-22 and 2026-11-21); the default branch of the repo is still 1.20.1 — ""tag_name": "latest-1.21"" — [S29], first two release objects; snapshot names move with each build
- **C217** [quoted] The GregTech Modern 1.21 branch builds against Minecraft 1.21.1 and NeoForge 21.1.248 — "neoForge = "21.1.248"" — [S30], versions block
- **C218** [measured] GregTech Modern also has a branch named 26.1, last commit 2026-04-28; its libs.versions.toml still pins Minecraft 1.21.1 + NeoForge 21.1.215, so no actual 26.1 port work is visible in it — Own measurements, M4
- **C219** [quoted] Open issues report breakage on the 1.21.1 8.0.0 snapshots as of autumn 2026, e.g. an incompatibility with the current JEI — "1.21-8.0.0 SNAPSHOT a373c6e is incompatible with latest JEI version" — [S31], issue title, opened 2026-10-30
- **C220** [inference] As of Nov 2026 GregTech Modern has no stable 8.x release on either version; a 1.21.1 pack would sit on either the mid-2025 7.0.2 beta or moving 8.0.0 snapshots — from [C214], [C215], [C216], [C219]
- **C221** [quoted] The GregTech Community Pack Modern (reference pack by the GregTech CEu org) is a 1.20.1 pack on its main branch; latest release v1.14.5 on 2026-02-09 — "a minecraft modpack for 1.20.1 highlighting" — [S32], first paragraph
- **C222** [quoted] The Community Pack has an unreleased 1.21 branch pinned to Minecraft 1.21.1 + NeoForge 21.1.249 with pack version 1.15.0 (last commit 2026-10-13); this is a migration effort in progress by the GregTech CEu org itself — "neoforge = "21.1.249"" — [S33], [versions]
- **C223** [quoted] GTNH is a Minecraft 1.7.10 pack and describes itself as still in development after more than 11 years — "big progressive kitchensink pack for Minecraft 1.7.10" — [S34], What is GT New Horizons?
- **C225** [quoted] The official GTNH download page offers the current pack in two builds, one for Java 8 and one for Java 17-26 — "Latest (2.9.1) for Java 17-26" — [S35], download buttons
- **C227** [quoted] GTNH maintains its own mod, lwjgl3ify, to run 1.7.10 on LWJGL3 and modern Java (latest release 3.1.0 on 2026-11-04) — "Brings LWJGL3 and modern Java versions to Minecraft 1.7.10." — [S36], line 3
- **C228** [inference] GTNH's observable strategy is to stay on 1.7.10 and modernise the runtime under it (Java, LWJGL, own forks) rather than port the pack; no explicit written version-policy statement was found in this run — from [C223], [C225], [C227]
- **C229** [quoted] Monifactory (repo now Omicron-Industries/Monifactory) is on Minecraft 1.20.1 with Forge 47.4.28 — ""id": "forge-47.4.28"" — [S37], minecraft.modLoaders
- **C230** [quoted] Monifactory's latest release is Beta 0.14.0, published 2026-11-12 — "2026-11-12T01:45:35Z" — [S38], first release object
- **C231** [quoted] Monifactory's FAQ leaves a port to a newer Minecraft version undecided and tells players not to ask for it — "Porting the pack to a future version of Minecraft may or may not happen" — [S39], Q on backports/ports, line 37
- **C232** [quoted] Monifactory rules out Fabric because required mods do not exist there — "Monifactory will not be ported to Fabric." — [S39], line 41
- **C233** [quoted] Monifactory removed parts of Ender IO because that mod's 1.20.1 support ended while still buggy: a concrete cost of staying on 1.20.1 — "Support for Ender IO in 1.20.1 has discontinued" — [S39], line 137
- **C234** [quoted] Star Technology is a GregTech pack on 1.20.1; it moved from 1.19.2 to 1.20.1 earlier (a legacy-1.19.2 branch remains) — "A 1.20.1 Minecraft modpack based on GregTech" — [S40], description field
- **C235** [quoted] Star Technology's latest release is Theta 2 Hotfix 4, published 2026-10-06; the repo was pushed to on 2026-11-22 — "2026-10-06T11:04:42Z" — [S41], first release object
- **C236** [measured] No issue in the Star Technology tracker discusses a 1.21 or NeoForge port, and its branch list has no 1.21 branch — Own measurements, M5
- **C237** [quoted] Nomifactory CEu is still on Minecraft 1.12.2 Forge; latest release 1.7.7 on 2026-01-05, main branch last committed 2026-10-15 — ""id": "forge-14.23.5.2860"" — [S42], minecraft.modLoaders
- **C238** [quoted] ATM10 is the 1.21.1 pack of the All the Mods team and is still being updated (changelog 8.6 dated 2026-11-14) — "All The Mods 10 1.21.1" — [S43], line 3; date from CHANGELOG.md in the same repo
- **C239** [quoted] The ATM team already develops ATM11 on Minecraft 26.1.2 (repo created 2026-04-14, pushed 2026-11-20): the mainstream kitchen-sink line has moved past 1.21.1 — "All The Mods 11 26.1.2" — [S44], line 3
- **C240** [quoted] The same team opened the repository of a GregTech-driven expert pack, ATM9 Expert, on 1.20.1 in late 2026 (repo created 2026-10-27, pushed 2026-11-22) rather than on 1.21.1 or 26.1; its README says the pack began as a solo fan project, 9 months of one-man work, before becoming an official ATM pack — "All The Mods 9 Expert 1.20.1" — [S45], line 3
- **C242** [snippet] Createch 2 is an expert pack on 1.21.1 NeoForge that includes GregTech, with a 0.7.1 file from Aug 2026 (pre-1.0) — "hardest expert pack on Minecraft 1.21.1" — [S46], search snippet
- **C243** [quoted] An expert pack built on Modern Industrialization exists on 1.21.1 NeoForge in 2026: Absolute Industrialization (the page lists Game Versions 1.21.1, Mod Loaders NeoForge, last update Oct 13, 2026) — "An Expert Tech modpack built on Modern Industrialization, featuring hundreds of custom recipes, items, and quests." — [S47], project header
- **C244** [inference] Every established GregTech-centred expert pack checked sits on an old line as of Nov 2026: TFG-Modern, Monifactory, Star Technology, GT Community Pack and the new ATM9 Expert on 1.20.1 Forge; Nomi-CEu on 1.12.2; GTNH on 1.7.10. None of the checked repositories shows a released 1.21.1 version — from [C202], [C207], [C229], [C234], [C236], [C221], [C222], [C240], [C237], [C223]
- **C245** [inference] Movement toward 1.21.1 NeoForge exists but is unreleased or unofficial: GregTech Modern 8.0.0 snapshots, the Community Pack 1.21 branch, a small fan port of TFG, and (known only from a search snippet whose page could not be read) the pre-1.0 Createch 2 — from [C216], [C222], [C211], [C242]
- **C246** [inference] Stated migration positions range from "no intent at this time" (TFG) through "may or may not happen" (Monifactory) to silence (Star Technology, GTNH); none of these four has announced a migration date — from [C205], [C231], [C236], [C228]
- **C247** [inference] Kitchen-sink packs move on faster than expert packs: ATM is on 1.21.1 and already building for 26.1.2, while the same team places its GregTech expert pack on 1.20.1 — from [C238], [C239], [C240]

### Q3 — Цена написания своего Java-мода на каждой платформе — answered with a gap

- **C301** [quoted] NeoForge publishes builds for Minecraft 26.3 (26.3.0.115 is in the maven version list, metadata last updated 2026-11-21) — "<version>26.3.0.115</version>" — [S48], versions list
- **C302** [quoted] NeoForge still publishes builds for Minecraft 1.21.1 in Nov 2026 (21.1.267 sits among the newest entries of the same list) — "<version>21.1.267</version>" — [S48], versions list
- **C303** [measured] In the NeoForge version list (publish order) the last builds of 20.4, 20.6, 21.3, 21.4, 21.5, 21.8 and 21.11 form one batch (20.4.253 … 21.11.48); every entry after it belongs to 21.1, 26.1.2, 26.2.0 or 26.3.0 — four lines are live — Own measurements, M6
- **C304** [quoted] NeoForge's own 1.20.1 fork (artifact net.neoforged:forge, 1.20.1-47.1.x) was last published 2024-05-28 — "<lastUpdated>20240528174429</lastUpdated>" — [S49], versioning
- **C305** [quoted] MinecraftForge supports Minecraft 26.3 with a recommended build — ""26.3-recommended": "66.1.9"" — [S50], promos
- **C306** [quoted] MinecraftForge 1.20.1 latest build is 47.4.29 (recommended 47.4.28) — ""1.20.1-latest": "47.4.29"" — [S50], promos
- **C307** [quoted] The Forge 1.20.1 branch got a commit on 2026-11-08 that moves its MDK to ForgeGradle 7 — the branch is still being worked on — "Update MDK to use FG7 (#10923)" — [S51], newest entry, feed updated 2026-11-08T17:39:33Z
- **C308** [quoted] Forge's FAQ lists 1.20.1 in its full-support tier together with 26.3, 26.2, 26.1.2, 1.21.11, 1.21.1 and 1.20.6 — "Full support for 26.3, 26.2, 26.1.2, 1.21.11, 1.21.1, 1.20.6 and 1.20.1" — [S52], first post, Supported versions and platforms
- **C310** [quoted] For Forge versions below the full tier (legacy) the default MDK may not work because of old toolchains — "developing mods using the default MDK may not work" — [S53], Legacy support
- **C311** [quoted] Fabric lists 26.3 as a stable game version — ""version": "26.3"," — [S54], array, first stable entry
- **C312** [quoted] Mojang is already shipping 26.4 snapshots (ten so far), so a fourth 2026 drop is coming — ""version": "26.4-snapshot-10"," — [S54], first entry
- **C313** [quoted] Fabric released for 26.3 on 2026-09-15 with Loom 1.17 and Gradle 9.6.0 — "Developers should use Loom 1.17 and Gradle 9.6.0 (at the time of writing) to develop mods for Minecraft 26.3." — [S55], Fabric changes
- **C314** [inference] Minecraft 26.3 is out (Fabric's release post is dated 2026-09-15) and 26.4 is in snapshots; with the quarterly drop cadence that NeoForge's docs attribute to Mojang this is a new modding target about every three months — from [C301], [C305], [C311], [C312], [C313], [C330]
- **C315** [quoted] ModDevGradle (MDG) is NeoForge's newer Gradle plugin; NeoGradle remains supported beside it — "is a new Gradle plugin for NeoForge mod development, focused on performance, simplicity, and ease of use." — [S56], What is ModDevGradle anyway? (2025-01-02)
- **C316** [quoted] MDG has a legacy plugin for MinecraftForge 1.17–1.20.1, and NeoForge recommends it over ForgeGradle for 1.20.1 — "If you are still supporting 1.20.1 or older versions, we recommend that you upgrade from ForgeGradle to the MDG legacy plugin." — [S56], Notable improvements
- **C320** [quoted] The Forge 1.20.1 MDK now requires ForgeGradle 7 — "id 'net.minecraftforge.gradle' version '[7.0,8)'" — [S57], plugins block
- **C321** [quoted] Forge 1.20.1 mods target Java 17 — "java.toolchain.languageVersion = JavaLanguageVersion.of(17)" — [S57], after plugins block
- **C322** [quoted] ForgeGradle 7 is distributed through the Gradle Plugin Portal and needs a different settings.gradle than older ForgeGradle — "ForgeGradle 7 now lives primarily on the Gradle Plugin Portal" — [S58], reply by Jonathing (Forge Team), Feb 18 2026
- **C324** [quoted] Minecraft 1.20.5 and later need Java 21 — "Minecraft 1.20.5 now uses Java 21." — [S59], Java 21
- **C325** [quoted] Minecraft 26.1 needs Java 25, and NeoForge tooling for it needs Gradle 9.1.0+ — "Minecraft is now using Java 25, so once you have updated MDG or NG, make sure to update your Gradle version as well." — [S60], Updating
- **C326** [quoted] On Fabric 26.1+ the Loom plugin no longer remaps; mod* configurations and remapJar are replaced by the plain Gradle ones — "plugin, which does not remap Minecraft or mods. Any use of" — [S61], Unobfuscated Loom
- **C327** [quoted] Fabric dropped Yarn: mods must be on Mojang names from 26.1 — "now is the time to do so, as Yarn is no longer officially supported by Fabric." — [S61], intro
- **C328** [measured] Architectury API (Modrinth version list): the last builds with the forge loader are from Feb–Mar 2024 (1.20.1: 9.2.14+forge, 2024-02-26); there is no forge-loader build for any 1.21.x or 26.x — Own measurements, M7
- **C330** [quoted] Mojang moved from yearly major updates to quarterly drops and to year.release.patch numbering starting with 26.1 — "As an example, 26.1 will be the first release to adopt this new versioning, representing the first drop of 2026." — [S62], The Game Drop Era
- **C331** [quoted] From the 26.1 snapshots on, Minecraft ships unobfuscated — "Starting with 26.1 snapshots, obfuscation is no longer present and game executables are shipped with real variable, class, field and method names." — [S63], Obfuscated game? No more
- **C332** [quoted] For NeoForge modders the removal changes little in daily work (official names were already used since 1.17); the gain is real parameter and local-variable names — "obfuscation was effectively invisible in NeoForge development environments" — [S63], Obfuscated game? No more
- **C333** [quoted] On Fabric the 26.1 boundary is hard: nothing built for 1.21.11 or older loads without at least a recompile — "no mods from 1.21.11 or before will work without, at a minimum, recompilation." — [S61], intro
- **C334** [quoted] NeoForge marks a line with the -beta suffix while breaking changes are still allowed, and drops the suffix to stabilise the line — "suffix indicating that breaking changes are still allowed. As usual the" — [S60], Versioning
- **C335** [quoted] Each new NeoForge line starts with some weeks of beta in which its API may break — "As usual, we will have a few weeks of beta where breaking changes are allowed in the 21.2 series." — [S64], near the end
- **C337** [quoted] 1.20.5 replaced item NBT with data components, a break that lies between 1.20.1 and 1.21.1 for any mod storing data on items — "Instead, a stack can now have any number of data components. Each data component is a plain Java object, identified by a" — [S59], Item Data Components
- **C338** [quoted] Between 1.20.1 and 1.21 NeoForge itself reworked registries, capabilities, networking (twice) and events; the 21.0 post lists them for people coming from 1.20.1 — "we would like to quickly go through the changes made to NeoForge since 1.20.1" — [S65], Previous changes
- **C340** [quoted] 26.1 changed item stacks again: creating one needs loaded registries, data files use ItemStackTemplate — "s now require the registries to be loaded. To represent stacks before that, for example in recipes and other data files, there is now" — [S60], ItemStack templates
- **C343** [measured] Size of the NeoForged migration primers (lines of index.md): 1.20.5 1155; 1.21 721; 1.21.2 3199; 1.21.4 1448; 1.21.5 3069; 1.21.6 2203; 1.21.9 3062; 1.21.11 3173; 26.1 5038; 26.2 2640; 26.3 5844. Hotfix versions are tiny: 1.20.6 15; 1.21.1 20; 1.21.7 38; 1.21.8 19; 1.21.10 27 — Own measurements, M8
- **C344** [inference] A mod kept on the newest Minecraft faces a multi-thousand-line migration primer about every three months, while a mod pinned to a stabilised line (1.21.1, 1.20.1) faces none — from [C314], [C334], [C343]
- **C346** [quoted] docs.neoforged.net is versioned; the oldest version in its dropdown is 1.20.3 - 1.20.4 (no 1.20.1) and the current one is labelled 26.1 — "1.20.3 - 1.20.4" — [S62], version dropdown
- **C347** [measured] The string 26.3 does not occur in the HTML of the NeoForge docs page docs.neoforged.net/docs/gettingstarted/versioning/ as fetched on 2026-11-23: no 26.3 docs version yet — Own measurements, M9
- **C348** [quoted] Forge keeps easily accessible docs only for the latest version and current LTS versions — "Only the latest version and any current LTS versions will have easily accessible documentation and be included in the version dropdown in the sidebar." — [S66], Documentation for Legacy Versions
- **C350** [measured] The string 1.20.1 does not occur in the HTML of docs.fabricmc.net/develop/ as fetched on 2026-11-23, while 26.3 does: Fabric's current docs site does not offer a 1.20.1 version — Own measurements, M9
- **C351** [quoted] Porting primers by ChampionAsh5357 exist for every step from 1.20.4 -> 1.20.5 up to 26.2 -> 26.3 — "* [26.2 -> 26.3](./26.3/index.md)" — [S67], Versions
- **C354** [quoted] NeoForge retrospective, 2026-01-01: 1.21.1 was the most popular NeoForge version, over 16000 mods, growing faster than Forge 1.20.1 did — "it still remains the most popular NeoForge version, having accumulated over 16000 mods, with a growth rate surpassing that of Forge for 1.20.1" — [S63], The Versions
- **C355** [quoted] A NeoForge maintainer predicted in March 2026 that 26.1 would replace 1.21.1 as the stable modding version — "There are strong indications that 26.1 will be the" — [S60], intro
- **C356** [quoted] That prediction is explicitly personal, not NeoForge policy — "This is a personal educated guess, not an official statement from the NeoForge team as a whole." — [S60], intro
- **C357** [quoted] One NeoForge maintainer (TG) said it is not NeoForge's place to name the version modders should stay on, and that players and pack makers settle on the version before a big Minecraft break by themselves; another (Orion) answered "Yes" to whether such recommendations can be expected — "place to state what versions modders should stay on" — [S68], NAP 2025 Team notes, answer by TG
- **C358** [quoted] The May 2026 network-exploit fix went into 26.1.2 and 1.21.1 only; older lines were sent to a third-party mod — "Upgrade to the latest NeoForge for 26.1.2 / 1.21.1." — [S69], intro (2026-05-11)
- **C359** [measured] Modrinth projects of type mod per version+loader, 2026-11-23: 1.20.1 forge 25968; 1.20.1 fabric 18252; 1.21.1 neoforge 22773; 1.21.1 fabric 20540; 1.21.1 forge 5706; 1.21.11 neoforge 8667; 26.1.2 neoforge 8987; 26.1.2 fabric 14263; 26.1.2 forge 3685; 26.3 neoforge 6107; 26.3 fabric 10823; 26.3 forge 2984 — Own measurements, M10
- **C360** [measured] Modrinth modpacks per version+loader, 2026-11-23: 1.20.1 forge 3083; 1.20.1 fabric 4320; 1.21.1 neoforge 2328; 1.21.1 fabric 3126; 26.1.2 neoforge 75; 26.1.2 fabric 1380; 26.3 neoforge 29; 26.3 fabric 1001 — Own measurements, M10
- **C361** [measured] Modrinth mods first published since 2026-06-01 that support the version+loader: 1.21.1 neoforge 5776; 1.20.1 forge 4023; 26.1.2 fabric 4257; 1.21.1 fabric 3668; 26.3 fabric 3508; 26.1.2 neoforge 2435; 1.20.1 fabric 2068; 26.3 neoforge 1738 — Own measurements, M10
- **C362** [inference] As of Nov 2026 the NeoForge modpack ecosystem has not moved to 26.x: 75 modpacks on 26.1.2 against 2328 on 1.21.1, and 1.21.1 NeoForge still gets the most new mods among the combinations measured; 1.20.1 Forge and 1.21.1 NeoForge hold the two largest mod catalogues measured, 1.20.1 Forge the largest — from [C359], [C360], [C361], [C302], [C358]

### Q4 — Цена портирования чужого мода между версиями — answered with a gap

- **C401** [quoted] TerraFirmaCraft's 1.21 port started on 2024-07-09: the first commit on branch 1.21.x after the fork point from 1.20.x is a buildscript update by AlcatrazEscapee — "1.21: Update gradle, buildscript" — [S70], commit.message; commit.author.date 2024-07-09T02:46:32Z
- **C402** [quoted] The first public TerraFirmaCraft build for 1.21.1 (v4.0.0-beta) was published on 2025-09-13 — "2025-09-13T18:49:57Z" — [S71], published_at; name "TerraFirmaCraft 1.21.1-4.0.0-beta"
- **C403** [quoted] The TFC maintainer's release note for that build says the previous (1.20.1) first build was two years earlier and that the gap was hard work — "Our first build for 1.20.1 came out two years ago" — [S71], body, first paragraph (signed eerussianguy)
- **C404** [quoted] In the same note its author (eerussianguy) calls the mod unmaintainable and reports that AlcatrazEscapee, whom the note names as the most vital contributor, has retired from it — "continue fighting with this unmaintainable beast of a mod" — [S71], body, third paragraph; second paragraph: "decided to hang up the keyboard once and for all"
- **C405** [quoted] TFC dropped the beta label for 1.21.1 with v4.1.0, published 2026-04-04 — "This release drops the beta label." — [S72], body, first line; published_at 2026-04-04T20:01:29Z
- **C406** [measured] Scale of the TFC port: 806 commits landed on branch 1.21.x between 2024-07-01 and 2025-09-14; today 1.21.x is 2281 commits ahead of and 266 behind 1.20.x; the repo holds 9,126,167 bytes of Java — Own measurements, M11
- **C407** [inference] TFC needed about 14 months from first port commit to first public 1.21.1 beta and about 21 months to a non-beta release — from [C401], [C402], [C405], [C404]
- **C409** [quoted] GregTech CEu Modern's last non-snapshot release for 1.21.1 is v7.0.2-1.21.1 from 2025-07-20 — "2025-07-20T21:23:00Z" — [S73], published_at
- **C410** [quoted] GregTech Modern kept shipping stable releases for 1.20.1 after that, up to v7.5.3-1.20.1 on 2026-04-12 — "2026-04-12T20:45:02Z" — [S74], published_at
- **C411** [quoted] An issue opened on 2026-10-30 was filed against a 1.21-8.0.0 SNAPSHOT build of GregTech Modern — "1.21-8.0.0 SNAPSHOT a373c6e" — [S75], title; created_at 2026-10-30
- **C412** [quoted] Bringing GregTech's 1.21 branch level with 1.20.1 once (PR 4239, opened 2025-11-26, merged 2025-12-23) took about 46.7k added and 11.5k deleted lines in 1,157 files, and its author said it was too large to test fully — "Especially with 46,661 lines of new code, and 11,511 lines deleted, in 1,157 files" — [S76], body; fields additions 47382, changed_files 1160, commits 443
- **C416** [measured] GregTech Modern's default branch is still 1.20.1; branch 1.21 is 901 commits ahead of and 248 behind it (merge base 2026-07-12); the repo holds 11,077,142 bytes of Java; both lines now build as 8.0.0 SNAPSHOT — Own measurements, M12
- **C417** [inference] GregTech Modern has had no stable 1.21.1 release for 16 months (2025-07 to 2026-11) while both branches stayed active and one PR syncing 1.21 with 1.20.1 alone added about 47k lines; no maintainer statement giving the reason was found — from [C409], [C410], [C411], [C412], [C416]
- **C418** [quoted] Create's first version for 1.21.1 (6.0.0, NeoForge) was published on 2025-03-01 — "2025-03-01T00:46:58.205972Z" — [S77], entry with version_number "1.21.1-6.0.0"
- **C420** [measured] Create has no branch or release for any Minecraft version newer than 1.21.1; its latest release is mc1.21.1-6.0.10 (2026-04-21); the repo holds 9,635,922 bytes of Java — Own measurements, M13, M1
- **C421** [inference] Create reached 1.21.1 about 8.5 months after NeoForge's first 1.21 build and has not moved past 1.21.1 in the 20 months since — from [C418], [C420], [C434]
- **C423** [quoted] AgriCraft's 1.21 branch is still an alpha that was never published: its version string is 4.0.3-alpha against NeoForge 21.1.80 — "version=4.0.3-alpha" — [S78], first lines; neoforge_version=21.1.80
- **C426** [quoted] Serene Seasons shipped for 1.21 on Fabric, Forge and NeoForge on 2024-06-15, two days after NeoForge's first 1.21 beta — "2024-06-15T02:15:01.039345Z" — [S79], entry "10.0.0.2" (neoforge, 1.21)
- **C427** [measured] Serene Seasons has versions for most releases from 1.20.1 through 26.3 (latest 2026-09-21; none tagged 1.21.2 or 26.1 in the recorded output) and is 145,062 bytes of Java, about 1/60 of TerraFirmaCraft — Own measurements, M14, M2
- **C428** [quoted] NeoForge 20.2 (first post-fork version, 2023-10-30) renamed every package to net.neoforged and offers a remapping script for the mechanical part — "to automatically apply the class and package renames" — [S80], NeoForge Changes; same section: "All the packages have been changed to"
- **C429** [quoted] NeoForge 20.3 replaced the Forge capability system with two systems (data attachments and capabilities) — "comes with a fundamental redesign of the capability system" — [S81], Introduction, 2023-12-05
- **C430** [quoted] NeoForge 20.4 replaced Forge's SimpleChannel and EventChannel networking with one payload-based system — "it was decided to rework both implementations into a single system" — [S82], SimpleChannel and EventChannel, 2023-12-31
- **C431** [quoted] Minecraft 1.20.5 removed NBT tags from item stacks in favour of immutable data components — "Instead, a stack can now have any number of data components." — [S59], Item Data Components, 2024-04-23
- **C434** [quoted] The NeoForge 21.0 post (2024-06-13) tells modders coming from 1.20.1 to work through the per-version change list of 1.20.2 to 1.20.6 first — "we would like to quickly go through the changes made to NeoForge since 1.20.1" — [S65], Previous changes; dated June 13, 2024
- **C437** [quoted] After 1.21.1, NeoForge 21.9 (2025-09-30) redesigned item, fluid and energy transfer (IItemHandler, IFluidHandler, IEnergyStorage) — "comes with a fundamental redesign of the item, fluid and energy transfer systems" — [S83], Introduction
- **C440** [quoted] TerraFirmaCraft is EUPL-1.2 (copyleft, redistribution of a modified port allowed under the same licence) — "Licensed under the [EUPL, Version 1.2](LICENSE.txt) unless otherwise noted." — [S84], Legal
- **C441** [quoted] Create's code is MIT but its assets are All Rights Reserved, so a self-port cannot redistribute textures and models without permission — "## Assets License (All Rights Reserved)" — [S85], License Overview; next heading "## Code License (MIT)"
- **C442** [quoted] Serene Seasons is All Rights Reserved: its LICENSE file is that one sentence — "All rights reserved." — [S86], whole file (20 bytes)
- **C443** [quoted] FTB Quests declares All Rights Reserved in its mod metadata and has no LICENSE file in the repo — "All Rights Reserved" — [S87], license key
- **C444** [measured] GitHub licence detection: GregTech-Modern LGPL-3.0, KubeJS LGPL-3.0, AgriCraft MIT, Sinytra Connector MIT, Mekanism MIT, TerraFirmaCraft EUPL-1.2; Create and Serene Seasons NOASSERTION; FTB-Quests has no licence (404) — Own measurements, M15
- **C445** [quoted] Sinytra Connector runs Fabric mods on NeoForge only; it is not a Forge-to-NeoForge or cross-version layer — "is a translation/compatibility layer that allows running" — [S88], About
- **C446** [quoted] Connector's support table marks 26.2.x and 26.1.x as supported, 1.21.1 as LTS and 1.20.1 as EOL — "critical bugfixes only. The latest compatibility fixes might not be available." — [S88], Supported versions; all GitHub releases are betas, latest for 1.21.1 is 2.0.0-beta.17+1.21.1 (2026-08-15)
- **C447** [quoted] Counter to "no compat layer exists": Retromod (MIT, on Modrinth since 2026-03-13) claims to run mods built for older versions on newer ones, including (per its docs, [S90]) 1.20.1 Forge mods on NeoForge — "Retromod lets you take a mod built for 1.18 and get it to run on 26.3 without waiting for them to update it." — [S89], body; the Forge-on-NeoForge part is in [S90], Large Version Jumps; 44.5k downloads, GitHub Bownlux/Retromod 59 stars
- **C448** [quoted] Retromod's own docs say its Forge-on-NeoForge support is partial and that redesigned subsystems need a manual port — "This is partial compatibility, not a complete Forge runtime." — [S90], Large Version Jumps; top: "It cannot recreate a subsystem that Minecraft or a loader completely redesigned."
- **C449** [quoted] Retromod lists Create and Applied Energistics 2 among mods it cannot translate — "Create 6 for 1.21.1 also uses hundreds of classes and members that 26.3 removed or reshaped" — [S90], Common Examples table; AE2 row: "Broad networking, storage, and platform integration"
- **C453** [quoted] Create-UfoPort, a third-party port of Create Fabric to 1.21, was created 2024-08-01 and has had no push since 2025-03-21 — "Unoficial port of Create-fabric for MC-1.21" — [S91], description; pushed_at 2025-03-21T16:22:05Z, 18 open issues, not archived
- **C454** [quoted] Counter to "a large mod takes many months": Mekanism (12.4 MB of Java, MIT) published for 1.21 on 2024-06-13, the day of NeoForge's first 1.21 beta — "2024-06-13T22:33:35.754504Z" — [S92], entry "10.6.0.44" (neoforge, 1.21)
- **C455** [quoted] Applied Energistics 2 likewise had a 1.21 alpha on 2024-06-16 — "2024-06-16T23:55:55.271948Z" — [S93], entry "19.0.2-alpha" (neoforge, 1.21)
- **C456** [measured] Mekanism and AE2 had both already released for the intermediate versions (Mekanism 1.20.4 from 2024-03-01; AE2 1.20.4 from 2023-12-24 and 1.20.6 from 2024-04-29), while TFC, GregTech Modern and Create jumped from 1.20.1 straight to 1.21.x — Own measurements, M16, M1
- **C457** [inference] Among the five mods checked, port time tracks the size of the version jump and whether the team followed each intermediate version, not mod size alone: teams that ported at every step shipped 1.21 in days, the three that skipped from 1.20.1 took 8 to 14 months or are unfinished — from [C407], [C417], [C421], [C454], [C455], [C456]
- **C458** [inference] For a solo author, self-porting is legally distributable for TFC, GregTech, KubeJS, AgriCraft (open licences) but not for Serene Seasons or FTB Quests, and for Create only the code — from [C440], [C441], [C442], [C443], [C444]

## Disconfirming evidence

- [C105] — query: Modrinth search `terrafirmacraft` restricted by facets to versions 26.1.2 / 26.1 / 1.21.11, and unrestricted — nothing found against: only two addons are tagged for those versions; no TerraFirmaCraft port or fork above 1.21.1 and no Fabric port.
- [C113] — query: GitHub issue search `repo:GregTechCEu/GregTech-Modern 1.21 in:title`, plus the Modrinth and GitHub release lists — nothing found against: no stable 1.21.1 file newer than 7.0.2; issues from autumn 2026 refer to 8.0.0 snapshot builds ([C219], [C411]). Evidence that the port is alive, not finished: [C216], [C217], [C222].
- [C117] — query: Create GitHub releases and branches for any 1.20.1 activity after 6.0.8; issue search `repo:Creators-of-Create/Create 1.20.1 6.0.9` — nothing found against; no maintainer statement declaring 1.20.1 dropped was found either, so the freeze is observed, not announced.
- [C118] — query: GitHub issue search `repo:Fabricators-of-Create/Create 1.21` — confirms it: [C119].
- [C134] — query: Modrinth search `agricraft` and `agricraft reloaded`; GitHub repo search `agricraft in:name pushed:>2026-01-01`; AgriCraft issue search `1.21` — no AgriCraft 1.21.x port found; the only counter-evidence is the unmerged community pull request [C133].
- [C128] — query: FTB maven artifact lists for forge / neoforge / fabric — nothing found against: no Forge artifact above the 1.20.1 line ([C127]).
- [C205] — query: web search `TerraFirmaGreg 1.21 port NeoForge`; issue search `neoforge` and `port to 1.21` on the TerraFirmaGreg repo; the org's repo and branch lists — no official port, branch or announcement ([C207]). Found against: an unofficial fan port, [C211], [C212].
- [C244] — query: web search `expert modpack 1.21.1 NeoForge GregTech 2026 release` — found against: [C242] (Createch 2, GregTech on 1.21.1, pre-1.0) and [C243] (non-GregTech expert packs on 1.21.1). [C242] is a search snippet only (its CurseForge page could not be fetched); the page behind [C243] was read at audit and names Absolute Industrialization only.
- [C228] — query: web search `GT New Horizons FAQ "1.7.10" will the pack be updated ported to newer Minecraft version`; issue-title search on the GTNH repo — no port announcement and no explicit policy statement either way.
- [C308] — query: web search `Forge 1.20.1 end of life no longer supported ForgeGradle 6 deprecated FG7 2026` — the opposite of "Forge 1.20.1 is unmaintained" holds: full-support tier [C308], a build and an MDK change in November 2026 [C306], [C307]. Against 1.20.1 as a healthy target: NeoForge's own 1.20.1 fork has been dead since 2024 [C304], and Architectury stopped forge-loader builds in 2024 [C328].
- [C362] — query: web search `modders staying on NeoForge 1.21.1 not porting to 26.1 "1.21.1" modpack 2026 All the Mods 26.1` and `NeoForge supported versions policy 1.21.1 LTS "26.1" stable modding version 2026` — found against: [C355] (26.1 expected to replace 1.21.1), weakened by [C356]; [C239] (the All the Mods team already builds on 26.1.2). The Modrinth modpack counts [C360] do not show the move having happened.
- [C344] — lookup: primer sizes and NeoForge release posts, testing "NeoForge breaks API on every Minecraft release" — partly refuted: hotfix versions have near-empty primers [C343] and stabilised lines forbid breaks [C334]; it holds for each quarterly drop.
- [C407] — lookup: Modrinth first-release dates of other large mods for 1.21 — found against "a large mod takes many months": [C454], [C455], [C456]; the reconciling reading is [C457].
- [C445] — query: web search `compatibility layer run 1.20.1 Forge mods on NeoForge 1.21.1 without porting` — found against in part: Retromod exists [C447], but declares itself partial and excludes Create and AE2 [C448], [C449]. It was not run here.
- [C417] — query: web search `mod developer "not porting" OR "won't be ported" OR "no plans to port" 1.21 NeoForge large mod reason data components rewrite` — no public "we are not porting" statement found; stalled ports show only as silence ([C423], [C453]).

## Coverage

| Channel | Read | Gap |
|---|---|---|
| Users | [S24], [S10], [S58], [S31] | No Hacker News or GitHub Discussions threads; Discord (where TerraFirmaCraft, GregTech and TerraFirmaGreg discuss plans) cannot be read |
| Specialists | [S60], [S65], [S62], [S61], [S52], [S67], [S68] | minecraft.net blocked (curl HTTP 000, two fetch timeouts): Mojang's cadence, numbering and deobfuscation are sourced from NeoForge and Fabric texts, not Mojang's own |
| Data | [S28], [S48], Own measurements M2, M8, M10 | No download or player statistics; Modrinth counts are catalogue size. CurseForge is not fetchable by plain HTTP, so no CurseForge file dates or counts |
| Adversarial | [S39], [S71], [S76], [S90], [S91] | No written post-mortem of a port was found; cost is shown by dates, commits and line counts only |

Declared overruns of the ceiling (four searches and three full page reads per sub-question): all four sub-questions exceeded the page-read ceiling, because each covers several mods, packs or loaders that need their own registry lookups — Q1 about 40 API and README reads for seven mods; Q2 about 15 lookups for eight packs; Q3 about 45 page and metadata fetches for three loaders; Q4 eight blog pages plus about 40 API lookups for five mods. Web searches stayed at or under four per sub-question. Ledgers stayed under the 20,000-character cap (19,988 / 16,849 / 19,985 / 19,998).

Tools: the built-in floor only (web search, `curl`, `gh`); no connector. Every `quoted` claim was copied from `curl` or `gh` output. Failures: minecraft.net unreachable; CurseForge and its proxies unreachable (Cloudflare); the GTNH wiki answers page requests with a bot check; FTB Quests and the checked expert packs are not on Modrinth, so GitHub and maven.ftb.dev were used; unauthenticated api.github.com requests hit the rate limit during quote re-checks (60 per hour), which will also affect the plain-HTTP quote check of this report. Reddit was not used.

Not covered: Enigmatica, FTB packs and Create: Astral (Q2); ForgeGradle 7 documentation (none was reachable); NeoForge release notes 21.2–21.11 other than the 21.9 transfer rework (Q4).

## Own measurements

All commands are read-only lookups of public registries, run on 2026-11-23. M1, M2, M6–M10, M15 and M16 were re-run by the main thread after gathering and the output below is from that re-run; M3–M5 and M11–M14 are reproduced as the gathering subagents recorded them.

### M1 — GitHub state of the seven mods: releases, default branch, branch heads

Command: `bash meas-gh.sh` — a scratch script of `gh api repos/<owner>/<repo>/releases`, `/branches`, `/commits?sha=<branch>&per_page=1` with `--jq` filters, `curl` of `gradle.properties`, and `grep -c` over `https://maven.ftb.dev/releases/dev/ftb/mods/<artifact>/maven-metadata.xml`.

Output:

```
## Q1-M1 TerraFirmaCraft releases / branches
v4.2.16 2026-11-12 prerelease=false
v4.2.15 2026-11-08 prerelease=false
v3.2.30 2026-11-08 prerelease=false
v4.2.14 2026-11-07 prerelease=false
v3.2.29 2026-11-07 prerelease=false
v4.2.13 2026-11-01 prerelease=false
v3.2.28 2026-10-20 prerelease=false
v4.2.12 2026-10-14 prerelease=false
v3.2.27 2026-10-10 prerelease=false
v4.2.11 2026-09-27 prerelease=false
v3.2.26 2026-09-28 prerelease=false
v4.2.10 2026-09-07 prerelease=false
default_branch=1.21.x
1.12.x, 1.16.x, 1.17.x, 1.18.x, 1.20.x, 1.21.x
## Q1-M3 GregTech-Modern repo / releases / branch heads
default_branch=1.20.1
latest-1.21 | 1.21-8.0.0 SNAPSHOT 2918b17 | 2026-11-22 prerelease=true
latest-1.20.1 | 1.20.1-8.0.0 SNAPSHOT 5a6aff9 | 2026-11-21 prerelease=true
v7.5.3-1.20.1 | v7.5.3-1.20.1 | 2026-04-12 prerelease=false
v7.5.2-1.20.1 | v7.5.2-1.20.1 | 2026-02-14 prerelease=false
v7.5.1-1.20.1 | v7.5.1-1.20.1 | 2026-02-09 prerelease=false
v7.5.0-1.20.1 | v7.5.0-1.20.1 | 2026-02-08 prerelease=false
v7.4.1-1.20.1 | v7.4.1-1.20.1 | 2026-01-06 prerelease=false
v7.4.0-1.20.1 | v7.4.0-1.20.1 | 2025-11-25 prerelease=false
v7.3.0-1.20.1 | v7.3.0-1.20.1 | 2025-11-16 prerelease=false
v7.2.1-1.20.1 | v7.2.1-1.20.1 | 2025-10-26 prerelease=false
v.7.2.0-1.20.1 | v7.2.0-1.20.1 | 2025-09-22 prerelease=false
v7.1.4-1.20.1 | v7.1.4-1.20.1 | 2025-08-08 prerelease=false
v7.1.3-1.20.1 | v7.1.3-1.20.1 | 2025-08-07 prerelease=false
v7.1.2-1.20.1 | v7.1.2-1.20.1 | 2025-08-04 prerelease=false
branch 1.20.1 head 2026-11-22
branch 1.20.1 gradle.properties: mod_version = 8.0.0
branch 1.21 head 2026-11-22
branch 1.21 gradle.properties: mod_version = 8.0.0
branch 26.1 head 2026-04-28
branch 26.1 gradle.properties: mod_version = 8.0.0
## Q1-M5 Create releases / branch heads
mc1.21.1-6.0.10 2026-04-21
mc1.21.1-6.0.9 2026-01-02
mc1.21.1-6.0.8 2025-11-02
mc1.20.1-6.0.8 2025-11-02
mc1.21.1-6.0.7 2025-10-25
mc1.20.1-6.0.7 2025-10-25
default_branch=mc1.21.1/dev
l10n_crowdin_translations, mc1.14/dev, mc1.14/high-logistics, mc1.14/release, mc1.15/dev, mc1.15/release, mc1.16/chromatic-projector, mc1.16/dev, mc1.16/lastertag, mc1.17/dev, mc1.18/dev, mc1.18/solver, mc1.18/0.5.1, mc1.19/dev, mc1.19/0.5.1, mc1.19.3/dev, mc1.20.1/client-contraption, mc1.20.1/dev, mc1.20.1/0.5.1, mc1.21.1/dev, mc1.21.1/formatting, mc1.21.1/runtime-recipe, mc1.21.1/stockkeeper-jei-sync
branch mc1.20.1/dev head 2025-11-30
branch mc1.21.1/dev head 2026-11-17
## Q1-M6 KubeJS branch heads
default_branch=2601
branch 2001 head 2026-05-01
branch 2101 head 2026-11-15
branch 2601 head 2026-11-15
branch 2603 head 2026-10-05
## Q1-M7 FTB Quests tags
tag v2001.4.22 commit date 2026-05-12
tag v2111.1.5 commit date 2026-04-11
tag v26.1.2.10 commit date 2026-11-05
tag v2101.1.39 commit date 2026-11-16
ftb-quests-forge: 2001.x=37 2101.x=0 2111.x=0 26.x=0
ftb-quests-neoforge: 2001.x=0 2101.x=40 2111.x=5 26.x=10
ftb-quests-fabric: 2001.x=37 2101.x=40 2111.x=5 26.x=10
## Q1-M8 AgriCraft repo / branch heads
pushed_at=2025-11-24 default_branch=master
branch 1.20.1 head 2025-11-24
branch 1.21 head 2025-04-06
```

### M2 — Modrinth: latest version per (game version, loader) for six mods

Command: `python -I mr.py terrafirmacraft gregtechceu-modern create kubejs agricraft serene-seasons` — reads `https://api.modrinth.com/v2/project/<slug>/version` and prints, per game version and loader, the newest `version_number`, `date_published` and `version_type`. Output filtered to the lines for 1.7.10, 1.12.2, 1.18.2, 1.20.1, 1.20.4, 1.21.x and 26.x.

Output:

```
== terrafirmacraft versions: 94 bytes: 199553
  1.7.10     forge     0.79.30.925                              2023-09-24 release id=XhQYXiMy
  1.12.2     forge     1.7.23.181                               2023-09-24 release id=k4rM7PTU
  1.18.2     forge     2.2.33                                   2024-01-28 release id=gGitRm7b
  1.20.1     forge     3.2.30                                   2026-11-08 release id=h5kAR6P0
  1.20.1     neoforge  3.2.30                                   2026-11-08 release id=h5kAR6P0
  1.21.1     neoforge  4.2.16                                   2026-11-12 release id=zM6ZmilV
== gregtechceu-modern versions: 242 bytes: 688521
  1.20.1     fabric    mc1.20.1-1.0.20.b-fabric                 2024-01-08 alpha id=Wqs6yfdU
  1.20.1     forge     mc1.20.1-7.5.3                           2026-04-12 beta id=yLwvjuvy
  1.21       neoforge  mc1.21.1-7.0.2                           2025-07-20 beta id=iFPVZs0V
  1.21.1     neoforge  mc1.21.1-7.0.2                           2025-07-20 beta id=iFPVZs0V
== create versions: 58 bytes: 215585
  1.18.2     forge     1.18.2-0.5.1.i                           2024-10-09 release id=7BIftSin
  1.20.1     forge     mc1.20.1-6.0.8                           2025-11-02 release id=8amzvn9x
  1.20.1     neoforge  mc1.20.1-6.0.8                           2025-11-02 release id=8amzvn9x
  1.21.1     neoforge  6.0.10+mc1.21.1                          2026-04-21 release id=UjX6dr61
== kubejs versions: 330 bytes: 396540
  1.18.2     fabric    1802.5.5-build.569+fabric                2023-02-02 release id=TSy6UYgH
  1.18.2     forge     1802.5.5-build.569+forge                 2023-02-02 release id=MngQb49Q
  1.18.2     quilt     1802.5.5-build.569+fabric                2023-02-02 release id=TSy6UYgH
  1.20.1     fabric    2001.6.5-build.26+fabric                 2026-05-01 release id=Pu0Ygbq6
  1.20.1     forge     2001.6.5-build.26+forge                  2026-05-01 release id=hVR2xUSr
  1.20.1     neoforge  2001.6.5-build.14+forge                  2024-06-21 release id=AhYGbfUn
  1.20.1     quilt     2001.6.5-build.26+fabric                 2026-05-01 release id=Pu0Ygbq6
  1.20.4     neoforge  2004.7.0-build.26+KubeJS-1.20.4          2024-06-21 release id=XFk8J9v0
  1.21       neoforge  2101.7.2-build.381                       2026-11-01 release id=GDXCpKlp
  1.21.1     neoforge  2101.7.2-build.381                       2026-11-01 release id=GDXCpKlp
  26.1.2     neoforge  26.1.2-8.0.8+neoforge                    2026-11-01 beta id=5yNEdyfc
== agricraft versions: 28 bytes: 38967
  1.18.2     forge     1.18.2-3.1.6                             2023-06-18 release id=iWRjz6ig
  1.20.1     fabric    4.0.6                                    2025-11-24 release id=4WMdlCQl
  1.20.1     forge     4.0.6                                    2025-11-24 release id=qXNjK6M2
  1.20.4     fabric    4.0.2-beta                               2024-06-23 beta id=gzFfVQTt
  1.20.4     forge     4.0.2-beta                               2024-06-23 beta id=WuTR4AOu
  1.20.4     neoforge  4.0.2-beta                               2024-06-23 beta id=H977V6EU
== serene-seasons versions: 423 bytes: 1589402
  1.7.10     forge     1.2.18                                   2023-11-19 alpha id=NoGF2P4Y
  1.12.2     forge     1.2.18                                   2023-11-19 release id=BMsrDMPq
  1.18.2     forge     7.0.0.15                                 2023-11-19 beta id=k8G3p1jL
  1.20.1     fabric    9.1.0.3                                  2026-06-14 beta id=GICZwCsE
  1.20.1     forge     9.1.0.3                                  2026-06-14 beta id=WMrhrybF
  1.20.4     fabric    9.3.0.25                                 2024-03-23 beta id=9QwOxxTJ
  1.20.4     forge     9.3.0.25                                 2024-03-23 beta id=PaJ7DrkP
  1.20.4     neoforge  9.3.0.25                                 2024-03-23 beta id=xMj5T99Y
  1.21       fabric    10.0.0.6                                 2024-08-10 beta id=q8BN28TQ
  1.21       forge     10.0.0.6                                 2024-08-10 beta id=WZ9gfayc
  1.21       neoforge  10.0.0.6                                 2024-08-10 beta id=Ga3tXpjj
  1.21.1     fabric    10.1.0.9                                 2026-09-05 beta id=q34wStJM
  1.21.1     forge     10.1.0.9                                 2026-09-05 beta id=WTmfEUJ2
  1.21.1     neoforge  10.1.0.9                                 2026-09-05 beta id=pHEgQQUE
  1.21.3     fabric    10.2.0.3                                 2025-02-12 beta id=LYi0AP7g
  1.21.3     forge     10.2.0.3                                 2025-02-12 beta id=pjmGXpqr
  1.21.3     neoforge  10.2.0.3                                 2025-02-12 beta id=KQ2wyU1I
  1.21.4     fabric    10.4.0.6                                 2025-03-13 beta id=qanVNGyt
  1.21.4     forge     10.4.0.6                                 2025-03-13 beta id=Jer872tf
  1.21.4     neoforge  10.4.0.6                                 2025-03-13 beta id=D6fJLkyS
  1.21.5     fabric    10.5.0.5                                 2025-07-09 beta id=kf9KmhKn
  1.21.5     forge     10.5.0.5                                 2025-07-09 beta id=WqNK60vD
  1.21.5     neoforge  10.5.0.5                                 2025-07-09 beta id=Jrz0m31t
  1.21.6     fabric    21.6.0.0                                 2025-06-22 release id=i9QFEv4n
  1.21.6     forge     21.6.0.0                                 2025-06-22 release id=i0GLDKIN
  1.21.6     neoforge  21.6.0.0                                 2025-06-22 release id=FuJOYqfx
  1.21.7     fabric    21.7.0.0                                 2025-07-09 beta id=OpOJrIBd
  1.21.7     forge     21.7.0.0                                 2025-07-09 beta id=4WYLQMqQ
  1.21.7     neoforge  21.7.0.0                                 2025-07-09 beta id=49HIwv0J
  1.21.8     fabric    21.8.0.0                                 2025-07-26 beta id=30NJ0mrz
  1.21.8     forge     21.8.0.0                                 2025-07-26 beta id=L54W4P20
  1.21.8     neoforge  21.8.0.0                                 2025-07-26 beta id=yXn7Ysxn
  1.21.9     fabric    21.8.0.1                                 2025-10-04 beta id=5qHMm4uh
  1.21.9     forge     21.9.0.0                                 2025-10-04 beta id=56tVnCiq
  1.21.9     neoforge  21.9.0.0                                 2025-10-04 beta id=VfwDD31c
  1.21.10    fabric    21.10.0.0                                2025-10-19 beta id=Y0zdqWGx
  1.21.10    forge     21.9.0.1                                 2025-10-19 beta id=u6AwS6Or
  1.21.10    neoforge  21.9.0.1                                 2025-10-19 beta id=s5sv0NSL
  1.21.11    fabric    21.11.0.4                                2026-03-18 beta id=6GpaVF7N
  1.21.11    forge     21.11.0.4                                2026-03-18 beta id=hhE5jnEN
  1.21.11    neoforge  21.11.0.4                                2026-03-18 beta id=eDxStG5h
  26.1.1     fabric    26.1.1.0.1                               2026-04-13 beta id=6vz3W8ar
  26.1.1     forge     26.1.1.0.1                               2026-04-13 beta id=3vjAFp6O
  26.1.1     neoforge  26.1.1.0.1                               2026-04-13 beta id=ZwWxXUVt
  26.1.2     fabric    26.1.2.0.7                               2026-08-31 beta id=kxB8vG5W
  26.1.2     forge     26.1.2.0.7                               2026-08-31 beta id=KwznUXnK
  26.1.2     neoforge  26.1.2.0.7                               2026-08-31 beta id=atclxQCD
  26.2       fabric    26.1.2.0.6                               2026-09-21 beta id=q5mzi8wy
  26.2       forge     26.1.2.0.6                               2026-09-21 beta id=lBl6tsYm
  26.2       neoforge  26.1.2.0.6                               2026-09-21 beta id=MZd2wAtJ
  26.3       fabric    26.1.2.0.7                               2026-09-21 beta id=V9PxJPuw
  26.3       forge     26.1.2.0.7                               2026-09-21 beta id=RLW2r4KA
  26.3       neoforge  26.1.2.0.7                               2026-09-21 beta id=QnvW5HPm
```

### M3 — TerraFirmaGreg-Modern branches and lockfiles

Command and output, as recorded:

```
M1 — `gh api "repos/TerraFirmaGreg-Team/Modpack-Modern/branches?per_page=100"` → 0.14, current_private_server_branch, dev, feat/microbialcultivator, feature/immersive_vehicles, feature/large_boiler, feature/material-stats, feature/nebby_g/FirmacivImpl, feature/ore_proc_rework, feature/splice/all-modid, feature/starcatcher_compat, feature/venus_fire, field_guide_gt, gtm8, main, platline. `pakku-lock.json` on dev, 0.14, gtm8: mc_versions ['1.20.1'], loaders {'forge': '47.4.13'}. Last commits: dev 2026-11-22, 0.14 2026-11-22, gtm8 2026-11-22.
```

### M4 — GregTech Modern branch named 26.1

Command and output, as recorded:

```
M2 — `gh api repos/GregTechCEu/GregTech-Modern/commits/26.1` → 9bd36746, 2026-04-28, "Clean up unused, duplicated, and/or unnecessary tool handling code (#4493)". `curl -sL https://raw.githubusercontent.com/GregTechCEu/GregTech-Modern/26.1/gradle/libs.versions.toml | sed -n 1,4p` → minecraft = "1.21.1", neoForge = "21.1.215". Branch 1.21 head: 2918b172, 2026-11-22.
```

### M5 — Star Technology: issues and branches mentioning 1.21 or NeoForge

Command and output, as recorded:

```
M3 — `gh issue list --repo StarT-Dev-Team/Star-Technology --state all --search "1.21 in:title,body"` and the same for "neoforge" → one unrelated hit (#423, a recipe bug). Branches: develop, legacy-1.19.2, legacy-hardmode, main and feature branches; none named for 1.21.
```

### M6 — NeoForge maven version list: last build of each line, by list position

Command: `python -I neometa.py` over `https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml` (1877 versions). For each line it prints the list index and version of the last entry, and the first entry without a `-beta` suffix. List order is taken as publish order. Lines whose last entry is a beta are omitted here.

Output:

```
1877
0.25w14craftmine last 17 0.25w14craftmine.5-beta first-stable None
20.4 last 1829 20.4.253 first-stable (265, '20.4.167')
20.6 last 1828 20.6.144 first-stable (472, '20.6.115')
21.1 last 1865 21.1.267 first-stable (662, '21.1.1')
21.3 last 1827 21.3.100 first-stable (969, '21.3.56')
21.4 last 1825 21.4.161 first-stable (1130, '21.4.121')
21.5 last 1824 21.5.101 first-stable (1241, '21.5.74')
21.8 last 1822 21.8.57 first-stable (1322, '21.8.9')
21.10 last 1447 21.10.64 first-stable (1446, '21.10.63')
21.11 last 1823 21.11.48 first-stable (1490, '21.11.42')
26.1.2 last 1876 26.1.2.129 first-stable (1595, '26.1.2.71')
26.2.0 last 1867 26.2.0.95 first-stable (1696, '26.2.0.57')
26.3.0 last 1875 26.3.0.115 first-stable (1813, '26.3.0.76')
```

### M7 — Architectury API: builds with the forge loader

Command: `python -I arch2.py` over `https://api.modrinth.com/v2/project/architectury-api/version`.

Output:

```
latest forge-loader versions: [('2024-03-19', '10.1.20+minecraftforge', ['1.20.2']), ('2024-03-11', '11.1.17+minecraftforge', ['1.20.4']), ('2024-02-26', '9.2.14+forge', ['1.20', '1.20.1'])]
any forge loader on 26.x: False
any forge loader on 1.21.x: []
```

### M8 — Size of the NeoForged migration primers

Command: `python -I primers.py` — fetches `https://raw.githubusercontent.com/neoforged/.github/main/primers/<version>/index.md` and counts bytes and lines.

Output:

```
1.20.5 84942 bytes 1155 lines | # Minecraft 1.20.4 -> 1.20.5 Mod Migration Primer
1.20.6 1110 bytes 15 lines | # Minecraft 1.20.5 -> 1.20.6 Mod Migration Primer
1.21 56949 bytes 721 lines | # Minecraft 1.20.6 -> 1.21 Mod Migration Primer
1.21.1 1251 bytes 20 lines | # Minecraft 1.21 -> 1.21.1 Mod Migration Primer
1.21.2 200797 bytes 3199 lines | # Minecraft 1.21.1 -> 1.21.2 Mod Migration Primer
1.21.4 85387 bytes 1448 lines | # Minecraft 1.21.2/3 -> 1.21.4 Mod Migration Primer
1.21.5 194956 bytes 3069 lines | # Minecraft 1.21.4 -> 1.21.5 Mod Migration Primer
1.21.6 141467 bytes 2203 lines | # Minecraft 1.21.5 -> 1.21.6 Mod Migration Primer
1.21.7 2826 bytes 38 lines | # Minecraft 1.21.6 -> 1.21.7 Mod Migration Primer
1.21.8 1230 bytes 19 lines | # Minecraft 1.21.7 -> 1.21.8 Mod Migration Primer
1.21.9 210631 bytes 3062 lines | # Minecraft 1.21.8 -> 1.21.9 Mod Migration Primer
1.21.10 1486 bytes 27 lines | # Minecraft 1.21.9 -> 1.21.10 Mod Migration Primer
1.21.11 191129 bytes 3173 lines | # Minecraft 1.21.10 -> 1.21.11 Mod Migration Primer
26.1 307202 bytes 5038 lines | # Minecraft 1.21.11 -> 26.1 Mod Migration Primer
26.2 182451 bytes 2640 lines | # Minecraft 26.1.x -> 26.2 Mod Migration Primer
26.3 348269 bytes 5844 lines | # Minecraft 26.2 -> 26.3 Mod Migration Primer
```

### M9 — Version strings present in the loaders' documentation pages

Command: `python -I arch2.py` — substring tests on the `curl` output of `docs.minecraftforge.net/en/fg-6.x/` (forge-fgdocs.html), `docs.neoforged.net/docs/gettingstarted/versioning/` (neodocs-versioning.html), `docs.fabricmc.net/develop/` (fabdocs.html), `docs.minecraftforge.net/en/latest/` (forge-docs.html) and the NeoForge team notes (hackmd.html), pages fetched 2026-11-23.

Output:

```
== forge-fgdocs.html
  contains 26.2 False
  contains 26.3 False
  contains 1.20.1 False
== neodocs-versioning.html
  contains 26.2 True
  contains 26.3 False
  contains 1.20.1 True
== fabdocs.html
  contains 26.2 True
  contains 26.3 True
  contains 1.20.1 False
== forge-docs.html
  contains 26.2 False
  contains 26.3 False
  contains 1.20.1 True
== hackmd.html
  contains 26.2 False
  contains 26.3 False
  contains 1.20.1 True
```

### M10 — Modrinth catalogue counts per version and loader

Command: `python -I modrinth.py` and `python -I modrinth2.py` — `GET https://api.modrinth.com/v2/search?limit=1&facets=[["versions:<v>"],["categories:<loader>"],["project_type:mod|modpack"]]`, value = `total_hits`; the last block adds `["created_timestamp>=1780272000"]` (2026-06-01). These count projects that have any file for that version; they are not downloads or player counts.

Output (mods):

```
1.20.1 forge 25968
1.20.1 fabric 18252
1.20.1 neoforge 5281
1.21.1 neoforge 22773
1.21.1 fabric 20540
1.21.1 forge 5706
1.21.11 neoforge 8667
1.21.11 fabric 18834
26.1.2 neoforge 8987
26.1.2 fabric 14263
26.1.2 forge 3685
26.2 neoforge 7552
26.2 fabric 14516
26.3 neoforge 6107
26.3 fabric 10823
26.3 forge 2984
```

Output (modpacks, then mods first published since 2026-06-01):

```
modpacks
1.20.1 forge 3083
1.20.1 fabric 4320
1.21.1 neoforge 2328
1.21.1 fabric 3126
26.1.2 neoforge 75
26.1.2 fabric 1380
26.3 neoforge 29
26.3 fabric 1001
mods first published on Modrinth since 2026-06-01
1.20.1 forge 4023
1.20.1 fabric 2068
1.21.1 neoforge 5776
1.21.1 fabric 3668
26.1.2 neoforge 2435
26.1.2 fabric 4257
26.3 neoforge 1738
26.3 fabric 3508
```

### M11 — Scale of the TerraFirmaCraft 1.21 port

Command and output, as recorded:

```
M1: `gh api "repos/TerraFirmaCraft/TerraFirmaCraft/commits?sha=1.21.x&since=2024-07-01T00:00:00Z&until=2025-09-14T00:00:00Z&per_page=1" -i` gave Link rel=last page=806; `compare/1.20.x...1.21.x` gave ahead_by 2281, behind_by 266, merge base 2024-06-22; `/languages` Java 9126167.
```

### M12 — GregTech Modern: 1.21 branch against 1.20.1

Command and output, as recorded:

```
M2: `gh api repos/GregTechCEu/GregTech-Modern` default_branch 1.20.1; `compare/1.20.1...1.21` ahead 901, behind 248; `/languages` Java 11077142; release names "1.20.1-8.0.0 SNAPSHOT 5a6aff9", "1.21-8.0.0 SNAPSHOT 2918b17".
```

### M13 — Create: newest branches, latest release, code size

Command and output, as recorded (the full branch list is in M1):

```
M3: `gh api repos/Creators-of-Create/Create/branches` newest mc1.21.1/*; `/releases/latest` mc1.21.1-6.0.10 2026-04-21T22:20:00Z; `/languages` Java 9635922.
```

### M14 — Serene Seasons: code size

Command and output, as recorded (the per-version list is in M2):

```
M5: `python q4_modrinth.py serene-seasons`; `/languages` Java 145062.
```

### M15 — Licences as detected by GitHub

Command: `gh api repos/<owner>/<repo>/license --jq .license.spdx_id` per repository.

Output:

```
GregTechCEu/GregTech-Modern: LGPL-3.0
KubeJS-Mods/KubeJS: LGPL-3.0
AgriCraft/AgriCraft: MIT
Sinytra/Connector: MIT
mekanism/Mekanism: MIT
TerraFirmaCraft/TerraFirmaCraft: EUPL-1.2
Creators-of-Create/Create: NOASSERTION
Glitchfiend/SereneSeasons: NOASSERTION
FTBTeam/FTB-Quests: {"message":"Not Found","documentation_url":"https://docs.git
```

### M16 — Mekanism and AE2: first and last Modrinth version per game version; branch lists

Command: `python -I q4_modrinth.py ae2 mekanism`, filtered to 1.20.1–1.21.1; `gh api repos/<owner>/<repo>/branches` filtered to version-named branches (Create's branch list is in M1).

Output:

```
== ae2 https://api.modrinth.com/v2/project/ae2/version versions: 364
  1.20.1     forge     first 2023-06-14 (forge-15.0.1-alpha)  last 2026-10-28 (15.4.12)  n=59
  1.20.1     neoforge  first 2023-07-28 (15.0.6-beta)  last 2025-05-03 (15.3.6)  n=41
  1.20.2     neoforge  first 2023-11-27 (16.0.1-alpha)  last 2023-12-03 (16.0.2)  n=2
  1.20.4     neoforge  first 2023-12-24 (17.0.0-alpha)  last 2024-03-29 (17.13.0-beta)  n=37
  1.20.6     neoforge  first 2024-04-29 (18.1.0-alpha)  last 2024-05-28 (18.1.3-alpha)  n=4
  1.21       neoforge  first 2024-06-16 (19.0.2-alpha)  last 2024-08-03 (19.0.19-beta)  n=20
  1.21.1     neoforge  first 2024-08-09 (19.0.20-beta)  last 2026-11-18 (19.2.20)  n=33
== mekanism https://api.modrinth.com/v2/project/mekanism/version versions: 91
  1.20.1     forge     first 2023-09-30 (10.4.0.14)  last 2025-05-21 (10.4.16.80)  n=16
  1.20.1     neoforge  first 2023-09-30 (10.4.0.14)  last 2025-05-21 (10.4.16.80)  n=16
  1.20.4     neoforge  first 2024-03-01 (10.5.1.23)  last 2024-05-04 (10.5.20.41)  n=18
  1.21       neoforge  first 2024-06-13 (10.6.0.44)  last 2024-07-28 (10.6.7.54)  n=9
  1.21.1     neoforge  first 2024-08-20 (10.7.0.55)  last 2026-04-10 (10.7.19.85)  n=21
## Q4-M7 branch lists (looking for 1.20.2-1.20.6 branches)
TerraFirmaCraft/TerraFirmaCraft: 1.12.x, 1.16.x, 1.17.x, 1.18.x, 1.20.x, 1.21.x
GregTechCEu/GregTech-Modern: 1.19.2, 1.20.1, 1.21, 26.1
```

## Open questions

- Q1 — no new information — когда GregTech CEu Modern выпустит стабильный 8.0.0 и выйдет ли он под 1.21.1 одновременно с 1.20.1: заявления мейнтейнеров не найдено (обсуждение идёт в Discord). Решит: чтение анонсов в Discord GregTech CEu человеком либо появление релиза без пометки prerelease в `GregTechCEu/GregTech-Modern`.
- Q1 — no new information — явных заявлений о поддерживаемых версиях нет у TerraFirmaCraft, KubeJS, FTB Quests, Serene Seasons и Create; их статус выведен из дат релизов и коммитов. Решит: ответ мейнтейнеров в issue или Discord каждого мода.
- Q1 — blocked: CurseForge — существует ли неофициальная сборка AgriCraft под 1.21.1 (на неё намекает сторонний аддон совместимости). Решит: поиск на CurseForge вручную.
- Q2 — blocked: CurseForge — зрелость и активность сборок на 1.21.1 NeoForge известны плохо: Createch 2 с GregTech — только по поисковому сниппету, страница Absolute Industrialization прочитана при аудите, список её файлов — нет. Решит: чтение страниц CurseForge и modpackindex вручную (страница Gregnautics Continued на modpackindex при аудите отдала 404).
- Q2 — no new information — письменной политики версий GTNH не найдено; «остаются на 1.7.10» — вывод из README, страницы загрузок и lwjgl3ify. Решит: чтение вики или Discord GTNH человеком.
- Q3 — blocked: minecraft.net — собственные тексты Mojang о нумерации версий, снятии обфускации и датах выхода 26.1 и 26.2 не прочитаны. Решит: чтение minecraft.net вручную.
- Q3 — no new information — документация ForgeGradle 7 и его требования к Gradle и Java не найдены, хотя шаблон мода Forge 1.20.1 теперь требует именно его. Решит: пробная сборка пустого мода из этого шаблона.
- Q3 — no new information — NeoForge не называл дату, до которой будет выпускать сборки под 1.21.1. Решит: заявление команды NeoForge; до него — наблюдение за списком версий на maven.neoforged.net.
- Q4 — no new information — нет оценок в человеко-часах: цена порта показана только календарным временем, числом коммитов и строк. Решит: письменный разбор порта от команды мода либо собственный пробный порт небольшого мода с учётом времени.
- Следующий запуск, если GregTech войдёт в план: состояние снапшотов GregTech 8.0.0 на 1.21.1 (какие интеграции отключены, что сломано) и срок стабильного релиза.
- Следующий запуск, если выбран 1.20.1: какой инструмент сборки брать для нового мода на 1.20.1 в 2026 году — ForgeGradle 7 или ModDevGradle legacy — с пробной сборкой пустого мода.

## Sources

- [S1] api.modrinth.com — /v2/version/zM6ZmilV — https://api.modrinth.com/v2/version/zM6ZmilV — passage
- [S2] api.modrinth.com — /v2/version/h5kAR6P0 — https://api.modrinth.com/v2/version/h5kAR6P0 — passage
- [S3] api.modrinth.com — /v2/version/yLwvjuvy — https://api.modrinth.com/v2/version/yLwvjuvy — passage
- [S4] api.modrinth.com — /v2/version/iFPVZs0V — https://api.modrinth.com/v2/version/iFPVZs0V — passage
- [S5] api.github.com — /repos/GregTechCEu/GregTech-Modern/releases/tags/latest-1.21 — https://api.github.com/repos/GregTechCEu/GregTech-Modern/releases/tags/latest-1.21 — passage
- [S6] raw.githubusercontent.com — /GregTechCEu/GregTech-Modern/1.20.1/README.md — https://raw.githubusercontent.com/GregTechCEu/GregTech-Modern/1.20.1/README.md — passage
- [S7] api.modrinth.com — /v2/version/UjX6dr61 — https://api.modrinth.com/v2/version/UjX6dr61 — passage
- [S8] api.modrinth.com — /v2/version/8amzvn9x — https://api.modrinth.com/v2/version/8amzvn9x — passage
- [S9] api.modrinth.com — /v2/version/HAqwA6X1 — https://api.modrinth.com/v2/version/HAqwA6X1 — passage
- [S10] api.github.com — /repos/Fabricators-of-Create/Create/issues/comments/4618376774 — https://api.github.com/repos/Fabricators-of-Create/Create/issues/comments/4618376774 — passage
- [S11] api.modrinth.com — /v2/version/5yNEdyfc — https://api.modrinth.com/v2/version/5yNEdyfc — passage
- [S12] api.modrinth.com — /v2/version/GDXCpKlp — https://api.modrinth.com/v2/version/GDXCpKlp — passage
- [S13] api.modrinth.com — /v2/version/hVR2xUSr — https://api.modrinth.com/v2/version/hVR2xUSr — passage
- [S14] maven.ftb.dev — /releases/dev/ftb/mods/ftb-quests-forge/maven-metadata.xml — https://maven.ftb.dev/releases/dev/ftb/mods/ftb-quests-forge/maven-metadata.xml — passage
- [S15] maven.ftb.dev — /releases/dev/ftb/mods/ftb-quests-neoforge/maven-metadata.xml — https://maven.ftb.dev/releases/dev/ftb/mods/ftb-quests-neoforge/maven-metadata.xml — passage
- [S16] api.modrinth.com — /v2/version/qXNjK6M2 — https://api.modrinth.com/v2/version/qXNjK6M2 — passage
- [S17] api.modrinth.com — /v2/version/H977V6EU — https://api.modrinth.com/v2/version/H977V6EU — passage
- [S18] api.github.com — /repos/AgriCraft/AgriCraft/pulls/1710 — https://api.github.com/repos/AgriCraft/AgriCraft/pulls/1710 — passage
- [S19] api.modrinth.com — /v2/version/QnvW5HPm — https://api.modrinth.com/v2/version/QnvW5HPm — passage
- [S20] api.modrinth.com — /v2/version/pHEgQQUE — https://api.modrinth.com/v2/version/pHEgQQUE — passage
- [S21] api.modrinth.com — /v2/version/WMrhrybF — https://api.modrinth.com/v2/version/WMrhrybF — passage
- [S22] raw.githubusercontent.com — /TerraFirmaGreg-Team/Modpack-Modern/dev/pakku-lock.json — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/pakku-lock.json — passage
- [S23] api.github.com — /repos/TerraFirmaGreg-Team/Modpack-Modern/releases?per_page=3 — https://api.github.com/repos/TerraFirmaGreg-Team/Modpack-Modern/releases?per_page=3 — passage
- [S24] api.github.com — /repos/TerraFirmaGreg-Team/Modpack-Modern/issues/3659/comments — https://api.github.com/repos/TerraFirmaGreg-Team/Modpack-Modern/issues/3659/comments — passage
- [S25] api.github.com — /repos/TerraFirmaGreg-Team/Modpack-Modern/branches?per_page=100 — https://api.github.com/repos/TerraFirmaGreg-Team/Modpack-Modern/branches?per_page=100 — passage
- [S26] api.github.com — /repos/drainstar/Gregnautics-Continued-Test — https://api.github.com/repos/drainstar/Gregnautics-Continued-Test — passage
- [S27] www.modpackindex.com — /modpack/155522/gregnautics-continued — https://www.modpackindex.com/modpack/155522/gregnautics-continued — passage
- [S28] api.modrinth.com — /v2/project/gregtechceu-modern/version — https://api.modrinth.com/v2/project/gregtechceu-modern/version — passage
- [S29] api.github.com — /repos/GregTechCEu/GregTech-Modern/releases?per_page=6 — https://api.github.com/repos/GregTechCEu/GregTech-Modern/releases?per_page=6 — passage
- [S30] raw.githubusercontent.com — /GregTechCEu/GregTech-Modern/1.21/gradle/libs.versions.toml — https://raw.githubusercontent.com/GregTechCEu/GregTech-Modern/1.21/gradle/libs.versions.toml — passage
- [S31] api.github.com — /repos/GregTechCEu/GregTech-Modern/issues/5606 — https://api.github.com/repos/GregTechCEu/GregTech-Modern/issues/5606 — passage
- [S32] raw.githubusercontent.com — /GregTechCEu/GregTech-Modern-Community-Pack/main/README.md — https://raw.githubusercontent.com/GregTechCEu/GregTech-Modern-Community-Pack/main/README.md — passage
- [S33] raw.githubusercontent.com — /GregTechCEu/GregTech-Modern-Community-Pack/1.21/pack.toml — https://raw.githubusercontent.com/GregTechCEu/GregTech-Modern-Community-Pack/1.21/pack.toml — passage
- [S34] raw.githubusercontent.com — /GTNewHorizons/GT-New-Horizons-Modpack/master/README.md — https://raw.githubusercontent.com/GTNewHorizons/GT-New-Horizons-Modpack/master/README.md — passage
- [S35] www.gtnewhorizons.com — /downloads — https://www.gtnewhorizons.com/downloads — passage
- [S36] raw.githubusercontent.com — /GTNewHorizons/lwjgl3ify/master/README.MD — https://raw.githubusercontent.com/GTNewHorizons/lwjgl3ify/master/README.MD — passage
- [S37] raw.githubusercontent.com — /Omicron-Industries/Monifactory/main/manifest.json — https://raw.githubusercontent.com/Omicron-Industries/Monifactory/main/manifest.json — passage
- [S38] api.github.com — /repos/Omicron-Industries/Monifactory/releases?per_page=3 — https://api.github.com/repos/Omicron-Industries/Monifactory/releases?per_page=3 — passage
- [S39] raw.githubusercontent.com — /Omicron-Industries/Monifactory/main/FAQ.md — https://raw.githubusercontent.com/Omicron-Industries/Monifactory/main/FAQ.md — passage
- [S40] api.github.com — /repos/StarT-Dev-Team/Star-Technology — https://api.github.com/repos/StarT-Dev-Team/Star-Technology — passage
- [S41] api.github.com — /repos/StarT-Dev-Team/Star-Technology/releases?per_page=3 — https://api.github.com/repos/StarT-Dev-Team/Star-Technology/releases?per_page=3 — passage
- [S42] raw.githubusercontent.com — /Nomi-CEu/Nomi-CEu/main/manifest.json — https://raw.githubusercontent.com/Nomi-CEu/Nomi-CEu/main/manifest.json — passage
- [S43] raw.githubusercontent.com — /AllTheMods/ATM-10/main/README.md — https://raw.githubusercontent.com/AllTheMods/ATM-10/main/README.md — passage
- [S44] raw.githubusercontent.com — /AllTheMods/ATM-11/main/README.md — https://raw.githubusercontent.com/AllTheMods/ATM-11/main/README.md — passage
- [S45] raw.githubusercontent.com — /AllTheMods/atm9-expert/main/README.md — https://raw.githubusercontent.com/AllTheMods/atm9-expert/main/README.md — passage
- [S46] www.curseforge.com — /minecraft/modpacks/createch-2/files/8558561/dependencies — https://www.curseforge.com/minecraft/modpacks/createch-2/files/8558561/dependencies — passage
- [S47] www.curseforge.com — /minecraft/modpacks/absolute-industrialization/files/8412595 — https://www.curseforge.com/minecraft/modpacks/absolute-industrialization/files/8412595 — passage
- [S48] maven.neoforged.net — /releases/net/neoforged/neoforge/maven-metadata.xml — https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml — passage
- [S49] maven.neoforged.net — /releases/net/neoforged/forge/maven-metadata.xml — https://maven.neoforged.net/releases/net/neoforged/forge/maven-metadata.xml — passage
- [S50] files.minecraftforge.net — /net/minecraftforge/forge/promotions_slim.json — https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json — passage
- [S51] github.com — /MinecraftForge/MinecraftForge/commits/1.20.1.atom — https://github.com/MinecraftForge/MinecraftForge/commits/1.20.1.atom — passage
- [S52] forums.minecraftforge.net — /topic/125488-rules-and-frequently-asked-questions-faq/ — https://forums.minecraftforge.net/topic/125488-rules-and-frequently-asked-questions-faq/ — passage
- [S53] forums.minecraftforge.net — /topic/144690-new-tiered-support-policy — https://forums.minecraftforge.net/topic/144690-new-tiered-support-policy — passage
- [S54] meta.fabricmc.net — /v2/versions/game — https://meta.fabricmc.net/v2/versions/game — passage
- [S55] fabricmc.net — /2026/09/15/263.html — https://fabricmc.net/2026/09/15/263.html — passage
- [S56] neoforged.net — /news/moddevgradle2/ — https://neoforged.net/news/moddevgradle2/ — passage
- [S57] raw.githubusercontent.com — /MinecraftForge/MinecraftForge/1.20.1/mdk/build.gradle — https://raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.20.1/mdk/build.gradle — passage
- [S58] forums.minecraftforge.net — /topic/159800-mc-12111-build-failed-because-plugin-is-not-found/ — https://forums.minecraftforge.net/topic/159800-mc-12111-build-failed-because-plugin-is-not-found/ — passage
- [S59] neoforged.net — /news/20.5release/ — https://neoforged.net/news/20.5release/ — passage
- [S60] neoforged.net — /news/26.1release/ — https://neoforged.net/news/26.1release/ — passage
- [S61] fabricmc.net — /2026/03/14/261.html — https://fabricmc.net/2026/03/14/261.html — passage
- [S62] docs.neoforged.net — /docs/gettingstarted/versioning/ — https://docs.neoforged.net/docs/gettingstarted/versioning/ — passage
- [S63] neoforged.net — /news/2025-retrospection/ — https://neoforged.net/news/2025-retrospection/ — passage
- [S64] neoforged.net — /news/21.2release/ — https://neoforged.net/news/21.2release/ — passage
- [S65] neoforged.net — /news/21.0release/ — https://neoforged.net/news/21.0release/ — passage
- [S66] docs.minecraftforge.net — /en/latest/legacy/ — https://docs.minecraftforge.net/en/latest/legacy/ — passage
- [S67] raw.githubusercontent.com — /neoforged/.github/HEAD/primers/README.md — https://raw.githubusercontent.com/neoforged/.github/HEAD/primers/README.md — passage
- [S68] hackmd.io — /@neoforged/rJ_d8ZxLxl — https://hackmd.io/@neoforged/rJ_d8ZxLxl — passage
- [S69] neoforged.net — /news/mitigating-vulnerabilities-network/ — https://neoforged.net/news/mitigating-vulnerabilities-network/ — passage
- [S70] api.github.com — /repos/TerraFirmaCraft/TerraFirmaCraft/commits/f572a727050b6dd5508e22f9d4f8676a1b6d201b — https://api.github.com/repos/TerraFirmaCraft/TerraFirmaCraft/commits/f572a727050b6dd5508e22f9d4f8676a1b6d201b — passage
- [S71] api.github.com — /repos/TerraFirmaCraft/TerraFirmaCraft/releases/tags/v4.0.0-beta — https://api.github.com/repos/TerraFirmaCraft/TerraFirmaCraft/releases/tags/v4.0.0-beta — passage
- [S72] api.github.com — /repos/TerraFirmaCraft/TerraFirmaCraft/releases/tags/v4.1.0 — https://api.github.com/repos/TerraFirmaCraft/TerraFirmaCraft/releases/tags/v4.1.0 — passage
- [S73] api.github.com — /repos/GregTechCEu/GregTech-Modern/releases/tags/v7.0.2-1.21.1 — https://api.github.com/repos/GregTechCEu/GregTech-Modern/releases/tags/v7.0.2-1.21.1 — passage
- [S74] api.github.com — /repos/GregTechCEu/GregTech-Modern/releases/tags/v7.5.3-1.20.1 — https://api.github.com/repos/GregTechCEu/GregTech-Modern/releases/tags/v7.5.3-1.20.1 — passage
- [S75] api.github.com — /repos/GregTechCEu/GregTech-Modern/issues/5607 — https://api.github.com/repos/GregTechCEu/GregTech-Modern/issues/5607 — passage
- [S76] api.github.com — /repos/GregTechCEu/GregTech-Modern/pulls/4239 — https://api.github.com/repos/GregTechCEu/GregTech-Modern/pulls/4239 — passage
- [S77] api.modrinth.com — /v2/project/create/version — https://api.modrinth.com/v2/project/create/version — passage
- [S78] raw.githubusercontent.com — /AgriCraft/AgriCraft/1.21/gradle.properties — https://raw.githubusercontent.com/AgriCraft/AgriCraft/1.21/gradle.properties — passage
- [S79] api.modrinth.com — /v2/project/serene-seasons/version — https://api.modrinth.com/v2/project/serene-seasons/version — passage
- [S80] neoforged.net — /news/20.2release/ — https://neoforged.net/news/20.2release/ — passage
- [S81] neoforged.net — /news/20.3capability-rework/ — https://neoforged.net/news/20.3capability-rework/ — passage
- [S82] neoforged.net — /news/20.4networking-rework/ — https://neoforged.net/news/20.4networking-rework/ — passage
- [S83] neoforged.net — /news/21.9-transfer-rework/ — https://neoforged.net/news/21.9-transfer-rework/ — passage
- [S84] raw.githubusercontent.com — /TerraFirmaCraft/TerraFirmaCraft/1.21.x/README.md — https://raw.githubusercontent.com/TerraFirmaCraft/TerraFirmaCraft/1.21.x/README.md — passage
- [S85] raw.githubusercontent.com — /Creators-of-Create/Create/mc1.21.1/dev/LICENSE.md — https://raw.githubusercontent.com/Creators-of-Create/Create/mc1.21.1/dev/LICENSE.md — passage
- [S86] raw.githubusercontent.com — /Glitchfiend/SereneSeasons/26.3/LICENSE — https://raw.githubusercontent.com/Glitchfiend/SereneSeasons/26.3/LICENSE — passage
- [S87] raw.githubusercontent.com — /FTBTeam/FTB-Quests/main/neoforge/src/main/resources/META-INF/neoforge.mods.toml — https://raw.githubusercontent.com/FTBTeam/FTB-Quests/main/neoforge/src/main/resources/META-INF/neoforge.mods.toml — passage
- [S88] raw.githubusercontent.com — /Sinytra/Connector/26.2.x/README.md — https://raw.githubusercontent.com/Sinytra/Connector/26.2.x/README.md — passage
- [S89] api.modrinth.com — /v2/project/retromod — https://api.modrinth.com/v2/project/retromod — passage
- [S90] bownlux.github.io — /Retromod/incompatible-mods — https://bownlux.github.io/Retromod/incompatible-mods — passage
- [S91] api.github.com — /repos/vlad250906/Create-UfoPort — https://api.github.com/repos/vlad250906/Create-UfoPort — passage
- [S92] api.modrinth.com — /v2/project/mekanism/version — https://api.modrinth.com/v2/project/mekanism/version — passage
- [S93] api.modrinth.com — /v2/project/ae2/version — https://api.modrinth.com/v2/project/ae2/version — passage

## Audit

| Claim | Verdict | Note |
|---|---|---|
| C101 | SUPPORTED |  |
| C102 | SUPPORTED |  |
| C103 | SUPPORTED |  |
| C105 | SUPPORTED |  |
| C107 | SUPPORTED |  |
| C108 | PARTIAL | Modrinth lists older 1.21 builds too (7.0.1, 7.0.0, 1.4.x, 1.3.x); "only" changed to "newest" |
| C109 | SUPPORTED | quote check NOT FOUND kept: the checker fetches minified JSON (no space after the colon); the field and value are in the API response read at audit |
| C110 | SUPPORTED |  |
| C111 | SUPPORTED |  |
| C113 | SUPPORTED |  |
| C114 | SUPPORTED |  |
| C115 | SUPPORTED |  |
| C116 | SUPPORTED |  |
| C117 | SUPPORTED |  |
| C118 | SUPPORTED | absence of 1.21 re-checked at audit: api.modrinth.com create-fabric has 67 versions, game versions 1.18.2, 1.19.2, 1.20.1 only |
| C119 | SUPPORTED |  |
| C121 | SUPPORTED |  |
| C122 | SUPPORTED |  |
| C123 | SUPPORTED |  |
| C124 | SUPPORTED |  |
| C125 | SUPPORTED |  |
| C126 | SUPPORTED |  |
| C127 | SUPPORTED |  |
| C128 | SUPPORTED |  |
| C129 | SUPPORTED |  |
| C130 | SUPPORTED |  |
| C131 | SUPPORTED |  |
| C133 | SUPPORTED | quote check NOT FOUND kept: the checker fetches minified JSON (no space after the colon); the field and value are in the API response read at audit |
| C134 | SUPPORTED |  |
| C135 | SUPPORTED |  |
| C136 | SUPPORTED |  |
| C137 | SUPPORTED |  |
| C138 | PARTIAL | recorded output has no 1.21.2 or 26.1 builds; "every version" replaced by the versions listed |
| C140 | PARTIAL | "only version" also needs C105 and C130, now named; "daily" snapshots not shown, changed to "rolling" |
| C141 | PARTIAL | used Serene Seasons 1.21.1 (C136) and GTCEu Fabric dates (M2) without naming them; basis amended; "only an old beta" reworded after C108 |
| C202 | SUPPORTED |  |
| C203 | SUPPORTED |  |
| C205 | SUPPORTED |  |
| C206 | SUPPORTED |  |
| C207 | SUPPORTED |  |
| C208 | SUPPORTED | commit date is in M3, confirmed at audit (gtm8 head 2026-11-22); quote check NOT FOUND kept: the checker fetches minified JSON (no space after the colon); the field and value are in the API response read at audit |
| C209 | PARTIAL | "preparing a move to GregTech Modern 8" read from a branch name; cited C222 is about another pack; narrowed, basis changed to C216 |
| C211 | SUPPORTED |  |
| C212 | UNCHECKABLE | modpackindex page returns "Page not found" (404) at audit |
| C214 | SUPPORTED |  |
| C215 | SUPPORTED |  |
| C216 | SUPPORTED | quote check NOT FOUND kept: the checker fetches minified JSON (no space after the colon); the field and value are in the API response read at audit |
| C217 | SUPPORTED |  |
| C218 | SUPPORTED |  |
| C219 | SUPPORTED |  |
| C220 | SUPPORTED |  |
| C221 | SUPPORTED | release v1.14.5 2026-02-09 is not in the README; confirmed via the repo's releases API |
| C222 | SUPPORTED | last-commit date 2026-10-13 is not in pack.toml; confirmed via the commits API |
| C223 | SUPPORTED |  |
| C225 | SUPPORTED |  |
| C227 | SUPPORTED | release 3.1.0 2026-11-04 is not in the README; confirmed via the releases API |
| C228 | SUPPORTED |  |
| C229 | SUPPORTED |  |
| C230 | SUPPORTED |  |
| C231 | SUPPORTED |  |
| C232 | SUPPORTED |  |
| C233 | SUPPORTED |  |
| C234 | SUPPORTED |  |
| C235 | SUPPORTED |  |
| C236 | SUPPORTED |  |
| C237 | SUPPORTED | release 1.7.7 and main head 2026-10-15 are not in manifest.json; confirmed via the GitHub API |
| C238 | SUPPORTED | changelog date read at audit in ATM-10 main CHANGELOG.md: [8.6] 2026-11-14 |
| C239 | SUPPORTED | repo dates are not in the README; confirmed via the repo API |
| C240 | PARTIAL | README says the pack began as a 9-month solo fan project; "started a new pack in late 2026" narrowed to the repository date |
| C242 | UNCHECKABLE | CurseForge answers with a bot check ("Just a moment...") |
| C243 | PARTIAL | page read at audit: it supports Absolute Industrialization only; Entropy Zero and "non-GregTech" removed; label changed to quoted with the page's passage; quote check unfetchable for the checker |
| C244 | PARTIAL | "none has a released 1.21.1 version" rests on C207, C222, C236, now named, and is limited to the checked repositories |
| C245 | PARTIAL | the Createch 2 part rests on UNCHECKABLE C242; marked as snippet-only in the claim |
| C246 | PARTIAL | no claim checks Nomi-CEu for port statements; Nomi-CEu removed from the "silence" list |
| C247 | SUPPORTED |  |
| C301 | SUPPORTED |  |
| C302 | SUPPORTED |  |
| C303 | SUPPORTED | re-checked at audit in the maven list: entries after index 1829 are 21.1, 26.1.2, 26.2.0, 26.3.0 only |
| C304 | SUPPORTED |  |
| C305 | SUPPORTED |  |
| C306 | SUPPORTED |  |
| C307 | SUPPORTED |  |
| C308 | SUPPORTED |  |
| C310 | SUPPORTED |  |
| C311 | SUPPORTED |  |
| C312 | SUPPORTED |  |
| C313 | SUPPORTED |  |
| C314 | PARTIAL | no named claim dates 26.1 to March or 26.2 to June; narrowed to 26.3, the 26.4 snapshots and the quarterly cadence |
| C315 | SUPPORTED |  |
| C316 | SUPPORTED |  |
| C320 | SUPPORTED |  |
| C321 | SUPPORTED |  |
| C322 | SUPPORTED |  |
| C324 | SUPPORTED |  |
| C325 | SUPPORTED |  |
| C326 | SUPPORTED |  |
| C327 | SUPPORTED |  |
| C328 | SUPPORTED |  |
| C330 | SUPPORTED |  |
| C331 | SUPPORTED |  |
| C332 | SUPPORTED |  |
| C333 | SUPPORTED |  |
| C334 | PARTIAL | source says breaking changes are "still allowed" under -beta; "only" removed |
| C335 | SUPPORTED |  |
| C337 | PARTIAL | "the largest single break" is not in the source; removed |
| C338 | SUPPORTED |  |
| C340 | SUPPORTED |  |
| C343 | SUPPORTED |  |
| C344 | SUPPORTED |  |
| C346 | SUPPORTED |  |
| C347 | SUPPORTED |  |
| C348 | SUPPORTED |  |
| C350 | SUPPORTED |  |
| C351 | SUPPORTED |  |
| C354 | SUPPORTED |  |
| C355 | SUPPORTED |  |
| C356 | SUPPORTED |  |
| C357 | PARTIAL | one maintainer's answer (TG) was given as the maintainers' position; another (Orion) answers "Yes" in the same passage; rewritten |
| C358 | SUPPORTED |  |
| C359 | SUPPORTED |  |
| C360 | SUPPORTED |  |
| C361 | SUPPORTED |  |
| C362 | PARTIAL | "de-facto modded LTS" is not in the counts; restated as catalogue sizes |
| C401 | SUPPORTED |  |
| C402 | SUPPORTED |  |
| C403 | SUPPORTED |  |
| C404 | PARTIAL | note does not say "remaining maintainer" or "lead developer"; reworded to its own terms |
| C405 | SUPPORTED |  |
| C406 | SUPPORTED |  |
| C407 | SUPPORTED |  |
| C409 | SUPPORTED |  |
| C410 | SUPPORTED |  |
| C411 | PARTIAL | an issue title naming a snapshot build does not show the line "existed only as" snapshots; narrowed |
| C412 | SUPPORTED |  |
| C416 | SUPPORTED | merge-base date is not in the recorded M12 output; re-run at audit: 7a55019b, 2026-07-12, ahead 901, behind 248 |
| C417 | PARTIAL | one sync PR is cited, not "repeated" ones; "multi-person team" unsourced; narrowed |
| C418 | SUPPORTED |  |
| C420 | SUPPORTED |  |
| C421 | SUPPORTED |  |
| C423 | SUPPORTED |  |
| C426 | SUPPORTED |  |
| C427 | PARTIAL | not every release has a build (none for 1.21.2, 26.1); "every" changed to "most" |
| C428 | SUPPORTED |  |
| C429 | SUPPORTED |  |
| C430 | SUPPORTED |  |
| C431 | SUPPORTED |  |
| C434 | SUPPORTED |  |
| C437 | SUPPORTED |  |
| C440 | SUPPORTED |  |
| C441 | SUPPORTED |  |
| C442 | SUPPORTED | quote check unfetchable for the checker; LICENSE text read at audit from the fetched copy |
| C443 | SUPPORTED |  |
| C444 | SUPPORTED |  |
| C445 | SUPPORTED |  |
| C446 | SUPPORTED | release details are not in the README; confirmed via the releases API (all prerelease; 2.0.0-beta.17+1.21.1 on 2026-08-15) |
| C447 | PARTIAL | the Forge-on-NeoForge part is in S90, not S89; citation completed. 59 stars confirmed via the repo API |
| C448 | SUPPORTED |  |
| C449 | SUPPORTED |  |
| C453 | SUPPORTED |  |
| C454 | SUPPORTED | Java size is not in M15/M16; confirmed via the languages API (12,381,553 bytes) |
| C455 | SUPPORTED |  |
| C456 | SUPPORTED |  |
| C457 | PARTIAL | generalisation from five mods; scoped to them |
| C458 | SUPPORTED |  |

Label verdict: answered — the Summary names one platform per stated condition (1.21.1 NeoForge unless GregTech must be in the first playable release, then 1.20.1 Forge), every claim it cites is SUPPORTED or repaired, and the open items (GregTech 8.0.0 date, CurseForge packs, Mojang's own texts) are either the Summary's own stated condition or would not move it.

Quote check: platform-version-2026-11-23.md: 116 quoted claim(s): 110 found, 4 NOT FOUND, 2 unfetchable, 0 no source

## Disposition

| Recommendation | State | Where |
|---|---|---|
| Minecraft 1.21.1 + NeoForge — если GregTech не обязан быть в сборке с первого играбельного релиза | landed | журнал решений проекта (`decisions.md` в каталоге памяти проекта, запись 2026-11-23 «Платформа: Minecraft 1.21.1 + NeoForge»): пользователь выбрал этот вариант 2026-11-23; каталога `docs/decisions/` в репозитории нет, поэтому носитель — журнал |
| Minecraft 1.20.1 + Forge — если GregTech обязан быть в сборке с первого играбельного релиза, либо проект стартует с кода TerraFirmaGreg-Modern или с AgriCraft как есть | dropped | пользователь решил начинать без GregTech (2026-11-23), условие не выполняется; отвергнутый вариант записан в той же записи журнала |
| Версии 26.x и Fabric не брать; к 26.x возвращаться только когда там появятся релизы TerraFirmaCraft и Create | landed | та же запись журнала решений от 2026-11-23: 26.x и Fabric перечислены как отвергнутые |
