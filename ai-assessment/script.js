const video = document.getElementById("video");
const canvas = document.getElementById("canvas");
const ctx = canvas.getContext("2d");

const repCountElement = document.getElementById("repCount");
const positionElement = document.getElementById("position");
const feedbackElement = document.getElementById("feedback");
const detectionElement = document.getElementById("detection");
const assessmentStatus = document.getElementById("assessmentStatus");
const connectionStatus = document.getElementById("connectionStatus");
const cameraMessage = document.getElementById("cameraMessage");

const startButton = document.getElementById("startButton");
const resetButton = document.getElementById("resetButton");


// ======================================================
// SETTINGS
// ======================================================

const SETTINGS = {

    // Elbow angle
    UP_ANGLE: 155,
    DOWN_ANGLE: 100,

    // Body must be reasonably horizontal
    MAX_BODY_VERTICAL_RATIO: 0.38,

    // Minimum shoulder movement required
    MIN_SHOULDER_MOVEMENT: 0.035,

    // Required visibility
    MIN_VISIBILITY: 0.55,

    // Frames required before accepting state
    REQUIRED_FRAMES: 3,

    // Prevent instant double counting
    COOLDOWN_FRAMES: 12

};


// ======================================================
// STATE
// ======================================================

let reps = 0;

let assessmentStarted = false;

let currentPosition = "READY";

let candidateState = null;
let candidateFrames = 0;

let cooldown = 0;

let upShoulderY = null;

let lastBodyStatus = "";


// ======================================================
// MEDIAPIPE
// ======================================================

const pose = new Pose({

    locateFile: (file) => {
        return `https://cdn.jsdelivr.net/npm/@mediapipe/pose/${file}`;
    }

});


pose.setOptions({

    modelComplexity: 1,

    smoothLandmarks: true,

    enableSegmentation: false,

    minDetectionConfidence: 0.6,

    minTrackingConfidence: 0.6

});


// ======================================================
// ANGLE FUNCTION
// ======================================================

function calculateAngle(a, b, c) {

    const radians =
        Math.atan2(c.y - b.y, c.x - b.x) -
        Math.atan2(a.y - b.y, a.x - b.x);

    let angle =
        Math.abs(radians * 180 / Math.PI);

    if (angle > 180) {
        angle = 360 - angle;
    }

    return angle;
}


// ======================================================
// DISTANCE
// ======================================================

function distance(a, b) {

    const dx = a.x - b.x;
    const dy = a.y - b.y;

    return Math.sqrt(
        dx * dx + dy * dy
    );

}


// ======================================================
// BODY ORIENTATION
// ======================================================

function checkBodyPosition(
    shoulder,
    hip,
    knee,
    ankle
) {

    /*
        A real push-up should have the body
        approximately horizontal.

        Standing:
        shoulder
           |
          hip
           |
         knee
           |
         ankle

        Push-up:
        shoulder ---- hip ---- knee ---- ankle
    */


    const bodyLength =
        distance(shoulder, ankle);


    if (bodyLength < 0.05) {

        return {
            valid: false,
            ratio: 999
        };

    }


    const yValues = [

        shoulder.y,
        hip.y,
        knee.y,
        ankle.y

    ];


    const highest =
        Math.min(...yValues);

    const lowest =
        Math.max(...yValues);


    const verticalSpread =
        lowest - highest;


    const ratio =
        verticalSpread / bodyLength;


    return {

        valid:
            ratio <
            SETTINGS.MAX_BODY_VERTICAL_RATIO,

        ratio: ratio

    };

}


// ======================================================
// LANDMARK SELECTION
// ======================================================

function getBetterSide(landmarks) {

    const left = {

        shoulder: landmarks[11],
        elbow: landmarks[13],
        wrist: landmarks[15],
        hip: landmarks[23],
        knee: landmarks[25],
        ankle: landmarks[27]

    };


    const right = {

        shoulder: landmarks[12],
        elbow: landmarks[14],
        wrist: landmarks[16],
        hip: landmarks[24],
        knee: landmarks[26],
        ankle: landmarks[28]

    };


    const leftScore =
        left.shoulder.visibility +
        left.elbow.visibility +
        left.wrist.visibility +
        left.hip.visibility +
        left.knee.visibility +
        left.ankle.visibility;


    const rightScore =
        right.shoulder.visibility +
        right.elbow.visibility +
        right.wrist.visibility +
        right.hip.visibility +
        right.knee.visibility +
        right.ankle.visibility;


    return leftScore >= rightScore
        ? left
        : right;

}


// ======================================================
// VISIBILITY CHECK
// ======================================================

