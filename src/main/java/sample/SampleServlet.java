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

import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
/**
 * Although this appears to be a Web application, its sole purpose is to run
 * some initialization logic that retrieves a UserTransaction in an EE context
 * and make it available at a known location in the default JNDI namespace.
 */
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;
@WebServlet(value = "/sample", loadOnStartup = Integer.MAX_VALUE)
@ApplicationScoped
public class SampleServlet extends HttpServlet {
    static final Logger logger = Logger.getLogger(SampleServlet.class.getName());

    @Inject
    UserTransaction usertran;
    
    @PostConstruct
    void registerUserTransaction() {
        try {
            new InitialContext()
                    .createSubcontext("jta")
                    .rebind("usertransaction", usertran);
        } catch (NamingException e) {
            logger.log(Level.SEVERE, "Lookup or bind failed", e);
            throw new Error("Could not set up JNDI as needed", e);
        }
        logger.info("Checking async lookup from new thread");
        Thread asyncJob = new Thread(() -> {
            try {
                logger.info("Attempting lookup of user transaction object outside EE context");
                Object ut = new InitialContext().lookup("jta/usertransaction");
                logger.info("Looked up " + ut);
            } catch (NamingException e) {
                logger.log(Level.SEVERE, "Could not look up jta/usertransaction", e);
            }
        });
        asyncJob.start();
        try {
            asyncJob.join();
            logger.info("Async attempt finished");
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintStream ps = new PrintStream(resp.getOutputStream());
        try {
            ps.println("Injected user transaction object: " + usertran);
            ps.println("Looking up jta/usertransaction returns " + new InitialContext().lookup("jta/usertransaction"));
        } catch (Exception e) {
            ps.println("Attempting to look up jta/usertransaction resulted in an exception: " + e);
            e.printStackTrace(ps);
        }
    }
}
