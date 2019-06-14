package edu.mcw.rgd.phenominerExpectedRanges.dao;

import edu.mcw.rgd.dao.impl.OntologyXDAO;
import edu.mcw.rgd.dao.impl.PhenominerDAO;
import edu.mcw.rgd.dao.impl.PhenominerExpectedRangeDao;
import edu.mcw.rgd.dao.impl.PhenominerStrainGroupDao;

import edu.mcw.rgd.datamodel.ontologyx.Term;
import edu.mcw.rgd.datamodel.ontologyx.TermWithStats;
import edu.mcw.rgd.datamodel.pheno.Experiment;
import edu.mcw.rgd.datamodel.pheno.Record;
import edu.mcw.rgd.datamodel.phenominerExpectedRange.PhenominerExpectedRange;
import edu.mcw.rgd.datamodel.phenominerExpectedRange.PhenominerStrainGroup;
import edu.mcw.rgd.datamodel.phenominerExpectedRange.TraitObject;

import edu.mcw.rgd.phenominerExpectedRanges.model.*;
import edu.mcw.rgd.phenominerExpectedRanges.process.ExpectedRangeProcess;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

import static edu.mcw.rgd.phenominerExpectedRanges.Manager.log;


/**
 * Created by jthota on 3/1/2018.
 */
public class PhenotypeExpectedRangeDao extends OntologyXDAO {
    PhenominerDAO pdao= new PhenominerDAO();
    PhenominerStrainGroupDao strainGroupDao= new PhenominerStrainGroupDao();
    PhenominerExpectedRangeDao dao= new PhenominerExpectedRangeDao();
    OntologyXDAO xdao=new OntologyXDAO();

    RangeValues rangeValues= new RangeValues();

    private static List<String> vascularTermIds;
    private static List<String> tailTermIds;

   public Map<String, List<String>> getInbredStrainGroupMap2(String termAcc) throws Exception {

        List<TermWithStats> topLevelTerms=getActiveChildTerms(termAcc,3);
        Map<String, List<String>> strainGroupMap= new HashMap<>();
        for(TermWithStats p: topLevelTerms){
            List<String> rsIds=new ArrayList<>();
            // String term_acc="RS:0000113";
            List<Term> termAndChildTerms= getAllActiveTermDescendants(p.getAccId());
            //   List<Term> termAndChildTerms= getAllActiveTermDescendants(term_acc);
            termAndChildTerms.add(getTerm(p.getAccId()));
            //termAndChildTerms.add(getTerm(term_acc));
            for(Term t: termAndChildTerms){
                rsIds.add(t.getAccId());
                //    System.out.println(t.getAccId() + "||" +t.getTerm());
            }
            //   strainGroupMap.put(term_acc, rsTerms);

            strainGroupMap.put(p.getAccId(), rsIds);
        }
        return strainGroupMap;

    }
    public List<String> getMeasurementMethods() throws Exception {

        List<String> mmoTerms= new ArrayList<>();
        List<String> vascularIds= new ArrayList<>();
        List<String> tailIds=new ArrayList<>();
        /*MMO:0000076 -Plethysmography ,MMO:0000011 vascular indwelling , MMO:0000015 radiotelmetry , MMO:0000015 intra aortic*/
        List<Term> toplevelTerms= new ArrayList<>();
        List<Term> tailTerms=  getAllActiveTermDescendants("MMO:0000076");
        List<Term> vascularTerms= getAllActiveTermDescendants("MMO:0000011");

        vascularTerms.addAll(getAllActiveTermDescendants("MMO:0000015"));
        tailTerms.add(getTerm("MMO:0000076"));
        vascularTerms.add(getTerm("MMO:0000011"));
      //  vascularTerms.add(getTerm("MMO:0000015")); // As per Yiqing, radiotelmetry is not included in vasular
        toplevelTerms.addAll(getAllActiveTermDescendants("MMO:0000000"));
      /*  toplevelTerms.addAll(vascularTerms);
        toplevelTerms.addAll(tailTerms);
        toplevelTerms.add(getTerm("MMO:0000076"));
        toplevelTerms.add(getTerm("MMO:0000011"));
        toplevelTerms.add(getTerm("MMO:0000015"));
*/
        for(Term t:vascularTerms){
            if(!vascularIds.contains(t.getAccId()))
            vascularIds.add(t.getAccId());
        }
        for(Term t:tailTerms){
            if(!tailIds.contains(t.getAccId()))
            tailIds.add(t.getAccId());
        }
        for(Term p: toplevelTerms){
             mmoTerms.add(p.getAccId());
        }
       setTailTermIds(tailIds);
       setVascularTermIds(vascularIds);
        return mmoTerms;
    }
    public List<String> getConditons(String termAcc) throws Exception {

        List<String> conditions=new ArrayList<>();
        List<Term> xcoTerms=getAllActiveTermDescendants(termAcc); /*XCO:0000056|| naive control condition*/
        conditions.add(termAcc);
        for(Term t: xcoTerms){
             conditions.add(t.getAccId());
        }
        return conditions;
    }
    public int insertNormalRanges(List<String> xcoTerms, List<String> mmoTerms, Map phenotypeTraitMap, String phenotypeAccId) throws Exception {

        List<PhenominerExpectedRange> normalRanges = new ArrayList<>();
        normalRanges.addAll(getNormalStrainsRanges1(xcoTerms, mmoTerms, phenotypeTraitMap, phenotypeAccId));
        if(normalRanges.size()>0)
        return insert(normalRanges);
        return 0;
    }
   public List<PhenominerExpectedRange> getSummaryRanges(List<Record> records, String phenotypeAccId, int strainGroupId, Map<String, String> phenotypeTraitMap) throws Exception {

        List<PhenominerExpectedRange> ranges= new ArrayList<>();
        String traitOntId= this.getTraitOntId(records, phenotypeAccId, phenotypeTraitMap);

        List<TraitObject> traitAncestors= getTraitAncester(traitOntId);
        Set<String> rangeUnits= this.getRangeUnits(records);
        Map<String,List<Record>> categories= categorizeRecords(records);

        for(Map.Entry e: categories.entrySet()){
            String cat= (String) e.getKey();
    //      if(cat.equalsIgnoreCase("overall")) {
                String sex = "Mixed";
                String method = null;
                int ageLow = 0;
                int ageHigh = 999;
   //     if(cat.equalsIgnoreCase("overall")) {
                if (cat.equalsIgnoreCase("female")) {
                    sex = "Female";
                }
                if (cat.equalsIgnoreCase("male")) {
                    sex = "Male";
                }
                if (cat.equals("vascular")) {
                    method = "vascular";
                }
                if (cat.equals("tail")) {
                    method = "tail";
                }
                if (cat.equals("age1")) {
                    ageHigh = 79;
                }
                if (cat.equals("age2")) {
                    ageLow = 80;
                    ageHigh = 99;
                }
                if (cat.equals("age3")) {
                    ageLow = 100;
                    ageHigh = 998;
                }
                List<Record> recs = (List<Record>) e.getValue();
                if (recs.size() >= 4) {

                    PhenominerExpectedRange r= this.getRange(recs, phenotypeAccId, strainGroupId, sex, method, ageLow, ageHigh, traitOntId, rangeUnits, traitAncestors, false);
                    if(r!= null) {
                        ranges.add(r);
                    }
                }
       //   }

    }

        return ranges;
    }
    public Set<String> getRangeUnits(List<Record> records){
    Set<String> rangeUnits= new HashSet<>();
    for(Record r:records){
        rangeUnits.add(r.getMeasurementUnits());
    }
    return rangeUnits;
    }
    public String getTraitOntId(List<Record> records, String phenotypeAccId, Map<String, String> phenotypeTraitMap) throws Exception {
        List<String> traitsList= new ArrayList<>();
        String traitOntId=new String();
        for (Record r : records) {
            Experiment experiment = pdao.getExperiment(r.getExperimentId());
            if (experiment != null) {
                if (experiment.getTraitOntId() != null) {
                    String t= experiment.getTraitOntId();
                    if(!traitsList.contains(t))
                        traitsList.add(t);

                  }
            }
        }
        if(traitsList.size()==1) {
                traitOntId=traitsList.get(0);

        }else{
            if(traitsList.size()==0){
                 traitOntId = phenotypeTraitMap.get(phenotypeAccId);
            }else {
                log.info(phenotypeAccId+"\t"+ getTerm(phenotypeAccId).getTerm()+"\ttraits size: "+ traitsList.size());
    }
    }
     return traitOntId;
    }

