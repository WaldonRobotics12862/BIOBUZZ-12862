package org.firstinspires.ftc.teamcode.pedro;

import com.pedropathing.algorithm.Foresight;
import com.pedropathing.algorithm.ForesightConfig;
import com.pedropathing.controllers.Controller;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Matrix;
import com.pedropathing.math.Pose;
import com.pedropathing.math.Vector2D;
import com.pedropathing.revhub.drivetrains.Mecanum;
import com.pedropathing.revhub.drivetrains.MecanumConfig;
import com.pedropathing.revhub.localizers.OTOSConfig;
import com.pedropathing.revhub.localizers.OTOSLocalizer;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

public class Constants {
    public static MecanumConfig drivetrainConfig = new MecanumConfig(c -> {
        c.frontLeftName.set("Front Left");
        c.frontRightName.set("Front Right");
        c.backLeftName.set("Back Left");
        c.backRightName.set("Back Right");
        c.frontLeftDirection.set(DcMotorSimple.Direction.FORWARD);
        c.frontRightDirection.set(DcMotorSimple.Direction.FORWARD);
        c.backLeftDirection.set(DcMotorSimple.Direction.FORWARD);
        c.backRightDirection.set(DcMotorSimple.Direction.FORWARD);
    });
    public static OTOSConfig localizerConfig = new OTOSConfig(c -> {
        c.name.set("otos");
        c.linearScalar.set(0.992022884386174);
        c.angularScalar.set(0.9866957075966472);
        //c.offset.set(new Pose(0.0, -0.1441775344488189));
        c.offset.set(new Pose(0.0, -0.0));
        c.linearUnit.set(DistanceUnit.INCH);
    });

    public static ForesightConfig foresightConfig = new ForesightConfig(
            c -> {
                Controller primaryTranslationalForward = Controller.proportional(0.4360013462957814);
                Controller secondaryTranslationalForward = Controller.proportional(0.16109084728575396);
                Controller primaryTranslationalLateral = Controller.proportional(0.8688513517226453);
                Controller secondaryTranslationalLateral = Controller.proportional(0.32101735832582173);

                c.forwardTranslational.set(Controller.piecewise(secondaryTranslationalForward).put(2.5, primaryTranslationalForward));
                c.strafeTranslational.set(Controller.piecewise(secondaryTranslationalLateral).put(2.5, primaryTranslationalLateral));

                c.coast.set(Controller.proportionalFeedforward(0.015327191385132572));
                c.brake.set(Controller.proportionalFeedforward(0.013028112677362686));

                c.headingFeedback.set(Controller.proportional(8.647565292131755));
                c.headingBrakeCoefficients.set(Vector2D.cartesian(0.19870850147429478, -0.038035824681711385));

//                c.linearBrakeCoefficients.set(Matrix.diag(2.925008661001698, 0.016442271829659138));
                c.linearBrakeCoefficients.set(Matrix.diag(0.01, 0.016442271829659138));
                c.quadraticBrakeCoefficients.set(Matrix.diag(-0.04480052413850141, 0.0025788382070531302));

                c.maxAchievableForwardVelocity.set(62.58536295500811);
                c.maxAchievableStrafeVelocity.set(44.30296688644072);
                c.naturalForwardDeceleration.set(27.58509084252027);
                c.naturalStrafeDeceleration.set(65.27423882150062);
            }
    );
    public static Follower create(HardwareMap h) {
        return new Follower(
                new OTOSLocalizer(h, localizerConfig),
                new Mecanum(h, drivetrainConfig),
                new Foresight(foresightConfig)
        );
    }


}