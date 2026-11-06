#!/usr/bin/env python3
"""Add the chibi 0.5.4 art/animations to a source-compiled single-mod Forge JAR.

The unified Java classes are compiled by ForgeGradle, not copied from the old
chibi mod.  Only the old GeckoLib GEO, PNG, animation and data assets are
imported, keeping the original resource namespace for saves/UV compatibility.
"""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import hashlib
import sys

def bundle(compiled_path, chibi_path, output_path):
    with ZipFile(compiled_path) as compiled, ZipFile(chibi_path) as legacy:
        assert compiled.testzip() is None and legacy.testzip() is None
        names = set(compiled.namelist())
        with ZipFile(output_path, "w", compression=ZIP_DEFLATED, compresslevel=9) as out:
            for item in compiled.infolist():
                if not item.is_dir():
                    out.writestr(item, compiled.read(item.filename))
            for item in legacy.infolist():
                name = item.filename
                if item.is_dir() or name in names:
                    continue
                # Copy ART only. The older @Mod classes and mods.toml MUST NOT follow.
                if not (name.startswith("assets/prodigychibi/") or name.startswith("data/prodigychibi/")):
                    continue
                out.writestr(name, legacy.read(name))
                names.add(name)
            out.writestr("PRODIGY_UNIFIED_BUILD.txt",
                "The Stegosaurus Mod v4.27.0 beta - ONE Forge mod id\n"
                "Forge mod id: prodigycreatures\n"
                "Legacy chibi entity id: prodigychibi:stegosaurus_chibi\n"
                "Legacy chibi egg id: prodigychibi:stegosaurus_chibi_spawn_egg (hidden from creative)\n"
                "Visible egg: prodigycreatures:stegosaurus_spawn_egg\n"
                "Right-click block: chibi; sneak+right-click block: adult\n"
                "Both creatures are compiled together; legacy JAR only contributes resources.\n")
    with ZipFile(output_path) as merged:
        assert merged.testzip() is None
        entries = merged.namelist()
        assert len(entries) == len(set(entries))
        meta = merged.read("META-INF/mods.toml")
        assert meta.count(b"[[mods]]") == 1, "Not one Forge mod"
        assert b'modId="prodigycreatures"' in meta
        assert b'\nmodId="prodigychibi"' not in meta
        assert "com/prodigytown/creatures/ProdigyCreatures.class" in entries
        assert "com/prodigytown/chibi/ChibiAlpha.class" in entries
        assert "com/prodigytown/chibi/ChibiStego.class" in entries
        assert "com/prodigytown/creatures/ProdigyStegosaurusEgg.class" in entries
        assert "assets/prodigychibi/animations/stegosaurus_chibi.animation.json" in entries
        assert "assets/prodigychibi/geo/stegosaurus_chibi.geo.json" in entries
        assert "assets/prodigycreatures/textures/entity/stegosaurus_natural.png" in entries
        assert "assets/prodigychibi/textures/entity/stegosaurus_chibi_natural.png" in entries
        print("ONE MOD BUNDLE VALIDATED", output_path, Path(output_path).stat().st_size, "bytes")
        print("SHA256", hashlib.sha256(Path(output_path).read_bytes()).hexdigest())

if __name__ == "__main__":
    bundle(*sys.argv[1:])
