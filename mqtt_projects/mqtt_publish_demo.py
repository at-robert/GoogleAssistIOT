# MQTT Publish Demo
# Publish two messages, to two different topics

import paho.mqtt.publish as publish

publish.single("RobLab/test", "Hello", hostname="test.mosquitto.org")
publish.single("RobLab/topic", "User1/Fan/status 1", hostname="test.mosquitto.org")
print("Done")