    public Map<String,List<Record>> categorizeRecords(List<Record> records) throws Exception {
        Map<String, List<Record>> sortedRecords= new HashMap<>();
    Set<String> rangeUnits= new HashSet<>();

    List<Record> overAllRecords= new ArrayList<>();
    List<Record> maleRecords= new ArrayList<>();
    List<Record> femaleRecords= new ArrayList<>();
    List<Record> unspecifiedSexRecords= new ArrayList<>();
    List<Record> age1Records= new ArrayList<>();
    List<Record> age2Records= new ArrayList<>();
    List<Record> age3Records= new ArrayList<>();
    List<Record> vascularRecords= new ArrayList<>();
    List<Record> tailRecords= new ArrayList<>();

    for(Record r:records) {

        rangeUnits.add(r.getMeasurementUnits());
        if (r.getMeasurementSD() != null && r.getMeasurementValue() != null && !Objects.equals(r.getMeasurementValue(), "") && !Objects.equals(r.getMeasurementSD(), "")) {
            if (Double.parseDouble(r.getMeasurementSD()) > 0.0){
                overAllRecords.add(r);
                int ageLow = 0;
                int ageHigh = 0;
            if (vascularTermIds.contains(r.getMeasurementMethod().getAccId())) vascularRecords.add(r);
            if (tailTermIds.contains(r.getMeasurementMethod().getAccId()))   tailRecords.add(r);
            if (r.getSample().getSex().equalsIgnoreCase("male")) {
                     maleRecords.add(r);
               } else {
                if (r.getSample().getSex().equalsIgnoreCase("female")) {
                      femaleRecords.add(r);
                } else {
                    unspecifiedSexRecords.add(r);
                }
            }
            if (r.getSample().getAgeDaysFromLowBound() != null) ageLow = r.getSample().getAgeDaysFromLowBound();
            if (r.getSample().getAgeDaysFromHighBound() != null) ageHigh = r.getSample().getAgeDaysFromHighBound();
            if (ageLow >= 0 && ageHigh <= 79) age1Records.add(r);
            if (ageLow >= 80 && ageHigh <= 99) age2Records.add(r);
            if (ageLow >= 100 && ageHigh <= 998) age3Records.add(r);
        }}

    }
        sortedRecords.put("overall", overAllRecords);
        sortedRecords.put("female", femaleRecords);
        sortedRecords.put("male", maleRecords);
        sortedRecords.put("age1", age1Records);
        sortedRecords.put("age2", age2Records);
        sortedRecords.put("age3", age3Records);
        sortedRecords.put("vascular", vascularRecords);
        sortedRecords.put("tail", tailRecords);
        sortedRecords.put("unspecifiedSex", unspecifiedSexRecords);
        return sortedRecords;
            }
    public PhenominerExpectedRange getRange(List<Record> records, String phenotypeAccId,int strainGroupId, String sex,String method, int ageLow, int ageHigh, String traitOntId, Set<String> rangeUnits, List<TraitObject> traitAncestors, boolean isNormal) throws Exception {
      PhenominerExpectedRange  range= rangeValues.getRangeValues(records, isNormal);
        String phenotype=getTerm(phenotypeAccId).getTerm();
        String parentStrainName= strainGroupDao.getStrainGroupName(strainGroupId);
        if(range!=null) {
            range.setClinicalMeasurement(phenotype);
            range.setClinicalMeasurementOntId(phenotypeAccId);
            if (parentStrainName.toLowerCase().contains("normalstrain")) {
               range.setExpectedRangeName(parentStrainName+ "_"+sex);
            }
            else {
               String expectedRangeName=phenotype + "_"+sex+"_"+ageLow+"-"+ageHigh+" days";
                if(method!=null){
                    expectedRangeName+="_"+method;
                }
                range.setExpectedRangeName(expectedRangeName);
            }
            range.setStrainGroupName(parentStrainName);
            range.setStrainGroupId(strainGroupId);
            range.setExperimentRecords(records);
            range.setSex(sex);
            range.setAgeLowBound(ageLow);
            range.setAgeHighBound(ageHigh);
            range.setTraitOntId(traitOntId);
            range.setUnits(rangeUnits.toString());
            range.setTraitAncestors(traitAncestors);

        }
        return range;

    }
    public PhenominerExpectedRange get2StepRange(List<Record> records, String phenotypeAccId,int strainGroupId, String sex,String method, int ageLow, int ageHigh, String traitOntId, Set<String> rangeUnits, List<TraitObject> traitAncestors, boolean isNormal) throws Exception {
        PhenominerExpectedRange  range= rangeValues.getRangeValues(records, isNormal);
        String phenotype=getTerm(phenotypeAccId).getTerm();
        String parentStrainName= strainGroupDao.getStrainGroupName(strainGroupId);
        if(range!=null) {
            range.setClinicalMeasurement(phenotype);
            range.setClinicalMeasurementOntId(phenotypeAccId);
            if (parentStrainName.toLowerCase().contains("normalstrain")) {
                range.setExpectedRangeName(parentStrainName+ "_"+sex);
            }
            else {
                String expectedRangeName=phenotype + "_"+sex+"_"+ageLow+"-"+ageHigh+" days";
                if(method!=null){
                    expectedRangeName+="_"+method;
                }
                range.setExpectedRangeName(expectedRangeName);
            }
            range.setStrainGroupName(parentStrainName);
            range.setStrainGroupId(strainGroupId);
            range.setExperimentRecords(records);
            range.setSex(sex);
            range.setAgeLowBound(ageLow);
            range.setAgeHighBound(ageHigh);
            range.setTraitOntId(traitOntId);
            range.setUnits(rangeUnits.toString());
            range.setTraitAncestors(traitAncestors);

        }
        return range;

    }


