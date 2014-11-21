/*
 * Dependencies List 
 *
 * Copyright (c) 2014, Savoir Technologies, Inc., All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */

package com.savoirtech.karaf.commands;

import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import org.apache.karaf.bundle.core.BundleService;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;

@Command(scope = "aetos", name = "dependencies-list", description = "Karaf Bundle dependencies list Command")
public class DependenciesList extends OsgiCommandSupport {

    @Argument(index = 0, name = "ids", description = "The list of bundle (identified by IDs or name or name/version) separated by whitespaces", required = false, multiValued = true)
    List<String> ids;
    
    BundleService bundleService;
    
    protected Object doExecute() throws Exception {
        doExecute(true);
        return null;
    }

    protected Object doExecute(boolean force) throws Exception {
        List<Bundle> bundles = bundleService.selectBundles(ids, force);
        doExecute(bundles);
        return null;
    }

    protected void doExecute(List<Bundle> bundles) throws Exception {
        try {
            System.out.println("Note: For more detail use requirements command.");
        } catch (Exception e) {
            //Ignore
        }
    }

    public void setBundleService(BundleService bundleSelector) {
        this.bundleService = bundleSelector;
    }

    // Get bundle wiring
    // Create pruned list of BundleID - URL
    // Pretty Print table.

    private class Requirement {

        private String packageName;
        private String bundleId;

        public Requirement(String packageName, String bundleId) {
            this.packageName = packageName;
            this.bundleId = bundleId;
        }

        public String toString() {
            String result = String.format("%4s %s", bundleId, packageName);
            return result;
        }

    }
  
}
