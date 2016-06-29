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

// Internal Imports
import abfab3d.core.Grid;

/**
 * Creator of 3D geometry.
 *
 * @author Alan Hudson
 *
 * Coordinate System(X3D):
 *
 *           -------    --->X(width)
 *           |     |    |
 *           |     |    Z(depth)
 *           -------
 */
public abstract class GeometryCreator {
    /**
     * Generate the geometry and issue commands to the provided handler.
     *
     * @param dest The dest grid
     */
    public abstract void generate(Grid dest);
}
