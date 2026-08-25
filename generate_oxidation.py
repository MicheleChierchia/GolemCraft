"""
generate_oxidation.py  (full HSV delta-transfer edition)

Strategy
--------
For each pixel we measure the FULL colour change (in HSV space) from the
copper golem base to each oxidation level:
    Δh = hue rotation   (how much the hue shifts, e.g. orange→teal)
    Δs = saturation Δ   (desaturation or boost)
    Δv = value Δ        (darker or lighter)

We apply those same per-pixel HSV deltas to the custom golem texture.
This transfers BOTH the spatial pattern (which pixels change) AND the
exact type of colour change (hue direction, saturation drop, brightness drop).

Usage
-----
    python3 generate_oxidation.py                   # all BASE_TEXTURES
    python3 generate_oxidation.py flower_golem      # single golem
"""

import os, sys, colorsys
from PIL import Image

TEXTURE_DIR = "src/main/resources/assets/golemcraft/textures/entity"
COPPER_DIR  = "copper golem"
COPPER_BASE = os.path.join(COPPER_DIR, "copper_golem.png")

LEVELS = {
    "exposed":   os.path.join(COPPER_DIR, "exposed_copper_golem.png"),
    "weathered": os.path.join(COPPER_DIR, "weathered_copper_golem.png"),
    "oxidized":  os.path.join(COPPER_DIR, "oxidized_copper_golem.png"),
}

BASE_TEXTURES = [
    "base_golem",
    "farmer_golem",
    "flower_golem",
    "soldier_golem",
    "fisherman_golem",
    "lumberjack_golem",
]

# Pixel rows to protect fully from oxidation per golem
PROTECTED_ROWS = {
    "farmer_golem": list(range(40, 53)),  # straw hat brim
    "fisherman_golem": list(range(40, 53)), # bucket hat
    "soldier_golem": list(range(8, 11)), # helmet sides and bandana
}

# Individual pixels to protect: { "golem_name": set of (x, y) }
def _farmer_hat_pixels():
    pts = set()
    # Hat cylinder sides: cols 10-17, rows 0-9
    for y in range(0, 10):
        for x in range(10, 18):
            pts.add((x, y))
    # Hat brim border: entire row 10
    for x in range(0, 64):
        pts.add((x, 10))
    return pts

def _soldier_helmet_pixels():
    pts = set()
    for y in range(0, 8):
        for x in range(8, 24):
            pts.add((x, y))
    # the leather strap on the body
    for x in range(20, 28):
        pts.add((x, 20 + (x - 20)))
    return pts

def _lumberjack_pixels():
    pts = set()
    # Beanie top
    for y in range(0, 10):
        for x in range(10, 18): pts.add((x, y)) # beanie top
    for y in range(10, 11):
        for x in range(0, 36): pts.add((x, y)) # beanie sides
    # (Beard removed)
    # Shirt, Pants, Boots (y 15 to 35 for body and legs, 16 to 26 for arms)
    for y in range(15, 36):
        for x in range(0, 32): pts.add((x, y)) # body and legs
    for y in range(16, 27):
        for x in range(36, 64): pts.add((x, y)) # arms shirt part
    # Beanie Fold (Brim)
    for y in range(40, 53):
        for x in range(0, 40): pts.add((x, y))
    return pts

PROTECTED_PIXELS = {
    "farmer_golem": _farmer_hat_pixels(),
    "soldier_golem": _soldier_helmet_pixels(),
    "lumberjack_golem": _lumberjack_pixels(),
}

# ── helpers ────────────────────────────────────────────────────────────────────

def rgb_to_hsv(r, g, b):
    return colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)

def hsv_to_rgb(h, s, v):
    r, g, b = colorsys.hsv_to_rgb(
        h % 1.0,
        max(0.0, min(1.0, s)),
        max(0.0, min(1.0, v)),
    )
    return (round(r * 255), round(g * 255), round(b * 255))

def hue_delta(h_base, h_ref):
    """Shortest-path hue delta in [-0.5, 0.5]."""
    d = h_ref - h_base
    if d > 0.5:  d -= 1.0
    if d < -0.5: d += 1.0
    return d

# ── build per-pixel HSV delta map ─────────────────────────────────────────────

