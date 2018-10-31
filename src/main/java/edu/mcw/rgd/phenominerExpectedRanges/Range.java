package edu.mcw.rgd.phenominerExpectedRanges;


import edu.mcw.rgd.dao.impl.PhenominerDAO;
import edu.mcw.rgd.dao.impl.PhenominerExpectedRangeDao;
import edu.mcw.rgd.dao.impl.PhenominerStrainGroupDao;

import edu.mcw.rgd.datamodel.pheno.Record;
import edu.mcw.rgd.datamodel.phenominerExpectedRange.PhenominerExpectedRange;

import edu.mcw.rgd.phenominerExpectedRanges.dao.PhenotypeExpectedRangeDao;
import edu.mcw.rgd.phenominerExpectedRanges.model.PhenotypeTrait;

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
         //   log.info(Thread.currentThread().getName() + ": " + this.phenotypeAccId + " started " + new Date());
            try {
                records = pdao.getFullRecords(rsIds, methods, cmoIds, conditions, 3);
                System.out.println();
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
                                List<PhenominerExpectedRange> expectedRanges = this.getSummaryRanges(recordsByStrainGroup, phenotypeAccId, strainGroupId, phenotypeTraitMap);

                                ranges.addAll(expectedRanges);
                         //  }
                        }

                    }
                }
              //  System.out.println("RANGES SIZE: " + ranges.size());

                if(ranges.size()>0){
                 /*  System.out.println("============================================================");
                    for(PhenominerExpectedRange r:ranges){
                        System.out.println(r.getClinicalMeasurementOntId()+"\t"+ r.getTraitOntId()+"\t"+r.getStrainGroupName()+"\t"+r.getSex()+"\t"+r.getRangeValue()
                        +"\t"+r.getRangeSD()+"\t"+r.getRangeLow()+"\t"+ r.getRangeHigh());
                    }*/
                      insert(ranges);
                    System.out.println("Initiated normal strain insertion...." + getTerm(phenotypeAccId).getTerm());
                    insertNormalRanges(conditions, methods, phenotypeTraitMap,phenotypeAccId);
                }
            } catch (Exception e) {
                System.err.print(phenotypeAccId);
                e.printStackTrace();
            }
        }
    public static void main(String[] args) throws Exception {
        PhenotypeExpectedRangeDao dao= new PhenotypeExpectedRangeDao();

        List<String> conditions = new ArrayList<>(Arrays.asList("XCO:0000099")); //control condition
        List<String> mmoTerms = dao.getMeasurementMethods();
        PhenotypeTrait phenotypeTrait = PhenotypeTrait.getInstance();

        for (String condition : conditions) {
            List<String> xcoTerms = dao.getConditons(condition);
            //   List<String> phenotypes= process.getAllPhenotypesWithExpRecordsByConditions(xcoTerms);
            List<String> phenotypes = new ArrayList<>(Arrays.asList("CMO:0000002"));
            System.out.println("Phenotypes Size:" + phenotypes.size());
         //   ExecutorService executor = Executors.newFixedThreadPool(10);
            for (String cmo : phenotypes) {
                Range r= new Range();
                r.phenotypeAccId=cmo;
                r.methods= mmoTerms;
                r.conditions=xcoTerms;
               r.phenotypeTraitMap=phenotypeTrait.getPhenotypeTraitMap();
                r.run();
          //      Runnable workerThread = new Range(cmo, xcoTerms, mmoTerms, phenotypeTrait.getPhenotypeTraitMap());
        //        executor.execute(workerThread);
            }
        //    executor.shutdown();
        //    while (!executor.isTerminated()) {
        //    }
            System.out.println("Finished All Threads" + new Date());

        }
    }
}
