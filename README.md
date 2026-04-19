# Item Editor - Component & NBT

A powerful client-side in-game item component editor and custom NBT modifier for Minecraft Fabric.

Item Editor opens a comprehensive GUI for the item currently held by the player, allowing you to safely modify item data on a draft before applying changes. Designed as a modern alternative to traditional raw NBT data editors, this mod natively utilizes Minecraft's modern data components system instead of relying solely on outdated raw-NBT workflows. It features dynamic custom UI editors for modifying item properties, metadata, and component tags for special item types, including books, banners, custom fireworks, containers, signs, potions, and more.

## Links

- [Modrinth](https://modrinth.com/project/item-editor)
- [GitHub](https://github.com/noramibu/Item-Editor)
- [Discord](https://discord.gg/FaxbR9eEFW)

## Installation

1. Install Fabric Loader.
2. Put these files into your `mods` folder:
- `Item Editor` jar
- Fabric API
- owo-lib
3. Start the game.
4. Open the editor from Controls under `Item Editor` (default key: `I`).

## Supported Languages

- English (`en_us`)
- Spanish (`es_es`)
- Russian (`ru_ru`)
- Simplified Chinese (`zh_cn`)
- Traditional Chinese (`zh_tw`)
- Hindi (`hi_in`)

## Core Workflow

- Draft-first editing (all edits are made on a temporary copy of the held item).
- Live feedback (preview updates while editing, with validation shown inline).
- Safe apply flow (`Save / Apply` shows a diff/verification flow).
- Discard safety (close/reset asks for confirmation when needed).

## Editor Categories

Current categories are:

- General
- Components (dynamic)
- Display
- Attributes
- Enchantments
- Flags
- Book
- Raw Editor
- Special Data (dynamic)

### General

- Styled custom name editing (format, presets, gradient, color picker).
- Item core values (count, rarity, durability/damage, repair cost, unbreakable).
- Adventure predicates (`can_break` / `can_place_on`) with searchable block picking and list controls.
- Model/visual fields (glint override, item model id, custom model values).

### Components

Shown only when the held item supports advanced components.

- Food/consumable related values.
- Use behavior fields (`use_effects`, `use_remainder`, `use_cooldown`).
- Container-related metadata (lock, loot, bees, pot decorations).
- Combat/data components (`equippable`, `weapon`, `tool`, `repairable`, `attack_range`, charged projectiles).
- Advanced map data (`map_id`, decorations, lodestone tracking).
- Additional component tweaks for naming, tooltip, registry ids, block/behavior fields.

### Display

- Rich multi-line lore editor with formatting toolbar.
- Color/gradient styling tools.
- Related visual sub-sections when supported (like dyed color/trim/profile fields).

### Attributes

- Add/remove/reorder attribute modifiers.
- Modifier fields (attribute, amount, operation, slot, id).
- Effective preview and reset-to-original workflow.

### Enchantments

- Regular + stored enchantment editing.
- Searchable picker, reorder/remove/add.
- Level controls with safe/unsafe behavior support.

### Flags

- Tooltip visibility controls.
- Hide full tooltip and hidden-component toggles.
- Quick hide/show actions.

### Book

- Writable/written mode flow.
- Page editor with limits/validation.
- Add/remove/reorder/navigate pages.
- Title/author/generation editing when relevant.

### Raw Editor

- Full raw text editor for item data when you want direct control.
- Format/minify tools and editor options (wrap/scroll style, font size, and display options).
- Syntax-aware editing with clearer inline error targeting.
- Better handling for long/minified text.
- Smarter suggestions while editing.
- Undo/redo workflow for safer raw editing sessions.

### Special Data

Dynamic category. Sections appear only for compatible items.

- Potion/suspicious stew/firework editors.
- Banner/shield editor with layer workflow.
- Container/bundle/sign/spawner related editors.
- Bucket creature and other item-specific editors.
- New dedicated editors for Armor Stand, Item Frame, and Spawn Egg (including villager trade data).
- Armor Stand editor includes pose-focused controls and quick preset-based workflows.
- Item Frame editor includes facing/rotation and behavior-focused controls.
- Spawn Egg editor includes entity-focused settings and villager trade editing where relevant.

## Storage

- Save items into built-in storage pages.
- Browse/search/sort saved items.
- Search by item/name/lore and other metadata.
- Sort modes include regular ordering and metadata-based ordering.
- Command access via `/storage`.


## Screenshots

<div align="center">
  <img src="https://cdn.modrinth.com/data/FFDotM4D/images/c609dca904a5f0a4435a8e4a205bd590b8e39470.png" alt="General Editor" width="420" />
</div>

<div align="center">
  <img src="https://cdn.modrinth.com/data/FFDotM4D/images/a7961c5ab0533bd2551a164e0c22779c3df35f2e.png" alt="Lore Editor" width="420" />
</div>

<div align="center">
  <img src="https://cdn.modrinth.com/data/FFDotM4D/images/70b08ab32e1b42552dbaa84bfbcdf1acfca6e760.png" alt="New Banner Editor" width="420" />
</div>

<div align="center">
  <img src="https://cdn.modrinth.com/data/FFDotM4D/images/2af18c8809472030b85bcf0350e99de531fad85d.png" alt="Book Editor" width="420" />
</div>

<div align="center">
  <img src="https://cdn.modrinth.com/data/FFDotM4D/images/de1f3cca86eaeebc60803064fae60a140e68242c.png" alt="Container Editor" width="420" />
</div>

<div align="center">
  <img src="https://cdn.modrinth.com/data/FFDotM4D/images/770c3541c025a23498118464c8ac9b3650450746.png" alt="Armor Stand Editor" width="420" />
</div>

<div align="center">
  <img src="https://cdn.modrinth.com/data/FFDotM4D/images/92c926e9b14bc10a0dce63616a38a766af53cf6e.png" alt="Raw Editor" width="420" />
</div>

<div align="center">
  <img src="https://cdn.modrinth.com/data/FFDotM4D/images/9a7aa731749755886f99dea535e24d8911469206.png" alt="New Sign Editor" width="420" />
</div>

<div align="center">
  <img src="https://cdn.modrinth.com/data/FFDotM4D/images/1e2e771d9a3f1850d9d54c61f8abd3fa02fee8c7.png" alt="Storage" width="420" />
</div>