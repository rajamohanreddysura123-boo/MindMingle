import math
from PIL import Image, ImageDraw, ImageFilter

def create_app_icon():
    # Supersample 4x for pristine antialiasing (2048x2048 -> 512x512)
    SCALE = 4
    W, H = 512 * SCALE, 512 * SCALE

    # Create RGBA Image
    img = Image.new("RGBA", (W, H), (0, 0, 0, 255))
    draw = ImageDraw.Draw(img)

    # 1. Pitch Black Background
    draw.rectangle([0, 0, W, H], fill=(0, 0, 0, 255))

    # 2. Frosted Glass Container Card
    x0, y0, x1, y1 = 64 * SCALE, 64 * SCALE, 448 * SCALE, 448 * SCALE
    r = 96 * SCALE

    # Create gradient mask & fill for glass card
    card_mask = Image.new("L", (W, H), 0)
    card_mask_draw = ImageDraw.Draw(card_mask)
    card_mask_draw.rounded_rectangle([x0, y0, x1, y1], radius=r, fill=255)

    gradient = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    for y in range(int(y0), int(y1)):
        for x in range(int(x0), int(x1)):
            # Diagonal factor 0.0 -> 1.0
            t = ( (x - x0)/(x1 - x0) + (y - y0)/(y1 - y0) ) / 2.0
            t = max(0.0, min(1.0, t))

            if t <= 0.5:
                alpha = 0.14 + (0.05 - 0.14) * (t / 0.5)
            else:
                alpha = 0.05 + (0.015 - 0.05) * ((t - 0.5) / 0.5)
            
            a_byte = int(alpha * 255)
            gradient.putpixel((x, y), (255, 255, 255, a_byte))

    img.paste(gradient, (0, 0), card_mask)

    # 3. Glass Rim Highlight Outline
    outline_draw = ImageDraw.Draw(img)
    outline_draw.rounded_rectangle(
        [x0, y0, x1, y1],
        radius=r,
        outline=(255, 255, 255, int(0.21 * 255)),
        width=int(2.5 * SCALE)
    )

    # 4. Monogram Path Points
    points = [
        (160 * SCALE, 352 * SCALE),
        (160 * SCALE, 176 * SCALE),
        (256 * SCALE, 272 * SCALE),
        (352 * SCALE, 176 * SCALE),
        (352 * SCALE, 352 * SCALE)
    ]

    # Draw Frosted Glass Contour (Stroke Width 42 * SCALE, White alpha 0.31)
    contour_img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    c_draw = ImageDraw.Draw(contour_img)
    c_draw.line(points, fill=(255, 255, 255, int(0.31 * 255)), width=int(42 * SCALE), joint="round")
    
    # Draw round caps at start and end
    cap_r_c = (42 * SCALE) // 2
    for p in [points[0], points[-1]]:
        c_draw.ellipse([p[0]-cap_r_c, p[1]-cap_r_c, p[0]+cap_r_c, p[1]+cap_r_c], fill=(255, 255, 255, int(0.31 * 255)))

    img = Image.alpha_composite(img, contour_img)

    # 5. Core Monogram 'M' (Primary #D6A87C, Stroke Width 30 * SCALE)
    core_img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    m_draw = ImageDraw.Draw(core_img)
    m_draw.line(points, fill=(214, 168, 124, 255), width=int(30 * SCALE), joint="round")

    cap_r_m = (30 * SCALE) // 2
    for p in [points[0], points[-1]]:
        m_draw.ellipse([p[0]-cap_r_m, p[1]-cap_r_m, p[0]+cap_r_m, p[1]+cap_r_m], fill=(214, 168, 124, 255))

    img = Image.alpha_composite(img, core_img)

    # Downsample to 512x512 with high-quality LANCZOS filtering
    final_icon = img.resize((512, 512), resample=Image.Resampling.LANCZOS)
    
    output_path = "/Users/developer/AndroidStudioProjects/Rajamohan_Reddy/MindMingle/play_store_assets/app_icon.png"
    final_icon.save(output_path, "PNG")
    print(f"Successfully rendered 512x512 app icon to {output_path}")

if __name__ == "__main__":
    create_app_icon()
