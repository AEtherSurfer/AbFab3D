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

package abfab3d.grid;

// External Imports

/**
 * A grid backed by arrays.
 *
 * Likely better performance for memory access that is slice aligned.
 *
 * Uses the X3D coordinate system.  Y-up.  Grid is located
 * on positive right side octant.
 *
 * @author Alan Hudson
 */
public class ArrayAttributeGridInt extends BaseAttributeGrid {
    protected int[] data;

    /**
     * Constructor.
     *
     * @param w The number of voxels in width
     * @param h The number of voxels in height
     * @param d The number of voxels in depth
     * @param pixel The size of the pixels
     * @param sheight The slice height in meters
     */
    public ArrayAttributeGridInt(int w, int h, int d, double pixel, double sheight) {
        this(w,h,d,pixel,sheight,null);
    }

    /**
     * Constructor.
     *
     * @param bounds The grid bounds
     * @param pixel The size of the pixels
     * @param sheight The slice height in meters
     */
    public ArrayAttributeGridInt(Bounds bounds, double pixel, double sheight) {
        super(bounds, pixel,sheight);
        allocateData();
    }

    /**
     * Constructor.
     *
     * @param w The number of voxels in width
     * @param h The number of voxels in height
     * @param d The number of voxels in depth
     * @param pixel The size of the pixels
     * @param sheight The slice height in meters
     */
    public ArrayAttributeGridInt(int w, int h, int d, double pixel, double sheight, InsideOutsideFunc ioFunc) {
        super(w,h,d,pixel,sheight,ioFunc);
        allocateData();
    }

    protected void allocateData(){

        long dataLength = (long)height * width * depth;
        if(dataLength >= Integer.MAX_VALUE){
            throw new IllegalArgumentException("Size exceeds integer, use ArrayAttributeGridIntLongIndex");
        }
        data = new int[height * width * depth];
    }
    
    /**
     * Copy Constructor.
     *
     * @param grid The grid
     */
    public ArrayAttributeGridInt(ArrayAttributeGridInt grid) {
        super(grid.getWidth(), grid.getHeight(), grid.getDepth(),
            grid.getVoxelSize(), grid.getSliceHeight(),grid.ioFunc);
        this.data = grid.data.clone();
    }

    /**
     * Create an empty grid of the specified size.  Reuses
     * the grid type and material type(byte, short, int).
     *
     * @param w The number of voxels in width
     * @param h The number of voxels in height
     * @param d The number of voxels in depth
     * @param pixel The size of the pixels
     * @param sheight The slice height in meters
     */
    public Grid createEmpty(int w, int h, int d, double pixel, double sheight) {
        return new ArrayAttributeGridInt(w,h,d,pixel,sheight, ioFunc);
    }

    /**
     * Get a new instance of voxel data.  Returns this grids specific sized voxel data.
     *
     * @return The voxel data
     */
    public VoxelData getVoxelData() {
        return new VoxelDataInt();
    }

    /**
     * Get the data of the voxel
     *
     * @param x The x grid coordinate
     * @param y The y grid coordinate
     * @param z The z grid coordinate
     */
    public void getData(int x, int y, int z, VoxelData vd) {
        int idx = y * sliceSize + x * depth + z;

        int encoded = data[idx];
        long att = ioFunc.getAttribute(encoded);
        byte state = ioFunc.getState(encoded);

        vd.setData(state,att);
    }

    /**
     * Get the data of the voxel
     *  @param x The x world coordinate
     * @param y The y world coordinate
     * @param z The z world coordinate
     */
    public void getDataWorld(double x, double y, double z, VoxelData vd) {

        int slice = (int)((y-yorig) / sheight);
        int s_x =   (int)((x-xorig) / pixelSize);
        int s_z =   (int)((z-zorig) / pixelSize);

        int idx = slice * sliceSize + s_x * depth + s_z;

        long att = ioFunc.getAttribute(data[idx]);
        byte state = ioFunc.getState(data[idx]);

        vd.setData(state, att);
    }

    /**
     * Get the state of the voxel
     *  @param x The x world coordinate
     * @param y The y world coordinate
     * @param z The z world coordinate
     */
    public byte getStateWorld(double x, double y, double z) {
        int slice = (int)((y-yorig) / sheight);
        int s_x =   (int)((x-xorig) / pixelSize);
        int s_z =   (int)((z-zorig) / pixelSize);

        int idx = slice * sliceSize + s_x * depth + s_z;

        return ioFunc.getState(data[idx]);
    }

