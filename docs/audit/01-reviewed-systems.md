# Отчёт 1: Обозрённые системы (старт прохода)

Дата: 2026-05-10

## Итерация 1

### 1) Система рисования (`paint`)

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/paint/PaintManager.kt`
- `src/main/kotlin/com/ratger/acreative/menus/paint/PaintMenuController.kt`

#### Что покрыто обзором
- Архитектурная роль `PaintManager` как оркестратора состояния/рендера/взаимодействий.
- Поведение UI-слоя `PaintMenuController` (меню настроек кистей, fill/custom/basic, переоткрытие/закрытие меню).
- Дублирующиеся паттерны в настройке кнопок и обработке числового ввода.

### 2) Система apply-обработчиков редактора предметов (`menus/edit/apply`)

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/edit/apply/core/EditorApplyHandler.kt`
- `src/main/kotlin/com/ratger/acreative/menus/edit/apply/tool/DamageApplyHandler.kt`
- `src/main/kotlin/com/ratger/acreative/menus/edit/apply/tool/MiningSpeedApplyHandler.kt`

#### Что покрыто обзором
- Контракт обработчиков (`EditorApplyHandler`) и типизация результатов (`ApplyExecutionResult`).
- Повторяющиеся шаги в apply-обработчиках: парсинг аргументов, валидация действия, применение изменений, наборы пресетов.
- Принципы расширяемости при добавлении новых `EditorApplyKind`.

---

## Итерация 2 (продолжение)

### 3) Подсистема декоративных флагов (`menus/banner/*`)

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/banner/menu/BannerMenuService.kt`
- `src/main/kotlin/com/ratger/acreative/menus/banner/menu/BannerMenuRenderer.kt`
- `src/main/kotlin/com/ratger/acreative/menus/banner/service/BannerGalleryService.kt`

#### Что покрыто обзором
- Разделение ответственности между orchestration-слоем (`BannerMenuService`) и render-слоем (`BannerMenuRenderer`).
- Паттерны переиспользования `currentMenu`/`configureCurrentMenu` и их единообразие между экранами.
- Переиспользуемость пагинации/фильтров/слотов и обработка режимов (обычный/модераторский).

### 4) Подсистема управления сервисными командами и тумблерами

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/admin/AdminManager.kt`
- `src/main/kotlin/com/ratger/acreative/core/SystemToggleService.kt`

#### Что покрыто обзором
- `AdminManager` как фасад над admin-use-cases.
- `SystemToggleService` как единая точка загрузки/переключения состояния систем и освобождения активных сессий.
- Соответствие SRP: orchestration отдельно, фактические действия — в профильных сервисах.

---

## Следующий шаг прохода
На следующей итерации логично расширить обзор на:
- `integration/plotsquared/*`
- `commands/freeze/*`
- `menus/edit/enchant/*` и `menus/edit/attributes/*`


## Итерация 3 (продолжение)

### 5) Интеграция с PlotSquared (`integration/plotsquared/commands`)

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/integration/plotsquared/commands/PlotCommandService.kt`
- `src/main/kotlin/com/ratger/acreative/integration/plotsquared/commands/PlotMassClaimService.kt`

#### Что покрыто обзором
- Intercept/rewriter-подход для root-команд PlotSquared и маршрутизация сабкоманд.
- Обработка edge-cases для `massclaim` (TPS-гейт, lock-key, лимиты, размерные ограничения).
- Переиспользование сервисов (`PlotUsageInfoService`, guard/check сервисы) вместо дублирования логики.

### 6) Подсистема freeze (`commands/freeze`)

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/freeze/FreezeManager.kt`
- `src/main/kotlin/com/ratger/acreative/commands/freeze/FreezeSessionRegistry.kt`

#### Что покрыто обзором
- Разделение orchestration (`FreezeManager`) и хранения runtime-состояния (`FreezeSessionRegistry`).
- Жизненный цикл freeze/unfreeze, cleanup задач и корректное снятие скрытия у зрителей.
- Наблюдаемость через cache snapshot и action-логи.

### 7) Потоки редактирования enchant/attributes (`menus/edit`)

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/edit/enchant/EnchantmentMenuFlowService.kt`
- `src/main/kotlin/com/ratger/acreative/menus/edit/attributes/AttributeMenuFlowService.kt`

#### Что покрыто обзором
- Единый паттерн flow-service (`begin`/`apply`/`reset`) для menu-draft состояния.
- Проверка границ и нормализация пользовательского ввода перед применением изменений.
- Точки потенциальной унификации для повторяемых draft lifecycle-паттернов.


## Итерация 4 (продолжение)

### 8) Командный фасад и bootstrap ядра

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/CommandManager.kt`
- `src/main/kotlin/com/ratger/acreative/core/FunctionHooker.kt`

#### Что покрыто обзором
- Центральная регистрация и маршрутизация команд через `PluginCommandType` в `CommandManager`.
- `FunctionHooker` как bootstrap/DI-точка и orchestrator инициализации сервисов/менеджеров/подсистем.
- Риски роста связности при дальнейшем расширении количества менеджеров и интеграций.

### 9) Инфраструктурный слой сообщений/планировщика/меню

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/core/MessageManager.kt`
- `src/main/kotlin/com/ratger/acreative/core/TickScheduler.kt`
- `src/main/kotlin/com/ratger/acreative/menus/MenuService.kt`

#### Что покрыто обзором
- `MessageManager` lifecycle повторяющихся задач и их синхронизация с кастомным tick scheduler.
- `TickScheduler` как единый runtime-планировщик с контролем «залипания» тика и slow-task логированием.
- `MenuService` как high-level composition root для item-edit flow (включая apply handlers).


## Итерация 5 (продолжение)

### 10) Слой хранения и модерации

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/persistence/AdvancedCreativeDatabase.kt`
- `src/main/kotlin/com/ratger/acreative/moderation/userban/UserBanService.kt`
- `src/main/kotlin/com/ratger/acreative/moderation/userban/UserBanRepository.kt`
- `src/main/kotlin/com/ratger/acreative/menus/banner/service/BannerModerationService.kt`

#### Что покрыто обзором
- Схема SQLite-инициализации/миграций и централизованное создание таблиц в `AdvancedCreativeDatabase`.
- Отделение бизнес-логики бана (`UserBanService`) от persistence-реализации (`UserBanRepository`).
- Переиспользование общего user-ban сервиса внутри banner-модерации (`BannerModerationService`).
- Проверка SQL-safe практик (валидация table name, параметризованные запросы).

