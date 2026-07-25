"""Generate Wear & Wash launcher assets from the transparent master logo."""

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "design" / "wearwash-logo-transparent.png"
RES = ROOT / "app" / "src" / "main" / "res"
BRAND_BACKGROUND = "#E238CA"


def fitted_logo(source: Image.Image, size: int, coverage: float) -> Image.Image:
    bounds = source.getchannel("A").getbbox()
    if bounds is None:
        raise ValueError("The logo has no visible pixels")
    artwork = source.crop(bounds)
    target = round(size * coverage)
    artwork.thumbnail((target, target), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    position = ((size - artwork.width) // 2, (size - artwork.height) // 2)
    canvas.alpha_composite(artwork, position)
    return canvas


def save_png(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, "PNG", optimize=True)


def legacy_icon(source: Image.Image, size: int) -> Image.Image:
    scale = 4
    working_size = size * scale
    icon = Image.new("RGBA", (working_size, working_size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(icon)
    radius = round(working_size * 0.22)
    draw.rounded_rectangle(
        (0, 0, working_size - 1, working_size - 1),
        radius=radius,
        fill=BRAND_BACKGROUND,
    )
    icon.alpha_composite(fitted_logo(source, working_size, coverage=0.76))
    return icon.resize((size, size), Image.Resampling.LANCZOS)


def main() -> None:
    source = Image.open(SOURCE).convert("RGBA")
    save_png(
        fitted_logo(source, 512, coverage=0.88),
        RES / "drawable-nodpi" / "wearwash_logo.png",
    )
    save_png(
        fitted_logo(source, 432, coverage=0.62),
        RES / "drawable-nodpi" / "ic_launcher_foreground.png",
    )

    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    for directory, size in densities.items():
        icon = legacy_icon(source, size)
        save_png(icon, RES / directory / "ic_launcher.png")
        save_png(icon, RES / directory / "ic_launcher_round.png")


if __name__ == "__main__":
    main()
