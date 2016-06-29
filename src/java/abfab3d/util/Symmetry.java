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

import abfab3d.core.ResultCodes;

import javax.vecmath.Vector4d;
import javax.vecmath.Matrix4d;

import static java.lang.Math.sqrt;

import static abfab3d.core.Output.printf;

/**
   does calculations necessary for different symmetry operations 

   @author Vladimir Bulatov
 */
public class Symmetry {

    static final boolean DEBUG = false;
    static final double 
        SQRT2 = sqrt(2),
        SQRT3 = sqrt(3);

    
    // sides of Fundamental Domain (FD). 
    // Sides normals point outside of FD
    // points inside of FD satisfy pnt.dot(sides[i]) < 0
    // points outside of FD satisfy pnt.dot(sides[i]) > 0                
    public Vector4d planes[]; 
    
    // pairing transformations of fundamental domain 
    // trans[i] transform FD into area adjacent to side i
    public Matrix4d trans[];
    
    // inverse transformations 
    // intrans[i] transforms area adjacent to side i into FD 
    public Matrix4d invtrans[]; 

    
    public Symmetry(Vector4d planes[], Matrix4d transforms[]){

        this.planes = planes;
        this.trans = transforms;
        this.invtrans = makeInversion(trans);

    }

    public static Symmetry getII(double domainWidth){
        
        Vector4d[] planes = new Vector4d[2]; 
        planes[0] = new Vector4d(-1,0,0,-domainWidth/2);
        planes[1] = new Vector4d(1,0,0,-domainWidth/2);
        Vector4d p2 = new Vector4d(1,0,0,0);
        
        Matrix4d m0 = getReflection(planes[0]);
        Matrix4d m1 = getReflection(p2);
        
        Matrix4d[] trans = new Matrix4d[2];

        trans[0] = new Matrix4d(); 
        trans[1] = new Matrix4d(); 
        
        trans[0].mul(m0,m1);
        trans[1].mul(m1,m0);
        return new Symmetry(planes, trans);
    }
    
    public static Symmetry getIX(double domainWidth){
        
        Vector4d[] planes = new Vector4d[2]; 
        planes[0] = new Vector4d(-1,0,0,-domainWidth/2);
        planes[1] = new Vector4d(1,0,0,-domainWidth/2);
        Vector4d p2 = new Vector4d(1,0,0,0);
        Vector4d p3 = new Vector4d(0,1,0,0);
        
        Matrix4d m0 = getReflection(planes[0]);
        Matrix4d m1 = getReflection(p2);
        Matrix4d t = new Matrix4d(); 
        t.mul(m0, m1);
        t.mul(getReflection(p3));
        
        Matrix4d[] trans = new Matrix4d[2];
        
        trans[0] = t;
        trans[1] = new Matrix4d(); 
        trans[1].invert(t);

        return new Symmetry(planes, trans);

    }
    
    public static Symmetry get22I(double domainWidth){
        
        Vector4d[] planes = new Vector4d[2]; 
        planes[0] = new Vector4d(-1,0,0,-domainWidth/2);
        planes[1] = new Vector4d(1,0,0,-domainWidth/2);
        Vector4d p2 = new Vector4d(0,1,0,0);
        
        Matrix4d[] trans = new Matrix4d[2];
        
        trans[0] = new Matrix4d(); 
        trans[1] = new Matrix4d(); 
        
        trans[0].mul(getReflection(planes[0]),getReflection(p2));
        trans[1].mul(getReflection(planes[1]),getReflection(p2));

        return new Symmetry(planes, trans);
    }
    
    public static Symmetry getSII(double domainWidth){
        
        Vector4d planes[] = new Vector4d[2]; 
        planes[0] = new Vector4d(-1,0,0,-domainWidth/2);
        planes[1] = new Vector4d(1,0,0,-domainWidth/2);
        
        Matrix4d[] trans = new Matrix4d[2];
        
        trans[0] = getReflection(planes[0]);
        trans[1] = getReflection(planes[1]);
        
        return new Symmetry(planes, trans);

    }

