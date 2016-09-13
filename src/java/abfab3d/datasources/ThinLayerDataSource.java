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


import abfab3d.core.GridProducer;
import abfab3d.core.ResultCodes;
import abfab3d.core.Vec;

import abfab3d.core.Bounds;

import abfab3d.core.DataSource;
import abfab3d.core.GridDataChannel;
import abfab3d.core.GridDataDesc;
import abfab3d.core.AttributeGrid;
import abfab3d.core.AttributePacker;

import abfab3d.param.DoubleParameter;
import abfab3d.param.IntParameter;
import abfab3d.param.ObjectParameter;
import abfab3d.param.SNodeParameter;
import abfab3d.param.Parameter;

import abfab3d.grid.ArrayAttributeGridInt;
import abfab3d.grid.ArrayAttributeGridShort;
import abfab3d.grid.SparseGridInt;
import abfab3d.grid.util.GridUtil;

import abfab3d.grid.op.GridMaker;


import static abfab3d.core.Units.MM;
import static abfab3d.core.Output.printf;
import static abfab3d.core.Output.fmt;
import static abfab3d.core.Output.time;
import static abfab3d.core.MathUtil.clamp;
import static abfab3d.core.MathUtil.lerp3;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.abs;


/**
 * makes pre-calculated distance from data source
 * distance in thin layer near surface is calculated on high res grid
 * distances further from surface are calculated on low res grid 
 *  high res grid only stores difference between real data and approximated obtained from lowres grid
 * 
 * @author Vladimir Bulatov
 */
public class ThinLayerDataSource extends TransformableDataSource {

    static final int BYTE_BITS = 8;
    static final int SHORT_BITS = 16;
    static final int INT_BITS = 32;

    static final double HALF = 0.5;
    static final int HI_GRID_BLOCK_ORDER = 3;
    static final boolean DEBUG = true;
    static int debugCount = 1000;
    protected boolean m_useCombinedGrid = false;

    SNodeParameter  mp_source = new SNodeParameter("source","source to be used for calculations", null, ShapesFactory.getInstance());
    ObjectParameter  mp_bounds = new ObjectParameter("bounds","bounds of data source", null);
    DoubleParameter  mp_layerThickness = new DoubleParameter("layerThickness","thickness of surface layer", 2*MM);
    DoubleParameter  mp_hiVoxelSize = new DoubleParameter("hiVoxelSize","size of hi resolution voxel", 0.1*MM);
    IntParameter  mp_lowVoxelFactor = new IntParameter("lowVoxelFactor","relative size of low res grid voxel (should be odd)", 5);
    
    Parameter m_aparam[] = new Parameter[]{
        mp_source,
        mp_bounds,
        mp_layerThickness,
        mp_hiVoxelSize,
        mp_lowVoxelFactor,
    };

    protected Bounds m_bounds;
    DataSource m_source = null;
       
    AttributeGrid m_lowGrid; // grid of low res data
    SparseGridInt m_hiGrid; // grid of high res difference data
    GridInterpolator m_lowGridData;
    GridInterpolator m_hiGridData;
    CombinedInterpolator m_combinedGridData;

    int m_thinLayerCount = 0;

    public ThinLayerDataSource(){
        super.addParams(m_aparam);
    }
    /**
       constructs DataSource from the given grid
     */
    public ThinLayerDataSource(DataSource source, Bounds bounds){

        super.addParams(m_aparam);

        mp_source.setValue(source);
        mp_bounds.setValue(bounds);
    }
    

