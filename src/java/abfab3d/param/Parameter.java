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

/**
 * A parameter to a datasource.
 *
 * @author Alan Hudson
 */
public abstract class Parameter implements Cloneable {
    /** The name of the parameter. */
    private String name;

    /** The description */
    private String desc;

    /** The value */
    protected Object value;

    public Parameter(String name, String desc) {

        this.name = name;
        this.desc = desc;
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
    public void setValue(Object value) {

        validate(value);
        
        this.value = value;
    }

    public Parameter clone() {
        try {
            return (Parameter) super.clone();
        } catch(CloneNotSupportedException cnse) { cnse.printStackTrace(); }

        return null;
    }
}
