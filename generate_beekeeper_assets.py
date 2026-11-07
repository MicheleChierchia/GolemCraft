"""
generate_beekeeper_assets.py
Generates beekeeper_golem.png, beekeeper_golem_eyes.png, beekeeper_honey_layer.png, and honey_spit.png.
Includes:
- 3D sunflower on head
- Precise UV mapping for the Honey Dipper (raccogli miele)
- Dynamic Honey Layer overlay: The honey on the body only appears when accumulated (honeyLevel > 0)!
"""

import os
from PIL import Image

ROOT_DIR = os.path.dirname(os.path.abspath(__file__))
TEXTURE_DIR = os.path.join(ROOT_DIR, "src/main/resources/assets/golemcraft/textures/entity")
PROJECTILE_DIR = os.path.join(TEXTURE_DIR, "projectiles")
COPPER_BASE = os.path.join(ROOT_DIR, "copper golem/copper_golem.png")

BEEKEEPER_PATH = os.path.join(TEXTURE_DIR, "beekeeper_golem.png")
EYES_PATH = os.path.join(TEXTURE_DIR, "beekeeper_golem_eyes.png")
HONEY_LAYER_PATH = os.path.join(TEXTURE_DIR, "beekeeper_honey_layer.png")
HONEY_SPIT_PATH = os.path.join(PROJECTILE_DIR, "honey_spit.png")

os.makedirs(TEXTURE_DIR, exist_ok=True)
os.makedirs(PROJECTILE_DIR, exist_ok=True)