    /**

       @noRefGuide            
     */
    public int initialize(){

        super.initialize();
        m_source = (DataSource)mp_source.getValue();
        initializeChild(m_source);
        // voxel size of high res grid 
        double hiVoxel = mp_hiVoxelSize.getValue();
        double layerThickness = mp_layerThickness.getValue();
        // voxel size of low res grid 
        int blockSize = mp_lowVoxelFactor.getValue();
        blockSize |= 1;// make it odd
        double lowVoxel = blockSize*hiVoxel;
        m_bounds = (Bounds)mp_bounds.getValue();
        m_bounds.roundSize(lowVoxel);
        
        //m_lowGrid = new ArrayAttributeGridShort(m_bounds, lowVoxel, lowVoxel);
        m_lowGrid = new ArrayAttributeGridInt(m_bounds, lowVoxel, lowVoxel);
        
        //m_lowGrid = new SparseGridInt(m_bounds,  lowVoxel);
        GridMaker gm = new GridMaker();
        double maxDist = m_bounds.getSizeMax()/2;
        AttributePacker lowPacker = new UnsignedPacker(INT_BITS, -maxDist, maxDist);
        gm.setAttributePacker(lowPacker);
        m_lowGridData = new GridInterpolator(m_lowGrid, lowPacker);
        
        printf("blockSize: %d\n", blockSize);
        gm.setSource(m_source);
        gm.makeGrid(m_lowGrid);
        //GridUtil.printSliceAttribute(m_lowGrid, m_lowGrid.getDepth()/2, "%4x ");
        int nx = m_lowGrid.getWidth();
        int ny = m_lowGrid.getHeight();
        int nz = m_lowGrid.getDepth();
        Vec data = new Vec(3);
        int layerVoxelCount = 0;
        // max distance to store in hiGrid 
        double maxHiDistance = 0.5*mp_layerThickness.getValue();
        // min distance on lowGrid to trigger refinement 
        //double minLowDistance =  max(lowVoxel*1.8, maxHiDistance); 
        double minLowDistance =  max(lowVoxel, maxHiDistance); 
        //AttributePacker hiPacker = new SignedShortPacker(maxHiDistance);
        AttributePacker hiPacker;
        if(m_useCombinedGrid) {
            // store complete value in the hiGrid
            hiPacker = new UnsignedPacker(BYTE_BITS, -(maxHiDistance+hiVoxel), maxHiDistance);
        } else {
            // experimental bounds - error in low res grid is bounded by 1.5*lowVoxel
            hiPacker = new SignedBytePacker(1.5*lowVoxel);
            // hiPacker = new SignedIntPacker(maxHiDistance);
        }

        m_hiGrid = new SparseGridInt(m_bounds,  HI_GRID_BLOCK_ORDER, hiVoxel);
        m_hiGridData = new GridInterpolator(m_hiGrid, hiPacker);
        printf("hiGrid: [%d x %d x %d]\n",m_hiGrid.getWidth(), m_hiGrid.getHeight(), m_hiGrid.getDepth());
        long t0 = time();
        
        for(int y = 0; y < ny; y++){
            for(int x = 0; x < nx; x++){
                for(int z = 0; z < nz; z++){
                    lowPacker.getData(m_lowGrid.getAttribute(x,y,z), data);
                    if(abs(data.v[0]) < minLowDistance){
                        fillHiGridBlock(hiPacker, x, y, z, blockSize, maxHiDistance);
                        layerVoxelCount++;
                    }
                }
            }
        }
        if(m_useCombinedGrid) 
            m_combinedGridData = new CombinedInterpolator(m_hiGrid, hiPacker, m_lowGridData);

        printf("hiGrid renderTime: %d ms\n",(time()-t0));
        printf("hiGrid memory: %7.3f MB\n",m_hiGrid.getDataSize()*1.e-6);
        printf("layerVoxelCount:%d layerVolume: %7.3f\n", layerVoxelCount, (double)layerVoxelCount/(nx*ny*nz));
        printf("thin layer count:%d thin layerVolume: %7.3f\n", m_thinLayerCount, (double)m_thinLayerCount/(m_hiGrid.getWidth()* m_hiGrid.getHeight()*m_hiGrid.getDepth()));

        return ResultCodes.RESULT_OK;

    }

