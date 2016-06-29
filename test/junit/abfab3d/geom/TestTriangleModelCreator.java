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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import abfab3d.core.AttributeGrid;
import abfab3d.core.Grid;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.j3d.geom.GeometryData;
import org.j3d.geom.BoxGenerator;
import org.j3d.geom.CylinderGenerator;
import org.j3d.geom.TorusGenerator;
import org.web3d.util.ErrorReporter;
import org.web3d.vrml.export.PlainTextErrorReporter;

// Internal Imports
import abfab3d.grid.*;
import abfab3d.core.VoxelClasses;
import abfab3d.io.output.BoxesX3DExporter;

/**
 * Tests the functionality of TriangleModelCreator.
 *
 * @author Tony Wong
 * @version
 */
public class TestTriangleModelCreator extends TestCase {

    /** Horizontal resolution of the printer in meters.  */
    public static final double HORIZ_RESOLUTION = 0.004;

    /** Vertical resolution of the printer in meters.  */
    public static final double VERT_RESOLUTION = 0.004;

    /**
     * Creates a test suite consisting of all the methods that start with "test".
     */
    public static Test suite() {
        return new TestSuite(TestTriangleModelCreator.class);
    }

    /**
     * Test the voxelization of a simple triangle cube.
     */
    public void testCube() {

        // Use 0.0999 instead of 0.1 voxelization is unpredictable when model
        // lines up exactly with a grid
        float width = 0.0999f;
        float height = 0.0999f;
        float depth = 0.0999f;
        byte outerMaterial = 1;
        byte innerMaterial = 2;

        // set the grid size with a slight over allocation
        int gWidth = (int) (width / HORIZ_RESOLUTION) + 3;
        int gHeight = (int) (height / VERT_RESOLUTION) + 3;
        int gDepth = (int) (depth / HORIZ_RESOLUTION) + 3;

        AttributeGrid grid = new ArrayAttributeGridByte(gWidth, gHeight, gDepth, HORIZ_RESOLUTION, VERT_RESOLUTION);

        // translate the cube so it does not occupy one more row, height, and depth than it should
        // by having the "left" side start on a grid line
        int indexOffset = 1;
        double translateX = indexOffset * HORIZ_RESOLUTION;
        double translateY = indexOffset * VERT_RESOLUTION;
        double translateZ = indexOffset * HORIZ_RESOLUTION;

//System.out.println("test translate: " + Math.abs(Math.round((width / 2.0f) * 1000f)) / 1000f);
//System.out.println("translateX: " + translateX);

        //------------------------------------------------------
        // Test a cube with interior voxels filled
        //------------------------------------------------------

        createCubeInGrid(grid, width, height, depth,
                translateX, translateY, translateZ,
                innerMaterial,
                GeometryData.TRIANGLES, true);

//        generate(grid, "cube_IFVB.x3db");
//System.out.println("grid dimensions: " + grid.getWidth() + " " + grid.getHeight() + " " + grid.getDepth());

        int xVoxels = (int) Math.round(width / HORIZ_RESOLUTION);
        int yVoxels = (int) Math.round(height / VERT_RESOLUTION);
        int zVoxels = (int) Math.round(depth / HORIZ_RESOLUTION);

        int expectedIntCount = getCubeInteriorVoxelCount(xVoxels, yVoxels, zVoxels);

        // check exterior and interior voxel counts
        assertEquals("Inside count is not " + expectedIntCount,
                expectedIntCount,
                grid.findCount(VoxelClasses.INSIDE));

        assertEquals("Inner material count is not " + expectedIntCount,
                expectedIntCount,
                grid.findCount(innerMaterial));

        // check that the state and material is correct for each filled grid coordinate
        int cubeStartIndex = indexOffset;
        checkCubeVoxelStates(grid, innerMaterial,
                cubeStartIndex, cubeStartIndex, cubeStartIndex, xVoxels, yVoxels, zVoxels);

        //------------------------------------------------------
        // Test an exterior only cube
        //------------------------------------------------------

        grid = new ArrayAttributeGridByte(gWidth, gHeight, gDepth, HORIZ_RESOLUTION, VERT_RESOLUTION);

        createCubeInGrid(grid, width, height, depth,
                translateX, translateY, translateZ,
                innerMaterial,
                GeometryData.TRIANGLES, false);

        // check exterior and interior voxel counts
        assertTrue("Exterior count is not < full",
                grid.findCount(VoxelClasses.INSIDE) < expectedIntCount
                );

        // check outer and inner material counts
        assertTrue("Inner material count is not " + expectedIntCount,
                grid.findCount(innerMaterial) < expectedIntCount);

    }

