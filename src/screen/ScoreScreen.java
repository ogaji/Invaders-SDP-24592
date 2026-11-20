package screen;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import campaign.CampaignStageData;
import engine.Cooldown;
import engine.Core;
import engine.GameState;
import engine.GameplayConfig;
import engine.Score;
import audio.AudioManager;
import records.Achievement;
import records.RecordsService;
import java.util.ArrayList;

public class ScoreScreen extends Screen {

    private static final int SELECTION_TIME = 200;
    private static final int MAX_HIGH_SCORE_NUM = 7;
    private static final int FIRST_CHAR = 65;
    private static final int LAST_CHAR = 90;

    private int level;
    private boolean stageFailed;
    private final List<Achievement> runUnlocks;
    private boolean gameCleared;
    private String modeLabel;
    private int score;
    private int livesRemaining;
    private int bulletsShot;
    private int shipsDestroyed;
    private List<Score> highScores;
    private boolean isNewRecord;
    private char[] name;
    private int nameCharSelected;
    private Cooldown selectionCooldown;

    public ScoreScreen(final int width, final int height, final int fps, final GameState state) {
        this(width,height,fps,state,Collections.<Achievement>emptyList());
    }

    public ScoreScreen(final int width, final int height, final int fps,
                       final GameState gameState, final List<Achievement> unlocks) {
        super(width,height,fps);
        this.runUnlocks = new ArrayList<Achievement>(unlocks);

        this.modeLabel = "mode: " + difficultyName(GameplayConfig.getDifficulty());
        this.level = gameState.getLevel();
        this.stageFailed = gameState.getLivesRemaining() == 0;
        this.gameCleared = !this.stageFailed && gameState.getLevel() > CampaignStageData.getTotalStages();
        this.score = gameState.getScore();
        this.livesRemaining = gameState.getLivesRemaining();
        this.bulletsShot = gameState.getBulletsShot();
        this.shipsDestroyed = gameState.getShipsDestroyed();
        this.isNewRecord = false;
        this.name = "AAA".toCharArray();
        this.nameCharSelected = 0;
        this.selectionCooldown = Core.getCooldown(SELECTION_TIME);
        this.selectionCooldown.reset();

        try {
            this.highScores = Core.getFileManager().loadHighScores();
            if (highScores.size() < MAX_HIGH_SCORE_NUM
                || highScores.get(highScores.size() - 1).getScore()
                < this.score)
                this.isNewRecord = true;

        } catch (IOException e) {
            logger.warning("Couldn't load high scores!");
        }

        if (this.livesRemaining > 0){
            AudioManager.playBGM("audio/bgm/music_end_victory.wav");
        } else {
            AudioManager.playBGM("audio/bgm/music_end_defeat.wav");
        }

        if (this.isNewRecord) {
            AudioManager.playSFX("audio/sfx/new_record.wav");
        }

        RecordsService recordsService = Core.getRecordsService();
        if (recordsService != null) {
            boolean hasNewAchievement = false;
            Achievement achievement;
            while ((achievement = recordsService.pollNotification()) != null) {
                if (this.runUnlocks.contains(achievement)) {
                    hasNewAchievement = true;
                }
            }
            if (hasNewAchievement && !this.isNewRecord) {
                AudioManager.playSFX("audio/sfx/achievement_unlock.wav");
            }
        }
    }

    protected final void update() {
        super.update();

        draw();
        if (this.inputDelay.checkFinished()) {
            if (inputManager.isKeyDown(KeyEvent.VK_ESCAPE)) {
                AudioManager.playSFX("audio/sfx/menu_back.wav");
                this.nextDestination = Destination.TITLE;
                this.isRunning = false;
                if (this.isNewRecord)
                    saveScore();
            } else if (inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
                AudioManager.playSFX("audio/sfx/menu_select.wav");
                this.nextDestination = Destination.CAMPAIGN;
                this.isRunning = false;
                if (this.isNewRecord)
                    saveScore();
            }

            if (this.isNewRecord && this.selectionCooldown.checkFinished()) {
                if (inputManager.isKeyDown(KeyEvent.VK_RIGHT)) {
                    this.nameCharSelected = this.nameCharSelected == 2 ? 0
                        : this.nameCharSelected + 1;
                    this.selectionCooldown.reset();
                    AudioManager.playSFX("audio/sfx/name_cursor_move.wav");
                }
                if (inputManager.isKeyDown(KeyEvent.VK_LEFT)) {
                    this.nameCharSelected = this.nameCharSelected == 0 ? 2
                        : this.nameCharSelected - 1;
                    this.selectionCooldown.reset();
                    AudioManager.playSFX("audio/sfx/name_cursor_move.wav");
                }
                if (inputManager.isKeyDown(KeyEvent.VK_UP)) {
                    this.name[this.nameCharSelected] =
                        (char) (this.name[this.nameCharSelected]
                            == LAST_CHAR ? FIRST_CHAR
                            : this.name[this.nameCharSelected] + 1);
                    this.selectionCooldown.reset();
                    AudioManager.playSFX("audio/sfx/name_char_change.wav");
                }
                if (inputManager.isKeyDown(KeyEvent.VK_DOWN)) {
                    this.name[this.nameCharSelected] =
                        (char) (this.name[this.nameCharSelected]
                            == FIRST_CHAR ? LAST_CHAR
                            : this.name[this.nameCharSelected] - 1);
                    this.selectionCooldown.reset();
                    AudioManager.playSFX("audio/sfx/name_char_change.wav");
                }
            }
        }
    }

    private void saveScore() {
        highScores.add(new Score(new String(this.name), score));
        Collections.sort(highScores);
        if (highScores.size() > MAX_HIGH_SCORE_NUM)
            highScores.remove(highScores.size() - 1);

        try {
            Core.getFileManager().saveHighScores(highScores);
        } catch (IOException e) {
            logger.warning("Couldn't load high scores!");
        }
    }

    private static String difficultyName(final int difficulty) {
        switch (difficulty) {
            case GameplayConfig.EASY:  return "Easy";
            case GameplayConfig.HARD:  return "Hard";
            default:                   return "Normal";
        }
    }

    private void draw() {
        drawManager.initDrawing(this);
        float accuracy = this.bulletsShot == 0 ? 1f : (float)this.shipsDestroyed / this.bulletsShot;
        if (!this.runUnlocks.isEmpty()) {
            drawManager.drawAchievementResults(this,this.score,this.livesRemaining,
                this.shipsDestroyed,accuracy,this.isNewRecord,this.name,this.nameCharSelected,
                this.runUnlocks,this.inputDelay.checkFinished(),
                this.stageFailed ? this.level : 0, this.gameCleared,
                this.modeLabel);
            drawManager.completeDrawing(this); return;
        }

        drawManager.drawGameOver(this, this.inputDelay.checkFinished(),
            this.isNewRecord,this.gameCleared);
        if (this.stageFailed)
            drawManager.drawStageFailed(this, this.level, this.isNewRecord);
        drawManager.drawResults(this, this.score, this.livesRemaining,
            this.shipsDestroyed, accuracy, this.isNewRecord);
        drawManager.drawModeLabel(this, this.modeLabel, this.isNewRecord);

        if (this.isNewRecord)
            drawManager.drawNameInput(this, this.name, this.nameCharSelected);

        drawManager.completeDrawing(this);
    }
}
