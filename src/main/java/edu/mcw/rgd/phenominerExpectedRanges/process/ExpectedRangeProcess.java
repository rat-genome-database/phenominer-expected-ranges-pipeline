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

    public int insertOrUpdateStrainGroup(Map<String, List<String>> strainGroupMap, boolean normal) throws Exception {
        int count=0;
        int id=0;
        String normalStrainGroupName=new String();
     //   Map<String, List<Term>> strainGroupMap= dao.getInbredStrainGroupMap1("RS:0000765");

        for(Map.Entry e: strainGroupMap.entrySet()){
            String key= (String) e.getKey();
            List<String> strains= (List<String>) e.getValue();
            String strainGroupName=new String();
            if(normal) {
                strainGroupName = key;
                normalStrainGroupName=key;
            }
            else
            strainGroupName=getTerm(key).getTerm();

            List<PhenominerStrainGroup> newStrainGroups= new ArrayList<>();
            for(String t:strains){
                PhenominerStrainGroup strainGroup= new PhenominerStrainGroup();
                strainGroup.setName(strainGroupName);
                strainGroup.setStrain_ont_id(t);

                if(existsStrainGroup(strainGroup)==0) {
                   newStrainGroups.add(strainGroup);
                }
            }
            if(newStrainGroups.size()>0)
            id= insertStrainGroup(newStrainGroups,strainGroupName );
            count++;
        }

        if(normal){
            if(id==0){
                id=pdao.getStrainGroupId(normalStrainGroupName);
            }
          return id;
         }else
        return count;
    }
    public int insertStrainGroup(List<PhenominerStrainGroup> strainGroups, String strainGroupName ) throws Exception {
        int id=0;
        id= pdao.getStrainGroupId(strainGroupName);
         if(id==0)
             id= getNextKey("PHENOMINER_STRAIN_GROUP_SEQ");
        int count=0;
        for(PhenominerStrainGroup s:strainGroups){
            s.setId(id);
            pdao.insertOrUpdate(s);
            count++;
        }

        return id;
    }
  public int insertOrUpdateNormalStrainGroup(Map<String, List<String>> strainGroupMap) throws Exception {
        int id=0;
        for(Map.Entry e: strainGroupMap.entrySet()){
            String key= (String) e.getKey();
            List strains= (List) e.getValue();

            for(Object t:strains){
                PhenominerStrainGroup strainGroup= new PhenominerStrainGroup();
             //   strainGroup.setId(id);
                strainGroup.setName(key);

                strainGroup.setStrain_ont_id(t.toString());
                System.out.println("NORMAL STRAIN GROUP EXISTS: "+existsStrainGroup(strainGroup));
                if(existsStrainGroup(strainGroup)==0) { // if rs_id of strain group exists
                    id= newStrainGroup(strainGroup); // getting id of strain group if exists
                    if(id==0){
                        id=getNextKey("PHENOMINER_STRAIN_GROUP_SEQ");
                    }
                    strainGroup.setId(id);

                    pdao.insertOrUpdate(strainGroup);
                }else{
                   id= newStrainGroup(strainGroup);
            }
        }
    }
       // System.out.println("STRAIN GROUP ID: "+ id);
        return id;
    }
    public int newStrainGroup(PhenominerStrainGroup strainGroup) throws Exception {
        String strainGroupNmae= strainGroup.getName();
        int id= pdao.getStrainGroupId(strainGroupNmae);
        return id;
    }

    public int existsStrainGroup(PhenominerStrainGroup strainGroup) throws Exception {
        String strainGroupName = strainGroup.getName();
        String rsId = strainGroup.getStrain_ont_id();
        return pdao.getStrainGroupId(strainGroupName, rsId);
       // return id;
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
        System.out.println("TOTAL PHENOTYPES WITH EXPERIMENT RECORDS OF ALL CONDITIONS: "+ phenotypes.size());
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
