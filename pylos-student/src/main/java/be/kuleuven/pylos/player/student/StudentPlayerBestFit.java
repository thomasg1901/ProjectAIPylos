package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

public class StudentPlayerBestFit extends PylosPlayer{

    Movement previousMove = null;
    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        // Simulate
        Movement movement = new Movement(Movement.MovementType.PASS,null, null, this.PLAYER_COLOR.other(),game.getState() );
        Movement bestMove = movement.simulate(new PylosGameSimulator(game.getState(), this.PLAYER_COLOR, board), board, 0);
        previousMove = bestMove;
        if (bestMove.getMovementType() == Movement.MovementType.ADD ||bestMove.getMovementType() == Movement.MovementType.MOVE){
            game.moveSphere(bestMove.getSphere(), bestMove.getLocation());
        }
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        // Simulate
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        // Simulate
    }
}
