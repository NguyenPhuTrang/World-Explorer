import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),

  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
    },
    {
      path: '/tours',
      name: 'tours',
      component: () => import('../views/ToursView.vue'),
    },
    {
      path: '/destinations',
      name: 'destinations',
      component: () => import('../views/DestinationsView.vue'),
    },
    {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    },
  ],
})

export default router