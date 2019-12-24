var admin = require("firebase-admin");

// filestream library for writing file
var fs = require('fs');

// To define extra GPIO for LED display
var Gpio = require('onoff').Gpio; //include onoff to interact with the GPIO
var ONOFF = new Gpio(26, 'out');
var SPEED1 = new Gpio(5, 'out');
var SPEED2 = new Gpio(6, 'out');
var SPEED3 = new Gpio(21, 'out');

const ACTIVATE = "activate";
const DEACTIVATE = "deactivate";
const RELAY = "number";
const FANSPEED = 'fanspeed';

const PATH_TO_RELAYS = {
	1: "/sys/class/gpio/gpio26/value",
	2: "/sys/class/gpio/gpio24/value"
};

var serviceAccount = require("./serviceAccountKey.json");

admin.initializeApp({
	credential: admin.credential.cert(serviceAccount),
	databaseURL: "https://raspberry-relay-control-wwhlgt.firebaseio.com"
});

// Database Init
var defaultDatabase = admin.database();
var ref = defaultDatabase.ref();

entitiesFromAssistant = {}

ref.on('value', function (snapshot) {
	snapshot.forEach(function (childSnapshot) {

		var key = childSnapshot.key;
		var val = childSnapshot.val();

		entitiesFromAssistant[key] = val;

	}); // snapshot.forEach(function (childSnapshot)

	console.log(entitiesFromAssistant);

	if(entitiesFromAssistant[ACTIVATE] == '1'){
		ONOFF.writeSync(1);
	}
	else if(entitiesFromAssistant[DEACTIVATE] == '0'){
		ONOFF.writeSync(0);
	}
	else if(entitiesFromAssistant[FANSPEED] == '1'){
		SPEED1.writeSync(1);
		SPEED2.writeSync(0);
		SPEED3.writeSync(0);
	}
	else if(entitiesFromAssistant[FANSPEED] == '2'){
		SPEED1.writeSync(0);
		SPEED2.writeSync(1);
		SPEED3.writeSync(0);
	}
	else if(entitiesFromAssistant[FANSPEED] == '3'){
		SPEED1.writeSync(0);
		SPEED2.writeSync(0);
		SPEED3.writeSync(1);
	}


	// Object.entries(PATH_TO_RELAYS).forEach(([relay, path]) => {
	// 	//if (relay == entitiesFromAssistant[RELAY]) {
	// 		if (entitiesFromAssistant[ACTIVATE]) {
	// 			LED1.writeSync(1);
	// 			LED2.writeSync(0);
	// 			LED3.writeSync(1);

	// 			fs.writeFileSync(path, entitiesFromAssistant[ACTIVATE], function (err) {
	// 				if (err) throw err;
	// 			}); // fs.writeFileSync
	// 		} else {
	// 			LED1.writeSync(0);
	// 			LED2.writeSync(1);
	// 			LED3.writeSync(0);

	// 			fs.writeFileSync(path, entitiesFromAssistant[DEACTIVATE], function (err) {
	// 				if (err) throw err;
	// 			}); // fs.writeFileSync
	// 		} // else
	// 	//} // if (relay == entitiesFromAssistant[RELAY])
	// }); // Object.entries(PATH_TO_RELAYS).forEach

}); // ref.on('value', function (snapshot)