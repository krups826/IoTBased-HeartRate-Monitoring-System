# Heart Beat Monitoring System

A full-stack, real-time Heart Beat Monitoring System built with Spring Boot, Vanilla JS, and ESP32.

## Project Structure
- `backend/` : Spring Boot REST API for Authentication, BPM logging, and Email alerts.
- `frontend/` : Modern dark-themed dashboard using Chart.js.
- `esp32/` : Arduino sketch for reading pulse data and sending it to the backend via HTTP POST.

## How to Run the Backend (Spring Boot)
1. **Database Setup**: Make sure MySQL is running. In `backend/src/main/resources/application.properties`, configure your database credentials:
   ```properties
   spring.datasource.username=root
   spring.datasource.password=root
   ```
2. **Email Setup**: Also in `application.properties`, add your Gmail credentials so the app can send alerts for High/Low BPMs:
   ```properties
   spring.mail.username=your_email@gmail.com
   spring.mail.password=your_app_password
   ```
   *Note: Use a Google App Password, not your standard account password.*
3. **Run**: 
   - Navigate into `backend` via terminal: `cd backend`
   - Run via Maven: `mvn spring-boot:run`
   - The server will start on `http://localhost:8080`.

## How to Run the Frontend
1. The frontend uses a pure HTML/CSS/JS architecture.
2. Navigate to the `frontend/` folder.
3. Because the frontend uses module `fetch` API, it's best to run it through a local server to avoid CORS issues if opening directly from the file system.
   - Using Python: `python -m http.server 3000`
   - Using Node: `npx serve`
   - Or using **Live Server** extension in VS Code.
4. Open the app in your browser (e.g., `http://localhost:3000`). Go to `register.html` first to create an account, then sign in.

## How to Setup ESP32
1. Open the `esp32/PulseMonitor.ino` file using the Arduino IDE.
2. Edit the file to add your WiFi details, your computer's local IP Address (where Spring Boot is running), and your generated JWT Token:
   ```cpp
   const char* ssid = "YOUR_WIFI_SSID";
   const char* password = "YOUR_WIFI_PASSWORD";
   const String serverName = "http://YOUR_LOCAL_IP:8080/api/bpm"; 
   const String jwtToken = "JWT_TOKEN_AFTER_LOGIN";
   ```
   *To easily get a JWT token, log into the frontend, right click -> Inspect -> Application -> Local Storage, and copy the `jwt_token` value.*
3. Wire the Pulse Sensor to the ESP32 (Analog Pin 34, 3.3V, GND).
4. Flash the code to the ESP32 and open the Serial Monitor.

## Features Included:
- **Authentication**: JWT based Login & Registration. Password Encryption.
- **REST API**: Clean architecture inside Spring Boot.
- **Alert System**: Async Email handling for Low (<60) and High (>100) BPMs.
- **Real-Time UI**: High-end modern dark UI with auto-fetching (`setInterval`) Chart.js graphs mapping to Heart Rate Status.
