"""Render isometric 3D previews of the GolemCraft golems from their skin textures.

The geometry mirrors the Minecraft model definitions in
src/main/java/com/golemcraft/golemcraftmod/client/model/ and the box-UV layout
of net.minecraft.client.model.geom.ModelPart.Cube.

Usage: .venv/bin/python3 generate_golem_renders.py
Output: docs/images/*.png
"""

from __future__ import annotations

import math
from dataclasses import dataclass
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent
TEX = ROOT / "src/main/resources/assets/golemcraft/textures/entity"
OUT = ROOT / "docs/images"
OUT.mkdir(parents=True, exist_ok=True)

SCALE = 3          # supersampling factor
FACE_UNIT = 42     # pixels per model unit at 1x
YAW = math.radians(32)
PITCH = math.radians(16)

# face -> brightness multiplier
SHADE = {"up": 1.0, "west": 0.82, "north": 0.90, "east": 0.70, "south": 0.62, "down": 0.45}


@dataclass
class Box:
    tex: tuple[int, int]
    origin: tuple[float, float, float]
    size: tuple[float, float, float]


@dataclass
class Part:
    offset: tuple[float, float, float]
    boxes: list[Box]


def b(tex, ox, oy, oz, sx, sy, sz) -> Box:
    return Box(tex, (ox, oy, oz), (sx, sy, sz))


HEAD = b((0, 0), -4, -5, -5, 8, 5, 10)
ROD_THIN = b((37, 8), -1, -9, -1, 2, 4, 2)
ROD_TIP = b((37, 0), -2, -13, -2, 4, 4, 4)
NOSE = b((56, 0), -1, -3, -7, 2, 3, 2)
BODY = b((0, 15), -4, 0, -3, 8, 6, 6)
ARM_R = b((36, 16), -1.5, 0, -2, 3, 10, 4)
ARM_L = b((50, 16), -1.5, 0, -2, 3, 10, 4)
LEG_R = b((0, 27), -2, 0, -2, 4, 5, 4)
LEG_L = b((16, 27), -2, 0, -2, 4, 5, 4)
BOOT_R = b((0, 54), -2.2, 1.9, -2.2, 4.2, 3.2, 4.4)
BOOT_L = b((24, 54), -2.0, 1.9, -2.2, 4.2, 3.2, 4.4)


def base_parts(*head_extra: Box, body_extra: tuple[Box, ...] = (), boots: bool = False) -> list[Part]:
    right_leg = [LEG_R] + ([BOOT_R] if boots else [])
    left_leg = [LEG_L] + ([BOOT_L] if boots else [])
    return [
        Part((0, 13, 0), [HEAD, ROD_THIN, ROD_TIP, NOSE, *head_extra]),
        Part((0, 13, 0), [BODY, *body_extra]),
        Part((-5.5, 13, 0), [ARM_R]),
        Part((5.5, 13, 0), [ARM_L]),
        Part((-2, 19, 0), right_leg),
        Part((2, 19, 0), left_leg),
    ]


VARIANTS: dict[str, tuple[str, list[Part]]] = {
    "base": ("base_golem", base_parts()),
    "flower": ("flower_golem", base_parts()),
    "farmer": ("farmer_golem", base_parts(b((0, 40), -5, -4.5, -6, 10, 1, 12))),
    "fisherman": ("fisherman_golem", base_parts(b((0, 40), -4.5, -5.5, -5.5, 9, 2, 11))),
    "lumberjack": ("lumberjack_golem", base_parts(b((0, 40), -4, -4.5, -7, 8, 1, 2))),
    "soldier": ("soldier_golem", base_parts()),
    "depth": ("depth_golem", base_parts()),
    "explorer": (
        "explorer_golem",
        base_parts(
            b((0, 40), -4.5, -5.5, -5.5, 9, 2, 11),
            body_extra=(b((44, 40), -3.5, 0.5, 3, 7, 5, 2),),
            boots=True,
        ),
    ),
    "leveler": ("leveler_golem", base_parts(boots=True)),
    "totem": ("totem_golem", base_parts()),
}

OXIDATION = ["soldier_golem", "exposed_soldier_golem", "weathered_soldier_golem", "oxidized_soldier_golem"]


