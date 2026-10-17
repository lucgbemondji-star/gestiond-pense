package com.depenses.service;

import com.depenses.model.Depense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class RapportService {
    public Map<LocalDate, BigDecimal> rapportMensuel(List<Depense> depenses, int annee, int mois) {
        Map<LocalDate, BigDecimal> totals = new TreeMap<>();
        for (Depense depense : depenses) {
            if (depense == null || depense.getDateDepense() == null) {
                continue;
            }
            if (depense.getDateDepense().getYear() == annee && depense.getDateDepense().getMonthValue() == mois) {
                LocalDate date = depense.getDateDepense();
                totals.merge(date, depense.getMontantXof() == null ? BigDecimal.ZERO : depense.getMontantXof(), BigDecimal::add);
            }
        }
        return totals;
    }
}
