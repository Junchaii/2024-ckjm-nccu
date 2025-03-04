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

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;

import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Visit a class updating its Chidamber-Kemerer metrics.
 *
 * @author <a href="http://www.spinellis.gr">Diomidis Spinellis</a>
 * @version $Revision: 1.21 $
 * @see ClassMetrics
 */
public class ClassVisitor extends org.apache.bcel.classfile.EmptyVisitor {
    /**
     * The class being visited.
     */
    private JavaClass visitedClass;
    /**
     * The class's constant pool.
     */
    private ConstantPoolGen cp;
    /**
     * The class's fully qualified name.
     */
    private String myClassName;
    /**
     * The container where metrics for all classes are stored.
     */
    private ClassMetricsContainer cmap;
    /**
     * The emtrics for the class being visited.
     */
    private ClassMetrics cm;
    /* Classes encountered.
     * Its cardinality is used for calculating the CBO.
     */
    private HashSet<String> efferentCoupledClasses = new HashSet<String>();
    private HashSet<String> diEfferentCoupledClasses = new HashSet<String>();
    /**
     * Methods encountered in the same package.
     * Its cardinality is used for calculating the SRFC.
     */
    private HashSet<String> samePackageResponseSet = new HashSet<String>();
    /**
     * Methods encountered in different packages.
     * Its cardinality is used for calculating the DRFC.
     */
    private HashSet<String> differentPackageResponseSet = new HashSet<String>();
    /**
     * Use of fields in methods.
     * Its contents are used for calculating the LCOM.
     * We use a Tree rather than a Hash to calculate the
     * intersection in O(n) instead of O(n*n).
     */
    ArrayList<TreeSet<String>> mi = new ArrayList<TreeSet<String>>();
    /**
     * The maximum number of lines of code count in class
     * It is used to calculate complexity value of WMC
     */
    private float maxLoc = 0;
    /**
     * The minimum number of lines of code count in class
     * It is used to calculate complexity value of WMC
     */
    private float minLoc = 1;

    public ClassVisitor(JavaClass jc, ClassMetricsContainer classMap) {
        visitedClass = jc;
        cp = new ConstantPoolGen(visitedClass.getConstantPool());
        cmap = classMap;
        myClassName = jc.getClassName();
        cm = cmap.getMetrics(myClassName);
    }

    /**
     * Return the class's metrics container.
     */
    public ClassMetrics getMetrics() {
        return cm;
    }

    public void start() {
        visitJavaClass(visitedClass);
    }

    /**
     * Calculate the class's metrics based on its elements.
     */
    public void visitJavaClass(JavaClass jc) {
        AnnotationEntry[] annotations = jc.getAnnotationEntries();
        for (AnnotationEntry annotation : annotations) {
            registerCoupling(annotation.getAnnotationType());
        }

        String super_name = jc.getSuperclassName();
        String package_name = jc.getPackageName();

        cm.setVisited();
        if (jc.isPublic())
            cm.setPublic();
        ClassMetrics pm = cmap.getMetrics(super_name);

        pm.incNoc();
        try {
            int superClassesLength = jc.getSuperClasses().length;

            /* Measuring decision: don't couple to Java SDK */
            /* Print DIT details & Set DIT */
            for (JavaClass superClass : jc.getSuperClasses()) {
                if (ClassMetrics.isJdkClass(superClass.getClassName())) {
                    superClassesLength--;
                } else {
                    System.out.println("(DIT)SuperClass->" + superClass.getClassName());
                }
            }
            cm.setDit(superClassesLength);

        } catch (ClassNotFoundException ex) {
            System.err.println("Error obtaining all superclasses of " + jc);
        }
        registerCoupling(super_name);

        String ifs[] = jc.getInterfaceNames();
        /* Measuring decision: couple interfaces */
        for (int i = 0; i < ifs.length; i++)
            registerCoupling(ifs[i]);

        Field[] fields = jc.getFields();
        for (int i = 0; i < fields.length; i++)
            fields[i].accept(this);

        Method[] methods = jc.getMethods();
        for (int i = 0; i < methods.length; i++)
            methods[i].accept(this);
    }

    /**
     * Add a given class to the classes we are coupled to
     */
    public void registerCoupling(String className) {
        if(className.startsWith("Ljavax/") || className.startsWith("Ljava/") || className.startsWith("Lcom/")){
            return;
        }

        if (className.contains("springframework") && className.startsWith("Lorg")) {
            diEfferentCoupledClasses.add(className);
            return;
        }

        // expLambda: The class generated by the compiler expansion of lambda
        List<String> expLambda = Arrays.asList("accept", "test", "apply");
        /* Measuring decision: don't couple to Java SDK and the class generated by the compiler expansion of lambda*/
        if ((MetricsFilter.isJdkIncluded() ||
                !ClassMetrics.isJdkClass(className)) &&
                !myClassName.equals(className) && !expLambda.contains(className)) {
            efferentCoupledClasses.add(className);
            cmap.getMetrics(className).addAfferentCoupling(myClassName);
        }
    }

    /* Add the type's class to the classes we are coupled to */
    public void registerCoupling(Type t) {
        registerCoupling(className(t));
    }

    /* Add a given class to the classes we are coupled to */
    void registerFieldAccess(String className, String fieldName) {
        registerCoupling(className);
        if (className.equals(myClassName))
            mi.get(mi.size() - 1).add(fieldName);
    }

    /* Add a given method to our response set */
    void registerMethodInvocation(String className, String methodName, Type[] args) {
        registerCoupling(className);
        /* Measuring decision: calls to JDK methods are included in the RFC calculation */
        incRFC(className, methodName, args);
    }

