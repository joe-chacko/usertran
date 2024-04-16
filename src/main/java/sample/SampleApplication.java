/*
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package sample;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Although this appears to be a Web application, its sole purpose is to run
 * some initialization logic that retrieves a UserTransaction in an EE context
 * and make it available at a known location in the default JNDI namespace.
 */
@ApplicationPath("/sample")
public class SampleApplication extends Application {
    static final Logger logger = Logger.getLogger(SampleApplication.class.getName());
    static final String EE_CONTEXT_LOOKUP = "java:comp/UserTransaction";
    static final String USER_DEFINED_LOOKUP = "jta/usertransaction";

    @PostConstruct
    void registerUserTransaction() {
        lookupUserTransactionAndBindInUserDefinedLocation();
        verifyUserTransactionBindingFromNonEeContext();
    }

    private void lookupUserTransactionAndBindInUserDefinedLocation() {
        try {
            final InitialContext ic = new InitialContext();
            logger.info("Looking up user transaction object during @PostConstruct.");
            Object ut = ic.lookup(EE_CONTEXT_LOOKUP);
            logger.info("Retrieved object: " + ut);
            logger.info("Storing UserTransaction object in JNDI as " + USER_DEFINED_LOOKUP);
            createIntermediateContexts(ic, USER_DEFINED_LOOKUP);
            ic.rebind(USER_DEFINED_LOOKUP, ut);
        } catch (NamingException e) {
            logger.log(Level.SEVERE, "Lookup or bind failed", e);
            throw new Error("Could not set up JNDI as needed", e);
        }
    }

    private void createIntermediateContexts(InitialContext ic, String jndiName) throws NamingException {
        Context ctx = ic;
        String[] parts = jndiName.split("/");
        final int numContexts = parts.length - 1; // last part is not a context
        for (int i = 0; i < numContexts; i++) {
            final String name = parts[i];
            try {
                ctx = ctx.createSubcontext(name);
                logger.info("Created subcontext " + ctx.getNameInNamespace());
            } catch (NameAlreadyBoundException e) {
                logger.info(ctx.getNameInNamespace() + " already contains a binding for " + name);
                ctx = (Context) ctx.lookup(name);
                logger.info("Retrieved already bound context " + ctx.getNameInNamespace());
            }
        }
    }

    static final Runnable LOOKUP_USER_TRAN = new Runnable() {
        public void run() {
            try {
                logger.info("Attempting lookup of " + USER_DEFINED_LOOKUP);
                var ut = new InitialContext().lookup(USER_DEFINED_LOOKUP);
                logger.info("Looked up " + USER_DEFINED_LOOKUP + " -> " + ut);
            } catch (NamingException e) {
                logger.log(Level.SEVERE, "Could not look up " + USER_DEFINED_LOOKUP, e);
            }
        }
    };

    private void verifyUserTransactionBindingFromNonEeContext() {
        logger.info("Checking async lookup from new thread");
        Thread asyncJob = new Thread(LOOKUP_USER_TRAN);
        asyncJob.start();
        try {
            asyncJob.join();
            logger.info("Async attempt finished");
        } catch (InterruptedException ignored) {
        }
    }
}
