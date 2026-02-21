# Техническое описание: миграция Navigation2 → Navigation3 с Multiple Backstack

## 1. Обзор

Документ описывает миграцию навигации с **Navigation 2** (Compose Navigation) на **Navigation 3** (JetBrains Navigation3) с поддержкой **multiple backstack** для bottom tabs. Централизованная обработка навигации через `Navigator.kt` и `NavigationState.kt`.

---

## 2. Сравнение Navigation2 и Navigation3

### 2.1 Navigation 2 (Compose Navigation)

- **NavController** — закрытый API, внутреннее управление back stack
- **NavHost** — единый граф навигации
- **Bottom navigation** — один общий back stack, при переключении табов теряется история
- **NavBackStackEntry** — доступ к аргументам через `savedStateHandle`

### 2.2 Navigation 3

- **User-owned back stack** — вы управляете `SnapshotStateList` / `NavBackStack`, UI наблюдает напрямую
- **Низкоуровневые блоки** — `rememberNavBackStack`, `NavDisplay`, `entryProvider`
- **Multiple backstacks** — отдельный back stack на каждый tab, история сохраняется при переключении
- **KMP** — полная поддержка Android, iOS, Desktop, Web (с polymorphic serialization)

---

## 3. Архитектура: Navigator и NavigationState

### 3.1 Назначение компонентов

| Компонент | Назначение |
|-----------|------------|
| **NavigationState** | Хранит текущий tab (`topLevelRoute`) и map back stacks по табам |
| **Navigator** | Единая точка навигации: `navigate`, `goBack`, `replaceCurrent` |
| **rememberNavigationState** | Composable для создания state с сохранением при process death |

### 3.2 Схема Multiple Backstack

```
┌─────────────────────────────────────────────────────────────────┐
│                     NavigationState                               │
│  topLevelRoute: NavKey (текущий tab)                              │
│  backStacks: Map<NavKey, NavBackStack<NavKey>>                   │
│    ├── RouteMain      → [RouteMain]                              │
│    ├── RouteStatistics → [RouteStatistics, RouteAllWorkouts]     │
│    ├── RouteAchievements → [RouteAchievements]                    │
│    ├── RouteFriends   → [RouteFriends, RouteDuel(friendId), ...] │
│    └── RouteProfile   → [RouteProfile]                           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Navigator(state)                             │
│  navigate(route)   — переключить tab или добавить в стек         │
│  goBack()          — назад в стеке или на предыдущий tab         │
│  replaceCurrent(r) — заменить текущий экран                      │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. NavigationState.kt

### 4.1 Структура

```kotlin
class NavigationState(
    val startRoute: NavKey,           // Начальный tab (RouteMain)
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>
) {
    var topLevelRoute: NavKey by topLevelRoute

    /** Какие стеки отображать: startRoute + текущий tab (для анимации) */
    val stacksInUse: List<NavKey>
        get() = if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }
}
```

### 4.2 rememberNavigationState

Создаёт state с:
- **rememberSerializable** для `topLevelRoute` — сохранение при process death
- **rememberNavBackStack** для каждого tab — отдельный back stack с начальным route

```kotlin
@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>
): NavigationState
```

### 4.3 toEntries

Преобразует `backStacks` в плоский список `NavEntry` для `NavDisplay`:
- Применяет decorators (SaveableStateHolder, ViewModelStore)
- Объединяет entries из `stacksInUse` (startRoute + current tab)
- Возвращает `SnapshotStateList` для реактивного обновления UI

---

## 5. Navigator.kt

### 5.1 API

```kotlin
class Navigator(val state: NavigationState) {

    /** Перейти на route: переключить tab или добавить в текущий стек */
    fun navigate(route: NavKey)

    /** Назад: в стеке или на startRoute */
    fun goBack()

