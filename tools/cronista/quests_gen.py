"""Genera las misiones por defecto del Cronista (data/prodigycreatures/prodigy_quests/*.json). Uso: python3 quests_gen.py <repo>"""
import json, os, sys
repo = sys.argv[1]
out = os.path.join(repo, 'src/main/resources/data/prodigycreatures/prodigy_quests'); os.makedirs(out, exist_ok=True)
def it(i, n=1): return {"item": i, "count": n}
M = "prodigycreatures:"
Q = []
def q(id, order, cat, title, desc, type, entity, count, xp, items, requires=None, boss=False, mod=None, metric=None, building=None):
    d = {"title": title, "description": desc, "category": cat, "order": order, "type": type, "count": count, "rewards": {"xp": xp, "items": items}}
    if entity: d["entity"] = entity
    if boss: d["boss"] = True
    if requires: d["requires"] = requires
    if mod: d["requires_mod"] = mod
    if metric: d["metric"] = metric
    if building: d["building"] = building
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

# ---------------------------------------------------------------- misiones de otros mods (solo aparecen si el mod esta instalado)
D, E = "iceandfire", "legendary_monsters"
SD = "saintsdragons,saints_dragons"; SDE = "saintsdragons:*,saints_dragons:*"
IAF_DRAGONS = "iceandfire:fire_dragon,iceandfire:ice_dragon,iceandfire:lightning_dragon"
# MCA Reborn: aldeas (poblacion, edificios, tipos de edificio, nivel). Nivel = 1 + poblacion/4 + tipos de edificio distintos/2.
CM = "Aldeas MCA"
q("mca_poblacion_5", 500, CM, "Un pueblo con vida", "Haz que tu aldea de MCA tenga 5 habitantes. Acercate a ella y espera: la poblacion crece sola si hay camas libres.", "village", None, 5, 150, [it("minecraft:bread", 12), it("minecraft:emerald", 8)], mod="mca", metric="population")
q("mca_casas_5", 510, CM, "Casas para todos", "Construye y registra 5 casas completas en la aldea.", "village", None, 5, 250, [it("minecraft:oak_door", 6), it("minecraft:white_bed", 3), it("minecraft:emerald", 10)], "mca_poblacion_5", mod="mca", metric="building", building="house")
q("mca_herrero", 520, CM, "El yunque de la aldea", "Levanta una herreria completa dentro de la aldea.", "village", None, 1, 200, [it("minecraft:anvil"), it("minecraft:iron_ingot", 12)], "mca_casas_5", mod="mca", metric="building", building="blacksmith")
q("mca_posada", 530, CM, "Cama y comida caliente", "Construye una posada completa para los viajeros.", "village", None, 1, 200, [it("minecraft:cake"), it("minecraft:golden_carrot", 12)], "mca_casas_5", mod="mca", metric="building", building="inn")
q("mca_biblioteca", 540, CM, "Saber en estantes", "Construye una biblioteca completa.", "village", None, 1, 220, [it("minecraft:bookshelf", 8), it("minecraft:experience_bottle", 8)], "mca_casas_5", mod="mca", metric="building", building="library")
q("mca_poblacion_10", 550, CM, "Una aldea en marcha", "Alcanza 10 habitantes en la aldea.", "village", None, 10, 400, [it("minecraft:diamond", 3), it("minecraft:golden_apple", 2)], "mca_poblacion_5", mod="mca", metric="population")
q("mca_edificios_8", 560, CM, "Arquitecto de la aldea", "Ten 8 edificios completos registrados en una misma aldea.", "village", None, 8, 450, [it("minecraft:emerald", 24), it("minecraft:diamond", 3)], "mca_casas_5", mod="mca", metric="buildings")
q("mca_variedad_6", 570, CM, "De todo un poco", "Reune 6 tipos distintos de edificio (casa, herreria, posada, biblioteca, granja...).", "village", None, 6, 600, [it("minecraft:diamond", 5), it("minecraft:experience_bottle", 12)], "mca_edificios_8", mod="mca", metric="building_types")
q("mca_nivel_3", 580, CM, "Aldea nivel 3", "Lleva tu aldea al nivel 3 (nivel = 1 + poblacion/4 + tipos de edificio distintos/2).", "village", None, 3, 350, [it("minecraft:emerald", 20), it("minecraft:iron_block", 2)], "mca_poblacion_5", mod="mca", metric="level")
q("mca_nivel_6", 590, CM, "Aldea nivel 6", "Lleva tu aldea al nivel 6.", "village", None, 6, 900, [it("minecraft:diamond", 8), it("minecraft:totem_of_undying")], "mca_nivel_3", mod="mca", metric="level")
q("mca_poblacion_20", 600, CM, "Ciudad de leyenda", "Alcanza 20 habitantes en la aldea.", "village", None, 20, 1500, [it("minecraft:enchanted_golden_apple", 2), it("minecraft:diamond", 12), it(M + "stegosaurus_armor_gold")], "mca_poblacion_10", mod="mca", metric="population")
q("mca_nivel_10", 610, CM, "Capital de Prodigy", "Lleva tu aldea al nivel 10: una verdadera capital.", "village", None, 10, 2500, [it("minecraft:netherite_ingot", 2), it("minecraft:beacon"), it(M + "dilophosaurus_armor_diamond")], "mca_nivel_6", mod="mca", metric="level")

