# SlimePlus Documentation

## Overview
SlimePlus is a Spigot plugin that enhances slime block mechanics with customizable bounce heights, removal of vanilla limits, and an optional boost mechanic.

## Features
- **Custom Bounce Multiplier**: Adjust how high players bounce off slime blocks.
- **Max Bounce Height**: Set a hard cap on the maximum bounce height.
- **Boost Mechanic**: Add an extra upward boost when bouncing.
- **Remove Vanilla Limit**: Bypasses the default 44-block height limit for slime bounces.
- **Fall Damage Protection**: Optional protection to prevent dying when landing after a high bounce.
- **Admin GUI**: Easy-to-use interface for OPs to manage settings in-game.

## Compatibility
SlimePlus is built on the Spigot API and is designed to work across a wide range of server platforms:
- **Spigot**: Fully Supported
- **Paper**: Fully Supported (Recommended)
- **Purpur**: Fully Supported
- **Pufferfish**: Fully Supported
- **CraftBukkit**: Fully Supported

Tested on Minecraft **1.21.x**, but likely works on older versions that support the modern Spigot API.

## Commands
- `/slimeplus`: Opens the Admin Control GUI. (OP only)

## Permissions
- `slimeplus.admin`: Allows access to the `/slimeplus` command and the Admin GUI. (Default: OP)

## Installation
1. Build the plugin using the included Gradle wrapper:
   ```powershell
   .\gradlew build
   ```
2. Locate the built JAR file in `build/libs/SlimePlus-1.0-SNAPSHOT.jar`.
3. Place the JAR file into your server's `plugins` folder.
3. Restart or reload your server.
4. Use `/slimeplus` in-game (must be OP) to configure the plugin.

## Configuration
The `config.yml` file is located in `plugins/SlimePlus/config.yml`.

```yaml
# SlimePlus Configuration
bounce-multiplier: 1.0
max-bounce-height: 256.0
boost-enabled: false
boost-power: 0.5
remove-vanilla-limit: true
prevent-fall-damage: true
```

## Developer
Developed by **CRZX1337**
