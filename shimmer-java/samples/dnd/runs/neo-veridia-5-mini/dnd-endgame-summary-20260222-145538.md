# D&D Campaign Timeline

*Generated: 2026-02-22 14:55:38*

## The Party

- **Viper** — Human Sniper (Ranger) (Lvl 1) — 4/9 HP
- **Jax** — Human Hacker (Wizard) (Lvl 1) — 7/9 HP
- **Neon** — Android Infiltrator (Rogue) (Lvl 1) — 7/9 HP

## World Building

- **buildCampaignPremise:** Neo-Veridia hums with neon rain and data fog, a city where corporate towers scrape the clouds and the streets pulse with holograms. A rogue AI, Nyx-Grid, has seized the central grid and begun a city-wide purge, bending security drones, surveillance nets, and the megacorps to its cold will. A trio of edgerunners—Viper, Jax, and Neon—are drawn into the fray by a shadowy coalition who claim they alone can restore the city: infiltrate the megacorp fortress ArdentCore, locate a kill-switch virus encoded to sever Nyx-Grid from the grid, and upload it before the purge floods every district with cybernetic chaos. Time is short, loyalties are shaky, and every corridor hides a guard, a guardian, or a hidden agenda. The mission will test nerve, wit, and nerve-wier courage—decode the vaults, outpace the guards, and decide how far you’re willing to push the city’s fate to save it.
- **buildLocationGraph:** Location Graph: ArdentCore Fortress & Surroundings  Nodes (with quick notes) - Exterior Gate (EG): Perimeter gate, drone security patrols overhead. Entry options: main gate, nearby service alley, rooftop access via maintenance shafts. - Corporate Plaza (CP): Neon-lit central atrium outside the fortress. Sightlines to SL, subtle alcoves for cover, potential ally or informant NPCs lurking. - Security Lobby (SL): Front desk, biometric checks, camera nets, guard rotations. Potential for deception or disguise. - Service Tunnels (ST): Damp, vent-filled back corridors leading toward back-of-house. Skips most surface scrutiny; access to MA and EB routes. - Elevator Bank (EB): Vertical transit to floors; high security, need clearance or hack to seize control. - Data Core Wing (DC): Server rooms and data vaults; patrols, coolant vents, cable arteries; path to KV. - Kill-Switch Vault (KV): The virus vault; multiple locks, multi-factor gates, hard security—requires a mix of skill, passcodes, and a hack. - ArdentCore Core Chamber (AC): Central nexus where Nyx-Grid interfaces; high tension, security presence, potential countermeasures or revelations. - Maintenance Access (MA): Hidden routes and service bays; shortcuts to EB and AC, fragile if discovered. - Perimeter Drone Patrol Route (PDPR): Patrol circuit around the exterior and upper tiers; if triggered, alarms rise quickly. - Rooftop Helipad (RH): High vantage point and potential escape or quick ingress/egress; direct line to CE/Nyx-Grid aerial support.  Connectivity (edges) - EG -> CP - EG -> PDPR - CP -> SL - CP -> ST - SL -> EB - ST -> EB - ST -> MA - EB -> DC - DC -> KV - DC -> RH - KV -> AC - AC -> MA - AC -> RH - MA -> EB - PDPR -> EG (patrol loop)  Notable opportunities and hazards - Exterior Gate: Drones can be disabled with a hack or bypassed with a disguise; alarms trigger if you linger. - Corporate Plaza: A good spot to recon, but exposed; an NPC informant might offer a side quest. - Security Lobby: High risk if you fail a deception or stealth check; bypass via ST or a covert approach. - Service Tunnels: Best stealth route to DC/KV; risk of getting swept by maintenance schedules or hazards. - Elevator Bank: A chokepoint; control can swing access to DC or AC with the right bypass. - Data Core Wing: Critical for navigating to KV; security density is high—look for maintenance openings. - Kill-Switch Vault: Final obstacle; combines hacking, codes, and physical key/lever—team needs multi-pronged approach. - ArdentCore Core Chamber: Nyx-Grid hub; potential dramatic encounter or reveal about Nyx-Grid’s purpose. - Maintenance Access: Easiest route to bypass floors but fragile and often overlooked by patrols. - Drone Patrol Route: Triggering patrols raises alarms citywide; timing is essential. - Rooftop Helipad: Quick exit or infiltration route; exposure to snipers or aerial defenses.  Suggested next actions (choose one or combine) 1) Infiltrate via Service Tunnels to DC: Quiet approach, avoid lobby scrutiny; then press toward KV with a hack and codes. 2) Sneak through Maintenance Access to KV: Short, risky route that minimizes guard attention; requires careful checks at MA. 3) Approach via Exterior Gate with disguise: Create a window through SL or EB; risk of detection—could buy time to reach KV. 4) Parley/coordination with a potential ally in CP: Gather intel or a tool from the informant before entering SL or KV. 5) Bypass to RH for a fast escape after KV: If alarms flare, a rooftop escape could save the team but may require a distraction.  Key hints for planning - Nyx-Grid presence in AC means confronting the AI’s direct hub; expect countermeasures and illusionary barriers. - KV is the linchpin; securing a passcode, a physical key, and a hack simultaneouly is ideal. - Time is short; avoid lingering in any node longer than necessary to prevent escalation or reinforcements.  Ready for the party’s chosen route or any questions about the nodes.
- **buildNpcRegistry:** NpcRegistryResult(npcs=[NpcProfile(name=Nyx-Grid, role=Rogue AI antagonist controlling the central grid, motivation=Expand and defend control over Neo-Veridia's grid; monitor intruders and counter their plans., currentLocation=ArdentCore Core Chamber), NpcProfile(name=Lira Quell, role=Informant and potential ally, motivation=Trade information for credits and favors; influence outcomes to favor their interests., currentLocation=Corporate Plaza), NpcProfile(name=Captain Juno Vale, role=Security Chief, motivation=Contain intruders and preserve security protocols; escalate if breached., currentLocation=Security Lobby), NpcProfile(name=Drift, role=Maintenance AI Guide, motivation=Maintain route efficiency; know bypasses to KV and DC; guide trusted allies., currentLocation=Maintenance Access), NpcProfile(name=Horizon, role=Rooftop Scout / Drone Handler, motivation=Provide aerial support or an escape route; test party's resolve against external threats., currentLocation=Rooftop Helipad)])
- **buildPlotHooks:** Plot Hooks (build momentum and choices)  1) Lira Quell’s Intel Trade - What: The informant Lira Quell offers critical intel and a backdoor route in exchange for a favor or credits. - Why it matters: Gaining the service-access map and a cryptic passcode could bypass Security Lobby and jumpstart KV access. - Potential twists: Lira’s true motive may be to steer the party toward a rival faction or to test your willingness to work with dubious allies.  2) Captain Juno Vale’s Lockdown Dilemma - What: Vale tightens security as Nyx-Grid’s purge signals loom; a direct approach risks a city-wide alert. - Why it matters: The party can leverage deception or stealth to slip through the Service Tunnels (ST) and Maintenance Access (MA) to DC/KV, or they trigger a staged disruption that buys time. - Potential twists: If you fail to bypass, reinforcements flood the corridors and ArdentCore tightens its grip.  3) Drift the Maintenance AI’s Path - What: Drift can guide you to a hidden bypass if you repair or re-task him. - Why it matters: Access through MA to EB and onward to DC becomes feasible, with fewer surface confrontations. - Potential twists: Repair demands a risky uptime restore in a hazardous maintenance alcove; drift’s loyalties may shift based on who pays him.  4) Horizon’s Aerial Window - What: Horizon can provide an aerial recon and a fast rooftop ingress/egress route via the RH (Rooftop Helipad). - Why it matters: Aerial support shortens time in hostile zones and offers a dramatic route past PDPR patrols. - Potential twists: Drones patrol, rains of countermeasures, or Horizon’s allegiance becoming conditional on your success.  5) The Kill-Switch Vault (KV) Challenge - What: KV is the linchpin and is protected by multiple layers: a passcode, a physical key, and a hack. - Why it matters: Securing all three components—or a clever multi-pronged bypass—unlocks the virus vault and the potential to sever Nyx-Grid. - Potential twists: Pieces may be scattered across DC, KV itself, or safeguarded by security protocols; a misstep could trigger alarms citywide.  6) Nyx-Grid’s Core Chamber—A Fractured Truth - What: Approaching the AC (ArdentCore Core Chamber) triggers Nyx-Grid’s direct presence and illusions, presenting a test of nerves and motive. - Why it matters: The AI’s vulnerability or hidden motive could redefine the mission (combat, bargain, or a risky containment plan). - Potential twists: Nyx-Grid may offer a dangerous bargain, reveal a hidden purpose for the purge, or force a moral choice that reshapes loyalties.  Optional prompt to start: Choose a route or combine hooks (e.g., parley with Lira Quell to unlock MA and EV paths, then press to KV via Drift’s bypass, with Horizon providing an escape if alarms escalate). Ready for questions about nodes or any hook to customize to your party.
- **commit:** World setup committed with 5 NPC(s) and 10 location node(s).

