"""Downloads extra OFL fonts from github.com/google/fonts into res/font and writes fonts_extra.json.

Usage: python -I get_fonts.py
"""
import json
import os
import urllib.parse
import urllib.request

ROOT = os.path.dirname(os.path.abspath(__file__))
FONT_DIR = os.path.join(ROOT, "res", "font")

# (key, label, google/fonts ofl directory)
FAMILIES = [
    ("montserrat", "Montserrat", "montserrat"),
    ("poppins", "Poppins", "poppins"),
    ("dm_sans", "DM Sans", "dmsans"),
    ("manrope", "Manrope", "manrope"),
    ("outfit", "Outfit", "outfit"),
    ("sora", "Sora", "sora"),
    ("jakarta", "Plus Jakarta Sans", "plusjakartasans"),
    ("urbanist", "Urbanist", "urbanist"),
    ("raleway", "Raleway", "raleway"),
    ("josefin", "Josefin Sans", "josefinsans"),
    ("quicksand", "Quicksand", "quicksand"),
    ("comfortaa", "Comfortaa", "comfortaa"),
    ("syne", "Syne", "syne"),
    ("unbounded", "Unbounded", "unbounded"),
    ("orbitron", "Orbitron", "orbitron"),
    ("michroma", "Michroma", "michroma"),
    ("big_shoulders", "Big Shoulders", "bigshouldersdisplay"),
    ("anton", "Anton", "anton"),
    ("oswald", "Oswald", "oswald"),
    ("righteous", "Righteous", "righteous"),
    ("cormorant", "Cormorant Garamond", "cormorantgaramond"),
    ("eb_garamond", "EB Garamond", "ebgaramond"),
    ("fraunces", "Fraunces", "fraunces"),
    ("baskerville", "Libre Baskerville", "librebaskerville"),
    ("lora", "Lora", "lora"),
    ("bodoni", "Bodoni Moda", "bodonimoda"),
    ("prata", "Prata", "prata"),
    ("cinzel", "Cinzel", "cinzel"),
    ("marcellus", "Marcellus", "marcellus"),
    ("italiana", "Italiana", "italiana"),
    ("tenor", "Tenor Sans", "tenorsans"),
    ("abril", "Abril Fatface", "abrilfatface"),
    ("yeseva", "Yeseva One", "yesevaone"),
    ("dancing", "Dancing Script", "dancingscript"),
    ("great_vibes", "Great Vibes", "greatvibes"),
    ("satisfy", "Satisfy", "satisfy"),
    ("lobster", "Lobster", "lobster"),
    ("jetbrains", "JetBrains Mono", "jetbrainsmono"),
    ("space_mono", "Space Mono", "spacemono"),
]


def get(url):
    req = urllib.request.Request(url, headers={"User-Agent": "WidgetStudio-fontfetch"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read()


def pick(files):
    ttf = [f for f in files if f.endswith(".ttf") and "Italic" not in f]
    var = [f for f in ttf if "wght" in f]
    if var:
        return var[0], True
    for pref in ("-Regular.ttf", "-Medium.ttf"):
        for f in ttf:
            if f.endswith(pref):
                return f, False
    return (ttf[0], False) if ttf else (None, False)


def main():
    out = []
    for key, label, d in FAMILIES:
        dest = os.path.join(FONT_DIR, key + ".ttf")
        meta = os.path.join(FONT_DIR, "." + key + ".json")
        try:
            listing = json.loads(get("https://api.github.com/repos/google/fonts/contents/ofl/" + d))
            name, variable = pick([e["name"] for e in listing])
            if not name:
                print("skip (no ttf)", key)
                continue
            if not os.path.exists(dest):
                data = get("https://github.com/google/fonts/raw/main/ofl/%s/%s" % (d, urllib.parse.quote(name)))
                if data[:4] not in (b"\x00\x01\x00\x00", b"true", b"OTTO"):
                    print("skip (not a font)", key)
                    continue
                with open(dest, "wb") as f:
                    f.write(data)
            out.append({"key": key, "label": label, "weightable": variable})
            print("ok", key, name, os.path.getsize(dest))
        except Exception as e:
            print("fail", key, e)
    with open(os.path.join(ROOT, "fonts_extra.json"), "w", encoding="utf-8") as f:
        json.dump(out, f, indent=1)
    print("total", len(out))


if __name__ == "__main__":
    main()
