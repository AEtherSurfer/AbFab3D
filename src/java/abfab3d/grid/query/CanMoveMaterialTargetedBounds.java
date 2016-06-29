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

package abfab3d.grid.query;

// External Imports

// Internal Imports
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import abfab3d.core.AttributeGrid;
import abfab3d.core.ClassAttributeTraverser;
import abfab3d.core.Grid;
import abfab3d.core.VoxelData;
import abfab3d.grid.*;
import abfab3d.path.Path;

/**
 * Determines whether an object specified by a materialID can move in the
 * direction specified.  Only cares about intersections with
 * targeted material.
 *
 * Might be able to optimize by moving the axis most version of
 * of something and then not having to calculate the rest in that
 * axis.  Might only work for axis aligned?  cast ray along negative
 * axis and say all those are ok if connected.

 *
 *
 * @author Alan Hudson
 */
public class CanMoveMaterialTargetedBounds implements ClassAttributeTraverser {
    private static final boolean CONCURRENT = true;
    private static final boolean DEBUG = false;

    /** The material to remove */
    private long material;

    /** The target material */
    private long target;

    /** Minimum bounds of the target */
    private int[] targetMinBounds;

    /** Maximum bounds of the target */
    private int[] targetMaxBounds;

    /** The path to use */
    private Path path;

    /** Did all the voxels escape */
    private boolean allEscaped;

    /** The gridAtt we are using */
    private AttributeGrid gridAtt;

    /** Coordinates that can be ignored */
    private Set<VoxelCoordinate> ignoreSet;

    // Stats, only collected when DEBUG is true
    private long calcs;

    // scratch var
    private int[] pos = new int[3];
    private VoxelCoordinate vc = new VoxelCoordinate();
    private VoxelData vd;

    public CanMoveMaterialTargetedBounds(long material, long target,
            int[] targetMinBounds, int[] targetMaxBounds, Path path) {
        this.material = material;
        this.target = target;
        this.targetMinBounds = targetMinBounds.clone();
        this.targetMaxBounds = targetMaxBounds.clone();
        this.path = path;
    }

    /**
     * Can the specified material move along the path
     * to exit the voxel space.  Any intersection with
     * another materialID will cause failure.
     *
     * @param grid The gridAtt to use for gridAtt src
     * @return true if it can move to an exit.
     */
    public boolean execute(Grid grid) {
        allEscaped = true;
        this.gridAtt = (AttributeGrid) grid;
        vd = grid.getVoxelData();

        if (CONCURRENT) {
            ignoreSet = Collections.newSetFromMap(new ConcurrentHashMap<VoxelCoordinate, Boolean>());
        } else {
            ignoreSet = new HashSet<VoxelCoordinate>();
        }

        // TODO: just use material and say class only moves external?
//        gridAtt.findInterruptible(VoxelClasses.INSIDE, material, this);

        int skip = 1; // 100

        if (skip > 1 && grid instanceof MaterialIndexedWrapper) {
            //System.out.println("Sampled Route1");
            ((MaterialIndexedWrapper)gridAtt).findAttributeInterruptibleSampled(material, skip, this);
        } else {
            // New way is it faster?
        gridAtt.findAttributeInterruptible(material, this);
/*
            System.out.println("Grid type1: " + grid);
            VoxelData vd = grid.getVoxelData();
            int height = grid.getHeight();
            int width = grid.getWidth();
            int depth = grid.getDepth();

            loop:
            for (int y = 0; y < height; y = y + 2) {
                for (int x = 0; x < width; x = x + 2) {
                    for (int z = 0; z < depth; z = z + 2) {
                        grid.getData(x, y, z, vd);

                        if (vd.getMaterial() == material && vd.getState() != Grid.OUTSIDE) {
                            if (!this.foundInterruptible(x, y, z, vd))
                                break loop;
                        }
                    }
                }
            }
        */
        }
        if (DEBUG) {
            if (DEBUG) {
//                System.out.println("CMMTB: " + material + " t: " + target + " min: " + Arrays.toString(targetMinBounds) + " max: " + Arrays.toString(targetMaxBounds) + " path: " + Arrays.toString(path.getDir()));
                System.out.println("CMMTB: " + material + " t: " + target + " min: " + Arrays.toString(targetMinBounds) + " max: " + Arrays.toString(targetMaxBounds) + " path: " + Arrays.toString(path.getDir()) + " calcs: " + calcs + " ignored: " + ignoreSet.size());
            }
        }

        return allEscaped;
    }