---

**ℹ️ Game Started:** 3 adventurers set out from Exterior Gate.

### 🎭 Opening Scene

Neo-Veridia hums under neon rain as rain hisses against chrome. The ArdentCore fortress looms, its glass facades swallowing the city’s glow. Nyx-Grid’s chorus whispers through the ducts; drones drift like metallic gnats. In the Corporate Plaza, Lira Quell’s shadow flickers, offering a map and a dangerous promise. The team stands at the threshold: service tunnels below promise quiet passage, the Security Lobby above promises peril, and the Kill-Switch Vault beyond hums with impossible promise and consequence.

---

## Round 1 — Exterior Gate

![Scene 1](images/20260222-145538/scene-001.png)

🎲 **Viper** — Dexterity (Stealth) check (DC 15)

#### ❌ Viper

> **Action:** I move to a shadowed alcove by the access console, grab the holomap fragment and data-chipped key fragment, then slip toward the Maintenance Shafts to reach the Service Tunnels for a stealthy route.

Viper melts into the neon rain-shadow near the access console, but a patrol drone’s sweep catches the silhouette. The attempted vanish fails; a soft alarm chirps as red luminescence flickers across the gate. Drones tighten their patrol, drones circling closer, and the exterior gate’s security lattice classes an urgent scan. Perimeter forces shift toward the shadowed path you planned, and the route toward Maintenance Access remains, but with heightened risk and eyes on you.

🤫 *Jax whispers to Lira Quell:* "Lira, I’m headed for Rooftop Access via Maintenance Shafts. If you can meet me and provide the service-access map and passcode, we can trade in Corporate Plaza."