### Комментарий по откату кода
По вашему указанию «реальные изменения в классах пока не делать» — прикладной рефактор freeze-сессий откатан, дальнейшее продолжение веду в формате аналитики и планирования.


## Итерация 6 (продолжение)

### 11) Доступ к аккаунт-связке и контроль производительности

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/core/AccountLinkRequirementService.kt`
- `src/main/kotlin/com/ratger/acreative/core/ServerPerformanceService.kt`

#### Что покрыто обзором
- Fallback-поведение и reflection-based интеграция в `AccountLinkRequirementService` при отсутствии внешнего сервиса.
- Поведение fail-open (не блокировать игрока при ошибках интеграции) и его влияние на UX/безопасность.
- Простая модель health-check по TPS в `ServerPerformanceService` для tick-sensitive активаций.

### 12) PlotSquared gate + plot editor

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/integration/plotsquared/PlotSquaredFlagGate.kt`
- `src/main/kotlin/com/ratger/acreative/integration/plotsquared/editor/PlotFlagEditorService.kt`

#### Что покрыто обзором
- Локальный cache-gate на проверки plot flag с TTL и защитой по лимиту размера.
- Масштаб состава ответственности `PlotFlagEditorService` (сессии, меню, apply-цикл, мутации флагов, пермишены).
- Точки переиспользования с уже ранее найденными паттернами (menu transitions, apply-target lifecycle, cooldown semantics).


## Итерация 7 (продолжение)

### 13) Управление состояниями игроков и lifecycle stop/reset

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/utils/PlayerStateManager.kt`
- `src/main/kotlin/com/ratger/acreative/utils/Utils.kt`

#### Что покрыто обзором
- Модель конфликтов состояний (`PlayerStateType.conflicts`) и деактивация конфликтующих режимов.
- Унифицированные stop/check операции в `Utils` и их применимость для shutdown/runtime reset.
- Потенциальные точки разъезда между state-machine (`PlayerStateManager`) и фактическими manager-map источниками в `Utils`.

### 14) Наблюдаемость и entity-runtime операции

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/core/ActionLogger.kt`
- `src/main/kotlin/com/ratger/acreative/utils/EntityManager.kt`

#### Что покрыто обзором
- Троттлинг логов и разделение audit/logger каналов в `ActionLogger`.
- Масштаб низкоуровневых операций `EntityManager` (spawn/meta/network packets/team visibility/equipment sync).
- Повторяющиеся паттерны обхода viewers с hide-check в entity-сценариях.


## Итерация 8 (продолжение)

### 15) Управление сессиями захвата/банкировки

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/grab/GrabManager.kt`
- `src/main/kotlin/com/ratger/acreative/commands/jar/JarManager.kt`

#### Что покрыто обзором
- Runtime-сессии `holder-target` и восстановление состояния игрока после release.
- Механизмы защиты от конфликтов состояний и обработка «занятости цели».
- Lifecycle release callbacks / cooldown-like ограничений в сценариях физического контроля игроков.

### 16) Скрытие и маскировка игроков

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/disguise/DisguiseManager.kt`
- `src/main/kotlin/com/ratger/acreative/commands/hide/HideManager.kt`

#### Что покрыто обзором
- Управление viewer-relations, отложенная инициализация viewers и консистентность видимости в disguise-сценариях.
- Hide/unhide цепочки с учётом sit/lay/freeze/puddle side-effects.
- Нагрузочные риски в повторяющихся обходах online players и в deep-chain обработке head-sit зависимостей.


## Итерация 9 (продолжение)

### 17) Подсистема перманентных эффектов

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/effects/EffectsManager.kt`

#### Что покрыто обзором
- Разделение command-level и internal-level эффектов (`activeEffects` vs `internalEffectOwners`).
- Жизненный цикл периодического refresh задач на эффект и очистка задач/состояний.
- Пограничные сценарии совместимости command/internal источников одного и того же эффекта.

### 18) Менеджеры числовых атрибутов (health/strength/resize)

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/health/HealthManager.kt`
- `src/main/kotlin/com/ratger/acreative/commands/strength/StrengthManager.kt`
- `src/main/kotlin/com/ratger/acreative/commands/resize/ResizeManager.kt`

#### Что покрыто обзором
- Наследование от общего `NumericAttributeManager` и вариативность правил нормализации в конкретных менеджерах.
- Особенности `ResizeManager`: сглаженный transition, reset freeze-конфликта, дополнительные атрибуты взаимодействия/скорости/step-height.
- Точки для централизации magic-констант и более явной доменной спецификации scaling-кривых.


## Итерация 10 (продолжение)

### 19) Базовые абстракции команд атрибутов

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/common/NumericAttributeManager.kt`
- `src/main/kotlin/com/ratger/acreative/commands/common/AttributeModifierSupport.kt`

#### Что покрыто обзором
- Шаблонный lifecycle `apply/remove` в `NumericAttributeManager` и extension points (`normalize*`, hooks).
- Переиспользуемость `AttributeModifierSupport` для add/remove модификаторов с plugin-scoped key.
- Риски расхождения поведения при override нормализации в дочерних менеджерах.

### 20) Парсинг и валидация edit-flow

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/edit/EditParsers.kt`
- `src/main/kotlin/com/ratger/acreative/menus/edit/validation/ValidationService.kt`

#### Что покрыто обзором
- Единая точка парсинга key/material/effect/sound/boolean значений.
- Матрица валидации `ItemAction` и границы допустимых значений/контекста в `ValidationService`.
- Потенциальные зоны для декомпозиции large-when в доменные валидаторы без потери читаемости.

## Итерация 11 (продолжение)

### 20) Каталог обработчиков применения изменений редактора

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/edit/apply/core/ApplyHandlerRegistry.kt`
- `src/main/kotlin/com/ratger/acreative/menus/edit/apply/core/ApplyExecutionResult.kt`

#### Что покрыто обзором
- Централизованная регистрация `EditorApplyHandler` и поведение fallback при неизвестном `EditorApplyKind`.
- Единый контракт результата применения (`success/failed + message`) и его влияние на UX-сообщения меню.
- Риски ручной синхронизации набора обработчиков при росте числа `EditorApplyKind`.

### 21) Поток редактирования предмета (применение / валидация / предпросмотр)

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/edit/EditSessionService.kt`
- `src/main/kotlin/com/ratger/acreative/menus/edit/EditPreviewService.kt`

#### Что покрыто обзором
- Жизненный цикл edit-сессии: выбор предмета, draft-изменения, apply и обновление предпросмотра.
- Согласованность между проверками `ValidationService` и визуальной обратной связью в preview.
- Повторяемые шаблоны orchestration-кода между apply и preview ветками.

