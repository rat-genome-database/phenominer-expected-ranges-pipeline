package edu.mcw.rgd.phenominerExpectedRanges;



import edu.mcw.rgd.dao.impl.PhenominerStrainGroupDao;
import edu.mcw.rgd.datamodel.phenominerExpectedRange.PhenominerExpectedRange;

import edu.mcw.rgd.phenominerExpectedRanges.dao.PhenotypeExpectedRangeDao;
import edu.mcw.rgd.phenominerExpectedRanges.model.PhenotypeTrait;
import edu.mcw.rgd.phenominerExpectedRanges.process.ExpectedRangeProcess;

import org.apache.log4j.Logger;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.io.FileWriter;
import java.io.PrintWriter;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by jthota on 2/28/2018.
 */
public class Manager {
    private String version;

    PhenotypeExpectedRangeDao dao= new PhenotypeExpectedRangeDao();
    PhenominerStrainGroupDao strainGroupDao=new PhenominerStrainGroupDao();
    ExpectedRangeProcess process= new ExpectedRangeProcess();

    public static Logger log= Logger.getLogger("main");
    public static void main(String[] args){
        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfiguration.xml"));
        Manager manager = (Manager) (bf.getBean("manager"));
        System.out.println(manager.getVersion());
        log.info(manager.getVersion());
       try{
         /*  double testValue=Math.round(Double.parseDouble("0.038729999")*100.0)/100.0;
           System.out.println("TEST VALUES:============="+ testValue);*/
          manager.run();
       }catch (Exception e){
           e.printStackTrace();
           log.info(e.getMessage());
       }
    }
    public void run() throws Exception {

        long startTime = System.currentTimeMillis();

        System.out.println("START TIME: "+ startTime);

        Map<String, List<String>> strainGroupMap= dao.getInbredStrainGroupMap2("RS:0000765");
        int status= process.insertOrUpdateStrainGroup(strainGroupMap, false); // inserts strain groups
        log.info("Total Strain Groups inserted: "+ status);

        List<String> conditions= new ArrayList<>(Arrays.asList("XCO:0000099")); //control condition
        List<String> mmoTerms=dao.getMeasurementMethods();
        PhenotypeTrait phenotypeTrait= PhenotypeTrait.getInstance();
        insertRanges(conditions, mmoTerms, phenotypeTrait);
        /***************************************************PRINT RESUTLTS MATRIX*****************************************
            this.printResultsMatrix(phenotypes, ranges);
        /*******************************************************************************************************************************/
            long endTime=System.currentTimeMillis();
            System.out.println("END Time: " + endTime);
            long totalTime=(endTime-startTime)/1000;
            System.out.println("OVERALL TIME:"+ totalTime);

    }
    public void insertRanges(List<String> conditions, List<String> mmoTerms, PhenotypeTrait phenotypeTrait) throws Exception {

        for (String condition : conditions) {
            List<String> xcoTerms = dao.getConditons(condition);
            List<String> phenotypes= process.getAllPhenotypesWithExpRecordsByConditions(xcoTerms);
          // List<String> phenotypes = new ArrayList<>(Arrays.asList("CMO:0000009"));
            System.out.println("Phenotypes Size:" + phenotypes.size());
            ExecutorService executor = Executors.newFixedThreadPool(10);
            for (String cmo : phenotypes) {
                Runnable workerThread = new Range(cmo, xcoTerms, mmoTerms, phenotypeTrait.getPhenotypeTraitMap());
                executor.execute(workerThread);
            }
            executor.shutdown();
            while (!executor.isTerminated()) {}
            System.out.println("Finished All Threads" + new Date());
           // System.out.println("Initiated normal strain insertion....");
           //insertNormalRanges(xcoTerms, mmoTerms, phenotypeTrait);
            System.out.println("Completed inserting normal strains....");
        }
    }
    public int insertNormalRanges(List<String> xcoTerms, List<String> mmoTerms, PhenotypeTrait phenotypeTrait) throws Exception {

        List<PhenominerExpectedRange> normalRanges = new ArrayList<>();
        normalRanges.addAll(dao.getNormalStrainsRanges1(xcoTerms, mmoTerms, phenotypeTrait));
       /* System.out.println("ClinicalMeasurement" + "\t" + "ClinicalMeasurementOntId" + "\t" + "RangeValue" + "\t" + "RangeSD" + "\t" + "RangeLow" + "\t" + "RangeHigh" + "\t" + "Sex");
        for (PhenominerExpectedRange r : normalRanges) {
            System.out.println(r.getClinicalMeasurement() + "\t" + r.getClinicalMeasurementOntId() + "\t" + r.getRangeValue() + "\t" + r.getRangeSD() + "\t" + r.getRangeLow() + "\t" + r.getRangeHigh() + "\t" + r.getSex());
        }*/
        return dao.insert(normalRanges);
    }
    public void printResultsMatrix(List<String> phenotypes, List<PhenominerExpectedRange> ranges) throws Exception {
        List<String> phenotypeNames=new ArrayList<>();
        //  List<String> phenotypes1= new ArrayList<>(Arrays.asList("CMO:0000004"));
        for(String cmo:phenotypes){
            String term=dao.getTerm(cmo).getTerm();
            phenotypeNames.add(term+"_Mixed_0-999 days");
            phenotypeNames.add(term+"_Mixed_0-79 days");
            phenotypeNames.add(term+"_Mixed_80-99 days");
            phenotypeNames.add(term+"_Mixed_100-999 days");
            phenotypeNames.add(term+"_Male_0-999 days");
            phenotypeNames.add(term+"_Female_0-999 days");
            phenotypeNames.add(term+"_Mixed_0-999 days_vascular");
            phenotypeNames.add(term+"_Mixed_0-999 days_tail");
        }
        List<String> strainGroupNames= strainGroupDao.getAllDistinctStrainGroupNames();
        //    String[][] matrix= new String[ranges.size()+1][strainGroupNames.size()+1];
        String[][] matrix= new String[phenotypeNames.size()+1][strainGroupNames.size()+1];
        System.out.println("RANGES SIZE:"+ ranges.size());
        System.out.println("STRAIN GROUPS SIZE: "+ strainGroupNames.size());



        int i=0;
        matrix[0][0]="phenotype" ;
        int j=0;
        for (String strain : strainGroupNames) {
            matrix[0][j+1]=strain;
            j++;
        }
        for(String name:phenotypeNames){
            matrix[i+1][0]=name;
            i++;
        }

        for(int n=0; n<strainGroupNames.size();n++){
            int phenotypeCount=0;
            String strain=matrix[0][n+1];
            for(int m=0;m<phenotypeNames.size();m++){

                String phenotypeName=matrix[m+1][0];

                // System.out.println(matrix[0][n+1] +"\t"+ matrix[m+1][0]);
                for(PhenominerExpectedRange range:ranges){
                    if(range.getExpectedRangeName().equalsIgnoreCase(phenotypeName)&& strain.equalsIgnoreCase(range.getStrainGroupName())){
                        matrix[m+1][n+1]=range.getRangeLow()+"|"+range.getRangeHigh() + " ("+ range.getExperimentRecords().size()+")";
                       phenotypeCount++;
                    }

                }
             }
            matrix[0][n+1]=strain+"("+phenotypeCount+")";

        }
        try {
            FileWriter fos= new FileWriter("C:/Apps/expectedRanges.tab");
            PrintWriter dos= new PrintWriter(fos);
            for (int k = 0; k <= phenotypeNames.size(); k++) {
                int count=0;
                for (int m = 0; m <= strainGroupNames.size(); m++) {

                    if (matrix[k][m] == null) {
                       dos.print("-" + "\t");
                      // System.out.print("-" + "\t");
                    } else {
                        count++;
                        dos.print(matrix[k][m] + "\t");
                       // System.out.print(matrix[k][m] + "\t");
                    }
                }
                dos.print(count-1);
                dos.print("\n");
              // System.out.print("\n");
            }
            dos.close();
            fos.close();
        }catch (Exception e){

        }

    }

    public String getVersion() {
        return version;
    }
    public void setVersion(String version) {
        this.version = version;
    }
}
