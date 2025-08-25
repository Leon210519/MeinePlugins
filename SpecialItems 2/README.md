# SpecialItems2

A standalone clone of the original SpecialItems plugin targeting Minecraft 1.21.4.

## Custom Model Data policy
* CustomModelData values are **integers only** and must come from the internal whitelist in `CmdRegistry`.
* Allowed values:
  * Swords – 1001, 1003, 1004
  * Pickaxes – 1101, 1102, 1103, 1104
  * Hoes – 1201, 1202, 1203, 1204
* Any value outside this set is removed and a warning is logged.
* Legacy or floating point CMD representations are normalized using `CustomModelDataUtil` so that only the exact integer remains.

Templates are loaded from `plugins/SpecialItems2/templates.yml` and all items are normalized on load, join, inventory movement, and GUI rendering.
