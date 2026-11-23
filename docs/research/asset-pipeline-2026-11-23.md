---
topic: asset-pipeline
tags: area:assets, research
source_provenance: /research invocation from the farming-evolution planning session, 2026-11-23
status: open
audit: 2026-11-23 — 199 claims: 162 SUPPORTED, 28 PARTIAL, 2 UNSUPPORTED, 7 UNCHECKABLE
---

## Question

Как в 2026 году делать текстуры, модели и прочие ассеты для мода Minecraft силами ИИ-агентов.

Нужно ответить:
- Пиксель-арт 16×16 и 32×32: какие генеративные модели и сервисы дают пригодный результат, как добиваются единого стиля и палитры, совместимости с ванильным стилем, тайлинга блоков.
- Программная генерация: рисование текстур кодом, перекраска и комбинирование базовых спрайтов (варианты линий и пород из одной основы), процедурные стадии роста.
- Модели: блочные и предметные JSON-модели, модели сущностей (Blockbench, GeckoLib), анимации — что из этого агент может делать сам и как проверять результат.
- Проверка без человека: рендер текстуры или модели в картинку для агента, сравнение со стилем, контактные листы.
- Лицензии: что можно использовать из сгенерированного и из чужих ресурс-паков при публикации на CurseForge и Modrinth, правила площадок насчёт ИИ-контента.
- Опубликованный опыт модов и игр, где ассеты делали ИИ, и реакция сообщества.
- Рекомендуемый конвейер для этого проекта и объём ручной работы, который останется.

Asked семь пунктов, answering четыре подвопроса, because правило скилла — не больше четырёх подвопросов на прогон. Пункты «модели» и «проверка без человека» сведены в Q3, пункты «лицензии» и «опыт и реакция сообщества» — в Q4; пункт «рекомендуемый конвейер» не подвопрос, а синтез в Summary. Каждый ledger упёрся в предел 20 000 знаков; то, что из-за этого не собрано, перечислено в Open questions как следующие прогоны. Объём остающейся ручной работы в часах не оценён: источников с такими числами не найдено.

Предыдущий отчёт: `docs/research-baseline.md` (моды, AgriCraft, KubeJS) — про ассеты в нём ничего нет, этот отчёт его не дополняет и не опровергает; из него взято только рабочее допущение «Minecraft 1.20.1 Forge».

Sub-questions: Q1 — какие генеративные модели и сервисы дают пригодный пиксель-арт 16×16 и 32×32 и как держат стиль, палитру и тайлинг; Q2 — как текстуры делают программно (тинт, замена палитры, слои, стадии роста) и какие механизмы даёт игра и экосистема; Q3 — какие модели и анимации агент может делать сам и как отрендерить и проверить результат без человека; Q4 — что можно публиковать на CurseForge и Modrinth (ИИ-ассеты, чужие и ванильные ассеты), что говорят правила площадок об ИИ и как реагирует сообщество.

## Summary

**Главное ограничение — не техника, а правила Modrinth.** С 13 августа 2026 Modrinth запрещает публичный листинг проектов, содержимое которых «преимущественно или целиком» является выводом ИИ, и льготный период закончился 27 сентября 2026 [C401], [C402], [C406]. Любой ИИ-сгенерированный или сделанный с помощью ИИ ассет требует раскрытия [C403], [C411], а ИИ-изображения на странице проекта запрещены [C404]. Проект, где код, текстуры и модели целиком делают агенты, а человек только ставит задачи и тестирует, по тексту этих правил под публичный листинг на Modrinth не подходит [C448]; сильная работа ИИ в одной части допустима, если другая часть — существенно человеческая и ИИ раскрыт [C408]. В опубликованных Moderation Policies CurseForge единственное правило про ИИ касается витринных изображений [C418], [C419]; Terms of Use CurseForge в этом прогоне не читались. Как модераторы Modrinth применяют «преимущественно ИИ» на практике, не найдено.

**Рекомендуемый конвейер.**

1. *Основа — программная генерация, а не генеративные картинки.* Игра сама умеет варианты из одной основы: тинт по `tintindex` [C201], послойные предметные модели с отдельным тинтом на слой [C205], [C209] и замену палитры `paletted_permutations` при сборке атласа, добавленную в 1.19.4 [C215], [C216], которая в 1.20.1 встречается и в ванильном атласе `blocks` [C222]. С этим согласуется устройство GregTech Modern (послойная модель слитка на сером шаблоне, два цвета материала; код раскраски не читался) [C248]; в Mystical Agriculture на прочитанном примере алюминиевой культуры стадии 0–6 указывают на общие модели, а тинт стоит только на цветке последней стадии [C249], [C251], [C252]. Для линий и пород: агент рисует малое число шаблонов в ключевых серых и получает варианты заменой палитры; шаблон должен быть нарисован строго ключевыми цветами, иначе замена ломается [C265].
2. *Замена палитры предпочтительнее чистого тинта для заметных ассетов.* Тинт — покомпонентное умножение, он даёт прямую рампу одного тона [C202], [C262], а практика пиксель-арта считает такие рампы тусклыми [C256], [C257]; обратное мнение — что сдвиг тона не отдельный приём [C263]. В Mystical Agriculture рядом с четырьмя шаблонами цветков лежат 132 отдельные текстуры цветков по культурам; измерено по одному файлу каждого вида: шаблон серый, текстура культуры цветная [C254].
3. *Стадии роста* — по ванильной схеме: одна текстура 16×16 на стадию, модель из четырёх строк с родителем `crop` [C226], [C227], [C230]; JSON моделей и состояний генерировать через datagen, который сам проверяет наличие PNG [C232], [C234].
4. *Генеративные сервисы — только как источник черновика формы.* Через HTTP и MCP агенту доступны PixelLab, Retro Diffusion и Sprite Fusion [C158]; из прочитанного только Retro Diffusion документирует и генерацию меньше 32 px (модели RD Pro и RD Lite), и стили Minecraft, тайлинг и входную палитру; стили Minecraft относятся к другим моделям (RD Fast, RD Plus, RD Mini), для которых размер меньше 32 px не указан [C159]. Общие модели дают не спрайт, а большую картинку спрайта [C124], [C125], и именно на 16×16 преобразование, по одному вендорскому замеру, перестаёт работать [C132], [C160]. Независимых тестов PixelLab и Retro Diffusion на 16×16 не найдено. Локальная модель предметов 16 px с Hugging Face — лицензионный риск [C161]. Любой такой ассет попадает под раскрытие на Modrinth [C403].
5. *Модели.* Блочные и предметные модели — текстовый JSON из кубоидов [C301], [C307], плоским предметам геометрия не нужна [C306]; это агент делает сам. Для животных — GeckoLib, который выходит под 1.20.1 Forge [C330], [C331]; исходник держать в `.bbmodel` [C329] и компилировать `.geo.json` и анимации инструментом, а не писать вручную; загружается ли вывод такого инструмента в GeckoLib, не проверялось [C334]. Есть безголовый MCP-сервер, который правит, проверяет, экспортирует и рендерит `.bbmodel` без запущенного Blockbench [C317], [C318], [C319]; авторы таких инструментов сами называют типовые ошибки агента — плоские коробки, путаница лево/право, неспособность считать координаты [C353], [C354], [C355]. Измеренных данных о качестве моделей, сделанных агентом, нет.
6. *Проверка без человека.* Числовые тесты спрайта (число различных индексов, положение ограничивающей рамки, пропорции рампы) [C151]; рендер JSON-моделей в PNG вне игры через `block-model-renderer` [C335], [C337]; рендер и контактные листы `.bbmodel` [C319] (нужны Node 23.6+ и GPU [C320]); контактные листы текстур через ImageMagick `montage` [C360]. В игре: в прочитанной документации Forge 1.20.1 клиентского теста со скриншотом не найдено [C349], HeadlessMC ничего не рендерит [C347] — нужен свой клиентский код или мод-экспортёр иконок [C341]. Ни один инструмент в этом прогоне не запускался.
7. *Чужие и ванильные ассеты не брать за основу.* Текстура, выведенная кодом из ванильного спрайта, — производная от ассета Mojang [C429]; чужое без лицензии — «все права защищены» [C417], обе площадки требуют прав и указания автора [C414], [C415], [C420].

**Что остаётся человеку.** Приёмка внешнего вида: числовые тесты пропускают дефекты, которые видит глаз [C151], и Scenario пишет, что привязка к сетке не заменяет ручную доводку [C123]. Изображения страницы проекта на Modrinth — не ИИ [C404]. Решение, достаточно ли в проекте человеческого вклада для Modrinth, и раскрытие ИИ [C408], [C403]. По позиции Бюро авторского права США чисто «промптовые» текстуры могут не охраняться авторским правом [C430], [C433].

**Опыт и реакция.** Modrinth пишет, что ИИ-изображения «подавляющим образом непопулярны в любом контексте» [C438]; Planet Minecraft, по записи автора отчёта, запрещает ИИ-контент, подменяющий собственную работу (при аудите страница не открылась) [C440]; Steam требует раскрытия ИИ-контента [C442]; Nexus Mods оставляет ИИ-моды разрешёнными, но наказывает за неверную пометку [C443]. Задокументированный случай отката — Crimson Desert [C444]. Случая, когда автора мода Minecraft публично критиковали за ИИ-текстуры, и популярного хорошо принятого мода с ИИ-ассетами не найдено.

## Findings

### Q1 — какие генеративные модели и сервисы дают пригодный пиксель-арт 16×16 и 32×32 и как держат стиль, палитру и тайлинг — answered with a gap

