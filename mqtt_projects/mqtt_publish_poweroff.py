# MQTT Publish Demo
# Publish two messages, to two different topics

import paho.mqtt.publish as publish


publish.single("RobLab/topic", "PowerOff", hostname="test.mosquitto.org")
print("Done")