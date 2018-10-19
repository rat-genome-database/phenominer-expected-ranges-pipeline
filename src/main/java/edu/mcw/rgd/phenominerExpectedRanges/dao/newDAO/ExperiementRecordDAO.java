package edu.mcw.rgd.phenominerExpectedRanges.dao.newDAO;

import edu.mcw.rgd.dao.impl.OntologyXDAO;
import edu.mcw.rgd.dao.impl.PhenominerDAO;
import edu.mcw.rgd.dao.spring.StringListQuery;
import edu.mcw.rgd.datamodel.ontologyx.Term;

import java.util.List;

/**
 * Created by jthota on 4/19/2018.
 */
public class ExperiementRecordDAO extends OntologyXDAO{
    public List<Term> getTraitsWithExperimentRecords() throws Exception {
        String sql="SELECT * FROM ONT_TERMS WHERE TERM_ACC IN (\n" +
                " SELECT DISTINCT(E.TRAIT_ONT_ID) FROM \n" +
                " EXPERIMENT_RECORD ER, CLINICAL_MEASUREMENT CM, CONDITION_GROUP CG, COND_GROUP_EXPERIMENT_COND CGEC, EXPERIMENT E\n" +
                " WHERE ER.CLINICAL_MEASUREMENT_ID=CM.CLINICAL_MEASUREMENT_ID\n" +
                " AND ER.CONDITION_GROUP_ID= CG.CONDITION_GROUP_ID\n" +
                " AND CG.CONDITION_GROUP_ID=CGEC.CONDITION_GROUP_ID\n" +
                " AND ER.EXPERIMENT_ID=E.EXPERIMENT_ID\n" +
                " AND ER.CURATION_STATUS=40\n" +
                ")";


       return this.executeTermQuery(sql);

    }
    public List<String> getPhenotypesWithExpRecordsByTrait(String traitOntId) throws Exception {
        String sql="SELECT DISTINCT(CM.CLINICAL_MEASUREMENT_ONT_ID) FROM \n" +
                "  EXPERIMENT_RECORD ER, CLINICAL_MEASUREMENT CM, CONDITION_GROUP CG, COND_GROUP_EXPERIMENT_COND CGEC, EXPERIMENT E\n" +
                " WHERE ER.CLINICAL_MEASUREMENT_ID=CM.CLINICAL_MEASUREMENT_ID\n" +
                " AND ER.CONDITION_GROUP_ID= CG.CONDITION_GROUP_ID\n" +
                " AND CG.CONDITION_GROUP_ID=CGEC.CONDITION_GROUP_ID\n" +
                " AND ER.EXPERIMENT_ID=E.EXPERIMENT_ID\n" +
                " AND ER.CURATION_STATUS=40\n" +
                " AND E.TRAIT_ONT_ID=?";
        StringListQuery query=new StringListQuery(this.getDataSource(),sql);
        return execute(query, new Object[]{traitOntId});
    }
    public List<String> getAllPhenotypesWithExperimentRecords() throws Exception{
    /*    String sql=
                " SELECT DISTINCT(CM.CLINICAL_MEASUREMENT_ONT_ID) FROM EXPERIMENT_RECORD ER, CLINICAL_MEASUREMENT CM, CONDITION_GROUP CG, COND_GROUP_EXPERIMENT_COND CGEC\n" +
                " WHERE ER.CLINICAL_MEASUREMENT_ID=CM.CLINICAL_MEASUREMENT_ID\n" +
                " AND ER.CONDITION_GROUP_ID= CG.CONDITION_GROUP_ID\n" +
                " AND CG.CONDITION_GROUP_ID=CGEC.CONDITION_GROUP_ID\n" +
                " AND ER.CURATION_STATUS=40\n" ;
*/
        String sql=
                " SELECT DISTINCT(CM.CLINICAL_MEASUREMENT_ONT_ID) FROM EXPERIMENT_RECORD ER, CLINICAL_MEASUREMENT CM, \n" +
                        " EXPERIMENT_CONDITION EC \n" +
                        " WHERE ER.CLINICAL_MEASUREMENT_ID=CM.CLINICAL_MEASUREMENT_ID\n" +
                        " AND ER.EXPERIMENT_RECORD_ID= EC.EXPERIMENT_RECORD_ID\n" +
                        " AND ER.CURATION_STATUS=40\n" ;
        StringListQuery query=new StringListQuery(this.getDataSource(), sql);
        return query.execute();
    }

    public List<String> getAllPhenotypesWithExperimentRecords(List<String> conditions) throws Exception{

        String sql=
                " SELECT DISTINCT(CM.CLINICAL_MEASUREMENT_ONT_ID) FROM EXPERIMENT_RECORD ER, CLINICAL_MEASUREMENT CM, \n" +
                        " EXPERIMENT_CONDITION EC \n" +
                        " WHERE ER.CLINICAL_MEASUREMENT_ID=CM.CLINICAL_MEASUREMENT_ID\n" +
                        " AND ER.EXPERIMENT_RECORD_ID= EC.EXPERIMENT_RECORD_ID\n" +
                        " AND ER.CURATION_STATUS=40\n" ;

        if(conditions!=null){
            if(conditions.size()>0){
               sql+= this.buildConditionsQuery(conditions);
            }
        }
        StringListQuery query=new StringListQuery(this.getDataSource(), sql);
        return query.execute();
    }
    public String buildConditionsQuery(List<String> conditions){
      /*  String sql= " AND CGEC.EXPERIMENT_CONDITION_ID = EC.EXPERIMENT_CONDITION_ID AND EC.EXP_COND_ONT_ID IN (";*/
        String sql= " AND EC.EXP_COND_ONT_ID IN (";
       boolean first=true;
        for(String cond: conditions){
            if(first){
                sql+="'"+cond +"'";
                first=false;
            }else{
                sql+=", '"+cond+"'";
            }

        }
        sql+=")";
        return sql;
    }
    public List<String> getTraitByPhenotype(String phenotype) throws Exception {
        String sql= "SELECT distinct(E.TRAIT_ONT_ID) FROM EXPERIMENT E, EXPERIMENT_RECORD ER, CLINICAL_MEASUREMENT CM" +
                " WHERE ER.EXPERIMENT_ID=E.EXPERIMENT_ID" +
                " AND ER.CLINICAL_MEASUREMENT_ID=CM.CLINICAL_MEASUREMENT_ID" +
                " AND CM.CLINICAL_MEASUREMENT_ONT_ID=?" +
                " AND E.TRAIT_ONT_ID IS NOT NULL";
        StringListQuery query= new StringListQuery(this.getDataSource(), sql);
        return execute(query, phenotype);
    }


}
