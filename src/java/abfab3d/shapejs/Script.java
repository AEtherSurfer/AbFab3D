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

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * A ShapeJS script
 *
 * @author Alan Hudson
 */
public class Script {
    private URI m_uri;
    private EvaluatedScript m_evalScript;
    private String m_code;

    public Script(URI uri, EvaluatedScript evalScript) {
        m_uri = uri;
        m_evalScript = evalScript;
    }

    public Script(URI uri) {
        m_uri = uri;
    }

    public URI getURI() {
        return m_uri;
    }

    public void setURI(URI uri) {
        m_uri = uri;
    }

    public String getCode() {
        if (m_code != null) return m_code;
        try {
            m_code = resolveURI(m_uri);
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }

        return m_code;
    }
    public EvaluatedScript getEvaluatedScript() {
        if (m_evalScript == null) {
            ShapeJSEvaluator eval = new ShapeJSEvaluator();
            try {
                String code = resolveURI(m_uri);
                eval.prepareScript(code,null);
                m_evalScript = eval.executeScript("main");
            } catch(IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return m_evalScript;
    }

    public void setEvaluatedScript(EvaluatedScript evalScript) {
        m_evalScript = evalScript;
    }

    /**
     * Resolve the URI down to a string.
     * @param uri
     * @return
     */
    private String resolveURI(URI uri) throws IOException {
        if (uri.toString().startsWith("file:")) {
            File f = new File(uri);
            return FileUtils.readFileToString(f);
        } else {
            throw new IllegalArgumentException("Unsupported URI type");
        }
    }
}
