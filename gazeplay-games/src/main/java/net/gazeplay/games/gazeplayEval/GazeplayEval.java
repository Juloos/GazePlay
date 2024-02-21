package net.gazeplay.games.gazeplayEval;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;
import javafx.util.Duration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.gazeplay.GameLifeCycle;
import net.gazeplay.commons.configuration.ActiveConfigurationContext;
import net.gazeplay.commons.random.ReplayablePseudoRandom;
import net.gazeplay.games.gazeplayEval.config.*;
import net.gazeplay.games.gazeplayEval.round.EvalRound;

import java.util.Iterator;
import java.util.function.Function;

@Slf4j
public class GazeplayEval implements GameLifeCycle {
    @Getter
    private final ReplayablePseudoRandom random;

    private final Iterator<EvalRound> rounds;
    private final EvalConfig config;

    private final KeyboardEventHandler keyboardHandler = new KeyboardEventHandler();

    @Getter
    private EvalRound currentRound;

    public GazeplayEval(double gameSeed) {
        this.random = new ReplayablePseudoRandom(gameSeed);

        GameState.context.startScoreLimiter();
        GameState.context.startTimeLimiter();

        final Function<Void, Void> onRoundFinishDummy = (aVoid) -> {
            this.onRoundFinish();
            return null;
        };

        try {
            config = new EvalConfig(GameState.variant.getNameGame());
            rounds = config.getItems().map(item -> new EvalRound(item, onRoundFinishDummy)).iterator();
        } catch (Exception e) {
            log.error("Error while loading the configuration file for the game " + GameState.variant.getNameGame(), e);
            throw new RuntimeException(e);
        }

        GameState.context.getPrimaryScene().addEventFilter(KeyEvent.KEY_PRESSED, keyboardHandler);
    }

    @Override
    public void launch() {
        if (currentRound != null) {
            log.error("Trying to launch a new round while the current one is still running");
            return;
        }
        log.info("Starting new round");

        currentRound = rounds.next();

//        this.canRemoveItemManually = true;

        GameState.context.setLimiterAvailable();
        GameState.stats.notifyNewRoundReady();

        GameState.context.getGazeDeviceManager().addStats(GameState.stats);
        GameState.context.firstStart();

        keyboardHandler.enable();

        currentRound.launch();
    }

    @Override
    public void dispose() {
        currentRound = null;
    }

    private void onRoundFinish() {
        Timeline transition = new Timeline();
        transition.getKeyFrames().add(new KeyFrame(new Duration(ActiveConfigurationContext.getInstance().getTransitionTime())));
        transition.setOnFinished((event) -> this.transitionToRoundFinish());
//        this.removeEventHandlerPictureCard();
        transition.playFromStart();
    }

    private void transitionToRoundFinish() {
        if (rounds.hasNext()) {
            this.dispose();
            GameState.context.clear();
            this.launch();
        } else {
//            this.finalStats();
//            EvalState.gameContext.updateScore(EvalState.stats, this);
//            this.resetFromReplay();
            this.dispose();
//            EvalState.gameContext.clear();
//            EvalState.gameContext.showRoundStats(EvalState.stats, this);
        }
    }

    private class KeyboardEventHandler implements EventHandler<KeyEvent> {

        private boolean ignoreAnyInput = false;

        @Override
        public void handle(KeyEvent key) {
            if (ignoreAnyInput)
                return;

//            if (key.getCode().isArrowKey()) {
//                ignoreAnyInput = true;
//                timelineQuestion.stop();
//                next("null");
//            } else if (key.getCode().getChar().equals("X")) {
//                ignoreAnyInput = true;
//                timelineQuestion.stop();
//                next("True");
//            } else if (key.getCode().getChar().equals("C")) {
//                ignoreAnyInput = true;
//                timelineQuestion.stop();
//                next("False");
//            } else if (key.getCode().getChar().equals("V")) {
//                timelineQuestion.stop();
//                removeItemAddedManually();
//            }
        }

        public void disable() {
            ignoreAnyInput = true;
        }

        public void enable() {
            ignoreAnyInput = false;
        }
    }
}
