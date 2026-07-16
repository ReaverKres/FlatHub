# Current stage — multi-country parsing

**Updated:** 2026-07-17  
**Active market:** Poland (MVP)  
**TZ:** [TZ_MULTI_COUNTRY_PARSING.md](./TZ_MULTI_COUNTRY_PARSING.md)

## Done

- [x] Product decisions / API recon / geo resources
- [x] `listing/core` + BY adapters + PL Otodom/OLX/Gratka/Morizon search
- [x] `MergedRepository` via registry (parallel requests, single emit after all sources)
- [x] Country/city picker UI (country cards + animated city list)
- [x] PL districts + Warsaw metro geo catalogs
- [x] PLN-first prices
- [x] Otodom detail enrichment (lat/lng, phones) + emit list `base` before network
- [x] OLX/Gratka/Morizon detail enrichment (`emit(base)` + merge)
- [x] Gratka/Morizon list coords (`location.map.center`) + fuller description
- [x] Warsaw metro station filter UI (M1/M2) + proximity enrich for PL

## In progress

- [ ] Composite Room key `(flatPlatform, adId)`
- [ ] Remote config `enabled_platforms_pl`

## Next

1. Composite PK
2. Remote config for PL platforms
3. Stabilize, then GE/KZ

## API summary (PL)

| Site    | Mechanism    | Status          |
|---------|--------------|-----------------|
| Otodom  | Next.js data | Search + detail |
| OLX.pl  | REST offers  | Search + detail |
| Gratka  | GraphQL      | Search + detail |
| Morizon | GraphQL      | Search + detail |
