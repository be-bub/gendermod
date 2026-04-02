[![Download](https://img.shields.io/badge/Download-CurseForge-orange?style=for-the-badge&logo=curseforge)](https://curseforge.com/minecraft/mc-mods/genderbub)
[![Download](https://img.shields.io/badge/Download-Modrinth-green?style=for-the-badge&logo=modrinth)](https://modrinth.com/mod/genderbub)
[![Showcase](https://img.shields.io/badge/Showcase-YouTube-red?style=for-the-badge&logo=youtube)](https://youtu.be/x_alKNh-bEg)

# Important

1. **This mod adds genders and breeding restrictions for gameplay variety and a bit of realism. Not trying to offend anyone or make any statements.**

2. **Warning:** Before adding this mod to your world, make a backup. When adding new mobs to `enabledMobs` in the config, run `/genderbub reload`, then leave the chunk and come back (or restart the game) for changes to take effect.

## Magnifying Glass

**The mod adds a magnifying glass (found in the Tools & Utilities tab).**

- **Right-click animal with magnifying glass in any hand – shows gender + sterile status.**
- **In offhand – shows icons above scanned animals within radius.**
- **In main hand – shows icon only when aiming at a scanned mob.**

![lupa](https://cdn.modrinth.com/data/cached_images/a452afc9d215d65a87f3958b285f69ea9341c8b8.png)

---

## Config File

Config file location: `config/bebub/genderbub.json`

### Settings

- `maleChance` – chance for male gender in % (default 45)
- `femaleChance` – chance for female gender in % (default 45)
- `displayRadius` – radius for displaying icons in blocks (default 24)
- `allowMaleMaleBreed` – allow male + male breeding (default false)
- `allowFemaleFemaleBreed` – allow female + female breeding (default false)
- `allowSterileBreed` – allow sterile animals to breed (default false)
- `enabledMobs` – list of mobs affected by the mod. You can add any mob by its ID

**Note:** The mod only works for animals that can breed (entities that extend Animal class). If a mob doesn't get a gender assigned, it means it's not an animal and is not supported by the mod.

Sterile chance is calculated automatically from the remaining percentage: 100% - maleChance - femaleChance. By default male 45% + female 45% = 10% sterile. If you set male 50% and female 50%, sterile chance becomes 0%. Maximum value for male and female is 50% each. If you enter a higher value, it will be automatically reduced to 50%.

### Interaction Rules

Blocks certain interactions with mobs based on gender.

- `gender` – which gender the rule applies to ("male", "female", "sterile")
- `itemIds` – list of blocked items (e.g., ["minecraft:bucket", "mod:item"])

### Action Rules

Blocks certain actions (egg laying, milk production, etc.).

- `mobId` – entity ID
- `gender` – gender to block ("male", "female", "sterile")
- `action` – action to block ("lay_egg", "produce_milk", "grow_wool")
- `itemIds` – list of items that trigger the block
- `blocked` – true to block, false to allow

---

## Commands

- `/genderbub reload` – reloads the config without restarting the game (requires OP permissions)
- `/genderbub reset` – resets config to default values (requires OP permissions)

---

# FAQ

### Will there be a port to Fabric / NeoForge / other versions?

Not for now. The mod is made for Forge 1.20.1. If the mod becomes popular and I have the opportunity, I will try to port it to other loaders and versions.

### Can I use this mod in my modpack?

Yes, you can freely use this mod in any modpack. The only thing – if you have modded animals in your pack, you will need to add them manually to the config (enabledMobs), otherwise they will not receive genders.
