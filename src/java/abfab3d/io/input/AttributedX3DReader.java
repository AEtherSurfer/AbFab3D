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

package abfab3d.io.input;

import abfab3d.datasources.ImageColorMap;
import abfab3d.util.*;
import org.apache.commons.io.FilenameUtils;
import xj3d.filter.node.ArrayData;
import xj3d.filter.node.CommonEncodable;

import javax.vecmath.Vector3d;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import static abfab3d.util.Output.fmt;
import static abfab3d.util.Output.printf;


/**
 * Class to read collection of triangles and textures from X3D file
 *
 * @author Alan Hudson
 */
public class AttributedX3DReader implements TriangleProducer2, Transformer, DataSource {

    static final boolean DEBUG = false;

    /** Transformation to apply to positional vertices, null for none */
    private VecTransform m_transform;
    private X3DFileLoader m_fileLoader;

    /** Path to file to read from */
    private String m_path;
    private InputStream m_is;
    private String m_baseURL;

    private ImageColorMap[] textures;
    protected Bounds m_bounds = Bounds.INFINITE;

    public AttributedX3DReader(String path) {

        m_path = path;
        m_baseURL = FilenameUtils.getPath(path);
    }
    public AttributedX3DReader(InputStream is, String baseURL) {
        m_is = is;
        m_baseURL = baseURL;
    }

    /**
     * Set the transform to be applied to each triangle before passing it to TriangleCollector.
     *
     * @param transform The transform or null for identity.
     */
    public void setTransform(VecTransform transform) {
        this.m_transform = transform;
    }

    /**
       reads file and passes triangles to TriangleCollector
     */
    private void read(TriangleCollector2 out) throws IOException {

        if(m_fileLoader == null){
            m_fileLoader = new X3DFileLoader(new SysErrorReporter(SysErrorReporter.PRINT_ERRORS));

            if (m_is != null) {
                m_fileLoader.load(m_baseURL,m_is);
            } else {
                m_fileLoader.loadFile(new File(m_path));
            }
        }
                
        List<CommonEncodable> shapes = m_fileLoader.getShapes();

        Iterator<CommonEncodable> itr = shapes.iterator();
        int tex = 0;

        textures = new ImageColorMap[shapes.size()];

        while(itr.hasNext()) {

            CommonEncodable shape = itr.next();
            CommonEncodable appNode = (CommonEncodable) shape.getValue("appearance");
            CommonEncodable texNode = null;
            CommonEncodable transNode = null;
            if (appNode != null) {
                texNode = (CommonEncodable) appNode.getValue("texture");
                transNode = (CommonEncodable) appNode.getValue("textureTransform");  // not supported yet but will
            }
            CommonEncodable its = (CommonEncodable) shape.getValue("geometry");
            CommonEncodable coordNode = (CommonEncodable) its.getValue("coord");
            CommonEncodable tcoordNode = (CommonEncodable) its.getValue("texCoord");
            float[] coord = (float[]) ((ArrayData)coordNode.getValue("point")).data;
            int[] coordIndex = (int[]) ((ArrayData)its.getValue("index")).data;
            float[] tcoord = (float[]) ((ArrayData)tcoordNode.getValue("point")).data;

            addTriangles(coord, tcoord,tex,coordIndex, out);
            addTexture(tex,texNode,transNode);
            tex++;
        }
        
    }

    private void addTexture(int idx, CommonEncodable tex, CommonEncodable trans) {
        String[] url = (String[]) ((ArrayData)tex.getValue("url")).data;
        Boolean repeatX = (Boolean) tex.getValue("repeatX");
        Boolean repeatY = (Boolean) tex.getValue("repeatY");

        ImageColorMap icm = new ImageColorMap(m_baseURL + File.separator + url[0],1,1,1);
        if (repeatX != null) icm.setRepeatX(repeatX);
        if (repeatY != null) icm.setRepeatY(repeatY);

        icm.initialize();
        textures[idx] = icm;
    }

