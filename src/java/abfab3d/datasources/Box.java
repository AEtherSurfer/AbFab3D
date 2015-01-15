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

package abfab3d.datasources;


import abfab3d.param.DoubleParameter;
import abfab3d.param.Parameter;
import abfab3d.param.Vector3dParameter;
import abfab3d.util.Units;
import abfab3d.util.Vec;

import javax.vecmath.Vector3d;

import static abfab3d.util.MathUtil.intervalCap;
import static abfab3d.util.Output.printf;


/**
 * Solid box of given size
 
   <embed src="doc-files/Box.svg" type="image/svg+xml"/> 

 * @author Vladimir Bulatov
 */
public class Box extends TransformableDataSource {

    static final boolean DEBUG = false;
    static int debugCount = 1000;

    private double m_sizeX = 0.1, m_sizeY = 0.1, m_sizeZ = 0.1, m_centerX = 0, m_centerY = 0, m_centerZ = 0;

    private double
            xmin,
            xmax,
            ymin,
            ymax,
            zmin, zmax;

    protected boolean
            m_hasSmoothBoundaryX = true,
            m_hasSmoothBoundaryY = true,
            m_hasSmoothBoundaryZ = true;

    /**
     * Box with 0,0,0 center and given size
     *
     * @param sx x size
     * @param sy y size
     * @param sz z size
     */
    public Box(double sx, double sy, double sz) {
        this(0, 0, 0, sx, sy, sz);
    }

    /**
     * Box with 0,0,0 center and given size
     *
     * @param size Size vector
     */
    public Box(Vector3d size) {
        this(0,0,0,size.x,size.y,size.z);
    }


    /**
     * Box with given center and size
     *
     * @param cx  x coordinate of center
     * @param cy  y coordinate of center
     * @param cz  z coordinate of center
     * @param sx x size
     * @param sy y size
     * @param sz z size
     */
    public Box(double cx, double cy, double cz, double sx, double sy, double sz) {
        initParams();

        setCenter(cx, cy, cz);
        setSize(sx, sy, sz);
    }

    /**
     * @noRefGuide
     */
    protected void initParams() {
        super.initParams();

        Parameter p = new Vector3dParameter("size");
        params.put(p.getName(), p);

    }

    /**
     * Blah blah
     *
     * @noRefGuide
     * @param boundaryX
     * @param boundaryY
     * @param boundaryZ
     */
    public void setSmoothBoundaries(boolean boundaryX, boolean boundaryY, boolean boundaryZ) {
        m_hasSmoothBoundaryX = boundaryX;
        m_hasSmoothBoundaryY = boundaryY;
        m_hasSmoothBoundaryZ = boundaryZ;
    }

    /**
     * Set the size of the box
     *
     * @param sx x size
     * @param sy y size
     * @param sz z size
     */
    public void setSize(double sx, double sy, double sz) {

        if (sx < 0 || sy < 0 || sz < 0) {
            throw new IllegalArgumentException("Box size < 0. Value: " + sx + " " + sy + " " + sz);
        }
        m_sizeX = sx;
        m_sizeY = sy;
        m_sizeZ = sz;

        ((Vector3dParameter) params.get("size")).setValue(new Vector3d(m_sizeX,m_sizeY,m_sizeZ));
    }

    /**
     * Set the size of the box
     *
     * @param size Size vector
     */
    public void setSize(Vector3d size) {
        if (size.x < 0 || size.y < 0 || size.z < 0) {
            throw new IllegalArgumentException("Box size < 0. Value: " + size.x + " " + size.y + " " + size.z);
        }
        m_sizeX = size.x;
        m_sizeY = size.y;
        m_sizeZ = size.z;

        ((Vector3dParameter) params.get("size")).setValue(new Vector3d(m_sizeX,m_sizeY,m_sizeZ));
    }

    /**
     * Set the center of the box
     *
     * @param cx  x coordinate of center
     * @param cy  y coordinate of center
     * @param cz  z coordinate of center
     *
     */
    public void setCenter(double cx, double cy, double cz) {
        m_centerX = cx;
        m_centerY = cy;
        m_centerZ = cz;

        ((Vector3dParameter) params.get("center")).setValue(new Vector3d(cx,cy,cz));
    }

    /**
     * @noRefGuide
     */
    public int initialize() {

        super.initialize();

        xmin = m_centerX - m_sizeX / 2;
        xmax = m_centerX + m_sizeX / 2;

        ymin = m_centerY - m_sizeY / 2;
        ymax = m_centerY + m_sizeY / 2;

        zmin = m_centerZ - m_sizeZ / 2;
        zmax = m_centerZ + m_sizeZ / 2;

        return RESULT_OK;

    }

    /**
     * Get the data value for a pnt
     *
     * @noRefGuide
     *
     * @return 1 if pnt is inside of box of given size and center 0 otherwise
     */
    public int getDataValue(Vec pnt, Vec data) {

        super.transform(pnt);

        double res = 1.;
        double
                x = pnt.v[0],
                y = pnt.v[1],
                z = pnt.v[2];

        double vs = pnt.getScaledVoxelSize();
        if (DEBUG && debugCount-- > 0)
            printf("vs: %5.3fmm\n", vs / Units.MM);
        if (vs == 0.) {
            // zero voxel size
            if (x < xmin || x > xmax ||
                    y < ymin || y > ymax ||
                    z < zmin || z > zmax) {
                data.v[0] = 0.;
            } else {
                data.v[0] = 1.;
            }
        } else {

            // finite voxel size
            if (x <= xmin - vs || x >= xmax + vs ||
                    y <= ymin - vs || y >= ymax + vs ||
                    z <= zmin - vs || z >= zmax + vs) {
                data.v[0] = 0.;
                return RESULT_OK;
            }
            double finalValue = 1;

            if (m_hasSmoothBoundaryX)
                finalValue = Math.min(finalValue, intervalCap(x, xmin, xmax, vs));
            if (m_hasSmoothBoundaryY)
                finalValue = Math.min(finalValue, intervalCap(y, ymin, ymax, vs));
            if (m_hasSmoothBoundaryZ)
                finalValue = Math.min(finalValue, intervalCap(z, zmin, zmax, vs));

            data.v[0] = finalValue;
        }

        super.getMaterialDataValue(pnt, data);        
        return RESULT_OK;        

    }

}  // class Box
