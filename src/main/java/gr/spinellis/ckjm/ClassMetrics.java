/*
 * (C) Copyright 2005 Diomidis Spinellis
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package gr.spinellis.ckjm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Store details needed for calculating a class's Chidamber-Kemerer metrics.
 * Most fields in this class are set by ClassVisitor.
 * This class also encapsulates some policy decision regarding metrics
 * measurement.
 *
 * @see ClassVisitor
 * @version $Revision: 1.12 $
 * @author <a href="http://www.spinellis.gr">Diomidis Spinellis</a>
 */
public class ClassMetrics {
    /** Weighted methods per class */
    private float wmc;
    /** Complexity value per method */
    ArrayList<Float> locArray = new ArrayList<>();
    /** Number of children */
    private int noc;
    /** Response for a Class */
    private int rfc;
    /** Same Package Response for a Class */
    private int srfc;
    /** Different Package Response for a Class */
    private int drfc;
    /** Coupling between object classes */
    private int cbo;
    /** Coupling between object classes specific to the Spring Framework */
    private int dicbo;
    /** Depth of inheritence tree */
    private int dit;
    /** Lack of cohesion in methods */
    private int lcom;
    /** Number of public methods */
    private int npm;
    /** The maximum number of lines of code count in class
     * It is used to calculate complexity value of WMC
     * */
    private float maxLoc = 0;
    /** The minimum number of lines of code count in class
     * It is used to calculate complexity value of WMC
     * */
    private float minLoc = 0;
    /** True if the class has been visited by the metrics gatherer */
    private boolean visited;
    /** True if the class is public */
    private boolean isPublicClass;
    /** Coupled classes: classes that use this class */
    private HashSet<String> afferentCoupledClasses;

    /** Default constructor. */
    ClassMetrics() {
	wmc = 0;
	noc = 0;
	cbo = 0;
    dicbo = 0;
	npm = 0;
	visited = false;
	afferentCoupledClasses = new HashSet<String>();
    }

    /** set the minimum number of lines of code count */
    public void setMinLoc(float number) {
        minLoc = number;
    }

    /** set the maximum number of lines of code count */
    public void setMaxLoc(float number) {
        maxLoc = number;
    }

    /** Increment the weighted methods count */
    public void putLocArray(float cx) {
        locArray.add(cx);
    }
    /** Return the weighted methods per class metric */
    public float getWmc() {
        System.out.println("minLoc: " + minLoc);
        System.out.println("maxLoc: " + maxLoc);
        for(float eachLoc: locArray) {
            if(eachLoc == minLoc) {
                wmc = wmc + 1;
                System.out.println("loc: " + eachLoc + " cx: " + 1);
                continue;
            } else if(eachLoc == maxLoc) {
                wmc = wmc + 2;
                System.out.println("loc: " + eachLoc + " cx: " + 2);
                continue;
            }

            wmc = wmc + ((eachLoc - minLoc)/(maxLoc - minLoc) + 1);
            float thisWmc = ((eachLoc - minLoc)/(maxLoc - minLoc) + 1);
            System.out.println("loc: " + eachLoc + " cx: " + thisWmc);
        }
        return wmc;
    }

    /** Increment the number of children */
    public void incNoc() { noc++; }
    /** Return the number of children */
    public int getNoc() { return noc; }

    /** Increment the Response for a Class */
    public void setRfc(int r) { rfc = r; }
    /** Return the Response for a Class */
    public int getRfc() { return rfc; }

    /** Set the Same Package Response for a Class */
    public void setSrfc(int sr) { srfc = sr; }

    /** Return the Same Package Response for a Class */
    public int getSrfc() { return srfc; }

    /** Set the Different Package Response for a Class */
    public void setDrfc(int dr) { drfc = dr; }

    /** Return the Different Package Response for a Class */
    public int getDrfc() { return drfc; }

    /** Set the depth of inheritence tree metric */
    public void setDit(int d) { dit = d; }
    /** Return the depth of the class's inheritance tree */
    public int getDit() { return dit; }

    /** Set the coupling between object classes metric */
    public void setCbo(int c) { cbo = c; }
    /** Return the coupling between object classes metric */
    public int getCbo() { return cbo; }

    /** Set the coupling between object classes specific to the Spring Framework metric  */
    public void setDicbo(int dic) {
        dicbo = dic;
    }
    /** Return the coupling between object classes specific to the Spring Framework metric  */
    public int getDicbo() {
        return dicbo;
    }

    /** Return the class's lack of cohesion in methods metric */
    public int getLcom() { return lcom; }
    /** Set the class's lack of cohesion in methods metric */
    public void setLcom(int l) { lcom = l; }

    /** Return the class's afferent couplings metric */
    public int getCa() { return afferentCoupledClasses.size(); }
    /** Add a class to the set of classes that depend on this class */
    public void addAfferentCoupling(String name) { afferentCoupledClasses.add(name); }

    /** Increment the number of public methods count */
    public void incNpm() { npm++; }
    /** Return the number of public methods metric */
    public int getNpm() { return npm; }

    /** Return true if the class is public */
    public boolean isPublic() { return isPublicClass; }
    /** Call to set the class as public */
    public void setPublic() { isPublicClass = true; }

    /** Return true if the class name is part of the Java SDK */
    public static boolean isJdkClass(String s) {
	return (s.startsWith("java.") ||
		s.startsWith("javax.") ||
		s.startsWith("org.omg.") ||
		s.startsWith("org.w3c.dom.") ||
		s.startsWith("org.xml.sax."));
    }

    /** Return the 6 CK metrics plus Ce as a space-separated string */
    public String toString() {
	return (
		getWmc() +
		" " + getDit() +
		" " + noc +
		" " + cbo +
        " " + dicbo +
		" " + (srfc + drfc) +
		" " + lcom +
		" " + getCa()+
		" " + npm  +
        " " + srfc +
        " " + drfc
        );
    }

    /** Mark the instance as visited by the metrics analyzer */
    public void setVisited() { visited = true; }
    /**
     * Return true if the class has been visited by the metrics analyzer.
     * Classes may appear in the collection as a result of some kind
     * of coupling.  However, unless they are visited and analyzed,
     * we do not want them to appear in the output results.
     */
    public boolean isVisited() { return visited; }
}
