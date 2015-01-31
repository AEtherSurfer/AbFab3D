package render;

import abfab3d.param.Parameter;
import abfab3d.param.ParameterType;
import abfab3d.param.Parameterizable;

import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * OpenCL instruction
 *
 * @author Alan Hudson
 */
public class Instruction {
    private static final int CHUNK_SIZE = 5;

    private int op;
    
    /** Float Params */
    private int fcount;
    private float[] fparams;
    
    /** Integer Params */
    private int icount;
    private int[] iparams;
    
    /** Float Vector Params */
    private int fvcount;
    private float[] fvparams;
    
    /** Boolean Params */
    private int bcount;
    private boolean[] bparams;
    
    /** Matrix Params */
    private int mcount;
    private float[] mparams;
    
    private ParameterType types[];
    private int tcount;

    private static final HashMap<String, Integer> opcodes;

    static {
        opcodes=new HashMap<String, Integer>();
        opcodes.put("sphere",0);
        opcodes.put("box",1);
        opcodes.put("gyroid",2);
        opcodes.put("intersection",3);
        opcodes.put("union",4);
        opcodes.put("torus",5);
        opcodes.put("intersectionStart",6);
        opcodes.put("intersectionMid",7);
        opcodes.put("intersectionEnd",8);
        opcodes.put("unionStart",9);
        opcodes.put("unionMid",10);
        opcodes.put("unionEnd",11);
        opcodes.put("subtractionStart",12);
        opcodes.put("subtractionEnd",13);

        opcodes.put("reset",1000);
        opcodes.put("scale",1001);
        opcodes.put("translation",1002);
        opcodes.put("rotation",1003);
    }


    public Instruction() {
        fcount = 0;
        icount = 0;
        fvcount = 0;
        bcount = 0;

        tcount = 0;
        types = new ParameterType[CHUNK_SIZE];
    }

    public Instruction(String op) {
        this();

        setOpCode(getOpCode(op));
    }

    public void setOpCode(int code) {
        op = code;
    }

    public void addFloat(float f) {
        if (fparams == null) {
            fparams = new float[CHUNK_SIZE];
        } else if (fcount == fparams.length) {
            float[] na = new float[fparams.length + CHUNK_SIZE];
            for(int i=0; i < fparams.length; i++) {
                na[i] = fparams[i];
            }

            fparams = na;
        }

        fparams[fcount++] = f;

        addType(ParameterType.FLOAT);
    }
    public void addFloatVector3(float[] f) {
        if (fvparams == null) {
            fvparams = new float[CHUNK_SIZE * 3];
        } else if (fvcount * 3 == fvparams.length) {
            float[] na = new float[fvparams.length + CHUNK_SIZE * 3];
            for(int i=0; i < fvparams.length; i++) {
                na[i] = fvparams[i];
            }

            fvparams = na;
        }

        fvparams[fvcount*3] = f[0];
        fvparams[fvcount*3+1] = f[1];
        fvparams[fvcount*3+2] = f[2];
        fvcount++;
        addType(ParameterType.VECTOR_3D);
    }

    public void addFloatVector3(Vector3d f) {
        if (fvparams == null) {
            fvparams = new float[CHUNK_SIZE * 3];
        } else if (fvcount * 3 == fvparams.length) {
            float[] na = new float[fvparams.length + CHUNK_SIZE * 3];
            for(int i=0; i < fvparams.length; i++) {
                na[i] = fvparams[i];
            }

            fvparams = na;
        }

        fvparams[fvcount*3] = (float)f.x;
        fvparams[fvcount*3+1] = (float)f.y;
        fvparams[fvcount*3+2] = (float)f.z;
        fvcount++;
        addType(ParameterType.VECTOR_3D);
    }

    public void addMatrix(float[] f) {
        if (mparams == null) {
            mparams = new float[CHUNK_SIZE * 16];
        } else if (mcount * 16 == mparams.length) {
            float[] na = new float[mparams.length + CHUNK_SIZE * 3];
            for(int i=0; i < mparams.length; i++) {
                na[i] = mparams[i];
            }

            mparams = na;
        }

        mparams[mcount*16] = f[0];
        mparams[mcount*16+1] = f[1];
        mparams[mcount*16+2] = f[2];
        mparams[mcount*16+3] = f[3];
        mparams[mcount*16+4] = f[4];
        mparams[mcount*16+5] = f[5];
        mparams[mcount*16+6] = f[6];
        mparams[mcount*16+7] = f[7];
        mparams[mcount*16+8] = f[8];
        mparams[mcount*16+9] = f[9];
        mparams[mcount*16+10] = f[10];
        mparams[mcount*16+11] = f[11];
        mparams[mcount*16+12] = f[12];
        mparams[mcount*16+13] = f[13];
        mparams[mcount*16+14] = f[14];
        mparams[mcount*16+15] = f[15];
        mcount++;

        addType(ParameterType.MATRIX_4D);
    }

