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
package abfab3d.shapejs;

import javax.vecmath.Vector3d;

import abfab3d.core.Bounds;
import abfab3d.core.Color;
import abfab3d.core.MaterialType;
import abfab3d.datasources.ImageColorMap;
import abfab3d.param.BaseParameterizable;
import abfab3d.param.Parameterizable;

import static abfab3d.core.Units.MM;
import static abfab3d.core.Output.printf;
import static abfab3d.core.Output.fmt;

/**
 * ShapeJS script scene available to script author
 *
 * @author Alan Hudson
 */
public class Scene extends BaseParameterizable {

    final static boolean DEBUG = false;

    final public static Scene DEFAULT_SHAPE = new Scene("DEFAULT_SHAPE");
    final public static double DEFAULT_VOXEL_SIZE = 0.1*MM;
    final public static Vector3d DEFAULT_SIZE = new Vector3d(0.1,0.1,0.1);
    final public static Vector3d DEFAULT_CENTER = new Vector3d(0.,0.,0.);
    final public static Bounds DEFAULT_BOUNDS = new Bounds(0, 0.1, 0, 0.1, 0, 0.1);    
    final public static double DEFAULT_SMOOTHING_WIDTH = 0.5;
    final public static double DEFAULT_ERROR_FACTOR = 0.1;
    final public static int DEFAULT_MAX_PARTS_COUNT = Integer.MAX_VALUE;

    final public static int SINGLE_MATERIAL = 0;
    final public static int MULTI_MATERIAL = 1;
    final public static int COLOR_MATERIAL = 2;

    public static enum LightingRig {
        TWO_POINT,THREE_POINT, THREE_POINT_COLORED,
    };

    final public static LightingRig DEFAULT_LIGHTING_RIG = LightingRig.THREE_POINT_COLORED;

    protected Parameterizable m_source = null;
    protected Bounds m_bounds = DEFAULT_BOUNDS;
    protected double m_errorFactor = DEFAULT_ERROR_FACTOR;
    protected double m_smoothingWidth = DEFAULT_SMOOTHING_WIDTH;
    protected double m_minShellVolume = 0;
    protected int m_maxPartsCount = DEFAULT_MAX_PARTS_COUNT;
    protected String m_name = "ShapeJS";
    protected SceneLights m_lights = new SceneLights();

    // we support up to 4 materials
    // material[0] is base material
    // material[1,2,3] correspond to material channels
    protected SceneMaterials m_materials = new SceneMaterials();
    protected RenderingParams m_renderingParams;
    protected Parameterizable m_renderingSource;
    protected LightingRig m_lightingRig = DEFAULT_LIGHTING_RIG;
    protected Camera camera = new SimpleCamera();
    protected boolean m_userSetLights = false;
    protected ImageColorMap environmentMap;

    protected MaterialType m_materialType = MaterialType.SINGLE_MATERIAL;

    
    public Scene(String name){
        m_name = name;
        initParams();
    }

    public Scene(Parameterizable source, Bounds bounds){

        this(source, bounds, bounds.getVoxelSize());
        //if(DEBUG)printf("Shape(%s, source:%s, bounds:%s)\n", this, source, bounds);
    }

    public Scene(Parameterizable source, Bounds bounds, double voxelSize){
        //if(DEBUG)printf("Shape(%s, source:%s bounds:%s vs:%7.5f)\n", this, source, bounds, voxelSize);
        m_source = source;
        m_bounds = bounds;
        bounds.setVoxelSize(voxelSize);

        initParams();
        initRendering();
    }

    private void initParams() {
        addParams(m_lights.getParams());
        addParams(m_materials.getParams());
    }

    /**
     * Setup a default rendering setup
     */
    protected void initRendering() {
        setLightingRig(m_lightingRig);
    }

    public static Light[] getColoredLighting() {
        Light[] lights = new Light[3];

        double intensity = 0.9;
        lights[0] = new Light(new Vector3d(10,0,20),new Color(1,0,0),0.1,intensity);
        lights[1] = new Light(new Vector3d(10,10,20),new Color(0,1,0),0,intensity);
        lights[2] = new Light(new Vector3d(0,10,20),new Color(0,0,1),0,intensity);
        return lights;
    }

    public static Light[] getTwoPointLighting() {
        double a0 = 0.4;
        double a1 = 0.8;

        Light[] lights = new Light[2];        
        lights[0] = new Light(new Vector3d(20,0,20),new Color(a0,a0,a0),0.1,1);
        lights[1] = new Light(new Vector3d(-10,0,20),new Color(a1,a1,a1),0,1);
        return lights;
    }

