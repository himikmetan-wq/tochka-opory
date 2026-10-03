#!/usr/bin/env python3
"""Decode the base64-encoded source icon and generate all Android launcher
icon densities + a simple splash color stay untouched (splash is XML, not PNG).
Run from android-app/android as working directory (matches the CI workflow)."""
import base64
import os

HERE = os.path.dirname(os.path.abspath(__file__))
B64_PATH = os.path.join(HERE, "icon_512_base64.txt")
ANDROID_RES = os.path.join(HERE, "..", "android", "app", "src", "main", "res")

from PIL import Image
import io

with open(B64_PATH, "r") as f:
    raw = base64.b64decode(f.read().strip())

src = Image.open(io.BytesIO(raw)).convert("RGBA")
assert src.size == (512, 512), f"unexpected source size {src.size}"

sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

for density, size in sizes.items():
    out_dir = os.path.join(ANDROID_RES, f"mipmap-{density}")
    os.makedirs(out_dir, exist_ok=True)
    im = src.resize((size, size), Image.LANCZOS)
    im.save(os.path.join(out_dir, "ic_launcher.png"))
    im.save(os.path.join(out_dir, "ic_launcher_round.png"))
    im.save(os.path.join(out_dir, "ic_launcher_foreground.png"))
    print(f"{density}: {size}x{size} written")

print("Icon generation complete.")
