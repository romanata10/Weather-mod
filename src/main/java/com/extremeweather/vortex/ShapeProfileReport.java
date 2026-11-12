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
        int dead=0,elReno=0;
        for(long id=1;id<=100_000;id++) {
            FunnelShape shape=FunnelShape.natural(id);
            if(shape==FunnelShape.DEAD_MAN_WALKING)dead++;
            if(shape==FunnelShape.EL_RENO)elReno++;
        }
        System.out.println("Rare natural presets in 100000 deterministic storm IDs: dead_man_walking="+dead+" el_reno="+elReno);
        if(dead<30 || dead>200 || elReno<30 || elReno>200)
            throw new AssertionError("Rare natural shape selection outside expected 0.1% range");
        for(FunnelShape rare:new FunnelShape[]{FunnelShape.DEAD_MAN_WALKING,FunnelShape.EL_RENO}) {
            float original=20,peak=rare.rareMatureWidth(original);
            if(!(peak>original && rare.rareRecedingWidth(original,65)==original &&
                rare.rareRecedingWidth(original,87)>original &&
                rare.rareRecedingWidth(original,87)<peak &&
                rare.rareRecedingWidth(original,110)==peak))
                throw new AssertionError("Rare native cloud width did not recede with wind: "+rare);
        }
    }
}