def faces(cube: Box):
    """Yield (direction, [(vertex3d, uv), ...]) using ModelPart.Cube's exact layout."""
    ox, oy, oz = cube.origin
    sx, sy, sz = cube.size
    u, v = cube.tex
    x0, y0, z0 = ox, oy, oz
    x1, y1, z1 = ox + sx, oy + sy, oz + sz

    t = [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)]
    l = [(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]
    u1, u2 = u + sz, u + sz + sx
    u22, u3 = u + sz + 2 * sx, u + sz + sx + sz
    v0, v1, v2 = v, v + sz, v + sz + sy

    yield "down", list(zip([l[1], l[0], t[0], t[1]], [(u2, v0), (u1, v0), (u1, v1), (u2, v1)]))
    yield "up", list(zip([t[2], t[3], l[3], l[2]], [(u22, v1), (u2, v1), (u2, v0), (u22, v0)]))
    yield "west", list(zip([t[0], l[0], l[3], t[3]], [(u1, v1), (u, v1), (u, v2), (u1, v2)]))
    yield "north", list(zip([t[1], t[0], t[3], t[2]], [(u2, v1), (u1, v1), (u1, v2), (u2, v2)]))
    yield "east", list(zip([l[1], t[1], t[2], l[2]], [(u3, v1), (u2, v1), (u2, v2), (u3, v2)]))
    yield "south", list(zip([l[0], l[1], l[2], l[3]], [(u3, v1), (u + sz + sx + sz + sx, v1), (u + sz + sx + sz + sx, v2), (u3, v2)]))


def project(p):
    x, y, z = p
    ca, sa = math.cos(YAW), math.sin(YAW)
    x1, z1 = x * ca + z * sa, -x * sa + z * ca
    cb, sb = math.cos(PITCH), math.sin(PITCH)
    y1 = y * cb - z1 * sb
    depth = y * sb + z1 * cb
    return x1 * FACE_UNIT, -y1 * FACE_UNIT, depth


def affine_from_triangle(dst, src):
    """Affine mapping from destination pixel coords to source UV."""
    (x0, y0), (x1, y1), (x2, y2) = dst
    (u0, v0), (u1, v1), (u2, v2) = src
    det = x0 * (y1 - y2) - x1 * (y0 - y2) + x2 * (y0 - y1)
    a0 = (u0 * (y1 - y2) - u1 * (y0 - y2) + u2 * (y0 - y1)) / det
    a1 = (x0 * (u1 - u2) - x1 * (u0 - u2) + x2 * (u0 - u1)) / det
    a2 = (u0 * (x1 * y2 - y1 * x2) + u1 * (y0 * x2 - x0 * y2) + u2 * (x0 * y1 - y0 * x1)) / det
    b0 = (v0 * (y1 - y2) - v1 * (y0 - y2) + v2 * (y0 - y1)) / det
    b1 = (x0 * (v1 - v2) - x1 * (v0 - v2) + x2 * (v0 - v1)) / det
    b2 = (v0 * (x1 * y2 - y1 * x2) + v1 * (y0 * x2 - x0 * y2) + v2 * (x0 * y1 - y0 * x1)) / det
    return (a0, a1, a2, b0, b1, b2)


def load_texture(name: str) -> Image.Image:
    image = Image.open(TEX / f"{name}.png").convert("RGBA")
    return image


def golem_faces(name: str, texture: str):
    tex = load_texture(texture)
    w, h = tex.size
    polygons = []
    for part in VARIANTS[name][1]:
        px, py, pz = part.offset
        for cube in part.boxes:
            shifted = Box(cube.tex, (cube.origin[0] + px, cube.origin[1] + py, cube.origin[2] + pz), cube.size)
            for direction, verts in faces(shifted):
                pts = []
                for (x, y, z), (su, sv) in verts:
                    sx, sy, depth = project((x, 24 - y, z))
                    pts.append(((sx, sy), (su / w, sv / h), depth))
                polygons.append((sum(p[2] for p in pts) / 4, direction, pts))
    return polygons, tex


def render(name: str, texture: str, supersample: int = SCALE) -> Image.Image:
    polygons, tex = golem_faces(name, texture)
    coords = [p[0] for _, _, pts in polygons for p in pts]
    min_x = min(c[0] for c in coords) - 14
    max_x = max(c[0] for c in coords) + 14
    min_y = min(c[1] for c in coords) - 14
    max_y = max(c[1] for c in coords) + 14

    canvas_w = int((max_x - min_x) * supersample)
    canvas_h = int((max_y - min_y) * supersample)
    canvas = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))

    def to_canvas(p):
        return ((p[0] - min_x) * supersample, (p[1] - min_y) * supersample)

    for depth, direction, pts in sorted(polygons, key=lambda item: item[0], reverse=True):
        screen = [to_canvas(p[0]) for p in pts]
        if abs((screen[1][0] - screen[0][0]) * (screen[2][1] - screen[0][1])
               - (screen[1][1] - screen[0][1]) * (screen[2][0] - screen[0][0])) < 0.01:
            continue
        uvs = [(p[1][0] * tex.width, p[1][1] * tex.height) for p in pts]
        pad = 2 * supersample
        bx = int(min(s[0] for s in screen)) - pad
        by = int(min(s[1] for s in screen)) - pad
        bw = int(max(s[0] for s in screen)) - bx + pad
        bh = int(max(s[1] for s in screen)) - by + pad
        local = [(s[0] - bx, s[1] - by) for s in screen]
        coeffs = affine_from_triangle([local[0], local[1], local[2]], [uvs[0], uvs[1], uvs[2]])
        face = tex.transform((bw, bh), Image.AFFINE, coeffs, resample=Image.NEAREST)
        shade = SHADE[direction]
        if shade != 1.0:
            rgb = face.convert("RGB").point(lambda c: int(c * shade))
            face = Image.merge("RGBA", (*rgb.split(), face.getchannel("A")))
        mask = Image.new("L", (bw, bh), 0)
        ImageDraw.Draw(mask).polygon(local, fill=255)
        mask = Image.composite(mask, Image.new("L", (bw, bh), 0), face.getchannel("A"))
        canvas.paste(face, (bx, by), mask)

    canvas = canvas.resize((canvas_w // supersample, canvas_h // supersample), Image.LANCZOS)
    return canvas


def fit(image: Image.Image, size: int, margin: float = 0.06) -> Image.Image:
    box = image.getbbox()
    image = image.crop(box)
    target = int(size * (1 - 2 * margin))
    scale = min(target / image.width, target / image.height)
    image = image.resize((max(1, int(image.width * scale)), max(1, int(image.height * scale))), Image.LANCZOS)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(image, ((size - image.width) // 2, (size - image.height) // 2), image)
    return out


def shadow(size: int, y: int, width: int) -> Image.Image:
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.ellipse(((size - width) // 2, y - width // 6, (size + width) // 2, y + width // 6), fill=(0, 0, 0, 90))
    return layer.filter(ImageFilter.GaussianBlur(width // 12))


def gallery(names, cols: int, cell: int, path: Path, bg: bool = False):
    rows = (len(names) + cols - 1) // cols
    width, height = cols * cell, rows * cell
    image = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    for i, name in enumerate(names):
        texture = VARIANTS[name][0]
        golem = fit(render(name, texture), int(cell * 0.92))
        x = (i % cols) * cell + (cell - golem.width) // 2
        y = (i // cols) * cell + (cell - golem.height) // 2
        if bg:
            image.alpha_composite(shadow(cell, int(0.88 * cell), int(0.42 * cell)), ((i % cols) * cell, (i // cols) * cell))
        image.alpha_composite(golem, (x, y))
    image.save(path)
    print(f"wrote {path.relative_to(ROOT)} {image.size}")


def hero(names, path: Path, width: int = 1600, height: int = 560):
    image = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    # soft warm vignette background
    bg = Image.new("RGB", (width, height), (22, 26, 32))
    glow = Image.new("RGB", (width, height), (0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse((-width // 4, -height, width + width // 4, height * 2), fill=(56, 74, 92))
    gd.ellipse((width // 5, height // 6, width * 4 // 5, height * 2), fill=(92, 74, 46))
    bg = Image.blend(bg, glow.filter(ImageFilter.GaussianBlur(140)), 0.55)
    image.alpha_composite(bg.convert("RGBA"))

    cell = width // len(names)
    golem_h = int(height * 0.82)
    for i, name in enumerate(names):
        golem = fit(render(name, VARIANTS[name][0]), golem_h, margin=0.02)
        x = i * cell + (cell - golem.width) // 2
        y = int(height * 0.94) - golem.height
        image.alpha_composite(shadow(golem.width, int(golem.height * 0.97), int(golem.width * 0.66)),
                              (x, y))
        image.alpha_composite(golem, (x, y))
    image.convert("RGB").save(path)
    print(f"wrote {path.relative_to(ROOT)} {image.size}")


def oxidation_strip(path: Path) -> None:
    cell = 300
    image = Image.new("RGBA", (cell * len(OXIDATION), cell), (0, 0, 0, 0))
    for i, texture in enumerate(OXIDATION):
        golem = fit(render("soldier", texture), int(cell * 0.92))
        image.alpha_composite(golem, (i * cell + (cell - golem.width) // 2, (cell - golem.height) // 2))
    image.save(path)
    print(f"wrote {path.relative_to(ROOT)} {image.size}")


if __name__ == "__main__":
    thumbs = OUT / "thumbs"
    thumbs.mkdir(exist_ok=True)
    for variant in VARIANTS:
        image = render(variant, VARIANTS[variant][0])
        fit(image, 384).save(OUT / f"golem_{variant}.png")
        fit(image, 128).save(thumbs / f"golem_{variant}.png")
        print(f"wrote golem_{variant}.png (+ thumb)")

    oxidation_strip(OUT / "oxidation.png")
    gallery(list(VARIANTS), 5, 360, OUT / "golems.png")
    hero(["base", "farmer", "lumberjack", "soldier", "depth", "explorer"], OUT / "hero.png")
