#!/usr/bin/env bash
# test-mcauth.sh — Final, unbreakable version
set -euo pipefail

CONFIG="config.aurora"
[[ -f "$CONFIG" ]] || { echo "Error: config.aurora not found — run Aurora launcher first"; exit 1; }

TOKEN=$(grep -Po '(?<=access_token := ")[^"]+' "$CONFIG")
NAME=$(grep -Po '(?<=username      := ")[^"]+' "$CONFIG")
UUID=$(grep -Po '(?<=uuid          := ")[^"]+' "$CONFIG")
EXPIRES=$(grep -Po '(?<=expires_at    := )\d+' "$CONFIG")
NOW=$(date +%s)

echo "=== AURORA AUTH STATUS ==="
echo "Username     : $NAME"
echo "UUID         : $UUID"
echo "Token expires: $(date -d @"$EXPIRES" '+%Y-%m-%d %H:%M:%S') UTC"
echo "Time left    : $((EXPIRES - NOW)) seconds"

echo -e "\n[TEST] Fetching full profile from Mojang..."
RESPONSE=$(curl -fsS -H "Authorization: Bearer $TOKEN" \
     https://api.minecraftservices.com/minecraft/profile)

echo "$RESPONSE" | jq -r '
    "Skin URL     : \(.skins[]?.url // \"No skin\")",
    "Cape URL     : \(.capes[]?.url // \"No cape\")",
    "Name        : \(.name)",
    "UUID         : \(.id)"
' 2>/dev/null || echo "$RESPONSE" | grep -o "http://textures.minecraft.net/texture/[a-f0-9]\{64\}"

echo -e "\nAll checks passed. You are fully online."
echo "Launch Minecraft now — your skin will load instantly."