# Ice and Fire
CI = "Hielo y Fuego"
q("iaf_hipogrifo", 700, CI, "Alas de leon", "Doma un hipogrifo.", "tame", "iceandfire:hippogryph", 1, 300, [it("minecraft:saddle"), it("minecraft:golden_carrot", 16)], mod=D)
q("iaf_cockatrice", 710, CI, "Mirada de gallo", "Derrota a 3 cocatrices. No les mires a los ojos.", "kill", "iceandfire:cockatrice", 3, 300, [it("minecraft:diamond", 2), it("minecraft:experience_bottle", 6)], mod=D)
q("iaf_sirena", 720, CI, "Canto traicionero", "Derrota a 3 sirenas.", "kill", "iceandfire:siren", 3, 350, [it("minecraft:heart_of_the_sea"), it("minecraft:diamond", 3)], "iaf_cockatrice", mod=D)
q("iaf_trol", 730, CI, "Puente de trolls", "Derrota a 5 trolls.", "kill", "iceandfire:troll", 5, 400, [it("minecraft:iron_block", 2), it("minecraft:cooked_beef", 16)], "iaf_cockatrice", mod=D)
q("iaf_gorgona", 740, CI, "Piedra eterna", "Derrota a una gorgona.", "kill", "iceandfire:gorgon", 1, 600, [it("minecraft:diamond", 5), it("minecraft:golden_apple", 3)], "iaf_sirena", mod=D)
q("iaf_ciclope", 750, CI, "Gigante de un ojo", "Derrota a un cíclope.", "kill", "iceandfire:cyclops", 1, 700, [it("minecraft:diamond", 6), it("minecraft:experience_bottle", 12)], "iaf_trol", True, mod=D)
q("iaf_hidra", 760, CI, "Cabezas cortadas", "Derrota a una hidra.", "kill", "iceandfire:hydra", 1, 900, [it("minecraft:diamond", 8), it("minecraft:golden_apple", 4)], "iaf_gorgona", True, mod=D)
q("iaf_serpiente", 770, CI, "Terror de las mareas", "Derrota a una serpiente marina.", "kill", "iceandfire:sea_serpent", 1, 800, [it("minecraft:nautilus_shell", 2), it("minecraft:diamond", 6)], "iaf_sirena", True, mod=D)
q("iaf_domar_dragon", 780, CI, "Amo del dragon", "Consigue un dragon de Hielo y Fuego como tuyo (cria o doma uno).", "tame", IAF_DRAGONS, 1, 2000, [it("minecraft:netherite_ingot", 2), it("minecraft:diamond", 12), it(M + "carnotaurus_armor_gold")], "iaf_hipogrifo", mod=D)
q("iaf_matadragones", 790, CI, "Matadragones", "Derrota a un dragon de Hielo y Fuego salvaje.", "kill", IAF_DRAGONS, 1, 3500, [it("minecraft:netherite_ingot", 3), it("minecraft:diamond_block", 2), it(M + "stegosaurus_armor_netherite")], "iaf_hidra", True, mod=D)
q("iaf_lich", 800, CI, "El que no descansa", "Derrota a un Dread Lich.", "kill", "iceandfire:dread_lich", 1, 3000, [it("minecraft:netherite_ingot", 2), it("minecraft:nether_star")], "iaf_matadragones", True, mod=D)

# Saint's Dragons
CS = "Dragones de Saint"
q("sd_cazar", 900, CS, "Escamas ajenas", "Derrota a 5 criaturas de Saint's Dragons.", "kill", SDE, 5, 500, [it("minecraft:diamond", 4), it("minecraft:experience_bottle", 10)], mod=SD)
q("sd_domar", 910, CS, "Primer vuelo", "Doma un dragon de Saint's Dragons.", "tame", SDE, 1, 1800, [it("minecraft:netherite_scrap", 4), it("minecraft:diamond", 10), it(M + "dilophosaurus_armor_diamond")], mod=SD)
q("sd_domar_3", 920, CS, "Escuadron de dragones", "Ten 3 dragones domados de Saint's Dragons.", "tame", SDE, 3, 3000, [it("minecraft:netherite_ingot", 3), it("minecraft:diamond_block", 2), it("minecraft:elytra")], "sd_domar", mod=SD)

