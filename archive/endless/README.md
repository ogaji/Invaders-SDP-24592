# Endless Mode Archive

**Date Archived:** November 18, 2026
**Reason:** Endless mode (`InfiniteGameScreen`) was a separate screen copied from `GameScreen`. Maintaining it required duplicating all in-game changes, and in practice, campaign changes were not reaching this copy. To streamline refactoring and feature development so they only deal with `GameScreen`, Endless mode has been removed from the active build and archived here for reference.

## Archived Files
- `src/screen/InfiniteGameScreen.java`
- `src/engine/EndlessRewards.java`
- `src/entity/ZigzagEnemyShip.java`
- `test/screen/InfiniteGameScreenControlsTest.java`
- `test/engine/EndlessRewardsTest.java`
- `test/engine/EndlessRewardIntegrationTest.java`
- `docs/Infinite_Mode_Controls.md`

## Removed Connection Points
- **`src/screen/GameModeScreen.java`**: Reduced choices to 2 (Campaign Mode and Back) and removed the Endless branch.
- **`src/screen/TitleScreen.java`**: Removed the hidden `I` key shortcut.
- **`src/screen/ScoreScreen.java`**: Removed the `infiniteMode` flag and specific constructors; the screen now exclusively handles Campaign logic.
- **`src/engine/Core.java`**: Removed the `case 8` loop, `runEndless()`, and the `endlessRewards` settlement flow.
- **`src/engine/DrawManager.java`**: Removed the "Endless Mode" text label from `drawGameModeMenu`.
- **`build.gradle`**: Removed the Endless test class from `smokeTestClasses`.
- **Tests**: Cleaned up `CampaignEnemyRewardIntegrationTest`, `EnemyShipClassificationTest`, and `GameplayIntegrationTest` to remove Endless assertions and logic branches.