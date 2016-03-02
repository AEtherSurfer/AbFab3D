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

import java.util.List;


import javax.vecmath.Vector3d;


import abfab3d.param.BaseParameterizable;
import abfab3d.param.Parameter;
import abfab3d.param.SNodeListParameter;
import abfab3d.param.SNode;
import abfab3d.param.Parameterizable;
import abfab3d.param.BaseSNodeFactory;


import abfab3d.transforms.Rotation;
import abfab3d.transforms.Scale;
import abfab3d.transforms.Translation;
import abfab3d.transforms.CompositeTransform;
import abfab3d.transforms.TransformsFactory;

import abfab3d.util.DataSource;
import abfab3d.util.Initializable;
import abfab3d.util.VecTransform;
import abfab3d.util.Vec;
import abfab3d.util.Bounds;


import static abfab3d.util.Output.fmt;
import static abfab3d.util.Output.printf;

/**
   Base class for DataSources which want to be Transformable

   the TransformableDataSource may have it's own Transform and Material
   
   subclasses are responsible to implement getDataValue() 
   according to template 
   int getDataValue(Vec pnt, Vec data) {
      super.transform(pnt);
      //
      ...  do own calculations ...
      //
      super.getMaterialDataValue(pnt, data);      
   }
   

   @author Vladimir Bulatov

 */
public abstract class TransformableDataSource extends BaseParameterizable implements DataSource, Initializable {

    static final boolean DEBUG = true;
    // transformation which is applied to the data point before the calculation of data value
    protected VecTransform m_transform = null; 
    // count of data channels 

    protected int m_channelsCount = 1;
    // count of channels of material 
    protected int m_materialChannelsCount = 0;
    // material used for this shape 

    // the material is potential multichannel data source and it adds channels to the total channels count
    //TODO - make material a param
    protected DataSource m_material = null; 
    protected Bounds m_bounds = null;
    protected boolean boundsDirty = false;

    SNodeListParameter mp_transform = new SNodeListParameter("transform", new BaseSNodeFactory(TransformsFactory.getNames(), TransformsFactory.getClassNames()));
    
    private Parameter m_aparam[] = new Parameter[]{
        mp_transform
    };
    

    protected TransformableDataSource(){
        addParams(m_aparam);
    }

    /**
     * Transform the data source
     * @noRefGuide
     * @param transform General transformation to apply to the object before it is rendered
     */
    public void setTransform(VecTransform transform){
        
        mp_transform.set((Parameterizable)transform);

    }

    /**
     * Append transform to the data source
     * @noRefGuide
     * @param transform General transformation to apply to the object before it is rendered
     */
    /*
    public void addTransform(VecTransform transform){
        if(m_transform == null)
            m_transform = transform;        
        throw new RuntimeException();
        //TODO need to move transforms to this package 
        //else if(m_transform instanceof )
    }
    */
    /**
     * @noRefGuide
       @return Transformation the object
     */
    public VecTransform getTransform() {
        return m_transform;
    }

    public VecTransform makeTransform() {

        List list = mp_transform.getValue();
        
        Object tr[] = list.toArray();
        if(DEBUG)printf("makeTransform() %s count: %d\n", this, tr.length);
        if(tr.length == 0){
            return null;
        } else if(tr.length == 1){
            return (VecTransform)tr[0];
        } else {
            CompositeTransform ct = new CompositeTransform();
            for(int k = 0 ; k < tr.length; k++){
                ct.add((VecTransform)tr[k]);
            }
            return ct;
        }
    }


    // Helpers to make code less verbose
    /**
     * Translate the source.  Equivalent to setTransform(new Translation(vec))
     * @param vec
     * @noRefGuide
     */
    public void translate(Vector3d vec) {
        setTransform(new Translation(vec));
    }

    /**
     * Translate the source.  Equivalent to setTransform(new Translation(tx,ty,tz))
     * @noRefGuide
     */
    public void translate(double tx,double ty, double tz) {
        setTransform(new Translation(tx,ty,tz));
    }

    /**
     * Scale the source.  Equivalent to setTransform(new Scale(vec))
     * @param vec
     * @noRefGuide
     */
    public void scale(Vector3d vec) {
        setTransform(new Scale(vec));
    }

    /**
     * Scale the source.  Equivalent to setTransform(new Scale(sx,sy,sz))
     * @noRefGuide
     */
    public void scale(double sx,double sy, double sz) {
        setTransform(new Scale(sx,sy,sz));
    }