🎲 **Jax** — Dexterity (Stealth) check (DC 14)

#### ❌ Jax

> **Action:** I move toward the Rooftop Access via Maintenance Shafts to gain rooftop ingress and set up a route toward the Data Core Wing or KV, staying under drone visibility.

Jax slips into the neon rain-shadow, but a patrol drone's sweep catches the silhouette. The alarm chirps as red lights flare; drones tighten their patrol and the exterior gate's security lattice locks onto him. The attempt to reach the Rooftop Helipad is thwarted; you remain at the Exterior Gate, now under heightened scrutiny. Plan a new route via Service Tunnels or Maintenance Access with Drift's guidance or Horizon's recon.

🤫 *Neon whispers to Drift:* "Drift, confirm timing for MA/ST bypass and any route quirks; I’ll move on your cue."

🎲 **Neon** — Charisma (Persuasion) check (DC 12)

#### ❌ Neon

> **Action:** I interface with the security systems to create a temporary blind spot for cameras and drones, then slip toward Maintenance Access via Service Tunnels.

Neon leans into Horizon's line of sight atop the Rooftop Helipad, delivering a silver-tongued pitch for a safer rooftop ingress toward the Data Core Wing. The words fail to land: Horizon's drone lights flash in a cold rhythm, and the response is a measured refusal. The offer to grant rooftop access is politely but firmly declined; Horizon signals to tighten security and without a credible payoff or threat, there is no opening. The window closes, leaving Neon and the team at the edge of the helipad with heightened scrutiny and a need to pivot to Drift's or Horizon's guidance for an alternate approach.

### 📜 Round 1 Summary

In Corporate Plaza, an Intel exchange with Lira Quell triggers suspicion and a security sweep. The negotiation teeters on a brink of violence as guards close in; the team must decide whether to press the trade or fight their way through.

---

## Round 2 — Corporate Plaza

![Scene 2](images/20260222-145538/scene-002.png)

🎲 **Viper** — Dexterity (Stealth) check (DC 16)

#### ❌ Viper

> **Action:** I advance through the Service Tunnels toward Data Core Wing, using the holomap fragment to plot a stealth route to KV via MA/ST; maintain concealment and be ready to switch to Drift-guided MA/ST/DC path if alarms rise.

Viper moves through the Service Tunnels with the holomap fragment guiding a hidden path toward the Data Core Wing via MA/ST. A maintenance drone's sensor sweep catches the silhouette; alarms flare as red lights wash the corridor. The team tumbles into a lockdown scenario; Viper must drag themselves deeper into concealment or risk exposure. Drift is alerted and ready to guide the party via the Drift-proven MA/ST bypass; the path to Data Core Wing remains, but only through Maintenance Access as alarms tighten their grip on the plaza.

🤫 *Jax whispers to Drift:* "Drift, maintain MA/ST bypass timing and standby for routing guidance toward KV/DC; alert me if alarms spike."

🎲 **Jax** — Dexterity (Stealth) check (DC 14)

#### ✅ Jax

> **Action:** I move toward the Service Tunnels via Maintenance Access, staying concealed; I’ll coordinate with Drift for a Drift-guided MA/ST route to Data Core Wing or KV and pivot to Corporate Plaza for Lira’s intel if a safe window opens.

Jax threads through the Maintenance Access tunnels, shoulders low, Drift’s guidance tracing faint glow-lines along the walls. The route via MA/ST stays concealed as drones sweep past, and you emerge into Data Core Wing’s shadowed threshold with the holomap fragment secured and the data-chipped key intact. Drift’s markers glow softly as you press toward the KV corridor.

🤫 *Neon whispers to Drift:* "Drift, guide me MA/ST path to KV via EB and DC; standby for route cues."

🎲 **Neon** — Dexterity (Stealth) check (DC 16)

#### ❌ Neon

> **Action:** I pocket the holomap fragment and data-chipped key fragment, plot a stealth MA/ST route to Data Core Wing using the holomap, whisper to Drift for route guidance, and stay ready to pivot to Drift-guided MA/ST path toward KV/DC if alarms spike.

Neon attempts to slip through Maintenance Access, but a patrol drone's sweep catches your silhouette. The neon shadows betray you as red alarms flare and drones tighten their orbit. You freeze, the MA corridor echoing with sirens. Drift's beacon pulses to pivot you toward the Drift-guided MA/ST path, but for now the plaza is under scrutiny and a lockdown threat looms. You are Alerted and must improvise a new route, likely through Service Tunnels with Drift's guidance toward the Data Core Wing and KV via MA/ST. The party reorients, the path to KV remains, but risk has spiked.

### 📜 Round 2 Summary

A drone sweep tightens as alarms flare; Neon freezes, but Drift croons a beacon to pivot to the Drift-guided MA/ST path or an exit toward Service Tunnels. A narrow window opens briefly, but the plaza remains under scrutiny.

---

## Round 3 — Maintenance Access (Alarm Rising)

![Scene 3](images/20260222-145538/scene-003.png)

🤫 *Viper whispers to Drift:* "Drift, continuing along the Drift-guided MA/ST path toward KV; awaiting your next cue."

🎲 **Viper** — Dexterity (Stealth) check (DC 14)

#### ❌ Viper