    /**
     * Test the voxelization of a simple indexed triangle cube.
     */
    public void testCubeIndexed() {

        // Use 0.0999 instead of 0.1 voxelization is unpredictable when model
        // lines up exactly with a grid
        float width = 0.0999f;
        float height = 0.0999f;
        float depth = 0.0999f;
        byte outerMaterial = 1;
        byte innerMaterial = 2;

        // set the grid size with a slight over allocation
        int gWidth = (int) (width / HORIZ_RESOLUTION) + 3;
        int gHeight = (int) (height / VERT_RESOLUTION) + 3;
        int gDepth = (int) (depth / HORIZ_RESOLUTION) + 3;

        AttributeGrid grid = new ArrayAttributeGridByte(gWidth, gHeight, gDepth, HORIZ_RESOLUTION, VERT_RESOLUTION);

        // translate the cube so it does not occupy one more row, height, and depth than it should
        // by having the "left" side start on a grid line
        int indexOffset = 1;
        double translateX = indexOffset * HORIZ_RESOLUTION;
        double translateY = indexOffset * VERT_RESOLUTION;
        double translateZ = indexOffset * HORIZ_RESOLUTION;

//System.out.println("test translate: " + Math.abs(Math.round((width / 2.0f) * 1000f)) / 1000f);
//System.out.println("translateX: " + translateX);

        //------------------------------------------------------
        // Test a cube with interior voxels filled
        //------------------------------------------------------

        createCubeInGrid(grid, width, height, depth,
                translateX, translateY, translateZ,
                innerMaterial,
                GeometryData.INDEXED_TRIANGLES, true);
//        generate(grid, "cube_IFVB.x3db");
//System.out.println("grid dimensions: " + grid.getWidth() + " " + grid.getHeight() + " " + grid.getDepth());

        int xVoxels = (int) Math.round(width / HORIZ_RESOLUTION);
        int yVoxels = (int) Math.round(height / VERT_RESOLUTION);
        int zVoxels = (int) Math.round(depth / HORIZ_RESOLUTION);

        int expectedIntCount = getCubeInteriorVoxelCount(xVoxels, yVoxels, zVoxels);

        assertEquals("Inside count is not " + expectedIntCount,
                expectedIntCount,
                grid.findCount(VoxelClasses.INSIDE));

        // check outer and inner material counts
        assertEquals("Inner material count is not " + expectedIntCount,
                expectedIntCount,
                grid.findCount(innerMaterial));

        // check that the state and material is correct for each filled grid coordinate
        int cubeStartIndex = indexOffset;
        checkCubeVoxelStates(grid, innerMaterial,
                cubeStartIndex, cubeStartIndex, cubeStartIndex, xVoxels, yVoxels, zVoxels);

        //------------------------------------------------------
        // Test an exterior only cube
        //------------------------------------------------------

        grid = new ArrayAttributeGridByte(gWidth, gHeight, gDepth, HORIZ_RESOLUTION, VERT_RESOLUTION);

        createCubeInGrid(grid, width, height, depth,
                translateX, translateY, translateZ,
                innerMaterial,
                GeometryData.INDEXED_TRIANGLES, false);

        // check exterior and interior voxel counts
        assertTrue("Exterior count is not ",
                grid.findCount(VoxelClasses.INSIDE) < expectedIntCount
                );

        assertTrue("Inner material count is not ",
                grid.findCount(innerMaterial) < expectedIntCount);
    }

