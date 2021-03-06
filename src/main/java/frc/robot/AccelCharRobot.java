package frc.robot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.StatusFrameEnhanced;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;

import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.command.Scheduler;

/*
* This is a robot class that can be used to characterize the
* acceleration of a robot, creating a csv dataset under home/lvuser/dtchar
*/

//NOTE: If the folder home/lvuser/dtchar does not exist, it will crash
public class AccelCharRobot extends TimedRobot {
  private static final TalonSRX leftMotorA = new TalonSRX(3), leftMotorB = new TalonSRX(4),
      rightMotorA = new TalonSRX(2), rightMotorB = new TalonSRX(1);

  private static final TalonSRX[] motors = { leftMotorA, leftMotorB, rightMotorB, rightMotorA };
  private static final TalonSRX[] leftMotors = { leftMotorA, leftMotorB };
  private static final TalonSRX[] rightMotors = { rightMotorA, rightMotorB };

  private PrintWriter csvWriter = null;
  private Joystick joystick = new Joystick(0);
  public double appliedOutput;

  @Override
  public void robotInit() {
    // Setting master and follower talons
    leftMotorB.follow(leftMotorA);
    rightMotorB.follow(rightMotorA);

    // Negation settings
    Arrays.stream(leftMotors).forEach(motor -> motor.setInverted(true));
    Arrays.stream(rightMotors).forEach(motor -> motor.setInverted(false));

    for (TalonSRX motor : motors) {
      // Current and voltage settings
      motor.configPeakCurrentLimit(30, 10);
      motor.configPeakCurrentDuration(500, 10);
      motor.configContinuousCurrentLimit(35, 10);
      motor.configVoltageCompSaturation(12, 10);
      motor.enableVoltageCompensation(true);
      motor.enableCurrentLimit(true);
    }

    // Left drivetrain encoder
    leftMotorA.setStatusFramePeriod(StatusFrameEnhanced.Status_3_Quadrature, 1, 10);
    leftMotorA.configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 10);
    leftMotorA.setSensorPhase(true);

    // Right drivetrain encoder
    rightMotorA.setStatusFramePeriod(StatusFrameEnhanced.Status_3_Quadrature, 1, 10);
    rightMotorA.configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, 0, 10);
    rightMotorA.setSensorPhase(false);

  }

  @Override
  public void robotPeriodic() {
    if (!isOperatorControl() && csvWriter != null) {
      csvWriter.close();
      csvWriter = null;
    }
    Scheduler.getInstance().run();
  }

  public void teleopPeriodic() {
    if (joystick.getRawButton(1)) { // if button A is pressed
      if (csvWriter == null) {
        // create a new CSV writer, reset everything
        try {
          csvWriter = new PrintWriter(
              new File("/home/lvuser/dtmeasure/measure_acceleration-" + System.currentTimeMillis() + ".csv"));
          csvWriter.println("lvoltage,lvelocity,rvoltage,rvelocity");
        } catch (FileNotFoundException e) {
          throw new RuntimeException(e);
        }

        appliedOutput = 0;
        leftMotorA.set(ControlMode.PercentOutput, 0);
        rightMotorA.set(ControlMode.PercentOutput, 0);

      } else {
        //If PrintWriter exists, add data to csv and set voltage
        csvWriter.println(leftMotorA.getMotorOutputVoltage() + "," + leftMotorA.getSelectedSensorVelocity(0) + ","
            + rightMotorA.getMotorOutputVoltage() + "," + rightMotorA.getSelectedSensorVelocity(0));
        appliedOutput = 0.5; // Applies 6V
        leftMotorA.set(ControlMode.PercentOutput, appliedOutput);
        rightMotorA.set(ControlMode.PercentOutput, appliedOutput);
      }
    } else { //Resets robot movement to 0, closes PrintWriter if it is open to prepare for a new iteration
      appliedOutput = 0;
      if (csvWriter != null) {
        csvWriter.close();
        csvWriter = null;
      }
      leftMotorA.set(ControlMode.PercentOutput, 0);
      rightMotorA.set(ControlMode.PercentOutput, 0);
    }

  }

  private static double toFeet(double ticksPerDecisecond) {
    return (ticksPerDecisecond / 4517.0) * 6 * Math.PI * 10;
  }
}