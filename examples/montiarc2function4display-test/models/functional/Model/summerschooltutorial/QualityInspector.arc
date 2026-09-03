/* <- Step QUALITY_INSPECTOR FILE -> */
package summerschooltutorial;

import summerschooltutorial.IMMTypes.Part;
import summerschooltutorial.IMMTypes.QualityVerdict;  //<- SERVICE_QUALITY_CONTROL SOLUTION ->

/*
 * The machine's quality inspection: every produced part passes over the inspection
 * table before it may leave the factory. In the model the inspector is a relay on
 * the part flow — the machine itself decides which parts pass.
 */
component QualityInspector {

  // ports
/* <- Step QUALITY_INSPECTOR TASK : START -> */
  /* TODO: model the relay — every produced part enters as 'prodPart', passing parts
     leave as 'checkedPart' (both carry the Part type from IMMTypes). */
/* <- Step QUALITY_INSPECTOR TASK : END -> */
/* <- Step QUALITY_INSPECTOR SOLUTION : START -> */
  port in Part prodPart;
  port out Part checkedPart;
/* <- Step QUALITY_INSPECTOR SOLUTION : END -> */

/* <- Step SERVICE_QUALITY_CONTROL TASK : START -> */
  /* TODO: give the inspector its DT-facing ports — an in port for the QualityVerdict
     the DT sends and an out port for sorted-out (scrap) parts. */
/* <- Step SERVICE_QUALITY_CONTROL TASK : END -> */
/* <- Step SERVICE_QUALITY_CONTROL SOLUTION : START -> */
  port in QualityVerdict verdict;
  port out Part scrap;
/* <- Step SERVICE_QUALITY_CONTROL SOLUTION : END -> */

}