    /**
     * Test that two cubes with different material.
     */
    public void testMaterialofTwoShapes() {

        float width = 0.0399f;
        float height = 0.0399f;
        float depth = 0.0399f;
        byte outerMaterial1 = 1;
        byte innerMaterial1 = 2;
        byte outerMaterial2 = 5;
        byte innerMaterial2 = 6;

        // set the grid size for two cubes with a slight over allocation
        int gWidth = (int) Math.round((width / HORIZ_RESOLUTION)) * 3;
        int gHeight = (int) (height / VERT_RESOLUTION) + 3;
        int gDepth = (int) (depth / HORIZ_RESOLUTION) + 3;

        AttributeGrid grid = new ArrayAttributeGridByte(gWidth, gHeight, gDepth, HORIZ_RESOLUTION, VERT_RESOLUTION);
        grid = new RangeCheckAttributeWrapper(grid);

//System.out.println("grid dimensions: " + grid.getWidth() + " " + grid.getHeight() + " " + grid.getDepth());

        //-------------------------------------------------------
        // set up and add the first cube
        //-------------------------------------------------------

        // translate the first cube so it does not occupy one more row, height, and depth than it should
        // by having the "left" side start on a grid line
//        int indexOffset = 5;
        int xVoxels1 = (int) Math.round(width / HORIZ_RESOLUTION);
        int yVoxels1 = (int) Math.round(height / VERT_RESOLUTION);
        int zVoxels1 = (int) Math.round(depth / HORIZ_RESOLUTION);

//      double translateX = ((double)xVoxels1 / 2.0 + indexOffset) * HORIZ_RESOLUTION;
//        double translateY = ((double)yVoxels1 / 2.0 + indexOffset) * VERT_RESOLUTION;
//        double translateZ = ((double)zVoxels1 / 2.0 + indexOffset) * HORIZ_RESOLUTION;

        int indexOffset = 1;
        double translateX = indexOffset * HORIZ_RESOLUTION;
        double translateY = indexOffset * VERT_RESOLUTION;
        double translateZ = indexOffset * HORIZ_RESOLUTION;

//System.out.println("translateX: " + translateX);
//System.out.println("translateY: " + translateY);
//System.out.println("translateZ: " + translateZ);

        createCubeInGrid(grid, width, height, depth,
                translateX, translateY, translateZ,
                innerMaterial1,
                GeometryData.TRIANGLES, true);

//        generate(grid, "cube_IFVB.x3db");
//      int expectedMat1Count = xVoxels1 * yVoxels1 * zVoxels1;

//System.out.println("mat 1 count: " + grid.findCount(innerMaterial1));
//        assertEquals("Material 1 count before adding second cube is not " + expectedMat1Count,
//              expectedMat1Count, grid.findCount(innerMaterial1));

        //-------------------------------------------------------
        // set up and add the second cube of different size
        //-------------------------------------------------------

        width = 0.0199f;
        height = 0.0199f;
        depth = 0.0199f;

        int xVoxels2 = (int) Math.round(width / HORIZ_RESOLUTION);
        int yVoxels2 = (int) Math.round(height / VERT_RESOLUTION);
        int zVoxels2 = (int) Math.round(depth / HORIZ_RESOLUTION);
        int xStartIndex2 = xVoxels1 + 2 * indexOffset;

//      translateX = (xStartIndex2 + xVoxels2 / 2.0) * HORIZ_RESOLUTION;
//        translateY = ((double)yVoxels2 / 2.0 + indexOffset) * VERT_RESOLUTION;
//        translateZ = ((double)zVoxels2 / 2.0 + indexOffset) * HORIZ_RESOLUTION;

        translateX = xStartIndex2 * HORIZ_RESOLUTION;
        translateY = indexOffset * VERT_RESOLUTION;
        translateZ = indexOffset * HORIZ_RESOLUTION;

//System.out.println("translateX: " + translateX);
//System.out.println("translateY: " + translateY);
//System.out.println("translateZ: " + translateZ);

        createCubeInGrid(grid, width, height, depth,
                translateX, translateY, translateZ,
                innerMaterial2,
                GeometryData.TRIANGLES, true);

//        generate(grid, "cube_IFVB2.x3db");

        int expectedIntCount1 = getCubeInteriorVoxelCount(xVoxels1, yVoxels1, zVoxels1);
        int expectedIntCount2 = getCubeInteriorVoxelCount(xVoxels2, yVoxels2, zVoxels2);

        // check exterior and interior voxel counts
        assertEquals("Interior count for both cubes is not " + expectedIntCount1 + expectedIntCount2,
                expectedIntCount1 + expectedIntCount2,
                grid.findCount(VoxelClasses.INSIDE));

        // check outer and inner material counts for cube 1
        assertEquals("Inner material count for cube 1 is not " + expectedIntCount1,
                expectedIntCount1,
                grid.findCount(innerMaterial1));

        assertEquals("Inner material count for cube 2 is not " + expectedIntCount2,
                expectedIntCount2,
                grid.findCount(innerMaterial2));

        // check that the state and material is correct for each filled grid coordinate of cubes
        checkCubeOnlyVoxelStates(grid, innerMaterial1,
                indexOffset, indexOffset, indexOffset, xVoxels1, yVoxels1, zVoxels1);

//System.out.println("xStartIndex2: " + xStartIndex2);
        checkCubeOnlyVoxelStates(grid, innerMaterial2,
                xStartIndex2, indexOffset, indexOffset, xVoxels2, yVoxels2, zVoxels2);

    }

    /**
     * Test the voxelization of a simple triangle cylinder.
     */
    public void testCylinder() {
        // Use 0.0999 instead of 0.1 voxelization is unpredictable when model
        // lines up exactly with a grid
        float radius = 0.0499f;
        float height = 0.0999f;
        byte outerMaterial = 1;
        byte innerMaterial = 2;

//      double translateX = Math.round(radius * 1000f) / 1000f;
//      double translateY = Math.round((height / 2.0f) * 1000f) / 1000f;
//      double translateZ = Math.round(radius * 1000f) / 1000f;
        int indexOffset = 1;
        double translateX = indexOffset * HORIZ_RESOLUTION;
        double translateY = indexOffset * VERT_RESOLUTION;
        double translateZ = indexOffset * HORIZ_RESOLUTION;

        AttributeGrid grid = createCylinderInGrid(height, radius,
                translateX, translateY, translateZ,
                outerMaterial, innerMaterial, GeometryData.TRIANGLES);

//        generate(grid, "cylinder_IFVB.x3db");
//System.out.println("grid dimensions: " + grid.getWidth() + " " + grid.getHeight() + " " + grid.getDepth());

//      int radiusInVoxels = (int) Math.round(radius / HORIZ_RESOLUTION);
//      int areaInVoxels = (int) Math.round(Math.PI * (double) (radiusInVoxels * radiusInVoxels));
//      int heightInVoxels = (int) Math.round(height / VERT_RESOLUTION);
//      int expectedMatCount = areaInVoxels * heightInVoxels;
//System.out.println("radiusInVoxels: " + radiusInVoxels);
//System.out.println("areaInVoxels: " + areaInVoxels);
//System.out.println("expectedMatCount: " + expectedMatCount);

        checkCylinder(grid, height, radius, outerMaterial, innerMaterial);

    }

