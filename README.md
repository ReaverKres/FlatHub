# FlatZen

Клиентское приложение на **Kotlin Multiplatform** для **Android** и **iOS**: агрегатор объявлений о
недвижимости. Данные подтягиваются с нескольких площадок (в том числе Kufar, Onliner, Realt.by,
Domovita, Flathub), интерфейс и бизнес-логика общие для платформ.

Проект изначально опирался
на [официальный шаблон KMP с Compose Multiplatform](https://github.com/Kotlin/KMP-App-Template);
текущая кодовая база существенно переработана под задачи FlatZen.

## Возможности

- **Главная** — поиск и просмотр объявлений с учётом фильтров.
- **Избранное** — сохранённые объявления.
- **Карта** — отображение объектов на карте (MapCompose MP).
- **Ещё** — дополнительные разделы (FAQ, реферальная программа, настройки и др.).
- **Локация** — выбор города, района, станции метро для уточнения выдачи.
- **Уведомления** — интеграция с push (Firebase на Android, общий слой через KMP Notifier).
- **Deep links** — переходы по внешним ссылкам в нужные экраны приложения.

## Структура модулей

| Модуль                    | Назначение                                                                 |
|---------------------------|----------------------------------------------------------------------------|
| `composeApp`              | Точка входа, Compose UI, навигация (Navigation 3), DI на уровне приложения |
| `shared:commoncomponents` | Общие компоненты, сущности, сеть (мониторинг соединения и т.п.)            |
| `shared:data`             | Ktor, Ktorfit API, Room, парсинг/модели данных                             |
| `shared:domain`           | Use case'ы и доменный слой                                                 |
| `shared:presentation`     | ViewModel/контейнеры FlowMVI, состояние экранов                            |

## Технологии

- [Compose Multiplatform](https://jb.gg/compose) и Material 3 — общий UI
- [Navigation 3](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-navigation-routing.html) —
  маршрутизация
- [Ktor](https://ktor.io/) + [Ktorfit](https://github.com/Foso/Ktorfit) — HTTP и описание API
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) — JSON
- [Room](https://developer.android.com/kotlin/multiplatform/room) — локальное хранение (KMP)
- [Koin](https://insert-koin.io/) — внедрение зависимостей
- [FlowMVI](https://github.com/respawn-app/FlowMVI) — реализация MVI
- [Coil](https://github.com/coil-kt/coil) — загрузка изображений
- [moko-permissions](https://github.com/icerockdev/moko-permissions) — разрешения
- **Android:** Firebase (Messaging и др.), Yandex AppMetrica (аналитика)

Список библиотек не является исчерпывающим; альтернативы для KMP можно смотреть
в [kmp-awesome](https://github.com/terrakok/kmp-awesome).

## Сборка

Требуются JDK 11+, Android SDK для сборки под Android, Xcode — для iOS.

```bash
# Android (debug)
./gradlew :composeApp:assembleDebug

# iOS: откройте `iosApp/iosApp.xcodeproj` в Xcode и соберите схему под нужное устройство или симулятор
```

Для release-сборки Android настроены подпись и обфускация (R8).
