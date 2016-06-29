/*****************************************************************************
 *                        Shapeways, Inc Copyright (c) 2011
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package abfab3d.geom;

// External Imports

import abfab3d.core.AttributeGrid;
import abfab3d.grid.DualWrapper;

// Internal Imports
import abfab3d.core.Grid;

/**
 * Creates a Torus.
 *
 * Uses the implicit function of a torus to calculate exterior/interrior/outside state.
 *
 * TODO: EPS function might need to change based on grid resolution.  Not sure.
 *
 * @author Alan Hudson
 */
public class TorusCreator extends GeometryCreator {
    private static final double EPS = 0.000002;

    protected double ir;
    protected double or;

    // The center position
    protected double x0;
    protected double y0;
    protected double z0;

    // Rotation to apply
    protected double rx;
    protected double ry;
    protected double rz;
    protected double rangle;

    protected int outerMaterial;
    protected int innerMaterial;

    boolean swapYZ = false;

    /**
     * Constructor.
     *
     */
    public TorusCreator(
        double ir, double or, int imat, int omat) {

        this(ir,or,0,0,0,0,1,0,0,imat,omat);
    }

    /**
     * Constructor.
     *
     */
    public TorusCreator(
        double ir, double or,
        double x, double y, double z,
        double rx, double ry, double rz, double ra,
        int imat, int omat) {

        this.ir = ir;
        this.or = or;
        this.x0 = x;
        this.y0 = y;
        this.z0 = z;
        this.rx = rx;
        this.ry = ry;
        this.rz = rz;
        this.rangle = ra;
        outerMaterial = omat;
        innerMaterial = imat;
    }

    /**
     * Generate the geometry and issue commands to the provided handler.
     *
     * @param grid The stream to issue commands
     */
    public void generate(Grid grid) {
        AttributeGrid wrapper = null;

        if (grid instanceof AttributeGrid) {
            wrapper = (AttributeGrid) grid;
        } else {
            wrapper = new DualWrapper(grid);
        }
        
        // Wall all grid points and check implicit equations points = 0
        // f(x,y,z) = (or - sqrt(x^2 + y^2)) ^ 2 + z^2 - ir^2

        // TODO: can we detect surface points?

        int w = wrapper.getWidth();
        int h = wrapper.getHeight();
        int d = wrapper.getDepth();

        double[] wcoords = new double[3];
        double fval;
        double tmp;

        for(int i=0; i < w; i++) {
            for(int j=0; j < h; j++) {
                for(int k=0; k < d; k++) {
                    wrapper.getWorldCoords(i,j,k,wcoords);

                    if (surface(wcoords[0],wcoords[1],wcoords[2])) {
                        wrapper.setData(i,j,k,Grid.INSIDE, outerMaterial);
                    } else if (inside(wcoords[0],wcoords[1],wcoords[2])) {
                        wrapper.setData(i,j,k,Grid.INSIDE, innerMaterial);
                    }
                }
            }
        }
    }

    private boolean inside(double x,double y, double z) {
        double tmp = or - Math.sqrt((x - x0) * (x - x0) + (y - y0) * (y - y0));
        double fval = tmp * tmp + (z - z0) * (z - z0) - ir * ir;

        if (fval <= 0) {
            return true;
        }

        return false;
    }

    private boolean surface(double x, double y, double z) {
        double tmp = or - Math.sqrt((x - x0) * (x - x0) + (y - y0) * (y - y0));
        double fval = tmp * tmp + (z - z0) * (z - z0) - ir * ir;

        if (Math.abs(fval) < EPS) {
            return true;
        }

        return false;
    }
}
