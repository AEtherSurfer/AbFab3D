package viewer;

import abfab3d.param.*;
import abfab3d.datasources.TransformableDataSource;
import abfab3d.util.DataSource;
import abfab3d.util.VecTransform;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static abfab3d.util.Output.printf;
import static java.lang.Math.PI;

/**
 * Write a datasource tree to OpenCL
 *
 * @author Alan Hudson
 */
public class OpenCLWriter {
    /** Each node has a unique id for future reference */
    private int nodeId;
    private int transId;

    /** Map of each datasource to its nodeId */
    private HashMap<Parameterizable, Integer> idMap = new HashMap<Parameterizable, Integer>();
    private HashMap<Parameterizable, Integer> transIdMap = new HashMap<Parameterizable, Integer>();

    /** Parameter excludes for special cases */
    private static final HashSet<String> boxExclude;
    private static final HashSet<String> cylinderExclude;
    private static final HashSet<String> gyroidExclude;
    private static final HashSet<String> rotationExclude;

    private NumberFormat format = new DecimalFormat("####.0####");
    static {
        boxExclude = new HashSet<String>();
        boxExclude.add("size");
        boxExclude.add("center");

        gyroidExclude = new HashSet<String>();
        gyroidExclude.add("center");
        gyroidExclude.add("period");

        cylinderExclude = new HashSet<String>();

        rotationExclude = new HashSet<String>();
        rotationExclude.add("rotation");
    }

    public OpenCLWriter() {

        nodeId = 0;
        transId = 0;
    }

    /**
     * Generate OpenCL code from the data source
     *
     * @param source
     * @return
     */
    public String generate(Parameterizable source, Vector3d scale) {
        nodeId = 0;
        idMap.clear();
        transIdMap.clear();

        StringBuilder bldr = new StringBuilder();

        bldr.append("float readShapeJS(float3 pos) {\n");
        bldr.append("\tpos = pos * (float3)(");
        bldr.append(scale.x);
        bldr.append(",");
        bldr.append(scale.y);
        bldr.append(",");
        bldr.append(scale.z);
        bldr.append(");\n");
        generate(source, "pos",bldr);
        //bldr.append("\treturn clamp(ds");
        bldr.append("\treturn ds");
        bldr.append(nodeId-1);
        //bldr.append("; \n}\n");
        bldr.append("; \n}\n");

        return bldr.toString();
    }