    /**
     * Test that the correct voxels are set to interior.
     *
     * Create a torus, and place a cube on both side to verify the validity of
     * InteriorFinderVoxelBased.
     *
     * TODO: As of 10/07/11, InteriorFinderVoxelBased fails in assigning interior
     * voxels for this case. There are extra interior voxels where ther shouldn't be.
     */
/*    public void testInteriorFinder() {
        double ir = 0.004f;
        double or = 0.015f;
        int facets = 64;
        byte outerMaterial = 1;
        byte innerMaterial = 2;

        TorusGenerator tg = new TorusGenerator((float)ir, (float)or, facets, facets);
        GeometryData geom = new GeometryData();
        geom.geometryType = GeometryData.TRIANGLES;
        tg.generate(geom);

        double bounds = TriangleModelCreator.findMaxBounds(geom);
//        double size = 2.1 * bounds;  // Slightly over allocate

        double voxelSize = 0.001;
        int gridSize = (int) (2 * bounds / voxelSize) + 30; // add 2 voxels as buffer

        AttributeGrid grid = new ArrayAttributeGridByte(gridSize, gridSize, gridSize, voxelSize, voxelSize);

        int indexOffset = 5;
        double translateX = Math.round(bounds * 1000f) / 1000f + indexOffset * voxelSize;
        double translateY = Math.round(bounds * 1000f) / 1000f + indexOffset * voxelSize;
        double translateZ = Math.round(bounds * 1000f) / 1000f + indexOffset * voxelSize;

        double rx = 0,ry = 1,rz = 0,rangle = 0;
//      double rx = 1,ry = 0,rz = 0,rangle = 1.57079633;

        TriangleModelCreator tmc = null;
        tmc = new TriangleModelCreator(geom, translateX, translateY, translateZ,
              rx,ry,rz,rangle,outerMaterial,innerMaterial,true);

        tmc.generate(grid);

                // Use 0.0999 instead of 0.1 voxelization is unpredictable when model
        // lines up exactly with a grid
        float width = 0.0299f;
        float height = 0.0099f;
        float depth = 0.0299f;

        // translate the cube so it does not occupy one more row, height, and depth than it should
        // by having the "left" side start on a grid line
        double cTranslateX = translateX;
        double cTranslateY = translateY + 15 * voxelSize;
        double cTranslateZ = translateZ;

        createCubeInGrid(grid, width, height, depth, cTranslateX, cTranslateY, cTranslateZ,
                outerMaterial, innerMaterial,  GeometryData.TRIANGLES, true);

        cTranslateY = translateY - 15 * voxelSize;

        createCubeInGrid(grid, width, height, depth, cTranslateX, cTranslateY, cTranslateZ,
                outerMaterial, innerMaterial,  GeometryData.TRIANGLES, true);

        generate(grid, "interiorFinderTMCTest.x3db");
    }
*/
    /**
     * Test the voxelization of a simple triangle cylinder.
     */
/*    public void testTorus() {

        float ir = 0.04f;
        float or = 0.16f;
        int facets = 64;
        byte outerMaterial = 1;
        byte innerMaterial = 2;

        AttributeGrid grid = createTorusInGrid(ir, or, facets, outerMaterial, innerMaterial,
                GeometryData.TRIANGLES, true);

        printGridStates(grid);
        checkVoxelStatePattern(grid);

    }
*/
    /**
     * Creates a torus in a grid and returns the grid.
     *
     * @param ir The inside radius of the torus
     * @param or The outside radius of the grid
     * @param facets The tessellation accuracy
     * @param innerMaterial The inner material
     * @param geomType The geometry type to use
     * @param fill Should the interior be filled or just a shell
     * @return The grid containing the cube
     */
    private static AttributeGrid createTorusInGrid(float ir, float or, int facets,
            byte innerMaterial, int geomType, boolean fill) {

        TorusGenerator tg = new TorusGenerator(ir, or, facets, facets);
        GeometryData geom = new GeometryData();
        geom.geometryType = geomType;
        tg.generate(geom);

        double bounds = TriangleModelCreator.findMaxBounds(geom);
//System.out.println("geometry bounds: " + bounds);

        int bufferVoxel = 4;
        int size = (int) (2.0 * bounds / HORIZ_RESOLUTION) + bufferVoxel;
//System.out.println("grid voxels per side: " + size);

        AttributeGrid grid = new ArrayAttributeGridByte(size, size, size, HORIZ_RESOLUTION, VERT_RESOLUTION);

        double x = bounds + bufferVoxel/2 * HORIZ_RESOLUTION;
        double y = x;
        double z = x;

        double rx = 0,ry = 1,rz = 0,rangle = 0;
//        double rx = 1,ry = 0,rz = 0,rangle = 1.57079633;

        TriangleModelCreator tmc = null;
        tmc = new TriangleModelCreator(geom, x, y, z,
            rx,ry,rz,rangle,innerMaterial,fill);

        tmc.generate(grid);

        return grid;
    }