function landmarksVisible(side) {

    return (

        side.shoulder.visibility >= SETTINGS.MIN_VISIBILITY &&

        side.elbow.visibility >= SETTINGS.MIN_VISIBILITY &&

        side.wrist.visibility >= SETTINGS.MIN_VISIBILITY &&

        side.hip.visibility >= SETTINGS.MIN_VISIBILITY &&

        side.knee.visibility >= SETTINGS.MIN_VISIBILITY &&

        side.ankle.visibility >= SETTINGS.MIN_VISIBILITY

    );

}


// ======================================================
// STATE CONFIRMATION
// ======================================================

function confirmState(state) {

    if (candidateState === state) {

        candidateFrames++;

    }

    else {

        candidateState = state;

        candidateFrames = 1;

    }


    return candidateFrames >= SETTINGS.REQUIRED_FRAMES;

}


// ======================================================
// PROCESS PUSH-UP
// ======================================================

function processPushUp(side) {

    const shoulder = side.shoulder;
    const elbow = side.elbow;
    const wrist = side.wrist;
    const hip = side.hip;
    const knee = side.knee;
    const ankle = side.ankle;


    // --------------------------------------------------
    // BODY POSITION
    // --------------------------------------------------

    const body =
        checkBodyPosition(
            shoulder,
            hip,
            knee,
            ankle
        );


    if (!body.valid) {

        positionElement.textContent =
            "POSITION";

        feedbackElement.textContent =
            "Get into a straight, horizontal push-up position.";

        lastBodyStatus = "bad";

        return;

    }


    lastBodyStatus = "good";


    // --------------------------------------------------
    // ELBOW ANGLE
    // --------------------------------------------------

    const elbowAngle =
        calculateAngle(
            shoulder,
            elbow,
            wrist
        );


    // --------------------------------------------------
    // NORMALIZED SHOULDER POSITION
    // --------------------------------------------------

    /*
        We normalize shoulder movement using
        body length so camera distance matters less.
    */

    const bodyLength =
        distance(
            shoulder,
            ankle
        );


    const normalizedShoulderY =
        shoulder.y / bodyLength;


    // --------------------------------------------------
    // COOLDOWN
    // --------------------------------------------------

    if (cooldown > 0) {

        cooldown--;

    }


    // --------------------------------------------------
    // UP POSITION
    // --------------------------------------------------

    if (
        elbowAngle >= SETTINGS.UP_ANGLE
    ) {

        positionElement.textContent =
            "UP";


        if (
            confirmState("UP")
        ) {

            /*
                Store the shoulder position
                when the athlete reaches UP.
            */

            if (
                currentPosition !== "UP"
            ) {

                upShoulderY =
                    normalizedShoulderY;

            }


            /*
                If we previously reached DOWN,
                coming back UP completes one rep.
            */

            if (

                currentPosition === "DOWN" &&

                cooldown === 0 &&

                upShoulderY !== null

            ) {

                reps++;

                repCountElement.textContent =
                    reps;

                cooldown =
                    SETTINGS.COOLDOWN_FRAMES;

                feedbackElement.textContent =
                    "Rep counted ✓ Full movement detected.";

            }

            else {

                feedbackElement.textContent =
                    "Good extension. Lower your body.";

            }


            currentPosition = "UP";

        }

    }


    // --------------------------------------------------
    // DOWN POSITION
    // --------------------------------------------------

    else if (
        elbowAngle <= SETTINGS.DOWN_ANGLE
    ) {

        positionElement.textContent =
            "DOWN";


        if (
            confirmState("DOWN")
        ) {

            let shoulderMovement = 0;


            if (
                upShoulderY !== null
            ) {

                shoulderMovement =
                    normalizedShoulderY -
                    upShoulderY;

            }


            /*
                Because image Y increases downward,
                a positive value means the shoulder
                moved downward.
            */


            if (
                currentPosition === "UP"
            ) {

                if (
                    shoulderMovement >=
                    SETTINGS.MIN_SHOULDER_MOVEMENT
                ) {

                    currentPosition =
                        "DOWN";

                    feedbackElement.textContent =
                        "Good depth ✓ Push back up.";

                }

                else {

                    feedbackElement.textContent =
                        "Lower your whole body, not just your arms.";

                }

            }

        }

    }


    // --------------------------------------------------
    // MIDDLE POSITION
    // --------------------------------------------------

    else {

        positionElement.textContent =
            "MID";


        feedbackElement.textContent =
            "Continue through the full range of motion.";

    }


    // --------------------------------------------------
    // EXTRA FORM CHECK
    // --------------------------------------------------

    const shoulderHipDistance =
        Math.abs(
            shoulder.y -
            hip.y
        );


    if (
        shoulderHipDistance >
        bodyLength * 0.30
    ) {

        feedbackElement.textContent =
            "Keep your body straighter.";

    }

}


