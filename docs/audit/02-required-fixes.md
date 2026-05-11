# Отчёт 2: Потенциальные правки по обозрённым системам

Дата: 2026-05-10

## A. `paint` подсистема

### 1) Однотипный код для меню настроек кистей
**Наблюдение:**
В `PaintMenuController` повторяются однотипные блоки:
- кнопки `size` / `% fill`
- шаблон `requestNumericInput(...){ ...coerceIn... }`
- парные функции `setShadeButton` + `setShadeMixButton` с одинаковым паттерном обновления.

**Рекомендуемая правка (без фанатизма):**
Выделить локальные переиспользуемые helper-функции уровня класса `PaintMenuController`, например:
- `bindSizeButton(...)`
- `bindFillPercentButton(...)`
- `bindShadeControls(...)`

Это сократит копипаст, снизит риск расхождения поведения и ускорит добавление новых инструментов.

---

## B. `apply` подсистема редактора

### 2) Повтор в apply-обработчиках (велосипедная ритуальная последовательность)
**Наблюдение:**
В `DamageApplyHandler` и `MiningSpeedApplyHandler` повторяется один и тот же пайплайн:
1. Проверка количества аргументов.
2. Парсинг значения.
3. Сборка `ItemAction` + `ItemContext`.
4. Вызов `validationService.validate(...)`.
5. Применение к `editableItem`.

**Рекомендуемая правка:**
Вынести общий шаблон в абстракцию (например, базовый helper/abstract class для single-arg apply handler), чтобы конкретные классы определяли только:
- парсер входа,
- создание action,
- способ применения изменения.

Это улучшит SRP и снизит шум в каждом новом handler.

### 3) Унификация пресетов
**Наблюдение:**
`presets` хранятся локально в каждом обработчике и могут разъехаться по стилю/качеству.

**Рекомендуемая правка:**
Собрать пресеты в отдельный каталог/провайдер (`ApplyPresetCatalog`) по доменам (tool/meta/effects), чтобы:
- централизованно поддерживать UX-подсказки,
- избежать дублирования строковых литералов.

---

## C. Подсистема декоративных флагов (`menus/banner/*`)

### 4) Повтор в галерейных рендерах (public/my/moderation)
**Наблюдение:**
В `BannerMenuRenderer` повторяется общий lifecycle меню:
- вычисление interactive slots,
- `buildMenu` vs `configureCurrentMenu`,
- `clearTopArea` + `fillFooter`,
- отрисовка стрелок пагинации и common-кнопок.

**Рекомендуемая правка:**
Вынести каркас в общий template-метод (компоновщик), куда передавать:
- title,
- интерактивные слоты,
- стратегию отрисовки контента.

Это снизит риск расхождения UX между экранами и облегчит дальнейшие правки.

### 5) Магические индексы слотов
**Наблюдение:**
Используются числовые слоты (`46,47,48,49,50,51,52` и др.) напрямую.

**Рекомендуемая правка:**
Создать именованный layout-объект (`BannerMenuLayout`) с константами и наборами слотов.
Это повысит читаемость и уменьшит шанс ошибиться при перестановке кнопок.

---

## D. Core/Admin подсистема

### 6) Централизация release-поведения для отключаемых систем
**Наблюдение:**
`SystemToggleService.releaseActiveUsage` использует `when` по `ManagedSystem`, что нормально сейчас, но будет расти вместе с количеством систем.

**Рекомендуемая правка:**
Рассмотреть registry/callback модель:
- каждая подсистема регистрирует `onDisable` callback,
- `SystemToggleService` лишь вызывает зарегистрированный обработчик.

Это уменьшит связность core-сервиса с конкретными подсистемами и улучшит расширяемость.

---

## E. Потенциальный «мусор/магические значения»

### 7) Магические диапазоны и числа
**Наблюдение:**
Для числового ввода применяются инлайн-ограничения (`coerceIn(1, 100)`, `coerceIn(0, 50).coerceAtLeast(1)` и т.п.).

**Рекомендуемая правка:**
Свести такие границы в именованные константы/спеки (`BrushConstraints`, `InputRanges`) для единообразия и прозрачности.

---

## F. Что намеренно НЕ предлагается сейчас
- Не дробить небольшие классы ради формального «разбиения».
- Не переводить всё в иерархии наследования без явной выгоды.
- Не трогать производительность/микрооптимизации, пока нет фактического bottleneck.

---

## Приоритет внедрения
1. `FunctionHooker`: модульная декомпозиция bootstrap (core/commands/menus/integrations).
2. `PaintMenuController`: helper-рефакторинг повторяющихся button-bind паттернов.
3. `BannerMenuRenderer`: template-каркас + `BannerMenuLayout`.
4. `PlotCommandService`: декомпозиция на interceptor/rewrite/tab-complete сервисы.
5. `apply/tool/*`: шаблон общего single-arg обработчика.
6. `FreezeSessionRegistry`: переход на UUID-ключи для runtime-кеша.
7. `MenuService`: выделение sub-composer слоёв без изменения публичного фасада.
8. Централизация пресетов и числовых ограничений (включая massclaim constraints).
9. Опционально: callback-registry для `SystemToggleService`.


## G. PlotSquared интеграция

### 8) Укрупнение ответственности в `PlotCommandService`
**Наблюдение:**
`PlotCommandService` совмещает несколько ролей: install/uninstall hooks, rewrite аргументов, custom subcommand handling, tab-complete merge logic.

**Рекомендуемая правка:**
Разделить на композицию специализированных компонентов:
- `PlotCommandInterceptor` (install/uninstall + delegation),
- `PlotArgsRewriteService` (переписывание/нормализация аргументов),
- `PlotTabCompletionService` (tab-complete policies).

Так получится лучшее соблюдение SRP и упростится тестирование отдельных сценариев.

### 9) Централизация ограничений `massclaim`
**Наблюдение:**
`MAX_MASSCLAIM_SIZE` и `MIN_MASSCLAIM_ACTIVATION_TPS` заданы константами в сервисе.

**Рекомендуемая правка:**
Вынести ограничения в конфиг/спеку домена (`PlotMassClaimConstraints`) с единым местом чтения из конфига.
Это снизит hardcode и позволит гибко адаптировать ограничения под онлайн-нагрузку сервера.

---

## H. Freeze подсистема

### 10) Ключи runtime-кеша по `Player` объекту
**Наблюдение:**
`FreezeSessionRegistry` хранит сессии в `ConcurrentHashMap<Player, FreezeSession>`. Для долгоживущих серверов безопаснее держать ключи по `UUID`, чтобы не зависеть от ссылочной идентичности объекта `Player`.

**Рекомендуемая правка:**
Переехать на `ConcurrentHashMap<UUID, FreezeSession>` + helper-доступ по `Player`, сохранив текущий публичный контракт менеджера.

---

## I. Edit flow подсистемы

### 11) Общий lifecycle draft-сервисов
**Наблюдение:**
`EnchantmentMenuFlowService` и `AttributeMenuFlowService` имеют схожий lifecycle (`begin`, `apply`, `reset`) и похожие шаги валидации/нормализации.

**Рекомендуемая правка:**
Не вводя лишнюю сложность, выделить небольшой reusable helper/template для common lifecycle-поведения (инициализация draft-значений, safe-apply паттерн, reset defaults).

Это уменьшит дублирование при появлении новых edit-flow сервисов.


---

## J. Bootstrap и command orchestration

### 12) `FunctionHooker` как перегруженный composition root
**Наблюдение:**
`FunctionHooker` аккумулирует слишком много ролей: хранение всех сервисов, порядок init/deinit, интеграционные glue-операции, nullable-accessors. Это уже приближается к «god object» на уровне инфраструктуры.

**Рекомендуемая правка:**
Постепенно декомпозировать инициализацию в модули:
- `CoreModule` (config/message/permission/scheduler),
- `CommandsModule` (command managers + registration),
- `MenusModule` (menu services/subsystems),
- `IntegrationsModule` (PlotSquared, внешние подсистемы).

`FunctionHooker` оставить тонким orchestration-слоем, который собирает модули и управляет их lifecycle.

### 13) Масштабируемость `CommandManager`
**Наблюдение:**
Список handler-инициализаций в `CommandManager` линейно растёт и требует ручной правки в нескольких местах при добавлении команды.

**Рекомендуемая правка:**
Ввести реестр команд (registry/provider), где каждая команда регистрируется декларативно (тип, обработчик, опционально cooldown policy).
Это уменьшит риск ошибок и улучшит читаемость при росте числа команд.

---

## K. Message/Scheduler/Menu инфраструктура

### 14) Безопасность повторяющихся message-задач
**Наблюдение:**
`MessageManager` хранит repeat-задачи локально и ведёт собственный virtual tick. При высокой нагрузке важно иметь явные лимиты/метрики по числу активных задач и частоте.

**Рекомендуемая правка:**
Добавить лёгкие guardrails:
- soft-limit на число repeat tasks на игрока/канал,
- диагностические счётчики в debug-логи (`active`, `paused`, `cancelled`),
- единый метод health snapshot для admin-диагностики.

### 15) Дробление `MenuService` по функциональным пакетам
**Наблюдение:**
`MenuService` совмещает composition большого числа apply handlers, управление персональными предметами, item edit flow и часть инфраструктуры UI.

**Рекомендуемая правка:**
Постепенно вынести в sub-composers:
- `ApplyHandlersComposer`,
- `ItemEditFlowComposer`,
- `PersonalItemsComposer`.

При этом публичный API `MenuService` можно оставить стабильным как фасад.


---

## L. Persistence и moderation

### 16) Декомпозиция `AdvancedCreativeDatabase` по доменным миграциям
**Наблюдение:**
`AdvancedCreativeDatabase` содержит большой монолитный блок `createTables`/`migrate*` логики по нескольким доменам (paint, banner, heads, personal items).

**Рекомендуемая правка:**
Выделить доменные миграционные модули/регистратор миграций:
- `PaintSchemaMigration`,
- `BannerSchemaMigration`,
- `HeadsSchemaMigration`,
- `EditItemsSchemaMigration`.

Это улучшит сопровождаемость и упростит добавление миграций без риска ломать соседние домены.

### 17) Оптимизация `UserBanService.toggle` по числу запросов
**Наблюдение:**
Сценарий toggle сначала делает `find`, затем `delete`/`save`. Для высокочастотного moderation-flow можно уменьшить количество round-trip операций.

**Рекомендуемая правка:**
Рассмотреть явные команды API уровня сервиса:
- `ban(user, reason)`
- `unban(uuid)`
- `toggle(...)` оставить как orchestration-обёртку

Так появится более явная семантика и контроль производительности в местах вызова.

### 18) Единый moderation facade для banner + paint
**Наблюдение:**
`BannerModerationService` уже опирается на общий `UserBanService`, что хороший шаг к переиспользованию.

**Рекомендуемая правка:**
Зафиксировать общий moderation contract (например, `ModerationUserBanFacade`) и использовать его в подсистемах banner/paint/других точках, чтобы избежать скрытого дублирования бизнес-правил по мере роста функционала.


---

## M. Account-link / performance / plot editor

### 19) Явная политика для fail-open в `AccountLinkRequirementService`
**Наблюдение:**
Сейчас при ошибках reflection/integration сервис фактически работает в режиме fail-open (`return true`), что удобно для доступности, но политику лучше зафиксировать явно.

**Рекомендуемая правка:**
Вынести политику в конфиг-переключатель (`failOpenOnLinkCheckError`) и добавить заметный operational-log при срабатывании fallback, чтобы команда сервера осознанно управляла балансом UX/контроля.

### 20) Тонкая настройка TPS-гейта
**Наблюдение:**
`ServerPerformanceService` опирается на один primary TPS snapshot. Для пиковых моментов полезна мягкая гистерезис-логика (вход/выход порога), чтобы избежать «дребезга» разрешений.

**Рекомендуемая правка:**
Добавить optional hysteresis-порог в доменных местах использования (например, activation >= X, unlock <= Y), сохраняя текущий простой API как базовый путь.