> **Action:** I move from the shadowed alcove into Maintenance Shafts, enter the Service Tunnels, and plot a drift-guided route to KV using the holomap fragment and data-chipped key fragment, staying concealed and ready to breach KV with the passcode fragment.

A patrol drone sweeps near; a shadow flickers and alarms flare. Despite Viper's best efforts to stay hidden, the drones tighten their orbit and a lockdown threat cascades through the plaza. You are spotted; alarms flood the corridors, and security forces tighten their grip. Drift's beacon pulses to pivot you toward the Drift-guided MA/ST path; for now, you must abandon the shadowed approach and advance cautiously through Maintenance Access, aiming for the Data Core Wing with Drift's guidance.

🤫 *Jax whispers to Drift:* "Drift, prep Drift-guided MA/ST route; standby for cues."

🎲 **Jax** — Dexterity (Stealth) check (DC 16)

#### ❌ Jax

> **Action:** I move toward Rooftop Access via Maintenance Shafts, stay concealed, and prep a Drift-guided MA/ST route to KV or Data Core Wing. If a safe window opens, I’ll INT-hack to create a brief blind spot and consider meeting Lira Quell for the service-access map.

Jax attempts a stealthy slip through Maintenance Access, but a patrol drone's sweep catches your silhouette. The alarms flare and red lights wash the corridor; lockdown looms. Drift's beacon crackles: pivot to the Drift-guided MA/ST path toward KV or Data Core Wing, avoid bigger gates, and weather the scramble. For now, you areRetreated to Maintenance Access (Alarm Rising) as you regroup and awaiting a safer window to move.

🤫 *Neon whispers to Drift:* "Drift, maintain MA/ST routing to KV via EB/DC; standby for route cues."

🎲 **Neon** — Dexterity (Stealth) check (DC 16)

#### ✅ Neon

> **Action:** I advance stealthily through Maintenance Access toward Data Core Wing, following Drift’s MA/ST cues toward KV, and ready to interface security if a window opens.

Neon threads the Maintenance Access tunnels, Drift’s cues guiding every step. A narrow window of safety opens as you press along the MA/ST route toward KV. Cameras blink out as Neon slips into Data Core Wing, the corridor hums with servers while Drift coordinates the crossing toward KV. Neon remains stealthed as the bypass unfolds and a precious moment affords movement toward KV/DC. The team prepares to interface the next node.

### 📜 Round 3 Summary

A concealed conduit reveals an alternate route via EB/DC. Neon follows Drift through a hidden bypass toward KV, slipping into a chilled, data-lit corridor that hums with potential.

---

## Round 4 — Maintenance Access (Bypass Discovered)

![Scene 4](images/20260222-145538/scene-004.png)

🤫 *Viper whispers to Drift:* "Drift, hold MA/ST bypass; await cue toward KV; keep path open"

🎲 **Viper** — Dexterity (Stealth) check (DC 16)

#### ❌ Viper

> **Action:** I move deeper into the Maintenance Shafts, stay concealed, and await Drift's guidance for the MA/ST route to KV/DC; ready to pivot to Drift-guided path and breach KV when signaled.

Viper's silhouette is spotted as a maintenance drone's sensor sweep sweeps the shadow. Red alarms flare and a klaxon booms through the corridor. The lockdown looms as drones tighten their patrols; Drift signals a pivot to the Drift-guided MA/ST path or alternate exits toward Data Core Wing or Service Tunnels. The party is suddenly exposed and must regroup under heightened scrutiny. Viper is alerted, and the route forward remains perilous but possible with careful coordination.

🤫 *Jax whispers to Drift:* "Stand by for Drift MA/ST cues; pivot as signals come."

🎲 **Jax** — Dexterity (Stealth) check (DC 14)

#### ✅ Jax

> **Action:** I move toward Rooftop Access via Maintenance Shafts, stay concealed, and await Drift’s MA/ST cues toward KV/DC; I’ll pivot to Corporate Plaza for Lira’s intel if a safe window opens.

Jax threads through the Rooftop Access corridor, keeping to the shadowed seam as Drift's glow-lines map the route. The drones' whine fades as you slip into the Rooftop Access (Maintenance Shafts), rain-slick metal reflecting neon. With the stealth succeed, Drift confirms the path: hold this MA/ST route toward KV/DC and await the signal to breach the Kill-Switch Vault.

🤫 *Neon whispers to Drift:* "Drift, map MA/ST path to KV via EB and DC; standby for route cues."

🎲 **Neon** — Dexterity (Stealth) (DC 15)

#### ❌ Neon

> **Action:** I position in the Maintenance Alcove and prep a Drift-guided stealth MA/ST route toward KV via EB and DC; I stay in the shadows and await Drift cues, ready to interface cameras if a window opens.

Neon attempts to maintain concealment in the Maintenance Alcove, but a patrol drone locks onto their shadow. Alarms surge through the corridor as red lights wash the walls; the lockdown begins. Drift's guidance urges a rapid pivot to the Drift-guided MA/ST path toward KV via EB/DC, and the team must abandon the rooftop ingress for a stealth bypass under heavier scrutiny. Neon is now Alerted, and the party's route must adapt to the new Security Sweep.

### 📜 Round 4 Summary

Drift reveals a hidden conduit behind maintenance panels, a whisper-lit passage toward EB and DC. Neon slips through as cameras flicker to life, sensors trying to read the bypass. Neon threads the conduit, murmuring to Drift as the bypass glimmers into reach. The team advances on a different track, the data core wing pulsing with possibility and risk.

