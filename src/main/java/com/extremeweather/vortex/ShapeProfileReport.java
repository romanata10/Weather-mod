package com.extremeweather.vortex;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Build-time audit: this checks parameter expressiveness, NOT rendered fidelity. */
public final class ShapeProfileReport {
    private ShapeProfileReport() {}
    public static void main(String[] args) {
        List<String> collisions=new ArrayList<>();
        int curved=0;
        for(FunnelShape shape:FunnelShape.values()) {
            double bottom=shape.radius(0,100,0)/100;
            double middle=shape.radius(.5,100,0)/100;
            double top=shape.radius(1,100,0)/100;
            double lostMid=Math.abs(middle-(bottom+top)*.5);
            if(lostMid>.12)curved++;
            System.out.printf(Locale.ROOT,"%s ground=%.2f mid=%.2f cloud=%.2f nativeWidth=%.2f nativeTaper=%.2f satellites=%d midCurveLoss=%.2f%n",
                shape,bottom,middle,top,shape.nativeWidthScale(),shape.nativeTaper(),shape.satellites(),lostMid);
            for(FunnelShape other:FunnelShape.values()) {
                if(other.ordinal()>=shape.ordinal())break;
                if(Math.abs(shape.nativeWidthScale()-other.nativeWidthScale())<.001 &&
                   Math.abs(shape.nativeTaper()-other.nativeTaper())<.001 &&
                   shape.satellites()==other.satellites())
                    collisions.add(other+"="+shape);
            }
        }
        System.out.println("Identical native parameter pairs: "+String.join(", ",collisions));
        System.out.println("Profiles losing mid-height curvature under endpoint-only mapping: "+curved);
        if(FunnelShape.values().length!=23)throw new AssertionError("Expected 23 requested profiles");
    }
}