    /** Заменить текущий экран (без добавления в стек) */
    fun replaceCurrent(route: NavKey)
}
```

### 5.2 Логика navigate(route)

```kotlin
fun navigate(route: NavKey) {
    if (route in state.backStacks.keys) {
        // Route — top-level tab, переключаем tab
        state.topLevelRoute = route
    } else {
        // Route — вложенный экран, добавляем в стек текущего tab
        state.backStacks[state.topLevelRoute]?.add(route)
    }
}
```

### 5.3 Логика goBack()

```kotlin
fun goBack() {
    val currentStack = state.backStacks[state.topLevelRoute]
    val currentRoute = currentStack?.last()

    if (currentRoute == state.topLevelRoute) {
        // На корне таба — переключаем на startRoute (Home)
        state.topLevelRoute = state.startRoute
    } else {
        // Есть экраны в стеке — убираем последний
        currentStack?.removeLastOrNull()
    }
}
```

### 5.4 Логика replaceCurrent(route)

```kotlin
fun replaceCurrent(route: NavKey) {
    val currentStack = state.backStacks[state.topLevelRoute]
    currentStack?.removeLastOrNull()
    currentStack?.add(route)
}
```

Используется, например, при создании дуэля: DuelScreen → replaceCurrent(DuelDetail) вместо Duel → DuelDetail в стеке.

---

## 6. Интеграция в MainGraphNavigation

### 6.1 Инициализация

```kotlin
val topLevelRoutes = remember { BottomTab.entries.map { it.route }.toSet() }
val navigationState = rememberNavigationState(
    startRoute = MainGraph.RouteMain,
    topLevelRoutes = topLevelRoutes
)
val navigator = remember(navigationState) { Navigator(navigationState) }
```

### 6.2 Передача Navigator в секции

```kotlin
val provider: (NavKey) -> NavEntry<NavKey> = entryProvider {
    mainTabsSection(
        onNavigateToDuel = { friendId, activeDuelId ->
            if (activeDuelId != null) {
                navigator.navigate(FriendsGraph.RouteDuelDetail(activeDuelId))
            } else {
                navigator.navigate(FriendsGraph.RouteDuel(friendId))
            }
        },
        onNavigateToDuelDetail = { navigator.navigate(FriendsGraph.RouteDuelDetail(it)) },
        onNavigateToWallet = { navigator.navigate(WalletGraph) },
        onNavigateToAllWorkouts = { navigator.navigate(StatisticsGraph.RouteAllWorkouts) },
        // ...
    )
    friendsSection(onBack = { navigator.goBack() }, onChallengeSent = { duelId ->
        navigator.replaceCurrent(FriendsGraph.RouteDuelDetail(duelId))
    })
    statisticsSection(onBack = { navigator.goBack() })
    walletSection(onBack = { navigator.goBack() })
}
```

### 6.3 Отображение и Back

```kotlin
val entries = navigationState.toEntries(provider)

AppNavDisplay(
    entries = entries,
    onBack = { navigator.goBack() }
)
```
```kotlin
        AppNavDisplay(
            backStack = rootBackStack,
            modifier = modifier,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = entryProvider {
                entry<Route.Splash> {
                    val container: SplashScreenContainer = container()
                    container.store.subscribe { action ->
                        when (action) {
                            is SplashScreenAction.NavigateToHome -> {
                                rootBackStack.remove(Route.Splash)
                                rootBackStack.add(Route.MainGraph)
                            }

                            is SplashScreenAction.NavigateToAuth -> {
                                rootBackStack.remove(Route.Splash)
                                rootBackStack.add(Route.Auth)
                            }
                        }
                    }
                }
                entry<Route.Auth> {
                    AuthNavigation(
                        onAuthSuccess = {
                            rootBackStack.remove(Route.Auth)
                            rootBackStack.add(Route.MainGraph)
                        }
                    )
                }
                entry<Route.MainGraph> {
                    MainGraphNavigation(
                        onNavigateToAuth = {
                            rootBackStack.remove(Route.MainGraph)
                            rootBackStack.add(Route.Auth)
                        }
                    )
                }
            })
```

### 6.4 Bottom Bar — переключение табов

```kotlin
val currentStack = navigationState.backStacks[currentTopLevelRoute]
val isRootRoute = currentStack?.lastOrNull() == currentTopLevelRoute

if (BottomTab.entries.any { it.route == currentTopLevelRoute } && isRootRoute) {
    BottomBar(
        currentRoute = currentTopLevelRoute as? Route,
        onNavigate = { route ->
            if (currentTopLevelRoute != route) {
                navigator.navigate(route)
            }
        }
    )
}
```

Bottom bar показывается только на корневых экранах табов.

---

## 7. Иерархия навигации в проекте

```
AppNavigation (root)
├── rememberNavBackStack(Splash)
├── entry<Splash> → SplashScreen
├── entry<Auth> → AuthNavigation (свой backStack)
├── entry<MainGraph> → MainGraphNavigation
    │
    ├── rememberNavigationState + Navigator
    ├── entryProvider: mainTabsSection, friendsSection, statisticsSection, walletSection
    ├── AppNavDisplay(entries, onBack)
    └── BottomBar (на корневых экранах)