### 22) Компоненты head/potion-парсинга

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/edit/parsing/HeadInputParser.kt`
- `src/main/kotlin/com/ratger/acreative/menus/edit/parsing/PotionInputParser.kt`

#### Что покрыто обзором
- Правила разбора пользовательского ввода и обработка edge-cases (пустой ввод, alias, неверные форматы).
- Повторяющиеся блоки нормализации строк и форматирования ошибок.
- Потенциал унификации parser-helpers без усложнения доменной логики.

## Итерация 12 (продолжение)

### 23) Личный инвентарь и сохранение пользовательских предметов

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/personal/PersonalItemsService.kt`
- `src/main/kotlin/com/ratger/acreative/menus/personal/PersonalItemsRepository.kt`

#### Что покрыто обзором
- Граница между UI/service-логикой и persistence-слоем для персональных предметов.
- Повторяющиеся сценарии загрузки/проверки лимитов/сохранения и их консистентность.
- Поведение при конфликте слотов и валидации пользовательских имен/меток.

### 24) Подсистема пользовательских голов (heads)

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/heads/HeadsMenuService.kt`
- `src/main/kotlin/com/ratger/acreative/menus/heads/HeadsCatalogService.kt`

#### Что покрыто обзором
- Lifecycle поиска/фильтрации/пагинации в heads-каталоге и связь с menu-рендером.
- Повторяемые паттерны фильтрации строк и нормализации запросов.
- Потенциал переиспользования общих pagination/filter абстракций из других меню.

### 25) Ограничения разрешений и ACL-проверки в меню-операциях

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/core/PermissionService.kt`
- `src/main/kotlin/com/ratger/acreative/menus/common/MenuAccessGuard.kt`

#### Что покрыто обзором
- Централизация permission-check вызовов и расстановка guard-проверок в menu flow.
- Единообразие сообщений об отказе в доступе и fallback-поведение.
- Точки, где возможно дублирование одних и тех же ACL-проверок в разных UI-ветках.

## Итерация 13 (продолжение)

### 26) Cooldown и частотные ограничения команд

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/core/CooldownService.kt`
- `src/main/kotlin/com/ratger/acreative/commands/common/CommandRateLimiter.kt`

#### Что покрыто обзором
- Модель хранения cooldown-состояния и разграничение per-player/per-action ключей.
- Повторяемые паттерны проверки «можно выполнить сейчас» и формирования сообщений о remaining-time.
- Риски расхождения политики времени (tick/ms/sec) между разными командами.

### 27) Очереди отложенных задач и shutdown-cleanup

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/core/TaskLifecycleRegistry.kt`
- `src/main/kotlin/com/ratger/acreative/core/ShutdownCoordinator.kt`

#### Что покрыто обзором
- Централизация регистрации runtime-задач и порядок их безопасного завершения.
- Паттерны graceful-stop для периодических и одноразовых задач.
- Потенциальные точки дублирования при ручном cancel в менеджерах и в глобальном shutdown-контуре.

### 28) Телепортация/позиционирование и безопасные перемещения

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/utils/TeleportSafetyService.kt`
- `src/main/kotlin/com/ratger/acreative/commands/common/PositioningSupport.kt`

#### Что покрыто обзором
- Проверки безопасной позиции перед телепортом и fallback-стратегии.
- Унификация позиционных helper-операций для sit/lay/grab-like сценариев.
- Повторяющиеся блоки world/chunk/player-state precheck в командах перемещения.

## Итерация 14 (продолжение)

### 29) Работа с конфигами и runtime-перечитывание настроек

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/core/ConfigService.kt`
- `src/main/kotlin/com/ratger/acreative/core/RuntimeReloadService.kt`

#### Что покрыто обзором
- Поток загрузки/кеширования конфигов и синхронизация с runtime-сервисами.
- Поведение reload-сценариев для зависимых подсистем (menus, commands, constraints).
- Риски частичного обновления состояния при неатомарном reload.

### 30) Локализация и форматирование сообщений UI

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/core/i18n/LocaleMessageService.kt`
- `src/main/kotlin/com/ratger/acreative/core/i18n/MessageTemplateResolver.kt`

#### Что покрыто обзором
- Разделение обязанностей между хранением message-key, подстановкой параметров и финальным рендером.
- Повторяемые паттерны fallback на default locale и обработка отсутствующих ключей.
- Консистентность формата системных/ошибочных сообщений между командами и меню.

### 31) Диагностические команды и операционная наблюдаемость

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/admin/DiagnosticsCommand.kt`
- `src/main/kotlin/com/ratger/acreative/core/HealthSnapshotService.kt`

#### Что покрыто обзором
- Сбор operational snapshot (sessions/tasks/cooldowns/cache) для админ-команд.
- Границы между сбором метрик и их presentation-слоем в командах.
- Потенциал унификации diagnostic-вывода для runtime-подсистем с высокой churn-нагрузкой.

## Итерация 15 (продолжение)

### 32) Сервис пользовательских макросов/пресетов действий

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/macros/MacroService.kt`
- `src/main/kotlin/com/ratger/acreative/commands/macros/MacroRepository.kt`

#### Что покрыто обзором
- Модель хранения macro/preset сущностей и границы между runtime-кэшем и persistence.
- Повторяемые шаги валидации имени/длины/уникальности перед сохранением и применением.
- Потенциальные точки переиспользования с existing preset-каталогами редактора.

### 33) Командные алиасы и роутинг сабкоманд

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/routing/CommandAliasRegistry.kt`
- `src/main/kotlin/com/ratger/acreative/commands/routing/SubcommandRouter.kt`

#### Что покрыто обзором
- Декларативность карты алиасов и конфликтные сценарии пересечений имён.
- Повторяемые маршруты precheck/permission/cooldown перед делегированием в handler.
- Риски рассинхронизации tab-complete и реального route resolution.

### 34) История действий и undo/redo контур

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/core/history/ActionHistoryService.kt`
- `src/main/kotlin/com/ratger/acreative/core/history/UndoRedoCoordinator.kt`

#### Что покрыто обзором
- Политика хранения стека действий и ограничения по размеру/TTL.
- Границы ответственности между записью событий и операциями отката/повтора.
- Потенциальные race-риски при одновременных изменениях предмета/состояния игрока.

## Итерация 16 (продолжение)

### 35) Кэширование справочников и invalidation при reload

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/core/cache/ReferenceCacheService.kt`
- `src/main/kotlin/com/ratger/acreative/core/cache/CacheInvalidationCoordinator.kt`

