package net.gazeplay.games.gazeplayEval.round;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import net.gazeplay.commons.configuration.ActiveConfigurationContext;
import net.gazeplay.games.gazeplayEval.GameState;
import net.gazeplay.games.gazeplayEval.config.ItemConfig;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static net.gazeplay.games.gazeplayEval.config.Const.QUESTION_TEXT_SIZE;
import static net.gazeplay.games.gazeplayEval.config.QuestionScheduleType.*;
import static net.gazeplay.games.gazeplayEval.config.QuestionType.*;


@Slf4j
public class EvalRound {
    private final ItemConfig config;
    private final PictureCard[][] pictures;
    private final int picturesCount;
    private final Set<Pair<Integer, Integer>> selectedPictures = new HashSet<>();
    private final Function<Void, Void> onRoundFinish;

    private long startTime;

    public EvalRound(ItemConfig config, Function<Void, Void> onRoundFinish) {
        this.config = config;
        this.onRoundFinish = onRoundFinish;

        GameSizing.computeGameSizing(config.getRowSize(), config.getColumnSize(), GameState.context.getGamePanelDimensionProvider().getDimension2D());
        this.pictures = new PictureCard[config.getRowSize()][config.getColumnSize()];
        this.picturesCount = this.buildPictureGrid();
    }

    private int buildPictureGrid() {
        List<Integer> xOrder = new ArrayList<>(config.getRowSize());
        List<Integer> yOrder = new ArrayList<>(config.getColumnSize());

        for (int i = 0; i < config.getRowSize(); i++)
            xOrder.add(i);
        for (int j = 0; j < config.getColumnSize(); j++)
            yOrder.add(j);

        if (config.isGridRandomized()) {
            GameState.eval.getRandom().shuffle(xOrder);
            GameState.eval.getRandom().shuffle(yOrder);
        }

        int count = 0;
        for (int i = 0; i < config.getRowSize(); i++)
            for (int j = 0; j < config.getColumnSize(); j++)
                if (config.getGrid(i, j).isFile()) {
                    pictures[xOrder.get(i)][yOrder.get(j)] = new PictureCard(config, xOrder.get(i), yOrder.get(j), this::onPictureCardSelection);
                    count++;
                }
        return count;
    }

    private Pair<Integer, Integer> coordsOf(PictureCard pictureCard) {
        for (int i = 0; i < config.getRowSize(); i++)
            for (int j = 0; j < config.getColumnSize(); j++)
                if (pictures[i][j] == pictureCard)
                    return new Pair<>(i, j);
        return null;
    }

    private Void onPictureCardSelection(PictureCard pictureCard) {
        selectedPictures.add(coordsOf(pictureCard));
        GameState.stats.incrementNumberOfGoalsReached();
        GameState.context.updateScore(GameState.stats, GameState.eval);

        if (selectedPictures.size() >= config.getSelectionsRequired())
            onRoundFinish.apply(null);

        return null;  // Make Void happy
    }

    private Stream<PictureCard> getFlatPictures() {
        return Arrays.stream(pictures).flatMap(Arrays::stream).filter(Objects::nonNull);
    }

    public void launch() {
        startTime = System.currentTimeMillis();

        GameState.context.getChildren().addAll(this.getFlatPictures().toList());

        this.getFlatPictures().forEach(PictureCard::show);

//        If you need the TargetAOI list for this round, here it is
//        getFlatPictures().map(PictureCard::getTargetAOI).collect(Collectors.toCollection(ArrayList::new);

        GameState.stats.notifyNewRoundReady();
        GameState.context.onGameStarted(ActiveConfigurationContext.getInstance().getDelayBeforeSelectionTime());

        if (config.getQuestionSchedule() == BEFORE) {
            this.advertiseQuestion();
        } else {
            Timeline transition = new Timeline();
            transition.getKeyFrames().add(new KeyFrame(new Duration(ActiveConfigurationContext.getInstance().getDelayBeforeSelectionTime())));
            transition.setOnFinished((event) -> this.advertiseQuestion());
            transition.playFromStart();
        }
    }

    public void dispose() {
        getFlatPictures().forEach(PictureCard::dispose);
    }

    public RoundResults retrieveResults() {
        return new RoundResults(
            picturesCount,
            selectedPictures,
            System.currentTimeMillis() - startTime
        );
    }

    public void advertiseQuestion() {
        if (config.getQuestionType() == AUDIO) {
            if (ActiveConfigurationContext.getInstance().isSoundEnabled() && config.getQuestionAudioFile().isFile())
                GameState.context.getSoundManager().add(config.getQuestionAudioFile().getPath());
        } else {
            Text question = new Text(config.getQuestionText());
            question.setTextAlignment(TextAlignment.LEFT);
            question.setFill(Color.RED);  // Should be modified to be modular
            question.setX((GameState.context.getGamePanelDimensionProvider().getDimension2D().getWidth() - question.getLayoutBounds().getWidth()) / 2);
            question.setY(GameSizing.height + question.getLayoutBounds().getHeight() / 2);
            question.setScaleX(QUESTION_TEXT_SIZE);
            question.setScaleY(QUESTION_TEXT_SIZE);
            GameState.context.getChildren().add(question);
            // TODO: create a background for this text
        }
    }
}
