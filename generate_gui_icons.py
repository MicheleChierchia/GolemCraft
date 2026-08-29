from PIL import Image, ImageDraw

img = Image.open("/home/miky/IdeaProjects/GolemCraft/src/main/resources/assets/golemcraft/textures/gui/golem_beacon.png")
draw = ImageDraw.Draw(img)

def draw_regen(x, y):
    # Cuoricino rosa/rosso
    draw.polygon([(x+9, y+14), (x+4, y+8), (x+4, y+4), (x+7, y+2), (x+9, y+5), (x+11, y+2), (x+14, y+4), (x+14, y+8)], fill=(255, 50, 100, 255))

def draw_haste(x, y):
    # Piccone d'oro
    draw.line((x+3, y+15, x+12, y+6), fill=(100, 50, 0, 255), width=2)
    draw.polygon([(x+8, y+4), (x+14, y+2), (x+16, y+8), (x+12, y+10)], fill=(255, 200, 0, 255))

def draw_resistance(x, y):
    # Scudino
    draw.polygon([(x+4, y+2), (x+14, y+2), (x+14, y+10), (x+9, y+16), (x+4, y+10)], fill=(100, 100, 150, 255))
    draw.polygon([(x+6, y+4), (x+12, y+4), (x+12, y+9), (x+9, y+13), (x+6, y+9)], fill=(150, 150, 200, 255))

def draw_strength(x, y):
    # Spada di pietra/ferro
    draw.line((x+4, y+14, x+8, y+10), fill=(150, 75, 0, 255), width=2)
    draw.line((x+6, y+12, x+10, y+8), fill=(50, 50, 50, 255), width=2)
    draw.line((x+8, y+10, x+15, y+3), fill=(200, 200, 200, 255), width=3)

# Disegniamo le icone nello spritesheet, riga sotto ai bottoni (y=184)
# uv = (0, 184) Regen
# uv = (18, 184) Haste
# uv = (36, 184) Resistance
# uv = (54, 184) Strength
draw_regen(0, 184)
draw_haste(18, 184)
draw_resistance(36, 184)
draw_strength(54, 184)

img.save("/home/miky/IdeaProjects/GolemCraft/src/main/resources/assets/golemcraft/textures/gui/golem_beacon.png")
print("Added effect icons to spritesheet!")
