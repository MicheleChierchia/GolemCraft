"""
generate_alchemist_assets.py
Generates alchemist_golem.png, alchemist_golem_eyes.png, and alchemist_golem_robe.png.
Includes:
- Cool neutral gray skin converted from copper_golem.png
- Antenna pixels cleared (replaced by 4-tier witch hat in y=36..61)
- Amethyst violet glowing eyes
- Nose wart mole UV at (48..52, 50..52)
- Dedicated 64x64 witch robe overlay texture
"""

import os
from PIL import Image

ROOT_DIR = os.path.dirname(os.path.abspath(__file__))
TEXTURE_DIR = os.path.join(ROOT_DIR, "src/main/resources/assets/golemcraft/textures/entity")
COPPER_BASE = os.path.join(ROOT_DIR, "copper golem/copper_golem.png")

ALCHEMIST_PATH = os.path.join(TEXTURE_DIR, "alchemist_golem.png")
EYES_PATH = os.path.join(TEXTURE_DIR, "alchemist_golem_eyes.png")
ROBE_PATH = os.path.join(TEXTURE_DIR, "alchemist_golem_robe.png")

os.makedirs(TEXTURE_DIR, exist_ok=True)


def fill_rect(img, x0, y0, x1, y1, color_fn):
    for y in range(y0, y1):
        for x in range(x0, x1):
            img.putpixel((x, y), color_fn(x, y))


