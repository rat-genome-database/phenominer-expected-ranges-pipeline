package edu.mcw.rgd.phenominerExpectedRanges.dao.phenominerTrait;

import edu.mcw.rgd.dao.impl.OntologyXDAO;
import edu.mcw.rgd.dao.impl.PhenominerDAO;
import edu.mcw.rgd.datamodel.pheno.Experiment;
import edu.mcw.rgd.datamodel.pheno.Record;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by jthota on 10/9/2018.
 */
public class TraitOntology {


    public static void main(String[] args) throws Exception {

        TraitOntology traitOntology=new TraitOntology();

        String file1="data/CMO_terms_2_VT_IDs.txt";
        String file2="data/phenotypes_okay_trait.txt";
        int updatedRecCount=0;

        System.out.println("Reading "+ file1 +"...");

        updatedRecCount=  traitOntology.updateExperimentTraitOntIds(traitOntology.readFile(file1, false));
        System.out.println("Updated experiment trait_ont_ids from "+ file1 +" is done!!!");
        System.out.println("Reading "+ file2 +"...");

        updatedRecCount=updatedRecCount+traitOntology.updateExperimentTraitOntIds(traitOntology.readFile(file2, true));
        System.out.println("Updating trait_ont_ids from "+ file2 +" is done!!!");
        System.out.println("Total Experiments Updated=" +updatedRecCount);


   }
    public  Map<String, String> readFile(String file, boolean termNames) throws Exception {

        Map<String, String> cmoVtMap=new HashMap<>();
        try{
            BufferedReader reader= new BufferedReader(new FileReader(file));
            //     BufferedReader reader= new BufferedReader(new FileReader("data/testFile"));

            String line=null;
            String[] wordsArray;

            while(true){
                line=reader.readLine();
                if(line == null){
                    break;
                }else{
                    wordsArray=line.split("\t");
                    if(wordsArray[0]!=null){
                        try {
                            if (termNames) {
                                cmoVtMap.putAll(getPhenotypeTraitMap(wordsArray[0], wordsArray[1], true));

                            } else {
                                cmoVtMap.putAll(getPhenotypeTraitMap(wordsArray[0], wordsArray[1], false));
                            }
                        }catch (Exception e){
                            System.err.println(line);
                            e.printStackTrace();
                        }
                    }
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return cmoVtMap;
    }
    public Map<String, String> getPhenotypeTraitMap(String phenotype, String trait, boolean termNames){
        OntologyXDaoExt xdao= new OntologyXDaoExt();
        Map<String, String> cmoVtMap=new HashMap<>();

        if(termNames){
            try{
                String cmoId=   xdao.getTermAccByTerm(phenotype.trim());
                String traitId=xdao.getTermAccByTerm(trait.trim());

                if(cmoId!=null && traitId!=null)
                 cmoVtMap.put(cmoId, traitId);
                else
                    System.err.println("PHENOTYPE: " + phenotype+"\tTRAIT: "+ trait);

            }catch (Exception e){
                System.err.println("Phenotype Term: "+phenotype);
                e.printStackTrace();
            }

        }else{
            try{
                String cmoId=   xdao.getTermAccByTerm(phenotype);
                cmoVtMap.put(cmoId, trait);
                // System.out.println(wordsArray[0]+"\t"+ cmoId+"\t"+ wordsArray[1]);
            }catch (Exception e){
                System.err.println("hello"+phenotype);
                e.printStackTrace();
            }
        }
        return cmoVtMap;
    }
    public int updateExperimentTraitOntIds(Map<String, String> cmoVtMap) throws Exception {
        PhenominerDAO pdao=new PhenominerDAO();
        OntologyXDAO xdao=new OntologyXDAO();
        int updatedRecCount=0;
        List<String> sampleIds= new ArrayList<>();
        List<String> methodIds=new ArrayList<>();
        List<String> experimentalCond=new ArrayList<>();
        for(Map.Entry e: cmoVtMap.entrySet()) {
            try {
                  // if(e.getValue().toString().equals("VT:0010011")) {
                  //      System.out.println("KEY AND VALUE:"+e.getKey() + "\t" + e.getValue());
                        List<Record> records = pdao.getFullRecords(sampleIds, methodIds, Arrays.asList(e.getKey().toString()), experimentalCond, 3);
                        Set<Integer> expIds = new HashSet<>();
                        for (Record r : records) {
                            expIds.add(r.getExperimentId());


                       //     System.out.println(r.getId() + "\t" + e.getKey() + "\t" + r.getClinicalMeasurement().getAccId() + "\t" + xdao.getTerm(e.getKey().toString()).getTerm() + "\t" + r.getExperimentId() + "\t" + pdao.getExperiment(r.getExperimentId()).getTraitOntId() + "\t" + e.getValue());
                        }
                       // System.out.println(e.getKey().toString() + ":\t" + expIds.size());
                        updatedRecCount=updatedRecCount+ updateExperiments(expIds, e.getValue().toString());
                  //  }

            }catch(Exception e1){
                    System.out.println("CMO ID:" + e.getKey() +"\t TRAIT ID: "+ e.getValue());
                    e1.printStackTrace();
                }

        }
        System.out.println("Done updating experiment!!!!");
        return updatedRecCount;
    }
    public int updateExperiments(Set<Integer> expIds, String traitOntId ) throws Exception {
        OntologyXDAO xdao=new OntologyXDAO();
        OntologyXDaoExt dao= new OntologyXDaoExt();
        String traitTerm= null;
        int count=0;
        try {
            traitTerm = xdao.getTerm(traitOntId).getTerm();
            for(int i:expIds){
              count=count+ dao.updateExperiment(i,traitTerm,traitOntId);
             }
        } catch (Exception e) {
            e.printStackTrace();

        }

        return count;
    }
}