    public PhenominerExpectedRange getNormalRangeValues(List<PhenominerExpectedRange> records, String phenotypeAccId,int strainGroupId, String sex,String method, int ageLow, int ageHigh, String traitOntId, String rangeUnits,  boolean isNormal) throws Exception {
        PhenominerExpectedRange  range= rangeValues.getNormalRangeValues(records, isNormal);
        String phenotype=getTerm(phenotypeAccId).getTerm();
        String parentStrainName= strainGroupDao.getStrainGroupName(strainGroupId);
        if(range!=null) {
            range.setClinicalMeasurement(phenotype);
            range.setClinicalMeasurementOntId(phenotypeAccId);
            if (parentStrainName.toLowerCase().contains("normalstrain")) {
                range.setExpectedRangeName(parentStrainName+ "_"+sex);
            }
         /*   else {
                String expectedRangeName=phenotype + "_"+sex+"_"+ageLow+"-"+ageHigh+" days";
                if(method!=null){
                    expectedRangeName+="_"+method;
                }
                range.setExpectedRangeName(expectedRangeName);
            }*/
            range.setStrainGroupName(parentStrainName);
            range.setStrainGroupId(strainGroupId);
       //     range.setExperimentRecords(records);
            range.setSex(sex);
            range.setAgeLowBound(ageLow);
            range.setAgeHighBound(ageHigh);
            range.setTraitOntId(traitOntId);
            range.setUnits(rangeUnits);
          //  range.setTraitAncestors(traitAncestors);

        }
        return range;

    }
    public List<TraitObject> getTraitAncester(String traitOntId) throws Exception {

        List<TermWithStats> firstLevelTraitTerms= PhenotypeTrait.getInstance().getGetFirstLevelTraitTerms();
        List<String> traitAncestors= getAllActiveTermAncestorAccIds(traitOntId);
        List<TraitObject> traitObjects= new ArrayList<>();

        for(String s:traitAncestors){
        for(TermWithStats ts: firstLevelTraitTerms){
            List<TermWithStats> secondLevelTraitTerms=getActiveChildTerms(ts.getAccId(), 3);
              for(TermWithStats t:secondLevelTraitTerms){
                  List<TermWithStats> thirdLevelTraitTerms=getActiveChildTerms(t.getAccId(), 3);
                 for(TermWithStats term:thirdLevelTraitTerms){
                     if(s.equalsIgnoreCase(term.getAccId())){
                        traitObjects.add(getTraitObject(t, term));
                     }
                 }
                      }

            }
      }
            if(traitObjects.size()==0){
            for(String s:traitAncestors) {
                for (TermWithStats ts : firstLevelTraitTerms) {
                    List<TermWithStats> secondLevelTraitTerms = getActiveChildTerms(ts.getAccId(), 3);
                    for (TermWithStats t1 : secondLevelTraitTerms) {
                        if (s.equalsIgnoreCase(t1.getAccId())) {
                            traitObjects.add(getTraitObject(t1, getTerm(traitOntId)));
        }
        }

        }
        }
        }
        if(traitObjects.size()==0 && traitOntId!=null){
            if(!traitOntId.equals("")){
                traitObjects.add(getTraitObject(getTerm(traitOntId), getTerm(traitOntId)));
            }
        }

        return traitObjects;
    }

