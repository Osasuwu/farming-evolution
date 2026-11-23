---
topic: terrafirmagreg-process-reception
tags: area:modpack-architecture, research
source_provenance: /research re-run ordered by the farming-evolution planning session on 2026-11-23, follow-up to docs/research/terrafirmagreg-anatomy-2026-11-23.md
status: answered
audit: 2026-11-23 — 187 claims: 147 SUPPORTED, 38 PARTIAL, 1 UNSUPPORTED, 1 UNCHECKABLE
---

## Question

Дозапуск исследования по TerraFirmaGreg: пользователь решил закрыть два неотвеченных подвопроса сейчас. Сделай следующий прогон /research по подвопросам (5) процесс команды и (6) жалобы игроков из Open questions отчёта docs/research/terrafirmagreg-anatomy-2026-11-23.md и обнови отчёт (тот же файл или новый рядом, как требует скилл), чтобы он проходил check_artifact.py --gate. Что изменилось с первого прогона: платформа — Minecraft 1.21.1 + NeoForge, хребет технологий — Create, первый релиз без GregTech; выбор между зависимостью от TFC и своей моделью климата отложен до этапа мира; первым строится конвейер проверок на пустом моде (сборка, юнит-тесты, GameTest, загрузка сервера с разбором лога), затем вертикальный срез. В подвопросе (5) главное — что у команды TFG проверяется автоматически: CI, сборка релизов, тесты, проверки скриптов KubeJS, как ловят сломанные рецепты и недостижимую прогрессию. В подвопросе (6) — на что игроки жалуются в прогрессии и в био-части (еда, порча, животные, земледелие), и что хвалят. Ничего не коммить.

Подвопросы (5) и (6) в отчёте `terrafirmagreg-anatomy-2026-11-23.md` звучали так: «(5) процесс: сборка релизов, тесты, квесты, локализация, сколько людей и времени; (6) типичные жалобы игроков».

Asked «сколько людей и времени», answering «сколько участников, коммитов, релизов и лет видно в репозиториях», because the team publishes no statement of its size or of time spent, and person-hours cannot be derived from a commit history.

Asked «типичные жалобы игроков» и «что хвалят», answering «какие жалобы и похвалы записаны в источниках, которые можно прочитать (GitHub issues обоих проектов, отзывы на Moddex, журналы изменений команды, описания аддонов на Modrinth), и на какие из жалоб команда ответила изменениями», because Discord, Reddit, YouTube and CurseForge comments cannot be read by this run's tools, and the readable sources give single voices and the team's reactions, not a measured distribution of opinion.

This report adds to `docs/research/terrafirmagreg-anatomy-2026-11-23.md` (which covers layers, climate and worldgen, the TFC–GregTech coupling and vanilla blocking) the two sub-questions that report left under Open questions. It overturns nothing in it. That report was a lead here; every claim below was fetched in this run.

Sub-questions: Q1 что в разработке TFG проверяется автоматически и что нет; Q2 как делаются квесты и локализация и сколько участников и времени видно в репозиториях; Q3 на что жалуются и что хвалят в прогрессии; Q4 на что жалуются и что хвалят в био-части (еда, порча, жажда, животные, земледелие, сезоны).

## Summary

**Процесс (подвопрос 5).**

Автоматически у TFG проверяется только то, что сборка собирается и игра загружается. Общий репозиторий Actions собирает сборку через pakku и выпускает архивы для CurseForge, Modrinth, MultiMC, сервера и docker [C104]. Дымовой тест — запуск клиента и сервера без экрана через HeadlessMC на сторонних действиях `mc-runtime-test` и `mc-server-test` [C113], [C114], [C115]. Он включается только когда версия в журнале изменений ещё не имеет тега, то есть на кандидате в релиз [C122], [C123]: в 60 последних запусках Build задача Project Test пропущена в 43, прошла в 9, упала в 3 [C124]. Скрипт разбора лога считает строки с ошибкой разбора рецепта, но по собственному комментарию только сообщает находки и на исход не влияет [C117], [C119]. На ветке `main` обязательны три проверки (Info, Build, Test) [C125], на `dev` обязательных проверок нет, нужен один одобряющий отзыв [C126].

В Java-моде Core-Modern CI выполняет `gradle build` с исключёнными задачами spotless [C110]. Конфигурация `gameTestServer` есть [C127], но под `src/test` один файл-пример, первый тест которого утверждает константу `true` [C128], [C129], и workflow GameTest не запускает [C130].

Скрипты KubeJS автоматически не проверяются: скрипт `lint` с ESLint в `package.json` есть [C133], `tsconfig.json` поставляется с `noCheck: true` [C135], ни один workflow и ни один git-хук линтер не запускает [C137], каталогов с тестами в дереве сборки нет [C138].

Сломанные рецепты и недостижимую прогрессию ловят вручную и по сообщениям игроков. Перед релизом публикующий по инструкции открывает тестовый мир, смотрит `/kjs errors server_scripts` и ищет рецепты с `minecraft:barrier` — признак пустого тега [C148]; серверную сборку запускает человек с Linux-сервером [C149], и этот шаг разрешено пропустить для срочного выпуска [C150]. Блокирующей автоматической проверки рецептов или достижимости прогрессии в прочитанных файлах не найдено [C163]. В трекере 1867 issues типа Bug, 268 со словом recipe в заголовке [C156]; рецепты с пустыми тегами попали в релиз 0.13.19 (сообщил контрибьютор), и член команды ответил, что это проверяют перед релизом [C157], [C158]; о рецептах, пропавших на свежем выделенном сервере 0.13.15, тоже сообщил контрибьютор — по обсуждению они работали, но не показывались в EMI, помогли старые версии трёх сторонних модов, issue закрыта с меткой «Not Our Issue» [C159].

Квесты: 1085 объектов квестов в 17 файлах глав FTB Quests [C204]; в SNBT лежит ключ, текст — в lang-файлах по главам [C202], [C203], около 82 800 слов английского текста [C205]. README благодарит GregTech Community Pack Modern «for most of their quest text» [C206]. В дереве вики нет страницы, в пути которой названы квесты, локализация или перевод; из 13 страниц для разработчиков полностью прочитана одна [C212]. QuestBook-Modern — конвейер публикации книги квестов на сайт; проверки квестов в его описанном составе нет [C222].

Локализация идёт через Crowdin: источник — `en_us.json` каждого пространства имён [C224], синхронизация по расписанию [C226] с автоматическим слиянием PR переводов [C227]; 228 слитых PR с меткой перевода [C228], 19 локалей с покрытием от 130 из 132 файлов до 8 [C229].

Люди и время: первый коммит Modpack-Modern — 2022-09-13, 9407 коммитов в `dev` [C234]; 148 релизов с 2023-11-12, последние два года примерно раз в неделю [C236]; у Core-Modern 2469 коммитов с 2023-07-21 и 107 релизов [C235], [C237]. GitHub показывает 162 участника Modpack-Modern и 51 у Core-Modern [C238], но работа сосредоточена: один человек сделал 51,5 % всех коммитов сборки, пятеро — 65,3 % [C239]; за последние 12 месяцев — 3818 коммитов от 111 авторов, 62,8 % от одного [C242]. Заявления команды о своём размере, ролях или затратах времени не найдено [C247]; человеко-часы из этих чисел не выводятся.

Правила участия не разрешают использовать LLM во вкладах в сборку, мод ядра и переводы; тот же абзац допускает помощь LLM в диагностике проблем и вычитке переводов и требует, чтобы код был минимум на 90 % написан автором [C155].

**Приём игроками (подвопрос 6).**

Мера «типичности» ограничена: трекер TFG — в основном баги и вылеты [C303], слов о темпе в нём почти нет [C302], а сопровождающий TFC прямо пишет, что трекер принимает только баги и отправляет предложения в Discord [C403]. Ниже — отдельные голоса и то, на что команда ответила.

Прогрессия. Самая поддержанная реакциями просьба в трекере TFG — альтернативный рецепт каолинита [C305]. Жалоба — ранний ключевой ресурс зависит от удачи спавна и дальних походов: каолин, гарниерит [C307], [C308], [C309], [C312], [C314]. Команда на неё ответила: приняла альтернативу [C311], сделала гарниерит чаще [C313], в 0.12.0 подняла ранние руды к поверхности [C316], а член команды сформулировал, что механики удачи спавна не доставляют удовольствия [C317]. Вторая группа — гринд и ручная рутина: два автора отзывов на Moddex называют часть TFC очень гриндовой, оба при этом в положительном отзыве [C321], [C322]. Третья — обучение: ранние квесты мало объясняют [C333], игроку пришлось искать вне игры, что и когда автоматизировать [C335]; команда не хочет превращать книгу квестов в линейный чеклист [C336], перестроила главу квестов LV в 0.11.19 [C338], в 0.13.11 разделила сведения между полевым справочником и книгой квестов [C339], и удаление справочных глав вызвало возражение игрока [C340]. Четвёртая — стабильность: самые обсуждаемые issues — баги стабильности и производительности [C304]. Хвалят ощущение заработанного прогресса от старта с нуля и передачу от TFC к GregTech [C344], [C345]; оценка на Moddex 4,6 из 5 по 15 оценкам [C348]. Два читаемых случая остановки приходятся на стадию GregTech [C357]. Поиск «too easy» в трекере даёт 9 issues; что среди них нет просьб сделать раннюю игру медленнее или сложнее — оценка сборщика, в записи вывода только номера [C355].

Био-часть. Занесённые жалобы из читаемых источников касаются цены обслуживания [C448]:

- порча еды на постоянно работающем сервере: еда гниёт, пока игрок не в сети [C404]; сопровождающий TFC объяснил, что TFC не тикает предметы и не ищет их в мире ради порчи, поэтому при таком устройстве сделать ничего нельзя, и в 2022 году подтвердил, что задача для мультиплеера остаётся нерешённой [C405], [C406]; TFG ставит на серверах замедление порчи (0,25, затем 0,5), в одиночной игре оставляя 1,0 [C416];
- поддержание сбалансированного питания: один серверный игрок назвал его раздражающим даже при замедленной порче [C418], другой — весь пищевой цикл надоедающим со временем [C420];
- животные: по словам игрока, производство нельзя автоматизировать полностью — животные изнашиваются и стадо рушится [C433]; контрибьютор команды отклонил машину-разводчик из-за производительности [C434]; аддон TFC Grooming Station, снимающий уход за приручённостью, скачан 53 694 раза при 764 287 у самого TFC на Modrinth [C412], [C413];
- ручные шаги земледелия: обмолот больших урожаев до LV — в ответе член команды указал, что жернова автоматизируются механической энергией [C441], сбор фруктов в больших садах [C442];
- жажда в поздней игре [C428] и зима для новичка на общем сервере [C429], [C430].

Сопровождающие и участники команд обоих проектов защищают системы как задуманные и отвечают инструментами — множителями в конфиге, новыми видами еды, ранчером, изменениями теплицы, — а не удалением механик [C449]. Член команды TFG пишет, что команда вложила много сил в баланс еды, а игроки его игнорируют и делают одно блюдо [C424]. Прямой похвалы порче, питанию или сезонам в читаемых источниках не найдено; встречные голоса — игроки, просящие больше глубины: «непортящаяся еда должна быть достижением» [C422], просьба о генетике растений, которую член команды отклонил как лишнюю сложность [C440].

**Рекомендации для farming-evolution.**

1. В конвейере проверок запускать загрузку сервера на каждое изменение и делать разбор лога блокирующим. У TFG дымовой тест пропущен в 43 из 60 запусков, а разбор лога только сообщает [C117], [C124].
2. Сделать автоматическими проверки, которые TFG выполняет руками: ошибки разбора рецептов, рецепты с пустыми тегами, ошибки скриптов KubeJS. Рецепты с пустыми тегами у TFG попали в релиз при ручной проверке [C148], [C157].
3. Запускать линтер и проверку типов скриптов KubeJS в CI: у TFG конфигурация есть, запуска нет [C133], [C135], [C137].
4. Достижимость прогрессии проверять своим тестом: такой проверки в прочитанных файлах TFG не найдено [C163].
5. Не ставить ранний ключевой ресурс в зависимость от удачи спавна и дальних походов; климатическому уникуму дать второй путь получения. На эту жалобу команда TFG ответила альтернативным рецептом, изменением редкости гарниерита, мелкими ранними жилами и высказанным мнением, что механики удачи спавна не доставляют удовольствия [C356].
6. Порчу и уход за животными проектировать вместе с путём их автоматизации и с расчётом на сервер, который работает без игрока: на этом сосредоточены читаемые жалобы по био-части [C448].

## Findings

### Q1 — что в разработке TFG проверяется автоматически и что нет — answered with a gap