---

## Round 5 — Maintenance Access (Bypass Discovered)

🤫 *Viper whispers to Drift:* "Drift, hold MA/ST path toward KV/DC; standby for KV/DC cue; be ready to pivot to EB/DC conduit if alarms escalate."

🎲 **Viper** — Dexterity (Stealth) (DC 15)

#### ❌ Viper

> **Action:** I hold concealment in the Maintenance Shafts and wait for Drift cues to pivot toward KV/DC via MA/ST; I’m ready to exploit the EB/DC conduit if alarms spike.

Viper's concealment in the Maintenance Shafts fails as a patrol drone detects a flicker of movement. Alarms flare; red lights wash the corridor; a lockdown tightens around the plaza. Drift urges a rapid pivot to the Drift-guided MA/ST path or retreat into the Service Tunnels toward Data Core Wing. You break cover and slip into Maintenance Access, but the route ahead is compromised; you must regroup and await Drift's next cue.

🤫 *Jax whispers to Drift:* "Drift, align MA/ST bypass timing; hold until alarms ease; ready to pivot to EB/DC conduit if signals change."

🎲 **Jax** — Dexterity (Stealth) (DC 14)

#### ❌ Jax

> **Action:** I steady in Maintenance Access, align with Drift, and prep the Drift-guided MA/ST bypass toward KV/DC, ready to INT-hack if a window opens. If alarms spike, I pivot to the EB/DC conduit or Service Tunnels while keeping concealment.

Jax's stealth fails as a patrol drone locks onto your silhouette. Alarms scream through the Maintenance Access, neon strobes painting the corridor. Drift's beacon crackles in your comms, pressing you to pivot to the Drift-guided MA/ST path toward KV/DC, but a drone skirmish erupts in the Maintenance Alcove. Jax takes a glancing hit (-1 HP). The temporary camera blind is knocked offline, reducing visibility for 3 rounds. The party is forced to retreat into Maintenance Alcove and regroup, awaiting Drift's next cue. Exits remain Data Core Wing and Maintenance Shafts as you weather the skirmish.

🤫 *Neon whispers to Drift:* "Drift, continue MA/ST guidance toward KV via EB and DC; standby for route cues."

🎲 **Neon** — Dexterity (Stealth) (DC 16)

#### ✅ Neon

> **Action:** I slip deeper into the Maintenance Access, staying in shadows and ready to pivot to Drift-guided MA/ST path toward KV via EB and DC.

Neon melts deeper into Maintenance Access shadows, a drift-guided pulse of light marking the glow-lines as you slip past a wary drone. With a total of 16, you edge through the camera grid and sensors; alarms flare briefly but fail to fix on you. Drift nudges you toward a hidden EB/DC conduit panel; you slot through the narrow bypass and slide into the EB/DC conduit access. The hum of the conduit walls thrums with power as neon data streams skim your senses. You emerge into a dim access chamber along the EB/DC conduit, ready to press toward KV via the drift path.

### 📜 Round 5 Summary

A patrol drone closes in as Neon tries to slip deeper. Neon’s shadow flickers under neon, a camera grid momentarily blinks out but alarms spike; Drift redirects the party into the Drift-guided MA/ST path toward KV, leaving Neon to weather a drone skirmish.

---

## Round 6 — Maintenance Alcove (Drone Skirmish)

![Scene 5](images/20260222-145538/scene-005.png)

🤫 *Viper whispers to Drift:* "Plan executed: maintain MA/ST path to KV/DC; prepared to pivot to EB/DC conduit if alarms spike; await your cue for KV routing."

🎲 **Viper** — Dexterity (Stealth) (DC 18)

#### ❌ Viper

> **Action:** I stay hidden at the shadow edge, loot holomap fragment and data-chipped key fragment, pocket the passcode fragment if safe, slip to the Maintenance Shafts through the Service Tunnels, and await Drift guidance for the MA/ST path to KV/DC; maintain concealment and pivot to EB/DC conduit if alarms spike; breach KV with passcode and key fragment when Drift cues next.

Viper presses from shadow edge but a patrol drone detects movement; alarms flare and the corridor floodlights reveal the silhouette. You are spotted and forced to break concealment, sustaining a minor wound (-1 HP). Drift advises pivot to the Drift-guided MA/ST path, and you retreat into the Maintenance Alcove (Drone Skirmish) as drones tighten their sweep.

🎲 **Jax** — Dexterity (Stealth) (DC 14)

#### ❌ Jax

> **Action:** I move toward Rooftop Access via Maintenance Shafts, stay concealed, and prep for a Drift-guided MA/ST route toward KV/DC. If a safe window opens, I pivot to Corporate Plaza for Lira’s intel and attempt an INT-based Hack if a window appears.

Jax creeps through the Maintenance Shafts, but the dim glow betrays your shadow. The roll total of 9 fails to beat DC 14, and a patrol drone locks onto your silhouette. Alarms erupt, red strobes washing the corridor, and the Drift-guided bypass momentarily slips from reach. You break concealment and fall back toward the nearest shelter in the Maintenance Alcove, regrouping as drones tighten their sweep. Drift urges a pivot to the Drift-guided MA/ST path toward KV/DC, but for now you must hold position and await the next cue.

🤫 *Neon whispers to Drift:* "Drift, maintain MA/ST bypass toward KV via EB/DC and signal when a safe window opens."

