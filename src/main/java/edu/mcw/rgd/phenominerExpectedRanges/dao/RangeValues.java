package edu.mcw.rgd.phenominerExpectedRanges.dao;

import edu.mcw.rgd.dao.impl.OntologyXDAO;
import edu.mcw.rgd.datamodel.pheno.Record;
import edu.mcw.rgd.datamodel.phenominerExpectedRange.PhenominerExpectedRange;
import edu.mcw.rgd.process.Utils;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by jthota on 3/22/2018.
 */
public class RangeValues extends OntologyXDAO {

    public PhenominerExpectedRange getRangeValues(List<Record> records) throws Exception {
        PhenominerExpectedRange phenominerExpectedRange= new PhenominerExpectedRange();

        List<Double> ciStart= new ArrayList<>();
        List<Double> ciEnd= new ArrayList<>();
        List<Double> values= new ArrayList<>();
        double w = 0;
        double w2 = 0;
        double wv = 0;
        double n = 0;
        double number = 0;
        DecimalFormat f = new DecimalFormat(".##");

        records.sort(new Comparator<Record>() {
            @Override
            public int compare(Record o1, Record o2) {
                try {
                    return Utils.stringsCompareToIgnoreCase(getTerm(o1.getSample().getStrainAccId()).getTerm(), getTerm(o2.getSample().getStrainAccId()).getTerm());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });
    //    System.out.println("Study_ID\tstrain\tSex\tno_of_animals\tage_low_bound\t age_high_bound\tSD\tValue\tci_start\tci_end");
        for(Record r:records) {

            double value=Double.parseDouble(r.getMeasurementValue());

            int noOfAnimals=r.getSample().getNumberOfAnimals();

         //   System.out.println("RAW VALUE:"+ r.getMeasurementValue()+"\tVALUE: "+ value+ "\tRounded Value: " + Math.round(value*100.0)/100.0);
            double pSD = Double.parseDouble(r.getMeasurementSD());
            double se = (float) (pSD / Math.sqrt(noOfAnimals));
            double ci = 1.960 * se;
            double ci_start = Math.round((value - ci) * 100.0) / 100.0;
            double ci_end = Math.round((value + ci) * 100.0) / 100.0;

            values.add(value);

            ciStart.add(ci_start);
            ciEnd.add(ci_end);
            double pw = 1 / (pSD * pSD);
            double pw2 = pw * pw;
            double pwv = value * pw;
            wv+=pwv;
            w += pw;
            w2 += pw2;
            n++;
            number += noOfAnimals;

        /*    System.out.println(r.getStudyId()+"\t"+ getTerm(r.getSample().getStrainAccId()).getTerm()+
                    "\t"+ r.getSample().getSex()+ "\t"+r.getSample().getNumberOfAnimals()+
                    "\t"+ r.getSample().getAgeDaysFromLowBound() + "\t"+ r.getSample().getAgeDaysFromHighBound()
                    +"\t"+ r.getMeasurementSD() +"\t"+ r.getMeasurementValue()
                    +"\t" + f.format(ci_start)+ "\t" + f.format(ci_end)
                   ) ;*/
        }
        double xbar= wv/w;
        double varXbar=1/w;
        double newSD=1.96*Math.sqrt(varXbar);
        double meta_low=xbar-1.96*Math.sqrt(varXbar);
        double meta_high=xbar+1.96*Math.sqrt(varXbar);

        double Q=0;
        for(Record r:records) {
            double pvalue=Double.parseDouble(r.getMeasurementValue());
            double pSD= Double.parseDouble(r.getMeasurementSD());
            double pw = 1 / (pSD * pSD);

            double q=pw * (pvalue-xbar) * (pvalue-xbar);
            Q+=q;
        }

        double q=Q;

        double i2 =Math.max(0, ((q-n+1)/q)) ;
     /*   System.out.println("xbar="+xbar);
        System.out.println("q="+q);
        System.out.println("i2="+ i2);*/
        if(i2>0.85){ // RANDOM EFFECT
            double wNew=0;
            double wvNew=0;
            double c=w - w2/w;
            double I2=(q-n+1)/c;
            for(Record r: records){
                double value=Double.parseDouble(r.getMeasurementValue());
                double pSD= Double.parseDouble(r.getMeasurementSD());
                double pw=1/((pSD*pSD)+I2);
                double pwv = value * pw;
                wvNew+=pwv;
                wNew+=pw;
            }
            wv=wvNew;
            w=wNew;
            xbar= wv/w;
            varXbar=1/w;
            newSD=1.96*Math.sqrt(varXbar);
            meta_low=xbar-1.96*Math.sqrt(varXbar);
            meta_high=xbar+1.96*Math.sqrt(varXbar);
        }


        double min = Collections.min(values);
        double max = Collections.max(values);
        double range = Math.round((max - min) * 100.0) / 100.0;

  //      System.out.print(f.format(q)+"\t"+f.format(i2)+"\t"+f.format(xbar)+"\t"+f.format(meta_low)+"\t"+f.format(meta_high)+"\n");
       /* System.out.println("meta="+xbar);
        System.out.println("Meta Low="+ meta_low);
        System.out.println("Meta up="+ meta_high+"\n===========================================");*/
        if(number>0) { // if total number of animals of all the records is greater than 0 then return phenominerExpectedRange
            phenominerExpectedRange.setRangeValue(Double.parseDouble(f.format(xbar)));
            phenominerExpectedRange.setRangeLow(Double.parseDouble(f.format(meta_low)));
            phenominerExpectedRange.setRangeHigh(Double.parseDouble(f.format(meta_high)));
            phenominerExpectedRange.setMin(Double.parseDouble(f.format(min)));
            phenominerExpectedRange.setMax(Double.parseDouble(f.format(max)));
          phenominerExpectedRange.setRangeSD(Double.parseDouble(f.format(newSD)));
     //       phenominerExpectedRange.setRangeSD(newSD);
            phenominerExpectedRange.setRange(Double.parseDouble(f.format(range)));
            return phenominerExpectedRange;
        }else return null;
    }
}
