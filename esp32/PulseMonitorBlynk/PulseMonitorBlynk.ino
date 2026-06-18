#define BLYNK_TEMPLATE_ID "TMPL3OdQ3wPj7"
#define BLYNK_TEMPLATE_NAME "Heart Rate Monitor"
#define BLYNK_AUTH_TOKEN "sjBkevEpCXH0IjZKajUC-X5J1uY3JcuK"
#define BLYNK_PRINT Serial

#include <WiFi.h>
#include <BlynkSimpleEsp32.h>

char ssid[] = "OPPO F31 5G m3ua";
char pass[] = "nhms9438";

const int pulsePin = 34;
const int ledPin = 2;
int signalValue = 0;
int threshold = 1800;
int bpm = 0;

bool isAboveThreshold = false;
unsigned long lastBeatTime = 0;

BlynkTimer timer;

void sendPulseData() {
  if (!Blynk.connected()) {
    Serial.println("Blynk not connected. Skipping write.");
    return;
  }

  Blynk.beginGroup();
  Blynk.virtualWrite(V0, bpm);
  Blynk.virtualWrite(V1, signalValue);
  Blynk.endGroup();

  Serial.print("Sent to Blynk -> BPM: ");
  Serial.print(bpm);
  Serial.print(" | Signal: ");
  Serial.println(signalValue);
}

void setup() {
  Serial.begin(115200);
  pinMode(ledPin, OUTPUT);
  analogReadResolution(12);

  Serial.println("Starting Pulse Monitor with Blynk...");
  Serial.print("Threshold: ");
  Serial.println(threshold);

  Blynk.begin(BLYNK_AUTH_TOKEN, ssid, pass);
  timer.setInterval(2000L, sendPulseData);
}

void loop() {
  Blynk.run();
  timer.run();

  signalValue = analogRead(pulsePin);
  unsigned long now = millis();

  if (signalValue > threshold && !isAboveThreshold) {
    isAboveThreshold = true;
    digitalWrite(ledPin, HIGH);

    if (lastBeatTime > 0) {
      unsigned long interval = now - lastBeatTime;

      if (interval >= 300 && interval <= 2000) {
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

  delay(10);
}
