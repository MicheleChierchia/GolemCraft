import os
from PIL import Image

TEXTURE_DIR = "src/main/resources/assets/golemcraft/textures/entity"
base_path = os.path.join(TEXTURE_DIR, "base_golem.png")
out_path = os.path.join(TEXTURE_DIR, "lumberjack_golem.png")

img = Image.open(base_path).convert("RGBA")
px = img.load()

# Tint the whole base golem from grey to wood color
SKIN_LIGHT = (160, 110, 70, 255)
SKIN_MID   = (130, 85, 50, 255)
SKIN_DARK  = (90, 60, 35, 255)
for y in range(64):
    for x in range(64):
        c = px[x,y]
        if c[3] > 0:
            if c[0] > 120: px[x,y] = SKIN_LIGHT
            elif c[0] > 90: px[x,y] = SKIN_MID
            else: px[x,y] = SKIN_DARK
px = img.load()

# Palette
RED_LIGHT = (190, 50, 50, 255)
RED_MID   = (150, 40, 40, 255)
BLACK_RED = (60, 30, 30, 255)
BLACK     = (40, 40, 40, 255)
BLUE_PANTS = (50, 75, 120, 255)
BLUE_PANTS_DARK = (35, 55, 95, 255)
BROWN_BOOTS = (90, 60, 40, 255)
BEARD_BROWN = (70, 45, 25, 255)

def get_plaid(x, y):
    # Softer plaid pattern: 
    # Use horizontal and vertical bands of RED_MID, where they intersect it's BLACK_RED
    # The rest is RED_LIGHT
    is_v_band = (x // 2) % 2 == 0
    is_h_band = (y // 2) % 2 == 0
    if is_v_band and is_h_band:
        return BLACK_RED
    elif is_v_band or is_h_band:
        return RED_MID
    else:
        return RED_LIGHT

def put(x, y, color):
    if 0 <= x < 64 and 0 <= y < 64:
        px[x, y] = color

# 1. Beanie (Hat) - Top of the head
# Top face
for y in range(0, 10):
    for x in range(10, 18):
        if px[x, y][3] > 0:
            put(x, y, RED_LIGHT if (x+y)%2==0 else RED_MID)

# Beanie sides (top pixel of all side faces: y=10)
for y in range(10, 11):
    # All side faces from x=0 to 35
    for x in range(0, 36):
        put(x, y, RED_LIGHT if (x+y)%2==0 else RED_MID)


# Beanie Fold (Brim) - x:0-39, y:40-52
for y in range(40, 53):
    for x in range(0, 40):
        put(x, y, RED_LIGHT if (x+y)%2==0 else RED_MID)

# (Beard removed as requested)

# 3. Plaid Shirt - Body
# Body is x:6-13 (front), 0-5 (left), 14-19 (right), 20-27 (back), y:21-26
# Top is x:6-13, y:15-20
# Let's fill the whole body with plaid
for y in range(21, 27):
    # Left
    for x in range(0, 6): put(x, y, get_plaid(x, y))
    # Front
    for x in range(6, 14): put(x, y, get_plaid(x, y))
    # Right
    for x in range(14, 20): put(x, y, get_plaid(x, y))
    # Back
    for x in range(20, 28): put(x, y, get_plaid(x, y))

# Top/Bottom of body (shoulders, waist)
for y in range(15, 21):
    for x in range(6, 14): put(x, y, RED_LIGHT)
    for x in range(14, 22): put(x, y, BLUE_PANTS) # Bottom of body = pants top

# Suspenders (front and back)
for y in range(21, 27):
    # Front suspenders
    put(8, y, BLACK)
    put(11, y, BLACK)
    # Back suspenders
    put(22, y, BLACK)
    put(25, y, BLACK)

# 4. Plaid Shirt - Arms (Full arms are plaid, maybe rolled up at the bottom)
# Right Arm (x:36-49, y:16-29)
for y in range(16, 30):
    for x in range(36, 50):
        if px[x, y][3] > 0:
            rel_y = y - 16
            if rel_y < 11: # Shirt
                put(x, y, get_plaid(x, y))
            else: # Exposed copper hands
                pass

# Left Arm (x:50-63, y:16-29)
for y in range(16, 30):
    for x in range(50, 64):
        if px[x, y][3] > 0:
            rel_y = y - 16
            if rel_y < 11: # Shirt
                put(x, y, get_plaid(x, y))

# 5. Blue Jeans & Boots - Legs
# Legs are Right x:0-15, y:27-35 / Left x:16-31, y:27-35
# Top 4 rows are pants, bottom 5 are boots

# Right Leg
for y in range(27, 36):
    for x in range(0, 16):
        if px[x, y][3] > 0:
            rel_y = y - 27
            if rel_y < 7: # Pants (front/back/sides)
                # Front is 4-7, back is 12-15
                if rel_y < 6:
                    put(x, y, BLUE_PANTS_DARK if (x%2==0) else BLUE_PANTS)
                else: # the 7th pixel row
                    put(x, y, BLUE_PANTS_DARK if (x%2==0) else BLUE_PANTS)
            else: # Boots (last 2 pixels)
                put(x, y, BROWN_BOOTS)

# Left Leg
for y in range(27, 36):
    for x in range(16, 32):
        if px[x, y][3] > 0:
            rel_y = y - 27
            if rel_y < 7:
                if rel_y < 6:
                    put(x, y, BLUE_PANTS_DARK if (x%2==0) else BLUE_PANTS)
                else:
                    put(x, y, BLUE_PANTS_DARK if (x%2==0) else BLUE_PANTS)
            else:
                put(x, y, BROWN_BOOTS)

img.save(out_path)
print("Created lumberjack_golem.png")