- **C101** [quoted] PixelLab exposes a text-to-pixel-art HTTP endpoint (Pixflux) that accepts init images and a forced palette — "Supports init images and forced palettes" — [S1], Create image (Pixflux), POST /v2/create-image-pixflux
- **C102** [quoted] PixelLab's price list for the Pixen text-to-image endpoint starts at a 32x32 example costing about 0.7 US cent; no 16x16 generation price is listed for the create-image endpoints — "32x32 : $0.007" — [S1], Create image (Pixen), Estimated Price
- **C103** [quoted] PixelLab's Bitforge endpoint applies a custom art style taken from reference images; the 200x200 maximum listed for it is the size of the generated image — "Apply custom art styles using reference images" — [S1], Create image (Bitforge)
- **C104** [quoted] PixelLab has a tileset endpoint that produces mutually connecting top-down tiles and lists a 16x16 tile price — "Seamlessly connecting tiles for top-down maps" — [S1], Create top-down tileset, POST /v2/create-tileset (16×16 tiles : $0.0079)
- **C106** [quoted] PixelLab ships an MCP server that Claude Code adds with one command and a bearer token, so an agent can call it without a GUI — "claude mcp add pixellab https://api.pixellab.ai/mcp -t http -H" — [S2], Claude Code install box
- **C107** [quoted] PixelLab's terms say the user owns the copyright in outputs and may use them commercially — "You own the copyrights to your creations, permitting usage for both commercial and non-commercial purposes with no need for permission." — [S3], 1.3 (Last Updated: 2026-10-30)
- **C108** [quoted] PixelLab forbids training other models on its outputs without permission — "except for training other models without our explicit permission" — [S3], 3.3
- **C109** [quoted] PixelLab allows programmatic use only through its official API and describes that API as meant for vibe coding and live in-game asset creation; other uses need contact — "Programmatic usage is only allowed through our official API (api.pixellab.ai)." — [S3], 2.2
- **C110** [quoted] PixelLab's terms also bind the user to the Open RAIL-M licence — "you agree to comply with the terms listed in the Open RAIL-M license" — [S3], 4.1
- **C111** [quoted] Retro Diffusion offers image, animation and tileset generation over an HTTP API — "over a simple HTTP API" — [S4], intro
- **C112** [quoted] Retro Diffusion's API accepts width and height from 12 to 512 px, with per-style limits — "12–512 overall; each style enforces tighter limits" — [S4], request parameters table, width/height
- **C113** [quoted] RD Pro generates down to 12x12 px, 16 per batch, at $0.18 per image, and takes up to 9 reference images for style consistency — "Down to **12×12px** with **batch ≤ 16** (great for tiny sprites and icons)" — [S4], Models, RD Pro
- **C114** [quoted] RD Lite costs a flat $0.05 per image at any size from 12 to 512 px but has no tiling — "**$0.05/image flat at any size**" — [S4], Models, RD Lite
- **C115** [quoted] Retro Diffusion has dedicated Minecraft item and Minecraft texture styles, available on the API-only RD Fast / RD Plus models and the RD Mini aliases — "use it for batches over 4 or its 1-bit and Minecraft styles" — [S4], Models, RD Fast (style ids rd_fast__mc_item, rd_fast__mc_texture, rd_plus__mc_texture, rd_mini__mc_texture)
- **C116** [quoted] Retro Diffusion's API has a seamless-tiling switch per axis — "Seamless tiling on either axis." — [S4], parameters table, tile_x / tile_y
- **C117** [quoted] Retro Diffusion's API can constrain output colours to a supplied palette image — "Constrain output colors to a palette image." — [S4], parameters table, input_palette
- **C118** [quoted] Retro Diffusion's newer models RD Lite and RD Core are documented as having no tiling, so the tiling switch does not apply to every model — "isometric, platformer, and top-down art, and up to **4 images per request**. No tiling." — [S4], Models, RD Core
- **C120** [quoted] Retro Diffusion also has an MCP endpoint with typed tools, including a Pixel Fixer — "33 typed tools covering generation, Low-Poly 3D models, Pixel Fixer, free cost estimates," — [S4], MCP paragraph
- **C121** [quoted] Sprite Fusion runs a remote MCP server with the same operations as its pixel-art generation API; Claude Code connects by URL and signs in by OAuth, or by API key header — "It offers the same operations as the API." — [S5], MCP Server intro
- **C123** [quoted] Scenario, which hosts the open-source Pixel Snapper as a credit-priced utility model, says in its documentation that snapping does not replace manual touch-up — "Pixel Snapper is a time‑saver, not a magic wand." — [S6], Workflow and Tips, Check the results
- **C124** [quoted] A practitioner who tested general image models through APIs found the output is a large picture of a sprite, not a sprite — "But it’s a 1024 x 1024 picture of a sprite, not a sprite." — [S7], Pixel art is a style, not a format (Gemini 2.5 Flash Image, about $0.039)
- **C125** [quoted] The same test: general models cannot generate at small sizes; a 32x32 sprite has to be generated large and shrunk — "You can’t generate small. Models work best at roughly 512 to 1600 pixels." — [S7], limits section
- **C126** [quoted] The same author: plain general models do not reliably make seamless tiles; a trained model or LoRA or a tool tiling option is needed — "Plain models won’t reliably produce a seamless tile." — [S7], Tileable textures paragraph
- **C127** [quoted] The same author says PixelLab draws sprites at their real size and exposes palette, outline, camera angle and transparency as settings — "It draws real sprites at their actual size rather" — [S7], Doing pixel art properly
- **C128** [quoted] The same author's do-it-yourself route: a pixel-art add-on model such as Pixel Art XL, then shrink onto a clean grid and fix the palette in code — "then shrink the result onto a clean grid and fix the palette" — [S7], Doing pixel art properly
- **C129** [quoted] The author of the unfake tool describes the failure: diffusion works in a continuous latent space with no hard edges, clean pixels or fixed palettes — "There’s no concept of hard edges, clean pixels, or fixed palettes." — [S8], Introduction (posted 2025-07-30)
- **C130** [quoted] One vendor-blog measurement (n=1 generation): a sprite requested at 32x32 with about twelve colours came back with 44,725 colours and block widths of 26 to 32 image pixels — "asked for twelve colours and came back with 44,725, on a grid whose \"pixels\" varied between 26 and 32 pixels wide" — [S9], opening summary (published 2026-08-14; the model used is not named in the passage read)
- **C131** [quoted] The same post: explicit prompt wording for exact size, no anti-aliasing and a strict palette did not make the model comply — "The model complied stylistically and ignored every requirement literally." — [S9], why prompt engineering does not rescue it
- **C132** [quoted] The same post: converted general-model output survives at 64x64, loses the face at 32x32, and at 16x16 has to be redrawn rather than converted — "at 16×16 the result needs redrawing by hand rather than converting" — [S9], size section
- **C133** [quoted] The same post's palette heuristic: about 8 colours at 16x16, 12 at 32x32, 16 at 64x64, or quantise to the project's own palette file — "A 64×64 sprite works at about 16 colours, 32×32 at about 12, and 16×16 at 8." — [S9], How many colours should you use?
- **C134** [quoted] The same post: order of operations is downsample first, quantise second, with dithering off — "Downsample first, quantise second." — [S9], fix section
- **C135** [quoted] Pixel Snapper (MIT, Rust) states the three defects of AI pixel art it fixes: inconsistent pixel size and position, drifting grid resolution, colours not tied to a palette — "Colors are not tied to a strict palette." — [S10], Why ?
- **C136** [quoted] Pixel Snapper is a CLI that takes a file or a directory and a colour count (default 16), so an agent can batch it — "spritefusion-pixel-snapper <INPUT> <OUTPUT> [COLOR_COUNT] [OPTIONS]" — [S10], CLI, Usage
- **C137** [quoted] Pixel Snapper can force output onto a fixed list of hex colours, which is a way to hold one palette across assets — "Constrains the output to comma-separated six-digit hex colors." — [S10], CLI, --palette
- **C138** [quoted] proper-pixel-art (MIT, pip-installable, CLI `ppa` and Python API) recovers true-resolution pixel art from noisy generated images where plain downsampling fails — "Such images often have a non-uniform grid and random artifacts, so standard downsampling fails" — [S11], Summary
- **C141** [quoted] A Hugging Face full fine-tune of SD 1.5 outputs actual 16x16 RGBA Minecraft mod item sprites: generate at 256x256, fold each 16x16 block into one pixel — "Generate at 256×256, then fold each 16×16 block into one pixel." — [S12], The one thing you must know
- **C142** [quoted] That model covers items only; block textures were excluded from training — "Items only. Full-bleed block textures were deliberately excluded from training." — [S12], Limitations
- **C143** [quoted] That model's training set is textures scraped from Modrinth mods under 235 licence values including all-rights-reserved; the card says it is released for research and personal use — "This model is released for research and personal" — [S12], Training data provenance and licensing
- **C144** [quoted] The card warns that outputs may resemble existing mod assets and tells redistributors to verify originality — "generated outputs may resemble existing mod" — [S12], Training data provenance and licensing
- **C145** [quoted] The card reports failure rates: about 10–15% of generations fill the frame so the chroma key fails, and results must be judged by hit rate over several seeds — "Judge by hit rate, not by one image." — [S12], What it can and cannot do
- **C147** [quoted] Pixel Art Bench asks LLMs for a JSON palette plus a 24x24 index grid; in a local re-run with small open models the best one (Phi-3.5-mini) had render success about 0.74 and pixel-art quality about 0.42 — "with render success dropping to approximately 0.74 and pixel art quality to around 0.42" — [S14], From Benchmark to Evaluation Protocol (published 2026-05-05; frontier models are not in this re-run)
- **C149** [quoted] An HN commenter reports that having an LLM emit drawing instructions (procedural graphics) avoids the grid, palette and noise problems of image models — "which can have issues with grid alignment, palette choices, and high-frequency noise" — [S15], comment 50172003 by vunderba, 2026-10-20
- **C150** [quoted] A practitioner who had Claude Opus build pixel art through code made the model emit palette indices through a rasteriser instead of picking pixels or colours — "The output is not colour. It is a palette index." — [S16], rasteriser section
- **C151** [quoted] The same write-up checks the indexed buffer with automated tests on numbers (distinct indices, pixels that differ between frames, bounding-box position, ramp proportions) rather than by looking at images, and still needs an eye for defects the tests pass — "some defects are legal, and legal defects need an eye" — [S16], after the 16x24 sprite example
- **C153** [snippet] A toolchain README (PIXL) claims LLMs fail at raw pixel grids above about 12x12 and rates raw character grids at 16x16 or smaller at 85–95% accuracy; vendor claim, page returned 404 on fetch — "LLMs fail at raw pixel reasoning above 12x12" — [S17], search snippet
- **C154** [snippet] An experiment (Asset Forge) had Claude compose sprites from geometric primitives because raw pixel arrays came out incoherent, and found a judge works better given both the PNG and an ASCII grid; page returned 404 on fetch — "asking an LLM to output raw pixel arrays produces incoherent results" — [S18], search snippet
- **C155** [quoted] An HN commenter on AI isometric tiles: the minimum post-processing is palette reduction and square-grid alignment, and at tens of thousands of images it still needs much manual intervention — "still requires a lot of manual intervention" — [S19], comment 50488244, 2026-11-13
- **C156** [quoted] An HN commenter rejects AI pixel art outright as a quality matter and advises learning or hiring — "If you want pixel art for your project then learn or hire someone with that expertise" — [S20], comment 50219021 by __bias, thread 50171379
- **C157** [quoted] Another commenter lists concrete visible defects in a general-model sprite: leftover magenta fringe, incoherent palette, stray noise pixels — "random noise pixels of totally different colors are visible scattered around" — [S21], comment 50219658 by ramblurr, thread 50171379
- **C158** [inference] Two dedicated services (PixelLab, Retro Diffusion) and Sprite Fusion are callable by an agent without a GUI, through HTTP and MCP — from [C101], [C106], [C111], [C120], [C121]
- **C159** [inference] Retro Diffusion is the only service read here that documents generation below 32 px (RD Pro and RD Lite, from 12 px) and also Minecraft-specific styles, a tiling switch and a palette input; the Minecraft styles belong to RD Fast / RD Plus / RD Mini, for which no size below 32 px is stated in the claims above, and whether tiling and the Minecraft styles combine on one model was not checked — from [C112], [C113], [C114], [C115], [C116], [C117], [C118]
- **C160** [inference] For general image models the usable pipeline is generate large, snap to grid, quantise to a fixed project palette, and 16x16 is the size where this conversion is reported to break down — from [C124], [C125], [C132], [C134], [C137]
- **C161** [inference] The local 16px Minecraft item model is a licence risk for a public mod: research and personal use release, training data scraped from mods under mixed licences — from [C143], [C144]
- **C162** [inference] Evidence that frontier LLMs draw good 16x16 sprites directly as grids is anecdotal in this run; the one benchmark read covers small open models at 24x24 only — from [C147], [C149], [C150], [C153]

### Q2 — как текстуры делают программно и какие механизмы даёт игра и экосистема — answered with a gap

