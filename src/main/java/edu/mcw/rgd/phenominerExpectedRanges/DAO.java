package edu.mcw.rgd.phenominerExpectedRanges;

import edu.mcw.rgd.dao.impl.OntologyXDAO;
import edu.mcw.rgd.dao.spring.StringListQuery;

import java.util.List;

/**
 * central place for database access code
 */
public class DAO {

    private final OntologyXDAO xdao = new OntologyXDAO();

    public String getTermAccByTerm(String term) throws Exception {
        String sql = "SELECT term_acc FROM ont_terms WHERE term=? AND is_obsolete=0";
        List<String> ontIds = StringListQuery.execute(xdao, sql, term);
        return ontIds.isEmpty() ? null : ontIds.get(0);
    }
}
