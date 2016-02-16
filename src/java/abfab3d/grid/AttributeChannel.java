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

import abfab3d.util.LongConverter;
import abfab3d.util.MathUtil;

import static abfab3d.util.Output.printf;
import static abfab3d.util.Output.fmt;
import static abfab3d.util.MathUtil.clamp;

/**
 * A description of a single channel of a grid attribute
 *
 * @author Vladimir Bulatov
 */
public class AttributeChannel  implements LongConverter { // , ValueMaker { 
    
    // standard chnnel types
    public static final String DENSITY = "DENSITY";
    public static final String COLOR = "COLOR";
    public static final String DENSITY_COLOR = "DENSITY_COLOR";
    public static final String MATERIAL = "MATERIAL";
    public static final String DISTANCE = "DISTANCE";
    public static final String DATA_FLOAT = "DATA_FLOAT";

    
    
    // name of the channel 
    String m_name;
    // type of the channel 
    String m_type;
     // shift to move bits toward origin 
     int m_shift;
    int m_bits;

    // mask to get sign bit 
    long m_signMask;
    long m_complementMask;
    // bitmask to extract channel bits from unsigned long
    long m_mask;
    long m_maxLongValue;
    double m_maxValue;
    double m_minValue;
    double m_value0;
    double m_value1;
    double m_offset; // conversion offset 
    double m_D2B; // Double -> Bits conversion factor 
    double m_B2D; // Bits -> Double conversion factor 

    BitsExtractor m_bitsExtractor;
    
    /**
       attribute channel stores data in given number of bits 
       the physical value is interpolated between value1 and value 2
       value with bits 0000 is mapped to value0
       value bits with valus 1111 is maped to value1 

       this is unsigned variant of AttributeChannel 
       
     */
    public AttributeChannel(String type, String name, int bits, int shift, double value0, double value1){
        if (bits >= 64) {
            throw new IllegalArgumentException("Class doesn't work for >= 64 bits");
        }
        m_type = type;
        m_name = name;
        m_shift = shift;
        m_bits = bits;
        m_mask = MathUtil.getBitMask(bits);
        m_maxLongValue = (1l << bits)-1;

        m_bitsExtractor = new UnsignedBitsExtractor();

        if(value1 > value0){
            m_maxValue = value1;
            m_minValue = value0;
        } else if(value1 < value0){
            m_maxValue = value0;
            m_minValue = value1;
        } else {
            throw new IllegalArgumentException("AttributeChannel (value0 == value1) is not allowed");
        }
        m_offset = value0;
        m_value0 = value0;
        m_value1 = value1;
        m_D2B = m_maxLongValue/(value1 - value0);        
        m_B2D = 1./m_D2B;

    }

    public AttributeChannel(String type, String name, int bits, int shift){
        this(type, name, bits, shift, 0., 1.);
    }
    

    /**
       this is signed variant of AttributeChannel to work with short
       it is legacy varian to work with code which stores distance data as signed short 
       @param physicalUnit conversion factor from int to physical units 
     */
    public AttributeChannel(String type, String name, double physicalUnit, double minValue, double maxValue){

        m_type = type;
        m_name = name;
        m_shift = 0;
        m_mask = 0xFFFF;

        m_bitsExtractor = new ShortBitsExtractor();
        

        m_B2D = physicalUnit;        
        m_D2B = 1./m_B2D;

        m_offset = 0;
        m_value0 = minValue;
        m_value1 = maxValue;
        
        //m_value1 = physicalUnit*Short.MAX_VALUE;

    }

     /**
        bit count stored in the channel 
      */
     public int getBitCount(){

         return m_bits;

     }

     /**
        sets shift used to move bits toward origin 
      */
     public void setShift(int shift){
         m_shift = shift;
     }

     /**
        type of the channel. Return one of the standard types 
      */
     public String getType(){
         return m_type;
     }

    public String getName(){
        return m_name;
    }
    
    public String toString(){
        return  fmt("AttribiuteChannel(%s:%s:%d:%7.4f:%7.4f)", getType(),  getName(), getBitCount(), m_value0, m_value1);
    }

    public double getValue0(){
        return m_value0;
    }

    public double getValue1(){
        return m_value1;
    }

    /**
       method of interface LongConverter 
    */
    public final long get(long att){
        return getBits(att);
    }

    /**
       convert attribute bits into double value  
    */
    public double getValue(long attribute){
         return m_B2D*getBits(attribute)+m_offset;
    }

     public final long getBits(long att){
         //return (att >> m_shift) & m_mask;
         //return getBits_unsigned(att);
         return m_bitsExtractor.extract(att);
     }

    /**
       extract value bits out of attribute 
    */
    private final long getBits_unsigned(long att){
        
        return (att >> m_shift) & m_mask;
        
    }
    
    // code to get bits in signed version 
    private final long getBits_short(long att){
        return (long)(short)(att & 0xFFFF);
    }
    
    /**
       convert double value into attribute bits
    */
    public long makeAtt(double value){
        value = (clamp(value, m_minValue, m_maxValue)-m_value0)*m_D2B;
        return (((long)(value + 0.5))& m_mask) << m_shift;
    }

    // interface to get signed or unsigned bits from attribute
    private interface BitsExtractor {
        long extract(long att);        
    }

    /**
       legacy class to work with values stored as signed short
     */
    final class ShortBitsExtractor implements BitsExtractor {
        final public long extract(long att){
            return getBits_short(att);
        }
        
    }

    final class UnsignedBitsExtractor implements BitsExtractor {
        final public long extract(long att){
            return getBits_unsigned(att);
        }
    }    
}
