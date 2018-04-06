/******************************************************************************
 *                        Shapeways, Inc Copyright (c) 2012-2014
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package abfab3d.grid.op;

import abfab3d.core.FontProducer;
import abfab3d.param.BaseParameterizable;
import abfab3d.param.ParamCache;
import abfab3d.param.Parameter;
import abfab3d.param.URIParameter;
import org.apache.commons.io.IOUtils;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;


/**
 * loader of font from URL
 */
public class FontLoader extends BaseParameterizable implements FontProducer {

    static final boolean DEBUG = false;
    static final boolean CACHING_ENABLED = true;

    URIParameter mp_path = new URIParameter("path", "font URI");
    Parameter m_params[] = new Parameter[]{
            mp_path,
    };

    /**
     * @param fontPath - image path
     */
    public FontLoader(String fontPath) {
        addParams(m_params);
        mp_path.setValue(fontPath);
    }

    public Font getFont() {

        prepareFont();
        return m_font;

    }

    Font m_font;

    /**
     * @Override
     */
    public void prepareFont() {

        Object co = null;
        String label = null;
        if (CACHING_ENABLED) {
            label = getDataLabel();
            co = ParamCache.getInstance().get(label);
        }
        if (co == null) {
            String path = mp_path.getValue();
            FileInputStream fis = null;

            try {
                fis = new FileInputStream(path);

                int type = (path.toLowerCase().indexOf(".ttf") != -1) ? Font.TRUETYPE_FONT : Font.TYPE1_FONT;
                m_font = Font.createFont(type, fis);
            } catch (Exception e) {
                m_font = new Font("Times New Roman", Font.PLAIN, 12);
            } finally {
                IOUtils.closeQuietly(fis);
            }
            if (CACHING_ENABLED) {
                ParamCache.getInstance().put(label, m_font);
            }
        } else {
            m_font = (Font) co;
        }
    }


} // class SystemFontLoader