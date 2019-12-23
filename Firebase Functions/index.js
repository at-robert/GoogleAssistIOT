//  For more information about this code, please click
//  https://firebase.google.com/docs/functions/get-started

const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp(functions.config().firebase);

var database = admin.database();

exports.relay = functions.https.onRequest((request, response) => {

    //  Dialogflow sends API V2 json file to Firebase Functions via HTTP POST request.
    //  https://us-central1-MY_PROJECT.cloudfunctions.net/relay
    let params = request.body.queryResult.parameters;
    let outstr = "Fan Relay is controlled succesfully.";

    console.log("This message will be printed on /functions/logs on Firebase.");
    console.log(params);


    var fanspeed = parseInt(params['percentage']);

    console.log("Param 1 = " + params['activate']);
    console.log("Param 2 = " + params['number']);
    console.log("Param 3 = " + params['deactivate']);
    console.log("fanspeed = " + fanspeed);


    if(params['number'] == 1){
        outstr = "Fan 1 is controlled";
    }
    else if(params['number'] == 2){
        outstr = "Fan 2 is controlled";
    }
    else{
        outstr = "The Fan is ready for control"
    }

    if(fanspeed > 0){
        outstr = outstr + " In Fan Speed " + params['percentage'];
    }
    

    database.ref().set(params);

    //  API V2 needs below response format.
    response.send(
        {
            "payload": {
                "google": {
                    "expectUserResponse": true,
                    "richResponse": {
                        "items": [
                            {
                                "simpleResponse": {
                                    "textToSpeech": outstr
                                }
                            }
                        ]
                    }
                }
            }
        }
    );
});
