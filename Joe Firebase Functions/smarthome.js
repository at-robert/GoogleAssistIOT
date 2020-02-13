/**
 * Copyright 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

'use strict';

const functions = require('firebase-functions');
const {smarthome} = require('actions-on-google');
const {google} = require('googleapis');
//const util = require('util');
const admin = require('firebase-admin');
const uuid = require('uuid/v4');
//const {AuthenticationClient} = require('auth0');
//onst auth0 = new AuthenticationClient({
//  'clientId': '1Hk0gylgQ48vP4EHif8g55cVBz74Q4J9',
//  'domain': 'smarthome-fan2.auth0.com'
//});

// Initialize Firebase
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
//const firebaseRef = admin.database().ref('/');
const firestore = admin.firestore();

// Initialize Homegraph
const auth = new google.auth.GoogleAuth({
  scopes: ['https://www.googleapis.com/auth/homegraph']
});

const homegraph = google.homegraph({
  version: 'v1',
  auth: auth
});
/*
exports.fakeauth = functions.https.onRequest((request, response) => {
  const responseurl = util.format('%s?code=%s&state=%s',
    decodeURIComponent(request.query.redirect_uri), 'xxxxxx',
    request.query.state);
  console.log(responseurl);
  return response.redirect(responseurl);
});

exports.faketoken = functions.https.onRequest((request, response) => {
  const grantType = request.query.grant_type
    ? request.query.grant_type : request.body.grant_type;
  const secondsInDay = 86400; // 60 * 60 * 24
  const HTTP_STATUS_OK = 200;
  console.log(`Grant type ${grantType}`);

  let obj;
  if (grantType === 'authorization_code') {
    obj = {
      token_type: 'bearer',
      access_token: '123access',
      refresh_token: '123refresh',
      expires_in: secondsInDay,
    };
  } else if (grantType === 'refresh_token') {
    obj = {
      token_type: 'bearer',
      access_token: '123access',
      expires_in: secondsInDay,
    };
  }
  response.status(HTTP_STATUS_OK)
    .json(obj);
});
*/
const app = smarthome({
  debug: true,
  jwt: require('./jwtServiceAccountKey.json'),
});

let agentId ='123'; 

app.onSync(async (body, headers) => {
  console.log('onSync');
  return {
    requestId: body.requestId,
    payload: {
      agentUserId: agentId,
      devices: [{
        id: 'esp8266_18D78A',
        type: 'action.devices.types.FAN',
        traits: [
          'action.devices.traits.OnOff',
          'action.devices.traits.FanSpeed',
          //'action.devices.traits.Mode',
        ],
        name: {
          //defaultNames: ['My Fan'],
          name: 'Fan',
          //nicknames: ['Fan2'],
        },
        deviceInfo: {
          manufacturer: 'Amtran',
          model: 'Joe-Fan1',
          hwVersion: '1.0',
          swVersion: '1.0.1',
        },
        willReportState: true,
        attributes: {
			availableFanSpeeds: {
				speeds: [{
					speed_name: 'Low',
					speed_values: [{
						speed_synonym: ['speedslow','speedlow','speedsmall','slow','low','small','minimum'],
						lang: 'en'
					}]
				},{
					speed_name: 'Medium',
					speed_values: [{
						speed_synonym: ['medium','speed medium'],
						lang: 'en'
					}]
				},{
					speed_name: 'High',
					speed_values: [{
						speed_synonym: ['speed fast','speed high','speed big','fast','high','big','maximum'],
						lang: 'en'
					}]
				}],
            ordered: true
          },
          reversible: true,
		  /*
		  availableModes: [{
            name: 'windMode',
            name_values: [{
              name_synonym: ['wind'],
              lang: 'en'
            }],
            settings: [{
              setting_name: 'natural',
              setting_values: [{
                setting_synonym: ['natural', 'random'],
                lang: 'en'
              }]
            }, {
              setting_name: 'sleep',
              setting_values: [{
                setting_synonym: ['sleep'],
                lang: 'en'
              }]
            }],
            ordered: true
          }]*/
        },
      }],
    },
  };
});

