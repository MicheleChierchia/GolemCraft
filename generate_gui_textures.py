from PIL import Image, ImageDraw, ImageFilter

def draw_rect(draw, coords, fill, outline=None):
    x0, y0, x1, y1 = coords
    draw.rectangle([x0, y0, x1 - 1, y1 - 1], fill=fill, outline=outline)

def draw_mc_button(draw, x, y, size, state):
    # state: 0=normal, 1=selected, 2=disabled
    draw_rect(draw, (x, y, x + size, y + size), fill=(0, 0, 0, 255))
    inner = (198, 198, 198, 255)
    border_lt = (255, 255, 255, 255)
    border_rb = (85, 85, 85, 255)
    
    if state == 2: # disabled
        inner = (140, 140, 140, 255)
        border_lt = (180, 180, 180, 255)
        border_rb = (60, 60, 60, 255)
    
    # Fill
    draw_rect(draw, (x+1, y+1, x+size-1, y+size-1), fill=inner)
    
    # Borders
    draw.line((x, y, x+size-2, y), fill=border_lt)
    draw.line((x, y, x, y+size-2), fill=border_lt)
    draw.line((x+1, y+size-1, x+size-1, y+size-1), fill=border_rb)
    draw.line((x+size-1, y+1, x+size-1, y+size-1), fill=border_rb)
    
    if state == 1: # selected (thick green border or recessed)
        # Recessed style
        draw_rect(draw, (x, y, x+size, y+size), fill=None, outline=(0, 0, 0, 255))
        draw_rect(draw, (x+1, y+1, x+size-1, y+size-1), fill=None, outline=(85, 255, 85, 255))

def generate_beacon_gui():
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Base GUI (230x219)
    # Background color
    bg_color = (198, 198, 198, 255)
    draw_rect(draw, (0, 0, 230, 219), fill=bg_color)
    
    # Outer border
    draw_rect(draw, (0, 0, 230, 219), fill=None, outline=(0, 0, 0, 255))
    draw.line((1, 1, 228, 1), fill=(255, 255, 255, 255))
    draw.line((1, 1, 1, 217), fill=(255, 255, 255, 255))
    draw.line((2, 218, 229, 218), fill=(85, 85, 85, 255))
    draw.line((229, 2, 229, 218), fill=(85, 85, 85, 255))

    # Dark panels for effects (primary at left, secondary at right)
    # Primary: 2 columns, 2 rows (startX=26, startY=22, spacing=25) -> area ~ 55x55
    draw_rect(draw, (20, 18, 85, 85), fill=(139, 139, 139, 255), outline=(55, 55, 55, 255))
    draw_rect(draw, (20+1, 18+1, 85, 85), fill=None, outline=(255, 255, 255, 255)) # inset inner
    
    # Secondary: 2 columns, 1 row (startX=131, startY=22)
    draw_rect(draw, (125, 18, 185, 50), fill=(139, 139, 139, 255), outline=(55, 55, 55, 255))
    draw_rect(draw, (125+1, 18+1, 185, 50), fill=None, outline=(255, 255, 255, 255))

    # Inventory section bottom
    draw_rect(draw, (7, 100, 223, 211), fill=(139, 139, 139, 255), outline=(55, 55, 55, 255))
    
    # Draw Buttons states (0,219 to 66,241)
    draw_mc_button(draw, 0, 219, 22, 0)
    draw_mc_button(draw, 22, 219, 22, 1)
    draw_mc_button(draw, 44, 219, 22, 2)
    
    # Draw "Immunity to Oxidation" Icon (UvX=0, UvY=166, Size=16x16)
    # A shield-like shape or copper block with a tick
    ix, iy = 0, 166
    draw_rect(draw, (ix, iy, ix+16, iy+16), fill=(0,0,0,0))
    # Copper-ish shield
    draw.polygon([(ix+3, iy+2), (ix+13, iy+2), (ix+13, iy+10), (ix+8, iy+15), (ix+3, iy+10)], fill=(222, 122, 89, 255), outline=(0,0,0,255))
    # Sparkle / tick inside
    draw.line((ix+5, iy+7, ix+7, iy+9, ix+11, iy+5), fill=(255, 255, 255, 255), width=2)
    
    # Draw "Charged Auto" Icon (UvX=18, UvY=166, Size=18x18)
    # A lightning bolt
    cx, cy = 18, 166
    draw_rect(draw, (cx, cy, cx+18, cy+18), fill=(0,0,0,0))
    # Lightning path
    draw.polygon([(cx+9, cy+2), (cx+5, cy+10), (cx+10, cy+10), (cx+7, cy+17), (cx+14, cy+7), (cx+8, cy+7)], fill=(255, 255, 85, 255), outline=(0,0,0,255))

    img.save("src/main/resources/assets/golemcraft/textures/gui/golem_beacon.png")
    print("GUI texture generated.")

def generate_beam_texture():
    # Beam is typically 16x16, white core, fading edges.
    img = Image.new("RGBA", (16, 16), (255, 255, 255, 0))
    draw = ImageDraw.Draw(img)
    
    for x in range(16):
        for y in range(16):
            # Distance from center
            dist_x = abs(x - 7.5)
            dist_y = abs(y - 7.5)
            # Make a cross/plus shape like vanilla beam
            if dist_x < 2 or dist_y < 2:
                # Core
                img.putpixel((x, y), (255, 255, 255, 255))
            elif dist_x < 4 and dist_y < 4:
                # Glow
                img.putpixel((x, y), (255, 255, 255, 128))
            else:
                # Faint outer
                img.putpixel((x, y), (255, 255, 255, 20))
                
    # Add vertical lines for motion illusion
    for y in range(16):
        if y % 4 == 0:
            img.putpixel((7, y), (200, 200, 255, 255))
            img.putpixel((8, y), (200, 200, 255, 255))

    img.save("src/main/resources/assets/golemcraft/textures/misc/golem_beacon_beam.png")
    print("Beam texture generated.")

import os
os.makedirs("src/main/resources/assets/golemcraft/textures/gui/", exist_ok=True)
os.makedirs("src/main/resources/assets/golemcraft/textures/misc/", exist_ok=True)

generate_beacon_gui()
generate_beam_texture()