    /**
     * Creates a simple cube in a grid and returns the grid.
     *
     * @param cWidth The width of the cube
     * @param cHeight The height of the cube
     * @param cDepth The depth of the cube
     * @param innerMaterial The inner material
     * @return The grid containing the cube
     */
    private static void createCubeInGrid(AttributeGrid grid, float cWidth, float cHeight, float cDepth,
            double translateX, double translateY, double translateZ,
            byte innerMaterial, int geomType, boolean fill) {

        BoxGenerator bg = new BoxGenerator(cWidth, cHeight, cDepth);
        GeometryData geom = new GeometryData();
        geom.geometryType = geomType;
        bg.generate(geom);

        double bounds = findMaxBounds(geom);
//System.out.println("geometry bounds: " + bounds);

        double rx = 0,ry = 1,rz = 0,rangle = 0;

        TriangleModelCreator tmc = null;
        tmc = new TriangleModelCreator(geom,translateX+bounds,translateY+bounds,translateZ+bounds,
            rx,ry,rz,rangle,innerMaterial,fill);

        tmc.generate(grid);
    }

    /**
     * Creates a simple cube in a grid and returns the grid.
     *
     */
    private static AttributeGrid createCylinderInGrid(float height, float radius,
            double translateX, double translateY, double translatZ,
            byte outerMaterial, byte innerMaterial, int geomType) {

        CylinderGenerator cg = new CylinderGenerator(height, radius);
        GeometryData geom = new GeometryData();
        geom.geometryType = geomType;
        cg.generate(geom);

        double bounds = findMaxBounds(geom);
//System.out.println("geometry bounds: " + bounds);

        // twice the bounds (since centered at origin) plus a slight over allocate
        int gWidth = (int) (2.0f * radius / HORIZ_RESOLUTION) + 5;
        int gHeight = (int) (height / VERT_RESOLUTION) + 5;
        int gDepth = gWidth;

//System.out.println("grid dimensions: " + gWidth + " " + gHeight + " " + gDepth);

        AttributeGrid grid = new ArrayAttributeGridByte(gWidth, gHeight, gDepth, HORIZ_RESOLUTION, VERT_RESOLUTION);

//        double x = bounds;// + 5.0 * HORIZ_RESOLUTION;
//        double y = x;
//        double z = x;

        double rx = 0,ry = 1,rz = 0,rangle = 0;

        TriangleModelCreator tmc = null;
        tmc = new TriangleModelCreator(geom, translateX+bounds, translateY+bounds, translatZ+bounds,
            rx,ry,rz,rangle,innerMaterial,true);

        tmc.generate(grid);

        return grid;
    }

    /**
     * Check all voxels for correctness of its state. This function assumes a voxelized
     * cube in the grid and the starting index is the same in all three axes. Works
     * only for cubes aligned with the axes.
     *
     * @param grid The grid
     * @param xVoxels The number of cube voxels in the x axis
     * @param yVoxels The number of cube voxels in the y axis
     * @param zVoxels The number of cube voxels in the z axis
     */
    private void checkCubeVoxelStates(AttributeGrid grid, int innerMaterial,
            int xStartIndex, int yStartIndex, int zStartIndex,
            int xVoxels, int yVoxels, int zVoxels) {

//System.out.println("cube interior count: " + grid.findCount(VoxelClasses.INSIDE));
//System.out.println("cube exterior count: " + grid.findCount(VoxelClasses.INSIDE));

        int xEndIndex = xStartIndex + xVoxels - 1;
        int yEndIndex = yStartIndex + yVoxels - 1;
        int zEndIndex = zStartIndex + zVoxels - 1;

//System.out.println("xEndIndex: " + xEndIndex);
//System.out.println("yEndIndex: " + yEndIndex);
//System.out.println("zEndIndex: " + zEndIndex);

        // Check each voxel for state correctness
        for (int x=0; x<grid.getWidth(); x++) {
            for (int y=0; y<grid.getHeight(); y++) {
                for (int z=0; z<grid.getDepth(); z++) {
//System.out.println("[" + x + ", " + y + ", " + z + "]: " + grid.getState(x, y, z));

                    // If x or y or z is at the starting index of the cube voxels,
                    // and the other coordinates are less than the ending index
                    // of the cube voxels, it is an exterior voxel. If x or y or z
                    // is at the ending index and the other coordinates are less
                    // than the ending index, it is an exterior voxel.
                    //
                    // If the index is between the starting and ending cube voxels,
                    // exclusive, it is an interior voxel.

                    if ( (x == xStartIndex && (y >= yStartIndex && y <= yEndIndex) && (z >= zStartIndex && z <= zEndIndex) ) ||
                         (y == yStartIndex && (x >= xStartIndex && x <= xEndIndex) && (z >= zStartIndex && z <= zEndIndex) ) ||
                         (z == zStartIndex && (x >= xStartIndex && x <= xEndIndex) && (y >= yStartIndex && y <= yEndIndex) ) ||
                         (x == (xEndIndex) && (y >= yStartIndex && y <= yEndIndex) && (z >= zStartIndex && z <= zEndIndex) ) ||
                         (y == (yEndIndex) && (x >= xStartIndex && x <= xEndIndex) && (z >= zStartIndex && z <= zEndIndex) ) ||
                         (z == (zEndIndex) && (x >= xStartIndex && x <= xEndIndex) && (y >= yStartIndex && y <= yEndIndex) ) ) {

                        assertEquals("State is not exterior for grid: " + x + ", " + y + ", " + z,
                                Grid.INSIDE, grid.getState(x, y, z));

                    } else if (x > xStartIndex && x < (xEndIndex) &&
                               y > yStartIndex && y < (yEndIndex) &&
                               z > zStartIndex && z < (zEndIndex)) {

                        assertEquals("State is not interior for grid: " + x + ", " + y + ", " + z,
                                Grid.INSIDE, grid.getState(x, y, z));

                        assertEquals("Inner material is not " + innerMaterial + " for grid: " + x + ", " + y + ", " + z,
                                innerMaterial, grid.getAttribute(x, y, z));

                    } else {
                        assertEquals("State is not outside for grid: " + x + ", " + y + ", " + z,
                                Grid.OUTSIDE, grid.getState(x, y, z));
                    }
                }
            }
        }
    }

