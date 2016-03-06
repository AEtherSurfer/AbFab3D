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
import abfab3d.param.SNodeParameter;
import abfab3d.param.BaseSNodeFactory;
import abfab3d.util.VecTransform;
import abfab3d.util.Vec;
import abfab3d.util.Initializable;
import abfab3d.util.DataSource;
import abfab3d.util.Bounds;

import net.jafama.FastMath;

import static abfab3d.util.Output.printf;
import static abfab3d.util.Symmetry.getReflection;
import static abfab3d.util.Symmetry.toFundamentalDomain;


/**

   translates cooddinates by value of DataSource
   useful for shape warping

   @author Vladimir Bulatov   
 */
public class Warp extends BaseTransform implements VecTransform, Initializable {
   
    static final String sm_warpNames[] = new String[]{"Noise", "Abs", "Mul", "Constant"};
    static String sm_warpClasses[];

    static {

        sm_warpClasses = new String[sm_warpNames.length];
        String packName = "abfab3d.datasources.";        
        for(int i = 0; i < sm_warpNames.length; i++){
            sm_warpClasses[i] = packName + sm_warpNames[i];
        }
    }
    
    DataSource m_source;
    SNodeParameter mp_source = new SNodeParameter("source", new BaseSNodeFactory(sm_warpNames, sm_warpClasses));

    protected Parameter m_aparams[] = new Parameter[]{
        mp_source
    };

    /**
       creates empty 
     */
    public Warp() {
        addParams(m_aparams);
    }

    /**
       creates composite transform with single transform 
     */
    public Warp(DataSource source) {

        addParams(m_aparams);
        setSource(source);

    }

    /**
       set source used for coordinate
     */
    public void setSource(DataSource source){
        
        mp_source.setValue(source);
        
    }

    /**
       @noRefGuide
     */
    public int initialize(){

        m_source = (DataSource)mp_source.getValue();
        if(m_source == null) 
            m_source = new Zero();
        if(m_source instanceof Initializable){
            int res = ((Initializable)m_source).initialize();
            return res;
        }            
        return RESULT_OK;
    }

    /**
       @noRefGuide
     */
    public int transform(Vec in, Vec out) {
        
        inverse_transform(in,out);
        
        return RESULT_OK;
    }                
    
    /**
       @noRefGuide
     */
    public int inverse_transform(Vec in, Vec out) {

        m_source.getDataValue(in, out);
        // add initial values 
        out.v[0] += in.v[0];
        out.v[1] += in.v[1];
        out.v[2] += in.v[2];

        return RESULT_OK;
        
    }

    //
    // null data source 
    //
    static class Zero implements DataSource {
        public int getDataValue(Vec pnt, Vec dataValue){
            dataValue.v[0] = 0;
            dataValue.v[1] = 0;
            dataValue.v[2] = 0;
            dataValue.v[3] = 0;
            return RESULT_OK;
        }
        public int getChannelsCount(){
            return 1;
        }
        public Bounds getBounds(){
            return null;
        }
        public void setBounds(Bounds bounds){
        }
        
    }
}  // class Warp
