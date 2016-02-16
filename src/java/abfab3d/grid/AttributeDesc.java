/*****************************************************************************
 *                        Shapeways, Inc Copyright (c) 2011
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

import static abfab3d.util.Output.fmt;

/**
 * A description of a grid attribute
 *
 * @author Vladimir Bulatov
 */
public class AttributeDesc  {

    AttributeMaker m_attributeMaker; 

    public AttributeDesc(){
    }

    public AttributeDesc(AttributeChannel channel){
        addChannel(channel);
    }

    public AttributeDesc(AttributeChannel channel1, AttributeChannel channel2){
        addChannel(channel1);
        addChannel(channel2);
    }

    public AttributeDesc(AttributeChannel channel1, AttributeChannel channel2, AttributeChannel channel3){
        addChannel(channel1);
        addChannel(channel2);
        addChannel(channel3);
    }

    public AttributeDesc(AttributeChannel channel1, AttributeChannel channel2, AttributeChannel channel3,AttributeChannel channel4){
        addChannel(channel1);
        addChannel(channel2);
        addChannel(channel3);
        addChannel(channel4);
    }

    Vector<AttributeChannel> m_channels = new Vector<AttributeChannel>();

    public int size(){
        return m_channels.size();
    }
    
    public int getBitCount(){
        int cnt = 0;
        for(int i = 0; i < m_channels.size(); i++){
            AttributeChannel ac = m_channels.get(i);
            cnt += ac.getBitCount();
        }
        return cnt;
    }


    public AttributeChannel getChannel(int index){
        return m_channels.get(index);
    }

    public AttributeChannel getChannelWithType(String channelType){
        
        for(int i = 0; i < m_channels.size(); i++){
            AttributeChannel channel = m_channels.get(i);
            if(channel.getType().equals(channelType))
                return channel;
        }
        return null;
    }
    
    public void addChannel(AttributeChannel channel){

        m_channels.add(channel);

    }

    public AttributeMaker getAttributeMaker(){

        if(m_attributeMaker == null) {
            // TODO make real attribute maker 
            // this is temp hack !!!
            m_attributeMaker = new AttributeMakerDensity(255);
        }
        //m_attributeMaker = new DefaultAttributeMaker(this);

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

    public static class DefaultAttributeMaker implements AttributeMaker {
        int resolution[];
        public DefaultAttributeMaker(AttributeDesc attDesc){
            resolution = new int[attDesc.size()];
            for(int i = 0; i < resolution.length;i++){
                //TODO 
            }
        }
        
        public long makeAttribute(Vec v){
            //TODO 
            return 0;
        }        
    }

    public AttributeChannel getDensityChannel() {
        for(int i = 0; i < m_channels.size(); i++){
            AttributeChannel ac = m_channels.get(i);
            if(AttributeChannel.DENSITY.equals(ac.m_type))
                return ac;
        }
        return null;
    }

    public AttributeChannel getDefaultChannel() {
        return getChannel(0);
    }

    /**
       creates default attrinute descrition with single 8 bits density channel) 
     */
    public static AttributeDesc getDefaultAttributeDesc(int bitCount){
        AttributeDesc at = new AttributeDesc();
        at.addChannel(new AttributeChannel(AttributeChannel.DENSITY, "density", bitCount, 0, 0., 1.));
        return at;
    }

    /**
       creates AttributeDesc with color+density channel 
     */
    public static AttributeDesc getDensityColor(){

        return new AttributeDesc(new AttributeChannel(AttributeChannel.DENSITY_COLOR, "density_color", 32, 0, 0., 1.));

    }
}



