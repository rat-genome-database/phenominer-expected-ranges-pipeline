package edu.mcw.rgd.phenominerExpectedRanges;

import edu.mcw.rgd.dao.impl.PhenominerDAO;
import edu.mcw.rgd.dao.impl.PhenominerExpectedRangeDao;
import edu.mcw.rgd.dao.impl.PhenominerStrainGroupDao;
import edu.mcw.rgd.datamodel.ontologyx.Term;
import edu.mcw.rgd.datamodel.pheno.Record;
import edu.mcw.rgd.datamodel.phenominerExpectedRange.PhenominerExpectedRange;
import edu.mcw.rgd.datamodel.phenominerExpectedRange.TraitObject;
import edu.mcw.rgd.phenominerExpectedRanges.dao.PhenotypeExpectedRangeDao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by jthota on 6/26/2018.
 */
public class Range extends PhenotypeExpectedRangeDao implements Runnable {
    PhenominerDAO pdao= new PhenominerDAO();
    PhenominerStrainGroupDao strainGroupDao= new PhenominerStrainGroupDao();
    PhenominerExpectedRangeDao dao= new PhenominerExpectedRangeDao();
    private String phenotypeAccId;
    private List<String> methods;
    private List<String> conditions;
    private Map<String, String> phenotypeTraitMap;
    private Thread t;
    public Range(){}

    public Range(String phenotypeAccId, List<String> conditions, List<String> methods, Map<String, String> traitMap)  throws Exception {
        this.phenotypeAccId=phenotypeAccId;
        this.methods= methods;
        this.conditions=conditions;
        this.phenotypeTraitMap=traitMap;
    }

   @Override
    public void run() {
            List<PhenominerExpectedRange> ranges = new ArrayList<>();
            List<String> cmoIds = new ArrayList<>();
            cmoIds.add(phenotypeAccId);
            List<String> rsIds = new ArrayList<>();
            List<Record> records = null;
            System.out.println(Thread.currentThread().getName() + ": " + this.phenotypeAccId + " started " + new Date());
         //   log.info(Thread.currentThread().getName() + ": " + this.phenotypeAccId + " started " + new Date());
            try {
                records = pdao.getFullRecords(rsIds, methods, cmoIds, conditions, 3);
             //   String traitOntId=getTraitOntId(records, phenotypeAccId, phenotypeTraitMap);
            //    System.out.println(phenotypeAccId+"\t"+ traitOntId);

                List<String> strainOntIds = new ArrayList<>();
                for (Record r : records) {
                    String ontId = r.getSample().getStrainAccId();
                    if (!strainOntIds.contains(ontId))
                        strainOntIds.add(ontId);
                }

                List<String> strainGroupNames = new ArrayList<>();
                for (String id : strainOntIds) {
                    int strainGroupId = strainGroupDao.getStrainGroupIdByStrainOntId(id);
                    String strainName = strainGroupId != 0 ? strainGroupDao.getStrainGroupName(strainGroupId) : null;
                    if (!strainGroupNames.contains(strainName)) {
                        strainGroupNames.add(strainName);
                        List<Record> recordsByStrainGroup = new ArrayList<>();
                        for (Record r : records) {
                            int sgId = strainGroupDao.getStrainGroupIdByStrainOntId(r.getSample().getStrainAccId());
                            if (strainGroupId != 0 && sgId != 0) {
                                if (strainGroupId == sgId) {
                                    recordsByStrainGroup.add(r);
                                }
                            }
                        }

                        if (strainGroupId != 0 && recordsByStrainGroup.size() >= 4) {
                            List<PhenominerExpectedRange> expectedRanges = null;

                            expectedRanges = this.getSummaryRanges(recordsByStrainGroup, phenotypeAccId, strainGroupId, phenotypeTraitMap);

                            ranges.addAll(expectedRanges);
                        }

                    }
                }

                if(ranges.size()>0){
                       insert(ranges);
                }
            } catch (Exception e) {
                System.err.print(phenotypeAccId);
                e.printStackTrace();
            }
        }
}