    private void generate(Parameterizable source, String pos, StringBuilder bldr) {
        Parameterizable trans = null;

        if (source instanceof VecTransform) {
            trans = (Parameterizable) source;
        } else if (source instanceof TransformableDataSource) {
            trans = (Parameterizable)(((TransformableDataSource) source).getTransform());
        }

        if (trans != null) {
            pos = generateTransform(trans,pos,bldr);
        }


        SNode[] children = ((SNode)source).getChildren();

        if (children != null) {
            for(SNode child : children) {
                generate((Parameterizable)child,pos,bldr);
            }
        }

        idMap.put(source, new Integer(nodeId));

        // terminal add node details

        // TODO: decide if we add a getName() field or int ID
        String class_name = source.getClass().getSimpleName();

        //printf("Node: %s\n",class_name);

        // TODO: change to map
        if (class_name.equals("Sphere")) {
            addCallParams((Parameterizable)source, bldr);

            // Add initializable params

            DoubleParameter dp = ((DoubleParameter)((Parameterizable)source).getParam("radius"));
            double radius = dp.getValue();
            boolean sign;

            if( radius < 0) {
                sign = false;
            } else {
                sign = true;
            }

            bldr.append(",");
            bldr.append(sign);

            bldr.append(",");
            bldr.append(pos);
            bldr.append(")");
            //bldr.append(",0.0,1.0)");
            bldr.append(";\n");
        } else if(class_name.equals("Box")) {
            addCallParams((Parameterizable) source, bldr, boxExclude);

            // Add initializable params

            Vector3d center = ((Vector3d) ((Parameterizable) source).getParam("center").getValue());
            Vector3d size = ((Vector3d) ((Parameterizable) source).getParam("size").getValue());

            float xmin = (float) (center.x - size.x / 2);
            float xmax = (float) (center.x + size.x / 2);
            float ymin = (float) (center.y - size.y / 2);
            float ymax = (float) (center.y + size.y / 2);
            float zmin = (float) (center.z - size.z / 2);
            float zmax = (float) (center.z + size.z / 2);


            //float box(float vs, float xmin, float xmax, float ymin, float ymax, float zmin, float zmax) {
            bldr.append("(float3)(");
            bldr.append(format.format(xmin));
            bldr.append("f,");
            bldr.append(format.format(ymin));
            bldr.append("f,");
            bldr.append(format.format(zmin));
            bldr.append("f),(float3)(");
            bldr.append(format.format(xmax));
            bldr.append("f,");
            bldr.append(format.format(ymax));
            bldr.append("f,");
            bldr.append(format.format(zmax));
            bldr.append("f),");
            bldr.append(pos);
            bldr.append(")");
            //bldr.append(",0.0,1.0)");
            bldr.append(";\n");
        } else if (class_name.equals("Cylinder")) {
            addCallParams((Parameterizable) source, bldr, cylinderExclude);

            double EPSILON = 1.e-8;

            double R0,R1; // cylinder radiuses
            boolean uniform = true;
            double h2; // cylnder's half height of
            double scaleFactor = 0;
            Vector3d center;
            Matrix3d rotation;
            Vector3d v0, v1;
            // params for non uniform cylinder
            double R01, normalR, normalY;

            final Vector3d Yaxis = new Vector3d(0, 1, 0);

        } else if (class_name.equals("Gyroid")) {
            addCallParams((Parameterizable)source, bldr,gyroidExclude);

            // Add initializable params

            double period = ((Double)((Parameterizable)source).getParam("period").getValue()).doubleValue();
            double factor = 2*PI/period;
            bldr.append(",");
            bldr.append(format.format(factor));
            bldr.append(",");
            bldr.append(pos);
            bldr.append(")");
            //bldr.append(",0.0,1.0)");
            bldr.append(";\n");
        } else if (class_name.equals("Intersection")) {
            SNode[] nchilds = ((TransformableDataSource) source).getChildren();

            if (nchilds.length == 2) {
                int data0 = idMap.get((DataSource) nchilds[0]);
                int data1 = idMap.get((DataSource) nchilds[1]);
                bldr.append("\tfloat ds");
                bldr.append(nodeId++);
                //bldr.append(" = clamp(");
                bldr.append(" = intersectionOp(");
                bldr.append("ds");
                bldr.append(data0);
                bldr.append(",");
                bldr.append("ds");
                bldr.append(data1);
                bldr.append(")");
                //bldr.append(",0.0,1.0)");
                bldr.append(";\n");
            } else {
                bldr.append("\tfloat arr");
                bldr.append(nodeId);
                bldr.append("[] = {");
                int len = nchilds.length;
                for(int i=0; i < len; i++) {
                    bldr.append("ds");
                    bldr.append(idMap.get((DataSource) nchilds[i]));
                    if (i != len - 1) bldr.append(",");
                }
                bldr.append("};\n");

                bldr.append("\tfloat ds");
                bldr.append(nodeId++);
                //bldr.append(" = clamp(");
                bldr.append(" = intersectionArr(");
                bldr.append("arr");
                bldr.append((nodeId-1));
                bldr.append(",");
                bldr.append(len);
                bldr.append(")");
                //bldr.append(",0.0,1.0)");
                bldr.append(";\n");
            }
        } else if (class_name.equals("Union")) {
            SNode[] nchilds = ((TransformableDataSource) source).getChildren();

            if (nchilds.length == 2) {
                int data0 = idMap.get((DataSource) nchilds[0]);
                int data1 = idMap.get((DataSource) nchilds[1]);
                bldr.append("\tfloat ds");
                bldr.append(nodeId++);
                bldr.append(" = unionOp(");
                bldr.append("ds");
                bldr.append(data0);
                bldr.append(",");
                bldr.append("ds");
                bldr.append(data1);
                bldr.append(");\n");
            } else {
                bldr.append("\tfloat arr");
                bldr.append(nodeId);
                bldr.append("[] = {");
                int len = nchilds.length;
                for(int i=0; i < len; i++) {
                    bldr.append("ds");
                    bldr.append(idMap.get((DataSource) nchilds[i]));
                    if (i != len - 1) bldr.append(",");
                }
                bldr.append("};\n");

                bldr.append("\tfloat ds");
                bldr.append(nodeId++);
                bldr.append(" = unionArr(");
                bldr.append("arr");
                bldr.append((nodeId-1));
                bldr.append(",");
                bldr.append(len);
                bldr.append(");\n");
            }
        } else if (class_name.equals("Subtraction")) {
            SNode[] nchilds = ((TransformableDataSource) source).getChildren();
            int data0 = idMap.get((DataSource)nchilds[0]);
            int data1 = idMap.get((DataSource)nchilds[1]);
            bldr.append("\tfloat ds");
            bldr.append(nodeId++);
            bldr.append(" = subtraction(");
            bldr.append("ds");
            bldr.append(data0);
            bldr.append(",");
            bldr.append("ds");
            bldr.append(data1);
            bldr.append(");\n");
        } else {
            // generic mapper, will not work if the function has an initializer

            addCallParams((Parameterizable)source,bldr,null);
            bldr.append(",");
            bldr.append(pos);
            bldr.append(");\n");
        }
    }