# Legendary Monsters
CL = "Monstruos Legendarios"
LM_BOSSES = ",".join("legendary_monsters:*" + k + "*" for k in ("skeletosaurus", "possessed_paladin", "overgrown_colossus", "cumulonimbus", "dune_sentinel", "frostbitten_golem", "withered_abomination", "lava_eater", "chorusent", "obliterator", "shulker_mimic", "ancient_guardian"))
q("lm_cazador", 1000, CL, "Cazador de rarezas", "Derrota a 10 criaturas de Legendary Monsters.", "kill", "legendary_monsters:*", 10, 500, [it("minecraft:diamond", 4), it("minecraft:experience_bottle", 8)], mod=E)
LMQ = [("skeletosaurus", "Esqueleto colosal", "Derrota al Skeletosaurus.", 1200), ("possessed_paladin", "Paladin poseido", "Derrota al Paladin Poseido.", 1200), ("overgrown_colossus", "Coloso cubierto de musgo", "Derrota al Coloso Cubierto.", 1500),
       ("dune_sentinel", "Centinela de las dunas", "Derrota al Centinela de las Dunas.", 1500), ("frostbitten_golem", "Golem helado", "Derrota al Golem Escarchado.", 1500), ("withered_abomination", "Abominacion marchita", "Derrota a la Abominacion Marchita.", 2000),
       ("lava_eater", "Devoralava", "Derrota al Devoralava.", 2000), ("cumulonimbus", "Golem de nubes", "Derrota a Cumulonimbus, el Golem de Nubes.", 1800), ("chorusent", "Chorusent", "Derrota al Chorusent.", 2200), ("shulker_mimic", "Mimico shulker", "Derrota al Mimico Shulker.", 1500),
       ("ancient_guardian", "Guardian ancestral", "Derrota al Guardian Ancestral.", 1800), ("obliterator", "El Obliterador", "Derrota al Obliterador. Es el mas temible.", 4000)]
for i, (k, t, dsc, xp) in enumerate(LMQ):
    q("lm_" + k, 1010 + i * 10, CL, t, dsc, "kill", "legendary_monsters:*" + k + "*", 1, xp, [it("minecraft:diamond", 4 + i // 3), it("minecraft:golden_apple", 2)] + ([it("minecraft:netherite_scrap", 2)] if i >= 6 else []), "lm_cazador", True, mod=E)
q("lm_cazaleyendas", 1150, CL, "Cazaleyendas", "Derrota a 5 jefes de Legendary Monsters (cualquiera).", "kill", LM_BOSSES, 5, 4000, [it("minecraft:netherite_ingot", 3), it("minecraft:diamond_block", 2), it(M + "carnotaurus_armor_netherite")], "lm_cazador", True, mod=E)

# Raxiors of Raxor (ror)
CR = "Raxiors of Raxor"
q("ror_domar", 1200, CR, "Compania prehistorica", "Doma una criatura de Raxiors of Raxor.", "tame", "ror:*", 1, 400, [it("minecraft:saddle"), it("minecraft:cooked_beef", 16)], mod="ror")
q("ror_cazar", 1210, CR, "Caza mayor", "Derrota a 10 criaturas de Raxiors of Raxor.", "kill", "ror:*", 10, 500, [it("minecraft:diamond", 4), it("minecraft:experience_bottle", 8)], mod="ror")
q("ror_domar_3", 1220, CR, "Manada propia", "Ten 3 criaturas de Raxiors of Raxor domadas.", "tame", "ror:*", 3, 1200, [it("minecraft:diamond", 8), it(M + "stegosaurus_armor_iron")], "ror_domar", mod="ror")
q("ror_gran_cazador", 1230, CR, "Gran cazador", "Derrota a 30 criaturas de Raxiors of Raxor.", "kill", "ror:*", 30, 1800, [it("minecraft:netherite_ingot"), it("minecraft:diamond", 10)], "ror_cazar", mod="ror")

for i, (id, d) in enumerate(Q):
    json.dump(d, open(os.path.join(out, id + '.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=2); open(os.path.join(out, id + '.json'), 'a').write('\n')
print(len(Q), 'misiones')
