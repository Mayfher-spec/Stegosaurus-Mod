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
q("cronista_hogar", 5, C5, "Un hogar para la Cronista", "La Cronista viaja sin rumbo. Prepárale un hogar cerca de ti: una cama, un atril para sus crónicas, luz y un techo sobre la cama. Luego habla con ella.", "home", None, 4, 400, [it("minecraft:diamond", 3), it("minecraft:emerald", 16), it("minecraft:golden_apple", 2), it("minecraft:experience_bottle", 6)])
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
q("mca_poblacion_20", 600, CM, "Ciudad de leyenda", "Alcanza 20 habitantes en la aldea.", "village", None, 20, 1500, [it("minecraft:enchanted_golden_apple", 2), it("minecraft:diamond", 12), it(M + "stegosaurus_armor_gold")], "mca_poblacion_10", mod="mca", metric="population")
# Nivel de aldea = espiritu de Townstead (1-5 segun los puntos de sus edificios: 25 / 60 / 140 / 300 / 600)
CT = "Aldeas MCA"
q("town_nivel_1", 620, CT, "Aldea con identidad", "Tu aldea de Townstead debe alcanzar el nivel 1 de espiritu (25 puntos en un mismo espiritu).", "village", None, 1, 250, [it("minecraft:emerald", 16), it("minecraft:bread", 16)], "mca_casas_5", mod="townstead", metric="level")
q("town_nivel_2", 630, CT, "Aldea en ascenso", "Lleva tu aldea al nivel 2 de espiritu.", "village", None, 2, 500, [it("minecraft:emerald", 24), it("minecraft:diamond", 3)], "town_nivel_1", mod="townstead", metric="level")
q("town_nivel_3", 640, CT, "Aldea consolidada", "Lleva tu aldea al nivel 3 de espiritu.", "village", None, 3, 900, [it("minecraft:diamond", 6), it("minecraft:golden_apple", 3)], "town_nivel_2", mod="townstead", metric="level")
q("town_nivel_4", 650, CT, "Gran poblacion", "Lleva tu aldea al nivel 4 de espiritu.", "village", None, 4, 1600, [it("minecraft:diamond", 10), it("minecraft:totem_of_undying")], "town_nivel_3", mod="townstead", metric="level")
q("town_nivel_5", 660, CT, "Capital de Prodigy", "Lleva tu aldea al nivel 5, el maximo espiritu de aldea.", "village", None, 5, 3000, [it("minecraft:netherite_ingot", 2), it("minecraft:beacon"), it(M + "dilophosaurus_armor_diamond")], "town_nivel_4", mod="townstead", metric="level")
for sid, nm, b in (("martial", "marcial", "Guarnicion"), ("scholar", "erudita", "Escuela"), ("industrious", "industrial", "Forja"), ("pastoral", "pastoral", "Granja"), ("nautical", "nautica", "Puerto")):
    q("town_" + sid, 670 + 10 * ["martial", "scholar", "industrious", "pastoral", "nautical"].index(sid), CT, "Aldea " + nm, "Haz que el espiritu " + nm + " de tu aldea llegue al nivel 2 (60 puntos).", "village", None, 2, 700, [it("minecraft:diamond", 4), it("minecraft:experience_bottle", 10)], "town_nivel_1", mod="townstead", metric="spirit", building=sid)

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