def main():
    base = Image.open(COPPER_BASE).convert("RGBA")
    w, h = base.size
    golem_img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    for y in range(h):
        for x in range(w):
            r, g, b, a = base.getpixel((x, y))
            if a == 0:
                continue
            lum = 0.299 * r + 0.587 * g + 0.114 * b
            gray = 0.65 * lum + 0.35 * max(r, g, b)
            nr = max(0, min(255, round(gray * 0.97)))
            ng = max(0, min(255, round(gray * 0.99)))
            nb = max(0, min(255, round(gray * 1.02)))
            golem_img.putpixel((x, y), (nr, ng, nb, a))
    eyes_img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    robe_img = Image.new("RGBA", (w, h), (0, 0, 0, 0))

    # Clear lightning rod antenna UVs while keeping nose UVs (x >= 56).
    for y in range(0, 15):
        for x in range(37, 55):
            golem_img.putpixel((x, y), (0, 0, 0, 0))

    # Glowing mystic amethyst/violet eyes.
    eye_pixels = {
        (11, 12), (12, 12), (15, 12), (16, 12),
        (11, 13), (12, 13), (15, 13), (16, 13),
    }
    VIOLET_DARK = (139, 58, 224, 255)
    VIOLET_MID = (176, 102, 255, 255)
    VIOLET_BRIGHT = (232, 198, 255, 255)

    for (x, y) in eye_pixels:
        if x in (12, 15) and y == 13:
            col = VIOLET_BRIGHT
        elif x in (12, 15):
            col = VIOLET_MID
        else:
            col = VIOLET_DARK
        eyes_img.putpixel((x, y), col)

    # Witch hat palette.
    HAT_DARK = (27, 20, 36, 255)
    HAT_MID = (36, 28, 46, 255)
    HAT_LIGHT = (46, 36, 59, 255)
    BAND_DARK = (24, 108, 62, 255)
    BAND_LIGHT = (40, 152, 86, 255)
    GOLD_DARK = (196, 148, 28, 255)
    GOLD_BRIGHT = (244, 206, 72, 255)

    def hat_shade(x, y):
        if (x + y) % 5 == 0:
            return HAT_LIGHT
        if (x + y) % 3 == 0:
            return HAT_DARK
        return HAT_MID

    # hat brim: texOffs(0, 36), size (10, 2, 12) -> x: 0..44, y: 36..50
    def brim_color(x, y):
        if y == 49:
            return HAT_DARK
        if y in (36, 47, 48):
            return HAT_LIGHT if (x % 2 == 0) else HAT_MID
        return hat_shade(x, y)

    fill_rect(golem_img, 0, 36, 44, 50, brim_color)

    # hat2 crown: texOffs(0, 50), size (7, 4, 7) -> x: 0..28, y: 50..61
    def hat2_color(x, y):
        if 59 <= y <= 60:
            if 9 <= x <= 11 and y == 59:
                return GOLD_BRIGHT if x != 10 else HAT_DARK
            if 9 <= x <= 11 and y == 60:
                return GOLD_DARK
            return BAND_LIGHT if y == 59 else BAND_DARK
        if 9 <= x <= 11 and y == 58:
            return GOLD_BRIGHT
        return hat_shade(x, y)

    fill_rect(golem_img, 0, 50, 28, 61, hat2_color)

    # hat3 mid cone: texOffs(28, 50), size (4, 4, 4) -> x: 28..44, y: 50..58
    fill_rect(golem_img, 28, 50, 44, 58, hat_shade)

    # hat4 tip: texOffs(44, 50), size (1, 2, 1) -> x: 44..48, y: 50..53
    fill_rect(golem_img, 44, 50, 48, 53, hat_shade)

    # Nose wart mole: texOffs(48, 50), size (1, 1, 1) -> x: 48..52, y: 50..52
    WART_DARK = (73, 84, 55, 255)
    WART_LIGHT = (94, 107, 71, 255)
    fill_rect(golem_img, 48, 50, 52, 52, lambda x, y: WART_LIGHT if (x + y) % 2 == 0 else WART_DARK)

    # Robe layer texture (64x64).
    ROBE_DARK = (44, 22, 64, 255)
    ROBE_MID = (59, 31, 84, 255)
    ROBE_LIGHT = (75, 39, 107, 255)
    ROBE_SEAM = (30, 14, 46, 255)
    LEATHER_DARK = (72, 44, 26, 255)
    LEATHER_MID = (98, 62, 38, 255)
    VIAL_CRIMSON = (214, 54, 82, 255)
    VIAL_AQUA = (74, 212, 224, 255)

    def robe_cloth(x, y):
        if (x + y) % 4 == 0:
            return ROBE_LIGHT
        if (x + y) % 3 == 0:
            return ROBE_DARK
        return ROBE_MID

    # Body coat: texOffs(0, 0), size (8, 8, 6) -> x: 0..28, y: 0..14
    def body_coat_color(x, y):
        if y < 6:
            return robe_cloth(x, y)
        # Side faces: y in 6..13; front face is x in 6..13
        if y == 13:
            if (x % 3 == 0):
                return GOLD_DARK
            return BAND_LIGHT if x % 2 == 0 else BAND_DARK
        if y == 10:
            if 9 <= x <= 10:
                return GOLD_BRIGHT
            return LEATHER_MID if x % 2 == 0 else LEATHER_DARK
        if y == 11:
            if x == 7:
                return VIAL_CRIMSON
            if x == 12:
                return VIAL_AQUA
            if 9 <= x <= 10:
                return GOLD_DARK
        if 9 <= x <= 10 and 6 <= y <= 12:
            return ROBE_SEAM
        return robe_cloth(x, y)

    fill_rect(robe_img, 0, 0, 28, 14, body_coat_color)

    # Body collar: texOffs(0, 16), size (9, 2, 7) -> x: 0..32, y: 16..25
    def collar_color(x, y):
        if y == 24:
            return GOLD_DARK if x % 4 == 0 else BAND_DARK
        if y >= 23:
            return ROBE_DARK if x % 2 == 0 else ROBE_SEAM
        return ROBE_DARK

    fill_rect(robe_img, 0, 16, 32, 25, collar_color)

    # Sleeves: right_arm texOffs(32, 0), left_arm texOffs(48, 0), size (3, 7, 4) -> width 14, height 11
    def sleeve_color(x, y):
        if y == 10:
            return GOLD_DARK if x % 3 == 0 else BAND_LIGHT
        if y == 9:
            return BAND_DARK
        return robe_cloth(x, y)

    fill_rect(robe_img, 32, 0, 46, 11, sleeve_color)
    fill_rect(robe_img, 48, 0, 62, 11, sleeve_color)

    golem_img.save(ALCHEMIST_PATH)
    eyes_img.save(EYES_PATH)
    robe_img.save(ROBE_PATH)
    print(f"Generated {ALCHEMIST_PATH}, {EYES_PATH}, {ROBE_PATH}")


if __name__ == "__main__":
    main()