    /**
     * A voxel of the class requested has been found.
     * VoxelData classes may be reused so clone the object
     * if you keep a copy.
     *
     * @param x The x gridAtt coordinate
     * @param y The y gridAtt coordinate
     * @param z The z gridAtt coordinate
     * @param start The voxel data
     */
    public void found(int x, int y, int z, VoxelData start) {
        // ignore
    }

    /**
     * A voxel of the class requested has been found.
     * VoxelData classes may be reused so clone the object
     * if you keep a copy.
     *
     * @param x The x gridAtt coordinate
     * @param y The y gridAtt coordinate
     * @param z The z gridAtt coordinate
     * @param start The voxel data
     */
    public boolean foundInterruptible(int x, int y, int z, VoxelData start) {

        // TODO: would skipping odd voxels really change the answer?
/*
        if (x % 2 == 0 || y % 2 == 0 || z % 2 == 0) {
            return true;
        }
*/
        // All should be exterior voxels.

//System.out.println("Move voxel: " + x + " " + y + " " + z);
/*
// TODO: removed to test for MT
        if (canIgnore(x,y,z)) {
            return true;
        }
*/
        pos[0] = x;
        pos[1] = y;
        pos[2] = z;

        // Move along path till edge or
        path.init(pos, gridAtt.getWidth(), gridAtt.getHeight(), gridAtt.getDepth());

        boolean escaped = true;

        while(path.next(pos)) {
//System.out.println("checking: " + java.util.Arrays.toString(pos));
            // If position is pass target bounds, material can escape
            if (isPassTargetBounds(pos)) {
//System.out.println("Pass target bounds at: " + java.util.Arrays.toString(pos));
                break;
            }

            gridAtt.getData(pos[0], pos[1], pos[2],vd);

            if (DEBUG) {
                calcs++;
            }
//System.out.println(java.util.Arrays.toString(pos) + ": " + vd.getState() + "  " + vd.getAttribute());
            if (vd.getState() != Grid.OUTSIDE && vd.getMaterial() == target) {
//System.out.println("Collide at: " + java.util.Arrays.toString(pos));
                // found another materials voxel
                escaped = false;
                break;
            }
        }

        if (!escaped) {
            allEscaped = false;
            return false;
        }

        // TODO: removed to test for MT
        /*
        // walk along negative path, stop at first outside
        addIgnoredVoxels(x, y, z);
        */
        return true;
    }

    /**
     * Checks whether a position is pass the bounds of the target bounds.
     *
     * @param pos The position to check
     * @return True if the position is pass the bounds of the target bounds
     */
    private boolean isPassTargetBounds(int[] pos) {

        if (pos[0] > targetMaxBounds[0] && path.getDir()[0] >= 0) {
//System.out.println("pos[0] > targetMaxBounds[0] && path.getDir()[0] >= 0");
            return true;
        }

        if (pos[0] < targetMinBounds[0] && path.getDir()[0] <= 0) {
//System.out.println("pos[0] < targetMinBounds[0] && path.getDir()[0] <= 0");
            return true;
        }

        if (pos[1] > targetMaxBounds[1] && path.getDir()[1] >= 0) {
//System.out.println("pos[1] > targetMaxBounds[1] && path.getDir()[1] >= 0");
            return true;
        }

        if (pos[1] < targetMinBounds[1] && path.getDir()[1] <= 0) {
//System.out.println("pos[1] > targetMinBounds[1] && path.getDir()[1] <= 0");
            return true;
        }

        if (pos[2] > targetMaxBounds[2] && path.getDir()[2] >= 0) {
//System.out.println("pos[2] > targetMaxBounds[2] && path.getDir()[2] >= 0");
            return true;
        }

        if (pos[2] < targetMinBounds[2] && path.getDir()[2] <= 0) {
//System.out.println("pos[2] > targetMinBounds[2] && path.getDir()[2] <= 0");
            return true;
        }

        return false;
    }