    /**
     * Check all voxels for correctness of its state. This function assumes a voxelized
     * cube in the grid and the starting index is the same in all three axes. Works
     * only for cubes aligned with the axes.
     *
     * @param grid The grid
     * @param xVoxels The number of cube voxels in the x axis
     * @param yVoxels The number of cube voxels in the y axis
     * @param zVoxels The number of cube voxels in the z axis
     */
    private void checkCubeOnlyVoxelStates(AttributeGrid grid, int innerMaterial,
            int xStartIndex, int yStartIndex, int zStartIndex,
            int xVoxels, int yVoxels, int zVoxels) {

        int xEndIndex = xStartIndex + xVoxels - 1;
        int yEndIndex = yStartIndex + yVoxels - 1;
        int zEndIndex = zStartIndex + zVoxels - 1;

//System.out.println("xEndIndex: " + xEndIndex);
//System.out.println("yEndIndex: " + yEndIndex);
//System.out.println("zEndIndex: " + zEndIndex);

        // Check each voxel for state correctness
        for (int x=0; x<grid.getWidth(); x++) {
            for (int y=0; y<grid.getHeight(); y++) {
                for (int z=0; z<grid.getDepth(); z++) {
//System.out.println("[" + x + ", " + y + ", " + z + "]: " + grid.getState(x, y, z));

                    // If x or y or z is at the starting index of the cube voxels,
                    // and the other coordinates are less than the ending index
                    // of the cube voxels, it is an exterior voxel. If x or y or z
                    // is at the ending index and the other coordinates are less
                    // than the ending index, it is an exterior voxel.
                    //
                    // If the index is between the starting and ending cube voxels,
                    // exclusive, it is an interior voxel.

                    if ( (x == xStartIndex && (y >= yStartIndex && y <= yEndIndex) && (z >= zStartIndex && z <= zEndIndex) ) ||
                         (y == yStartIndex && (x >= xStartIndex && x <= xEndIndex) && (z >= zStartIndex && z <= zEndIndex) ) ||
                         (z == zStartIndex && (x >= xStartIndex && x <= xEndIndex) && (y >= yStartIndex && y <= yEndIndex) ) ||
                         (x == (xEndIndex) && (y >= yStartIndex && y <= yEndIndex) && (z >= zStartIndex && z <= zEndIndex) ) ||
                         (y == (yEndIndex) && (x >= xStartIndex && x <= xEndIndex) && (z >= zStartIndex && z <= zEndIndex) ) ||
                         (z == (zEndIndex) && (x >= xStartIndex && x <= xEndIndex) && (y >= yStartIndex && y <= yEndIndex) ) ) {

                        assertEquals("State is not exterior for grid: " + x + ", " + y + ", " + z,
                                Grid.INSIDE, grid.getState(x, y, z));

                    } else if (x > xStartIndex && x < (xEndIndex) &&
                               y > yStartIndex && y < (yEndIndex) &&
                               z > zStartIndex && z < (zEndIndex)) {

                        assertEquals("State is not interior for grid: " + x + ", " + y + ", " + z,
                                Grid.INSIDE, grid.getState(x, y, z));

                        assertEquals("Inner material is not " + innerMaterial + " for grid: " + x + ", " + y + ", " + z,
                                innerMaterial, grid.getAttribute(x, y, z));
                    }
                }
            }
        }
    }

