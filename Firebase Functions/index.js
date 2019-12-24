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


    var fanspeed = parseInt(params['fanspeed']);
    var activate = parseInt(params['activate']);
    var deactivate = parseInt(params['deactivate']);
    var number = parseInt(params['number']);

    console.log("activate = " + params['activate']);
    console.log("deactivate = " + params['deactivate']);
    console.log("fanspeed = " + fanspeed);

    if(activate == 1){
        outstr = "電扇開始運轉";
    }
    else if(deactivate == 0){
        outstr = "電扇停止運轉";
    }
    else if(fanspeed == 1){
        outstr = "電扇設定為風速 1 ";
    }
    else if(fanspeed == 2){
        outstr = "電扇設定為風速 2";
    }
    else if(fanspeed == 3){
            outstr = "電扇設定為風速 3";
    }
    else if(number > 3 ){
        outstr = "此電扇目前沒有支援風速 ＝ " + number;
    }
    else{
        outstr = "電扇已經可以控制了"
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