### 21) Декомпозиция `PlotFlagEditorService`
**Наблюдение:**
`PlotFlagEditorService` объединяет большой набор обязанностей: command alias parsing, session store, apply request lifecycle, menu render/control, mutation permissions.

**Рекомендуемая правка:**
Разбить на компоненты:
- `PlotFlagEditorSessionStore`,
- `PlotFlagApplyCoordinator`,
- `PlotFlagMenuController`,
- `PlotFlagMutationService`.

Это сократит связность и упростит безопасное расширение функционала plot editor в будущем.

### 22) Улучшение cache-стратегии в `PlotSquaredFlagGate`
**Наблюдение:**
При достижении лимита cache очищается полностью (`clear`), что может приводить к волнообразным повторным вычислениям при активном онлайне.

**Рекомендуемая правка:**
Рассмотреть bounded cache с вытеснением (LRU/size-bounded map) и метриками hit/miss для контроля эффективности на онлайн 70-100+.


---

## N. State machine / utils / entity runtime

### 23) Централизация матрицы конфликтов состояний
**Наблюдение:**
`PlayerStateType.conflicts()` содержит вручную поддерживаемую матрицу конфликтов. При росте режимов повышается риск несимметричных/пропущенных правил.

**Рекомендуемая правка:**
Вынести конфликты в централизованную декларативную структуру (`StateConflictMatrix`) с валидацией симметрии на старте (или debug-check), чтобы предотвратить регрессии при добавлении новых состояний.

### 24) Снижение дублирования stop/check операций в `Utils`
**Наблюдение:**
`Utils` содержит большой объём однотипных `stopAllX`/`checkX` методов и прямых обращений к разным менеджерам.

**Рекомендуемая правка:**
Постепенно перейти к registry-подходу для state handlers:
- `state -> isActive`,
- `state -> deactivate`,
- `state -> stopAll`.

Это уменьшит boilerplate и упростит поддержку новых режимов.

### 25) Viewer-selection policy в `EntityManager`
**Наблюдение:**
В `EntityManager` неоднократно повторяется фильтрация `Bukkit.getOnlinePlayers()` с `!isHiddenFromPlayer(...)` перед отправкой пакетов/команд.

**Рекомендуемая правка:**
Выделить единый provider/selectors слой (например, `VisibleViewerSelector`) и использовать его в всех entity/network операциях.
Это упростит изменение политики видимости и снизит риск несовпадения логики между методами.

### 26) Метрики для троттлинга логов
**Наблюдение:**
`ActionLogger` умеет throttled logging, но не экспортирует статистику (число подавленных событий/ключей), что усложняет диагностику при инцидентах.

**Рекомендуемая правка:**
Добавить лёгкий snapshot по throttle-state (keys count + optional suppressed counters) и вывести его в admin diagnostics команды.


---

## O. Grab / Jar / Hide / Disguise

### 27) Единая абстракция runtime-сессий контроля игроков
**Наблюдение:**
`GrabManager` и `JarManager` реализуют схожий жизненный цикл: start session, conflict handling, release/cleanup, восстановление состояния, scheduled tick actions.

**Рекомендуемая правка:**
Выделить общий session-runtime каркас (например, `PlayerControlSessionEngine`) с переиспользуемыми частями:
- lock/ownership checks,
- release pipeline,
- state restore hooks,
- shutdown-safe cleanup.

Это сократит дублирование и снизит риск расхождения поведения в похожих механиках контроля игрока.

### 28) Нормализация viewer-management между hide/disguise
**Наблюдение:**
И `HideManager`, и `DisguiseManager` управляют viewer-видимостью, но делают это через разные локальные механизмы и повторяющиеся обходы онлайн игроков.

**Рекомендуемая правка:**
Сформировать единый `VisibilityRelationService` для:
- проверки видимости,
- применения/пере-применения hide/disguise отношений,
- массового reconcile после respawn/world-change/reload.

### 29) Ограничение глубины цепочек и единый guard helper
**Наблюдение:**
В `HideManager` несколько циклов с ограничением `maxDepth` и ручной anti-loop логикой. Это корректно, но дублируется и потенциально расходится в future-правках.

**Рекомендуемая правка:**
Вынести traversal-guard helper (`ChainTraversalGuard`) с едиными правилами глубины/цикла/ошибок и переиспользовать в sit/hide-подсистемах.

### 30) Метрики на high-churn viewer operations
**Наблюдение:**
Сценарии hide/disguise/jar/grab активно модифицируют viewer relations и отправляют пакеты, что при онлайне 70-100 требует наблюдаемости по churn-паттернам.

**Рекомендуемая правка:**
Добавить lightweight метрики:
- active sessions count (grab/jar/disguise/hide),
- viewer add/remove rates,
- reconcile queue sizes/pending viewers.

Выводить снимок в admin-диагностику для оперативного анализа лагов/аномалий.


---

## P. Effects и numeric attribute managers

### 31) Единый runtime-реестр «источников эффектов»
**Наблюдение:**
`EffectsManager` поддерживает два канала владения эффектами (command/internal), что правильно по идее, но при росте механик может усложниться reconciliation логика.

**Рекомендуемая правка:**
Ввести единый owner-model реестр (`EffectOwnershipRegistry`), где каждый источник (command/grab/jar/...) регистрирует claim/release, а финальный effective-level рассчитывается централизованно.

### 32) Шаблон для scheduled-refresh lifecycle
**Наблюдение:**
Логика запуска/перезапуска/cancel задач обновления эффектов повторяет паттерн, который встречается и в других runtime подсистемах.

**Рекомендуемая правка:**
Выделить reusable helper для managed repeating tasks (перезапуск по ключу, safe-cancel, auto-cleanup при offline), чтобы уменьшить дублирование и ошибки управления задачами.

### 33) Централизация scaling-констант в `ResizeManager`
**Наблюдение:**
В `ResizeManager` инлайн зашиты пороги/формулы для interaction-range, step-height и movement-speed.

**Рекомендуемая правка:**
Вынести параметры в конфигурируемый `ResizeScalingProfile` (или статический spec-класс) с именованными полями и возможностью тонкой правки без правки бизнес-кода.

### 34) Консистентная политика normalize/reset для numeric managers
**Наблюдение:**
`HealthManager`, `StrengthManager`, `ResizeManager` используют близкие, но не полностью унифицированные normalize/reset правила (например, поведение для нулевого значения).

**Рекомендуемая правка:**
Зафиксировать общую policy-матрицу в базовом слое `NumericAttributeManager` (override только по необходимости), чтобы снизить риск разных UX-правил между командами.


---

## Q. Common command abstractions / parsing / validation

### 35) Policy-объекты для `NumericAttributeManager`
**Наблюдение:**
Базовый класс `NumericAttributeManager` уже хороший template, но normalize/reset поведение частично разъезжается по наследникам.

**Рекомендуемая правка:**
Ввести явные policy-объекты (`NumericValuePolicy`) и передавать их в менеджеры, чтобы минимизировать ad-hoc override и сделать UX-поведение прозрачным/тестируемым.

### 36) Кэш/индексация suggestions в `EditParsers`
**Наблюдение:**
`suggestions` методы для material/sound каждый раз проходят по registry-итераторам, что при активном tab-complete может давать лишнюю нагрузку.

**Рекомендуемая правка:**
Добавить lazy-кэш списков (с invalidate при reload, если требуется) и фильтровать уже по кэшированным коллекциям.

### 37) Декомпозиция `ValidationService` по action-доменам
**Наблюдение:**
`ValidationService.validate` содержит крупный `when` по множеству `ItemAction` сценариев; масштаб будет расти вместе с функционалом редактора.

**Рекомендуемая правка:**
Выделить доменные validators:
- `HeadActionValidator`,
- `PotionActionValidator`,
- `EquippableActionValidator`,
- `ContainerActionValidator`,
- и т.д.

Оставить `ValidationService` orchestration-слоем маршрутизации, чтобы сохранить единый вход и улучшить поддержку.

### 38) Валидационные константы и спецификации
**Наблюдение:**
Часть лимитов (уровни, диапазоны, длительности) остаётся инлайн-значениями в валидации.

**Рекомендуемая правка:**
Свести лимиты в `ValidationConstraints`/domain specs, переиспользовать в UI подсказках и apply handlers для полной консистентности между «что можно ввести» и «что реально валидируется».


---

## R. Apply registry / edit session / domain parsers

### 39) Декларативная регистрация apply-обработчиков
**Наблюдение:**
`ApplyHandlerRegistry` решает задачу централизации, но при добавлении новых `EditorApplyKind` всё ещё высок риск ручного расхождения между enum и фактом регистрации.

**Рекомендуемая правка:**
Ввести декларативную схему регистрации (provider/descriptor), где обработчик объявляет поддерживаемый `kind`, а реестр валидирует полноту покрытия на старте (debug/assert mode).

### 40) Унификация `ApplyExecutionResult` и сообщений для UI
**Наблюдение:**
Разные ветки apply могут возвращать близкие по смыслу ошибки в немного разном текстовом формате, что осложняет единый UX и локализацию.

**Рекомендуемая правка:**
Добавить слой кодов/типов ошибок (`ApplyErrorCode`) и map в человекочитаемые сообщения в одном месте.
Это упростит поддержку локализации и выровняет ответы между handler-ами.

### 41) Лёгкая декомпозиция `EditSessionService` по сценариям
**Наблюдение:**
В `EditSessionService` в одном месте концентрируются операции создания сессии, применения изменений и служебная синхронизация состояния предпросмотра.

**Рекомендуемая правка:**
Без избыточного дробления выделить 2 внутренних компонента:
- `EditSessionLifecycle` (open/close/reset),
- `EditApplyCoordinator` (apply + validation orchestration).

Публичный фасад можно оставить прежним.

### 42) Общие parser-helpers для head/potion ввода
**Наблюдение:**
`HeadInputParser` и `PotionInputParser` содержат однотипные шаги: trim/normalize, alias resolve, формирование типовых ошибок.

**Рекомендуемая правка:**
Вынести базовые операции в небольшой `ParserInputSupport` (или extension helpers), а доменные парсеры оставить ответственными только за специфические правила.

### 43) Единый формат ошибок парсинга для tab/apply потоков
**Наблюдение:**
Сейчас сообщения об ошибках парсинга и подсказках могут формироваться разными кусками кода, что создаёт риск несоответствия между tab-complete и финальным apply.

**Рекомендуемая правка:**
Зафиксировать `ParseIssue` модель (code + context), которую можно использовать и в suggestions, и в apply-response, чтобы пользователь видел консистентную обратную связь на всех шагах.


---

## S. Personal items / heads / permission guards

### 44) Унификация сценариев save/load в personal items
**Наблюдение:**
`PersonalItemsService` и repository-вызовы обычно проходят похожий путь: загрузка профиля, проверка лимита, проверка слота, сохранение/обновление.

**Рекомендуемая правка:**
Вынести reusable pipeline/helper для типовых операций (`withPersonalItemsProfile { ... }`), чтобы сократить копипаст и расхождения бизнес-правил.

### 45) Декларативные ограничения на слоты и лимиты
**Наблюдение:**
Лимиты personal items и правила по слотам часто задаются рядом с операционной логикой.

**Рекомендуемая правка:**
Свести ограничения в `PersonalItemsConstraints` (или domain spec), чтобы UI, validation и persistence использовали один источник правил.

### 46) Общий filter/pagination engine для каталогов
**Наблюдение:**
`HeadsMenuService`/`HeadsCatalogService` используют шаблонный цикл: normalize query -> filter -> sort -> paginate -> render.

**Рекомендуемая правка:**
Выделить лёгкий `CatalogQueryEngine` с настраиваемыми стратегиями фильтра/сортировки.
Это снизит дублирование и упростит добавление новых каталогов (heads/banner/другие коллекции).

### 47) Кэш предобработанных ключей поиска
**Наблюдение:**
В каталогах и поиске голов могут повторно вызываться lowercasing/trim/normalization для одинаковых записей.

**Рекомендуемая правка:**
Добавить precomputed search-key cache (или memoized поле в DTO), чтобы уменьшить стоимость массовой фильтрации при частых вводах в поиске.