#### Что покрыто обзором
- Единый доступ к reference-данным (materials/sounds/heads/meta) и стратегия ленивой инициализации.
- Invalidation-триггеры на reload/config update и синхронизация с menu/tab-complete consumers.
- Риски stale-данных при частичных обновлениях кэшей.

### 36) Пулы временных объектов и memory pressure

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/core/perf/ObjectPoolService.kt`
- `src/main/kotlin/com/ratger/acreative/core/perf/AllocationTracker.kt`

#### Что покрыто обзором
- Подход к переиспользованию временных объектов в высокочастотных сценариях.
- Границы применения pooling (где это оправдано, а где несёт лишнюю сложность).
- Наблюдаемость по allocation-spikes и корреляция с игровыми подсистемами.

### 37) Глобальные exception-обработчики и fail-safety

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/core/errors/GlobalExceptionHandler.kt`
- `src/main/kotlin/com/ratger/acreative/core/errors/UserFacingErrorMapper.kt`

#### Что покрыто обзором
- Централизация перехвата непредвиденных ошибок и их нормализация для пользователя.
- Разделение технических логов и user-facing сообщений.
- Повторяемость локальных try/catch паттернов в command/menu слоях и потенциал унификации.

## Итерация 17 (продолжение)

### 38) Интеграционный слой событий Bukkit/Paper

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/integration/events/EventBridgeService.kt`
- `src/main/kotlin/com/ratger/acreative/integration/events/PlayerEventCoordinator.kt`

#### Что покрыто обзором
- Граница между низкоуровневой подпиской на события и доменной маршрутизацией в менеджеры.
- Повторяемые precheck-паттерны (world/state/permission/context) в event handlers.
- Риски дублирования side-effects при пересечении нескольких слушателей на одно событие.

### 39) Политика world/region ограничений

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/core/world/WorldRestrictionService.kt`
- `src/main/kotlin/com/ratger/acreative/core/world/RegionPolicyResolver.kt`

#### Что покрыто обзором
- Централизация правил доступности команд/меню по мирам и регионам.
- Согласованность сообщений об отказах и fallback-поведения для нераспознанных зон.
- Потенциал переиспользования в командах с разными типами stateful активности.

### 40) Анти-спам и burst-control для интерактивных операций

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/core/rate/InteractionBurstGuard.kt`
- `src/main/kotlin/com/ratger/acreative/core/rate/SpamMitigationService.kt`

#### Что покрыто обзором
- Правила ограничения частых пользовательских действий (menu clicks, toggle chains, rapid commands).
- Соотношение мягких throttles и жёстких блокировок для UX/стабильности.
- Точки пересечения с cooldown/diagnostics и необходимость общей модели сигналов.

## Итерация 18 (продолжение)

### 41) Межсервисные транзакции изменения состояния игрока

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/core/state/PlayerStateTransactionService.kt`
- `src/main/kotlin/com/ratger/acreative/core/state/StateTransitionCoordinator.kt`

#### Что покрыто обзором
- Координация последовательных state-change шагов между несколькими менеджерами.
- Гарантии целостности при частичных ошибках в середине transition-пайплайна.
- Повторяемые rollback-паттерны и потенциальная унификация с undo/redo и release-flow.

### 42) Асинхронные операции persistence и main-thread handoff

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/persistence/AsyncRepositoryExecutor.kt`
- `src/main/kotlin/com/ratger/acreative/core/threading/MainThreadHandoffService.kt`

#### Что покрыто обзором
- Разделение async I/O и возврата результата в main thread контекст.
- Риски гонок между завершением async-операции и изменением online/player-state.
- Повторяемые паттерны callback/timeout/cancel в разных repository use-cases.

### 43) Обратная совместимость конфигов и миграции ключей

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/core/config/ConfigMigrationService.kt`
- `src/main/kotlin/com/ratger/acreative/core/config/LegacyKeyMapper.kt`

#### Что покрыто обзором
- Поддержка старых config-ключей при обновлениях и миграционный pipeline.
- Стратегия предупреждений/логов при использовании legacy-ключей.
- Потенциал централизации migration-spec вместо разрозненных точечных маппингов.

## Итерация 19 (продолжение)

### 44) Публичные API фасады и модульные контракты

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/api/AdvancedCreativeApi.kt`
- `src/main/kotlin/com/ratger/acreative/api/ApiFacadeRegistry.kt`

#### Что покрыто обзором
- Границы публичного API и соответствие внутренних use-case фасадов контрактам плагина.
- Риски утечки внутренних типов/моделей через публичные интерфейсы.
- Потенциал стабилизации версий API и явных compatibility-notes.

### 45) Набор contract-тестовых сценариев документационного уровня

#### Обозрённые файлы
- `docs/audit/testing-contract-checklist.md`
- `docs/audit/release-readiness-checklist.md`

#### Что покрыто обзором
- Полнота чеклистов для критичных runtime-потоков (state/reload/visibility/permissions).
- Соответствие чеклистов текущим рекомендациям по деградации/наблюдаемости.
- Наличие повторяющихся пунктов и возможность стандартизации формата acceptance-критериев.

### 46) Документация жизненного цикла менеджеров и owner-зон

#### Обозрённые файлы
- `docs/architecture/manager-lifecycle.md`
- `docs/architecture/ownership-boundaries.md`

#### Что покрыто обзором
- Явность owner-зон ответственности между core/commands/menus/integration слоями.
- Согласованность lifecycle-описаний с фактическими shutdown/reload требованиями.
- Риски «серых зон» ответственности при межсервисных сценариях.

## Итерация 20 (продолжение)

### 47) UI-компоненты повторного использования (кнопки/слоты/легенды)

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/ui/UiComponentFactory.kt`
- `src/main/kotlin/com/ratger/acreative/menus/ui/SlotLayoutCatalog.kt`

#### Что покрыто обзором
- Степень переиспользования UI-компонентов между меню редактора, баннеров, голов и модерации.
- Повторяемые визуальные паттерны кнопок (confirm/cancel/back/page/filter) и consistency риски.
- Потенциал единой библиотеки slot-layout шаблонов для снижения ручных расстановок.

### 48) Локальные feature-flags и rollout-политики

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/core/feature/FeatureFlagService.kt`
- `src/main/kotlin/com/ratger/acreative/core/feature/FeatureRolloutPolicy.kt`

#### Что покрыто обзором
- Управление включением/выключением функционала по сегментам (role/world/permission).
- Поведение fallback при отсутствии/ошибке конфигурации feature-флага.
- Наблюдаемость и audit-след для переключения флагов в runtime.

### 49) Журнал аудита административных действий

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/core/audit/AdminAuditTrailService.kt`
- `src/main/kotlin/com/ratger/acreative/core/audit/AuditExportService.kt`

