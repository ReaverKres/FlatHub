# Техническое описание: миграция Presentation слоя на FlowMVI

## 1. Обзор

Документ описывает план переноса presentation слоя проекта Flathub с кастомного `BaseMviViewModel` на библиотеку [FlowMVI](https://github.com/respawn-app/FlowMVI).

---

## 2. Сравнение архитектур

### 2.1 Текущая архитектура (Flathub BaseMviViewModel)

```
┌─────────────┐     handleIntent      ┌──────────────┐     reduce      ┌────────┐
│   Action    │ ───────────────────►  │ Flow<Event>  │ ─────────────► │ State  │
└─────────────┘                       └──────────────┘                 └────────┘
                                             │
                                             │ onEvent (optional)
                                             ▼
                                      ┌──────────────┐
                                      │   Effect     │  (one-time side effects)
                                      └──────────────┘
```

**Компоненты:**
- **MviAction** — действия пользователя (входные события)
- **MviEvent** — внутренние события, результат обработки Action
- **MviState** — текущее состояние экрана
- **MviEffect** — одноразовые side effects (навигация, snackbar и т.д.)

**Особенности:**
- `handleIntent(action, state): Flow<Event>` — асинхронная обработка, может эмитить несколько Event
- `reduce(event, state): State` — чистая функция перехода состояния
- `onEvent(event): Effect?` — опциональная генерация Effect из Event
- Наследование от `ViewModel`, привязка к Android lifecycle

### 2.2 Целевая архитектура (FlowMVI)

```
┌─────────────┐     reduce (PipelineContext)     ┌────────┐
│   Intent    │ ─────────────────────────────►  │ State  │
└─────────────┘     updateState { }              └────────┘
       │
       │ action()
       ▼
┌─────────────┐
│   Action    │  (one-time side effects)
└─────────────┘
```

**Компоненты:**
- **Intent (MVIIntent)** — действия пользователя
- **State (MVIState)** — иммутабельное состояние
- **Action (MVIAction)** — одноразовые side effects

**Особенности:**
- Прямая обработка: Intent → reduce → updateState / action
- Нет промежуточного слоя Event
- `store { }` DSL с плагинами: `init`, `reduce`, `whileSubscribed`, `recover`
- `Container` вместо ViewModel — KMP-совместимость, не привязан к Android

---

## 3. Маппинг концепций

| Flathub (BaseMviViewModel) | FlowMVI |
|----------------------------|---------|
| MviAction                  | Intent (MVIIntent) |
| MviState                   | State (MVIState) |
| MviEvent                   | **Устраняется** — логика переносится в reduce |
| MviEffect                  | Action (MVIAction) |
| ViewModel                  | Container |
| handleIntent()             | reduce { } |
| reduce()                   | updateState { } внутри reduce |
| onEvent()                  | action() внутри reduce |
| init { flow.collect }      | whileSubscribed { flow.collect } |

---

## 4. Рекомендация по BaseMviViewModel

**От BaseMviViewModel можно и нужно отказаться.**

Причины:
1. FlowMVI предоставляет `Container` и `store` DSL — базовый класс не нужен
2. FlowMVI не требует наследования, вся логика описывается в `store { }`
3. Отказ от базового класса упрощает код и убирает лишнюю абстракцию

---

## 5. Миграция FilterViewModel (выполнено)

Статус на текущем этапе:

- `FilterViewModel` заменён на `FilterContainer` (FlowMVI `Container` + `store`).
- Подписка на `cashedFilterFlow` используется только для синхронизации state фильтра.
- One-shot сетевой reload вынесен в отдельный `forceReloadFlow` (без replay), чтобы исключить
  повторные network calls при восстановлении подписки.

### 5.1 Структура файлов после миграции

```
viewmodel/filter/
├── FilterContainer.kt      # FlowMVI Container (бывший FilterViewModel)
├── FilterState.kt          # State, Intent, Action
└── (существующие) FilterState.kt, мапперы и т.д.
```

### 5.2 Переименования

| Было                    | Стало                    |
|-------------------------|--------------------------|
| FilterScreenAction      | FilterIntent             |
| FilterScreenState       | FilterState              |
| FilterScreenEvent       | **Устраняется**          |
| FilterEffect            | FilterAction             |
| FilterViewModel         | FilterContainer          |

### 5.3 Миграция init-блока

import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce

**Было:**
```kotlin
init {
    filterRepository.cashedFilterFlow.onEach { newFilters ->
        val filterState = mapFilterModelToFilterState(newFilters.commonFilterRequestModel)
        onIntent(FilterScreenAction.UpdateFilter(filterState, false))
    }.launchIn(viewModelScope)

    filterRepository.getAllSavedFilters().onEach { savedFilters ->
        onIntent(FilterScreenAction.LoadSavedFilters)
    }.launchIn(viewModelScope)
}
```

**Стало (вариант A — прямой updateState):**
```kotlin
store(FilterState.Initial) {
    whileSubscribed {
        filterRepository.cashedFilterFlow.collect { newFilters ->
            val filterState = mapFilterModelToFilterState(newFilters.commonFilterRequestModel)
            applyFiltersUpdate(filterState, false)
        }
    }
    whileSubscribed {
        filterRepository.getAllSavedFilters().collect { savedFilters ->
            val savedFilterStates = savedFilters.map { mapSavedFilterToSavedFilterState(it) }
            updateState { copy(savedFilters = savedFilterStates) }
        }
    }
    // ...
}

private suspend fun PipeCtx.applyFiltersUpdate(newFilterState: FilterState, doNetworkCall: Boolean) {
    // Общая логика: проверка selected filter, updateState, filterRepository.updateFilter
}
```

**Стало (вариант B — через self-intent, если store поддерживает):**
```kotlin
whileSubscribed {
    filterRepository.cashedFilterFlow.collect { newFilters ->
        store.intent(FilterIntent.UpdateFilter(
            mapFilterModelToFilterState(newFilters.commonFilterRequestModel),
            false
        ))
    }
}
```

**Рекомендация:** Вариант A — вынести общую логику в `applyFiltersUpdate()` и вызывать её и из `reduce` (для user intents), и из `whileSubscribed` (для cache updates). FlowMVI не предоставляет встроенный способ эмитить intent изнутри store, поэтому прямой `updateState` + общий helper — предпочтительный подход.

### 5.4 Миграция handleIntent → reduce

Каждый case из `handleIntent` превращается в ветку `when` в `reduce`:

| Intent case | Логика | В FlowMVI |
|-------------|--------|-----------|
| UpdateFilter | Эмит FiltersUpdated, возможно SavedFilterSelectionUpdated | updateState + вызов filterRepository.updateFilter |
| UpdateSelectedCommercialPropertyType | FiltersUpdated | updateState |
| UpdateMetroFilter | FiltersUpdated | updateState |
| UpdateDistrictFilter | FiltersUpdated | updateState |
| UpdateAddressFilter | FiltersUpdated | updateState |
| UpdateSortOption | FiltersUpdated | updateState |
| ClearAllFilters | FiltersUpdated(FilterState()) | updateState |
| ClearMetroFilters | FiltersUpdated | updateState |
| ClearLocationFilters | FiltersUpdated | updateState |
| ShowSaveFilterDialog | SaveDialogStateUpdated | updateState |
| HideSaveFilterDialog | SaveDialogStateUpdated | updateState |
| UpdateFilterName | SaveDialogStateUpdated | updateState |
| NotificationEnable | SaveDialogStateUpdated или NavigateToReferralScreen | updateState или action(NavigateToReferral) |
| SaveFilter | FilterSaved + SaveDialogStateUpdated | filterRepository.saveFilter + updateState |
| LoadSavedFilters | SavedFiltersLoaded | filterRepository.getAllSavedFilters().first() + updateState |
| DeleteSavedFilter | FilterDeleted | filterRepository.deleteSavedFilter + updateState |
| ToggleSavedFilterSelection | SavedFilterSelectionUpdated | filterRepository + updateState |
| TrackScreenView | analyticsManager.registerEvent | launch { analyticsManager... } (fire-and-forget) |
| ActivateMapArea | FiltersUpdated + SavedAreasDialogStateUpdated | updateState (2 раза или один copy) |
| DeleteMapArea | FiltersUpdated + SavedAreasDialogStateUpdated | updateState |
| ShowSavedAreaListDialog | SavedAreasDialogStateUpdated | updateState |
| HideSavedAreaListDialog | SavedAreasDialogStateUpdated | updateState |

### 5.5 Миграция reduce(event) → updateState

В текущей реализации `reduce` принимает Event и возвращает новый State. В FlowMVI вместо этого вызывается `updateState { copy(...) }` внутри соответствующей ветки reduce.

**Особый случай — FiltersUpdated с вызовом repository:**
```kotlin
// Было в reduce:
is FiltersUpdated -> {
    currentState.copy(filters = event.newFilterState).also {
        val filterModel = mapFilterStateToFilterModel(it.filters)
        if (filterModel != filterRepository.lastFilter()) {
            filterRepository.updateFilter(filterModel, event.doNetworkCall)
        }
    }
}

// Стало в reduce:
is FilterIntent.UpdateFilter -> {
    val newFilterState = intent.newFilterState
    val currency = if (state.filters.adType == AdType.DAILY) Currency.BYR else Currency.USD
    val updatedFilter = newFilterState.copy(currency = currency)
    
    // Check selected saved filter
    val selectedFilter = state.savedFilters.find { it.selected }
    selectedFilter?.let { selected ->
        val selectedFilterData = filterRepository.getSavedFilterById(selected.id)?.filterData
        val currentFilterData = mapFilterStateToFilterModel(updatedFilter)
        if (selectedFilterData != currentFilterData) {
            filterRepository.clearAllSavedFilterSelections()
            updateState { copy(filters = updatedFilter, savedFilters = ...) }
        }
    } ?: updateState { copy(filters = updatedFilter) }
    
    val filterModel = mapFilterStateToFilterModel(updatedFilter)
    if (filterModel != filterRepository.lastFilter()) {
        filterRepository.updateFilter(filterModel, intent.doNetworkCall)
    }
}
```

### 5.6 Миграция onEvent → action()

**Было:**
```kotlin
override suspend fun onEvent(event: FilterScreenEvent): MviEffect? {
    return when (event) {
        is FilterScreenEvent.NavigateToReferralScreen -> FilterEffect.NavigateToReferralEffect
        else -> super.onEvent(event)
    }
}
```

**Стало:** В ветке `FilterIntent.NotificationEnable` при условии перехода на referral:
```kotlin
is FilterIntent.NotificationEnable -> {
    if (userPreferencesRepository.getUserPreferences().firstOrNull()
            ?.deviceDocumentResponse?.referralStats?.isNotificationAvailable == true) {
        updateState { copy(saveDialogState = saveDialogState.copy(isNotificationEnabled = intent.enabled)) }
    } else {
        action(FilterAction.NavigateToReferralScreen)
    }
}
```

### 5.7 Analytics (TrackScreenView)

**Вариант 1 — в reduce:**
```kotlin
is FilterIntent.TrackScreenView -> {
    launch {
        analyticsManager.registerEvent(
            AnalyticsEvent(
                eventName = AppMetrcica.Events.SCREEN_VIEW,
                parameters = mapOf(...) + intent.parameters
            )
        )
    }
    // ничего не обновляем
}
```

**Вариант 2 — FlowMVI plugin (рекомендуется для глобального трекинга):**
```kotlin
fun analyticsPlugin(analytics: AnalyticsManager) = plugin {
    onIntent { intent ->
        if (intent is FilterIntent.TrackScreenView) {
            analytics.registerEvent(...)
        }
    }
}
```

---

## 6. Шаблон FilterContainer после миграции

```kotlin
private typealias PipeCtx = PipelineContext<FilterState, FilterIntent, FilterAction>

class FilterContainer(
    private val filterRepository: FilterRepository,
    private val userMapAreaRepository: UserMapAreaRepository,
    private val analyticsManager: AnalyticsManager,
    private val userPreferencesRepository: UserPreferencesRepository
) : Container<FilterState, FilterIntent, FilterAction> {

    override val store: Store<FilterState, FilterIntent, FilterAction> =
        store(FilterState.Initial) {
            whileSubscribed {
                filterRepository.cashedFilterFlow.collect { newFilters ->
                    intent(FilterIntent.UpdateFilter(
                        mapFilterModelToFilterState(newFilters.commonFilterRequestModel),
                        false
                    ))
                }
            }
            whileSubscribed {
                filterRepository.getAllSavedFilters().collect {
                    intent(FilterIntent.LoadSavedFilters)
                }
            }

            reduce { intent ->
                when (intent) {
                    is FilterIntent.UpdateFilter -> handleUpdateFilter(intent)
                    is FilterIntent.UpdateSelectedCommercialPropertyType -> { ... }
                    // ... остальные cases
                    is FilterIntent.TrackScreenView -> { launch { analyticsManager.registerEvent(...) } }
                }
            }
        }

    private suspend fun PipeCtx.handleUpdateFilter(intent: FilterIntent.UpdateFilter) {
        // Логика из handleIntent + reduce
    }
}
```

---

## 7. DI (Koin) — создание Container

### 7.1 Регистрация в модуле

FlowMVI Container регистрируется через расширение `Module.container()`, которое под капотом использует `viewModel` и оборачивает Container в `ContainerViewModel`:

```kotlin
// KoinFlowMVI.kt
@FlowMVIDSL
inline fun <reified T : Container<S, I, A>, S : MVIState, I : MVIIntent, A : MVIAction> Module.container(
    crossinline definition: Definition<T>,
) = viewModel(qualifier<T>()) { params ->
    ContainerViewModel<T, _, _, _>(container = definition(params))
}
```

**Примеры регистрации:**

```kotlin
val containersModule = module {
    // Container без параметров
    container { new(::WalletContainer) }
    container { new(::AuthContainer) }

    // Container с зависимостями из Koin (get)
    container { new(::FilterContainer) }

    // Container с параметрами, передаваемыми при вызове из UI
    container { new(::HomeContainer) }      // kHealth передаётся через parametersOf
    container { new(::AllWorkoutsContainer) }
    container { new(::DuelContainer) }      // opponentId передаётся через parametersOf
    container { new(::DuelDetailContainer) } // duelId передаётся через parametersOf
}
```

**Порядок параметров:** Первые N параметров конструктора берутся из `parametersOf(...)`, остальные — из `get()`.

### 7.2 ContainerViewModel

`ContainerViewModel` — обёртка над Container для интеграции с Android ViewModel:

- Сохраняет Container при смене конфигурации (rotation, theme)
- Автоматически запускает Store при создании: `store.start(viewModelScope)`
- Делегирует `Store` и `Container` интерфейсы к внутреннему container



```kotlin
class ContainerViewModel<T : Container<S, I, A>, ...>(
    public val container: T,
    start: Boolean = true,
) : ViewModel(), Store<S, I, A> by container.store, Container<S, I, A> by container {
    init {
        if (start) addCloseable(store.start(viewModelScope))
    }
}
```

import org.koin.core.definition.Definition
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.qualifier
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.FlowMVIDSL
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

/**
* Registers FlowMVI Container in Koin module as ViewModel.
* Container will be preserved across configuration changes.
*
* Usage:
* ```kotlin
* val authModule = module {
*     container { AuthContainer() }
* }
* ```
*
* @param definition Factory function for creating Container
  */
  @FlowMVIDSL
  inline fun <reified T : Container<S, I, A>, S : MVIState, I : MVIIntent, A : MVIAction> Module.container(
  crossinline definition: Definition<T>,
  ) = viewModel(qualifier<T>()) { params ->
  ContainerViewModel<T, _, _, _>(container = definition(params))
  }

### 7.3 Composable `container()` — получение из UI

```kotlin
@Composable
inline fun <reified T : Container<S, I, A>, ...> container(
    key: String? = null,
    scope: Scope = currentKoinScope(),
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current),
    extras: CreationExtras = defaultExtras(viewModelStoreOwner),
    noinline params: ParametersDefinition? = null,
): T = koinViewModel<ContainerViewModel<T, S, I, A>>(
    qualifier = qualifier<T>(),
    parameters = params,  // ← сюда передаются parametersOf(...)
    key = key,
    scope = scope,
    viewModelStoreOwner = viewModelStoreOwner,
    extras = extras
).container
```

**Важно:** `container()` привязан к `ViewModelStoreOwner` (обычно экран/навигационный граф). При уходе с экрана Container уничтожается.

### 7.4 Передача параметров из UI

```kotlin
// Без параметров
val container: WalletContainer = container()

// С одним параметром (например, KHealth из CompositionLocal)
val container: AllWorkoutsContainer = container { parametersOf(kHealth) }

// С параметром навигации (id дуэлянта, id дуэля)
val container: DuelContainer = container { parametersOf(opponentId) }
val container: DuelDetailContainer = container { parametersOf(duelId) }

// С несколькими параметрами
val container: SomeContainer = container { parametersOf(param1, param2) }
```

### 7.5 Key для нескольких экземпляров на одном экране

Если на одном экране нужны несколько Container одного типа (например, разные вкладки с разными параметрами):

```kotlin
val container1 = container(key = "tab1") { parametersOf(id1) }
val container2 = container(key = "tab2") { parametersOf(id2) }
```

---

## 8. Подписка в UI (Compose)

### 8.1 Базовый паттерн

**Импорты:**
```kotlin
import org.koin.core.parameter.parametersOf
import pro.respawn.flowmvi.compose.dsl.subscribe
import pro.respawn.flowmvi.dsl.intent
import ru.x5.x5run.di.container  // или ваш пакет
```

**Код:**
```kotlin
@Composable
fun FilterScreen(
    onNavigateToReferral: () -> Unit
) {
    val container: FilterContainer = container()
    val state by container.store.subscribe { action ->
        when (action) {
            is FilterAction.NavigateToReferralScreen -> onNavigateToReferral()
        }
    }

    LaunchedEffectOnce(Unit) {
        container.intent(FilterIntent.LoadData)
    }

    FilterContent(
        state = state,
        onIntent = container::intent
    )
}
```

### 8.2 `container.store.subscribe`

`subscribe` — Composable-функция из `pro.respawn.flowmvi.compose.dsl.subscribe`:

- Подписывается на State и Action Store
- Привязана к lifecycle Composable (отмена при dispose)
- Возвращает `State` через `by` (делегат)
- Колбэк `{ action -> }` вызывается при каждом эмите Action (side effects)

```kotlin
val state by container.store.subscribe { action ->
    // Обработка одноразовых событий: навигация, snackbar, диалоги
    when (action) {
        is FilterAction.NavigateToReferralScreen -> onNavigateToReferral()
        is FilterAction.ShowSnackbar -> { /* show snackbar */ }
    }
}
```

### 8.3 Варианты подписки

**С обработкой Actions:**
```kotlin
val state by container.store.subscribe { action ->
    when (action) {
        is HomeAction.NavigateToWorkout -> onNavigateToWorkout(action.id)
        is HomeAction.NavigateToDuelDetail -> onNavigateToDuelDetail(action.duelId)
        is HomeAction.ShowRewardDialog -> onRewardDialogShown(action.isVisible)
    }
}
```

**Без Actions (если side effects не нужны):**
```kotlin
val state by container.store.subscribe { }
// или
val state by container.store.subscribe()
```

### 8.4 Отправка Intent

```kotlin
// Прямой вызов
container.intent(FilterIntent.UpdateFilter(newState, doNetworkCall = true))

// Передача как callback в дочерние Composable
FilterContent(
    onIntent = container::intent
)

// В LaunchedEffect для начальной загрузки
LaunchedEffectOnce(Unit) {
    container.intent(AllWorkoutsIntent.CheckHealthPermission)
    container.intent(AllWorkoutsIntent.LoadData)
}
```

### 8.5 Полный пример экрана с параметрами

```kotlin
@Composable
fun AllWorkoutsScreen(onBack: () -> Unit) {
    val kHealth = LocalKHealth.current
    val container: AllWorkoutsContainer = container { parametersOf(kHealth) }

    val state by container.store.subscribe { action ->
        // Handle actions if needed
    }

    LaunchedEffectOnce(Unit) {
        container.intent(AllWorkoutsIntent.CheckHealthPermission)
        container.intent(AllWorkoutsIntent.LoadData)
    }

    Scaffold(topBar = { ... }) { innerPadding ->
        AllWorkoutsContent(
            modifier = Modifier.padding(innerPadding),
            state = state,
            onIntent = container::intent
        )
    }
}
```

### 8.6 Миграция с ViewModel

**Было (ViewModel):**
```kotlin
val container: FilterContainer = container()
val state by container.store.subscribe { action -> /* handle actions */ }
```

**Стало (FlowMVI):**
```kotlin
val container: FilterContainer = container()
val state by container.store.subscribe { action -> /* effect handling */ }
```

---

## 9. Чек-лист миграции одного экрана

- [ ] Добавить зависимость FlowMVI в `libs.versions.toml` и `build.gradle.kts`
- [ ] Переименовать Action → Intent, Effect → Action
- [ ] Удалить sealed interface Event
- [ ] Создать FilterContainer с `store { }`
- [ ] Перенести `init` логику в `whileSubscribed`
- [ ] Перенести `handleIntent` в `reduce`, заменив Event на прямые `updateState` / `action`
- [ ] там где в Intent обрабатывется набор текста для обновления state используй `updateStateImmediate`
- [ ] Удалить `reduce(event, state)` — логика в reduce
- [ ] Удалить `onEvent` — side effects через `action()`
- [ ] **DI:** Зарегистрировать в модуле: `container { new(::FilterContainer) }` или с `get()`
- [ ] **UI:** Получить container: `container()` или `container { parametersOf(...) }`
- [ ] **UI:** Подписаться: `val state by container.store.subscribe { action -> ... }`
- [ ] **UI:** Отправлять intent: `container.intent(...)`, передать `container::intent` в дочерние
- [ ] Удалить BaseMviViewModel (если больше не используется)
- [ ] Удалить старые MviAction, MviEvent, MviEffect интерфейсы (если были общие)

---

## 10. Порядок миграции экранов

Рекомендуемый порядок (от простых к сложным):

1. Простые экраны без flow-подписок (например, экран с одной кнопкой)
2. Экраны с `init { loadData() }`
3. Экраны с подписками на Flow (`whileSubscribed`)
4. FlatSearchViewModel — самый сложный, много Intent и связь с repository

---

## 11. Ссылки

- [FlowMVI GitHub](https://github.com/respawn-app/FlowMVI)
- [FlowMVI Quickstart](https://opensource.respawn.pro/FlowMVI/quickstart)
- [FlowMVI API Docs](https://opensource.respawn.pro/FlowMVI/api/)
- Правила проекта: `.cursor/rules/kmprules.mdc`