    /**
     * Called when a field access is encountered.
     */
    public void visitField(Field field) {
        AnnotationEntry[] annotations = field.getAnnotationEntries();
        for (AnnotationEntry annotation : annotations) {
            registerCoupling(annotation.getAnnotationType());
        }

        registerCoupling(field.getType());
    }

    /**
     * Called when encountering a method that should be included in the
     * class's RFC.
     */
    private void incRFC(String className, String methodName, Type[] arguments) {
        // expLambda: The method generated by the compiler expansion of lambda
        List<String> expLambda = Arrays.asList("accept", "test", "apply");
        /* Measuring decision: <init> method generated by creating a JDK object is not included in the calculation.
         *   and the method generated by the compiler expansion of lambda is not included in the calculation. */
        if (!(ClassMetrics.isJdkClass(className) && methodName.equals("<init>"))
                && !expLambda.contains(className)) {
            String argumentList = Arrays.asList(arguments).toString();
            // remove [ ] chars from begin and end
            String args = argumentList.substring(1, argumentList.length() - 1);
            String signature = className + "." + methodName + "(" + args + ")";

            if (getPackageName(className).equals(getPackageName(myClassName))) {
                // same package
                samePackageResponseSet.add(signature);
            } else {
                // different package
                differentPackageResponseSet.add(signature);
            }
        }
    }

    /** Helper method to extract package name from a fully qualified class name. */
    private String getPackageName(String className) {
        int lastDotIndex = className.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return ""; // default package
        }
        return className.substring(0, lastDotIndex);
    }

    /**
     * Called when a method invocation is encountered.
     */
    public void visitMethod(Method method) {
        MethodGen mg = new MethodGen(method, visitedClass.getClassName(), cp);

        AnnotationEntry[] annotations = method.getAnnotationEntries();
        for (AnnotationEntry annotation : annotations) {
            registerCoupling(annotation.getAnnotationType());
        }

        Type result_type = mg.getReturnType();
        Type[] argTypes = mg.getArgumentTypes();

        registerCoupling(mg.getReturnType());
        for (int i = 0; i < argTypes.length; i++)
            registerCoupling(argTypes[i]);

        String[] exceptions = mg.getExceptions();
        for (int i = 0; i < exceptions.length; i++)
            registerCoupling(exceptions[i]);

        /* Loc: lines of code count (each method)  */
        float loc = 0;
        LineNumberGen[] il = mg.getLineNumbers();
        if (il != null) {
            loc = il.length;
            System.out.println(method.getName() + " (LOC): " + loc);
            if (loc < minLoc) {
                minLoc = loc;
            } else if (loc > maxLoc) {
                maxLoc = loc;
            }
        }

        /* Measuring decision: A class's own methods contribute to its RFC */
        incRFC(myClassName, method.getName(), argTypes);

        /* Measuring decision: lambda methods generated by compiler are not included in the WMC and NPM calculation. */
        if (!method.getName().startsWith("lambda$")) {
            cm.putLocArray(loc);
            /* Print WMC details */
            String argumentList = Arrays.asList(argTypes).toString();
            // remove [ ] chars from begin and end
            String args = argumentList.substring(1, argumentList.length() - 1);
            System.out.println("(WMC)all methods->>" + myClassName + "." + method.getName() + "(" + args + ")");


            if (Modifier.isPublic(method.getModifiers())) {
                cm.incNpm();
                /* Print NPM details */
                System.out.println("(NPM)public methods->>" + myClassName + "." + method.getName() + "(" + args + ")");
            }
        }


        mi.add(new TreeSet<String>());
        MethodVisitor factory = new MethodVisitor(mg, this);
        factory.start();
    }

    /**
     * Return a class name associated with a type.
     */
    static String className(Type t) {
        String ts = t.toString();

        if (t.getType() <= Constants.T_VOID) {
            return "java.PRIMITIVE";
        } else if (t instanceof ArrayType) {
            ArrayType at = (ArrayType) t;
            return className(at.getBasicType());
        } else {
            return t.toString();
        }
    }

    /**
     * Do final accounting at the end of the visit.
     */
    public void end() {
        /* set minLoc and maxLoc */
        cm.setMinLoc(minLoc);
        cm.setMaxLoc(maxLoc);

        cm.setCbo(efferentCoupledClasses.size());
        /* Print CBO details */
        for (String className : efferentCoupledClasses) {
            System.out.println("(CBO)CoupledClass->>" + className);
        }

        cm.setDicbo(diEfferentCoupledClasses.size());
        for (String className : diEfferentCoupledClasses) {
            System.out.println("(DICBO)CoupledClass->>" + className);
        }

        cm.setSrfc(samePackageResponseSet.size());
        cm.setDrfc(differentPackageResponseSet.size());
        /* Print SRFC & DRFC details */
        System.out.println("(SRFC) Same Package Response Set Size: " + cm.getSrfc());
        for (String response : samePackageResponseSet) {
            System.out.println("(SRFC) Response ->> " + response);
        }

        System.out.println("(DRFC) Different Package Response Set Size: " + cm.getDrfc());
        for (String response : differentPackageResponseSet) {
            System.out.println("(DRFC) Response ->> " + response);
        }

        /*
         * Calculate LCOM  as |P| - |Q| if |P| - |Q| > 0 or 0 otherwise
         * where
         * P = set of all empty set intersections
         * Q = set of all nonempty set intersections
         */
        int lcom = 0;
        for (int i = 0; i < mi.size(); i++)
            for (int j = i + 1; j < mi.size(); j++) {
                /* A shallow unknown-type copy is enough */
                TreeSet<?> intersection = (TreeSet<?>) mi.get(i).clone();
                intersection.retainAll(mi.get(j));
                if (intersection.size() == 0)
                    lcom++;
                else
                    lcom--;
            }
        cm.setLcom(lcom > 0 ? lcom : 0);
    }
}
