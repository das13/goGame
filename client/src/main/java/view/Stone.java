package view;

import javafx.scene.layout.StackPane;
import javafx.scene.paint.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import static view.StoneColor.BLACK;
import static view.GoGame.TILE_SIZE;

public class Stone extends StackPane {

    private double x;
    private double y;
    private StoneColor stoneColor;

    public Stone(StoneColor stoneColor, double x, double y) {
        this.x = x;
        this.y = y;
        this.stoneColor = stoneColor;
        relocate(x, y);

        double backCircleRadius = TILE_SIZE / 2 - TILE_SIZE * 0.06;
        Circle backCircle = new Circle(backCircleRadius);
        backCircle.setFill(Color.valueOf("#db9900"));
        backCircle.setTranslateX((TILE_SIZE - backCircleRadius * 2) / 2);
        backCircle.setTranslateY((TILE_SIZE - backCircleRadius * 2) / 2);

        double frontCircleRadius = TILE_SIZE / 2 - TILE_SIZE * 0.1;
        Circle frontCircle = new Circle(frontCircleRadius);
        frontCircle.setTranslateX((TILE_SIZE - backCircleRadius * 2) / 2);
        frontCircle.setTranslateY((TILE_SIZE - backCircleRadius * 2) / 2);
        if (stoneColor.equals(BLACK)) {
            RadialGradient blackGrad = new RadialGradient(0,
                    .0,
                    frontCircle.getCenterX(),
                    frontCircle.getCenterY(),
                    frontCircle.getRadius(),
                    false,
                    CycleMethod.NO_CYCLE,
                    new Stop(0, Color.WHITE),
                    new Stop(1, Color.BLACK));
            frontCircle.setFill(blackGrad);
            getChildren().addAll(backCircle, frontCircle);
        } else {
            RadialGradient whiteGrad = new RadialGradient(0,
                    .0,
                    frontCircle.getCenterX(),
                    frontCircle.getCenterY(),
                    frontCircle.getRadius(),
                    false,
                    CycleMethod.NO_CYCLE,
                    new Stop(0, Color.WHITE),
                    new Stop(0.35, Color.WHITE),
                    new Stop(1, Color.SILVER));
            frontCircle.setFill(whiteGrad);
            getChildren().addAll(backCircle, frontCircle);
        }
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public StoneColor getStoneColor() {
        return stoneColor;
    }

}
