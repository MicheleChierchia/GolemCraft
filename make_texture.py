"""
Fisherman Golem Texture - Pixel Art Pixel by Pixel
Aspetto: Vero pescatore/marinaio
- Cappello da Skipper (blu navy con visiera nera)
- Giubbotto arancione di salvataggio sopra camicia bianca
- Pantaloni blu da marinaro
- Stivali di gomma neri
"""
import os
from PIL import Image

TEXTURE_DIR = "src/main/resources/assets/golemcraft/textures/entity"
base_path = os.path.join(TEXTURE_DIR, "base_golem.png")
out_path = os.path.join(TEXTURE_DIR, "fisherman_golem.png")

# Start with a COPY of the base texture (keeps copper structure: lightning rod, nose, eyes glow)
img = Image.open(base_path).convert("RGBA")
px = img.load()
# Tint the whole base golem from grey to copper color
SKIN_LIGHT = (215, 120, 60, 255) # Copper light
SKIN_MID   = (175, 85, 35, 255) # Copper mid
SKIN_DARK  = (130, 55, 20, 255) # Copper dark
for y in range(64):
    for x in range(64):
        c = px[x,y]
        if c[3] > 0:
            if c[0] > 120: px[x,y] = SKIN_LIGHT
            elif c[0] > 90: px[x,y] = SKIN_MID
            else: px[x,y] = SKIN_DARK
px = img.load()

# ─────────────────────────── PALETTE ────────────────────────────
# Hat - Blue Bucket Hat
HAT_BLUE_LIGHT   = (65, 85, 145, 255)
HAT_BLUE_MID     = (45, 65, 115, 255)
HAT_BLUE_DARK    = (25, 45, 85,  255)
HAT_BAND_LIGHT   = (140, 170, 210, 255)
HAT_BAND_DARK    = (100, 130, 180, 255)

# Vest - Blue vest
VEST_BLUE_LIGHT = (100, 130, 170, 255)
VEST_BLUE_MID   = (70, 100, 140, 255)
VEST_BLUE_DARK  = (50, 75,  110,  255)
VEST_STRAP_DARK = (40, 60,  90,  255)

# Shirt (under vest, visible on arms)
SHIRT_BEIGE_LIGHT = (215, 205, 180, 255)
SHIRT_BEIGE_MID   = (190, 180, 155, 255)
SHIRT_BEIGE_DARK  = (160, 150, 125, 255)

# Pants - Dark grey shorts
PANTS_GREY_LIGHT  = (90, 95, 100, 255)
PANTS_GREY_MID    = (70, 75, 80,  255)
PANTS_GREY_DARK   = (50, 55, 60,  255)

# Boots - Rubber yellow
BOOT_YELLOW_LIGHT = (245, 215, 60, 255)
BOOT_YELLOW_MID   = (215, 185, 30, 255)
BOOT_YELLOW_DARK  = (175, 145, 10, 255)

# Seam/pocket details
DETAIL_DARK       = (30, 30, 30, 255)

def put(x, y, color):
    """Set a pixel if it's within bounds"""
    if 0 <= x < 64 and 0 <= y < 64:
        px[x, y] = color

def fill_rect(x1, y1, x2, y2, color_func):
    """Fill a rectangle with a color function(x, y) -> color"""
    for y in range(y1, y2 + 1):
        for x in range(x1, x2 + 1):
            orig = px[x, y]
            if orig[3] > 0:
                c = color_func(x, y, orig)
                if c:
                    px[x, y] = c

# ═══════════════════════════════════════════════════════════════
# HEAD REGION: x:0-35, y:0-14
# UV Layout for box(-4,-5,-5, 8,5,10):
#   Top face:    x:10-17, y:0-9  (10 deep, 8 wide)
#   Left face:   x:0-9,   y:5-9  (10 wide, 5 tall) <- from right in game
#   Front face:  x:10-17, y:5-9  (8 wide, 5 tall) <- face
#   Right face:  x:18-27, y:5-9  (10 wide, 5 tall)
#   Back face:   x:28-35, y:5-9  (8 wide, 5 tall)
#   Bottom face: x:18-25, y:0-9  (8 wide, 10 deep) <- under head
# ═══════════════════════════════════════════════════════════════

# HEAD - Paint all copper head regions with copper-like tone
# We'll keep the existing copper golem head colors since it IS made of copper
# but tint the TOP slightly with hat influence
# (The captain's hat goes over the top, not replacing it)

# Keep head as-is (copper), just add hat-adjacent coloring for the hat geometry