### 48) Единый guard-фасад для menu ACL
**Наблюдение:**
`PermissionService` и `MenuAccessGuard` концептуально решают одну задачу, но в UI-ветках возможны локальные ad-hoc проверки.

**Рекомендуемая правка:**
Зафиксировать единый `MenuPermissionGuardFacade`:
- `require(permission)`
- `requireAny(...)`
- `denyWithMessage(...)`

чтобы все меню использовали единообразный контракт доступа и отказов.

### 49) Стандартизация deny-сообщений и audit-контекста
**Наблюдение:**
Сообщения о запрете доступа могут формироваться в нескольких местах и с разной детализацией.

**Рекомендуемая правка:**
Ввести централизованный deny-message builder + audit context (permission, source menu, action), чтобы улучшить и UX, и диагностику инцидентов прав доступа.


---

## T. Cooldown / task lifecycle / teleport safety

### 50) Единая policy-модель для cooldown
**Наблюдение:**
`CooldownService` и локальные rate-limit проверки в командах могут использовать близкую, но не полностью идентичную семантику ключей и единиц времени.

**Рекомендуемая правка:**
Ввести `CooldownPolicy` (scope, duration, message strategy) и централизованный executor проверки, чтобы команды описывали только декларацию политики без ручного дублирования.

### 51) Нормализация форматирования remaining-time
**Наблюдение:**
Сообщения «подождите N секунд» нередко формируются локально в разных ветках команд.

**Рекомендуемая правка:**
Свести это в общий formatter (`CooldownMessageFormatter`) с единым UX-стилем, plural rules и округлением времени.

### 52) Дедупликация cancel-пайплайна задач
**Наблюдение:**
При наличии `TaskLifecycleRegistry` и `ShutdownCoordinator` часть менеджеров может всё равно вручную держать похожие `cancelAll` блоки.

**Рекомендуемая правка:**
Ввести стандартный `ManagedTaskHandle` контракт (register/cancel/isActive), чтобы runtime-задачи управлялись единообразно и не расходились по cleanup-политике.

### 53) Shutdown-фазы и порядок остановки
**Наблюдение:**
В системах с активными сессиями (hide/disguise/effects/freeze) важен предсказуемый порядок деинициализации, иначе возможны остаточные состояния.

**Рекомендуемая правка:**
Зафиксировать фазовую модель shutdown (`pre-stop`, `stop-active-sessions`, `cancel-tasks`, `final-release`) и подключить подсистемы через явный lifecycle интерфейс.

### 54) Общий precheck для safe teleport/positioning
**Наблюдение:**
`TeleportSafetyService` и командные positioning-хелперы используют похожие предварительные проверки (online/world/loaded chunk/state conflicts).

**Рекомендуемая правка:**
Выделить reusable `PositioningPrecheck` слой, чтобы команды переиспользовали один и тот же набор правил и не расходились в edge-cases.

### 55) Переиспользуемая fallback-стратегия safe location
**Наблюдение:**
Алгоритм fallback-поиска безопасной точки может дублироваться между сценариями телепортации и release-позиционирования.

**Рекомендуемая правка:**
Сформировать `SafeLocationStrategy` (primary -> nearby scan -> last-safe -> deny), который можно использовать в разных подсистемах, сохраняя консистентный результат и причины отказа.


---

## U. Config reload / i18n / diagnostics

### 56) Двухфазный reload-конвейер конфигов
**Наблюдение:**
`ConfigService` и `RuntimeReloadService` обычно затрагивают много зависимых подсистем, и при последовательном «частичном» обновлении возможны кратковременные рассинхронизации.

**Рекомендуемая правка:**
Ввести двухфазный pipeline:
1. `prepare` (валидация + построение нового snapshot),
2. `commit` (атомарная подмена ссылок/настроек).

При ошибке на phase-1 runtime остаётся на предыдущем стабильном snapshot.

### 57) Реестр reload-обработчиков вместо точечных вызовов
**Наблюдение:**
Перезагрузка зависимых подсистем может выполняться точечно/вручную, что усложняет расширение при росте числа компонентов.

**Рекомендуемая правка:**
Сформировать `ReloadParticipantRegistry` (priority + callback + timeout policy), чтобы порядок и охват reload были декларативными и проверяемыми.

### 58) Централизация message-key и параметров шаблонов
**Наблюдение:**
При локализации часть ключей/плейсхолдеров может использоваться ad-hoc в разных слоях (command/menu/service).

**Рекомендуемая правка:**
Добавить typed message descriptors (`MessageKey<TArgs>`) и единый resolver-контракт, чтобы исключить ошибки в именах плейсхолдеров и улучшить рефакторинг i18n.

### 59) Единая политика fallback/отсутствующих переводов
**Наблюдение:**
Fallback на default locale и поведение при отсутствующих ключах важно держать предсказуемым для UX и поддержки.

**Рекомендуемая правка:**
Зафиксировать `LocalizationFallbackPolicy` (default locale, mark-missing, audit logging rate-limit) и применять её в одном месте рендера сообщений.

### 60) Декомпозиция диагностики: сбор vs представление
**Наблюдение:**
`DiagnosticsCommand` часто вынужден агрегировать разнотипные данные и одновременно форматировать UI-вывод.

**Рекомендуемая правка:**
Разделить на:
- `HealthSnapshotCollectors` (данные),
- `DiagnosticsPresenter` (формат вывода),

чтобы упрощать расширение отчётов без дублирования форматирования.

### 61) Базовый contract для runtime-метрик high-churn систем
**Наблюдение:**
Для систем с ежедневными рестартами и онлайном 70–100 критично иметь единый формат метрик (sessions/tasks/cache hit/miss/cooldown pressure).

**Рекомендуемая правка:**
Ввести `RuntimeMetricSnapshot` контракт и обязательный минимум полей для всех подсистем, подключаемых к диагностике.
Это повысит сопоставимость и ускорит расследование деградаций.


---

## V. Macros / command routing / undo-redo

### 62) Унификация domain-модели пресетов и макросов
**Наблюдение:**
`MacroService`/`MacroRepository` и preset-потоки в редакторе решают близкие задачи (именованные пользовательские шаблоны действий), но могут эволюционировать независимо.

**Рекомендуемая правка:**
Выделить общий контракт (`NamedActionPreset`) и shared validation policy (name length, symbols, uniqueness), чтобы не дублировать бизнес-правила в двух подсистемах.

### 63) Единый pipeline валидации create/apply/delete для макросов
**Наблюдение:**
Операции над макросами обычно повторяют один и тот же набор шагов: lookup, access-check, constraints-check, mutate, persist, notify.

**Рекомендуемая правка:**
Собрать reusable orchestration helper (`MacroOperationPipeline`) с явными extension-точками, сохранив thin facade в `MacroService`.

### 64) Декларативные alias-политики и детектор конфликтов
**Наблюдение:**
`CommandAliasRegistry` при росте числа команд нуждается в автоматической проверке коллизий/теней (alias shadowing).

**Рекомендуемая правка:**
Добавить startup-валидацию alias map + отчёт о конфликтах (`AliasConflictReport`) и policy разрешения (strict fail / warning mode).

### 65) Согласование route-resolution и tab-complete
**Наблюдение:**
`SubcommandRouter` и tab-complete нередко поддерживаются раздельно, что может приводить к рассинхрону пользовательского UX.

**Рекомендуемая правка:**
Использовать единый source of truth (`SubcommandDescriptor`) для route match и suggestions, чтобы поведение автодополнения строго соответствовало фактическому роутингу.

### 66) Политика размера/TTL стека undo-redo
**Наблюдение:**
`ActionHistoryService` требует явного баланса между глубиной истории и memory pressure на онлайн 70–100.

**Рекомендуемая правка:**
Вынести ограничения в `HistoryRetentionPolicy` (max entries, ttl, per-user caps) и подключить их к диагностике runtime snapshot.

### 67) Безопасный откат в multi-step сценариях
**Наблюдение:**
`UndoRedoCoordinator` в сложных изменениях (несколько связанных мутаций) нуждается в гарантии целостности rollback.

**Рекомендуемая правка:**
Ввести `CompositeUndoTransaction` с фазами prepare/apply/rollback и явным journal событий, чтобы исключать частично применённые откаты.


---

## W. Caching / memory pressure / error handling

### 68) Декларативные cache-профили по доменам
**Наблюдение:**
`ReferenceCacheService` может обслуживать разные наборы данных с разной стоимостью построения и разной чувствительностью к stale-значениям.

**Рекомендуемая правка:**
Ввести `CacheProfile` (ttl, warmup, invalidation triggers, max size) для каждого домена, чтобы политика кэша была явной и настраиваемой.

### 69) Централизованный invalidation bus
**Наблюдение:**
При росте числа кэшей ручные вызовы invalidation могут стать источником пропусков и рассинхронизаций.

**Рекомендуемая правка:**
Добавить `CacheInvalidationBus` (event -> subscribers), связав его с reload/config-change событиями для предсказуемого обновления всех зависимых кэшей.

### 70) Метрики качества кэша для диагностики
**Наблюдение:**
Без метрик hit/miss/eviction сложно оценивать реальную пользу и побочные эффекты кэширования под живой нагрузкой.

**Рекомендуемая правка:**
Экспортировать `CacheHealthSnapshot` (hit rate, miss burst, stale reloads, rebuild time p95) в diagnostics-команду.

### 71) Политика использования object pools
**Наблюдение:**
`ObjectPoolService` полезен только для горячих short-lived объектов; без явных критериев есть риск premature optimization и усложнения кода.

**Рекомендуемая правка:**
Зафиксировать `PoolingEligibilityPolicy` (allocation rate threshold, object size, contention risk) и применять pooling только к сценариям, проходящим порог.

### 72) Связка allocation tracker с runtime snapshot
**Наблюдение:**
`AllocationTracker` даёт ценность, когда его данные сопоставимы с активными подсистемами (sessions/tasks/effects/hide/...)

**Рекомендуемая правка:**
Добавить correlation fields (subsystem, action, player scope) и вывод в `RuntimeMetricSnapshot`, чтобы быстрее находить источники memory spikes.

### 73) Единый error taxonomy для command/menu слоёв
**Наблюдение:**
`GlobalExceptionHandler` и локальные catch-блоки могут возвращать неодинаковые сообщения/коды ошибок для схожих сценариев.

**Рекомендуемая правка:**
Ввести `AppErrorCode` taxonomy + `UserFacingErrorMapper` как единую точку рендера пользовательских ошибок и policy для логирования stacktrace.

### 74) Guard-обёртка вместо дублирования try/catch в handlers
**Наблюдение:**
В command/menu handlers повторяется шаблон: try -> log -> send fail message -> cleanup.

**Рекомендуемая правка:**
Сделать reusable `SafeHandlerExecutor` (withContext/withUserFeedback/withCleanup), чтобы снизить boilerplate и выровнять fail-safety поведение.


---

## X. Event bridge / world policies / anti-spam

### 75) Единый event-dispatch фасад
**Наблюдение:**
`EventBridgeService` и локальные listeners могут дублировать однотипные precheck и guard-шаги до делегирования в доменные менеджеры.

**Рекомендуемая правка:**
Ввести `EventDispatchFacade` с общим пайплайном: precheck -> context build -> guard -> delegate -> audit.
Это снизит копипаст и уменьшит риск расхождения поведения между обработчиками событий.

### 76) Контракт идемпотентности для event side-effects
**Наблюдение:**
При пересечении нескольких слушателей на одно событие возможны повторные side-effects (двойной stop/release/notify).

**Рекомендуемая правка:**
Зафиксировать `EventEffectIdempotencyPolicy` (effect key + dedupe window) и использовать её в критичных event-сценариях.

### 77) Централизация world/region policy checks
**Наблюдение:**
`WorldRestrictionService` и `RegionPolicyResolver` логически едины, но проверки могут вызываться ad-hoc в разных командах.

**Рекомендуемая правка:**
Создать `AccessPolicyEvaluator` (player + action + location -> decision + reason), чтобы унифицировать проверки и сообщения отказа.

### 78) Кэш решения policy с коротким TTL
**Наблюдение:**
В частых действиях повторные region-checks могут создавать лишнюю нагрузку, особенно при активном интерактиве.