    public void addMatrix(Matrix4d f) {
        if (mparams == null) {
            mparams = new float[CHUNK_SIZE * 16];
        } else if (mcount * 16 == mparams.length) {
            float[] na = new float[mparams.length + CHUNK_SIZE * 3];
            for(int i=0; i < mparams.length; i++) {
                na[i] = mparams[i];
            }

            mparams = na;
        }

        mparams[mcount*16] = (float)f.m00;
        mparams[mcount*16+1] = (float)f.m01;
        mparams[mcount*16+2] = (float)f.m02;
        mparams[mcount*16+3] = (float)f.m03;
        mparams[mcount*16+4] = (float)f.m10;
        mparams[mcount*16+5] = (float)f.m11;
        mparams[mcount*16+6] = (float)f.m12;
        mparams[mcount*16+7] = (float)f.m13;
        mparams[mcount*16+8] = (float)f.m20;
        mparams[mcount*16+9] = (float)f.m21;
        mparams[mcount*16+10] = (float)f.m22;
        mparams[mcount*16+11] = (float)f.m23;
        mparams[mcount*16+12] = (float)f.m30;
        mparams[mcount*16+13] = (float)f.m31;
        mparams[mcount*16+14] = (float)f.m32;
        mparams[mcount*16+15] = (float)f.m33;
        
        mcount++;
        addType(ParameterType.MATRIX_4D);
    }

    public void addInt(int val) {
        if (iparams == null) {
            iparams = new int[CHUNK_SIZE];
        } else if (icount == iparams.length) {
            int[] na = new int[iparams.length + CHUNK_SIZE];
            for(int i=0; i < iparams.length; i++) {
                na[i] = iparams[i];
            }

            iparams = na;
        }

        iparams[icount++] = val;
        addType(ParameterType.INTEGER);
    }

    public void addBoolean(boolean val) {
        if (bparams == null) {
            bparams = new boolean[CHUNK_SIZE];
        } else if (bcount == bparams.length) {
            boolean[] na = new boolean[bparams.length + CHUNK_SIZE];
            for(int i=0; i < bparams.length; i++) {
                na[i] = bparams[i];
            }

            bparams = na;
        }

        bparams[bcount++] = val;
        addType(ParameterType.BOOLEAN);
    }

    /**
     * Compact all arrays to used sizes
     */
    public void compact() {
        if (fparams != null && fparams.length > fcount) {
            float[] na = new float[fcount];
            for(int i=0; i < fcount; i++) {
                na[i] = fparams[i];
            }
            fparams = na;
        }
        if (iparams != null && iparams.length > icount) {
            int[] na = new int[icount];
            for(int i=0; i < icount; i++) {
                na[i] = iparams[i];
            }
            iparams = na;
        }
        if (bparams != null && bparams.length > bcount) {
            boolean[] na = new boolean[bcount];
            for(int i=0; i < bcount; i++) {
                na[i] = bparams[i];
            }
            bparams = na;
        }
        if (fvparams != null && fvparams.length > fvcount * 3) {
            float[] na = new float[fvcount * 3];
            for(int i=0; i < fvcount * 3; i++) {
                na[i] = fvparams[i];
            }
            fvparams = na;
        }

        if (mparams != null && mparams.length > mcount * 16) {
            float[] na = new float[mcount * 16];
            for(int i=0; i < mcount * 16; i++) {
                na[i] = mparams[i];
            }
            mparams = na;
        }

        if (types != null && types.length > tcount) {
            ParameterType[] na = new ParameterType[tcount];
            for(int i=0; i < tcount; i++) {
                na[i] = types[i];
            }
            types = na;
        }
    }

    public int getFloatCount() {
        return fcount;
    }

    public int getIntCount() {
        return icount;
    }

    public int getFloatVectorCount() {
        return fvcount;
    }

    public int getMatrixCount() {
        return mcount;
    }

    public int getBooleanCount() {
        return bcount;
    }

    public float getFloatParam(int idx) {
        return fparams[idx];
    }

    public void getFloatParams(float[] arr, int start) {
        int idx = start;
        for(int i=0; i < fcount; i++) {
            arr[idx++] = fparams[i];
        }
    }

    public int getIntParam(int idx) {
        return iparams[idx];
    }

    public void getIntParams(int[] arr, int start) {
        int idx = start;
        for(int i=0; i < icount; i++) {
            arr[idx++] = iparams[i];
        }
    }

    public void getFloatVector(int idx, Vector3d dest) {
        dest.x = fvparams[idx*3];
        dest.y = fvparams[idx*3+1];
        dest.z = fvparams[idx*3+2];
    }

    public void getFloatVectorParams(float[] arr, int start) {
        int idx = start * 3;
        for(int i=0; i < fvcount * 3; i++) {
            arr[idx++] = fvparams[i];
        }
    }

