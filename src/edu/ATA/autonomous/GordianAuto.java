package edu.ATA.autonomous;

import edu.ATA.commands.AlignCommand;
import edu.ATA.commands.AlignShooter;
import edu.ATA.commands.ArcadeDriveCommand;
import edu.ATA.commands.AutoShoot;
import edu.ATA.commands.BangBangCommand;
import edu.ATA.commands.DriveDistance;
import edu.ATA.commands.GearShift;
import edu.ATA.commands.ResetAngleCommand;
import edu.ATA.commands.ResetEncoderCommand;
import edu.ATA.commands.ShootCommand;
import edu.ATA.commands.TankDriveCommand;
import edu.ATA.commands.TurnToAngle;
import edu.ATA.twolf.subsystems.AlignmentSystem;
import edu.ATA.twolf.subsystems.ShiftingDrivetrain;
import edu.ATA.twolf.subsystems.Shooter;
import edu.first.commands.LogCommand;
import edu.first.commands.PauseCommand;
import edu.first.module.sensor.EncoderModule;
import edu.first.module.sensor.GyroModule;
import edu.first.module.target.BangBangModule;
import edu.first.module.target.PIDModule;
import edu.first.utils.DriverstationInfo;
import edu.first.utils.Logger;
import edu.gordian.Gordian;
import edu.gordian.Variable;
import edu.gordian.method.BooleanReturningMethod;
import edu.gordian.method.NumberReturningMethod;
import edu.gordian.method.RunningMethod;
import edu.gordian.variable.NumberInterface;
import java.io.IOException;
import javax.microedition.io.Connector;

/**
 * Static class meant to keep Gordian in a state where it can run the current
 * script. To make sure the script uses all of the methods given, the
 * {@link Gordian#ensureInit()} method makes sure that all storage of methods
 * and variables are stored. {@code ensureInit()} is called every time
 * {@link Gordian#run(java.lang.String)} is called, so you usually don't have to
 * worry about it.
 *
 * <p> In almost every case, you should run gordian script using
 * {@link Gordian#run(java.lang.String)}. Running
 * {@link Script#run(java.lang.String)} will not automatically include all
 * methods that you have made, and will most likely not be useful for any
 * practical applications other than basic logic and delays.
 *
 * @author Joel Gallant <joelgallant236@gmail.com>
 */
public final class GordianAuto {

    private static boolean init = false;
    private static Gordian gordian;
    private static ShiftingDrivetrain drivetrain;
    private static Shooter shooter;
    private static BangBangModule bangBangModule;
    private static AlignmentSystem alignmentSystem;
    private static PIDModule drivetrainPID;
    private static EncoderModule encoder;
    private static GyroModule gyro;

    private GordianAuto() throws IllegalAccessException {
        throw new IllegalAccessException();
    }

    /**
     * Makes sure that everything is ready to be run in Gordian. This includes
     * methods, variables, etc. You can generally accept that everything is
     * ready to be run after running this method.
     *
     * @param drivetrain the drivetrain you are using
     * @param shooter the shooter you are using
     * @param bangBangModule the bang-bang module that you are using
     * @param alignmentSystem that alignment system that your are using
     */
    public static void ensureInit(ShiftingDrivetrain drivetrain, Shooter shooter,
            BangBangModule bangBangModule, AlignmentSystem alignmentSystem, PIDModule drivetrainPID,
            EncoderModule encoder, GyroModule gyro) {
        if (!init) {
            GordianAuto.drivetrain = drivetrain;
            GordianAuto.shooter = shooter;
            GordianAuto.bangBangModule = bangBangModule;
            GordianAuto.alignmentSystem = alignmentSystem;
            GordianAuto.drivetrainPID = drivetrainPID;
            GordianAuto.encoder = encoder;
            GordianAuto.gyro = gyro;
            init = true;
        }
    }

    /**
     * Accesses the file at {@code "file:///"+fileName}, and uses the text found
     * there as the script.
     *
     * @param fileName name of the file to retrieve text from
     * @throws IOException thrown when accessing file fails
     */
    public static void run(String fileName) throws IOException {
        String script = Logger.getTextFromFile(Connector.openDataInputStream("file:///" + fileName));
        gordian = new Gordian(script);
        init();
        gordian.run();
    }

