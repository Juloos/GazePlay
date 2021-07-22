package net.gazeplay.games.cassebrique;

import javafx.animation.PauseTransition;
import javafx.event.EventHandler;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import net.gazeplay.GameLifeCycle;
import net.gazeplay.IGameContext;
import net.gazeplay.commons.gaze.devicemanager.GazeEvent;
import net.gazeplay.commons.utils.stats.Stats;

import java.util.ArrayList;

public class CasseBrique implements GameLifeCycle {

    final private IGameContext gameContext;
    final private Stats stats;
    final private CasseBriqueGameVariant variant;

    final private Dimension2D dimension2D;

    private Circle ball;

    private Rectangle barre;

    private double widthbarre;
    private double heightbarre;
    private double sizeball;

    private double rad;
    private double speed;
    
    private double oldXbarre;

    final private ArrayList<Rectangle> walllist;
    final private ArrayList<Rectangle> wallhardlist;

    private double widthwall;
    private double heightwall;

    CasseBrique(final IGameContext gameContext, final Stats stats, final CasseBriqueGameVariant variant){
        this.gameContext = gameContext;
        this.stats = stats;
        this.variant = variant;

        dimension2D = gameContext.getGamePanelDimensionProvider().getDimension2D();

        walllist = new ArrayList<>();
        wallhardlist = new ArrayList<>();
    }

    public void launch(){
        widthbarre = dimension2D.getWidth()/5;
        heightbarre = dimension2D.getHeight()/35;
        sizeball = dimension2D.getHeight()*0.015;

        widthwall = dimension2D.getWidth()/20;
        heightwall = dimension2D.getHeight()/15;

        createbarre();
        createball();
        oldXbarre = barre.getX();
        
        if (variant.getLabel().equals("LV1")){
            LV1();
        }

        speed = 0;
        rad = 0;
        move();
        startafterdelay(5000);

        {
            Scene gameContextScene = gameContext.getPrimaryScene();

            EventHandler<GazeEvent> recordGazeMovements = e -> {
                Point2D toSceneCoordinate = gameContextScene.getRoot().localToScene(e.getX(), e.getY());
                barre.setX(toSceneCoordinate.getX() - widthbarre/2);

            };

            EventHandler<MouseEvent> recordMouseMovements = e -> {
                Point2D toSceneCoordinate = gameContextScene.getRoot().localToScene(e.getX(), e.getY());
                if (((toSceneCoordinate.getX()-dimension2D.getWidth()/2)*(toSceneCoordinate.getX()-dimension2D.getWidth()/2) + (toSceneCoordinate.getY()-dimension2D.getHeight()/2)*(toSceneCoordinate.getY()-dimension2D.getHeight()/2))>0.15) {
                    barre.setX(toSceneCoordinate.getX() - widthbarre/2);
                }

            };

            gameContextScene.getRoot().addEventFilter(GazeEvent.GAZE_MOVED, recordGazeMovements);
            gameContextScene.getRoot().addEventFilter(MouseEvent.MOUSE_MOVED, recordMouseMovements);
        }

        stats.notifyNewRoundReady();
        gameContext.getGazeDeviceManager().addStats(stats);
        gameContext.firstStart();
    }

    public void dispose(){

    }

    private void ballfall(){
        speed = 0;
        rad = 0;
        gameContext.getChildren().remove(ball);
        createball();
        startafterdelay(4000);
    }

    private void createball(){
        ball = new Circle(barre.getX() + widthbarre/2, dimension2D.getHeight() * 0.85, sizeball);
        gameContext.getChildren().add(ball);
    }

    private void createbarre(){
        barre = new Rectangle(dimension2D.getWidth()/2 - widthbarre/2, dimension2D.getHeight() * 0.92 - heightbarre/2, widthbarre, heightbarre);
        barre.setFill(Color.BROWN);
        gameContext.getChildren().add(barre);
    }

    private void startafterdelay(int delay){
        PauseTransition wait = new PauseTransition(Duration.millis(delay));
        wait.setOnFinished(e -> speed = 10);
        wait.play();
    }

    private void move(){
        PauseTransition wait = new PauseTransition(Duration.millis(15));
        wait.setOnFinished(e -> {
            wait.play();
            if (speed==0){
                ball.setFill(Color.GRAY);
                ball.setCenterX(barre.getX() + widthbarre/2);
            }
            else {
                ball.setFill(Color.RED);
            }
            if (ball.getCenterX() + sizeball >= dimension2D.getWidth()){
                rad = -rad;
            }
            else if (ball.getCenterX() - sizeball <= 0){
                rad = -rad;
            }
            if (ball.getCenterY() - sizeball <= 0){
                rad = -rad + Math.PI;
            }
            else if (ball.getCenterY()>=dimension2D.getHeight()){
                ballfall();
            }
            bounceBarre();
            for (Rectangle wall : walllist){
                bounceWall(wall, true);
            }
            for (Rectangle wall : wallhardlist){
                bounceWall(wall, false);
            }
            ball.setCenterX(ball.getCenterX() + speed * Math.sin(rad));
            ball.setCenterY(ball.getCenterY() + speed * Math.cos(rad));
            boolean test = false;
            for (Rectangle wall : walllist){
                test = test || ball.getCenterX()>wall.getX() && ball.getCenterX()<wall.getX()+wall.getWidth() && ball.getCenterY()>wall.getY() && ball.getCenterY()<wall.getY()+wall.getHeight();
            }
            for (Rectangle wall : wallhardlist){
                test = test || ball.getCenterX()>wall.getX() && ball.getCenterX()<wall.getX()+wall.getWidth() && ball.getCenterY()>wall.getY() && ball.getCenterY()<wall.getY()+wall.getHeight();
            }
            test = test || ball.getCenterX()>barre.getX() && ball.getCenterX()<barre.getX()+barre.getWidth() && ball.getCenterY()>barre.getY() && ball.getCenterY()<barre.getY()+barre.getHeight();
            if (test){
                ball.setCenterX(ball.getCenterX() - speed * Math.sin(rad));
                ball.setCenterY(ball.getCenterY() - speed * Math.cos(rad));
            }
            oldXbarre = barre.getX() - widthbarre/2;
        });
        wait.play();
    }

