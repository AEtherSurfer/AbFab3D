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
 * @author Vladimir Bulatov
 */
public class ArrayAttributeGridLong extends BaseAttributeGrid {
    protected long[] data;

    /**
     * Constructor.
     *
     * @param w The number of voxels in width
     * @param h The number of voxels in height
     * @param d The number of voxels in depth
     * @param pixel The size of the pixels
     * @param sheight The slice height in meters
     */
    public ArrayAttributeGridLong(int w, int h, int d, double pixel, double sheight) {
        this(w,h,d,pixel,sheight,null);
    }

    /**
     * Constructor.
     *
     * @param w The width in world coords
     * @param h The height in world coords
     * @param d The depth in world coords
     * @param pixel The size of the pixels
     * @param sheight The slice height in meters
     */
    public ArrayAttributeGridLong(double w, double h, double d, double pixel, double sheight) {
        this(roundSize(w / pixel),roundSize(h / sheight),roundSize(d / pixel),
                pixel,
                sheight, null);
    }

    /**
     * Constructor.
     *
     * @param w The width in world coords
     * @param h The height in world coords
     * @param d The depth in world coords
     * @param pixel The size of the pixels
     * @param sheight The slice height in meters
     */
    public ArrayAttributeGridLong(double w, double h, double d, double pixel, double sheight, InsideOutsideFunc ioFunc) {
        this(roundSize(w / pixel),roundSize(h / sheight),roundSize(d / pixel),
                pixel,
                sheight, ioFunc);
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
    public ArrayAttributeGridLong(int w, int h, int d, double pixel, double sheight, InsideOutsideFunc ioFunc) {
        super(w,h,d,pixel,sheight,ioFunc);
        long dataLength = (long)height * width * depth;
        if(dataLength >= Integer.MAX_VALUE){
            throw new IllegalArgumentException("Size exceeds integer, use ArrayAttributeGridIntLongIndex");
        }
        data = new long[(int)dataLength];
    }
    
    /**
     * Copy Constructor.
     *
     * @param grid The grid
     */
    public ArrayAttributeGridLong(ArrayAttributeGridLong grid) {
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
        return new ArrayAttributeGridLong(w,h,d,pixel,sheight, ioFunc);
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

        long encoded = data[idx];
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

        data[idx] = ioFunc.combineStateAndAttribute(state,material);
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

        data[idx] = ioFunc.combineStateAndAttribute(state,material);
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

        data[idx] = ioFunc.updateAttribute(data[idx], material);
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
        data[idx] = ioFunc.combineStateAndAttribute(state,att);
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
        data[idx] = ioFunc.combineStateAndAttribute(state,att);
    }

    /**
     * Clone the object.
     */
    public Object clone() {
        ArrayAttributeGridLong ret_val = new ArrayAttributeGridLong(this);

        BaseGrid.copyBounds(this, ret_val);
        return ret_val;
    }
}