    void fillHiGridBlock(AttributePacker packer, int bx, int by, int bz, int blockSize, double maxDistance){

        int x0 = bx * blockSize;
        int x1 = x0 + blockSize;
        int y0 = by * blockSize;
        int y1 = y0 + blockSize;
        int z0 = bz * blockSize;
        int z1 = z0 + blockSize;
        //printf("fill block(%d %d %d: %d %d %d %d %d %d )\n", bx, by, bz, x0, x1, y0, y1, z0, z1);
        int 
            xc = (x0 + x1)/2,
            yc = (y0 + y1)/2,
            zc = (z0 + z1)/2;

        Vec pnt = new Vec(3);
        Vec sourceData = new Vec(1);
        Vec gridData = new Vec(1);
        double vs = m_hiGrid.getVoxelSize();
        
        double xmin = m_bounds.xmin + vs/2;
        double ymin = m_bounds.ymin + vs/2;
        double zmin = m_bounds.zmin + vs/2;

        for(int y = y0; y < y1; y++){
            double yy = ymin + vs*y;
            for(int x = x0; x < x1; x++){
                double xx = xmin + vs*x;
                for(int z = z0; z < z1; z++){
                    double zz = zmin + vs*z;
                    pnt.v[0] = xx;
                    pnt.v[1] = yy;
                    pnt.v[2] = zz;
                    m_source.getDataValue(pnt, sourceData);                    
                    if(abs(sourceData.v[0]) < maxDistance){
                        m_thinLayerCount++;
                        m_lowGridData.getValue(pnt, gridData);
                        if(!m_useCombinedGrid){
                            // save the difference                         
                            sourceData.v[0] -= gridData.v[0];
                        } 
                        long hiatt = packer.makeAttribute(sourceData);
                        //if( y == yc && z == zc){
                        //    printf("(%8.5f %8.5f %8.5f) -> %8.5f: %x \n", pnt.v[0]/MM, pnt.v[1]/MM, pnt.v[2]/MM,sourceData.v[0]/MM, hiatt);
                        //}
                        m_hiGrid.setAttribute(x,y,z,hiatt);
                    }
                }
            }
        }
    }

    int m_dataType = 0;
    
    public void setDataType(int type){
        m_dataType = type;
    }

    public void setUseCombined(boolean value){
        m_useCombinedGrid = value;
    }

    /**
       return base value of this data source (without transforms and material applied)
       @noRefGuide            
     */
    public int getBaseValue(Vec pnt, Vec data) {

        switch(m_dataType){

        default: 
        case 0:
            m_lowGridData.getValue(pnt, data);
            break;

        case 1: 
            m_hiGridData.getValue(pnt, data);
            break;

        case 2: 
            if(m_useCombinedGrid){ 
                m_combinedGridData.getValue(pnt, data);
            } else {
                m_lowGridData.getValue(pnt, data);
                double dd = data.v[0];
                m_hiGridData.getValue(pnt, data);
                data.v[0] += dd;
            }
            break;
        case 3: 
            break;
        }
        return ResultCodes.RESULT_OK;        
    }
       

    /**
       simple interpolator for single grid 
     */
    static public class GridInterpolator {

        AttributeGrid m_grid;
        AttributePacker m_packer;
        double m_xmin, m_xmax, m_ymin, m_ymax, m_zmin, m_zmax;
        double m_xmin1, m_xmax1, m_ymin1, m_ymax1, m_zmin1, m_zmax1;
        double m_factor;
        int m_nxmax, m_nymax, m_nzmax;

        public GridInterpolator(AttributeGrid grid, AttributePacker packer){
            
            m_grid = grid;
            m_packer = packer;
            Bounds bounds = grid.getGridBounds();
            double vs = grid.getVoxelSize();
            double vs2 = vs/2;
            m_xmin = bounds.xmin;
            m_xmax = bounds.xmax;
            m_ymin = bounds.ymin;
            m_ymax = bounds.ymax;
            m_zmin = bounds.zmin;
            m_zmax = bounds.zmax;

            m_xmin1 = m_xmin + vs2;
            m_ymin1 = m_ymin + vs2;
            m_zmin1 = m_zmin + vs2;
            m_xmax1 = m_xmax - vs2;
            m_ymax1 = m_ymax - vs2;
            m_zmax1 = m_zmax - vs2;


            m_nxmax = grid.getWidth() - 1;
            m_nymax = grid.getHeight() - 1;
            m_nzmax = grid.getDepth() - 1;            
            m_factor = 1./vs;
        }

