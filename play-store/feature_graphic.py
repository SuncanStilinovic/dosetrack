"""Play Store feature graphic (1024x500, RGB, no alpha).

Usage, from the repository root:
    python3 play-store/feature_graphic.py play-store screenshots/1_log_drug.png

Needs Pillow and the Noto Sans fonts (Debian/Ubuntu: fonts-noto-core).
"""
import sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter

O, SHOT = sys.argv[1:]
W, H, SS = 1024, 500, 2
w, h = W * SS, H * SS
F = "/usr/share/fonts/truetype/noto/NotoSans-"
bold = lambda s: ImageFont.truetype(F + "Bold.ttf", s * SS)
reg = lambda s: ImageFont.truetype(F + "Regular.ttf", s * SS)
bg = Image.new("RGB", (w, h))
px = bg.load()
c1, c2 = (0x19, 0x76, 0xD2), (0x0D, 0x47, 0xA1)
for y in range(h):
    for x in range(w):
        t = (x / w * 0.6 + y / h * 0.4)
        px[x, y] = tuple(int(a + (b - a) * t) for a, b in zip(c1, c2))
d = ImageDraw.Draw(bg)
L = 72 * SS
# row: icon tile + name
T = 88 * SS
X0, Y0 = L, 112 * SS
d.rounded_rectangle((X0, Y0, X0 + T, Y0 + T), radius=22 * SS, fill="white")
k = T / 108
pts = [(49, 24), (59, 24), (59, 44), (79, 44), (79, 54), (59, 54), (59, 74), (49, 74), (49, 54), (29, 54), (29, 44), (49, 44)]  # same as ic_launcher_foreground
d.polygon([(X0 + x * k, Y0 + y * k) for x, y in pts], fill=(0x15, 0x65, 0xC0))
d.text((X0 + T + 24 * SS, Y0 + T / 2), "DoseTrack", font=bold(60), fill="white", anchor="lm")
# taglines
d.text((L, 268 * SS), "Log every dose in seconds.", font=reg(28), fill=(0xE3, 0xF2, 0xFD), anchor="ls")
d.text((L, 306 * SS), "Rotate sites. Never miss a dose.", font=reg(28), fill=(0xE3, 0xF2, 0xFD), anchor="ls")
px0 = L
py = 344 * SS
for label in ["Offline", "No account", "No ads"]:
    f = bold(18)
    tw = d.textlength(label, font=f)
    d.rounded_rectangle((px0, py, px0 + tw + 32 * SS, py + 40 * SS), radius=20 * SS, outline=(255, 255, 255), width=2 * SS)
    d.text((px0 + 16 * SS, py + 20 * SS), label, font=f, fill="white", anchor="lm")
    px0 += tw + 44 * SS
shot = Image.open(SHOT).convert("RGB")
sw = 250 * SS
sh = int(shot.height * sw / shot.width)
scr = shot.resize((sw, sh), Image.LANCZOS)
fx, fy, b = 690 * SS, 52 * SS, 12 * SS
frame = (fx, fy, fx + sw + 2 * b, fy + sh + 2 * b)
sl = Image.new("L", (w, h), 0)
ImageDraw.Draw(sl).rounded_rectangle((frame[0] + 10 * SS, frame[1] + 16 * SS, frame[2] + 10 * SS, frame[3] + 16 * SS), radius=44 * SS, fill=110)
sl = sl.filter(ImageFilter.GaussianBlur(18 * SS))
bg = Image.composite(Image.new("RGB", (w, h), (5, 25, 60)), bg, sl)
d = ImageDraw.Draw(bg)
d.rounded_rectangle(frame, radius=44 * SS, fill=(0x1B, 0x1F, 0x24))
m = Image.new("L", (sw, sh), 0)
ImageDraw.Draw(m).rounded_rectangle((0, 0, sw, sh), radius=34 * SS, fill=255)
bg.paste(scr, (fx + b, fy + b), m)
bg.resize((W, H), Image.LANCZOS).save(f"{O}/feature_graphic_1024x500.png", optimize=True)