def build_delta_map(copper_base_img, copper_ref_img, target_size):
    """
    Returns a list of (dh, ds, dv) per pixel of target_size.
    Transparent pixels in the copper base get (0, 0, 0).
    """
    cb = copper_base_img.resize(target_size, Image.NEAREST).convert("RGBA")
    cr = copper_ref_img.resize(target_size,  Image.NEAREST).convert("RGBA")

    deltas = []
    for (br, bg, bb, ba), (rr, rg, rb, ra) in zip(cb.getdata(), cr.getdata()):
        if ba == 0:
            deltas.append((0.0, 0.0, 0.0))
            continue
        bh, bs, bv = rgb_to_hsv(br, bg, bb)
        rh, rs, rv = rgb_to_hsv(rr, rg, rb)
        deltas.append((hue_delta(bh, rh), rs - bs, rv - bv))
    return deltas

# ── process one golem texture ──────────────────────────────────────────────────

def process_texture(base_name, delta_maps):
    src_path = os.path.join(TEXTURE_DIR, f"{base_name}.png")
    if not os.path.exists(src_path):
        print(f"  [skip] {src_path} not found")
        return

    img = Image.open(src_path).convert("RGBA")
    base_pixels = list(img.getdata())
    size = img.size

    for level_name, deltas in delta_maps.items():
        level_deltas = deltas[size]
        out_pixels = []

        for i, (r, g, b, a) in enumerate(base_pixels):
            if a == 0:
                out_pixels.append((0, 0, 0, 0))
                continue

            dh, ds, dv = level_deltas[i]
            h, s, v = rgb_to_hsv(r, g, b)
            nr, ng, nb = hsv_to_rgb(h + dh, s + ds, v + dv)
            out_pixels.append((nr, ng, nb, a))

        out_img = Image.new("RGBA", size)
        out_img.putdata(out_pixels)

        # Restore protected regions (e.g. non-copper parts like hats)
        if base_name in PROTECTED_ROWS or base_name in PROTECTED_PIXELS:
            base_src = Image.open(src_path).convert("RGBA")
            out_px   = out_img.load()
            base_px  = base_src.load()
            if base_name in PROTECTED_ROWS:
                for row in PROTECTED_ROWS[base_name]:
                    for x in range(size[0]):
                        out_px[x, row] = base_px[x, row]
            if base_name in PROTECTED_PIXELS:
                for (x, y) in PROTECTED_PIXELS[base_name]:
                    out_px[x, y] = base_px[x, y]

        if level_name == "oxidized":
            dead_eyes_path = os.path.join(TEXTURE_DIR, "died_golem_eyes.png")
            if os.path.exists(dead_eyes_path):
                dead_eyes_img = Image.open(dead_eyes_path).convert("RGBA")
                if dead_eyes_img.size != out_img.size:
                    dead_eyes_img = dead_eyes_img.resize(out_img.size, Image.NEAREST)
                out_img.alpha_composite(dead_eyes_img)

        out_path = os.path.join(TEXTURE_DIR, f"{level_name}_{base_name}.png")
        out_img.save(out_path)
        print(f"  [ok] {out_path}")

# ── main ───────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    targets = sys.argv[1:] if len(sys.argv) > 1 else BASE_TEXTURES
    targets = [t.replace(".png", "").replace(TEXTURE_DIR + "/", "").strip() for t in targets]

    # Collect all unique texture sizes
    sizes_needed = set()
    for name in targets:
        p = os.path.join(TEXTURE_DIR, f"{name}.png")
        if os.path.exists(p):
            sizes_needed.add(Image.open(p).size)

    # Load copper reference and build delta maps per level and size
    print("Loading copper golem reference textures…")
    copper_base_img = Image.open(COPPER_BASE)
    delta_maps = {}   # delta_maps[level_name][size] = list of (dh, ds, dv)
    for level_name, ref_path in LEVELS.items():
        copper_ref_img = Image.open(ref_path)
        delta_maps[level_name] = {}
        for sz in sizes_needed:
            delta_maps[level_name][sz] = build_delta_map(copper_base_img, copper_ref_img, sz)
        print(f"  [{level_name}] delta maps built for sizes: {sizes_needed}")

    for name in targets:
        print(f"\nProcessing: {name}")
        process_texture(name, delta_maps)

    print("\nDone.")
