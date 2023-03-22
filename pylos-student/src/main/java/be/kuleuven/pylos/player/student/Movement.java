package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;

import java.util.ArrayList;

public class Movement {

    private MovementType movementType;

    private PylosSphere sphere;

    private PylosLocation location;

    private PylosPlayerColor currentColor;
    private  PylosPlayerColor playerColor;

    private PylosGameState state;

    private int movementScore;

    private final int MAX_TREE_DEPTH = 6;

    public Movement(MovementType movementType, PylosSphere sphere, PylosLocation location, PylosPlayerColor color, PylosPlayerColor playerColor, PylosGameState state) {
        this.movementType = movementType;
        this.sphere = sphere;
        this.location = location;
        this.currentColor = color;
        this.playerColor = playerColor;
        this.state = state;
    }

    public Movement(PylosPlayerColor color, PylosGameState state){
        this.currentColor = color;
        this.playerColor = color.other();
        this.state = state;
    }

    public Movement simulate(PylosGameSimulator simulator, PylosBoard board, int depth, boolean initialMovement){
        if(depth > MAX_TREE_DEPTH || simulator.getState() == PylosGameState.COMPLETED){
            this.movementScore = Integer.MIN_VALUE;
            return null;
        }

        PylosLocation prevLocation = sphere == null || sphere.isReserve() ? null : sphere.getLocation();

        if(!initialMovement){
            execute(simulator, board, currentColor);
            this.movementScore = evaluateState(board);
        }


        ArrayList<Movement> possibleMovements = null;
        if(simulator.getState() == PylosGameState.REMOVE_FIRST){
            possibleMovements = getPossibleRemovalMovements(board,simulator.getState(), currentColor, MovementType.YOINK_FIRST);
        } else if (simulator.getState() == PylosGameState.REMOVE_SECOND) {
            possibleMovements = getPossibleRemovalMovements(board, simulator.getState(), currentColor, MovementType.YOINK_SECOND);
        } else {
            PylosPlayerColor nextColor = currentColor.other();
            possibleMovements = getPossibleMovements(board, simulator.getState(), nextColor);
        }

        Movement bestMovement = null;
        int bestScore = Integer.MIN_VALUE;
        int worstScore = Integer.MAX_VALUE;
        for(Movement possibleMovement : possibleMovements){
            possibleMovement.simulate(simulator, board, depth+1, false);
            if(bestScore < possibleMovement.movementScore){
                bestMovement = possibleMovement;
                bestScore = possibleMovement.movementScore;
            }
            if(possibleMovement.movementScore < worstScore){
                worstScore = possibleMovement.movementScore;
            }
        }
        if( currentColor == playerColor){
            this.movementScore = Math.max(bestScore, this.movementScore);
        }else if(bestScore != Integer.MIN_VALUE) {
            this.movementScore = Math.min(worstScore, this.movementScore);
        }

        if(this.movementType != null)
            reverseSimulation(simulator, prevLocation);
        return bestMovement;
    }

    private int evaluateState(PylosBoard board){
        int ourReserve = board.getReservesSize(this.playerColor);
        int enemyReserve = board.getReservesSize(this.playerColor.other());
        return ourReserve - enemyReserve;
    }

    private ArrayList<Movement> getPossibleMovements(PylosBoard board, PylosGameState state ,PylosPlayerColor color){
        ArrayList<Movement> possibleMovements = new ArrayList<>();

        //Get place locations
        for (PylosLocation loc : board.getLocations()) {
            if (loc.isUsable()) {
                // Possible MOVE movements
                if(loc.Z > 0){
                    PylosSphere[] spheres = board.getSpheres(color);
                    for (PylosSphere pylosSphere : spheres){
                        if(!pylosSphere.isReserve() && pylosSphere.canMoveTo(loc) && pylosSphere.PLAYER_COLOR == color){
                            possibleMovements.add(new Movement(MovementType.MOVE, pylosSphere, loc, color, playerColor, state));
                        }
                    }
                }
                // Possible ADD movements
                possibleMovements.add(new Movement(MovementType.ADD, board.getReserve(color), loc, color, playerColor, state));
            }
        }
        return possibleMovements;
    }

    private void reverseSimulation(PylosGameSimulator simulator, PylosLocation previousLocation){
        switch (movementType){
            case ADD:
                simulator.undoAddSphere(sphere, state, currentColor);
                break;
            case MOVE:
                simulator.undoMoveSphere(sphere, previousLocation, state, currentColor);
                // undo move
                break;
            case YOINK_FIRST:
                simulator.undoRemoveFirstSphere(sphere, previousLocation,state , currentColor);
                // undo yoink
                break;
            case YOINK_SECOND:
                simulator.undoRemoveSecondSphere(sphere, previousLocation, state, currentColor);
                // undo yoink
                break;
            default:
                simulator.undoPass(state, currentColor);
                break;
        }
    }

    private ArrayList<Movement> getPossibleRemovalMovements(PylosBoard board,PylosGameState state , PylosPlayerColor color, MovementType type){
        ArrayList<Movement> possibleYoinks = new ArrayList<>();
        if (type == MovementType.YOINK_SECOND){
            possibleYoinks.add(new Movement(MovementType.PASS, null, null, color, playerColor, state));
        }
        ArrayList<PylosSphere> removableSpheres = getRemovableSpheres(board, color);
        for(PylosSphere sphere : removableSpheres){
            possibleYoinks.add(new Movement(type, sphere, null, color, playerColor, state));
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

    public PylosPlayerColor getCurrentColor() {
        return currentColor;
    }

    public void setCurrentColor(PylosPlayerColor currentColor) {
        this.currentColor = currentColor;
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

