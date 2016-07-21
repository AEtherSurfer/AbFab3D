/*****************************************************************************
 *                        Shapeways, Inc Copyright (c) 2014
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package abfab3d.io.input;

import abfab3d.grid.ArrayAttributeGridByte;
import abfab3d.grid.ArrayAttributeGridInt;
import abfab3d.core.AttributeGrid;
import abfab3d.core.Bounds;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static abfab3d.core.Output.printf;

/**
 * SVX Reader.
 *
 * Reads the Shapeways voxel transmittal format.
 *
 * @author Alan Hudson
 */
public class SVXReader {
    /** The manifest for the last load call */
    private SVXManifest mf;

    /**
     * Load a SVX file into a grid.
     *
     * @param file The zip file
     * @return
     * @throws IOException
     */
    public AttributeGrid load(String file) throws IOException {

        ZipFile zip = null;

        try {
            zip = new ZipFile(file);

            ZipEntry entry = zip.getEntry("manifest.xml");
            if (entry == null) {
                throw new IOException("Cannot find manifest.xml in top level");
            }

            InputStream is = zip.getInputStream(entry);
            mf = parseManifest(is);

            if (mf == null) {
                throw new IOException("Could not parse manifest file");
            }
            int nx = mf.getGridSizeX();
            int ny = mf.getGridSizeY();
            int nz = mf.getGridSizeZ();
            double vs = mf.getVoxelSize();
            //TODO - select right grid 
            AttributeGrid grid = new ArrayAttributeGridByte(nx, ny, nz,vs, vs);
            //AttributeGrid grid = new ArrayAttributeGridInt(nx, ny, nz,vs, vs);

            double 
                xmin = mf.getOriginX(),
                ymin = mf.getOriginY(),
                zmin = mf.getOriginZ();
            
            grid.setGridBounds(new Bounds(xmin, xmin + nx*vs, ymin, ymin + ny*vs,zmin, zmin+ny*vs));

            List<Channel> channels = mf.getChannels();
            //TODO - implement this properly 
            for(Channel chan : channels) {
                if (chan.getType().getId() == Channel.Type.DENSITY.getId() ||                    
                    chan.getType().getId() == Channel.Type.DISTANCE.getId()||
                    chan.getType().getId() == Channel.Type.RED.getId()||
                    chan.getType().getId() == Channel.Type.GREEN.getId()||
                    chan.getType().getId() == Channel.Type.BLUE.getId() ||
                    chan.getType().getId() == Channel.Type.DISTANCE_COLOR.getId()) {
                    SlicesReader sr = new SlicesReader();
                    sr.readSlices(grid,zip,chan.getSlicesPath(),0,0,mf.getGridSizeY());
                }
            }
            return grid;
        } finally {
            if (zip != null) zip.close();
        }
    }

    public SVXManifest getManifest() {
        return mf;
    }

    /**
     * Parse the manifest file
     * @param src The manifest file
     * @return
     */
    private SVXManifest parseManifest(InputStream src) throws IOException {

        String field = null;
        String val = null;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document doc = builder.parse(src);

            NodeList grid_list = doc.getElementsByTagName("grid");

            if (grid_list == null) {
                throw new IllegalArgumentException("File contains no grid element");
            }

            Element grid = (Element) grid_list.item(0);

            if (grid == null) {
                throw new IllegalArgumentException("File contains no grid element");
            }

            SVXManifest ret_val = new SVXManifest();

            field = "gridSizeX";
            val = grid.getAttribute(field);
            ret_val.setGridSizeX(Integer.parseInt(val));
            field = "gridSizeY";
            val = grid.getAttribute(field);
            ret_val.setGridSizeY(Integer.parseInt(val));
            field = "gridSizeZ";
            val = grid.getAttribute(field);
            ret_val.setGridSizeZ(Integer.parseInt(val));
            field = "voxelSize";
            val = grid.getAttribute(field);
            ret_val.setVoxelSize(Double.parseDouble(val));
            field = "originX";
            val = grid.getAttribute(field);
            if (val != null && val.length() > 0) {
                ret_val.setOriginX(Double.parseDouble(val));
            }
            field = "originY";
            val = grid.getAttribute(field);
            if (val != null && val.length() > 0) {
                ret_val.setOriginY(Double.parseDouble(val));
            }
            field = "originZ";
            val = grid.getAttribute(field);
            if (val != null && val.length() > 0) {
                ret_val.setOriginZ(Double.parseDouble(val));
            }
            field = "subvoxelBits";
            val = grid.getAttribute(field);
            ret_val.setSubvoxelBits(Integer.parseInt(val));

            field = "channels";

            NodeList channel_list = doc.getElementsByTagName("channel");

            if (channel_list == null) {
                throw new IllegalArgumentException("File contains no channels element");
            }

            int len = channel_list.getLength();

            ArrayList<Channel> clist = new ArrayList<Channel>();
            for(int i=0; i < len; i++) {
                Element channel = (Element) channel_list.item(i);

                field = "type";
                val = channel.getAttribute(field);
                String type = val;
                printf("channel type: %s\n", type);

                field = "slices";
                val = channel.getAttribute(field);
                String slices = val;

                clist.add(new Channel(Channel.Type.valueOf(type),slices));
            }

            ret_val.setChannels(clist);

            NodeList metadata_list = doc.getElementsByTagName("entry");

            if (metadata_list != null) {
                HashMap<String,String> map = new HashMap<String,String>();
                len = metadata_list.getLength();

                for(int i=0; i < len; i++) {
                    Element entry = (Element) metadata_list.item(i);
                    String key = entry.getAttribute("key");
                    String value = entry.getAttribute("value");

                    if (key != null && key.length() > 0 && value != null && value.length() > 0) {
                        map.put(key, value);
                    }
                }
                ret_val.setMetadata(map);
            }

            return ret_val;
        } catch(ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch(SAXException saxe) {
            saxe.printStackTrace();
        } catch(Exception e) {
            printf("Cannot parse field: %s  val: %s\n",field,val);
            e.printStackTrace();
        }

        return null;
    }
}
