package com.jh.usbhostmanage;

/**
 * Created by admin on 2017/6/3.
 */

public class UsbCard {

    public static final String[] scheme1={
            "D0 A",	"D0 2",	"D0 3",	"D0 4", "D0 5",	"D0 6",	"D0 7",	"D0 8", "D0 9",	"D1 0",	"D1 J",	"D1 Q", "D1 K",
            "E0 A",	"E0 2",	"E0 3",	"E0 4",	"E0 5",	"E0 6",	"E0 7",	"E0 8",	"E0 9",	"E1 0",	"E0 J", "E0 Q",	"E0 K",
            "F0 A",	"F0 2",	"F0 3",	"F0 4",	"F0 5",	"F0 6",	"F0 7",	"F0 8",	"F0 9",	"F1 0",	"F0 J",	"F0 Q",	"F0 K",
            "G0 A",	"G0 2",	"G0 3",	"G0 4",	"G0 5",	"G0 6",	"G0 7",	"G0 8",	"G0 9",	"G1 0",	"G0 J",	"G0 Q",	"G0 K"
    };
    public static final int[] scheme2={
            101,102,103,104,105,106,107,108,109,110,111,112,113,
            201,202,203,204,205,206,207,208,209,210,211,212,213,
            301,302,303,304,305,306,307,308,309,310,311,312,313,
            401,402,403,404,405,406,407,408,409,410,411,412,413
    };

    public static int getCardIndex(String card,String[] scheme){
        for (int i=0; i< scheme.length; i++){
            if (scheme[i].equals(card))  return i;
        }
        return -1;
    }
    public static int getCardIndex(int card,int[] scheme){
        for (int i=0; i<scheme.length;i++){
            if (scheme[i]==card) return i;
        }
        return -1;
    }
}
