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

package abfab3d.grid.op;

// External Imports

// Internal Imports

import abfab3d.core.AttributeGrid;
import abfab3d.core.Grid;
import abfab3d.grid.*;
import abfab3d.core.MathUtil;

/**
 * Dilate an object based on the dilation morphology technique.  The object should
 * increase in size after dilation.  The dilating element is a sphere.
 *
 * @author Tony Wong
 */
public class DilationSphere implements Operation, AttributeOperation {

    /** The distance from a voxel to dilate */
    private int radius;

    public DilationSphere(int radius) {
        this.radius = radius;
    }

    /**
     * Execute an operation on a grid.  If the operation changes the grid
     * dimensions then a new one will be returned from the call.
     *
     * @param dest The grid to use for grid A.
     * @return The new grid
     */
    public Grid execute(Grid dest) {

        // Nothing to do if distance is 0
        if (radius == 0) {
            return dest;
        }

        int height = dest.getHeight();
        int width = dest.getWidth();
        int depth = dest.getDepth();

        // Create an empty copy of the grid, increased by twice the size of
        // the dilation distance
        Grid dilatedGrid = dest.createEmpty(width + 2 * radius,
                                            height + 2 * radius,
                                            depth + 2 * radius,
                                            dest.getVoxelSize(),
                                            dest.getSliceHeight());

        // Loop through original grid to find filled voxels and apply dilation
        for(int y=0; y < height; y++) {
            for(int x=0; x < width; x++) {
                for(int z=0; z < depth; z++) {
                    byte state = dest.getState(x, y, z);

                    if (state != Grid.OUTSIDE) {
                        dilateVoxel(dilatedGrid, x+radius, y+radius, z+radius, state);
                    }
                }
            }
        }

        return dilatedGrid;
    }

    /**
     * Execute an operation on a grid.  If the operation changes the grid
     * dimensions then a new one will be returned from the call.
     *
     * @param dest The grid to use for grid A.
     * @return The new grid
     */
    public AttributeGrid execute(AttributeGrid dest) {

        // Nothing to do if distance is 0
        if (radius == 0) {
            return dest;
        }

        int height = dest.getHeight();
        int width = dest.getWidth();
        int depth = dest.getDepth();

        // Create an empty copy of the grid, increased by twice the size of
        // the dilation distance
        AttributeGrid dilatedGrid = (AttributeGrid) dest.createEmpty(width + 2 * radius,
                                                                     height + 2 * radius,
                                                                     depth + 2 * radius,
                                                                     dest.getVoxelSize(),
                                                                     dest.getSliceHeight());

        // Loop through original grid to find filled voxels and apply dilation
        for(int y=0; y < height; y++) {
            for(int x=0; x < width; x++) {
                for(int z=0; z < depth; z++) {
                    byte state = dest.getState(x, y, z);

                    if (state != Grid.OUTSIDE) {
                        long mat = dest.getAttribute(x, y, z);
                        dilateVoxel(dilatedGrid, x+radius, y+radius, z+radius, state, mat);
                    }
                }
            }
        }

        return dilatedGrid;
    }

    private void dilateVoxel(AttributeGrid grid, int xPos, int yPos, int zPos, byte state, long mat) {
        int[] origin = {xPos, yPos, zPos};

        int xStart = xPos - radius;
        int xEnd = xPos + radius;
        int yStart = yPos - radius;
        int yEnd = yPos + radius;
        int zStart = zPos - radius;
        int zEnd = zPos + radius;

        for (int y=yStart; y<=yEnd; y++) {
            for (int x=xStart; x<=xEnd; x++) {
                for (int z=zStart; z<=zEnd; z++) {
                    int[] pos = {x, y, z};

                    if (MathUtil.getDistance(origin, pos) <= radius) {
                        grid.setData(x, y, z, state, mat);
                    }
                }
            }
        }
    }

    private void dilateVoxel(Grid grid, int xPos, int yPos, int zPos, byte state) {
        int[] origin = {xPos, yPos, zPos};

        int xStart = xPos - radius;
        int xEnd = xPos + radius;
        int yStart = yPos - radius;
        int yEnd = yPos + radius;
        int zStart = zPos - radius;
        int zEnd = zPos + radius;

        for (int y=yStart; y<=yEnd; y++) {
            for (int x=xStart; x<=xEnd; x++) {
                for (int z=zStart; z<=zEnd; z++) {
                    int[] pos = {x, y, z};

                    if (MathUtil.getDistance(origin, pos) <= radius) {
                        grid.setState(x, y, z, state);
                    }
                }
            }
        }
    }

}
