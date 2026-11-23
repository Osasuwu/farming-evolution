---
topic: breeding-mods
tags: area:breeding-core, research
source_provenance: /research invocation from the farming-evolution parent session, 2026-11-23
status: answered
audit: 2026-11-23 — 208 claims: 163 SUPPORTED, 41 PARTIAL, 0 UNSUPPORTED, 4 UNCHECKABLE
---

## Question

Существующие моды генетики и селекции растений и животных в Minecraft как база или образец для собственного мода.

Нужно ответить:
- AgriCraft: лицензия и можно ли форкать, архитектура (модель генома, мутации, API для аддонов), состояние разработки, известные ограничения и причины, по которым автоуборка и совместимость ломаются.
- Другие модели генетики: Forestry (пчёлы, деревья, бабочки) и его наследники, Gendustry, Binnie's Mods, Mystical Agriculture, Productive Bees и другие — как устроено наследование и что игроки в них хвалят и ругают.
- Животные: какие моды моделируют генетику, породы или одомашнивание (например Genetic Animals, животноводство TerraFirmaCraft, Hostile Neural Networks не в счёт); их лицензии, состояние, глубина модели.
- Как моды селекции связывают признаки с продукцией: есть ли прецеденты, где качество продукта зависит от значения признака и гейтит рецепты.
- Как сборки блокируют ванильную еду и делают одомашнивание диких животных.
- Вывод: форк AgriCraft, аддон к нему или мод с нуля — с условиями, при которых вывод меняется.

Sub-questions: Q1 AgriCraft как база; Q2 другие модели генетики; Q3 животные; Q4 связь признака с продукцией и гейтинг рецептов; Q5 блокировка ванильной еды и одомашнивание. Шестой пункт вопроса (вывод) — синтез в Summary, отдельного под-вопроса у него нет.

Отступление от правила скилла «не больше четырёх под-вопросов за прогон»: взято пять, потому что задача требует все пять тем в одном отчёте. Чтобы суммарный объём леджеров остался тем же (80 000 знаков), лимит каждого леджера снижен с 20 000 до 16 000 знаков. Все пять леджеров упёрлись в лимит, поэтому у каждого под-вопроса есть объявленные пробелы.

Предыдущего отчёта в `docs/research/` нет. Есть `docs/research-baseline.md` (2026-05-24) — он использован как наводка, не как источник. Что этот отчёт в нём меняет — в конце Summary.

Предполагаемая цель — Minecraft 1.20.1 Forge (как в baseline); данные по 1.21.1 приведены там, где они найдены.

## Summary

**Рекомендация: писать ядро селекции с нуля, заимствуя у AgriCraft отдельные решения и, где удобно, код под MIT; не форкать AgriCraft целиком и не строить ядро как аддон к нему.**

Почему не форк и не аддон:

- Юридически форк разрешён: AgriCraft под MIT [C101], [C102], у версий 4.0.3–4.0.6 одна обязательная зависимость [C103].
- Модель генома AgriCraft жёстко задана: одна пара аллелей вида плюс карта целочисленных пар статов [C110], [C113]; у каждой пары доминантный и рецессивный аллель, для статов доминирует большее число [C108], [C109]. Родитель передаёт один из двух аллелей случайно [C115], после чего аллель стата может сдвинуться на ±1 в зависимости от mutativity [C116]; новый вид появляется по правилу «родитель1 + родитель2 → потомок» с вероятностью [C118].
- Аддон, судя по исходникам, может добавить новый целочисленный стат через публичные реестры [C121], [C122], но ген другого типа в геном не помещается без форка или mixin; это вывод из чтения кода, не проверенный запуском [C123]. Стороннего кода, который так делает, поиск кода не нашёл [C129].
- Описание растения в JSON не содержит ничего про статы или гены [C127], а стат Gain меняет только количество плодов [C432]. То есть «признак на продукте» в AgriCraft отсутствует, и его пришлось бы достраивать в любом варианте.
- Автоуборка ломается из-за устройства блока: все культуры — один класс, наследующий обычный `Block`, с block entity; в blockstate нет возраста, геном и стадия роста лежат в block entity, сбор — правый клик с собственным вызовом [C130], [C131], [C132]. Харвестер Create 1.20.1 узнаёт зрелую культуру по ванильному `CropBlock` или по целочисленному свойству age [C133]. Отсюда вывод, что каждому харвестеру нужен отдельный плагин или mixin; заявления мейнтейнера о причине не найдено [C134]. Сторонний патч для Create устроен именно так [C135]. В дереве 1.20.1 каталоги плагинов есть только для Industrial Foregoing, PneumaticCraft и ванили [C142], [C139]; пользователи сообщают, что харвестеры Create, Actually Additions, Immersive Engineering не работают [C137], [C138], а в запросе от 2025-12-17 пользователь пишет, что сбор дронами PneumaticCraft и Plant Gatherer из Industrial Foregoing недавно включён, и просит добавить Hydroponic Bed [C141]. Форк унаследует эту архитектуру.
- Семена с разными генами не стакаются [C147].
- Состояние разработки: последний релиз 4.0.6 от 2025-11-24, только 1.20.1 Forge и Fabric [C105]; после этой даты в ветке 1.20.1 нет коммитов, 73 открытых issue и 7 открытых PR [C151], [C104]; два прочитанных открытых обращения остались без ответа мейнтейнера [C137], [C150]. Ветка 1.21 — альфа под NeoForge 1.21.1, на Modrinth версии под 1.21 нет [C152]. Репозиторий не архивирован, заявления о прекращении разработки не найдено [C104]. Существуют сторонние форки под 1.21.1 [C153], [C155].
- AgriCraft описывает растения, сорняки, почвы, мутации и удобрения [C125]; для животных он ничего не даёт.

Что стоит взять как образец:

- У AgriCraft: пары аллелей с доминированием, случайная передача аллеля, мутация ±1, определение растений и мутаций через datapack-реестры [C108], [C115], [C116], [C125], [C126].
- У Forestry: Community Edition (LGPL-3.0, активен, 1.20.1 и альфа под 1.21.1 [C202], [C203], [C212]): хромосома несёт активный и неактивный аллель [C204], потомок получает по одному случайному аллелю от каждого родителя независимо по хромосомам [C205], экспрессию решает флаг доминантности аллеля [C206]; хромосомы типизированы (скорость, срок жизни, плодовитость, устойчивости) [C207]; мутация — два разных вида-родителя дают третий с базовым шансом и списком условий [C208], условия включают биом, температуру, влажность, время суток, блок-ресурс [C209]. В 3.0.0 виды пчёл вынесены в datapack [C211], а дроны по умолчанию сделаны гаплоидными ради упрощения селекции [C210]. API генетики лежит внутри основного jar, Maven-репозитория нет, между 1.20.1 и 1.21.1 API сломан [C213], [C215], [C216]; найденный кандидат на отдельную библиотеку, Nedelosk/Genetics, не получал push с 2019 года [C245].
- Productive Bees (All Rights Reserved [C217]) — пример упрощённой модели: у атрибута 3–4 дискретных уровня [C219], продуктивность потомка — случайное целое между значениями родителей, но не ниже собственного значения потомка, без доминирования и скрытых аллелей [C220]; гены — предметы с процентом чистоты, которые складываются до 100 [C221], [C222].
- Chickens: три стата 1–10 [C225]; стат потомка — взвешенное по strength среднее родителей плюс 1–2, в пределах 1–10 [C226]; статы растут только при одинаковой породе родителей и не повышаются при создании новой породы [C227].
- Gendustry: CE и Binnie's Genetics заменяют случайность машинами: выборка аллелей, шаблон, импринтинг [C230], [C233]; Mutatron убирает случайность мутаций совсем [C231]. Репозитории оригинальных Gendustry и Binnie's не получали push с 2018 и 2024 года, их ветки по умолчанию — 1.10.2 и 1.12 [C229].
- Mystical Agriculture в своём описании не называет признаков или скрещивания [C234]; утверждение «генетики нет, семена крафтятся из эссенций по тирам» в этом прогоне не подтверждено чтением источника [C235].

Что игроки ругают и хвалят (источники в основном про пчёл Forestry, 2014–2016 и одна запись 2026): случайность плюс время плюс микроменеджмент плюс инвентарь [C236], сундуки ненужных особей без целевого вида [C238]; машины Binnie и Gendustry перегибают в обратную сторону, давая выбрать мутацию, и нужен средний путь [C237]; один участник пишет, что бросает любую сборку, требующую пчёл, если в ней нет Gendustry [C240]; большинство хочет продукцию, а не селекцию [C242]; при этом другой участник играет в Forestry именно ради пчёл [C241]. В GTNH (2026) основная боль — первый шаблон генов, дальше новый вид занимает минуты [C243], [C244]. По Productive Bees отзывов игроков не найдено.

Животные:

- Genetic Animals — самая глубокая модель: диплоидное менделевское расщепление с половыми генами [C319], у коровы 64 аутосомных гена [C311]; генетически задаются количества продукции — шерсть, молоко, мясо и кожа [C306], [C307], [C312], [C313]; мод заменяет ванильных животных своими сущностями [C304]. Исходники открыты для чтения, но мод объявлен All Rights Reserved [C301], [C315], [C316], поэтому форк или заимствование кода без разрешения автора исключены. Есть ветки под 1.20.1 Forge и 1.21.1 NeoForge [C317]; запись на Modrinth обновлена 2026-11-20 [C302]. Породы заданы в Java-коде [C320]; datapack-вариант найден только как ветка, последний коммит которой называет свой JSON «гипотетическим» [C318].
- TerraFirmaCraft (EUPL-1.2 [C322]): у млекопитающих есть один наследуемый числовой признак `geneticSize` — среднее родителей плюс случайное от −3 до +3, с шансом карлика [C325]; он масштабирует выход при забое [C328]. Лошади наследуют здоровье, прыжок и скорость [C326]. В прочитанных файлах аллелей и доминирования нет; молоко, шерсть и яйца по коду не проверялись [C329]. Остальное — состояние особи: пол, стадии жизни, износ [C323], привыкание и беременность [C324].
- Мелкие и новые: Chromosome Lib — библиотека генетики под 1.20.1 (LGPL-2.1, 469 загрузок) [C332]; Animal Genetics (MIT, до 1.20.2, пять числовых наследуемых значений) [C331]; Animal Husbandry (All Rights Reserved, только 1.21.1) [C333]; DragN's Livestock Overhaul (All Rights Reserved, 1.20.1, глубина не прочитана) [C334]; Modern Chickens (MIT) [C335]; Realistic Horse Genetics (LGPL 3+, только лошади) [C330]. Ванильные лошади уже наследуют статы [C338].
- Готовой открытой базы для животных с глубиной Genetic Animals нет: глубокая модель закрыта лицензией, открытые — мелкие.

Связь признака с продукцией:

- В прочитанных модах наследуемые признаки меняют тип продукта, скорость или количество либо шанс уровня качества; ни один прочитанный источник не описывает наследуемое числовое значение на предмете-продукте или рецепт с минимумом такого значения [C437]. Это вывод из отсутствия находок; скрипты сборок индексируются плохо, так что отсутствие — слабое свидетельство. В GTNH вид пчелы задаёт продукт [C424], скорость — только шанс за цикл [C425], а гейтинг идёт в обратную сторону: тех-уровень открывает вид [C426]. Гайд по Regrowth не называет рецептов, проверяющих статы семян [C433].
- Ближайшие прецеденты. Quality Food хранит качество в NBT существующих предметов как 4 уровня [C405], [C406]; на качество урожая влияет блок грядки — настраиваемый бонус или штраф [C410]; у животных есть частично наследуемый «потенциал», повышающий шанс качественной продукции [C408]. TerraFirmaCraft проверяет в рецептах состояние, записанное на стаке, включая числовой минимум (температура) [C427], [C428], и переносит свойства через рецепты модификаторами выхода [C429], [C430]. В Productive Bees предмет-ген несёт числовую чистоту, и игроки отбирают его по порогу трубой с NBT-фильтром — это логистика, не рецепт [C436].
- Сама проверка порога дешёвая: свой `Ingredient` на Forge 1.20.1 [C403], `ICustomIngredient` на NeoForge 1.21.1 с примером порогового ингредиента в документации [C413], `Ingredient.customNBT` в KubeJS 1.20.1 [C420]; встроенные NBT- и компонентные ингредиенты проверяют только равенство [C402], [C412]; итог — [C438].
- Цена лежит вне рецепта: стаки с разными значениями не сливаются [C407]; Quality Food держит отдельные mixin под машины чужих модов [C411], а машины Create 6, по сообщению пользователя, качество не применяют и не сохраняют [C417]; хранилища могут терять или дублировать его [C418]; автокрафт AE2 требует фиксированных данных выхода [C435]; рецепт чужого типа может игнорировать NBT входа (Thermal, 2021) [C422]; итог — [C439]. Подтверждений для Create, Mekanism и GregTech на 1.20.1 не найдено — это пробел.
- Следствие для дизайна (вывод из [C407], [C406], [C438], [C439]): значение на био-материале стоит хранить несколькими дискретными уровнями, а не непрерывным числом, и принимать, что каждая чужая машина в цепочке потребует своей совместимости. Взаимная зависимость «рецепт принимает био-материал выше порога признака» в найденных модах прецедента не имеет — это новая механика, а не сборка из готового.

Блокировка ванильной еды:

- TerraFirmaCraft отключает ванильные рецепты файлами-переопределениями с всегда ложным условием [C514]; в его дереве 230 файлов в `data/minecraft/recipes/` и 44 пути в `data/minecraft/loot_tables` [C515]; мгновенный рост от костной муки отключён флагом конфига [C516]; еда всегда портится [C513].
- TerraFirmaGreg-Modern убирает дроп с ванильных животных отдельным модификатором LootJS на каждую сущность [C517], опустошает сундуки структур таблица за таблицей [C518] и вручную перечисляет таблицы, добавленные другими модами [C519]; правила спавна чужого мода отключает переопределением его biome modifier [C528].
- Примитивы на 1.20.1: `removeLoot` по фильтру [C520], модификатор на целый тип лута [C521], готовый `ItemFilter.FOOD` [C522]; `ItemEvents.modification` в KubeJS снимает или переписывает свойства еды [C525], [C526]; `forge:remove_spawns` убирает сущности из спавна биомов [C527]; MoreJS убирает торговлю жителей [C537].
- Известные дыры: LootJS не закрывает моды, которые кладут предметы прямо в таблицы [C523]; в форме callback у `foodProperties` на 1.20.1 есть открытый дефект [C538]. Один модификатор на тип лута с `ItemFilter.FOOD` выразим документированным API и избавил бы от ручного списка, но в выпущенной сборке такого не найдено, и закрывает ли он все утечки, здесь не проверено [C524].

Одомашнивание:

