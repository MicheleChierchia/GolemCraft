"""
generate_totem_golem.py
Generates totem_golem.png and totem_golem_eyes.png matching Minecraft Totem of Undying aesthetic.
"""

import os, colorsys
from PIL import Image

ROOT_DIR = os.path.dirname(os.path.abspath(__file__))
TEXTURE_DIR = os.path.join(ROOT_DIR, "src/main/resources/assets/golemcraft/textures/entity")
COPPER_BASE = os.path.join(ROOT_DIR, "copper golem/copper_golem.png")

TOTEM_GOLEM_PATH = os.path.join(TEXTURE_DIR, "totem_golem.png")
TOTEM_EYES_PATH = os.path.join(TEXTURE_DIR, "totem_golem_eyes.png")

def main():
    base = Image.open(COPPER_BASE).convert("RGBA")
    w, h = base.size
    totem_img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    eyes_img = Image.new("RGBA", (w, h), (0, 0, 0, 0))

    # Eye positions in 64x64 UV:
    # x in [11, 12] and [15, 16], y in [12, 13]
    eye_pixels = {
        (11, 12), (12, 12), (15, 12), (16, 12),
        (11, 13), (12, 13), (15, 13), (16, 13)
    }

    # Emerald eye colors
    EMERALD_OUTER = (0, 187, 70, 255)    # #00bb46
    EMERALD_INNER = (65, 230, 127, 255)  # #41e67f
    EMERALD_BRIGHT = (165, 255, 190, 255)

    for y in range(h):
        for x in range(w):
            r, g, b, a = base.getpixel((x, y))
            if a == 0:
                continue

            if (x, y) in eye_pixels:
                if (x in (12, 15) and y == 13):
                    col = EMERALD_BRIGHT
                elif (x in (12, 15)):
                    col = EMERALD_INNER
                else:
                    col = EMERALD_OUTER
                totem_img.putpixel((x, y), col)
                eyes_img.putpixel((x, y), col)
                continue

            # Convert copper orange/brown to golden totem hue
            # Copper hue is around 0.04-0.08 (15°-30°).
            # Gold hue in totem is around 0.11-0.14 (40°-50°).
            rf, gf, bf = r / 255.0, g / 255.0, b / 255.0
            hue, sat, val = colorsys.rgb_to_hsv(rf, gf, bf)

            # Map hue towards golden yellow (~0.12)
            new_hue = 0.12 + (hue - 0.06) * 0.4
            new_sat = min(1.0, sat * 1.05 + 0.05)
            new_val = min(1.0, val * 1.08)

            nr, ng, nb = colorsys.hsv_to_rgb(new_hue, new_sat, new_val)
            totem_img.putpixel((x, y), (int(nr * 255), int(ng * 255), int(nb * 255), a))

    totem_img.save(TOTEM_GOLEM_PATH)
    eyes_img.save(TOTEM_EYES_PATH)
    print(f"Generated {TOTEM_GOLEM_PATH} and {TOTEM_EYES_PATH}")

if __name__ == "__main__":
    main()