    private String generateTransform(Parameterizable source, String parent, StringBuilder bldr) {
        // TODO: decide if we add a getName() field or int ID
        String class_name = source.getClass().getSimpleName();

        //printf("Transform: %s\n",class_name);

        Parameterizable trans = null;

        if (source instanceof VecTransform) {
            trans = (Parameterizable) source;
        } else if (source instanceof TransformableDataSource) {
            trans = (Parameterizable)(((TransformableDataSource) source).getTransform());
        }

        if (trans == null) return parent;

        SNode[] tchildren = ((SNode)trans).getChildren();

        if (tchildren != null) {
            String ret = parent;
            int len = tchildren.length;
            for (int i=len-1; i >= 0; i--) {
                SNode child = tchildren[i];
                String pos;

                if (i == len -1) pos = parent;
                else {
                    pos = "trans" + (transId-1);
                    transIdMap.put((Parameterizable)child,(transId));
                }
                ret = generateTransform((Parameterizable) child, pos, bldr);
            }

            return ret;
        }

        String ret = null;
        String trans_name = trans.getClass().getSimpleName();
        if (trans_name.equals("Translation")) {
            ret = addTransform(trans, bldr, null);
            bldr.append(",");
            bldr.append(parent);
            bldr.append(");\n");
        } else if (trans_name.equals("Scale")) {
            ret = addTransform(trans, bldr, null);
            bldr.append(",");
            bldr.append(parent);
            bldr.append(");\n");
            // TODO: not sure if we need scaleFactor
        } else if (trans_name.equals("Rotation")) {
            ret = addTransform(trans, bldr, rotationExclude);

            AxisAngle4d rotation = (AxisAngle4d) trans.getParam("rotation").getValue();

            Matrix4d mat_inv = new Matrix4d();
            mat_inv.setIdentity();
            mat_inv.set(new AxisAngle4d(rotation.x, rotation.y, rotation.z, -rotation.angle));
            bldr.append(",");
            addMatrix4d(mat_inv, bldr);
            bldr.append(",");
            bldr.append(parent);
            bldr.append(");\n");
        } else if (trans_name.equals("CompositeTransform")) {
            ret = parent;
        }

        return ret;
    }