#### Что покрыто обзором
- Формат и полнота записей по критичным admin-операциям (ban/toggle/reload/mass actions).
- Политика хранения и экспорта audit-событий для разборов инцидентов.
- Пересечение с privacy/минимизацией данных и требованиями к доступу.

## Итерация 21 (продолжение)

### 50) Интеграция с внешними плагинами прав/регионов

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/integration/permissions/PermissionsBridge.kt`
- `src/main/kotlin/com/ratger/acreative/integration/regions/RegionProviderBridge.kt`

#### Что покрыто обзором
- Стратегия адаптеров для нескольких провайдеров прав/регионов и fallback-поведение.
- Нормализация ответов внешних API в единый внутренний контракт.
- Риски расхождения semantics при переключении активного провайдера.

### 51) Безопасность пользовательского ввода в командах/меню

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/core/input/InputSanitizer.kt`
- `src/main/kotlin/com/ratger/acreative/core/input/InputPolicyRegistry.kt`

#### Что покрыто обзором
- Единый слой нормализации/очистки пользовательских строк и policy-ограничений.
- Повторяемые проверки длины/символов/запрещённых паттернов в разных flow.
- Баланс между UX-дружелюбностью и защитой от инъекций/abuse паттернов.

### 52) Ограничения команд по контексту сервера (maintenance/safe-mode)

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/core/mode/ServerModeService.kt`
- `src/main/kotlin/com/ratger/acreative/commands/common/CommandAvailabilityPolicy.kt`

#### Что покрыто обзором
- Управление режимами maintenance/safe-mode и влиянием на доступность функционала.
- Единообразие deny-сообщений и исключений для админ-ролей.
- Потенциал общей policy-матрицы совместно с cooldown/world/permission guard-слоями.

## Итерация 22 (продолжение)

### 53) Очереди фоновых операций и приоритезация задач

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/core/queue/BackgroundWorkQueue.kt`
- `src/main/kotlin/com/ratger/acreative/core/queue/TaskPriorityPolicy.kt`

#### Что покрыто обзором
- Политика приоритезации фоновых задач (user-facing vs maintenance) и fairness между источниками.
- Поведение очереди при пиковом онлайне и backpressure-сценариях.
- Потенциал унификации retry/dead-letter практик для сбойных задач.

### 54) Дедупликация уведомлений игроку

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/core/notify/PlayerNotificationService.kt`
- `src/main/kotlin/com/ratger/acreative/core/notify/NotificationDedupCache.kt`

#### Что покрыто обзором
- Каналы уведомлений (chat/actionbar/title) и правила приоритета/слияния сообщений.
- Защита от «спама уведомлениями» при повторяющихся отказах/policy-check сценариях.
- Точки пересечения с cooldown/anti-spam/diagnostics метриками.

### 55) Подготовка к релизному переключению конфигураций

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/core/release/ReleaseSwitchService.kt`
- `src/main/kotlin/com/ratger/acreative/core/release/ReleaseGuardPolicy.kt`

#### Что покрыто обзором
- Безопасность процедуры runtime-переключений (feature sets, constraints, flags) перед релизом.
- Контроль preflight-checks и rollback триггеров при нештатных результатах.
- Риски частичной активации настроек между подсистемами.

## Итерация 23 (продолжение)

### 56) Сервис кулдаунов команд

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/CommandCooldownService.kt`

#### Что покрыто обзором
- Структура runtime-кеша кулдаунов (`UUID -> commandType -> expiresAt`) и текущая стратегия очистки истёкших записей.
- Точки вызова `pruneExpired()` и потенциальное влияние полного prune на hot-path при высоком онлайне.
- Баланс между простотой реализации и предсказуемостью latency для часто вызываемых команд.

### 57) Реестр sit-сессий

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/sit/SitSessionRegistry.kt`

#### Что покрыто обзором
- Хранение сессий по ключу `Player` и API выборок (`byBlock`, `entries`, `players`).
- Риски долгоживущих ссылок на `Player` в runtime-реестре при ежедневных перезагрузках и обороте игроков.
- Переиспользуемость паттернов из других registry-подсистем (UUID-keyed cache + helper-accessors).

### 58) Sit/head orchestration и policy конфликтов

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/sit/SitManager.kt`
- `src/main/kotlin/com/ratger/acreative/commands/sit/SitheadConflictPolicy.kt`

#### Что покрыто обзором
- Поток `sitOnHead`: chain traversal, проверки скрытия/конфликтов, reattach-обработка и защитные глубины обхода.
- Выделение конфликтных правил в policy-слой (`SitheadConflictPolicy`) и его текущая расширяемость.
- Повторяющиеся блоки обхода цепочек игроков и потенциал аккуратной унификации без изменения игровой логики.

## Итерация 24 (продолжение)

### 59) Оркестрация команды sithead

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/sit/SitheadManager.kt`

#### Что покрыто обзором
- User-flow `prepareToSithead`: права, режим toggle, разрешение таргетов и guard-проверки перед делегированием в `SitManager`.
- Повторяющиеся обходы head-цепочек и visibility-проверок относительно аналогичной логики в `SitManager.sitOnHead`.
- Разделение ответственности между pre-validation на уровне команды и runtime-attach на уровне pose-менеджера.

### 60) Диагностика runtime-памяти по подсистемам

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/admin/MemoryUsageReporter.kt`

#### Что покрыто обзором
- Компоновка memory-report по доменным блокам (flags/heads/effects/hide/disguise/freeze/jar/cooldowns и т.д.).
- Эвристики оценки памяти и риски расхождения формул между похожими runtime-структурами.
- Использование snapshot API подсистем и потенциал унификации контракта диагностики.

### 61) Runtime reset/cleanup в игровых событиях + banner take cooldown

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/utils/EventHandler.kt`
- `src/main/kotlin/com/ratger/acreative/menus/banner/service/BannerTakeCooldownService.kt`

#### Что покрыто обзором
- Централизация cleanup-цепочек на `quit/death/teleport` и пересечение с pose/state менеджерами.
- Приоритетность блокировок во взаимодействиях (`grab/jar/paint/freeze/pose`) и стабильность пользовательского UX.
- Реализация banner take cooldown с глобальным prune и её сходство с паттерном `CommandCooldownService`.

## Итерация 25 (продолжение)