**Рекомендуемая правка:**
Добавить short-lived decision cache (`PolicyDecisionCache`) с TTL и invalidation на world/region change события.

### 79) Общая модель anti-spam сигналов
**Наблюдение:**
`InteractionBurstGuard`, `SpamMitigationService`, cooldown и throttling механики могут формировать несогласованные сигналы блокировок.

**Рекомендуемая правка:**
Ввести `RateLimitSignal` модель (source, severity, retryAfter, context), чтобы все системы ограничений работали согласованно.

### 80) Интеграция anti-spam с диагностикой и админ-инструментами
**Наблюдение:**
Без сводных метрик сложно понять, где реальные злоупотребления, а где слишком строгие лимиты.

**Рекомендуемая правка:**
Экспортировать `SpamControlSnapshot` (top triggers, blocked actions, false-positive hints) в diagnostics/admin-команды для оперативной калибровки лимитов.


---

## Y. State transactions / async persistence / config compatibility

### 81) Унифицированный контракт state-транзакций
**Наблюдение:**
`PlayerStateTransactionService` и manager-level transition логика могут повторять однотипные шаги prepare/apply/rollback/notify.

**Рекомендуемая правка:**
Ввести `StateTransaction<TContext>` контракт с явными фазами и единым обработчиком ошибок, чтобы исключить частично завершённые state-change операции.

### 82) Общий rollback-реестр для stateful операций
**Наблюдение:**
Release/undo/state-transition подсистемы имеют похожие rollback-потребности, но реализуют их локально.

**Рекомендуемая правка:**
Добавить `RollbackActionRegistry` с приоритетами и идемпотентными action-ключами для безопасного отката в межсервисных сценариях.

### 83) Стандартизация async->main thread handoff
**Наблюдение:**
`AsyncRepositoryExecutor` и локальные callback-пути могут по-разному обрабатывать отмену, таймауты и проверку online-состояния перед применением результата.

**Рекомендуемая правка:**
Сформировать `AsyncHandoffPolicy` (timeout, cancelOnOffline, staleResultGuard) и общий executor, чтобы одинаково обрабатывать все repository use-cases.

### 84) Контекстные correlation-id для async persistence
**Наблюдение:**
Диагностика race-condition сценариев усложняется без связки async задачи с игроком/операцией/источником команды.

**Рекомендуемая правка:**
Добавить `PersistenceCorrelationContext` (requestId, playerId, action, startedAt) в логи и diagnostics snapshot.

### 85) Декларативная карта миграции config-ключей
**Наблюдение:**
`LegacyKeyMapper` в точечных if/when ветках со временем становится трудно поддерживать и проверять на полноту.

**Рекомендуемая правка:**
Перевести mapping в декларативный `ConfigMigrationSpec` (from -> to, transform, deprecation phase), выполняемый единым `ConfigMigrationService`.

### 86) Политика deprecation lifecycle для legacy-ключей
**Наблюдение:**
Без формальной политики сложно управлять тем, когда legacy-ключ только предупреждает, а когда блокирует запуск/режим.

**Рекомендуемая правка:**
Зафиксировать `ConfigDeprecationPolicy` (warn -> soft-fail -> hard-fail по версиям) и выводить понятные upgrade-hints в админ-логи.


---

## Z. Public API / quality gates / ownership docs

### 87) Политика стабильности публичного API
**Наблюдение:**
`AdvancedCreativeApi` может эволюционировать вместе с внутренними рефакторами, что создаёт риск непредсказуемых breaking changes для внешних интеграций.

**Рекомендуемая правка:**
Ввести `ApiStabilityPolicy` (stable/experimental/internal), версионирование контрактов и changelog по API-уровню.

### 88) Антикоррупционный слой между API и внутренними моделями
**Наблюдение:**
Утечка внутренних DTO/enum через публичный API повышает связанность и усложняет внутренние изменения.

**Рекомендуемая правка:**
Сформировать `ApiMappingLayer` (public contracts <-> internal models), чтобы внутренние рефакторинги не ломали внешних потребителей.

### 89) Единый реестр compatibility-заметок
**Наблюдение:**
Изменения поведения в разных подсистемах сложно отслеживать без централизованных compatibility-notes.

**Рекомендуемая правка:**
Добавить `compatibility-notes.md` с рубрикацией по версиям и доменам (commands/menus/api/runtime), включая migration hints.

### 90) Нормализация quality-gates чеклистов
**Наблюдение:**
`testing-contract-checklist` и `release-readiness-checklist` могут содержать пересекающиеся пункты и разный стиль acceptance-критериев.

**Рекомендуемая правка:**
Ввести общий шаблон `QualityGateItem` (scope, preconditions, expected, rollback signal, observability) и синхронизировать оба чеклиста.

### 91) Трассируемость: рекомендация -> чеклист -> релиз
**Наблюдение:**
Рекомендации аудита не всегда явно связаны с release-check пунктами, из-за чего часть задач теряется.

**Рекомендуемая правка:**
Добавить матрицу трассируемости (`audit item` -> `quality gate` -> `release verification`) для приоритезации внедрения.

### 92) Формализация owner-зон и эскалации ответственности
**Наблюдение:**
Документация owner-зон полезна, но без формальной escalation-схемы межсервисные инциденты могут «зависать» между командами/модулями.

**Рекомендуемая правка:**
Ввести `OwnershipEscalationPolicy` (primary owner, fallback owner, SLA реакции, канал эскалации) для core сценариев с высоким влиянием.


---

## AA. UI reuse / feature flags / admin audit trail

### 93) Каталог переиспользуемых UI-компонентов
**Наблюдение:**
`UiComponentFactory` и меню-рендеры могут по-разному оформлять одинаковые действия (back/confirm/cancel/page/filter), что создаёт UX-расхождения.

**Рекомендуемая правка:**
Сформировать `UiPatternCatalog` (component spec + semantics + accessibility hints) и использовать его как единый источник для всех меню.

### 94) Декларативные slot-layout шаблоны
**Наблюдение:**
`SlotLayoutCatalog` полезен, но часть меню может всё ещё иметь локальные «магические» расстановки.

**Рекомендуемая правка:**
Расширить layout-каталог шаблонами (`paged-grid`, `confirm-dialog`, `moderation-panel`) и валидатором коллизий слотов при рендере.

### 95) Контракт UI-консистентности для общих действий
**Наблюдение:**
Одинаковые действия в разных меню иногда различаются по тексту/иконке/цвету, что повышает когнитивную нагрузку.

**Рекомендуемая правка:**
Ввести `CommonActionUiContract` (label/icon/color/tooltip rules) и применять его ко всем кнопкам общего назначения.

### 96) Многоуровневые feature-flags с fallback policy
**Наблюдение:**
`FeatureFlagService` и rollout логика нуждаются в едином порядке разрешения (global -> role -> world -> user override).

**Рекомендуемая правка:**
Зафиксировать `FeatureResolutionPolicy` с явным precedence, default behavior и fail-safe режимом при ошибках конфигурации.

### 97) Наблюдаемость флагов: кто/когда/почему переключил
**Наблюдение:**
Без trace данных по feature-toggle сложно анализировать регрессии после runtime-переключений.

**Рекомендуемая правка:**
Добавить `FeatureToggleAuditEvent` (actor, scope, old/new value, reason, timestamp) и вывод в diagnostics/admin audit trail.

### 98) Нормализация формата admin audit trail
**Наблюдение:**
`AdminAuditTrailService` полезен, но без стандартизованного формата событий сложнее автоматизировать экспорт и корреляцию инцидентов.

**Рекомендуемая правка:**
Ввести `AdminAuditEvent` schema (action, actor, target, context, outcome, correlationId) и обязательные поля для high-impact операций.

### 99) Политика retention/экспорта и контроль доступа к аудит-данным
**Наблюдение:**
`AuditExportService` требует формальной политики хранения, маскирования чувствительных полей и прав доступа к выгрузке.

**Рекомендуемая правка:**
Зафиксировать `AuditDataGovernancePolicy` (retention windows, redaction rules, export permissions, access logs) для соответствия operational и privacy требованиям.


---

## AB. External bridges / input safety / server modes

### 100) Единый адаптерный контракт для внешних провайдеров
**Наблюдение:**
`PermissionsBridge` и `RegionProviderBridge` решают схожую задачу нормализации внешних API, но могут развиваться разными паттернами.

**Рекомендуемая правка:**
Ввести общий `ExternalProviderAdapter<TRequest, TResponse>` контракт (capabilities, availability, normalizeError), чтобы унифицировать интеграции и fallback.

### 101) Capability-matrix для провайдеров
**Наблюдение:**
Разные внешние плагины поддерживают неодинаковые функции; без явной capability-модели растёт риск скрытых деградаций.

**Рекомендуемая правка:**
Добавить `ProviderCapabilityMatrix` и проверку на старте/reload с отчётом по недостающим возможностям и планом graceful fallback.

### 102) Централизованный реестр input-policy правил
**Наблюдение:**
`InputSanitizer` и локальные проверочные блоки в командах/меню могут дублировать валидацию символов, длины и запрещённых шаблонов.

**Рекомендуемая правка:**
Свести правила в `InputPolicyRegistry` (policy per field/use-case) и применять через единый `sanitize+validate` pipeline.

### 103) Стандартизованный формат ошибок input-валидации
**Наблюдение:**
Пользовательские ошибки ввода могут формироваться в разных форматах, что осложняет UX и локализацию.

**Рекомендуемая правка:**
Ввести `InputValidationIssue` (code, field, hint, severity) и единый mapper в user-facing сообщение.

### 104) Policy-матрица доступности команд по server-mode
**Наблюдение:**
`ServerModeService` и `CommandAvailabilityPolicy` нуждаются в явной матрице «режим -> разрешённые действия», иначе легко получить ad-hoc исключения.

**Рекомендуемая правка:**
Зафиксировать `ModeAvailabilityMatrix` с поддержкой role overrides и reason-codes для админ-диагностики.

### 105) Интеграция mode/cooldown/permission в единый decision engine
**Наблюдение:**
Доступность команды сейчас может определяться несколькими слоями независимо (mode, permission, world, cooldown), что усложняет объяснимость отказов.

**Рекомендуемая правка:**
Сформировать `CommandAccessDecisionEngine`, который возвращает агрегированное решение + причину + retry hints, переиспользуемые в командах и меню.


---

## AC. Background queue / notification dedup / release switching

### 106) Политика приоритезации и fairness для background queue
**Наблюдение:**
`BackgroundWorkQueue` при смешении user-facing и maintenance задач требует явной fairness-модели, иначе низкоприоритетные задачи могут голодать.

**Рекомендуемая правка:**
Ввести `QueueSchedulingPolicy` (priority class, quota, starvation prevention, burst limits) и публиковать queue health метрики.

### 107) Единая retry/dead-letter стратегия
**Наблюдение:**
Сбойные фоновые операции могут ретраиться ad-hoc, создавая непредсказуемые паттерны нагрузки.

**Рекомендуемая правка:**
Сформировать `RetryPolicyRegistry` + `DeadLetterStore` (max attempts, backoff, terminal reason), чтобы стандартизировать восстановление после ошибок.

### 108) Дедуп и агрегация пользовательских уведомлений
**Наблюдение:**
`PlayerNotificationService` и локальные send-message вызовы могут выдавать повторяющиеся однотипные сообщения в коротком интервале.

**Рекомендуемая правка:**
Использовать `NotificationDedupCache` с policy по ключам (player+reason+channel), TTL и агрегированным counter для «повторено N раз».

### 109) Контракт приоритетов каналов уведомлений
**Наблюдение:**
Chat/actionbar/title уведомления иногда конкурируют между собой, что ухудшает читаемость UX-сигналов.

**Рекомендуемая правка:**
Ввести `NotificationChannelPolicy` (severity -> preferred channel, fallback channel, suppression rules).

### 110) Preflight-гейт для release-switch
**Наблюдение:**
`ReleaseSwitchService` в сценариях runtime-переключений нуждается в едином preflight-наборе проверок, чтобы предотвратить частичную активацию конфигурации.