# ---------------------------------------------------------------- presentacion: icono y texto del objetivo
ICONS = {
 "cronista_hogar": "red_bed", "zombis": "rotten_flesh", "esqueletos": "bone", "aranas": "spider_eye", "creepers": "gunpowder", "endermen": "ender_pearl", "brujas": "potion", "saqueadores": "crossbow",
 "lobo": "bone", "domador_novato": "lead", "domar_stego": M + "stegosaurus_spawn_egg", "domar_carno": M + "carnotaurus_spawn_egg", "domar_dilo": M + "dilophosaurus_spawn_egg", "domar_dragon": "dragon_egg",
 "carno_salvaje": M + "carnotaurus_spawn_egg", "asedio1": "shield", "asedio3": "totem_of_undying", "guardian_anciano": "prismarine_shard", "warden": "echo_shard", "wither": "nether_star", "dragon_ender": "dragon_head", "dragon_mod": "dragon_breath",
 "mca_poblacion_5": "emerald", "mca_casas_5": "oak_door", "mca_herrero": "anvil", "mca_posada": "cake", "mca_biblioteca": "bookshelf", "mca_poblacion_10": "emerald", "mca_edificios_8": "bricks", "mca_variedad_6": "clock", "mca_poblacion_20": "emerald_block",
 "town_nivel_1": "lectern", "town_nivel_2": "lectern", "town_nivel_3": "beacon", "town_nivel_4": "beacon", "town_nivel_5": "beacon",
 "town_martial": "iron_sword", "town_scholar": "enchanted_book", "town_industrious": "iron_pickaxe", "town_pastoral": "wheat", "town_nautical": "fishing_rod",
 "iaf_hipogrifo": "feather", "iaf_cockatrice": "egg", "iaf_sirena": "nautilus_shell", "iaf_trol": "iron_axe", "iaf_gorgona": "ender_eye", "iaf_ciclope": "iron_block", "iaf_hidra": "poisonous_potato", "iaf_serpiente": "heart_of_the_sea",
 "iaf_domar_dragon": "dragon_egg", "iaf_matadragones": "netherite_sword", "iaf_lich": "wither_skeleton_skull",
 "sd_cazar": "iron_sword", "sd_domar": "dragon_egg", "sd_domar_3": "dragon_egg",
 "lm_cazador": "iron_sword", "lm_skeletosaurus": "bone", "lm_possessed_paladin": "iron_chestplate", "lm_overgrown_colossus": "mossy_cobblestone", "lm_dune_sentinel": "sandstone", "lm_frostbitten_golem": "packed_ice", "lm_withered_abomination": "wither_rose",
 "lm_lava_eater": "magma_cream", "lm_cumulonimbus": "lightning_rod", "lm_chorusent": "chorus_fruit", "lm_shulker_mimic": "shulker_shell", "lm_ancient_guardian": "prismarine_crystals", "lm_obliterator": "end_crystal", "lm_cazaleyendas": "nether_star",
 "ror_domar": "saddle", "ror_cazar": "iron_sword", "ror_domar_3": "lead", "ror_gran_cazador": "diamond_sword",
}
NOUN = {  # entidad -> (singular, plural)
 "minecraft:zombie": ("zombi", "zombis"), "minecraft:skeleton": ("esqueleto", "esqueletos"), "minecraft:spider": ("arana", "aranas"), "minecraft:creeper": ("creeper", "creepers"), "minecraft:enderman": ("enderman", "endermen"),
 "minecraft:witch": ("bruja", "brujas"), "minecraft:pillager": ("saqueador", "saqueadores"), "minecraft:wolf": ("lobo", "lobos"), "*": ("animal", "animales de cualquier especie"),
 M + "stegosaurus": ("Stegosaurus", "Stegosaurus"), M + "carnotaurus": ("Carnotaurus salvaje", "Carnotaurus"), M + "dilophosaurus": ("Dilophosaurus", "Dilophosaurus"), "*dragon*": ("dragon", "dragones"),
 "minecraft:elder_guardian": ("Guardian Anciano", "Guardianes Ancianos"), "minecraft:warden": ("Warden", "Wardens"), "minecraft:wither": ("Wither", "Withers"), "minecraft:ender_dragon": ("Dragon del Ender", "Dragones del Ender"),
 "iceandfire:hippogryph": ("hipogrifo", "hipogrifos"), "iceandfire:cockatrice": ("cocatriz", "cocatrices"), "iceandfire:siren": ("sirena", "sirenas"), "iceandfire:troll": ("troll", "trolls"), "iceandfire:gorgon": ("gorgona", "gorgonas"),
 "iceandfire:cyclops": ("ciclope", "ciclopes"), "iceandfire:hydra": ("hidra", "hidras"), "iceandfire:sea_serpent": ("serpiente marina", "serpientes marinas"), "iceandfire:dread_lich": ("Dread Lich", "Dread Lich"),
 IAF_DRAGONS: ("dragon de Hielo y Fuego", "dragones de Hielo y Fuego"), SDE: ("criatura de Saint's Dragons", "criaturas de Saint's Dragons"),
 "legendary_monsters:*": ("criatura de Legendary Monsters", "criaturas de Legendary Monsters"), LM_BOSSES: ("jefe de Legendary Monsters", "jefes de Legendary Monsters"), "ror:*": ("criatura de Raxiors of Raxor", "criaturas de Raxiors of Raxor"),
}
LMN = {"skeletosaurus": "Skeletosaurus", "possessed_paladin": "Paladin Poseido", "overgrown_colossus": "Coloso Cubierto", "dune_sentinel": "Centinela de las Dunas", "frostbitten_golem": "Golem Escarchado", "withered_abomination": "Abominacion Marchita",
       "lava_eater": "Devoralava", "cumulonimbus": "Cumulonimbus", "chorusent": "Chorusent", "shulker_mimic": "Mimico Shulker", "ancient_guardian": "Guardian Ancestral", "obliterator": "Obliterador"}
