package frc.robot.Drivetrain;

import edu.wpi.first.wpilibj.DoubleSolenoid.Value;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.Robot;

//import static frc.robot.Drivetrain.DrivetrainSubsystem.motors;

public class CurvatureDrive extends Command {
    private static double right, left;
    private double driveRamp = 0, quickturnRamp = 0;

    private static final double kJoystickDeadband = 0.05;

    /**
     * Left: 21.51 ft/s, 6.559 m/s, equation: 582x-796, intercept at 1.3677v
     * Right 20.51 ft/s, 6.253 m/s, equation: 553x-736, intercept at 1.3309v
     * Average: 21.02 ft/s, 6.406 m/s
     */
    public static final double kLinterceptHigh = 0.0;//1.3677; 
    public static final double kRinterceptHigh = 0.0;//1.3309;

    /**
     * Left: 10.22 ft/s, 3.11 m/s, equation: 265x-242, intercept at 0.913v
     * Right: 9.67 ft/s, 2.94 m/s, equation: 251x-231, intercept at 0.920v
     * Average: 9.94ft/s, 3.025 m/s
     */
    public static final double kLinterceptLow = 0; //0.968
    public static final double kRinterceptLow = 0; //1.058

    public CurvatureDrive(){
        requires(DrivetrainSubsystem.getInstance());
    }

    @Override
    protected boolean isFinished() {
        return true;
    }
    
    protected void execute(){

        SmartDashboard.putBoolean("Photoelectric", Robot.oi.getPhotoelectricA());
        
        //Getting the raw joystick values from OI
        double throttle = Robot.oi.throttleValue();
        double turn = Robot.oi.turnValue();
        
        //Deadbanding the joystick values to avoid movement due to controller drift
        throttle = deadbandX(throttle , kJoystickDeadband);
        turn = deadbandX(turn, kJoystickDeadband);
        
        //Handles quickturning
        boolean quickturn = Robot.oi.isQuickturn();
        double qLeft = Robot.oi.getLeftTrigger();
        double qRight = Robot.oi.getRightTrigger();
        
        if(!quickturn){
            DrivetrainSubsystem.setOpenLoopRamp(driveRamp);

            left = throttle+throttle*turn + 0.5*(-qLeft+qRight);
            right = throttle-throttle*turn + 0.5*(-qRight+qLeft);

            left = exponentiate(left, 2);
            right = exponentiate(right, 2);

        } else {
            DrivetrainSubsystem.setOpenLoopRamp(quickturnRamp);

            left = 0.5*(-qLeft+qRight);
            right = 0.5*(-qRight+qLeft);
        }

        DrivetrainSubsystem.drive(left, right);
        
}


    /**
     * Essentially an implementation of the slope formula, m = (y1-y2)/(x1-x2) = (1 - 0)/(1 - deadband)
     * Uses this slope and multiplies it by the input to deadband a controller from deadband to 1
     * 
     * @param input      an input value to test for deadband
     * @param deadband   deadband value to check the input against
     * @return           returns a double rescaled to fit a deadband
     */
    public static double deadbandX(double input, double deadband){
        if(Math.abs(input) <= deadband){
            return 0;
        } else if(Math.abs(input)==1) {
            return input;
        } else {
            return (1/(1-deadband)*(input+Math.signum(-input)*deadband));
        }
    }

    /**
     * Used to achieve a higher degree of sensitivity when driving
     * Smaller inputs are smaller, larger inputs are larger, without exceeding maximum
     * 
     * @param input   number to put to a power
     * @param power   number to exponentiate input by (not necessarily an integer)
     * @return        returns a double, squared, with the same sign as input
     *     
     */
    public static double exponentiate(double input, double power){
        return Math.copySign(Math.pow(input, power),input);
    }

    /**
     * Essentially deadbandX but in the y direction
     * m = (y1-y2)/(x1-x2) = (1-deadband)(1-0)
     * 
     * @param input      input value to test for deadband 
     * @param deadband   determines values where method returns 0
     * @return           returns a double rescaled according to deadband
     */
    public static double deadbandY(double input, double deadband){
        if(Math.abs(input) == 0.0){
            return 0;
        } else if(Math.abs(input) == 1){
            return input;
        } else {
            return input*(1.0-deadband)+Math.signum(input)*deadband;
        }
    }

}