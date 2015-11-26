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

import abfab3d.mesh.AreaCalculator;
import abfab3d.util.BoundingBoxCalculator;
import abfab3d.util.Bounds;


import abfab3d.grid.AttributeGrid;
import abfab3d.grid.AttributeChannel;
import abfab3d.grid.AttributeDesc;
import abfab3d.grid.ArrayAttributeGridByte;
import abfab3d.grid.ArrayAttributeGridShort;

import static abfab3d.util.Output.printf;
import static abfab3d.util.Output.fmt;
import static abfab3d.util.Output.time;
import static abfab3d.util.Units.MM;
import static abfab3d.util.MathUtil.getMaxValue;

/**
   class to load mesh or grid from a file into a grid 

   @author Vladimir Bulatov
 */
public class GridLoader {

    static final boolean DEBUG = true;
    

    protected double m_preferredVoxelSize = 0.2*MM;

    protected int m_densityBitCount = 8;
    protected int m_distanceBitCount = 16;
    protected int m_marginsVoxels = 2;

    protected long m_maxGridSize = 1000L*1000L*1000L;

    protected AttributeGrid m_densityGridTemplate = new ArrayAttributeGridByte(1,1,1, 10*MM, 10*MM);
    protected AttributeGrid m_distanceGridTemplate = new ArrayAttributeGridShort(1,1,1, 10*MM, 10*MM);

    public static final int RASTERIZER_WAVELET = 1, RASTERIZER_DISTANCE = 2, RASTERIZER_ZBUFFER = 3;

    protected int m_densityAlgorithm = RASTERIZER_WAVELET;
    protected int m_distanceAlgorithm = RASTERIZER_DISTANCE;
    protected double m_maxOutDistance = 2*MM;
    protected double m_maxInDistance = 2*MM;
    protected double m_shellHalfThickness = 2;
    protected int m_threadCount = 1;
    protected int m_lastNumTriangles;

    public GridLoader(){
        
    }

    public static String getAlgorithmName(int algorithm){
        switch(algorithm){
        default: 
            return "Unknow Algorithm";
            
        case RASTERIZER_WAVELET: return  "RASTERIZER_WAVELET";
        case RASTERIZER_DISTANCE: return "RASTERIZER_DISTANCE"; 
        case RASTERIZER_ZBUFFER:  return "RASTERIZER_ZBUFFER";
        }
    }
    
    public void setThreadCount(int threadCount){
        m_threadCount = threadCount;
    }

    public void setMaxGridSize(long maxGridSize){

        m_maxGridSize = maxGridSize;

    }

    public void setPreferredVoxelSize(double voxelSize){
        m_preferredVoxelSize = voxelSize;
    }

    public void setMaxOutDistance(double value){
        m_maxOutDistance = value;
    }

    public void setMaxInDistance(double value){
        m_maxInDistance = value;
    }

    public void setShellHalfThickness(double value){  
        m_shellHalfThickness = value;
    }

    public void setDensityAlgorithm(int algorithm){
        switch(algorithm){
        default: throw new IllegalArgumentException(fmt("unknown Density Rasterization Algorithm: %d",algorithm));
        case RASTERIZER_ZBUFFER:
        case RASTERIZER_WAVELET:
        case RASTERIZER_DISTANCE:
            m_densityAlgorithm = algorithm;
            break;
        }
    }

    public void setDistanceAlgorithm(int algorithm){
        switch(algorithm){
        default: throw new IllegalArgumentException(fmt("unknown Density Rasterization Algorithm: %d",algorithm));
        case RASTERIZER_ZBUFFER:
        case RASTERIZER_WAVELET:
        case RASTERIZER_DISTANCE:
            m_distanceAlgorithm = algorithm;
            break;
        }
    }
    
    /**
       set bit count to be used for density grid representation 
     */
    public void setDensityBitCount(int bitCount){
        
        m_densityBitCount = bitCount;

    }

    /**
       set bit count used for distance grid 
     */
    public void setDistanceBitCount(int bitCount){
        
        m_distanceBitCount = bitCount;

    }


    /**
       open mesh on given path and return distance grid to that mesh 
       
       @return distance grid to the rasterized mesh
     */
    public AttributeGrid loadDistanceGrid(String filePath){
        
        printf("loadDistanceGrid(%s)\n",filePath);

        MeshReader reader = new MeshReader(filePath);

        Bounds bounds = getModelBounds(reader);

        long t0 = time();
        AreaCalculator ac = new AreaCalculator();  // TODO: should we make a combined bounds/area calculator?
        reader.getTriangles(ac);
        printf("Area calc: %f %d\n",ac.getArea(),(time() - t0));
        int nx = bounds.getGridWidth();
        int ny = bounds.getGridHeight();
        int nz = bounds.getGridDepth();
        double voxelSize = bounds.getVoxelSize();

        AttributeGrid distanceGrid = createDistanceGrid(bounds);
        
        switch(m_densityAlgorithm){
        default: 
            throw new IllegalArgumentException(fmt("unknown Distance Rasterization Algorithm: %d",m_distanceAlgorithm));
            
        case RASTERIZER_DISTANCE:
            {
                DistanceRasterizer rasterizer = new DistanceRasterizer(bounds, nx, ny, nz);
                
                // set params 
                rasterizer.setMaxInDistance(m_maxInDistance);
                rasterizer.setMaxOutDistance(m_maxOutDistance);                
                rasterizer.setShellHalfThickness(m_shellHalfThickness);
                rasterizer.setThreadCount(m_threadCount);
                // run rasterization
                int estimatedPoints = (int) (ac.getArea() / (voxelSize * voxelSize) * m_shellHalfThickness * 2 * 1.4);  // 40% overage to avoid allocations
                printf("Estimated points: %d\n",estimatedPoints);
                rasterizer.setEstimatePoints(estimatedPoints);
                rasterizer.getDistances(reader, distanceGrid);
            }
        }
        return distanceGrid;
    }

