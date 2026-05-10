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
