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

package abfab3d.util;

import javax.vecmath.Vector3d;

/**
   interface accepts a collection of triangles with possible extra information (texture, color, etc)
   
 */
public interface AttributedTriangleCollector {

    /**
       add triangle 
       vertices are copied into internal structure and can be reused after return       

       returns true if success, false if faiure        
     */
    public boolean addAttTri(Vec v0,Vec v1,Vec v2);
    
}
