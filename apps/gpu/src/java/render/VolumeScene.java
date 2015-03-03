/*****************************************************************************
 *                        Shapeways, Inc Copyright (c) 2015
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package render;

import javax.vecmath.Vector3d;

import java.util.List;
import java.util.ArrayList;

import datasources.Instruction;
import opencl.CLCodeBuffer;

import abfab3d.grid.Bounds;


/**
   wraper for volume scene parameters 

   @author Vladimir Bulatov 
 */
public class VolumeScene {

    private Vector3d worldSize = new Vector3d(1,1,1);
    private Vector3d worldCenter = new Vector3d(0,0,0);

    public String opts = "";

    public String version = null;

    // different representations of CL program 
    private List<String> progs = new ArrayList();  // list of code fragments to compile to compile     
    private String code; // single piece of code (
    private List<Instruction> instructions = null; // list of instructions to perform 
    private CLCodeBuffer codeBuffer = null;  // buffer of opcodes with structs

    public VolumeScene(String version){
        this.version = version;
    }

    public VolumeScene(List<String> progs, List<Instruction> inst, String opts,String version){

        this.progs = progs;
        this.instructions = inst;
        this.opts = opts;
        this.version = version;
    }

    public List<Instruction> getInstructions(){
        return this.instructions;
    }
    public void setInstructions(List<Instruction> instructions){
        this.instructions = instructions;
    }

    public void setPrograms(List<String> progs){
        this.progs = progs;
    }

    public void setCode(String code){
        
        progs = new ArrayList();
        progs.add(code);
        this.code = code;
        
    }

    public String getCode(){
        return code;
    }

    public List getProgs(){
        return progs;
    }

    public void setCLCode(CLCodeBuffer codeBuffer){

        this.codeBuffer = codeBuffer;

        if (codeBuffer.opcodesCount() == 0) {
            throw new IllegalArgumentException("Empty CL code");
        }
    }

   public CLCodeBuffer getCLCode(){
       return codeBuffer;
    }

    public void setWorldCenter(Vector3d center){
        this.worldCenter = new Vector3d(center);
    }

    public Vector3d getWorldCenter(){
        return worldCenter;
    }

    public void setWorldSize(Vector3d size){
        this.worldSize = new Vector3d(size);
    }

    public void setWorldBounds(Bounds bounds){
        this.worldSize = new Vector3d(bounds.getSize());
        this.worldCenter = new Vector3d(bounds.getCenter());
    }

    public Vector3d getWorldSize(){
        return worldSize;
    }
}