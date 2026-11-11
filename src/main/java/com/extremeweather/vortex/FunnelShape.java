package com.extremeweather.vortex;

import java.util.Locale;

/** Outline families from the user's tornado reference; visual categories, NOT EF ratings. */
public enum FunnelShape {
    WIRE, ROPE, NEEDLE, V_SHAPED, CIGAR, CYLINDER, CONE, LANDSPOUT,
    CONCAVE_SIDED, STRAIGHT_SIDED, CONVEX_SIDED, SEGMENTED,
    TRUNCATED_CONE, DRILL_SHAPED, BOWL_SHAPED, WEDGE,
    PLUME_BASE, HOURGLASS, BRAIDED, LOOPING,
    MULTI_VORTEX, DEAD_MAN_WALKING, EL_RENO;

    /** Height-normalised outline, 0 = surface, 1 = cloud base. */
    public double radius(double h,double width,double time) {
        h=Math.max(0,Math.min(1,h));
        double curve=switch(this) {
            case WIRE -> .12+.12*h;
            case ROPE, LANDSPOUT -> .22+.16*h;
            case NEEDLE -> .12+.75*h*h;
            case V_SHAPED -> .18+.90*h;
            case CIGAR -> .48+.30*Math.sin(Math.PI*h);
            case CYLINDER, STRAIGHT_SIDED -> .75;
            case CONE -> .25+.74*h;
            case CONCAVE_SIDED -> .22+.75*h*h;
            case CONVEX_SIDED -> .18+.83*Math.sqrt(h);
            case SEGMENTED -> (.4+.4*h)*(1+.18*Math.sin(h*26+time*.2));
            case TRUNCATED_CONE -> .43+.57*h;
            case DRILL_SHAPED -> (.22+.6*h)*(1+.14*Math.sin(h*35+time*.4));
            case BOWL_SHAPED -> .95-.5*h;
            case WEDGE -> 1.8-.8*h;
            case PLUME_BASE -> 1.1-.6*h+.3*Math.exp(-h*14);
            case HOURGLASS -> .80-.53*Math.sin(h*Math.PI);
            case BRAIDED -> (.45+.40*h)*(1+.12*Math.sin(h*22+time*.25));
            case LOOPING -> .45+.35*h;
            case MULTI_VORTEX -> 1.05-.2*h;
            case DEAD_MAN_WALKING -> 1.45-.55*h;
            case EL_RENO -> 2.8-.9*h;
        };
        return Math.max(.5,width*curve);
    }
    /** Approximate native PMWeather width from the requested near-ground profile.
     * The native renderer has no per-height profile API; this cannot recreate
     * the complete 23-shape outline used by the separate addon preview. */
    public float nativeWidthScale() {
        return (float)Math.max(.4,Math.min(2.6,radius(0,100,0)/100));
    }
    /** PMWeather exposes a continuous tornadoShape value, not an enum.
     * Map ground/cloud width ratio to that bounded native control rather than
     * assigning unrelated values based on enum declaration order. */
    public float nativeTaper() {
        double ratio=radius(0,100,0)/radius(1,100,0);
        return (float)(6+10*Math.max(0,Math.min(1,(ratio-.2)/1.6)));
    }
    public int satellites() {
        return switch(this) {
            case MULTI_VORTEX -> 4;
            case DEAD_MAN_WALKING -> 3;
            case EL_RENO -> 5;
            case BRAIDED -> 2;
            default -> 0;
        };
    }
    /** Stable, weighted selection for PMWeather-generated tornadic supercells.
     * No per-tick dice roll: the same native storm keeps its chosen preset. */
    public static FunnelShape natural(long stormId) {
        FunnelShape[] common={ROPE,CONE,CYLINDER,V_SHAPED,CIGAR,CONCAVE_SIDED,
            STRAIGHT_SIDED,CONVEX_SIDED,TRUNCATED_CONE,WEDGE,LANDSPOUT};
        int roll=(int)Math.floorMod(stormId ^ (stormId >>> 32),100L);
        return roll<85?common[Math.floorMod(roll,common.length)]
            : values()[(int)Math.floorMod((stormId * 0x9e3779b97f4a7c15L) >>> 17,values().length)];
    }
    public static FunnelShape parse(String text) {
        try{return valueOf(text.toUpperCase(Locale.ROOT).replace('-','_'));}
        catch(IllegalArgumentException e){return null;}
    }
}
