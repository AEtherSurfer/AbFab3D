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

package abfab3d.distance;

import javax.vecmath.Vector3d;
import javax.vecmath.Matrix3d;
import javax.vecmath.AxisAngle4d;
import javax.vecmath.Quat4d;

import static java.lang.Math.sqrt;
import static java.lang.Math.abs;


/**
   returns the signed distance to 3D sphere
   if radius > 0 the interipor of sphere is inside 
   if radius < 0 the exterior of sphere is inside    
   @author Vladimir Bulatov
 */
public class DistanceDataSphere implements DistanceData {
        
    double cx,cy, cz; 
    double radius;

    public DistanceDataSphere(double radius){
        this(radius, new Vector3d(0,0,0));
    }

    public DistanceDataSphere(double radius, Vector3d center) {
        this.cx = center.x;
        this.cy = center.y;
        this.cz = center.z;
        this.radius = radius;

    }

    //
    // return distance to the sphere in 3D 
    //
    public double getDistance(double x, double y, double z){

        // move center to origin 
        x -= cx;
        y -= cy;
        z -= cz;       
 
        if(radius > 0) // interior of sphere 
            return sqrt(x*x + y*y + z*z) - radius;
        else  // exterior of sphere 
            return (- sqrt(x*x + y*y + z*z) - radius);        
    }
}
