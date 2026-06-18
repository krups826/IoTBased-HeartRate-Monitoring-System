// Configuration Theme Colors
const COLORS = {
    normal: '#00ff88',
    high: '#ff3366',
    low: '#33ccff',
    white: '#ffffff',
    bg: '#0f172a',
    grid: '#1e293b'
};

document.addEventListener('DOMContentLoaded', () => {
    if (!window.location.pathname.includes('dashboard.html')) return;

    document.getElementById('logoutBtn').addEventListener('click', () => {
        localStorage.removeItem('token');
        window.location.href = 'login.html';
    });

    const token = localStorage.getItem('token');
    if (!token) {
        alert('Please login first');
        window.location.href = 'login.html';
        return;
    }

    const ctx = document.getElementById('ecgChart').getContext('2d');
    const ecgChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: Array(20).fill(''),
            datasets: [{
                label: 'BPM',
                data: Array(20).fill(null),
                borderColor: COLORS.normal,
                borderWidth: 3,
                tension: 0.4,
                pointRadius: 0,
                fill: false
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            animation: { duration: 0 },
            plugins: { legend: { display: false } },
            scales: {
                y: {
                    min: 40,
                    max: 180,
                    grid: { color: COLORS.grid },
                    ticks: { color: COLORS.white }
                },
                x: {
                    grid: { display: false },
                    ticks: { display: false }
                }
            }
        }
    });

    function updateStatus(bpm) {
        const statusText = document.getElementById('statusText');
        const bpmDisplay = document.getElementById('currentBpmDisplay');

        bpmDisplay.textContent = bpm;

        let color;
        let text;

        if (bpm < 60) {
            color = COLORS.low;
            text = 'Low';
        } else if (bpm > 100) {
            color = COLORS.high;
            text = 'High';
        } else {
            color = COLORS.normal;
            text = 'Normal';
        }

        statusText.textContent = text;
        statusText.style.color = color;

        ecgChart.data.datasets[0].borderColor = color;
        ecgChart.update();
    }

    function renderReadings(readings) {
        const list = document.getElementById('readingsList');
        list.innerHTML = '';

        if (!Array.isArray(readings) || readings.length === 0) {
            list.innerHTML = '<div>No readings yet</div>';
            return;
        }

        readings.slice(0, 10).forEach((record) => {
            let timestamp = record.timestamp;

            if (Array.isArray(timestamp)) {
                timestamp = new Date(
                    timestamp[0],
                    timestamp[1] - 1,
                    timestamp[2],
                    timestamp[3],
                    timestamp[4],
                    timestamp[5]
                );
            }

            const time = new Date(timestamp).toLocaleTimeString([], {
                hour: '2-digit',
                minute: '2-digit'
            });

            let colorClass = 'normal';
            if (record.bpm < 60) colorClass = 'low';
            if (record.bpm > 100) colorClass = 'high';

            const item = document.createElement('div');
            item.innerHTML = `
                <span>${time}</span>
                <span class="${colorClass}">${record.bpm} BPM</span>
            `;
            list.appendChild(item);
        });
    }

    function renderChart(readings) {
        const latestReadings = readings.slice(0, 20).reverse();
        ecgChart.data.labels = latestReadings.map(() => '');
        ecgChart.data.datasets[0].data = latestReadings.map((record) => record.bpm);
        ecgChart.update();
    }

    function resetDashboard() {
        document.getElementById('currentBpmDisplay').textContent = '--';
        document.getElementById('statusText').textContent = 'Waiting...';
        document.getElementById('statusText').style.color = COLORS.white;
        ecgChart.data.labels = Array(20).fill('');
        ecgChart.data.datasets[0].data = Array(20).fill(null);
        ecgChart.data.datasets[0].borderColor = COLORS.normal;
        ecgChart.update();
    }

    async function fetchAndUpdateData() {
        try {
            let readings = await ApiService.getTodayReadings();

            if (Array.isArray(readings) && readings.length === 0) {
                const userInfoRaw = localStorage.getItem('user_info');
                const userInfo = userInfoRaw ? JSON.parse(userInfoRaw) : null;

                if (userInfo && userInfo.email) {
                    readings = await ApiService.getDeviceReadingsByEmail(userInfo.email);
                }
            }

            console.log('Fetched readings:', readings);

            if (!Array.isArray(readings)) {
                console.error('Invalid data:', readings);
                return;
            }

            renderReadings(readings);

            if (readings.length > 0) {
                updateStatus(readings[0].bpm);
                renderChart(readings);
            } else {
                resetDashboard();
            }
        } catch (error) {
            console.error('Fetch Error:', error);
        }
    }

    fetchAndUpdateData();
    setInterval(fetchAndUpdateData, 2000);
});
