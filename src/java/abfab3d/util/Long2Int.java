/*****************************************************************************
 *                        Shapeways, Inc Copyright (c) 2012
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package abfab3d.util;


import abfab3d.core.LongConverter;

/**
   returns signed int stored in long
 */
public class Long2Int implements LongConverter {

    /**
       return data custom data component stored in long attribute
     */
    public final long get(long data){
        return (long)((int)(data & 0xFFFFFFFF));
    }
    
}

