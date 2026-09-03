/* <- Step CPS_MODELING FILE -> */
package summerschooltutorial;

import summerschooltutorial.InjectionUnit;
import summerschooltutorial.IMMTypes.*;
//~ import summerschooltutorial.CpsModelingBypass;  //<- CPS_MODELING PLACEHOLDER ->
import summerschooltutorial.ClampingUnit;           //<- CPS_MODELING SOLUTION ->
import summerschooltutorial.QualityInspector;       //<- QUALITY_INSPECTOR SOLUTION ->

component IMM {
  port in double granule_in, // in gram
       // actuator inputs: setpoints the digital twin sends to the physical machine
       in double nozzleTempSetpoint_in, // °C
       in double holdingPressureSetpoint_in; // bar
  // the finished parts leave the machine at its boundary
  port out Part part_out;

/* <- Step SERVICE_QUALITY_CONTROL TASK : START -> */
  /* TODO: add the DT-facing quality ports at the machine boundary — an actuator in-port
     for the QualityVerdict and an out-port for scrap parts — and connect them to the
     quality inspector below. */
/* <- Step SERVICE_QUALITY_CONTROL TASK : END -> */
/* <- Step SERVICE_QUALITY_CONTROL SOLUTION : START -> */
  port in QualityVerdict qualityVerdict_in,
       out Part scrap_out;
/* <- Step SERVICE_QUALITY_CONTROL SOLUTION : END -> */

  /* TODO: include the clamping unit into the top-level component by adding it as a sub-component. */  //<- CPS_MODELING TASK ->
  // sub-components
  InjectionUnit injectionUnit;
//~   CpsModelingBypass tutorialBypass;                                            //<- CPS_MODELING PLACEHOLDER ->
  ClampingUnit clampingUnit;                                                       //<- CPS_MODELING SOLUTION ->
  /* TODO: instantiate your QualityInspector next to the other sub-components. */  //<- QUALITY_INSPECTOR TASK ->
  QualityInspector qualityInspector;                                               //<- QUALITY_INSPECTOR SOLUTION ->

  /* TODO: connect the ports appropriately. */  //<- CPS_MODELING TASK ->
  // connectors
  granule_in -> injectionUnit.granule;
  nozzleTempSetpoint_in -> injectionUnit.nozzleTempSetpoint;
/* <- Step CPS_MODELING PLACEHOLDER : START -> */
//~   holdingPressureSetpoint_in -> tutorialBypass.holdingPressureSetpoint;
//~   injectionUnit.melt -> tutorialBypass.melt;
//~   tutorialBypass.part -> part_out;
/* <- Step CPS_MODELING PLACEHOLDER : END -> */
/* <- Step CPS_MODELING SOLUTION : START -> */
  holdingPressureSetpoint_in -> clampingUnit.holdingPressureSetpoint;
  injectionUnit.melt -> clampingUnit.melt;
/* <- Step CPS_MODELING SOLUTION : END -> */
//~   clampingUnit.part -> part_out;  //<- CPS_MODELING SOLUTION UNTIL QUALITY_INSPECTOR ->
/* <- Step QUALITY_INSPECTOR TASK : START -> */
  /* TODO: slot the inspector into the existing part flow — replace the direct
     connector from the clamping unit's part output to part_out by routing every
     produced part into the inspector's prodPart input and its checked parts on to
     part_out. */
/* <- Step QUALITY_INSPECTOR TASK : END -> */
/* <- Step QUALITY_INSPECTOR SOLUTION : START -> */
  clampingUnit.part -> qualityInspector.prodPart;
  qualityInspector.checkedPart -> part_out;
/* <- Step QUALITY_INSPECTOR SOLUTION : END -> */
/* <- Step SERVICE_QUALITY_CONTROL SOLUTION : START -> */
  qualityVerdict_in -> qualityInspector.verdict;
  qualityInspector.scrap -> scrap_out;
/* <- Step SERVICE_QUALITY_CONTROL SOLUTION : END -> */

}