    private void bounceBarre(){
        if (ball.getCenterY() + sizeball >= barre.getY() && ball.getCenterY() <= barre.getY() && ball.getCenterX() + sizeball >= barre.getX() && ball.getCenterX() - sizeball <= barre.getX() + widthbarre){
            rad = -rad + Math.PI + radMoveBarre();
        }
        if (ball.getCenterY() - sizeball <= barre.getY() + barre.getHeight() && ball.getCenterY() >= barre.getY() + barre.getHeight() && ball.getCenterX() + sizeball >= barre.getX() && ball.getCenterX() - sizeball <= barre.getX() + widthbarre){
            rad = -rad + Math.PI + radMoveBarre();
        }
        if (ball.getCenterX() + sizeball >= barre.getX() && ball.getCenterX() <= barre.getX() && ball.getCenterY() + sizeball >= barre.getY() && ball.getCenterY() - sizeball <= barre.getY() + heightbarre){
            rad = -rad + radMoveBarre();
        }
        if (ball.getCenterX() - sizeball <= barre.getX() + barre.getWidth() && ball.getCenterX() >= barre.getX() + barre.getWidth() && ball.getCenterY() + sizeball >= barre.getY() && ball.getCenterY() - sizeball <= barre.getY() + heightbarre){
            rad = -rad + radMoveBarre();
        }
    }

    private void bounceWall(Rectangle wall, boolean remove){
        boolean touch = false;
        if (ball.getCenterY() + sizeball >= wall.getY() && ball.getCenterY() <= wall.getY() && ball.getCenterX() + sizeball >= wall.getX() && ball.getCenterX() - sizeball <= wall.getX() + wall.getWidth()){
            rad = -rad + Math.PI;
            touch = true;
        }
        if (ball.getCenterY() - sizeball <= wall.getY() + wall.getHeight() && ball.getCenterY() >= wall.getY() + wall.getHeight() && ball.getCenterX() + sizeball >= wall.getX() && ball.getCenterX() - sizeball <= wall.getX() + wall.getWidth()){
            rad = -rad + Math.PI;
            touch = true;
        }
        if (ball.getCenterX() + sizeball >= wall.getX() && ball.getCenterX() <= wall.getX() && ball.getCenterY() + sizeball >= wall.getY() && ball.getCenterY() - sizeball <= wall.getY() + wall.getHeight()){
            rad = -rad;
            touch = true;
        }
        if (ball.getCenterX() - sizeball <= wall.getX() + wall.getWidth() && ball.getCenterX() >= wall.getX() + wall.getWidth() && ball.getCenterY() + sizeball >= wall.getY() && ball.getCenterY() - sizeball <= wall.getY() + wall.getHeight()){
            rad = -rad;
            touch = true;
        }
        if (touch && remove){
            walllist.remove(wall);
            gameContext.getChildren().remove(wall);
            stats.incrementNumberOfGoalsReached();
            testwin();
        }
    }

    private double radMoveBarre(){
        return 2*(oldXbarre - barre.getX())/dimension2D.getWidth();
    }

    private void testwin(){
        if (walllist.isEmpty()){
            stats.stop();

            gameContext.updateScore(stats, this);

            gameContext.playWinTransition(500, actionEvent -> {

                gameContext.getGazeDeviceManager().clear();

                gameContext.clear();

                gameContext.showRoundStats(stats, this);
            });
        }
    }

    private void build(int[][] Map){
        Rectangle wall;
        for (int i=0; i<20; i++){
            for (int j=0; j<10; j++){
                if (Map[j][i]==1){
                    wall = new Rectangle(i*widthwall, j*heightwall, widthwall, heightwall);
                    wall.setFill(Color.GREEN);
                    walllist.add(wall);
                    gameContext.getChildren().add(wall);
                }
                else if (Map[j][i]==2){
                    wall = new Rectangle(i*widthwall, j*heightwall, widthwall, heightwall);
                    wall.setFill(Color.GRAY);
                    wallhardlist.add(wall);
                    gameContext.getChildren().add(wall);
                }
            }
        }
    }
    
    private void LV1(){
        int[][] Map = new int[][]
            {
                {0,0,0,0,0,0,0,2,0,0,0,1,0,0,2,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
                {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}
            };
        build(Map);
    }
}
