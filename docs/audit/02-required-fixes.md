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