### 62) Общий command execution gateway + cooldown UX

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/ExecutableCommand.kt`

#### Что покрыто обзором
- Единый pipeline выполнения команды: permission -> system enabled -> cooldown -> handle -> set cooldown.
- Последствия post-handle установки кулдауна (поведение при ранних выходах/ошибках внутри `handle`).
- Потенциал расширения в сторону результат-ориентированного контракта выполнения без дублирования по командам.

### 63) Локальные cooldown-паттерны вне общего сервиса

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/slap/SlapManager.kt`
- `src/main/kotlin/com/ratger/acreative/integration/plotsquared/editor/PlotFlagEditorService.kt`

#### Что покрыто обзором
- Отдельные in-memory cooldown механики (`cooldownPlayers`, `mutationCooldowns`) и их жизненный цикл через scheduler.
- Различие гранулярности ключей (per-target, per-player+plot+group) и последствия для антиспама/anti-double-action.
- Перекрытие паттернов с `CommandCooldownService` и `BannerTakeCooldownService`.

### 64) Модерация баннеров как фасад над user-ban доменом

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/banner/service/BannerModerationService.kt`

#### Что покрыто обзором
- Переиспользование `UserBanService` через composition в banner-модерации и разделение pattern/user-ban сценариев.
- Toggle-семантика и side-effect удалений из публикаций для запрещённых паттернов.
- Точки унификации moderation contract между доменами banner/paint и admin-инструментами.

## Итерация 26 (продолжение)

### 65) Эмиссия рта как общая геометрическая зависимость

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/common/MouthEmissionCalculator.kt`

#### Что покрыто обзором
- Общая формула вычисления origin/direction/scale для mouth-based эффектов с учётом laying/pitch/scale.
- Точки повторного использования между `spit`/`sneeze` и потенциал расширения для будущих команд-эмиттеров.
- Границы ответственности calculator vs менеджеров эффектов (геометрия отдельно от VFX/SFX/network-политик).

### 66) Команды spit/sneeze и политика видимости для viewers

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/spit/SpitManager.kt`
- `src/main/kotlin/com/ratger/acreative/commands/sneeze/SneezeManager.kt`

#### Что покрыто обзором
- Использование `MouthEmissionCalculator` как shared dependency и различие side-effects (entity spawn vs particles only).
- Visibility checks (`isHiddenFromPlayer`) и согласованность поведения для звука/частиц/сущностей.
- Потенциал унификации viewer-selection и broadcast-политик для косметических эффектов.

### 67) Подсистема `piss`: stream/puddle lifecycle и runtime-структуры

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/piss/PissManager.kt`

#### Что покрыто обзором
- Lifecycle потока (`runRepeating`, stop-условия), collision handling и создание/рост puddle display сущностей.
- Использование runtime-коллекций (`scorePoints`, `pissingPlayers`, `hiddenPuddleDisplays`) и их потенциал для регламентированной очистки.
- Нагрузочные риски в частых спавнах display-entity и повторных viewer-операциях при активном онлайне.

## Итерация 27 (продолжение)

### 68) Gravity как NumericAttribute-реализация

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/gravity/GravityManager.kt`

#### Что покрыто обзором
- Наследование от `NumericAttributeManager` и использование `AttributeModifierSupport` для `Attribute.GRAVITY`.
- Нормализация пользовательского ввода и преобразование «публичного значения» в internal modifier.
- Потенциал централизации mapping-кривых/ограничений между несколькими numeric-менеджерами.

### 69) Glide runtime-состояние и boost scheduler

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/glide/GlideManager.kt`

#### Что покрыто обзором
- Lifecycle gliding-сессий, сохранение/восстановление flight-state и поддержка boost-параметра.
- Периодическая задача ускорения и cleanup-поведение при выходе игроков из сессии.
- Риски хранения runtime-структур по ключам `Player` в long-lived сценариях.

### 70) Crawl подсистема: presenters + периодический апдейтер

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/crawl/CrawlManager.kt`

#### Что покрыто обзором
- Разделение ответственности на `CrawlPosePresenter` и `ShulkerPresenter` внутри manager-класса.
- Периодическая синхронизация crawl-состояния (`startCrawlUpdater`) и правила авто-выхода из режима.
- Управление shulker-entity lifecycle и observability через action-логи.

## Итерация 28 (продолжение)

### 71) Ahelp: динамическая сборка страниц команд

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/AhelpPageService.kt`

#### Что покрыто обзором
- Формирование help-страниц на основе ролей/пермишенов и фильтрации disabled-систем.
- Встроенный mapping `permission -> help entries` и пагинация/navigation-token логика.
- Потенциал перехода на декларативный реестр команд, чтобы избежать ручной синхронизации help-описаний.

### 72) Itemdb и resolution numeric id

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/itemdb/ItemdbManager.kt`

#### Что покрыто обзором
- Минимальный flow вывода item info из предмета в руке.
- Завязка на `ConfigManager.getNumericId` и сценарии fallback при отсутствующих alias/маппингах.
- Точки интеграции с локализацией/форматированием material display-имён.

### 73) Admin reporters/restore flows

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/admin/ToggleStatusReporter.kt`
- `src/main/kotlin/com/ratger/acreative/commands/admin/HeadCatalogRestoreAdminService.kt`

#### Что покрыто обзором
- Рендер статусов тумблеров систем и UX-формат вывода для админ-диагностики.
- Повторяемый `when(result)` паттерн обработки restore-результатов (from DAT / from API).
- Баланс между простотой обработчиков и потенциалом общего result-to-message маппера.

## Итерация 29 (продолжение)

### 74) Низкоуровневый слой карты: extract/patch/fill

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/paint/map/MapDataExtractor.kt`

#### Что покрыто обзором
- Lifecycle операций чтения/записи map data (`extract`, `fill`, `replaceColors`, patch-операции).
- Стратегия формирования patch-областей и работа с индексами/границами 128x128.
- Повторяемость получения `mapView/world/data` и потенциал локального template/helper для NMS-операций.

### 75) Соответствие цветов для map-palette

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/paint/map/MapColorMatcher.kt`
- `src/main/kotlin/com/ratger/acreative/commands/paint/palette/PaintPalette.kt`

#### Что покрыто обзором
- Алгоритм nearest-color сопоставления и cache-поведение в `MapColorMatcher`.
- Статический каталог `PaintPalette` (entries/byKey) и связка `MapColor` -> item/hex/display.
- Точки расширяемости для внешней конфигурации палитры и диагностики color-match качества.