```

**Root level** (Splash, Auth, MainGraph) — один back stack, ручное `add`/`remove`.

**MainGraph** — multiple backstack через Navigator + NavigationState.

**AuthNavigation, WalletNavigation** — вложенные графы с `rememberNavBackStack`, прямой доступ к `backStack.add`/`removeLast`.

---

## 8. Миграция с Navigation2

### 8.1 Зависимости

**Удалить (если не используется):**
```kotlin
// navigation-compose (Navigation 2)
implementation(libs.navigation.compose)
```

**Добавить:**
```toml
[versions]
multiplatform-nav3-ui = "1.0.0-alpha06"
compose-multiplatform-lifecycle = "2.10.0-alpha07"

[libraries]
jetbrains-navigation3-ui = { module = "org.jetbrains.androidx.navigation3:navigation3-ui", version.ref = "multiplatform-nav3-ui" }
jetbrains-lifecycle-viewmodelNavigation3 = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "compose-multiplatform-lifecycle" }
```

### 8.2 Маппинг концепций

| Navigation 2 | Navigation 3 |
|-------------|--------------|
| NavController | Navigator + NavigationState |
| NavHost | AppNavDisplay + entryProvider |
| NavBackStackEntry | NavEntry + NavKey |
| navController.navigate(route) | navigator.navigate(route) |
| navController.popBackStack() | navigator.goBack() |
| NavGraphBuilder.composable | entryProvider { entry<Route> { } } |

### 8.3 Polymorphic Serialization (KMP)

Navigation 3 для iOS/Desktop требует явной сериализации `NavKey`:

```kotlin
// NavigationConfig.kt
val config = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Route.MainGraph.RouteMain::class, ...)
            subclass(Route.FriendsGraph.RouteDuel::class, ...)
            // все Route
        }
    }
}
```

Используется в `rememberNavBackStack(config, initialRoute)` и `rememberSerializable`.

### 8.4 Передача параметров в Route

**Navigation 2:**
```kotlin
navController.navigate("duel/$opponentId")
// или
navController.navigate(DuelRoute(opponentId))
```

**Navigation 3:**
```kotlin
@Serializable
data class RouteDuel(val friendId: String) : Route

navigator.navigate(FriendsGraph.RouteDuel(friendId))
```

Параметры — часть типа Route, type-safe.

---

## 9. Decorators

Для корректной работы ViewModel и SaveableState с Navigation 3:

```kotlin
val decorators = listOf(
    rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
    rememberViewModelStoreNavEntryDecorator()
)
```

- **SaveableStateHolder** — сохранение состояния Composable при смене экрана
- **ViewModelStore** — привязка ViewModel/Container к NavEntry (уничтожение при pop)

---

## 10. Чек-лист миграции

- [ ] Добавить зависимости Navigation3 и lifecycle-viewmodel-navigation3
- [ ] Создать `NavigationConfig.kt` с polymorphic serialization для всех Route
- [ ] Создать `NavigationState.kt` с `rememberNavigationState` и `toEntries`
- [ ] Создать `Navigator.kt` с `navigate`, `goBack`, `replaceCurrent`
- [ ] Определить `Routes.kt` — все Route как `@Serializable` sealed interface
- [ ] Заменить NavHost на `entryProvider` + `AppNavDisplay`
- [ ] Пробросить `navigator` / callbacks в экраны вместо NavController
- [ ] Реализовать multiple backstack для bottom tabs через NavigationState
- [ ] Добавить decorators (SaveableStateHolder, ViewModelStore) в toEntries
- [ ] Удалить navigation-compose (Navigation 2) при полной миграции

---

## 11. Ссылки

- [Navigation 3 in Compose Multiplatform (Kotlin Docs)](https://kotlinlang.org/docs/multiplatform/compose-navigation-3.html)
- [Navigation 3 Overview (Android)](https://developer.android.com/guide/navigation/navigation-3)
- [Migration from Navigation 2 to 3](https://developer.android.com/guide/navigation/navigation-3/migration-guide)
- [nav3-recipes (KMP examples)](https://github.com/terrakok/nav3-recipes)
