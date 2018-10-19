package edu.mcw.rgd.phenominerExpectedRanges.model;

import java.util.*;

/**
 * Created by jthota on 3/23/2018.
 */
public  class NormalStrainGroup {

    public static Map<String, List<String>>  phenotypeNormalStrainsMap;
    static {
        /*
         "CMO:0000009" ,
                "CMO:0000069" ,
                "CMO:0000071" ,
                "CMO:0000072" ,
                "CMO:0000074" ,
                "CMO:0000075" ,
                "CMO:0000108" ,
                "CMO:0000530"
         */
        Map<String, List<String>> normalStrainsMap= new HashMap<>();


  normalStrainsMap.put("CMO:0000005", new ArrayList<>(Arrays.asList("BN", "HTG", "LEW", "MHS", "WKY")));
     normalStrainsMap.put("CMO:0000004", new ArrayList<>(Arrays.asList("ACI","BN", "BUF", "DA", "F344","GK","LE (inbred)", "LEW", "LN",
               "M520", "MNS", "MR", "MWF", "WKY")));
        normalStrainsMap.put("CMO:0000002", new ArrayList<>(Arrays.asList("BN", "BUF", "F344", "FHH", "GH", "LE (inbred)",
               "LEW", "SHR", "SHRSP", "SS", "WAG", "WKY")));
        normalStrainsMap.put("CMO:0000009", new ArrayList<>(Arrays.asList("BN", "LE (inbred)", "LEW", "SS", "WKY")));
        normalStrainsMap.put("CMO:0000069", new ArrayList<>(Arrays.asList("ACI", "BN", "BUF", "COP", "DA", "F344", "GK", "LE (inbred)", "LEW", "MWF", "SHR", "WAG", "WKY")));
        normalStrainsMap.put("CMO:0000071", new ArrayList<>(Arrays.asList("BN", "COP", "DA", "DRY", "F344", "LEW" , "SHR", "WKY")));
        normalStrainsMap.put("CMO:0000072", new ArrayList<>(Arrays.asList("F344", "WKY")));
        normalStrainsMap.put("CMO:0000074", new ArrayList<>(Arrays.asList("ACI", "BN", "BUF", "COP", "DA", "F344", "ISIAH", "M520", "MWF", "SHR", "WAG", "WKY", "WN")));
        normalStrainsMap.put("CMO:0000075", new ArrayList<>(Arrays.asList("ACI", "BN", "BUF","COP", "DA", "F344", "GK", "LE (inbred)", "LEW", "SS", "WAG", "WKY")));
        normalStrainsMap.put("CMO:0000108", new ArrayList<>(Arrays.asList("LE (inbred)", "LEW", "SHR", "SS", "WKY")));
        normalStrainsMap.put("CMO:0000530", new ArrayList<>(Arrays.asList("COP", "DA", "DRY", "F344", "LEW", "WKY")));

        phenotypeNormalStrainsMap= Collections.unmodifiableMap(normalStrainsMap);
    }
}