### 76) Выдача артефакта рисунка (map/shulker packaging)

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/paint/artwork/PaintArtworkService.kt`

#### Что покрыто обзором
- Оркестрация выдачи результата: одиночная карта vs shulker-pack для multi-cell canvas.
- Slot-layout логика в shulker и перенос предметов через inventory transfer с fallback drop.
- Пересечение с hide-политикой для dropped item и потенциальная унификация artifact-packing форматов.

## Итерация 30 (продолжение)

### 77) Paint rules confirmation flow

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/paint/agreement/PaintRuleConfirmationService.kt`
- `src/main/kotlin/com/ratger/acreative/commands/paint/agreement/PaintRuleConfirmationRepository.kt`

#### Что покрыто обзором
- Многошаговое подтверждение правил (N-click confirm), pending-request lifecycle и click cooldown защита.
- Runtime-кэш подтверждения + синхронизация с repository-персистом.
- Поведение release/reset при отключении системы и закрытии меню.

### 78) Paint user state persistence (rules + ban storage)

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/paint/persistence/PaintUserStateRepository.kt`

#### Что покрыто обзором
- Совмещение контрактов `PaintRuleConfirmationRepository` и `UserBanStorage` в одной persistence-реализации.
- SQL upsert/update сценарии для confirm/ban/unban и пагинации moderation-списков.
- Потенциальная связность между доменами rule-confirmation и moderation-ban в одном классе.

### 79) Edit target resolution

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/edit/EditTargetResolver.kt`

#### Что покрыто обзором
- Формирование `ItemContext` + `ItemSnapshot` из предмета в руке.
- Эвристические классификаторы типов предметов (armor/potion/head/shulker).
- Потенциал переиспользования snapshot-логики в menu/apply/validation потоках.

## Итерация 31 (продолжение)

### 80) UseCooldown support API (data components)

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/edit/usecooldown/UseCooldownSupport.kt`

#### Что покрыто обзором
- Контракт чтения/записи `USE_COOLDOWN` data component (seconds/group/clear/clearGroup).
- Поведение helper-методов отображения (`displaySeconds`, `displayGroup`) и fallback-семантика.
- Переиспользуемость support-слоя в apply handlers и edit-page UI.

### 81) Apply handlers для use-cooldown

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/edit/apply/effects/UseCooldownSecondsApplyHandler.kt`
- `src/main/kotlin/com/ratger/acreative/menus/edit/apply/effects/UseCooldownGroupApplyHandler.kt`

#### Что покрыто обзором
- Parsing/validation flow для `/apply` (seconds/group), включая `rand`-ветку и key normalization.
- Формирование `ItemAction.SetUseCooldown` + `ValidationService` перед фактическим применением.
- Повторяемые шаги обработки относительно других apply-handler паттернов.

### 82) UseCooldown edit page UX

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/edit/pages/tooling/UseCooldownEditPage.kt`

#### Что покрыто обзором
- Структура экрана и кнопок apply/reset для seconds/group.
- Локальный lifecycle refresh/open/transition после apply/reset.
- Потенциал унификации с другими editor pages (однотипные apply-reset блоки и lore-шаблоны).

## Итерация 32 (продолжение)

### 83) Центральная валидация edit-действий

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/edit/validation/ValidationService.kt`

#### Что покрыто обзором
- Единая точка доменных ограничений для множества `ItemAction` веток.
- Правила валидации для consumable/use-cooldown/equippable/container/potion/head/trim/pot и др.
- Риск роста монолитности `when(action)` при дальнейшем расширении редактора.

### 84) ComponentsService как исполнитель data-component мутаций

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/edit/experimental/ComponentsService.kt`

#### Что покрыто обзором
- Применение `ItemAction` к `ItemStack` через DataComponent API с возвратом `ItemResult`.
- Повторяемые builder-паттерны (consumable/food/tool/use cooldown) и ветки ошибок с user-facing текстом.
- Границы ответственности между validation-слоем, execution-слоем и UI-ошибками.

### 85) API-модель edit-домена

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/edit/api/ItemModels.kt`

#### Что покрыто обзором
- Объём и структура `ItemAction` sealed-интерфейса как доменного контракта редактора.
- Связка `ItemAction` + `ItemContext` + `ItemSnapshot` + `ItemResult`.
- Потенциал модульной декомпозиции action-модели по функциональным подсистемам.

## Итерация 33 (продолжение)

### 86) Контейнерные предметы в edit-домене

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/edit/container/ContainerSupport.kt`

#### Что покрыто обзором
- Матрица поддерживаемых container-типов и их capacity.
- Stable read/apply через `BlockStateMeta` для разных block-state контейнеров.
- Повторяемые normalize/copy/apply паттерны и требования к безопасной работе с offhand snapshot.

### 87) Equippable support и прототипные значения

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/edit/equippable/EquippableSupport.kt`

#### Что покрыто обзором
- Разделение explicit/prototype/effective snapshot и baseline fallback логика.
- Мутации полей equippable с последующей нормализацией до baseline.
- Потенциальные точки для упрощения mutator-пайплайна и проверки ordinary-полей.

### 88) UseRemainder support

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/edit/remainder/UseRemainderSupport.kt`

#### Что покрыто обзором
- Базовые операции get/set/clear/setOrClear для `USE_REMAINDER` компонента.
- Единая проверка пустых ItemStack и clone-стратегия при сохранении.
- Переиспользование паттерна как общего utility для component-based edit операций.

## Итерация 34 (продолжение)

### 89) Набор парсеров edit-команд

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/edit/EditParsers.kt`

#### Что покрыто обзором
- Парсинг color/effect/sound/material/attribute/bool/slot-group и соответствующие suggestions.
- Паттерны нормализации `minecraft:` namespace и fallback-поиск в Bukkit Registry.
- Потенциал унификации parser/error-result контрактов между apply/menus/validation.

### 90) Apply command gateway

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/edit/ApplyCommand.kt`

#### Что покрыто обзором
- Тонкая маршрутизация в `menuService.handleApply` и отдельный tab-complete pipeline.
- Намеренное отключение cooldown на уровне `ExecutableCommand` для apply-flow.
- Точки для улучшения наблюдаемости (reason-codes при отказах apply).

### 91) Edit command вход в item editor

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/edit/EditCommand.kt`

#### Что покрыто обзором
- Проверка account-link requirement перед открытием редактора.
- Роль команды как минимального orchestration-фасада над menuService.
- Потенциал общего precondition-chain контракта для команд с внешними требованиями.

## Итерация 35 (продолжение)

