package com.jeonbuk.repair.repository;

import com.jeonbuk.repair.model.InsuranceCompany;
import com.jeonbuk.repair.util.Tx;

import java.util.List;

public class InsuranceCompanyRepository {

    public List<InsuranceCompany> findAll() {
        return Tx.read(s -> s.createQuery(
                        "from InsuranceCompany order by name", InsuranceCompany.class)
                .getResultList());
    }

    public List<String> findAllNames() {
        return Tx.read(s -> s.createQuery(
                        "select name from InsuranceCompany order by name", String.class)
                .getResultList());
    }
}
