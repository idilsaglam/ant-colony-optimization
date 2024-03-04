/*
22015094 - SAGLAM Idil
*/
package aco.gui;

import aco.core.Board;

interface IVisitor {
    void visit(
            Board.Builder boardBuilder,
            EmptyCallback callback,
            EmptyCallback callback1,
            EmptyCallback callback2,
            EmptyCallback callback3,
            EmptyCallback callback4);
}