    /**
     * Rotate the source.  Equivalent to setTransform(new Rotation(axis,angle))
     * @param axis
     * @param angle
     * @noRefGuide
     */
    public void rotate(Vector3d axis, double angle){
        setTransform( new Rotation(axis,angle));
    }

    /**
     * Rotate the source.  Equivalent to setTransform(new Rotation(ax,ay,az,angle))
     * @param ax  x component of rotation axis
     * @param ay  y component of rotation axis
     * @param az  z component of rotation axis
     * @param angle  rotation angle is measured in radians
     * @noRefGuide
     */
    public void rotate(double ax, double ay, double az, double angle){
        setTransform( new Rotation(ax,ay,az,angle));
    }

    /**
     * Union this datasource with another.  Equivalent to new Union(this,ds2)
     * @noRefGuide
     */
    public TransformableDataSource union(TransformableDataSource ds2) {
        return new Union(this,ds2);
    }

    /**
     * Intersect this datasource with another.  Equivalent to new Intersection(this,ds2)
     * @noRefGuide
     */
    public TransformableDataSource intersect(TransformableDataSource ds2) {
        return new Intersection(this,ds2);
    }

    /**
     * Subtract a datasource from this one.  Equivalent to new Subtraction(this,ds2)
     * @param ds2
     * @return
     * @noRefGuide
     */
    public TransformableDataSource subtract(TransformableDataSource ds2) {
        return new Subtraction(this,ds2);
    }

    /**
     * Take the opposite of this data source.
     * @return
     * @noRefGuide
     */
    public TransformableDataSource complement() {
        return new Complement(this);
    }


    /**
     * set objects material
     *
     * @noRefGuide
     * @param material Material to apply to the object after it is rendered
     */
    public void setMaterial(DataSource material){
        m_material = material; 
    }

    /**
     * @noRefGuide
       @return Material of the object
     */
    public DataSource getMaterial() {
        return m_material;
    }

    /**
     * Get the bounds of this data source.  The data source can be infinite.
     * @noRefGuide
     * @return
     */
    public Bounds getBounds() {
        if (boundsDirty) updateBounds();

        return m_bounds;
    }

    /**
     * Call to update bounds after each param change that affects bounds
     * @noRefGuide
     */
    protected void updateBounds() {
        boundsDirty = false;
    }

    /**
     * Set the bounds of this data source.  For infinite bounds use Bounds.INFINITE
     * @noRefGuide
     * @param bounds
     */
    public void setBounds(Bounds bounds) {
        this.m_bounds = bounds.clone();
        boundsDirty = false;
    }

    /**
     * @noRefGuide
     */
    public int initialize(){

        int res = RESULT_OK;

        m_transform = makeTransform();
        if(m_transform != null && m_transform  instanceof Initializable){
            res = ((Initializable)m_transform).initialize();
        }


        if(m_material != null){
            if( m_material instanceof Initializable){
                res = res | ((Initializable)m_material).initialize();
            }
            m_materialChannelsCount = m_material.getChannelsCount();
        }        
        return res;
    }

    /**
     * @noRefGuide
     */
    public abstract int getDataValue(Vec pnt, Vec data);


    /**
     * @noRefGuide
     */
    protected final int transform(Vec pnt){
        if(m_transform != null){
            return m_transform.inverse_transform(pnt, pnt);
        }
        return RESULT_OK;
    }
    
    /**
     *  @return number of channes this data source generates 
     *  
     * @noRefGuide
     */
    public int getChannelsCount(){
        return m_channelsCount + m_materialChannelsCount;
    }

    /**
       fills data with values from he material channel
     * @noRefGuide
     */
    protected int getMaterialDataValue(Vec pnt, Vec data){

        if(m_material == null)
            return RESULT_OK;

        // TODO - garbage generation !
        Vec mdata = new Vec(m_materialChannelsCount);
        
        m_material.getDataValue(pnt, mdata);

        // copy material into data 
        switch(m_materialChannelsCount){
        default: 
            for(int k = 0; k < m_materialChannelsCount; k++)
                data.v[m_channelsCount + k] = mdata.v[k];
            break;
            
        case 3: data.v[m_channelsCount + 2] = mdata.v[2]; // no break here 
        case 2: data.v[m_channelsCount + 1] = mdata.v[1]; // no break here 
        case 1: data.v[m_channelsCount ] = mdata.v[0];    // no break here 
        case 0: break;
        }

        //TODO 
        return RESULT_OK;
    }

}
