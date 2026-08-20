import sys
import os
import random
import colorsys
from PIL import Image, ImageFilter

def create_noise_map(width, height, radius=1.5, seed=42):
    random.seed(seed)
    
    noise = Image.new('L', (width, height))
    pixels = noise.load()
    
    # Generate full resolution random noise
    for y in range(height):
        for x in range(width):
            pixels[x, y] = random.randint(0, 255)
            
    # Blur it slightly to create organic blobs instead of pure static
    noise = noise.filter(ImageFilter.GaussianBlur(radius=radius))
    
    pixels = noise.load()
    min_v = 255
    max_v = 0
    for y in range(height):
        for x in range(width):
            v = pixels[x, y]
            if v < min_v: min_v = v
            if v > max_v: max_v = v
            
    noise_map = []
    for y in range(height):
        row = []
        for x in range(width):
            v = pixels[x, y]
            normalized = (v - min_v) / (max_v - min_v) if max_v > min_v else 0.5
            row.append(normalized)
        noise_map.append(row)
        
    return noise_map

def get_rusted_color(r, g, b):
    # Convert to HLS
    h, l, s = colorsys.rgb_to_hls(r / 255.0, g / 255.0, b / 255.0)
    
    # Darken and desaturate to derive the rust color
    target_l = l * 0.5 
    target_s = s * 0.4
    target_h = h
    
    tr, tg, tb = colorsys.hls_to_rgb(target_h, target_l, target_s)
    return int(tr * 255), int(tg * 255), int(tb * 255)

def generate_textures(input_path):
    if not os.path.exists(input_path):
        print(f"Error: {input_path} not found.")
        return
        
    print(f"Processing: {input_path}")
    base_img = Image.open(input_path).convert("RGBA")
    width, height = base_img.size
    
    dir_name = os.path.dirname(input_path)
    base_name = os.path.basename(input_path)
    
    # Generate noise maps (using radius instead of scaling for sharper pixel art edges)
    noise1 = create_noise_map(width, height, radius=2.5, seed=101)
    noise2 = create_noise_map(width, height, radius=1.0, seed=202)
    
    stages = [
        ("exposed", 0.35),   
        ("weathered", 0.60), 
        ("oxidized", 0.95)    
    ]
    
    for stage_name, oxidation_level in stages:
        new_img = Image.new("RGBA", (width, height))
        pixels = new_img.load()
        base_pixels = base_img.load()
        
        for y in range(height):
            for x in range(width):
                r, g, b, a = base_pixels[x, y]
                
                if a == 0:
                    pixels[x, y] = (0, 0, 0, 0)
                    continue
                    
                n = noise1[y][x] * 0.7 + noise2[y][x] * 0.3
                
                # Protect pure black or pure white details
                if r < 10 and g < 10 and b < 10:
                    pixels[x, y] = (r, g, b, a)
                    continue
                if r > 245 and g > 245 and b > 245:
                    pixels[x, y] = (r, g, b, a)
                    continue
                
                # Add a tiny bit of per-pixel random noise for dithered edges
                random.seed(x * 1000 + y + int(oxidation_level * 100))
                pixel_noise = random.random() * 0.15 - 0.075
                
                intensity = n + pixel_noise + oxidation_level - 0.5
                
                # HARD THRESHOLD: Pixels are either completely rusted or completely original
                # This creates the "macchie" (patches) pixel-art style of Minecraft
                if intensity > 0.5:
                    new_r, new_g, new_b = get_rusted_color(r, g, b)
                    pixels[x, y] = (new_r, new_g, new_b, a)
                else:
                    pixels[x, y] = (r, g, b, a)
                
        out_path = os.path.join(dir_name, f"{stage_name}_{base_name}")
        new_img.save(out_path)
        print(f"Generated: {out_path}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python generate_oxidation.py <path_to_texture>")
        sys.exit(1)
        
    for path in sys.argv[1:]:
        generate_textures(path)
