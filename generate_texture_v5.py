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
        
    # Draw leaf shapes on the texture at (56, 32)
    # The new tendril model will use a 3x5 flat plane.
    # UV will be at 56, 32 for a 3x5 area. We can draw it explicitly.
    leaf_pattern = [
        [0, 1, 0],
        [1, 1, 1],
        [1, 1, 1],
        [0, 1, 0],
        [0, 1, 0]
    ]
        
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
                
                if local_x == 3 or local_x == 4:
                    is_mouth_dark_line = True
                elif local_x == 2 or local_x == 5:
                    if local_y % 2 != 0: 
                        is_mouth_center_glow = True
                
                if local_y % 2 == 0 and not is_mouth_dark_line:
                    is_rib = True

            r, g, b, a = pixels[x, y]
            
            # Tendrils UV is 56-62, 32-37 for two leaves
            is_tendril = False
            is_tendril_solid = False
            if 56 <= x <= 61 and 32 <= y <= 36:
                is_tendril = True
                local_x = x - 56
                local_y = y - 32
                
                # Leaf 1 is at local_x 0..2
                # Leaf 2 is at local_x 3..5
                if local_x < 3:
                    if leaf_pattern[local_y][local_x] == 1:
                        is_tendril_solid = True
                else:
                    if leaf_pattern[local_y][local_x - 3] == 1:
                        is_tendril_solid = True
                        
                # Force alpha so we overwrite it
                a = 255 if is_tendril_solid else 0
                r, g, b = (128, 128, 128)
                
            if a > 0:
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
                        # Re-add random dots for sculk!
                        if random.random() < 0.1 and not is_tendril:
                            new_r, new_g, new_b = SCULK_VEIN
                            # Slightly vary intensity
                            mult = 1.0 + random.random() * 0.5
                            new_r = min(255, int(new_r * mult))
                            new_g = min(255, int(new_g * mult))
                            new_b = min(255, int(new_b * mult))
                            glow_pixels[x, y] = (new_r, new_g, new_b, 255)
                        else:
                            new_r = min(255, int(30 * intensity))
                            new_g = min(255, int(50 * intensity))
                            new_b = min(255, int(60 * intensity))
                        
                        if is_tendril and is_tendril_solid:
                            # Tips of tendrils glow
                            if y <= 33:
                                new_r, new_g, new_b = SCULK_VEIN
                                glow_pixels[x, y] = (new_r, new_g, new_b, 255)
                            else:
                                new_r = min(255, int(30 * intensity))
                                new_g = min(255, int(50 * intensity))
                                new_b = min(255, int(60 * intensity))
                                
                    else:
                        c_r, c_g, c_b = BONE_BASE
                        if random.random() < 0.1:
                            intensity *= 0.9
                        
                        new_r = min(255, int(c_r * intensity))
                        new_g = min(255, int(c_g * intensity))
                        new_b = min(255, int(c_b * intensity))
                    
                    if not is_tendril or (is_tendril and is_tendril_solid):
                        pixels[x, y] = (new_r, new_g, new_b, a)
                    elif is_tendril and not is_tendril_solid:
                        pixels[x, y] = (0, 0, 0, 0)
                
    img.save('src/main/resources/assets/golemcraft/textures/entity/depth_golem.png')
    glow_img.save('src/main/resources/assets/golemcraft/textures/entity/depth_golem_glow.png')
    
    print("Texture v5 generated successfully.")
except Exception as e:
    print(f"Error: {e}")
