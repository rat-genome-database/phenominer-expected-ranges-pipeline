package edu.mcw.rgd.phenominerExpectedRanges.dao;

import edu.mcw.rgd.dao.impl.OntologyXDAO;
import edu.mcw.rgd.datamodel.pheno.Record;
import edu.mcw.rgd.datamodel.phenominerExpectedRange.PhenominerExpectedRange;
import edu.mcw.rgd.phenominerExpectedRanges.model.RecordExt;
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
        double wt = 0;
        double es = 0;
        double wes = 0;
        double wes2 = 0;
        double wv = 0;
        double wv2 = 0;
        double sd = 0;
        double wsd = 0;
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
     //     System.out.println("Strain_acc_id\tStrain\tCMO_id\tClinicalMeasurement\tcondition_description\tmeasurement_method_id\tmesaurement_method\tSex\tno_of_animals\tage_low_bound\t age_high_bound\tSD\tValue\tci_start\tci_end\tw\tw2");
        for(Record r:records) {

            double value=Double.parseDouble(r.getMeasurementValue());
            int noOfAnimals=r.getSample().getNumberOfAnimals();
            double pSD = Double.parseDouble(r.getMeasurementSD());
            double se = (value) / Math.sqrt(r.getSample().getNumberOfAnimals());
            double ci = 1.960 * se;
            double ci_start =value - ci;
            double ci_end = value + ci;

            values.add(value);
            ciStart.add(ci_start);
            ciEnd.add(ci_end);
           /****************************************************************/
            double pw = 1 / (pSD * pSD);
            double pw2 = pw * pw;
            w += pw;
            w2 += pw2;
            n++;
            number += noOfAnimals;

        /*  System.out.println(r.getSample().getStrainAccId()+ "\t"+getTerm(r.getSample().getStrainAccId()).getTerm() + "\t"+
                    r.getClinicalMeasurement().getAccId() + "\t"+getTerm(r.getClinicalMeasurement().getAccId()).getTerm()  + "\t"+ r.getConditionDescription() + "\t"+r.getMeasurementMethodId() + "\t"+ getTerm(r.getMeasurementMethod().getAccId()).getTerm() + "\t"+ r.getSample().getSex()+ "\t"+r.getSample().getNumberOfAnimals()+
                    "\t"+ r.getSample().getAgeDaysFromLowBound() + "\t"+ r.getSample().getAgeDaysFromHighBound()
                    +"\t"+ r.getMeasurementSD() +"\t"+ r.getMeasurementValue()
                    +"\t" + f.format(ci_start)+ "\t" + f.format(ci_end)
                    +"\t"+w+"\t"+ w2) ;*/
        }
  //  System.err.println("before w: "+ w);
        for(Record r:records) {
            double pvalue=Double.parseDouble(r.getMeasurementValue());
            double pSD= Double.parseDouble(r.getMeasurementSD());
            double pw = 1 / (pSD * pSD);
            double pes = pvalue/ pSD;
            double pwt = pw / w;
            double pwv = pvalue * pwt;
            double pwsd = pSD * pwt;
            double pwes = pes * pes * pw;
            double pwes2 = pes * pes * pw * pw;

            wt += pwt;
            wv += pwv;
            wes += pwes;
            wes2 += pwes2;
            sd += pSD;
            wsd += pwsd;

        }
        double q = wes-wes2/w;
        double i2 = (q-n+1)/q;
        double fixed = wv; double meta = fixed; double random= fixed;

        if(i2>0.85){

           double v = (q - number - 1)/(w-w2/w);
            w = 0;  wt = 0;  wes = 0;  wes2 = 0;  wv = 0;  wsd = 0;
            for(Record r:records) {
                double pSD=Math.round(Double.parseDouble(r.getMeasurementSD())*100.0)/100.0;
                 double pw = 1 / (pSD * pSD + v);
                 w += pw;

            }

            for(Record r:records) {
                double pSD=Math.round(Double.parseDouble(r.getMeasurementSD())*100.0)/100.0;

                double pvalue=Math.round((Double.parseDouble(r.getMeasurementValue()))*100.0)/100.0;
                double  pw = 1 / (pSD * pSD + v);
                double pwt = pw / w;
                double pwv = pvalue * pwt;
                double pwsd = pSD * pwt;
                double pes = pvalue/ pSD;
                double pwes = pes * pes * pw;
                double  pwes2 = pes * pes * pw * pw;

                wt += pwt;
                wv += pwv;
                wes += pwes;
                wes2 += pwes2;
                wsd += pwsd;
            }
        }

        double asd = sd/n;
        double newSD = (3 * asd / Math.sqrt(number));
        double meta_low = meta - 3*asd/Math.sqrt(number);
        double meta_up = meta + 3*asd/Math.sqrt(number);
        if(i2>0.85){
            random = wv;
            if(random> Collections.min(ciStart))
            {meta = random;}
            meta_low = meta - 3*asd/Math.sqrt(number);
            meta_up = meta + 3*asd/Math.sqrt(number);
        }

       /* System.out.println("META: " + meta);
        System.out.println("FIXED: " + fixed);*/
        double min = Collections.min(values);
        double max = Collections.max(values);
        double range = Math.round((max - min) * 100.0) / 100.0;


        if(number>0) { // if total number of animals of all the records is greater than 0 then return phenominerExpectedRange
            phenominerExpectedRange.setRangeValue(Double.parseDouble(f.format(meta)));
            phenominerExpectedRange.setRangeLow(Double.parseDouble(f.format(meta_low)));
            phenominerExpectedRange.setRangeHigh(Double.parseDouble(f.format(meta_up)));
            phenominerExpectedRange.setMin(Double.parseDouble(f.format(min)));
            phenominerExpectedRange.setMax(Double.parseDouble(f.format(max)));
            phenominerExpectedRange.setRangeSD(Double.parseDouble(f.format(asd)));
            phenominerExpectedRange.setRange(Double.parseDouble(f.format(range)));
            return phenominerExpectedRange;
        }else return null;
    }
}
