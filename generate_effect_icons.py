from PIL import Image, ImageDraw

def create_charge_icon():
    img = Image.new('RGBA', (18, 18), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    # Fulmine
    draw.polygon([(9, 2), (3, 10), (8, 10), (7, 16), (15, 7), (10, 7)], fill=(255, 255, 0, 255), outline=(0, 0, 0, 255))
    img.save("/home/miky/IdeaProjects/GolemCraft/src/main/resources/assets/golemcraft/textures/mob_effect/charge.png")

def create_oxidation_immunity_icon():
    img = Image.new('RGBA', (18, 18), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    # Scudo di Rame pulito
    draw.polygon([(4, 2), (14, 2), (14, 10), (9, 16), (4, 10)], fill=(255, 120, 50, 255), outline=(0, 0, 0, 255))
    draw.polygon([(6, 4), (12, 4), (12, 9), (9, 13), (6, 9)], fill=(255, 180, 100, 255))
    img.save("/home/miky/IdeaProjects/GolemCraft/src/main/resources/assets/golemcraft/textures/mob_effect/oxidation_immunity.png")

create_charge_icon()
create_oxidation_immunity_icon()
