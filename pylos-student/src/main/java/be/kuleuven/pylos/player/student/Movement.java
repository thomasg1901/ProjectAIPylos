package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.HashMap;

public class Movement {

    private MovementType movementType;

    private PylosSphere sphere;

    private PylosLocation location;

    private PylosPlayerColor movementColor;
    private  PylosPlayerColor playerColor;

    private PylosGameState state;

    private int movementScore;
    private final int MAX_BOARD_STATE_COUNT = 3;

    private final int MAX_TREE_DEPTH = 6;


    public Movement(MovementType movementType, PylosSphere sphere, PylosLocation location, PylosPlayerColor color, PylosPlayerColor playerColor, PylosGameState state) {
        this.movementType = movementType;
        this.sphere = sphere;
        this.location = location;
        this.movementColor = color;
        this.playerColor = playerColor;
        this.state = state;
    }

    public Movement(PylosPlayerColor color, PylosGameState state){
        this.movementColor = color;
        this.playerColor = color.other();
        this.state = state;
    }

    public Movement simulate(PylosGameSimulator simulator, PylosBoard board, int depth, boolean initialMovement, HashMap<Long, Integer> boardStateCounts, int alpha, int beta){
        if(depth > MAX_TREE_DEPTH){
            this.movementScore = Integer.MIN_VALUE;
            return null;
        }else if (simulator.getState() == PylosGameState.COMPLETED && simulator.getWinner() == this.playerColor){
            this.movementScore = 1000;
            return null;
        } else if (simulator.getState() == PylosGameState.COMPLETED && simulator.getWinner().other() == this.playerColor) {
            this.movementScore = -1000;
            return null;
        }

        PylosLocation prevLocation = sphere == null || sphere.isReserve() ? null : sphere.getLocation();

        if(!initialMovement){
            execute(simulator, board, movementColor);
            this.movementScore = evaluateState(board);

            if (isTieState(boardStateCounts, board)){
                this.movementScore = 0;
                reverseSimulation(simulator, prevLocation, board, boardStateCounts);
                return this;
            }
        }

        if(depth+1 > MAX_TREE_DEPTH){
            reverseSimulation(simulator, prevLocation, board, boardStateCounts);
            return this;
        }
        int nextDepth = depth + 1;
        ArrayList<Movement> possibleMovements = null;
        if(simulator.getState() == PylosGameState.REMOVE_FIRST){
            nextDepth = depth;
            possibleMovements = getPossibleRemovalMovements(board,simulator.getState(), movementColor, MovementType.YOINK_FIRST);
        } else if (simulator.getState() == PylosGameState.REMOVE_SECOND) {
            nextDepth = depth;
            possibleMovements = getPossibleRemovalMovements(board, simulator.getState(), movementColor, MovementType.YOINK_SECOND);
        } else {
            movementColor = movementColor.other();
            possibleMovements = getPossibleMovements(board, simulator.getState(), movementColor);
            if(possibleMovements.isEmpty()){
                reverseSimulation(simulator, prevLocation, board, boardStateCounts);
                return this;
            }

        }

        Movement bestMovement = null;
        if(movementColor == playerColor){
            bestMovement = maxValue(simulator, board, nextDepth, possibleMovements, boardStateCounts, alpha, beta);
        }else{
            bestMovement = minValue(simulator, board, nextDepth, possibleMovements, boardStateCounts, alpha, beta);
        }

        if(this.movementType != null)
            reverseSimulation(simulator, prevLocation, board, boardStateCounts);
        return bestMovement;
    }

    public Movement maxValue(PylosGameSimulator simulator, PylosBoard board, int nextDepth , ArrayList<Movement> possibleMovements, HashMap<Long, Integer> boardStateCounts, int alpha, int beta){
        Movement bestMovement = null;
        if(nextDepth > MAX_TREE_DEPTH)
            return null;
        this.movementScore = Integer.MIN_VALUE;
        for(Movement possibleMovement : possibleMovements){
            possibleMovement.simulate(simulator, board, nextDepth, false, boardStateCounts, alpha, beta);
            if(this.movementScore < possibleMovement.movementScore){
                bestMovement = possibleMovement;
            }
            this.movementScore = Math.max(this.movementScore, possibleMovement.movementScore);
            alpha = Math.max(alpha, possibleMovement.movementScore);
            if (beta <= alpha)
                break;
        }

        return bestMovement;
    }

    public Movement minValue(PylosGameSimulator simulator, PylosBoard board, int nextDepth , ArrayList<Movement> possibleMovements, HashMap<Long, Integer> boardStateCounts, int alpha, int beta){
        Movement bestMovement = null;
        this.movementScore = Integer.MAX_VALUE;
        if(nextDepth > MAX_TREE_DEPTH)
            return null;
        for(Movement possibleMovement : possibleMovements){
            possibleMovement.simulate(simulator, board, nextDepth, false, boardStateCounts, alpha, beta);
            if(this.movementScore > possibleMovement.movementScore){
                bestMovement = possibleMovement;
            }
            this.movementScore = Math.min(this.movementScore, possibleMovement.movementScore);
            beta = Math.min(beta, possibleMovement.movementScore);
            if (beta <= alpha)
                break;
        }

        return bestMovement;
    }

    private boolean isTieState(HashMap<Long, Integer> boardStateCounts, PylosBoard board) {
        long boardState = board.toLong();
        Integer stateCount = boardStateCounts.get(boardState);
        if (stateCount == null) {
            boardStateCounts.put(boardState, 1);
        } else {
            boardStateCounts.put(boardState, ++stateCount);
            if (stateCount == MAX_BOARD_STATE_COUNT) {
                setState(PylosGameState.DRAW);
                return true;
            }
        }
        return false;
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
                if(board.getReservesSize(color) > 0){
                    possibleMovements.add(new Movement(MovementType.ADD, board.getReserve(color), loc, color, playerColor, state));
                }
            }
        }
        return possibleMovements;
    }

    private void reverseSimulation(PylosGameSimulator simulator, PylosLocation previousLocation, PylosBoard board, HashMap<Long, Integer> boardStateCounts){
        boardStateCounts.put(board.toLong(), boardStateCounts.get(board.toLong())-1);
        switch (movementType){
            case ADD:
                simulator.undoAddSphere(sphere, state, movementColor);
                break;
            case MOVE:
                simulator.undoMoveSphere(sphere, previousLocation, state, movementColor);
                // undo move
                break;
            case YOINK_FIRST:
                simulator.undoRemoveFirstSphere(sphere, previousLocation,state , movementColor);
                // undo yoink
                break;
            case YOINK_SECOND:
                simulator.undoRemoveSecondSphere(sphere, previousLocation, state, movementColor);
                // undo yoink
                break;
            default:
                simulator.undoPass(state, movementColor);
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
            try{
                simulator.removeSphere(sphere);
            }catch (Exception e){
                System.out.println("GODVERDOMMEN EH");
            }

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

    public PylosPlayerColor getMovementColor() {
        return movementColor;
    }

    public void setMovementColor(PylosPlayerColor movementColor) {
        this.movementColor = movementColor;
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