# ═══════════════════════════════════════════════════════════════
# BODY: x:0-27, y:15-26
# UV Layout for box(-4,0,-3, 8,6,6):
#   Top face:   x:6-13,  y:15-20 (8 wide, 6 deep)
#   Front face: x:6-13,  y:21-26 (8 wide, 6 tall)
#   Bottom:     x:14-21, y:15-20 (8 wide, 6 deep)
#   Left:       x:0-5,   y:21-26 (6 wide, 6 tall)
#   Right:      x:14-19, y:21-26 (6 wide, 6 tall)
#   Back:       x:20-27, y:21-26 (8 wide, 6 tall)
# ═══════════════════════════════════════════════════════════════

# BODY - Orange life vest over white shirt
# Front face: x:6-13, y:21-26
for y in range(21, 27):
    for x in range(6, 14):
        rel_y = y - 21  # 0-5
        rel_x = x - 6   # 0-7
        # Central white shirt stripe down the middle
        if rel_x in [3, 4]:
            if rel_y < 4:
                c = SHIRT_BEIGE_LIGHT if rel_y < 2 else SHIRT_BEIGE_MID
            else:
                c = SHIRT_BEIGE_DARK
        # Orange vest sides
        elif rel_x in [0, 1]:
            c = VEST_BLUE_MID if rel_y > 0 else VEST_BLUE_LIGHT
        elif rel_x in [6, 7]:
            c = VEST_BLUE_MID if rel_y > 0 else VEST_BLUE_LIGHT
        # Vest main body
        elif rel_x in [2]:
            c = VEST_BLUE_LIGHT if rel_y < 2 else VEST_BLUE_MID
        elif rel_x in [5]:
            c = VEST_BLUE_LIGHT if rel_y < 2 else VEST_BLUE_MID
        else:
            c = VEST_BLUE_DARK
        # Bottom strap
        if rel_y == 5:
            c = VEST_STRAP_DARK
        put(x, y, c)

# Left face of body: x:0-5, y:21-26
for y in range(21, 27):
    for x in range(0, 6):
        rel_y = y - 21
        c = VEST_BLUE_MID if rel_y < 3 else VEST_BLUE_DARK
        if rel_y == 5: c = VEST_STRAP_DARK
        put(x, y, c)

# Right face of body: x:14-19, y:21-26
for y in range(21, 27):
    for x in range(14, 20):
        rel_y = y - 21
        c = VEST_BLUE_MID if rel_y < 3 else VEST_BLUE_DARK
        if rel_y == 5: c = VEST_STRAP_DARK
        put(x, y, c)

# Back face of body: x:20-27, y:21-26
for y in range(21, 27):
    for x in range(20, 28):
        rel_y = y - 21
        c = VEST_BLUE_LIGHT if rel_y < 2 else VEST_BLUE_MID
        if rel_y == 5: c = VEST_STRAP_DARK
        put(x, y, c)

# Top of body: x:6-13, y:15-20
for y in range(15, 21):
    for x in range(6, 14):
        c = VEST_BLUE_MID
        put(x, y, c)

# Bottom of body: x:14-21, y:15-20
for y in range(15, 21):
    for x in range(14, 22):
        c = VEST_BLUE_DARK
        put(x, y, c)

# ═══════════════════════════════════════════════════════════════
# ARMS: Right x:36-49, y:16-29 / Left x:50-63, y:16-29
# UV for box(-1.5,0,-2, 3,10,4):
#   Top:   x+0  to x+3,  y+0 to y+3   (4 wide, 4 deep) - actually 3w, 4d
#   Front: x+4  to x+6,  y+4 to y+13  (3 wide, 10 tall)
#   Bottom:x+7  to x+9,  y+0 to y+3   
#   Left:  x+0  to x+3,  y+4 to y+13  (4 wide, 10 tall)
#   Right: x+7  to x+10, y+4 to y+13  (4 wide, 10 tall)
#   Back:  x+11 to x+13, y+4 to y+13  (3 wide, 10 tall)
# ═══════════════════════════════════════════════════════════════

