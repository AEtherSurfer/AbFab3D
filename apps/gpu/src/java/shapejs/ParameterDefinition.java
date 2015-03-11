package shapejs;

/**
 * Definition of a parameter for a ShapeJS script
 *
 * @author Alan Hudson
 */
public class ParameterDefinition {
    private String id;
    private String displayName;
    private String type;
    private String onChange;

    public ParameterDefinition(String id, String displayName, String type, String onChange) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.onChange = onChange;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOnChange() {
        return onChange;
    }

    public void setOnChange(String onChange) {
        this.onChange = onChange;
    }
}