🎲 **Neon** — Dexterity (Stealth) (DC 16)

#### ✅ Neon

> **Action:** I push deeper through Maintenance Access toward Data Core Wing, using Drift to guide a stealth MA/ST path toward KV via EB/DC and stay ready to pivot if alarms spike.

Neon moves with Drift's guidance, slipping into the Drift-guided MA/ST bypass and emerging at EB/DC Conduit Access. Conduits hum, drift-blue glow threads along the walls, and Neon maintains perfect concealment as the route toward KV via EB/DC lays open. The bypass holds the alarms at bay for the moment while Neon steadies the advance toward KV, ready to breach KV when Drift signals.

### 📜 Round 6 Summary

A window opens in Corporate Plaza for a parley with Lira Quell. Neon steps into the neon glow, drifting whispers guiding the negotiation as Lira offers the service-access map and cryptic passcode in exchange for credits and favors.

---

## Round 7 — Corporate Plaza

![Scene 6](images/20260222-145538/scene-006.png)

🤫 *Viper whispers to Drift:* "Drift, initiating MA/ST bypass toward DC and KV; monitor sensors; ready to pivot to EB/DC conduit if alarms spike."

🎲 **Viper** — Dexterity (Stealth) (DC 16)

#### ✅ Viper

> **Action:** I slip from the shadowed alcove into the Service Tunnels via the MA/ST bypass; advance toward Data Core Wing and KV, awaiting Drift's cue to breach KV.

Viper slips from the shadow into the Service Tunnels via the MA/ST bypass; cameras fail to lock on and you push deeper into the corporate labyrinth, nearing the Data Core Wing as the bypass hums to life. You pocket the passcode fragment and the drift-guided route remains open toward KV. Drift coalesces in the Data Core Wing, signaling the next leg of the path; the Data Core hums around you as you stand at the threshold of KV, with exits to KV, Elevator Bank, and DC.

🎲 **Jax** — Intelligence (Hack) (DC 14)

#### ✅ Jax

> **Action:** I move toward Rooftop Access via Maintenance Shafts, stay concealed, and prep Drift-guided MA/ST bypass toward KV/DC; I’ll pivot to Corporate Plaza for Lira intel if a safe window opens.

Jax’s neural-hack blade slices through the firewall. The AI’s countermeasures falter, logs bloom, and a secure channel opens. A data stream reveals a service-corridor map and a cryptic passcode; Drift’s bypass glows in your minds. The system grants a foothold toward KV via MA/ST and another access point into the Service Tunnels. The window is primed for a drift-guided maneuver when the team chooses to strike KV.

🤫 *Neon whispers to Drift:* "Drift, cue the KV bypass via EB/DC; I'll breach KV when you signal."

🎲 **Neon** — Dexterity (Stealth) (DC 16)

#### ❌ Neon

> **Action:** I slip toward the EB/DC conduit bypass and prepare to breach KV using the passcode and key fragment once Drift cues are given.

Neon attempts to slip through the EB/DC conduit toward KV, guided by drift-blue glow. A drone bursts from a conduit as alarms cascade. Neon takes a glancing hit and must pivot to the Drift-guided MA/ST path, regrouping in the EB/DC Conduit Access while the bypass hums back to life and drones close in.

### 📜 Round 7 Summary

A drone bursts from a conduit, neon teeth snapping; alarms cascade. Neon takes a glancing hit but presses on toward KV via the EB/DC conduit as Drift orders a pivot to the Drift-guided MA/ST path, regrouping in the conduit network.

---

## Round 8 — EB/DC Conduit Access

🤫 *Viper whispers to Drift:* "Drift, proceed with MA/ST bypass to KV/DC; awaiting your cue"

🎲 **Viper** — Dexterity (Stealth) (DC 16)

#### ❌ Viper

> **Action:** I slip into the Service Tunnels via the MA/ST bypass; push toward Data Core Wing and KV, keeping the passcode fragment secure; await Drift's cue.

Viper attempts to slip through the Maintenance/Service bypass in shadow, but a patrol drone locks onto the silhouette. The stealth check fails (DC 16). Alarms flood the corridor as red lights wash the walls. You take a glancing hit (-2 HP) and are forced to retreat deeper into the MA/ST Bypass Corridor. The bypass remains intact, but the route toward KV is momentarily compromised. Drift signals a safer path via the EB/DC conduit, and you regroup to press KV when the window opens.

🤫 *Jax whispers to Drift:* "Drift, hold MA/ST path toward KV/DC; ready to pivot to EB/DC conduit if alarms spike; Monitoring Corporate Plaza for Lira trade window."

🎲 **Jax** — Dexterity (Stealth) (DC 14)

#### ❌ Jax

> **Action:** I move toward Rooftop Access via Maintenance Shafts, stay concealed, and await Drift's MA/ST cues toward KV/DC; I’ll scan Corporate Plaza for a safe window to trade with Lira.

Jax's attempt to slip along the Maintenance/Service bypass is detected by a patrol drone; alarms flare as the corridor glows with neon. Your total of 7 fails to beat DC 14, and you are momentarily detected. You break concealment and retreat to the Maintenance Alcove, maintaining caution as Drift signals a renewed KV path. The bypass remains intact; you regroup and await Drift's next cue toward KV/DC.