    /**
     * Check all voxels for correctness of its state. This function assumes a voxelized
     * cube in the grid and the starting index is the same in all three axes. Works
     * only for cubes aligned with the axes.
     *
     * @param grid The grid
     */
    private void checkCylinder(AttributeGrid grid, float height, float radius,
            byte outerMaterial, byte innerMaterial) {

        int outerMatPerSlice = 0;
        int innerMatPerSlice = 0;
        int exteriorPerSlice = 0;
        int interiorPerSlice = 0;

        int heightInVoxels = (int) Math.round(height / VERT_RESOLUTION);
        int midHeight = heightInVoxels / 2;
        int gridWidth = grid.getWidth();
        int gridDepth = grid.getDepth();
        byte state;
        long mat;
        boolean inside;

        // Material count per slice of the cylinder (not counting the top and bottom slice)
        for (int x=0; x<gridWidth; x++) {
            inside = false;

            for (int z=0; z<gridDepth; z++) {

                mat = grid.getAttribute(x, midHeight, z);
                if (mat == outerMaterial) {
                    outerMatPerSlice++;
                } else if (mat == innerMaterial) {
                    innerMatPerSlice++;
                }

                state = grid.getState(x, midHeight, z);
//System.out.println(x + ", " + midHeight + ", " + z + ": " + state);

                if (state == Grid.INSIDE) {
                    exteriorPerSlice++;

                    if (inside) {
                        inside = false;
                    } else {
                        if (z < (gridDepth - 1) && grid.getState(x, midHeight, z+1) == Grid.INSIDE) {
                            inside = true;
                        }
                    }

                    continue;
                }

                if (inside) {
                    assertEquals("State is not INSIDE for: " + x + ", " + midHeight + ", " + z,
                            Grid.INSIDE, state);
                } else {
                    assertFalse("State is INSIDE for: " + x + ", " + midHeight + ", " + z,
                            state == Grid.INSIDE);
                }
            }

            // if a row ends at inside, something is wrong
            assertFalse("Grid state should not be inside at end of row", inside);
        }

//System.out.println("outerMatPerSlice: " + outerMatPerSlice);
//System.out.println("innerMatPerSlice: " + innerMatPerSlice);
//System.out.println("heightInVoxels: " + heightInVoxels);
//System.out.println("interiorPerSlice: " + interiorPerSlice);
//System.out.println("exteriorPerSlice: " + exteriorPerSlice);

        int expectedInteriorCount = interiorPerSlice * (heightInVoxels - 2);
        int expectedExteriorCount = exteriorPerSlice * heightInVoxels + 2 * interiorPerSlice;
        int expectedInMatCount = expectedInteriorCount + expectedExteriorCount;

        assertEquals("Inside count is not " + expectedExteriorCount + expectedInteriorCount,
                expectedExteriorCount + expectedInteriorCount, grid.findCount(VoxelClasses.INSIDE));

        assertEquals("Inside material count is not " + expectedInMatCount,
                expectedInMatCount, grid.findCount(innerMaterial));
    }

    private void checkVoxelStatePattern(AttributeGrid grid) {

        int gridWidth = grid.getWidth();
        int gridHeight = grid.getHeight();
        int gridDepth = grid.getDepth();
        byte state;
        boolean inside;

        for (int y=0; y<gridHeight; y++) {

            for (int z=0; z<gridDepth; z++) {
                inside = false;

                for (int x=0; x<gridWidth; x++) {

                    state = grid.getState(x, y, z);
//System.out.println(x + ", " + y + ", " + z + ": " + state);

                    if (state == Grid.INSIDE) {
                        if (inside) {
                            inside = false;
                        } else {
                            if (x < (gridWidth - 1) && grid.getState(x+1, y, z) == Grid.INSIDE) {
                                inside = true;
                            }
                        }

                        continue;
                    }

                    if (inside) {
                        assertEquals("State is not INSIDE for: " + x + ", " + y + ", " + z,
                                Grid.INSIDE, state);
                    } else {
                        assertFalse("State is INSIDE for: " + x + ", " + y + ", " + z,
                                state == Grid.INSIDE);
                    }
                }

                // if a row ends at inside, something is wrong
                assertFalse("Grid state should not be inside at end of row", inside);
            }
        }
    }

    /**
     * Find the absolute maximum bounds of a geometry.
     *
     * @return The max
     */
    private static double findMaxBounds(GeometryData geom) {
        double max = Double.NEGATIVE_INFINITY;

        int len = geom.coordinates.length;

        for(int i=0; i < len; i++) {
            if (geom.coordinates[i] > max) {
                max = geom.coordinates[i];
            }
        }

        return Math.abs(max);
    }

    /**
     * Get the total number of voxels of a cube.
     *
     * @param xVoxels Number of voxels in the x direction
     * @param yVoxels Number of voxels in the y direction
     * @param zVoxels Number of voxels in the z direction
     * @return The number of voxels of the cube
     */
    private int getCubeTotalVoxelCount(int xVoxels, int yVoxels, int zVoxels) {
        return xVoxels * yVoxels * zVoxels;
    }

