/*****************************************************************************
 *                        Shapeways, Inc Copyright (c) 2016
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package abfab3d.symmetry;

import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;
import javax.vecmath.Matrix4d;
import abfab3d.util.Vec;

import static abfab3d.symmetry.SymmetryUtil.len2; 
import static abfab3d.symmetry.SymmetryUtil.subSet;
import static abfab3d.symmetry.SymmetryUtil.mulSet;
import static abfab3d.symmetry.SymmetryUtil.addSet;
import static abfab3d.util.Output.printf;
 
/**
 * class to represent 3d euclidean transforms which can be represented as 4d matrix 
 *
 *
 * @author Vladimir Bulatov
 */
public class ETransform implements PairingTransform {
    
    // matrix of transform
    protected Matrix4d m_matrix;
    //matrix of oiverted transform 
    protected Matrix4d m_imatrix;
    
    /**
     *  constructor to use matrix as the source 
     *
     */
    public ETransform(Matrix4d matrix){
        m_matrix = new Matrix4d();
        m_matrix.set(matrix);
        init();
        
    }

    public ETransform(Plane plane){
        
        m_matrix = getReflectionMatrix(plane.getNormal(), plane.getDistance());
        init();
    }

    void init(){
        m_imatrix = new Matrix4d();
        m_imatrix.set(m_matrix);
        m_imatrix.invert();
    }
    
    
    public void transform(Vec pnt){
        
        transform(m_imatrix, pnt.v);

    }

    /**
     * transform 3d vector p by 4d matrix 
     */
    public static void transform(Matrix4d t, double p[]){
        double p0 = p[0];
        double p1 = p[1];
        double p2 = p[2];
        // p3 = 1;
        p[0]= t.m00*p0 + t.m01*p1 + t.m02*p2 + t.m03;
        p[1]= t.m10*p0 + t.m11*p1 + t.m12*p2 + t.m13;
        p[2]= t.m20*p0 + t.m21*p1 + t.m22*p2 + t.m23;

    }

    /**

       return matrix of given translation 

     */
    public static Matrix4d getTranslationMatrix(double tx, double ty, double tz){

        Matrix4d m = new Matrix4d();
        m.m00 = 1;
        m.m11 = 1;
        m.m22 = 1;
        m.m33 = 1;

        m.m03 = tx; 
        m.m13 = ty; 
        m.m23 = tz; 

        return m;
    }

    /**
       return transform matrix of reflection in the given plane
       @param p plane equation (represent plane via equation dot(p,v) = 0
       plane should be normalized ( p.x*p.x + p.y*p.y + p.z*p.z == 1) 
       
    */
    public static Matrix4d getReflectionMatrix(Vector4d p){
        
        Matrix4d m = new Matrix4d();

        // transformation act on 4D vectors as follows 
        //  x-> x - 2*p*dot_3(p,x);  

        m.m00 = 1-2*p.x*p.x;
        m.m01 =  -2*p.x*p.y;
        m.m02 =  -2*p.x*p.z;
        m.m03 =  -2*p.x*p.w;

        m.m10 =  -2*p.y*p.x;
        m.m11 = 1-2*p.y*p.y;
        m.m12 =  -2*p.y*p.z;
        m.m13 =  -2*p.y*p.w;

        m.m20 =  -2*p.z*p.x;
        m.m21 =  -2*p.z*p.y;
        m.m22 = 1-2*p.z*p.z;
        m.m23 =  -2*p.z*p.w;
        // last row is not used at the moment  
        m.m30 =  0;
        m.m31 =  0;
        m.m32 =  0;
        m.m33 =  1;
       
        return m;
    }

    public static Matrix4d getReflectionMatrix(Vector3d normal, double distance){

        return getReflectionMatrix(new Vector4d(normal.x,normal.y,normal.z,-distance));
        
    }

} // class ETransform