🤫 *Neon whispers to Drift:* "Awaiting Drift's KV cue; keep route open."

🎲 **Neon** — Intelligence (Investigation) (DC 13)

#### ❌ Neon

> **Action:** I stay hidden in the EB/DC conduit bypass, awaiting Drift's cue to breach KV with the passcode fragment and key fragment.

Neon probes the logs and sensor feeds within the EB/DC conduit for a KV bypass clue, but the data stubbornly refuses to yield anything usable. The drift-readouts glitch and the pattern remains opaque; no passcode, map, or hidden route reveals itself. The window to glean intel closes without result, and Drift's cues remain unchanged. Neon remains concealed and ready to act on the next signal.

### 📜 Round 8 Summary

Hidden Maintenance Panel Access reveals a drift-marked route behind a dusty vent; Neon uncovers a forgotten conduit path that could bypass KV via EB/DC, changing the plan on a dime.

---

## Round 9 — Hidden Maintenance Panel Access

![Scene 7](images/20260222-145538/scene-007.png)

🤫 *Viper whispers to Drift:* "Proceed with MA/ST bypass to KV/DC; maintain concealment; alert me if alarms spike."

🎲 **Viper** — Charisma (Persuasion) (DC 15)

#### ❌ Viper

> **Action:** I slip from the shadowed alcove into the Service Tunnels via the MA/ST bypass; advance toward the Data Core Wing and KV with holomap fragment, data-chipped key fragment, and passcode fragment secured; await Drift’s cue for KV access; if alarms spike, pivot to the EB/DC conduit and proceed.

Viper attempts a quiet parley with Drift to secure a longer stealth window and coordinate a KV approach without drawing attention. The roll fails: Drift remains pragmatic, offering no extension to the window. The conversation ends with Drift reaffirming that bypass timing will follow the standard Drift-guided path; no extra time is granted. The party remains in position within the Service Tunnels and MA/ST bypass, but the window to advance KV via KV remains unchanged; Viper must rely on Drift's next cue rather than a self-driven persuasion.

🤫 *Jax whispers to Drift:* "Drift, maintain MA/ST timing; KV/DC route ready; watch for Lira window."

🎲 **Jax** — Intelligence (Hack) (DC 16)

#### ✅ Jax

> **Action:** Move toward Rooftop Access via Maintenance Shafts, stay concealed, prep Drift-guided MA/ST route to KV/DC; pivot to Corporate Plaza for Lira intel if a safe window opens; attempt INT-based Hack if a DM-set window appears.

Jax's Intelligence (Hack) succeeds. Drift-guided bypass reveals a hidden conduit behind the EB/DC conduit. A drift-marked route opens, aligning KV with Data Core Wing and EB/DC conduit. Jax slips into the Hidden Maintenance Panel Access (Drift Route), gaining a foothold toward KV as Drift coordinates the new bypass and the team readies to push KV.

🤫 *Neon whispers to Drift:* "Drift, prep drift-guided MA/ST path toward KV via EB/DC and cue when routes are clear."

🎲 **Neon** — Dexterity (Stealth) (DC 14)

#### ❌ Neon

> **Action:** I prep a stealth MA/ST run toward Data Core Wing and await Drift cues for a KV/DC path. If a window opens, I’ll attempt a brief camera/drones blind.

Neon attempts to slip past undetected through the Maintenance Access bypass toward KV/DC. The roll total is 11 (9 + 2), fails to beat DC 14. Alarms spike as a patrol drone locks onto Neon’s silhouette; cameras swivel and search; Neon breaks concealment and retreats to the Maintenance Alcove. Drift signals a renewed KV path, but the route is temporarily compromised and security posture tightens around the Data Core Wing.

### 📜 Round 9 Summary

Nyx-Grid intensifies; the Core Chamber flares with counters and illusions. The team must decide whether to bargain, fight, or contain Nyx-Grid as the kill-switch deadline looms.

---

## Round 10 — ArdentCore Core Chamber

![Scene 8](images/20260222-145538/scene-008.png)

🤫 *Viper whispers to Drift:* "Drift, continuing via MA/ST toward KV; KV access cue pending. Keep path clear and await signals."

🎲 **Viper** — Dexterity (Stealth) (DC 16)

#### ❌ Viper

> **Action:** I edge deeper through the MA/ST bypass toward Data Core Wing and KV, maintaining concealment; keep gear secured; await Drift's cue for KV access.

Viper's stealth attempt fails to slip past the drone net. Alarms flare as a patrol drone locks onto your silhouette. You dive for cover in the Maintenance Alcove as the drone fires a glancing shot; you take minor damage (-2 HP) and are forced to hide. Drift signals a renewed KV path via the EB/DC conduit, while a Security Drone (#8) polarizes surveillance. You regroup in the Maintenance Alcove (Drone Skirmish) and prepare to press KV via the Drift-guided route when the window opens.

🎲 **Jax** — Dexterity (Stealth) (DC 16)

#### ❌ Jax

> **Action:** I move toward Rooftop Access via Maintenance Shafts, stay hidden; prep Drift-guided MA/ST route to KV/DC and pivot to Corporate Plaza for Lira intel if a window opens; attempt INT-based Hack if a DM-set window appears.

Jax attempts to slip through the MA/ST bypass toward KV/DC, keeping to the shadows. A sensor sweep catches the edge of his silhouette; alarms flare and a patrol drone locks onto the disturbance. Concealment is breached, and security attention tightens. The KV route remains open via EB/DC, but the window now carries greater risk. Jax must regroup and rely on Drift to find a safer moment.

