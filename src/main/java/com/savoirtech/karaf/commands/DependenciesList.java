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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.shell.util.ShellUtil;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;

@Command(scope = "aetos", name = "dependencies-list", description = "Karaf Bundle dependencies list Command")
public class DependenciesList extends OsgiCommandSupport {

    public static final String NONSTANDARD_SERVICE_NAMESPACE = "service";
    private static final String EMPTY_MESSAGE = "[EMPTY]";
    private static final String UNRESOLVED_MESSAGE = "[UNRESOLVED]";

    @Argument(index = 0, name = "ids", description = "The list of bundle (identified by IDs or name or name/version) separated by whitespaces", required = false, multiValued = true)
    List<String> ids;

    @Option(name = "--namespace")
    String namespace = "*";
    
    BundleService bundleService;
    private static Set allDependencies;
    
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
            Pattern ns = Pattern.compile(namespace.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*"));
            for (Bundle b : bundles) {

                allDependencies = new HashSet<Requirement>();
                BundleWiring wiring = b.adapt(BundleWiring.class);
                if (wiring != null) {
                    System.out.println(" BundleID  SymbolicName");
                    boolean matches = printMatchingRequirements(wiring, ns);
                    // Handle service requirements separately, since they aren't part
                    // of the generic model in OSGi.
                    if (matchNamespace(ns, NONSTANDARD_SERVICE_NAMESPACE)) {
                        matches |= printServiceRequirements(b);
                    }
                    if (!matches) {
                        System.out.println(namespace + " " + EMPTY_MESSAGE);
                    }
                    Iterator<Requirement> it = allDependencies.iterator();
                    while(it.hasNext()) {
                        System.out.println(it.next().toString());
                    }
                } else {
                    System.out.println("Bundle " + b.getBundleId() + " is not resolved.");
                }
            }
        } catch (Exception e) {
            //Ignore
        }
    }

    private static boolean printMatchingRequirements(BundleWiring wiring, Pattern namespace) {
        List<BundleWire> wires = wiring.getRequiredWires(null);
        Map<BundleRequirement, List<BundleWire>> aggregateReqs = aggregateRequirements(namespace, wires);
        List<BundleRequirement> allReqs = wiring.getRequirements(null);
        boolean matches = false;
        for (BundleRequirement req : allReqs) {
            if (matchNamespace(namespace, req.getNamespace())) {
                matches = true;
                List<BundleWire> providers = aggregateReqs.get(req);
                if (providers != null) {
                    for (BundleWire wire : providers) {
                        Bundle b = wire.getProviderWiring().getBundle();
                        Requirement dep = new Requirement(b);
                        allDependencies.add(dep);
                    }
                } else {
                    System.out.println(req.getNamespace() + "; "
                                    + req.getDirectives().get(Constants.FILTER_DIRECTIVE) + " " + UNRESOLVED_MESSAGE);
                }
            }
        }
        return matches;
    }

    private static Map<BundleRequirement, List<BundleWire>> aggregateRequirements(
            Pattern namespace, List<BundleWire> wires) {
        // Aggregate matching capabilities.
        Map<BundleRequirement, List<BundleWire>> map = new HashMap<BundleRequirement, List<BundleWire>>();
        for (BundleWire wire : wires) {
            if (matchNamespace(namespace, wire.getRequirement().getNamespace())) {
                List<BundleWire> providers = map.get(wire.getRequirement());
                if (providers == null) {
                    providers = new ArrayList<BundleWire>();
                    map.put(wire.getRequirement(), providers);
                }
                providers.add(wire);
            }
        }
        return map;
    }

    static boolean printServiceRequirements(Bundle b) {
        boolean matches = false;

        try {
            ServiceReference<?>[] refs = b.getServicesInUse();

            if ((refs != null) && (refs.length > 0)) {
                matches = true;
                // Print properties for each service.
                for (ServiceReference<?> ref : refs) {
                    // Print object class with "namespace".
                    Requirement dep = new Requirement(ref.getBundle());
                    allDependencies.add(dep);
                }
            }
        } catch (Exception ex) {
            System.err.println(ex.toString());
        }

        return matches;
    }

    private static boolean matchNamespace(Pattern namespace, String actual) {
        return namespace.matcher(actual).matches();
    }

    public void setBundleService(BundleService bundleSelector) {
        this.bundleService = bundleSelector;
    }

    private static class Requirement {

        public String packageName;
        public Long bundleId;

        public Requirement(Bundle bundle) {
            this.packageName = bundle.getSymbolicName();
            this.bundleId = bundle.getBundleId();
        }

        public String toString() {
            String result = String.format(" %8d  %s", bundleId, packageName);
            return result;
        }

        public boolean equals(Object o) {
            if(o == null) return false;
            if(this == o) return true;

            Requirement other = (Requirement) o;
            if (! this.packageName.equals(other.packageName)) return false;
            if (this.bundleId != other.bundleId) return false;
 
            return true;
        }

        public int hashCode() {
            int result = 0;
            result = 31 * result + this.packageName.hashCode();
            return result;
        }

    }
  
}
