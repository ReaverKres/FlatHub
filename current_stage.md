# Current stage — multi-country parsing

**Updated:** 2026-07-17  
**Active market:** Georgia (MVP in progress)  
**TZ:** [docs/TZ_MULTI_COUNTRY_PARSING.md](./docs/TZ_MULTI_COUNTRY_PARSING.md)  
**Lessons:** [docs/LESSONS_MULTI_COUNTRY.md](./docs/LESSONS_MULTI_COUNTRY.md)  
**Canonical copy:** [docs/current_stage.md](./docs/current_stage.md)

## Done

### Poland MVP
- [x] Product decisions / API recon / geo resources
- [x] `listing/core` + BY adapters + PL Otodom/OLX/Gratka/Morizon search+detail
- [x] `MergedRepository` via registry (parallel, single emit)
- [x] Country/city picker UI + PL districts + Warsaw metro
- [x] PLN-first prices, composite PK, `enabled_platforms_pl`, bundled PL icons

### Georgia recon (subagents + local probes)

- [x] **SS.ge** — NOTES + filters (`realEstateDealType` / `realEstateType`) + cities + list sample  
  Auth: Bearer `credentialsToken` from `__NEXT_DATA__` + cookies; retry on 401
- [x] **Livo** — working anonymous `GET /v1/statements` + detail + city IDs
- [x] **Binebi** — CSRF/session HTML scrape documented → **stub later**
- [x] **MyHome** — Cloudflare 403 → **skip MVP**

### Georgia core (this pass)

- [x] `CountryCode.GE` + cities TBILISI/BATUMI/KUTAISI/RUSTAVI
- [x] `FlatPlatform.SS_GE`, `LIVO`
- [x] `listing/ge/ss/` + `listing/ge/livo/` search (+ Livo detail)
- [x] DI registry + `enabled_platforms_ge` + GEL currency + bundled icons
- [x] LocationUiMapper Georgia card

## In progress / verify on device

- [ ] Run search for GE Tbilisi rent — confirm SS + Livo results in UI
- [ ] Confirm SS auth stable on Android/iOS (cookie + Bearer)
- [ ] Livo detail URL / description enrichment smoke test

## Deferred (try later — not blocking GE MVP)

| Item                                               | Why deferred                                                       |
|----------------------------------------------------|--------------------------------------------------------------------|
| SS.ge detail PUT `/v1/RealEstate/details` + phones | List payload rich enough; OpenAPI PUT not fully confirmed in probe |
| SS.ge MapSearch coords                             | Rare on list; enrich later                                         |
| Binebi ListingSource                               | CSRF + HTML fragment scrape — heavy                                |
| MyHome ListingSource                               | Cloudflare block anonymous                                         |
| Tbilisi metro catalog / filter UI                  | SS has `nearbySubwayStations` titles only; no full geo catalog yet |
| GE districts filter UI                             | Catalog not wired                                                  |
| Country picker layout polish for 3+ cards          | Row+weight works; may need wrap later                              |

## Next

1. Device/smoke verify GE search+detail
2. Soft-fail stubs optional for Binebi/MyHome if needed for error-dialog completeness
3. Stabilize GE → KZ

## API summary (GE)

| Site   | Mechanism                 | Status                 |
|--------|---------------------------|------------------------|
| SS.ge  | LegendSearch + Next token | Search (detail = list) |
| Livo   | REST statements           | Search + detail        |
| Binebi | HTML/CSRF                 | Deferred               |
| MyHome | CF blocked                | Skip MVP               |

## Remote config

| Key                    | Format                         | Empty means               |
|------------------------|--------------------------------|---------------------------|
| `enabled_platforms_pl` | `OTODOM,OLX_PL,GRATKA,MORIZON` | all registered PL sources |
| `enabled_platforms_ge` | `SS_GE,LIVO`                   | all registered GE sources |