const queryFirebase = async (deviceId) => {
  //const snapshot = await firebaseRef.child(deviceId).once('value');
  const deviceRef = firestore.doc(`device-configs/${deviceId}`);
  //const snapshotVal = snapshot.val();

  const snapshotVal = await deviceRef.get();
  /*.then(doc => {
      console.log('Document data:', doc.data());
  });
  */
  //console.log('snapshotVal:', snapshotVal.data());
  //console.log('on:', snapshotVal.data().OnOff);
  console.log('queryFirebase currentFanSpeedSetting:', snapshotVal.data().FanSpeed);
  return {
	on: snapshotVal.data().OnOff,
	currentFanSpeedSetting: snapshotVal.data().FanSpeed,
    //on: snapshotVal.on,
	//currentFanSpeedSetting: snapshotVal.currentFanSpeedSetting,
  };
}
const queryDevice = async (deviceId) => {
  const data = await queryFirebase(deviceId);
  console.log('queryDevice currentFanSpeedSetting :'+data.currentFanSpeedSetting);
  return {
	on: data.on,
	currentFanSpeedSetting: data.currentFanSpeedSetting,
    //on: data.on,
	//currentFanSpeedSetting: data.currentFanSpeedSetting,
  };
}

app.onQuery(async (body) => {
  console.log('onQuery');
  const {requestId} = body;
  const payload = {
    devices: {},
  };
  const queryPromises = [];
  const intent = body.inputs[0];
  for (const device of intent.payload.devices) {
    const deviceId = device.id;
    queryPromises.push(queryDevice(deviceId)
      .then((data) => {
        // Add response to device payload
        payload.devices[deviceId] = data;
		console.log('onQuery currentFanSpeedSetting :'+data.currentFanSpeedSetting);
      }
    ));
  }
  // Wait for all promises to resolve
  await Promise.all(queryPromises)
  return {
    requestId: requestId,
    payload: payload,
  };
});

const updateDevice = async (execution,deviceId) => {
  const {params,command} = execution;
  let state, ref;
  const deviceRef = firestore.doc(`device-configs/${deviceId}`);
  switch (command) {
    case 'action.devices.commands.OnOff':
      state = {OnOff: params.on};
      //ref = firebaseRef.child(deviceId).child('OnOff');
      break;
	case 'action.devices.commands.SetFanSpeed':
      state = {FanSpeed: params.fanSpeed};
      //ref = firebaseRef.child(deviceId).child('FanSpeed');
      break;
  }
  //return ref.update(state)
  return deviceRef.update(state)
    .then(() => state);
};

app.onExecute(async (body) => {
  console.log('onExecute');
  const {requestId} = body;
  // Execution results are grouped by status
  const result = {
    ids: [],
    status: 'SUCCESS',
    states: {
      online: true,
    },
  };

  const executePromises = [];
  const intent = body.inputs[0];
  for (const command of intent.payload.commands) {
    for (const device of command.devices) {
      for (const execution of command.execution) {
        executePromises.push(
          updateDevice(execution,device.id)
            .then((data) => {
              result.ids.push(device.id);
              Object.assign(result.states, data);
            })
            .catch(() => console.error(`Unable to update ${device.id}`))
        );
      }
    }
  }

  await Promise.all(executePromises)
  return {
    requestId: requestId,
    payload: {
      commands: [result],
    },
  };
});


exports.smarthome = functions.https.onRequest(app);

exports.requestsync = functions.https.onRequest(async (request, response) => {
  console.log('requestsync');
  response.set('Access-Control-Allow-Origin', '*');
  console.info('Request SYNC for user 123');
  try {

    const res = await homegraph.devices.requestSync({
      requestBody: {
        agentUserId: agentId
      }
    });
    console.info('Request sync response:', res.status, res.data);
    response.json(res.data);
  } catch (err) {
    console.error(err);
    response.status(500).send(`Error requesting sync: ${err}`)
  }
});

/**
 * Send a REPORT STATE call to the homegraph when data for any device id
 * has been changed.
 */
//exports.reportstate = functions.database.ref('{deviceId}').onWrite(async (change, context) => {
exports.reportstate = functions.firestore.document(`device-configs/{deviceId}`).onWrite(async (change, context) => {
  console.log('reportstate');
  console.info('Firebase write event triggered this cloud function');
  //const snapshot = change.after.val();
  const snapshot = change.after.data();
  console.log('reportstate-on:'+snapshot.OnOff);
  console.log('reportstate-currentFanSpeedSetting:'+snapshot.FanSpeed);
  console.log('uuid:'+uuid());
  const requestBody = {
    requestId: uuid(), /* Any unique ID */
    agentUserId: agentId, /* Hardcoded user ID */
    payload: {
      devices: {
        states: {
          /* Report the current state of our washer */
          [context.params.deviceId]: {
			OnOff: snapshot.OnOff,
			FanSpeed: snapshot.FanSpeed,
            //on: snapshot.OnOff.on,
			//currentFanSpeedSetting: snapshot.FanSpeed.currentFanSpeedSetting,
          },
        },
      },
    },
  };

  const res = await homegraph.devices.reportStateAndNotification({
    requestBody
  });
  console.info('Report state response:', res.status, res.data);
});

