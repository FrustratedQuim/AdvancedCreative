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
