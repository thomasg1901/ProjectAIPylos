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

    public Movement(PylosPlayerColor color, PylosGameState state){
        this.color = color;
        this.state = state;
    }

    public Movement simulate(PylosGameSimulator simulator, PylosBoard board, int depth, boolean initialMovement){
        if(depth > MAX_TREE_DEPTH || simulator.getState() == PylosGameState.COMPLETED){
            return null;
        }

        boolean isFinished = true;
        PylosLocation prevLocation = sphere == null || sphere.isReserve() ? null : sphere.getLocation();

        if(!initialMovement)
            isFinished = execute(simulator, board, color);

        this.movementScore = evaluateState(board, color);

        ArrayList<Movement> possibleMovements = null;
        if(simulator.getState() == PylosGameState.REMOVE_FIRST){
            possibleMovements = getPossibleRemovalMovements(board,simulator.getState(), color, MovementType.YOINK_FIRST);
        } else if (simulator.getState() == PylosGameState.REMOVE_SECOND) {
            possibleMovements = getPossibleRemovalMovements(board, simulator.getState(), color, MovementType.YOINK_SECOND);
        } else {
            PylosPlayerColor nextColor = color.other();
            possibleMovements = getPossibleMovements(board, simulator.getState(), nextColor);
        }

        Movement bestMovement = null;
        int bestScore = Integer.MIN_VALUE;
        for(Movement possibleMovement : possibleMovements){
            possibleMovement.simulate(simulator, board, depth+1, false);
            if(bestScore < possibleMovement.movementScore){
                bestMovement = possibleMovement;
                bestScore = possibleMovement.movementScore;
            }
        }
        this.movementScore = bestScore;
        if(this.movementType != null)
            reverseSimulation(simulator, prevLocation);
        return bestMovement;
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
                    for (PylosSphere pylosSphere : spheres){
                        if(!pylosSphere.isReserve() && pylosSphere.canMoveTo(loc) && pylosSphere.PLAYER_COLOR == color){
                            possibleMovements.add(new Movement(MovementType.MOVE, pylosSphere, loc, color, state));
                        }
                    }
                }
            }
        }
        return possibleMovements;
    }

    private void reverseSimulation(PylosGameSimulator simulator, PylosLocation previousLocation){
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
        if (movementType == MovementType.PASS){
            simulator.pass();
            return true;
        }

//        PylosLocation previousLocation = sphere.getLocation();
//        previousLocation = location;
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

    public MovementType getMovementType() {
        return movementType;
    }

    public void setMovementType(MovementType movementType) {
        this.movementType = movementType;
    }

    public PylosSphere getSphere() {
        return sphere;
    }

    public void setSphere(PylosSphere sphere) {
        this.sphere = sphere;
    }

    public PylosLocation getLocation() {
        return location;
    }

    public void setLocation(PylosLocation location) {
        this.location = location;
    }

//    public PylosLocation getPreviousLocation() {
//        return previousLocation;
//    }
//
//    public void setPreviousLocation(PylosLocation previousLocation) {
//        this.previousLocation = previousLocation;
//    }

    public PylosPlayerColor getColor() {
        return color;
    }

    public void setColor(PylosPlayerColor color) {
        this.color = color;
    }

    public PylosGameState getState() {
        return state;
    }

    public void setState(PylosGameState state) {
        this.state = state;
    }

    public int getMovementScore() {
        return movementScore;
    }

    public void setMovementScore(int movementScore) {
        this.movementScore = movementScore;
    }

    public int getMAX_TREE_DEPTH() {
        return MAX_TREE_DEPTH;
    }

    public enum MovementType {
        ADD,
        MOVE,
        YOINK_FIRST,
        YOINK_SECOND,
        PASS
    }
}

