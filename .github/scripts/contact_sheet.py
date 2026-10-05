"""Builds one contact sheet per device folder of emulator renders: <in_dir>/<device>/*.png -> <out_dir>/<device>.jpg"""
import os
import sys
from PIL import Image, ImageDraw

src, out = sys.argv[1], sys.argv[2]
os.makedirs(out, exist_ok=True)
for device in sorted(os.listdir(src)):
    folder = os.path.join(src, device)
    if not os.path.isdir(folder):
        continue
    names = sorted(f for f in os.listdir(folder) if f.endswith(".png"))
    ims = [Image.open(os.path.join(folder, n)).convert("RGB") for n in names]
    w, h = ims[0].size
    tw = 360 if h > w else 480
    th = int(h * tw / w)
    cols = 5 if h > w else 4
    rows = (len(ims) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * tw, rows * (th + 24)), (24, 24, 24))
    d = ImageDraw.Draw(sheet)
    for i, (n, im) in enumerate(zip(names, ims)):
        x, y = (i % cols) * tw, (i // cols) * (th + 24)
        sheet.paste(im.resize((tw, th)), (x, y))
        d.text((x + 6, y + th + 5), n[:-4], fill=(230, 230, 230))
    sheet.save(os.path.join(out, device + ".jpg"), quality=85)
    print(device, len(ims))
