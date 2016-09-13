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
package abfab3d.datasources;

import javax.vecmath.Vector3d;

import abfab3d.core.TriangleProducer;
import abfab3d.core.AttributedTriangleProducer;
import abfab3d.core.DataSource;
import abfab3d.core.ResultCodes;
import abfab3d.core.Vec;
import abfab3d.core.Bounds;
import abfab3d.core.TriangleCollector;
import abfab3d.core.AttributeGrid;


import abfab3d.util.BoundingBoxCalculator;
import abfab3d.util.PointSetCoordArrays;
import abfab3d.util.MeshRasterizer;

import abfab3d.util.TriangleMeshSurfaceBuilder;

import abfab3d.grid.ArrayAttributeGridInt;
import abfab3d.grid.GridMask;

import abfab3d.grid.op.PointSetShellBuilder;
import abfab3d.grid.op.ClosestPointIndexer;
import abfab3d.grid.op.ClosestPointIndexerMT;


import abfab3d.datasources.TransformableDataSource;

import abfab3d.param.Parameter;
import abfab3d.param.DoubleParameter;
import abfab3d.param.ObjectParameter;
import abfab3d.param.BooleanParameter;
import abfab3d.param.IntParameter;

import static java.lang.Math.floor;
import static java.lang.Math.min;
import static java.lang.Math.abs;
import static abfab3d.core.Units.MM;
import static abfab3d.core.Output.printf;
import static abfab3d.core.Output.time;


/**
   
   represent distance to triangle mesh as data source 
   during initialization it does 
   1) rasterizes the mesh into set of points on its surface on the grid
   2) generates interior/exterio grid using z-buffer voxelization 
   3) initialies thin layer of voxel around surface to index closest point on surface 
   4) sweeps thin layer to the whole grid of closes point indixes

   during calculation it find the closest voxel to the given point and uses 
   that voxel closest point index to calculate the actual euclidean distance  

   
   @author Vladimir Bulatov
   
 */
public class DistanceToMeshDataSource extends TransformableDataSource {

    static final boolean DEBUG = true;
    static final double DEFAULT_VOXEL_SIZE = 0.2*MM;
    static final int INTERIOR_VALUE = 1; // interior value for interior grid 
    
    static final public int INTERPOLATION_BOX = IndexedDistanceInterpolator.INTERPOLATION_BOX;
    static final public int INTERPOLATION_LINEAR = IndexedDistanceInterpolator.INTERPOLATION_LINEAR;
    static final public int INTERPOLATION_COMBINED = IndexedDistanceInterpolator.INTERPOLATION_COMBINED;
    
    static final double MAX_DISTANCE_UNDEFINED = 1.e10;
    
    ObjectParameter mp_meshProducer = new ObjectParameter("meshProducer", "mesh producer", null);
    DoubleParameter mp_voxelSize = new DoubleParameter("voxelSize", "size of rasterization voxel", DEFAULT_VOXEL_SIZE);
    DoubleParameter mp_surfaceVoxelSize = new DoubleParameter("surfaceVoxelSize", "surface voxel size", 1.);
    DoubleParameter mp_margins = new DoubleParameter("margins", "width of margins around model", DEFAULT_VOXEL_SIZE);
    DoubleParameter mp_maxDistance = new DoubleParameter("maxDistance", "max distance to calculate", MAX_DISTANCE_UNDEFINED);
    DoubleParameter mp_shellHalfThickness = new DoubleParameter("shellHalfThickness", "shell half thickness (in voxels)", 1);
    BooleanParameter mp_attributeLoading = new BooleanParameter("attributeLoading", "Load attribute data",false);
    BooleanParameter mp_useMultiPass = new BooleanParameter("useMultiPass", "use Multi Pass algorithm in distance sweeping",false);
    IntParameter mp_interpolationType = new IntParameter("interpolationType", "0 - nearest neighbor, 1 - best neighbor",INTERPOLATION_BOX);

    protected long m_maxGridSize = 1000L*1000L*1000L;
    protected long m_minGridSize = 1000L;

