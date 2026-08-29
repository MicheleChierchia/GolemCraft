from PIL import Image, ImageDraw

img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# Un bel diamante / ottaedro dorato e ramato brillante
points = [(8, 1), (14, 8), (8, 15), (2, 8)]
draw.polygon(points, fill=(255, 170, 50, 200))

# Faccia illuminata interna (per renderlo sfaccettato 3D)
points_inner = [(8, 1), (14, 8), (8, 15), (8, 8)]
draw.polygon(points_inner, fill=(255, 200, 100, 255))

points_inner_left = [(8, 1), (8, 15), (2, 8), (8, 8)]
draw.polygon(points_inner_left, fill=(200, 120, 30, 255))

# Highlights (magia)
draw.point((8, 2), fill=(255, 255, 255, 255))
draw.point((13, 8), fill=(255, 255, 255, 255))
draw.line((7, 4, 9, 4), fill=(255, 255, 200, 200))
draw.line((8, 3, 8, 5), fill=(255, 255, 200, 200))

import os
img.save("/home/miky/IdeaProjects/GolemCraft/src/main/resources/assets/golemcraft/textures/block/golem_beacon_core.png")
print("Generated better core")
