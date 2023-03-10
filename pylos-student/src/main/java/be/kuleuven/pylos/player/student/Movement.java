package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import com.sun.source.tree.Tree;

import java.util.ArrayList;
import java.util.Map;

public class Movement {

    private MovementType movementType;

    private PylosSphere sphere;

    private PylosLocation location;

    private PylosLocation previousLocation;

    private PylosPlayerColor color;

    private int movementScore;

    private final int MAX_TREE_DEPTH = 2;

    public Movement(MovementType movementType, PylosSphere sphere, PylosLocation location, PylosPlayerColor color) {
        this.movementType = movementType;
        this.sphere = sphere;
        this.location = location;
        this.color = color;
    }

    private void simulate(PylosGameSimulator simulator, PylosBoard board, int depth){
        if(depth > MAX_TREE_DEPTH){
            return;
        }

        execute(simulator, board, color);
        PylosPlayerColor nextColor = color.other();
        ArrayList<Movement> possibleMovementsOtherColor = getPossibleMovements(board, nextColor);


        for(Movement possibleMovement : possibleMovementsOtherColor){
            possibleMovement.simulate(simulator, board, depth+1);
        }
    }

    private ArrayList<Movement> getPossibleMovements(PylosBoard board, PylosPlayerColor color){
        ArrayList<Movement> possibleMovements = new ArrayList<>();
        //Get place locations
        for (PylosLocation loc : board.getLocations()) {
            if (loc.isUsable()) {
                // Possible ADD movements
                possibleMovements.add(new Movement(MovementType.ADD, board.getReserve(color), loc, color));

                // Possible MOVE movements
                if(loc.Z > 0){
                    PylosSphere[] spheres = board.getSpheres(color);
                    for (PylosSphere sphere : spheres){
                        if(sphere.canMoveTo(loc)){
                            possibleMovements.add(new Movement(MovementType.MOVE, board.getReserve(color), loc, color));
                        }
                    }
                }
            }
        }

        return possibleMovements;
    }

    private void reverseSimulation(){

    }

    private ArrayList<Movement> getPossibleRemovalMovements(PylosBoard board, PylosPlayerColor color, MovementType type){
        ArrayList<Movement> possibleYoinks = new ArrayList<>();
        if (type == MovementType.YOINK_SECOND){
            possibleYoinks.add(new Movement(MovementType.PASS, null, null, color));
        }
        ArrayList<PylosSphere> removableSpheres = getRemovableSpheres(board, color);
        for(PylosSphere sphere : removableSpheres){
            possibleYoinks.add(new Movement(type, sphere, null, color));
        }
        return possibleYoinks;
    }

    private void execute(PylosGameSimulator simulator, PylosBoard board, PylosPlayerColor color){
        previousLocation = location;
        if(movementType == MovementType.ADD || movementType == MovementType.MOVE) {
            simulator.moveSphere(sphere, location);

            PylosSquare[] squares = board.getAllSquares();
            boolean completedSquare = false;
            for (PylosSquare bsInSquare : squares) {
                completedSquare |= bsInSquare.isSquare(color);
            }
            if(completedSquare){
                // Do removal
            }
        } else if (movementType == MovementType.YOINK_FIRST || movementType == MovementType.YOINK_SECOND) {
            simulator.removeSphere(sphere);
        }
        location = sphere.getLocation();
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