    /**
     * Checks whether a position is pass the bounds of the target bounds.
     * TODO: This method does not work for all cases!!!
     *
     * @param pos The position to check
     * @return True if the position is pass the bounds of the target bounds
     */
/*    private boolean isPassTargetBounds2(int[] pos) {
        // When the direction of the path is positive or negative x
        if (path.getDir()[0] == 1) {
            if (pos[0] > targetMaxBounds[0] ||
                isOutsideTargetHeightBounds(pos) ||
                isOutsideTargetDepthBounds(pos)) {

                return true;
            }
        } else if (path.getDir()[0] == -1) {
            if (pos[0] < targetMinBounds[0] ||
                isOutsideTargetHeightBounds(pos) ||
                isOutsideTargetDepthBounds(pos)) {

                return true;
            }
        }

        // When the direction of the path is positive or negative y
        if (path.getDir()[1] == 1) {
            if (pos[1] > targetMaxBounds[1] ||
                isOutsideTargetWidthBounds(pos) ||
                isOutsideTargetDepthBounds(pos)) {

                return true;
            }
        } else if (path.getDir()[1] == -1) {
            if (pos[1] < targetMinBounds[1] ||
                isOutsideTargetWidthBounds(pos) ||
                isOutsideTargetDepthBounds(pos)) {

                return true;
            }
        }

        // When the direction of the path is positive or negative z
        if (path.getDir()[2] == 1) {
            if (pos[2] > targetMaxBounds[2] ||
                isOutsideTargetWidthBounds(pos) ||
                isOutsideTargetHeightBounds(pos)) {

                return true;
            }
        } else if (path.getDir()[2] == -1) {
            if (pos[2] < targetMinBounds[2] ||
                isOutsideTargetWidthBounds(pos) ||
                isOutsideTargetHeightBounds(pos)) {

                return true;
            }
        }

        return false;
    }
*/
    /**
     * Checks whether a position is outside the targets width bounds.
     *
     * @param pos The position to check
     * @return True if the position is outside the targets width bounds
     */
/*    private boolean isOutsideTargetWidthBounds(int[] pos) {
        if (pos[0] > targetMaxBounds[0] || pos[0] < targetMinBounds[0]) {
            return true;
        }

        return false;
    }
*/
    /**
     * Checks whether a position is outside the targets height bounds.
     *
     * @param pos The position to check
     * @return True if the position is outside the targets height bounds
     */
/*    private boolean isOutsideTargetHeightBounds(int[] pos) {
        if (pos[1] > targetMaxBounds[1] || pos[1] < targetMinBounds[1]) {
            return true;
        }

        return false;
    }
*/
    /**
     * Checks whether a position is outside the targets depth bounds.
     *
     * @param pos The position to check
     * @return True if the position is outside the targets depth bounds
     */
/*    private boolean isOutsideTargetDepthBounds(int[] pos) {
        if (pos[2] > targetMaxBounds[2] || pos[2] < targetMinBounds[2]) {
            return true;
        }

        return false;
    }
*/

    /**
     * Get the count of the ignored voxels.
     *
     * @return Count of the ignored voxels.
     */
    public int getIgnoredCount() {
        return ignoreSet.size();
    }

    /**
     * Add voxels to be ignored for a given path as specified by ignoreSetIndex.
     *
     * @param x The X coordinate for the starting position
     * @param y The Y coordinate for the starting position
     * @param z The Z coordinate for the starting position
     */
    private void addIgnoredVoxels(int x, int y, int z) {
        pos[0] = x;
        pos[1] = y;
        pos[2] = z;

        Path invertedPath = path.invertPath();
        invertedPath.init(pos, gridAtt.getWidth(), gridAtt.getHeight(), gridAtt.getDepth());

        while(invertedPath.next(pos)) {
            byte state = gridAtt.getState(pos[0], pos[1], pos[2]);

            // can optimize by ignoring interior voxels and only checking for exterior voxels
            if (state == Grid.OUTSIDE)
                break;

            if (state == Grid.INSIDE) {
//System.out.println("placing in ignore list: " + pos[0] + " " + pos[1] + " " + pos[2]);
                ignoreSet.add(new VoxelCoordinate(pos[0], pos[1], pos[2]));
            }
        }
    }

    /**
     * Checks if a voxel can be ignored.
     *
     * @param x The X coordinate of the voxel to check
     * @param y The Y coordinate of the voxel to check
     * @param z The Z coordinate of the voxel to check
     * @return True if the voxel can be ignored.
     */
    private boolean canIgnore(int x, int y, int z) {
        vc.setValue(x,y,z);

        if (ignoreSet.contains(vc)) {
//System.out.println("can ignore: " + x + " " + y + " " + z);
            return true;
        }

        return false;
    }

}
