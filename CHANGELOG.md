![logo.png](logo.png)

# Changelog

All notable changes to this project will be documented here.

## [2.0.0+neoforge-mc-26.1.2] - 2026-05-25
- Port to 26.1.2
- Removed all fluid management

## [2.0.0.1+mc-26.1.2] - 2026-05-26
### Fixed
- Fallback recipe for cabinets

## [2.0.1+mc-26.1.2] - 2026-06-04
### Fixed
- Pale oak cabinet recipes
- Block properties refinements for all cabinet blocks

### Added
- Single Cabinets in all wood types - holds 1 folder
- Double Cabinets in all wood types - holds 2 folders

## [2.0.2+mc-26.1.2] - 2026-06-06
### Added
- Proper Cabinet and Index interaction flows

## [2.0.3+mc-26.1.2] - 2026-11-27
### Added
- Fluid storage is back: Fluid Cabinet holding 4 Canisters, 6 Canister tiers from 64,000 mB to 2,097,152,000 mB
- Fluid Cabinets can be linked to the Filing Index, which exposes the fluids of every linked Fluid Cabinet to pipes
- Jade tooltips for Filing Cabinets, Fluid Cabinets and the Filing Index
- JEI recipe categories for folder and canister upgrades and resets
- Right-click a Filing Folder or Canister in an inventory with a stack or bucket on the cursor to insert it without opening the GUI

### Changed
- Folder and Canister upgrades now cost 1 material instead of 3

### Fixed
- Hoppers and pipes can no longer pull Filing Folders out of cabinets, or insert stacked folders into them
- Items with extra data (custom names, enchantments, contents) are no longer accepted into folders
- Filing Index insertion now spreads across every matching folder instead of stopping at the first full one
- Breaking a Filing Index now unlinks its cabinets and clears Ledgers pointing at it
- Double-click deposit is tracked per player
- Cabinet fallback recipes no longer compete with the wood-specific recipes