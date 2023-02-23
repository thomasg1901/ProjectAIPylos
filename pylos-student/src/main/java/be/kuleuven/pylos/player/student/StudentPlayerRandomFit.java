package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Ine on 5/05/2015.
 */
public class StudentPlayerRandomFit extends PylosPlayer{

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
		/* add a reserve sphere to a feasible random location */
        PylosSphere selectedSphere = board.getReserve(this);

        ArrayList<PylosLocation> allPossibleLocations = new ArrayList<>();
        for (PylosLocation loc : board.getLocations()) {
            if (loc.isUsable()) {
                allPossibleLocations.add(loc);
            }
        }
        PylosLocation location = allPossibleLocations.size() == 1 ? allPossibleLocations.get(0) : allPossibleLocations.get(getRandom().nextInt(allPossibleLocations.size() - 1));
        if(location.Z > 0){
            PylosSphere[] spheres = board.getSpheres(this);
            for (PylosSphere sphere : spheres){
                if(sphere.canMoveTo(location)){
                    selectedSphere = sphere;
                }
            }
        }
        game.moveSphere(selectedSphere, location);
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
		/* removeSphere a random sphere */
        ArrayList<PylosSphere> removableSpheres = getRemovableSpheres(board);
        PylosSphere sphereToRemove;
        sphereToRemove = removableSpheres.size() == 1 ? removableSpheres.get(0) : removableSpheres.get(getRandom().nextInt(removableSpheres.size() - 1));
        game.removeSphere(sphereToRemove);
    }

    private ArrayList<PylosSphere> getRemovableSpheres(PylosBoard board){
        ArrayList<PylosSphere> removableSpheres = new ArrayList<>();
        for (PylosSphere ps : board.getSpheres(this)) {
            if (!ps.isReserve() && !ps.getLocation().hasAbove()) {
                removableSpheres.add(ps);
            }
        }
        return removableSpheres;
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
		/* always pass */
        game.pass();
    }
}