    public static Light[] getThreePointLighting() {

        double ambient = 0;

        double lscale = 1;
        double key = 0.8 * lscale;
        double fill = 0.4 * lscale;
        double rim = 0.25 * lscale;

        double intensity = 0.9;
        Color key_light_color = new Color(key,key,key );  // high noon
        Color fill_light_color = new Color(fill,fill,fill);  // high noon
        Color rim_light_color = new Color(rim,rim,rim);  // high noon
/*
        Light key_light = new Light(new Vector3d(10,-10,1000),key_light_color,ambient,1);
        Light fill_light = new Light(new Vector3d(1000,100,100),fill_light_color,0,1);
        Light rim_light = new Light(new Vector3d(-1000,900,-200),rim_light_color,0,1);

        Light key_light = new Light(new Vector3d(10,-10,20),key_light_color,ambient,intensity);
        Light fill_light = new Light(new Vector3d(10,10,20),fill_light_color,0,intensity);
        Light rim_light = new Light(new Vector3d(-10,90,-20),rim_light_color,0,intensity);
*/        
        Light key_light = new Light(new Vector3d(10,-10,100),key_light_color,ambient,1);
        Light fill_light = new Light(new Vector3d(1000,100,100),fill_light_color,0,1);
        Light rim_light = new Light(new Vector3d(-1000,900,-200),rim_light_color,0,1);
        
        Light lights[] = new Light[] {key_light,fill_light,rim_light};

        for(int i=0; i < lights.length; i++) {
            lights[i].setCastShadows(true);
            lights[i].setSamples(4);
            lights[i].setRadius(50);
        }
        return lights;
    }

    public void setCamera(Camera val) {
        this.camera = val;
    }

    public Camera getCamera() {
        return camera;
    }

    public void setName(String val) {
        m_name = val;
    }

    public String getName() {
        return m_name;
    }

    public Bounds getBounds(){
        return m_bounds;
    }
    public void setBounds(Bounds bounds) {
        m_bounds = bounds.clone();
    }

    public Parameterizable getSource(){
        return m_source;
    }

    public void setSource(Parameterizable source) {
        m_source = source;
    }

    public void setVoxelSize(double voxelSize) {
        m_bounds.setVoxelSize(voxelSize);
    }
    public double getVoxelSize() {
        return m_bounds.getVoxelSize();
    }

    public void setMeshErrorFactor(double value){
        m_errorFactor = value;
    }

    public double getMeshErrorFactor(){
        return m_errorFactor;
    }

    public void setMeshSmoothingWidth(double value){
        m_smoothingWidth = value;
    }

    public double getMeshSmoothingWidth(){
        return m_smoothingWidth;
    }

    public void setMinShellVolume(double value){
        printf("setMinShellVolume.  minShell: %f  hashcode: %d\n",value,hashCode());
        printf("shell: ");

        m_minShellVolume = value;
    }

    public double getMinShellVolume(){
        printf("getMinShellVolume.  minShell: %f  hashcode: %d\n",m_minShellVolume,hashCode());

        return m_minShellVolume;
    }

    public int getMaxPartsCount() {
        return m_maxPartsCount;
    }

    public void setMaxPartsCount(int maxPartsCount) {
        m_maxPartsCount = maxPartsCount;
    }

    public void setRenderingMaterial(RenderingMaterial mat) {

        m_materials.setRenderingMaterial(mat);
        buildParams();
    }


    /**
       set rendering material for given index 
     */
    public void setMaterial(int index, RenderingMaterial mat) {
        m_materials.setMaterial(index, mat);
    }

    /**
       return type of material used in scene 
     */
    public MaterialType getMaterialType() {
        return m_materialType;
    }

    /**
     set type of material used in scene
     SINGLE_MATERIAL
     MULTI_MATERIAL
     COLOR_MATERIAL
     */
    public void setMaterialType(MaterialType type) {
        if (type == null) throw new IllegalArgumentException("Type cannot be null");
        m_materialType = type;
    }
    /**
       return base material 
     */
    public RenderingMaterial getRenderingMaterial() {
        return m_materials.getRenderingMaterial();
    }

    /*
    public RenderingMaterial[] getRenderingMaterials() {
        return m_materials;
    }
    */

    public void setLights(Light[] lights) {
        m_userSetLights = true;
        m_lights.setLights(lights);

        buildParams();
    }

    public SceneLights getLights() {
        return m_lights;
    }

    public SceneMaterials getRenderingMaterials() {
        return m_materials;
    }

    public void setLightingRig(LightingRig rig) {
        // Ignore lighting rig if the user explicitly sets lights
        if (m_userSetLights) return;
        m_lightingRig = rig;

        switch(m_lightingRig) {
            case TWO_POINT:
                m_lights.setLights(getColoredLighting());
                break;
            case THREE_POINT_COLORED:
                m_lights.setLights(getColoredLighting());
                break;
            case THREE_POINT:
                m_lights.setLights(getThreePointLighting());
                break;
            default:
                throw new IllegalArgumentException("Unhandled lighting rig: " + rig);
        }

    }

    public LightingRig getLightingRig() {
        return m_lightingRig;
    }

    /**
     * Set an alternate source to use for rendering.  This source might apply embossing to the surface to approximate
     * a rendering technique.
     *
     * @param source
     */
    public void _setRenderingSource(Parameterizable source) {
        m_renderingSource = source;
    }

    public Parameterizable getRenderingSource() {
        return m_renderingSource;
    }

    public void _setRenderingParams(RenderingParams params) {
        m_renderingParams = params;
    }

    public RenderingParams _getRenderingParams() {
        return m_renderingParams;
    }

    public void setEnvironmentMap(ImageColorMap map) {
        environmentMap = map;
    }

    public ImageColorMap getEnvironmentMap() {
        return environmentMap;
    }

    /**
     * Build up params from underlying children
     */
    private void buildParams() {
        // TODO: Dodgy make real
        clearParams();
        addParams(m_lights.getParams());
        addParams(m_materials.getParams());


    }
    public String toString(){
        return fmt("Shape(\"%s\",%s, vs: %7.5f)", m_name, m_bounds, m_bounds.getVoxelSize());
    }

}