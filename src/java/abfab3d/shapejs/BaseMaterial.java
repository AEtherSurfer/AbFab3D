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
package abfab3d.shapejs;

import abfab3d.param.BaseParameterizable;
import abfab3d.param.Parameter;
import abfab3d.param.SNodeParameter;
import abfab3d.core.DataSource;

/**
 * Base implementation for all Materials
 *
 * @author Alan Hudson
 */
public abstract class BaseMaterial extends BaseParameterizable implements RenderingMaterial {
    protected SNodeParameter mp_renderingParams = new SNodeParameter("renderingParams");
    protected SNodeParameter mp_source = new SNodeParameter("source");

    private Parameter m_aparam[] = new Parameter[]{
            mp_renderingParams, mp_source
    };


    public BaseMaterial() {
        addParams(m_aparam);
    }

    public DataSource getRenderingSource(DataSource source) {
        return (DataSource) mp_source.getValue();
    }
}