    public TraitObject getTraitObject(Term trait, Term subttrait){
        TraitObject obj= new TraitObject();
        if(trait!=null)
        obj.setTrait(trait);
        if(subttrait!=null)
        obj.setSubTrait(subttrait);
        return obj;
    }

     public List<String> insertAllNormalStrainGroups(List<String> strainGroups, String strainGroupName, int strainGroupId) throws Exception {
         List<String> rsIds=new ArrayList<>();
         for(String strain:strainGroups){

             List<PhenominerStrainGroup> strains= strainGroupDao.getStrainGroupByName(strain);
             if(strains.size()>0) {

                 for (PhenominerStrainGroup s : strains) {
                     PhenominerStrainGroup strainGroup = new PhenominerStrainGroup();
                      strainGroup.setId(strainGroupId);
                     strainGroup.setName(strainGroupName);
                     strainGroup.setStrain_ont_id(s.getStrain_ont_id());
                     rsIds.add(s.getStrain_ont_id());
                     //  System.out.println(s.getStrain_ont_id() +"\t"+ getTerm(s.getStrain_ont_id()).getTerm());
                     strainGroupDao.insertOrUpdate(strainGroup);
                 }
             }
         }
     return rsIds;
    }




    public List<PhenominerExpectedRange> getNormalStrainsRanges1(List<String> xcoTerms, List<String> mmoTerms, PhenotypeTrait phenotypeTrait) throws Exception {
        ExpectedRangeProcess process= new ExpectedRangeProcess();
        List<PhenominerExpectedRange> normalRanges= new ArrayList<>();

        for(Map.Entry e: NormalStrainGroup.phenotypeNormalStrainsMap.entrySet()){

                    String cmoAccId= (String) e.getKey();

            List<String> strainGroups= (List<String>) e.getValue();

            String phenotype=getTerm(cmoAccId).getTerm();
            String normalStrainGroupName="NormalStrain_"+phenotype;

            Map<String, List<String>> normalStrainGroupMap= new HashMap<>();
            List<String> strainOntIds=this.getNormalStrainGroupOntIds(strainGroups);

            normalStrainGroupMap.put(normalStrainGroupName, strainOntIds);
            int strainGroupId= process.insertOrUpdateStrainGroup(normalStrainGroupMap, true);
            normalRanges.addAll(this.getNormalRanges(strainOntIds,mmoTerms, cmoAccId,xcoTerms, phenotypeTrait,strainGroupId));


        }

         return normalRanges;
    }

