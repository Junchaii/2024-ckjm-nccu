# Chidamber and Kemerer Java Metrics - NCCU Mod (a modified version from _ckjm_)
original version: https://github.com/dspinellis/ckjm

* The differences:
  * This version relies solely on Maven. The resulting JAR file, named 'xxx-with-dependencies.jar,' encapsulates all its dependencies within the same archive.
  * This version customizes several counting details to align with our research requirements, particularly those pertaining to the JDK and other infrastructural libraries and frameworks. Our focus lies on assessing the actual costs incurred by the end developer.
* Usage: 
  * In the terminal, switch to the project dir and then execute: java -jar .\target\ckjm-1.0-SNAPSHOT-jar-with-dependencies.jar <target to inspect>
  * (Example) java -jar .\target\ckjm-1.0-SNAPSHOT-jar-with-dependencies.jar .\target\classes\gr\spinellis\ckjm\*.class


--- 
# Original README.md

The program _ckjm_ calculates Chidamber and Kemerer object-oriented metrics by processing the bytecode of compiled Java files. The program calculates for each class the following six metrics proposed by Chidamber and Kemerer.

* WMC: Weighted methods per class
* DIT: Depth of Inheritance Tree
* NOC: Number of Children
* CBO: Coupling between object classes
* RFC: Response for a Class
* LCOM: Lack of cohesion in methods

In addition it also calculates for each class

* Ca: Afferent couplings
* NPM: Number of public methods

If you use this tool in your research, please cite the following paper.

Diomidis Spinellis. [Tool writing: A forgotten art?](http://www.spinellis.gr/pubs/jrnl/2005-IEEESW-TotT/html/v22n4.html) _IEEE Software_, 22(4):9–11, July/August 2005. [doi:10.1109/MS.2005.111](http://dx.doi.org/10.1109/MS.2005.111).

Visit the project's [home page](http://www.spinellis.gr/sw/ckjm/) for more information.
