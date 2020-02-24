# JS-enabled demo Mongoose OS firmware

This is the JS demo Mongoose OS app. It gets installed by default at
[Mongoose OS installation step](https://mongoose-os.com/docs/). It has
a lot of functionality enabled - cloud integrations, JavaScript engine, etc.
Its main purpose is to demonstrate the capabilities of Mongoose OS.

1. mos put gcp-esp32_814FDC.key.pem

2. Set up GCP key and parameters
mos call AMT.SetupGCP '{"enable": true, "project": "miles-simple-iot", "region": "europe-west1", "registry": "iot-registry"}'

3. System will auto-reboot

4. Connect to AP
mos call AMT.ConnectToAP '{"ssid": "VXA", "pass": "12345678"}'