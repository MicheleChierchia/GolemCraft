import sys
import math
import random
try:
    from PIL import Image, ImageDraw
    
    img = Image.open('src/main/resources/assets/golemcraft/textures/entity/base_golem.png').convert('RGBA')
    width, height = img.size
    
    glow_img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    
    pixels = img.load()
    glow_pixels = glow_img.load()
    
    SCULK_VEIN = (0, 200, 200)
    BONE_BASE = (220, 215, 190)
    MOUTH_BG = (5, 10, 15)
    
    def noise(x, y):
        return (math.sin(x * 0.5) + math.cos(y * 0.5) + math.sin((x+y)*0.8)) / 3.0
        
    for y in range(height):
        for x in range(width):
            is_mouth = False
            is_rib = False
            is_mouth_center_glow = False
            is_mouth_dark_line = False
            
            if 6 <= x <= 13 and 21 <= y <= 26:
                is_mouth = True
                local_x = x - 6 # 0 to 7
                local_y = y - 21 # 0 to 5
                
                # Dark center line at local_x = 3, 4 (X=9, 10)
                if local_x == 3 or local_x == 4:
                    is_mouth_dark_line = True
                # Glowing cyan around the center line at local_x = 2, 5 (X=8, 11)
                elif local_x == 2 or local_x == 5:
                    # Only glow on the gaps, not where the ribs are? 
                    # The Warden's souls are mostly visible in the gaps.
                    if local_y % 2 != 0: # gaps
                        is_mouth_center_glow = True
                
                # Ribs are at local_y = 0, 2, 4 but not on the center line
                if local_y % 2 == 0 and not is_mouth_dark_line:
                    is_rib = True

            r, g, b, a = pixels[x, y]
            
            is_tendril = False
            if 56 <= x <= 64 and 32 <= y <= 40:
                is_tendril = True
                a = 255
                r, g, b = (128, 128, 128)
                
            if a > 0:
                # OBLITERATE EYES (X:10-18, Y:10-15)
                # Instead of keeping intensity of the red/yellow eyes which stands out,
                # we just use the intensity of a nearby skin pixel (e.g., X=8, Y=12)
                if 10 <= x <= 18 and 10 <= y <= 15:
                    ref_r, ref_g, ref_b, _ = pixels[8, 12]
                    intensity = ((ref_r + ref_g + ref_b) / 3.0) / 128.0
                else:
                    intensity = ((r + g + b) / 3.0) / 128.0
                
                if is_tendril:
                    intensity = 1.0
                    
                is_sculk = False
                
                if y < 15:
                    is_sculk = True
                elif 0 <= x <= 28 and 15 <= y <= 27:
                    local_y = y - 15
                    threshold = local_y / 12.0
                    n = noise(x, y) * 0.3 + 0.5
                    if threshold < n:
                        is_sculk = True
                elif 36 <= x <= 64 and 16 <= y <= 30:
                    local_y = y - 16
                    threshold = local_y / 14.0
                    n = noise(x, y) * 0.3 + 0.5
                    if threshold < n:
                        is_sculk = True
                elif x < 32 and y >= 27:
                    local_y = y - 27
                    if local_y == 0 and random.random() < 0.3:
                        is_sculk = True
                        
                if is_tendril:
                    is_sculk = True
                
                # Render logic
                if is_mouth:
                    if is_mouth_dark_line:
                        c_r, c_g, c_b = MOUTH_BG
                        pixels[x, y] = (c_r, c_g, c_b, 255)
                    elif is_mouth_center_glow:
                        c_r, c_g, c_b = SCULK_VEIN
                        glow_pixels[x, y] = (c_r, c_g, c_b, 255)
                        pixels[x, y] = (c_r, c_g, c_b, 255)
                    elif is_rib:
                        c_r, c_g, c_b = BONE_BASE
                        pixels[x, y] = (min(255, int(c_r * 1.2)), min(255, int(c_g * 1.2)), min(255, int(c_b * 1.2)), 255)
                    else:
                        c_r, c_g, c_b = MOUTH_BG
                        pixels[x, y] = (c_r, c_g, c_b, 255)
                else:
                    if is_sculk:
                        # Revert to original tint method instead of random dots!
                        # original tint: r*0.2, g*0.4, b*0.5 but based on intensity
                        # Let's say sculk base is dark teal.
                        # Instead of replacing, we multiply intensity by specific rgb ratios.
                        # (15, 25, 30) is very dark. 
                        # Let's do new_r = intensity * 30, new_g = intensity * 50, new_b = intensity * 60
                        
                        new_r = min(255, int(30 * intensity))
                        new_g = min(255, int(50 * intensity))
                        new_b = min(255, int(60 * intensity))
                        
                        # Glowing tendril tips!
                        if is_tendril and y <= 33:
                            new_r, new_g, new_b = SCULK_VEIN
                            glow_pixels[x, y] = (new_r, new_g, new_b, 255)
                    else:
                        c_r, c_g, c_b = BONE_BASE
                        if random.random() < 0.1:
                            intensity *= 0.9
                        
                        new_r = min(255, int(c_r * intensity))
                        new_g = min(255, int(c_g * intensity))
                        new_b = min(255, int(c_b * intensity))
                    
                    pixels[x, y] = (new_r, new_g, new_b, a)
                
    img.save('src/main/resources/assets/golemcraft/textures/entity/depth_golem.png')
    glow_img.save('src/main/resources/assets/golemcraft/textures/entity/depth_golem_glow.png')
    
    print("Texture generated successfully.")
except Exception as e:
    print(f"Error: {e}")
