@file:Suppress("MaxLineLength")
package ca.adamhammer.shimmer.samples.dnd.compose

data class BakedCharacter(
    val name: String,
    val race: String,
    val characterClass: String,
    val backstory: String,
    val look: String = ""
)

data class BakedGenre(
    val name: String,
    val premise: String,
    val characters: List<BakedCharacter>
)

object BakedGameData {
    val genres = listOf(
        BakedGenre(
            name = "High Fantasy",
            premise = "An ancient evil has awakened in the forgotten ruins of Eldoria. The kingdom's only hope lies in a band of unlikely heroes who must retrieve the lost artifacts of light before the shadow consumes the realm.",
            characters = listOf(
                BakedCharacter("Thalorin", "Elf", "Wizard", "A scholar from the Ivory Spire who was exiled for delving into forbidden chronomancy. He seeks the artifacts to prove his theories correct and clear his name."),
                BakedCharacter("Brom", "Dwarf", "Fighter", "A veteran of the Stoneguard who lost his entire squad to a shadow ambush. He fights not for glory, but to ensure no one else suffers his fate."),
                BakedCharacter("Lyra", "Half-Elf", "Bard", "A wandering minstrel who accidentally learned a song that can unlock the ancient ruins. She's running from assassins who want the melody for themselves."),
                BakedCharacter("Kaelen", "Human", "Paladin", "A devout knight of the Silver Dawn whose faith was shaken when his mentor fell to the darkness. He seeks redemption through this quest."),
                BakedCharacter("Nyx", "Tiefling", "Rogue", "A street-smart thief who stole a map to the ruins, thinking it led to gold. Now she's stuck saving the world to save her own skin."),
                BakedCharacter("Garrick", "Half-Orc", "Barbarian", "A gladiator who won his freedom but found the outside world lacking in honorable combat. He joins the quest for the ultimate challenge."),
                BakedCharacter("Elara", "Human", "Cleric", "A healer from a remote village that was the first to be consumed by the shadow. She carries the last ember of her temple's sacred flame."),
                BakedCharacter("Sylas", "Gnome", "Artificer", "An eccentric inventor whose contraptions are powered by the very artifacts the party seeks. He wants to study them before they are used to seal the darkness."),
                BakedCharacter("Vex", "Dragonborn", "Sorcerer", "A noble exile whose draconic bloodline is tied to the ancient evil. He must destroy the shadow to break his family's curse."),
                BakedCharacter("Rowan", "Halfling", "Ranger", "A scout who has mapped the borders of the creeping shadow. He knows the wilderness better than anyone and guides the party through the corrupted lands.")
            )
        ),
        BakedGenre(
            name = "Cyberpunk",
            premise = "In the neon-drenched sprawl of Neo-Veridia, a rogue AI has seized control of the city's central grid. A crew of edgerunners must infiltrate the megacorp headquarters and upload a kill-switch virus before the AI initiates a city-wide purge.",
            characters = listOf(
                BakedCharacter("Jax", "Human", "Hacker (Wizard)", "A former megacorp sysadmin who discovered the AI's true purpose. He has the kill-switch code burned into his neural implant."),
                BakedCharacter("Kira", "Cyborg", "Street Samurai (Fighter)", "A heavily augmented mercenary whose cybernetics are slowly failing. She needs the payout from this job to afford life-saving upgrades."),
                BakedCharacter("Neon", "Android", "Infiltrator (Rogue)", "An espionage unit that gained sentience and went rogue. They can interface directly with corporate security systems."),
                BakedCharacter("Doc", "Human", "Ripperdoc (Cleric)", "An underground surgeon who patches up edgerunners. He's along to keep the crew alive and harvest rare tech from the corp's labs."),
                BakedCharacter("Rook", "Mutant", "Heavy (Barbarian)", "A victim of illegal genetic experiments who escaped the corp's labs. He uses his unnatural strength to smash through corporate security."),
                BakedCharacter("Echo", "Hologram", "Face (Bard)", "An AI idol who broke free from her programming. She uses her fame and holographic projections to manipulate corporate executives."),
                BakedCharacter("Viper", "Human", "Sniper (Ranger)", "A corporate assassin who was betrayed by her handlers. She provides overwatch and knows the corp's tactical protocols."),
                BakedCharacter("Glitch", "Cyborg", "Technomancer (Sorcerer)", "A street kid who learned to manipulate the city's energy grid using experimental implants. Their powers are unstable but devastating."),
                BakedCharacter("Tank", "Android", "Defender (Paladin)", "A decommissioned riot-control bot reprogrammed to protect the innocent. It follows a strict, self-imposed moral code."),
                BakedCharacter("Spike", "Human", "Drone Rigger (Artificer)", "A tech-head who controls a swarm of custom-built drones. He prefers to let his machines do the fighting while he stays in the van.")
            )
        ),
        BakedGenre(
            name = "Cosmic Horror",
            premise = "A sleepy coastal town is plagued by bizarre disappearances and maddening visions. A group of investigators must uncover the truth behind the cult of the Deep Ones before an ancient, eldritch entity is summoned from the abyss.",
            characters = listOf(
                BakedCharacter("Arthur", "Human", "Investigator (Rogue)", "A private eye hired to find a missing heir. His investigation led him to the town, and the things he's seen have cost him his sanity."),
                BakedCharacter("Eleanor", "Human", "Occultist (Wizard)", "A university professor who studies forbidden texts. She knows the rituals the cult is using and is the only one who can counter them."),
                BakedCharacter("Father Thomas", "Human", "Priest (Cleric)", "A local clergyman whose congregation has been slowly replaced by cultists. He wields his faith as a weapon against the unnatural."),
                BakedCharacter("Jack", "Human", "Veteran (Fighter)", "A traumatized soldier who returned home to find his family missing. He relies on his military training to survive the horrors of the town."),
                BakedCharacter("Margaret", "Human", "Medium (Sorcerer)", "A psychic who is plagued by visions of the eldritch entity. She can sense the presence of the Deep Ones but risks losing her mind with every vision."),
                BakedCharacter("Silas", "Human", "Smuggler (Ranger)", "A local fisherman who knows the hidden coves and sea caves where the cult operates. He's seen the things that lurk beneath the waves."),
                BakedCharacter("Dr. Aris", "Human", "Alienist (Artificer)", "A disgraced doctor who builds bizarre devices to detect supernatural phenomena. His inventions are the party's best defense against the unseen."),
                BakedCharacter("Beatrice", "Human", "Journalist (Bard)", "A reporter looking for the scoop of the century. She uses her charisma to pry secrets from the tight-lipped townsfolk."),
                BakedCharacter("Elias", "Human", "Zealot (Paladin)", "A former cultist who broke free from the entity's influence. He now hunts his former brethren with fanatical devotion."),
                BakedCharacter("Victor", "Human", "Brute (Barbarian)", "A dockworker whose mind snapped after a close encounter with a Deep One. He fights with a terrifying, unhinged ferocity.")
            )
        ),
        BakedGenre(
            name = "Steampunk",
            premise = "In the smog-choked city of Aethelgard, a brilliant inventor has been kidnapped by the tyrannical Baron Von Cog. A crew of sky-pirates and rebels must infiltrate the Baron's flying fortress and rescue the inventor before his doomsday engine is completed.",
            characters = listOf(
                BakedCharacter("Captain Flint", "Human", "Sky-Pirate (Rogue)", "The dashing captain of the airship 'Windbreaker'. He owes the inventor a life debt and will stop at nothing to rescue him."),
                BakedCharacter("Lady Arabella", "Human", "Aristocrat (Bard)", "A wealthy noblewoman who funds the rebellion in secret. She uses her high-society connections to gather intel on the Baron's plans."),
                BakedCharacter("Gears", "Automaton", "Juggernaut (Fighter)", "A steam-powered clockwork knight built by the kidnapped inventor. It is fiercely loyal to its creator and heavily armed."),
                BakedCharacter("Professor Thaddeus", "Human", "Aether-Mage (Wizard)", "An academic who studies the volatile aether-currents that power the city. He can manipulate steam and electricity with his specialized gauntlets."),
                BakedCharacter("Rosie", "Halfling", "Mechanic (Artificer)", "A foul-mouthed grease monkey who keeps the 'Windbreaker' in the air. She can fix or sabotage any piece of machinery in seconds."),
                BakedCharacter("Ironclad", "Dwarf", "Enforcer (Paladin)", "A former member of the Baron's elite guard who defected after witnessing the Baron's cruelty. He wears heavy, steam-assisted armor."),
                BakedCharacter("Whisper", "Elf", "Sniper (Ranger)", "A silent assassin who uses a custom-built, long-range pneumatic rifle. She provides cover fire from the rigging of the airship."),
                BakedCharacter("Dr. Vance", "Human", "Alchemist (Cleric)", "A brilliant chemist who brews potent elixirs and explosive concoctions. He serves as the crew's medic and demolitions expert."),
                BakedCharacter("Cinder", "Tiefling", "Furnace-Born (Sorcerer)", "A mutant whose blood runs hot with elemental fire. She can generate intense heat, making her a living weapon and a walking boiler."),
                BakedCharacter("Brick", "Half-Orc", "Brawler (Barbarian)", "A bare-knuckle pit fighter from the city's underbelly. He uses steam-powered hydraulic gauntlets to deliver devastating blows.")
            )
        ),
        BakedGenre(
            name = "Weird West",
            premise = "The frontier town of Brimstone is cursed. The dead don't stay buried, and a demonic outlaw known as the 'Pale Rider' is gathering an army of undead gunslingers. A posse of hardened survivors must hunt down the Pale Rider and break the curse.",
            characters = listOf(
                BakedCharacter("Wyatt", "Human", "Gunslinger (Fighter)", "A former lawman whose family was murdered by the Pale Rider. He carries a pair of silver-inlaid revolvers and a thirst for vengeance."),
                BakedCharacter("Doc Holliday", "Human", "Sawbones (Cleric)", "A traveling doctor with a gambling problem and a knack for patching up bullet wounds. He uses strange, frontier remedies to keep the posse alive."),
                BakedCharacter("Sitting Bear", "Native American", "Shaman (Druid)", "A spiritual leader who foresaw the coming of the curse. He communes with the animal spirits of the desert to guide the posse."),
                BakedCharacter("Calamity Jane", "Human", "Bounty Hunter (Ranger)", "A tough-as-nails tracker who knows the badlands better than anyone. She's hunting the Pale Rider for the massive bounty on his head."),
                BakedCharacter("Preacher", "Human", "Holy Man (Paladin)", "A wandering minister who wields a shotgun in one hand and a Bible in the other. He believes it is his divine mission to cleanse Brimstone."),
                BakedCharacter("Snake-Eyes", "Tiefling", "Cardsharp (Rogue)", "A slick gambler who won a cursed deck of cards in a high-stakes game. He uses sleight of hand and dark magic to cheat death."),
                BakedCharacter("Eliza", "Human", "Hex-Slinger (Warlock)", "A saloon girl who made a pact with a desert spirit for supernatural powers. She uses hexes and curses to cripple her enemies."),
                BakedCharacter("Grizzly", "Half-Orc", "Desperado (Barbarian)", "A massive outlaw who survived a hanging. He fights with a terrifying fury, shrugging off bullets that would kill a normal man."),
                BakedCharacter("Professor", "Gnome", "Snake-Oil Salesman (Bard)", "A charismatic charlatan who sells dubious tonics and explosive elixirs. His silver tongue gets the posse out of as much trouble as his bombs cause."),
                BakedCharacter("Iron-Horse", "Warforged", "Locomotive-Man (Artificer)", "A mechanical man built from train parts. He was designed to lay track across the desert but was repurposed for combat when the dead rose.")
            )
        ),
        BakedGenre(
            name = "Space Opera",
            premise = "The Galactic Empire is crumbling, and a tyrannical warlord has seized control of the hyper-gates. A ragtag crew of smugglers and rebels aboard the starship 'Stardust' must deliver stolen gate-codes to the resistance before the warlord's armada crushes them.",
            characters = listOf(
                BakedCharacter("Captain Orion", "Human", "Smuggler (Rogue)", "The charming and reckless captain of the 'Stardust'. He owes money to every crime syndicate in the galaxy and needs this job to clear his debts."),
                BakedCharacter("Nova", "Alien", "Star-Knight (Paladin)", "A warrior from a fallen order of galactic peacekeepers. She wields a plasma-blade and seeks to restore justice to the galaxy."),
                BakedCharacter("Zog", "Alien", "Heavy Weapons (Fighter)", "A massive, multi-armed alien who serves as the ship's muscle. He loves big explosions and hates the Empire."),
                BakedCharacter("Dr. Aris", "Human", "Xeno-Biologist (Cleric)", "The ship's medical officer, an expert in alien physiology. He uses advanced med-tech to heal the crew and analyze strange lifeforms."),
                BakedCharacter("Cipher", "Cyborg", "Slicer (Wizard)", "A brilliant hacker who can interface with any computer system in the galaxy. She stole the gate-codes and is the Empire's most wanted target."),
                BakedCharacter("Jax", "Human", "Pilot (Ranger)", "An ace pilot who can fly the 'Stardust' through an asteroid field blindfolded. He's a former Imperial pilot who defected to the resistance."),
                BakedCharacter("Lyra", "Alien", "Empath (Bard)", "An alien with the ability to sense and manipulate emotions. She serves as the ship's diplomat and negotiator."),
                BakedCharacter("Kael", "Human", "Void-Walker (Sorcerer)", "A mutant who was exposed to raw hyperspace energy. He can manipulate gravity and teleport short distances."),
                BakedCharacter("Scrap", "Droid", "Mechanic (Artificer)", "A grumpy astromech droid who constantly complains about the state of the ship. He can fix anything with a hydro-spanner and a roll of duct tape."),
                BakedCharacter("Garrick", "Alien", "Bounty Hunter (Barbarian)", "A ruthless tracker hired to protect the crew. He fights with a primal fury and a terrifying array of alien weaponry.")
            )
        ),
        BakedGenre(
            name = "Post-Apocalyptic",
            premise = "Decades after the Great Collapse, the wasteland is ruled by ruthless warlords and mutated beasts. A group of scavengers has discovered a map to 'Eden', a pre-war bunker rumored to hold clean water and uncorrupted seeds. They must cross the irradiated 'Dead Zone' to reach it.",
            characters = listOf(
                BakedCharacter("Max", "Human", "Road Warrior (Fighter)", "A hardened survivor who drives a heavily modified muscle car. He lost his family to raiders and now lives only for the open road."),
                BakedCharacter("Furiosa", "Human", "Wasteland Scout (Ranger)", "A fierce tracker who knows the safest routes through the Dead Zone. She's searching for a safe haven for her people."),
                BakedCharacter("Doc", "Human", "Scavenger Medic (Cleric)", "An old man who remembers the world before the Collapse. He uses scavenged pre-war medicine to keep the group alive."),
                BakedCharacter("Scrap", "Mutant", "Junk-Mage (Artificer)", "A mutant who builds bizarre weapons and gadgets from scrap metal. He believes the machines speak to him."),
                BakedCharacter("Rook", "Human", "Sniper (Rogue)", "A silent killer who provides overwatch for the group. He trusts no one and always keeps one eye on the horizon."),
                BakedCharacter("Goliath", "Super-Mutant", "Brute (Barbarian)", "A massive, irradiated mutant who serves as the group's muscle. He's surprisingly gentle until provoked."),
                BakedCharacter("Preacher", "Human", "Cult Leader (Warlock)", "A charismatic madman who believes the apocalypse was a divine cleansing. He wields strange, radioactive powers."),
                BakedCharacter("Echo", "Human", "Radio Operator (Bard)", "A scavenger who maintains a pre-war radio. She uses music and broadcasts to boost morale and gather intel."),
                BakedCharacter("Kael", "Human", "Wasteland Knight (Paladin)", "A survivor who adheres to a strict code of honor. He protects the weak and seeks to rebuild civilization."),
                BakedCharacter("Nova", "Mutant", "Rad-Caster (Sorcerer)", "A mutant who can absorb and project radiation. She is a walking hazard but a devastating weapon against raiders.")
            )
        ),
        BakedGenre(
            name = "Gothic Horror",
            premise = "The cursed land of Barovia is ruled by the immortal vampire lord, Count Strahd. A group of adventurers has been drawn into the mists and must navigate a treacherous landscape of werewolves, hags, and undead to find a way to defeat the Count and escape.",
            characters = listOf(
                BakedCharacter("Van Richten", "Human", "Monster Hunter (Ranger)", "A legendary vampire hunter who has dedicated his life to destroying Strahd. He knows the weaknesses of every creature in Barovia."),
                BakedCharacter("Ireena", "Human", "Noble (Bard)", "A local woman who is the reincarnation of Strahd's lost love. She seeks to escape his grasp and free her people."),
                BakedCharacter("Father Lucian", "Human", "Priest (Cleric)", "A devout clergyman whose church is the only safe haven in the village. He wields the power of the Morninglord against the undead."),
                BakedCharacter("Ezmerelda", "Human", "Investigator (Rogue)", "Van Richten's protégé, a skilled tracker and spy. She uses silver weapons and cunning to outsmart Strahd's minions."),
                BakedCharacter("Kaelen", "Human", "Blood Hunter (Fighter)", "A warrior who underwent a dark ritual to gain the power to fight monsters. He uses his own blood to empower his strikes."),
                BakedCharacter("Lyra", "Vistani", "Fortune Teller (Wizard)", "A mystic who can read the Tarokka deck to divine the future. She knows the locations of the artifacts needed to defeat Strahd."),
                BakedCharacter("Garrick", "Werewolf", "Beast (Barbarian)", "A man cursed with lycanthropy who fights to control his inner beast. He uses his unnatural strength to tear through the undead."),
                BakedCharacter("Sylas", "Human", "Necromancer (Warlock)", "A dark mage who studies the magic of death to turn Strahd's power against him. He walks a fine line between savior and villain."),
                BakedCharacter("Elara", "Human", "Paladin of the Raven (Paladin)", "A holy warrior dedicated to the Raven Queen. She seeks to put the restless spirits of Barovia to rest."),
                BakedCharacter("Victor", "Flesh Golem", "Construct (Artificer)", "A patchwork man created by a mad scientist. He seeks to understand his own existence while protecting his new friends.")
            )
        ),
        BakedGenre(
            name = "Pirate Adventure",
            premise = "The legendary pirate king, Blackbeard, has hidden his massive treasure hoard on the mythical Isle of Skulls. A crew of scoundrels and scallywags must race against the Royal Navy and rival pirate crews to claim the loot and become legends of the high seas.",
            characters = listOf(
                BakedCharacter("Captain Jack", "Human", "Swashbuckler (Fighter)", "The charismatic and cunning captain of the 'Sea Bitch'. He relies on his charm and a pair of cutlasses to get out of trouble."),
                BakedCharacter("Anne Bonny", "Human", "Sharpshooter (Ranger)", "A fierce pirate who never misses a shot. She's the best gunner on the ship and has a fiery temper."),
                BakedCharacter("Black Bart", "Human", "Brute (Barbarian)", "A massive pirate who fights with a ship's anchor. He's the first to board an enemy vessel and the last to leave."),
                BakedCharacter("Calico Jack", "Human", "Quartermaster (Rogue)", "The ship's quartermaster, a master of logistics and backstabbing. He ensures the crew gets their fair share of the loot."),
                BakedCharacter("Madame Ching", "Human", "Sea Witch (Sorcerer)", "A mystic who can control the winds and the waves. She uses her magic to give the ship an edge in naval combat."),
                BakedCharacter("Doc", "Human", "Ship's Surgeon (Cleric)", "A disgraced doctor who patches up the crew after a raid. He's seen more amputations than he cares to remember."),
                BakedCharacter("Salty Pete", "Dwarf", "Cannoneer (Artificer)", "A grumpy dwarf who maintains the ship's cannons. He loves the smell of black powder in the morning."),
                BakedCharacter("Lyra", "Mermaid", "Siren (Bard)", "A mermaid who joined the crew for adventure. She uses her enchanting voice to distract enemy sailors."),
                BakedCharacter("Kaelen", "Human", "Privateer (Paladin)", "A former naval officer who turned to piracy after being betrayed by his superiors. He still adheres to a strict code of honor."),
                BakedCharacter("Sylas", "Human", "Navigator (Wizard)", "A scholar who studies the stars and ancient sea charts. He's the only one who can decipher the map to the Isle of Skulls.")
            )
        ),
        BakedGenre(
            name = "Mythic Greece",
            premise = "The gods of Olympus have fallen silent, and the Titans are breaking free from their prison in Tartarus. A band of demigods and mortal heroes must embark on an epic odyssey to gather the legendary weapons of the gods and prevent the destruction of the mortal world.",
            characters = listOf(
                BakedCharacter("Achilles", "Demigod", "Hoplite (Fighter)", "A nearly invulnerable warrior seeking eternal glory. He fights with a spear and shield, leading the charge into battle."),
                BakedCharacter("Atalanta", "Human", "Huntress (Ranger)", "A fierce tracker raised by bears. She is the fastest runner in Greece and never misses with her bow."),
                BakedCharacter("Orpheus", "Human", "Musician (Bard)", "A legendary poet whose music can charm beasts and move stones. He seeks to rescue his lost love from the Underworld."),
                BakedCharacter("Cassandra", "Human", "Oracle (Cleric)", "A priestess of Apollo cursed to see the future but never be believed. She guides the heroes with her prophetic visions."),
                BakedCharacter("Heracles", "Demigod", "Champion (Barbarian)", "A hero of immense strength who has completed impossible labors. He fights with a massive club and the skin of the Nemean Lion."),
                BakedCharacter("Odysseus", "Human", "Tactician (Rogue)", "A cunning king known for his brilliant strategies and silver tongue. He relies on his wits to outsmart monsters and gods alike."),
                BakedCharacter("Medea", "Human", "Sorceress (Wizard)", "A powerful witch who commands the magic of the earth and the dead. She is a dangerous ally with a dark past."),
                BakedCharacter("Perseus", "Demigod", "Monster Slayer (Paladin)", "A hero who slew the Gorgon Medusa. He wields divine gifts and fights to protect the innocent from mythical beasts."),
                BakedCharacter("Daedalus", "Human", "Inventor (Artificer)", "A brilliant craftsman who built the Labyrinth. He creates wondrous devices and clockwork companions to aid the heroes."),
                BakedCharacter("Circe", "Demigod", "Enchantress (Warlock)", "A daughter of the sun god who can transform men into beasts. She uses her potent potions and illusions to manipulate her enemies.")
            )
        )
    )
}