BN = {"house": ("casa completa", "casas completas"), "blacksmith": ("herreria completa", "herrerias completas"), "inn": ("posada completa", "posadas completas"), "library": ("biblioteca completa", "bibliotecas completas")}
SPN = {"martial": "marcial", "scholar": "erudito", "industrious": "industrial", "pastoral": "pastoral", "nautical": "nautico"}
def objective(id, d):
    t, c, e = d["type"], d["count"], d.get("entity", "*")
    if t == "home": return "Preparar un hogar: cama, atril, luz y techo"
    if t == "raid": return "Vencer %d asedio%s" % (c, "s" if c > 1 else "")
    if t == "village":
        m = d.get("metric", "population")
        if m == "population": return "Tener %d habitantes en la aldea" % c
        if m == "buildings": return "Tener %d edificios completos" % c
        if m == "building_types": return "Reunir %d tipos de edificio distintos" % c
        if m == "building": sg, pl = BN.get(d.get("building"), ("edificio", "edificios")); return "Tener %d %s" % (c, pl if c > 1 else sg)
        if m == "level": return "Alcanzar el nivel %d de espiritu de aldea" % c
        if m == "spirit": return "Alcanzar el nivel %d de espiritu %s" % (c, SPN.get(d.get("building"), d.get("building")))
    verb = "Domar" if t == "tame" else "Derrotar"
    if id.startswith("lm_") and id not in ("lm_cazador", "lm_cazaleyendas"): return "Derrotar a " + LMN[id[3:]]
    sg, pl = NOUN.get(e, ("objetivo", "objetivos"))
    return "%s %s" % (verb, ("%s" % sg) if c == 1 else "%d %s" % (c, pl))