**Рекомендуемая правка:**
Добавить `ReleasePreflightChecklist` (dependencies ready, cache warm, policies loaded, rollback ready) как обязательный этап перед commit switch.

### 111) Атомарность переключения и rollback orchestration
**Наблюдение:**
Частичное применение release-настроек между подсистемами повышает риск несогласованных состояний.

**Рекомендуемая правка:**
Применять `ReleaseSwitchTransaction` (prepare -> commit -> verify -> rollback) с журналом шагов и correlation-id для диагностики.

---

## AD. Command cooldown + sit/session orchestration

### 112) Инкрементальная очистка кулдаунов вместо полного prune в hot-path
**Наблюдение:**
`CommandCooldownService` вызывает `pruneExpired()` перед записью кулдауна и в диагностических методах. При росте кэша полный проход по всем игрокам/командам может давать лишние пики latency.

**Рекомендуемая правка:**
Оставляя текущую структуру простой, перейти на более щадящую стратегию:
- локальная очистка только для активного игрока в `setCooldown/remainingMillis`,
- периодический глобальный sweep в отдельной низкочастотной задаче,
- лимитированная очистка «порциями» при крупных кэшах.

Это сохранит предсказуемость времени отклика на онлайн 70–100+ без преждевременной усложнённости.

### 113) Унификация модели runtime-реестров по UUID-ключам
**Наблюдение:**
`SitSessionRegistry` использует `MutableMap<Player, SitSession>`, что повторяет ранее замеченный паттерн с риском удержания ссылок на runtime-объекты игрока.

**Рекомендуемая правка:**
Стандартизировать registry-подход:
- хранение `UUID -> SitSession`,
- адаптерные методы `get(player)` / `set(player, ...)` для удобства вызовов,
- опциональный `PlayerLookup` только на границах интеграции.

Это повысит устойчивость для долгоживущего сервера и унифицирует архитектурные решения между подсистемами.

### 114) Декомпозиция `SitManager` на chain-check компоненты
**Наблюдение:**
В `SitManager.sitOnHead` присутствуют несколько похожих циклов обхода цепочки пассажиров/носителей с близкими проверками конфликтов/visibility/maxDepth.

**Рекомендуемая правка:**
Без ломки текущего поведения вынести повторяемые части в небольшие helper-компоненты:
- `HeadChainTraversal` (безопасный обход цепочки с cycle guard),
- `HeadInteractionGuards` (visibility/conflict checks),
- `HeadAttachCoordinator` (attach/reattach sequence).

`SitManager` оставить orchestration-фасадом, а детали проверок/обходов сделать переиспользуемыми и проще тестируемыми.

### 115) Расширяемость `SitheadConflictPolicy` через регистр правил
**Наблюдение:**
`SitheadConflictPolicy` уже выделяет правила в отдельные классы, но список rules зашит конструктором. При добавлении новых ограничений придётся править policy-класс.

**Рекомендуемая правка:**
Перевести на реестр правил (provider/list injection) с явным приоритетом и reason-code:
- `SitheadConflictRuleResult(allowed, code)` для объяснимых отказов,
- декларативная регистрация правил в bootstrap,
- единый лог/метрика по причинам блокировок.

Это улучшит OCP и снизит риск «точечных» ad-hoc проверок в самом `SitManager`.

### 116) Централизация констант взаимодействия sit/head
**Наблюдение:**
В `SitManager` собраны доменные константы (`INTERACT_DELAY_MS`, `MAX_INTERACT_DISTANCE`, `ARMORSTAND_REATTACH_DELAY_TICKS` и др.), которые со временем могут понадобиться другим pose/interaction системам.

**Рекомендуемая правка:**
Вынести значения в именованную спецификацию (`SitInteractionConstraints`) с возможностью чтения из конфигурации и безопасными дефолтами.

Это уберёт магические значения из orchestration-класса и упростит эксплуатационную настройку без форков кода.

---

## AE. Sithead command flow / memory diagnostics / runtime reset

### 117) Устранение дублирования chain/visibility проверок между `SitheadManager` и `SitManager`
**Наблюдение:**
`SitheadManager.prepareToSithead` и `SitManager.sitOnHead` содержат схожие циклы обхода цепочек и проверки скрытия/цикла/глубины. Это повышает риск расхождения правил при дальнейших изменениях.

**Рекомендуемая правка:**
Собрать общие проверки в переиспользуемый сервис (`SitheadValidationService`) с двумя сценариями:
- pre-command validation (с сообщениями для sender),
- runtime validation (безопасная проверка перед attach).

Это уменьшит дублирование и сохранит единый источник доменных правил.

### 118) Структурированный результат sithead-валидации вместо ad-hoc `return`
**Наблюдение:**
Текущий flow в `SitheadManager` использует большое число ранних `return` с локальным выбором `MessageKey`, что усложняет сопровождение и объяснимость отказов.

**Рекомендуемая правка:**
Ввести `SitheadValidationResult` (`allowed`, `reasonCode`, `messageKey`, `variables`) и единый mapper в ответ игроку.

Плюс: проще логировать причины отказов и собирать метрики по частоте блокировок.

### 119) Единый контракт memory-snapshot для подсистем
**Наблюдение:**
`MemoryUsageReporter` вручную агрегирует разные snapshot-модели и применяет эвристики прямо в одном классе.

**Рекомендуемая правка:**
Ввести интерфейс `MemoryFootprintProvider` (name, estimateBytes, units, optional details), чтобы каждая подсистема отдавала оценку через общий контракт.

`MemoryUsageReporter` оставить тонким сборщиком/renderer, а формулы держать ближе к владельцам данных.

### 120) Калибровка и переиспользование формул оценки памяти
**Наблюдение:**
Эвристические коэффициенты (`+96L`, `*24L`, `*320L` и т.п.) полезны, но сейчас распределены внутри отчётного класса и трудно верифицируются при изменении структур.

**Рекомендуемая правка:**
Сконцентрировать коэффициенты в `MemoryEstimationPolicy` с versioning/комментарием по происхождению чисел и периодической калибровкой на production-like дампах.

Это сделает отчёт стабильнее при эволюции подсистем.

### 121) Шаблон runtime-reset сценариев в `EventHandler`
**Наблюдение:**
В `onPlayerQuit`, `onPlayerDeath`, `onGameModeChange`, `onPlayerTeleport` повторяются похожие цепочки cleanup/reset вызовов.

**Рекомендуемая правка:**
Выделить `PlayerRuntimeResetCoordinator` с именованными сценариями (`disconnect`, `death`, `modeSwitch`, `teleport`) и декларативным порядком шагов.

Это снизит вероятность пропуска критичного cleanup шага при добавлении новой подсистемы.

### 122) Унификация cooldown-подходов в командах и banner-сервисах
**Наблюдение:**
`CommandCooldownService` и `BannerTakeCooldownService` реализуют родственные паттерны (TTL cache + prune), но эволюционируют независимо.

**Рекомендуемая правка:**
Сформировать общий reusable компонент (`ExpiringKeyValueIndex` / `CooldownIndex`) с параметризацией по ключам и политике очистки.

Это снизит «велосипед» и позволит единообразно внедрить оптимизации (инкрементальный prune, bounded sweep, diagnostics).

---

## AF. Command gateway / distributed cooldown patterns / moderation facade

### 123) Result-aware контракт для `ExecutableCommand`
**Наблюдение:**
Сейчас `ExecutableCommand` всегда ставит кулдаун после `handle(player, args)` при включённом `useCooldown`, независимо от фактического исхода доменной операции.

**Рекомендуемая правка:**
Перейти к контракту результата (`CommandExecutionResult`), где команда явно сообщает:
- `SUCCESS` (ставить кулдаун),
- `REJECTED_USER_INPUT` (опционально не ставить кулдаун),
- `NO_OP` (не ставить),
- `ERROR` (политика зависит от типа ошибки).

Это даст более предсказуемый UX и уменьшит случайные «штрафные» кулдауны.

### 124) Централизация локальных anti-spam/cooldown механизмов
**Наблюдение:**
`SlapManager`, `PlotFlagEditorService`, `BannerTakeCooldownService`, `CommandCooldownService` реализуют близкие паттерны ограничения частоты, но каждый по-своему.

**Рекомендуемая правка:**
Сформировать единый `RateLimit/Cooldown` toolkit:
- ключевые стратегии (per-player, per-target, composite key),
- TTL + release hooks,
- optional jitter/backoff,
- общая диагностика hit/deny.

Это сократит дублирование и облегчит эксплуатационную настройку.

### 125) Cleanup/eviction политика для локальных cooldown set/map
**Наблюдение:**
Часть локальных cooldown структур (`cooldownPlayers`, `mutationCooldowns`) чистится только через отложенные задачи, без унифицированного fallback-eviction при форс-мажорах.

**Рекомендуемая правка:**
Добавить страховочную очистку:
- periodic sweep,
- cleanup on player disconnect/session close,
- hard cap + oldest eviction для защит от аномального роста.

Это повысит устойчивость на долгих аптаймах между рестартами.

### 126) Единая политика формирования cooldown key
**Наблюдение:**
В `PlotFlagEditorService` ключ включает `System.identityHashCode(reference.area)`, что удобно локально, но усложняет наблюдаемость и сравнительный анализ deny-событий.

**Рекомендуемая правка:**
Стандартизовать `CooldownKeyFactory` (stable scope parts + readable serialization + hash suffix при необходимости), чтобы ключи были:
- достаточно стабильными,
- пригодными для логов/метрик,
- единообразными между подсистемами.

### 127) Явный moderation facade между доменными подсистемами
**Наблюдение:**
`BannerModerationService` корректно переиспользует `UserBanService`, но аналогичный контракт может понадобиться и другим доменам с похожими ban/unban flow.

**Рекомендуемая правка:**
Выделить `ModerationFacade` с общими use-cases (toggle/ban/unban/list/audit hook), оставив доменные сервисы тонкими адаптерами для своих сущностей.

Это улучшит SOLID-границы и уменьшит расхождения правил между подсистемами.

### 128) Стандартизованные moderation side-effects и аудит
**Наблюдение:**
В `togglePattern` выполняется важный side-effect (`removeBlockedPatternEverywhere`), но подобные пост-действия в других доменах могут реализовываться неравномерно.

**Рекомендуемая правка:**
Ввести `ModerationSideEffectPolicy` + audit event для каждого high-impact действия:
- что удалено/пересчитано,
- сколько затронуто записей,
- кто инициатор,
- корреляция с командой/сессией.

Это повысит объяснимость и упростит разбор инцидентов.

---

## AG. Emission geometry / cosmetic viewers / piss lifecycle

### 129) Формализация доменной модели для mouth-emission геометрии
**Наблюдение:**
`MouthEmissionCalculator` уже переиспользуется, но формулы и константы смещений остаются неявными и трудны для доменной валидации при появлении новых эмиттер-команд.

**Рекомендуемая правка:**
Ввести `EmissionGeometryProfile` (standing/laying variants, pitch mapping, scale rules) и держать вычислитель как pure-domain сервис без побочных эффектов.

Это упростит тестирование и безопасное расширение команд с похожей физикой.

### 130) Централизация viewer-selection политики для косметических эффектов
**Наблюдение:**
`SpitManager`, `SneezeManager`, а также части `PissManager` используют похожие обходы viewers + hide checks, но делают это локально.

**Рекомендуемая правка:**
Собрать `CosmeticViewerPolicy` / `VisibleAudienceSelector` с едиными режимами:
- who sees entity,
- who hears sound,
- who receives particles.

Это снизит расхождения UX между эффектами и уберёт повторяющийся код.

### 131) Контракт emission-action для spit/sneeze/piss
**Наблюдение:**
У команд-эмиттеров схожий pipeline: рассчитать origin/direction -> применить effect payload -> разослать viewers.

**Рекомендуемая правка:**
Ввести шаблон `EmissionAction` (calculate, buildPayload, dispatch, audit) с расширениями для конкретных эффектов.

Это позволит повторно использовать orchestration и централизовать общие guard/logging/metrics.

