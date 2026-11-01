"""Play mağazası görselleri: 512x512 uygulama simgesi ve 1024x500 öne çıkan görsel.

Simge ic_launcher_foreground.xml ile aynı çizimdir; öne çıkan görsel uygulamadaki akşam gökyüzü ve cami siluetidir.
Kullanım: python fastlane/make_graphics.py
"""
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

OUT = Path(__file__).parent / "metadata" / "android" / "en-US" / "images"
SS = 4  # kenar yumuşatma için büyük çizip küçültme katsayısı

GREEN = (15, 92, 77)
GOLD = (232, 198, 106)
STAR = [(70, 40), (71.41, 44.06), (75.71, 44.15), (72.28, 46.74), (73.53, 50.85), (70, 48.4),
        (66.47, 50.85), (67.72, 46.74), (64.29, 44.15), (68.59, 44.06)]


def draw_emblem(d, scale, ox, oy, cut_color, fg="white", star=GOLD):
    """Hilal ve yıldız; koordinatlar simge vektörünün 108'lik viewport'unda."""
    def p(x, y):
        return ox + x * scale, oy + y * scale
    d.ellipse([*p(28, 32), *p(72, 76)], fill=fg)
    d.ellipse([*p(39, 31), *p(77, 69)], fill=cut_color)
    if star is not None:
        d.polygon([p(x, y) for x, y in STAR], fill=star)


def icon():
    size = 512 * SS
    img = Image.new("RGB", (size, size), GREEN)
    # Uyarlanabilir simgenin güvenli alanı (18..90) tüm kareyi kaplasın
    scale = size / 72
    draw_emblem(ImageDraw.Draw(img), scale, -18 * scale, -18 * scale, GREEN)
    img.resize((512, 512), Image.LANCZOS).save(OUT / "icon.png")


def lerp(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def feature():
    w, h = 1024 * SS, 500 * SS
    top, mid, horizon = (35, 22, 64), (122, 49, 87), (217, 100, 60)
    img = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / h
        d.line([(0, y), (w, y)], fill=lerp(top, mid, t / 0.55) if t < 0.55 else lerp(mid, horizon, (t - 0.55) / 0.45))

    rnd = random.Random(42)
    for _ in range(60):
        x, y, r = rnd.random() * w, rnd.random() * h * 0.5, (0.6 + rnd.random()) * SS * 1.6
        d.ellipse([x - r, y - r, x + r, y + r], fill=(255, 255, 255))

    # Cami silueti (uygulamadaki Sky.kt ile aynı oranlar), sağ alta
    sil = (10, 14, 28)
    u = 3.4 * SS
    cx, ground = w * 0.74, h

    def X(v):
        return cx + (v - 50) * u

    def Y(v):
        return ground - v * u
    d.rectangle([0, Y(2), w, h], fill=sil)
    d.rectangle([X(30), Y(18), X(70), h], fill=sil)
    d.rectangle([X(20), Y(11), X(30), h], fill=sil)
    d.rectangle([X(70), Y(11), X(80), h], fill=sil)
    for l, base, r, hh in [(35, 18, 65, 18), (21, 11, 29, 7), (71, 11, 79, 7), (28, 18, 36, 6), (64, 18, 72, 6)]:
        d.pieslice([X(l), Y(base) - hh * u, X(r), Y(base) + hh * u], 180, 360, fill=sil)
    d.rectangle([X(49.6), Y(41), X(50.4), Y(35)], fill=sil)
    for m in (10, 90):
        d.rectangle([X(m - 1.5), Y(44), X(m + 1.5), h], fill=sil)
        d.rectangle([X(m - 2.6), Y(31.5), X(m + 2.6), Y(30)], fill=sil)
        d.rectangle([X(m - 2.3), Y(40.5), X(m + 2.3), Y(39)], fill=sil)
        d.polygon([(X(m - 1.5), Y(44)), (X(m), Y(54)), (X(m + 1.5), Y(44))], fill=sil)
    for wx in (36, 43, 54, 61):
        d.rounded_rectangle([X(wx), Y(12), X(wx + 2.6), Y(6)], radius=1.3 * u, fill=GOLD)

    # Hilal ve yıldız, sol üstte: hilal maskeyle oyulur ki arkadaki gökyüzü görünsün
    mask = Image.new("L", img.size, 0)
    draw_emblem(ImageDraw.Draw(mask), 2.2 * SS, 40 * SS, 30 * SS, 0, fg=255, star=None)
    img.paste(Image.new("RGB", img.size, "white"), mask=mask)
    star = Image.new("L", img.size, 0)
    s, ox, oy = 2.2 * SS, 40 * SS, 30 * SS
    ImageDraw.Draw(star).polygon([(ox + x * s, oy + y * s) for x, y in STAR], fill=255)
    img.paste(Image.new("RGB", img.size, GOLD), mask=star)

    img = img.resize((1024, 500), Image.LANCZOS)
    d = ImageDraw.Draw(img)
    title = ImageFont.truetype("C:/Windows/Fonts/seguisb.ttf", 92)
    sub = ImageFont.truetype("C:/Windows/Fonts/segoeui.ttf", 34)
    d.text((70, 215), "Ezan Saati", font=title, fill="white")
    d.text((74, 330), "Prayer Times · Namaz Vakitleri", font=sub, fill=GOLD)
    img.save(OUT / "featureGraphic.png")


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    icon()
    feature()
    print("Kaydedildi:", OUT)