    /**
     * Get the total number of interior voxels of a cube.
     *
     * @param xVoxels Number of voxels in the x direction
     * @param yVoxels Number of voxels in the y direction
     * @param zVoxels Number of voxels in the z direction
     * @return The number of interior voxels of the cube
     */
    private int getCubeInteriorVoxelCount(int xVoxels, int yVoxels, int zVoxels) {
        return (xVoxels - 2) * (yVoxels - 2) * (xVoxels - 2) + (xVoxels * yVoxels * 2) + (xVoxels * zVoxels * 2) + (yVoxels * zVoxels * 2) -
                (4 * xVoxels) - (4 * yVoxels) - (4 * zVoxels) + 8;
    }

    /**
     * Print the voxels states of a grid.  Only rows that have non-outside states
     * are printed.
     *
     * @param grid The grid
     */
    private void printGridStates(AttributeGrid grid) {
        int gridWidth = grid.getWidth();
        int gridHeight = grid.getHeight();
        int gridDepth = grid.getDepth();
        byte state;

//      for (int y=0; y<gridHeight; y++) {

//          for (int z=0; z<gridDepth; z++) {
                boolean rowHasState = false;
                String temp = "";
                int y = 12;
                int z = 14;
                for (int x=0; x<gridWidth; x++) {

                    state = grid.getState(x, y, z);
//System.out.println(x + ", " + y + ", " + z + ": " + state);

                    if (state == Grid.OUTSIDE) {
                        temp = temp + x + " " + y + " " + z + ": OUTSIDE\n";
                    } else if (state == Grid.INSIDE) {
                        temp = temp + x + " " + y + " " + z + ": =>EXTERIOR\n";
                        rowHasState = true;
                    } else if (state == Grid.INSIDE) {
                        temp = temp + x + " " + y + " " + z + ": ==>INSIDE\n";
                        rowHasState = true;
                    }
                }

                if (rowHasState) {
                    System.out.println(temp);
                }

//          }
//        }
    }

    /**
     * Generate an X3D file
     * @param filename
     */
    public static void generate(String filename) {
        try {
            ErrorReporter console = new PlainTextErrorReporter();

            long stime = System.currentTimeMillis();

            float ir = 0.02f;
            float or = 0.03f;
            int facets = 64;
            byte outerMaterial = 1;
            byte innerMaterial = 2;

            AttributeGrid grid = createTorusInGrid(ir, or, facets, innerMaterial,
                    GeometryData.TRIANGLES, true);

            FileOutputStream fos = new FileOutputStream(filename);
            String encoding = filename.substring(filename.lastIndexOf(".")+1);
            BoxesX3DExporter exporter = new BoxesX3DExporter(encoding, fos, console);

            HashMap<Integer, float[]> colors = new HashMap<Integer, float[]>();
            colors.put(new Integer(Grid.INSIDE), new float[] {0,1,0});
            colors.put(new Integer(Grid.INSIDE), new float[] {1,0,0});
            colors.put(new Integer(Grid.OUTSIDE), new float[] {0,1,1});

            HashMap<Integer, Float> transparency = new HashMap<Integer, Float>();
            transparency.put(new Integer(Grid.INSIDE), new Float(0));
            transparency.put(new Integer(Grid.INSIDE), new Float(0.5));
            transparency.put(new Integer(Grid.OUTSIDE), new Float(1));

//            exporter.write(grid, null);
            exporter.writeDebug(grid, colors, transparency);

            System.out.println("Gen time: " + (System.currentTimeMillis() - stime));

            System.out.println("Writing x3d");
            stime = System.currentTimeMillis();

            exporter.close();

            System.out.println("GenX3D time: " + (System.currentTimeMillis() - stime));

            fos.close();

        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void generate(AttributeGrid grid, String filename) {
        try {
            ErrorReporter console = new PlainTextErrorReporter();

            FileOutputStream fos = new FileOutputStream(filename);
            String encoding = filename.substring(filename.lastIndexOf(".")+1);
            BoxesX3DExporter exporter = new BoxesX3DExporter(encoding, fos, console);

            HashMap<Integer, float[]> colors = new HashMap<Integer, float[]>();
            colors.put(new Integer(Grid.INSIDE), new float[] {0,1,0});
            colors.put(new Integer(Grid.INSIDE), new float[] {1,0,0});
            colors.put(new Integer(Grid.OUTSIDE), new float[] {0,1,1});

            HashMap<Integer, Float> transparency = new HashMap<Integer, Float>();
            transparency.put(new Integer(Grid.INSIDE), new Float(0.0));
            transparency.put(new Integer(Grid.INSIDE), new Float(0.5));
            transparency.put(new Integer(Grid.OUTSIDE), new Float(0.9));

            exporter.writeDebug(grid, colors, transparency);

            exporter.close();

            fos.close();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }
/*
    public static void main(String[] args) {
        generate("out.x3db");
    }
*/
}
