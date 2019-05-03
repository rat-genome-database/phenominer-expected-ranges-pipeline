package edu.mcw.rgd.phenominerExpectedRanges;


import edu.mcw.rgd.dao.impl.PhenominerDAO;
import edu.mcw.rgd.dao.impl.PhenominerExpectedRangeDao;
import edu.mcw.rgd.dao.impl.PhenominerStrainGroupDao;

import edu.mcw.rgd.datamodel.pheno.Record;
import edu.mcw.rgd.datamodel.phenominerExpectedRange.PhenominerExpectedRange;

import edu.mcw.rgd.phenominerExpectedRanges.dao.PhenotypeExpectedRangeDao;
import edu.mcw.rgd.phenominerExpectedRanges.model.PhenotypeTrait;
import edu.mcw.rgd.phenominerExpectedRanges.process.ExpectedRangeProcess;

import java.util.*;


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
          //  log.info(Thread.currentThread().getName() + ": " + this.phenotypeAccId + " started " + new Date());
            try {
                records = pdao.getFullRecords(rsIds, methods, cmoIds, conditions, 3);

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
                            String ontId = r.getSample().getStrainAccId();
                            int sgId = strainGroupDao.getStrainGroupIdByStrainOntId(ontId);

                            if (strainGroupId != 0 && sgId != 0) {
                                if (strainGroupId == sgId) {
                                    recordsByStrainGroup.add(r);
                                }
                            }
                        }
                        if (strainGroupId != 0 && recordsByStrainGroup.size() >= 4) {
                          // if(strainName.equalsIgnoreCase("F344")) {
                         //  System.out.print(strainName+"\t");
                                List<PhenominerExpectedRange> expectedRanges = this.getSummaryRanges(recordsByStrainGroup, phenotypeAccId, strainGroupId, phenotypeTraitMap);
                            if(expectedRanges!=null && expectedRanges.size()>0) {
                                for (PhenominerExpectedRange r : expectedRanges) {
                                    if(r!=null) {
                                        if (r.getRangeValue() > 0 && r.getRangeHigh() > 0 && r.getRangeLow() > 0)
                                            ranges.addAll(expectedRanges);
                                    }
                                }
                            }
                         //  }
                        }

                    }
                }
                if(ranges.size()>0){
                       insert(ranges);
                       insertNormalRanges(conditions, methods, phenotypeTraitMap,phenotypeAccId);

                }
            } catch (Exception e) {
                System.err.println(phenotypeAccId);
                e.printStackTrace();
            }
        }
    public static void main(String[] args) throws Exception {
        PhenotypeExpectedRangeDao dao= new PhenotypeExpectedRangeDao();
        ExpectedRangeProcess process= new ExpectedRangeProcess();
        Map<String, List<String>> strainGroupMap= dao.getInbredStrainGroupMap2("RS:0000765");
        int status= process.insertOrUpdateStrainGroup(strainGroupMap, false);
        List<String> conditions = new ArrayList<>(Arrays.asList("XCO:0000099")); //control condition
        List<String> mmoTerms = dao.getMeasurementMethods();
        PhenotypeTrait phenotypeTrait = PhenotypeTrait.getInstance();

        for (String condition : conditions) {
            List<String> xcoTerms = dao.getConditons(condition);
          //   List<String> phenotypes= process.getAllPhenotypesWithExpRecordsByConditions(xcoTerms);
      /*  List<String> phenotypes = new ArrayList<>(Arrays.asList("CMO:0000002","CMO:0000004","CMO:0000005","CMO:0000009",
             "CMO:0000069","CMO:0000071", "CMO:0000072","CMO:0000074" ,"CMO:0000075","CMO:0000108","CMO:0000530"));*/
           List<String> phenotypes = new ArrayList<>(Arrays.asList("CMO:0000075"));

            System.out.println("Phenotypes Size:" + phenotypes.size());
            for (String cmo : phenotypes) {
                Range r= new Range();
                r.phenotypeAccId=cmo;
                r.methods= mmoTerms;
                r.conditions=xcoTerms;
                r.phenotypeTraitMap=phenotypeTrait.getPhenotypeTraitMap();
                r.run();

            }
            System.out.println("Finished All Threads" + new Date());

        }
    }
}