    private String addTransform(Parameterizable source, StringBuilder bldr, Set<String> exclude) {
        String ret = "trans" + (transId);

        bldr.append("\tfloat3 trans");
        bldr.append(transId++);
        bldr.append(" = ");

        bldr.append(source.getClass().getSimpleName().toLowerCase());
        bldr.append("(");

        Parameter[] params = ((Parameterizable) source).getParams();

        int len = params.length;
        boolean first = true;

        //if (len > 0) bldr.append(",");

        for(int i=0; i < len; i++) {
            if (exclude != null && exclude.contains(params[i].getName())) continue;

            ParameterType type = params[i].getType();

            switch(type) {
                case DOUBLE:
                    if (!first) bldr.append(",");
                    DoubleParameter dp = (DoubleParameter)params[i];
                    double d = dp.getValue();
                    bldr.append(format.format(d));
                    break;
                case VECTOR_3D:
                    if (!first) bldr.append(",");
                    Vector3dParameter v3dp = ((Vector3dParameter)params[i]);
                    Vector3d v3d = v3dp.getValue();
                    addVector3d(v3d, bldr);
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled parameter type");
            }
            first = false;
        }

        //bldr.append(");");

        return ret;
    }

    /**
     * Add in the call params.
     * @param source
     * @param bldr
     * @param exclude Params to exclude
     */
    private void addCallParams(Parameterizable source, StringBuilder bldr, Set<String> exclude) {
        Parameter[] params = ((Parameterizable) source).getParams();
        bldr.append("\tfloat ds");
        bldr.append(nodeId++);
        bldr.append(" = ");
        //bldr.append(" clamp(");
        bldr.append(source.getClass().getSimpleName().toLowerCase());
        bldr.append("(voxelSize,");

        // generic mapper, will not work if the function has an initializer

        int len = params.length;

        boolean first = true;
        for(int i=0; i < len; i++) {
            if (exclude != null && exclude.contains(params[i].getName())) continue;

            ParameterType type = params[i].getType();
            switch(type) {
                case DOUBLE:
                    if (!first) bldr.append(",");
                    DoubleParameter dp = (DoubleParameter)params[i];
                    double d = dp.getValue();
                    bldr.append(format.format(d));
                    break;
                case VECTOR_3D:
                    if (!first) bldr.append(",");
                    Vector3dParameter v3dp = ((Vector3dParameter)params[i]);
                    Vector3d v3d = v3dp.getValue();
                    addVector3d(v3d, bldr);
                    break;
            }
            first = false;
        }

    }

    private void addCallParams(Parameterizable source, StringBuilder bldr) {
        addCallParams(source,bldr,null);
    }

    private void addVector3d(Vector3d vec, StringBuilder sb) {
        sb.append("(float3)(");
        sb.append(format.format(vec.x)).append(",");
        sb.append(format.format(vec.y)).append(",");
        sb.append(format.format(vec.z));
        sb.append(")");
    }

    private void addMatrix4d(Matrix4d mat, StringBuilder sb) {
        sb.append("(float16)(");
        sb.append(format.format(mat.m00)).append(",");
        sb.append(format.format(mat.m01)).append(",");
        sb.append(format.format(mat.m02)).append(",");
        sb.append(format.format(mat.m03)).append(",");
        sb.append(format.format(mat.m10)).append(",");
        sb.append(format.format(mat.m11)).append(",");
        sb.append(format.format(mat.m12)).append(",");
        sb.append(format.format(mat.m13)).append(",");
        sb.append(format.format(mat.m20)).append(",");
        sb.append(format.format(mat.m21)).append(",");
        sb.append(format.format(mat.m22)).append(",");
        sb.append(format.format(mat.m23)).append(",");
        sb.append(format.format(mat.m30)).append(",");
        sb.append(format.format(mat.m31)).append(",");
        sb.append(format.format(mat.m32)).append(",");
        sb.append(format.format(mat.m33));
        sb.append(")");
    }
}
