"""Builds docs/img/demo.gif from the numbered PNG frames written by `npm run capture`.

    docker run --rm -v "$PWD:/repo" -w /repo python:3.13.16-alpine sh -c \
      "pip install --quiet pillow==12.3.0 && python e2e/capture/make_gif.py e2e/capture-frames docs/img/demo.gif"
"""
import sys
from pathlib import Path

from PIL import Image

WIDTH = 360
FRAME_MS = 1800
LAST_FRAME_MS = 3000


def main(frames_dir: str, output: str) -> None:
    files = sorted(Path(frames_dir).glob("*.png"))
    if len(files) < 2:
        raise SystemExit(f"Se esperaban al menos 2 fotogramas en {frames_dir}")
    frames = []
    for file in files:
        image = Image.open(file).convert("RGB")
        height = round(image.height * WIDTH / image.width)
        resized = image.resize((WIDTH, height), Image.Resampling.LANCZOS)
        frames.append(resized.quantize(colors=128, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE))
    durations = [FRAME_MS] * (len(frames) - 1) + [LAST_FRAME_MS]
    frames[0].save(
        output,
        save_all=True,
        append_images=frames[1:],
        duration=durations,
        loop=0,
        optimize=True,
        disposal=2,
    )
    print(f"{output}: {len(frames)} fotogramas, {Path(output).stat().st_size // 1024} KB")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("Uso: make_gif.py <carpeta_de_fotogramas> <salida.gif>")
    main(sys.argv[1], sys.argv[2])
