package edu.mcw.rgd.phenominerExpectedRanges;


import edu.mcw.rgd.dao.impl.PhenominerDAO;
import edu.mcw.rgd.dao.impl.PhenominerStrainGroupDao;
import edu.mcw.rgd.datamodel.ontologyx.Term;
import edu.mcw.rgd.datamodel.pheno.Condition;
import edu.mcw.rgd.datamodel.pheno.Record;

import edu.mcw.rgd.datamodel.phenominerExpectedRange.PhenominerExpectedRange;
import edu.mcw.rgd.phenominerExpectedRanges.dao.PhenotypeExpectedRangeDao;

import edu.mcw.rgd.phenominerExpectedRanges.model.PhenotypeTrait;
import edu.mcw.rgd.phenominerExpectedRanges.process.ExpectedRangeProcess;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
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
           manager.run();
       }catch (Exception e){
           e.printStackTrace();
           log.info(e.getMessage());
       }
    }
    public void run() throws Exception {

        long startTime = System.currentTimeMillis();
        System.out.println("START TIME: "+ startTime);
       int status= process.insertOrUpdateStrainGroup(); // inserts strain groups
        log.info("Total Strain Groups inserted: "+ status);

        List<String> conditions= new ArrayList<>(Arrays.asList("XCO:0000099")); //control condition
        List<String> mmoTerms=dao.getMeasurementMethods();

        PhenotypeTrait phenotypeTrait= PhenotypeTrait.getInstance();

        for(String condition:conditions){
            List<String> xcoTerms=dao.getConditons(condition);
            List<String> phenotypes= process.getAllPhenotypesWithExpRecordsByConditions(xcoTerms);
        //   List<String> phenotypes=new ArrayList<>(Arrays.asList("CMO:0000581"));
            System.out.println("Phenotypes Size:" + phenotypes.size());
            ExecutorService executor = Executors.newFixedThreadPool(10);
        for(String cmo:phenotypes){
             Runnable workerThread=new Range(cmo, xcoTerms, mmoTerms,phenotypeTrait.getPhenotypeTraitMap() );
             executor.execute(workerThread);
         }
            executor.shutdown();
            while(!executor.isTerminated()){}
            System.out.println("Finished All Threads"+ new Date());
           /***************************************************PRINT RESUTLTS MATRIX*****************************************/
        //      this.printResultsMatrix(phenotypes, ranges);
        /*******************************************************************************************************************************/
            long endTime=System.currentTimeMillis();
            System.out.println("END Time: " + endTime);
            long totalTime=(endTime-startTime)/1000;
            System.out.println("OVERALL TIME:"+ totalTime);
        }
    }
 /*   public void run() throws Exception {
        long startTime = System.currentTimeMillis();
        System.out.println("START TIME: "+ System.currentTimeMillis());
        List<PhenominerExpectedRange> ranges= new ArrayList<>();
        PhenominerExpectedRangeDao expectedRangeDao= new PhenominerExpectedRangeDao();
        int inserted=0;
        int insertedNormal=0;

        Map<String, List<Term>> strainGroupMap= dao.getInbredStrainGroupMap1("RS:0000765");
         int status= process.insertOrUpdate(strainGroupMap); // inserts strain groups

           List<String> cmoTerms= this.getClinicalMeaurements();
           List<String> xcoTerms=dao.getConditons("XCO:0000099"); //control condition
           List<String> mmoTerms=dao.getMeasurementMethods();
        List<Integer> strainGroupIds= strainGroupDao.getAllDistinctStrainGroupIds();
        for(String cmo:cmoTerms) {
            List<PhenominerExpectedRange> rangeList = dao.getRanges(cmo, strainGroupIds, xcoTerms, mmoTerms);
            ranges.addAll(rangeList);
         }
        if(ranges.size()>0){
           inserted= dao.insert(ranges);
        }

        List<PhenominerExpectedRange> normalRanges= new ArrayList<>();
        normalRanges.addAll(dao.getNormalStrainsRanges1()); // inserts normal strain groups and gets expected ranges for normal strains
       insertedNormal=dao.insert(normalRanges);

        System.out.println("END TIME: "+ (System.currentTimeMillis() - startTime));
        System.out.println("DONE");
     }*/

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
