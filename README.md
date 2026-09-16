# Seamless Loading Screen Rethinking

A client-side rework of [Minenash's Seamless Loading Screen](https://github.com/Minenash/Seamless-Loading-Screen). The mod saves a view of a world or server when you leave it normally, shows that screenshot during the next load, and fades it into the live game when the world is ready.

Rethinking keeps Minecraft's loading indicator visible and does not hold the player behind an extra transition screen after the world becomes playable.

## Features

- Saved world and server screenshots during loading, followed by a smooth transition into the game.
- Two screenshot reveal modes: **New** follows loading progress with a short opacity animation; **Classic** reveals the screenshot independently of progress.
- **Native** and **Optimized** screenshot output sizes.
- Configurable tint, transition duration, and optional world-icon updates.
- Optional use of Minecraft's built-in menu blur where available.
- A substantial under-the-hood rework of screenshot capture and saving, loading-screen state, and world transitions. These parts are designed to behave more reliably across supported Minecraft versions and mod loaders—not just look different.
