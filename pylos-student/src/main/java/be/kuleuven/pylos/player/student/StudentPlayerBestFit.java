package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

public class StudentPlayerBestFit extends PylosPlayer{

    Movement previousMove = null;
    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        // Simulate
        Movement emptyMovement = new Movement(this.PLAYER_COLOR.other(),PylosGameState.MOVE);
        Movement bestMove = emptyMovement.simulate(new PylosGameSimulator(game.getState(), this.PLAYER_COLOR, board), board, 0, true);
        previousMove = bestMove;
        if (bestMove.getMovementType() == Movement.MovementType.ADD || bestMove.getMovementType() == Movement.MovementType.MOVE){
            game.moveSphere(bestMove.getSphere(), bestMove.getLocation());
        }
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        // Simulate
        Movement emptyMovement = new Movement(this.PLAYER_COLOR,PylosGameState.REMOVE_FIRST);
        Movement bestRemoveFirst = emptyMovement.simulate(new PylosGameSimulator(game.getState(), this.PLAYER_COLOR, board), board, 0, true);
        previousMove = bestRemoveFirst;
        if (bestRemoveFirst.getMovementType() == Movement.MovementType.YOINK_FIRST){
            game.removeSphere(bestRemoveFirst.getSphere());
        }
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        // Simulate
        Movement emptyMovement = new Movement(this.PLAYER_COLOR, PylosGameState.REMOVE_SECOND);
        Movement bestRemoveSecond = emptyMovement.simulate(new PylosGameSimulator(game.getState(), this.PLAYER_COLOR, board), board, 0, true);
        previousMove = bestRemoveSecond;
        if (bestRemoveSecond.getMovementType() == Movement.MovementType.YOINK_SECOND){
            game.removeSphere(bestRemoveSecond.getSphere());
        } else {
            game.pass();
        }
    }
}
