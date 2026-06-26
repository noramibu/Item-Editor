<p align="center">
  <img src="https://raw.githubusercontent.com/noramibu/Item-Editor/main/src/main/resources/assets/itemeditor/icon.png" alt="Item Editor Logo" width="150">
</p>

<h1 align="center">Item Editor</h1>

<p align="center">
  <strong>A powerful client-side in-game component and NBT editor for Minecraft: Java using the Fabric mod-loader.</strong>
</p>

<div align="center">

[![Modrinth Downloads](https://img.shields.io/modrinth/dt/FFDotM4D?style=for-the-badge&logo=Modrinth&label=Modrinth&color=1bd96a)](https://modrinth.com/project/item-editor)
[![GitHub Source](https://img.shields.io/badge/Source-181717?style=for-the-badge&logo=GitHub&label=GitHub)](https://github.com/noramibu/Item-Editor)
[![Discord Join](https://img.shields.io/badge/Join-5865F2?style=for-the-badge&logo=Discord&label=Discord)](https://discord.gg/FaxbR9eEFW)

</div>

---

## Overview

Item Editor opens a comprehensive GUI for the currently held item, allowing you to safely modify item data on a draft before applying changes. Designed as a modern alternative to traditional raw NBT data editors, this mod utilizes Minecraft's modern data component system instead of relying solely on outdated raw-NBT workflows.

It features dynamic custom UI editors for modifying item properties, metadata, and components, including menus for special item types such as books, banners, custom fireworks, containers, signs, potions, and more.

---

## Key Features
- **Draft-first Editing** - Safe modifications with live preview
- **Modern Component System** - Native support for Minecraft 1.20.5+ data components
- **Dynamic Editors** - Specialized UIs for books, banners, fireworks, containers, villager trades, command blocks and more
- **Raw NBT Fallback** - Full text editor with syntax highlighting and validation
- **Built-in Storage** - Save, search, and organize items, and import saved items from other mods
- **Multiplayer Compatible** - Does not require a server-side plugin to function, only requires creative mode.

---

## Installation

### Requirements
- **Minecraft Fabric** (compatible version)
- **Fabric API**
- **owo-lib**

### Steps
1. Install [Fabric Loader](https://fabricmc.net/use/)
2. Download the following files:
    - [Item Editor](https://modrinth.com/project/item-editor) (this mod)
    - [Fabric API](https://modrinth.com/mod/fabric-api)
    - [owo-lib](https://modrinth.com/mod/owo-lib)
3. Place all `.jar` files into your `mods` folder
4. Launch the game
5. Open the editor via **Controls → Item Editor** (default key: `I`)

---

## Core Workflow

Item Editor uses a **draft-first editing** system to ensure safe, predictable modifications:

| Feature | Description|
|---------|------------|
| **Draft Editing** | All edits are made on a temporary copy of the held item|
| **Live Feedback** | Preview updates while editing, with validation shown inline|
| **Safe Apply** | `Save / Apply` shows a diff/verification flow before committing|
| **Discard Safety** | Close/Reset asks for confirmation when needed|

---

## Editor Categories

### General
- **Custom Name Editing:** Styled formatting with presets, gradients, and color picker
- **Core Values:** Count, Rarity, Durability/Damage, Repair Cost, Unbreakable
- **Visual/Model Fields:** Glint override, Item Model, Custom Model values
- **Adventure Predicates:** Can Break / Can Place On with searchable block picking

### Components

**Always Available**
- **Name/Stack/Flags:** Item Name, Glider, Death Protection
- **Food/Consumable:** Nutrition, Saturation, Use Behavior
- **Combat Behavior:** Animation Type & Duration, Hit Sounds with searchable sound picking
- **Registry Components:** Damage Type, Note Block & Break Sound, Painting Variant
- **Equipment / Combat:** Equipment Slot, Equip and Shearing Sounds, Asset ID, Camera Overlay, Weapon & Tool settings, Attack Range
- **Block Attacks:** Block Attacks timings, Damage Reduction, Item Damage
- **Custom Data:** Raw Editor for Custom Data compounds


**Item Specific**
- **Block State:** Block State properties
- **Charged Projectiles:** Item ID, Count
- **Map Metadata:** Map Color, Post-Processing (Lock/Scale)
- **Advanced Map:** Map ID, Decorations, Lodestone Tracker
- **Container Metadata:** Lock ID, Loot Table, Bee Occupants, Pot Decorations

### Display
- Rich multi-line editor with formatting toolbar
- Color/Gradient styling, Head & Sprite insertion and rendering
- Save system for your favorite colors, gradients and shadows

### Attributes
- Add, Edit or Remove Attributes
- Menu for each Attribute, with Attribute, Amount, Operation, Slot, and Modifier ID fields
- Summarizes current attribute values and allows resetting to original

### Enchantments
- Searchable Enchantment picker and Level adjuster
- Reorder or remove Enchantments
- Allow "unsafe" enchantment levels over the survival limit

### Flags
- Hide all or specific components from the tooltip or hide the entire tooltip

### Raw Editor
- Full raw text editor for direct control
- Format/Minify tools in editor options
- Syntax-aware editing with inline error targeting and validation
- Collapse components at multiple levels
- Smart suggestions while editing

### Special Data
*Dynamic category. Sections appear only for compatible items.*
- **Written Book/Writable Book/Sign:** Rich multi-line editor with formatting toolbar, even insertion, and preview
- **Potion/Suspicious Stew:** Potion ID, Color, Custom Name, Custom Effect with searchable picker
- **Firework Explosion:** Add Explosions, editing Shape, Material, Colors, Fade Colors, Trail and Twinkle
- **Banner/Shield:** Creator with Layer Picker, Color Picker, and Preview
- **Container/Bundle:** Add and Modify Slots of Chests, Shulkers and Bundles
- **Spawner/Spawn Egg/Bucket:** Add and Modify Entity Data
- **Armor Stand:** Pose with Presets and Preview, Custom Name, Inventory Locks, Scale
- **Item Frame:** Custom Name, Flags, Item Rotation, Drop Chance, Facing
- **Command Block:** Type, Command, Custom Name, Activation, Output / Runtime State

### Storage
Save items into a built-in storage menu:
- **Browse/Search/Sort:** Filter saved items by Item, Name, Lore, Amount, Size, and Modification Date
- **Sort Modes:** Regular Slot, Saved Time, Alphabetical, Stack Count, or Size
- **Page Organization:** Set custom page names and orders
- **Import:** Bring saved items from other mods (NBT Editor, Librarian)
- **Access:** Open via `/storage` command, or via `O` keybind (rebindable)

---

## Screenshots

<div align="center">
  <img src="https://cdn.modrinth.com/data/FFDotM4D/images/4a5e54b2688d95c6111c7af9f2e5d19fc11c77be.png" alt="General Editor" width="420" />
  <img src="https://cdn.modrinth.com/data/FFDotM4D/images/4e1fc4db42082d2f314ea23b0505c8800f1b8a9c.png" alt="Lore Editor" width="420" />
</div>

<div align="center">
  <img src="https://cdn.modrinth.com/data/FFDotM4D/images/70b08ab32e1b42552dbaa84bfbcdf1acfca6e760.png" alt="Banner Editor" width="420" />
  <img src="https://cdn.modrinth.com/data/FFDotM4D/images/2af18c8809472030b85bcf0350e99de531fad85d.png" alt="Book Editor" width="420" />
</div>

<div align="center">
  <img src="https://cdn.modrinth.com/data/FFDotM4D/images/d2ec293faac69a0cb3105edbd0cf0f9cf8a09544.png" alt="Enchants" width="420" />
  <img src="https://cdn.modrinth.com/data/FFDotM4D/images/770c3541c025a23498118464c8ac9b3650450746.png" alt="Armor Stand Editor" width="420" />
</div>

<div align="center">
  <img src="https://cdn.modrinth.com/data/FFDotM4D/images/7161db8e3660b78ad4c4dffe8e98b59a49615332.png" alt="Raw Editor" width="420" />
  <img src="https://cdn.modrinth.com/data/FFDotM4D/images/9a7aa731749755886f99dea535e24d8911469206.png" alt="Sign Editor" width="420" />
</div>

<div align="center">
  <img src="https://cdn.modrinth.com/data/FFDotM4D/images/cb1fdbd2701f22801d49a2a2eb08810bba2b7223.png" alt="Storage" width="420" />
  <img src="https://cdn.modrinth.com/data/FFDotM4D/images/ace4a0eb199c27305ae927bf6e71b30b6f9857b6.png" alt="Storage Pages" width="420" />
</div>

---

## Supported Languages

| Language | Native name | Code |
|----------|-------------|------|
| English | English | `en_us` |
| Spanish | Español | `es_es` |
| Russian | Русский | `ru_ru` |
| Simplified Chinese | 简体中文 | `zh_cn` |
| Traditional Chinese | 繁體中文 | `zh_tw` |
| Hindi | हिन्दी | `hi_in` |

---

## Contributing

Contributions are welcome! Please feel free to:
- Report bugs via [GitHub Issues](https://github.com/noramibu/Item-Editor/issues)
- Submit feature requests
- Join the [Discord](https://discord.gg/FaxbR9eEFW) for discussion

---

### NBT Library

If you are interested in item making, NBT etc, also check out NBT Library. The NBT Library is focused on a "quality over quantity" approach, providing meticulously curated hotbars full of items, kits, books, and more for your creative servers and single-player worlds.
<br><small> Also thanks a lot to Kaddicus for his countless helps while developing this mod, without him this mod would be a simple Lore Editor.<small>

- [Website](https://kadthehunter.github.io/NBT-Library/)
- [Discord](https://discord.gg/cfq25qURfv)

---

## License

This project is licensed under the terms of the license specified in the [LICENSE](LICENSE) file.

---

<p align="center">
  <a href="https://modrinth.com/project/item-editor">Modrinth</a> •
  <a href="https://github.com/noramibu/Item-Editor">GitHub</a> •
  <a href="https://discord.gg/FaxbR9eEFW">Discord</a>
</p>