        public void getValue(Vec pnt, Vec data){

            double v[] = pnt.v;

            // coord in grid units
            double 
                x = (clamp(v[0],m_xmin1, m_xmax1) - m_xmin)*m_factor - HALF,
                y = (clamp(v[1],m_ymin1, m_ymax1) - m_ymin)*m_factor - HALF,
                z = (clamp(v[2],m_zmin1, m_zmax1) - m_zmin)*m_factor - HALF;
            int ix = clamp((int)x, 0, m_nxmax);
            int iy = clamp((int)y, 0, m_nymax);
            int iz = clamp((int)z, 0, m_nzmax);
            int ix1 = clamp(ix+1,0,m_nxmax);
            int iy1 = clamp(iy+1,0,m_nymax);
            int iz1 = clamp(iz+1,0,m_nzmax);
            
            double 
                dx = x - ix,
                dy = y - iy,
                dz = z - iz;
            double v000, v100, v110, v010, v001, v101, v111, v011;
            long 
                a000 = m_grid.getAttribute(ix,  iy,  iz),
                a100 = m_grid.getAttribute(ix1, iy,  iz),
                a010 = m_grid.getAttribute(ix,  iy1, iz),
                a110 = m_grid.getAttribute(ix1, iy1, iz),
                a001 = m_grid.getAttribute(ix,  iy,  iz1),
                a101 = m_grid.getAttribute(ix1, iy,  iz1),
                a011 = m_grid.getAttribute(ix,  iy1, iz1),
                a111 = m_grid.getAttribute(ix1, iy1, iz1);
                
            m_packer.getData(a000, data); v000 = data.v[0];
            m_packer.getData(a100, data); v100 = data.v[0];
            m_packer.getData(a110, data); v110 = data.v[0];
            m_packer.getData(a010, data); v010 = data.v[0];
            m_packer.getData(a001, data); v001 = data.v[0];
            m_packer.getData(a101, data); v101 = data.v[0];
            m_packer.getData(a111, data); v111 = data.v[0];
            m_packer.getData(a011, data); v011 = data.v[0];

            data.v[0] = lerp3(v000,v100,v010,v110,v001,v101,v011,v111,dx, dy, dz);            
        }
    } // class GridInterpolator 


    /**
       interpolator for combined grids hiGrid and lowGrid 
     */
    static public class CombinedInterpolator {

        AttributeGrid m_grid;
        AttributePacker m_packer;
        double m_xmin, m_xmax, m_ymin, m_ymax, m_zmin, m_zmax;
        double m_xmin1, m_xmax1, m_ymin1, m_ymax1, m_zmin1, m_zmax1;
        double m_factor;
        double m_voxelSize;
        int m_nxmax, m_nymax, m_nzmax;
        GridInterpolator lowGridData;

        public CombinedInterpolator(AttributeGrid grid, AttributePacker packer, GridInterpolator lowGridData){
            
            m_grid = grid;
            m_packer = packer;
            Bounds bounds = grid.getGridBounds();
            double vs = grid.getVoxelSize();
            double vs2 = vs/2;
            m_xmin = bounds.xmin;
            m_xmax = bounds.xmax;
            m_ymin = bounds.ymin;
            m_ymax = bounds.ymax;
            m_zmin = bounds.zmin;
            m_zmax = bounds.zmax;

            m_xmin1 = m_xmin + vs2;
            m_ymin1 = m_ymin + vs2;
            m_zmin1 = m_zmin + vs2;
            m_xmax1 = m_xmax - vs2;
            m_ymax1 = m_ymax - vs2;
            m_zmax1 = m_zmax - vs2;


            m_nxmax = grid.getWidth() - 1;
            m_nymax = grid.getHeight() - 1;
            m_nzmax = grid.getDepth() - 1;            
            m_voxelSize = vs;
            m_factor = 1./vs;

            this.lowGridData = lowGridData;

        }