- **C101** [measured] The workflow directories hold: Modpack-Modern automerge.yml, build.yml, crowdin.yml, giscus-rename.yml, server.yml; Core-Modern build.yml; Actions release.yml; Modpack-Template build.yml; Mod-Template build.yml — Own measurements, M101
- **C102** [measured] In the six workflow files of Modpack-Modern and Core-Modern a grep for pull_request trigger keys matches only automerge.yml; build.yml of both repos has no pull_request trigger — Own measurements, M102
- **C103** [measured] One Modpack-Modern Build run on a dev push (run 45073940784, 2026-11-22) had nine jobs: Project Info and Project Build and Route succeeded; Create Release PR, Project Release, Project Test, Game Export, Discord Message and Wiki Changelog were skipped — Own measurements, M103
- **C104** [quoted] The shared Actions repo describes its modpack build action as placeholder substitution plus pakku export plus artifacts for CurseForge, Modrinth, MultiMC, server pack and docker — "placeholders + `pakku export` + artifacts (cf/mr/multimc/serverpack/docker)" — [S1], table row `build`
- **C105** [quoted] The Crowdin Translations workflow of Modpack-Modern has a cron schedule — "- cron: '0 3 */2 * *'" — [S2], line 19
- **C106** [quoted] The Automerge workflow job runs for pull requests whose head branch is dev (the condition continues with base main, non-draft, same repo) — "(github.event.pull_request.head.ref == 'dev' &&" — [S3], line 31
- **C109** [quoted] The Actions repo's own release.yml creates a GitHub release on push to main — "uses: softprops/action-gh-release@v3.0.3" — [S4], line 54
- **C110** [quoted] Core-Modern's build job runs Gradle build with the spotless tasks excluded — "gradle_args: build -x spotlessCheck -x spotlessApply -x spotlessJava -x spotlessJson" — [S5], line 52
- **C112** [quoted] In Modpack-Template the release job depends on the test job — "needs: [info, build, test]" — [S6], line 95, job release
- **C113** [quoted] The shared modpack test action describes itself as HeadlessMC client and server smoke tests — "HeadlessMC client + server smoke tests; summary, logs, PR comment on failure" — [S7], line 2, description
- **C114** [quoted] The client test step uses the third-party action headlesshq/mc-runtime-test — "uses: headlesshq/mc-runtime-test@5.0.0" — [S7], line 90, step Client test
- **C115** [quoted] The server test step uses the third-party action headlesshq/mc-server-test — "uses: headlesshq/mc-server-test@2.0.0" — [S7], line 115, step Server test
- **C117** [quoted] The log-analysis script states that it only reports findings and that pass or fail comes from the test steps — "# Findings only — pass/fail comes from the test step outcomes." — [S8], line 4
- **C119** [quoted] The log-analysis script counts lines with 'Parsing error loading recipe' in latest.log — "rec=$(grep -c 'Parsing error loading recipe' $dir/logs/latest.log 2>/dev/null || true)" — [S8], line 34
- **C122** [quoted] The route action sets the test flag to true only when the ref is not main, the build succeeded and make_pr is true — "flag test      "$([ "$REF" != "main" ] && [ "$BUILD" = "success" ] && [ "$MAKE_PR" = "true" ] && echo true || echo false)"" — [S9], line 50
- **C123** [quoted] The info action sets make_pr to true when the changelog version's tag does not exist and the run is on a non-stable branch — "make_pr=$([ "$TAG_EXISTS" = "false" ] && [ "$dev_env" = "true" ] && echo true || echo false)" — [S10], step Collect metadata
- **C124** [measured] In the 60 most recent Build runs of Modpack-Modern the Project Test job was skipped in 43, succeeded in 9, failed in 3, was cancelled in 3, and was absent in 2 — Own measurements, M104
- **C125** [measured] The branch rules for main of Modpack-Modern list three required status checks: Project Info, Project Build and Project Test — Own measurements, M105
- **C126** [measured] The branch rules for dev of Modpack-Modern contain no required_status_checks rule; the pull_request rule requires 1 approving review — Own measurements, M106
- **C127** [quoted] Core-Modern's build.gradle defines a gameTestServer run configuration — "type = "gameTestServer"" — [S11], line 193, runs block
- **C128** [measured] A tree listing of Core-Modern dev shows exactly one .java file under src/test: gametest/example/ExampleTest.java — Own measurements, M107
- **C129** [quoted] The first test in ExampleTest.java asserts a constant true — "helper.assertTrue(true, "true is false");" — [S12], line 21
- **C130** [measured] Core-Modern's build.yml contains no occurrence of gameTestServer, runGameTestServer or runData; no file in the Actions repo contains eslint, prettier, npm run, runGameTestServer, runData, spotlessCheck or checkstyle — Own measurements, M108
- **C133** [quoted] kubejs/package.json defines an npm lint script that runs ESLint — ""lint": "eslint --config ./eslint.config.js"," — [S13], scripts
- **C135** [quoted] kubejs/tsconfig.json ships with type checking disabled (noCheck true) — ""noCheck": true," — [S14], compilerOptions, under comment "Type checking options, only disable if probeJS installed"
- **C136** [quoted] The contribution guide tells contributors not to run prettier or lint over a whole file — "Note: Do not run prettier or lint over an entire file. Or it will become difficult to review and will likely be rejected." — [S15], Linting & TypeScript support
- **C137** [measured] None of the five Modpack-Modern workflow files contains eslint, prettier, 'npm ', lint or tsc; the Modpack-Modern dev tree has no .husky, .pre-commit-config.yaml, lefthook.yml, .lintstagedrc or .githooks path — Own measurements, M109
- **C138** [measured] The Modpack-Modern dev tree has no path with a test, tests, __tests__ or spec directory segment and no .test.js or .spec.js file — Own measurements, M110
- **C139** [quoted] The publishing guide says changing the topmost changelog header away from Unreleased starts the modpack publishing process — "Until everything else is ready, keep the topmost header as `## Unreleased`, as changing this will start the modpack publishing process." — [S16], step 4
- **C140** [quoted] After the push to dev the build script opens a pull request — "Wait a little bit and the build script will create a pull request." — [S16], step 9
- **C141** [quoted] Publishing begins when that maintainer approves the pull request — "Whenever he approves it, the publishing process begins." — [S16], step 9
- **C143** [quoted] The modpack release action publishes through the action Xikaro/game-publish — "uses: Xikaro/game-publish@main" — [S17], step Publish
- **C144** [measured] Modpack-Modern's build.yml passes a CurseForge id to the release action and never mentions modrinth_id or enable_modrinth; the only modrinth line is the .mrpack entry in the release file list — Own measurements, M111
- **C145** [quoted] For Core-Modern the guide says a version bump pushed to dev is published automatically to CurseForge and Modrinth — "Simply open `gradle.properties` and change the version number of `mod_version`, then push to `dev`. The build scripts will automatically publish a new release to curseforge and modrinth." — [S16], step 3
- **C146** [quoted] AutoPack-Director's README describes it as a tool for mods that cannot be packed and are downloaded at runtime — "but can be downloaded at runtime either due to not being present on your target distribution platform," — [S18], intro paragraph
- **C147** [measured] The Modpack-Modern dev tree has 11 paths under config/mod-director/, 10 of them *.curse.json files — Own measurements, M112
- **C148** [quoted] The publishing guide's pre-release check is a manual in-game list: among them the KubeJS error command and barrier-block recipe uses for empty tags — "Run some quick tests like opening your test world, checking `/kjs errors server_scripts` to check for any warnings, check if `minecraft:barrier` has any recipe uses (which indicates empty tags)" — [S16], step 7
- **C149** [quoted] The guide's server test is a person with a Linux server downloading and running the built server pack — "Get someone with a linux server (tom and sakura can both do this) to download the built serverpack and run it. Some bugs and crashes only show up on servers!" — [S16], step 8
- **C150** [quoted] The guide allows skipping the server test for a small urgent publish — "You can maybe skip this step if you're doing a small but urgent publish with a bunch of recipe hotfixes or something, but if it's not urgent, it's good to test this." — [S16], step 8
- **C152** [quoted] A separate tool, PakkuLockChecker in Tools-Modern, checks pakku-lock.json for version mismatches between CurseForge and Modrinth; the guide describes it as a step the publisher runs — "In Tools-Modern, there's a tool named PakkuLockChecker. It will go through your pakku-lock.json file and check for any version mismatches between curseforge and modrinth, and if mods are missing on either site." — [S16], step 5
- **C153** [quoted] The contribution guide says the merge of a PR triggers build and testing — "The merge of PR triggers build and testing processes to ensure the stability of the changes made." — [S15], Step 8, Post-Merge
- **C155** [quoted] The contribution guide says LLM use is not allowed in contributions to the modpack, the core mod or translations; the same paragraph adds that LLM help in diagnosing issues or peer-reviewing translations may be begrudgingly accepted and that code must be at least 90% written by the contributor — "The use of Artificial Intelligence or more specifically LLM's is not allowed when contributing to TerraFirmaGreg-Modern, our core mod, or translations." — [S15], AI Use / Disclosure, line 73
- **C156** [measured] GitHub search counts for Modpack-Modern on 2026-11-23: issues of type Bug 44 open and 1823 closed; 909 of them created since 2026-01-01; 268 with 'recipe' in the title; all issues 100 open and 3397 closed — Own measurements, M113
- **C157** [quoted] A reporter with GitHub contributor association, on 0.13.19, reported recipes with empty tags as input (issue 5363, open) — "There are 65 recipes containing empty tags as the input." — [S19], issue body, Actual Behavior
- **C158** [quoted] A team member answered that report by saying this is something they check before a release — "that's odd, this is always one of the things I check before doing a release" — [S20], comment by Pyritie (MEMBER), 2026-11-21
- **C159** [quoted] A reporter with GitHub contributor association, on 0.13.15, reported recipes missing on a fresh dedicated server (issue 5181); later in the thread he wrote that the recipes work but are not shown in EMI, older versions of three third-party mods fixed it for him, a team member added that it also happens in single player, and the issue was closed as not planned with the label "Triage: Not Our Issue" — "recipe is missing, I found many other recipes to be missing as well (e.g. all electrolyzers)." — [S21], issue body, Actual Behavior
- **C162** [measured] `gh release list` shows five Modpack-Modern releases, 0.13.13 to 0.13.17, dated 2026-10-18 to 2026-10-27 — Own measurements, M114
- **C163** [inference] No blocking automated check of recipes or progression was found: the automation seen is a boot smoke test that was skipped in most recent Build runs, the log script only reports, no workflow runs a linter and the pack tree has no test directory; recipe checks before a release are manual and recipe bugs arrive as issue reports — from [C113], [C117], [C124], [C137], [C138], [C148], [C156]

### Q2 — квесты, локализация, участники и время — answered with a gap