### 132) UUID-keyed runtime storage для `PissManager`
**Наблюдение:**
`pissingPlayers` хранится как `MutableMap<Player, Int>`, что несёт те же риски удержания runtime-ссылок, уже отмеченные в других registry-паттернах.

**Рекомендуемая правка:**
Перейти на `UUID`-ключи + helper-доступ по `Player`, унифицируя стратегию хранения runtime-состояния между подсистемами.

### 133) Bounded lifecycle для puddle score/display структур
**Наблюдение:**
`scorePoints` и `hiddenPuddleDisplays` могут расти в длительных сессиях при активном использовании эффекта, если не иметь явных лимитов/retention правил.

**Рекомендуемая правка:**
Добавить `PuddleRetentionPolicy`:
- max entries per chunk/world,
- TTL/decay для неактивных точек,
- periodic cleanup и admin diagnostics snapshot.

Это снизит риск незаметного накопления runtime-мусора.

### 134) Единая политика частоты эмиссии и backpressure
**Наблюдение:**
`PissManager` использует частый `runRepeating` + spawn display entities; при массовом использовании можно получить всплески по сети/тик-нагрузке.

**Рекомендуемая правка:**
Ввести `EmissionRatePolicy` (per-player rate, world budget, degradation mode under low TPS) и интегрировать с `ServerPerformanceService`.

Так эффект останется играбельным без деградации сервера на онлайне 70–100+.

---

## AH. Gravity/glide/crawl pose systems

### 135) Единая спецификация numeric-атрибутов и mapping-кривых
**Наблюдение:**
`GravityManager` корректно реализует NumericAttribute-паттерн, но формула `value -> modifier` и доменные ограничения живут локально в классе.

**Рекомендуемая правка:**
Ввести `NumericAttributeSpec` (min/max/default, parse policy, mapping curve) и использовать его в профильных менеджерах (`gravity`, `resize`, `strength`, `health`) для единообразия.

Это снизит риск расхождения поведения при эволюции команд.

### 136) UUID-keyed runtime storage для glide-состояний
**Наблюдение:**
`GlideManager` хранит `glidingPlayers`, `flightStates`, `glideBoostByPlayer` с ключами `Player`, что повторяет уже зафиксированный риск удержания runtime-ссылок.

**Рекомендуемая правка:**
Перейти на UUID-ключи + thin adapters по `Player` и добавить явный cleanup на disconnect/death/reset.

Это повысит устойчивость на ежедневных рестартах и обороте игроков.

### 137) Контракт управления задачей boost-loop
**Наблюдение:**
`GlideManager` вручную управляет `boostTaskId` и cleanup через проверки пустоты коллекций.

**Рекомендуемая правка:**
Вынести в reusable `ManagedRepeatingTask` helper (start-if-needed / stop-when-idle / safe-cancel), чтобы убрать дублирование task-lifecycle паттернов в других менеджерах.

### 138) Безопасный error-handling в crawl updater
**Наблюдение:**
`CrawlManager.startCrawlUpdater` ловит `Throwable` и делает `printStackTrace`, что в продакшне шумно и затрудняет структурированную диагностику.

**Рекомендуемая правка:**
Перейти на `actionLogger.error` с контекстом (player/session phase) + throttling и reason-code; при этом оставить fail-safe отписку сессии.

### 139) Вынос presenter-компонентов crawl в отдельные классы
**Наблюдение:**
`CrawlPosePresenter` и `ShulkerPresenter` логически отделены, но вложены в `CrawlManager`, что ограничивает переиспользование и локальное тестирование.

**Рекомендуемая правка:**
Вынести presenters в отдельные классы/файлы с явными зависимостями (logger/entity factory/pose api), оставив `CrawlManager` orchestration-слоем.

### 140) Унифицированный pose-mode contract для glide/crawl/sit/lay
**Наблюдение:**
У систем поз есть похожие этапы: canEnter -> activateState -> applyVisual -> periodicMaintain -> release/reset.

**Рекомендуемая правка:**
Определить общий `PoseModeController` контракт и реестр режимов, чтобы стандартизовать lifecycle и уменьшить ad-hoc cleanup в `Utils`/`EventHandler`.

Это снизит связность и упростит добавление новых pose-команд.

---

## AI. Help/ItemDB/Admin reporting flows

### 141) Декларативный help-registry вместо ручного permission-map
**Наблюдение:**
`AhelpPageService` содержит крупный hardcoded `helpEntriesByPermission`, который требует ручной синхронизации с `PluginCommandType`, permission-node и runtime-доступностью.

**Рекомендуемая правка:**
Перейти к `HelpEntryRegistry`, где entries собираются декларативно из command metadata (usage/description/permission/managedSystem/role-hints).

Это снизит риск рассинхронизации и упростит расширение команд.

### 142) Централизация пагинации/навигации для текстовых страниц
**Наблюдение:**
Логика `visiblePageNumbers` и render navigation токенов в `AhelpPageService` полезна как reusable паттерн для других admin/help списков.

**Рекомендуемая правка:**
Вынести в общий `PagedTextRenderer` (window strategy, ellipsis handling, click actions), чтобы переиспользовать в help/moderation/audit-экранах.

### 143) Контракт fallback для ItemDB numeric-id lookup
**Наблюдение:**
`ItemdbManager` напрямую отдаёт результат `getNumericId(itemName)` в сообщение; без явной fallback-политики UX может быть неочевидным при отсутствии записи.

**Рекомендуемая правка:**
Зафиксировать `ItemIdLookupResult` (`FOUND`, `NOT_FOUND`, `DEPRECATED_ALIAS`) и единый маппер user-facing ответов, включая рекомендации/подсказки.

### 144) Унификация admin report renderer (toggle/memory/audit)
**Наблюдение:**
`ToggleStatusReporter` и `MemoryUsageReporter` используют похожий MiniMessage-шаблон заголовков/списков/футеров, но реализованы отдельно.

**Рекомендуемая правка:**
Сформировать `AdminReportRenderer` с композиционными блоками (header, section list, summary, footer), чтобы стандартизовать стиль и снизить дублирование строковых шаблонов.

### 145) Result-to-message mapper для restore/maintenance операций
**Наблюдение:**
`HeadCatalogRestoreAdminService` содержит повторяемый `when(result)` с сопоставлением `RestoreResult -> MessageKey` для DAT/API путей.

**Рекомендуемая правка:**
Вынести в `RestoreResultMessageMapper` с поддержкой переменных (`amount`, `name`) и reuse в других admin maintenance командах.

Это улучшит SRP и сократит boilerplate в manager/service слоях.

### 146) Идемпотентный admin action contract + аудит результата
**Наблюдение:**
Операции восстановления/переключений важны для эксплуатации; полезно явно фиксировать outcome (`changed/no-op/error`) и публиковать audit-след.

**Рекомендуемая правка:**
Ввести `AdminActionOutcome` с обязательной записью в audit (actor, action, source, result, details), чтобы упростить пост-инцидентный разбор и контроль повторных запусков.

---

## AJ. Paint map internals / palette / artwork packaging

### 147) Template-обёртка для map-data операций
**Наблюдение:**
В `MapDataExtractor` повторяется паттерн получения `mapView -> world -> serverLevel -> mapData` с одинаковыми null-check ветками.

**Рекомендуемая правка:**
Ввести локальный helper (`withMapData(mapId) { data, view, world -> ... }`) для единообразной обработки ошибок/логов и сокращения boilerplate.

Это повысит читаемость и снизит риск расхождения поведения между map-операциями.

### 148) Централизация констант карты и индексной математики
**Наблюдение:**
Размеры карты (`128`, `128*128`) и индексные преобразования (`x/y/index`) используются во множестве методов низкоуровневого слоя.

**Рекомендуемая правка:**
Выделить `MapCanvasGeometry` (size, fullSize, indexOf, xOf, yOf, bounds check), чтобы убрать дублирование и повысить безопасность вычислений.

### 149) Observability для quality map-color matching
**Наблюдение:**
`MapColorMatcher` кеширует match по RGB, но не предоставляет метрик hit/miss/eviction и не фиксирует качество сопоставления для сложных палитр.

**Рекомендуемая правка:**
Добавить diagnostics snapshot (`cacheSize`, `hitRate`, optional worstDistance samples) и административную команду/вывод для контроля качества преобразования.

### 150) Политика источника палитры (code vs config)
**Наблюдение:**
`PaintPalette` полностью hardcoded, что усложняет мягкие изменения отображаемых названий/материалов/порядка без релиза кода.

**Рекомендуемая правка:**
Зафиксировать `PaletteSourcePolicy`: базовая встроенная палитра + optional override из конфигурации (с валидацией mapColor keys и fallback на дефолт).

### 151) Явный layout-contract для shulker packaging
**Наблюдение:**
`PaintArtworkService` использует `SHULKER_ROW_STARTS` и вычисление slot через локальные правила; это доменная логика, которая может понадобиться в других форматах экспорта/предпросмотра.

**Рекомендуемая правка:**
Вынести в `ArtworkLayoutStrategy` (grid -> slot mapping, max cells, overflow policy), чтобы отделить упаковку от бизнес-потока выдачи.

### 152) Единый artifact-delivery policy (inventory/drop/hide)
**Наблюдение:**
Логика `giveItem` сочетает inventory-transfer, drop fallback и hide-операции. Похожие сценарии встречаются и в других подсистемах выдачи предметов.

**Рекомендуемая правка:**
Ввести `ItemDeliveryPolicy` с режимами `mainHandPreferred`, `inventoryPreferred`, `dropFallback`, `hideDroppedFromHiders` и единым результатом (`delivered`, `droppedAmount`, `hiddenFor`).

Это позволит переиспользовать безопасную выдачу предметов по всему проекту.

---

## AK. Paint confirmation/persistence + edit target resolution

### 153) State-machine контракт для подтверждения правил paint
**Наблюдение:**
`PaintRuleConfirmationService` реализует multi-click подтверждение через `PendingRequest`, но состояние и переходы (idle/pending/confirmed/cancelled) зафиксированы неявно в нескольких map-структурах.

**Рекомендуемая правка:**
Выделить `PaintConfirmationStateMachine` с явными переходами и reason-codes, сохранив UI-слой отдельно.

Это повысит тестируемость и снизит риск неконсистентных переходов при будущих изменениях.

### 154) Унифицированный cooldown gate для menu-click сценариев
**Наблюдение:**
`clickCooldownTasks` в `PaintRuleConfirmationService` повторяет паттерн локального anti-spam gate, уже замеченный в других подсистемах.

**Рекомендуемая правка:**
Использовать общий reusable cooldown gate (из ранее предложенного toolkit), чтобы стандартизовать click-throttle, cleanup и диагностику.

### 155) Декомпозиция `PaintUserStateRepository` по доменным интерфейсам
**Наблюдение:**
Одна реализация обслуживает и rule confirmation, и moderation-ban storage, что увеличивает связность persistence-слоя.

**Рекомендуемая правка:**
Разделить на два специализированных репозитория (`PaintRulesRepository`, `PaintBanRepository`) поверх общего SQL helper/DAO.

Это упростит сопровождение схемы и независимую эволюцию доменных правил.

### 156) Единый SQL query policy для ban/rules домена
**Наблюдение:**
В `PaintUserStateRepository` часть SQL-паттернов (select existence, upsert, paging) потенциально дублируется с другими moderation-репозиториями.

**Рекомендуемая правка:**
Ввести общий `UserBanSqlTemplate`/`PagingSqlSupport` слой для повторяющихся запросов и нормализовать naming/ordering/limits.

### 157) Централизация item-classification в edit domain
**Наблюдение:**
`EditTargetResolver.snapshot` использует строковые эвристики (`endsWith("_HELMET")`, `endsWith("POTION")`, и т.п.). Аналогичные проверки вероятны в других местах validation/apply.

**Рекомендуемая правка:**
Выделить `ItemTypeClassifier` с кэшируемыми правилами и покрыть его unit-level тестами по категориям.

Это уберёт «магические» string checks из orchestration-классов.

### 158) Стандартизованный user-feedback для resolve ошибок
**Наблюдение:**
`EditTargetResolver` отправляет inline MiniMessage при пустой руке. В других командах похожие ошибки обычно идут через `MessageManager/MessageKey`.

