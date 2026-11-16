"""Genera las misiones por defecto del Cronista (data/prodigycreatures/prodigy_quests/*.json). Uso: python3 quests_gen.py <repo>"""
import json, os, sys
repo = sys.argv[1]
out = os.path.join(repo, 'src/main/resources/data/prodigycreatures/prodigy_quests'); os.makedirs(out, exist_ok=True)
def it(i, n=1): return {"item": i, "count": n}
M = "prodigycreatures:"
Q = []
def q(id, order, cat, title, desc, type, entity, count, xp, items, requires=None, boss=False, mod=None):
    d = {"title": title, "description": desc, "category": cat, "order": order, "type": type, "count": count, "rewards": {"xp": xp, "items": items}}
    if entity: d["entity"] = entity
    if boss: d["boss"] = True
    if requires: d["requires"] = requires
    if mod: d["requires_mod"] = mod
    Q.append((id, d))
C1, C2, C3, C4, C5 = "Cazador", "Domador", "Asedio", "Jefes", "Prodigy"
q("zombis", 10, C1, "Limpieza de zombis", "Los muertos vivientes rondan de noche. Derrota 15 zombis.", "kill", "minecraft:zombie", 15, 120, [it("minecraft:bread", 8), it("minecraft:iron_ingot", 4)])
q("esqueletos", 20, C1, "Huesos y flechas", "Los arqueros esqueleto acechan las cuevas. Derrota 12 esqueletos.", "kill", "minecraft:skeleton", 12, 140, [it("minecraft:arrow", 32), it("minecraft:bow")], "zombis")
q("aranas", 30, C1, "Plaga de aranas", "Sus telas cubren las minas. Elimina 12 aranas.", "kill", "minecraft:spider", 12, 140, [it("minecraft:string", 16), it("minecraft:golden_carrot", 6)], "zombis")
q("creepers", 40, C1, "Silencio, por favor", "Un creeper menos es una casa a salvo. Derrota 8 creepers.", "kill", "minecraft:creeper", 8, 180, [it("minecraft:gunpowder", 16), it("minecraft:diamond", 1)], "esqueletos")
q("endermen", 50, C1, "Los altos del vacio", "Mira sus pies, no sus ojos. Derrota 6 endermen.", "kill", "minecraft:enderman", 6, 260, [it("minecraft:ender_pearl", 4), it("minecraft:experience_bottle", 6)], "creepers")
q("brujas", 60, C1, "Cazabrujas", "Sus pociones envenenan los pantanos. Derrota 3 brujas.", "kill", "minecraft:witch", 3, 220, [it("minecraft:glistering_melon_slice", 4), it("minecraft:golden_apple", 2)], "esqueletos")
q("saqueadores", 70, C1, "Frente a los saqueadores", "Los illagers vigilan las aldeas. Derrota 10 saqueadores.", "kill", "minecraft:pillager", 10, 300, [it("minecraft:crossbow"), it("minecraft:emerald", 8)], "esqueletos")
q("lobo", 100, C2, "Un amigo peludo", "Todo explorador necesita compania. Doma un lobo.", "tame", "minecraft:wolf", 1, 80, [it("minecraft:bone", 8), it("minecraft:cooked_beef", 8)])
q("domador_novato", 110, C2, "Domador novato", "Doma 3 animales de cualquier especie (lobos, gatos, caballos, loros...).", "tame", "*", 3, 200, [it("minecraft:saddle"), it("minecraft:golden_carrot", 10)], "lobo")
q("domar_stego", 120, C2, "Gigante de placas", "Doma un Stegosaurus. Su ensalada especial ayuda.", "tame", M + "stegosaurus", 1, 400, [it(M + "stegosaurus_armor_iron"), it(M + "stegosaurus_feed", 16)], "domador_novato")
q("domar_carno", 130, C2, "Rey de los cuernos", "Doma un Carnotaurus. Que no te vea con hambre.", "tame", M + "carnotaurus", 1, 450, [it(M + "carnotaurus_armor_iron"), it(M + "golden_mutton", 4)], "domar_stego")
q("domar_dilo", 140, C2, "Veneno y plumas", "Doma un Dilophosaurus. Cuidado con su escupitajo.", "tame", M + "dilophosaurus", 1, 450, [it(M + "dilophosaurus_armor_iron"), it("minecraft:cooked_chicken", 8)], "domar_carno")
q("domar_dragon", 150, C2, "Aliento de leyenda", "Doma un dragon (de cualquier mod). Solo los mas valientes lo logran.", "tame", "*dragon*", 1, 1500, [it("minecraft:netherite_ingot", 2), it("minecraft:diamond", 12), it("minecraft:experience_bottle", 24)], "domar_dilo")
q("carno_salvaje", 200, C4, "El cuerno salvaje", "Derrota a un Carnotaurus salvaje. Debe ser el mas fuerte de la manada.", "kill", M + "carnotaurus", 1, 500, [it("minecraft:diamond", 6), it("minecraft:golden_apple", 3)], "creepers")
q("asedio1", 300, C3, "Primera defensa", "Una banda de saqueadores asalta la aldea. Sobrevive y vence 1 asedio.", "raid", None, 1, 600, [it("minecraft:totem_of_undying"), it("minecraft:emerald", 24)], "saqueadores")
q("asedio3", 310, C3, "Veterano de asedios", "Vence 3 asedios. La aldea ya conoce tu nombre.", "raid", None, 3, 1500, [it("minecraft:diamond", 10), it("minecraft:enchanted_golden_apple", 2), it(M + "dilophosaurus_armor_gold")], "asedio1")
q("guardian_anciano", 400, C4, "Pesadilla del oceano", "Derrota a un Guardian Anciano en su monumento.", "kill", "minecraft:elder_guardian", 1, 800, [it("minecraft:sponge", 4), it("minecraft:diamond", 6), it("minecraft:heart_of_the_sea")], "creepers", True)
q("warden", 410, C4, "Silencio en las profundidades", "Derrota al Warden. No hagas ruido... o tendras que correr.", "kill", "minecraft:warden", 1, 2000, [it("minecraft:netherite_scrap", 4), it("minecraft:diamond", 10), it(M + "carnotaurus_armor_diamond")], "endermen", True)
q("wither", 420, C4, "La tormenta de tres cabezas", "Invoca y derrota al Wither.", "kill", "minecraft:wither", 1, 2500, [it("minecraft:nether_star"), it("minecraft:netherite_ingot", 2), it(M + "stegosaurus_armor_diamond")], "endermen", True)
q("dragon_ender", 430, C4, "El fin del Fin", "Derrota al Dragon del Ender.", "kill", "minecraft:ender_dragon", 1, 5000, [it("minecraft:elytra"), it("minecraft:netherite_ingot", 4), it(M + "dilophosaurus_armor_netherite"), it(M + "carnotaurus_armor_netherite")], "wither", True)
q("dragon_mod", 440, C4, "Cazadragones", "Derrota a un dragon de cualquier mod. Su tesoro es tuyo.", "kill", "*dragon*", 1, 3000, [it("minecraft:netherite_ingot", 3), it("minecraft:diamond_block", 2), it(M + "stegosaurus_armor_netherite")], "domar_dragon", True)
for i, (id, d) in enumerate(Q):
    json.dump(d, open(os.path.join(out, id + '.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=2); open(os.path.join(out, id + '.json'), 'a').write('\n')
print(len(Q), 'misiones')
