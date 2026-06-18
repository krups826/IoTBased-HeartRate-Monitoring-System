const API_BASE_URL = 'http://10.81.79.38:8080/api';

const ApiService = {
    getToken() {
        return localStorage.getItem('token');
    },

    setToken(token) {
        localStorage.setItem('token', token);
    },

    clearAuth() {
        localStorage.removeItem('token');
        localStorage.removeItem('user_info');
    },

    getHeaders() {
        const headers = {
            'Content-Type': 'application/json'
        };
        const token = this.getToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }
        return headers;
    },

    async login(email, password) {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: this.getHeaders(),
            body: JSON.stringify({ email, password })
        });

        if (!response.ok) {
            const error = await response.json().catch(() => ({ message: 'Login failed' }));
            throw new Error(error.message || 'Login failed');
        }

        const data = await response.json();
        this.setToken(data.token);
        localStorage.setItem('user_info', JSON.stringify({
            id: data.id,
            firstName: data.firstName,
            age: data.age,
            email: data.email
        }));
        return data;
    },

    async sendOtp(email) {
        const response = await fetch(`${API_BASE_URL}/auth/send-otp`, {
            method: 'POST',
            headers: this.getHeaders(),
            body: JSON.stringify({ email })
        });
        if (!response.ok) {
            const error = await response.json().catch(() => ({ message: 'Failed to send OTP' }));
            throw new Error(error.message || 'Failed to send OTP');
        }
        return await response.json();
    },

    async register(firstName, age, email, password, confirmPassword, otp) {
        const response = await fetch(`${API_BASE_URL}/auth/register`, {
            method: 'POST',
            headers: this.getHeaders(),
            body: JSON.stringify({ firstName, age, email, password, confirmPassword, otp })
        });

        if (!response.ok) {
            const error = await response.json().catch(() => ({ message: 'Registration failed' }));
            throw new Error(error.message || 'Registration failed');
        }

        return await response.json();
    },

    async getTodayReadings() {
        const response = await fetch(`${API_BASE_URL}/bpm/today`, {
            method: 'GET',
            headers: this.getHeaders()
        });

        if (response.status === 401) {
            this.clearAuth();
            window.location.href = 'login.html';
            throw new Error('Unauthorized');
        }

        return await response.json();
    },

    async getDeviceReadingsByEmail(email) {
        const response = await fetch(`${API_BASE_URL}/bpm/device?email=${encodeURIComponent(email)}`, {
            method: 'GET',
            headers: this.getHeaders()
        });

        return await response.json();
    },

    async getAlerts() {
        const response = await fetch(`${API_BASE_URL}/alerts`, {
            method: 'GET',
            headers: this.getHeaders()
        });

        return await response.json();
    },

    async notifyCheck() {
        const response = await fetch(`${API_BASE_URL}/bpm/notify-check`, {
            method: 'GET',
            headers: this.getHeaders()
        });
        return await response.json();
    }
};

// Handle Authentication Pages
document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');
    const alertBox = document.getElementById('alertMessage');

    function showAlert(msg, isError = true) {
        if (!alertBox) return;
        alertBox.textContent = msg;
        alertBox.className = `alert ${isError ? 'alert-danger' : 'alert-success'}`;
        alertBox.classList.remove('hidden');
    }

    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const email = document.getElementById('email').value;
            const pwd = document.getElementById('password').value;
            try {
                await ApiService.login(email, pwd);
                window.location.href = 'dashboard.html';
            } catch (error) {
                showAlert(error.message);
            }
        });
    }

    if (registerForm) {
        const sendOtpBtn = document.getElementById('sendOtpBtn');
        const otpGroup = document.getElementById('otpGroup');

        if (sendOtpBtn) {
            sendOtpBtn.addEventListener('click', async () => {
                const email = document.getElementById('email').value;
                if (!email) {
                    showAlert('Please enter an email address first.', true);
                    return;
                }
                try {
                    sendOtpBtn.disabled = true;
                    sendOtpBtn.textContent = 'Sending...';
                    await ApiService.sendOtp(email);
                    showAlert('Verification code sent! Please check your email.', false);
                    if (otpGroup) {
                        otpGroup.classList.remove('hidden');
                        otpGroup.style.display = 'block';
                    }
                } catch (error) {
                    showAlert(error.message, true);
                } finally {
                    sendOtpBtn.disabled = false;
                    sendOtpBtn.textContent = 'Resend Code';
                }
            });
        }

        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const fName = document.getElementById('firstName').value;
            const age = document.getElementById('age').value;
            const email = document.getElementById('email').value;
            const pwd = document.getElementById('password').value;
            const cPwd = document.getElementById('confirmPassword').value;
            const otpCode = document.getElementById('otp') ? document.getElementById('otp').value : null;

            if (!otpCode) {
                showAlert('Please verify your email to get the code first.', true);
                return;
            }

            try {
                await ApiService.register(fName, parseInt(age), email, pwd, cPwd, otpCode);
                showAlert('Registration successful! Redirecting...', false);
                setTimeout(() => window.location.href = 'login.html', 1500);
            } catch (error) {
                showAlert(error.message, true);
            }
        });
    }

    // Protection logic for dashboard
    if (window.location.pathname.includes('dashboard.html')) {
        if (!ApiService.getToken()) {
            window.location.href = 'login.html';
        }
    }
});