- **C201** [quoted] Forge 1.20.1 docs: vanilla models put integer tint indices on faces, and code-side BlockColor / ItemColor handlers turn an index into a colour — "which are integers that can then be handled by" — [S22], Texture Tinting, first paragraph
- **C202** [quoted] The tint is a per-channel multiply: each channel of the texture pixel is multiplied by the returned colour's channel and divided by 255 — "(int)((float) base * multiplier / 255.0)" — [S22], Texture Tinting, BlockColor/ItemColor paragraph
- **C203** [quoted] The vanilla grass texture is a white-and-grey template that the biome colour multiplies into green — "For example, the grass texture, untinted, looks white and gray." — [S22], Texture Tinting, BlockColor/ItemColor paragraph
- **C205** [quoted] In an item model derived from the generated model every texture layer gets its own tint index equal to the layer number, so each layer can be coloured independently — "has a tint index corresponding to its layer index." — [S22], Texture Tinting, paragraph on builtin/generated
- **C207** [quoted] Vanilla 1.20.1 grass block model sets tintindex 0 on the top face and on the side faces of a second element that draws a separate overlay texture; the base side texture carries no tint index — "\"texture\": \"#top\",    \"cullface\": \"up\", \"tintindex\": 0 }" — [S23], elements[0].faces.up
- **C208** [quoted] Vanilla ships a ready tinted variant of the cross plant model, `tinted_cross`, identical to `cross` except every face carries tintindex 0 — "\"texture\": \"#cross\", \"tintindex\": 0 }" — [S24], elements[*].faces
- **C209** [quoted] Vanilla 1.20.1 has a spawn-egg template item model with two texture layers, a base sprite and an overlay sprite — "\"layer1\": \"item/spawn_egg_overlay\"" — [S25], textures
- **C212** [quoted] The vanilla 1.20.1 leather helmet item model has two texture layers: a base sprite and an overlay sprite — "\"layer1\": \"minecraft:item/leather_helmet_overlay\"" — [S26], textures (end of file)
- **C215** [quoted] Java Edition 1.19.4 added the atlas source `paletted_permutations`, which generates new textures in memory from base textures and colour palettes at atlas-load time — "is a new type of atlas configuration source used to dynamically generate new textures in memory based on a set of color palettes" — [S27], Technical changes, Paletted Permutations
- **C216** [quoted] Mojang's stated purpose of the source is exactly the many-variants-of-one-base case: recolour without shipping a file per variant — "Color palettes allow you to swap out the colors of a texture without having to supply all files for the variants of a texture in a resource pack" — [S27], Paletted Permutations
- **C217** [quoted] The mechanism is an exact-colour key lookup: the palette file must have as many pixels as the key file, and each key colour is replaced by the palette pixel at the same position — "The number of pixels in each color palette must be the same as that of the" — [S27], Paletted Permutations, permutations parameter
- **C218** [quoted] Key matching is on RGB only; the base pixel's alpha is kept and multiplied by the palette's alpha, so translucent template pixels stay translucent — "The alpha channel is ignored for key matching, but in the resulting texture the alpha channel is multiplied with the color palette" — [S27], Paletted Permutations, permutations parameter
- **C219** [quoted] Base-texture pixels whose colour is not in the key are copied unchanged, so one sprite can mix swappable and fixed colours — "are copied over to the resulting texture as-is" — [S27], Paletted Permutations, permutations parameter
- **C221** [quoted] Vanilla 1.20.1 uses the source for armour trims: one key file `trim_palette` and 14 material palettes (10 materials plus 4 darker variants) in `armor_trims.json` — "\"palette_key\": \"trims/color_palettes/trim_palette\"," — [S28], sources[0]
- **C222** [quoted] The same source type also appears in the vanilla `blocks` atlas, there for the item-icon trim overlays — "\"trims/items/leggings_trim\"," — [S29], sources, paletted_permutations entry
- **C223** [measured] The vanilla trim key and a trim palette are both 8x1 pixel PNGs, and the key is 8 distinct pure greys; the vanilla trim system is therefore an 8-colour ramp swap — Own measurements, M1
- **C224** [quoted] Vanilla seagrass has a `.png.mcmeta` file with an `animation` object that sets frametime 2; its PNG is 16x288 (Own measurements, M1) — "\"frametime\": 2" — [S30], whole file
- **C226** [quoted] Vanilla wheat has one blockstate variant per age value 0–7, each pointing at its own model `wheat_stage0` … `wheat_stage7` — "\"age=7\": {" — [S31], variants
- **C227** [quoted] Each wheat stage model is four lines: parent `minecraft:block/crop` plus one texture key `crop` pointing at the stage's PNG — a growth stage is one texture, no geometry of its own — "\"crop\": \"minecraft:block/wheat_stage0\"" — [S32], whole file
- **C228** [quoted] The `crop` parent draws the single texture on four vertical planes set 4 px in from the block edges (the # layout), lowered by 1 px and unshaded — "\"from\": [ 4, -1, 0 ]," — [S33], elements[0]
- **C229** [quoted] The `cross` parent draws one texture on two diagonal planes (the X layout) and has no tint index; `tinted_cross` is the tintable twin (C208) — "\"rotation\": { \"origin\": [ 8, 8, 8 ], \"axis\": \"y\", \"angle\": 45, \"rescale\": true }," — [S34], elements[0]
- **C230** [measured] The 1.20.1 texture tree holds exactly eight wheat stage files, `block/wheat_stage0.png` … `wheat_stage7.png`; stage0 and stage7 are 16x16 — Own measurements, M2
- **C232** [quoted] Datagen validates that every referenced texture already exists as a file: it generates model and blockstate JSON, not the PNGs themselves — "All references to textures or other data files not generated for data generation must reference existing files on the system." — [S35], Existing Files
- **C234** [quoted] Forge's datagen docs advise building complex models in modelling software beforehand; the data provider then generates child models that apply specific textures through references defined in the parent — "Then, the data provider can generate the children models with specific textures applied through the defined references in the parent complex model." — [S36], Model Files / ModelBuilder
- **C235** [quoted] Moonlight Lib generates textures in code at load time: open a texture, take its colours apart, build a new one — "Open a texture, take its colors apart, and build a new one out of them, in code, at load time." — [S37], intro
- **C236** [quoted] Its standard job is one template recoloured for every installed wood type: extract the target's palette, repaint the template — "extract the palette of the target wood, then" — [S37], intro, second paragraph
- **C237** [quoted] Moonlight's `Respriter` is a palette swap that keeps the template's shapes and takes colours from a supplied palette — "which keeps the shape of one texture and swaps in the colors of a palette." — [S37], The pieces
- **C239** [quoted] Moonlight's texture object carries the mcmeta, so animated templates stay animated after recolouring — "plus its mcmeta, so animated textures keep working." — [S37], The pieces
- **C241** [quoted] Autopalette (Forge mod by Commoble) generates palette swaps of other textures when resource packs load, from JSON files mapping exact input colours to output colours — "For each specific hexidecimal color in the palette map, every pixel in the original texture will be replaced with the output color in the newly generated texture." — [S38], Autotexture Jsons
- **C243** [quoted] GregTech Modern picks textures for generated material blocks and items by an "icon set", not per material — "The material system uses icon sets to determine the textures of generated blocks and items." — [S39], Icon Sets
- **C245** [quoted] GregTech Modern's material docs describe the secondary colour as an overlay over the primary colour on the material — "The secondary color is the overlay over the primary color on the material." — [S40], material builder options
- **C246** [quoted] The icon-set ingot model is a three-layer generated item: base, `_secondary` and `_overlay` sprites — "\"layer1\": \"gtceu:item/material_sets/dull/ingot_secondary\"," — [S41], textures
- **C247** [measured] GregTech Modern's `dull/ingot.png` template is fully greyscale (all 114 opaque pixels have r=g=b, 8 distinct greys) — Own measurements, M3
- **C248** [inference] GregTech Modern's dull-set ingot is a layered generated item model over a greyscale base template, textures are chosen per icon set, and the docs give a material a primary colour and a secondary overlay colour; this is consistent with the tintindex-per-layer mechanism of C205, but only one ingot model was read and the colour-handler code was not — from [C243], [C245], [C246], [C247], [C205]
- **C249** [quoted] Mystical Agriculture's resource-crop model puts tintindex 0 only on the flower texture; the stem texture is drawn untinted on the same planes — "\"texture\": \"#flower\", \"tintindex\": 0 }," — [S42], elements (flower faces)
- **C250** [quoted] Its colour handler registers the crop's flower colour as the block tint, and only for crops flagged as flower-coloured — "if (crop.isFlowerColored() && crop.getCropBlock() != null)" — [S43], onBlockColors
- **C251** [quoted] In the aluminum crop's blockstate, ages 0–6 point at shared `mystical_resource_crop_N` models and age 7 at the crop's own model; other crops' blockstates were not read — "\"model\": \"mysticalagriculture:block/mystical_resource_crop_0\"" — [S44], variants age=0
- **C252** [quoted] The per-crop final-stage model is datagen output (under `src/generated`) that parents the shared stage-7 model and swaps only the flower texture — "\"flower\": \"mysticalagriculture:block/flower/aluminum_flower\"" — [S45], whole file
- **C253** [quoted] Mystical Customization lets pack makers add crops by setting hex colours for the crop, essence and seeds; a crop's textures block can point at built-in template textures such as `mysticalagriculture:block/flower_dust` — "You should set a color for the crop, essence and seeds. Colors are set using HEX values." — [S46], Colors
- **C254** [measured] Mystical Agriculture's `1.20` tree has 4 blank flower templates `flower_*.png` and 132 per-crop `*_flower.png` textures; one of each was measured: `flower_ingot.png` is 32x32 with all 96 opaque pixels grey, `flower/aluminum_flower.png` has 0 grey pixels and 6 colours — Own measurements, M3
- **C256** [quoted] Slynyrd (Raymond Schlitter): a good ramp shifts hue across brightness; ramps that vary only brightness and saturation are a beginner mistake — "Many beginners overlook hue-shifting and end up with 'straight ramps' that only transition brightness and saturation." — [S47], Color ramps
- **C257** [quoted] He names the cost of straight ramps: dull colours that do not harmonise with other ramps — "the resulting colors will lack interest and be difficult to harmonize with ramps of a different hue." — [S47], Color ramps
- **C262** [inference] A single multiplicative tint over a greyscale template yields a straight ramp (one hue, values scaled), which is the ramp shape C256 and C257 call dull; a palette swap (C217, C237, C241) can carry a hue-shifted ramp per variant because each key grey maps to an arbitrary colour — from [C202], [C256], [C257], [C217], [C237], [C241]
- **C263** [quoted] Counter to treating hue shift as a recipe: Pixel Parmesan argues hue-shifting is not a distinct technique but a by-product of light on local colour, and that the framing oversimplifies — "Approaching rendering from this \"hue-shifting\" perspective oversimplifies the paradigm of 2D art as a whole while also failing to provide any explanation for why this would be the case." — [S48], So-called Hue-Shifting
- **C265** [inference] Exact-colour palette swaps (paletted_permutations, Autopalette) need templates drawn with a fixed small set of key colours: any pixel off the key is left unchanged (C219) or unmapped (C241), so anti-aliased or resampled templates break the swap — from [C217], [C219], [C241]

### Q3 — какие модели и анимации агент может делать сам и как проверить результат без человека — answered with a gap

- **C301** [quoted] A block/item model is a JSON file made of cuboid elements with a texture assigned per face, i.e. a plain-text format — "Models are JSON files that determine the visual shape and texture(s) of a block or item. A model consists of cuboid elements, each with their own size, that then get assigned a texture to each face." — [S49], intro
- **C302** [quoted] Element corners are given in 1/16-block units as from/to coordinates — "from: The coordinate of the start corner of the cuboid, specified as [x, y, z]. Specified in 1/16 block units." — [S49], Elements
- **C303** [quoted] from/to are limited to -16..32 and going outside 0..16 is discouraged because of lighting/culling issues — "Values in from and to are limited by Minecraft to the range [-16, 32]. However, it is highly discouraged to exceed [0, 16], as that will lead to lighting and/or culling issues." — [S49], Elements, tip
- **C304** [quoted] Face UV is an explicit [u1, v1, u2, v2] rectangle — "uv: The uv of the face, specified as [u1, v1, u2, v2], where u1, v1 is the top left uv coordinates and u2, v2 is the bottom right uv coordinates." — [S49], Elements, faces
- **C306** [quoted] Flat 2D items need no geometry: parent item/generated builds quads from the texture layers — "minecraft:item/generated: Parent for classic 2D flat item models. Used by most items in the game. Ignores an elements block since its quads are generated from the textures." — [S49], parent
- **C307** [quoted] JSON models can express only cuboids — "JSON models only support cuboid elements; there is no way to express a triangular wedge or anything like it. To have more complicated models, another format must be used." — [S50], intro
- **C308** [quoted] Forge 1.20.1 datagen can generate model and blockstate JSON from code via ModelBuilder — "A ModelBuilder represents a to-be-generated ModelFile. It contains all the data about a model: its parent, faces, textures, transformations, lighting, and loader." — [S36], Model Files
- **C310** [quoted] .bbmodel is Blockbench's project file format and is JSON, but has no complete specification; the source code is the reference — "There is no complete specification of the JSON format at this point in time. To understand it, it is recommended to look at example .bbmodel files and to examine the Blockbench source code: js/formats/bbmodel.js" — [S51], Format documentation
- **C312** [quoted] Blockbench's format table points to GeckoLib for running Blockbench animations in Java mods — "Use GeckoLib to run Blockbench animations in Minecraft mods" — [S52], Notes, footnote 1
- **C313** [quoted] A community MCP server (sosadly/blockbench-mcp, MIT) lets Claude Code drive the live Blockbench desktop editor: Java, Bedrock and GeckoLib models, textures, rigs, animations, screenshots — "The AI can create **Minecraft Java, Bedrock and [GeckoLib](https://github.com/bernie-g/geckolib) models**, paint textures pixel by pixel, build rigs, write keyframe animations, install Blockbench plugins and take screenshots to check its own work." — [S53], intro
- **C314** [quoted] That server returns screenshots inline and has automated audits for z-fighting, floating parts, buried cubes, stretched UVs and left/right mistakes — "`screenshot`, `screenshot_views` and `get_texture` return images inline. `check_model` audits for z-fighting, floating parts, hidden (buried) cubes, stretched UVs and side mistakes" — [S53], Features, Vision and close-up inspection
- **C315** [quoted] It requires the Blockbench desktop app with a bridge plugin; the web app cannot host it — "**Desktop only.** The bridge relies on Node modules (`net`), which the Blockbench web app does not expose to plugins." — [S53], Limitations
- **C316** [quoted] Its GeckoLib workflow claims to yield model, texture and animation files usable in a Forge/Fabric/NeoForge mod (author's claim, not independently measured) — "the GeckoLib workflow produces model, texture and animation files ready for a Forge / Fabric / NeoForge mod" — [S53], FAQ
- **C317** [quoted] A second project (jasonjgardner/blockbench-mcp-plugin) ships a headless stdio MCP server that edits, validates, converts and renders .bbmodel files with no Blockbench running — "A separate stdio MCP server edits, validates, converts and renders `.bbmodel` files directly, without Blockbench running." — [S54], Headless .bbmodel (stdio)
- **C318** [quoted] The headless server exports Java block/item model JSON, Bedrock .geo.json, Bedrock animation JSON and a Java entity model class (Forge 1.7-1.17+ templates) — "`bbmodel_export_modded_entity` | Java entity model class from Blockbench's Modded Entity templates (Forge 1.7–1.17+, Fabric Yarn)" — [S55], tool table
- **C319** [quoted] The headless server renders single views and multi-view contact sheets to PNG — "`bbmodel_render`, `bbmodel_contact_sheet` | Render one view, or several views at once, to PNG" — [S55], tool table
- **C320** [quoted] Headless rendering needs Node 23.6+ and a GPU (WebGPU via Dawn), names a Windows cache path, and installs about 130 MB on first render — "It needs Node 23.6 or newer with npm, and a GPU, because Bun cannot load its WebGPU module yet. The first render installs `three`, `three-blockbench` and Dawn (about 130 MB) into a per-user cache folder (`%LOCALAPPDATA%\blockbench-mcp-headless` on Windows" — [S55], Rendering
- **C321** [quoted] The headless server has geometry validation with a self-test and animation checks, but animation checks skip Molang channels and linearise bezier curves — "The animation checks sample numeric keyframes only. Bezier curves are sampled as straight lines, and channels with Molang expressions are skipped." — [S55], Limits
- **C324** [prior] Blockbench itself has no documented official CLI/headless render mode; automation goes through its JavaScript plugin API or third-party tools — not fetched in this run
- **C325** [quoted] GeckoLib animatables are driven by three resource files: .geo.json model, .png texture, .animation.json — "This method should return the `ResourceLocation` path to your `.geo.json` model file for this animatable" — [S56], getModelResource
- **C326** [quoted] File locations are fixed: assets/<modid>/geo and assets/<modid>/animations — "Model files have to go in `resources/assets/<modid>/geo`, or in any sub-directory thereof." — [S56], File Locations
- **C327** [quoted] GeckoLib's own docs assume the JSON files are produced in Blockbench with its plugin, not hand-written — "This page and by extension this wiki assumes you are utilising Blockbench to create your model and animation jsons" — [S57], intro
- **C328** [quoted] Only groups (bones) can be animated in a GeckoLib model, so every cube must sit in a group — "Models consist of cubes and groups. Only groups can be animated, so place all your cubes in groups." — [S57], Grouping
- **C329** [quoted] A Java entity model class cannot be converted back to a GeckoLib model; the .bbmodel is the source of truth to keep — "Unfortunately, there is no good way to convert a .java file directly into a GeckoLib model - make sure you save your .bbmodel project files so you always have them!" — [S57], Converting an Existing Model
- **C330** [quoted] GeckoLib is available for Forge, Fabric and NeoForge — "Available for Forge, Fabric, and NeoForge" — [S58], header
- **C331** [measured] GeckoLib still ships for Minecraft 1.20.1 on Forge and Fabric: Modrinth API lists 4.8.5 for 1.20.1 (forge and fabric) published 2026-11-21, 71 versions for 1.20.1 in total — Own measurements, M4
- **C332** [quoted] AzureLib is a fork of GeckoLib 4.x with the same feature class (keyframe animation engine) — "AzureLib represents a branch derived from Geckolib 4.x, serving as an animation engine tailored for Minecraft Mods." — [S59], intro
- **C333** [prior] The vanilla alternative is a Java EntityModel class with LayerDefinition/CubeListBuilder (addBox with pixel sizes and texOffs box-UV), animated in setupAnim code; no extra dependency, but geometry lives in code and box UV is forced — not fetched in this run
- **C334** [inference] A tool can compile Bedrock-format `.geo.json` and animation JSON from `.bbmodel` (C318), the kind of files GeckoLib's docs expect to come out of Blockbench (C327), so an agent need not hand-write them; whether that tool's output loads in GeckoLib was not checked — from [C318], [C312], [C327]
- **C335** [quoted] block-model-renderer (npm, MPL-2.0) renders blocks, items and custom model JSON to image files on Node.js — "Render any block, item, or custom model JSON, with full support for vanilla resource pack features. On Node renders go to image files or buffers" — [S60], intro
- **C337** [quoted] It can render with the gui display transform by default, i.e. the inventory-icon view — "Display transform: a model display context (`"gui"`, `"fixed"`, etc) or custom rotation/translation/scale." — [S60], Options, display
- **C339** [quoted] minecraft-render (npm, co3moz) renders block models from a Minecraft .jar with THREE.js to 1000x1000 PNG — "Renders minecraft block models from .jar file using `THREE.js`. " — [S61], intro
- **C340** [quoted] The Python fork minecraft-render-py needs a display server (xvfb) for headless CI and its docs are unfinished — "If you are automating generation process on github or similar CI environments, make sure you configured display server. `xvfb` can be used for this purpose." — [S62], Headless render and CI
- **C341** [quoted] IconExporter (CyclopsMC) is an in-game mod that exports item and block icons to files — "Exports item and block icons to files" — [S63], intro
- **C342** [quoted] IconExporter has a 1.20.1-1.3.0 build for Minecraft 1.20.1 on Forge and requires CyclopsCore — "Requires CyclopsCore version 1.18.14 or higher." — [S64], version page, Changes
- **C343** [quoted] Forge 1.20.1 GameTest has a dedicated server run that returns the failed-test count as exit code — "The Game Test Server is a special configuration which runs a build server. The build server returns an exit code of the number of required, failed Game Tests." — [S65], Game Test Server Run Configuration
- **C345** [quoted] Fabric (not Forge) provides client game tests for client-side features, with a takeScreenshot call — "Minecraft provides the game test framework for testing server-side features. Fabric additionally provides client game tests for testing client-side features, similar to an end-to-end test." — [S66], Game Tests
- **C347** [quoted] HeadlessMC launches the game in headless mode by patching LWJGL to not render anything — "This is achieved by patching the LWJGL library to not render anything" — [S67], Headless Launch
- **C349** [inference] The Forge 1.20.1 GameTest docs read here describe only a server run, and Fabric's docs present client game tests as something Fabric adds; so no first-party client screenshot test for Forge 1.20.1 was found in the pages read, and an in-game visual check would need a third-party icon-exporter mod or own client code — from [C343], [C345], [C341]
- **C350** [inference] Of the tools read here, the out-of-game renderers for JSON models cover block/item models, and GeckoLib/entity models are rendered out of game by the Blockbench-based tools — from [C335], [C339], [C319], [C313]
- **C351** [quoted] A 2026 arXiv report states that strong multimodal LLMs fail at one-stage generation of a Minecraft skin UV atlas — "Figure 1: Current Powerful MLLMs fail to generate Minecraft skin from character concept." — [S68], Figure 1 caption
- **C352** [quoted] The same report avoids asking one model to solve semantics, 3D rendering and strict UV validity together; it adds a deterministic decoder — "BLOCK thus avoids asking a single model to simultaneously solve semantic understanding, 3D rendering, and strict UV atlas validity." — [S68], Introduction
- **C353** [quoted] The author of sosadly/blockbench-mcp states an LLM cannot compute many cube coordinates itself, so tools do the math — "An LLM cannot compute 200 sets of `[from,to]` in its head, so these tools do the math." — [S53], Features, Procedural detail
- **C354** [quoted] The same README names the default failure it gates against: models of a few flat boxes — "with quality gates between the stages so a model ends up with real detail instead of 15 flat boxes" — [S53], intro
- **C355** [quoted] Left/right confusion is treated as a known agent error: the tool refuses a cube whose coordinate contradicts its declared side — "`add_cube` / `add_group` take `side:"left"|"right"` and **refuse** a call whose coordinate contradicts it, and every screenshot is labelled with which image edge is the model's right." — [S53], Features, Left and right stay correct
- **C356** [quoted] Box-UV formats (Java entity) cannot give thin cubes their own pixels; Java block models rotate one axis in 22.5 degree steps — "Formats with box UV only (e.g. Java entity / OptiFine) cannot give thin cubes their own pixels; `pack_uv` warns about them. Java block models only rotate one axis in 22.5° steps" — [S53], Limitations
- **C357** [quoted] Analogous code-CAD report (OpenSCAD, one practitioner, no numbers): LLM geometry parses and renders yet is spatially wrong — "The code parses. It even renders. But the geometry is wrong in subtle, infuriating ways — walls float, windows clip through floors, rotations go the wrong direction." — [S69], intro
- **C358** [quoted] The same post claims spatial reasoning degrades with size of free-form geometry and recommends a constrained intermediate format — "The model's spatial reasoning collapses past about 20 lines of geometry." — [S69], closing recommendations
- **C360** [quoted] ImageMagick montage builds an array (proof sheet) of thumbnails from many images — "The "magick montage" command is designed to produce an array of thumbnail images. Sort of like a proof sheet of a large collection of images." — [S70], Montage, Introduction
- **C362** [prior] Nearest-neighbour upscale of 16x16 textures before tiling is done with `-filter point` (or `-scale`) in ImageMagick or `Image.NEAREST` in Pillow; pixel diffing with `magick compare` — not fetched in this run

### Q4 — что можно публиковать на CurseForge и Modrinth, правила площадок об ИИ и реакция сообщества — answered with a gap

- **C401** [quoted] Modrinth Content Rules (last modified 2026-08-13) forbid projects that are entirely or primarily made of generative-AI output — "Projects cannot be entirely or primarily comprised of content created or derived from generative AI output." — [S71], section 6, Usage of Generative "AI"
- **C402** [quoted] Modrinth: a project whose contents are primarily or entirely AI output may not be published publicly — "Projects may not be published publicly if the contents are primarily or entirely a product of AI output." — [S71], section 6.2
- **C403** [quoted] Modrinth requires the AI content disclosure when a project includes any asset that is primarily or entirely AI output, and also when a substantial portion of code is AI output — "the project includes any assets that are primarily or entirely a product of AI output." — [S71], section 6.1
- **C404** [quoted] Modrinth bans AI-created or AI-derived images anywhere on the project page (gallery, icon, description) — "No images uploaded to a gallery, icon, description, or any other part of a project page may be created or derived from generative AI output." — [S71], section 6.2
- **C405** [quoted] Modrinth's announcement summarises the policy as: fully or nearly fully AI-generated projects are no longer allowed, AI-assisted ones with primary human contribution are allowed with disclosure — "AI-assisted projects, where the human creator contributed in a primary and significant way, are allowed but must disclose that AI assistance was used." — [S72], New AI policy, "To summarize"
- **C406** [quoted] Modrinth's grace period for AI-policy and disclosure reports ended on 2026-09-27, so the policy is enforced at the date of this run — "The 45-day grace period ends on September 27, 2026." — [S72], Enforcement
- **C407** [quoted] Modrinth help centre defines the banned class as projects made by prompting and testing with little or no human-made content — "Any project that is created entirely or almost entirely through the use of generative AI with little-to-no human input beyond prompting or testing cannot be published on Modrinth publicly." — [S73], Entirely AI-generated Projects
- **C408** [quoted] Modrinth: heavy AI use in one portion of a project may be permitted if another portion is substantially genuine human work and AI use is disclosed — "Content that makes heavy use of generative AI in specific portions of a project may still be permitted if genuine human code/assets/writing makes up a significant portion of the project in another portion, so long as you are honest with your users about your usage of AI." — [S73], Ok, but what projects really?
- **C409** [quoted] Modrinth: an AI-generated project can usually stay unlisted but is kept out of public search — "In most cases, such projects can still be made unlisted, but will be prohibited from appearing in public search results." — [S73], What happens to AI projects?
- **C410** [quoted] Modrinth: where generative AI is effectively the primary developer, artist or author, the content may be unsuitable for the platform — "If the answer instead reveals that generative AI has played a role in your content worthy of being the primary developer, artist, or author, your content may not be suitable for Modrinth." — [S73], What makes for a "substantial portion" of the project?
- **C411** [quoted] Modrinth's asset disclosure covers AI-assisted as well as AI-generated textures, models and logos — "If a project distributes any AI-generated or AI-assisted assets such as textures, models, or logos, this must be disclosed." — [S74], AI-generated assets
- **C412** [quoted] Modrinth says disclosing AI use does not lower search placement or revenue — "Disclosing the usage of generative AI in your project has no negative impact on your project's place in search results or revenue." — [S73], Does the use of AI impact a project's Monetization?
- **C413** [quoted] Modrinth lets users filter out projects by disclosure in search — "Projects with disclosures you’d like to avoid can be filtered out in search under the “Advanced exclusions” options." — [S72], Content disclosures
- **C414** [quoted] Modrinth requires rights to everything uploaded and forbids reuploads without the original author's explicit permission, except licence-abiding forks — "Content may not be directly reuploaded from another source without explicit permission from the original author." — [S71], section 4, Copyright and Reuploads
- **C415** [quoted] Modrinth requires crediting each original source for permitted third-party content — "When uploading content you have the necessary permissions to distribute but did not author yourself, you must make a meaningful effort to credit each original source properly." — [S71], section 4
- **C416** [quoted] Modrinth has a derivative-content disclosure for projects using substantial parts of another work — "If your project is a fork or uses substantial parts of another work, you must declare it as a derivative work of the source work." — [S72], Contains derivative content
- **C417** [quoted] Modrinth licensing guide: a work with no licence is all rights reserved, which in practice means no modification or redistribution — "Without a license software is considered proprietary and all rights reserved." — [S75], What if I don't want to choose a license?
- **C418** [quoted] CurseForge Moderation Policies (modified 2026-10-22) contain one AI rule: AI-modified showcase images that may misrepresent the mod need a visible disclaimer — "Any AI-modified (generated, edited, or enhanced) showcase image that may misrepresent the mod’s actual content must include a clear and visible disclaimer (e.g., on the image or in the description)." — [S76], General CurseForge Moderation Policies, AI Misleading Content Disclosure
- **C419** [inference] CurseForge's published moderation policies do not ban or require labelling of AI-generated in-mod assets or code; the only AI rule concerns showcase images (the CurseForge Terms of Use and Mod Author Terms were not read) — from [C418], [C422]
- **C420** [quoted] CurseForge: using copyrighted content of other works or authors requires a licence or explicit permission, plus credit — "If you upload a project that is using copyrighted content from other registered works or belonging to other authors on the platform, you must either be allowed to do so under their respective license or have explicit permission to do so." — [S76], Content Copyright
- **C421** [quoted] CurseForge follows the game developer's EULA and requires distinct content and assets — "CurseForge follows game developers EULA and ToS, and so should your projects." — [S76], General Guidelines
- **C422** [quoted] A CurseForge Ideas request (2026-02-10, status "Future consideration", 24 votes) asks for AI labelling and filtering — "These changes would not ban AI usage entirely, but rather encourage transparency and higher-quality submissions." — [S77], idea body
- **C423** [quoted] CurseForge (2023 guideline update): outright clones are not allowed without the owner's explicit permission even if the licence allows them, and reuse always needs credit — "Outright cloning is not allowed on CurseForge, unless you have explicit permission from the content owner, even if their license allows that." — [S78], Updated Guidelines
- **C424** [quoted] Minecraft EULA defines a mod as original work without a substantial part of Mojang's code or content — "Remember that a Mod means something that is your original work and that does not contain a substantial part of our code or content." — [S79], USING mods
- **C425** [quoted] Minecraft EULA: Mojang owns copies, substantial copies and derivatives of its property; a block's textures are given as the example of what it owns — "We will however own things that are copies (or substantial copies) or derivatives of our property and creations - but if you create original things, they aren't ours." — [S79], CONTENT
- **C426** [quoted] Minecraft EULA names a single block's textures and look and feel as Mojang-owned — "a single Minecraft block (including its textures and its “look and feel”) - we own that;" — [S79], CONTENT
- **C427** [quoted] Minecraft Usage Guidelines count textures, graphics and models as Mojang "assets" and reserve all rights in assets and derivatives — "All rights (including copyright, trademark rights, and related rights) in the name, brand, assets, and any derivatives are and will remain owned by Mojang and Microsoft" — [S80], All uses
- **C428** [quoted] Usage Guidelines define a mod as an original plug-in without a substantial part of Mojang's copyrightable code or content — "By modifications (“mods”) we mean an original plug-in that extends the functionality of Minecraft but that doesn’t contain a substantial part of our copyrightable code or content." — [S80], modifications
- **C429** [inference] A mod texture derived by code from a vanilla sprite is a derivative of a Mojang asset: Mojang claims ownership of it, and it is tolerated only while the mod does not contain a "substantial part" of Mojang content, a threshold Mojang decides and no page read here quantifies — from [C424], [C425], [C426], [C427]
- **C430** [quoted] US Copyright Office (Part 2, 2025-01-29): AI output is protectable only where a human determined sufficient expressive elements; prompts alone are not enough — "It concludes that the outputs of generative AI can be protected by copyright only where a human author has determined sufficient expressive elements." — [S81], NewsNet Issue 1060
- **C431** [quoted] US Copyright Office: human-authored work perceptible in the output, or creative arrangement or modification of output, can be protected; AI material inside a larger human work does not bar copyright — "This can include situations where a human-authored work is perceptible in an AI output, or a human makes creative arrangements or modifications of the output, but not the mere provision of prompts." — [S81], NewsNet Issue 1060
- **C432** [quoted] OpenAI terms assign output to the user only as between the parties and to the extent the law permits — "As between you and OpenAI, and to the extent permitted by applicable law, you (a) retain your ownership rights in Input and (b) own the Output." — [S82], Content, Ownership of content
- **C433** [inference] Under the US position, purely prompted textures may carry no copyright, and a service's terms assign output only to the extent the law permits; human-edited or human-arranged parts can be protected — from [C430], [C431], [C432]
- **C434** [quoted] Faithful licence allows use, modification and distribution of its textures in mods, with credit, link and licence text — "Use, modify, and distribute our work for both Minecraft and non-Minecraft projects." — [S83], You may do the following
- **C435** [quoted] Faithful licence forbids training AI models on its work — "Train neural networks or AI models on our work." — [S83], You may not do the following
- **C436** [quoted] Faithful may not be used as a stand-in for vanilla graphics where vanilla textures would not be allowed — "Use our work as a substitute for Minecraft's graphics when default textures otherwise wouldn't be allowed." — [S83], You may not do the following
- **C437** [quoted] Modrinth's stated reason for the AI policy: a fully AI-generated project is not the uploader's own work — "generating it completely with AI is not your work" — [S72], introduction
- **C438** [quoted] Modrinth states that AI images are overwhelmingly unpopular in any context — "we've found that AI images are overwhelmingly unpopular in any context" — [S73], AI Icons and branding images
- **C439** [quoted] Modrinth forbids harassing creators over AI disclosures — "In addition, we will not tolerate witch-hunting on the Modrinth platform." — [S72], Enforcement
- **C440** [quoted] Planet Minecraft rules forbid AI content that replaces the submitter's own work, cover images included — "Generate AI content that replaces your own work in submissions. This includes cover images to promote otherwise human generated content." — [S84], submissions, "Do not" list
- **C441** [quoted] Mojang's vanilla game director said (reported 2025-03-24) that Mojang wants its own teams, not generative AI, to make its games — "So for us, we really want it to be our teams that make our games." — [S85], Agnes Larsson quote, citing IGN
- **C442** [quoted] Steam requires disclosure of AI-made content that ships with a game and is consumed by players (artwork included) — "Pre-Generated: Any kind of content that ships with your game and is consumed by players that is created with the help of AI tools during development." — [S86], AI section of Content Survey
- **C443** [quoted] Nexus Mods (reported 2026-07-31) keeps AI mods allowed but makes mis-tagging undisclosed AI use a moderation offence — "Failure to correctly tag mods, including undisclosed generative AI usage, may be subject to moderation action." — [S87], quote of Nexus blog post
- **C444** [quoted] Backlash case outside Minecraft: Pearl Abyss apologised after players found AI-generated art in Crimson Desert (2026-03-22) — "has issued an official apology after players discovered several instances of AI-generated art in the game" — [S88], first paragraph
- **C445** [quoted] Accepting evidence, weak: a fully ChatGPT-retextured resource pack is listed on Modrinth, with 77 downloads and 0 followers at fetch time — "This resource pack was fully retextured using ChatGPT." — [S89], project summary
- **C446** [quoted] Dissent on CurseForge Ideas: one commenter who uses AI for mods rejects the premise that manual work means higher quality — "As someone who has designed mods manually, I don't find my scrutiny of quality to be any less when I use AI to help me." — [S77], comment, 22 Aug
- **C447** [snippet] Project Zomboid removed AI-generated Build 42 loading screens after community anger; Arc Raiders' developer is replacing AI material after backlash — "Project Zomboid's AI-generated Build 42 loading screens draw ire from the community, get taken out of the game." — [S90], search snippet
- **C448** [inference] If the project is what its brief describes (code, textures and models all produced by AI agents, the human only prompting and testing — a premise of this report, not a sourced fact), Modrinth's rules as written exclude public listing, and at minimum require the AI disclosure and human-made page images; the CurseForge Moderation Policies page read does not exclude it — from [C401], [C402], [C407], [C408], [C404], [C419]

## Disconfirming evidence

- [C158], [C159], [C160] — query: `AI pixel art "not real pixel art" OR "fake pixel art" 16x16 low resolution fails illegible criticism PixelLab OR "Retro Diffusion"` — общая критика ИИ-пиксель-арта: [C132], [C155], [C156], [C157], [C123]; против PixelLab или Retro Diffusion именно на 16×16 ничего не найдено, независимого теста тоже.
- [C160] — HN-тред 50171379 прочитан как противная сторона тезиса «общая модель плюс постобработка достаточно»: комментаторы его оспаривают [C156], [C157]; один возражает, что LLM, рисующая напрямую, этих проблем не имеет [C149].
- [C248], [C262] — query: `minecraft mod tinted grayscale textures look flat drawbacks tintindex color multiply loses hue shifting hand-drawn textures instead` — страницы, прямо называющей недостаток, не найдено; контр-свидетельство получено чтением репозитория: [C254].
- [C215], [C217] — query: `paletted_permutations limitations armor trim palette 8 colors cannot add colors mod issue "paletted_permutations" problem` — только сниппеты, страницы не читались: палитра отделки «принимает 8 образцов» (согласуется с [C223]) и несовместимость с OptiFine CIT; источника от сопровождающих о жёстком пределе размера палитры не найдено.
- [C256], [C257] — query: `Pixel Parmesan "hue-shifting" oversimplifies color pixel art critique` — найдено и прочитано: [C263].
- [C317], [C334], [C350] — query: `AI generated Blockbench models bad quality problems "Blockbench" LLM Claude model looks wrong z-fighting UV texture stretched` — про Blockbench или Claude ничего; только аналоги [C357], [C358] и [C351]. Независимых отчётов о моделях, сделанных агентом, — ни положительных, ни отрицательных.
- [C349] — в источнике: HeadlessMC в безголовом режиме ничего не рендерит [C347], против допущения, что безголовый клиент снимет скриншот.
- [C419], [C448] — query: `popular Minecraft mod made with AI well received OR accepted "AI-generated" textures downloads no backlash CurseForge allows AI mods` — популярного хорошо принятого мода с ИИ-ассетами не найдено; слабые принимающие свидетельства: [C445], [C446].
- [C401], [C448] — query: `Modrinth AI policy reaction criticism "vibe coded" mods unfair OR overreach creators respond August 2026` — сообщений о протесте авторов против политики не найдено; тред на форуме MCreator не открылся и не цитируется.
- [C444] — query: `Minecraft mod removed AI-generated textures after criticism "AI" textures replaced apology mod developer Modrinth CurseForge 2025 2026` — случаев именно в Minecraft не найдено, только вне его: [C444], [C447].

## Coverage

| Channel | Read | Gap |
|---|---|---|
| Users | [S7], [S16], [S15], [S77] | Reddit не источник; Discord-сообщества моддеров не читаются |
| Specialists | [S22], [S27], [S37], [S56], [S1], [S4], [S71], [S76], [S79], [S81] | minecraft.wiki закрыт проверкой на бота; Terms of Use CurseForge не читались |
| Data | [S68], [S14], Own measurements M1–M4 | измерений качества сервисов на 16×16 и качества моделей от агента не найдено |
| Adversarial | [S20], [S19], [S48], [S88], [S87], [S67] | случаев отката именно в моддинге Minecraft не найдено |

Перерасход потолка (четыре поиска и три полных чтения на подвопрос), во всех четырёх подвопросах: Q1 — 4 поиска + 2 по продлению + 1 опровергающий + 4 запроса к HN Algolia, около 14 коротких страниц; Q2 — 5 поисков + 3 опровергающих, больше трёх чтений (в основном JSON-файлы по пять строк); Q3 — 5 поисков + 1 опровергающий, около 14 коротких страниц; Q4 — 9 поисков, около 14 страниц, большей частью читались отрывки по ключевым словам. Причина одна: в каждом подвопросе несколько тем, каждой нужна своя первичная страница. Все четыре ledger'а остановлены пределом 20 000 знаков; часть собранных утверждений ради него вырезана.

Инструменты: только встроенные поиск и `curl`. Не открылись: minecraft.wiki (проверка на бота), retrodiffusion.ai/terms (страница строится скриптом), mcreator.net и videogameschronicle.com (стена для ботов), страницы PIXL и Asset Forge (404). Цитаты Q1 сверены с локально сохранённым текстом страниц, не повторной загрузкой; девять цитат Q3 (C325–C330, C332, C341, C357) скопированы вручную из вывода curl без машинной сверки. В одной выдаче поиска (Q4) были страницы профилей linux.do с текстом, обращённым к ИИ-ассистентам; он проигнорирован, страницы не использованы.

## Own measurements

### M1 — размеры ванильной палитры отделки и анимированной текстуры (1.20.1)

Command: чтение PNG IHDR скриптом на стандартной библиотеке (`q2/gray.py`, `struct.unpack('>II', d[16:24])`) на файлах из `https://raw.githubusercontent.com/misode/mcmeta/1.20.1-assets/assets/minecraft/textures/…`
Output: `trims/color_palettes/trim_palette (8, 1)`; `trims/color_palettes/gold (8, 1)`; `block/seagrass (16, 288)`; `img/trim_palette.png (8, 1) opaque px 8 r==g==b 8 distinct colours 8`

### M2 — файлы стадий пшеницы в 1.20.1

Command: `curl -sL https://api.github.com/repos/misode/mcmeta/git/trees/1.20.1-assets?recursive=1 | grep -o '"path": *"assets/minecraft/textures/block/wheat[^"]*"'`
Output: ровно `wheat_stage0.png` … `wheat_stage7.png` (8 строк). IHDR: `block/wheat_stage0 (16, 16)`, `block/wheat_stage7 (16, 16)`

### M3 — серые шаблоны GregTech Modern и Mystical Agriculture

Command: `python -I gray.py img/*.png` (считает непрозрачные пиксели с r==g==b)
Output: `gt_ingot.png (16, 16) opaque px 114 r==g==b 114 distinct colours 8`; `gt_ingot_overlay.png (16, 16) opaque px 11 r==g==b 11 distinct colours 1`; `ma_flower_ingot.png (32, 32) opaque px 96 r==g==b 96 distinct colours 9`; `ma_aluminum_flower.png (32, 32) opaque px 96 r==g==b 0 distinct colours 6`; `spawn_egg.png (16, 16) opaque px 68 r==g==b 68`; `grass_top.png (16, 16) opaque px 256 r==g==b 256`. Дерево Mystical Agriculture `1.20` (GitHub trees API): 132 пути `textures/block/flower/*_flower.png`, 4 заготовки `textures/block/flower_*.png`, 136 сгенерированных моделей `*_crop.json`

### M4 — GeckoLib под Minecraft 1.20.1

Command: `curl -sL "https://api.modrinth.com/v2/project/geckolib/version?game_versions=%5B%221.20.1%22%5D"` (2026-11-23)
Output: 71 запись; новейшие `4.8.5 ['forge'] ['1.20.1'] 2026-11-21` и `4.8.5 ['fabric'] ['1.20.1'] 2026-11-21`

## Open questions

- Q1 — ledger cap / no new information — независимый тест PixelLab и Retro Diffusion на 16×16 не найден; решает собственный замер: одна партия предметов и одна блочная текстура через каждый API, прогон через числовые тесты и контактный лист.
- Q1 — blocked: страница условий Retro Diffusion строится скриптом — лицензия на вывод Retro Diffusion не прочитана; решает чтение страницы в браузере.
- Q1 — no new information — сочетается ли тайлинг Retro Diffusion со стилями Minecraft на одной модели; открытой модели для бесшовных блочных текстур 16×16 не найдено.
- Q1 — no new information — умеют ли передовые LLM рисовать годные спрайты 16×16 напрямую сеткой индексов: свидетельства единичные; решает собственный замер на 10–20 спрайтах проекта.
- Q2 — no new information — опубликованного описания оффлайн-конвейера (Python/Pillow, ImageMagick), генерирующего варианты текстур мода, не найдено; сравнения сдвига тона и замены палитры на одном спрайте тоже.
- Q2 — ledger cap — не прочитаны: Resourceful Bees и моды Chickens, документация NeoForge, справочник JSON-генераторов Dynamic Asset Generator, Java-код ванильных поставщиков цвета.
- Q3 — no new information — измеренных данных о качестве моделей Blockbench/GeckoLib, сделанных агентом, нет; решает пилот: одно животное через безголовый MCP-сервер, рендер, оценка.
- Q3 — not run — ни один инструмент (два MCP-сервера Blockbench, `block-model-renderer`) не запускался на Windows 11; совпадение рендера вне игры с видом в игре не найдено.
- Q3 — no new information — автоматический клиентский скриншот на Forge 1.20.1: первичного механизма не найдено; страница версии IconExporter прочитана, сам мод не запускался.
- Q4 — ledger cap — Terms of Use и Mod Author Terms CurseForge не прочитаны: вывод «у CurseForge нет правила об ИИ-ассетах» стоит на одной странице Moderation Policies.
- Q4 — no new information — как модераторы Modrinth применяют «преимущественно ИИ» после 2026-09-27; решает вопрос в поддержку Modrinth с описанием проекта.
- Q4 — ledger cap — не собраны: отчёт Бюро авторского права США целиком (читан только пресс-релиз), позиции ЕС и Великобритании, порог «существенной части» ванильного контента, коллекции ассетов CC0/CC-BY, условия других сервисов генерации, правила Discord-сообществ.
- Q1 — blocked: страницы PIXL [S17] и Asset Forge [S18] отдают 404 — C153 и C154 не проверены; решает архивная копия или новый адрес этих страниц.
- Q3 — not fetched — C324, C333 и C362 записаны по памяти, страниц под ними нет; решает чтение документации Blockbench, Forge (EntityModel) и ImageMagick.
- Q4 — blocked: правила Planet Minecraft [S84] и статья Destructoid [S90] закрыты проверкой на бота — C440 и C447 не проверены; решает чтение в браузере.
- Следующие прогоны: (1) «Опыт и реакция сообщества на ИИ-ассеты в модах Minecraft» отдельным подвопросом, с чтением Terms CurseForge и практики модерации Modrinth; (2) «Лицензии открытых коллекций ассетов и авторское право на ИИ-вывод вне США»; (3) пилотные замеры из пунктов выше — это не поиск, а эксперимент в репозитории.

## Sources

- [S1] pixellab.ai — pixellab-api — https://pixellab.ai/pixellab-api — passage
- [S2] pixellab.ai — mcp — https://pixellab.ai/mcp — passage
- [S3] www.pixellab.ai — termsofservice — https://www.pixellab.ai/termsofservice — passage
- [S4] raw.githubusercontent.com — main/README.md — https://raw.githubusercontent.com/Retro-Diffusion/api-examples/main/README.md — passage
- [S5] www.spritefusion.com — api/mcp — https://www.spritefusion.com/docs/pixel-art-generator/api/mcp — passage
- [S6] help.scenario.com — articles/1753861053-pixel-snapper-the-essentials — https://help.scenario.com/articles/1753861053-pixel-snapper-the-essentials — passage
- [S7] danielball.com — notes/ai_image_generation.html — https://danielball.com/notes/ai_image_generation.html — passage
- [S8] dev.to — jenissimo/how-to-tame-your-ai-pixel-art-3pk5 — https://dev.to/jenissimo/how-to-tame-your-ai-pixel-art-3pk5 — passage
- [S9] rangy.ai — blog/ai-pixel-art — https://rangy.ai/blog/ai-pixel-art — passage
- [S10] raw.githubusercontent.com — main/README.md — https://raw.githubusercontent.com/Hugo-Dz/spritefusion-pixel-snapper/main/README.md — passage
- [S11] raw.githubusercontent.com — main/README.md — https://raw.githubusercontent.com/KennethJAllen/proper-pixel-art/main/README.md — passage
- [S12] huggingface.co — main/README.md — https://huggingface.co/yuuki14202028/minecraft-item-16px/raw/main/README.md — passage
- [S13] huggingface.co — yuuki14202028/minecraft-item-16px — https://huggingface.co/yuuki14202028/minecraft-item-16px — snippet only
- [S14] huggingface.co — AINovice2005/pixel-art-bench — https://huggingface.co/blog/AINovice2005/pixel-art-bench — passage
- [S15] hn.algolia.com — items/50172003 — https://hn.algolia.com/api/v1/items/50172003 — passage
- [S16] dev.to — jucelinux/i-taught-claude-opus-5-to-draw-with-no-image-model-just-code-then-i-asked-for-a-self-portrait-2894 — https://dev.to/jucelinux/i-taught-claude-opus-5-to-draw-with-no-image-model-just-code-then-i-asked-for-a-self-portrait-2894 — passage
- [S17] github.com — SimplyLiz/PIXL — https://github.com/SimplyLiz/PIXL — snippet only
- [S18] matthewdeaves.github.io — assetforge — https://matthewdeaves.github.io/assetforge — snippet only
- [S19] hn.algolia.com — items/50488244 — https://hn.algolia.com/api/v1/items/50488244 — passage
- [S20] hn.algolia.com — items/50219021 — https://hn.algolia.com/api/v1/items/50219021 — passage
- [S21] hn.algolia.com — items/50219658 — https://hn.algolia.com/api/v1/items/50219658 — passage
- [S22] docs.minecraftforge.net — models/tinting — https://docs.minecraftforge.net/en/1.20.1/resources/client/models/tinting/ — passage
- [S23] raw.githubusercontent.com — block/grass_block.json — https://raw.githubusercontent.com/misode/mcmeta/1.20.1-assets/assets/minecraft/models/block/grass_block.json — passage
- [S24] raw.githubusercontent.com — block/tinted_cross.json — https://raw.githubusercontent.com/misode/mcmeta/1.20.1-assets/assets/minecraft/models/block/tinted_cross.json — passage
- [S25] raw.githubusercontent.com — item/template_spawn_egg.json — https://raw.githubusercontent.com/misode/mcmeta/1.20.1-assets/assets/minecraft/models/item/template_spawn_egg.json — passage
- [S26] raw.githubusercontent.com — item/leather_helmet.json — https://raw.githubusercontent.com/misode/mcmeta/1.20.1-assets/assets/minecraft/models/item/leather_helmet.json — passage
- [S27] www.minecraft.net — article/minecraft-java-edition-1-19-4 — https://www.minecraft.net/en-us/article/minecraft-java-edition-1-19-4 — passage
- [S28] raw.githubusercontent.com — atlases/armor_trims.json — https://raw.githubusercontent.com/misode/mcmeta/1.20.1-assets/assets/minecraft/atlases/armor_trims.json — passage
- [S29] raw.githubusercontent.com — atlases/blocks.json — https://raw.githubusercontent.com/misode/mcmeta/1.20.1-assets/assets/minecraft/atlases/blocks.json — passage
- [S30] raw.githubusercontent.com — block/seagrass.png.mcmeta — https://raw.githubusercontent.com/misode/mcmeta/1.20.1-assets/assets/minecraft/textures/block/seagrass.png.mcmeta — passage
- [S31] raw.githubusercontent.com — blockstates/wheat.json — https://raw.githubusercontent.com/misode/mcmeta/1.20.1-assets/assets/minecraft/blockstates/wheat.json — passage
- [S32] raw.githubusercontent.com — block/wheat_stage0.json — https://raw.githubusercontent.com/misode/mcmeta/1.20.1-assets/assets/minecraft/models/block/wheat_stage0.json — passage
- [S33] raw.githubusercontent.com — block/crop.json — https://raw.githubusercontent.com/misode/mcmeta/1.20.1-assets/assets/minecraft/models/block/crop.json — passage
- [S34] raw.githubusercontent.com — block/cross.json — https://raw.githubusercontent.com/misode/mcmeta/1.20.1-assets/assets/minecraft/models/block/cross.json — passage
- [S35] docs.minecraftforge.net — 1.20.1/datagen — https://docs.minecraftforge.net/en/1.20.1/datagen/ — passage
- [S36] docs.minecraftforge.net — client/modelproviders — https://docs.minecraftforge.net/en/1.20.1/datagen/client/modelproviders/ — passage
- [S37] raw.githubusercontent.com — Moonlight/Texture-Manipulation.md — https://raw.githubusercontent.com/wiki/MehVahdJukaar/Moonlight/Texture-Manipulation.md — passage
- [S38] raw.githubusercontent.com — HEAD/README.md — https://raw.githubusercontent.com/Commoble/autopalette/HEAD/README.md — passage
- [S39] raw.githubusercontent.com — Materials-and-Elements/Material-Icon-Sets.md — https://raw.githubusercontent.com/GregTechCEu/GregTech-Modern/1.20.1/docs/content/Modpacks/Materials-and-Elements/Material-Icon-Sets.md — passage
- [S40] raw.githubusercontent.com — Materials-and-Elements/01-Material-Creation.md — https://raw.githubusercontent.com/GregTechCEu/GregTech-Modern/1.20.1/docs/content/Modpacks/Materials-and-Elements/01-Material-Creation.md — passage
- [S41] raw.githubusercontent.com — dull/ingot.json — https://raw.githubusercontent.com/GregTechCEu/GregTech-Modern/1.20.1/src/main/resources/assets/gtceu/models/item/material_sets/dull/ingot.json — passage
- [S42] raw.githubusercontent.com — block/mystical_resource_crop_7.json — https://raw.githubusercontent.com/BlakeBr0/MysticalAgriculture/1.20/src/main/resources/assets/mysticalagriculture/models/block/mystical_resource_crop_7.json — passage
- [S43] raw.githubusercontent.com — handler/ColorHandler.java — https://raw.githubusercontent.com/BlakeBr0/MysticalAgriculture/1.20/src/main/java/com/blakebr0/mysticalagriculture/client/handler/ColorHandler.java — passage
- [S44] raw.githubusercontent.com — blockstates/aluminum_crop.json — https://raw.githubusercontent.com/BlakeBr0/MysticalAgriculture/1.20/src/generated/resources/assets/mysticalagriculture/blockstates/aluminum_crop.json — passage
- [S45] raw.githubusercontent.com — block/aluminum_crop.json — https://raw.githubusercontent.com/BlakeBr0/MysticalAgriculture/1.20/src/generated/resources/assets/mysticalagriculture/models/block/aluminum_crop.json — passage
- [S46] www.blakesmods.com — mysticalcustomization/adding-crops — https://www.blakesmods.com/docs/1.20/mysticalcustomization/adding-crops — passage
- [S47] www.slynyrd.com — 10/pixelblog-1-color-palettes — https://www.slynyrd.com/blog/2018/1/10/pixelblog-1-color-palettes — passage
- [S48] pixelparmesan.com — color-theory-for-pixel-artists-its-all-relative — https://pixelparmesan.com/color-theory-for-pixel-artists-its-all-relative/ — passage
- [S49] docs.neoforged.net — client/models — https://docs.neoforged.net/docs/1.20.4/resources/client/models/ — passage
- [S50] docs.minecraftforge.net — client/models — https://docs.minecraftforge.net/en/1.20.1/resources/client/models/ — passage
- [S51] www.blockbench.net — docs/bbmodel — https://www.blockbench.net/wiki/docs/bbmodel — passage
- [S52] www.blockbench.net — blockbench/formats — https://www.blockbench.net/wiki/blockbench/formats — passage
- [S53] github.com — HEAD/README.md — https://github.com/sosadly/blockbench-mcp/blob/HEAD/README.md — passage
- [S54] github.com — HEAD/README.md — https://github.com/jasonjgardner/blockbench-mcp-plugin/blob/HEAD/README.md — passage
- [S55] github.com — headless/README.md — https://github.com/jasonjgardner/blockbench-mcp-plugin/blob/HEAD/headless/README.md — passage
- [S56] github.com — wiki/Geo-Models-(Geckolib4) — https://github.com/bernie-g/geckolib/wiki/Geo-Models-%28Geckolib4%29 — passage
- [S57] github.com — wiki/Making-Your-Models-(Blockbench) — https://github.com/bernie-g/geckolib/wiki/Making-Your-Models-%28Blockbench%29 — passage
- [S58] github.com — HEAD/README.md — https://github.com/bernie-g/geckolib/blob/HEAD/README.md — passage
- [S59] github.com — HEAD/README.md — https://github.com/AzureDoom/AzureLib/blob/HEAD/README.md — passage
- [S60] cdn.jsdelivr.net — block-model-renderer/README.md — https://cdn.jsdelivr.net/npm/block-model-renderer/README.md — passage
- [S61] cdn.jsdelivr.net — minecraft-render@1.1.1/README.md — https://cdn.jsdelivr.net/npm/minecraft-render@1.1.1/README.md — passage
- [S62] github.com — master/README.md — https://github.com/hexdoc-dev/minecraft-render-py/blob/master/README.md — passage
- [S63] github.com — HEAD/README.md — https://github.com/CyclopsMC/IconExporter/blob/HEAD/README.md — passage
- [S64] modrinth.com — version/1.20.1-1.3.0 — https://modrinth.com/mod/icon-exporter/version/1.20.1-1.3.0 — passage
- [S65] github.com — misc/gametest.md — https://github.com/MinecraftForge/Documentation/blob/1.20.1/docs/misc/gametest.md — passage
- [S66] docs.fabricmc.net — develop/automatic-testing — https://docs.fabricmc.net/develop/automatic-testing — passage
- [S67] github.com — docs/launch.md — https://github.com/headlesshq/headlessmc/blob/HEAD/docs/launch.md — passage
- [S68] arxiv.org — html/2603.03964v1 — https://arxiv.org/html/2603.03964v1 — passage
- [S69] dev.to — alanwest/why-llms-fail-at-openscad-code-generation-and-how-to-fix-it-2bel — https://dev.to/alanwest/why-llms-fail-at-openscad-code-generation-and-how-to-fix-it-2bel — passage
- [S70] usage.imagemagick.org — montage — https://usage.imagemagick.org/montage/ — passage
- [S71] modrinth.com — legal/rules — https://modrinth.com/legal/rules — passage
- [S72] modrinth.com — article/ai-policy-and-disclosures — https://modrinth.com/news/article/ai-policy-and-disclosures/ — passage
- [S73] support.modrinth.com — articles/16551575 — https://support.modrinth.com/en/articles/16551575 — passage
- [S74] support.modrinth.com — articles/16567675 — https://support.modrinth.com/en/articles/16567675 — passage
- [S75] modrinth.com — article/licensing-guide — https://modrinth.com/news/article/licensing-guide/ — passage
- [S76] support.curseforge.com — articles/9000197279 — https://support.curseforge.com/en/support/solutions/articles/9000197279 — passage
- [S77] curseforge-ideas.overwolf.com — ideas/CF-I-7917 — https://curseforge-ideas.overwolf.com/ideas/CF-I-7917 — passage
- [S78] medium.com — overwolf/curseforge-guidelines-update-mod-cloning-ban-and-copyright-compliance-9e9398d37c37 — https://medium.com/overwolf/curseforge-guidelines-update-mod-cloning-ban-and-copyright-compliance-9e9398d37c37 — passage
- [S79] www.minecraft.net — en-us/eula — https://www.minecraft.net/en-us/eula — passage
- [S80] www.minecraft.net — en-us/usage-guidelines — https://www.minecraft.net/en-us/usage-guidelines — passage
- [S81] www.copyright.gov — 2025/1060.html — https://www.copyright.gov/newsnet/2025/1060.html — passage
- [S82] openai.com — policies/row-terms-of-use — https://openai.com/policies/row-terms-of-use/ — passage
- [S83] faithfulpack.net — license — https://faithfulpack.net/license — passage
- [S84] www.planetminecraft.com — rules — https://www.planetminecraft.com/rules/ — passage
- [S85] www.techradar.com — gaming-industry/minecraft-developer-rejects-generative-ai-its-important-that-it-makes-us-feel-happy-to-create-as-humans — https://www.techradar.com/gaming/gaming-industry/minecraft-developer-rejects-generative-ai-its-important-that-it-makes-us-feel-happy-to-create-as-humans — passage
- [S86] partner.steamgames.com — gettingstarted/contentsurvey — https://partner.steamgames.com/doc/gettingstarted/contentsurvey — passage
- [S87] www.pcgamer.com — gaming-industry/nexus-mods-finally-tightens-the-screws-on-ai-though-its-still-playing-softball-with-the-tide-of-slop — https://www.pcgamer.com/gaming-industry/nexus-mods-finally-tightens-the-screws-on-ai-though-its-still-playing-softball-with-the-tide-of-slop — passage
- [S88] www.engadget.com — gaming/crimson-desert-developer-apologizes-and-promises-to-replace-ai-generated-art-183716439.html — https://www.engadget.com/gaming/crimson-desert-developer-apologizes-and-promises-to-replace-ai-generated-art-183716439.html — passage
- [S89] modrinth.com — project/wN4viUwd — https://modrinth.com/project/wN4viUwd — passage
- [S90] www.destructoid.com — https://www.destructoid.com/?p=981505 — snippet only

## Audit

| Claim | Verdict | Note |
|---|---|---|
| C101 | SUPPORTED | quote found on the fetched page |
| C102 | SUPPORTED | quote found on the fetched page |
| C103 | PARTIAL | rewritten: 200x200 is the output size, and the page does not call this the route to style consistency |
| C104 | SUPPORTED | quote found on the fetched page |
| C106 | SUPPORTED | quote found on the fetched page |
| C107 | SUPPORTED | quote found on the fetched page |
| C108 | SUPPORTED | quote found on the fetched page |
| C109 | SUPPORTED | quote found on the fetched page |
| C110 | SUPPORTED | quote found on the fetched page |
| C111 | SUPPORTED | quote found on the fetched page |
| C112 | SUPPORTED | quote found on the fetched page |
| C113 | SUPPORTED | quote found on the fetched page |
| C114 | SUPPORTED | quote found on the fetched page |
| C115 | SUPPORTED | quote found on the fetched page |
| C116 | SUPPORTED | quote found on the fetched page |
| C117 | SUPPORTED | quote found on the fetched page |
| C118 | SUPPORTED | quote found on the fetched page |
| C120 | SUPPORTED | quote found on the fetched page |
| C121 | SUPPORTED | quote found on the fetched page |
| C123 | SUPPORTED | quote found on the fetched page |
| C124 | SUPPORTED | quote found on the fetched page |
| C125 | SUPPORTED | quote found on the fetched page |
| C126 | SUPPORTED | quote found on the fetched page |
| C127 | SUPPORTED | quote found on the fetched page |
| C128 | SUPPORTED | quote found on the fetched page |
| C129 | SUPPORTED | quote found on the fetched page |
| C130 | SUPPORTED | quote found on the fetched page |
| C131 | SUPPORTED | quote found on the fetched page |
| C132 | SUPPORTED | quote found on the fetched page |
| C133 | SUPPORTED | quote found on the fetched page |
| C134 | SUPPORTED | quote found on the fetched page |
| C135 | SUPPORTED | quote found on the fetched page |
| C136 | SUPPORTED | quote found on the fetched page |
| C137 | SUPPORTED | quote found on the fetched page |
| C138 | SUPPORTED | quote found on the fetched page |
| C141 | SUPPORTED | quote found on the fetched page |
| C142 | SUPPORTED | quote found on the fetched page |
| C143 | SUPPORTED | quote found on the fetched page |
| C144 | SUPPORTED | quote found on the fetched page |
| C145 | SUPPORTED | quote found on the fetched page |
| C146 | UNSUPPORTED | deleted: S13 has no such passage; the quote is a search tool's own summary |
| C147 | SUPPORTED | quote found on the fetched page |
| C149 | SUPPORTED | quote found on the fetched page |
| C150 | SUPPORTED | quote found on the fetched page |
| C151 | PARTIAL | rewritten: the page lists distinct indices, pixels differing between frames, bounding-box position, ramp proportions; palette membership is not there |
| C153 | UNCHECKABLE | S17 returns 404 |
| C154 | UNCHECKABLE | S18 returns 404 |
| C155 | SUPPORTED | quote found on the fetched page |
| C156 | SUPPORTED | quote found on the fetched page |
| C157 | SUPPORTED | quote found on the fetched page |
| C158 | SUPPORTED | follows from the named claims |
| C159 | PARTIAL | rewritten: sizes below 32 px are documented for RD Pro and RD Lite, the Minecraft styles for other models |
| C160 | SUPPORTED | follows from the named claims |
| C161 | SUPPORTED | follows from the named claims |
| C162 | SUPPORTED | follows from the named claims |
| C201 | SUPPORTED | quote found on the fetched page |
| C202 | PARTIAL | rewritten: the quote is the multiply formula only; the darkening and ordering conclusions are not on the page |
| C203 | SUPPORTED | quote found on the fetched page |
| C205 | SUPPORTED | quote found on the fetched page |
| C207 | PARTIAL | rewritten: tintindex 0 is also on the side overlay faces, not on the top face only |
| C208 | SUPPORTED | quote found on the fetched page |
| C209 | PARTIAL | rewritten: the file shows a two-layer template only; nothing about per-mob models or tint colours; read by curl (script could not fetch) |
| C212 | PARTIAL | rewritten: the file shows two layers for the helmet; dyeable/untinted and other armour are not in it |
| C215 | SUPPORTED | quote found on the fetched page |
| C216 | SUPPORTED | quote found on the fetched page |
| C217 | SUPPORTED | quote found on the fetched page |
| C218 | SUPPORTED | quote found on the fetched page |
| C219 | SUPPORTED | quote found on the fetched page |
| C221 | SUPPORTED | quote found on the fetched page |
| C222 | PARTIAL | rewritten: dropped the conclusion that the source is usable for ordinary sprites |
| C223 | SUPPORTED | matches the recorded output; not re-run |
| C224 | PARTIAL | rewritten: narrowed to the seagrass file; the unit of frametime is not in it; read by curl (script could not fetch) |
| C226 | SUPPORTED | quote found on the fetched page |
| C227 | SUPPORTED | read by curl (script could not fetch); quote present |
| C228 | SUPPORTED | quote found on the fetched page |
| C229 | SUPPORTED | quote found on the fetched page |
| C230 | SUPPORTED | matches the recorded output; the tree listing re-run returned nothing (API) |
| C232 | SUPPORTED | quote found on the fetched page |
| C234 | PARTIAL | rewritten: the tip is about complex models built beforehand, not about many variants or wheat |
| C235 | PARTIAL | rewritten: the loader list is not on the page |
| C236 | SUPPORTED | quote found on the fetched page |
| C237 | SUPPORTED | quote found on the fetched page |
| C239 | SUPPORTED | quote found on the fetched page |
| C241 | SUPPORTED | quote found on the fetched page |
| C243 | SUPPORTED | quote found on the fetched page |
| C245 | PARTIAL | rewritten: the page says overlay over the primary colour, not a tinted second layer |
| C246 | SUPPORTED | read by curl (script could not fetch); quote present |
| C247 | SUPPORTED | matches the recorded output; not re-run |
| C248 | PARTIAL | rewritten: shown for one ingot model and the docs; the colour handler was not read |
| C249 | SUPPORTED | quote found on the fetched page |
| C250 | SUPPORTED | quote found on the fetched page |
| C251 | PARTIAL | rewritten: only the aluminum crop blockstate was read |
| C252 | SUPPORTED | read by curl (script could not fetch); quote present |
| C253 | PARTIAL | rewritten: the page also has a textures block; not hex colours only |
| C254 | PARTIAL | rewritten: one template and one per-crop texture were measured, not all 4 and all 132 |
| C256 | SUPPORTED | quote found on the fetched page |
| C257 | SUPPORTED | quote found on the fetched page |
| C262 | SUPPORTED | follows from the named claims |
| C263 | SUPPORTED | quote found on the fetched page |
| C265 | SUPPORTED | follows from the named claims |
| C301 | SUPPORTED | quote found; the page is NeoForge 1.20.4 docs, not Forge 1.20.1 |
| C302 | SUPPORTED | quote found on the fetched page |
| C303 | SUPPORTED | quote found on the fetched page |
| C304 | SUPPORTED | quote found on the fetched page |
| C306 | SUPPORTED | quote found on the fetched page |
| C307 | SUPPORTED | quote found on the fetched page |
| C308 | SUPPORTED | quote found on the fetched page |
| C310 | SUPPORTED | quote found on the fetched page |
| C312 | SUPPORTED | quote found on the fetched page |
| C313 | SUPPORTED | quote found on the fetched page |
| C314 | SUPPORTED | quote found on the fetched page |
| C315 | SUPPORTED | quote found on the fetched page |
| C316 | SUPPORTED | quote found on the fetched page |
| C317 | SUPPORTED | quote found on the fetched page |
| C318 | SUPPORTED | quote found on the fetched page |
| C319 | SUPPORTED | quote found on the fetched page |
| C320 | SUPPORTED | quote found on the fetched page |
| C321 | SUPPORTED | quote found on the fetched page |
| C324 | UNCHECKABLE | prior, no source |
| C325 | SUPPORTED | quote found; source URL parentheses percent-encoded so the quote script can fetch it |
| C326 | SUPPORTED | quote found; source URL parentheses percent-encoded so the quote script can fetch it |
| C327 | SUPPORTED | quote found; source URL parentheses percent-encoded so the quote script can fetch it |
| C328 | SUPPORTED | quote found; source URL parentheses percent-encoded so the quote script can fetch it |
| C329 | SUPPORTED | quote found; source URL parentheses percent-encoded so the quote script can fetch it |
| C330 | SUPPORTED | quote found on the fetched page |
| C331 | SUPPORTED | re-run 2026-11-23: 71 versions, 4.8.5 forge and fabric |
| C332 | SUPPORTED | quote found on the fetched page |
| C333 | UNCHECKABLE | prior, no source |
| C334 | PARTIAL | rewritten: 'deterministically' unsupported; compatibility of the output with GeckoLib not established |
| C335 | PARTIAL | rewritten: the version number is not in the README |
| C337 | SUPPORTED | quote found on the fetched page |
| C339 | SUPPORTED | quote found on the fetched page |
| C340 | SUPPORTED | quote found on the fetched page |
| C341 | SUPPORTED | quote found on the fetched page |
| C342 | PARTIAL | rewritten and relabelled quoted: the page was readable and supports the IconExporter half; BlockExporter is not on it |
| C343 | SUPPORTED | quote found on the fetched page |
| C345 | SUPPORTED | quote found on the fetched page |
| C347 | PARTIAL | rewritten: the page does not say it cannot take screenshots, and the loader list is not in the passage |
| C348 | UNSUPPORTED | deleted: does not follow from C340 and C320; rests on unstated premises about runClient and Blockbench |
| C349 | PARTIAL | rewritten: limited to the pages read; absence of a mechanism is not shown |
| C350 | PARTIAL | rewritten: limited to the tools read; 'only' is not shown |
| C351 | SUPPORTED | quote found on the fetched page |
| C352 | SUPPORTED | quote found on the fetched page |
| C353 | SUPPORTED | quote found on the fetched page |
| C354 | SUPPORTED | quote found on the fetched page |
| C355 | SUPPORTED | quote found on the fetched page |
| C356 | SUPPORTED | quote found on the fetched page |
| C357 | SUPPORTED | quote found on the fetched page |
| C358 | SUPPORTED | quote found on the fetched page |
| C360 | SUPPORTED | quote found on the fetched page |
| C362 | UNCHECKABLE | prior, no source |
| C401 | SUPPORTED | quote found on the fetched page |
| C402 | SUPPORTED | quote found on the fetched page |
| C403 | SUPPORTED | quote found on the fetched page |
| C404 | SUPPORTED | quote found on the fetched page |
| C405 | SUPPORTED | quote found on the fetched page |
| C406 | SUPPORTED | quote found on the fetched page |
| C407 | SUPPORTED | quote found on the fetched page |
| C408 | SUPPORTED | quote found on the fetched page |
| C409 | SUPPORTED | quote found on the fetched page |
| C410 | SUPPORTED | quote found on the fetched page |
| C411 | SUPPORTED | quote found on the fetched page |
| C412 | SUPPORTED | quote found on the fetched page |
| C413 | SUPPORTED | quote found on the fetched page |
| C414 | SUPPORTED | quote found on the fetched page |
| C415 | SUPPORTED | quote found on the fetched page |
| C416 | SUPPORTED | quote found on the fetched page |
| C417 | SUPPORTED | quote found on the fetched page |
| C418 | SUPPORTED | quote found; the page has exactly one AI rule |
| C419 | SUPPORTED | follows from the named claims |
| C420 | SUPPORTED | quote found on the fetched page |
| C421 | SUPPORTED | quote found on the fetched page |
| C422 | PARTIAL | rewritten: a feature request does not show that no policy existed |
| C423 | SUPPORTED | read by curl (script could not fetch); quote present |
| C424 | SUPPORTED | quote found on the fetched page |
| C425 | SUPPORTED | quote found on the fetched page |
| C426 | SUPPORTED | quote found on the fetched page |
| C427 | SUPPORTED | quote found on the fetched page |
| C428 | PARTIAL | rewritten: the quote defines a mod; it does not grant distribution |
| C429 | SUPPORTED | follows from the named claims |
| C430 | SUPPORTED | quote found on the fetched page |
| C431 | SUPPORTED | quote found on the fetched page |
| C432 | SUPPORTED | quote found on the fetched page |
| C433 | PARTIAL | rewritten: enforceability of a licence is not in C430-C432 |
| C434 | SUPPORTED | quote found on the fetched page |
| C435 | SUPPORTED | quote found on the fetched page |
| C436 | SUPPORTED | quote found on the fetched page |
| C437 | SUPPORTED | quote found on the fetched page |
| C438 | PARTIAL | rewritten: the page says 'in any context', not 'with its users' |
| C439 | SUPPORTED | quote found on the fetched page |
| C440 | UNCHECKABLE | S84 is behind a bot check; not read |
| C441 | SUPPORTED | quote found on the fetched page |
| C442 | PARTIAL | rewritten: the passage is the disclosure definition; 'does not ban' is not in it |
| C443 | SUPPORTED | quote found; 'allowed' rests on the article's framing, not on the quoted sentence |
| C444 | SUPPORTED | quote found on the fetched page |
| C445 | SUPPORTED | quote found on the fetched page |
| C446 | SUPPORTED | quote found on the fetched page |
| C447 | UNCHECKABLE | S90 is behind a bot check; snippet not confirmed |
| C448 | PARTIAL | rewritten: the project description is an unsourced premise, now stated as a condition |

Label verdict: open — open sub-questions could change the recommendation: CurseForge Terms of Use were not read, how Modrinth applies "primarily AI" is unknown, and no tool in the Q3 pipeline was run.
Quote check asset-pipeline-2026-11-23.md: 171 quoted claim(s): 164 found, 0 NOT FOUND, 7 unfetchable, 0 no source

## Disposition

| Recommendation | State | Where |
|---|---|---|
| Решить вопрос площадки: проект, целиком сделанный агентами, по тексту правил Modrinth не проходит в публичный листинг; уточнить у поддержки Modrinth и прочитать Terms of Use CurseForge | deferred | unfiled: прогон запущен планирующей сессией с условием трогать только файл отчёта; решение за автором |
| Основа конвейера — программная генерация: шаблоны в ключевых серых и варианты линий и пород заменой палитры или тинтом | deferred | unfiled: архитектурное решение принимает планирующая сессия после сведения всех исследований |
| Для заметных ассетов — замена палитры, а не чистый тинт | deferred | unfiled: часть того же архитектурного решения |
| Стадии роста по ванильной схеме (одна текстура на стадию, родитель `crop`), JSON через datagen | deferred | unfiled: часть того же архитектурного решения |
| Генеративные сервисы — только как источник черновика формы, с раскрытием ИИ | deferred | unfiled: сначала нужен пилотный замер сервисов на 16×16 из Open questions |
| Модели животных — GeckoLib, исходник в `.bbmodel`, экспорт инструментом | deferred | unfiled: сначала нужен пилот с одним животным из Open questions |
| Проверка без человека: числовые тесты спрайтов, рендер моделей вне игры, контактные листы, свой клиентский скриншот | deferred | unfiled: ни один инструмент не запускался; сначала пилот на Windows 11 |
| Чужие и ванильные ассеты не брать за основу | deferred | unfiled: правило для будущего AGENTS.md проекта, файла правил в репозитории ещё нет |