    public static Symmetry getIS(double domainWidth){
        
        Vector4d planes[] = new Vector4d[3]; 
        planes[0] = new Vector4d(-1,0,0,-domainWidth/2);
        planes[1] = new Vector4d(1,0,0,-domainWidth/2);
        planes[2] = new Vector4d(0,-1,0,0);
        
        Vector4d p2 = new Vector4d(1,0,0,0);
        
        Matrix4d m0 = getReflection(planes[0]);
        Matrix4d m1 = getReflection(p2);
        
        Matrix4d[] trans = new Matrix4d[3];
        
        trans[0] = new Matrix4d(); 
        trans[1] = new Matrix4d(); 
        
        trans[0].mul(m0,m1);
        trans[1].mul(m1,m0);
        trans[2] = getReflection(planes[2]);
        return new Symmetry(planes, trans);
        
    }

    public static Symmetry getS22I(double domainWidth){
        
        Vector4d planes[] = new Vector4d[3]; 
        planes[0] = new Vector4d(-1,0,0,-domainWidth/2);
        planes[1] = new Vector4d(1,0,0,-domainWidth/2);
        planes[2] = new Vector4d(0,-1,0,0);
        
        Matrix4d[] trans = new Matrix4d[3];
        
        trans[0] = getReflection(planes[0]);
        trans[1] = getReflection(planes[1]);
        trans[2] = getReflection(planes[2]);
        
        return new Symmetry(planes, trans);
    }
    
    public static Symmetry get2SI(double domainWidth){
        
        Vector4d planes[] = new Vector4d[3];
        
        planes[0] = new Vector4d(-1,0,0,-domainWidth/2);
        planes[1] = new Vector4d(1,0,0,-domainWidth/2);
        planes[2] = new Vector4d(0,-1,0,0);
        
        Matrix4d trans[] = new Matrix4d[3];
        
        trans[0] = getReflection(planes[0]);
        trans[1] = getReflection(planes[1]);
        
        trans[2] = new Matrix4d();
        trans[2].mul(getReflection(planes[2]), getReflection(new Vector4d(1,0,0,0))); // rotation pi
        
        return new Symmetry(planes, trans);

    }
    
    /**
       wallpaper group *442
       
     */
    public static Symmetry getS442(double domainWidth){
        
        Vector4d planes[] = new Vector4d[3]; 
        planes[0] = new Vector4d(0,-1,0,0);
        planes[1] = new Vector4d(1/SQRT2,1/SQRT2,0,-domainWidth/SQRT2);
        planes[2] = new Vector4d(-1,0,0,0);

        Matrix4d trans[] = new Matrix4d[3];
        
        for(int i=0; i < 3; i++){
            trans[i] = getReflection(planes[i]);
        }
        
        return new Symmetry(planes, trans);
        
    }

    
    /**
       wallpaper group 442
       
     */
    public static Symmetry get442(double domainWidth){
        

        Vector4d planes[] = new Vector4d[3]; 
        
        planes[0] = new Vector4d(1./SQRT2,-1./SQRT2, 0., -domainWidth/SQRT2);
        planes[1] = new Vector4d(1./SQRT2,1./SQRT2, 0., -domainWidth/SQRT2);
        planes[2] = new Vector4d(-1, 0,0, 0);

        Matrix4d trans[] = new Matrix4d[3];
        
        Vector4d py = new Vector4d(0,1,0,0);
        for(int i = 0; i < 3; i++){
            trans[i] = new Matrix4d();
            trans[i].mul(getReflection(planes[i]), getReflection(py));
        }
        
        return new Symmetry(planes, trans);
        
    }

