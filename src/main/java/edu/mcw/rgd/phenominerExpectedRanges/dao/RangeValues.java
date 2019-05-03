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
    OntologyXDAO xdao= new OntologyXDAO();
    public PhenominerExpectedRange getRangeValues(List<Record> records, boolean isNormal) throws Exception {
        PhenominerExpectedRange phenominerExpectedRange= new PhenominerExpectedRange();
        double sigma= 1.96;
        if(isNormal){
            sigma=6;
        }
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
   if(records.size()>1) {
       for (Record r : records) {

           double value = Double.parseDouble(r.getMeasurementValue());

           int noOfAnimals = r.getSample().getNumberOfAnimals();


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
           wv += pwv;
           w += pw;
           w2 += pw2;
           n++;
           number += noOfAnimals;

       }
       double xbar = wv / w;
       double varXbar = 1 / w;
       double newSD = sigma * Math.sqrt(varXbar);
       double meta_low = xbar - sigma * Math.sqrt(varXbar);
       double meta_high = xbar + sigma * Math.sqrt(varXbar);

       double Q = 0;
       for (Record r : records) {
           double pvalue = Double.parseDouble(r.getMeasurementValue());
           double pSD = Double.parseDouble(r.getMeasurementSD());
           double pw = 1 / (pSD * pSD);

           double q = pw * (pvalue - xbar) * (pvalue - xbar);
           Q += q;
       }

       double q = Q;

       double i2 = Math.max(0, ((q - n + 1) / q));

       if (i2 > 0.85 || isNormal) { // RANDOM EFFECT

           double wNew = 0;
           double wvNew = 0;
           double c = w - w2 / w;
           double I2 = (q - n + 1) / c;
           for (Record r : records) {
               double value = Double.parseDouble(r.getMeasurementValue());
               double pSD = Double.parseDouble(r.getMeasurementSD());
               double pw = 1 / ((pSD * pSD) + I2);
               double pwv = value * pw;
               wvNew += pwv;
               wNew += pw;
           }
           wv = wvNew;
           w = wNew;
           xbar = wv / w;
           varXbar = 1 / w;
           newSD = sigma * Math.sqrt(varXbar);

           meta_low = xbar - sigma * Math.sqrt(varXbar);
           meta_high = xbar + sigma * Math.sqrt(varXbar);
       }


       double min = Collections.min(values);
       double max = Collections.max(values);
       double range = Math.round((max - min) * 100.0) / 100.0;


       if (number > 0 && varXbar > 0 && xbar > 0 && meta_low > 0 && meta_high > 0) { // if total number of animals of all the records is greater than 0 then return phenominerExpectedRange
           phenominerExpectedRange.setRangeValue((xbar));
           phenominerExpectedRange.setRangeLow((meta_low));
           phenominerExpectedRange.setRangeHigh((meta_high));
           phenominerExpectedRange.setMin(min);
           phenominerExpectedRange.setMax(max);
           phenominerExpectedRange.setRangeSD(newSD);
           //       phenominerExpectedRange.setRangeSD(newSD);
           phenominerExpectedRange.setRange(range);
           return phenominerExpectedRange;
       } else return null;
   }
        return null;
    }


    public PhenominerExpectedRange getNormalRangeValues(List<PhenominerExpectedRange> records, boolean isNormal) throws Exception {
        PhenominerExpectedRange phenominerExpectedRange= new PhenominerExpectedRange();

        double sigma= 6;
        if(records.size()>1) {
            List<Double> values = new ArrayList<>();
            double w = 0;
            double w2 = 0;
            double wv = 0;
            double n = 0;

            DecimalFormat f = new DecimalFormat(".##");


            for (PhenominerExpectedRange r : records) {

                double value = r.getRangeValue();
                values.add(r.getRangeValue());
                double pSD = r.getRangeSD();

                double pw = 1 / (pSD * pSD);
                double pw2 = pw * pw;
                double pwv = value * pw;
                wv += pwv;
                w += pw;
                w2 += pw2;
                n++;

            }
            double xbar = wv / w;
            double varXbar = 1 / w;
            double newSD = sigma * Math.sqrt(varXbar);
            double meta_low = xbar - sigma * Math.sqrt(varXbar);
            double meta_high = xbar + sigma * Math.sqrt(varXbar);

            double Q = 0;
            for (PhenominerExpectedRange r : records) {
                double pvalue = r.getRangeValue();
                double pSD = r.getRangeSD();
                double pw = 1 / (pSD * pSD);

                double q = pw * (pvalue - xbar) * (pvalue - xbar);
                Q += q;
            }

            double q = Q;

            double i2 = Math.max(0, ((Q - n + 1) / Q));

            if (i2 > 0.85 || isNormal) { // RANDOM EFFECT
                double wNew = 0;
                double wvNew = 0;
                double c = w - w2 / w;
                double I2 = (Q - n + 1) / c;
                for (PhenominerExpectedRange r : records) {
                    double value = r.getRangeValue();
                    double pSD = r.getRangeSD();
                    double pw = 1 / ((pSD * pSD) + I2);
                    double pwv = value * pw;
                    wvNew += pwv;
                    wNew += pw;
                }
                wv = wvNew;
                w = wNew;
                xbar = wv / w;
                varXbar = 1 / w;
                newSD = Math.sqrt(varXbar);
                meta_low = xbar - sigma * Math.sqrt(varXbar);
                meta_high = xbar + sigma* Math.sqrt(varXbar);
            }


            double min = Collections.min(values);
            double max = Collections.max(values);
            double range = Math.round((max - min) * 100.0) / 100.0;



            if (n > 0 && xbar > 0 && meta_low > 0 && meta_high > 0) { // if total number of animals of all the records is greater than 0 then return phenominerExpectedRange
                phenominerExpectedRange.setRangeValue(xbar);
                phenominerExpectedRange.setRangeLow(meta_low);
                phenominerExpectedRange.setRangeHigh(meta_high);
                phenominerExpectedRange.setMin(min);
                phenominerExpectedRange.setMax(max);
                phenominerExpectedRange.setRangeSD(newSD);
                //       phenominerExpectedRange.setRangeSD(newSD);
                phenominerExpectedRange.setRange(range);
                return phenominerExpectedRange;

            } else return null;
        }else{

            return records.get(0);
        }

    }
}

