package screen;

import java.util.Collections;
import java.util.List;

import campaign.CampaignUpgrades;
import engine.GameSettings;
import engine.GameState;
import entity.ShipType;
import records.RunSession;

/** Creates game screens with the defaults used by gameplay tests. */
public final class GameScreenFixtures {
    private GameScreenFixtures() { }

    public static GameScreen gameScreen(final GameState state, final GameSettings settings,
            final boolean bonusLife, final int width, final int height, final int fps) {
        return gameScreen(state, settings, bonusLife, width, height, fps, ShipType.STANDARD);
    }

    public static GameScreen gameScreen(final GameState state, final List<GameSettings> waves,
            final boolean bonusLife, final int width, final int height, final int fps) {
        return gameScreen(state, waves, bonusLife, width, height, fps, ShipType.STANDARD);
    }

    public static GameScreen gameScreen(final GameState state, final GameSettings settings,
            final boolean bonusLife, final int width, final int height, final int fps,
            final ShipType shipType) {
        return gameScreen(state, settings, bonusLife, width, height, fps, shipType, null);
    }

    public static GameScreen gameScreen(final GameState state, final List<GameSettings> waves,
            final boolean bonusLife, final int width, final int height, final int fps,
            final ShipType shipType) {
        return new GameScreen(state, waves, bonusLife, width, height, fps, shipType, null,
                CampaignUpgrades.none());
    }

    public static GameScreen gameScreen(final GameState state, final GameSettings settings,
            final boolean bonusLife, final int width, final int height, final int fps,
            final RunSession runSession) {
        return gameScreen(state, settings, bonusLife, width, height, fps, ShipType.STANDARD,
                runSession);
    }

    public static GameScreen gameScreen(final GameState state, final List<GameSettings> waves,
            final boolean bonusLife, final int width, final int height, final int fps,
            final RunSession runSession) {
        return new GameScreen(state, waves, bonusLife, width, height, fps, ShipType.STANDARD,
                runSession, CampaignUpgrades.none());
    }

    public static GameScreen gameScreen(final GameState state, final GameSettings settings,
            final boolean bonusLife, final int width, final int height, final int fps,
            final ShipType shipType, final RunSession runSession) {
        return gameScreen(state, settings, bonusLife, width, height, fps, shipType, runSession,
                CampaignUpgrades.none());
    }

    public static GameScreen gameScreen(final GameState state, final GameSettings settings,
            final boolean bonusLife, final int width, final int height, final int fps,
            final ShipType shipType, final RunSession runSession,
            final CampaignUpgrades campaignUpgrades) {
        return new GameScreen(state, Collections.singletonList(settings), bonusLife,
                width, height, fps, shipType, runSession, campaignUpgrades);
    }
}
