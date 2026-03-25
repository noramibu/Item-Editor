# Item Editor

A client-side in-game item component editor for Fabric.

Item Editor opens a full editor UI for the item currently held by the player and lets you work on a safe draft before applying changes. It uses modern item component editing instead of old raw-NBT-only workflows and supports dynamic editors for many special item types, including books, banners, fireworks, containers, signs, potions, and more.

## Compatibility

- Current: Minecraft `26.1`
- Previous: Minecraft `1.21.11`

## Links

- GitHub: https://github.com/noramibu/Item-Editor
- Discord: https://discord.gg/FaxbR9eEFW

## Installation

1. Install Fabric Loader.
2. Put these files in your `mods` folder:
  - `Item Editor-version-b<build>.jar`
  - Fabric API (`0.144.3+26.1` or newer)
  - owo-lib (`0.13.0+26.1` or newer)
3. Start the game.
4. Open the editor using the keybind in Controls under `Item Editor` (default: `I`).

## Supported Languages

- English (`en_us`)
- Spanish (`es_es`)
- Russian (`ru_ru`)
- Simplified Chinese (`zh_cn`)
- Traditional Chinese (`zh_tw`)
- Hindi (`hi_in`)

## Features

### Session Flow

- Edits are made on a cloned draft of the held item.
- Preview updates live while editing.
- `Save / Apply` shows a diff before writing changes.
- Post-apply verification warns when a server rewrites or rejects fields.

### Apply Behavior

- **Creative mode:** writes through the creative inventory packet.
- **Singleplayer:** writes through the integrated server.
- **Multiplayer survival:** preview-only when server authority is required.

### Category-Based Editor

- General
- Display
- Attributes
- Enchantments
- Flags
- Book
- Special Data

The **Special Data** category is dynamic and changes based on the held item type.

## Editor Categories

### General

- Styled custom name editor
- Stack count and rarity
- Durability, current damage, max damage, and repair cost when supported
- Unbreakable and glint override
- Item model id and custom model data fields (float, string, color, flag)

### Display

- Lore editor with rich text styling
- Multiple lines
- Selection-based formatting
- Preset colors
- Hex color picker
- Gradient support

### Attributes

- Add, remove, and reorder attribute modifiers
- Searchable attribute picker
- Operation, slot group, amount, and modifier id editing
- Effective attribute preview

### Enchantments

- Regular and stored enchantment lists
- Searchable enchantment picker
- Optional unsafe or high-level values

### Flags

- Hide full tooltip toggle
- Hidden tooltip component toggles for common component lines

### Book

- Writable and written book workflow
- Page editor with vanilla-like page limits
- Page navigation
- Add, remove, and reorder pages
- Mini-map page list
- Title, author, and generation editing
- Rich text styling tools for page content

### Special Data

Available only when supported by the held item.

- **Potion contents:** base potion, color, custom name, and custom effects
- **Suspicious stew effects**
- **Fireworks:** rocket and star editor with live burst preview, shape, colors, fade, trail, and twinkle
- **Banners:** base color, pattern layers, and live canvas preview
- **Signs:** front and back text, style, color, glowing text, and board preview
- **Containers:** visual slot grid, click move or swap, and searchable item picker
- **Bucket creatures:** axolotl, salmon, tropical fish variants, and bucket entity flags
- **Dyed item colors**
- **Armor trims:** material and pattern pickers
- **Player heads:** profile and skin texture fields
- **Instruments and jukebox songs**
- **Map color and post-processing metadata**

## Screenshots

<div align="center">
  <img src="https://cdn.modrinth.com/data/FFDotM4D/images/c609dca904a5f0a4435a8e4a205bd590b8e39470.png" alt="General Editor" width="420" />
</div>

### General Editor

Edit core item data like name, rarity, durability, unbreakable state, glint override, and custom model fields.

<div align="center">
  <img src="https://cdn.modrinth.com/data/FFDotM4D/images/a7961c5ab0533bd2551a164e0c22779c3df35f2e.png" alt="Lore Editor" width="420" />
</div>

### Lore Editor

Write and style lore with multiple lines, formatting, preset colors, hex colors, and gradients.

<div align="center">
  <img src="https://cdn.modrinth.com/data/FFDotM4D/images/40503432cef7f5496b853f236c2c125298743844.png" alt="Banner Editor" width="420" />
</div>

### Banner Editor

Change banner base color, manage pattern layers, and preview the result live in the editor.

<div align="center">
  <img src="https://cdn.modrinth.com/data/FFDotM4D/images/2af18c8809472030b85bcf0350e99de531fad85d.png" alt="Book Editor" width="420" />
</div>

### Book Editor

Edit pages, reorder them, and manage written book data like title, author, and generation.

<div align="center">
  <img src="https://cdn.modrinth.com/data/FFDotM4D/images/de1f3cca86eaeebc60803064fae60a140e68242c.png" alt="Container Editor" width="420" />
</div>

### Container Editor

Open a visual slot-based container editor with move, swap, and searchable item selection support.