    public List<PhenominerExpectedRange> getNormalStrainsRanges1(List<String> xcoTerms, List<String> mmoTerms, Map phenotypeTrait, String cmoAccId) throws Exception {
        ExpectedRangeProcess process= new ExpectedRangeProcess();
        List<PhenominerExpectedRange> normalRanges= new ArrayList<>();
        if(NormalStrainGroup.phenotypeNormalStrainsMap.get(cmoAccId)!=null) {
            List<String> strainGroups = NormalStrainGroup.phenotypeNormalStrainsMap.get(cmoAccId);
            String phenotype = getTerm(cmoAccId).getTerm();
            String normalStrainGroupName = "NormalStrain_" + phenotype;
            Map<String, List<String>> normalStrainGroupMap = new HashMap<>();
            List<String> strainOntIds = this.getNormalStrainGroupOntIds(strainGroups);

            normalStrainGroupMap.put(normalStrainGroupName, strainOntIds);
            int strainGroupId = process.insertOrUpdateStrainGroup(normalStrainGroupMap, true);
       //    normalRanges.addAll(this.getNormalRanges(strainOntIds, mmoTerms, cmoAccId, xcoTerms, phenotypeTrait, strainGroupId));
                normalRanges.addAll(this.getNormalRanges(strainGroups, cmoAccId, strainGroupId));
      //      normalRanges.addAll(this.getNormalRanges(strainGroups, cmoAccId, strainGroupId, xcoTerms, mmoTerms, phenotypeTrait));
        }
    //    System.out.println("NORMAL RANGES SIZE: "+ normalRanges.size());
        return normalRanges;
    }
    public List<PhenominerExpectedRange> getNormalRanges(List<String> strainGroups, String phenotypeAccId, int strainGroupId, List<String> xcoTerms, List<String> mmoTerms, Map phenotypeTrait) throws Exception {
        List<PhenominerExpectedRange> ranges= new ArrayList<>();
        Map<String, List<PhenominerExpectedRange>> map= this.getRangesByStrainGroups(strainGroups, phenotypeAccId,xcoTerms, mmoTerms,phenotypeTrait);
               for (Map.Entry e : map.entrySet()) {
            String cat = (String) e.getKey();
            List<PhenominerExpectedRange> recs = (List<PhenominerExpectedRange>) e.getValue();
            if (recs.size() > 0) {
                String traitOntId = recs.get(0).getTraitOntId();
                String rangeUnits = recs.get(0).getUnits();
                List<TraitObject> traitAncestors = recs.get(0).getTraitAncestors();
                String sex = "Mixed";
                int ageLow = 0;
                int ageHigh = 999;
                if (cat.equalsIgnoreCase("male") || cat.equalsIgnoreCase("female") || cat.equals("overall")) {
                    if (cat.equalsIgnoreCase("female")) {
                        sex = "Female";
                    }
                    if (cat.equalsIgnoreCase("male")) {
                        sex = "Male";
                    }
                    System.out.println("CATEGORY: "+cat +" -\t"+ recs.size()+"\t***********************");
                    for(PhenominerExpectedRange r:recs){
                        System.err.println(r.getStrainGroupName()+"\t"+ r.getRangeValue()+"\t"+r.getRangeLow()+"\t"+ r.getRangeHigh());
                    }

                 //     if(cat.equalsIgnoreCase("overall"))
                    ranges.add(this.getNormalRangeValues(recs, phenotypeAccId, strainGroupId, sex, null, ageLow, ageHigh, traitOntId, rangeUnits,  true));
                }

            }
        }

        return ranges;
    }
   public List<PhenominerExpectedRange> getNormalRanges(List<String> strainGroups, String phenotypeAccId, int strainGroupId) throws Exception {
        List<PhenominerExpectedRange> ranges= new ArrayList<>();
        Map<String, List<PhenominerExpectedRange>> map= this.getRangesByStrainGroups(strainGroups, phenotypeAccId);
             for (Map.Entry e : map.entrySet()) {
                String cat = (String) e.getKey();
                List<PhenominerExpectedRange> recs = (List<PhenominerExpectedRange>) e.getValue();
                if (recs.size() > 0) {
                    String traitOntId = recs.get(0).getTraitOntId();
                    String rangeUnits = recs.get(0).getUnits();
                //    List<TraitObject> traitAncestors = recs.get(0).getTraitAncestors();

                    String sex = "Mixed";
                    int ageLow = 0;
                    int ageHigh = 999;
                    if (cat.equalsIgnoreCase("male") || cat.equalsIgnoreCase("female") || cat.equals("overall")) {
                        if (cat.equalsIgnoreCase("female")) {
                            sex = "Female";
                        }
                        if (cat.equalsIgnoreCase("male")) {
                            sex = "Male";
                        }
                    //   if(cat.equalsIgnoreCase("overall"))
                        ranges.add(this.getNormalRangeValues(recs, phenotypeAccId, strainGroupId, sex, null, ageLow, ageHigh, traitOntId, rangeUnits,  true));
                    }

                }
            }

        return ranges;
    }
    public Map<String, List<PhenominerExpectedRange>> getRangesByStrainGroups(List<String> strainGroups, String phenotypeAccId) throws Exception {
        PhenominerExpectedRangeDao rdao = new PhenominerExpectedRangeDao();
        List<Integer> strainGroupdIds = new ArrayList<>();
        for (String s : strainGroups) {
            strainGroupdIds.add(strainGroupDao.getStrainGroupId(s));
        }
        List<PhenominerExpectedRange> overall = new ArrayList<>();
        List<PhenominerExpectedRange> male = new ArrayList<>();
        List<PhenominerExpectedRange> female = new ArrayList<>();
        Map<String, List<PhenominerExpectedRange>> map = new HashMap<>();
        if(strainGroupdIds.size()>1) {
            for (int id : strainGroupdIds) {
                List<PhenominerExpectedRange> ranges = rdao.getExpectedRanges(phenotypeAccId, id, 0, 999);

                for (PhenominerExpectedRange r : ranges) {

                    if (r.getSex().equalsIgnoreCase("male")) {
                        male.add(r);
                    }
                    if (r.getSex().equalsIgnoreCase("female")) {
                        female.add(r);
                    }
                    if (r.getSex().equalsIgnoreCase("mixed")) {
                        if(!r.getExpectedRangeName().contains("tail") && !r.getExpectedRangeName().contains("vascular"))
                        overall.add(r);
                    }
                }
            }

            map.put("overall", overall);
            map.put("male", male);
            map.put("female", female);
        }
        return map;
    }

    public Map<String, List<PhenominerExpectedRange>> getRangesByStrainGroups(List<String> strainGroups, String phenotypeAccId, List<String> xcoTerms, List<String> mmoTerms, Map phenotypeTrait) throws Exception {
        PhenominerExpectedRangeDao rdao = new PhenominerExpectedRangeDao();
        List<Integer> strainGroupdIds = new ArrayList<>();
        for (String s : strainGroups) {
            strainGroupdIds.add(strainGroupDao.getStrainGroupId(s));
        }
        List<PhenominerExpectedRange> overall = new ArrayList<>();
        List<PhenominerExpectedRange> male = new ArrayList<>();
        List<PhenominerExpectedRange> female = new ArrayList<>();
        Map<String, List<PhenominerExpectedRange>> map = new HashMap<>();
        if(strainGroupdIds.size()>1) {
            for (int id : strainGroupdIds) {
                List<PhenominerExpectedRange> ranges = rdao.getExpectedRanges(phenotypeAccId, id, 0, 999);
                if (ranges.size() == 0) {
                    ranges.addAll(this.getExcludedStrainGroupRanges(phenotypeAccId, id, xcoTerms, mmoTerms, phenotypeTrait));

                }
                for (PhenominerExpectedRange r : ranges) {
                    if (r != null) {
                        if (r.getSex().equalsIgnoreCase("male")) {
                            male.add(r);
                        }
                        if (r.getSex().equalsIgnoreCase("female")) {
                            female.add(r);
                        }
                        if (r.getSex().equalsIgnoreCase("mixed")) {
                            if (!r.getExpectedRangeName().contains("tail") && !r.getExpectedRangeName().contains("vascular"))
                                overall.add(r);
                        }
                    }
                }
            }
            //System.err.println(xdao.getTerm(phenotypeAccId).getTerm() + "\tOVERALL: " + overall.size() + "\t" + "FEMALE:" + female.size() + "\tMALE: " + male.size());
            map.put("overall", overall);
            map.put("male", male);
            map.put("female", female);
        }
        return map;
    }
    public List<PhenominerExpectedRange> getExcludedStrainGroupRanges(String phenotypeAccId, int id, List<String> xcoTerms, List<String> mmoTerms, Map phenotypeTrait) throws Exception {
     return    getExcludedStrainRanges1(xcoTerms,  mmoTerms, phenotypeTrait, phenotypeAccId,  id);

    }
    public List<PhenominerExpectedRange> getExcludedStrainRanges1(List<String> xcoTerms, List<String> mmoTerms, Map phenotypeTrait, String cmoAccId, int id) throws Exception {
        ExpectedRangeProcess process= new ExpectedRangeProcess();
        List<PhenominerExpectedRange> normalRanges= new ArrayList<>();
        if(NormalStrainGroup.phenotypeNormalStrainsMap.get(cmoAccId)!=null) {
            String phenotype = getTerm(cmoAccId).getTerm();
            String normalStrainGroupName = strainGroupDao.getStrainGroupName(id);
            Map<String, List<String>> normalStrainGroupMap = new HashMap<>();
            List<String> strainOntIds= new ArrayList<>();
            List<String> ontIds=strainGroupDao.getStrainsOfStrainGroup(id);
            for(String i:ontIds){
                if(!strainOntIds.contains(i)){
                    strainOntIds.add(i);
                }
            }

            normalStrainGroupMap.put(normalStrainGroupName, strainOntIds);

               normalRanges.addAll(this.getNormalRanges(strainOntIds, mmoTerms, cmoAccId, xcoTerms, phenotypeTrait, id));

        }

        return normalRanges;
    }