import re
ACC = [(r"\baranas\b", "arañas"), (r"\bAranas\b", "Arañas"), (r"\bGuardian\b", "Guardián"), (r"\bDragon\b", "Dragón"), (r"\bdragon\b", "dragón"), (r"\bMision\b", "Misión"), (r"\bmision\b", "misión"),
       (r"\bespiritu\b", "espíritu"), (r"\bpoblacion\b", "población"), (r"\bherreria", "herrería"), (r"\bherrerias", "herrerías"), (r"\bPaladin\b", "Paladín"), (r"\bAbominacion\b", "Abominación"),
       (r"\bMimico\b", "Mímico"), (r"\bciclope", "cíclope"), (r"\bnautico\b", "náutico"), (r"\bGolem\b", "Gólem"), (r"\bAncestral\b", "Ancestral"), (r"\bHeroe\b", "Héroe"), (r"\bbiblioteca", "biblioteca"),
       (r"\bEscuadron\b", "Escuadrón"), (r"\bcompania\b", "compañía"), (r"\bCompania\b", "Compañía"), (r"\bCaza\b", "Caza"), (r"\bhipogrifo", "hipogrifo"), (r"\bGorgona\b", "Gorgona"),
       (r"\bdefensa\b", "defensa"), (r"\bAbandonar\b", "Abandonar"), (r"\bpiedra eterna", "piedra eterna"), (r"\bTormenta\b", "Tormenta"), (r"\bun yunque", "un yunque"), (r"\bsuenos\b", "sueños"),
       (r"\bSilencio\b", "Silencio"), (r"\bocéano\b", "océano"), (r"\boceano\b", "océano"), (r"\bmareas\b", "mareas"), (r"\bMirada de gallo", "Mirada de gallo"), (r"\bAmo\b", "Amo"), (r"\bPuente de trolls", "Puente de trolls"),
       (r"\bpeludo\b", "peludo"), (r"\bAliento\b", "Aliento"), (r"\bdomadas\b", "domadas"), (r"\bcuerno\b", "cuerno"), (r"\bCuernos\b", "Cuernos"), (r"\bplacas\b", "placas"), (r"\bveneno\b", "veneno"),
       (r"\bAsedio\b", "Asedio"), (r"\bun ojo\b", "un ojo"), (r"\bsaben\b", "saben"), (r"\bnunca\b", "nunca"), (r"\bAlas de leon\b", "Alas de león"), (r"\bleon\b", "león"), (r"\bcuentan\b", "cuentan"),
       (r"\bestan\b", "están"), (r"\bsolo\b", "solo"), (r"\bAqui\b", "Aquí"), (r"\bmas\b", "más"), (r"\btu\b", "tu"), (r"\bmitico\b", "mítico"), (r"\bcria\b", "cría"), (r"\bcrea\b", "crea"),
       (r"\bAcercate\b", "Acércate"), (r"\bAcercate a ella y espera: la poblacion", "Acércate a ella y espera: la población"), (r"\bcamas\b", "camas"), (r"\bAldeas\b", "Aldeas"), (r"\bPrimer vuelo", "Primer vuelo"),
       (r"\bEscuadron de dragones", "Escuadrón de dragones"), (r"\bdominio\b", "dominio"), (r"\bTodo explorador", "Todo explorador"), (r"\bnecesita compania", "necesita compañía"), (r"\bleyenda\b", "leyenda"), (r"\bAsi\b", "Así"),
       (r"\bpuntos\b", "puntos"), (r"\bnivel\b", "nivel"), (r"\bmaximo\b", "máximo"), (r"\bhacha\b", "hacha"), (r"\bGuarnicion\b", "Guarnición"), (r"\bForja\b", "Forja"), (r"\bPuerto\b", "Puerto"), (r"\bEscuela\b", "Escuela"),
       (r"\bCabezas cortadas", "Cabezas cortadas"), (r"\bterror\b", "terror"), (r"\bPesadilla\b", "Pesadilla"), (r"\bprofundidades\b", "profundidades"), (r"\bduenio\b", "dueño"), (r"\bsalvaje\b", "salvaje"),
       (r"\bdebe ser el mas fuerte", "debe ser el más fuerte"), (r"\bGigante de placas", "Gigante de placas"), (r"\bEnsalada especial ayuda", "Ensalada especial ayuda"), (r"\bRey de los cuernos", "Rey de los cuernos"),
       (r"\bpara el Dragón del Ender", "para el Dragón del Ender"), (r"\bfin del Fin", "fin del Fin")]
def accent(s):
    for a, b in ACC: s = re.sub(a, b, s)
    return s
for id, d in Q:
    for k in ("title", "description"): d[k] = accent(d[k])
for id, d in Q:
    if id in ICONS: d["icon"] = ICONS[id] if ":" in ICONS[id] else "minecraft:" + ICONS[id]
    d["objective"] = accent(objective(id, d))

for i, (id, d) in enumerate(Q):
    json.dump(d, open(os.path.join(out, id + '.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=2); open(os.path.join(out, id + '.json'), 'a').write('\n')
print(len(Q), 'misiones')