def main():
    base = Image.open(COPPER_BASE).convert("RGBA")
    w, h = base.size
    golem_img = base.copy()
    eyes_img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    honey_layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))

    # Eyes: warm amber glowing eyes
    eye_pixels = {
        (11, 12), (12, 12), (15, 12), (16, 12),
        (11, 13), (12, 13), (15, 13), (16, 13)
    }
    AMBER_DARK = (212, 119, 6, 255)
    AMBER_MID = (245, 158, 11, 255)
    AMBER_BRIGHT = (254, 240, 138, 255)

    for (x, y) in eye_pixels:
        if (x in (12, 15) and y == 13):
            col = AMBER_BRIGHT
        elif x in (12, 15):
            col = AMBER_MID
        else:
            col = AMBER_DARK
        golem_img.putpixel((x, y), col)
        eyes_img.putpixel((x, y), col)

    # 1. Sunflower UV on golem texture:
    # Stem: texOffs(40, 40) - 1x5x1 cube -> x: 40..44, y: 40..46
    STEM_GREEN_DARK = (45, 80, 22, 255)
    STEM_GREEN_LIGHT = (74, 119, 35, 255)
    for y in range(40, 47):
        for x in range(40, 44):
            c = STEM_GREEN_LIGHT if (x + y) % 2 == 0 else STEM_GREEN_DARK
            golem_img.putpixel((x, y), c)

    # Sunflower head: texOffs(44, 40) - 5x5x2 cube -> x: 44..58, y: 40..47
    PETAL_YELLOW = (250, 204, 21, 255)
    PETAL_ORANGE = (245, 158, 11, 255)
    FLOWER_CENTER_DARK = (92, 45, 10, 255)
    FLOWER_CENTER_LIGHT = (130, 68, 18, 255)

    for y in range(40, 48):
        for x in range(44, 58):
            c = PETAL_ORANGE if (x + y) % 2 == 0 else PETAL_YELLOW
            golem_img.putpixel((x, y), c)

    for y in range(43, 46):
        for x in range(47, 50):
            c = FLOWER_CENTER_LIGHT if (x == 48 and y == 44) else FLOWER_CENTER_DARK
            golem_img.putpixel((x, y), c)

    # 2. Honey Dipper (raccogli miele) UV layout:
    # Handle Box: texOffs(0, 48), size (1, 1, 10) -> (x: 0..22, y: 48..59)
    WOOD_LIGHT = (195, 145, 95, 255)
    WOOD_MID   = (165, 115, 65, 255)
    WOOD_DARK  = (130, 85, 45, 255)
    for y in range(48, 60):
        for x in range(0, 23):
            c = WOOD_LIGHT if (x + y) % 3 == 0 else (WOOD_MID if (x + y) % 2 == 0 else WOOD_DARK)
            golem_img.putpixel((x, y), c)

    # Dipper Head Box: texOffs(24, 48), size (3, 3, 3) -> (x: 24..36, y: 48..54)
    HONEY_AMBER_BRIGHT = (254, 240, 138, 255) # glistening highlight
    HONEY_AMBER_GOLD   = (251, 191, 36, 255)  # vivid golden honey
    HONEY_AMBER_MID    = (245, 158, 11, 255)  # warm pure honey
    HONEY_AMBER_DEEP   = (217, 119, 6, 255)   # darker shadow / thick honey
    HONEY_GROOVE_RING  = (146, 64, 14, 255)   # groove contrast

    for y in range(48, 55):
        for x in range(24, 37):
            if y in (48, 50):
                c = HONEY_AMBER_GOLD if (x + y) % 2 == 0 else HONEY_AMBER_BRIGHT
            elif y in (51, 53):
                c = HONEY_GROOVE_RING if x % 2 == 0 else HONEY_AMBER_DEEP
            elif y == 52:
                c = HONEY_AMBER_GOLD if x % 2 == 0 else HONEY_AMBER_MID
            else:
                c = HONEY_AMBER_DEEP if x % 2 == 0 else HONEY_AMBER_GOLD
            golem_img.putpixel((x, y), c)

    # 3. Dynamic Honey Accumulation Layer Overlay
    # Notice: NOT applied to golem_img! The golem's copper body is clean by default.
    # The honey layer overlay (beekeeper_honey_layer.png) only renders when honeyLevel > 0!
    drips_dense = []

    # Head top & forehead honey pools (y in 0..14)
    for y in range(2, 9):
        for x in range(9, 15):
            drips_dense.append((x, y, HONEY_AMBER_GOLD))
    for x in range(10, 18):
        drips_dense.append((x, 10, HONEY_AMBER_GOLD))
    drips_dense.extend([
        (10, 11, HONEY_AMBER_MID), (11, 11, HONEY_AMBER_BRIGHT),
        (16, 11, HONEY_AMBER_BRIGHT), (17, 11, HONEY_AMBER_MID),
        (11, 12, HONEY_AMBER_GOLD), (16, 12, HONEY_AMBER_GOLD)
    ])

    # Body Front Chest drips (x in 4..12, y in 19..27)
    for y in range(19, 23):
        for x in range(5, 11):
            drips_dense.append((x, y, HONEY_AMBER_GOLD if (x + y) % 2 == 0 else HONEY_AMBER_BRIGHT))
    drips_dense.extend([
        (5, 23, HONEY_AMBER_MID), (5, 24, HONEY_AMBER_DEEP),
        (7, 23, HONEY_AMBER_GOLD), (7, 24, HONEY_AMBER_MID), (7, 25, HONEY_AMBER_DEEP),
        (9, 23, HONEY_AMBER_GOLD), (9, 24, HONEY_AMBER_GOLD), (9, 25, HONEY_AMBER_MID), (9, 26, HONEY_AMBER_DEEP),
        (10, 23, HONEY_AMBER_BRIGHT), (10, 24, HONEY_AMBER_MID),
        # Body back drips (x in 16..24, y in 19..25)
        (17, 19, HONEY_AMBER_GOLD), (18, 19, HONEY_AMBER_GOLD), (18, 20, HONEY_AMBER_MID),
        (20, 19, HONEY_AMBER_GOLD), (21, 19, HONEY_AMBER_BRIGHT), (21, 20, HONEY_AMBER_GOLD), (21, 21, HONEY_AMBER_DEEP),
        (22, 19, HONEY_AMBER_GOLD), (22, 20, HONEY_AMBER_MID)
    ])

    # Arms drips
    for y in range(16, 21):
        for x in (38, 39, 42, 43, 52, 53, 56, 57):
            drips_dense.append((x, y, HONEY_AMBER_GOLD if (x + y) % 2 == 0 else HONEY_AMBER_BRIGHT))
    drips_dense.extend([
        (38, 21, HONEY_AMBER_MID), (38, 22, HONEY_AMBER_DEEP),
        (43, 21, HONEY_AMBER_GOLD), (43, 22, HONEY_AMBER_MID), (43, 23, HONEY_AMBER_DEEP),
        (52, 21, HONEY_AMBER_GOLD), (52, 22, HONEY_AMBER_MID), (52, 23, HONEY_AMBER_DEEP),
        (57, 21, HONEY_AMBER_MID), (57, 22, HONEY_AMBER_DEEP)
    ])

    # Legs drips
    drips_dense.extend([
        (5, 27, HONEY_AMBER_GOLD), (5, 28, HONEY_AMBER_MID), (5, 29, HONEY_AMBER_DEEP),
        (8, 27, HONEY_AMBER_GOLD), (8, 28, HONEY_AMBER_DEEP),
        (21, 27, HONEY_AMBER_GOLD), (21, 28, HONEY_AMBER_MID), (21, 29, HONEY_AMBER_DEEP),
        (24, 27, HONEY_AMBER_GOLD), (24, 28, HONEY_AMBER_DEEP)
    ])

    # Apply ONLY to translucent honey layer overlay
    for (x, y, col) in drips_dense:
        honey_layer.putpixel((x, y), (col[0], col[1], col[2], 255))
        for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < w and 0 <= ny < h and honey_layer.getpixel((nx, ny))[3] == 0:
                honey_layer.putpixel((nx, ny), (col[0], col[1], col[2], 180))

    golem_img.save(BEEKEEPER_PATH)
    eyes_img.save(EYES_PATH)
    honey_layer.save(HONEY_LAYER_PATH)
    print(f"Generated {BEEKEEPER_PATH}, {EYES_PATH}, {HONEY_LAYER_PATH}")

    # Honey spit texture (64x32 like llama spit)
    spit = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    for y in range(0, 10):
        for x in range(0, 18):
            if (x in (0, 17) or y in (0, 9)):
                spit.putpixel((x, y), (217, 119, 6, 220))
            elif (x in (1, 16) or y in (1, 8)):
                spit.putpixel((x, y), (245, 158, 11, 240))
            else:
                spit.putpixel((x, y), (253, 230, 138, 255))
    spit.save(HONEY_SPIT_PATH)
    print(f"Generated {HONEY_SPIT_PATH}")

if __name__ == "__main__":
    main()
