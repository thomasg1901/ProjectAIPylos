package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import com.sun.source.tree.Tree;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Map;

public class Movement {

    private MovementType movementType;

    private PylosSphere sphere;

    private PylosLocation location;

    private PylosLocation previousLocation;

    private PylosPlayerColor color;

    private PylosGameState state;

    private int movementScore;

    private final int MAX_TREE_DEPTH = 2;

    public Movement(MovementType movementType, PylosSphere sphere, PylosLocation location, PylosPlayerColor color, PylosGameState state) {
        this.movementType = movementType;
        this.sphere = sphere;
        this.location = location;
        this.color = color;
        this.state = state;
    }

    private void simulate(PylosGameSimulator simulator, PylosBoard board, int depth){
        if(depth > MAX_TREE_DEPTH){
            return;
        }

        boolean isFinished = execute(simulator, board, color);
        this.movementScore = evaluateState(board, color);

        ArrayList<Movement> possibleMovements = null;
        if(!isFinished){
            possibleMovements = getPossibleRemovalMovements(board,simulator.getState(), color, MovementType.YOINK_FIRST);
        } else if (movementType == MovementType.YOINK_FIRST) {
            possibleMovements = getPossibleRemovalMovements(board, simulator.getState(), color, MovementType.YOINK_SECOND);
        } else{
            PylosPlayerColor nextColor = color.other();
            possibleMovements = getPossibleMovements(board, simulator.getState(), nextColor);
        }

        for(Movement possibleMovement : possibleMovements){
            possibleMovement.simulate(simulator, board, depth+1);
        }

        reverseSimulation(simulator);
    }

    private int evaluateState(PylosBoard board, PylosPlayerColor playerColor){
        int ourReserver = board.getReservesSize(playerColor);
        int enemyReserve = board.getReservesSize(playerColor.other());
        return ourReserver - enemyReserve;
    }

    private ArrayList<Movement> getPossibleMovements(PylosBoard board, PylosGameState state ,PylosPlayerColor color){
        ArrayList<Movement> possibleMovements = new ArrayList<>();
        //Get place locations
        for (PylosLocation loc : board.getLocations()) {
            if (loc.isUsable()) {
                // Possible ADD movements
                possibleMovements.add(new Movement(MovementType.ADD, board.getReserve(color), loc, color, state));

                // Possible MOVE movements
                if(loc.Z > 0){
                    PylosSphere[] spheres = board.getSpheres(color);
                    for (PylosSphere sphere : spheres){
                        if(sphere.canMoveTo(loc)){
                            possibleMovements.add(new Movement(MovementType.MOVE, board.getReserve(color), loc, color, state));
                        }
                    }
                }
            }
        }

        return possibleMovements;
    }

    private void reverseSimulation(PylosGameSimulator simulator){
        switch (movementType){
            case ADD:
                simulator.undoAddSphere(sphere, state,color);
                break;
            case MOVE:
                simulator.undoMoveSphere(sphere, previousLocation, state,color);
                // undo move
                break;
            case YOINK_FIRST:
                simulator.undoRemoveFirstSphere(sphere, previousLocation,state , color);
                // undo yoink
                break;
            case YOINK_SECOND:
                simulator.undoRemoveSecondSphere(sphere, previousLocation, state, color);
                // undo yoink
                break;
            default:
                simulator.undoPass(state, color);
                break;
        }
    }

    private ArrayList<Movement> getPossibleRemovalMovements(PylosBoard board,PylosGameState state , PylosPlayerColor color, MovementType type){
        ArrayList<Movement> possibleYoinks = new ArrayList<>();
        if (type == MovementType.YOINK_SECOND){
            possibleYoinks.add(new Movement(MovementType.PASS, null, null, color, state));
        }
        ArrayList<PylosSphere> removableSpheres = getRemovableSpheres(board, color);
        for(PylosSphere sphere : removableSpheres){
            possibleYoinks.add(new Movement(type, sphere, null, color, state));
        }
        return possibleYoinks;
    }

    // return true if the turn is fully finished; false when an extra movement needs to be done (remove/pass sphere)
    private boolean execute(PylosGameSimulator simulator, PylosBoard board, PylosPlayerColor color){
        if (movementType == MovementType.PASS)
            return true;

        previousLocation = location;
        if(movementType == MovementType.ADD || movementType == MovementType.MOVE) {
            simulator.moveSphere(sphere, location);

            PylosSquare[] squares = board.getAllSquares();
            boolean completedSquare = false;
            for (PylosSquare bsInSquare : squares) {
                completedSquare |= bsInSquare.isSquare(color);
            }
            if(completedSquare){
                return false;
            }

        } else if (movementType == MovementType.YOINK_FIRST || movementType == MovementType.YOINK_SECOND) {
            simulator.removeSphere(sphere);
        }
        location = sphere.getLocation();
        return true;
    }

    private ArrayList<PylosSphere> getRemovableSpheres(PylosBoard board, PylosPlayerColor color){
        ArrayList<PylosSphere> removableSpheres = new ArrayList<>();
        for (PylosSphere pylosSphere : board.getSpheres(color)) {
            if (!pylosSphere.isReserve() && !pylosSphere.getLocation().hasAbove()) {
                removableSpheres.add(pylosSphere);
            }
        }
        return removableSpheres;
    }

    private enum MovementType {
        ADD,
        MOVE,
        YOINK_FIRST,
        YOINK_SECOND,
        PASS
    }
}