    /**
       wallpaper group 4S2
       
     */
    public static Symmetry get4S2(double domainWidth){
        

        Vector4d planes[] = new Vector4d[3]; 
        
        planes[0] = new Vector4d(1./SQRT2,-1./SQRT2, 0., -domainWidth/SQRT2);
        planes[1] = new Vector4d(1./SQRT2,1./SQRT2, 0., -domainWidth/SQRT2);
        planes[2] = new Vector4d(-1, 0,0, 0);

        Matrix4d trans[] = new Matrix4d[3];
        
        Vector4d py = new Vector4d(0,1,0,0);
        for(int i = 0; i < 2; i++){
            trans[i] = new Matrix4d();
            trans[i].mul(getReflection(planes[i]), getReflection(py));
        }
        trans[2] = getReflection(planes[2]);
        
        return new Symmetry(planes, trans);
        
    }

    /**
       wallpaper group 3S3
       
     */
    public static Symmetry get3S3(double domainWidth){
        

        Vector4d planes[] = new Vector4d[3]; 
        
        planes[0] = new Vector4d(SQRT3/2,-0.5,0., -domainWidth*(SQRT3/2.));
        planes[1] = new Vector4d(SQRT3/2, 0.5,0., -domainWidth*(SQRT3/2.));
        planes[2] = new Vector4d(-1,0,0,0);

        Matrix4d trans[] = new Matrix4d[3];
        
        Vector4d py = new Vector4d(0,1,0,0);
        for(int i = 0; i < 2; i++){
            trans[i] = new Matrix4d();
            trans[i].mul(getReflection(planes[i]), getReflection(py));
        }
        trans[2] = getReflection(planes[2]);
        
        return new Symmetry(planes, trans);
        
    }

    /**
       wallpaper group *632
       
     */
    public static Symmetry getS632(double domainWidth){
        
        Vector4d planes[] = new Vector4d[3]; 
        planes[0] = new Vector4d(0,-1,0,0);
        planes[1] = new Vector4d(SQRT3/2,0.5,0., -domainWidth*(SQRT3/2.));
        planes[2] = new Vector4d(-1,0,0,0);

        Matrix4d trans[] = new Matrix4d[3];
        
        for(int i=0; i < 3; i++){
            trans[i] = getReflection(planes[i]);
        }
        
        return new Symmetry(planes, trans);
        
    }

    /**
       wallpaper group *333
       
     */
    public static Symmetry getS333(double domainWidth){
        
        Vector4d planes[] = new Vector4d[3]; 
        planes[0] = new Vector4d(0, -1, 0, 0);
        planes[1] = new Vector4d(SQRT3/2,0.5,0., -domainWidth/2*(SQRT3/2.));
        planes[2] = new Vector4d(-SQRT3/2,0.5,0., -domainWidth/2*(SQRT3/2.));

        Matrix4d trans[] = new Matrix4d[3];
        
        for(int i=0; i < 3; i++){
            trans[i] = getReflection(planes[i]);
        }
        
        return new Symmetry(planes, trans);
        
    }

    /**
       wallpaper group 333
       
     */
    public static Symmetry get333(double domainWidth){
        
        Vector4d planes[] = new Vector4d[4]; 
        
        planes[0] = new Vector4d(SQRT3/2,-0.5,0., -domainWidth/2*(SQRT3/2.));
        planes[1] = new Vector4d(SQRT3/2, 0.5,0., -domainWidth/2*(SQRT3/2.));
        planes[2] = new Vector4d(-SQRT3/2,0.5,0., -domainWidth/2*(SQRT3/2.));
        planes[3] = new Vector4d(-SQRT3/2,-0.5,0., -domainWidth/2*(SQRT3/2.));

        Matrix4d trans[] = new Matrix4d[4];
        
        Vector4d py = new Vector4d(0,1,0,0);
        for(int i = 0; i < 4; i++){
            trans[i] = new Matrix4d();
            trans[i].mul(getReflection(planes[i]), getReflection(py));
        }
        
        return new Symmetry(planes, trans);
        
    }


