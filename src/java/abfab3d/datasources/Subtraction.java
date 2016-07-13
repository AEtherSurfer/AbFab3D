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


//import java.awt.image.Raster;


import abfab3d.core.ResultCodes;
import abfab3d.param.Parameter;
import abfab3d.param.SNode;
import abfab3d.param.SNodeParameter;
import abfab3d.param.DoubleParameter;

import abfab3d.core.Vec;
import abfab3d.core.DataSource;
import abfab3d.core.Initializable;

import static java.lang.Math.abs;

import static abfab3d.core.Output.printf;

import static abfab3d.core.MathUtil.clamp;
import static abfab3d.core.MathUtil.step10;
import static abfab3d.core.MathUtil.blendMax;


/**

   Boolean difference between two data sources 
   <br/>

   <embed src="doc-files/Subtraction.svg" type="image/svg+xml"/> 

   
   @author Vladimir Bulatov

 */
public class Subtraction extends TransformableDataSource implements SNode {
    
    private DataSource m_dataSource1;
    private DataSource m_dataSource2;
    private double m_blend;

    SNodeParameter mp_shape1 = new SNodeParameter("shape1", ShapesFactory.getInstance());
    SNodeParameter mp_shape2 = new SNodeParameter("shape2", ShapesFactory.getInstance());
    DoubleParameter mp_blend = new DoubleParameter("blend", "blend width", 0.);

    Parameter m_aparam[] = new Parameter[]{
        mp_shape1,
        mp_shape2,
        mp_blend,
    };    

    public Subtraction(){
        super.addParams(m_aparam);        
    }

    /**
       shape which is result of subtracting shape2 from shape1
     */
    public Subtraction(DataSource shape1, DataSource shape2){
        super.addParams(m_aparam);

        setShape1(shape1);
        setShape2(shape2);
    }

    /**
     * Set the blending width
     */
    public void setBlend(double r){
        mp_blend.setValue(r);
    }

    public void setShape1(DataSource shape1) {
        mp_shape1.setValue(shape1);
    }

    public void setShape2(DataSource shape2) {
        mp_shape2.setValue(shape2);
    }

    /**
       @noRefGuide
     */
    public int initialize(){

        super.initialize();

        m_blend = mp_blend.getValue();
        m_dataSource1 = (DataSource)mp_shape1.getValue();
        m_dataSource2 = (DataSource)mp_shape2.getValue();
        
        initializeChild(m_dataSource1);
        initializeChild(m_dataSource2);
        
        return ResultCodes.RESULT_OK;
        
    }
    
    public int getBaseValue(Vec pnt, Vec data) {
        switch(m_dataType){
        default:
        case DATA_TYPE_DENSITY:
            getDensityValue(pnt, data);
            break;
        case DATA_TYPE_DISTANCE:
            getDistanceValue(pnt, data);
            break;
        }
        return ResultCodes.RESULT_OK;        
    }

    public int getDistanceValue(Vec pnt, Vec data) {

        Vec p = new Vec(pnt);

        m_dataSource1.getDataValue(p, data);
        double d1 = data.v[0];

        p.set(pnt);
        m_dataSource2.getDataValue(p, data);
        double d2 = data.v[0];
        
        data.v[0] = blendMax(d1, -d2, m_blend);
        return ResultCodes.RESULT_OK;        
        
    }

    /**
     * @noRefGuide
       
     * calculates values of all data sources and return maximal value
     * can be used to make union of few shapes
     */
    public int getDensityValue(Vec pnt, Vec data) {
        
        double v1 = 0, v2 = 0;
        
        int res = m_dataSource1.getDataValue(new Vec(pnt), data);        
        v1 = data.v[0];
        
        if(v1 <= 0.){
            data.v[0] = 0.0;
            return ResultCodes.RESULT_OK;
        }
        
        // we are here if v1 > 0
        
        res = m_dataSource2.getDataValue(new Vec(pnt), data);
                
        v2 = data.v[0];
        if(v2 >= 1.){
            data.v[0] = 0.;
            return ResultCodes.RESULT_OK;
        }
        data.v[0] = v1*(1-v2);
        
        return ResultCodes.RESULT_OK;
    }


    /**
     * @override
     * @noRefGuide
     */
    public SNode[] getChildren() {
        return new SNode[] {(SNode)mp_shape1.getValue(),(SNode)mp_shape2.getValue()};
    }
} // class Subtraction
