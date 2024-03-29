package edu.mcw.rgd.phenominerExpectedRanges;

import edu.mcw.rgd.phenominerExpectedRanges.dao.PhenotypeExpectedRangeDao;
import edu.mcw.rgd.phenominerExpectedRanges.model.PhenotypeTrait;
import edu.mcw.rgd.phenominerExpectedRanges.process.ExpectedRangeProcess;

import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by jthota on 2/28/2018.
 */
public class Manager {
    private String version;

    PhenotypeExpectedRangeDao dao= new PhenotypeExpectedRangeDao();
    ExpectedRangeProcess process= new ExpectedRangeProcess();

    public static Logger log = LogManager.getLogger("main");

    public static void main(String[] args){
        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfiguration.xml"));
        Manager manager = (Manager) (bf.getBean("manager"));
        log.info(manager.getVersion());

        try {
            manager.run();
        }catch (Exception e){
            Utils.printStackTrace(e, manager.log);
        }
    }

    public void run() throws Exception {

        long startTime = System.currentTimeMillis();

        System.out.println("START TIME: "+ new Date(startTime));

        Map<String, List<String>> strainGroupMap= dao.getInbredStrainGroupMap2("RS:0000765");
        int status= process.insertOrUpdateStrainGroup(strainGroupMap, false); // inserts strain groups
        log.info("Total Strain Groups inserted: "+ status);

        List<String> conditions= new ArrayList<>(Arrays.asList("XCO:0000099")); //control condition
        List<String> mmoTerms=dao.getMeasurementMethods();
        PhenotypeTrait phenotypeTrait= PhenotypeTrait.getInstance();

        insertRanges(conditions, mmoTerms, phenotypeTrait);

        // dao.printResultsMatrix(phenotypes, ranges);

        long endTime=System.currentTimeMillis();
        System.out.println("==== OK ====   time elapsed: "+ Utils.formatElapsedTime(startTime, endTime));
    }

    public void insertRanges(List<String> conditions, List<String> mmoTerms, PhenotypeTrait phenotypeTrait) throws Exception {

        for (String condition : conditions) {
            List<String> xcoTerms = dao.getConditons(condition);
    List<String> phenotypes= process.getAllPhenotypesWithExpRecordsByConditions(xcoTerms);
   //        List<String> phenotypes = new ArrayList<>(Arrays.asList("CMO:0000046"));
  /*   List<String> phenotypes = new ArrayList<>(Arrays.asList(
         "CMO:0000002"  ,"CMO:0000004","CMO:0000005","CMO:0000009",
            "CMO:0000069","CMO:0000071", "CMO:0000072","CMO:0000074" ,"CMO:0000075","CMO:0000108","CMO:0000530"));*/
            System.out.println("Phenotypes Size:" + phenotypes.size());
            ExecutorService executor = Executors.newFixedThreadPool(10);
        //   System.out.println("CMO term\tStrain Group\tsigma\tNo.of Experiments\tQ\tI2\tCP\tmeta\tmeta_low\tmeta_high\tEFFECT");
            for (String cmo : phenotypes) {

                Runnable workerThread = new Range(cmo, xcoTerms, mmoTerms, phenotypeTrait.getPhenotypeTraitMap());

                executor.execute(workerThread);
            }
            executor.shutdown();
            while (!executor.isTerminated()) {}
            System.out.println("Finished All Threads" + new Date());
            System.out.println("Completed inserting normal strains....");
        }
    }


    public String getVersion() {
        return version;
    }
    public void setVersion(String version) {
        this.version = version;
    }
}