- **C201** [measured] On Modpack-Modern `dev` the quest definitions are 17 chapter SNBT files under config/ftbquests/quests/chapters, and the English quest text is 18 JSON files under kubejs/assets/tfg/lang/en_us/quests — Own measurements, M201
- **C202** [quoted] A quest in a chapter SNBT file carries a lang-key reference as its title, not the text itself — "title: "{quests.low_voltage.lv_fluid_regulator.title}"" — [S22], line 90
- **C203** [quoted] The English text for that key sits in a per-chapter lang JSON file — ""quests.low_voltage.lv_fluid_regulator.title": "LV Fluid Regulator"," — [S23], line 140
- **C204** [measured] The 17 chapter files hold 1085 quest objects in total, and chapter_groups.snbt lists 5 chapter groups — Own measurements, M202
- **C205** [measured] The 18 English quest lang files hold 3312 keys, of which 868 end in `.title`; their values total about 82,800 whitespace-separated words (formatting codes included) in 670,157 bytes — Own measurements, M203
- **C206** [quoted] The Modpack-Modern README, under external credits, thanks GregTech Community Pack Modern for most of their quest text — "- [GregTech Community Pack Modern](https://www.curseforge.com/minecraft/modpacks/gregtech-community-pack-modern) for most of their quest text." — [S24], section "External Credits and Special Thanks"
- **C207** [quoted] The wiki contributing guide forbids hard-coded text and sends lang strings to the tfg en_us language folder for translation — "- **No hard-coded text!** Lang strings should be used in all relevant places and submitted to our [Language Folder](https://github.com/TerraFirmaGreg-Team/Modpack-Modern/tree/dev/kubejs/assets/tfg/lang/en_us) for translation." — [S15], Quality Guidelines, line 28
- **C208** [quoted] The contributing guide says LLM use is not allowed for contributions to the modpack, the core mod or translations; the same paragraph adds that LLM help in diagnosing issues or peer-reviewing translations may be begrudgingly accepted and that code must be at least 90% written by the contributor — "The use of Artificial Intelligence or more specifically LLM's is not allowed when contributing to TerraFirmaGreg-Modern, our core mod, or translations." — [S15], AI Use / Disclosure, line 73
- **C210** [quoted] The guide states that merging into dev needs at least two approvals from the Contributor-Modern team — "- For merging into dev – at least two approvals from members of the [Contributor-Modern] team are required." — [S15], Step 8, line 396
- **C211** [quoted] The guide states that approved PRs are merged with Squash and Merge — "- After approval, an authorized member or maintainer performs the merge PR (using Squash and Merge according to project rules)." — [S15], Step 8, line 399
- **C212** [measured] The Wiki file tree has no English page whose path contains quest, locali, translat or lang; of the 13 developer pages only contributing.md was read in full, the other 12 were judged by file name — Own measurements, M204
- **C214** [measured] 525 commits touch config/ftbquests/quests/chapters, by 41 distinct authors; one author (Pyritie) has 311 of them, the next are an unlinked author "Dmitry" 59 and TomPlop 47 — Own measurements, M205
- **C215** [measured] The folder kubejs/assets/tfg/lang/en_us/quests has only 27 commits, the earliest dated 2026-09-29, so the per-chapter quest lang files at this path are under two months old — Own measurements, M206
- **C216** [quoted] ci/run.sh describes itself as the CI for config, release resolution, export, deploy and site build — "# QuestBook-Modern CI — config, release resolution, export, deploy, site build." — [S25], line 2
- **C217** [quoted] The build workflow runs on a daily schedule — "- cron: "0 0 * * *"" — [S26], line 5
- **C218** [quoted] The export tool the pipeline installs is configured as a repository outside the org — "FQE_REPO=jmecn/ftb-quest-export" — [S27], FQE_REPO line
- **C219** [quoted] The export step launches the game with a JVM flag that makes the quest export run and exit — "-Dquest.export.runAndExit=true" — [S26], job export, step "Run export", line 155
- **C222** [inference] QuestBook-Modern is a publishing pipeline: on a daily schedule it resolves a release, exports the quest book from a game run with an export tool from outside the org, and deploys a site; its self-described scope names no authoring or validation of quests — from [C216], [C217], [C218], [C219]
- **C223** [quoted] The Modpack-Modern README sends translators to a Crowdin project — "Want to help translate? Check out our [Crowdin](https://terrafirmagreg.crowdin.com/modern)!" — [S24], section Development
- **C224** [quoted] The Crowdin config takes every namespace's en_us.json under kubejs/assets as a source file — "- source: "/*/lang/en_us.json"" — [S28], files, first entry
- **C225** [quoted] When a source string changes, Crowdin is told to keep existing translations but mark them unapproved — "update_option: "update_as_unapproved"" — [S28], files entries
- **C226** [quoted] The Crowdin sync workflow is scheduled at 03:00 on every second day of the month — "- cron: '0 3 */2 * *'" — [S2], on.schedule
- **C227** [quoted] The workflow's final job opens a translations pull request and has a step that merges it automatically — "- name: "Automatically Merge PR"" — [S2], job create-pr, last step
- **C228** [measured] 228 merged PRs in Modpack-Modern carry the label "Pull request: Translation", and github-actions[bot] is the author of 300 merged PRs and 223 commits — Own measurements, M208
- **C229** [measured] The pack carries 19 locales including en_us: 19 locale folders under kubejs/assets/tfg/lang, and 132 namespaces have an en_us.json; coverage varies by locale from 130 of 132 namespace files (zh_cn, zh_tw, uk_ua, pl_pl) down to 8 (et_ee) — Own measurements, M209
- **C230** [quoted] The changelog credits translators per language, some by GitHub handle and some by plain name — "- Russian: @Nixieeunrare, Regrifone, Arka, iDzmitry, Neon430, MichalichMIC, ehnis" — [S29], "## [0.12.0] - 30-03-2026", "Translation updates"
- **C231** [quoted] The Tools-Modern README describes the repo as tooling to centralise translations and field guide pages — "**TerraFirmaGreg-Tools - Modern** is the backbone behind automated and centrilized files for the TerraFirmaGreg-Modern modpack. Developed mostly in C#, this tools repo is designed to centrilized translations, field guide pages and more." — [S30], section "Tools for TerraFirmaGreg-Modern"
- **C233** [measured] No Weblate or Paratranz file was found in Modpack-Modern or Tools-Modern; Crowdin is the only translation platform referenced in their trees, and Tools-Modern has no Crowdin file — Own measurements, M210
- **C234** [measured] Modpack-Modern's first commit is dated 2022-09-13 and `dev` has 9407 commits; by year: 2022 8, 2023 883, 2024 1139, 2025 4058, 2026 to date 3319 — Own measurements, M211
- **C235** [measured] Core-Modern's first commit is dated 2023-07-21 and `dev` has 2469 commits — Own measurements, M212
- **C236** [measured] Modpack-Modern has 148 GitHub releases (28 flagged prerelease), from 0.0.1 on 2023-11-12 to 0.13.19 on 2026-11-19: 14 in 2023, 39 in 2024, 51 in 2025, 44 in 2026 to date, roughly one per week over the last two years — Own measurements, M213
- **C237** [measured] Core-Modern has 107 GitHub releases, from 0.5.0 on 2024-04-29 to 0.9.36 on 2026-11-19: 11 in 2024, 42 in 2025, 54 in 2026 to date — Own measurements, M214
- **C238** [measured] GitHub lists 162 contributors to Modpack-Modern (169 with anonymous ones, 3 of them bots) and 51 to Core-Modern (54 with anonymous, 1 bot) — Own measurements, M215
- **C239** [measured] Modpack-Modern commits are concentrated: one contributor (Pyritie) has 4847 of 9407 (51.5%), the top five humans have 6141 (65.3%), 34 contributors have 10 or more commits and 99 of 169 have one or two — Own measurements, M216
- **C240** [measured] Core-Modern is similarly concentrated: Pyritie 1041 of 2469 commits (42.2%), top five 1486 (60.2%), 19 contributors with 10 or more — Own measurements, M217
- **C241** [measured] The top contributor's first Modpack-Modern commit is dated 2024-12-19, more than two years after the repo's first commit — Own measurements, M218
- **C242** [measured] In the 12 months to 2026-11-23 Modpack-Modern `dev` received 3818 commits from 111 distinct authors, 21 of them with 10 or more; Pyritie made 2397 (62.8%) — Own measurements, M219
- **C243** [measured] Modpack-Modern has 1567 merged PRs of 1825 opened, by 162 distinct PR authors; Core-Modern has 607 merged of 653 — Own measurements, M220
- **C245** [measured] The GitHub org shows 11 public members and was created 2022-09-12; its description spans Minecraft 1.12.x to 1.20.x, and a sibling repo Modpack-Vintage (created 2022-09-12, 3029 commits) is described as the 1.12.x pack — Own measurements, M221
- **C246** [snippet] A web search summary says the CurseForge listing names eight people for the Modern pack (Xikaro, Gamestar, Pyritie, jmecn, M_qrius, Redeix, gustavo47, TomTomEyo) — "The CurseForge listing names Xikaro, Gamestar, Pyritie, jmecn, M_qrius, Redeix, gustavo47, and TomTomEyo." — [S31], search snippet
- **C247** [measured] No statement by the team about its size, roles or history was found in the Modpack-Modern README, the org profile README, the Wiki index or the Wiki file tree — Own measurements, M222

### Q3 — жалобы и похвалы по прогрессии — answered with a gap