- TerraFirmaCraft: привыкание растёт от кормления [C501], [C509], убывает ежедневно без кормления [C502], [C511]; взрослые не достигают полного привыкания [C503]; спаривание выше 30% [C504], стрижка — только у взрослых и достаточно привыкших [C505]; в конфиге по умолчанию у животных стоят числа 0,35 и 0,15, которые сборщик прочитал как потолок взрослых и порог продукции, но имён этих аргументов в процитированном файле нет [C510]; детёныш получает половину привыкания матери или 90% при её 0,9 и выше [C508]. Это состояние особи, поднимаемое кормлением, а не наследуемый признак [C512]. Животные изнашиваются [C507].
- Cattlestrophic!: бинарное правило — заспавненные животные дикие, рождённые в неволе домашние; дикие хуже размножаются и дают меньше [C535].
- Одомашнивание за несколько поколений описано у Fauna and Ecology, но мод только под 1.12.2, последняя версия — бета 2021 года [C534]. Мода под 1.20.1 с наследуемым значением «ручности» поиск не нашёл (запросы — в Disconfirming evidence). У Genetic Animals ручность, по сниппету, только в планах [C536]. Domestication Innovation — про питомцев, не про наследование [C529], [C337].
- Следствие: ручность как наследуемый признак линии не занята ни одним живым модом под 1.20.1; образец кормления, порогов и износа — TFC.

Условия, при которых вывод меняется:

