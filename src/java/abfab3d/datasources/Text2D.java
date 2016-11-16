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

package abfab3d.datasources;


import java.awt.image.BufferedImage;

import java.awt.Font;


import abfab3d.core.ResultCodes;
import abfab3d.param.*;


import abfab3d.core.DataSource;
import abfab3d.util.TextUtil;
import abfab3d.util.Insets2;

import static java.lang.Math.abs;

import static abfab3d.core.Output.printf;


import static abfab3d.core.MathUtil.clamp;
import static abfab3d.core.MathUtil.step10;

import static abfab3d.core.Units.MM;
import static abfab3d.core.Output.fmt;


/**

   makes 2D image of text 
    
   @author Vladimir Bulatov

 */
public class Text2D extends BaseParameterizable implements ExpensiveInitializable {
    static final boolean DEBUG = false;

    public enum Fit {VERTICAL, HORIZONTAL, BOTH}
    public enum HorizAlign {LEFT, CENTER, RIGHT}
    public enum VertAlign {TOP, CENTER, BOTTOM}

    static public final int BOLD = Font.BOLD; 
    static public final int ITALIC = Font.ITALIC; 
    static public final int PLAIN = Font.PLAIN; 

    static public final int 
        ALIGN_LEFT = TextUtil.ALIGN_LEFT,
        ALIGN_RIGHT = TextUtil.ALIGN_RIGHT, 
        ALIGN_CENTER = TextUtil.ALIGN_CENTER, 
        ALIGN_TOP = TextUtil.ALIGN_TOP, 
        ALIGN_BOTTOM = TextUtil.ALIGN_BOTTOM,
        FIT_VERTICAL = TextUtil.FIT_VERTICAL, 
        FIT_HORIZONTAL=TextUtil.FIT_HORIZONTAL, 
        FIT_BOTH = TextUtil.FIT_BOTH;

    static int m_fitValues[] = new int[]{FIT_VERTICAL,FIT_HORIZONTAL, FIT_BOTH};
    static int m_hAlignValues[] = new int[]{ALIGN_LEFT,ALIGN_CENTER,ALIGN_RIGHT};
    static int m_vAlignValues[] = new int[]{ALIGN_TOP,ALIGN_CENTER,ALIGN_BOTTOM};

    static int debugCount = 1000;

    // arbitrary font size, text is scaled to fit the box, but the size is affecting text rasterization somewhat 
    private int m_fontSize = 50; 

    BufferedImage m_bitmap = null;

    final int SCALING = 5;// scaling factor for text bitmap 

    // public params of the image 
    // NOTE: Should expose one of mp_fontName or mp_font to set Text2D font. May not work well mixing them 
    EnumParameter mp_horizAlign = new EnumParameter("horizAlign","horizontal text alignment (left, right, center)",
            EnumParameter.enumArray(HorizAlign.values()), HorizAlign.LEFT.toString());
    EnumParameter mp_vertAlign = new EnumParameter("vertAlign","vertical text alignment (top, bottom, center)",
            EnumParameter.enumArray(VertAlign.values()), VertAlign.CENTER.toString());
    EnumParameter    mp_fit       = new EnumParameter("fit","fitting of text (horizontal, vertical, both)",
            EnumParameter.enumArray(Fit.values()), Fit.VERTICAL.toString());
    BooleanParameter mp_aspect    = new BooleanParameter("preserveAspect","keep text aspect ratio",true);
    DoubleParameter  mp_fontSize  = new DoubleParameter("fontSize","size of text font to use in points",30);
    DoubleParameter  mp_height = new DoubleParameter("height","height of text",5*MM);
    DoubleParameter  mp_width = new DoubleParameter("width","width of text",0*MM); // width is initially undefined 
    DoubleParameter  mp_voxelSize = new DoubleParameter("voxelSize","size of voxel for text rendering", 0.05*MM);
    StringParameter  mp_fontName  = new StringParameter("fontName","Name of the font", "Times New Roman");
    StringParameter  mp_text      = new StringParameter("text","text to be created", "Text");
    IntParameter     mp_fontStyle = new IntParameter("fontStyle","style of font (BOLD ,ITALIC, PLAIN)", PLAIN);
    DoubleParameter mp_inset      = new DoubleParameter("inset","white space around text", 0.5*MM);
    DoubleParameter mp_spacing    = new DoubleParameter("spacing","extra white space between characters in relative units", 0.);
    ObjectParameter mp_font = new ObjectParameter("font","Specific font to use",null);

