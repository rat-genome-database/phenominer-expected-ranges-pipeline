package edu.mcw.rgd.phenominerExpectedRanges.process;


import edu.mcw.rgd.dao.impl.OntologyXDAO;
import edu.mcw.rgd.dao.impl.PhenominerStrainGroupDao;

import edu.mcw.rgd.datamodel.ontologyx.Term;

import edu.mcw.rgd.datamodel.ontologyx.TermWithStats;
import edu.mcw.rgd.datamodel.phenominerExpectedRange.PhenominerStrainGroup;
import edu.mcw.rgd.phenominerExpectedRanges.dao.PhenotypeExpectedRangeDao;
import edu.mcw.rgd.phenominerExpectedRanges.dao.newDAO.ExperiementRecordDAO;
import edu.mcw.rgd.process.Utils;


import java.util.*;

import static edu.mcw.rgd.phenominerExpectedRanges.Manager.log;

/**
 * Created by jthota on 4/3/2018.
 */
public class ExpectedRangeProcess extends OntologyXDAO {
    PhenominerStrainGroupDao pdao= new PhenominerStrainGroupDao();
    ExperiementRecordDAO edao= new ExperiementRecordDAO();
    PhenotypeExpectedRangeDao dao= new PhenotypeExpectedRangeDao();

    public int insertOrUpdateStrainGroup() throws Exception {
        int count=0;
        Map<String, List<Term>> strainGroupMap= dao.getInbredStrainGroupMap1("RS:0000765");
        for(Map.Entry e: strainGroupMap.entrySet()){
            String key= (String) e.getKey();
            List<Term> strains= (List<Term>) e.getValue();
            int id=getNextKey("PHENOMINER_STRAIN_GROUP_SEQ");

            for(Term t:strains){
                PhenominerStrainGroup strainGroup= new PhenominerStrainGroup();
                strainGroup.setId(id);
                strainGroup.setName(getTerm(key).getTerm());
                strainGroup.setStrain_ont_id(t.getAccId());
                if(!existsStrainGroup(strainGroup))
                pdao.insertOrUpdate(strainGroup);
            }
            count++;
        }
        return count;
    }
    public int insertOrUpdateNormalStrainGroup(Map<String, List<String>> strainGroupMap) throws Exception {
        int id=0;
        for(Map.Entry e: strainGroupMap.entrySet()){
            String key= (String) e.getKey();
            List strains= (List) e.getValue();

//System.out.println("Strain Group Name: "+ key+"\tSTRAIN ONT IDS SIZE: "+ strains.size());
            for(Object t:strains){
                PhenominerStrainGroup strainGroup= new PhenominerStrainGroup();
             //   strainGroup.setId(id);
                strainGroup.setName(key);
                strainGroup.setStrain_ont_id(t.toString());
                if(!existsStrainGroup(strainGroup)) { // if rs_id of strain group exists
                    id= newStrainGroup(strainGroup); // getting id of strain group if exists
                    if(id==0){
                        id=getNextKey("PHENOMINER_STRAIN_GROUP_SEQ");
                    }
                    strainGroup.setId(id);
               //     System.out.println("Normal Strain GROUP: "+ strainGroup.getName());
                    pdao.insertOrUpdate(strainGroup);
                }else{
                   id= newStrainGroup(strainGroup);
            }
        }
    }
        System.out.println("STRAIN GROUP ID: "+ id);
        return id;
    }
    public int newStrainGroup(PhenominerStrainGroup strainGroup) throws Exception {
        String strainGroupNmae= strainGroup.getName();
        int id= pdao.getStrainGroupId(strainGroupNmae);
        return id;
    }
    public  boolean existsStrainGroup(PhenominerStrainGroup strainGroup) throws Exception {
        String strainGroupName = strainGroup.getName();
        String rsId = strainGroup.getStrain_ont_id();
        int id = pdao.getStrainGroupId(strainGroupName, rsId);
        return id != 0;
    }


    public List<String> getAllPhenotypesWithExpRecordsByConditions(List<String> conditions) throws Exception {
        return edao.getAllPhenotypesWithExperimentRecords(conditions);
    }
    public List<String> getExperimentalConditions(String condition) throws Exception {
        return dao.getConditons(condition); //control condition

    }
    public Map<String, String> getAllPhenotypeTraitMap() throws Exception {
        ExperiementRecordDAO dao=new ExperiementRecordDAO();
        Map<String, String> phenotypeTraitMap = new HashMap<>();
        List<String> phenotypes = dao.getAllPhenotypesWithExperimentRecords(null);
        phenotypes.sort((o1,o2)-> Utils.stringsCompareToIgnoreCase(o1,o2));

        int rdoCount=0;
        System.out.println("TOTAL PHENOTYPES WITH EXPERIMENT RECORDS: "+ phenotypes.size());
        for(String p:phenotypes){
            List<String> traits= dao.getTraitByPhenotype(p);
            if(traits.size()==1){
                if(traits.get(0).contains("RDO")){
                    rdoCount++;
                }else {
                    phenotypeTraitMap.put(p, traits.get(0));
                }
            }else{
                if(traits.size()>1)
            log.info(p+"\ttraits size: "+traits.size() );
            }
        }
        log.info("RDO as trait:" + rdoCount);

        return phenotypeTraitMap;
    }
    public List<TermWithStats> getFirstLevelTraitTerms() throws Exception {
       String rootTerm= getRootTerm("VT");
       return getActiveChildTerms(rootTerm,3);
}
}