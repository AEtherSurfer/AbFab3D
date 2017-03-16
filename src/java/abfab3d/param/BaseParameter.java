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

package abfab3d.param;

import java.util.Vector;

import static abfab3d.core.Output.printf;

/**
 * A convenient base class for most Parametes
 *
 * @author Alan Hudson
 */
public abstract class BaseParameter implements Parameter {
    /** The name of the parameter. */
    protected String name;

    /** The description */
    protected String desc;

    /** The value */
    protected Object value;

    /** The default value */
    protected Object defaultValue;

    /** Method to run on change */
    protected String onChange;
    
    protected String group;
    
    /** The label */
    protected String label;

    protected boolean changed;

    protected Vector<Editor> m_editors;

	public BaseParameter(String name, String desc) {

        this.name = name;
        this.desc = desc;
    }
	
	public BaseParameter(String name, String desc, String group) {

        this.name = name;
        this.desc = desc;
        this.group = group;
    }

    public BaseParameter(String name, String desc, String group, String label) {

        this.name = name;
        this.label = label;
        this.desc = desc;
        this.group = group;
    }

    /**
     * Get the parameter type enum.
     * @return The type
     */
    public abstract ParameterType getType();

    /**
     * Validate that the object's value meets the parameters requirements.  Throws InvalidArgumentException on
     * error.
     *
     * @param val The proposed value
     */
    public abstract void validate(Object val);

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the desc
     */
    public String getDesc() {
        return desc;
    }

    /**
     * @param desc the desc to set
     */
    public void setDesc(String desc) {
        this.desc = desc;
    }

    /**
     * Get the parameters value.
     * @return
     */
    public Object getValue() {
        return value;
    }

    /**
     * Set the parameters value
     * @param value
     */
    public void setDefaultValue(Object value) {

        validate(value);
        
        this.defaultValue = value;
    }

    /**
     * Get the parameters value.
     * @return
     */
    public Object getDefaultValue() {
        return defaultValue;
    }

    /**
     * Is this parameter set at its default value
     * @return
     */
    public boolean isDefaultValue() {
        return (getDefaultValue().equals(getValue()));
    }

    /**
     * Set the parameters value
     * @param value
     */
    public void setValue(Object value) {
        validate(value);

        this.value = value;
        changed = true;
        updateUI();
    }


    /**
       
     */
    public void updateUI(){
        if(m_editors != null){
            for(int i = 0; i < m_editors.size(); i++){
                m_editors.get(i).updateUI();
            }
        }
    }

    public String getOnChange() {
        return onChange;
    }

    public void setOnChange(String onChange) {
        this.onChange = onChange;
    }
    
    public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}
	
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}

    public BaseParameter clone() {
        try {
            return (BaseParameter) super.clone();
        } catch(CloneNotSupportedException cnse) { cnse.printStackTrace(); }

        return null;
    }

    /**
     * Get the string value to use for parameter hashes, append value to existing string builder to lower garbage
     * @return
     */
    public void getParamString(StringBuilder sb) {

        if (value == null) {

            sb.append("null");

        } else if (value instanceof String) {

            sb.append((String) value);

        } else if(value instanceof Parameterizable) {

            Parameterizable par = (Parameterizable)value;
            
            sb.append(par.getParamString());

        } else {
            sb.append(value.toString());
        }
    }

    public String getParamString() {
        
        if (value == null) return "null";

        if (value instanceof String) {
            return (String) value;
        }
        if(value instanceof Parameterizable) {            
            Parameterizable par = (Parameterizable)value;            
            return par.getParamString();
        }
        return value.toString();
    }

    /**
     * Has the value changed since the last call.  This method will clear the changed state.
     * @return
     */
    public boolean hasChanged() {
        boolean ret_val = changed;
        changed = false;
        return ret_val;
    }


    public void addEditor(Editor editor){
        if(m_editors == null) {
            m_editors = new Vector<Editor>(1);
        }
        m_editors.add(editor);
    }
}
