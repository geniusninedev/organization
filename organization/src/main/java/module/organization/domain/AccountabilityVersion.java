/*
 * @(#)AccountabilityVersion.java
 *
 * Copyright 2012 Instituto Superior Tecnico
 * Founding Authors: João Figueiredo, Luis Cruz
 * 
 *      https://fenix-ashes.ist.utl.pt/
 * 
 *   This file is part of the Organization Module.
 *
 *   The Organization Module is free software: you can
 *   redistribute it and/or modify it under the terms of the GNU Lesser General
 *   Public License as published by the Free Software Foundation, either version 
 *   3 of the License, or (at your option) any later version.
 *
 *   The Organization Module is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with the Organization Module. If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package module.organization.domain;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import jvstm.cps.ConsistencyPredicate;
import module.organization.domain.util.OrganizationConsistencyException;

/**
 * 
 * @author João Antunes
 * @author João Neves
 * @author Susana Fernandes
 * 
 */
public class AccountabilityVersion extends AccountabilityVersion_Base {

    private AccountabilityVersion(LocalDate beginDate, LocalDate endDate, Accountability acc, boolean erased,
            String justification) {
        super();
        super.setAccountability(acc);
        super.setJustification(justification);
        super.setErased(erased);
        super.setBeginDate(beginDate);
        super.setEndDate(endDate);
        super.setCreationDate(new DateTime());
        super.setUserWhoCreated(Authenticate.getUser());
    }

    @Override
    public Accountability getAccountability() {
        return super.getAccountability();
    }

    @Override
    public boolean getErased() {
        return super.getErased();
    }

    @Override
    public LocalDate getBeginDate() {
        return super.getBeginDate();
    }

    @Override
    public LocalDate getEndDate() {
        return super.getEndDate();
    }

    @Override
    public DateTime getCreationDate() {
        return super.getCreationDate();
    }

    @Override
    public User getUserWhoCreated() {
        return super.getUserWhoCreated();
    }

    @ConsistencyPredicate
    public boolean checkIsConnectedToList() {
        return (getPreviousAccVersion() != null && getAccountability() == null)
                || (getPreviousAccVersion() == null && getAccountability() != null);
    }

    @ConsistencyPredicate
    public boolean checkErasedAsFinalVersion() {
        return !getErased() || getAccountability() != null;
    }

    @ConsistencyPredicate(OrganizationConsistencyException.class)
    protected boolean checkDateInterval() {
        return getBeginDate() != null && (getEndDate() == null || !getBeginDate().isAfter(getEndDate()));
    }

    public void delete() {
        super.setUserWhoCreated(null);
        deleteDomainObject();
    }

    /**
     * It creates a new AccountabilityHistory item and pushes the others (if
     * they exist)
     * 
     * @param beginDate beginDate
     * @param endDate endDate
     * @param acc
     *            the Accountability which
     * @param erased erased
     * @param justification an information justification/reason for the change of accountability, or null if there is none, or
     *            none is provided
     * 
     * 
     */
    protected static void insertAccountabilityVersion(LocalDate beginDate, LocalDate endDate, Accountability acc, boolean erased,
            String justification) {
        if (acc == null) {
            throw new IllegalArgumentException("cant.provide.a.null.accountability");
        }
        // let's check on the first case i.e. when the given acc does not have
        // an AccountabilityHistory associated
        AccountabilityVersion firstAccVersion = acc.getAccountabilityVersion();
        if (firstAccVersion == null) {
            //we are the first ones, let's just create ourselves
            if (erased) {
                throw new IllegalArgumentException("creating.a.deleted.acc.does.not.make.sense");//we shouldn't be creating a deleted accountability to start with!
            }
            new AccountabilityVersion(beginDate, endDate, acc, erased, justification);
        } else {
            // let's push all of the next accHistories into their rightful
            // position
            if (firstAccVersion.getBeginDate().equals(beginDate) && firstAccVersion.getErased() == erased
                    && matchingDates(firstAccVersion.getEndDate(), endDate)) {
                // do not create a new version with exactly the same data
                return;
            }
            AccountabilityVersion newAccountabilityHistory =
                    new AccountabilityVersion(beginDate, endDate, acc, erased, justification);
            newAccountabilityHistory.setNextAccVersion(firstAccVersion);
        }
    }

    public static boolean redundantInfo(AccountabilityVersion av1, AccountabilityVersion av2) {
        return ((av1.getBeginDate().equals(av2.getBeginDate())) && (av1.getErased() == av2.getErased())
                && matchingDates(av1.getEndDate(), av2.getEndDate()));
    }

    private static boolean matchingDates(LocalDate date1, LocalDate date2) {
        return date1 == null ? date2 == null : date1.equals(date2);
    }
}
