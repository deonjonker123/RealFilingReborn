---
navigation:
  title: Filing Index
  icon: realfilingreborn:filing_index
  parent: index.md
  position: 4
item_ids:
  - realfilingreborn:filing_index
  - realfilingreborn:ledger
---

# Filing Index

The Filing Index links multiple Filing Cabinets into a **unified storage network**, exposing all their contents as a single inventory.

<RecipeFor id="filing_index" />

## Linking Cabinets

Use the <ItemLink id="ledger" /> to link and unlink Filing Cabinets.

<RecipeFor id="ledger" />

- **Right-click** the Filing Index to select its position, then select the Filing Cabinet(s) to link.
- The Ledger supports **single and multi-selection modes** for linking individual Filing Cabinets or entire areas at once.
- Visual wireframes show range and connections in real-time.

## Range

The Filing Index has a base range of **8 blocks**. Filing Cabinets outside this range cannot be linked. Extend the range with a [Range Upgrade](range_upgrades.md).

## Interaction

| Action | Result |
|--------|--------|
| Right-click with items | Inserts held items into a matching Filing Folder (single deposit) |
| **Double-right-click (any hand state)** | **Deposits ALL items in your entire inventory into their matching Filing Folders across all connected cabinets** |
| Shift + Right-click | Opens the Filing Index GUI |

Hoppers and pipes connected to the Filing Index will also route items into matching Filing Folders across the network automatically.