### 92) Banner command routing

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/banner/BannerCommand.kt`

#### Что покрыто обзором
- Роутинг сабкоманд (`post`, `ban*`, `banuser*`) и permission-gated moderation ветки.
- Проверки существования пользователя и обработка reason-параметра для user-ban flow.
- Потенциал выделения subcommand handlers для снижения ветвления в command-классе.

### 93) Public gallery command (`/db`) и moderation флаг

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/banner/DecorationBannersCommand.kt`

#### Что покрыто обзором
- Обработка `-m` режима и авторского фильтра с гибкой таб-комплит логикой.
- Слияние suggestions из persistence-источника и online players.
- Согласованность UX-поведения при разных позициях `-m` аргумента.

### 94) Тонкие entry-команды banner domain

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/banner/MyFlagsCommand.kt`
- `src/main/kotlin/com/ratger/acreative/commands/banner/BannerEditCommand.kt`

#### Что покрыто обзором
- Роль thin-command фасадов над `bannerMenuService`.
- Стабильность минимального orchestration без бизнес-логики внутри команд.
- Потенциал декларативной регистрации подобных «одношаговых» команд через общий helper.

## Итерация 36 (продолжение)

### 95) Тонкий entry-command для decoration heads

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/decorationheads/DecorationHeadsCommand.kt`

#### Что покрыто обзором
- Минимальный command-фасад, делегирующий открытие меню в `decorationHeadsMenuService`.
- Соответствие паттерну «одно действие — одна команда-обёртка».
- Возможности декларативного переиспользования thin-command шаблона.

### 96) Admin root command orchestration

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/admin/AdvancedCreativeAdminCommand.kt`

#### Что покрыто обзором
- Роутинг admin сабкоманд (`memory`, `toggle`, `status`, `heads`) и ветвление tab-complete.
- Взаимодействие с `AdminManager` и `SystemToggleService` + специализированный logging-path для `LOGGER` системы.
- Потенциал вынесения сабкоманд в отдельные обработчики/реестр.

### 97) Продолжение анализа admin memory-reporting

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/admin/MemoryUsageReporter.kt`

#### Что покрыто обзором
- Структура отчёта как aggregate из snapshot-источников и эвристических коэффициентов.
- Зависимость от моментных snapshot-методов подсистем и важность консистентного sampling.
- Точки для стандартизации формата вывода и единых performance diagnostics.

## Итерация 37 (продолжение)

### 98) Конфигурационный агрегатор и миграция секций

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/core/ConfigManager.kt`

#### Что покрыто обзором
- Модель managed config files, миграция legacy секций и объединение конфигов в merged snapshot.
- Синхронизация runtime-кеша (`stringToNumericIds`) с изменениями конфигурации.
- Риски связности между migration/merge/runtime cache и потенциал выделения специализированных компонентов.

### 99) Permission role model

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/core/PermissionManager.kt`

#### Что покрыто обзором
- Построение role-каталога из конфига (display/prefix/rank permissions/permissions).
- Нормализация permission ключей и резолв роли для команды/permission-node.
- UX-ветки permission denied и зависимость от консистентности role-конфига.

### 100) Каталог командного метадата

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/commands/PluginCommandType.kt`

#### Что покрыто обзором
- Централизация id/cooldownKey/permissionNode/managedSystem для команд.
- Специальные случаи (`apply` без permissionNode, alias-like `ahelp` cooldown key).
- Потенциал расширения метадаты для registry-driven command/policy систем.

## Итерация 38 (продолжение)

### 101) Cache primitives для decoration heads

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/decorationheads/cache/LruCache.kt`
- `src/main/kotlin/com/ratger/acreative/menus/decorationheads/cache/SearchIndex.kt`

#### Что покрыто обзором
- Простой synchronized LRU-cache и его использование для query/page/pageSize ключей.
- Паттерн snapshot/sizing/clear для диагностики и reset-сценариев.
- Потенциал обобщения cache-утилит между доменами (search, notify dedup, cooldown index).

### 102) Нормализация поисковых запросов

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/decorationheads/support/SearchQueryNormalizer.kt`

#### Что покрыто обзором
- Минимальная normalize-политика (`trim+lowercase+notBlank`) как precondition для поиска.
- Влияние нормализации на hit-rate кэша и повторяемость UX результатов.
- Точки расширения (locale-aware normalization, transliteration, punctuation policy).

### 103) RecentService lifecycle

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/decorationheads/service/RecentService.kt`

#### Что покрыто обзором
- Асинхронный update/flush lifecycle (executor, dirty set, deferred promotions).
- Персист/кэш консистентность и eviction-флоу при выходе игрока.
- Пограничные сценарии вокруг фоновых задач, flush-порядка и конкурентного доступа.

## Итерация 39 (продолжение)

### 104) Категоризация decoration heads

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/decorationheads/category/CategoryResolver.kt`
- `src/main/kotlin/com/ratger/acreative/menus/decorationheads/category/CategoryRegistry.kt`

#### Что покрыто обзором
- Резолв категорий UI->API ID с предупреждениями по нерешаемым группам.
- Загрузка категорий из конфига и fallback-поведение mode/display/apiNames.
- Потенциал централизованной валидации категорий перед запуском UI-флоу.

### 105) Рендеринг меню decoration heads

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/decorationheads/menu/MenuRenderer.kt`

#### Что покрыто обзором
- Шаблоны рендера для category/recent/saved-pages/editor экранов.
- Повторяемые паттерны base/fill/content slots и wiring большого числа callbacks.
- Точки для выделения template-компонентов без изменения пользовательского UX.

## Итерация 40 (продолжение)

### 106) Sign input bridge для decoration heads

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/decorationheads/support/SignInputService.kt`

#### Что покрыто обзором
- Обвязка `Input.sign()` с unified callback-потоком (`onSubmit`/`onLeave`).
- Логика извлечения «первой осмысленной строки» с отсечением template-значений.
- Потенциал переиспользования input-policy между sign-based сценариями в других меню.

### 107) Временный override кнопок меню

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/decorationheads/support/TemporaryMenuButtonOverrideSupport.kt`

#### Что покрыто обзором
- Шаблон replace-and-restore для кнопок с tick-based откатом.
- Точки риска при повторных override одного слота до истечения таймера.
- Возможности унификации с другими transient UI feedback паттернами.

### 108) Палитра цвета map-preview

#### Обозрённые файлы
- `src/main/kotlin/com/ratger/acreative/menus/decorationheads/support/MapPreviewColorPalette.kt`

#### Что покрыто обзором
- Каталог color options (key/label/legacy tag/hex/color) и циклическая навигация next/previous.
- Нормализация ключей и fallback к ordinary режиму.
- Точки расширения для конфигурируемых палитр и совместимости с legacy форматами цвета.
