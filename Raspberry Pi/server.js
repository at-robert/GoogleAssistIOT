var admin = require("firebase-admin");

// filestream library for writing file
var fs = require('fs');

// To define extra GPIO for LED display
var Gpio = require('onoff').Gpio; //include onoff to interact with the GPIO
var LED1 = new Gpio(5, 'out');
var LED2 = new Gpio(6, 'out');
var LED3 = new Gpio(21, 'out');

const ACTIVATE = "activate";
const DEACTIVATE = "deactivate";
const RELAY = "number";

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

	Object.entries(PATH_TO_RELAYS).forEach(([relay, path]) => {
		if (relay == entitiesFromAssistant[RELAY]) {
			if (entitiesFromAssistant[ACTIVATE]) {
				LED1.writeSync(1);
				LED2.writeSync(0);
				LED3.writeSync(1);

				fs.writeFileSync(path, entitiesFromAssistant[ACTIVATE], function (err) {
					if (err) throw err;
				}); // fs.writeFileSync
			} else {
				LED1.writeSync(0);
				LED2.writeSync(1);
				LED3.writeSync(0);

				fs.writeFileSync(path, entitiesFromAssistant[DEACTIVATE], function (err) {
					if (err) throw err;
				}); // fs.writeFileSync
			} // else
		} // if (relay == entitiesFromAssistant[RELAY])
	}); // Object.entries(PATH_TO_RELAYS).forEach
}); // ref.on('value', function (snapshot)