    Parameter m_aparam[] = new Parameter[]{
        mp_vertAlign,
        mp_horizAlign,
        mp_fit,
        mp_aspect,
        mp_height,
        mp_width,
        mp_voxelSize,
        mp_fontName,
        mp_text,
        mp_fontStyle,  
        mp_inset,
        mp_spacing,
        mp_fontSize,
    };


    /**
     * Constructor
     @param text the string to convert into 3D text
     @param fontName name of font to be used for 3D text
     @param voxelSize size of voxel used for text rasterizetion
     */
    public Text2D(String text, String fontName, double voxelSize){
        super.addParams(m_aparam);
        mp_text.setValue(text);
        setFontName(fontName);
        setVoxelSize(voxelSize);
    }

    /**
     * Constructor
     @param text the string to convert into 3D text
     @param font font to be used for 3D text
     @param voxelSize size of voxel used for text rasterizetion
     */
    public Text2D(String text, Font font, double voxelSize){
        super.addParams(m_aparam);
        mp_text.setValue(text);
        setFont(font);
        setVoxelSize(voxelSize);
    }

    /**
     * Constructor
       @param text the string to convert into 3D text 
     */
    public Text2D(String text){
        super.addParams(m_aparam);
        mp_text.setValue(text);
    }

    /**
     * Get the font name
     * @return
     */
    public String getFontName() {
        return mp_fontName.getValue();
    }

    /**
     * Set the font style
     * @param fontStyle
     */
    public void setFontStyle(int fontStyle){
        mp_fontStyle.setValue(new Integer(fontStyle));
    }

    /**
     * Get the font style
     * @return
     */
    public int getFontStyle() {
        return mp_fontStyle.getValue();
    }


    /**
     * Set the specific font.  This overrides any value set in fontName
     * @param font The font, or null to clear
     */
    public void setFont(Font font) {
        mp_font.setValue(font);
        
    }

    /**
     * Get the font set by setFont.  This will not return a font for Text created via the fontName route.
     */
    public Font getFont() {
        return (Font) mp_font.getValue();
    }
    /**
     * Get the voxel size
     * @return
     */
    public double getVoxelSize() {
        return mp_voxelSize.getValue();
    }

    public void setText(String val) {
        m_bitmap = null;
        mp_text.setValue(val);
    }

    /**
     * Set the font name.  The available fonts are server dependent.
     */
    public void setFontName(String val) {
        m_bitmap = null;
        validateFontName(val);
        mp_fontName.setValue(val); 
    }

    public void setFontStyle(Integer val) {
        m_bitmap = null;
        mp_fontStyle.setValue(val);
    }

    /**
     * Set the voxel size
     * @param val The size in meters
     */
    public void setVoxelSize(double val) {
        m_bitmap = null;
        mp_voxelSize.setValue(val);
    }

    public void setSpacing(double val) {
        m_bitmap = null;

        System.out.println("Inside setspacing: " + val + " isNaN: " + Double.isNaN(val));
        mp_spacing.setValue(val);
    }

    public double getSpacing() {
        return mp_spacing.getValue();
    }

    public void setInset(double val) {
        m_bitmap = null;
        mp_inset.setValue(val);
    }

    public double getInset() {
        return mp_inset.getValue();
    }

    public void set(String param, Object val) {
        // change of param causes change of bitmap
        m_bitmap = null;
        super.set(param, val);
    }

    public void setWidth(double val) {
        if (val <= 0) {
            throw new IllegalArgumentException("Text2D width cannot be <= 0");
        }

        m_bitmap = null;
        mp_width.setValue(val);
    }

    public double getWidth() {
        return mp_width.getValue();
    }

    public void setHeight(double val) {
        if (val <= 0) {
            throw new IllegalArgumentException(fmt("illegal Text2D height:%11.9", val));
        }

        m_bitmap = null;
        mp_height.setValue(val);
    }

    public double getHeight() {
        return mp_height.getValue();
    }

    public void setHorizAlign(String val) {
        m_bitmap = null;
        mp_horizAlign.setValue(val.toUpperCase());
    }

    public String getHorizAlign() {
        return mp_horizAlign.getValue();
    }

    public void setVertAlign(String val) {
        m_bitmap = null;
        mp_vertAlign.setValue(val.toUpperCase());
    }

    public String getVertAlign() {
        return mp_vertAlign.getValue();
    }

