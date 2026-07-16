# Current stage — multi-country parsing

**Updated:** 2026-07-17  
**Active market:** Poland (MVP)  
**TZ:** [TZ_MULTI_COUNTRY_PARSING.md](./TZ_MULTI_COUNTRY_PARSING.md)

## Done

- [x] Product decisions / TZ / API recon / geo resources
- [x] `listing/core` + BY adapters
- [x] PL search: Otodom, OLX.pl, Gratka, Morizon
- [x] `MergedRepository` fully via `ListingSourceRegistry` (search + detail + getById + clearCache)
- [x] Country picker + PL catalogs + PLN-first prices

## In progress

- [ ] PL detail enrichment (lat/lng, phones)
- [ ] Composite Room key `(flatPlatform, adId)`
- [ ] Remote config `enabled_platforms_pl`
- [ ] Warsaw metro filter UI

## API summary (PL)

| Site    | Mechanism    | Status |
|---------|--------------|--------|
| Otodom  | Next.js data | Search |
| OLX.pl  | REST offers  | Search |
| Gratka  | GraphQL      | Search |
| Morizon | GraphQL      | Search |
