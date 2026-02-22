# FlowMVI Migration — Current Stage

## Plan: All ViewModels in Project

| #  | ViewModel                    | Type             | Complexity | Status     |
|----|------------------------------|------------------|------------|------------|
| 1  | FaqViewModel                 | Simple ViewModel | Low        | ✅ Migrated |
| 2  | MoreScreenViewModel          | Simple ViewModel | Low        | ✅ Migrated |
| 3  | SplashScreenViewModel        | Simple ViewModel | Low        | ✅ Migrated |
| 4  | ToggleNotificationsViewModel | Simple ViewModel | Low        | ✅ Migrated |
| 5  | ReferralViewModel            | BaseMviViewModel | Medium     | ✅ Migrated |
| 6  | DistrictsViewModel           | BaseMviViewModel | Medium     | ✅ Migrated |
| 7  | MapViewModel                 | BaseMviViewModel | Medium     | ✅ Migrated |
| 8  | FavoritesViewModel           | BaseMviViewModel | Medium     | ✅ Migrated |
| 9  | FlatDetailViewModel          | BaseMviViewModel | Medium     | ✅ Migrated |
| 10 | NotificationListViewModel    | BaseMviViewModel | Medium     | ✅ Migrated |
| 11 | FlatSearchViewModel          | BaseMviViewModel | High       | ⏳ Pending  |
| 12 | FilterViewModel              | BaseMviViewModel | High       | ✅ Migrated |

## Migrated to FlowMVI (Containers)

| ViewModel                    | Container                    | Date       |
|------------------------------|------------------------------|------------|
| FaqViewModel                 | FaqContainer                 | 2025-02-21 |
| MoreScreenViewModel          | MoreContainer                | 2025-02-22 |
| SplashScreenViewModel        | SplashContainer              | 2025-02-22 |
| ToggleNotificationsViewModel | ToggleNotificationsContainer | 2025-02-22 |
| ReferralViewModel            | ReferralContainer            | 2025-02-22 |
| DistrictsViewModel           | DistrictsContainer           | 2025-02-22 |
| MapViewModel                 | MapContainer                 | 2025-02-22 |
| FavoritesViewModel           | FavoritesContainer           | 2025-02-22 |
| FlatDetailViewModel          | FlatDetailContainer          | 2025-02-22 |
| FilterViewModel              | FilterContainer              | 2026-02-22 |
## Remaining (Not Yet Migrated)

| ViewModel | Notes |
| FlatSearchViewModel | |
