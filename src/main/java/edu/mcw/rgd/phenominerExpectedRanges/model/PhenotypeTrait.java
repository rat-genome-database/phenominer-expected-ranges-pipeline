package edu.mcw.rgd.phenominerExpectedRanges.model;

import edu.mcw.rgd.datamodel.ontologyx.TermWithStats;
import edu.mcw.rgd.phenominerExpectedRanges.process.ExpectedRangeProcess;

import java.util.List;
import java.util.Map;

/**
 * Created by jthota on 5/4/2018.
 */
public class PhenotypeTrait {
    private  Map<String, String> phenotypeTraitMap;
    private List<TermWithStats> getFirstLevelTraitTerms;
    private static  PhenotypeTrait instance=null;
    public static PhenotypeTrait getInstance() throws Exception {
        if(instance==null){
            ExpectedRangeProcess process=new ExpectedRangeProcess();
            instance=new PhenotypeTrait();
            instance.setGetFirstLevelTraitTerms(process.getFirstLevelTraitTerms());
            instance.setPhenotypeTraitMap(process.getAllPhenotypeTraitMap());
        }
        return instance;
    }

    public Map<String, String> getPhenotypeTraitMap() {
        return phenotypeTraitMap;
    }

    public void setPhenotypeTraitMap(Map<String, String> phenotypeTraitMap) {
        this.phenotypeTraitMap = phenotypeTraitMap;
    }

    public List<TermWithStats> getGetFirstLevelTraitTerms() {
        return getFirstLevelTraitTerms;
    }

    public void setGetFirstLevelTraitTerms(List<TermWithStats> getFirstLevelTraitTerms) {
        this.getFirstLevelTraitTerms = getFirstLevelTraitTerms;
    }

    public static void setInstance(PhenotypeTrait instance) {
        PhenotypeTrait.instance = instance;
    }
}