**Рекомендуемая правка:**
Перевести на единый `MessageKey` + formatter pipeline, чтобы сохранить консистентный UX, локализацию и централизованный контроль текста.

---

## AL. UseCooldown edit/apply subsystem

### 159) Формализация доменной модели UseCooldown
**Наблюдение:**
`UseCooldownSupport` инкапсулирует DataComponent API, но доменные инварианты (seconds > 0, group optional, display/fallback) частично дублируются на уровне apply/UI.

**Рекомендуемая правка:**
Ввести `UseCooldownValue` (seconds, group) + factory/validator, чтобы держать инварианты в одном месте и упростить reuse.

### 160) Общий parser/normalizer для apply-group input
**Наблюдение:**
`UseCooldownGroupApplyHandler` содержит inline-нормализацию (`lowercase`, пробелы -> `_`, `rand`), которая может понадобиться и в других key-based полях.

**Рекомендуемая правка:**
Выделить `ApplyKeyInputNormalizer` с поддержкой стратегий (`strict`, `slug`, `random-token`) и единых error-codes.

### 161) Дедупликация шаблона apply handlers (seconds/group)
**Наблюдение:**
`UseCooldownSecondsApplyHandler` и `UseCooldownGroupApplyHandler` повторяют общий pipeline: parse -> action/context -> validate -> apply -> success.

**Рекомендуемая правка:**
Переиспользовать общий apply-template helper (из ранее отмеченных apply-рефакторов) для single-field mutations, оставив в классах только domain-specific шаги.

### 162) Централизованный random-token policy
**Наблюдение:**
`rand`-ветка генерирует 8-символьный токен с локальным charset. Для эксплуатационной консистентности лучше единая политика генерации.

**Рекомендуемая правка:**
Вынести в `RandomGroupKeyPolicy` (length, charset, collision strategy, optional prefix), чтобы переиспользовать в других random-id сценариях.

### 163) Page-builder abstraction для apply/reset UI кнопок
**Наблюдение:**
`UseCooldownEditPage` содержит объёмные inline-lore и повторяемую логику apply/reset кнопок, аналогичную другим editor pages.

**Рекомендуемая правка:**
Выделить `ApplyResetButtonSectionBuilder` (title, active state, lore template, apply callback, reset callback), чтобы ускорить создание новых страниц и уменьшить дублирование.

### 164) Единая message/lore локализация для edit-page подсказок
**Наблюдение:**
Тексты подсказок use-cooldown UI жёстко зашиты в классе страницы, что усложняет консистентные правки copywriting и локализации.

**Рекомендуемая правка:**
Свести пользовательские тексты в `EditUiTextCatalog` (или message-keys), сохранив шаблоны MiniMessage отдельно от логики кнопок.

---

## AM. Edit validation/execution domain model

### 165) Rule-registry декомпозиция для `ValidationService`
**Наблюдение:**
`ValidationService` концентрирует большой `when(action)` с правилами для многих доменов, что со временем повышает риск регрессий и сложность поддержки.

**Рекомендуемая правка:**
Ввести `ValidationRuleRegistry` (`ItemAction` -> validator) с модульными rule-провайдерами (consumable, tool, equippable, container, potion, head).

Это улучшит OCP и сделает добавление новых действий более безопасным.

### 166) Разделение execution и user-messaging в `ComponentsService`
**Наблюдение:**
`ComponentsService` возвращает `ItemResult` с inline MiniMessage-текстами, смешивая доменную мутацию и presentation/UX слой.

**Рекомендуемая правка:**
Перейти к `ExecutionOutcome` (code + payload), а преобразование в user-facing тексты вынести в отдельный message mapper.

Это упростит локализацию и переиспользование execution-логики вне чата/меню.

### 167) Builder facade для data-component мутаций
**Наблюдение:**
В `ComponentsService` повторяются операции получения builder, применения поля и записи компонента.

**Рекомендуемая правка:**
Создать `ComponentMutationFacade` (`mutateConsumable`, `mutateFood`, `mutateTool`, `mutateUseCooldown`) с единым error handling и telemetry hooks.

### 168) Модульная сегментация `ItemAction`
**Наблюдение:**
`ItemAction` включает очень широкий набор действий (meta/effects/head/equippable/tool/container/pot и др.), что усложняет навигацию и поддержку.

**Рекомендуемая правка:**
Сохранить публичный sealed-контракт, но логически сегментировать действия по подпакетам/подтипам (`MetaActions`, `EffectActions`, `EquipmentActions`, `ContainerActions`) с явными namespace-группами.

### 169) Версионирование action-contract для совместимости
**Наблюдение:**
При росте редактора изменение семантики существующих `ItemAction` может ломать сохранённые сценарии/макросы apply-потоков.

**Рекомендуемая правка:**
Ввести `ActionContractVersion` и migration-policy для критичных изменений в parse/validation/execution semantics.

### 170) Snapshot enrichment policy
**Наблюдение:**
`ItemSnapshot` содержит базовые поля, но часть валидаторов снова идёт напрямую в meta/item типы. Это создаёт смешение проверок snapshot-level и runtime-level.

**Рекомендуемая правка:**
Зафиксировать `SnapshotEnrichmentPolicy`: какие признаки вычисляются заранее (категории/capabilities), а какие читаются лениво из item meta.

Это повысит предсказуемость валидации и упростит тест-кейсы.

---

## AN. Container/Equippable/Remainder edit supports

### 171) Capability registry для container-edit
**Наблюдение:**
`ContainerSupport.containerCapacity` и ветвления по state-типам расширяемы, но при росте списка контейнеров усложняют сопровождение.

**Рекомендуемая правка:**
Ввести `ContainerCapabilityRegistry` (`material -> capacity + read/apply strategy`) вместо централизованного большого `when`.

Это упростит добавление новых типов и уменьшит вероятность пропусков.

### 172) Единый block-state adapter слой
**Наблюдение:**
`readContainerContents`/`applyContainerContents` дублируют идеи адаптации разных state-интерфейсов к общему списку слотов.

**Рекомендуемая правка:**
Выделить `ContainerStateAdapter` (readSlots/applySlots) с реализациями для `Container`, `Campfire`, `BrushableBlock`, `Lectern` и т.д.

### 173) Mutation pipeline simplification в `EquippableSupport`
**Наблюдение:**
В `EquippableSupport` есть несколько близких методов мутации (`mutateFromExistingOrPrototype`, `mutateFromExistingOrNew`, `apply`), что повышает когнитивную сложность.

**Рекомендуемая правка:**
Свести к единому `mutate(item, sourcePolicy, mutator)` с явной политикой источника snapshot (`explicit`, `prototype`, `new`) и общим post-normalize этапом.

### 174) Ordinary-field comparator как reusable policy
**Наблюдение:**
Проверки ordinary-полей и `componentsMatch` завязаны на ручной список полей equippable; при изменении API легко получить рассинхрон.

**Рекомендуемая правка:**
Вынести `EquippableDiffPolicy` (field-by-field diff + explainable result), чтобы централизованно управлять normalizing и диагностиками.

### 175) Unified component-empty policy
**Наблюдение:**
`UseRemainderSupport.isEmpty` повторяет общие проверки пустого ItemStack, которые также встречаются в других support-классах.

**Рекомендуемая правка:**
Зафиксировать `ItemStackEmptyPolicy` utility и использовать его во всех component supports для единообразия.

### 176) Generic component support base
**Наблюдение:**
Support-классы (`UseCooldownSupport`, `UseRemainderSupport`, частично `EquippableSupport`) повторяют схожую структуру операций get/has/set/clear.

**Рекомендуемая правка:**
Ввести `ComponentSupportBase<T>` (read/write/clear/display hooks) и строить доменные supports как тонкие адаптеры поверх общего шаблона.

Это уменьшит дублирование и упростит развитие editor component API.

---

## AO. Edit command parsers and gateways

### 177) Единый parse-result контракт для `EditParsers`
**Наблюдение:**
`EditParsers` возвращает nullable значения, из-за чего вызывающий код вынужден вручную определять причину ошибки и формат user-feedback.

**Рекомендуемая правка:**
Перейти на `ParseResult<T>` (`ok/value/errorCode/hint`) с единым mapper в MessageKey/подсказку.

Это улучшит объяснимость отказов и сократит ad-hoc обработку null.

### 178) Декомпозиция `EditParsers` по доменным группам
**Наблюдение:**
Один класс обслуживает много разных доменов (registry keys, materials, sounds, effects, booleans, colors, slot groups).

**Рекомендуемая правка:**
Выделить модульные парсеры (`RegistryKeyParsers`, `MaterialParsers`, `EffectParsers`, `PrimitiveParsers`) и собрать их в фасад.

### 179) Общий suggestion-engine для registry-backed подсказок
**Наблюдение:**
`materialSuggestions`, `blockItemSuggestions`, `soundSuggestions` повторяют общий паттерн normalize/filter/sort.

**Рекомендуемая правка:**
Вынести `RegistrySuggestionEngine` с параметрами источника и фильтров, чтобы уменьшить дублирование и упростить кэширование результатов.

### 180) Precondition chain abstraction для команд `edit`/`apply`
**Наблюдение:**
`EditCommand` вручную выполняет link-check, а похожие preconditions встречаются в других командах (mode/permission/world/toggle).

**Рекомендуемая правка:**
Ввести `CommandPreconditionChain` (ordered checks + fail reason + message strategy) и подключать её декларативно в командах.

### 181) Наблюдаемость apply-flow отказов
**Наблюдение:**
`ApplyCommand` делегирует выполнение в menuService, но на уровне gateway полезно иметь стандартизованные reason-codes по отказам/валидации.

**Рекомендуемая правка:**
Добавить `ApplyExecutionAudit` (kind, args shape, result code, duration, actor context) для диагностики UX-проблем и тонкой настройки подсказок.

### 182) Политика cooldown исключений для технических команд
**Наблюдение:**
`ApplyCommand` отключает cooldown (`useCooldown = false`) — оправданно для интерактивного editor flow, но желательно формально закрепить правила для подобных исключений.

**Рекомендуемая правка:**
Зафиксировать `CommandCooldownPolicy` с категориями команд (interactive/editor/admin/gameplay) и явным rationale, чтобы избегать непоследовательности при новых командах.

---

## AP. Banner command routing layer

### 183) Subcommand dispatcher для `BannerCommand`
**Наблюдение:**
`BannerCommand` объединяет несколько moderation/user сценариев в одном `when`, что повышает связность при добавлении новых сабкоманд.

**Рекомендуемая правка:**
Ввести `BannerSubcommandDispatcher` (`name -> handler`) с отдельными handler-классами для `post/ban/banuser/banlists`.

Это улучшит SRP и упростит тестирование каждого сценария.

### 184) Единый permission-gate helper для moderation команд
**Наблюдение:**
Проверки permission и denial message повторяются между banner-командами (`BannerCommand`, `DecorationBannersCommand`).

**Рекомендуемая правка:**
Сформировать `ModerationPermissionGate` с унифицированным deny-reason и telemetry hooks.

### 185) Нормализатор аргументов для флагов (`-m`) и positional params
**Наблюдение:**
`DecorationBannersCommand` вручную извлекает `-m` и authorName, что при расширении аргументов может стать источником неоднозначностей.

**Рекомендуемая правка:**
Добавить `BannerCommandArgsParser` (flags + positional extraction + validation), чтобы централизовать интерпретацию аргументов.

### 186) Suggestion aggregation policy
**Наблюдение:**
Таб-комплит объединяет источники suggestions (storage + online players) ad-hoc с `distinct()`; полезно формализовать приоритет/лимиты/сортировку.

**Рекомендуемая правка:**
Ввести `SuggestionAggregationPolicy` (source priority, dedup strategy, max size, locale sort), применимый и в других командах.

### 187) User existence resolver contract для moderation flows
**Наблюдение:**
`hasKnownUser` проверяет цель через `BukkitHelper.getUser(...)`, но подобная проверка может дублироваться в других moderation командах.

