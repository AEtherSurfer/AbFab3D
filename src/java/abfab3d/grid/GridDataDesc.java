/*****************************************************************************
 *                        Shapeways, Inc Copyright (c) 2011-2016
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/


package abfab3d.grid;

import java.util.Vector;

import abfab3d.util.Vec;

import static abfab3d.util.Output.printf;

/**
 * A description of a grid attribute with multiple data channels
 *
 * @author Vladimir Bulatov
 */
public class GridDataDesc {

    static final boolean DEBUG = false;
    static int debugCount = 0;

    AttributeMaker m_attributeMaker; 

    /**
       constructor with no data channel
       not rreally useful before adding some data  channels
     */
    public GridDataDesc(){
    }

    /**
       constructor with single data channel
     */
    public GridDataDesc(GridDataChannel channel){
        addChannel(channel);
    }

    /**
       constructor with two data channels
     */
    public GridDataDesc(GridDataChannel channel1, GridDataChannel channel2){
        addChannel(channel1);
        addChannel(channel2);
    }

    /**
       constructor with three data channels
     */
    public GridDataDesc(GridDataChannel channel1, GridDataChannel channel2, GridDataChannel channel3){
        addChannel(channel1);
        addChannel(channel2);
        addChannel(channel3);
    }

    /**
       constructor with four data channels
     */
    public GridDataDesc(GridDataChannel channel1, GridDataChannel channel2, GridDataChannel channel3, GridDataChannel channel4){
        addChannel(channel1);
        addChannel(channel2);
        addChannel(channel3);
        addChannel(channel4);
    }

    Vector<GridDataChannel> m_channels = new Vector<GridDataChannel>();

    /**
       @return count of data channels 
     */
    public int size(){
        return m_channels.size();
    }

    /**
       return total bit count needed to store data in all channels 
     */
    public int getBitCount(){
        int cnt = 0;
        for(int i = 0; i < m_channels.size(); i++){
            GridDataChannel ac = m_channels.get(i);
            cnt += ac.getBitCount();
        }
        return cnt;
    }


    /**
       @return channel of given index
     */
    public GridDataChannel getChannel(int index){
        return m_channels.get(index);
    }

    /**
       @return first found channel of given type 
     */
    public GridDataChannel getChannelWithType(String channelType){
        
        for(int i = 0; i < m_channels.size(); i++){
            GridDataChannel channel = m_channels.get(i);
            if(channel.getType().equals(channelType))
                return channel;
        }
        return null;
    }

    /**
       
     */
    public void addChannel(GridDataChannel channel){

        m_channels.add(channel);

    }


    /**
       @return converter which converts from multihgcannel double[] data into long attribute 
     */
    public AttributeMaker getAttributeMaker(){

        if(m_attributeMaker == null) {

            m_attributeMaker = new DefaultAttributeMaker(this);

        }

        return m_attributeMaker;

    }

    public String toString(){
        StringBuffer sb = new StringBuffer();
        sb.append("AttributeDesc[");
        for(int i = 0; i < m_channels.size(); i++){
            sb.append(m_channels.get(i).toString());
            if(i < m_channels.size()-1)sb.append(",");
        }
        sb.append("]");        
        return sb.toString();
    }


    /**
       @return channel which has type GridDataChannel.DENSITY 
     */
    public GridDataChannel getDensityChannel() {
        for(int i = 0; i < m_channels.size(); i++){
            GridDataChannel ac = m_channels.get(i);
            if(GridDataChannel.TYPE_DENSITY == ac.getIType())
                return ac;
        }
        return null;
    }


    /**
       
     */
    public GridDataChannel getDefaultChannel() {
        return getChannel(0);
    }


    /**

       @return default attrinute descrition with single 8 bits density channel)  
       
     */
    public static GridDataDesc getDefaultAttributeDesc(int bitCount){
        GridDataDesc at = new GridDataDesc();
        at.addChannel(new GridDataChannel(GridDataChannel.DENSITY, "density", bitCount, 0, 0., 1.));
        return at;
    }

    /**
       creates AttributeDesc with color+density channel 
       this is wrong way to use channels, it is actually composite channel 
     */
    public static GridDataDesc getDensityColor(){
        return getDensBGRcomposite();
    }

    public static GridDataDesc getDensBGRcomposite(){

        return new GridDataDesc(new GridDataChannel(GridDataChannel.DENSITY_COLOR, "density_color", 32, 0, 0., 1.));

    }

    /**
       creates data description for multichannel grid with density and color 
     */
    public static GridDataDesc getDensBGR(){

        int bitCount = 8;
        GridDataDesc at = new GridDataDesc();
        at.addChannel(new GridDataChannel(GridDataChannel.DENSITY,     "0_density", bitCount,  0,  0., 1.));
        at.addChannel(new GridDataChannel(GridDataChannel.COLOR_RED,   "1_red",     bitCount,  24, 0., 1.));
        at.addChannel(new GridDataChannel(GridDataChannel.COLOR_GREEN, "2_green",   bitCount,  16, 0., 1.));
        at.addChannel(new GridDataChannel(GridDataChannel.COLOR_BLUE,  "3_blue",    bitCount,   8, 0., 1.));
        
        return at;

    }



    /**
       convert multi component data into grid attribute 
     */
    public static class DefaultAttributeMaker implements AttributeMaker {

        GridDataChannel channels[];
        
        public DefaultAttributeMaker(GridDataDesc dataDesc){
            channels = new GridDataChannel[dataDesc.size()];
            for(int i = 0; i < channels.length; i++){
                channels[i] = dataDesc.getChannel(i);
            }
        }
        
        public long makeAttribute(Vec vec){

            long att = 0;
            for(int i = 0; i < channels.length; i++){
                long catt = channels[i].makeAtt(vec.v[i]);
                if(DEBUG){
                    if(debugCount-- >0) printf(".%8x", catt);
                }
                att |= catt;
            }
            if(DEBUG){
                if(debugCount-- >0) printf(".->att:%8x\n", att);
            }
            return att;
        }        

    } //static class DefaultAttributeMaker

} // class GridDataDesc 