    /**
       open mesh on given path and return density grid for that mesh 
       
       @return distance grid to the rasterized mesh
     */
    public AttributeGrid loadDensityGrid(String filePath){
                
        printf("loadDensityGrid(%s)\n",filePath);
        
        MeshReader reader = new MeshReader(filePath);

        Bounds bounds = getModelBounds(reader);
        
        int nx = bounds.getGridWidth();
        int ny = bounds.getGridHeight();
        int nz = bounds.getGridDepth();
        double voxelSize = bounds.getVoxelSize();
        
        AttributeGrid densityGrid = createDensityGrid(bounds);

        switch(m_densityAlgorithm){
        default: 
            throw new IllegalArgumentException(fmt("unknown Density Rasterization Algorithm: %d",m_densityAlgorithm));
            
        case RASTERIZER_DISTANCE:
            {
                long t0 = time();
                DistanceRasterizer rasterizer = new DistanceRasterizer(bounds, nx, ny, nz);
                //rasterizer.setSubvoxelResolution(getMaxValue(m_densityBitCount)); 
                rasterizer.getDensity(reader, densityGrid);                
                printf("DistanceRasterizer() done %d ms\n", time() - t0);
            }
            break;

        case RASTERIZER_WAVELET:
            {
                WaveletRasterizer rasterizer = new WaveletRasterizer(bounds, nx, ny, nz);
                rasterizer.setSubvoxelResolution(getMaxValue(m_densityBitCount));        
                reader.getTriangles(rasterizer);        
                rasterizer.getRaster(densityGrid);
            }
            break;

        case RASTERIZER_ZBUFFER:
            {
                MeshRasterizer rasterizer = new MeshRasterizer(bounds, nx, ny, nz);
                rasterizer.setInteriorValue(getMaxValue(m_densityBitCount));        
                reader.getTriangles(rasterizer);        
                rasterizer.getRaster(densityGrid);
            }            
        }
        
        return densityGrid;
                
    }

    protected Bounds getModelBounds(MeshReader meshReader){

        long t0 = time();
        BoundingBoxCalculator bb = new BoundingBoxCalculator();
        meshReader.getTriangles(bb);
        m_lastNumTriangles = bb.getTriangleCount();

        if(DEBUG)printf("model read time time: %d ms\n", (time() - t0));
        Bounds modelBounds = bb.getBounds(); 
        Bounds gridBounds = modelBounds.clone();
        double voxelSize = m_preferredVoxelSize;

        gridBounds.setVoxelSize(voxelSize);
        gridBounds.expand(m_marginsVoxels*voxelSize);
        gridBounds.roundBounds();

        int ng[] = gridBounds.getGridSize();
        
        while((long) ng[0] * ng[1]*ng[2] > m_maxGridSize) {                
            //voxelSize = Math.pow(bounds.getVolume()/m_maxGridSize, 1./3);
            voxelSize *= 1.01;
            
            gridBounds.setVoxelSize(voxelSize);
            gridBounds.expand(m_marginsVoxels*voxelSize);
            gridBounds.roundBounds();
            // round up to the nearest voxel
            ng = gridBounds.getGridSize();
        } 

        printf("actual voxelSize: %7.3fmm\n",voxelSize/MM);
        int nx = gridBounds.getGridWidth();
        int ny = gridBounds.getGridHeight();
        int nz = gridBounds.getGridDepth();

        printf("  grid size: [%d x %d x %d] = %d\n", nx, ny, nz, (long) nx*ny*nz);
        printf("  grid bounds: [ %8.3f, %8.3f], [%8.3f, %8.3f], [%8.3f, %8.3f] mm; vs: %5.3f mm\n",
               gridBounds.xmin/MM, gridBounds.xmax/MM, gridBounds.ymin/MM, gridBounds.ymax/MM, gridBounds.zmin/MM, gridBounds.zmax/MM, voxelSize/MM);
        
        if(nx < 2 || ny < 2 || nz < 2) throw new IllegalArgumentException(fmt("bad grid size (%d x %d x %d)\n", nx, ny, nz));
      
        return gridBounds;
    }

    protected AttributeGrid createDistanceGrid(Bounds bounds){
        
        int nx = bounds.getGridWidth();
        int ny = bounds.getGridHeight();
        int nz = bounds.getGridDepth();
        double voxelSize = bounds.getVoxelSize();
        AttributeGrid distanceGrid = (AttributeGrid)m_distanceGridTemplate.createEmpty(nx, ny, nz, voxelSize, voxelSize);
        distanceGrid.setGridBounds(bounds);
        AttributeChannel distanceChannel = new AttributeChannel(AttributeChannel.DISTANCE, "dist", m_distanceBitCount, 0, -m_maxInDistance, m_maxOutDistance);
        distanceGrid.setAttributeDesc(new AttributeDesc(distanceChannel));        
        return distanceGrid;
    }


    protected AttributeGrid createDensityGrid(Bounds bounds){

        int nx = bounds.getGridWidth();
        int ny = bounds.getGridHeight();
        int nz = bounds.getGridDepth();
        double voxelSize = bounds.getVoxelSize();

        AttributeGrid densityGrid = (AttributeGrid)m_densityGridTemplate.createEmpty(nx, ny, nz, voxelSize, voxelSize);
        densityGrid.setGridBounds(bounds);
        AttributeChannel densityChannel = new AttributeChannel(AttributeChannel.DENSITY, "dens", m_densityBitCount, 0, 0., 1.);
        densityGrid.setAttributeDesc(new AttributeDesc(densityChannel));
        return densityGrid;

    }

}