    Parameter[] m_aparams = new Parameter[]{
        mp_meshProducer,
        mp_voxelSize,
        mp_margins,
        mp_maxDistance, 
        mp_shellHalfThickness,
        mp_attributeLoading,
        mp_useMultiPass,
        mp_surfaceVoxelSize,
        mp_interpolationType,
    };

    
    public DistanceToMeshDataSource(TriangleProducer meshProducer){

        super.addParams(m_aparams);

        mp_meshProducer.setValue(meshProducer);

    }
    
    IndexedDistanceInterpolator m_distCalc;

    /**
       initialization
       makes all disntace calculations here 
     */
    public int initialize(){
        
        super.initialize();
        long t0 = time();
        TriangleProducer producer = (TriangleProducer)mp_meshProducer.getValue();

        int threadCount = 8;
        // find mesh bounds
        Bounds gridBounds = calculateGridBounds(producer);
        super.setBounds(gridBounds);

        double maxDistance = mp_maxDistance.getValue();
        if(maxDistance == MAX_DISTANCE_UNDEFINED)         
            maxDistance = gridBounds.getSizeMax()/2;        

        //TODO - use it 
        int interpolationType = mp_interpolationType.getValue();

        int gridDim[] = gridBounds.getGridSize();
        
        // z-buffer rasterizer to get mesh interior 
        MeshRasterizer interiorRasterizer = new MeshRasterizer(gridBounds, gridDim[0],gridDim[1],gridDim[2]);
        interiorRasterizer.setInteriorValue(INTERIOR_VALUE);
                
        Bounds surfaceBounds = gridBounds.clone();
        double voxelSize = gridBounds.getVoxelSize();

        // surface voxel ratio = volumeVoxelSize/surfaceVoxelSize
        int svratio = Math.max(1, (int)Math.round(1./mp_surfaceVoxelSize.getValue()));
        // surface voxel size 
        double svs = gridBounds.getVoxelSize()/svratio;
        surfaceBounds.setVoxelSize(svs);
        if((svratio & 1) == 0){ // even ratio
            double shift = svs/2;
            // shift grid of surface rasterization by half voxel to align centers of surface grid with center of voliume grid
            surfaceBounds.translate(shift,shift,shift);
        }

        // triangles rasterizer         
        TriangleMeshSurfaceBuilder surfaceBuilder = new TriangleMeshSurfaceBuilder(surfaceBounds);        
        
        surfaceBuilder.initialize();

        // aggregator otf 2 triaangle collectors 
        TC2 tc2 = new TC2(interiorRasterizer, surfaceBuilder);
        
        // get mesh from producer 
        producer.getTriangles(tc2);

        int pcount = surfaceBuilder.getPointCount();
        if(DEBUG)printf("DistanceToMeshDataSource pcount: %d\n", pcount);
        double pnts[][] = new double[3][pcount];
        surfaceBuilder.getPoints(pnts[0], pnts[1], pnts[2]);
        
        // builder of shell around rasterized points 
        PointSetShellBuilder shellBuilder = new PointSetShellBuilder(); 
        shellBuilder.setShellHalfThickness(mp_shellHalfThickness.getValue());
        shellBuilder.setPoints(new PointSetCoordArrays(pnts[0], pnts[1], pnts[2]));
        shellBuilder.setShellHalfThickness(mp_shellHalfThickness.getValue());

        // create index grid 
        AttributeGrid indexGrid = createIndexGrid(gridBounds);
        // thicken surface points into thin layer 
        shellBuilder.execute(indexGrid);

        // create interior grid 
        AttributeGrid interiorGrid = new GridMask(gridDim[0],gridDim[1],gridDim[2]);        

        interiorRasterizer.getRaster(interiorGrid);
        printf("surface building time: %d ms\n", time() - t0);

        double maxDistanceVoxels = maxDistance/voxelSize;
       
        if(maxDistanceVoxels > mp_shellHalfThickness.getValue()){
            t0 = time();
            // spread distances to the whole grid 
            ClosestPointIndexer.getPointsInGridUnits(indexGrid, pnts[0], pnts[1], pnts[2]);
            if(mp_useMultiPass.getValue()){
                ClosestPointIndexerMT.PI3_multiPass_MT(pnts[0], pnts[1], pnts[2], maxDistanceVoxels, indexGrid, threadCount);
            } else {
                ClosestPointIndexerMT.PI3_MT(pnts[0], pnts[1], pnts[2], maxDistanceVoxels, indexGrid, threadCount);
            }        
            ClosestPointIndexer.getPointsInWorldUnits(indexGrid, pnts[0], pnts[1], pnts[2]);
            printf("distance sweeping time: %d ms\n", time() - t0);
        }
        m_distCalc = new IndexedDistanceInterpolator(pnts, indexGrid, interiorGrid, maxDistance);
        return ResultCodes.RESULT_OK;
    }