    private static void init() {
        // Insert all methods, variables, returning methods and initialization code here.
        gordian.addMethod(new NumberReturningMethod("encoderDistance") {
            public double getDouble() {
                return encoder.getDistance();
            }
        });
        gordian.addMethod(new BooleanReturningMethod("isPastSetpoint") {
            public boolean getBoolean() {
                return bangBangModule.pastSetpoint();
            }
        });
        gordian.addMethod(new NumberReturningMethod("gyro") {
            public double getDouble() {
                return gyro.getAngle();
            }
        });
        gordian.addMethod(new BooleanReturningMethod("isEnabled") {
            public boolean getBoolean() {
                return DriverstationInfo.isEnabled();
            }
        });
        gordian.addMethod(new RunningMethod("print") {
            public void run(Variable[] args) {
                System.out.println(args[0].getValue());
            }
        });
        gordian.addMethod(new RunningMethod("wait") {
            public void run(Variable[] args) {
                new PauseCommand(((NumberInterface) args[0]).doubleValue()).run();
            }
        });
        gordian.addMethod(new RunningMethod("log") {
            public void run(Variable[] args) {
                new LogCommand(args[0].getValue().toString()).run();
            }
        });
        gordian.addMethod(new RunningMethod("arcade") {
            public void run(Variable[] args) {
                new ArcadeDriveCommand(drivetrain, ((NumberInterface) args[0]).doubleValue(),
                        ((NumberInterface) args[1]).doubleValue(), false).run();
            }
        });
        gordian.addMethod(new RunningMethod("tank") {
            public void run(Variable[] args) {
                new TankDriveCommand(drivetrain, ((NumberInterface) args[0]).doubleValue(),
                        ((NumberInterface) args[1]).doubleValue(), false).run();
            }
        });
        gordian.addMethod(new RunningMethod("stop") {
            public void run(Variable[] args) {
                new TankDriveCommand(drivetrain, 0, 0, false).run();
            }
        });
        gordian.addMethod(new RunningMethod("shiftGear") {
            public void run(Variable[] args) {
                new GearShift(drivetrain, false).run();
            }
        });
        gordian.addMethod(new RunningMethod("driveToSetpoint") {
            public void run(Variable[] args) {
                double setpoint = ((NumberInterface) args[0]).doubleValue();
                Logger.log(Logger.Urgency.USERMESSAGE, "Driving to " + setpoint);
                new DriveDistance(encoder, drivetrainPID, setpoint).run();
            }
        });
        gordian.addMethod(new RunningMethod("gyroTurn") {
            public void run(Variable[] args) {
                double setpoint = ((NumberInterface) args[0]).doubleValue();
                double lspeed = ((NumberInterface) args[1]).doubleValue();
                double rspeed = ((NumberInterface) args[2]).doubleValue();
                Logger.log(Logger.Urgency.USERMESSAGE, "Turning to " + setpoint);
                new TurnToAngle(lspeed, rspeed, setpoint, gyro, drivetrain, false).run();
            }
        });
        gordian.addMethod(new RunningMethod("autoShoot") {
            public void run(Variable[] args) {
                new AutoShoot(shooter, bangBangModule, false).run();
            }
        });
        gordian.addMethod(new RunningMethod("shoot") {
            public void run(Variable[] args) {
                new ShootCommand(shooter, false).run();
            }
        });
        gordian.addMethod(new RunningMethod("alignShooter") {
            public void run(Variable[] args) {
                new AlignShooter(shooter, ((NumberInterface) args[0]).doubleValue(), false).run();
            }
        });
        gordian.addMethod(new RunningMethod("setShooterSpeed") {
            public void run(Variable[] args) {
                double speed = ((NumberInterface) args[0]).doubleValue();
                Logger.log(Logger.Urgency.USERMESSAGE, "Setting shooter to " + speed);
                new BangBangCommand(bangBangModule, speed, false).run();
            }
        });
        gordian.addMethod(new RunningMethod("stopShooter") {
            public void run(Variable[] args) {
                new BangBangCommand(bangBangModule, 0, false).run();
            }
        });
        gordian.addMethod(new RunningMethod("resetAngle") {
            public void run(Variable[] args) {
                new ResetAngleCommand(gyro, false).run();
            }
        });
        gordian.addMethod(new RunningMethod("resetDistance") {
            public void run(Variable[] args) {
                new ResetEncoderCommand(encoder, false).run();
            }
        });
        gordian.addMethod(new RunningMethod("collapseAlignment") {
            public void run(Variable[] args) {
                new AlignCommand(alignmentSystem, AlignCommand.COLLAPSE, false).run();
            }
        });
        gordian.addMethod(new RunningMethod("extendAlignment") {
            public void run(Variable[] args) {
                new AlignCommand(alignmentSystem, AlignCommand.EXTEND, false).run();
            }
        });
    }
}
