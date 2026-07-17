# Current stage — multi-country parsing

**Updated:** 2026-07-17  
**Active market:** KZ stabilize + GE smoke  
**Canonical:** [docs/current_stage.md](./docs/current_stage.md)

## Done recently

- PL dates via DateConverter; hide commercial property type for non-BY
- Network ISO default country + equals/hashCode
- Location country/city as ElevatedCard (no Face icon)
- Background coord enrich (Krisha/kn/Livo) + `coordsEnriched` + MapScreen progress

## Verify on device

- [ ] ISO default country on fresh install / filter reset
- [ ] PL commercial: no property-type card
- [ ] KZ map pins appear after background enrich
- [ ] GE Livo coords after enrich