    public void getMatrix(int idx, Matrix4d dest) {
        dest.m00 = mparams[idx*16];
        dest.m01 = mparams[idx*16+1];
        dest.m02 = mparams[idx*16+2];
        dest.m03 = mparams[idx*16+3];
        dest.m10 = mparams[idx*16+4];
        dest.m11 = mparams[idx*16+5];
        dest.m12 = mparams[idx*16+6];
        dest.m13 = mparams[idx*16+7];
        dest.m20 = mparams[idx*16+8];
        dest.m21 = mparams[idx*16+9];
        dest.m22 = mparams[idx*16+10];
        dest.m23 = mparams[idx*16+11];
        dest.m30 = mparams[idx*16+12];
        dest.m31 = mparams[idx*16+13];
        dest.m32 = mparams[idx*16+14];
        dest.m33 = mparams[idx*16+15];
    }

    public void getMatrixParams(float[] arr, int start) {
        int idx = start * 16;
        for(int i=0; i < mcount * 16; i++) {
            arr[idx++] = mparams[i];
        }
    }

    public boolean getBooleanParam(int idx) {
        return bparams[idx];
    }

    public void getBooleanParams(boolean[] arr, int start) {
        int idx = start;
        for(int i=0; i < bcount; i++) {
            arr[idx++] = bparams[i];
        }
    }

    public int getOpCode() {
        return op;
    }

    private void addType(ParameterType type) {
        if (tcount >= types.length) {
            ParameterType[] na = new ParameterType[types.length + CHUNK_SIZE];
            for (int i = 0; i < types.length; i++) {
                na[i] = types[i];
            }

            types = na;
        }

        types[tcount++] = type;
    }

    public ParameterType[] getTypes() {
        return types;
    }

    public int getTypeCount() {
        return tcount;
    }

    /**
     * Add in the call params.
     * @param source
     * @param inst
     * @param exclude Params to exclude
     */
    public static void addCallParams(Parameterizable source, Instruction inst, Set<String> exclude) {
        Parameter[] params = ((Parameterizable) source).getParams();

        // generic mapper, will not work if the function has an initializer

        int len = params.length;

        for(int i=0; i < len; i++) {
            if (exclude != null && exclude.contains(params[i].getName())) continue;

            addCallParam(params[i],inst);
        }

    }

    public static void addCallParam(Parameter param, Instruction inst) {
        ParameterType type = param.getType();
        Object value = param.getValue();

        addCallParam(type, value, inst);
    }

    /**
     * Add in the call params.
     * @param inst
     */
    public static void addCallParam(ParameterType type, Object value, Instruction inst) {
        switch(type) {
            case INTEGER:
                inst.addInt((Integer) value);
                break;
            case DOUBLE:
                inst.addFloat(((Double)value).floatValue());
                break;
            case VECTOR_3D:
                Vector3d v3d = (Vector3d) value;
                float[] vec = new float[3];
                vec[0] = (float) v3d.x;
                vec[1] = (float) v3d.y;
                vec[2] = (float) v3d.z;
                inst.addFloatVector3(vec);
                break;
            case MATRIX_4D:
                Matrix4d m4d = (Matrix4d) value;
                float[] mvec = new float[16];
                mvec[0] = (float) m4d.m00;
                mvec[1] = (float) m4d.m01;
                mvec[2] = (float) m4d.m02;
                mvec[3] = (float) m4d.m03;
                mvec[4] = (float) m4d.m10;
                mvec[5] = (float) m4d.m11;
                mvec[6] = (float) m4d.m12;
                mvec[7] = (float) m4d.m13;
                mvec[8] = (float) m4d.m20;
                mvec[9] = (float) m4d.m21;
                mvec[10] = (float) m4d.m22;
                mvec[11] = (float) m4d.m23;
                mvec[12] = (float) m4d.m30;
                mvec[13] = (float) m4d.m31;
                mvec[14] = (float) m4d.m32;
                mvec[15] = (float) m4d.m33;

                inst.addMatrix(mvec);
                break;
            case BOOLEAN:
                inst.addBoolean((Boolean)value);
                break;
            default:
                throw new IllegalArgumentException("Parameter type not mapped: " + type);
        }
    }

    public static void addCallParams(Parameterizable source, Instruction inst) {
        addCallParams(source,inst,null);
    }

    public static int getOpCode(String name) {
        Integer op = opcodes.get(name);

        if (op ==  null) {
            throw new IllegalArgumentException("Undefined op: " + name);
        }

        return op.intValue();
    }

    public static String convertOpToFunction(int op) {
        for(Map.Entry<String,Integer> entry : opcodes.entrySet()) {
            if (entry.getValue().equals(op)) {
                return entry.getKey();
            }
        }

        return null;
    }


}