/*
22015094 - SAGLAM Idil
*/
package aco.gui;

import aco.core.Board;

class MapPanelCurrentStateVisitor implements IVisitor {
    public void visit(
            Board.Builder boardBuilder,
            EmptyCallback nextState,
            EmptyCallback updateBoardBuilder,
            EmptyCallback updateSourcePoint,
            EmptyCallback updateDestinationPoint,
            EmptyCallback updateObstacles) {
        if (boardBuilder.isEnclosingRectangleIsPresent()) {
            nextState.call();
            updateBoardBuilder.call();
            if (boardBuilder.isSourcePointPresent()) {
                updateSourcePoint.call();
                nextState.call();
                if (boardBuilder.isDestinationPointPresent()) {
                    updateDestinationPoint.call();
                    nextState.call();
                }
            }
            updateObstacles.call();
        }
    }
}
