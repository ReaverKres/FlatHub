# Current stage — multi-country parsing

**Updated:** 2026-07-19  
**Active market:** United States (US) MVP  
**Canonical lessons:** [docs/LESSONS_MULTI_COUNTRY.md](./docs/LESSONS_MULTI_COUNTRY.md)

## Done recently

### US market

- **Registered:** Zumper rent only (`listing/us/zumper/`)
- **Sale:** disabled — `houses-for-sale/{slug}` 301→sitemap; `supportsSale=false`
- **Removed from app UI/enum:** Zillow + Apartments.com (PerimeterX / Akamai; GraphQL+list POST also
  403/404 anonymously)
- Restore notes: `tmp/us/api/*/NOTES.md`, `tmp/us/api/SUMMARY.md`
- Currency USD → `mainPrice`; CitySelect localization; ListingInsights UI

## Verify on device

- [x] US rent on Zumper (NYC smoke OK earlier)
- [ ] US sale → empty / no crash (capability off)
- [ ] Location card for US shows **only Zumper** icon
- [ ] CitySelect search latin + localized
- [ ] TH / AE / TR still OK after price rename