        /**
           get value conbined from hi and low grid 
           1) access hiGrid data in 8 surrounding voxels 
           if al voxels are not empty - return linear interpolated value 
           if all voxels are empty - uses low grid to provide interpolation 
           if any non empty voxel exist uses low grid to provide values for empty voxels 
        */
        public void getValue(Vec pnt, Vec data){

            double v[] = pnt.v;
            double 
                x = v[0], 
                y = v[1], 
                z = v[2],
                x1 = x + m_voxelSize,
                y1 = y + m_voxelSize,
                z1 = z + m_voxelSize;

            
            // coord in grid units
            double 
                gx = (clamp(x,m_xmin1, m_xmax1) - m_xmin)*m_factor - HALF,
                gy = (clamp(y,m_ymin1, m_ymax1) - m_ymin)*m_factor - HALF,
                gz = (clamp(z,m_zmin1, m_zmax1) - m_zmin)*m_factor - HALF;
            int ix = clamp((int)gx, 0, m_nxmax);
            int iy = clamp((int)gy, 0, m_nymax);
            int iz = clamp((int)gz, 0, m_nzmax);
            int ix1 = clamp(ix+1,0,m_nxmax);
            int iy1 = clamp(iy+1,0,m_nymax);
            int iz1 = clamp(iz+1,0,m_nzmax);
            
            double 
                dx = gx - ix,
                dy = gy - iy,
                dz = gz - iz;
            double v000, v100, v110, v010, v001, v101, v111, v011;

            boolean hasData = false;
            long a000 = m_grid.getAttribute(ix,  iy,  iz);  hasData = hasData || (a000 != 0);
            long a100 = m_grid.getAttribute(ix1, iy,  iz);  hasData = hasData || (a100 != 0);
            long a010 = m_grid.getAttribute(ix,  iy1, iz);  hasData = hasData || (a010 != 0);
            long a110 = m_grid.getAttribute(ix1, iy1, iz);  hasData = hasData || (a110 != 0);
            long a001 = m_grid.getAttribute(ix,  iy,  iz1); hasData = hasData || (a001 != 0);
            long a101 = m_grid.getAttribute(ix1, iy,  iz1); hasData = hasData || (a101 != 0);
            long a011 = m_grid.getAttribute(ix,  iy1, iz1); hasData = hasData || (a011 != 0);
            long a111 = m_grid.getAttribute(ix1, iy1, iz1); hasData = hasData || (a111 != 0);
                
            if(!hasData){
                lowGridData.getValue(pnt, data);
                return;
            }
            setCoord(pnt, ix, iy, iz);   v000 = getCombinedValue(a000,pnt,data);
            setCoord(pnt, ix1,iy, iz);   v100 = getCombinedValue(a100,pnt,data);
            setCoord(pnt, ix, iy1,iz);   v010 = getCombinedValue(a010,pnt,data);
            setCoord(pnt, ix1,iy1,iz);   v110 = getCombinedValue(a110,pnt,data);
            setCoord(pnt, ix, iy, iz1);  v001 = getCombinedValue(a001,pnt,data);
            setCoord(pnt, ix1,iy, iz1);  v101 = getCombinedValue(a101,pnt,data);
            setCoord(pnt, ix, iy1,iz1);  v011 = getCombinedValue(a011,pnt,data);
            setCoord(pnt, ix1,iy1,iz1);  v111 = getCombinedValue(a111,pnt,data);
            
            data.v[0] = lerp3(v000,v100,v010,v110,v001,v101,v011,v111,dx, dy, dz);            
        }
        
        void setCoord(Vec pnt, int ix, int iy, int iz){
            pnt.v[0] = (ix + HALF)*m_voxelSize + m_xmin;
            pnt.v[1] = (iy + HALF)*m_voxelSize + m_ymin;
            pnt.v[2] = (iz + HALF)*m_voxelSize + m_zmin;
        }

        /**
           return unpacked data or data from low res grid 
         */
        double getCombinedValue(long att, Vec pnt, Vec data){
            if(att != 0) {
                // non empty data
                m_packer.getData(att, data);
                return data.v[0];
            } else {
                // voxel data is empty - take missing data from low res grid 
                lowGridData.getValue(pnt, data);
                return data.v[0];
            }
        }

    } // class CombinedInterpolator 

    /**
       packer into unsigned value 
     */
    static class UnsignedPacker implements AttributePacker {
        
        int m_bitCount;
        long m_mask;

        double m_minValue, m_maxValue;
        double m_factor;
        double m_invfactor;

        UnsignedPacker(int bitCount, double minValue, double maxValue){

            m_bitCount = bitCount;
            m_mask = (1L << (bitCount)) - 1;
            m_minValue = min(minValue, maxValue);
            m_maxValue = max(minValue, maxValue);
            m_factor = m_mask/(m_maxValue - m_minValue);
            m_invfactor = 1./m_factor;
            //printf("UnsignedPacker(minValue:%8.5f, maxValue:%8.5f,m_factor:%8.5f, mask: %x)\n", m_minValue, m_maxValue, m_factor, m_mask);
        }