- Если первая версия ограничивается растениями и не требует значения признака на продукте, а важна скорость выхода, допустим datapack плюс небольшой аддон к AgriCraft 4.0.6: новые целочисленные статы, по чтению кода, регистрируются без форка [C123]. Цена — плагин под каждый харвестер [C134] и зависимость от мода без коммитов в ветке 1.20.1 с 2025-11-24 [C151].
- Если цель — 1.21.1, довод против AgriCraft усиливается: опубликованной версии под 1.21 нет [C105], [C152].
- Если мейнтейнер AgriCraft вернётся (PR #1710 под 1.21.1 открыт без ревью [C152]) и добавит совместимость с харвестерами, стоимость аддона падает; вывод по животным это не меняет.
- Если автор Genetic Animals даст письменное разрешение, для животных возможна зависимость от его мода; сейчас это All Rights Reserved [C315].
- Chromosome Lib (LGPL-2.1) [C332] может оказаться готовым движком наследования; её API в этом прогоне не читался, загрузок 469 — вопрос открыт.

Что меняется относительно `docs/research-baseline.md`:

- «AgriCraft активно поддерживается» — после релиза 4.0.6 от 2025-11-24 в ветке 1.20.1 нет коммитов [C151].
- «Харвестеры Industrial Foregoing не работают» — с 4.0.6 поддержка добавлена [C139]; в декабре 2025 пользователь пишет, что Plant Gatherer недавно включён, и просит добавить Hydroponic Bed [C141].
- «Farmer's Delight не входит в систему из коробки» — определения растений Farmer's Delight и Mystical Agriculture добавлены в 4.0.3 [C145]; в июле 2024 мейнтейнер писал, что поддержки ещё нет [C144].
- «Конфиг не синхронизируется на сервер» — подтверждения не найдено; есть открытое сообщение о сбросе конфига между запусками [C150].
- Baseline не упоминал отдельный мод-исправление для рецептов, которым нужны ванильные семена пшеницы [C149].

## Findings

Формулировки утверждений оставлены на английском, как их записали сборщики в момент чтения страницы: перевод после чтения добавил бы расхождений с цитатой.

### Q1 — AgriCraft как база: лицензия, архитектура, состояние, ограничения — answered with a gap


*Licence*

- **C101** [quoted] AgriCraft's LICENSE (branch 1.20.1) is MIT, copyright 2014 InfinityRaider, granting modify, distribute and sublicense rights — "Copyright (c) 2014 InfinityRaider" + "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell" — [S1], lines 1-8
- **C102** [measured] GitHub API and Modrinth API both report licence MIT for AgriCraft — Own measurements, M1, M2
- **C103** [measured] Each listed 4.0.3-4.0.6 version declares exactly one required dependency (Modrinth project id M1953qlQ) — Own measurements, M2

*Status*

- **C104** [measured] Repo not archived. Last commit: master 2023-08-08, branch 1.20.1 2025-11-24 (Ketheroth), branch 1.21 2025-04-06 — Own measurements, M1
- **C105** [measured] Modrinth: 4.0.6 (2025-11-24) and 4.0.3-4.0.5 (2024-11) are 1.20.1 Forge + Fabric only; the project record lists game versions 1.18.2, 1.20, 1.20.1, 1.20.4 (no 1.21) and loaders fabric, forge, neoforge — Own measurements, M2
- **C106** [quoted] README lists only 1.20.1 as supported — "| 1.18    | Unsupported |" — [S2], Available versions
- **C151** [measured] 73 open issues, 7 open PRs; no commit on 1.20.1 after 2025-11-24 (last commit there 2025-11-24, by Ketheroth) — Own measurements, M1
- **C152** [measured] Branch 1.21 = Minecraft 1.21.1, NeoForge 21.1.80, version 4.0.3-alpha; Modrinth lists no 1.21 version (M2); PR #1710, titled [1.21.1] Update to NeoForge 21.1.255 and port fixes from 1.20.1, is open with 0 reviews since 2026-10-04 — Own measurements, M6
- **C153** [quoted] A third-party 1.21.1 NeoForge fork "AgriCraft Reloaded" exists and is named as a requirement by a compat addon — "A small compatibility addon for AgriCraft Reloaded and Create on NeoForge 1.21.1." — [S3], body
- **C155** [measured] Fork coredex-source/AgriCraft-Reloaded (MIT, default branch 1.21.1), last push 2026-06-23 — Own measurements, M5

*Genome*

- **C108** [quoted] A gene pair has a dominant and a recessive allele; the expressed trait is the dominant one — "return this.getDominant().trait();" — [S4], getTrait
- **C109** [quoted] For stat genes the higher integer is dominant — "return this.trait >= other.trait();" — [S5], IntAllele.isDominant
- **C110** [quoted] The genome class is fixed to one String species pair plus a map of Integer stat pairs — "private final AgriGenePair<String> species;" + "private final Map<String, AgriGenePair<Integer>> stats;" — [S6], fields
- **C111** [quoted] Built-in stats are gain, growth, strength, resistance, fertility, mutativity, with min/max from config — "this.gain = new AgriStat(\"gain\", () -> StatsConfig.gainMin, () -> StatsConfig.gainMax, () -> StatsConfig.gainHidden, 0xff0000ff);" — [S7], constructor
- **C112** [quoted] Default stat range is 1 to 10 — "public static int gainMin = 1;" + "public static int gainMax = 10;" — [S8], gain_min, gain_max
- **C113** [quoted] Stat alleles are integers from min to max; values are clamped; stored in NBT as ints dom and rec — "int val = Mth.clamp(value, this.stat.getMin(), this.stat.getMax());" + "stat.putInt(\"dom\", dominant.trait());" — [S5], getAllele, writeToNBT

*Inheritance and mutation*

- **C115** [quoted] Each parent passes one of its two alleles per gene at random — "return random.nextBoolean() ? pair.getDominant() : pair.getRecessive();" — [S9], pickRandomAllele
- **C116** [quoted] Each passed stat allele may then shift by +1 or -1, with odds set by the parent's mutativity — "int delta = random.nextInt(max) < (max + statValue) / 2 ? 1 : -1; return gene.getAllele(allele.trait() + delta);" — [S10], DefaultStatMutator
- **C118** [quoted] Species mutation: a matching parent1+parent2 mutation succeeds with its `chance` — "if (mutation.chance() > random.nextDouble()) {" — [S10], DefaultPlantMutator.evaluate

*Addon API*

- **C120** [quoted] A generic gene interface exists — "public interface AgriGene<T> extends AgriRegistrable" — [S11], declaration
- **C121** [quoted] Gene and stat registries have a public add(), reachable via AgriApi.getGeneRegistry / getStatRegistry — "public boolean add(T object) {" — [S12], add
- **C122** [quoted] Genome loading iterates the stat registry and takes the Integer gene with the same id — "for (AgriStat stat : AgriStatRegistry.getInstance()) {" + "AgriGeneRegistry.getInstance().getGeneStat(stat).ifPresent(gene -> stats.add(gene.readFromNBT(genes)));" — [S6], fromNBT
- **C123** [inference] Without forking, an addon can add a new INTEGER stat (AgriStat + GeneStat in the two registries) and the stock genome code will carry, inherit and save it. A gene of another type (float, enum, multi-locus) has no slot in AgriGenome and needs a fork or mixin. Read from source, not run — from [C110], [C113], [C121], [C122]
- **C124** [quoted] Crossbreed engine and the single global stat mutator are replaceable through public setters — "public void setActiveStatMutator(AgriGeneMutator<Integer> mutator) {" — [S10], setters
- **C125** [quoted] Plants, weeds, soils, mutations, fertilizers are datapack registries — "ResourceKey.createRegistryKey(new ResourceLocation(AgriApi.MOD_ID, \"plants\"));" — [S13], lines 36-40
- **C126** [quoted] Docs: 4.0 is customised by datapack plus resourcepack — "This guide will help you customize AgriCraft 4.0 using a datapack and a resourcepack." — [S14], intro
- **C127** [quoted] Plant JSON fields include stages, harvest_stage, growth_chance, spread_chance, products, clip_products, requirement, modifiers; nothing per-plant about stats or genes — "Codec.INT.fieldOf(\"harvest_stage\").forGetter(plant -> plant.harvestStage)," — [S15], CODEC, lines 37-51
- **C128** [measured] No path under /api/ in the 1.20.1 tree (761 paths) contains the word event — Own measurements, M9
- **C129** [measured] Code search for AgriStatRegistry.getInstance().add returned only AgriCraft's own repository — Own measurements, M7

*Crop block, harvesting, seeds*

- **C130** [quoted] All crops are one block class extending plain Block with a block entity; blockstate has stick variant, crop state, light and no age — "public class CropBlock extends Block implements EntityBlock, BonemealableBlock, SimpleFluidloggedBlock {" — [S16], lines 61-84
- **C131** [quoted] Genome and growth stage live in the block entity — "private AgriGenome genome;" + "private AgriGrowthStage growthStage = null;" — [S17], lines 52-55
- **C132** [quoted] Player harvest is a right-click calling crop.harvest — "crop.harvest(itemStack -> spawnItem(level, pos, itemStack), player);" — [S16], use()
- **C133** [quoted] Create 1.20.1's harvester recognises a ripe crop by instanceof vanilla CropBlock + isMaxAge, else by an integer age property — "if (state.getBlock() instanceof CropBlock crop) {" + "return crop.isMaxAge(state);" — [S18], isValidCrop
- **C134** [inference] Cause of harvester breakage: generic harvesters detect crops by vanilla CropBlock type or an age blockstate and break the block; AgriCraft's crop is a plain Block with maturity in a block entity and its own harvest call, so every harvester needs a dedicated plugin or mixin. No maintainer statement of this was found — from [C130], [C131], [C132], [C133]
- **C135** [quoted] A third-party patch fixes Create by mixing into its harvester and calling AgriCraft's block entity — "if (world.getBlockEntity(pos) instanceof CropBlockEntity crop) {" — [S19], onVisitNewPosition
- **C137** [quoted] User report on 4.0.5 with only IF, Immersive Engineering, Thermal Cultivation: nothing harvests or plants AgriCraft crops (open, no maintainer reply) — "No machines can harvest AgriCraft crops." — [S20], issue body, 2025-03-03
- **C138** [quoted] Same thread: Create harvester, Actually Additions farmer, IF Plant Gatherer, IE cloche all fail — "None of those automation methods seem to harvest Agricraft-altered seeds/crops, but they do harvest non-Agricraft crops" — [S20], comment by vixianne, 2025-08-23
- **C139** [quoted] 4.0.6 added Industrial Foregoing and PneumaticCraft drone support — "- ADDED: Industrial Foregoing support (PiotrO15)" — [S21], 1.20.1-4.0.6
- **C140** [quoted] That IF support is an explicit plugin calling AgriCraft's API — "AgriApi.getCrop(level, blockPos).ifPresent(crop -> crop.harvest(drops::add, null));" — [S22], doHarvestOperation
- **C141** [quoted] A 2025-12-17 user request says AgriCraft recently enabled harvesting with PneumaticCraft drones and the Industrial Foregoing Plant Gatherer, and asks to add the IF Hydroponic Bed — "would it be possible to add the Industrial Foregoing **Hydroponic Bed** at the same time?" — [S23], issue body, 2025-12-17
- **C142** [measured] Plugin directories in the 1.20.1 tree: industrialforegoing, minecraft, pneumaticcraft only; no Create, Cyclic, Immersive Engineering or Farmer's Delight plugin directory — Own measurements, M9
- **C144** [quoted] Maintainer, 2024-07: Farmer's Delight not yet supported on 1.20 — "farmer's delight is not yet supported on 1.20 (but was on earlier minecraft version, hence the note on the curseforge page)" — [S24], comment by Ketheroth
- **C145** [quoted] 4.0.3 added Farmer's Delight and Mystical Agriculture plant definitions — "- ADDED: Farmer's Delight plants compatibility (PiotrO15)" — [S21], 1.20.1-4.0.3
- **C147** [quoted] Seeds with non-identical genes, hidden ones included, do not stack — "If these genes aren't identical, the seeds won't stack." — [S25], Why won't some plant seeds stack
- **C149** [quoted] A separate fix mod exists because AgriCraft seeds break recipes needing vanilla wheat seeds — "1.16, 1.18, and 1.20.1 versions of AgriCraft break crafting for other mods which require vanilla wheat seeds." — [S26], body
- **C150** [quoted] Open unanswered report on 4.0.6: config resets to defaults between launches — "the JSONC file keeps regenerating between launches of the pack" — [S27], issue body, 2026-11-11

### Q2 — Другие модели генетики (Forestry, Gendustry, Binnie's, Mystical Agriculture, Productive Bees и др.) — answered with a gap


*Forestry / Forestry: Community Edition*

- **C201** [quoted] Forestry CE is a fork started because the original Forestry maintainers went inactive after 1.13 — "This fork began after all of Forestry's maintainers eventually went inactive after the 1.13+ flattening." — [S28], intro
- **C202** [measured] ForestryMC/ForestryMC default branch is mc-1.12, last push 2025-04-22; thedarkcolour/ForestryCE default branch 1.21.1, last push 2026-11-09; the GitHub API reports the licence of both as NOASSERTION — Own measurements, M3
- **C203** [measured] Modrinth lists forestry-community-edition as LGPL-3.0-only, game versions 1.12.2, 1.19.2, 1.20.1, 1.21.1, loaders forge and neoforge, updated 2026-11-09, about 52k downloads — Own measurements, M3
- **C204** [quoted] In Forestry CE each chromosome of a genome holds a pair of an active (expressed) and an inactive allele — "A pair of an active allele and an inactive allele for one chromosome" — [S29], class javadoc
- **C205** [quoted] Offspring get, per chromosome, one randomly chosen allele from each parent's pair (50/50 active or inactive), independently per chromosome — "Allele<V> firstChoice = rand.nextBoolean() ? this.active : this.inactive;" — [S29], inheritOther
- **C206** [quoted] Which of the two inherited alleles is expressed is decided by a per-allele dominance flag: the first dominant allele becomes active; two recessives leave the first one active — "A pair where the active allele is the first dominant allele and the inactive allele is the other." — [S29], create / activeOf
- **C207** [quoted] The bee karyotype has species as one chromosome among typed trait chromosomes (speed float, lifespan int, fertility int, tolerances, effect, territory…) — "public static final IChromosome<Float> SPEED = ChromosomeFactory.floatChromosome(forestry(\"speed\"));" — [S30], field list
- **C208** [quoted] A mutation is two different parent species giving a third, with a base chance and a list of conditions — "Represents a mutation between two species (in any order) into a third species." — [S31], interface javadoc; getChance, getConditions
- **C209** [measured] Forestry CE ships mutation condition classes for biome, cave, daytime, humidity, temperature, required resource block and time-limited — Own measurements, M10
- **C210** [quoted] Forestry CE 3.0.0 made haploid drones the default to simplify breeding — "Haploid Drones option now enabled by default (drones no longer have Inactive alleles, simplifying breeding)" — [S32], Forestry 3.0.0
- **C211** [quoted] Forestry CE 3.0.0 moved bee species to datapacks; trees and butterflies partly — "Bees are now data-driven (trees and butterflies are too, to a lesser extent)" — [S32], Forestry 3.0.0
- **C212** [quoted] The 1.21.1 line is still alpha (3.0.0-alpha9 is the top changelog entry) and it exposes addon API hooks for generating species JSON — "Add `createSpeciesProvider` to bee, tree, and butterfly species types, so addons can generate species JSON from the API alone" — [S32], Forestry 3.0.0-alpha9
- **C213** [quoted] Forestry CE has no Maven repository; addon developers depend on CurseForge or Modrinth artifacts — "The Maven repository is not available yet. Use CurseForge or Modrinth's artifacts." — [S28], For Mod Developers
- **C214** [quoted] Before the 2026 redesign, alleles were interned singletons in a global registry that froze at startup; the maintainers called that incompatible with datapacks — "fundamentally incompatible with runtime datapacks and clean client sync" — [S33], 1. Background & motivation
- **C215** [quoted] The redesign replaced registry alleles with inline values plus a dominance flag, with no save migration (clean break) — "Replace interned singleton alleles with **plain inline values + a dominance flag**" — [S33], 2. Goals
- **C216** [inference] The genetics API lives inside the main Forestry jar (package forestry.api.core.genetics under src/main) and is generic over species types (karyotype, chromosome, allele pair, mutation); it is not published as a standalone library and the 1.21.1 redesign is a clean break from 1.20.1 — from [C213], [C215], [C204]

*Productive Bees*

- **C217** [measured] Productive Bees repo LICENSE file is the single line "All Rights Reserved"; Modrinth lists LicenseRef-All-Rights-Reserved, versions up to 1.20.1 and 1.21.1, loaders forge and neoforge, Modrinth page updated 2026-01-13, repo pushed 2026-11-04 — Own measurements, M4
- **C218** [quoted] Productive Bees has six gene attributes: type, productivity, endurance, temper, behavior, weather tolerance — "WEATHER_TOLERANCE(5, \"weather_tolerance\");" — [S34], enum GeneAttribute
- **C219** [quoted] Each attribute has 3 or 4 discrete levels (e.g. productivity normal/medium/high/very high mapped to 0..3), not a continuous number — "PRODUCTIVITY_VERY_HIGH(4, \"productivity.very_high\", 3)," — [S35], enum GeneValue
- **C220** [quoted] On breeding, the child's productivity is the larger of the child's own value and a uniform random integer between the two parents' values: no dominance, no hidden alleles — "int parentProductivity = Mth.nextInt(newBee.level().random, Math.min(prod1, prod2), Math.max(prod1, prod2));" — [S36], setOffspringAttributes
- **C221** [quoted] An extracted gene item carries a purity percentage, rolled 15–54 by default — "return getStack(attribute, value, count, ProductiveBees.random.nextInt(40) + 15);" — [S37], getStack
- **C222** [quoted] Gene items of the same attribute and value are merged by adding purities, capped at 100 with the remainder returned as a leftover gene — "var combineStack = Gene.getStack(geneGroup.attribute(), geneGroup.value(), 1, Math.min(100, purity));" — [S38], mergeGenes
- **C223** [quoted] New bee species in Productive Bees are obtained through datapack breeding recipes or item conversion recipes — "Next you can make breeding recipes or item conversion recipes, depending on how you want to obtain the bee." — [S39], Adding or removing bees
- **C224** [measured] Productive Bees has Gene Indexer classes (GeneIndexer.java, GeneIndexerBlockEntity.java, GeneIndexerContainer.java, GeneIndexerScreen.java) in its source tree — Own measurements, M10

*Other inheritance models*

- **C225** [quoted] Chickens gives each chicken three stats on a 1–10 scale: growth, gain, strength — "Every stat is on scale from 1 (the lowest) to 10 (the highest)." — [S40], Chicken stats
- **C226** [quoted] Chickens child stat is the strength-weighted average of the parents' stats plus a random +1 or +2, clamped to 1..10 — "int newStatValue = (stat1 * thisStrength + stat2 * mateStrength) / (thisStrength + mateStrength) + mutation;" — [S41], calculateNewStat
- **C227** [quoted] In Chickens, stats rise only when both parents are the same breed; stats are not increased when a new breed is created — "Please make a note, that stats are not-increased when a new breed is created." — [S40], Chicken stats
- **C228** [quoted] Complicated Bees is a Forestry-inspired bee genetics mod with data-driven species — "A bee genetic mod inspired by forestry bees for modern versions of minecraft." — [S42], intro; licence and versions from gh api + Modrinth API

*Gendustry, Binnie's Mods, Mystical Agriculture*

- **C229** [measured] Original Gendustry (bdew-minecraft/gendustry): default branch mc1.10.2, last push 2018-08-16, no licence detected by the GitHub API; ForestryMC/Binnie: default branch master-MC1.12, last push 2024-08-09, licence NOASSERTION — Own measurements, M5
- **C230** [quoted] Gendustry: CE (GPL-3.0-only, Forge 1.20.1 only on Modrinth, updated 2026-01-05) samples random alleles into gene samples, combines them into a template and imprints the template onto an organism — "**Sampler**: Extracts random alleles from organisms to create Gene Samples, which can be combined into a Genetic Template" — [S43], body, machine list
- **C231** [quoted] Gendustry's Mutatron removes mutation RNG entirely; the advanced one lets the player pick the result — "Triggers a mutation between two organisms with 100% success rate." — [S43], body, Mutatron
- **C232** [quoted] Binnie's Botany adds flower breeding where colour is a heritable trait (80 colours, 46 species) and soil pH and moisture must be maintained — "The colour of the flowers can be transferred and selectively bred, meaning that each flower can be one of a possible 80 different colours." — [S44], Botany
- **C233** [quoted] Binnie's Genetics lets genes be isolated, sequenced and inoculated into other individuals, for bees, trees, butterflies and flowers — "genes can be isolated, sequenced and then inoculated back into different species, making it easier to obtain the perfect genome" — [S44], Genetics
- **C234** [quoted] Mystical Agriculture (MIT, Modrinth versions through 1.21.1 and 26.1.2, updated 2026-10-22) describes itself as growing essences from resource crops; its description names no traits or breeding — "Use crops to grow essences used to make materials, mob drops, tools, armor and possibly even more!" — [S45], body, first paragraph
- **C235** [prior] Mystical Agriculture seeds are crafted per resource from tiered essences (inferium upward) with no inheritance; IC2 crops used three hidden stats growth/gain/resistance (0–31) with crossbreeding on crop sticks, the model AgriCraft adapted; players commonly call Mystical Agriculture grindy and overpowered — not fetched in this run

*Player-side praise and complaints*

- **C236** [quoted] A 2016 FTB forum poster calls Forestry-only bee breeding RNG plus time sink plus micromanagement plus inventory trouble — "It combines RNG mechanics with massive time sinks as you have to micromanage the process and leaves you with the worst inventory management problems." — [S46], post-1581335
- **C237** [quoted] The same poster says Binnie/Gendustry machines overcorrect by letting the player choose the mutation, and asks for a middle ground — "the machines they add give the ability to simply pick your desired mutation" — [S46], post-1581335
- **C238** [quoted] The thread opener reports three days of breeding and chests of unwanted bees without the target species — "I have over 3 double chests full of useless" — [S46], post-1580951
- **C239** [quoted] A defender in the thread concedes repeat playthroughs turn breeding into a chore — "Done it once or twice already and it becomes a chore" — [S46], post-1591357
- **C240** [quoted] One poster says any pack that requires bees makes him quit unless Gendustry is present — "Unless Gensustry is there to cheese through it" — [S46], post-1582375
- **C241** [quoted] Counter-view in the same thread: one poster plays Forestry specifically for the bees — "I play Forestry specifically for the bees" — [S46], post-1589877
- **C242** [quoted] A 2014 FTB poster says most players want bee products, not breeding — "They just want to have the resources the bees give them." — [S47], thread page 1
- **C243** [quoted] A GTNH contributor (2026) says the main pain is building the first gene template, after which a new species takes minutes — "the biggest pain point, when getting into bees is building your initial gene template" — [S48], comment by C0bra5
- **C244** [quoted] The 2026 GTNH issue (open) is titled as a tedium complaint and lists scarce speed-up frames and biome requirements as friction — "Breeding bees is riddled with points of frictions" — [S48], issue body
- **C245** [measured] Nedelosk/Genetics (MIT) was last pushed 2019-03-28 — Own measurements, M5

### Q3 — Моды с генетикой, породами и одомашниванием животных — answered with a gap


*Genetic Animals (mokiyoki)*

- **C301** [quoted] Genetic Animals is listed on Modrinth under an All-Rights-Reserved licence id, with no source URL in the project record (source_url is null). — "LicenseRef-All-Rights-Reserved" — [S49], JSON field license.id (source_url: null), read 2026-11-23
- **C302** [quoted] On Modrinth Genetic Animals lists game versions up to 1.20.1 and only the Forge loader; the project record was updated 2026-11-20. — "updated":"2026-11-20T01:12:20.829993Z" — [S49], JSON fields updated, game_versions (…1.18.2, 1.20, 1.20.1), loaders (forge)
- **C303** [quoted] Genetic Animals covers sheep, cows, mooshrooms, pigs, rabbits, chickens, llamas, turtles, axolotls and mooblooms, and describes the variation as based on real genetics. — "variation for sheep, cows, mooshrooms, pigs, rabbits, chickens, llamas, turtles, axolotls and even mooblooms that are all based on real genetics" — [S49], JSON field body
- **C304** [quoted] Genetic Animals replaces the vanilla animals with its own genetically driven entities (by default vanilla spawns are replaced with genetic variants). — "By default, vanilla animal spawns are replaced with their genetic variants." — [S50], Getting Started (wikitext via api.php)
- **C305** [quoted] Its genes are inherited by offspring, and the stated breeding targets are colours, size, shape and loot. — "unique genes that can be inherited by offspring allowing the player to selectively breed for colors, size, shape, and loot" — [S50], intro
- **C306** [quoted] Production traits vary between individual sheep: wool yield ranges from almost none to 3 blocks per day, and hair sheep drop leather instead of wool. — "Some wool sheep will make almost no wool while others will make 3 blocks of wool per day." — [S50], Animals - Sheep
- **C307** [quoted] Milk is a quantity trait in items: milk comes in six bucket volumes, and a cow gives up to five buckets after birth. — "comes in six different bucket volumes and two bottle volumes" — [S50], Items - Milk
- **C308** [quoted] Beyond genetics the mod adds hunger, grazing, sleeping, genders and pregnancy (mammals fall pregnant instead of spawning a baby at once). — "If the animal is a mammal they will fall pregnant rather than instantly spawning a baby." — [S50], Getting Started
- **C309** [quoted] The wiki describes the mod as still in beta; its infobox gives first release February 3, 2019 and lists 1.20.1 as newest Minecraft version (wiki infobox last says version 0.8.25, July 8, 2023 - the wiki may lag the mod). — "is a mod for 1.16.4+ Minecraft Forge that is still in '''beta'''" — [S50], intro and infobox
- **C310** [quoted] Villages hold a few pure breeds each, and animals found in the wild tend to have traits adapted to their environment or endemic to the biome — "Villages will have a few pure breeds per village, explore the world to find more breeds." — [S50], Getting Started
- **C311** [quoted] The Genetic Animals cow has 64 autosomal genes and an XY gender type; the model is locus/allele based (named loci such as Extension with up to five alleles, Agouti with six, with dominance stated per allele). — "gender_type=XY|number_of_genes=64 autosomal genes" — [S51], infobox template and Genes table (wikitext via api.php)
- **C312** [quoted] In Genetic Animals cows can be selectively bred for production: amounts of beef, leather and milk. — "cows can be selectively bred to produce varying amounts of beef, leather, and milk" — [S51], Genetics
- **C313** [quoted] Cow drops depend on body shape: wider cows give more beef, narrower more leather; drop range 0-6 beef — "Wider cows tend to produce more beef, while narrower cows tend to produce more leather." — [S51], Drops
- **C314** [quoted] The wiki does not document the full genome: gene recording is stated as in progress and only a sample of genes is listed. — "Gene recording is in progress, the following is only a small sampling of the genes available in the mod" — [S51], Genes
- **C315** [quoted] Genetic Animals' source is publicly readable on GitHub (SaemonMoki/ExtendedAnimals, package mokiyoki.enhancedanimals), but the mod declares itself All Rights Reserved in gradle.properties on both the 1.20 and 1.21 branches. — "mod_license=All Rights Reserved" — [S52], gradle.properties, branches 1.20 and 1.21 (gh api, 2026-11-23)
- **C316** [quoted] LICENSE.txt in the repo is the Forge MDK's licence text (LGPL 2.1 for Forge itself) — "Unless noted below, Minecraft Forge, Forge Mod Loader, and all" — [S53], first line of LICENSE.txt
- **C317** [quoted] The 1.20 branch targets Minecraft 1.20.1 on Forge 47.4.20, and the 1.21 branch targets 1.21.1 on NeoForge 21.1.265 — "minecraft_version=1.21.1" — [S54], gradle.properties on branch 1.21 (neo_version=21.1.265); branch 1.20 has minecraft_version=1.20.1, forge_version=47.4.20
- **C318** [quoted] A branch 1.18_datapack_breed_gen exists whose last commit (2026-06-05) calls its breed/gene JSON hypothetical — "made some hypothetical json for breeds/genes" — [S55], commit 142e6e17 message
- **C319** [quoted] Genome storage is two int arrays (sex-linked and autosomal) read as allele pairs; a gamete is built by a 50/50 coin flip per pair, i.e. diploid Mendelian segregation, with sex-linked genes and an optional linkage table (only one link, for chickens, at 0.97). — "linkages.add(new GeneLink(48, 62, 0.97F));" — [S56], getGamite() lines 223-256, getLinkages() line 381
- **C320** [quoted] Breeds are defined in Java code (init/breeds/CowBreeds.java etc.) as allele presets per gene index, not in datapack JSON; some gene slots hold wide numeric ranges, i.e. quantitative values stored as alleles. — "public static final Breed HEREFORD = new Breed(new Breed.Properties().setData("Hereford", 0.6F, 0.6F, Breed.Rarity.ORDINARY)" — [S57], class CowBreeds; JERSEY sketch has .add(100, "150-222")
- **C321** [quoted] A user-reported complaint: freezing and crashing when logging in near the mod's animals on a server (issue opened 2022-03-26, closed) — "when I log in next to mod animals it freezes and crashes my game" — [S58], issue body (gh api)

*TerraFirmaCraft*

- **C322** [quoted] TerraFirmaCraft is licensed EUPL-1.2 (Modrinth record and repo LICENSE.txt); Modrinth lists versions for 1.18.2, 1.20.1 and 1.21.1 and the record was updated 2026-11-12. — "European Union Public Licence" — [S59], first line; Modrinth API /v2/project/terrafirmacraft license.id "EUPL-1.2"
- **C323** [quoted] TFC livestock have gender and three life stages; an animal becomes old after breeding or being used enough times (the "wear" mechanic), after which it is only good for meat. — "After they breed or are used enough times, animals become $(thing)old$(), and are only useful for their meat." — [S60], field guide entry, page 2
- **C324** [quoted] Familiarity is raised by feeding, decays daily, and gates breeding: mammals mate above 30% familiarity when fed that day, and the female becomes pregnant for a set number of days. — "Adult mammals that are above 30% familiarity and have been fed that day will mate" — [S60], Mammals page
- **C325** [quoted] TFC 1.20 does have one heritable numeric trait for mammals: geneticSize, set in the baby to the parents' mean plus a random -3..+3, with a 1-in-20 runt chance — "baby.setGeneticSize(Mth.floor(EntityHelpers.getIntOrDefault(tag, "size", 16) / 2d + Mth.nextInt(baby.getEntity().getRandom(), -3, 3)));" — [S61], applyGenes(), createGenes() lines 93-118
- **C326** [quoted] TFC horses additionally inherit max health, jump strength and movement speed from both parents (vanilla-style offspring attribute blending). — "speed = EntityHelpers.createOffspringAttribute(tag.getDouble("movementSpeed1"), tag.getDouble("movementSpeed2"), MIN_MOVEMENT_SPEED, MAX_MOVEMENT_SPEED, getEntity().getRandom());" — [S62], applyGenes() line 103
- **C327** [quoted] Familiarity is partly passed to the baby (half of the mother's, or 90% if she is at 0.9+) - an inherited state, not a genetic trait; baby gender is a coin flip. — "baby.setFamiliarity(this.getFamiliarity() < 0.9F ? this.getFamiliarity() / 2.0F : this.getFamiliarity() * 0.9F);" — [S63], setBabyTraits() line 459
- **C328** [quoted] geneticSize is production-relevant: kill-drop yield scales with it (plus player attack damage and familiarity above 0.5). — "float adjustedSize = properties.getGeneticSize();" — [S64], getFloat()
- **C329** [inference] In the TFC 1.20 files read here the heritable model is one blended scalar (size, affecting drops) plus horse attributes, with no alleles or dominance; DairyAnimal/WoolyAnimal were not read line by line and 1.21.x was not checked, so milk, wool and eggs are not covered — from [C325], [C326], [C328]

*Others*

- **C330** [quoted] Realistic Horse Genetics (horses only): code LGPL 3+, assets CC-BY 4.0; Modrinth lists 1.20.1-1.20.4 on Forge/NeoForge, last updated 2024-10-28; real size loci (LCORL, HMGA2) and size changes carrying capacity and base health. — "Everything else is licensed under LGPL 3 or later." — [S65], license.txt; wiki Size.md; Modrinth API realistic-horse-genetics
- **C331** [quoted] Animal Genetics (MIT, datapack-style, 1.19.2-1.20.2, last updated 2024-12-18, ~5k downloads) gives many animals five numeric heritable "genomes" - speed, strength, health, jump, visual - with mutation; strength scales size and drop count (needs Pehkui). — "There are 5 genomes in each animal" — [S66], JSON field body
- **C332** [quoted] Chromosome Lib (LGPL-2.1, Fabric/Forge 1.20-1.20.1, 469 downloads) is a genetics library with registries for chromosomes, loci, genes and traits and automatic inheritance with recombination and mutation; it provides no functionality on its own — "Five custom registries for chromosomes, gene loci, genes, traits, and trait types." — [S67], JSON field body
- **C333** [quoted] Animal Husbandry (ARR, Fabric/NeoForge, 1.21.1 only, updated 2026-11-18, ~7.7k downloads) is a 2026 newcomer with inherited production traits. — "Breed animals with inherited genetics, including traits, color,   pattern, yield, fertility, growth, and constitution." — [S68], JSON field body
- **C334** [quoted] DragN's Livestock Overhaul (ARR, source visible, Forge 1.18.2/1.20.1, updated 2026-08-25, ~115k downloads) advertises genetics, and genders that affect by-product output; depth not read. — "-New animal colors, patterns and genetics" — [S69], JSON field body
- **C335** [quoted] Chicken stat breeding survives as Modern Chickens (MIT, Forge/NeoForge 1.20.1 and 1.21.1, port of Chickens + Roost): three numeric stats, with a data-driven roster. — "view Growth, Gain, and Strength" — [S70], JSON field body
- **C336** [quoted] Genetics: Resequenced (LGPL-3.0, 1.20.1/1.21.1; remake of Genetics Reborn / Advanced Genetics) is a different concept: genes are scraped from mobs and infused as powers, not bred. — "scrape mobs for their Genes" — [S71], JSON field body
- **C337** [quoted] Domestication Innovation (GPL-3.0, up to 1.20.1, last updated 2023-11-29) is about pet behaviour and taming, not heredity. SWEM is ARR and stops at 1.18.2 on Modrinth. — "Improves upon and compliments tameable mobs, both from vanilla and other mods." — [S72], JSON field description; /v2/project/swem for SWEM
- **C338** [quoted] Vanilla horses and donkeys already inherit stats: the foal's stats are the parents' average plus random variation. — "the foal's stats are determined by averaging both parent's stats and adding a random variation to it" — [S73], Bred values (wikitext via api.php)
- **C339** [prior] Animania (breeds per species, needs, no genetics) stopped at 1.12.2/partial 1.16; not on Modrinth under that slug (404). Vanilla llama strength, rabbit/cat/axolotl/panda variants are inherited too. — not fetched in this run

### Q4 — Связь признаков с продукцией и гейтинг рецептов по значению признака — answered with a gap


*Mechanism: Forge 1.20.1*

- **C401** [quoted] Forge 1.20.x ships a strict NBT ingredient (type forge:nbt) that requires exact equality of item, damage and share tag — "compare the item, damage, and the share tags (as defined by IForgeItem#getShareTag ) on an ItemStack for exact equivalency. This can be used by specifying the type as forge:nbt" — [S74], StrictNBTIngredient
- **C402** [quoted] Forge 1.20.x partial NBT ingredient (forge:partial_nbt) checks only the listed keys for equivalency, i.e. equality and not a numeric range — "Checks only for equivalency on 'key1' and 'key2'" — [S74], PartialNBTIngredient
- **C403** [quoted] On Forge 1.20.x a mod can define its own ingredient with an arbitrary test by subclassing Ingredient and registering an IIngredientSerializer — "Custom ingredients can be created by implementing IIngredientSerializer for the created Ingredient subclass." — [S74], Creating Custom Ingredients
- **C404** [quoted] A Forge custom ingredient that looks at the stack tag must report isSimple false — "Returns false if the ingredient matches on the stack" — [S74], Ingredient Subclass table, isSimple

*Quality-carrying item mods*

- **C405** [quoted] Quality Food stores quality on existing items as NBT rather than adding items per tier — "adds quality to food and certain food-related material (e.g. crops) through NBT data (meaning no new items)" — [S75], body, General
- **C406** [quoted] Quality Food quality is a 4-level integer (0 none to 3 diamond) under a quality_food NBT compound — "`quality` is a value between `0` (`NONE`) and `3` (`DIAMOND`)" — [S75], body, after Animals
- **C407** [quoted] In Quality Food an item with a quality tag does not stack with the same item without it — "just makes the item unstackable with non-quality items" — [S75], body, after Animals
- **C408** [quoted] Quality Food has a semi-heritable animal stat: feeding quality food raises an animal's potential, offspring inherit part of it, and potential raises the chance of quality loot, milk and eggs (a chance, not a deterministic value) — "Baby animals will gain (using default values) ~ 60% to 90% of the parents' potential" — [S75], body, Animals
- **C409** [quoted] Quality Food propagates quality through crafting by config lists: the result keeps the lowest common quality of the ingredients — "If three items are `gold` quality and two are `diamond` the result will be `gold`" — [S75], body, crafting configs
- **C410** [quoted] Quality Food has a farmland configuration that gives crop quality a bonus (or penalty) based on the farm block the crop is planted on — "There is a farmland configuration which allows you to define a bonus (can also be negative, i.e. 0.5) based on the farm block the crop is planted on" — [S75], body, Configuration
- **C411** [quoted] Quality Food's repository (branch 1.20.1) contains per-mod mixin classes for other mods' machines (Create: fan processing, basin, millstone, saw, harvester; Farmer's Delight: stove, cooking pot, cutting board) — "src/main/java/de/cadentem/quality_food/mixin/create/RecipeApplierMixin.java" — [S76], gh search code, repo file path

*Mechanism: NeoForge 1.21.1*

- **C412** [quoted] NeoForge 1.21.1 DataComponentIngredient matches item plus data components, strictly (exact) or partially (listed components equal, others allowed) - equality, not a range — "Strict matching means the data components must match exactly, while partial matching means the data components must match, but other data components may also be present." — [S77], Built-In Ingredient Types
- **C413** [quoted] NeoForge 1.21.1 docs show a threshold-style custom ingredient (item tag plus minimum enchantment levels) built on ICustomIngredient - the same shape as a minimum-trait ingredient — "let's make an enchanted item ingredient that accepts an item tag and a map of enchantments to min levels" — [S77], Custom Ingredient Types
- **C414** [quoted] For a non-simple NeoForge custom ingredient the displayed item list is only approximate, so recipe viewers show example stacks rather than the true accepted set — "If not, this should be as exact as possible, but doesn't need to be super accurate." — [S77], Custom Ingredient Types, getItems comment

*Adversarial: Quality Food issue tracker (NBT/component quality vs automation)*

- **C415** [quoted] On 1.21.1 Quality Food moved quality to a data component (quality_food:quality with level and type) — "The actual quality data component which gets attached to items" — [S78], data component example
- **C416** [quoted] Quality Food author on filtering quality with Create attribute filters: not possible unless Create itself supports filtering the NBT — "If Create does not support filtering NBT data then that's that" — [S79], owner comment
- **C417** [quoted] User report: Create 6 machines neither apply nor keep quality (mixer output without quality, harvester strips it) — "mechanical mixer does not apply quality to mixed foods and harvesters strip quality when they harvest seeds" — [S80], issue body
- **C418** [quoted] User report: a compacting drawer of another mod turns quality crops into non-quality crates and back, so quality is lost or duplicated — "the Compacting Drawer will compact crops with quality into crop crates that have no quality" — [S81], issue body
- **C419** [quoted] User report: hopper extraction from a furnace bypassed quality assignment until the author patched it — "pulling food out of a furnace automatically via a hopper causes the output to never get an assigned quality" — [S82], issue body

*Mechanism: KubeJS 1.20.1 (branch 2001)*

- **C420** [quoted] KubeJS for 1.20.1 exposes Ingredient.customNBT(parent, predicate): a pack script can make an ingredient whose NBT must satisfy an arbitrary JS predicate (so a numeric threshold is expressible without a Java mod) — "Make a custom ingredient where an item must match both the parent ingredient and the item's nbt must match the custom predicate function" — [S83], customNBT @Info
- **C421** [quoted] KubeJS 1.20.1 also has strongNBT and weakNBT ingredient constructors on its platform helper (exact vs subset NBT match) — "Ingredient weakNBT(ItemStack item);" — [S84], lines 49-51

*Adversarial: foreign machines and NBT-sensitive ingredients*

- **C422** [quoted] A Thermal machine recipe type (Magma Crucible, 2021 report) picked recipes without regard to input NBT, and the thread says this is intended by Thermal - a foreign recipe type may not honour NBT-distinct inputs — "Actually, according to #221 this is intended by Thermal" — [S85], Spiker985 comment
- **C423** [quoted] In KubeJS 1.19 plain Item.of ingredients ignore NBT unless the script opts in per ingredient — "You need to add .weakNBT() after it, as all ingredient are ignoreNBT() by default in 1.19" — [S86], ChiefArug comment

*Trait -> product in breeding mods; pack precedents*

- **C424** [quoted] In GTNH (Forestry bees) the species determines which product item appears, as a per-cycle chance — "Species-specific outputs. For example, Majestic bees have a 30% chance of generating a Dripping Comb" — [S87], bee life cycle section
- **C425** [quoted] In GTNH the bee speed trait enters only the probability of producing a comb per cycle, i.e. rate, not a property of the comb — "P(X) = Bee Speed x Production Modifier x Comb Chance." — [S87], Comb calculation
- **C426** [quoted] GTNH gates in the opposite direction: breeding certain species needs a foundation block under the apiary, so tech tier gates the species (and the species then yields the resource) — "These are for bee tiering - otherwise you could breed the Infinity bee in steam age" — [S87], Foundation Requirements

*Precedent: TerraFirmaCraft 1.20 (item state tested by recipes)*

- **C427** [quoted] TFC 1.20 ships a custom ingredient that gates a recipe input on a property stored on the food stack (a food trait such as brined) — "This is an ingredient which only accepts food items if they have a specific trait." — [S88], Has Trait
- **C428** [quoted] TFC 1.20 ships a numeric-threshold ingredient: heatable items accepted only at or above a minimum temperature carried by the stack — "`min_temp`: Optional integer. The minimum temperature this item must have, in degrees Celsius." — [S88], Heatable
- **C429** [quoted] TFC propagates stack properties through recipes with output modifiers that copy them from the input (food expiry and traits; also a forging bonus) — "copy the food properties (expiration date, food traits) from the input stack" — [S89], Copy Food
- **C430** [quoted] TFC stores a forging bonus as NBT on the stack and copies it through recipes — "copy the Forging bonus, which is recorded as a NBT tag on the stack, from the input stack" — [S89], Copy Forging Bonus
- **C431** [quoted] Genetic Animals cows can be selectively bred for the quantity of products (beef, leather, milk) — "cows can be selectively bred to produce varying amounts of beef, leather, and milk" — [S90], Appearance section (wikitext via api.php, read at audit)
- **C432** [quoted] AgriCraft's Gain stat changes only how many fruits a plant yields; no AgriCraft stat is described as a property of the fruit item — "Gain: affects the number of fruits that the plant will produce;" — [S91], body, stats list
- **C433** [quoted] Regrowth player guide treats 10/10/10 AgriCraft stats as a breeding and yield practice; the guide names no recipe that checks seed stats — "Do not use seeds less than 10/10/10 for breeding!" — [S92], line 68

*Adversarial: AE2 and NBT-distinct items*

- **C434** [quoted] AE2 1.20.1 handles NBT-distinct items in filters via a Fuzzy Card that can ignore NBT entirely (all-or-nothing, no threshold on a value) — "Fuzzy cards let devices and tools with filters filter by damage level and/or ignore item NBT" — [S93], Fuzzy Card
- **C435** [quoted] AE2 autocrafting expects a pattern's output to have fixed NBT/components; results whose data varies (as a per-line quality value would) are not supported and the maintainer calls a change a lot of work — "All of ae2 code in a lot of places assumes there the crafting result has fixed components (what used to be NBT)." — [S94], Mari023 comment, issue open

*Counter-example found by disconfirming search*

- **C436** [quoted] Productive Bees: a bred-derived item (gene sample) carries a numeric NBT value (purity) and players select it by threshold with an NBT-filtering pipe; this is filtering in logistics, not a recipe that requires a minimum — "Item: productivebees:gene NBT: {productivebees_gene_purity:100}" — [S95], Optional Step, Pipez NBT filtering
- **C437** [inference] Across the sources read here, heritable traits change product type (bee species), rate or count (bee speed, AgriCraft Gain, Genetic Animals cow product amounts) or the chance of a quality tier (Quality Food potential); no source read describes a heritable numeric value on the product item or a recipe requiring a minimum of such a value — from [C408], [C424], [C425], [C431], [C432], [C433]
- **C438** [inference] The threshold test itself is cheap on both targets (Forge custom Ingredient, NeoForge ICustomIngredient, KubeJS customNBT); the built-in NBT/component ingredients only test equality, so a numeric minimum needs a custom ingredient or discrete tiers — from [C402], [C403], [C412], [C413], [C420]
- **C439** [inference] The cost sits outside the recipe test: value-distinct stacks do not merge, foreign machines and storage may drop, ignore or mis-handle the value unless patched per machine, and AE2 autocrafting needs fixed output data — from [C407], [C411], [C416], [C417], [C418], [C422], [C435]

### Q5 — Блокировка ванильной еды и одомашнивание диких животных в сборках — answered with a gap


*(b) TerraFirmaCraft domestication (1.20.x branch, read via raw.githubusercontent.com)*

- **C501** [quoted] In TFC livestock gain familiarity by being fed their preferred foods by the player (sneak + use). — "Livestock can be fed to raise $(thing)familiarity$(). Each animal has foods that it prefers to eat." — [S96], page 4
- **C502** [quoted] TFC familiarity decays daily if the animal is not fed, until it is raised past a threshold. — "Familiarity decays a little each day if not fed. Raising the familiarity enough prevents decay" — [S96], page 6
- **C503** [quoted] In TFC adult livestock cannot reach full familiarity; only babies can reach 100%. — "Adult livestock cannot be fully familiarized, indicated by the red outlined heart. Babies can reach 100% familiarity." — [S96], page 7
- **C504** [quoted] TFC mammals mate only above 30% familiarity and when fed that day. — "Adult mammals that are above 30% familiarity and have been fed that day will mate" — [S96], page 8
- **C505** [quoted] TFC gates shearing on the animal being adult and familiar enough. — "can be $(thing)Sheared$() if they are adults and familiar enough to you" — [S96], page 9
- **C506** [quoted] TFC equines become rideable after 15% familiarity. — "They become rideable after reaching 15% familiarity." — [S96], page 13
- **C507** [quoted] TFC animals wear out: after breeding or being used enough times they become old and are useful only for meat. — "After they breed or are used enough times, animals become $(thing)old$(), and are only useful for their meat." — [S96], page 2
- **C508** [quoted] In TFC code a baby inherits familiarity from the mother: half of hers if she is below 0.9, otherwise 90% of hers. — "baby.setFamiliarity(this.getFamiliarity() < 0.9F ? this.getFamiliarity() / 2.0F : this.getFamiliarity() * 0.9F);" — [S97], setBabyTraits, line 459
- **C509** [quoted] In TFC code each feeding adds 0.06 familiarity, clamped to the adult cap for non-children. — "float familiarity = getFamiliarity() + 0.06f;" — [S97], lines 224-231
- **C510** [quoted] TFC's default config builds each animal with 0.35 as the first numeric argument and, for producing animals such as the cow, 0.15 as the last; the cited file does not name these arguments (the collector read them as adult familiarity cap and minimum familiarity for products) — "cowConfig = ProducingMammalConfig.build(builder, \"cow\", 0.35, 192, 128, true, 58, 2, 24000, 0.15);" — [S98], line 679
- **C511** [quoted] TFC familiarity stops decaying above a configurable limit, default 30%. — "Familiarity value above which familiarity no longer will decay. Default is 0.3, or 30%." — [S98], line 706
- **C512** [inference] TFC familiarity is partly carried across generations (a baby starts at half of the mother's value, or 90% of it when she is at 0.9 or more, and babies can reach 100%), but it is a per-animal state raised by feeding, not a genetic trait: adults cannot be fully familiarized, so each generation must still be hand-fed while young. — from [C503], [C508], [C509]

*(a) TerraFirmaCraft food*

- **C513** [quoted] TFC food always expires; rotten food restores no hunger and may give Hunger or Poison. — "Food will $(thing)expire$() over time, turning rotten. Rotten food will not restore any hunger" — [S99], page 1
- **C514** [quoted] TFC removes vanilla recipes (e.g. beetroot soup) by shipping an override file in data/minecraft/recipes whose only content is an always-false Forge condition. — "\"type\": \"forge:false\"" — [S100], whole file
- **C515** [measured] The TFC 1.20.x repo tree holds 230 files under data/minecraft/recipes/ and 44 paths under data/minecraft/loot_tables (overrides of vanilla data). — Own measurements, M8
- **C516** [quoted] TFC disables vanilla bonemeal instant growth by default through a server config flag. — "enableVanillaBonemeal = builder.comment(\"If vanilla bonemeal's instant-growth effect should be enabled.\").define(\"enableVanillaBonemeal\", false);" — [S98], line 645

*(a) TerraFirmaGreg-Modern precedent (KubeJS + LootJS, repo HEAD read 2026-11-23)*

- **C517** [quoted] TerraFirmaGreg-Modern strips all drops from vanilla animals (chicken, cow, pig, sheep, rabbit, cod, salmon and others) with one LootJS entity loot modifier per entity. — "event.addEntityLootModifier('minecraft:cow').removeLoot(ItemFilter.ALWAYS_TRUE)" — [S101], registerMinecraftLoots, lines 5-45
- **C518** [quoted] TerraFirmaGreg-Modern empties vanilla structure chests table by table (dungeon, mineshaft, shipwreck, every village house and others), including the butcher chest. — "event.addLootTableModifier('minecraft:chests/village/village_butcher').removeLoot(ItemFilter.ALWAYS_TRUE)" — [S101], lines 72-111
- **C519** [quoted] The same script also has to empty loot tables that other mods add (chalk, lootr, exposure), i.e. each added mod brings its own tables that must be listed by hand. — "event.addLootTableModifier('exposure:chests/village_plains_house').removeLoot(ItemFilter.ALWAYS_TRUE)" — [S101], lines 113-122

*(a) LootJS 1.20.1 wiki (cloned github.com/AlmostReliable/lootjs.wiki.git)*

- **C520** [quoted] LootJS removeLoot deletes every item matching an ItemFilter from the loot being generated. — "Removes all `items` from the current loot pool which matches the given [`ItemFilter`]." — [S102], removeLoot
- **C521** [quoted] LootJS on 1.20.1 can attach one modifier to whole loot types (BLOCK, ENTITY, CHEST, FISHING, GIFT) instead of single tables. — ".addLootTypeModifier(LootType.ENTITY) // or multiple LootType.BLOCK, LootType.CHEST ..." — [S102], addLootTypeModifier; Existing loot types
- **C522** [quoted] LootJS 1.20.1 ships a predefined ItemFilter.FOOD, shown in the wiki on a chicken entity modifier. — "event.addEntityLootModifier(\"minecraft:chicken\").functions(ItemFilter.FOOD, (f) => {" — [S102], functions
- **C523** [quoted] LootJS (Forge) can remove other mods' global loot modifiers, but this does not cover mods that put items directly into loot tables. — "This will not work if the mod adds their items directly into the loot tables." — [S102], removeGlobalModifier FORGE ONLY
- **C524** [inference] A single type-wide modifier (chest + fishing + entity + gift with removeLoot(ItemFilter.FOOD)) is expressible with the documented LootJS API and would avoid listing each table as TerraFirmaGreg does; not found in a shipped pack in this run, and whether it closes every leak is untested — from [C519], [C520], [C521], [C522]

*(a) KubeJS / Forge primitives*

- **C525** [quoted] KubeJS ItemEvents.modification can strip an existing item's food component by setting foodProperties to null (wiki example: pumpkin pie). — "item.foodProperties = null // make pumpkin pies inedible" — [S103], Food
- **C526** [quoted] The same KubeJS hook can rewrite an existing item's hunger and saturation instead of removing them. — "food.hunger(2)" — [S103], Food
- **C527** [quoted] Forge 1.20.1 has a stock biome modifier that removes entity types (id, list or tag) from the spawn lists of biomes (id, list or tag); its type id is forge:remove_spawns (the javadoc sample wrongly shows add_spawns). — "Stock biome modifier that removes mob spawns from a biome." — [S104], RemoveSpawnsBiomeModifier javadoc, lines 184-196
- **C528** [quoted] TerraFirmaGreg-Modern disables another mod's spawn rules by overriding that mod's biome modifier file with a no-op type. — "\"type\": \"forge:none\"" — [S105], whole file

*(b) Other mods (Modrinth API, project JSON)*

- **C529** [quoted] Domestication Innovation (Forge/NeoForge, up to 1.20.1) describes itself as an upgrade for already-tameable mobs, not a livestock domestication system. — "Improves upon and compliments tameable mobs, both from vanilla and other mods." — [S72], description
- **C530** [quoted] The mod "Animal Husbandry" (Modrinth slug animalhusbandry) domesticates animals by feeding preferred food and separately has inherited genetics for traits, colour, yield, fertility, growth and constitution; tameness is not in the inherited list. — "Breed animals with inherited genetics, including traits, color,   pattern, yield, fertility, growth, and constitution." — [S68], body, Features
- **C531** [quoted] That Animal Husbandry mod exists only for 1.21.1 (Fabric/NeoForge), not for 1.20.1. — "Domesticate animals by feeding them their preferred food." — [S68], body, Features; game_versions = ["1.21.1"]
- **C532** [quoted] Genetic Animals (Forge, 1.18.2-1.20.1) is a genetics-based replacement for vanilla livestock; its Modrinth description names no wild or tameness mechanic. — "Realism upgrade for Minecraft's livestock and other animals with a true to life genetics core." — [S49], description
- **C533** [quoted] Diet (Forge/Fabric/Quilt, up to 1.20.1) is a configurable food-group nutrition system — "A customizable food group-based nutrition and diet system." — [S106], description
- **C534** [quoted] Fauna and Ecology (CurseForge, 1.12.2 only, last file a 2021 beta) describes multi-generation domestication: animals are feral until tamed and offspring become domesticated after a few tame generations. — "after a few tame generations the newest offspring will be domesticated and become more useful and compliant" — [S107], description; versions key = 1.12.2 only
- **C535** [quoted] Cattlestrophic! (Forge/NeoForge/Fabric, 1.19.2-1.21.1) uses a binary, one-generation rule: naturally spawned animals are wild, bred offspring are domesticated; wild ones cannot be leashed, drop less and breed worse. — "Breeding animals will result in domesticated animals." — [S108], body, Wild vs. Domesticated Animals
- **C536** [snippet] Genetic Animals' CurseForge page reportedly lists tameness as planned, not implemented. — "tameness is not yet implemented" — [S109], search summary, page not read

*(a) Villager trades and a known KubeJS defect*

- **C537** [quoted] MoreJS (KubeJS addon) can remove all vanilla and all modded villager trades, optionally per profession and level; wanderer trades have their own event. — "Will remove all vanilla trades. You can also just remove them for specific professions." — [S110], Clear existing trades
- **C538** [quoted] Open KubeJS issue on 1.20.1 (build 2001.6.5-build.26): the foodProperties callback form can replace an item's existing food values with builder defaults instead of preserving them. — "the existing food properties can be replaced by a builder with default values instead of being preserved" — [S111], issue body, opened 2026-08-08, OPEN
- **C539** [snippet] Old TFC forum threads (2014-2016) call daily familiarity feeding monotonous and ask for a feeding trough. — "help with the monotony of raising animal familiarity" — [S112], search summary; curl blocked (60 bytes)

## Disconfirming evidence

Записи перенесены из леджеров без изменений.

- re C101: WebSearch `AgriCraft mod license CurseForge "MIT" OR "All Rights Reserved" InfinityRaider fork redistribute` -> nothing against; forks shown are MIT. CurseForge licence field itself not read.
- re C123: `gh search code` for `AgriStatRegistry.getInstance().add`, `AgriGeneRegistry.getInstance().add`, `getStatRegistry().add` -> 0 hits outside AgriCraft: no working third-party stat exists to confirm it [C129].
- re C134: WebSearch `AgriCraft 4.0 1.20.1 automation harvest Create harvester works OR "now works" OR compatible crops not harvested` -> no report of generic harvesters working unaided; only patch mods [C135], [C153]. Counter-claims: IF and PNC work since 4.0.6 [C139], [C141].
- re C151: looked for 2026 commits, merges, maintainer replies via gh -> none. Counter: not archived [C104], README says supported [C106]. No abandonment statement found.
- C216: "Forestry genetics API standalone library separate jar" → found Nedelosk/Genetics, stale since 2019 (C245); nothing maintained.
- C220/C223: "Productive Bees gene breeding too easy / no depth criticism" → no source on Productive Bees; only Forestry threads.
- C236–C240: counter-views found in the same sources: C241, C243.
- C315/C301 (GA is ARR, no fork/dependency grant). Query: "Genetic Animals" mokiyoki license open source fork permission modpack addon API. Back: third-party listing also says All Rights Reserved; a third-party add-on (genetic tests) exists; no permission statement found. Nothing against. Not checked: CurseForge licence field, Discord.
- "No mod offers extensible/data-driven numeric animal traits" - REFUTED in part. Query: Minecraft mod animal genetics API datapack custom traits livestock breeding inheritable stats 1.20.1. Back: C331, C332, C333, C334; also Wildlore (MIT, 1.21.1, 9 downloads). None is datapack-defined; GA's JSON was found only on a branch (C318).
- "TFC has no heritable trait" - REFUTED by code: gh code search getGeneticSize repo:TerraFirmaCraft/TerraFirmaCraft gives C325, C328.
- Re C437 (no mod gates recipes on a heritable trait value). Query: `modpack recipe requires seed with stats 10/10/10 AgriCraft OR "IC2 crop" OR chicken "10/10/10" as crafting ingredient NBT gate progression` -> only Regrowth breeding guides and ATM seed-recipe bugs; no stat-gated recipe (C433). Query: `minecraft mod recipe ingredient requires bee OR animal OR crop with minimum gene OR stat value ... Productive Bees OR Forestry OR "Mystical Agriculture" OR Roost` -> no such recipe; closest is Productive Bees gene purity carried as NBT and filtered by pipe (C436). Nothing found against; search engine coverage of pack scripts is thin, so absence is weak evidence.
- Re C437, nearest precedent in a non-breeding mod: TFC recipes do test stack-carried state incl. a numeric minimum (C427, C428) - the mechanism has shipped at scale, only not tied to heredity.
- Re C439 (NBT-distinct products hurt stacking/automation). Query: `Create mod filter "Respect Data" OR "Ignore Data" items with NBT data components attribute filter custom data; AE2 pattern NBT items exact match fuzzy card` -> counter-evidence partial: AE2 can ignore NBT in filters (C434) and Pipez can filter on an NBT value (C436), so storage/logistics is workable; autocrafting with variable output data is not (C435). Nothing found showing Create filters matching a custom numeric value.
- Re C422 (foreign recipe types ignoring NBT) rests on one 2021 Thermal thread; no 1.20.1 confirmation for Create, Mekanism or GregTech recipe types was found - gap.
- Re [C524], [C525] (KubeJS+LootJS can lock out vanilla food on 1.20.1) — query: `KubeJS 1.20.1 "foodProperties" null not working OR crash OR "still edible" ItemEvents.modification issue`. Came back: no report that `= null` fails; one open defect in the callback form [C538]; LootJS itself documents a hole for mods writing items straight into tables [C523]. No shipped pack found that removes food by ItemFilter.FOOD type-wide; TFG lists tables by hand [C517-C519].
- "No mod models multi-generation domestication" — query: `Minecraft mod domestication over generations wild animals tameness inherited offspring become tamer each generation`. Came back: counter-claim [C534] (1.12.2 only, beta), binary variant [C535]; nothing for 1.20.1 with a heritable tameness value. Modrinth API queries "tameness", "wild tame trust", "taming overhaul": 0 hits.
- Re [C512] (TFC familiarity is not multi-generation) — gh search issues TFC "familiarity": #685 "Animal's offspring don't inherit mother's familarity?" (closed), consistent with inheritance being intended [C508].

## Coverage

| Channel | Read | Gap |
|---|---|---|
| Users | Issue-треды AgriCraft (#1576, #1590, #1712, #1540), Quality Food (#1, #35, #56, #77), GTNH #25978, два треда форума FTB, Genetic Animals #88 | По Productive Bees и Genetic Animals отзывов почти нет; жалобы на селекцию в основном 2013–2016 годов; Discord и комментарии CurseForge не читались; Hacker News по теме пуст |
| Specialists | Исходники AgriCraft 1.20.1, Forestry CE, Productive Bees, Chickens, Genetic Animals, TerraFirmaCraft, Create; документация Forge, NeoForge, KubeJS (исходники ветки 2001 и wiki), LootJS, MoreJS, TFC, AE2; проектная спецификация Forestry CE | Исходники ветки AgriCraft 1.21, Binnie's и оригинального Gendustry, Chromosome Lib, DragN's Livestock Overhaul не читались; вики Genetic Animals сама называет описание генов неполным |
| Data | Собственные замеры по API GitHub и Modrinth (M1–M10) | Бенчмарков производительности и опросов игроков по теме не найдено и не искалось |
| Adversarial | Трекер Quality Food (качество в NBT против автоматизации), AE2 #7588, KubeJS #176 и #1171, тред «Bees in FTB are the worst addition ever», сообщения о неработающих харвестерах | Нет разборов от авторов сборок о блокировке еды; нет подтверждений для рецептов Create, Mekanism, GregTech на 1.20.1 |

Объявленные превышения потолка поиска: Q3 — 2 веб-поиска, около 11 поисков по API Modrinth и около 25 чтений API и raw-файлов (несколько модов в одном под-вопросе); Q4 — 6 веб-поисков при потолке 4 и около 7 чтений страниц при потолке 3. Q1, Q2, Q5 — в пределах потолка. Все пять леджеров остановлены по лимиту 16 000 знаков.

Сбои инструментов: поиск кода и issue через `gh` упёрся в rate limit в Q4; CurseForge закрыт Cloudflare; minecraft.wiki и форум TFC не отдали страницы; вики Genetic Animals на Fandom вернула 403 одному из сборщиков; страница документации Mystical Agriculture вернула 404. Использовались `curl`, `gh`, API Modrinth и встроенные поиск и чтение страниц; сторонних коннекторов не было.

Замеры: сборщики записали `measured`-утверждения без дословного вывода команд; основной поток повторил их одной серией (M1–M10). Расхождения с леджерами видны в выводе (аудит сузил [C142], [C155], [C202] до того, что показывает вывод; ниже — формулировки до аудита): в M9 каталогов плагинов три (industrialforegoing, minecraft, pneumaticcraft) — каталога Botania среди них нет, хотя [C142] называет Botania horn; в M5 дата последнего push форка AgriCraft-Reloaded — 2026-06-23, [C155] называет дату последнего коммита 2026-04-18; M3 показывает лицензию репозиториев Forestry как NOASSERTION по API GitHub, текст LGPL v3 в [C202] взят сборщиком из файла лицензии.

Сохранение: по условию задачи отчёт кладётся в `docs/research/` рабочего дерева без коммита и без PR — шаг скилла «Saved — a PR» не выполняется.

## Own measurements

Все команды только читают публичные API; выполнены основным потоком 2026-11-23 повтором замеров, которые сборщики записали в леджеры без дословного вывода. Скрипт: `measure.sh` в scratch-каталоге прогона.

### M1 — AgriCraft: лицензия, ветки, открытые issues и PR, коммиты после 4.0.6

Command: `gh api repos/AgriCraft/AgriCraft; gh api repos/AgriCraft/AgriCraft/commits?sha=<branch>&per_page=1; gh api search/issues?q=repo:AgriCraft/AgriCraft+is:issue+is:open (и is:pr); gh api repos/AgriCraft/AgriCraft/commits?sha=1.20.1&since=2025-11-25T00:00:00Z --jq length`

Output:

```
{"archived":false,"default_branch":"master","license":"MIT","pushed_at":"2025-11-24T14:10:09Z"}
master 2023-08-08T06:22:07Z AndyCat1811
1.20.1 2025-11-24T14:09:59Z Ketheroth
1.21 2025-04-06T14:17:25Z Ketheroth
open issues:
73
open PRs:
7
commits on 1.20.1 since 2025-11-25:
0
```

### M2 — AgriCraft на Modrinth: лицензия, версии, загрузчики, зависимости

Command: `curl -s https://api.modrinth.com/v2/project/agricraft; curl -s https://api.modrinth.com/v2/project/agricraft/version (первые 8 версий: номер, дата, game_versions, loaders, dependencies)`

Output:

```
agricraft | license: MIT | loaders: ['fabric', 'forge', 'neoforge'] | game_versions(last 6): ['1.18.2', '1.20', '1.20.1', '1.20.4'] | updated: 2025-11-24 | downloads: 31886 | source_url: https://github.com/AgriCraft/AgriCraft
4.0.6 2025-11-24 ['1.20.1'] ['forge'] [('M1953qlQ', 'required')]
4.0.6 2025-11-24 ['1.20.1'] ['fabric'] [('M1953qlQ', 'required')]
4.0.5 2024-11-13 ['1.20.1'] ['fabric'] [('M1953qlQ', 'required')]
4.0.5 2024-11-13 ['1.20.1'] ['forge'] [('M1953qlQ', 'required')]
4.0.4 2024-11-12 ['1.20.1'] ['fabric'] [('M1953qlQ', 'required')]
4.0.4 2024-11-12 ['1.20.1'] ['forge'] [('M1953qlQ', 'required')]
4.0.3 2024-11-12 ['1.20.1'] ['fabric'] [('M1953qlQ', 'required')]
4.0.3 2024-11-12 ['1.20.1'] ['forge'] [('M1953qlQ', 'required')]
```

### M3 — Forestry и Forestry CE: ветки, даты, лицензия

Command: `gh api repos/ForestryMC/ForestryMC; gh api repos/thedarkcolour/ForestryCE; curl -s https://api.modrinth.com/v2/project/forestry-community-edition`

Output:

```
{"default_branch":"mc-1.12","license":"NOASSERTION","pushed_at":"2025-04-22T01:46:38Z"}
{"default_branch":"1.21.1","license":"NOASSERTION","pushed_at":"2026-11-09T00:14:51Z"}
forestry-community-edition | license: LGPL-3.0-only | loaders: ['forge', 'neoforge'] | game_versions(last 6): ['1.12.2', '1.19.2', '1.20.1', '1.21.1'] | updated: 2026-11-09 | downloads: 52454 | source_url: https://github.com/thedarkcolour/ForestryMC/
```

### M4 — Productive Bees: файл LICENSE, дата, Modrinth

Command: `gh api repos/JDKDigital/productive-bees/contents/LICENSE --jq .content | base64 -d; gh api repos/JDKDigital/productive-bees; curl -s https://api.modrinth.com/v2/project/productivebees`

Output:

```
All Rights Reserved

{"pushed_at":"2026-11-04T21:02:50Z"}
productivebees | license: LicenseRef-All-Rights-Reserved | loaders: ['forge', 'neoforge'] | game_versions(last 6): ['1.19.3', '1.19.4', '1.20', '1.20.1', '1.21', '1.21.1'] | updated: 2026-01-13 | downloads: 276939 | source_url: https://github.com/JDKDigital/productive-bees
```

### M5 — Gendustry, Binnie, Nedelosk/Genetics, AgriCraft-Reloaded: ветка, дата, лицензия (порядок строк тот же)

Command: `gh api repos/bdew-minecraft/gendustry; gh api repos/ForestryMC/Binnie; gh api repos/Nedelosk/Genetics; gh api repos/coredex-source/AgriCraft-Reloaded`

Output:

```
{"default_branch":"mc1.10.2","license":null,"pushed_at":"2018-08-16T17:56:09Z"}
{"default_branch":"master-MC1.12","license":"NOASSERTION","pushed_at":"2024-08-09T11:44:53Z"}
{"license":"MIT","pushed_at":"2019-03-28T20:53:28Z"}
{"default_branch":"1.21.1","license":"MIT","pushed_at":"2026-06-23T17:44:21Z"}
```

### M6 — AgriCraft, ветка 1.21 и PR #1710

Command: `curl -s https://raw.githubusercontent.com/AgriCraft/AgriCraft/1.21/gradle.properties | grep -iE 'minecraft_version|neoforge_version|mod_version|^version'; gh pr view 1710 --repo AgriCraft/AgriCraft --json title,state,createdAt,reviews`

Output:

```
version=4.0.3-alpha
minecraft_version=1.21.1
neoforge_version=21.1.80
{"createdAt":"2026-10-04T16:26:35Z","reviews":0,"state":"OPEN","title":"[1.21.1] Update to NeoForge 21.1.255 and port fixes from 1.20.1"}
```

### M7 — Сторонний код, добавляющий стат в реестр AgriCraft

Command: `gh search code "AgriStatRegistry.getInstance().add" --limit 10 --json repository,path`

Output:

```
AgriCraft/AgriCraft src/main/java/com/infinityraider/agricraft/impl/v1/genetics/AgriGeneRegistry.java
(end of code search)
```

### M8 — TFC 1.20.x: число переопределений ванильных данных

Command: `gh api repos/TerraFirmaCraft/TerraFirmaCraft/git/trees/1.20.x?recursive=1 --jq '.tree[].path'; grep -c 'data/minecraft/recipes/'; grep -c 'data/minecraft/loot_tables'`

Output:

```
data/minecraft/recipes/: 230
data/minecraft/loot_tables: 44
```

### M9 — AgriCraft 1.20.1: дерево файлов, каталоги плагинов, события в api

Command: `gh api repos/AgriCraft/AgriCraft/git/trees/1.20.1?recursive=1 --jq '.tree[].path'; grep -oE 'plugin/[a-z]+' | sort -u; grep -i '/api/' | grep -ci event`

Output:

```
paths: 761
plugin dirs:
plugin/industrialforegoing
plugin/minecraft
plugin/pneumaticcraft
api paths containing 'event': 0
```

### M10 — Forestry CE: классы условий мутации; Productive Bees: Gene Indexer

Command: `gh api repos/thedarkcolour/ForestryCE/git/trees/1.21.1?recursive=1 | grep -oE 'MutationCondition[A-Za-z]*\.java'; gh api repos/JDKDigital/productive-bees/git/trees/dev-1.21.0?recursive=1 | grep -oE 'GeneIndexer[A-Za-z]*\.java'`

Output:

```
MutationCondition.java
MutationConditionBiome.java
MutationConditionCave.java
MutationConditionDaytime.java
MutationConditionHumidity.java
MutationConditionRequiresResource.java
MutationConditionTemperature.java
MutationConditionTimeLimited.java
MutationConditionType.java
MutationConditionTypes.java
GeneIndexer.java
GeneIndexerBlockEntity.java
GeneIndexerContainer.java
GeneIndexerScreen.java
```

## Open questions

- Q1 — ledger cap — Расширяемость генома AgriCraft не проверена запуском: нужен минимальный аддон, регистрирующий стат в `AgriStatRegistry`, и проверка, что стат наследуется и сохраняется.
- Q1 — no new information — Причина поломки харвестеров — вывод из кода AgriCraft и Create; харвестеры Cyclic, Immersive Engineering, Mystical Agriculture не читались, заявления мейнтейнера нет.
- Q1 — blocked: CurseForge behind Cloudflare, Discord not read — Статус разработки AgriCraft и качество форков под 1.21.1 (AgriCraft Reloaded, ReReloaded) не установлены.
- Q1 — ledger cap — Схемы JSON продуктов и требований роста не читались: неизвестно, может ли продукт зависеть от значения стата через JSON.
- Q2 — ledger cap — Mystical Agriculture и IC2 crops остались `prior`; модели Resourceful Bees и Complicated Bees, механика Binnie's Botany не прочитаны из исходников; специфика деревьев и бабочек Forestry не цитировалась.
- Q2 — no new information — Отзывы игроков о Productive Bees и о селекции вне пчёл Forestry не найдены.
- Q3 — blocked: CurseForge behind Cloudflare — Лицензионное поле Genetic Animals на CurseForge и позиция автора о зависимостях и аддонах не проверены; выпущена ли сборка под 1.21, не подтверждено.
- Q3 — ledger cap — Как гены Genetic Animals отображаются в молоко, шерсть и мясо, по коду не прослежено; API Chromosome Lib и глубина DragN's Livestock Overhaul, Animal Husbandry не читались. Это следующий прогон, если рассматривается зависимость от готового движка наследования.
- Q4 — no new information — Учитывают ли типы рецептов Create, Mekanism, GregTech на 1.20.1 NBT-чувствительные и собственные ингредиенты; как JEI и EMI показывают такие ингредиенты. Решается локальным экспериментом: рецепт с собственным ингредиентом в каждой машине тестовой сборки.
- Q4 — no new information — Отсутствие прецедента гейтинга рецептов по наследуемому признаку — слабое свидетельство: данные квестов и рецептов GTNH и скрипты других сборок не читались.
- Q5 — ledger cap — Блокировка еды одним модификатором на тип лута не проверена; не исследованы рыбалка, костная мука и семена из травы вне TFC, еда с мобов других модов; Spice of Life, питание TFC, Untamed Wilds, Naturalist, Alex's Mobs не читались. Решается экспериментом в тестовой сборке.
- Q5 — blocked: TFC forum refuses fetch, CurseForge behind Cloudflare — Жалобы игроков на одомашнивание в TFC и статус ручности в Genetic Animals остались сниппетами.
- Q1–Q3 — audit — Часть деталей в утверждениях не видна в записанном выводе замеров или на цитируемой странице и убрана: дата последнего комментария мейнтейнера AgriCraft и список обращений без ответа, аддон на mixin (Roll54), Botania horn и PR #1550, тексты файлов лицензий Forestry, Gendustry, Binnie, даты слияний в ветках Genetic Animals. Решается повтором замера с дословным выводом (`gh api` по issue/PR и файлам LICENSE).
- Q5 — audit — Смысл чисел 0,35 и 0,15 в конфиге TFC (потолок привыкания взрослых, порог для продукции) в процитированном файле не назван. Решается чтением сигнатуры `build()` конфиг-классов животных TFC.

## Sources

- [S1] https://raw.githubusercontent.com/AgriCraft/AgriCraft/1.20.1/LICENSE — passage
- [S2] https://raw.githubusercontent.com/AgriCraft/AgriCraft/1.20.1/README.md — passage
- [S3] https://api.modrinth.com/v2/project/agricraft-create-compat-by-kuki — passage
- [S4] https://raw.githubusercontent.com/AgriCraft/AgriCraft/1.20.1/common/src/main/java/com/agricraft/agricraft/api/genetic/AgriGenePair.java — passage
- [S5] https://raw.githubusercontent.com/AgriCraft/AgriCraft/1.20.1/common/src/main/java/com/agricraft/agricraft/api/genetic/GeneStat.java — passage
- [S6] https://raw.githubusercontent.com/AgriCraft/AgriCraft/1.20.1/common/src/main/java/com/agricraft/agricraft/api/genetic/AgriGenome.java — passage
- [S7] https://raw.githubusercontent.com/AgriCraft/AgriCraft/1.20.1/common/src/main/java/com/agricraft/agricraft/api/stat/AgriStatRegistry.java — passage
- [S8] https://raw.githubusercontent.com/AgriCraft/AgriCraft/1.20.1/common/src/main/java/com/agricraft/agricraft/api/config/StatsConfig.java — passage
- [S9] https://raw.githubusercontent.com/AgriCraft/AgriCraft/1.20.1/common/src/main/java/com/agricraft/agricraft/api/genetic/AgriCrossBreedEngine.java — passage
- [S10] https://raw.githubusercontent.com/AgriCraft/AgriCraft/1.20.1/common/src/main/java/com/agricraft/agricraft/api/genetic/AgriMutationHandler.java — passage
- [S11] https://raw.githubusercontent.com/AgriCraft/AgriCraft/1.20.1/common/src/main/java/com/agricraft/agricraft/api/genetic/AgriGene.java — passage
- [S12] https://raw.githubusercontent.com/AgriCraft/AgriCraft/1.20.1/common/src/main/java/com/agricraft/agricraft/api/AgriRegistry.java — passage
- [S13] https://raw.githubusercontent.com/AgriCraft/AgriCraft/1.20.1/common/src/main/java/com/agricraft/agricraft/api/AgriApi.java — passage
- [S14] https://raw.githubusercontent.com/AgriCraft/agricraft.github.io/master/docs/customization/index.md — passage
- [S15] https://raw.githubusercontent.com/AgriCraft/AgriCraft/1.20.1/common/src/main/java/com/agricraft/agricraft/api/plant/AgriPlant.java — passage
- [S16] https://raw.githubusercontent.com/AgriCraft/AgriCraft/1.20.1/common/src/main/java/com/agricraft/agricraft/common/block/CropBlock.java — passage
- [S17] https://raw.githubusercontent.com/AgriCraft/AgriCraft/1.20.1/common/src/main/java/com/agricraft/agricraft/common/block/entity/CropBlockEntity.java — passage
- [S18] https://raw.githubusercontent.com/Creators-of-Create/Create/mc1.20.1/dev/src/main/java/com/simibubi/create/content/contraptions/actors/harvester/HarvesterMovementBehaviour.java — passage
- [S19] https://raw.githubusercontent.com/ACCBDD/agricraft-create-compat/main/src/main/java/com/accbdd/agricreatecompat/mixins/CreateHarvesterPatch.java — passage
- [S20] https://github.com/AgriCraft/AgriCraft/issues/1576 — passage
- [S21] https://raw.githubusercontent.com/AgriCraft/AgriCraft/1.20.1/CHANGELOG.md — passage
- [S22] https://raw.githubusercontent.com/AgriCraft/AgriCraft/1.20.1/forge/src/main/java/com/agricraft/agricraft/common/plugin/industrialforegoing/AgriCraftPlantRecollectable.java — passage
- [S23] https://github.com/AgriCraft/AgriCraft/issues/1590 — passage
- [S24] https://github.com/AgriCraft/AgriCraft/issues/1540 — passage
- [S25] https://raw.githubusercontent.com/AgriCraft/agricraft.github.io/master/docs/wiki/faq.md — passage
- [S26] https://api.modrinth.com/v2/project/agricraft-seed-fix — passage
- [S27] https://github.com/AgriCraft/AgriCraft/issues/1712 — passage
- [S28] https://raw.githubusercontent.com/thedarkcolour/ForestryCE/1.21.1/README.md — passage
- [S29] https://raw.githubusercontent.com/thedarkcolour/ForestryCE/1.21.1/src/main/java/forestry/api/core/genetics/alleles/AllelePair.java — passage
- [S30] https://raw.githubusercontent.com/thedarkcolour/ForestryCE/1.21.1/src/main/java/forestry/api/core/genetics/alleles/BeeChromosomes.java — passage
- [S31] https://raw.githubusercontent.com/thedarkcolour/ForestryCE/1.21.1/src/main/java/forestry/api/core/genetics/IMutation.java — passage
- [S32] https://raw.githubusercontent.com/thedarkcolour/ForestryCE/1.21.1/changelog.md — passage
- [S33] https://raw.githubusercontent.com/thedarkcolour/ForestryCE/1.21.1/docs/superpowers/specs/2026-06-27-allele-foundation-design.md — passage
- [S34] https://raw.githubusercontent.com/JDKDigital/productive-bees/dev-1.21.0/src/main/java/cy/jdkdigital/productivebees/util/GeneAttribute.java — passage
- [S35] https://raw.githubusercontent.com/JDKDigital/productive-bees/dev-1.21.0/src/main/java/cy/jdkdigital/productivebees/util/GeneValue.java — passage
- [S36] https://raw.githubusercontent.com/JDKDigital/productive-bees/dev-1.21.0/src/main/java/cy/jdkdigital/productivebees/util/BeeHelper.java — passage
- [S37] https://raw.githubusercontent.com/JDKDigital/productive-bees/dev-1.21.0/src/main/java/cy/jdkdigital/productivebees/common/item/Gene.java — passage
- [S38] https://raw.githubusercontent.com/JDKDigital/productive-bees/dev-1.21.0/src/main/java/cy/jdkdigital/productivebees/common/recipe/CombineGeneRecipe.java — passage
- [S39] https://raw.githubusercontent.com/JDKDigital/productive-bees/dev-1.21.0/readme.md — passage
- [S40] https://raw.githubusercontent.com/setycz/ChickensMod/main/README.md — passage
- [S41] https://raw.githubusercontent.com/setycz/ChickensMod/main/src/main/java/com/setycz/chickens/entity/EntityChickensChicken.java — passage
- [S42] https://raw.githubusercontent.com/ACCBDD/complicated_bees/1.21.1/README.md — passage
- [S43] https://api.modrinth.com/v2/project/2zkSGMyK — passage
- [S44] https://raw.githubusercontent.com/ForestryMC/Binnie/master-MC1.12/README.md — passage
- [S45] https://api.modrinth.com/v2/project/mystical-agriculture — passage
- [S46] https://forum.feed-the-beast.com/threads/bees-in-ftb-are-the-worst-addition-ever.144590/ — passage
- [S47] https://forum.feed-the-beast.com/threads/gendustry-vs-extra-bees.47448/ — passage
- [S48] https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/issues/25978 — passage
- [S49] https://api.modrinth.com/v2/project/genetic-animals — passage
- [S50] https://minecraft-modsmc.fandom.com/wiki/Genetic_Animals — passage
- [S51] https://minecraft-modsmc.fandom.com/wiki/Genetic_Animals/Cows — passage
- [S52] https://github.com/SaemonMoki/ExtendedAnimals/blob/1.20/gradle.properties — passage
- [S53] https://github.com/SaemonMoki/ExtendedAnimals/blob/master/LICENSE.txt — passage
- [S54] https://github.com/SaemonMoki/ExtendedAnimals/blob/1.21/gradle.properties — passage
- [S55] https://github.com/SaemonMoki/ExtendedAnimals/commits/1.18_datapack_breed_gen — passage
- [S56] https://github.com/SaemonMoki/ExtendedAnimals/blob/1.20/src/main/java/mokiyoki/enhancedanimals/util/Genes.java — passage
- [S57] https://github.com/SaemonMoki/ExtendedAnimals/blob/1.20/src/main/java/mokiyoki/enhancedanimals/init/breeds/CowBreeds.java — passage
- [S58] https://github.com/SaemonMoki/ExtendedAnimals/issues/88 — passage
- [S59] https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/LICENSE.txt — passage
- [S60] https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/src/main/resources/assets/tfc/patchouli_books/field_guide/en_us/entries/mechanics/animal_husbandry.json — passage
- [S61] https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/src/main/java/net/dries007/tfc/common/entities/livestock/MammalProperties.java — passage
- [S62] https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/src/main/java/net/dries007/tfc/common/entities/livestock/horse/HorseProperties.java — passage
- [S63] https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/src/main/java/net/dries007/tfc/common/entities/livestock/TFCAnimalProperties.java — passage
- [S64] https://github.com/TerraFirmaCraft/TerraFirmaCraft/blob/1.20.x/src/main/java/net/dries007/tfc/util/loot/AnimalYieldProvider.java — passage
- [S65] https://github.com/sekelsta/horse-colors/blob/master/license.txt — passage
- [S66] https://api.modrinth.com/v2/project/animal-genetics — passage
- [S67] https://api.modrinth.com/v2/project/chromosome-lib — passage
- [S68] https://api.modrinth.com/v2/project/animalhusbandry — passage
- [S69] https://api.modrinth.com/v2/project/dragns-livestock-overhaul — passage
- [S70] https://api.modrinth.com/v2/project/modern-chickens — passage
- [S71] https://api.modrinth.com/v2/project/genetics-resequenced — passage
- [S72] https://api.modrinth.com/v2/project/domestication-innovation — passage
- [S73] https://minecraft.fandom.com/wiki/Horse — passage
- [S74] https://docs.minecraftforge.net/en/1.20.x/resources/server/recipes/ingredients/ — passage
- [S75] https://api.modrinth.com/v2/project/quality-food — passage
- [S76] https://github.com/SiverDX/quality_food — passage
- [S77] https://docs.neoforged.net/docs/1.21.1/resources/server/recipes/ingredients — passage
- [S78] https://github.com/SiverDX/quality_food/blob/1.21.1/README.md — passage
- [S79] https://github.com/SiverDX/quality_food/issues/1 — passage
- [S80] https://github.com/SiverDX/quality_food/issues/77 — passage
- [S81] https://github.com/SiverDX/quality_food/issues/56 — passage
- [S82] https://github.com/SiverDX/quality_food/issues/35 — passage
- [S83] https://raw.githubusercontent.com/KubeJS-Mods/KubeJS/2001/common/src/main/java/dev/latvian/mods/kubejs/bindings/IngredientWrapper.java — passage
- [S84] https://raw.githubusercontent.com/KubeJS-Mods/KubeJS/2001/common/src/main/java/dev/latvian/mods/kubejs/platform/IngredientPlatformHelper.java — passage
- [S85] https://github.com/KubeJS-Mods/KubeJS/issues/176 — passage
- [S86] https://github.com/KubeJS-Mods/KubeJS/issues/588 — passage
- [S87] https://wiki.gtnewhorizons.com/wiki/Bees — passage
- [S88] https://raw.githubusercontent.com/TerraFirmaCraft/Documentation/main/1.20.x/ingredients.md — passage
- [S89] https://raw.githubusercontent.com/TerraFirmaCraft/Documentation/main/1.20.x/item-stack-modifiers.md — passage
- [S90] https://minecraft-mods.fandom.com/wiki/Genetic_Animals/Cows — passage
- [S91] https://api.modrinth.com/v2/project/agricraft — passage
- [S92] https://gist.github.com/Mordenkainen/8d0ec70e67f5036f257d1f3ba5f8318c — passage
- [S93] https://guide.appliedenergistics.org/1.20.1/items-blocks-machines/upgrade_cards/ — passage
- [S94] https://github.com/AppliedEnergistics/Applied-Energistics-2/issues/7588 — passage
- [S95] https://allthemods.github.io/alltheguides/atm9/productivebees/ — passage
- [S96] https://raw.githubusercontent.com/TerraFirmaCraft/TerraFirmaCraft/1.20.x/src/main/resources/assets/tfc/patchouli_books/field_guide/en_us/entries/mechanics/animal_husbandry.json — passage
- [S97] https://raw.githubusercontent.com/TerraFirmaCraft/TerraFirmaCraft/1.20.x/src/main/java/net/dries007/tfc/common/entities/livestock/TFCAnimalProperties.java — passage
- [S98] https://raw.githubusercontent.com/TerraFirmaCraft/TerraFirmaCraft/1.20.x/src/main/java/net/dries007/tfc/config/ServerConfig.java — passage
- [S99] https://raw.githubusercontent.com/TerraFirmaCraft/TerraFirmaCraft/1.20.x/src/main/resources/assets/tfc/patchouli_books/field_guide/en_us/entries/mechanics/decay.json — passage
- [S100] https://raw.githubusercontent.com/TerraFirmaCraft/TerraFirmaCraft/1.20.x/src/main/resources/data/minecraft/recipes/beetroot_soup.json — passage
- [S101] https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/HEAD/kubejs/server_scripts/minecraft/loot.js — passage
- [S102] https://github.com/AlmostReliable/lootjs/wiki/1.20.1 — passage
- [S103] https://kubejs.com/wiki/tutorials/item-modification — passage
- [S104] https://raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.20.1/src/main/java/net/minecraftforge/common/world/ForgeBiomeModifiers.java — passage
- [S105] https://raw.githubusercontent.com/TerraFirmaGreg-Team/Modpack-Modern/HEAD/kubejs/data/fowlplay/forge/biome_modifier/biome_modifier.json — passage
- [S106] https://api.modrinth.com/v2/project/diet — passage
- [S107] https://api.cfwidget.com/minecraft/mc-mods/fauna-and-ecology — passage
- [S108] https://api.modrinth.com/v2/project/cattlestrophic — passage
- [S109] https://www.curseforge.com/minecraft/mc-mods/genetic-animals — snippet only
- [S110] https://github.com/AlmostReliable/morejs/wiki/Villager-Trades — passage
- [S111] https://github.com/kube-mods/kubejs/issues/1171 — passage
- [S112] https://terrafirmacraft.com/f/topic/7503-animal-feeding-trough/ — snippet only

## Audit

| Claim | Verdict | Note |
|---|---|---|
| C101 | SUPPORTED | quote check NOT FOUND: the quote is two passages joined with +; each piece found verbatim in the raw file by hand |
| C102 | SUPPORTED |  |
| C103 | PARTIAL | rewritten: M2 shows one required dependency id only; its name, licence and the InfinityLib statement are not in the output |
| C104 | SUPPORTED |  |
| C105 | PARTIAL | rewritten: beta dates and 'no NeoForge build for 1.20.1' are not in M2; cut to the listed versions and project fields |
| C106 | SUPPORTED |  |
| C108 | SUPPORTED |  |
| C109 | SUPPORTED |  |
| C110 | SUPPORTED | quote check NOT FOUND: the quote is two passages joined with +; each piece found verbatim in the raw file by hand |
| C111 | SUPPORTED |  |
| C112 | SUPPORTED | quote check NOT FOUND: the quote is two passages joined with +; each piece found verbatim in the raw file by hand |
| C113 | SUPPORTED | quote check NOT FOUND: the quote is two passages joined with +; each piece found verbatim in the raw file by hand |
| C115 | SUPPORTED |  |
| C116 | SUPPORTED |  |
| C118 | SUPPORTED |  |
| C120 | SUPPORTED |  |
| C121 | SUPPORTED |  |
| C122 | SUPPORTED | quote check NOT FOUND: the quote is two passages joined with +; each piece found verbatim in the raw file by hand |
| C123 | SUPPORTED |  |
| C124 | SUPPORTED |  |
| C125 | SUPPORTED |  |
| C126 | SUPPORTED |  |
| C127 | SUPPORTED |  |
| C128 | PARTIAL | rewritten: M9 shows only the path count and the zero; the list of extension points and the datagen statement are not in the output |
| C129 | PARTIAL | rewritten: M7 shows one search with one own-repo hit; the Roll54 mixin addon is not in the output |
| C130 | SUPPORTED |  |
| C131 | SUPPORTED | quote check NOT FOUND: the quote is two passages joined with +; each piece found verbatim in the raw file by hand |
| C132 | SUPPORTED |  |
| C133 | SUPPORTED | quote check NOT FOUND: the quote is two passages joined with +; each piece found verbatim in the raw file by hand |
| C134 | SUPPORTED |  |
| C135 | PARTIAL | rewritten: the cited file does not state a game version; '1.20.1' removed |
| C137 | SUPPORTED |  |
| C138 | SUPPORTED |  |
| C139 | SUPPORTED |  |
| C140 | SUPPORTED |  |
| C141 | PARTIAL | rewritten: the issue is a feature request; it does not say the Hydroponic Bed fails, only asks for support |
| C142 | PARTIAL | rewritten: M9 lists three plugin directories; Botania horn, the Mystical Agriculture tint plugin and PR #1550 are not in the output |
| C144 | SUPPORTED |  |
| C145 | SUPPORTED |  |
| C147 | SUPPORTED |  |
| C149 | SUPPORTED |  |
| C150 | SUPPORTED |  |
| C151 | PARTIAL | rewritten: M1 shows counts and commit dates; the newest-maintainer-comment date and the list of unanswered issues/PRs are not in the output |
| C152 | PARTIAL | rewritten: 'only' and the Create harvester mixin are not in M6; cut to the recorded fields |
| C153 | SUPPORTED |  |
| C155 | PARTIAL | rewritten: M5 shows push date 2026-06-23, not a 2026-04-18 commit; the CurseForge ReReloaded fork is not in the output |
| C201 | SUPPORTED |  |
| C202 | PARTIAL | rewritten: M3 shows NOASSERTION, not the LICENSE file text; the branch list is not in the output |
| C203 | SUPPORTED |  |
| C204 | SUPPORTED |  |
| C205 | SUPPORTED |  |
| C206 | SUPPORTED |  |
| C207 | SUPPORTED |  |
| C208 | SUPPORTED |  |
| C209 | SUPPORTED |  |
| C210 | SUPPORTED |  |
| C211 | SUPPORTED |  |
| C212 | SUPPORTED |  |
| C213 | SUPPORTED |  |
| C214 | SUPPORTED |  |
| C215 | SUPPORTED |  |
| C216 | PARTIAL | rewritten: LGPL-3.0 rested on C202, which no longer says it; licence clause and the C202 reference removed |
| C217 | SUPPORTED |  |
| C218 | SUPPORTED |  |
| C219 | SUPPORTED |  |
| C220 | PARTIAL | rewritten: the code takes Math.max with the child's own value, so 'never above the better parent' does not hold |
| C221 | SUPPORTED |  |
| C222 | SUPPORTED |  |
| C223 | PARTIAL | rewritten: 'not from allele recombination' is not in the passage |
| C224 | PARTIAL | rewritten: M10 lists file names only; 'dedicated storage for gene items' is not in the output |
| C225 | PARTIAL | rewritten: licence and push date are not in the cited README |
| C226 | PARTIAL | rewritten: a weighted average plus 1-2 can be below the better parent; 'only ratchet upward' removed |
| C227 | PARTIAL | rewritten: 'starts over' is more than the passage says |
| C228 | PARTIAL | rewritten: licence, versions and update date are not in the cited README |
| C229 | PARTIAL | rewritten: MMPL, LGPL v3 and the Modrinth 1.7.10 statement are not in M5 |
| C230 | SUPPORTED |  |
| C231 | SUPPORTED |  |
| C232 | SUPPORTED |  |
| C233 | SUPPORTED |  |
| C234 | SUPPORTED |  |
| C235 | UNCHECKABLE | prior |
| C236 | SUPPORTED |  |
| C237 | SUPPORTED |  |
| C238 | SUPPORTED |  |
| C239 | SUPPORTED |  |
| C240 | PARTIAL | rewritten: one post, not 'some players' |
| C241 | PARTIAL | rewritten: one post, not 'some' |
| C242 | SUPPORTED |  |
| C243 | SUPPORTED |  |
| C244 | SUPPORTED |  |
| C245 | PARTIAL | rewritten: M5 shows licence and push date; that it is an extraction of Forestry's genetics API is not in the output |
| C301 | SUPPORTED |  |
| C302 | SUPPORTED |  |
| C303 | SUPPORTED |  |
| C304 | SUPPORTED | quote check unfetchable; read at audit through the wiki's api.php (wikitext), quote found |
| C305 | SUPPORTED | quote check unfetchable; read at audit through the wiki's api.php (wikitext), quote found |
| C306 | SUPPORTED | quote check unfetchable; read at audit through the wiki's api.php (wikitext), quote found |
| C307 | SUPPORTED | quote check unfetchable; read at audit through the wiki's api.php (wikitext), quote found |
| C308 | SUPPORTED | quote check unfetchable; read at audit through the wiki's api.php (wikitext), quote found |
| C309 | SUPPORTED | quote check unfetchable; read at audit through the wiki's api.php (wikitext), quote found |
| C310 | PARTIAL | rewritten: 'expressed through genes, not separate species' is not in the passage; quote check unfetchable; read at audit through the wiki's api.php (wikitext), quote found |
| C311 | SUPPORTED | quote check unfetchable; read at audit through the wiki's api.php (wikitext), quote found |
| C312 | SUPPORTED | quote check unfetchable; read at audit through the wiki's api.php (wikitext), quote found |
| C313 | PARTIAL | rewritten: 'itself genetic' is not stated in the Drops passage; quote check unfetchable; read at audit through the wiki's api.php (wikitext), quote found |
| C314 | SUPPORTED | quote check unfetchable; read at audit through the wiki's api.php (wikitext), quote found |
| C315 | SUPPORTED |  |
| C316 | PARTIAL | rewritten: 'only licence file' and the GitHub NOASSERTION value are not in the cited file |
| C317 | PARTIAL | rewritten: gradle.properties shows targets only; 'actively developed' and the 2026-11-19 merges are not in it |
| C318 | PARTIAL | rewritten: the commit list does not show what is or is not shipped |
| C319 | SUPPORTED |  |
| C320 | SUPPORTED |  |
| C321 | PARTIAL | rewritten: the cited issue does not show the other tracker entries (Atum 2, Quark) |
| C322 | SUPPORTED |  |
| C323 | SUPPORTED |  |
| C324 | SUPPORTED |  |
| C325 | PARTIAL | rewritten: the 1-32 range is not in the cited file |
| C326 | SUPPORTED |  |
| C327 | SUPPORTED |  |
| C328 | SUPPORTED |  |
| C329 | PARTIAL | rewritten: 'no gene found for milk, wool or eggs' rests on a code search that is not a recorded claim; narrowed to the files behind C325, C326, C328 |
| C330 | SUPPORTED | quote check unfetchable (file under 300 characters); raw file read at audit, quote found |
| C331 | SUPPORTED |  |
| C332 | PARTIAL | rewritten: repo push date and 'registration is by code' are not in the Modrinth record |
| C333 | SUPPORTED |  |
| C334 | SUPPORTED |  |
| C335 | SUPPORTED |  |
| C336 | SUPPORTED |  |
| C337 | SUPPORTED |  |
| C338 | SUPPORTED | quote check unfetchable; read at audit through the wiki's api.php (wikitext), quote found |
| C339 | UNCHECKABLE | prior |
| C401 | SUPPORTED |  |
| C402 | SUPPORTED |  |
| C403 | SUPPORTED |  |
| C404 | SUPPORTED |  |
| C405 | SUPPORTED |  |
| C406 | SUPPORTED |  |
| C407 | SUPPORTED |  |
| C408 | SUPPORTED |  |
| C409 | SUPPORTED |  |
| C410 | PARTIAL | rewritten: the body ties luck to crafted items, and says nothing of a seed line; cut to the farmland bonus |
| C411 | PARTIAL | rewritten: the file tree shows the mixins exist (no press mixin seen); 'propagation is not automatic' is a reading, not in the tree; quote check NOT FOUND: the path is not on the repo's HTML page; found in the branch 1.20.1 file tree via gh api |
| C412 | SUPPORTED |  |
| C413 | SUPPORTED |  |
| C414 | SUPPORTED |  |
| C415 | SUPPORTED |  |
| C416 | SUPPORTED |  |
| C417 | SUPPORTED |  |
| C418 | SUPPORTED |  |
| C419 | SUPPORTED |  |
| C420 | SUPPORTED |  |
| C421 | SUPPORTED |  |
| C422 | SUPPORTED |  |
| C423 | SUPPORTED |  |
| C424 | SUPPORTED |  |
| C425 | SUPPORTED |  |
| C426 | SUPPORTED |  |
| C427 | SUPPORTED |  |
| C428 | SUPPORTED |  |
| C429 | SUPPORTED |  |
| C430 | PARTIAL | rewritten: 'skill-derived quality' is not in the passage |
| C431 | PARTIAL | rewritten: page read directly (api.php wikitext); relabelled quoted; narrowed to cows and quantity - wool blocks and 'not a quality on the item' are not on the Cows page; quote check unfetchable, quote found in the api.php wikitext |
| C432 | SUPPORTED |  |
| C433 | SUPPORTED |  |
| C434 | SUPPORTED |  |
| C435 | SUPPORTED |  |
| C436 | SUPPORTED |  |
| C437 | PARTIAL | rewritten: 'none ships a recipe' needs a full reading of every mod; narrowed to what the read sources describe (C431 narrowed to cows) |
| C438 | SUPPORTED |  |
| C439 | SUPPORTED |  |
| C501 | SUPPORTED |  |
| C502 | SUPPORTED |  |
| C503 | SUPPORTED |  |
| C504 | SUPPORTED |  |
| C505 | SUPPORTED |  |
| C506 | SUPPORTED |  |
| C507 | SUPPORTED |  |
| C508 | SUPPORTED |  |
| C509 | SUPPORTED |  |
| C510 | PARTIAL | rewritten: the argument meanings are not visible in the cited config file |
| C511 | SUPPORTED |  |
| C512 | PARTIAL | rewritten: the 0.35 cap figure rested on C510, now narrowed; numeric example and the C510 reference removed |
| C513 | SUPPORTED |  |
| C514 | SUPPORTED | quote check unfetchable (file under 300 characters); raw file read at audit, quote found |
| C515 | SUPPORTED |  |
| C516 | SUPPORTED |  |
| C517 | SUPPORTED |  |
| C518 | SUPPORTED |  |
| C519 | SUPPORTED |  |
| C520 | SUPPORTED | quote check NOT FOUND on the rendered wiki page; found verbatim in the raw wiki markdown (1.20.1.md) |
| C521 | SUPPORTED | quote check NOT FOUND on the rendered wiki page; found verbatim in the raw wiki markdown (1.20.1.md) |
| C522 | SUPPORTED | quote check NOT FOUND on the rendered wiki page; found verbatim in the raw wiki markdown (1.20.1.md) |
| C523 | SUPPORTED |  |
| C524 | PARTIAL | rewritten: 'would close loot leaks' does not follow from API docs alone (C523 documents a hole); narrowed to expressibility |
| C525 | SUPPORTED |  |
| C526 | SUPPORTED |  |
| C527 | SUPPORTED |  |
| C528 | SUPPORTED | quote check unfetchable (file under 300 characters); raw file read at audit, quote found |
| C529 | SUPPORTED |  |
| C530 | SUPPORTED |  |
| C531 | SUPPORTED |  |
| C532 | SUPPORTED |  |
| C533 | PARTIAL | rewritten: 'usable to devalue monotonous eating rather than to block foods' is not in the record |
| C534 | SUPPORTED |  |
| C535 | SUPPORTED |  |
| C536 | UNCHECKABLE | snippet; CurseForge returned 403 |
| C537 | SUPPORTED |  |
| C538 | SUPPORTED |  |
| C539 | UNCHECKABLE | snippet; TFC forum returned 403 |

Label verdict: answered — the Summary states one recommendation (write the breeding core from scratch, no fork and no addon core) and every claim it rests on is SUPPORTED or was rewritten to its source; the gaps left open (AgriCraft stat extension not run, Chromosome Lib API unread, foreign recipe types on 1.20.1, loot-type food removal untested) would, resolved against the report, strengthen the case against a fork or add a library dependency to a from-scratch mod, not switch the choice among fork, addon and from scratch.

Quote check: 177 quoted claim(s): 150 found, 11 NOT FOUND, 16 unfetchable, 0 no source — the 11 NOT FOUND and 16 unfetchable claims were each found by hand in the source's raw text; see the Note column

## Disposition

| Recommendation | State | Where |
|---|---|---|
| Писать ядро селекции с нуля, заимствуя у AgriCraft отдельные решения и код под MIT; не форкать AgriCraft целиком и не строить ядро как аддон к нему | deferred | unfiled: решение об архитектуре принимает автор проекта в родительской сессии; задача этого прогона разрешала менять только файл отчёта |
| Хранить значение признака на био-материале несколькими дискретными уровнями, а не непрерывным числом | deferred | unfiled: проектное решение ядра, принимается вместе с выбором архитектуры; сначала нужен эксперимент с рецептами чужих машин из Open questions |
| Взять кормление, пороги и износ животных по образцу TerraFirmaCraft, а ручность сделать наследуемым признаком линии | deferred | unfiled: проектное решение по животным, принимается автором; issue в этом прогоне не заводились |