    public List<PhenominerExpectedRange> getNormalRanges(List<String> strainOntIds, List<String> mmoTerms, String cmoAccId, List<String> xcoTerms, PhenotypeTrait phenotypeTrait, int strainGroupId) throws Exception {
        List<String> cmoIds= new ArrayList<>(Arrays.asList(cmoAccId));
        List<PhenominerExpectedRange> ranges= new ArrayList<>();
        List<Record> records= new ArrayList<>();
        try{
            records = pdao.getFullRecords(strainOntIds, mmoTerms, cmoIds, xcoTerms, 3);
        }catch (Exception exp){
            System.err.println(cmoIds.get(0)+"\t"+ strainOntIds.size());
            exp.printStackTrace();
        }
        String traitOntId= this.getTraitOntId(records, cmoAccId, phenotypeTrait.getPhenotypeTraitMap());

        List<TraitObject> traitAncestors= getTraitAncester(traitOntId);
        Set<String> rangeUnits= this.getRangeUnits(records);
        Map<String,List<Record>> categories= categorizeRecords(records);

         for(Map.Entry entry: categories.entrySet()) {
            String cat = (String) entry.getKey();
            String sex = "Mixed";
            int ageLow = 0;
            int ageHigh = 999;
            //    if (cat.equals("overall")){
            if (cat.equalsIgnoreCase("male") || cat.equalsIgnoreCase("female") || cat.equals("overall")){
                if (cat.equalsIgnoreCase("female")) {
                    sex = "Female";
                }
                if (cat.equalsIgnoreCase("male")) {
                    sex = "Male";
                }
           /*     if (cat.equals("vascular")) {
                    method = "vascular";
                }
                if (cat.equals("tail")) {
                    method = "tail";
                }
                if (cat.equals("age1")) {
                    ageHigh = 79;
                }
                if (cat.equals("age2")) {
                    ageLow = 80;
                    ageHigh = 99;
                }
                if (cat.equals("age3")) {
                    ageLow = 100;
                    ageHigh = 999;
                }*/

                List<Record> recs = (List<Record>) entry.getValue();
                if(recs.size()>0)
                    ranges.add(this.getRange(recs, cmoAccId, strainGroupId, sex, null, ageLow, ageHigh, traitOntId, rangeUnits, traitAncestors, true));
            }
        }
     //   normalRanges.addAll(ranges);
        return ranges;
    }

    public List<PhenominerExpectedRange> getNormalRanges(List<String> strainOntIds, List<String> mmoTerms, String cmoAccId, List<String> xcoTerms, Map phenotypeTrait, int strainGroupId) throws Exception {
        List<String> cmoIds= new ArrayList<>(Arrays.asList(cmoAccId));
        List<PhenominerExpectedRange> ranges= new ArrayList<>();
        List<Record> records= new ArrayList<>();
        try{
            records = pdao.getFullRecords(strainOntIds, mmoTerms, cmoIds, xcoTerms, 3);
        }catch (Exception exp){
            System.err.println(cmoIds.get(0)+"\t"+ strainOntIds.size());
            exp.printStackTrace();
        }
        String traitOntId= this.getTraitOntId(records, cmoAccId, phenotypeTrait);

        List<TraitObject> traitAncestors= getTraitAncester(traitOntId);
        Set<String> rangeUnits= this.getRangeUnits(records);
        Map<String,List<Record>> categories= categorizeRecords(records);

        for(Map.Entry entry: categories.entrySet()) {
            String cat = (String) entry.getKey();
            String sex = "Mixed";
            int ageLow = 0;
            int ageHigh = 999;
    //   if (cat.equalsIgnoreCase("overall")) {
                  if (cat.equalsIgnoreCase("male") || cat.equalsIgnoreCase("female") || cat.equals("overall")) {
                      if (cat.equalsIgnoreCase("female")) {
                          sex = "Female";
                      }
                      if (cat.equalsIgnoreCase("male")) {
                          sex = "Male";
                      }
           /*     if (cat.equals("vascular")) {
                    method = "vascular";
                }
                if (cat.equals("tail")) {
                    method = "tail";
                }
                if (cat.equals("age1")) {
                    ageHigh = 79;
                }
                if (cat.equals("age2")) {
                    ageLow = 80;
                    ageHigh = 99;
                }
                if (cat.equals("age3")) {
                    ageLow = 100;
                    ageHigh = 999;
                }*/

                      List<Record> recs = (List<Record>) entry.getValue();

                      if (recs.size() > 0)
                    System.out.println(strainGroupDao.getStrainGroupName(strainGroupId)+"\t"+cat+"\t");
                          ranges.add(this.getRange(recs, cmoAccId, strainGroupId, sex, null, ageLow, ageHigh, traitOntId, rangeUnits, traitAncestors, true));
                  }
            }
    //    }
        //   normalRanges.addAll(ranges);
        return ranges;
    }

