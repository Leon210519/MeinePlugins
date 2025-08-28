# LootPets

LootPets adds collectible pets that grant configurable boost multipliers.
This release targets **Java 21** and **Paper 1.21.4**.

## Setup
1. Place the LootPets jar in your `plugins/` folder.
2. Start the server once to generate the default configuration and pet
   definitions.
3. Edit `config.yml` and `pets_definitions.yml` as needed and run
   `/lootpets reload`.

## Key Configuration
* `boosts.per_level` – boost gained per pet level (default `0.01`).
* `boosts.star_multiplier` – multiplier applied per star (default `1.35`).
* `boosts.max_stars` – maximum stars a pet can reach (default `5`).
* `caps.global_multiplier_max` – hard cap for any multiplier (default `6.0`).
* `storage.provider` – `YAML`, `SQLITE` or `MYSQL`.
* `cross_server` – settings for optional multi-server mode.

## Permissions & Commands
* `lootpets.admin` – access to `/lootpets` administrative command.
* `/pets` – opens the player GUI.
* `/lootpets doctor` – runs validation and troubleshooting checks.
* `/lootpets backup now` – creates a manual backup.

## Placeholders
Requires PlaceholderAPI when enabled in `config.yml`.
* `%lootpets_boost_percent_<type>%` – current boost percent for an earning type.
* `%lootpets_slots%` – active/maximum pet slots.
* `%lootpets_active_list%` – formatted list of active pets.
* `%lootpets_top_pet_<type>%` – top contributing pet for a type.

## API Usage
```java
BigDecimal total = LootPetsAPI.apply(player,
    EarningType.EARNINGS_LOOTFACTORY,
    BigDecimal.valueOf(base));
```
`getMultiplier` and `apply` always return values `>= 1.0` and respect the
configured caps. The underlying `BoostService` can be obtained via
`LootPetsAPI.boosts()`.

## Storage
LootPets ships with YAML and SQL adapters. Changing providers will trigger a
migration when enabled in `config.yml`.

## Reload & Backup
The plugin supports safe reloads with `/lootpets reload` and automatic or manual
backups. If issues occur, use `/lootpets doctor` for diagnostics.

## Troubleshooting
* Run `/lootpets doctor` to validate configuration and data files.
* Use debug categories in `config.yml` to enable detailed logging.