# RIGHT ARM (x:36-49, y:16-29) - White shirt with orange vest sleeve
for y in range(16, 30):
    for x in range(36, 50):
        rel_x = x - 36  # 0-13
        rel_y = y - 16  # 0-13
        orig = px[x, y]
        
        # Top of arm (rel_y 0-3)
        if rel_y < 4:
            if rel_x < 4:      c = VEST_BLUE_MID    # left top
            elif rel_x < 7:    c = VEST_BLUE_LIGHT   # front top
            elif rel_x < 10:   c = VEST_BLUE_MID    # right top (bottom face)
            else:              c = (0,0,0,0)            # transparent (unused)
        # Arm faces (rel_y 4-13)
        else:
            arm_y = rel_y - 4  # 0-9
            if rel_x < 4:       # left face
                c = SHIRT_BEIGE_MID if arm_y < 4 else orig
                if arm_y == 4: c = DETAIL_DARK  # cuff line
            elif rel_x < 7:     # front face
                c = SHIRT_BEIGE_LIGHT if arm_y < 4 else orig
                if arm_y == 4: c = DETAIL_DARK
                # Orange shoulder patch
                if arm_y < 3:
                    c = VEST_BLUE_MID
            elif rel_x < 11:    # right face
                c = SHIRT_BEIGE_MID if arm_y < 4 else orig
                if arm_y == 4: c = DETAIL_DARK
            else:               # back face
                c = SHIRT_BEIGE_DARK if arm_y < 4 else orig
                if arm_y == 4: c = DETAIL_DARK
                if arm_y < 3:
                    c = VEST_BLUE_DARK
        
        if 0 <= x < 64 and 0 <= y < 64:
            px[x, y] = c

# LEFT ARM (x:50-63, y:16-29) - mirror of right
for y in range(16, 30):
    for x in range(50, 64):
        rel_x = x - 50  # 0-13
        rel_y = y - 16  # 0-13
        orig = px[x, y]
        
        if rel_y < 4:
            if rel_x < 4:      c = VEST_BLUE_MID
            elif rel_x < 7:    c = VEST_BLUE_LIGHT
            elif rel_x < 10:   c = VEST_BLUE_MID
            else:              c = (0,0,0,0)
        else:
            arm_y = rel_y - 4
            if rel_x < 4:
                c = SHIRT_BEIGE_DARK if arm_y < 4 else orig
                if arm_y == 4: c = DETAIL_DARK
                if arm_y < 3: c = VEST_BLUE_DARK
            elif rel_x < 7:
                c = SHIRT_BEIGE_LIGHT if arm_y < 4 else orig
                if arm_y == 4: c = DETAIL_DARK
                if arm_y < 3: c = VEST_BLUE_MID
            elif rel_x < 11:
                c = SHIRT_BEIGE_MID if arm_y < 4 else orig
                if arm_y == 4: c = DETAIL_DARK
            else:
                c = SHIRT_BEIGE_MID if arm_y < 4 else orig
                if arm_y == 4: c = DETAIL_DARK
                if arm_y < 3: c = VEST_BLUE_MID
        
        if 0 <= x < 64 and 0 <= y < 64:
            px[x, y] = c

# ═══════════════════════════════════════════════════════════════
# LEGS: Right x:0-15, y:27-35 / Left x:16-31, y:27-35
# UV for box(-2,0,-2, 4,5,4):
#   Top:   x+4 to x+7, y+0 to y+3
#   Front: x+4 to x+7, y+4 to y+8
#   Bottom:x+8 to x+11,y+0 to y+3
#   Left:  x+0 to x+3, y+4 to y+8
#   Right: x+8 to x+11,y+4 to y+8
#   Back:  x+12 to x+15,y+4 to y+8
# ═══════════════════════════════════════════════════════════════

# RIGHT LEG (x:0-15, y:27-35)
for y in range(27, 36):
    for x in range(0, 16):
        rel_x = x
        rel_y = y - 27
        
        if rel_y < 4:  # tops
            if rel_x < 4:      c = PANTS_GREY_DARK
            elif rel_x < 8:    c = PANTS_GREY_MID
            elif rel_x < 12:   c = PANTS_GREY_DARK
            else:              c = (0,0,0,0)
        else:
            leg_y = rel_y - 4  # 0-4, last 2 are boot
            is_boot = leg_y >= 3
            if rel_x < 4:       # left face
                c = BOOT_YELLOW_MID if is_boot else PANTS_GREY_MID
            elif rel_x < 8:     # front face
                c = BOOT_YELLOW_LIGHT if is_boot else PANTS_GREY_LIGHT
                if is_boot and leg_y == 3: c = BOOT_YELLOW_MID  # boot top seam
            elif rel_x < 12:    # right face
                c = BOOT_YELLOW_MID if is_boot else PANTS_GREY_MID
            else:               # back face
                c = BOOT_YELLOW_DARK if is_boot else PANTS_GREY_DARK
        
        put(x, y, c)

