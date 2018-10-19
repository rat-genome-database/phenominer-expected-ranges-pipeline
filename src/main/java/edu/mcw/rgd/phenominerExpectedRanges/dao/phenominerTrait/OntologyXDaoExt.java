package edu.mcw.rgd.phenominerExpectedRanges.dao.phenominerTrait;

import edu.mcw.rgd.dao.impl.OntologyXDAO;
import edu.mcw.rgd.dao.spring.StringListQuery;
import edu.mcw.rgd.dao.spring.ontologyx.OntologyQuery;

import java.util.List;

/**
 * Created by jthota on 10/10/2018.
 */
public class OntologyXDaoExt extends OntologyXDAO {
    public String getTermAccByTerm(String term) throws Exception {
        String sql="select term_acc from ont_terms where term=? and is_obsolete=0 ";
        StringListQuery query= new StringListQuery(this.getDataSource(), sql);
        List<String> ontIds=execute(query, term);
        return ontIds.size()>0?ontIds.get(0):null;

    }
    public int updateExperiment(int expId,String expName, String traitOntId) throws Exception {
        String sql="Update experiment set experiment_name=?, trait_ont_id=? where experiment_id=?";
        return update(sql, expName, traitOntId, expId);
    }
}
