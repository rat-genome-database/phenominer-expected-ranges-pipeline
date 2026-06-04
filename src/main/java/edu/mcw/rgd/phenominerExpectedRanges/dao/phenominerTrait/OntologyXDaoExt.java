package edu.mcw.rgd.phenominerExpectedRanges.dao.phenominerTrait;

import edu.mcw.rgd.dao.impl.OntologyXDAO;
import edu.mcw.rgd.dao.spring.StringListQuery;
import edu.mcw.rgd.dao.spring.ontologyx.OntologyQuery;

import java.util.List;

/**
 * Created by jthota on 10/10/2018.
 */
public class OntologyXDaoExt extends OntologyXDAO {
    public int updateExperiment(int expId,String expName, String traitOntId) throws Exception {
        String sql="Update experiment set experiment_name=?, trait_ont_id=? where experiment_id=?";
        return update(sql, expName, traitOntId, expId);
    }
}
