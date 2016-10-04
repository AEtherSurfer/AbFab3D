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
package abfab3d.param.editor;

import abfab3d.param.Parameterizable;
import abfab3d.param.Parameter;

import java.awt.*;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.*;

import static abfab3d.core.Output.printf;

/**
 * Creates an editing panel for a parameterizable
 *
 * @author Alan Hudson
 * @author Vladimir Bulatov
 */
public class ParamPanel extends Frame {

    static final int SPACE = 2;

    private java.util.List<Parameterizable> m_node;
    private static EditorFactory sm_factory;
    private Vector<ParamChangedListener> m_plisteners;
    
    private ArrayList<Editor> editors;
    private boolean closeAllowed;

    public ParamPanel(Parameterizable node) {

        super(node.getClass().getSimpleName());

        editors = new ArrayList<Editor>();
        setLayout(new GridBagLayout());
        m_node = new ArrayList<>();
        m_node.add(node);
        if(sm_factory == null)
            sm_factory = new EditorFactory();

        Component parametersPanel = makeParamPanel(m_node);
        WindowUtils.constrain(this, parametersPanel, 0,0,1,1,
                GridBagConstraints.BOTH, GridBagConstraints.NORTH, 1.,1.,2,2,2,2);

        this.pack();

        WindowManager wm = WindowManager.getInstance();
        wm.addPanel(this);
    }

    public ParamPanel(java.util.List<Parameterizable> node) {

        super(node.getClass().getSimpleName());

        editors = new ArrayList<Editor>();
        setLayout(new GridBagLayout());
        m_node = node;
        if(sm_factory == null)
            sm_factory = new EditorFactory();

        Component parametersPanel = makeParamPanel(m_node);
        JScrollPane scrollPane = new JScrollPane(parametersPanel);

        WindowUtils.constrain(this, scrollPane, 0,0,1,1,
                              GridBagConstraints.BOTH, GridBagConstraints.NORTH, 1.,1.,2,2,2,2);

        this.pack();

        WindowManager wm = WindowManager.getInstance();
        wm.addPanel(this);
    }

    public void clearParamChangedListeners() {
        if (m_plisteners != null) {
            m_plisteners.clear();
        }
    }

    /**
     * Get notification of any parameter changes from this editor
     * @param listener
     */
    public void addParamChangedListener(ParamChangedListener listener) {
        if(m_plisteners == null){
            m_plisteners = new Vector<ParamChangedListener>();
        }
        m_plisteners.add(listener);
        
        for(Editor e: editors) {
            e.addParamChangedListener(listener);
        }
    }

    public void addParamChangedListeners(Vector<ParamChangedListener> listeners) {
        if(listeners == null)
            return;
        for(int i = 0; i < listeners.size(); i++){
            addParamChangedListener(listeners.get(i));
        }
    }


    Component makeParamPanel(java.util.List<Parameterizable> nodes){

        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        int tot = 0;

        for(Parameterizable node : nodes) {
            Parameter[] param = node.getParams();
            tot += param.length;
        }

        int cnt = 0;
        for(Parameterizable node : nodes) {
            // TODO: Having some visual separator would be nice
            Parameter[] param = node.getParams();

            for (int i = 0; i < param.length; i++) {

                double hWeight = (i < tot - 1) ? (0.) : (1.);

                WindowUtils.constrain(panel, new JLabel(param[i].getName()), 0, cnt, 1, 1,
                        GridBagConstraints.NONE, GridBagConstraints.NORTHEAST, 0., hWeight, SPACE, SPACE, SPACE, 0);

                Editor editor = sm_factory.createEditor(param[i]);
                editor.addParamChangedListeners(m_plisteners);
                editors.add(editor);

                WindowUtils.constrain(panel, editor.getComponent(), 1, cnt, 1, 1,
                        GridBagConstraints.HORIZONTAL, GridBagConstraints.NORTH, 1., hWeight, SPACE, SPACE, SPACE, 0);

                cnt++;

            }
        }
        return panel;
                
    }

    public void closeWithChildren(){
        //TODO 
        // close all children 
        setVisible(false);
        
    }

    public boolean isCloseAllowed() {
        return closeAllowed;
    }

    public void setCloseAllowed(boolean val) {
        closeAllowed = val;
    }
}
