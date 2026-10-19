# JalebiHomes

Dialog API based `/home` plugin for Jalebi SMP.
Requires **Paper 1.21.6+** (uses the Dialog API) and **Java 21**.

## Commands
- `/homes` – open the homes dialog
- `/sethome [name]` – save a home at your position (no name = first free slot, named "Home N")

## Config
`default-homes` (default 3) – number of home slots per player.

## Build
Push to GitHub, open the **Actions** tab, open the latest run and download the
`JalebiHomes` artifact. Or locally: `mvn package` -> `target/JalebiHomes.jar`.
