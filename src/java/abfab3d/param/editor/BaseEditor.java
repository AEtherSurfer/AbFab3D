package abfab3d.param.editor;

/**
 * Base code for all parameter editors
 *
 * @author Alan Hudson
 */
public abstract class BaseEditor implements Editor {
    private ParamChangedListener m_listener;

    /**
     * Get notification of any parameter changes from this editor
     * @param l
     */
    public void addChangeListener(ParamChangedListener l) {
        m_listener = l;
    }
}
