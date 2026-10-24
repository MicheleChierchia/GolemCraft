"""
generate_leveler_assets.py

Generates textures for the Golem Spianatore (Leveler Golem) and the Area Marker block/item.
"""

import os
from PIL import Image, ImageDraw

ROOT_DIR = os.path.dirname(os.path.abspath(__file__))
ENTITY_DIR = os.path.join(ROOT_DIR, "src/main/resources/assets/golemcraft/textures/entity")
BLOCK_DIR = os.path.join(ROOT_DIR, "src/main/resources/assets/golemcraft/textures/block")
ITEM_DIR = os.path.join(ROOT_DIR, "src/main/resources/assets/golemcraft/textures/item")

os.makedirs(ENTITY_DIR, exist_ok=True)
os.makedirs(BLOCK_DIR, exist_ok=True)
os.makedirs(ITEM_DIR, exist_ok=True)

BASE_GOLEM_PATH = os.path.join(ENTITY_DIR, "base_golem.png")

def create_leveler_golem_texture():
    base = Image.open(BASE_GOLEM_PATH).convert("RGBA")
    w, h = base.size
    tex = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    p_base = base.load()
    pixels = tex.load()

    # -------------------------------------------------------------
    # 1. OTTONE / BRASS SKIN
    # Map the iron/copper gray tones of base_golem to a rich golden brass alloy
    # -------------------------------------------------------------
    for y in range(h):
        for x in range(w):
            r, g, b, a = p_base[x, y]
            if a > 0:
                v = (r + g + b) // 3
                r_brass = min(255, int(v * 1.08 + 26))
                g_brass = min(255, int(v * 0.84 + 10))
                b_brass = min(255, int(v * 0.35 + 2))
                pixels[x, y] = (r_brass, g_brass, b_brass, a)

    # -------------------------------------------------------------
    # 2. BLUE WORK OVERALLS (Salopette da lavoro blu sul busto)
    # Uniform solid denim blue with subtle seams and brass buttons (no checkerboard)
    # Body UV: texOffs(0, 15), size 8x6x6
    #   Top:   x in 6..14, y in 15..21
    #   Front: x in 6..14, y in 21..27
    #   Right: x in 0..6,  y in 21..27
    #   Left:  x in 14..20,y in 21..27
    #   Back:  x in 20..28,y in 21..27
    # -------------------------------------------------------------
    DENIM_MAIN   = (44, 90, 152, 255)  # Solid rich work denim
    DENIM_LGT    = (56, 108, 175, 255) # Top edge highlight
    DENIM_DARK   = (30, 64, 114, 255)  # Seams, borders, shadow
    BRASS_BUCKLE = (245, 202, 60, 255)  # Metallic buckle
    BUCKLE_DARK  = (175, 135, 25, 255)  # Buckle shadow

    # Shoulder straps on top of body (y in 15..21, x at 7,8 and 11,12)
    for y in range(15, 21):
        pixels[7, y] = DENIM_MAIN
        pixels[8, y] = DENIM_DARK
        pixels[11, y] = DENIM_DARK
        pixels[12, y] = DENIM_MAIN

    # Front bib (x in 6..14, y in 21..27)
    # Waistband across the bottom (y=25..26, x in 6..14)
    for y in (25, 26):
        for x in range(6, 14):
            pixels[x, y] = DENIM_MAIN
    for x in range(6, 14):
        pixels[x, 26] = DENIM_DARK # Bottom waist seam

    # Front central bib (x in 7..13, y in 22..25)
    for y in range(22, 25):
        for x in range(7, 13):
            pixels[x, y] = DENIM_MAIN

    # Top border of the bib
    for x in range(7, 13):
        pixels[x, 22] = DENIM_LGT

    # Side edges of the bib
    for y in range(22, 26):
        pixels[7, y] = DENIM_DARK
        pixels[12, y] = DENIM_DARK

    # Front bib pocket (x in 8..11, y in 23..24)
    pixels[8, 23] = DENIM_DARK
    pixels[9, 23] = DENIM_DARK
    pixels[10, 23] = DENIM_DARK
    pixels[11, 23] = DENIM_DARK
    pixels[8, 24] = DENIM_LGT
    pixels[9, 24] = DENIM_MAIN
    pixels[10, 24] = DENIM_MAIN
    pixels[11, 24] = DENIM_DARK

    # Brass suspender buckles on front (y=21, x at 7 and 12)
    pixels[7, 21] = BRASS_BUCKLE
    pixels[8, 21] = BUCKLE_DARK
    pixels[11, 21] = BUCKLE_DARK
    pixels[12, 21] = BRASS_BUCKLE

    # Sides of torso (x in 0..6 and x in 14..20): solid blue waist at y=25..26
    for y in (25, 26):
        for x in range(0, 6):
            pixels[x, y] = DENIM_MAIN if y == 25 else DENIM_DARK
        for x in range(14, 20):
            pixels[x, y] = DENIM_MAIN if y == 25 else DENIM_DARK

    # Back of torso (x in 20..28, y in 21..27): criss-cross suspenders & waistband
    for y in (25, 26):
        for x in range(20, 28):
            pixels[x, y] = DENIM_MAIN if y == 25 else DENIM_DARK
    # Cross suspenders on back
    pixels[22, 21] = DENIM_MAIN
    pixels[25, 21] = DENIM_MAIN
    pixels[23, 22] = DENIM_MAIN
    pixels[24, 22] = DENIM_MAIN
    pixels[24, 23] = DENIM_MAIN
    pixels[23, 23] = DENIM_MAIN
    pixels[22, 24] = DENIM_MAIN
    pixels[25, 24] = DENIM_MAIN

    # -------------------------------------------------------------
    # 3. DARK GREEN WORK PANTS (Pantaloni verde scuro uniformi)
    # Right leg: x in 0..16,  y in 27..33
    # Left leg:  x in 16..32, y in 27..33
    # -------------------------------------------------------------
    PANTS_MAIN = (42, 86, 52, 255)  # Solid dark work forest green
    PANTS_LGT  = (54, 106, 66, 255) # Front crease / knee highlight
    PANTS_DARK = (30, 64, 38, 255)  # Seams, cuffs, inner shadow

    # Base uniform pants color across all leg surfaces
    for y in range(27, 34):
        for x in range(0, 32):
            pixels[x, y] = PANTS_MAIN

    # Top waist seam shadow
    for x in range(0, 32):
        pixels[x, 27] = PANTS_DARK

    # Bottom cuff shadow
    for x in range(0, 32):
        pixels[x, 33] = PANTS_DARK

    # Front knee / vertical crease highlights
    for y in range(28, 33):
        pixels[6, y] = PANTS_LGT   # Right leg front crease
        pixels[22, y] = PANTS_LGT  # Left leg front crease

    # Outer side seams
    for y in range(28, 33):
        pixels[0, y] = PANTS_DARK
        pixels[15, y] = PANTS_DARK
        pixels[16, y] = PANTS_DARK
        pixels[31, y] = PANTS_DARK

    # -------------------------------------------------------------
    # 4. RED WORK BOOTS (Stivali da lavoro rossi uniformi)
    # Lower leg: y in 34..36
    # 3D Boot layer: texOffs(0, 54) and (24, 54), y in 54..64
    # -------------------------------------------------------------
    BOOT_MAIN = (162, 40, 40, 255)  # Solid rich work boot red
    BOOT_LGT  = (188, 52, 52, 255)  # Toe highlight
    BOOT_DARK = (122, 28, 28, 255)  # Seam / heel shadow
    BOOT_SOLE = (45, 24, 24, 255)   # Heavy dark sole

    # Lower leg boot base
    for y in (34, 35):
        for x in range(0, 32):
            pixels[x, y] = BOOT_MAIN
    # Front toe highlight on legs
    pixels[6, 35] = BOOT_LGT
    pixels[7, 35] = BOOT_LGT
    pixels[22, 35] = BOOT_LGT
    pixels[23, 35] = BOOT_LGT

    # Ankle seam
    for x in range(0, 32):
        pixels[x, 34] = BOOT_DARK

    # Bottom of feet on leg texture (y=36 and soles at y in 27..31)
    for x in range(0, 32):
        pixels[x, 36] = BOOT_SOLE
    for y in range(27, 31):
        for x in range(4, 8):
            pixels[x, y] = BOOT_SOLE
        for x in range(20, 24):
            pixels[x, y] = BOOT_SOLE

    # 3D Boot layer: texOffs(0, 54) for right, texOffs(24, 54) for left
    for y in range(54, 63):
        for x in range(0, 48):
            pixels[x, y] = BOOT_MAIN

    # Top collar rim of 3D boots (y=54)
    for x in range(0, 48):
        pixels[x, 54] = BOOT_DARK

    # Front toe highlights on 3D boots (front face: x in 4..8 and 28..32, y in 59..62)
    for y in (60, 61):
        for x in range(4, 8):
            pixels[x, y] = BOOT_LGT
        for x in range(28, 32):
            pixels[x, y] = BOOT_LGT

    # Heavy bottom sole of 3D boots (y=63)
    for x in range(0, 48):
        pixels[x, 63] = BOOT_SOLE

    # Clear old helmet rows (40..53) completely
    for y in range(40, 54):
        for x in range(0, 64):
            pixels[x, y] = (0, 0, 0, 0)

    out_path = os.path.join(ENTITY_DIR, "leveler_golem.png")
    tex.save(out_path)
    print(f"Saved leveler golem texture to: {out_path}")

    out_path = os.path.join(ENTITY_DIR, "leveler_golem.png")
    tex.save(out_path)
    print(f"Saved leveler golem texture to: {out_path}")