    public List<String> getNormalStrainGroupOntIds(List<String> strainGroups) throws Exception {
        List<String> strainOntIds=new ArrayList<>();
        for(String strainGroupName:strainGroups) {
            int id= strainGroupDao.getStrainGroupId(strainGroupName);
            List<String> ontIds=strainGroupDao.getStrainsOfStrainGroup(id);
            for(String i:ontIds){
                if(!strainOntIds.contains(i)){
                    strainOntIds.addAll(ontIds);
                }
            }

        }
        return strainOntIds;
    }

    public List<PhenominerExpectedRange> getExpectedRangeOfMixedAndAll(int strainGroupId, String phenotype) throws Exception {
      return dao.getExpectedRanges(phenotype, strainGroupId, 0, 999);

    }
    public List<PhenominerExpectedRange> getSortedRangesBySex(List<PhenominerExpectedRange> ranges, String cmoAccId, int newStrainGroupId) throws Exception {
       PhenominerDAO phenominerDAOExt = new PhenominerDAO();
        String phenotype=getTerm(cmoAccId).getTerm();
        List<PhenominerExpectedRange> normalRanges= new ArrayList<>();
        Map<String, List<PhenominerExpectedRange>> rangeMap= new HashMap<>();
        List<PhenominerExpectedRange> maleRanges=new ArrayList<>();
        List<PhenominerExpectedRange> femaleRanges=new ArrayList<>();
        List<PhenominerExpectedRange> mixedRanges=new ArrayList<>();
        for(PhenominerExpectedRange r:ranges){
          List<Integer> experimentRecIds=  dao.getExperimentRecordIds(r.getExpectedRangeId());
            List<Record> records= phenominerDAOExt.getFullRecords(experimentRecIds);
            r.setExperimentRecords(records);
            if(r.getSex().equalsIgnoreCase("Male")){
                maleRanges.add(r);
            }
            if(r.getSex().equalsIgnoreCase("Female")){
                femaleRanges.add(r);
            }
            if(r.getSex().equalsIgnoreCase("Mixed")){
                mixedRanges.add(r);
            }
            if(maleRanges.size()>0)
            rangeMap.put("Male", maleRanges);
            if(femaleRanges.size()>0)
            rangeMap.put("Female", femaleRanges);
            if(mixedRanges.size()>0)
            rangeMap.put("Mixed", mixedRanges);
            //   System.out.println(r.getExpectedRangeName()+"\t"+r.getRangeValue()+"\t"+ r.getRangeLow()+"\t"+ r.getRangeHigh());
        }
        /**************************************************************************************/
        for(Map.Entry entry:rangeMap.entrySet()){
            List<Record> experimentRecords= new ArrayList<>();
            int recordCount=0;
            PhenominerExpectedRange normalRange= new PhenominerExpectedRange();
            List<Double> rangesLow=new ArrayList<>();
            List<Double> rangesHigh=new ArrayList<>();
            Double rangeValue=0.0;
            Double rangeSD=0.0;
            String sex= (String) entry.getKey();
            List<PhenominerExpectedRange> ranges1= (List<PhenominerExpectedRange>) entry.getValue();
            for(PhenominerExpectedRange r:ranges1){
                rangesLow.add(r.getRangeLow());
                rangesHigh.add(r.getRangeHigh());

                rangeValue=rangeValue+r.getRangeValue();
                rangeSD=rangeSD+r.getRangeSD();
                experimentRecords.addAll(r.getExperimentRecords());
                recordCount++;
            }

            rangeValue=rangeValue/recordCount;
            rangeSD=rangeSD/recordCount;
            String expectedRangeName="NormalStrain_"+phenotype+"_"+sex+"_allAges";
            normalRange.setStrainGroupId(newStrainGroupId);
            normalRange.setClinicalMeasurementOntId(cmoAccId);
            normalRange.setClinicalMeasurement(phenotype);
            normalRange.setAgeLowBound(0);
            normalRange.setAgeHighBound(999);
            normalRange.setSex(sex);
            normalRange.setExpectedRangeName(expectedRangeName);
            normalRange.setRangeLow(Collections.min(rangesLow));
            normalRange.setRangeHigh(Collections.max(rangesHigh));
            normalRange.setRangeValue(rangeValue);
            normalRange.setRangeSD(rangeSD);
            normalRange.setExperimentRecords(experimentRecords);
            normalRanges.add(normalRange);

        }
        return normalRanges;
    }

    public int insert(List<PhenominerExpectedRange> ranges) throws Exception {
        int status=0;

       for(PhenominerExpectedRange range:ranges) {
            if (range != null) {
                int expectedRangeId = getExpectedRangeId(range);
                if (expectedRangeId == 0) {
                    expectedRangeId = dao.getNextKey("PHENOMINER_EXPECTED_RANGE_SEQ");
                    range.setExpectedRangeId(expectedRangeId);
                    status += dao.insert(range);
                } else {

                    dao.updateExpectedRange(range, expectedRangeId);
                }
                if (range.getExperimentRecords() != null && range.getExperimentRecords().size() > 0) {
                    for (Record r : range.getExperimentRecords()) {
                        updateExpectedRangeExperiment(expectedRangeId, r.getId());
                    }
                }
                if (range.getTraitAncestors() != null && range.getTraitAncestors().size() > 0) {
                    for (TraitObject o : range.getTraitAncestors()) {
                        if (o.getTrait() != null && o.getSubTrait() != null)
                            insertOrUpdateRangeTraits(o, expectedRangeId);
                    }
                }
              /*  if (range.getExpectedRangeName().toLowerCase().contains("normalstrain"))
                    System.out.println(expectedRangeId + "\t" + range.getRangeValue() + "\t" + range.getRangeLow() + "\t" + range.getRangeHigh());*/
          }
        }

        return status;
    }