- **C301** [measured] The Modpack-Modern repo has 3497 issues and 25 GitHub Discussions; the discussions listed are team TODO lists and wiki/field-guide comment threads, not player feedback threads — Own measurements, M301
- **C302** [measured] Full-text issue matches for pacing vocabulary are few against 3497 issues and the top hits for "grind" are recipe/dupe bug reports, so the tracker holds little direct pacing complaint; "pacing" and "burnout" match 1 issue each — Own measurements, M302
- **C303** [measured] By title prefix the tracker is mostly bugs and crashes (1545 bug, 468 crash) against 749 suggestion-type issues and 107 questions — Own measurements, M303
- **C304** [measured] The most-commented issues are stability and performance bugs (chunk reloading 34 comments, server join failure 32, startup crash 27, memory leak 21, still open); the only balance thread in the top five is a late-game fuel line — Own measurements, M304
- **C305** [measured] The issue ranked first by thumbs-up is the request for an alternative kaolinite recipe (#455, 9 thumbs-up, 13 comments); the reply dismissing it drew 4 thumbs-down and 7 confused reactions — Own measurements, M305
- **C306** [measured] CurseForge lists 1,793,794 total downloads for the pack; the pack itself is not on Modrinth (search returns only its core mod and shaders) and Hacker News has no hits — Own measurements, M306
- **C307** [quoted] One player (pack 0.7.12) wrote that kaolinite, needed early, can force long travel — "kaolinite can require players to travel large distances for an extremely basic and vital resource" — [S32], issue body, flatpop, 2024-08-29
- **C308** [quoted] One commenter said that without an alternative fire clay recipe the whole GregTech part is gated by luck and walking — "otherwise all of gregtech is locked behind RNG and hours of just walking." — [S32], comment Wyrmlet, 2024-09-05
- **C309** [quoted] One player reported days of group searching for kaolin without success — "I have spent multiple days searching for it with people, and even now as ive started tping around the map and using commands i cannot find any" — [S32], comment jam-jar-binks, 2024-10-07
- **C310** [quoted] A team member (Pyritie, before or while on the team) said graphite veins had no usable hint — "there's nothing helpful besides \"find gabbro, prop pick randomly, and pray\"" — [S32], comment Pyritie (member), 2024-09-26
- **C311** [quoted] The kaolinite alternative was accepted by the team — "Implemented" — [S32], comment Exzept1on (contributor), 2024-10-26
- **C312** [quoted] An earlier request (0.7.8) named kaolin and garnierite as frustrating for players new to TFC or with a bad spawn — "Finding Kaolinite Clay may be frustrating to find for players that arent experienced with TFC or didnt spawn in a proper world" — [S33], issue body, Lollovader, 2024-05-10
- **C313** [quoted] The team answered that request by making garnierite more common — "Rarity for garnierite has been changed from 60 -> 40. Accepted in 0.7.9" — [S33], comment Exzept1on (contributor), 2024-05-22
- **C314** [quoted] A year later (0.11.5) a player still called finding kaolin the big problem — "The big great problem of cant find kaolin clay" — [S34], issue body, wierdPatatas, 2025-11-08
- **C315** [quoted] A reviewer (0.12.8, 100 h) confirmed the kaolin search is bad but said the pack's nether-equivalent dimension fixes it — "The search for kaolin clay was about as terrible as the horror stories I'd heard, but the ability to go to the pack's equivalent of the nether and find it much easier solved that problem." — [S35], review by Rezza20, 2026-05-27
- **C316** [quoted] In 0.12.0 the team moved early ores near the surface, naming deep digging with stone tools as the thing removed — "\"Early game\" ores now generate pretty commonly in small veins just below the surface, so you don't have to dig down 80 blocks with stone tools." — [S36], [0.12.0] 30-03-2026, line 1296
- **C317** [quoted] A team member rejected making ores rarer, stating spawn-luck mechanics are not fun — "It's just going to mean more running around and getting annoyed. Any mechanic that relies on getting lucky with spawn rng isn't fun." — [S37], comment Pyritie (member), 2026-10-29
- **C319** [quoted] A contributor stated that players pick easy-but-slow over complex-but-fast, and that this caused a feature removal — "People would much rather do something easy that takes a long time, than something complex that's faster. It's the reason why the alternator was removed, as people would rather set up 128 water wheels + alternators than set up a proper energy source." — [S37], comment Jeuvke (contributor), 2026-10-29
- **C320** [quoted] A team member said setting up new lines all the time is tiring, that the team does not want more than about two ore processing lines per tier, and that it has already had plenty of pushback from the other direction — "Also, setting up new lines all the time is tiring. We don't really want to do more than about two different ore processing lines per tier. We've already had plenty of pushback from the other direction" — [S37], comment Pyritie (member), 2026-10-29
- **C321** [quoted] One reviewer (1000 h) called the TFC part so grindy that the first machines felt like relief, and said this pack reversed his dread of GregTech — "Terrafirmacraft is so grindy that getting my first few GT machines was a welcome reprieve." — [S38], review by Tem, 2025-12-25
- **C322** [quoted] One reviewer (0.7.14, 100 h) called the pack really grindy, framed that as rewarding, and asked for a gentler early GregTech stage — "As expected of a GregTech modpack, it is really grindy, but this only makes it a better feeling of acomplishment when concluding your objectives. I personally think that the GT early game in the pack could be a little more forgiving." — [S39], review by jeanrds, 2024-11-11
- **C323** [quoted] A player asked for workbench-adjacent storage before AE2 because early inventory handling frustrated him (4 thumbs-up, status Done) — "An early-game option to store and access some items from a workbench would make inventory management a less frustrating experience. AE2 is, reasonably enough, locked to HV" — [S40], issue body, Froffy025, 2026-04-02
- **C324** [quoted] A player said the practical pre-steel way to process kaolin is unintuitive for newcomers, while liking kaolin's role — "I like kaolin and what it does for early TFG gameplay. But I think this method is obtuse for a newer player." — [S41], issue body, deadlyjackattack, 2026-04-29
- **C325** [quoted] The team then added a time-saving kaolin recipe — "Kaolin clay blocks can now also be cooked into powder, to save time processing it all (#3922)" — [S36], [0.12.6] 02-05-2026, line 1115
- **C326** [quoted] One player (0.7.12) complained that hazardous dust kills during early mining with no protection available — "This is so annoying when mining in early game, when there is absolutely no way to get hazardous protection suits." — [S42], issue body, yuesha-yc, 2024-08-07
- **C327** [quoted] A team member said this is a pack-level problem created by joining the two mods — "It's a tfg issue because progression requires ores from hazardous veins a lot sooner than in normal gregtech and you can't easily mine around them because of cave-ins." — [S42], comment Pyritie (member), 2024-09-26
- **C329** [quoted] A first-time player with no GregTech experience, on reaching HV, wrote that an incident put him off playing for a long time — "It was my first time playing TFG & getting to HV. No experience with GT so didnt scale properly. it put me off playing for a long time because of it." — [S43], comment BigMenz, 2026-10-15
- **C333** [quoted] One player (0.7.12) said early quests explain almost nothing — "Almost all the quests for early game give little or no information on how to complete them, for instance I had to look up how to weld ingots as it's not in the GUI." — [S44], issue body, Hexicube, 2024-09-01
- **C334** [quoted] One player praised the pack as his best ever and in the same sentence called the quest book weak — "This is by far the greatest modpack ive ever played, however i think the quest book is extremely lackluster" — [S45], issue body, MasonWilly, 2026-04-01
- **C335** [quoted] The same player had to leave the game to learn what to automate when — "i find myself having to go to the discord or online just to figure out what things i should have automated by the point im at." — [S45], issue body, MasonWilly, 2026-04-01
- **C336** [quoted] A team member said the team does not want the quest book to become a linear checklist — "we don't really want the quest book to become a linear checklist." — [S45], comment Pyritie (member), 2026-04-05
- **C338** [quoted] In 0.11.19, under "LV changes", the team rearranged the quest chapter, saying it should now be much less intimidating for new players — "Completely rearranged the quest chapter! Now it should be much less intimidating for new players" — [S36], [0.11.19] 03-02-2026, line 1692
- **C339** [quoted] In 0.13.11 the team split information: mechanics in the field guide, progression in the quest book — "From now on the field guide will be your main source of detailed information about mechanics. To view progression related information, please view the quest book." — [S36], [0.13.11] 05-10-2026, line 262
- **C340** [quoted] After that split one player objected that removed quest chapters cost him the overview of what is possible — "now I am forced to read the \"how\" to understand \"what\" is possible" — [S46], comment mlaidma, 2026-10-21
- **C341** [quoted] The maintainer's reason for removing information-dump quest chapters — "From my past experiences, few people were reading them, and having information in two different places is more work to maintain" — [S46], comment Pyritie (member), 2026-10-21
- **C342** [quoted] A player reported that Create's in-game tutorials mislead newcomers because the pack changed the recipes (open, low priority) — "Ponder scenes are unaltered from their base Create and its addons, leading to the recipes used as examples often being wrong due to TFC's sweeping changes" — [S47], issue body, LorefiendElianne, 2025-07-19
- **C343** [quoted] In 0.12 the team added selectable spawn climate as a difficulty lever — "added a new system to set your spawn in different climates for an easier or harder experience!" — [S48], section Overworld
- **C344** [quoted] One reviewer (0.12.8, 100 h, played with friends) praised the from-nothing start as earned progression — "Having to actually plan your survival properly is simply amazingly fun and every bit of progression feels spectacularly earned." — [S35], review by Rezza20, 2026-05-27
- **C345** [quoted] The same reviewer praised the hand-over from TFC to GregTech — "The intro to gregtech was very smooth, too, and felt like a natural progression of the terrafirmacraft portion of the game, very seamless." — [S35], review by Rezza20, 2026-05-27
- **C346** [quoted] The same reviewer stopped during the GregTech part without regret — "I burnt out of it very naturally during the gregtech portion and I don't feel bad about it, it was a very fun pack that I knew I wasn't going to finish." — [S35], review by Rezza20, 2026-05-27
- **C347** [quoted] One reviewer (20 h played) said progression does not drag — "Progression doesn't feel dragged out and performance is good for the amount of mods." — [S49], review by icon, 2026-01-29
- **C348** [measured] Moddex shows an aggregate rating of 4.6 of 5 from 15 ratings and 6 reviews; the five reviews readable in the page data rate it 5, 5, 5, 5 and 4 — Own measurements, M307
- **C349** [quoted] One reviewer praised performance, including with shaders — "The pack runs super well, and is the first modpack I've played that I am able to use shaders on without bricking my computer." — [S38], review by Tem, 2025-12-25
- **C350** [quoted] One reviewer (0.7.4, self-hosted co-op) praised performance but hit client/server script mismatches — "occasionally my pack would stop working due to a broken mod or I would be unable to connect to my own server due to misaligned mods / kubejs" — [S50], review by megaglub, 2024-05-01
- **C351** [quoted] One player reported memory climbing until the game stutters and crashes — "my RAM usage starts at around 4 GB but steadily climbs over an hour or two of gameplay, eventually reaching the 16 GB I have allocated. This inevitably leads to significant stuttering and, eventually, a game crash." — [S51], issue body, BOX369, 2025-10-13
- **C355** [measured] A tracker search for "too easy" matches 9 issues; the recorded output lists their numbers only, so that none of them asks for a slower or harder early game is the gatherer's reading, not a recorded result — Own measurements, M308
- **C356** [inference] Luck- and travel-gated access to early key resources (kaolin, garnierite, deep early ores) drew the most thumbs-up in the tracker and was answered by the team over 2024–2026 with an accepted alternative kaolinite recipe, a garnierite rarity change, shallow early veins and one team member's stated view that spawn-RNG mechanics are not fun — from [C305], [C311], [C313], [C316], [C317]
- **C357** [inference] Readable praise credits the earned feel of the TFC start and the smooth hand-over to GregTech; the two readable cases of stopping are both in the GregTech stage (one player put off at HV, one reviewer burned out during the GregTech portion) — from [C329], [C344], [C345], [C346]

### Q4 — жалобы и похвалы по био-части — answered with a gap

- **C401** [quoted] TFC's own Modrinth description says the player must manage food expiration, nutrition and thirst — "The player must manage their food's expiration, nutrition, and thirst using our detailed food and cooking mechanics." — [S52], project description body (API)
- **C402** [quoted] TFC's own description says seasons, weather and climate cause the player to need to stockpile food for winter — "Seasons, weather, and a realistic climate and calendar model cause the player to need to stockpile food for winter to survive." — [S52], project description body (API)
- **C403** [quoted] A TFC maintainer states the issue tracker takes bug reports only and sends suggestions to Discord — "We use the issue tracker exclusively for bug reports, not suggestions or support requests." — [S53], comment eerussianguy 2025-06-25
- **C404** [quoted] One player (2020, 1.12 era) wrote that food expiration makes nutrition upkeep almost impossible on a shared always-on server — "Unless the server you play is only up when you're on it, food expiration makes multiplayer nutrition maintenance almost impossible." — [S54], issue body, Gaelmare 2020-04-20
- **C405** [quoted] A TFC maintainer explained that TFC does not tick items or search the world for items to decay, so nothing can be done under that assumption — "We don't tick items, we don't try and find every item in the world and check it for decay, so there's *nothing* that can be done operating under that assumption." — [S54], comment alcatrazEscapee 2020-04-20
- **C406** [quoted] In 2022 the TFC maintainer confirmed the multiplayer food decay problem stays unsolved — "Playing on servers where people are on at different frequencies will unfortunately have this problem for the foreseeable future, with respect to food decay." — [S54], comment alcatrazEscapee 2022-07-20
- **C407** [quoted] In 2024 one server player asked again for a config that protects inventory food while the player is offline — "Easy solution, literally just add a config option that protects inventory food when offline and leave it off by default." — [S54], comment Triforcey 2024-02-12
- **C408** [quoted] TFC ships a server config multiplier that can soften or switch off food decay — "Setting this to zero will cause decay not to apply." — [S55], foodDecayModifier comment, line 615 (1.20.x)
- **C409** [quoted] A TFC maintainer (2020, 1.12 era) said disabling seasons means forcing a constant temperature, with massive effects on every crop, bush, tree and grass — "Disabling seasons basically involves forcing the temperature to be constant which is going to have quite massive effects on every crop, bush, tree, grass, etc." — [S56], comment alcatrazEscapee 2020-05-13
- **C410** [quoted] One player (Jan 2022) wanted animals that never age out and vanish — "I want a way they can live forever without turning into old boomers and Thanos eventually killing them" — [S57], comment HatcheryRoosters 2022-01-29
- **C411** [quoted] In TFC 1.21 beta a player reported crops dying under favourable conditions; the thread has 10 comments — "It has been observed that some crops die even under favorable conditions..." — [S58], issue body 2025-09-23
- **C412** [measured] Modrinth downloads on 2026-11-23: TerraFirmaCraft 764,287; TFC Grooming Station (automatic animal feeding/familiarity) 53,694; TFC Lunch Box (portable preservation) 18,296; FirmaNutrition 3,402; tfcfreezer 1,200; AutoDrinking 273. Searches for 'tfc decay', 'tfc no decay', 'tfc thirst', 'tfc crops', 'tfc familiarity' returned 0 projects — Own measurements, M401
- **C413** [quoted] The Grooming Station addon sells itself on removing the chore of keeping animals familiar — "No longer will you wake up after a week of neglect and find all your animals estranged and unwilling." — [S59], project body (API)
- **C414** [quoted] The FirmaNutrition addon's stated motive is that vanilla TFC nutrition drops in sudden large steps — "This makes decay feel more realistic and prevents sudden large decreases in nutrition, while also encouraging higher-nutrition meals." — [S60], project body (API)
- **C415** [quoted] The TFC Lunch Box addon, made for TerraFirmaGreg, exists to slow spoilage of carried food — "Applies the **"Preserved"** trait - slows food spoilage by 45%" — [S61], project body (API)
- **C416** [measured] TFG's default config has foodDecayModifier = 1.0, and the pack ships a server override that slows decay: 0.25 from Oct 2025 (PR #2076), later raised to 0.5 (commit e05c2c12) — Own measurements, M402
- **C417** [quoted] TFG 0.13.7 changelog: decay on servers was made faster, single player untouched — "increased the rate at which food decays *on servers using the default settings* (single player is not affected)" — [S62], [0.13.7] 7-08-2026
- **C418** [quoted] One TFG server player (Mar 2026, when the server modifier was 0.25) still found balanced-diet upkeep annoying because food rots fast — "managing a balance diet does get annoying on the server with things rotting so fast" — [S63], issue body, BLSK 2026-03-15
- **C419** [quoted] The same player reported ending up with only rotten grain again — "I found myself with nothing but Rotten Grains and what not again" — [S63], comment BLSK 2026-03-15
- **C420** [quoted] One TFG player (0.9.11) wrote that the whole food loop becomes annoying over time and asked for a nutrient paste machine — "Food gets kind of annoying at some point. Farming cooking and Hunting etc..." — [S64], issue body, mobinator 2025-07-22
- **C421** [quoted] TFG 0.10 added advanced food items (freeze-dried fruit, calorie paste, meal bags) — "Added many new advanced food items including freeze-dried fruit, calorie paste, and meal bags." — [S65], 0.10 major changelog, line 27
- **C422** [quoted] Counter-voice: one TFG player (Sept 2024), told food would stop spoiling, said non-spoiling food should be earned — "Non-spoiling food feels like it should be a milestone you achieve, such as being able to can the food." — [S66], comment Hexicube 2024-09-09
- **C423** [quoted] One player called hot cooked food not stacking incredibly annoying; maintainers refused to change it — "This is incredibly annoying to manage when cooking multiple items." — [S67], issue body, LemonTreaz 2026-08-26
- **C424** [quoted] TFG team member Pyritie says food is deliberately balanced so no single perfect food exists, and complains players ignore the system — "It's intentional that there's no 100% perfect food with zero drawbacks. We put a lot of effort into the food balance of the pack only for people to ignore almost all of it and just make one thing." — [S68], comment Pyritie 2026-11-18
- **C425** [quoted] One player's stated goal is a single automated greenhouse covering full nutrition; eggs and milk block it — "I think it would be really nice if I could make a giant greenhouse with everything I need for a perfect nutrition" — [S68], issue body, Xisengan 2026-11-18
- **C426** [quoted] TFG team member Pyritie's view: early-game food is not hard, players neglect it and then complain about low health — "people are just lazy and try to survive off of the first few seeds they find and nothing else." — [S69], comment Pyritie 2026-03-19
- **C427** [quoted] TFG 0.13 food rework adds a positive buff per nutrient above a threshold — "each nutrient type has a special buff on the player when a certain threshold is crossed, such as >85% fruit nutrition granting a passive 30% mining speed buff" — [S70], Food rework section
- **C428** [quoted] One TFG player called thirst annoying in late game and asked for auto-drink; maintainers pointed to a helmet auto-drink upgrade — "thirst mechanic is pretty annoying on late game." — [S71], issue body, dhhdhshdja 2024-10-31
- **C429** [quoted] One player on the public server (2024) reported starving in winter in the primitive age — "During winter in the primitive age, access to food is much harder then access to water, thus causing the player to starve" — [S72], issue body, Nexuee 2024-02-17
- **C430** [quoted] A reply points at the multiplayer case: a newcomer joining in winter cannot have a greenhouse yet — "Can't make green house if you just joined lol xd" — [S72], comment WaterMelwin 2024-02-21
- **C431** [quoted] One group reported repeated deaths from cold in winter despite insulated clothing — "We died a lot in a cold temperate climate." — [S73], comment fakekerman 2026-07-08
- **C432** [quoted] TFG later cut temperature damage and stopped it draining thirst and hunger — "Temperature damage has been lowered significantly and no longer makes the player lose thirst/hunger" — [S62], [0.13.4] 12-07-2026
- **C433** [quoted] One player says animal production cannot be fully automated because animals wear out and herds collapse — "There is no real way to fully automate animal production." — [S74], issue body, Luxuride 2026-09-20
- **C434** [quoted] TFG contributor TomPlop rejects a breeder machine on performance grounds; synthetic milk/eggs are planned for IV instead — "Having a lot of entities in your base is terrible for your performance and there is better ways to mass produce the amount of material products you may need in the future." — [S74], comment TomPlop 2026-09-20
- **C435** [quoted] TFG contributor TomPlop: the Pastoral Rancher is an early-game fix for janky Create deployer setups, not an endgame source — "the Rancher was never thought to be an endgame method of producing animal drops but just an early game multiblock solving the janky Create Deployer setup" — [S75], comment TomPlop 2026-08-22
- **C436** [quoted] TFG contributor TomPlop admits a Mars-tier animal product was too slow to gather and had to be buffed — "For the glacial sheep we had the issue of it being incredibly slow to gather enough wools to make the Fission Reactor for Uranium so GameStar buffed them." — [S75], comment TomPlop 2026-08-22
- **C437** [quoted] A new machine for automating animal milking and shearing was introduced with the caveat that it does not speed animals up or replace old ones — "Does not go any faster than the in-world times, and you'll have to replace old animals yourself, but hey it's automatic." — [S62], [0.12.2] 11-04-2026
- **C438** [quoted] One player called egg collection automation laggy and janky and wanted eggs from the rancher; refused — "currently you have to do laggy, stinky, janky create/hopper bs to get eggs and i think this should be changed" — [S76], issue body, 0xAdachi 2026-09-19
- **C439** [quoted] One player said raising animals for meat is pointless in TFG and asked for crop rotation; rejected as too much work — "at the moment raising animals for meat does not make much sense" — [S77], issue body, Debches 2026-03-19
- **C440** [quoted] TFG team member Pyritie considers crop farming already deep and rejected plant genetics as needless complexity — "I don't really see what genetics would add besides too much complexity for a system that already has a lot of depth" — [S69], comment Pyritie 2026-03-19
- **C441** [quoted] One player called manual threshing of large harvests before LV tedious; one team member asked why so much grain is needed, another replied that querns can be automated with mechanical power, and the issue was closed as a duplicate with the label "Triage: Wontfix" — "Forcing players to manually craft thousands of wheat sheaves with a knife in their inventory is not just tedious and a strain on the wrists" — [S78], issue body, DerBurri 2026-09-11
- **C442** [quoted] One player called fruit pickup in large orchards grindy; the feature (baskets) already existed and he had not found it — "Managing large orchards in TFC/FirmaLife can become extremely grindy and time-consuming due to micro-management." — [S79], issue body 2026-11-12
- **C443** [quoted] TFG made greenhouses also affect tree taps and bees — "Greenhouses now also affect tree taps and bees, so you don't need a bedrock farm to get latex all year round" — [S62], [0.13.18] 07-11-2026
- **C444** [quoted] TFG team member Pyritie on the Beneath dimension: too few food sources, no grain or dairy — "Right now the only real option is pork/venison and glow berries." — [S80], issue body, Pyritie 2026-03-05
- **C445** [measured] Issue counts per search term (issues only, all states). TFC repo: animals 120, crops 57, nutrition 47, food decay 40, hunger 33, thirst 29, rot 27, winter 25, annoying 21, familiarity 20, breeding 12, spoil 11, farming 8, seasons 8, greenhouse 2, tedious 2. TFG repo: annoying 86, greenhouse 49, animals 42, crops 35, nutrition 34, tedious 30, hunger 22, spoil 17, winter 16, farming 14, familiarity 12, rot 10, food decay 8, seasons 6, breeding 5, thirst 3. The most-commented TFG thread for 'food' has 14 comments — Own measurements, M403
- **C446** [measured] TFG's shipped TFC config has: cow adulthood 192 days, gestation 58, 128 uses; pig 80/19/60; sheep 56/32/60; chicken adulthood 24 days, 100 uses; familiarityDecayLimit 0.3 — Own measurements, M404
- **C447** [measured] GitHub Discussions: disabled on TFC; TFG has 25, mostly developer TODO lists and wiki comment threads, none a player feedback thread on food, animals or crops. Hacker News: 11 hits for TerraFirmaCraft, 0 for TerraFirmaGreg, none about food, animals or farming — Own measurements, M405
- **C448** [inference] The complaints entered from the readable sources concern upkeep cost: spoilage on always-on servers, repetitive manual steps (threshing, pickup, familiarity, eggs), animals that wear out and resist automation, thirst late in the game, winter for newcomers — from [C404], [C407], [C413], [C418], [C428], [C429], [C430], [C433], [C438], [C441], [C442]
- **C449** [inference] Maintainers and team members of both projects defend the systems as intended design and answer complaints with tools (config multipliers, new food items such as meal bags, the rancher, greenhouse changes) instead of removing mechanics — from [C405], [C408], [C416], [C421], [C424], [C434], [C435], [C443]

## Disconfirming evidence

- [C163] — query: `gh search code --owner TerraFirmaGreg-Team "gametest" --limit 20` — 10 hits: Core-Modern build.gradle and ExampleTest.java, vendored TFC files, build.gradle of Jellies-Modern and Core-Magica, Actions modpack/test/action.yml. Found against the first assumption: a gameTestServer run configuration and an example test exist ([C127], [C128], [C129]) and CI does boot the pack headlessly ([C113], [C124]). Nothing found that runs GameTests or validates recipes in CI.
- [C163] — query: `gh api search/issues` with `org:TerraFirmaGreg-Team "recipe" validation OR validator OR "unit test" OR gametest in:title` — 653 hits, the first 15 are ordinary recipe bug reports and suggestions, none about a validation tool: nothing found against.
- [C239] — query: WebSearch `TerraFirmaGreg Modern development team members artists translators credits "TerraFirmaGreg" team history 1.12.2 Exception Xikaro` — only the CurseForge roster of [C246]; nothing contradicting the concentration. Repository evidence that commit counts understate helpers: translations arrive as bot PRs ([C228]), the changelog names translators without a GitHub handle ([C230]), quest text is credited to another pack ([C206]).
- [C356] — query: GitHub issue search `repo:TerraFirmaGreg-Team/Modpack-Modern is:issue "too easy"` and `"too easy" OR "make harder" OR "more difficult" OR "more challenging"` — 9 and 26 matches, none about early-game pacing ([C355]); WebSearch `"TerraFirmaGreg" "early game" best part OR "worth it" OR "burned out" OR "gave up"` — no page about the pack except Moddex. Counter-voices entered as [C344], [C345], [C347]; [C321] and [C322] frame the grind as rewarding.
- [C448] — query: `gh api search/issues` with `repo:TerraFirmaGreg-Team/Modpack-Modern food love OR enjoy OR fun OR favorite OR favourite` (317 hits, top 10 are bugs and feature ideas), the same for `farming` (15 hits), and for the TFC repo `decay OR nutrition OR seasons love OR enjoy OR favorite` (165 hits); WebSearch `TerraFirmaCraft "food" "seasons" best part love farming review blog TerraFirmaGreg` — wikis and promo sites, no first-hand review. No explicit praise of decay or nutrition found in readable sources; counter-voices entered as [C422] and [C440].

## Coverage

| Channel | Read | Gap |
|---|---|---|
| Users | GitHub issue threads of TerraFirmaGreg-Team/Modpack-Modern and TerraFirmaCraft/TerraFirmaCraft; Moddex reviews; Modrinth addon pages | Discord of both projects, Reddit, YouTube and CurseForge comments could not be read; the pack is not listed on Modrinth; Hacker News has no thread on it |
| Specialists | workflow, action and config files of the TerraFirmaGreg-Team repositories; the team's wiki developer pages; changelogs; maintainer replies in issues | the team's Discord; the bodies of the pull-request, export, changelog and crowdin actions; the Crowdin project itself |
| Data | own measurements M101–M114, M201–M222, M301–M308, M401–M405 (GitHub API counts, tree listings, config values, download counts) | no person-hours; no survey of players exists in readable sources |
| Adversarial | bug and complaint threads; the team's own statements of what went wrong ([C327], [C436]) | post-mortems or long-form critical reviews: three web searches for Q3 and two for Q4 found none |

Declared overruns of the ceiling (four searches and three full page reads per sub-question): Q1 — about 25 gh/curl calls, 3 wiki pages and about 20 repository files, because each of its five parts needed file text; Q2 — more than three page reads, because it spans small config files in five repositories; Q3 — about 5 search batches and about 12 issue threads, after the GitHub search API rate-limited the first pass; Q4 — about 9 search batches and about 45 short threads, because each batch brought new voices. All four ledgers reached the 20,000-character cap; entries cut for the cap are listed under Open questions where they matter.

Tools: no search-and-scrape connector was used; the run used `gh`, `curl` and built-in WebSearch. Reddit hits were not cited.

## Own measurements

### M101 — Q1 gathering, run 2026-11-23

Command and output: `for r in Modpack-Modern Core-Modern Actions Modpack-Template Mod-Template; do gh api repos/TerraFirmaGreg-Team/$r/contents/.github/workflows --jq '.[].name'; done` -> Modpack-Modern: automerge.yml build.yml crowdin.yml giscus-rename.yml server.yml / Core-Modern: build.yml / Actions: release.yml / Modpack-Template: build.yml / Mod-Template: build.yml

### M102 — Q1 gathering, run 2026-11-23

Command and output: `grep -nE '^\s+pull_request(_target|_review)?:' <5 MM workflow files> <CM build.yml>` -> automerge.yml:5: pull_request: / automerge.yml:8: pull_request_review: / automerge.yml:16: pull_request: / automerge.yml:40: pull_request: ${{ inputs.pull_request || github.event.pull_request.number }} / crowdin.yml:111: pull_request: ${{ steps.create_pr.outputs.pr_number }} (16, 40, 111 are input names)

### M103 — Q1 gathering, run 2026-11-23

Command and output: `gh run view 45073940784 --repo TerraFirmaGreg-Team/Modpack-Modern --json jobs --jq '.jobs[] | "\(.name) | \(.conclusion)"'` -> [🖥️] Project Info | success / [📦] Project Build | success / [🤝] Create Release PR | skipped / [🚦] Route | success / [🚀] Project Release | skipped / [🧪] Project Test | skipped / [📤] Game Export | skipped / [📱] Discord Message | skipped / [📰] Wiki Changelog | skipped

### M104 — Q1 gathering, run 2026-11-23

Command and output: `gh run list --repo TerraFirmaGreg-Team/Modpack-Modern --workflow build.yml --limit 60 --json databaseId` then per run `gh api repos/TerraFirmaGreg-Team/Modpack-Modern/actions/runs/<id>/jobs --jq '[.jobs[] | select(.name|test("Test")) | .conclusion] | join(",")'`, tallied with `sort | uniq -c` -> 2 test= / 3 test=cancelled / 3 test=failure / 43 test=skipped / 9 test=success. Non-skipped examples: 44545331385 2026-11-19T14:35:07Z dev test=success; 44537534034 2026-11-19T13:50:52Z dev test=failure; 42415357851 2026-11-07T21:20:05Z dev test=success (releases 0.13.19 and 0.13.18 are dated 2026-11-19 and 2026-11-07 in `gh release list`)

### M105 — Q1 gathering, run 2026-11-23

Command and output: `gh api repos/TerraFirmaGreg-Team/Modpack-Modern/rules/branches/main --jq '.[] | select(.type=="required_status_checks") | [.parameters.required_status_checks[].context] | join("; ")'` -> [🖥️] Project Info; [📦] Project Build; [🧪] Project Test

### M106 — Q1 gathering, run 2026-11-23

Command and output: `gh api repos/TerraFirmaGreg-Team/Modpack-Modern/rules/branches/dev --jq '[.[].type] | join(",")'` -> deletion,non_fast_forward,non_fast_forward,pull_request ; `... --jq '.[] | select(.type=="pull_request") | "required_approving_review_count=\(.parameters.required_approving_review_count)"'` -> required_approving_review_count=1

### M107 — Q1 gathering, run 2026-11-23

Command and output: `gh api "repos/TerraFirmaGreg-Team/Core-Modern/git/trees/dev?recursive=1" --jq '.truncated, (.tree[].path)' > cm_tree.txt` (first line: false) ; `grep -E '^src/test/.*\.java$' cm_tree.txt` -> src/test/java/su/terrafirmagreg/core/gametest/example/ExampleTest.java

### M108 — Q1 gathering, run 2026-11-23

Command and output: `grep -ciE 'gameTestServer|runData|runGameTestServer' Core-Modern_dev_.github_workflows_build.yml` -> 0 ; loop over every blob path of `gh api "repos/TerraFirmaGreg-Team/Actions/git/trees/main?recursive=1"` with `curl -sL <raw url> | grep -ilE 'eslint|prettier|npm run|runGameTestServer|runData|spotlessCheck|checkstyle'` -> no HIT lines printed

### M109 — Q1 gathering, run 2026-11-23

Command and output: `grep -ciE 'eslint|prettier|npm |lint|tsc' Modpack-Modern_dev_.github_workflows_*.yml` -> automerge.yml:0 / build.yml:0 / crowdin.yml:0 / giscus-rename.yml:0 / server.yml:0 ; `gh api "repos/TerraFirmaGreg-Team/Modpack-Modern/git/trees/dev?recursive=1" --jq '.truncated, (.tree[].path)' > mm_tree.txt` (first line: false, 19005 lines) ; `grep -ciE '(^|/)(\.husky|\.pre-commit-config\.yaml|lefthook\.yml|\.lintstagedrc|\.githooks)' mm_tree.txt` -> 0

### M110 — Q1 gathering, run 2026-11-23

Command and output: `grep -ciE '(^|/)(test|tests|__tests__|spec)(/|$)|\.test\.js$|\.spec\.js$' mm_tree.txt` -> 0

### M111 — Q1 gathering, run 2026-11-23

Command and output: `grep -c 'modrinth_id\|enable_modrinth' Modpack-Modern_dev_.github_workflows_build.yml` -> 0 ; `grep -n 'curseforge_id\|modrinth' <same file>` -> 179: ${{ needs.info.outputs.project_full_name }}-modrinth.mrpack / 185: curseforge_id: ${{ vars.CURSEFORGE_ID }}

### M112 — Q1 gathering, run 2026-11-23

Command and output: `grep -c '^config/mod-director/' mm_tree.txt` -> 11 ; `grep -c '^config/mod-director/.*\.curse\.json$' mm_tree.txt` -> 10

### M113 — Q1 gathering, run 2026-11-23

Command and output: `gh api -X GET search/issues -f q="repo:TerraFirmaGreg-Team/Modpack-Modern <Q>" --jq .total_count` -> `is:issue type:Bug is:open` => 44 ; `is:issue type:Bug is:closed` => 1823 ; `is:issue type:Bug created:>=2026-01-01` => 909 ; `is:issue type:Bug recipe in:title` => 268 ; `is:issue is:open` => 100 ; `is:issue is:closed` => 3397

### M114 — Q1 gathering, run 2026-11-23

Command and output: `gh release list --repo TerraFirmaGreg-Team/Modpack-Modern --limit 8` -> 0.13.19 Latest 2026-11-19T15:22:00Z / 0.13.18 2026-11-07T22:53:52Z / 0.13.17 2026-10-27T05:21:54Z / 0.13.16 2026-10-25T15:56:36Z / 0.13.15 2026-10-21T02:45:40Z / 0.13.14 2026-10-19T21:52:48Z / 0.13.13 2026-10-18T22:17:37Z / 0.13.12 Pre-release 2026-10-06T14:04:45Z

### M201 — Q2 gathering, run 2026-11-23

Command and output: `grep -c -E '^config/ftbquests/quests/chapters/.*\.snbt$' mm-tree.txt` -> `17`; `grep -c -E '^kubejs/assets/tfg/lang/en_us/quests/.*\.json$' mm-tree.txt` -> `18`

### M202 — Q2 gathering, run 2026-11-23

Command and output: `python -I q2-scripts/count_quests.py q2/chapters` (counts objects that are direct elements of each file's top-level `quests: [` list) on the 17 files fetched with curl -> last line `TOTAL 1085`; chapter_groups.snbt read whole: 5 `{ id: ... title: ... }` rows.

### M203 — Q2 gathering, run 2026-11-23

Command and output: `cat qlang/*.json | grep -c -E '^\s*"[^"]+\.title"\s*:'` -> `868`; `... grep -c -E '^\s*"[^"]+"\s*:'` -> `3312`; `cat qlang/*.json | sed -E 's/^\s*"[^"]+"\s*:\s*//' | wc -w` -> `82764`; `cat qlang/*.json | wc -c` -> `670157`. 868 title keys vs 1085 quest objects: not every quest has its own title key (not investigated further).

### M204 — Q2 gathering, run 2026-11-23

Command and output: `grep -c -i -E 'docs/modern/en_us/.*(quest|locali|translat|lang)' wiki-tree.txt` -> `0`; `grep -c -E '^docs/modern/en_us/developer/.*\.md$' wiki-tree.txt` -> `13`. Only contributing.md was read in full; the other 12 were judged by file name.

### M205 — Q2 gathering, run 2026-11-23

Command and output: `gh api "repos/ORG/Modpack-Modern/commits?per_page=1&path=config/ftbquests/quests/chapters" -i` Link header -> `page=525>; rel="last"`; same query `per_page=100 --paginate --jq '.[] | (.author.login // ("anon:" + .commit.author.name))' | sort | uniq -c | sort -rn` -> 41 lines, top `311 Pyritie`, `59 anon:Dmitry`, `47 TomPlop`, `17 Redeix`.

### M206 — Q2 gathering, run 2026-11-23

Command and output: `gh api "repos/ORG/Modpack-Modern/commits?per_page=100&path=kubejs/assets/tfg/lang/en_us/quests" --paginate --jq '.[] | "\(.commit.author.date) \(.author.login)"'` -> 27 lines, last `2026-09-29T17:33:13Z Xikaro`. Where quest text lived before that date was not traced.

### M207 — Q2 gathering, run 2026-11-23

Command and output: `python -I -c "import json;d=json.load(open('qb-language.json',encoding='utf-8'));print(len(d['enabledLocales']))"` on QuestBook-Modern/main/language.json -> `16`. build.yml job order read in full: check -> export (steps "Fetch mods", "Prepare game", "Run export") -> deploy ("Build site", "Publish site release", "Deploy" to GitHub Pages). No lint or validation step for quest files was seen in build.yml.

### M208 — Q2 gathering, run 2026-11-23

Command and output: `gh api "search/issues?q=repo:ORG/Modpack-Modern+is:pr+is:merged+label:%22Pull+request:+Translation%22&per_page=1" --jq '.total_count'` -> `228`; `gh api "repos/ORG/Modpack-Modern/pulls?state=closed&per_page=100" --paginate --jq '.[] | select(.merged_at != null) | .user.login' | sort | uniq -c | sort -rn` -> top line `300 github-actions[bot]`; contributors API -> `github-actions[bot] 223`.

### M209 — Q2 gathering, run 2026-11-23

Command and output: `grep -c -E '^kubejs/assets/tfg/lang/[a-z_]+$' mm-tree.txt` -> `19`; `grep -E '^kubejs/assets/[^/]+/lang/[a-z_]+\.json$' mm-tree.txt | sed -E 's#.*/##' | sort | uniq -c | sort -rn` -> en_us 132, zh_tw 130, zh_cn 130, uk_ua 130, pl_pl 130, ru_ru 124, ja_jp 117, pt_br 102, es_es 95, hu_hu 93, fr_fr 92, de_de 89, tr_tr 84, ko_kr 69, lt_lt 55, sv_se 46, it_it 17, vi_vn 14, et_ee 8. File presence only; per-string completeness not measured.

### M210 — Q2 gathering, run 2026-11-23

Command and output: `grep -c -i -E 'crowdin|weblate|paratranz' tools-tree.txt mm-tree.txt` -> `tools-tree.txt:0`, `mm-tree.txt:8` (all 8 are crowdin paths). The Wiki repo has its own .github/workflows/crowdin.yml for docs in 7 locale folders (`grep -c -E '^docs/modern/[a-z_]+$' wiki-tree.txt` -> `7`).

### M211 — Q2 gathering, run 2026-11-23

Command and output: `gh api "repos/ORG/Modpack-Modern/commits?per_page=1" -i` Link -> `page=9407>; rel="last"`; `...&page=9407 --jq` -> `fbab7377 2022-09-13T01:27:39Z Dmitry | initial`; per year `...?per_page=1&since=Y-01-01T00:00:00Z&until=(Y+1)-01-01T00:00:00Z -i` Link last page -> 8, 883, 1139, 4058, 3319. March 2023 commits are titled "Delete mods directory" and "Delete config directory"; content resumes 2023-09-15 (read from commit titles only).

### M212 — Q2 gathering, run 2026-11-23

Command and output: `gh api "repos/ORG/Core-Modern/commits?per_page=1" -i` Link -> `page=2469>; rel="last"`; `...&page=2469` -> `59ab6f07 2023-07-21T04:24:58Z Dmitry | Initial commit`.

### M213 — Q2 gathering, run 2026-11-23

Command and output: `gh api "repos/ORG/Modpack-Modern/releases?per_page=100" --paginate --jq '.[] | "\(.tag_name) \(.published_at) pre=\(.prerelease)"'` -> 148 lines; last `0.0.1 2023-11-12T13:48:01Z pre=true`; first `0.13.19 2026-11-19T15:22:00Z pre=false`; by year `14 2023`, `39 2024`, `51 2025`, `44 2026`; `grep -c pre=true` -> `28`.

### M214 — Q2 gathering, run 2026-11-23

Command and output: same command on Core-Modern -> 107 lines; last `0.5.0 2024-04-29T13:59:47Z pre=false`; first `0.9.36 2026-11-19T13:10:00Z pre=false`; by year `11 2024`, `42 2025`, `54 2026`. Tags without a GitHub release were not checked.

### M215 — Q2 gathering, run 2026-11-23

Command and output: `gh api "repos/ORG/Modpack-Modern/contributors?per_page=100" --paginate --jq '.[].login' | wc -l` -> `162`; with `&anon=1` -> `169`; Core-Modern -> `51` and `54`. Bots: github-actions[bot], dependabot[bot], terrafirmagreg-app[bot]; Core: dependabot[bot]. Without anon=1 the API omits authors whose commit e-mail is not linked to an account.

### M216 — Q2 gathering, run 2026-11-23

Command and output: contributors API top lines -> `Pyritie 4847`, `Exzept1on 373`, `Xikaro 358`, `Redeix 283`, `TomPlop 280`, `github-actions[bot] 223`; sum of contributions with anon=1 -> `9406`; `awk '$2>=10'` -> 34 lines (bots included); `awk '$2<=2'` -> 99 lines.

### M217 — Q2 gathering, run 2026-11-23

Command and output: contributors API -> `Pyritie 1041`, `gustovafing 136`, `Redeix 118`, `Exzept1on 111`, `Mqrius 80`; `awk '$2>=10'` -> 19 lines.

### M218 — Q2 gathering, run 2026-11-23

Command and output: `gh api "repos/ORG/Modpack-Modern/commits?per_page=1&author=Pyritie" -i` Link -> `page=4846>; rel="last"`; `...&page=4846` -> `747f3cf0 2024-12-19T19:03:01Z | Added missing english language strings for some GTCEU machines`.

### M219 — Q2 gathering, run 2026-11-23

Command and output: `gh api "repos/ORG/Modpack-Modern/commits?per_page=100&since=2025-11-23T00:00:00Z" --paginate --jq '.[] | (.author.login // ("anon:" + .commit.author.name))'` -> 3818 lines; `sort | uniq -c | sort -rn` -> 111 lines, top `2397 Pyritie`, `283 Xikaro`, `223 github-actions[bot]`, `202 TomPlop`, `105 Redeix`; `awk '$1>=10'` -> 21.

### M220 — Q2 gathering, run 2026-11-23

Command and output: `gh api "search/issues?q=repo:ORG/Modpack-Modern+is:pr+is:merged&per_page=1" --jq '.total_count'` -> `1567`; `+is:pr` -> `1825`; Core-Modern -> `607` and `653`; pulls API merged authors (command in M208) -> 1568 lines, 162 distinct, top `300 github-actions[bot]`, `169 Redeix`, `163 TomPlop`, `123 Pyritie`.

### M221 — Q2 gathering, run 2026-11-23

Command and output: `gh api orgs/ORG/members --paginate --jq '.[].login'` -> `ashleney BlueBoat29 gustovafing jmecn Mqrius Pyritie Redeix SakuraKitsurugi Ujhik woopSmile Xikaro`; `gh api orgs/ORG --jq '.description, .created_at'` -> `Modpack development with TerraFirmaCraft and GregTech on 1.12.x-1.20.x.`, `2022-09-12T15:58:12Z`; `gh api repos/ORG/Modpack-Vintage --jq '.description'` -> `An innovative modpack that contains GregTech and TerraFirmaCraft on 1.12.x.`; its commits Link -> `page=3029>; rel="last"`. Private members are not visible; team rosters (Dev-Modern, Contributor-Modern) returned HTTP 403.

### M222 — Q2 gathering, run 2026-11-23

Command and output: `grep -i -E 'quest|locali|translat|team|credit|history|about' wiki-tree.txt` (images excluded) -> 4 paths, none a team or history page; org `.github/main/profile/README.md` contains one image line only; README sections read: description, Development, External Credits. Discord and CurseForge pages were not read.

### M301 — Q3 gathering, run 2026-11-23

Command and output: `gh api graphql -f query='{repository(owner:"TerraFirmaGreg-Team",name:"Modpack-Modern"){issues{totalCount} discussions(first:60,...){totalCount nodes{number title category{name} upvoteCount comments{totalCount}}}}}'` -> `issues=3497 discussions=25`; top by comments: `1037 TODOs up=6 c=7`, `174 TODOs up=1 c=3`, `4328 General up=0 c=2`; the rest c<=1.

### M302 — Q3 gathering, run 2026-11-23

Command and output: `gh api -X GET search/issues -f q="repo:TerraFirmaGreg-Team/Modpack-Modern is:issue <term>" --jq .total_count` -> `grind => 40; tedious => 30; too slow => 16; too long => 56; pacing => 1; balance => 63; boring => 13; frustrating => 16; quit => 15; burnout => 1; suggestion => 54; feedback => 15; early game => 83; prospecting => 7; quest => 193`. First "grind" hits: `5038 [🐛]: incorrect lv miner part grinding recipe`, `4551 [🐛]: ... ingot duping`.

### M303 — Q3 gathering, run 2026-11-23

Command and output: `gh api graphql ... search(query:"repo:TerraFirmaGreg-Team/Modpack-Modern is:issue <emoji> in:title"){issueCount}` -> `💥 => 468; 🐛 => 1545; 💡 => 152; 💎 => 298; 🪶 => 299; ❓ => 107`.

### M304 — Q3 gathering, run 2026-11-23

Command and output: `issues(first:70,orderBy:{field:COMMENTS,direction:DESC})` -> `4685 OPEN c=34 [🐛]: chunk keeps randomly reloading every minute or so; 4085 CLOSED c=32 [💥]:Unable to Join Server Due To getsockopt; 2568 CLOSED c=31 [🪡]: The BTX Fuel Balance Issue ...; 1812 CLOSED c=27 [💥]: After launching the game, it crashes ...; 3672 OPEN c=21 [🐛]: Memory Leak and Max CPU usage in alpha 0.12.1`.

### M305 — Q3 gathering, run 2026-11-23

Command and output: `search(query:"repo:... is:issue sort:reactions-+1-desc")` first row `455 CLOSED c=13 r=9 2024-08-29 Alternative Kaolinite Recipe`; `gh issue view 455 --json ...reactionGroups,comments` -> `reactions={'THUMBS_UP': 9}`; `WaterMelwin (NONE) 2024-08-29 {'THUMBS_DOWN': 4, 'CONFUSED': 7}`.

### M306 — Q3 gathering, run 2026-11-23

Command and output: `curl -s https://api.cfwidget.com/minecraft/modpacks/terrafirmagreg-modern` -> `"title": "TerraFirmaGreg-Modern" ... "total": 1793794`; `curl -s "https://api.modrinth.com/v2/search?query=terrafirmagreg"` -> slugs `terrafirmagreg-core` (mod, 30988), `terrafirmagreg-shaders` (shader, 20248), no modpack; `curl -s "https://hn.algolia.com/api/v1/search?query=TerraFirmaGreg"` -> `"nbHits": 0`.

### M307 — Q3 gathering, run 2026-11-23

Command and output: `curl -sL https://moddex.gg/modpack/terrafirmagreg-modern` + JSON-LD parse -> `AGG {'ratingValue': 4.6, 'bestRating': 5, 'worstRating': 0.5, 'ratingCount': 15, 'reviewCount': 6}`; reviews 1275 rating 5, 810 rating 5, 808 rating 5, 809 rating 5, 812 rating 4 (review 811 not in the page data).

### M308 — Q3 gathering, run 2026-11-23

Command and output: `gh api graphql ... search(query:"repo:TerraFirmaGreg-Team/Modpack-Modern is:issue \"too easy\"")` -> `count=9`: issues 5328, 3667, 5343, 1950, 1539, 1472, 2630, 1325, 389.

### M401 — Q4 gathering, run 2026-11-23

Command and output: `curl -s https://api.modrinth.com/v2/project/<slug> | jq .downloads` -> terrafirmacraft dl=764287; tfc-grooming-station dl=53694; tfc-lunch-box dl=18296; firmanutrition dl=3402; tfcfreezer dl=1200; autodrinking dl=273. `curl -G https://api.modrinth.com/v2/search --data-urlencode query=<q>` -> 'tfc decay' total=0; 'tfc no decay' total=0; 'tfc thirst' total=0; 'tfc crops' total=0; 'tfc familiarity' total=0. CurseForge not queried (no API key)

### M402 — Q4 gathering, run 2026-11-23

Command and output: `grep foodDecayModifier` on raw dev/defaultconfigs/tfc-server.toml -> `foodDecayModifier = 1.0`; on dev/.pakku/server-overrides/defaultconfigs/tfc-server.toml -> `foodDecayModifier = 0.5`. `gh api repos/TerraFirmaGreg-Team/Modpack-Modern/commits/e05c2c12` patch -> `-foodDecayModifier = 0.25` `+foodDecayModifier = 0.5`, message 'increase the food decay rate on servers from 0.25 to 0.5, reduce item cooling speed'. `gh pr view 2076` body -> 'Changed tfc food decay modifier to 0.25 for servers' (Pyritie 2025-10-22).

### M403 — Q4 gathering, run 2026-11-23

Command and output: `gh api -X GET search/issues -f q="repo:<repo> is:issue <term>" --jq .total_count` run 2026-11-23 -> values as listed in the claim. With `-f sort=comments`: TFG 'food' #2630 14c, #3469 13c, #3266 12c; TFC 'crops' #3164 10c, 'food decay' #1059 9c.

### M404 — Q4 gathering, run 2026-11-23

Command and output: `grep -E 'AdulthoodDays|Uses|GestationDays|familiarityDecayLimit' tfg-tfc-server.toml` (raw dev/defaultconfigs/tfc-server.toml) -> familiarityDecayLimit = 0.3; cowAdulthoodDays = 192; cowUses = 128; cowGestationDays = 58; pigAdulthoodDays = 80; pigUses = 60; pigGestationDays = 19; sheepAdulthoodDays = 56; sheepUses = 60; sheepGestationDays = 32; chickenAdulthoodDays = 24; chickenUses = 100. TFC defaults not fetched.

### M405 — Q4 gathering, run 2026-11-23

Command and output: `gh api repos/TerraFirmaCraft/TerraFirmaCraft --jq .has_discussions` -> false; Modpack-Modern -> true, graphql discussions.totalCount -> 25; graphql search 'spoil OR rot OR decay' count=0, 'animals OR husbandry OR breeding' count=1 (a TODO list). `curl hn.algolia.com/api/v1/search?query=TerraFirmaCraft` -> hits=11; query=TerraFirmaGreg -> hits=0.


## Open questions

- Q1 — ceiling — what the third-party actions `headlesshq/mc-runtime-test` and `mc-server-test` assert was not opened, and the logs of the 3 failed Project Test runs were not read, so what the smoke test has caught in practice is unknown. Reading those two action definitions and `gh run view --log-failed` on the three runs would settle it.
- Q1 — no new information — why ten mods are delivered through `config/mod-director/` ([C146], [C147]) is not stated in any TFG source read.
- Q1 — sources disagree — the wiki says a PR merge triggers build and testing ([C153]) while measured runs show the test job skipped on most Build runs ([C124]). Reported as is.
- Q2 — blocked: no published statement — person-hours and the real team roster. The Dev-Modern and Contributor-Modern team rosters returned HTTP 403; the only named roster is a search snippet ([C246]).
- Q2 — no new information — how quests are edited in practice (in-game editor or hand-edited SNBT) and why 868 title keys stand against 1085 quest objects.
- Q3, Q4 — blocked: Discord, Reddit, YouTube, CurseForge comments — the largest body of player opinion, and most likely the praise, is there. A person with a Discord account reading the feedback channels of both projects would settle how typical each complaint is.
- Q3 — ceiling — the length of the stone and metal ages has no direct measure, and solo versus multiplayer has only incidental mentions.
- Q4 — ceiling — the planets' crops and animals, gestation and aging compared with TFC defaults ([C446] gives TFG's values only), and crop nutrients have no player opinion entered. Several TFC voices are from the 1.12 version ([C404], [C409]).
- Q4 — not verified — a TFG contributor's 2024 statement that food would stop spoiling in the next update appears only as context of [C422] and was not confirmed from the changelog.

## Sources

- [S1] TerraFirmaGreg-Team/Actions: modpack/README.md — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Actions/main/modpack/README.md — passage
- [S2] TerraFirmaGreg-Team/Modpack-Modern: .github/workflows/crowdin.yml — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/.github/workflows/crowdin.yml — passage
- [S3] TerraFirmaGreg-Team/Modpack-Modern: .github/workflows/automerge.yml — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/.github/workflows/automerge.yml — passage
- [S4] TerraFirmaGreg-Team/Actions: .github/workflows/release.yml — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Actions/main/.github/workflows/release.yml — passage
- [S5] TerraFirmaGreg-Team/Core-Modern: .github/workflows/build.yml — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Core-Modern/dev/.github/workflows/build.yml — passage
- [S6] TerraFirmaGreg-Team/Modpack-Template: .github/workflows/build.yml — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Template/main/.github/workflows/build.yml — passage
- [S7] TerraFirmaGreg-Team/Actions: modpack/test/action.yml — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Actions/main/modpack/test/action.yml — passage
- [S8] TerraFirmaGreg-Team/Actions: modpack/test/scripts/analyze-logs.sh — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Actions/main/modpack/test/scripts/analyze-logs.sh — passage
- [S9] TerraFirmaGreg-Team/Actions: modpack/route/action.yml — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Actions/main/modpack/route/action.yml — passage
- [S10] TerraFirmaGreg-Team/Actions: modpack/info/action.yml — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Actions/main/modpack/info/action.yml — passage
- [S11] TerraFirmaGreg-Team/Core-Modern: build.gradle — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Core-Modern/dev/build.gradle — passage
- [S12] TerraFirmaGreg-Team/Core-Modern: src/test/java/su/terrafirmagreg/core/gametest/example/ExampleTest.java — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Core-Modern/dev/src/test/java/su/terrafirmagreg/core/gametest/example/ExampleTest.java — passage
- [S13] TerraFirmaGreg-Team/Modpack-Modern: kubejs/package.json — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/kubejs/package.json — passage
- [S14] TerraFirmaGreg-Team/Modpack-Modern: kubejs/tsconfig.json — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/kubejs/tsconfig.json — passage
- [S15] TerraFirmaGreg-Team/Wiki: docs/modern/en_us/developer/contributing.md — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Wiki/main/docs/modern/en_us/developer/contributing.md — passage
- [S16] TerraFirmaGreg-Team/Wiki: docs/modern/en_us/developer/publishing.md — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Wiki/main/docs/modern/en_us/developer/publishing.md — passage
- [S17] TerraFirmaGreg-Team/Actions: modpack/release/action.yml — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Actions/main/modpack/release/action.yml — passage
- [S18] TerraFirmaGreg-Team/AutoPack-Director: docs/README.md — https://raw.githubusercontent.com/TerraFirmaGreg-Team/AutoPack-Director/main/docs/README.md — passage
- [S19] TerraFirmaGreg-Team/Modpack-Modern: 5363 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/5363 — passage
- [S20] TerraFirmaGreg-Team/Modpack-Modern: 5363#issuecomment-7066886275 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/5363#issuecomment-7066886275 — passage
- [S21] TerraFirmaGreg-Team/Modpack-Modern: 5181 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/5181 — passage
- [S22] TerraFirmaGreg-Team/Modpack-Modern: config/ftbquests/quests/chapters/lv__low_voltage.snbt — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/config/ftbquests/quests/chapters/lv__low_voltage.snbt — passage
- [S23] TerraFirmaGreg-Team/Modpack-Modern: kubejs/assets/tfg/lang/en_us/quests/low_voltage.json — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/kubejs/assets/tfg/lang/en_us/quests/low_voltage.json — passage
- [S24] TerraFirmaGreg-Team/Modpack-Modern: .github/README.md — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/.github/README.md — passage
- [S25] TerraFirmaGreg-Team/QuestBook-Modern: ci/run.sh — https://raw.githubusercontent.com/TerraFirmaGreg-Team/QuestBook-Modern/main/ci/run.sh — passage
- [S26] TerraFirmaGreg-Team/QuestBook-Modern: .github/workflows/build.yml — https://raw.githubusercontent.com/TerraFirmaGreg-Team/QuestBook-Modern/main/.github/workflows/build.yml — passage
- [S27] TerraFirmaGreg-Team/QuestBook-Modern: ci/build.env — https://raw.githubusercontent.com/TerraFirmaGreg-Team/QuestBook-Modern/main/ci/build.env — passage
- [S28] TerraFirmaGreg-Team/Modpack-Modern: .github/config/crowdin/lang.yml — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/.github/config/crowdin/lang.yml — passage
- [S29] TerraFirmaGreg-Team/Modpack-Modern: CHANGELOG.md — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/dev/CHANGELOG.md — passage
- [S30] TerraFirmaGreg-Team/Tools-Modern: README.md — https://raw.githubusercontent.com/TerraFirmaGreg-Team/Tools-Modern/dev/README.md — passage
- [S31] www.curseforge.com/minecraft/modpacks/terrafirmagreg-modern — https://www.curseforge.com/minecraft/modpacks/terrafirmagreg-modern — passage
- [S32] TerraFirmaGreg-Team/Modpack-Modern: 455 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/455 — passage
- [S33] TerraFirmaGreg-Team/Modpack-Modern: 358 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/358 — passage
- [S34] TerraFirmaGreg-Team/Modpack-Modern: 2189 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/2189 — passage
- [S35] moddex.gg/modpack/terrafirmagreg-modern/reviews/1275 — https://moddex.gg/modpack/terrafirmagreg-modern/reviews/1275 — passage
- [S36] TerraFirmaGreg-Team/Modpack-Modern: CHANGELOG.md — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/blob/main/CHANGELOG.md — passage
- [S37] TerraFirmaGreg-Team/Modpack-Modern: 5244 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/5244 — passage
- [S38] moddex.gg/modpack/terrafirmagreg-modern/reviews/808 — https://moddex.gg/modpack/terrafirmagreg-modern/reviews/808 — passage
- [S39] moddex.gg/modpack/terrafirmagreg-modern/reviews/809 — https://moddex.gg/modpack/terrafirmagreg-modern/reviews/809 — passage
- [S40] TerraFirmaGreg-Team/Modpack-Modern: 3673 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/3673 — passage
- [S41] TerraFirmaGreg-Team/Modpack-Modern: 3913 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/3913 — passage
- [S42] TerraFirmaGreg-Team/Modpack-Modern: 438 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/438 — passage
- [S43] TerraFirmaGreg-Team/Modpack-Modern: 5131 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/5131 — passage
- [S44] TerraFirmaGreg-Team/Modpack-Modern: 458 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/458 — passage
- [S45] TerraFirmaGreg-Team/Modpack-Modern: 3656 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/3656 — passage
- [S46] TerraFirmaGreg-Team/Modpack-Modern: 5177 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/5177 — passage
- [S47] TerraFirmaGreg-Team/Modpack-Modern: 1384 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/1384 — passage
- [S48] TerraFirmaGreg-Team/Wiki: docs/modern/en_us/major-changelogs/0.12.md — https://github.com/TerraFirmaGreg-Team/Wiki/blob/main/docs/modern/en_us/major-changelogs/0.12.md — passage
- [S49] moddex.gg/modpack/terrafirmagreg-modern/reviews/812 — https://moddex.gg/modpack/terrafirmagreg-modern/reviews/812 — passage
- [S50] moddex.gg/modpack/terrafirmagreg-modern/reviews/810 — https://moddex.gg/modpack/terrafirmagreg-modern/reviews/810 — passage
- [S51] TerraFirmaGreg-Team/Modpack-Modern: 2027 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/2027 — passage
- [S52] modrinth.com/mod/terrafirmacraft — https://modrinth.com/mod/terrafirmacraft — passage
- [S53] TerraFirmaCraft/TerraFirmaCraft: 2947 — https://github.com/TerraFirmaCraft/TerraFirmaCraft/issues/2947 — passage
- [S54] TerraFirmaCraft/TerraFirmaCraft: 1059 — https://github.com/TerraFirmaCraft/TerraFirmaCraft/issues/1059 — passage
- [S55] TerraFirmaCraft/TerraFirmaCraft: src/main/java/net/dries007/tfc/config/ServerConfig.java — https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/src/main/java/net/dries007/tfc/config/ServerConfig.java — passage
- [S56] TerraFirmaCraft/TerraFirmaCraft: 1264 — https://github.com/TerraFirmaCraft/TerraFirmaCraft/issues/1264 — passage
- [S57] TerraFirmaCraft/TerraFirmaCraft: 1861 — https://github.com/TerraFirmaCraft/TerraFirmaCraft/issues/1861 — passage
- [S58] TerraFirmaCraft/TerraFirmaCraft: 3164 — https://github.com/TerraFirmaCraft/TerraFirmaCraft/issues/3164 — passage
- [S59] modrinth.com/mod/tfc-grooming-station — https://modrinth.com/mod/tfc-grooming-station — passage
- [S60] modrinth.com/mod/firmanutrition — https://modrinth.com/mod/firmanutrition — passage
- [S61] modrinth.com/mod/tfc-lunch-box — https://modrinth.com/mod/tfc-lunch-box — passage
- [S62] TerraFirmaGreg-Team/Modpack-Modern: CHANGELOG.md — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/blob/dev/CHANGELOG.md — passage
- [S63] TerraFirmaGreg-Team/Modpack-Modern: 3424 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/3424 — passage
- [S64] TerraFirmaGreg-Team/Modpack-Modern: 1418 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/1418 — passage
- [S65] TerraFirmaGreg-Team/Wiki: docs/modern/en_us/major-changelogs/0.10.md — https://github.com/TerraFirmaGreg-Team/Wiki/blob/main/docs/modern/en_us/major-changelogs/0.10.md — passage
- [S66] TerraFirmaGreg-Team/Modpack-Modern: 470 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/470 — passage
- [S67] TerraFirmaGreg-Team/Modpack-Modern: 4848 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/4848 — passage
- [S68] TerraFirmaGreg-Team/Modpack-Modern: 5343 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/5343 — passage
- [S69] TerraFirmaGreg-Team/Modpack-Modern: 3469 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/3469 — passage
- [S70] TerraFirmaGreg-Team/Wiki: docs/modern/en_us/major-changelogs/0.13.md — https://github.com/TerraFirmaGreg-Team/Wiki/blob/main/docs/modern/en_us/major-changelogs/0.13.md — passage
- [S71] TerraFirmaGreg-Team/Modpack-Modern: 575 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/575 — passage
- [S72] TerraFirmaGreg-Team/Modpack-Modern: 250 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/250 — passage
- [S73] TerraFirmaGreg-Team/Modpack-Modern: 4450 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/4450 — passage
- [S74] TerraFirmaGreg-Team/Modpack-Modern: 5011 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/5011 — passage
- [S75] TerraFirmaGreg-Team/Modpack-Modern: 4814 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/4814 — passage
- [S76] TerraFirmaGreg-Team/Modpack-Modern: 5002 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/5002 — passage
- [S77] TerraFirmaGreg-Team/Modpack-Modern: 3487 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/3487 — passage
- [S78] TerraFirmaGreg-Team/Modpack-Modern: 4958 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/4958 — passage
- [S79] TerraFirmaGreg-Team/Modpack-Modern: 5310 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/5310 — passage
- [S80] TerraFirmaGreg-Team/Modpack-Modern: 3266 — https://github.com/TerraFirmaGreg-Team/Modpack-Modern/issues/3266 — passage

## Audit

| Claim | Verdict | Note |
|---|---|---|
| C101 | SUPPORTED |  |
| C102 | SUPPORTED |  |
| C103 | SUPPORTED |  |
| C104 | SUPPORTED |  |
| C105 | SUPPORTED |  |
| C106 | SUPPORTED |  |
| C109 | SUPPORTED |  |
| C110 | SUPPORTED |  |
| C112 | SUPPORTED |  |
| C113 | SUPPORTED |  |
| C114 | SUPPORTED |  |
| C115 | SUPPORTED |  |
| C117 | SUPPORTED | Script only writes ANALYZE_BAD to the environment; no exit. |
| C119 | SUPPORTED |  |
| C122 | SUPPORTED |  |
| C123 | SUPPORTED | dev_env is true when the ref is not the stable branch. |
| C124 | PARTIAL | Counts match the recorded output of M104; the date range of the 60 runs is not in it. Range removed. |
| C125 | SUPPORTED |  |
| C126 | SUPPORTED |  |
| C127 | SUPPORTED |  |
| C128 | SUPPORTED |  |
| C129 | SUPPORTED |  |
| C130 | SUPPORTED |  |
| C133 | SUPPORTED |  |
| C135 | SUPPORTED |  |
| C136 | SUPPORTED |  |
| C137 | SUPPORTED |  |
| C138 | SUPPORTED |  |
| C139 | SUPPORTED |  |
| C140 | SUPPORTED |  |
| C141 | SUPPORTED |  |
| C143 | SUPPORTED |  |
| C144 | SUPPORTED |  |
| C145 | SUPPORTED |  |
| C146 | SUPPORTED |  |
| C147 | SUPPORTED |  |
| C148 | SUPPORTED |  |
| C149 | SUPPORTED |  |
| C150 | SUPPORTED |  |
| C152 | SUPPORTED |  |
| C153 | SUPPORTED |  |
| C155 | PARTIAL | Quote verbatim, but the same paragraph allows LLM help in diagnosing issues and peer-reviewing translations and sets a 90% own-code rule; the section is "AI Use / Disclosure". Claim and place rewritten. |
| C156 | SUPPORTED |  |
| C157 | PARTIAL | Reporter's GitHub association is CONTRIBUTOR, not simply a player. Reworded. |
| C158 | SUPPORTED |  |
| C159 | PARTIAL | Reporter is a CONTRIBUTOR; thread says the recipes work but are not shown in EMI, third-party mod versions fixed it, also seen in single player; closed not planned, label "Triage: Not Our Issue". Reworded. |
| C162 | SUPPORTED |  |
| C163 | PARTIAL | "Recipe conflicts" and "limited to" exceed the named claims. Narrowed to what C113, C117, C124, C137, C138, C148, C156 carry. |
| C201 | SUPPORTED |  |
| C202 | SUPPORTED |  |
| C203 | SUPPORTED |  |
| C204 | PARTIAL | M202 records only TOTAL 1085 and 5 chapter groups; per-chapter numbers are not in the recorded output. Removed. |
| C205 | SUPPORTED |  |
| C206 | SUPPORTED |  |
| C207 | SUPPORTED |  |
| C208 | PARTIAL | Same paragraph as C155: qualification added, place corrected to "AI Use / Disclosure". |
| C210 | SUPPORTED | Guide says two approvals; the measured ruleset (C126) requires one. |
| C211 | SUPPORTED |  |
| C212 | PARTIAL | M204: only contributing.md was read in full, the other 12 developer pages were judged by file name. Narrowed to the path search. |
| C214 | SUPPORTED |  |
| C215 | SUPPORTED |  |
| C216 | SUPPORTED |  |
| C217 | SUPPORTED |  |
| C218 | SUPPORTED |  |
| C219 | SUPPORTED |  |
| C222 | PARTIAL | "16 locales" and "does not validate" rest on M207, not on the named claims. Narrowed to C216-C219. |
| C223 | SUPPORTED |  |
| C224 | SUPPORTED |  |
| C225 | SUPPORTED |  |
| C226 | SUPPORTED |  |
| C227 | SUPPORTED |  |
| C228 | SUPPORTED |  |
| C229 | SUPPORTED |  |
| C230 | SUPPORTED |  |
| C231 | SUPPORTED |  |
| C233 | SUPPORTED |  |
| C234 | SUPPORTED |  |
| C235 | SUPPORTED |  |
| C236 | SUPPORTED |  |
| C237 | SUPPORTED |  |
| C238 | SUPPORTED |  |
| C239 | SUPPORTED |  |
| C240 | SUPPORTED |  |
| C241 | SUPPORTED |  |
| C242 | SUPPORTED |  |
| C243 | SUPPORTED |  |
| C245 | SUPPORTED |  |
| C246 | UNCHECKABLE | Search snippet; the CurseForge page returned HTTP 403 to the auditor. |
| C247 | SUPPORTED |  |
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
| C312 | SUPPORTED |  |
| C313 | SUPPORTED |  |
| C314 | SUPPORTED |  |
| C315 | SUPPORTED |  |
| C316 | SUPPORTED |  |
| C317 | SUPPORTED |  |
| C319 | SUPPORTED |  |
| C320 | PARTIAL | Source: new lines are tiring, about two ore lines per tier, "pushback from the other direction"; it does not say whose pushback or that it is the reason. Reworded. |
| C321 | SUPPORTED |  |
| C322 | SUPPORTED |  |
| C323 | SUPPORTED | Author's GitHub association is CONTRIBUTOR. |
| C324 | SUPPORTED |  |
| C325 | SUPPORTED |  |
| C326 | SUPPORTED |  |
| C327 | SUPPORTED |  |
| C329 | PARTIAL | Quote says the incident put him off playing for a long time; "quit" and "could not afford to rebuild" are not in it. Reworded. |
| C333 | SUPPORTED |  |
| C334 | SUPPORTED |  |
| C335 | SUPPORTED |  |
| C336 | PARTIAL | Quote only refuses a linear checklist; "defined the quest book's job" removed. |
| C338 | PARTIAL | The changelog line sits under "LV changes": one quest chapter, not the quests as a whole. Reworded. |
| C339 | SUPPORTED |  |
| C340 | SUPPORTED |  |
| C341 | SUPPORTED |  |
| C342 | SUPPORTED |  |
| C343 | SUPPORTED |  |
| C344 | SUPPORTED |  |
| C345 | SUPPORTED |  |
| C346 | SUPPORTED |  |
| C347 | SUPPORTED |  |
| C348 | SUPPORTED |  |
| C349 | SUPPORTED |  |
| C350 | SUPPORTED |  |
| C351 | SUPPORTED |  |
| C354 | UNSUPPORTED | Claim said decay is faster on servers than in single player; the quote says the server rate was increased, and M402 shows servers at 0.5 against 1.0 in single player. Deleted. |
| C355 | PARTIAL | M308 records the count 9 and issue numbers only; "none about early pacing" is not a recorded result. Reworded. |
| C356 | PARTIAL | "Most visibly", graphite and "rarity changes" exceed the named claims. Narrowed. |
| C357 | PARTIAL | Rests on two cases; "not in the stone/metal ages" generalises beyond them. Narrowed. |
| C401 | PARTIAL | "Headline feature" is not in the source. Reworded. |
| C402 | PARTIAL | Source says seasons "cause the player to need to stockpile", not that they exist to force it. Reworded. |
| C403 | PARTIAL | The conclusion that the tracker under-represents design feedback is not in the quote. Removed. |
| C404 | SUPPORTED |  |
| C405 | PARTIAL | "Timestamp-based" is not in the quote. Reworded to what it says. |
| C406 | SUPPORTED |  |
| C407 | SUPPORTED |  |
| C408 | SUPPORTED |  |
| C409 | PARTIAL | Source: disabling seasons means constant temperature with massive effects; "temperature drives every plant system" overstated it. Reworded. |
| C410 | SUPPORTED |  |
| C411 | PARTIAL | The cited issue does not mention #3342 (it is closed as completed, not as not reproducible). That half removed. |
| C412 | SUPPORTED |  |
| C413 | SUPPORTED |  |
| C414 | SUPPORTED |  |
| C415 | SUPPORTED |  |
| C416 | PARTIAL | M402 records only foodDecayModifier; the other three config values and the date 2026-08-03 are not in the recorded output. Trimmed. |
| C417 | PARTIAL | "Again" is not in the quote. Removed. |
| C418 | SUPPORTED |  |
| C419 | SUPPORTED |  |
| C420 | SUPPORTED |  |
| C421 | PARTIAL | Quote lists new food items; "answered" and "long-life" are not in it. Reworded. |
| C422 | SUPPORTED |  |
| C423 | SUPPORTED |  |
| C424 | PARTIAL | "TFG lead" is not established by the source (GitHub association MEMBER). Reworded. |
| C425 | SUPPORTED |  |
| C426 | PARTIAL | "TFG lead" not established by the source. Reworded. |
| C427 | SUPPORTED |  |
| C428 | SUPPORTED |  |
| C429 | SUPPORTED |  |
| C430 | SUPPORTED |  |
| C431 | SUPPORTED |  |
| C432 | SUPPORTED |  |
| C433 | SUPPORTED |  |
| C434 | PARTIAL | TomPlop's GitHub association is CONTRIBUTOR; "Maintainer" not established. Reworded. |
| C435 | PARTIAL | "Maintainer" not established (CONTRIBUTOR). Reworded. |
| C436 | PARTIAL | "Maintainer" not established (CONTRIBUTOR). Reworded. |
| C437 | PARTIAL | Quote names "a new machine for automating animal milking and shearing", not the rancher. Reworded. |
| C438 | SUPPORTED |  |
| C439 | SUPPORTED |  |
| C440 | PARTIAL | "TFG lead" not established by the source. Reworded. |
| C441 | PARTIAL | "Lead" not established; the thread also has a member's reply that querns can be automated, and the issue is closed duplicate / Wontfix. Added. |
| C442 | SUPPORTED |  |
| C443 | PARTIAL | Quote covers tree taps and bees only; "without sky light" is another changelog line. Narrowed. |
| C444 | PARTIAL | "TFG lead" not established by the source. Reworded. |
| C445 | PARTIAL | M403 does not record that most hits are bug reports or that reactions are near zero. Removed. |
| C446 | SUPPORTED |  |
| C447 | SUPPORTED |  |
| C448 | PARTIAL | "Rather than on the existence of the systems" does not follow from the named claims. Removed. |
| C449 | PARTIAL | Fridges and lunchboxes are not in the named claims (the lunch box is a third-party addon). Removed. |

Label verdict: answered — the question as narrowed under Question is answered in the Summary by claims that are SUPPORTED or PARTIAL and repaired; the gaps (Discord, Reddit, YouTube, CurseForge comments; team roster; what the third-party smoke-test actions assert) limit how typical the complaints are and are stated, they do not reverse the narrowed answer

Quote check terrafirmagreg-process-reception-2026-11-23.md: 131 quoted claim(s): 131 found, 0 NOT FOUND, 0 unfetchable, 0 no source

## Disposition

| Recommendation | State | Where |
|---|---|---|
| В конвейере проверок запускать загрузку сервера на каждое изменение и делать разбор лога блокирующим | deferred | unfiled: this run was told to commit nothing and file nothing; the planning session that ordered it decides whether it becomes an issue or part of the check-pipeline design |
| Сделать автоматическими проверки, которые TFG выполняет руками: ошибки разбора рецептов, рецепты с пустыми тегами, ошибки скриптов KubeJS | deferred | unfiled: this run was told to commit nothing and file nothing; the planning session that ordered it decides whether it becomes an issue or part of the check-pipeline design |
| Запускать линтер и проверку типов скриптов KubeJS в CI | deferred | unfiled: this run was told to commit nothing and file nothing; the planning session that ordered it decides whether it becomes an issue or part of the check-pipeline design |
| Достижимость прогрессии проверять своим тестом | deferred | unfiled: this run was told to commit nothing and file nothing; the planning session that ordered it decides whether it becomes an issue or part of the check-pipeline design |
| Не ставить ранний ключевой ресурс в зависимость от удачи спавна и дальних походов; климатическому уникуму дать второй путь получения | deferred | unfiled: this run was told to commit nothing and file nothing; the planning session that ordered it decides whether it becomes an issue or part of the check-pipeline design |
| Порчу и уход за животными проектировать вместе с путём их автоматизации и с расчётом на сервер, который работает без игрока | deferred | unfiled: this run was told to commit nothing and file nothing; the planning session that ordered it decides whether it becomes an issue or part of the check-pipeline design |
