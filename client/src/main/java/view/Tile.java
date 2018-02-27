package view;

import javafx.geometry.Bounds;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import model.GameField;
import model.Point;
import model.PointState;

public class Tile extends Rectangle {
    private GoGame game;
    private LastStone lastStone;
    private Stone stone;
    private double[] corner;

    public Tile(int x, int y, GoGame game) {
        this.game = game;
        setWidth(GoGame.TILE_SIZE);
        setHeight(GoGame.TILE_SIZE);
        relocate(x * GoGame.TILE_SIZE, y * GoGame.TILE_SIZE);
        setFill(Color.valueOf("#db9900"));
        setStroke(Color.BLACK);
        setStrokeWidth(2);

        this.setOnMousePressed(event -> {
            if(event.getButton() == MouseButton.PRIMARY) {
                System.out.println("Mouse coordinates : X = " + event.getSceneX() + " Y = " + event.getSceneY());
                this.getTileCorner(event.getSceneX(), event.getSceneY());
                double xCoordinate = corner[0];
                double yCoordinate = corner[1];
                System.out.println("Corner coordinates: X = " + xCoordinate + " Y = " + yCoordinate);

                //will be removed when we have two players
                PointState estimatedPointState = game.clickCount % 2 == 0 ? PointState.STONE_WHITE : PointState.STONE_BLACK;

                if (GameField.isAllowedToPlace(xCoordinate, yCoordinate, estimatedPointState)) {
                    if(game.clickCount % 2 != 0){
                        if(game.getLastStone() != null){
                            game.removeLastStone();
                        }
                        stone = new Stone(StoneColor.BLACK, xCoordinate, yCoordinate);
                        this.game.drawStone(stone);
                        lastStone = new LastStone(StoneColor.BLACK, xCoordinate, yCoordinate);
                        this.game.drawLastStone(lastStone);
                    } else {
                        game.removeLastStone();
                        stone = new Stone(StoneColor.WHITE, xCoordinate, yCoordinate);
                        this.game.drawStone(stone);
                        lastStone = new LastStone(StoneColor.WHITE, xCoordinate, yCoordinate);
                        this.game.drawLastStone(lastStone);

                    }
                   GameField.addStone(new Point(xCoordinate, yCoordinate), estimatedPointState);
                }
            } else if (event.getButton() == MouseButton.SECONDARY) {
                System.out.println("Mouse coordinates : X = " + event.getSceneX() + " Y = " + event.getSceneY());
                this.getTileCorner(event.getSceneX(), event.getSceneY());
                System.out.println("Corner coordinates: X = " + corner[0] + " Y = " + corner[1]);
                game.removeStone(corner[0], corner[1]);
            }
        });
    }

    public void getTileCorner(double mouseClickX, double mouseClickY) {
        double layoutOffsetX = game.getTileGroup().getLayoutX();
        double layoutOffsetY = game.getTileGroup().getLayoutY();
        double strokeWidth = this.getStrokeWidth() / 2;
        Bounds bounds = this.getBoundsInParent();
        corner = new double[2];
        double middleX = ((layoutOffsetX + bounds.getMinX()) + (layoutOffsetX + bounds.getMaxX())) / 2;
        double middleY = ((layoutOffsetY + bounds.getMinY()) +(layoutOffsetY +  bounds.getMaxY())) / 2;
        if(mouseClickX <= middleX) {
            if(mouseClickY <= middleY) {
                corner[0] = bounds.getMinX() + strokeWidth;
                corner[1] = bounds.getMinY() + strokeWidth;
            } else {
                corner[0] = bounds.getMinX() + strokeWidth;
                corner[1] = bounds.getMaxY() - strokeWidth;
            }
        } else {
            if(mouseClickY <= middleY) {
                corner[0] = bounds.getMaxX() - strokeWidth;
                corner[1] = bounds.getMinY() + strokeWidth;
            } else {
                corner[0] = bounds.getMaxX() - strokeWidth;
                corner[1] = bounds.getMaxY() - strokeWidth;
            }
        }
    }
}