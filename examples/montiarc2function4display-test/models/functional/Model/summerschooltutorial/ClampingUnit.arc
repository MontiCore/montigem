/* <- Step CPS_MODELING FILE -> */
package summerschooltutorial;

import summerschooltutorial.IMMTypes.Part;  //<- CPS_MODELING SOLUTION ->

component ClampingUnit {

/* <- Step CPS_MODELING TASK : START -> */
  /* TODO: add the attributes holdingPressure, clampPos, and meltTemp (double, with
     meaningful default values; see the worksheet). */
/* <- Step CPS_MODELING TASK : END -> */
/* <- Step CPS_MODELING SOLUTION : START -> */
  double holdingPressure = 0.0; // bar
  double clampPos = 0.0; // mm
  double meltTemp = 0.0; // °C
/* <- Step CPS_MODELING SOLUTION : END -> */

/* <- Step CPS_MODELING TASK : START -> */
  /* TODO: define the ports: in melt, in holdingPressureSetpoint, out part (see the
     worksheet for types and units). */
/* <- Step CPS_MODELING TASK : END -> */

/* <- Step CPS_MODELING SOLUTION : START -> */
  port in double melt; // g
  port in double holdingPressureSetpoint; // bar, packing pressure requested by the DT
  port out Part part;
/* <- Step CPS_MODELING SOLUTION : END -> */

}
