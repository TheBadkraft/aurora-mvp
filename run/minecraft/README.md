# run/minecraft — runtime template and instructions

This folder is a small template and documentation area stored in the repository. It intentionally does NOT contain any jars, server binaries, or user-specific configuration.

Purpose
- Provide a safe place for small templates, examples, and instructions that help users create their own local `minecraft/` runtime folder.
- Keep all actual runtime binaries and user data out of source control.

What to put here
- README.md (this file)
- small example files, e.g. `server.properties.example`, `mods-list.example`, or `.template` files
- a `.gitkeep` if you want to ensure the directory is tracked (already included)

What NOT to put here
- Any `.jar` files (server jars, mods that are redistributed, native libraries)
- Any credentials, saved worlds, or user-specific configs

How to create the real runtime folder
- Run the helper script in the repository:
  - ./scripts/create-minecraft-structure.sh
- The script will create a top-level `minecraft/` folder (ignored by git) with common subfolders and a nested `.gitignore` that prevents *.jar files from being committed.

If you want to include configuration templates
- Add files with the `.example` or `.template` suffix in this folder. Users can copy and rename them when setting up their local runtime.

If you must distribute a tiny config file that is safe to share, keep it in `run/minecraft/` and mark it as an example.