**Рекомендуемая правка:**
Выделить `ModerationTargetResolver` с едиными результатами (`FOUND/NOT_FOUND/AMBIGUOUS`) и стандартным user-feedback.

### 188) Декларативные one-step command adapters
**Наблюдение:**
`MyFlagsCommand` и `BannerEditCommand` являются тонкими прокси к одному вызову сервиса.

**Рекомендуемая правка:**
Добавить lightweight `ServiceDelegatingCommand` adapter для одношаговых команд, чтобы уменьшить boilerplate при сохранении читаемости регистрации.

---

## AQ. Admin/decor-head command layer

### 189) Generic thin-command adapter для menu-entry команд
**Наблюдение:**
`DecorationHeadsCommand` полностью делегирует действие в сервис; аналогичные команды уже встречаются в других доменах.

**Рекомендуемая правка:**
Ввести `MenuEntryCommandAdapter` (service callback + optional preconditions), чтобы сократить boilerplate и унифицировать поведение thin-команд.

### 190) Subcommand registry для `AdvancedCreativeAdminCommand`
**Наблюдение:**
Admin root-команда использует `when` для сабкоманд и локальные `handleX` методы; при росте числа maintenance функций это усложнит расширение.

**Рекомендуемая правка:**
Перейти на `AdminSubcommandRegistry` (name/aliases -> handler + usage/help + permission), сохранив текущий UX.

### 191) Unified audit path для admin toggle/maintenance действий
**Наблюдение:**
Для `ManagedSystem.LOGGER` используется отдельный `plugin.logger`, тогда как остальное идёт через `actionLogger`; это оправдано, но policy не зафиксирована явно.

**Рекомендуемая правка:**
Определить `AdminAuditRoutingPolicy` (which channel for which system/action) и обеспечить консистентные correlation-id записи.

### 192) Consistent admin tab-complete contract
**Наблюдение:**
Tab-complete для admin сабкоманд реализован вручную и может разрастаться вместе с реестром операций.

**Рекомендуемая правка:**
Ввести `AdminTabCompletionProvider` на основе реестра сабкоманд (arguments schema + dynamic providers), чтобы убрать ручные ветвления.

### 193) Snapshot timestamping для memory-report
**Наблюдение:**
`MemoryUsageReporter` агрегирует несколько snapshot-источников; без явной метки времени сложно сравнивать отчёты между вызовами и коррелировать с инцидентами.

**Рекомендуемая правка:**
Добавить `reportTimestamp`/`samplingDuration` и выводить их в отчёте + audit событиях для повышения диагностической ценности.

### 194) Quality tiers для memory-estimation accuracy
**Наблюдение:**
Эвристические коэффициенты полезны, но разные подсистемы могут иметь разную точность оценок.

**Рекомендуемая правка:**
Ввести `MemoryEstimateQualityTier` (exact/estimated/coarse) на блок отчёта, чтобы операторы видели доверие к цифрам и могли приоритезировать проверку.

---

## AR. Config/permission/command metadata core

### 195) Декомпозиция `ConfigManager` на migration/load/cache модули
**Наблюдение:**
`ConfigManager` совмещает обеспечение файлов, миграции legacy секций, merge defaults, runtime cache (`stringToNumericIds`) и set/save операции.

**Рекомендуемая правка:**
Разделить на компоненты:
- `ConfigFileProvisioner`,
- `ConfigMigrationService`,
- `ConfigMergeService`,
- `ConfigRuntimeCache`.

Это снизит связность и упростит тестирование edge-cases миграций.

### 196) Явный ownership-map для config paths
**Наблюдение:**
`resolveOwner(path)` определяет владельца по `substringBefore('.')` и rootKeys; при росте структуры конфигов возможны неоднозначности.

**Рекомендуемая правка:**
Ввести `ConfigPathOwnershipMap` с валидацией конфликтов rootKeys на старте и диагностикой для неизвестных путей.

### 197) Role config validation policy
**Наблюдение:**
`PermissionManager.reload` доверяет структуре role-конфига; ошибки в ролях/permission списках могут проявиться только в runtime UX.

**Рекомендуемая правка:**
Добавить `RoleConfigValidator` (required fields, duplicate permissions, invalid MiniMessage/prefix issues) с отчётом при загрузке.

### 198) Permission resolution diagnostics
**Наблюдение:**
При permission denied полезно знать не только required role, но и цепочку резолва (node normalization -> role mapping source).

**Рекомендуемая правка:**
Ввести `PermissionResolutionTrace` (normalized key, matched role, fallback path) и экспонировать в debug/admin diagnostics.

### 199) Metadata enrichment для `PluginCommandType`
**Наблюдение:**
Текущая модель команды покрывает базовые поля, но для policy-driven систем могут понадобиться дополнительные атрибуты.

**Рекомендуемая правка:**
Расширить метадату командами типа:
- `category` (gameplay/editor/admin),
- `cooldownPolicy` override,
- `preconditions` hints,
- `uiVisibility` flags.

Это упростит декларативную сборку command registries/help/access policies.

### 200) Contract consistency checks между command metadata и permission config
**Наблюдение:**
`PluginCommandType.permissionNode` и role-based permission mapping живут в разных источниках; возможны рассинхроны (команда есть, роли не знают permission или наоборот).

**Рекомендуемая правка:**
Добавить startup-check `CommandPermissionConsistencyReport`, который сверяет метаданные команд с role-конфигом и выдаёт actionable предупреждения.

---

## AS. Decoration-heads caches and recent history

### 201) Generic concurrent LRU cache contract
**Наблюдение:**
`LruCache` решает локальную задачу, но похожие bounded-cache паттерны уже встречались в других подсистемах.

**Рекомендуемая правка:**
Сформировать общий `ConcurrentBoundedCache<K,V>` контракт (LRU policy, snapshot, stats, clear hooks), переиспользуемый across domains.

### 202) Structured key model для `SearchIndex`
**Наблюдение:**
`SearchIndex` формирует ключ строкой `"$query:$page:$pageSize"`, что просто, но хрупко при расширении параметров поиска.

**Рекомендуемая правка:**
Ввести typed key (`SearchCacheKey(query,page,pageSize,filters?)`) и сериализацию/хэш отдельно от бизнес-логики.

### 203) Query normalization policy tiers
**Наблюдение:**
`SearchQueryNormalizer` ограничивается `trim+lowercase`; для мультиязычного контента может потребоваться более богатая нормализация.

**Рекомендуемая правка:**
Добавить `SearchNormalizationPolicy` уровней (basic/locale-aware/transliteration) с конфиг-переключателем и A/B-метриками hit-rate.

### 204) Flush scheduler policy для `RecentService`
**Наблюдение:**
`RecentService` использует dirty-set и явный flush; при высокой активности важен прогнозируемый интервал сброса и backpressure.

**Рекомендуемая правка:**
Определить `RecentFlushPolicy` (interval, max batch, shutdown flush, retry on failure) и связать с server lifecycle hooks.

### 205) Deferred promotion consistency guarantees
**Наблюдение:**
`playersWithDeferredPromotions` + `commitDeferredPromotionsInternal` обеспечивает lazy reorder, но полезно явно зафиксировать guarantee при race-сценариях.

**Рекомендуемая правка:**
Ввести `DeferredPromotionContract` (at-least-once reorder semantics, idempotent merge, ordering timestamp rules) и покрыть тестами конкурентных кейсов.

### 206) Recent history diagnostics snapshot
**Наблюдение:**
Сейчас есть memory snapshot (players/entries), но нет расширенной операционной картины по dirty/queued/promotions/flush latency.

**Рекомендуемая правка:**
Добавить `RecentServiceDiagnostics` (dirtyPlayers, pendingPromotions, lastFlushDuration, flushFailures, evictions) для admin observability.

---

## AT. Decoration-heads category and menu rendering

### 207) Category config validation на старте
**Наблюдение:**
`CategoryRegistry` и `CategoryResolver` обрабатывают fallback-моды и warnings, но полезно иметь централизованный preflight для конфигурации категорий.

**Рекомендуемая правка:**
Добавить `DecorationHeadCategoryValidator` (unique keys, valid mode, non-empty apiNames for CATEGORY_GROUP, display checks) с отчётом при загрузке.

### 208) Typed category resolution result
**Наблюдение:**
`applyApiCategories` возвращает список warning-строк; при расширении логики удобнее структурированный результат.

**Рекомендуемая правка:**
Перейти на `CategoryResolutionReport` (resolvedCount, unresolvedGroups, unknownApiNames, warnings, severity) для лучшей диагностики и UI/admin-интеграции.

### 209) Renderer template extraction
**Наблюдение:**
`MenuRenderer` содержит несколько методов с повторяющимися шагами (`baseMenu`, fill, navigation buttons, content slots).

**Рекомендуемая правка:**
Выделить `MenuRenderTemplate`/`MenuSectionComposer` для общих блоков рендера, оставив в методах только screen-specific данные и callbacks.

### 210) Callback bundling для screen actions
**Наблюдение:**
Сигнатуры методов `render*` содержат много callback-параметров, что усложняет читаемость и передачу контекста.

**Рекомендуемая правка:**
Использовать screen-specific action objects (`CategoryMenuActions`, `RecentMenuActions`, `SavedPagesActions`) для компактного и типобезопасного контракта.

### 211) Slot layout contracts вместо inline чисел
**Наблюдение:**
Несмотря на helper-методы, конкретные интерактивные слоты по-прежнему задаются в рендер-методах вручную.

**Рекомендуемая правка:**
Вынести layout в `DecorationHeadsMenuLayout` (named slot groups per screen), чтобы уменьшить риск ошибки при перестановках.

### 212) Renderer observability hooks
**Наблюдение:**
Текущая логика рендера почти не публикует метрики (время рендера, entry count, active mode/filter), что затрудняет диагностику UI-лагов.

**Рекомендуемая правка:**
Добавить `MenuRenderTelemetry` (screen type, entries count, render duration, action source) и lightweight throttled logging для проблемных сценариев.

---

## AU. Decoration-heads input/temporary UI/color palette

### 213) Reusable text-input contract поверх sign API
**Наблюдение:**
`SignInputService` решает задачу sign-input локально; похожие сценарии сбора текста могут появляться в других меню.

**Рекомендуемая правка:**
Выделить `TextInputService` abstraction (sign/chat/anvil strategies) с единым `InputResult` контрактом и политикой отмены/таймаута.

### 214) Normalize policy для template-aware input
**Наблюдение:**
Логика «первой осмысленной строки» (с исключением шаблона) полезна, но зашита в текущей реализации.

**Рекомендуемая правка:**
Вынести `InputNormalizationPolicy` (trim/blank/template-filter/multi-line strategy), чтобы использовать единообразно в интерактивных формах.

### 215) Cancel-safe temporary button overrides
**Наблюдение:**
`TemporaryMenuButtonOverrideSupport` не хранит версионирование override-операций на слот; при нескольких быстрых replace может произойти неожиданный restore.

**Рекомендуемая правка:**
Добавить token/version guard per slot (`overrideId`) и отмену устаревших restore-задач.

### 216) Unified transient UI feedback toolkit
**Наблюдение:**
Временные override-кнопки используются как форма ephemeral feedback, что повторяется и в других menu-подсистемах.

**Рекомендуемая правка:**
Сформировать `TransientMenuFeedbackService` (toast-button, cooldown-indicator, temporary disable) поверх scheduler + restore policies.

### 217) Palette source and theme policy
**Наблюдение:**
`MapPreviewColorPalette` hardcoded и сочетает legacy mini-tag + hex + `Color`; полезно иметь явную стратегию источника и совместимости.

**Рекомендуемая правка:**
Ввести `PreviewPalettePolicy` (built-in defaults + config overrides + legacy mapping), с валидацией уникальности key и корректности hex.

### 218) Palette observability and usage analytics
**Наблюдение:**
Выборы цветов пользователями не агрегируются; без метрик сложно понять, какие опции реально востребованы.

**Рекомендуемая правка:**
Добавить `PaletteUsageMetrics` (selected key counts, fallback hits, invalid key recoveries) для UI-тюнинга и очистки неиспользуемых опций.
