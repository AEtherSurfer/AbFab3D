/*****************************************************************************
 *                        Shapeways, Inc Copyright (c) 2012
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/
package abfab3d.mesh;

import java.util.Set;
import java.util.HashSet;
import javax.vecmath.Point3d;

/**
   structure to describe edge collapse event 
*/
public class EdgeCollapseData {

    Edge edgeToCollapse;
    Point3d point; 
    Set<Edge> removedEdges = new HashSet<Edge>();  // place for removed edges 
    
    int faceCount;  // face count after collapse 
    int edgeCount;  // edge cont after collapse 
    int vertexCount; // vertex count after collapse  
        
}