    public void setFit(String val) {
        m_bitmap = null;
        mp_fit.setValue(val.toUpperCase());
    }

    public String getFit() {
        return mp_fit.getValue();
    }

    public void setPreserveAspect(boolean val) {
        m_bitmap = null;
        mp_aspect.setValue(val);
    }

    public boolean getPreserveAspect() {
        return mp_aspect.getValue();
    }

    /**
     * @noRefGuide
     * @return
     */
    public BufferedImage getImage(){

        if(m_bitmap == null)
            initialize();

        return m_bitmap;
    }

    /**
     * @noRefGuide
     */
    protected void initParams(){
        super.addParams(m_aparam);
    }

    protected void validateFontName(String fontName){
        if (!TextUtil.fontExists(fontName)) {
            throw new IllegalArgumentException(fmt("Text2D: Font \"%s\" not found",fontName));
        }
    }
    
    /**
       calculates preferred width of text box with for given font, insets and height
       the returned width includes width of text and insets on all sides 
       @return preferred text box width

     @noRefGuide
     */
    public double getPreferredWidth(){
        double voxelSize = mp_voxelSize.getValue();
        double inset = (mp_inset.getValue()/voxelSize);
        int fontStyle = mp_fontStyle.getValue();

        Font font = getFont();

        if (font == null) {
			String fontName = mp_fontName.getValue();
			validateFontName(fontName);
			font = new Font(fontName, fontStyle, m_fontSize);
        }
        
        String text = mp_text.getValue();

        double spacing = mp_spacing.getValue();
        Insets2 insets = new Insets2(inset,inset,inset,inset);

        int heightVoxels = (int)Math.round(mp_height.getValue()/voxelSize);         
        return voxelSize * TextUtil.getTextWidth(heightVoxels, text, font, spacing, insets);

    }

    /**
       @noRefGuide
     */
    public int initialize(){

        if(DEBUG) printf("Text2D.initialize()\n");

        double voxelSize = mp_voxelSize.getValue();
        if(DEBUG) printf("  voxelSize:%7.5f\n", voxelSize);
        String fontName = mp_fontName.getValue();
        
        int fontStyle = mp_fontStyle.getValue();
        double inset = (mp_inset.getValue()/voxelSize);        
        Insets2 insets = new Insets2(inset,inset,inset,inset);
        boolean aspect = mp_aspect.getValue();

        int fit = m_fitValues[mp_fit.getIndex()];
        int halign = m_hAlignValues[mp_horizAlign.getIndex()];
        int valign = m_vAlignValues[mp_vertAlign.getIndex()];
        
        // No need to validate font name if font is already set
        Font font = getFont();
        if (font == null) {
	        try {
	        	validateFontName(fontName);
	        } catch (IllegalArgumentException iae) {
	        	printf("%s\n", iae.getMessage());
	        } finally {
	        	// Set a font closest to the name specified by fontName
	        	font = new Font(fontName, fontStyle, m_fontSize);
	        	setFont(font);
	        }
        } else {
        	fontName = font.getFontName();
        }

        if(DEBUG) printf("  fontName:%s\n", fontName);
        
        int height = (int)Math.round(mp_height.getValue()/voxelSize);
        if(DEBUG) printf("  text height:%d pixels\n", height);

        try {
	        if(mp_width.getValue() <= 0.) mp_width.setValue(getPreferredWidth());
        } catch (IllegalArgumentException iae) {
        	printf("%s\n", iae.getMessage());
        }

        int width = (int)Math.round(mp_width.getValue()/voxelSize);         
        if(DEBUG) printf("  text width:%d pixels\n", width);

        String text = mp_text.getValue();

        if(DEBUG) printf("  text:\"%s\"\n", text);

        font = font.deriveFont(fontStyle,m_fontSize);
        m_bitmap = TextUtil.createTextImage(width, height, text, font, mp_spacing.getValue().doubleValue(),insets, aspect, fit, halign, valign);

        if(DEBUG)printf("Text2D bitmap height: %d x %d\n", m_bitmap.getWidth(), m_bitmap.getHeight());
        
        return ResultCodes.RESULT_OK;
        
    }

    /**
     * Implement this as a value
     * @return
     */
    public String getParamString() {
        return BaseParameterizable.getParamString("Text2D",getParams());
    }

    public void getParamString(StringBuilder sb) {
        sb.append(BaseParameterizable.getParamString("Text2D", getParams()));
    }

}  // class Text2D 