    /**
       wallpaper group 632
       
     */
    public static Symmetry get632(double domainWidth){
        
        Vector4d planes[] = new Vector4d[3]; 
        
        planes[0] = new Vector4d(SQRT3/2,-0.5,0., -domainWidth*(SQRT3/2.));
        planes[1] = new Vector4d(SQRT3/2, 0.5,0., -domainWidth*(SQRT3/2.));
        planes[2] = new Vector4d(-1, 0,0, 0);

        Matrix4d trans[] = new Matrix4d[3];
        
        Vector4d py = new Vector4d(0,1,0,0);
        for(int i = 0; i < 3; i++){
            trans[i] = new Matrix4d();
            trans[i].mul(getReflection(planes[i]), getReflection(py));
        }
        
        return new Symmetry(planes, trans);
        
    }


    
    /**
       wallpaper group *2222
       
     */
    public static Symmetry getS2222(double domainWidth, double domainHeight){
        
        Vector4d planes[] = new Vector4d[4]; 
        planes[0] = new Vector4d(1,0,0,-domainWidth);
        planes[1] = new Vector4d(0,1,0,-domainHeight);
        planes[2] = new Vector4d(-1,0,0,0);
        planes[3] = new Vector4d(0,-1,0,0);

        Matrix4d trans[] = new Matrix4d[4];
        
        for(int i=0; i < 4; i++){
            trans[i] = getReflection(planes[i]);
        }
        
        return new Symmetry(planes, trans);
        
    }

    /**
       wallpaper group 2222
       
     */
    public static Symmetry get2222(double domainWidth, double domainHeight){
        
        Vector4d p[] = new Vector4d[4]; 
        p[0] = new Vector4d(0,-1,0,-domainHeight/2);
        p[1] = new Vector4d(1, 0,0,-domainWidth);
        p[2] = new Vector4d(0, 1,0,-domainHeight/2);
        p[3] = new Vector4d(-1,0,0,0);
        
        Vector4d p01 = new Vector4d(0,1,0,0);

        Matrix4d trans[] = new Matrix4d[4];
        
        for(int i=0; i < 4; i++){
            trans[i] = new Matrix4d();
            trans[i].mul(getReflection(p[i]),getReflection(p01));
        }
        
        return new Symmetry(p, trans);
        
    }

    /**
       wallpaper group 2*22
       
     */
    public static Symmetry get2S22(double domainWidth, double domainHeight){
        
        Vector4d p[] = new Vector4d[4]; 
        p[0] = new Vector4d(0,-1,0,-domainHeight/2);
        p[1] = new Vector4d(1,0,0,-domainWidth);
        p[2] = new Vector4d(0,1,0,-domainHeight/2);
        p[3] = new Vector4d(-1,0,0,0);
        
        Vector4d p01 = new Vector4d(0,1,0,0);

        Matrix4d trans[] = new Matrix4d[4];
        
        trans[0] = getReflection(p[0]);
        trans[1] = getReflection(p[1]);
        trans[2] = getReflection(p[2]);
        trans[3] = new Matrix4d();
        trans[3].mul(getReflection(p[3]),getReflection(p01));
        
        return new Symmetry(p, trans);
        
    }

    /**
       wallpaper group 22*
       
     */
    public static Symmetry get22S(double domainWidth, double domainHeight){
        
        Vector4d p[] = new Vector4d[4]; 
        p[0] = new Vector4d(0,-1,0,-domainHeight/2);
        p[1] = new Vector4d(1,0,0, -domainWidth);
        p[2] = new Vector4d(0,1,0, -domainHeight/2);
        p[3] = new Vector4d(-1,0,0,0);
        
        Vector4d p01 = new Vector4d(0,1,0,0);

        Matrix4d trans[] = new Matrix4d[4];
        
        trans[0] = getReflection(p[0]);

        trans[1] = new Matrix4d();
        trans[1].mul(getReflection(p[1]),getReflection(p01));

        trans[2] = getReflection(p[2]);

        trans[3] = new Matrix4d();
        trans[3].mul(getReflection(p[3]),getReflection(p01));
        
        return new Symmetry(p, trans);
        
    }