# LEFT LEG (x:16-31, y:27-35)
for y in range(27, 36):
    for x in range(16, 32):
        rel_x = x - 16
        rel_y = y - 27
        
        if rel_y < 4:
            if rel_x < 4:      c = PANTS_GREY_DARK
            elif rel_x < 8:    c = PANTS_GREY_MID
            elif rel_x < 12:   c = PANTS_GREY_DARK
            else:              c = (0,0,0,0)
        else:
            leg_y = rel_y - 4
            is_boot = leg_y >= 3
            if rel_x < 4:
                c = BOOT_YELLOW_DARK if is_boot else PANTS_GREY_DARK
            elif rel_x < 8:
                c = BOOT_YELLOW_LIGHT if is_boot else PANTS_GREY_LIGHT
                if is_boot and leg_y == 3: c = BOOT_YELLOW_MID
            elif rel_x < 12:
                c = BOOT_YELLOW_MID if is_boot else PANTS_GREY_MID
            else:
                c = BOOT_YELLOW_MID if is_boot else PANTS_GREY_MID
        
        put(x, y, c)

# ═══════════════════════════════════════════════════════════════
# CAPTAIN HAT BRIM: x:0-39, y:40-52
# UV for box(-4.5,-5.5,-5.5, 9,2,11):
# Width=9+11+9+11=40=x:0-39, Height=2+11=13=y:40-52
#   Front: x:11-19, y:51-52 (2 tall)
#   Back:  x:30-38, y:51-52 (2 tall)
#   Left:  x:0-10,  y:51-52 (2 tall side)  <- in game = right brim
#   Right: x:20-30, y:51-52 (2 tall side)  <- in game = left brim
#   Top:   x:11-19, y:40-50 (11 deep, 9 wide) <- top of brim
#   Bottom:x:20-28, y:40-50 (11 deep, 9 wide)
# ═══════════════════════════════════════════════════════════════

# HAT BRIM - Navy Captain hat brim
for y in range(40, 53):
    for x in range(0, 40):
        rel_y = y - 40  # 0-12
        # Top of the brim = hat top plate (lighter)
        if rel_y < 11:
            if x < 11:        c = HAT_BLUE_MID        # left side face
            elif x < 20:      c = HAT_BLUE_LIGHT         # top face
            elif x < 29:      c = HAT_BLUE_MID           # bottom face 
            elif x < 31:      c = HAT_BLUE_MID        # right side face
            else:             c = HAT_BLUE_DARK          # extra
        else:  # Bottom 2 rows = brim edge: front, sides, back
            c = HAT_BLUE_MID  # Black visor brim
        put(x, y, c)

# Now paint the head top zone to look like the hat continuation
# The hat cylinder top is at head top area
# Head top UV: roughly x:10-17, y:0-9 based on standard head UV
# This is the hat crown - navy color
for y in range(0, 5):
    for x in range(10, 18):
        orig = px[x, y]
        if orig[3] > 0:
            # Hat crown top
            c = HAT_BLUE_LIGHT
            if y > 2: c = HAT_BLUE_MID
            put(x, y, c)

# Hat band (gold stripe around the hat)
# This would be at the base of the crown, around y=3-4 in head UV
# Front of head/hat: x:10-17, y:5-9 - paint bottom 2 rows as gold band
for y in range(8, 10):  # bottom of head front
    for x in range(10, 18):
        orig = px[x, y]
        if orig[3] > 0:
            c = HAT_BAND_LIGHT if y == 9 else HAT_BAND_LIGHT
            put(x, y, c)

# Hat sides gold band
for y in range(8, 10):
    # Left side head: x:0-9, y:5-9
    for x in range(0, 10):
        orig = px[x, y]
        if orig[3] > 0:
            put(x, y, HAT_BAND_LIGHT)
    # Right side: x:18-27, y:5-9
    for x in range(18, 28):
        orig = px[x, y]
        if orig[3] > 0:
            put(x, y, HAT_BAND_LIGHT)
    # Back: x:28-35, y:5-9
    for x in range(28, 36):
        orig = px[x, y]
        if orig[3] > 0:
            put(x, y, HAT_BAND_LIGHT)

# Upper head crown area (above band) = navy
for y in range(5, 8):
    for x in range(0, 36):
        orig = px[x, y]
        if orig[3] > 0:
            c = HAT_BLUE_MID
            put(x, y, c)

img.save(out_path)
print("Created detailed fisherman_golem.png (Captain style)")