    /**
     * Get the state of the voxel.
     *
     * @param x The x world coordinate
     * @param y The y world coordinate
     * @param z The z world coordinate
     */
    public byte getState(int x, int y, int z) {
        int idx = y * sliceSize + x * depth + z;

        return ioFunc.getState(data[idx]);
    }

    /**
     * Get the state of the voxel
     *  @param x The x world coordinate
     * @param y The y world coordinate
     * @param z The z world coordinate
     */
    public long getAttributeWorld(double x, double y, double z) {
        int slice = (int)((y-yorig) / sheight);
        int s_x =   (int)((x-xorig) / pixelSize);
        int s_z =   (int)((z-zorig) / pixelSize);

        int idx = slice * sliceSize + s_x * depth + s_z;

        return ioFunc.getAttribute(data[idx]);
    }

    /**
     * Get the material of the voxel.
     *
     * @param x The x world coordinate
     * @param y The y world coordinate
     * @param z The z world coordinate
     */
    public long getAttribute(int x, int y, int z) {
        int idx = y * sliceSize + x * depth + z;

        return ioFunc.getAttribute(data[idx]);
    }

    /**
     * Set the value of a voxel.
     *  @param x The x world coordinate
     * @param y The y world coordinate
     * @param z The z world coordinate
     * @param state The voxel state
     * @param material The material
     */
    public void setDataWorld(double x, double y, double z, byte state, long material) {
        int slice = (int)((y-yorig) / sheight);
        int s_x =   (int)((x-xorig) / pixelSize);
        int s_z =   (int)((z-zorig) / pixelSize);

        int idx = slice * sliceSize + s_x * depth + s_z;

        data[idx] = (int) ioFunc.combineStateAndAttribute(state,material);
    }

    /**
     * Set the value of a voxel.
     *
     * @param x The x grid coordinate
     * @param y The y grid coordinate
     * @param z The z grid coordinate
     * @param state The voxel state
     * @param material The material
     */
    public void setData(int x, int y, int z, byte state, long material) {
        int idx = y * sliceSize + x * depth + z;

        data[idx] = (int) ioFunc.combineStateAndAttribute(state,material);
    }

    /**
     * Set the material value of a voxel.  Leaves the state unchanged.
     *
     * @param x The x world coordinate
     * @param y The y world coordinate
     * @param z The z world coordinate
     * @param material The materialID
     */
    public void setAttribute(int x, int y, int z, long material) {
        int idx = y * sliceSize + x * depth + z;

        data[idx] = (int) ioFunc.updateAttribute(data[idx], material);
    }

    /**
     * Set the state value of a voxel.  Leaves the material unchanged.
     *
     * @param x The x world coordinate
     * @param y The y world coordinate
     * @param z The z world coordinate
     * @param state The value.  0 = nothing. > 0 materialID
     */
    public void setState(int x, int y, int z, byte state) {
        int idx = y * sliceSize + x * depth + z;

        long att = ioFunc.getAttribute(data[idx]);
        data[idx] = (int) ioFunc.combineStateAndAttribute(state,att);
    }

    /**
     * Set the state value of a voxel.  Leaves the material unchanged.
     *  @param x The x world coordinate
     * @param y The y world coordinate
     * @param z The z world coordinate
     * @param state The value.  0 = nothing. > 0 materialID
     */
    public void setStateWorld(double x, double y, double z, byte state) {
        int slice = (int)((y-yorig) / sheight);
        int s_x =   (int)((x-xorig) / pixelSize);
        int s_z =   (int)((z-zorig) / pixelSize);

        int idx = slice * sliceSize + s_x * depth + s_z;

        long att = ioFunc.getAttribute(data[idx]);
        data[idx] = (int) ioFunc.combineStateAndAttribute(state,att);
    }

    /**
     * Clone the object.
     */
    public Object clone() {
        ArrayAttributeGridInt ret_val = new ArrayAttributeGridInt(this);

        BaseGrid.copyBounds(this, ret_val);
        return ret_val;
    }
}

