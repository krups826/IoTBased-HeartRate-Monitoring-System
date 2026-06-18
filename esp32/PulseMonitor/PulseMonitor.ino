#include <WiFi.h>
// --- WiFi Configuration ---
const char* ssid = "OPPO F31 5G m3ua";
const char* password = "nhms9438";

// --- Backend API Configuration ---
// Use your computer's Wi-Fi IP address here, not localhost.
const String serverName = "http://10.81.79.38:8080/api/bpm/device";

// --- Device/User Configuration ---
// This email must be the same account you log in with on the website.
const String userEmail = "nirmalkrupa2006@gmail.com";

// --- Pulse Sensor Configuration ---
const int pulsePin = 34;
const int ledPin = 2;
int signalValue = 0;
int threshold = 1800;
int bpm = 0;

bool isAboveThreshold = false;
unsigned long lastBeatTime = 0;
unsigned long lastPostTime = 0;
unsigned long lastDebugTime = 0;

const unsigned long postInterval = 5000;
const unsigned long minBeatInterval = 300;
const unsigned long maxBeatInterval = 2000;
const unsigned long debugInterval = 1000;

void setup() {
  Serial.begin(115200);
  pinMode(ledPin, OUTPUT);
  analogReadResolution(12);

  Serial.println("Starting Pulse Monitor...");
  Serial.print("Configured user email: ");
  Serial.println(userEmail);
  Serial.print("Configured backend URL: ");
  Serial.println(serverName);
  Serial.print("Current threshold: ");
  Serial.println(threshold);

  WiFi.begin(ssid, password);
  Serial.println("Connecting to WiFi...");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println();
  Serial.print("Connected to WiFi. ESP32 IP: ");
  Serial.println(WiFi.localIP());
}

void loop() {
  signalValue = analogRead(pulsePin);
  unsigned long now = millis();

  if ((now - lastDebugTime) >= debugInterval) {
    Serial.print("Signal Value: ");
    Serial.print(signalValue);
    Serial.print(" | Threshold: ");
    Serial.print(threshold);
    Serial.print(" | BPM: ");
    Serial.println(bpm);
    lastDebugTime = now;
  }

  if (signalValue > threshold && !isAboveThreshold) {
    isAboveThreshold = true;
    digitalWrite(ledPin, HIGH);

    if (lastBeatTime > 0) {
      unsigned long interval = now - lastBeatTime;

      if (interval >= minBeatInterval && interval <= maxBeatInterval) {
        bpm = 60000 / interval;
        Serial.print("Beat detected. Interval: ");
        Serial.print(interval);
        Serial.print(" ms | BPM: ");
        Serial.println(bpm);
      } else {
        Serial.print("Ignored noisy beat. Interval: ");
        Serial.println(interval);
      }
    } else {
      Serial.println("First beat detected. Waiting for next beat to calculate BPM.");
    }

    lastBeatTime = now;
  }

  if (signalValue < threshold) {
    isAboveThreshold = false;
    digitalWrite(ledPin, LOW);
  }

  if ((now - lastPostTime) >= postInterval) {
    if (WiFi.status() == WL_CONNECTED) {
      if (bpm > 0) {
        sendBpmToServer(bpm);
      } else {
        Serial.println("BPM not ready yet, skipping send.");
      }
    } else {
      Serial.println("WiFi disconnected, skipping send.");
    }
    lastPostTime = now;
  }

  // --- Manual BPM Test via Serial ---
  if (Serial.available() > 0) {
    String input = Serial.readStringUntil('\n');
    input.trim();
    if (input.startsWith("TEST:")) {
      int testVal = input.substring(5).toInt();
      if (testVal > 0) {
        bpm = testVal;
        Serial.print(">>> MANUAL TEST TRIGGERED: Setting BPM to ");
        Serial.println(bpm);
        sendBpmToServer(bpm);
      }
    }
  }

  delay(10);
}

void sendBpmToServer(int currentBpm) {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("WiFi not connected. Skipping send.");
    return;
  }

  String jsonPayload = "{\"email\":\"" + userEmail + "\",\"bpm\":" + String(currentBpm) + "}";
  WiFiClient client;
  const char* host = "10.81.79.38";
  const uint16_t port = 8080;

  Serial.println("--- Starting POST Request ---");
  Serial.print("Target Host: ");
  Serial.println(host);
  Serial.print("Target Port: ");
  Serial.println(port);
  Serial.print("ESP32 IP: ");
  Serial.println(WiFi.localIP());

  Serial.print("Payload: ");
  Serial.println(jsonPayload);

  if (!client.connect(host, port)) {
    Serial.println("FAILED. Raw socket connect() could not reach backend.");
    Serial.println("TIP: backend IP/port is still unreachable from ESP32 on this network.");
    Serial.println("--- End of Request ---");
    return;
  }

  client.print(String("POST /api/bpm/device HTTP/1.1\r\n") +
               "Host: " + host + "\r\n" +
               "Content-Type: application/json\r\n" +
               "Content-Length: " + jsonPayload.length() + "\r\n" +
               "Connection: close\r\n\r\n" +
               jsonPayload);

  unsigned long timeout = millis();
  while (client.connected() && !client.available()) {
    if (millis() - timeout > 10000) {
      Serial.println("FAILED. Timed out waiting for backend response.");
      client.stop();
      Serial.println("--- End of Request ---");
      return;
    }
    delay(10);
  }

  Serial.println("Backend response:");
  while (client.available()) {
    String line = client.readStringUntil('\n');
    Serial.println(line);
  }

  client.stop();
  Serial.println("--- End of Request ---");
}
