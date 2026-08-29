from PIL import Image, ImageDraw

img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# Core background glow (dark copperish/yellow)
for x in range(2, 14):
    for y in range(2, 14):
        draw.point((x, y), fill=(255, 180, 50, 150))

# Inner box (brighter)
draw.rectangle([4, 4, 11, 11], fill=(255, 200, 100, 200))

# Golem Face
# Eye left
draw.rectangle([5, 6, 6, 6], fill=(255, 255, 255, 255))
# Eye right
draw.rectangle([9, 6, 10, 6], fill=(255, 255, 255, 255))
# Nose
draw.rectangle([7, 7, 8, 9], fill=(200, 120, 50, 255))
# Brow line
draw.line((4, 5, 11, 5), fill=(100, 60, 20, 255))

import os
os.makedirs("/home/miky/IdeaProjects/GolemCraft/src/main/resources/assets/golemcraft/textures/block/", exist_ok=True)
img.save("/home/miky/IdeaProjects/GolemCraft/src/main/resources/assets/golemcraft/textures/block/golem_beacon_core.png")
print("Generated golem_beacon_core.png")
