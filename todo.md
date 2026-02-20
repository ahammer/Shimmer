# Shimmer — TODO

## Completed — Phase 3 (Usability Improvements)

### P0-A: Fix decideNextAction hallucination
- [x] Moved `@Terminal` annotation from shimmer-agents to shimmer-core annotations
- [x] Enriched `toJsonClassMetadata()` with `terminal`, `returnType`, and parameter `name` fields
- [x] Replaced fragile regex-based method exclusion in `decide()` with structured `excludedMethods` filtering at the `KClass` level
- [x] Added backward-compatible typealias in shimmer-agents

### P0-B: Prevent cross-round action repetition
- [x] Added `previousRoundActions` field to `TurnState`
- [x] Injected "BANNED — Do NOT Repeat" section in `TurnStateInterceptor`
- [x] Track character actions per round in `GameSession.characterPreviousActions`

### P1-A: Sequential world-build pipeline
- [x] Replaced autonomous agent loop in `buildWorldSetup()` with deterministic sequential calls
- [x] Removed DecidingAgentAPI from world-build (no more recovery/fallback needed)

### P1-B: Narrative escalation
- [x] Added round-scaled escalation guidance in `WorldStateInterceptor`
- [x] Round 1: establish, Round 2: complicate, Round 3+: escalate/force crisis

### P2-A: Surface dice mechanics
- [x] Dice roll results now logged to `world.actionLog` (visible in player observations)

### P2-B: Class personality differentiation
- [x] Added `classInstincts()` in `CharacterInterceptor` with directives for 11 D&D classes
- [x] Injected as "Class Instincts" section in character prompt

## Pre-existing detekt issues (not introduced by Phase 3)
- `resolveAiAction` LongMethod/CyclomaticComplexMethod
- `expectedRollModifier` CyclomaticComplexMethod
- `GameEventListener` TooManyFunctions threshold
- Various MaxLineLength in GameSession and PlayerToolProvider