// ======================================================
// MEDIAPIPE RESULTS
// ======================================================

pose.onResults((results) => {

    canvas.width =
        video.videoWidth;

    canvas.height =
        video.videoHeight;


    ctx.save();

    ctx.clearRect(
        0,
        0,
        canvas.width,
        canvas.height
    );


    ctx.drawImage(
        results.image,
        0,
        0,
        canvas.width,
        canvas.height
    );


    // --------------------------------------------------
    // NO PERSON
    // --------------------------------------------------

    if (
        !results.poseLandmarks
    ) {

        detectionElement.textContent =
            "Not detected";

        cameraMessage.textContent =
            "Move into camera view";

        feedbackElement.textContent =
            "Make sure your full body is visible.";

        positionElement.textContent =
            "READY";

        ctx.restore();

        return;

    }


    // --------------------------------------------------
    // DRAW SKELETON
    // --------------------------------------------------

    drawConnectors(
        ctx,
        results.poseLandmarks,
        POSE_CONNECTIONS,
        {

            color: "#65e572",

            lineWidth: 4

        }
    );


    drawLandmarks(
        ctx,
        results.poseLandmarks,
        {

            color: "#ffffff",

            lineWidth: 2,

            radius: 5

        }
    );


    detectionElement.textContent =
        "Detected ✓";

    cameraMessage.textContent =
        "AI Pose Detection Active";


    // --------------------------------------------------
    // GET BEST SIDE
    // --------------------------------------------------

    const side =
        getBetterSide(
            results.poseLandmarks
        );


    // --------------------------------------------------
    // VISIBILITY
    // --------------------------------------------------

    if (
        !landmarksVisible(side)
    ) {

        feedbackElement.textContent =
            "Keep your full body visible to the camera.";

        positionElement.textContent =
            "BODY";

        ctx.restore();

        return;

    }


    // --------------------------------------------------
    // PROCESS
    // --------------------------------------------------

    if (
        assessmentStarted
    ) {

        processPushUp(side);

    }

    else {

        feedbackElement.textContent =
            "Press Start Assessment, then perform a push-up.";

    }


    ctx.restore();

});


// ======================================================
// CAMERA
// ======================================================

async function startCamera() {

    try {

        const stream =
            await navigator.mediaDevices.getUserMedia({

                video: {

                    width: 640,

                    height: 480,

                    facingMode: "user"

                },

                audio: false

            });


        video.srcObject =
            stream;


        await video.play();


        connectionStatus.textContent =
            "CAMERA READY";


        const camera =
            new Camera(
                video,
                {

                    onFrame: async () => {

                        await pose.send({
                            image: video
                        });

                    },

                    width: 640,

                    height: 480

                }
            );


        camera.start();

    }


    catch (error) {

        console.error(error);

        connectionStatus.textContent =
            "CAMERA ERROR";

        cameraMessage.textContent =
            "Camera permission required.";

    }

}


// ======================================================
// START ASSESSMENT
// ======================================================

startButton.addEventListener(
    "click",
    () => {

        assessmentStarted =
            true;

        reps = 0;

        currentPosition =
            "READY";

        candidateState =
            null;

        candidateFrames =
            0;

        cooldown =
            0;

        upShoulderY =
            null;


        repCountElement.textContent =
            "0";

        positionElement.textContent =
            "READY";

        assessmentStatus.textContent =
            "In Progress";


        feedbackElement.textContent =
            "Get into a push-up position.";

        startButton.textContent =
            "Assessment Running";

    }
);


// ======================================================
// RESET
// ======================================================

resetButton.addEventListener(
    "click",
    () => {

        reps = 0;

        assessmentStarted =
            false;

        currentPosition =
            "READY";

        candidateState =
            null;

        candidateFrames =
            0;

        cooldown =
            0;

        upShoulderY =
            null;


        repCountElement.textContent =
            "0";

        positionElement.textContent =
            "READY";

        assessmentStatus.textContent =
            "Ready";


        feedbackElement.textContent =
            "Position yourself sideways to the camera.";

        startButton.textContent =
            "Start Assessment";

    }
);


// ======================================================
// EQUIPMENT DEMO
// ======================================================

function issueEquipment(name) {

    alert(

        name +
        " issued successfully.\n\n" +
        "Issue record created."

    );

}


// ======================================================
// GROUND BOOKING DEMO
// ======================================================

function bookSlot(button) {

    button.textContent =
        "Booked ✓";

    button.style.background =
        "#275b2d";

    button.style.color =
        "#65e572";

    button.disabled =
        true;

}


// ======================================================
// START CAMERA
// ======================================================

startCamera();