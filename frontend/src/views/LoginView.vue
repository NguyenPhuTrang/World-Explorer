<script setup lang="ts">
import { onMounted } from 'vue'

declare global {
  interface Window {
    google: {
      accounts: {
        id: {
          initialize: (config: {
            client_id: string
            callback: (response: { credential: string }) => void
          }) => void
          renderButton: (
            element: HTMLElement,
            options: {
              theme: string
              size: string
              text: string
              shape: string
              width: number
            }
          ) => void
        }
      }
    }
  }
}

onMounted(() => {
  if (!window.google) {
    console.error('Google Identity Services chưa được tải')
    return
  }

  window.google.accounts.id.initialize({
    client_id: '1054992285573-08ve5e9pm8hor51qkete8jm2pirh8l68.apps.googleusercontent.com',

    callback: (response) => {
    loginWithGoogle(response.credential)
    },
  })

  const buttonElement = document.getElementById('google-login-button')

  if (buttonElement) {
    window.google.accounts.id.renderButton(buttonElement, {
      theme: 'outline',
      size: 'large',
      text: 'signin_with',
      shape: 'rectangular',
      width: 300,
    })
  }
})

async function loginWithGoogle(idToken: string) {
  try {
    const response = await fetch('http://localhost:8080/api/auth/google', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        idToken: idToken,
      }),
    })

    if (!response.ok) {
      const error = await response.text()
      throw new Error(error || 'Google Login failed')
    }

    const data = await response.json()

    console.log('Login successful:', data)

  } catch (error) {
    console.error('Login error:', error)
  }
}

</script>

<template>
  <main class="login-page">
    <div class="login-card">
      <h1>Welcome to World Explorer</h1>
      <p>Sign in to explore tours and destinations.</p>

      <div id="google-login-button"></div>
    </div>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 40px;
  text-align: center;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
}

.login-card h1 {
  margin-bottom: 12px;
  font-size: 24px;
}

.login-card p {
  margin-bottom: 28px;
  color: #6b7280;
}
</style>