    protected AttributeGrid createIndexGrid(Bounds bounds){

        //TODO - select here appropriate grid to create 
        
        return new ArrayAttributeGridInt(bounds, bounds.getVoxelSize(),bounds.getVoxelSize());

    }
        
    protected Bounds calculateGridBounds(TriangleProducer producer){
        
        BoundingBoxCalculator bb = new BoundingBoxCalculator();
        producer.getTriangles(bb);
        
        double margins = mp_margins.getValue();
        double voxelSize = mp_voxelSize.getValue();
        Bounds meshBounds = bb.getBounds(); 
        Bounds gridBounds = meshBounds.clone();
        gridBounds.expand(margins);
        int ng[] = gridBounds.getGridSize(voxelSize);
        long voxels = (long) ng[0] * ng[1]*ng[2];
        double gridVolume = gridBounds.getVolume();
        if(voxels > m_maxGridSize) {
            voxelSize = Math.pow(gridVolume /m_maxGridSize, 1./3);
        } else if (voxels < m_minGridSize){
            voxelSize = Math.pow(gridVolume/m_minGridSize, 1./3);
        }
        gridBounds.setVoxelSize(voxelSize);
        if(DEBUG){
            printf("DistanceToMeshDataSource()  grid:[%d x %d x %d] voxelSize: %7.3f mm\n",ng[0],ng[1],ng[2],voxelSize/MM);
            printf("DistanceToMeshDataSource()  meshBounds: (%7.3f %7.3f; %7.3f %7.3f; %7.3f %7.3f) mm)\n",
                   meshBounds.xmin/MM,meshBounds.xmax/MM,meshBounds.ymin/MM,meshBounds.ymax/MM,meshBounds.zmin/MM,meshBounds.zmax/MM);
            printf("DistanceToMeshDataSource()  gridBounds: (%7.3f %7.3f; %7.3f %7.3f; %7.3f %7.3f) mm)\n",
                   gridBounds.xmin/MM,gridBounds.xmax/MM,gridBounds.ymin/MM,gridBounds.ymax/MM,gridBounds.zmin/MM,gridBounds.zmax/MM);
        }                
        return gridBounds;
    }


    /**
       implementation method of TransformableDataSource 
     */
    public int getBaseValue(Vec pnt, Vec data){
        
        m_distCalc.getDataValue(pnt, data);

        return ResultCodes.RESULT_OK;
    }


    /**
       aggregator ffor 2 triangle collector 
     */
    static class TC2 implements TriangleCollector {
        
        TriangleCollector tc1;
        TriangleCollector tc2;

        TC2(TriangleCollector tc1, TriangleCollector tc2){
            this.tc1 = tc1;
            this.tc2 = tc2;
            
        }
        
        /**
           interface of triangle consumer 
        */
        public boolean addTri(Vector3d v0,Vector3d v1,Vector3d v2){
            
            tc1.addTri(v0, v1, v2);
            tc2.addTri(v0, v1, v2);
            return true;
            
        }
    } // class TC2 



} // class DistanceToMeshDataSource