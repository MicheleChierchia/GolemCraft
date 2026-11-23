import os
from PIL import Image

ROOT_DIR = os.path.dirname(os.path.abspath(__file__))
COPPER_BASE = os.path.join(ROOT_DIR, "copper golem", "copper_golem.png")
TEXTURE_DIR = os.path.join(ROOT_DIR, "src/main/resources/assets/golemcraft/textures/entity")

EYE_PIXELS = {
    (11, 12), (12, 12), (15, 12), (16, 12),
    (11, 13), (12, 13), (15, 13), (16, 13),
}

# Ocean Monument palette ramps (darkest -> brightest)
PRISMARINE_RAMP = [
    (32, 62, 56),
    (46, 89, 80),
    (66, 124, 111),
    (89, 158, 141),
    (118, 194, 173),
    (156, 224, 203),
]

DARK_PRISMARINE_RAMP = [
    (22, 40, 37),
    (31, 56, 51),
    (42, 74, 67),
    (56, 97, 87),
    (75, 125, 112),
]

SEA_LANTERN_FRAME = (118, 186, 177, 255)
SEA_LANTERN_MID = (176, 237, 227, 255)
SEA_LANTERN_CORE = (232, 255, 249, 255)
EYE_BRIGHT = (214, 255, 247, 255)
EYE_GLOW = (138, 242, 222, 255)


def sample_ramp(ramp, t):
    t = max(0.0, min(1.0, t))
    scaled = t * (len(ramp) - 1)
    idx = int(scaled)
    frac = scaled - idx
    if idx >= len(ramp) - 1:
        return ramp[-1]
    c0 = ramp[idx]
    c1 = ramp[idx + 1]
    return (
        round(c0[0] + (c1[0] - c0[0]) * frac),
        round(c0[1] + (c1[1] - c0[1]) * frac),
        round(c0[2] + (c1[2] - c0[2]) * frac),
    )


def paint_sea_lantern_face(px, x0, y0, w, h):
    for dy in range(h):
        for dx in range(w):
            if dx == 0 or dy == 0 or dx == w - 1 or dy == h - 1:
                px[x0 + dx, y0 + dy] = SEA_LANTERN_FRAME
            elif dx == 1 and dy == 1 and w >= 4 and h >= 4:
                px[x0 + dx, y0 + dy] = SEA_LANTERN_CORE
            elif (dx in (1, 2) and dy in (1, 2)):
                px[x0 + dx, y0 + dy] = SEA_LANTERN_CORE if (dx + dy) % 2 == 0 else SEA_LANTERN_MID
            else:
                px[x0 + dx, y0 + dy] = SEA_LANTERN_MID


def main():
    os.makedirs(TEXTURE_DIR, exist_ok=True)
    base = Image.open(COPPER_BASE).convert("RGBA")
    out = Image.new("RGBA", base.size, (0, 0, 0, 0))
    bpx = base.load()
    opx = out.load()

    for y in range(base.height):
        for x in range(base.width):
            r, g, b, a = bpx[x, y]
            if a == 0:
                continue
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0

            # Dark prismarine for antenna stem, nose, and lower leg boots
            is_stem = 37 <= x <= 44 and 8 <= y <= 13
            is_nose = 56 <= x <= 63 and 0 <= y <= 4
            is_boots = (0 <= x <= 31 and 32 <= y <= 35)
            is_trim = (0 <= x <= 27 and y in (15, 25, 26))

            if is_stem or is_nose or is_boots or is_trim:
                nr, ng, nb = sample_ramp(DARK_PRISMARINE_RAMP, lum * 1.15)
            else:
                nr, ng, nb = sample_ramp(PRISMARINE_RAMP, lum * 1.1)
            opx[x, y] = (nr, ng, nb, a)

    # Paint Sea Lantern 4x4x4 cube at texOffs(37, 0): top (41..44, 0..3), bottom (45..48, 0..3), sides (37..52, 4..7)
    paint_sea_lantern_face(opx, 41, 0, 4, 4)
    paint_sea_lantern_face(opx, 45, 0, 4, 4)
    for sx in (37, 41, 45, 49):
        paint_sea_lantern_face(opx, sx, 4, 4, 4)

    # Paint emissive aquamarine eyes
    for ex, ey in EYE_PIXELS:
        opx[ex, ey] = EYE_BRIGHT if ey == 12 else EYE_GLOW

    marine_path = os.path.join(TEXTURE_DIR, "marine_golem.png")
    out.save(marine_path)
    print(f"[ok] {marine_path}")

    # Emissive overlay: eyes + Sea Lantern
    eyes = Image.new("RGBA", base.size, (0, 0, 0, 0))
    epx = eyes.load()
    for ex, ey in EYE_PIXELS:
        epx[ex, ey] = opx[ex, ey]
    for y in range(0, 8):
        for x in range(37, 53):
            if opx[x, y][3] > 0:
                epx[x, y] = opx[x, y]

    eyes_path = os.path.join(TEXTURE_DIR, "marine_golem_eyes.png")
    eyes.save(eyes_path)
    print(f"[ok] {eyes_path}")


if __name__ == "__main__":
    main()
