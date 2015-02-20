/*****************************************************************************
 *                        Shapeways, Inc Copyright (c) 2011-2015
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package abfab3d.transforms;

import java.util.Vector;
import java.util.List;

import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;
import javax.vecmath.Matrix4d;
import javax.vecmath.AxisAngle4d;

import abfab3d.param.Parameter;
import abfab3d.param.Parameterizable;
import abfab3d.param.SNode;
import abfab3d.param.SNodeListParameter;
import abfab3d.util.*;

import net.jafama.FastMath;

import static abfab3d.util.Output.printf;
import static abfab3d.util.Symmetry.getReflection;
import static abfab3d.util.Symmetry.toFundamentalDomain;


/**

   Arbitrary chain of transformations to be applied to the point 
   
   @author Vladimir Bulatov   
 */
public class CompositeTransform extends BaseTransform implements VecTransform, Initializable {
        
    private VecTransform aTransforms[]; // array of transforms used in calculations 

    SNodeListParameter mp_transforms = new SNodeListParameter("transforms");

    protected Parameter m_aparams[] = new Parameter[]{
        mp_transforms
    };

    public CompositeTransform() {
        addParams(m_aparams);
    }

    /**
       add transform to the chain of transforms
     */
    public void add(VecTransform transform){
        
        ((List)mp_transforms.getValue()).add(transform);
        
    }

    /**
       @noRefGuide
     */
    public int initialize(){
        
        aTransforms = getTransformsArray();
        int size = aTransforms.length;
        for(int i = 0; i < size; i++){
            VecTransform tr = aTransforms[i];
            if(tr instanceof Initializable){
                int res = ((Initializable)tr).initialize();
                if(res != RESULT_OK)
                    return res;
            }
        }
        
        return RESULT_OK;
    }

    public VecTransform [] getTransformsArray(){

        List<SNode> trans = (List<SNode>)mp_transforms.getValue();
        int size = trans.size();
        VecTransform ta[] = new VecTransform[size];
        int k = 0;
        for(SNode t: trans){
            ta[k++] = (VecTransform)t;
        }
        return ta;
    }

    /**
       @noRefGuide
     */
    public int transform(Vec in, Vec out) {
        
        int len = aTransforms.length;
        if(len < 1){
            // copy input to output                 
            out.set(in);
            return RESULT_OK;                
        }
        
        //TODO garbage generation 
        Vec vin = new Vec(in);
        
        for(int i = 0; i < len; i++){
            
            VecTransform tr = aTransforms[i];
            int res = tr.transform(in, out);
                if(res != RESULT_OK)
                    return res;
                
                in.set(out);
        }
        
        return RESULT_OK;
    }                
    
    /**
       @noRefGuide
     */
    public int inverse_transform(Vec in, Vec out) {
        
        int len = aTransforms.length;
        if(len < 1){
            // copy input to output                 
            out.set(in);
            return RESULT_OK;                
        }
        
        //TODO garbage generation 
        Vec vin = new Vec(in);
        
        for(int i = aTransforms.length-1; i >= 0; i--){
            
            VecTransform tr = aTransforms[i];
            int res = tr.inverse_transform(vin, out);
            
            if(res != RESULT_OK)
                return res;
            vin.set(out);
        }
        
        return RESULT_OK;
        
    }

    @Override
    public SNode[] getChildren() {
        
        VecTransform[] ta = getTransformsArray();

        SNode[] ret = new SNode[ta.length];
        for(int i=0; i < ta.length; i++) {
            ret[i] = (SNode)ta[i];
        }
        return ret;
    }

}  // class CompositeTransform