    /**
       wallpaper group **
       
     */
    public static Symmetry getSS(double domainWidth, double domainHeight){
        
        Vector4d p[] = new Vector4d[4]; 
        p[0] = new Vector4d(0,-1,0,-domainHeight/2);
        p[1] = new Vector4d(1,0,0, -domainWidth);
        p[2] = new Vector4d(0,1,0, -domainHeight/2);
        p[3] = new Vector4d(-1,0,0,0);
        
        Vector4d p01 = new Vector4d(0,1,0,0);

        Matrix4d trans[] = new Matrix4d[4];
        

        trans[0] = new Matrix4d();
        trans[0].mul(getReflection(p[0]),getReflection(p01));

        trans[1] = getReflection(p[1]);

        trans[2] = new Matrix4d();
        trans[2].mul(getReflection(p[2]),getReflection(p01));

        trans[3] = getReflection(p[3]);
        
        return new Symmetry(p, trans);
        
    }
 
    /**
       wallpaper group *X
       
     */
    public static Symmetry getSX(double domainWidth, double domainHeight){
        
        Vector4d p[] = new Vector4d[4]; 
        p[0] = new Vector4d(0,-1,0,-domainHeight/2);
        p[1] = new Vector4d(1,0,0, -domainWidth/2);
        p[2] = new Vector4d(0,1,0, -domainHeight/2);
        p[3] = new Vector4d(-1,0,0,-domainWidth/2);
        
        Vector4d py = new Vector4d(0,1,0,0);
        Vector4d px = new Vector4d(1,0,0,0);
        
        Matrix4d trans[] = new Matrix4d[4];
        
        
        trans[0] = getReflection(p[0]);
        
        trans[2] = getReflection(p[2]);
        
        trans[1] = new Matrix4d();
        trans[1].mul(getReflection(p[1]),getReflection(py));
        trans[1].mul(getReflection(px));
        
        trans[2] = getReflection(p[2]);
        
        trans[3] = new Matrix4d();
        trans[3].mul(getReflection(p[3]),getReflection(py));
        trans[3].mul(getReflection(px));
        
        return new Symmetry(p, trans);
        
    }

    /**
       wallpaper group 22X
       
     */
    public static Symmetry get22X(double domainWidth, double domainHeight){
        
        Vector4d p[] = new Vector4d[4]; 
        p[0] = new Vector4d(0,-1,0,-domainHeight/2);
        p[1] = new Vector4d(1,0,0, -domainWidth/2);
        p[2] = new Vector4d(0,1,0, -domainHeight/2);
        p[3] = new Vector4d(-1,0,0,-domainWidth/2);
        
        Vector4d py = new Vector4d(0,1,0,0);
        Vector4d px = new Vector4d(1,0,0,0);

        Matrix4d trans[] = new Matrix4d[4];
        

        trans[0] = new Matrix4d();
        trans[0].mul(getReflection(p[0]),getReflection(px));
        trans[0].mul(getReflection(py));

        trans[1] = new Matrix4d();
        trans[1].mul(getReflection(p[1]),getReflection(py));
        trans[1].mul(getReflection(px));

        trans[2] = new Matrix4d();
        trans[2].mul(getReflection(p[2]),getReflection(px));
        trans[2].mul(getReflection(py));

        trans[3] = new Matrix4d();
        trans[3].mul(getReflection(p[3]),getReflection(py));
        trans[3].mul(getReflection(px));
        
        return new Symmetry(p, trans);
        
    }

