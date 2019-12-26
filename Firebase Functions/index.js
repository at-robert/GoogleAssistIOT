//  For more information about this code, please click
//  https://firebase.google.com/docs/functions/get-started

const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp(functions.config().firebase);

var database = admin.database();

// To read out database values
var activate_db;
var deactivate_db;
var fanspeed_db;
database.ref('/activate').on('value',e=>{
    activate_db = e.val();
    activate_db = parseInt(activate_db);
    console.log("activate_db = " + activate_db);
});
database.ref('/deactivate').on('value',e=>{
    deactivate_db = e.val();
    deactivate_db = parseInt(deactivate_db);
    console.log("deactivate_db = " + deactivate_db);
});
database.ref('/fanspeed').on('value',e=>{
    fanspeed_db = e.val();
    fanspeed_db = parseInt(fanspeed_db);
    console.log("fanspeed_db = " + fanspeed_db);
});
// -- END read out database values

exports.relay = functions.https.onRequest((request, response) => {

    //  Dialogflow sends API V2 json file to Firebase Functions via HTTP POST request.
    //  https://us-central1-MY_PROJECT.cloudfunctions.net/relay
    let params = request.body.queryResult.parameters;
    let outstr = "Fan Relay is controlled succesfully.";

    console.log("This message will be printed on /functions/logs on Firebase.");
    console.log(params);

    // Fan Control Logic ------
    var fanspeed = parseInt(params['fanspeed']);
    var activate = parseInt(params['activate']);
    var deactivate = parseInt(params['deactivate']);
    var number = parseInt(params['number']);
    var speedchange = parseInt(params['speedchange']);

    console.log("activate = " + params['activate']);
    console.log("deactivate = " + params['deactivate']);
    console.log("fanspeed = " + fanspeed);

    // To restore database value
    // params['activate'] = activate_db;
    // params['deactivate'] = deactivate_db;
    // params['fanspeed'] = fanspeed_db;
    // --- END

    if(activate == 1){
        if(activate_db == 1){
            outstr = "電扇已經在運轉";
        }
        else{
            outstr = "電扇開始運轉";
            database.ref('/activate').set('1');
            database.ref('/deactivate').set('');
        }
            
    }
    else if(deactivate == 0){
        if(deactivate_db == 0){
            outstr = "電扇已經停止運轉";
        }
        else{
            outstr = "電扇停止運轉";
            database.ref('/activate').set('');
            database.ref('/deactivate').set('0');
        }
    }
    else if(fanspeed == 1){
        if(deactivate_db == 0){
            outstr = "電扇已經停止運轉,開啟電扇並設定為風速1";
            database.ref('/activate').set('1');
            database.ref('/deactivate').set('');
            database.ref('/fanspeed').set(1);
        }
        else{
            outstr = "電扇設定為風速 1 ";
            database.ref('/fanspeed').set(1);
        }
    }
    else if(fanspeed == 2){
        if(deactivate_db == 0){
            outstr = "電扇已經停止運轉,開啟電扇並設定為風速2";
            database.ref('/activate').set('1');
            database.ref('/deactivate').set('');
            database.ref('/fanspeed').set(2);
        }
        else{
            outstr = "電扇設定為風速 2 ";
            database.ref('/fanspeed').set(2);
        }
    }
    else if(fanspeed == 3){
        if(deactivate_db == 0){
            outstr = "電扇已經停止運轉,開啟電扇並設定為風速3";
            database.ref('/activate').set('1');
            database.ref('/deactivate').set('');
            database.ref('/fanspeed').set(3);
        }
        else{
            outstr = "電扇設定為風速 3 ";
            database.ref('/fanspeed').set(3);
        }
    }
    else if(number > 3 ){
        outstr = "此電扇目前沒有支援風速 ＝ " + number;
    }
    else if(speedchange > 0){
        if(speedchange == 1){
            // outstr = "風速往上";
            if(deactivate_db == 0){
                outstr = "電扇已經停止運轉,開啟電扇並設定為風速1";
                database.ref('/activate').set('1');
                database.ref('/deactivate').set('');
                database.ref('/fanspeed').set(1);
            }
            else{
                if(fanspeed_db < 3){
                    var fanspeed_ = fanspeed_db;
                    fanspeed_ = fanspeed_ + 1;
                    outstr = "電扇設定為風速 " + fanspeed_;
                    database.ref('/fanspeed').set(fanspeed_);
                }
                else{
                    outstr = "風速已經是最大,建議開啟門窗";
                }
            }
        }
        else{
            // outstr = "風速往下";
            if(deactivate_db == 0){
                outstr = "電扇已經停止運轉,建議開啟暖氣";
            }
            else{
                if(fanspeed_db > 1){
                    var fanspeed_ = fanspeed_db;
                    fanspeed_ = fanspeed_ - 1;
                    outstr = "電扇設定為風速 " + fanspeed_;
                    database.ref('/fanspeed').set(fanspeed_);
                }
                else{
                    outstr = "電扇停止運轉";
                    database.ref('/activate').set('');
                    database.ref('/deactivate').set('0');
                }
            }
        }
    }
    else{
        outstr = "電扇已經可以控制了"
        // database.ref().set(params);
    }
    // Fan Control Logic END ------

    

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
