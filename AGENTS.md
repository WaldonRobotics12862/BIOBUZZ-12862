# AGENTS.md — BIOBUZZ-12862 FTC Robot Controller

> **Season:** BIOBUZZ (2026–2027) · **SDK Version:** 12.0 · **Platform:** Android (FTC)

This document provides a structured overview of the project for AI agents, new contributors, and automated tools. It covers the repository layout, key source files, coding conventions, and how to add or modify robot code.

---

## Table of Contents

1. [Repository Overview](#repository-overview)
2. [Project Modules](#project-modules)
3. [Hardware Configuration](#hardware-configuration)
4. [TeamCode — Custom Robot Software](#teamcode--custom-robot-software)
   - [Package Structure](#package-structure)
   - [Entry Points](#entry-points)
   - [Pedro Pathing Integration](#pedro-pathing-integration)
   - [Tuning Procedures](#tuning-procedures)
   - [Test Procedures](#test-procedures)
5. [FtcRobotController Module](#ftcrobotcontroller-module)
6. [Build System](#build-system)
7. [Coding Conventions](#coding-conventions)
8. [Adding New Code](#adding-new-code)
9. [Key External Libraries](#key-external-libraries)
10. [GitHub Workflows](#github-workflows)

---

## Repository Overview

This is an Android Studio project for FIRST Tech Challenge (FTC) robot programming. The repository is a fork of the [official FTC SDK](https://github.com/FIRST-Tech-Challenge/FtcRobotController), extended with team-specific autonomous path-following code built on the **Pedro Pathing** library.

```
BIOBUZZ-12862/
├── FtcRobotController/          # Official SDK app module (do not modify)
├── TeamCode/                    # Team-specific robot code ← primary workspace
├── gradle/                      # Gradle wrapper configuration
├── libs/                        # Keystore and shared libraries
├── .github/                     # Workflows and contribution guidelines
├── README.md                    # SDK release notes and setup instructions
└── AGENTS.md                    # This file
```

---

## Project Modules

| Module | Purpose |
|---|---|
| `FtcRobotController` | SDK framework — OpMode runner, hardware abstraction, Driver Station communication |
| `TeamCode` | Team robot code — OpModes, tuners, hardware configs |

> **Rule:** Never modify files in `FtcRobotController/` unless explicitly required by an SDK upgrade. All robot logic belongs in `TeamCode/`.

---

## Hardware Configuration

The active robot configuration is defined in [`TeamCode/src/main/res/xml/biobuzz.xml`](TeamCode/src/main/res/xml/biobuzz.xml).

### Configured Hardware

| Hub | Device | Type | Port | Hardware Map Name |
|---|---|---|---|---|
| Control Hub | Front Left motor | goBILDA 5202 | 0 | `Front Left` |
| Control Hub | Front Right motor | goBILDA 5202 | 1 | `Front Right` |
| Control Hub | Back Right motor | goBILDA 5202 | 2 | `Back Right` |
| Control Hub | Back Left motor | goBILDA 5202 | 3 | `Back Left` |
| Expansion Hub 2 | Intake motor | goBILDA 5202 | 0 | `Intake` |
| Expansion Hub 2 | Transfer motor | goBILDA 5202 | 1 | `Transfer` |
| Expansion Hub 2 | Launcher motor | goBILDA 5202 | 2 | `Laucher` *(typo in config)* |
| Expansion Hub 2 | Launch Gate servo | Servo | 0 | `Launch Gate` |
| Expansion Hub 2 | Car Wash servo | CR Servo | 1 | `Car Wash` |

> **Commented out (currently disabled):** Color Sensor, HuskyLens, SparkFun OTOS

---

## TeamCode — Custom Robot Software

All team code lives under:

```
TeamCode/src/main/java/org/firstinspires/ftc/teamcode/
```

### Package Structure

```
teamcode/
└── pedro/
    ├── Constants.java          # Follower factory — wire your Drivetrain + Localizer here
    ├── Tuning.java             # Declares tuning Procedures exposed to the Pedro auto-tune UI
    └── procedures/
        ├── MecanumTuner.java       # Identify mecanum motor directions
        ├── ForesightTuner.java     # Full Foresight algorithm parameter identification
        ├── OctoQuadTuner.java      # OctoQuad (Digital Chicken Labs) localizer tuner
        ├── OTOSTuner.java          # SparkFun OTOS optical tracking sensor tuner
        ├── PinpointTuner.java      # goBILDA Pinpoint odometry tuner
        ├── ThreeWheelTuner.java    # Three-wheel dead-reckoning localizer tuner
        ├── ThreeWheelIMUTuner.java # Three-wheel + REV Hub IMU hybrid localizer tuner
        ├── TwoWheelTuner.java      # Two-wheel + IMU localizer tuner
        └── Tests.java              # Interactive follower and localization tests
```

### Entry Points

#### [`Constants.java`](TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/Constants.java)

The single place where the robot's `Follower` is constructed. Currently returns `null` — **this is the primary TODO** for teams to wire in their drivetrain and localizer.

```java
public static Follower create(HardwareMap h) {
    // TODO: return new Follower(Drivetrain, Localizer, Foresight);
    return null;
}
```

**When adding autonomous OpModes**, call `Constants.create(hardwareMap)` to obtain a configured `Follower`.

#### [`Tuning.java`](TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/Tuning.java)

Registers `Procedure` objects with the Pedro auto-tune framework via the `@Tuner` annotation. Currently only `MecanumTuner` is registered. Add additional tuners here as the robot is configured.

```java
@Tuner
public static Procedure mecanumTuner() {
    return new MecanumTuner();
}
```

---

### Pedro Pathing Integration

This project uses [Pedro Pathing](https://pedropathing.com) for autonomous path following. The key Pedro concepts used in this codebase:

| Concept | Class | Description |
|---|---|---|
| `Follower` | `com.pedropathing.follower.Follower` | Top-level path follower — owns Drivetrain + Localizer + Algorithm |
| `Drivetrain` | `com.pedropathing.drivetrain.Drivetrain` | Abstraction over mecanum/tank drive |
| `Localizer` | `com.pedropathing.localization.Localizer` | Pose estimation (odometry/sensor) |
| `Algorithm` | `com.pedropathing.algorithm.Algorithm` | Path correction algorithm (Foresight) |
| `Pose` | `com.pedropathing.math.Pose` | 2D pose: `(x, y, heading)` in inches/radians |
| `Path` | `com.pedropathing.paths.Path` | Parametric path (line or Bézier curve) |
| `Procedure` | `com.pedropathing.tuning.autotune.Procedure` | Auto-tune procedure base class |
| `TuningOpMode<T>` | `com.pedropathing.tuning.autotune.TuningOpMode` | OpMode executed as part of a Procedure |

**Coordinate system:** X = forward, Y = left (strafe), heading in radians (CCW positive).

---

### Tuning Procedures

All tuning procedures extend `Procedure` and are run through the Pedro auto-tune UI (accessible from the Driver Station). Each `Procedure` orchestrates one or more `TuningOpMode` sub-runs.

#### [`MecanumTuner`](TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/procedures/MecanumTuner.java)
- **Purpose:** Identifies the spin direction of each mecanum wheel motor.
- **Steps:** Spins each motor one at a time; operator confirms direction (Forward/Reversed).
- **Output:** Java code snippet for `MecanumConfig` with correct `DcMotorSimple.Direction` values.
- **Inner class:** `SpinMotor` (TuningOpMode) — drives a single motor at 50% power.

#### [`ForesightTuner`](TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/procedures/ForesightTuner.java)
- **Purpose:** Full system-identification for the Foresight path-following algorithm.
- **Constructor:** Requires `Function<HardwareMap, Localizer>` and `Function<HardwareMap, Drivetrain>`.
- **Identified parameters:**
  - `maxAchievableForwardVelocity` / `maxAchievableStrafeVelocity`
  - `naturalForwardDeceleration` / `naturalStrafeDeceleration`
  - Heading braking coefficients (linear + quadratic)
  - Heading kP (via first-order system ID)
  - Forward/strafe braking coefficients (linear + quadratic)
  - Translational PID gains (primary + secondary, forward + strafe)
  - Feedforward coast/brake gains
- **Inner TuningOpModes:** `ForwardVelocity`, `StrafeVelocity`, `ForwardDeceleration`, `StrafeDeceleration`, `HeadingBraking`, `HeadingTuner`, `ForwardBraking`, `StrafeBraking`, `ForwardTranslational`, `StrafeTranslational`.
- **Output:** Java code snippet for `ForesightConfig`.

#### [`OctoQuadTuner`](TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/procedures/OctoQuadTuner.java)
- **Purpose:** Tunes the OctoQuad (Digital Chicken Labs) quadrature encoder interface for odometry.
- **Pod types:** `SWING_ARM` (336.88 ticks/unit), `FOUR_BAR` (505.32 ticks/unit), `CUSTOM`.
- **Steps:** Heading scalar → (optional) custom pod scalar → forward direction → strafe direction → offsets.
- **Output:** Java code snippet for `OctoQuadConfig`.

#### [`OTOSTuner`](TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/procedures/OTOSTuner.java)
- **Purpose:** Tunes the SparkFun Optical Tracking Odometry Sensor (OTOS).
- **Steps:** Angular scalar identification → linear scalar identification → X/Y offset identification.
- **Output:** Java code snippet for `OTOSConfig`.

#### [`PinpointTuner`](TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/procedures/PinpointTuner.java)
- **Purpose:** Tunes the goBILDA Pinpoint odometry computer.
- **Pod types:** `SWING_ARM`, `FOUR_BAR`, `CUSTOM`.
- **Steps:** (optional) custom pod scalar → forward direction → strafe direction → offsets.
- **Output:** Java code snippet for `PinpointConfig`.

#### [`ThreeWheelTuner`](TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/procedures/ThreeWheelTuner.java)
- **Purpose:** Tunes a pure three-wheel dead-reckoning localizer (no IMU for heading).
- **Steps:** Encoder resolution (left, right, strafe) → pod offsets (left, right) → turn multiplier.
- **Default encoder names:** `lf` (left), `rr` (right), `lr` (strafe).
- **Output:** Java code snippet for `ThreeWheelConfig`.

#### [`ThreeWheelIMUTuner`](TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/procedures/ThreeWheelIMUTuner.java)
- **Purpose:** Tunes a three-wheel + REV Hub IMU hybrid localizer.
- **Additional inputs:** IMU hardware map name, Control Hub logo/USB facing directions.
- **Steps:** Same as `ThreeWheelTuner` + IMU orientation configuration.
- **Output:** Java code snippet for `ThreeWheelIMUConfig`.

#### [`TwoWheelTuner`](TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/procedures/TwoWheelTuner.java)
- **Purpose:** Tunes a two-wheel (forward + strafe) + IMU localizer.
- **Steps:** Forward resolution → strafe resolution → forward direction → strafe direction → offsets.
- **Output:** Java code snippet for `TwoWheelConfig`.

---

### Test Procedures

#### [`Tests`](TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/procedures/Tests.java)

A single `Procedure` that presents a menu of interactive robot tests. It requires `Drivetrain`, `Localizer`, and `Algorithm` (via supplier) to be passed in.

| Test Name | Requires | Description |
|---|---|---|
| **Hold Test** | Follower (all 3) | Robot holds position at origin |
| **Line Test** | Follower | Robot follows a 48-inch forward/back line loop |
| **Curve Test** | Follower | Robot follows a 48-inch Bézier curve loop |
| **Interpolation Test** | Follower | Curve with multiple heading interpolation modes |
| **Localization Test** | Drivetrain + Localizer | Manual drive with live pose telemetry |
| **Odometry Test** | Drivetrain + Localizer | Auto forward/strafe/turn, validates pod directions & resolution |
| **Driving Test** | Drivetrain only | Raw gamepad control, no localization |
| **Pose Test** | Localizer only | Displays live localizer pose without driving |

**Odometry Test** automatically diagnoses common issues (reversed pods, wrong resolution) and returns `false` (failing the procedure) if any axis fails.

---

## FtcRobotController Module

Located at `FtcRobotController/`. **Do not modify these files.**

Key internal classes (for reference only):

| Class | Path | Purpose |
|---|---|---|
| `FtcRobotControllerActivity` | `internal/` | Main Android Activity — boots the robot |
| `FtcOpModeRegister` | `internal/` | Registers OpModes visible to the Driver Station |
| `PermissionValidatorWrapper` | `internal/` | Handles Android runtime permissions |

**Sample OpModes** are in `FtcRobotController/src/main/java/.../external/samples/`. Copy samples into `TeamCode/` before modifying them; do not edit them in place.

---

## Build System

| File | Description |
|---|---|
| `gradle/wrapper/gradle-wrapper.properties` | Gradle version (9.1) |
| `gradle/gradle-daemon-jvm.properties` | Daemon JVM settings |
| `TeamCode/build.gradle` | TeamCode module build config (depends on SDK AAR files in `libs/`) |

**Android Gradle Plugin:** 8.13.2 — requires **Android Studio Narwhal 3 Feature Drop** or later.

**Java version:** Source/target compatibility is Java 1.8 (for OnBot Java compatibility).

To build: open the project in Android Studio and use **Build → Make Project** or deploy directly to the Control Hub via USB.

---

## Coding Conventions

### OpMode Annotations

```java
@TeleOp(name = "My TeleOp", group = "Competition")
@Autonomous(name = "My Auto", group = "Competition")
@Utility(name = "My Utility")   // Shows in Utility menu (SDK 11.2+)
@Disabled                        // Hides from Driver Station
```

### OpMode Types

| Base Class | Style | Use Case |
|---|---|---|
| `LinearOpMode` | Sequential, blocking | Autonomous routines, complex initialization |
| `OpMode` | Iterative (`init()`, `loop()`) | TeleOp, background tasks |

### Naming Conventions (from FTC SDK)

OpMode class names follow the pattern `{Prefix}{Company}{Type}`:
- `Basic*` — skeleton/structure demos
- `Sensor*` — single sensor demonstrations
- `Robot*` — two-motor differential drive examples
- `Concept*` — specific concept demonstrations

### Pedro Pathing Patterns

When writing autonomous OpModes with Pedro:

```java
// 1. Create follower
Follower follower = Constants.create(hardwareMap);

// 2. Set starting pose
follower.setPose(new Pose(startX, startY, startHeading));

// 3. Build a path
Path path = line(Pose.zero(), new Pose(48, 0, 0)).constant(0);

// 4. Follow it
follower.follow(path);

// 5. Update in loop
while (opModeIsActive()) {
    follower.update();
    if (follower.atParametricEnd()) { /* ... */ }
}
```

### Tuning Procedure Patterns

To add a new `Procedure` to the auto-tune UI:

1. Create a class extending `Procedure` in `pedro/procedures/`.
2. Implement `run() throws InterruptedException`.
3. Use `inputs(...)`, `awaitInputs(...)`, `runOpMode(...)`, `result(...)`, `code(...)`, `abort(...)`.
4. Register it with `@Tuner` in [`Tuning.java`](TeamCode/src/main/java/org/firstinspires/ftc/teamcode/pedro/Tuning.java).

---

## Adding New Code

### New TeleOp or Autonomous OpMode

1. Create a `.java` file in `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/`.
2. Annotate with `@TeleOp` or `@Autonomous` (remove `@Disabled` to make it visible).
3. Extend `LinearOpMode` or `OpMode`.
4. Use `Constants.create(hardwareMap)` for path following, or access hardware directly via `hardwareMap`.

### New Localizer

1. Implement the Pedro `Localizer` interface.
2. Create a corresponding `Procedure` subclass (following the pattern of existing tuners).
3. Wire it into `Constants.java`.

### New Mechanism / Subsystem

1. Create a helper class in `teamcode/` (e.g., `teamcode/subsystems/Intake.java`).
2. Instantiate it in your OpMode's `init()` using the `hardwareMap`.
3. Reference hardware map names from [`biobuzz.xml`](TeamCode/src/main/res/xml/biobuzz.xml).

---

## Key External Libraries

All SDK libraries are pre-built `.aar` files referenced from Maven/local. Pedro Pathing classes are accessed via:

| Package | Contents |
|---|---|
| `com.pedropathing.follower` | `Follower` |
| `com.pedropathing.drivetrain` | `Drivetrain`, `DrivePowers` |
| `com.pedropathing.localization` | `Localizer` interface |
| `com.pedropathing.math` | `Pose`, `Vector2D`, `Matrix` |
| `com.pedropathing.paths` | `Path`, `Interpolator` |
| `com.pedropathing.algorithm` | `Algorithm` (Foresight) |
| `com.pedropathing.tuning.autotune` | `Procedure`, `TuningOpMode`, `Inputs`, `@Tuner` |
| `com.pedropathing.revhub.localizers` | `ThreeWheelLocalizer`, `TwoWheelLocalizer`, `OctoQuadLocalizer`, `OTOSLocalizer`, `PinpointLocalizer`, and their configs |
| `com.qualcomm.robotcore.hardware` | `DcMotor`, `Servo`, `HardwareMap`, `IMU`, etc. |
| `com.qualcomm.hardware.gobilda` | `GoBildaPinpointDriver` |
| `com.qualcomm.hardware.digitalchickenlabs` | `OctoQuad` |

---

## GitHub Workflows

Located in [`.github/workflows/`](.github/workflows/):

| Workflow | Trigger | Purpose |
|---|---|---|
| `disable-auto-reopen-after-second-close.yml` | Issue closed | Prevents auto-reopening issues closed a second time |
| `mistake-pr.yml` | Pull request opened | Warns contributors who accidentally PR against upstream |
| `reopen-once.yml` | Issue closed | Allows a single auto-reopen after first close |

See [`.github/CONTRIBUTING.md`](.github/CONTRIBUTING.md) for contribution guidelines. Note: most PRs from team forks are **not** intended to be merged upstream — team code is team-specific.

---

*Last updated: October 2026 · Generated by scanning BIOBUZZ-12862 source code.*
