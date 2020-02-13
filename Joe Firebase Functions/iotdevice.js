// // Create and Deploy Your First Cloud Functions
// // https://firebase.google.com/docs/functions/write-firebase-functions
//


const functions = require("firebase-functions"),
  PubSub = require(`@google-cloud/pubsub`),
  admin = require("firebase-admin");
const iot = require('@google-cloud/iot');
const client = new iot.v1.DeviceManagerClient();
/*
var serviceAccount = require("./firebaseServiceAccountKey.json");

try{
	admin.initializeApp({
		credential: admin.credential.cert(serviceAccount),
		databaseURL: "https://myesp8266-1.firebaseio.com"
	});
}catch(err){
	console.log(err);
}
*/
try{
	admin.initializeApp();
}catch(err){
	console.log(err);
}
//const app = admin.initializeApp();
//const firestore = app.firestore();
const firestore = admin.firestore();
var num = 0;

firestore.settings({ timestampsInSnapshots: true });

//const auth = app.auth();

exports.deviceState = functions.pubsub.topic('iot-topic').onPublish(async (message) => {
    const deviceId = message.attributes.deviceId;
    const deviceRef = firestore.doc(`device-configs/${deviceId}`);
    try {
      //await deviceRef.update({ 'state': message.json, 'online': true, 'timestamp' : admin.firestore.Timestamp.now() });
      await deviceRef.update(message.json);
      console.log(`State updated for ${deviceId}`);
    } catch (error) {
      console.error(`${deviceId} not yet registered to a user`, error);
    }
});
/*
exports.deviceStateArray = functions.pubsub.topic('iot-topic').onPublish(async (message) => {
  const deviceId = message.attributes.deviceId;
  console.log(message);
  const deviceRef = firestore.doc(`device-configs/${deviceId}`);
  try {
    var list = [];
    await deviceRef.get().then((doc) => {
      var data = doc.data();
      if ("states" in data)
        list = data.states;
      if (list.length > 19) {
        list = list.slice(1);
      }
      list.push({'state': message.json});
      deviceRef.update({
        states: list
      });
      return null;  
    }).catch((error) => {
        console.log("Error getting document:", error);
    });
  } catch (error) {
    console.error(`${deviceId} not yet registered to a user`, error);
  }
});
*/
/*
exports.deviceOnlineState = functions.pubsub.topic('online-state').onPublish(async (message) => {
  const logEntry = JSON.parse(Buffer.from(message.data, 'base64').toString());
  const deviceId = logEntry.labels.device_id;
  //const deviceId = message.attributes.deviceId;
  let online;
  switch (logEntry.jsonPayload.eventType) {
    case 'CONNECT':
      online = true;
      break;
    case 'DISCONNECT':
      online = false;
      break;
    default:
      throw new Error(`Invalid event type received from IoT Core: ${logEntry.jsonPayload.eventType}`);
  }

  // Write the online state into firestore
  const deviceRef = firestore.doc(`device-configs/${deviceId}`);
  try {
    await deviceRef.update({ 'online': online });
    console.log(`Connectivity updated for ${deviceId}`);
  } catch (error) {
    console.error(`${deviceId} not yet registered to a user`, error);
  }
});
*/

//send configuration to device
const projectId = admin.instanceId().app.options.projectId
const cloudRegion    = 'europe-west1';
const registryId     = 'iot-registry';

exports.configUpdate = functions.firestore.document(`device-configs/{deviceId}`).onWrite(async (change,context) => {
    if (context) {
      console.log(context.params.deviceId);
      const request = generateRequest(context.params.deviceId, change.after.data(), false);
      return client.modifyCloudToDeviceConfig(request);
    } else {
      throw(Error("no context from trigger"));
    }
  });


function generateRequest(currDeviceId, configData, isBinary) {
  const currDeviceIDString = `${currDeviceId}`
  const formattedName = client.devicePath(projectId,cloudRegion,registryId,currDeviceIDString);
  let dataValue;
  if (isBinary) {
    const encoded = cbor.encode(configData);
    dataValue = encoded.toString("base64");
  } else {
    dataValue = Buffer.from(JSON.stringify(configData)).toString("base64");
  }
  return {
    name: formattedName,
    binaryData: dataValue
  };
}
