    /**
       wallpaper group XX
       
     */
    public static Symmetry getXX(double domainWidth, double domainHeight){
        
        Vector4d p[] = new Vector4d[4]; 
        p[0] = new Vector4d(0,-1,0,-domainHeight/2);
        p[1] = new Vector4d(1,0,0, -domainWidth/2);
        p[2] = new Vector4d(0,1,0, -domainHeight/2);
        p[3] = new Vector4d(-1,0,0,-domainWidth/2);
        
        Vector4d py = new Vector4d(0,1,0,0);
        Vector4d px = new Vector4d(1,0,0,0);

        Matrix4d trans[] = new Matrix4d[4];
        

        trans[0] = new Matrix4d();
        trans[0].mul(getReflection(p[0]),getReflection(py));

        trans[1] = new Matrix4d();
        trans[1].mul(getReflection(p[1]),getReflection(py));
        trans[1].mul(getReflection(px));

        trans[2] = new Matrix4d();
        trans[2].mul(getReflection(p[2]),getReflection(py));

        trans[3] = new Matrix4d();
        trans[3].mul(getReflection(p[3]),getReflection(py));
        trans[3].mul(getReflection(px));
        
        return new Symmetry(p, trans);
        
    }

    /**
       wallpaper group o
       
     */
    public static Symmetry getO(double domainWidth, double domainHeight, double skew){
       

        double w = domainWidth;
        double h = domainHeight;
        double sx = domainWidth*skew; 
        double norm = sqrt(sx*sx + h*h);
        double nx = h/norm;
        double ny = sx/norm;
        int sign = (sx >= 0.)? (1):(-1);

        Vector4d p[] = new Vector4d[4]; 
               
        p[0] = new Vector4d(0,-1,0,-h/2);
        p[1] = new Vector4d(nx,-ny, 0,-(nx*w - sign*ny*h)/2);
        p[2] = new Vector4d(0,1,0, -h/2);
        p[3] = new Vector4d(-nx,ny, 0,-(nx*w - sign*ny*h)/2);

        Matrix4d trans[] = new Matrix4d[4];        

        trans[0] = getTranslation(-sx, -h, 0);
        trans[1] = getTranslation(w-sign*sx, 0, 0);
        trans[2] = getTranslation(sx, h, 0);
        trans[3] = getTranslation(-(w-sign*sx), 0, 0);
        
        return new Symmetry(p, trans);
        
    }
  
    /**
       return array of inverse transforms 
    */
    public static Matrix4d[] makeInversion(Matrix4d trans[]){
        
        Matrix4d itr[] = new Matrix4d[trans.length];
        for(int i = 0; i < itr.length; i++){
            itr[i] = new Matrix4d();
            itr[i].invert(trans[i]);
        }
        
        return itr;
    }


    /**
       return tranform matrix of reflection in the given plane
       plane is expected to be normalized ( p.x*p.x + p.y*p.y + p.z*p.z == 1) 
    */
    public static Matrix4d getReflection(Vector4d p){
        
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
       
        m.m30 =  0;
        m.m31 =  0;
        m.m32 =  0;
        m.m33 =  1;
       
        return m;
    }

    /**

       return matrix of given translation 

     */
    public static Matrix4d getTranslation(double tx, double ty, double tz){

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
       transforms input vector v into fundamental domain using given Symmetry 
     */
    public static int toFundamentalDomain(Vector4d v, Symmetry sym, int maxCount ){

        return toFundamentalDomain(v, sym.planes, sym.invtrans, maxCount);

    }

    
    public static int toFundamentalDomain(Vector4d v, Vector4d planes[], Matrix4d[] trans, int maxCount ){
        
        // transform point to fundamental domain
        int count = maxCount;

        while(count-- > 0){

            boolean found = false; 

            for(int i =0; i < planes.length; i++){
                double d = planes[i].dot(v);
                if(DEBUG)printf("d[%d]:%7.2f\n", i, d);

                if(d > 0) {
                    found = true;
                    trans[i].transform(v);
                    if(DEBUG)printf("    v: (%5.2f,%5.2f,%5.2f,%5.2f)\n", v.x, v.y, v.z, v.w);
                    break; // out of planes cycle                     
                }                           
            }
            
            if(!found){
                // we are in FD
                return ResultCodes.RESULT_OK;
            }
        }        
        // if we are here - we haven't found FD 
        // return out of domain do something 
        //if(DEBUG)printf("out of iterations\n");
        return ResultCodes.RESULT_OUTSIDE;
    }   
}