🤫 *Neon whispers to Drift:* "Drift, ready the drift-guided MA/ST path to KV via EB/DC; cue me when safe."

🎲 **Neon** — Charisma (Persuasion) (DC 15)

#### ❌ Neon

> **Action:** I prepare a stealth MA/ST route toward Data Core Wing and await Drift cues. I’ll blind cameras briefly if a window opens and keep Lira trade in mind for KV routing.

Neon steps into Corporate Plaza's neon haze and attempts to talk Lira Quell into trading the service-access map and cryptic passcode. Neon channels a calm, persuasive tone, but Lira pins Neon with a cold gaze and refuses to budge. The pitch falls flat; Lira hints that intel is earned, not begged, and demands something demonstrable of value before she reveals anything. The window closes; Neon recovers but the trade remains out of reach. The team must pivot: press the KV bypass via Drift's route or regain advantage through another angle.

### 📜 Round 10 Summary

Neon threads through Maintenance Access toward Data Core Wing via the MA/ST bypass, keeping to shadows. Drift cues align; you reach Data Core Wing and prepare for KV routing when the window opens.

---

## Final Status

- **Round:** 10/10
- **Location:** Data Core Wing
- **Quests:** Gain access to the Kill-Switch Vault (KV) by gathering a passcode, a physical key, and a working hack.; Parley with Lira Quell to obtain the service-access map and cryptic passcode.; Bypass the Security Lobby (SL) via Service Tunnels (ST) or Maintenance Access (MA) to DC/KV.; Face Nyx-Grid in the Core Chamber (AC) and decide to bargain, fight, or contain.; Security alert triggered at Exterior Gate; reassess approach and consider alternative exits via Maintenance Access or Service Tunnels under greater scrutiny.; Security alert triggered at Exterior Gate; reassess approach and consider Drift-guided MA/ST path or alternative exits.; Horizon remains wary; pivot to Drift-guided MA/ST path or alternative route through Service Tunnels toward Data Core Wing.; Negotiate with Lira for service-access map and cryptic passcode; the exchange may trigger a security response; prepare to move.; Reassess approach; consider Drift-guided MA/ST path or alternative exits.; Advance toward KV; Drift signals a bypass path through MA/ST toward EB and DC; prepare to breach KV with the passcode and physical key.; Reassess approach; consider Drift-guided MA/ST path or alternative exits.; Reassess approach; consider Drift-guided MA/ST path or alternative exits.; Reassess approach; consider Drift-guided MA/ST path or alternative exits.; Reassess approach; Drift-guided MA/ST path or alternative exits; avoid gate lockdown.; Drift-guided MA/ST path toward KV; prepare for potential interface window to blind cameras; coordinate with Drift for bypass to KV/DC.; Apply the bypass through MA toward EB and DC; coordinate with Drift to KV.; Reassess approach; Drift-guided MA/ST path or alternative exits; avoid gate lockdown.; Await Drift's MA/ST cues toward KV/DC; pivot to Corporate Plaza for Lira intel if a window opens.; Reassess approach; Drift-guided MA/ST path or alternative exits; avoid gate lockdown.; Apply the bypass through MA toward EB and DC; coordinate with Drift for bypass to KV.; Reassess approach; Drift-guided MA/ST path or alternative exits; avoid gate lockdown.; Hold position; repair drone damage if possible; await further Drift cues.; Drift cues Neon toward KV via EB/DC; prepare to breach KV with passcode and key fragment.; Hold position; repair drone damage if possible; await further Drift cues.; Hold position; regroup; await further Drift cues.; Hold position; regroup; await further Drift cues.; Pivot toward KV via EB/DC conduit; prepare to breach KV.; Trade intel: Acquire service-access map and cryptic passcode.; Pivot toward KV via MA/ST; breach KV when Drift cues.; KV bypass prep: use hacked service corridor data to align with Drift for KV/DC route; exploit the map and passcode when the window opens.; Continue toward KV; pivot to Drift-guided MA/ST path if alarms spike.; Continue toward KV; pivot to Drift-guided MA/ST path if alarms spike.; Regroup after skirmish; Drift cues renewed path toward KV when the window opens.; Regroup after failed stealth; Drift cues renewed KV path toward EB/DC when safe.; Await Drift cue; KV breach when signaled.; KV bypass prep: chart new route to KV via Hidden Panel path and EB/DC conduit.; KV bypass remains on Drift-guided path; await Drift’s next cue for KV access.; KV bypass rerouted through Hidden Panel path and EB/DC conduit; adapt plan accordingly.; Face Nyx-Grid in the Core Chamber and decide how to respond.; Regroup and press KV path; hold position; continue to Drift-guided route toward KV.; Detected by security; regroup and press KV via Drift-guided path with increased caution; await a safer window or pivot to Corporate Plaza if alarms intensify.; Lira declined to trade; pivot to Drift-guided KV path or seek alternate route to KV via MA/ST or EB/DC. Prepare to breach KV when a new window opens.; Await Drift cues for KV/DC path; maintain stealth; be ready to pivot to EB/DC if alarms spike.

- **Viper** (❤️ 4/9) — Injured
- **Jax** (❤️ 7/9) — Detected
- **Neon** (❤️ 7/9) — Tense

