document.addEventListener('DOMContentLoaded', () => {
    const loginView = document.getElementById('login-view');
    const resultView = document.getElementById('result-view');
    const loginForm = document.getElementById('loginForm');
    const messageElement = document.getElementById('message');
    const resultTitle = document.getElementById('result-title');
    const resultMessage = document.getElementById('result-message');
    const backToLoginButton = document.getElementById('back-to-login');

    function showResultView(success, title, message) {
        loginView.hidden = true;
        resultView.hidden = false;
        resultTitle.textContent = title;
        resultMessage.textContent = message;
        resultMessage.className = success ? 'message success' : 'message error';
    }

    function showLoginView() {
        resultView.hidden = true;
        loginView.hidden = false;
        document.getElementById('username').value = '';
        document.getElementById('password').value = '';
        messageElement.textContent = '';
    }

    loginForm.addEventListener('submit', async (event) => {
        event.preventDefault();

        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;

        messageElement.textContent = '';

        try {
            const response = await fetch('http://localhost:8080/api/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ username, password }),
            });

            const text = await response.text();

            if (response.ok) {
                showResultView(true, 'Login Successful', 'Welcome, ' + username + '!');
            } else {
                showResultView(false, 'Login Failed', text);
            }
        } catch (error) {
            showResultView(false, 'Error', 'An error occurred during login. Please try again.');
            console.error('Fetch error:', error);
        }
    });

    backToLoginButton.addEventListener('click', showLoginView);
});
