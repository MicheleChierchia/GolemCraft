from PIL import Image, ImageDraw

img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# Copper Base
draw.rectangle([2, 10, 13, 14], fill=(184, 94, 47, 255))
draw.rectangle([3, 11, 12, 13], fill=(204, 114, 67, 255))

# Glass Shell (light blue semi-transparent)
draw.rectangle([3, 2, 12, 10], fill=(200, 240, 255, 120))
draw.rectangle([4, 3, 11, 9], fill=(200, 240, 255, 60))

# Core (Golem eye/face inside)
draw.rectangle([6, 5, 9, 8], fill=(255, 200, 50, 255))
draw.point((6, 6), fill=(255, 255, 255, 255))
draw.point((9, 6), fill=(255, 255, 255, 255))

import os
os.makedirs("/home/miky/IdeaProjects/GolemCraft/src/main/resources/assets/golemcraft/textures/item/", exist_ok=True)
img.save("/home/miky/IdeaProjects/GolemCraft/src/main/resources/assets/golemcraft/textures/item/golem_beacon.png")
print("Generated golem_beacon.png item texture")