    public int getExpectedRangeId(PhenominerExpectedRange range) throws Exception {
      return   dao.getExpectedRangeId(range);
    }


    public int updateExpectedRangeExperiment(int expectedRangeId, int experimentRecordId) throws Exception {

        if(dao.getPhenominerRangeExperimentRecCount(experimentRecordId, expectedRangeId)>0){
            return 0;
        }else{
        return dao.insertExpectedRangeExperiment(expectedRangeId, experimentRecordId);}
    }

    public int insertOrUpdateRangeTraits(TraitObject o, int expectedRangeId) throws Exception {

        List<TraitObject> traitObjects= dao.getPhenominerRangeTraitAncestors(o, expectedRangeId);
        if(traitObjects.size()==0)
        return dao.insertRangeTrait(o,expectedRangeId);
       else return 0;

    }
    public static List<String> getVascularTermIds() {
        return vascularTermIds;
    }

    public static void setVascularTermIds(List<String> vascularTermIds) {
        PhenotypeExpectedRangeDao.vascularTermIds = vascularTermIds;
    }

    public static List<String> getTailTermIds() {
        return tailTermIds;
    }

    public static void setTailTermIds(List<String> tailTermIds) {
        PhenotypeExpectedRangeDao.tailTermIds = tailTermIds;
    }
    public void printResultsMatrix(List<String> phenotypes, List<PhenominerExpectedRange> ranges) throws Exception {
        List<String> phenotypeNames=new ArrayList<>();
        //  List<String> phenotypes1= new ArrayList<>(Arrays.asList("CMO:0000004"));
        for(String cmo:phenotypes){
            String term=getTerm(cmo).getTerm();
            phenotypeNames.add(term+"_Mixed_0-999 days");
            phenotypeNames.add(term+"_Mixed_0-79 days");
            phenotypeNames.add(term+"_Mixed_80-99 days");
            phenotypeNames.add(term+"_Mixed_100-999 days");
            phenotypeNames.add(term+"_Male_0-999 days");
            phenotypeNames.add(term+"_Female_0-999 days");
            phenotypeNames.add(term+"_Mixed_0-999 days_vascular");
            phenotypeNames.add(term+"_Mixed_0-999 days_tail");
        }
        List<String> strainGroupNames= strainGroupDao.getAllDistinctStrainGroupNames();
        //    String[][] matrix= new String[ranges.size()+1][strainGroupNames.size()+1];
        String[][] matrix= new String[phenotypeNames.size()+1][strainGroupNames.size()+1];
        System.out.println("RANGES SIZE:"+ ranges.size());
        System.out.println("STRAIN GROUPS SIZE: "+ strainGroupNames.size());



        int i=0;
        matrix[0][0]="phenotype" ;
        int j=0;
        for (String strain : strainGroupNames) {
            matrix[0][j+1]=strain;
            j++;
        }
        for(String name:phenotypeNames){
            matrix[i+1][0]=name;
            i++;
        }

        for(int n=0; n<strainGroupNames.size();n++){
            int phenotypeCount=0;
            String strain=matrix[0][n+1];
            for(int m=0;m<phenotypeNames.size();m++){

                String phenotypeName=matrix[m+1][0];

                // System.out.println(matrix[0][n+1] +"\t"+ matrix[m+1][0]);
                for(PhenominerExpectedRange range:ranges){
                    if(range.getExpectedRangeName().equalsIgnoreCase(phenotypeName)&& strain.equalsIgnoreCase(range.getStrainGroupName())){
                        matrix[m+1][n+1]=range.getRangeLow()+"|"+range.getRangeHigh() + " ("+ range.getExperimentRecords().size()+")";
                        phenotypeCount++;
                    }

                }
            }
            matrix[0][n+1]=strain+"("+phenotypeCount+")";

        }
        try {
            FileWriter fos= new FileWriter("C:/Apps/expectedRanges.tab");
            PrintWriter dos= new PrintWriter(fos);
            for (int k = 0; k <= phenotypeNames.size(); k++) {
                int count=0;
                for (int m = 0; m <= strainGroupNames.size(); m++) {

                    if (matrix[k][m] == null) {
                        dos.print("-" + "\t");
                        // System.out.print("-" + "\t");
                    } else {
                        count++;
                        dos.print(matrix[k][m] + "\t");
                        // System.out.print(matrix[k][m] + "\t");
                    }
                }
                dos.print(count-1);
                dos.print("\n");
                // System.out.print("\n");
            }
            dos.close();
            fos.close();
        }catch (Exception e){

        }

    }

    public static  void main(String[] args) throws Exception {
        PhenotypeExpectedRangeDao dao= new PhenotypeExpectedRangeDao();
        List<TraitObject> traitObjects=dao.getTraitAncester("VT:0005535");
        System.out.println("TRAIT ANCESTORS SIZE: "+ traitObjects.size());
        for(TraitObject t: traitObjects){
            System.out.println(t.getTrait().getAccId()+"\t"+ t.getTrait().getTerm() +"\t" + t.getSubTrait().getAccId()+"\t"+ t.getSubTrait().getTerm());
        }
        System.out.println("DONE");
    }

}
