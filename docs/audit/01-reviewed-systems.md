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