        public long makeAttribute(Vec data){

            return (long) ((clamp(data.v[0], m_minValue, m_maxValue) - m_minValue)*m_factor+0.5);
                
        }
        
        /**
           converts attribute into vector of double data 
           @param attribute 
           @param data values of data stored in attribute 
        */
        public void getData(long att, Vec data){
            data.v[0] = (att & m_mask) * m_invfactor + m_minValue;
        }
        
        /**
           bit count used by this packer to pack attribute 
        */
        public int getBitCount(){
            return m_bitCount;
        }
        
    } // class UnsignedPacker 

    /**
       packs values into signed int 
     */
    static class SignedIntPacker implements AttributePacker {
        
        long m_mask;
        int m_bitCount;
        double m_maxValue;
        double m_factor;
        double m_invfactor;
        
        SignedIntPacker(double maxValue){

            m_maxValue = maxValue;
            m_bitCount = INT_BITS;
            m_mask = (1L << (m_bitCount)) - 1;
            m_factor = m_mask/m_maxValue;
            m_invfactor = 1./m_factor;
            //printf("SignedIntPacker(maxValue:%8.5f,m_factor:%8.5f)\n", m_maxValue, m_factor);
        }

        public long makeAttribute(Vec data){
            
            return ((long)((clamp(data.v[0], -m_maxValue, m_maxValue))*m_factor+0.5)) & m_mask;
            
        }
        
        /**
           converts attribute into vector of double data 
           @param attribute 
           @param data values of data stored in attribute 
        */
        public void getData(long att, Vec data){
            data.v[0] = ((long)((int)(att & m_mask))) * m_invfactor;
        }        
        /**
           bit count used by this packer to pack attribute 
        */
        public int getBitCount(){
            return m_bitCount;
        }        
    } // class  SignedIntPacker

    /**
       packs values into signed int 
     */
    static class SignedBytePacker implements AttributePacker {
        
        long m_mask;
        int m_bitCount;
        double m_maxValue;
        double m_factor;
        double m_invfactor;
        
        SignedBytePacker(double maxValue){

            m_maxValue = maxValue;
            m_bitCount = BYTE_BITS;
            m_mask = (1L << (m_bitCount)) - 1;
            m_factor = m_mask/m_maxValue;
            m_invfactor = 1./m_factor;
        }

        public long makeAttribute(Vec data){
            
            return ((long)((clamp(data.v[0], -m_maxValue, m_maxValue))*m_factor+0.5)) & m_mask;
            
        }
        
        /**
           converts attribute into vector of double data 
           @param attribute 
           @param data values of data stored in attribute 
        */
        public void getData(long att, Vec data){
            data.v[0] = ((long)((byte)(att & m_mask))) * m_invfactor;
        }        
        /**
           bit count used by this packer to pack attribute 
        */
        public int getBitCount(){
            return m_bitCount;
        }        
    } // class  SignedBytePacker

    /**
       packs values into signed int 
     */
    static class SignedShortPacker implements AttributePacker {
        
        long m_mask;
        int m_bitCount;
        double m_maxValue;
        double m_factor;
        double m_invfactor;
        
        SignedShortPacker(double maxValue){

            m_maxValue = maxValue;
            m_bitCount = SHORT_BITS;
            m_mask = (1L << (m_bitCount)) - 1;
            m_factor = m_mask/m_maxValue;
            m_invfactor = 1./m_factor;
        }

        public long makeAttribute(Vec data){
            
            return ((long)((clamp(data.v[0], -m_maxValue, m_maxValue))*m_factor+0.5)) & m_mask;
            
        }
        
        /**
           converts attribute into vector of double data 
           @param attribute 
           @param data values of data stored in attribute 
        */
        public void getData(long att, Vec data){
            data.v[0] = ((long)((short)(att & m_mask))) * m_invfactor;
        }        
        /**
           bit count used by this packer to pack attribute 
        */
        public int getBitCount(){
            return m_bitCount;
        }        
    } // class  SignedShortPacker
    
} // class ThinLayerDataSource 
