# ТЗ: мультистрановый парсинг объявлений (PL → GE → KZ)

## 1. Цель

Расширить Flatzen с рынка Беларуси на Польшу (MVP), затем Грузию и Казахстан.  
Пользователь ищет **одну страну и один город** за раз. UI-язык независим от рынка.

## 2. Продуктовые решения (зафиксировано)

| Тема                     | Решение                                                                                  |
|--------------------------|------------------------------------------------------------------------------------------|
| Модель приложения        | Мультистрановое                                                                          |
| MVP №1                   | **Польша**                                                                               |
| Города                   | Все крупные города страны                                                                |
| Типы сделок              | Долгосрок / продажа / посуточно / комнаты / коммерция                                    |
| Scope поиска             | 1 страна + 1 город                                                                       |
| Охват сайтов             | Максимальный (включая агрегаторы)                                                        |
| Парсинг                  | С клиента, публичный anonymous traffic                                                   |
| Rate limit / proxy       | Пока не нужно                                                                            |
| Деградация площадки      | Soft-fail (как сейчас)                                                                   |
| Remote config            | Список включённых площадок по стране                                                     |
| Фильтры                  | Общие + адаптация под страну                                                             |
| Выбор страны             | Рядом с выбором города                                                                   |
| Карта / полигоны / метро | Нужны в MVP                                                                              |
| Контакты                 | Обязательны, **если есть в API**                                                         |
| Detail                   | Полный парсинг желателен; если list API достаточно — detail можно не парсить (как Realt) |
| Иконки сайтов            | `https://{host}/favicon.ico`                                                             |

### 2.1 MVP Польша — площадки

1. Otodom.pl
2. OLX.pl
3. Gratka.pl
4. Morizon.pl

Phase 2+: MyHome.ge, Ss.ge, Livo.ge, Binebi.ge, Krisha.kz, OLX.kz, Kn.kz, Nedvizhimost.kz.

### 2.2 Валюты

- BY: как сейчас (USD основной в UI, BYN вторичный).
- PL / GE / KZ: **local price (PLN/GEL/KZT) основной в UI**, USD рядом если есть.

## 3. Архитектура

### 3.1 Пакеты (по странам)

```
shared/data/.../listing/
  core/                 # ListingSource, registry, capabilities
  by/                   # adapters for existing BY repos
  pl/
    otodom/
    olx/
    gratka/
    morizon/
  ge/                   # phase 2
  kz/                   # phase 2
```

### 3.2 ListingSource + Registry

- `ListingSource`: platform, country, capabilities, search(), detail()
- `ListingSourceRegistry` + optional `ListingPlatformConfig` (remote config)
- `MergedRepositoryImpl` оркестрирует fan-out через registry
- Не Dagger `@IntoSet` — Koin list registration

### 3.3 Идентичность

Composite PK `(flatPlatform, adId)` — implemented (Room DB v10).

### 3.4 Иконки

`FlatPlatform.faviconUrl()` → Coil AsyncImage в UI для новых площадок.

## 4–9

См. также ход работ в `current_stage.md` и сырые API-заметки в `tmp/pl/api/` (локально, gitignored).