def create_marker_textures():
    COPPER_LGT = (205, 115, 75, 255)
    COPPER_MID = (180, 95, 60, 255)
    COPPER_DRK = (145, 70, 40, 255)
    COPPER_BASE_COLOR = (120, 55, 30, 255)

    def draw_shaft(pixels):
        # Base ring (y=14..15, x=5..10)
        for y in (14, 15):
            for x in range(5, 11):
                pixels[x, y] = COPPER_BASE_COLOR if (x == 5 or x == 10) else COPPER_MID

        # Shaft (y=7..13, x=7..8)
        for y in range(7, 14):
            pixels[7, y] = COPPER_LGT
            pixels[8, y] = COPPER_MID
            if y % 3 == 0:
                pixels[6, y] = COPPER_DRK
                pixels[9, y] = COPPER_DRK

        # Collar (y=6, x=6..9)
        for x in range(6, 10):
            pixels[x, 6] = COPPER_LGT if x in (6, 7) else COPPER_MID

    # 1. UNLIT MARKER (Dimmed / waiting tip)
    unlit_img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p_unlit = unlit_img.load()
    draw_shaft(p_unlit)

    UNLIT_CORE = (65, 80, 70, 255)
    UNLIT_MID  = (40, 55, 45, 255)
    UNLIT_DRK  = (25, 35, 30, 255)

    for y in range(1, 6):
        for x in range(6, 10):
            p_unlit[x, y] = UNLIT_MID
    p_unlit[7, 2] = UNLIT_CORE
    p_unlit[8, 2] = UNLIT_MID
    p_unlit[6, 3] = UNLIT_DRK
    p_unlit[9, 3] = UNLIT_DRK
    p_unlit[6, 4] = UNLIT_DRK
    p_unlit[9, 4] = UNLIT_DRK
    p_unlit[7, 5] = UNLIT_DRK
    p_unlit[8, 5] = UNLIT_DRK

    unlit_path = os.path.join(BLOCK_DIR, "area_marker.png")
    unlit_img.save(unlit_path)
    print(f"Saved unlit area marker texture to: {unlit_path}")

    # 2. LIT MARKER (Glowing bright green neon tip)
    lit_img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    p_lit = lit_img.load()
    draw_shaft(p_lit)

    GREEN_CORE = (235, 255, 240, 255) # Pure shining center
    GREEN_BRT  = (50, 255, 130, 255)  # Vivid neon green
    GREEN_MID  = (20, 220, 95, 255)
    GREEN_DRK  = (10, 160, 65, 255)

    for y in range(1, 6):
        for x in range(6, 10):
            p_lit[x, y] = GREEN_MID

    p_lit[7, 2] = GREEN_CORE
    p_lit[8, 2] = GREEN_CORE
    p_lit[7, 3] = GREEN_BRT
    p_lit[8, 3] = GREEN_BRT
    p_lit[7, 1] = GREEN_CORE
    p_lit[8, 1] = GREEN_BRT
    p_lit[6, 2] = GREEN_BRT
    p_lit[9, 2] = GREEN_BRT
    p_lit[6, 3] = GREEN_DRK
    p_lit[9, 3] = GREEN_DRK
    p_lit[6, 4] = GREEN_DRK
    p_lit[9, 4] = GREEN_DRK
    p_lit[7, 5] = GREEN_DRK
    p_lit[8, 5] = GREEN_DRK

    lit_path = os.path.join(BLOCK_DIR, "area_marker_lit.png")
    lit_img.save(lit_path)
    print(f"Saved lit area marker texture to: {lit_path}")

    # Item texture uses the lit glowing version
    item_path = os.path.join(ITEM_DIR, "area_marker.png")
    lit_img.save(item_path)
    print(f"Saved area marker item texture to: {item_path}")

if __name__ == "__main__":
    create_leveler_golem_texture()
    create_marker_textures()
