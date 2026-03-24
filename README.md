# Item Editor

Client-side Fabric item editor for Minecraft `1.21.11`, built with `owo-ui`.

![Item Editor Icon](src/main/resources/assets/itemeditor/icon.png)

[![Modrinth](https://img.shields.io/badge/Modrinth-item--editor-1bd96a?logo=modrinth&logoColor=white)](https://modrinth.com/project/item-editor)

## Links

- Modrinth: https://modrinth.com/project/item-editor
- GitHub: https://github.com/noramibu/Item-Editor
- Discord: https://discord.gg/FaxbR9eEFW

## Installation

1. Install Fabric Loader for Minecraft `1.21.11`.
2. Put these files in your `mods` folder:
   - `Item Editor-1.21.11-b<build>.jar`
   - Fabric API (`0.141.3+1.21.11` or newer)
   - owo-lib (`0.13.0+1.21.11` or newer)
3. Start the game and open the editor with the keybind in Controls under `Item Editor` (default: `I`).

## What This Mod Is

Item Editor is a client-side in-game item component editor for Fabric `1.21.11`.

- Opens a full editor UI for the item currently held by the player.
- Applies edits to a draft first, then lets you confirm with `Save / Apply`.
- Uses modern item component editing (not legacy raw-NBT-only workflows).
- Designed for creative/singleplayer authoring, with safe preview behavior in multiplayer survival.
- Includes dynamic editors for special item types (books, banners, fireworks, containers, signs, potions, and more).

## Supported Languages

- English (`en_us`)
- Spanish (`es_es`)
- Russian (`ru_ru`)
- Simplified Chinese (`zh_cn`)
- Traditional Chinese (`zh_tw`)
- Hindi (`hi_in`)

## Features

- **Session flow**
  - Edits are made on a cloned draft of the held item.
  - Preview updates live while editing.
  - `Save / Apply` shows a diff and then writes if allowed.
  - Post-apply verification warns when a server rewrites/rejects fields.

- **Apply behavior**
  - Creative mode: writes via creative inventory packet.
  - Singleplayer: writes through the integrated server.
  - Multiplayer survival: preview-only when server authority is required.

- **Category-based editor**
  - General, Display, Attributes, Enchantments, Flags, Book.
  - Special Data category is dynamic and changes by held item type.

- **General**
  - Styled custom name editor.
  - Stack count and rarity.
  - Durability/current damage/max damage/repair cost (when item supports them).
  - Unbreakable and glint override.
  - Item model id and custom model data fields (float/string/color/flag).

- **Display**
  - Lore editor with rich text styling.
  - Multiple lines, selection-based formatting, preset colors.
  - Hex color picker and gradients.

- **Attributes**
  - Add/remove/reorder attribute modifiers.
  - Searchable attribute picker.
  - Operation, slot group, amount, modifier id editing.
  - Effective attribute preview.

- **Enchantments**
  - Regular and stored enchantment lists.
  - Searchable enchantment picker.
  - Optional unsafe/high-level values.

- **Flags**
  - Hide full tooltip toggle.
  - Hidden tooltip component toggles for common component lines.

- **Book**
  - Writable/written workflow.
  - Page editor with vanilla-like page limits.
  - Page navigation, add/remove/reorder, and mini-map list.
  - Written-book metadata: title, author, generation.
  - Rich text styling tools for page content.

- **Special Data** (only when supported by held item)
  - Potion contents: base potion, color, custom name, custom effects.
  - Suspicious stew effects.
  - Firework rocket/star editor with live burst preview, shape/colors/fade/trail/twinkle.
  - Banner editor with base color, pattern layers, and live canvas preview.
  - Sign editor with front/back text, style, color, glowing, and board preview.
  - Container editor with visual slot grid, click move/swap, and searchable item picker.
  - Bucket creature editor (axolotl/salmon/tropical variants and bucket entity flags).
  - Dyed item color editor.
  - Armor trim material/pattern pickers.
  - Player head profile/skin texture fields.
  - Instrument and jukebox song fields.
  - Map color and map post-processing metadata.
