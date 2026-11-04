#!/usr/bin/env python3
"""Build one Forge mod archive with two @Mod entrypoints and stable entity IDs."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import hashlib
import sys

def merge(adult_path, chibi_path, output_path):
    with ZipFile(adult_path) as adult, ZipFile(chibi_path) as chibi:
        assert adult.testzip() is None and chibi.testzip() is None, "Invalid source JAR"
        a = adult.read("META-INF/mods.toml").decode()
        b = chibi.read("META-INF/mods.toml").decode()
        # Use only one global loader header, two [[mods]] blocks, distinct dependency groups.
        header = 'modLoader="javafml"\nloaderVersion="[47,)"\nlicense="MIT"\n\n'
        a = a[a.index("[[mods]]"):]
        b = b[b.index("[[mods]]"):]
        b = b.replace('version="0.5.2-alpha.1"', 'version="0.5.4-integrated.1"')
        b = b.replace('displayName="Prodigy Creatures Chibi Alpha 0.5.2"', 'displayName="Prodigy Creatures Chibi 0.5.4"')
        mods = header + a.rstrip() + "\n\n" + b.rstrip() + "\n"
        seen = set()
        with ZipFile(output_path, "w", compression=ZIP_DEFLATED, compresslevel=9) as merged:
            for archive in (adult, chibi):
                for entry in archive.infolist():
                    name = entry.filename
                    if name.endswith("/") or name in seen:
                        continue
                    if name == "META-INF/mods.toml":
                        merged.writestr(name, mods.encode())
                    elif name == "META-INF/MANIFEST.MF" and archive is chibi:
                        continue
                    else:
                        merged.writestr(name, archive.read(name))
                    seen.add(name)
            merged.writestr("PRODIGY_INTEGRATED_VERSION.txt",
                "Prodigy Creatures 4.2.0 integrated beta\n"
                "Adult modid: prodigycreatures; Chibi modid: prodigychibi\n"
                "Adult animations: adapted native motion; Chibi: original GeckoLib animations\n"
                "Requires Forge 1.20.1, GeckoLib 4.4.9+ and ROR beta 0.71.\n")
    with ZipFile(output_path) as check:
        assert check.testzip() is None
        entries = check.namelist()
        assert len(entries) == len(set(entries))
        assert "com/prodigytown/creatures/ProdigyCreatures.class" in entries
        assert "com/prodigytown/chibi/ChibiAlpha.class" in entries
        assert check.read("META-INF/mods.toml").count(b"[[mods]]") == 2
        assert check.read("META-INF/mods.toml").count(b'\nmodId="prodigychibi"') == 1
        assert check.read("META-INF/mods.toml").count(b'\nmodId="prodigycreatures"') == 1
        assert "assets/prodigychibi/animations/stegosaurus_chibi.animation.json" in entries
        assert "assets/prodigycreatures/textures/entity/stegosaurus_natural.png" in entries
    print(f"READY {output_path}: {Path(output_path).stat().st_size} bytes")
    print("SHA256", hashlib.sha256(Path(output_path).read_bytes()).hexdigest())

if __name__ == "__main__":
    merge(*sys.argv[1:])
