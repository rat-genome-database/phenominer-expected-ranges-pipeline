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
import edu.mcw.rgd.phenominerExpectedRanges.Range;
import edu.mcw.rgd.phenominerExpectedRanges.model.*;
import edu.mcw.rgd.phenominerExpectedRanges.process.ExpectedRangeProcess;

import java.util.*;

import static edu.mcw.rgd.phenominerExpectedRanges.Manager.log;


/**
 * Created by jthota on 3/1/2018.
 */
public class PhenotypeExpectedRangeDao extends OntologyXDAO {
    PhenominerDAO pdao= new PhenominerDAO();
    PhenominerStrainGroupDao strainGroupDao= new PhenominerStrainGroupDao();
    PhenominerExpectedRangeDao dao= new PhenominerExpectedRangeDao();


    RangeValues rangeValues= new RangeValues();

    private static List<String> vascularTermIds;
    private static List<String> tailTermIds;


    public Map<String, List<Term>> getInbredStrainGroupMap1(String termAcc) throws Exception {

        List<TermWithStats> topLevelTerms=getActiveChildTerms(termAcc,3);

        Map<String, List<Term>> strainGroupMap= new HashMap<>();
        for(TermWithStats p: topLevelTerms){
                   List<Term> rsTerms=new ArrayList<>();
      // String term_acc="RS:0000113";
       List<Term> termAndChildTerms= getAllActiveTermDescendants(p.getAccId());
      //   List<Term> termAndChildTerms= getAllActiveTermDescendants(term_acc);
           termAndChildTerms.add(getTerm(p.getAccId()));
     //termAndChildTerms.add(getTerm(term_acc));
            for(Term t: termAndChildTerms){
                rsTerms.add(t);
          //    System.out.println(t.getAccId() + "||" +t.getTerm());
            }
       //   strainGroupMap.put(term_acc, rsTerms);

        strainGroupMap.put(p.getAccId(), rsTerms);
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
   public List<PhenominerExpectedRange> getSummaryRanges(List<Record> records, String phenotypeAccId, int strainGroupId, Map<String, String> phenotypeTraitMap) throws Exception {

        List<PhenominerExpectedRange> ranges= new ArrayList<>();
        String traitOntId= this.getTraitOntId(records, phenotypeAccId, phenotypeTraitMap);

        List<TraitObject> traitAncestors= getTraitAncester(traitOntId);
        Set<String> rangeUnits= this.getRangeUnits(records);
        Map<String,List<Record>> categories= categorizeRecords(records);

        for(Map.Entry e: categories.entrySet()){
            String cat= (String) e.getKey();
            String sex="Mixed";
            String method=null;
            int ageLow=0;
            int ageHigh=999;
            if(cat.equalsIgnoreCase("female")){
                sex="Female";
                }
            if(cat.equalsIgnoreCase("male")){
                sex="Male";
                }
            if(cat.equals("vascular")){
                method="vascular";
            }
            if(cat.equals("tail")){
               method="tail";
        }
            if(cat.equals("age1")){
                ageHigh=79;
            }
            if(cat.equals("age2")){
               ageLow=80; ageHigh=99;
            }
            if(cat.equals("age3")){
               ageLow=100; ageHigh=999;
            }
            List<Record> recs= (List<Record>) e.getValue();
            if(recs.size()>=4) {

                ranges.add(this.getRange(recs, phenotypeAccId, strainGroupId, sex, method,ageLow, ageHigh, traitOntId, rangeUnits, traitAncestors));
            }
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

    public Map<String,List<Record>> categorizeRecords(List<Record> records){
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

    for(Record r:records){
          rangeUnits.add(r.getMeasurementUnits());
        if(vascularTermIds.contains(r.getMeasurementMethod().getAccId())){
            if(r.getMeasurementSD()!=null && r.getMeasurementValue()!=null) {
                if(Double.parseDouble(r.getMeasurementSD())>0.0)
                    vascularRecords.add(r);
            }

        }
        if(tailTermIds.contains(r.getMeasurementMethod().getAccId())){
            if(r.getMeasurementSD()!=null && r.getMeasurementValue()!=null) {
                if(Double.parseDouble(r.getMeasurementSD())>0.0)
                tailRecords.add(r);
            }
        }
        if(r.getMeasurementSD()!=null && r.getMeasurementValue()!=null){
            if(Double.parseDouble(r.getMeasurementSD())>0.0)
            overAllRecords.add(r);

        }
        if(r.getSample().getSex().equalsIgnoreCase("male")){
            if(r.getMeasurementSD()!=null && r.getMeasurementValue()!=null) {
                if(Double.parseDouble(r.getMeasurementSD())>0.0)
                maleRecords.add(r);
            }
        }else{
            if(r.getSample().getSex().equalsIgnoreCase("female")){
                if(r.getMeasurementSD()!=null && r.getMeasurementValue()!=null) {
                    if(Double.parseDouble(r.getMeasurementSD())>0.0)
                       femaleRecords.add(r);
                }
            }else{
                unspecifiedSexRecords.add(r);
            }
        }

        int ageLow= 0;
        if(r.getSample().getAgeDaysFromLowBound()!=null){
            ageLow=r.getSample().getAgeDaysFromLowBound();
        }
        int ageHigh= 0;
        if(r.getSample().getAgeDaysFromHighBound()!=null){
            ageHigh=r.getSample().getAgeDaysFromHighBound();
        }
        if(ageLow>=0 && ageHigh<=79){
            if(r.getMeasurementSD()!=null && r.getMeasurementValue()!=null) {
                if(Double.parseDouble(r.getMeasurementSD())>0.0)
                age1Records.add(r);
            }
        }
        if(ageLow>=80 && ageHigh<=99){
            if(r.getMeasurementSD()!=null && r.getMeasurementValue()!=null) {
                if(Double.parseDouble(r.getMeasurementSD())>0.0)
                age2Records.add(r);
            }
        }
        if(ageLow>=100 && ageHigh<=999){
            if(r.getMeasurementSD()!=null && r.getMeasurementValue()!=null) {
                if(Double.parseDouble(r.getMeasurementSD())>0.0)
                age3Records.add(r);
            }
        }

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
    public PhenominerExpectedRange getRange(List<Record> records, String phenotypeAccId,int strainGroupId, String sex,String method, int ageLow, int ageHigh, String traitOntId, Set<String> rangeUnits, List<TraitObject> traitAncestors) throws Exception {
      PhenominerExpectedRange  range= rangeValues.getRangeValues(records);
        String phenotype=getTerm(phenotypeAccId).getTerm();
        String parentStrainName= strainGroupDao.getStrainGroupName(strainGroupId);
        if(range!=null) {
            range.setClinicalMeasurement(phenotype);
            range.setClinicalMeasurementOntId(phenotypeAccId);
            if (parentStrainName.toLowerCase().contains("normalstrain"))
                range.setExpectedRangeName(parentStrainName + "_" + phenotype );
            else {
            //    String expectedRangeName=parentStrainName + "_"+sex+"_"+ageLow+"-"+ageHigh+" days";
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
//            range.setTrait(getTermByAccId(traitOntId).getTerm());
            range.setTraitOntId(traitOntId);
            range.setUnits(rangeUnits.toString());
            range.setTraitAncestors(traitAncestors);

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

    /**
     * First inserts normal strain groups in phenominer_strain_group and then caculated expected ranges.
     * @return
     * @throws Exception
     */
    public List<PhenominerExpectedRange> getNormalStrainsRanges() throws Exception {
        PhenominerDAO pdao= new PhenominerDAO();
        List<String> measurementMethds= getMeasurementMethods();
        List<String> conditions= getConditons("XCO:0000099");

        List<PhenominerExpectedRange> ranges= new ArrayList<>();
        for(Map.Entry e: NormalStrainGroup.phenotypeNormalStrainsMap.entrySet()){
            int strainGroupId = getNextKey("PHENOMINER_STRAIN_GROUP_SEQ");

            String cmoAccId= (String) e.getKey();
            List<String> strainGroups= (List<String>) e.getValue();
            List<String> cmoIds=new ArrayList<>(Arrays.asList(cmoAccId));
            String phenotype=getTerm(cmoAccId).getTerm();

            String strainGroupName="NormalStrain_"+phenotype +"_mixed_ageAll";

            List<String> rsIds=insertAllNormalStrainGroups(strainGroups, strainGroupName, strainGroupId);

            List<Record> records= pdao.getFullRecords(rsIds,measurementMethds, cmoIds, conditions, 3 );
            if(records.size()>0){
                ranges.addAll(this.getSummaryRanges(records, cmoAccId, strainGroupId, null) );

           //     System.out.println("SUMMARY\n=================================");
           //     System.out.println("PHENOTYPE"+"\t"+ "RANGE_NAME"+"\t"+ "GROUP VALUE"

           //             +"\t"+ "GROUP SD"+"\t"+ "GROUP_LOW"+"\t"+ "GROUP_HIGH");
                for(PhenominerExpectedRange range:ranges) {
            //        System.out.println(range.getClinicalMeasurement() + "\t" + range.getExpectedRangeName() + "\t" + range.getGroupValue()
            //                + "\t" + range.getGroupSD() + "\t" + range.getGroupLow() + "\t" + range.getGroupHigh());
                }

            }

        }
        return ranges;
    }
    public List<PhenominerExpectedRange> getNormalStrainsRanges1() throws Exception {
        ExpectedRangeProcess process= new ExpectedRangeProcess();
        List<PhenominerExpectedRange> normalRanges= new ArrayList<>();
        for(Map.Entry e: NormalStrainGroup.phenotypeNormalStrainsMap.entrySet()){
       //     int newStrainGroupId=getNextKey("PHENOMINER_STRAIN_GROUP_SEQ");
            Map<String, List<String>> normalStrainGroupMap= new HashMap<>();
            String cmoAccId= (String) e.getKey();
            List<PhenominerExpectedRange> ranges= new ArrayList<>();
            List<String> strainGroups= (List<String>) e.getValue();
            String phenotype=getTerm(cmoAccId).getTerm();
        //    System.out.println("STRAIN GROUPS: "+ phenotype+"\n======================");
            String strainGroupName="NormalStrain_"+phenotype;
            List<String> strainOntIds=new ArrayList<>();

            for(String strainGroup:strainGroups) {
               int id= strainGroupDao.getStrainGroupId(strainGroup);
               strainOntIds.addAll(strainGroupDao.getStrainsOfStrainGroup(id));
               ranges.addAll(this.getExpectedRangeOfMixedAndAll(id, phenotype));
            }
            normalStrainGroupMap.put(strainGroupName, strainOntIds);
     //       process.insertOrUpdateNormalStrainGroup(normalStrainGroupMap, newStrainGroupId);
    //        normalRanges.addAll(this.getSortedRangesBySex(ranges,cmoAccId, newStrainGroupId));
           int strainGroupId= process.insertOrUpdateNormalStrainGroup(normalStrainGroupMap);
            normalRanges.addAll(this.getSortedRangesBySex(ranges,cmoAccId, strainGroupId));

        }
     /*   System.out.println("RANGE NAME"+"\t"+ "ClinicalMeasurement"+"\t"+ "ClinicalMeasurementOntId"
                +"\t"+ "RangeValue"+"\t"+"RangeSD"+"\t"+ "RangeLow"+"\t"+ "RangeHigh");
        for(PhenominerExpectedRange r: normalRanges){
            System.out.println(r.getExpectedRangeName()+"\t"+ r.getClinicalMeasurement()+"\t"+ r.getClinicalMeasurementOntId()
            +"\t"+ r.getRangeValue()+"\t"+ r.getRangeSD()+"\t"+ r.getRangeLow()+"\t"+ r.getRangeHigh());
        }
        System.out.println("==============================================");*/

        return normalRanges;
    }

    public List<PhenominerExpectedRange> getNormalStrainsRanges1(List<String> xcoTerms, List<String> mmoTerms, PhenotypeTrait phenotypeTrait) throws Exception {
        ExpectedRangeProcess process= new ExpectedRangeProcess();
        List<PhenominerExpectedRange> normalRanges= new ArrayList<>();

        for(Map.Entry e: NormalStrainGroup.phenotypeNormalStrainsMap.entrySet()){

            Map<String, List<String>> normalStrainGroupMap= new HashMap<>();
            String cmoAccId= (String) e.getKey();
            List<String> cmoIds= new ArrayList<>(Arrays.asList(cmoAccId));
            List<PhenominerExpectedRange> ranges= new ArrayList<>();
            List<String> strainGroups= (List<String>) e.getValue();
            String phenotype=getTerm(cmoAccId).getTerm();
            //    System.out.println("STRAIN GROUPS: "+ phenotype+"\n======================");
            String strainGroupName="NormalStrain_"+phenotype;
            List<String> strainOntIds=new ArrayList<>();
            System.out.println("PHENOTYPE: " + phenotype + "\tACC_ID: "+ cmoAccId);
            for(String strainGroup:strainGroups) {
                int id= strainGroupDao.getStrainGroupId(strainGroup);
                List<String> ontIds=strainGroupDao.getStrainsOfStrainGroup(id);
                strainOntIds.addAll(ontIds);
                System.out.println("STRAIN GROUP: "+ strainGroup);
                for(String ontId:ontIds){
                    System.out.println(ontId);
                }
            }


            List<Record>  records = pdao.getFullRecords(strainOntIds, mmoTerms, cmoIds, xcoTerms, 3);
         /*   System.out.println("RECORDS SIZE: "+ records.size());
            System.out.println("REc Id()"+"\t"+ "StrainAccId"+"\t"+ "NumberOfAnimals"+"\t"+
                    "ClinicalMeasurementAccId"+"\t"+"MeasurementValue"+"\tMeasurementSD"+"\t"+ "Sex");
            for(Record r: records){
                System.out.println(r.getId()+"\t"+ r.getSample().getStrainAccId()+"\t"+ r.getSample().getNumberOfAnimals()+"\t"+
                r.getClinicalMeasurement().getAccId()+"\t"+r.getMeasurementValue()+"\t"+r.getMeasurementSD()+"\t"+ r.getSample().getSex());
            }*/
            normalStrainGroupMap.put(strainGroupName, strainOntIds);

            int strainGroupId= process.insertOrUpdateNormalStrainGroup(normalStrainGroupMap);

            String traitOntId= this.getTraitOntId(records, cmoAccId, phenotypeTrait.getPhenotypeTraitMap());

            List<TraitObject> traitAncestors= getTraitAncester(traitOntId);
            Set<String> rangeUnits= this.getRangeUnits(records);
            Map<String,List<Record>> categories= categorizeRecords(records);

         //   PhenominerExpectedRange range= getRange(records, cmoAccId, strainGroupId, "Mixed",null, 0, 999,traitOntId, rangeUnits, traitAncestors);
            for(Map.Entry entry: categories.entrySet()) {
                String cat = (String) entry.getKey();
                String sex = "Mixed";
                int ageLow = 0;
                int ageHigh = 999;
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
          /*          System.out.println("RECORDS SIZE OF CATEGORY "+ cat + " : "+ recs.size());

                System.out.println("Cat: "+ cat+"\tSTRAINGROUP ID:"+strainGroupId+"\tcmoID: "+cmoAccId+ "\tRECORDS SIZE: "+ recs.size());
                    System.out.println("StrainAccId"+"\t"+ "ClinicalMeasurementAccId"+"\t"+"getMeasurementValue"+"\t"+ "MeasurementSD");
                    for (Record r: recs){
                        System.out.println(r.getSample().getStrainAccId()+"\t"+ r.getClinicalMeasurement().getAccId()+"\t"+r.getMeasurementValue()+"\t"+ r.getMeasurementSD());
                    }
                    System.out.println("=========================================================");*/
                    if(recs.size()>0)
                ranges.add(this.getRange(recs, cmoAccId, strainGroupId, sex, null, ageLow, ageHigh, traitOntId, rangeUnits, traitAncestors));
            }
            }
            normalRanges.addAll(ranges);

        }
   /*         System.out.println("RANGE NAME"+"\t"+ "ClinicalMeasurement"+"\t"+ "ClinicalMeasurementOntId"
                +"\t"+ "RangeValue"+"\t"+"RangeSD"+"\t"+ "RangeLow"+"\t"+ "RangeHigh"+"\tSex");
        for(PhenominerExpectedRange r: normalRanges){
            System.out.println(r.getExpectedRangeName()+"\t"+ r.getClinicalMeasurement()+"\t"+ r.getClinicalMeasurementOntId()
            +"\t"+ r.getRangeValue()+"\t"+ r.getRangeSD()+"\t"+ r.getRangeLow()+"\t"+ r.getRangeHigh()+"\t"+ r.getSex());
        }
        System.out.println("==============================================");*/
         return normalRanges;
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
        for(PhenominerExpectedRange range:ranges){
            int expectedRangeId=getExpectedRangeId(range);
            if(expectedRangeId==0) {
                expectedRangeId = dao.getNextKey("PHENOMINER_EXPECTED_RANGE_SEQ");
            range.setExpectedRangeId(expectedRangeId);
            status+=dao.insert(range);
            }else{
                 dao.updateExpectedRange(range, expectedRangeId);
            }
            for(Record r: range.getExperimentRecords()){
               updateExpectedRangeExperiment(expectedRangeId,r.getId())  ;
            }
           if(range.getTraitAncestors().size()>0) {
            for(TraitObject o:range.getTraitAncestors()){
                    if(o.getTrait()!=null && o.getSubTrait()!=null)
                insertOrUpdateRangeTraits(o, expectedRangeId);
            }
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
