---
navigation:
  title: External Storage
  icon: minecraft:comparator
  parent: index.md
  position: 6
---

# External Storage

The <ItemLink id="filing_index" /> exposes its entire network as a standard item handler, making it compatible with most storage network mods out of the box.

## Supported Mods

- **Applied Energistics 2** — Attach a Storage Bus to any face of the Filing Index or Filing Cabinet.
- **Refined Storage** — Attach an External Storage to any face of the Filing Index or Filing Cabinet.
- **Tom's Simple Storage** — Attach a Storage or Crafting Terminal to any face of the Filing Index or Filing Cabinet.
- **Integrated Dynamics** — Place an Item Interface on any face of the Filing Index or Filing Cabinet, connect it to a Logic Cable, then connect a Storage Terminal to access the network.

## How It Works

The Filing Index presents one logical slot per unique item type across all linked Filing Cabinets. The total count and capacity reported to the external system reflects the sum across **all** Filing Folders holding that item type in the network.

Items inserted or extracted by the external storage mod are automatically spread across or pulled from the appropriate Filing Folders in order.