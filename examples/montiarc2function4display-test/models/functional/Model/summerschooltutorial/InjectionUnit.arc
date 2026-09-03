package summerschooltutorial;

component InjectionUnit {

  // attributes
  double injectionPressure = 20.0; // bar
  double backPressure = 20.0; // bar
  double barrel1Temp = 20.0; // °C
  double barrel2Temp = 20.0; // °C
  double nozzleTemp = 20.0; // °C
  double heaterPower = 0.0; // kW
  double screwVelocity = 0.0; // cm³/s

  // ports
  port in double granule; // g
  port in double nozzleTempSetpoint; // °C, target operating temperature requested by the DT
  port out double melt; // g

}