    /**
       send triangles stored as indices to TriangleCollector
     */
    private void addTriangles(float coord[],float tcoord[],int tex, int coordIndex[], TriangleCollector2 out){
        if(DEBUG)printf("%s.addTriangles(coord:%d, coordIndex:%d)\n", this,coord.length, coordIndex.length );
        // count of triangles 
        int len = coordIndex.length / 3;

        Vec
            v0 = new Vec(6),
            v1 = new Vec(6),
            v2 = new Vec(6);

        for(int i=0, idx = 0; i < len; i++ ) {
            
            int off = coordIndex[idx] * 3;
            int toff = coordIndex[idx++] * 2;
            v0.v[0] = coord[off++];
            v0.v[1] = coord[off++];
            v0.v[2] = coord[off++];

            v0.v[3] = tcoord[toff++];
            v0.v[4] = tcoord[toff++];
            v0.v[5] = tex;

            off = coordIndex[idx] * 3;
            toff = coordIndex[idx++] * 2;

            v1.v[0] = coord[off++];
            v1.v[1] = coord[off++];
            v1.v[2] = coord[off++];

            v1.v[3] = tcoord[toff++];
            v1.v[4] = tcoord[toff++];
            v1.v[5] = tex;

            off = coordIndex[idx] * 3;
            toff = coordIndex[idx++] * 2;

            v2.v[0] = coord[off++];
            v2.v[1] = coord[off++];
            v2.v[2] = coord[off++];

            v2.v[3] = tcoord[toff++];
            v2.v[4] = tcoord[toff++];
            v2.v[5] = tex;
            makeTransform(v0, v1, v2);
            out.addTri2(v0, v1, v2);
        }
        
    }

    /**
     send tiangles stored as indices to TriangleCollector
     */
    private void addTriangles(float coord[],int coordIndex[], TriangleCollector2 out){
        if(DEBUG)printf("%s.addTriangles(coord:%d, coordIndex:%d)\n", this,coord.length, coordIndex.length );
        // count of triangles
        int len = coordIndex.length / 3;

        Vec
                v0 = new Vec(6),
                v1 = new Vec(6),
                v2 = new Vec(6);

        for(int i=0, idx = 0; i < len; i++ ) {

            int off = coordIndex[idx++] * 3;

            v0.v[0] = coord[off++];
            v0.v[1] = coord[off++];
            v0.v[2] = coord[off++];

            off = coordIndex[idx++] * 3;

            v1.v[0] = coord[off++];
            v1.v[1] = coord[off++];
            v1.v[2] = coord[off++];

            off = coordIndex[idx++] * 3;
            v2.v[0] = coord[off++];
            v2.v[1] = coord[off++];
            v2.v[2] = coord[off++];
            makeTransform(v0, v1, v2);
            out.addTri2(v0, v1, v2);
        }

    }

    final void makeTransform(Vec v0, Vec v1, Vec v2){
        
        if(m_transform == null)
            return;
        
        m_transform.transform(v0,v0);
        m_transform.transform(v1,v1);
        m_transform.transform(v2,v2);
    }

    
    /**
     * interface TriangleProducer2
     */
    public boolean getTriangles2(TriangleCollector2 out) {
        try {

            read(out);
            return true;

        } catch (Exception e) {
            throw new RuntimeException(fmt("Exception while reading file:%s\n", m_path), e);
        }
    }

    /**
     *  Calculate attribute properties at (u,v, tz) point.   Think about reusing ImageColorMap.
     * @return
     */
    public DataSource getAttributeData() {
        return this; }


    /**
     * Given u,v,tz return color
     *
     * @param pnt Point where the data is calculated
     * @param dataValue - storage for returned calculated data
     * @return
     */
    public int getDataValue(Vec pnt, Vec dataValue) {
        int tz = (int)pnt.v[2];
        ImageColorMap icm = textures[tz];

        icm.getDataValue(pnt,dataValue);

        return RESULT_OK;        }

    @Override
    public int getChannelsCount() {
        return 3;
    }

    @Override
    public Bounds getBounds() {
        return m_bounds;
    }

    /**
     * Set the bounds of this data source.  For infinite bounds use Bounds.INFINITE
     * @param bounds
     */
    public void setBounds(Bounds bounds) {
        this.m_bounds = bounds.clone();
    }
}
