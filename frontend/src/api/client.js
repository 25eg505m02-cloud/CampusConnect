import axios from 'axios';

const baseURL = import.meta.env.VITE_API_BASE_URL;

export const api = axios.create({
  baseURL: baseURL || '/api/v1',
  timeout: 15000,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('campus-connect-token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export function apiMessage(error) {
  if (error?.response?.data?.message) return error.response.data.message;
  if (error?.code === 'ERR_NETWORK') {
    return 'Could not reach Campus Connect. Check that the backend is running and your API URL is correct.';
  }
  if (error?.code === 'ECONNABORTED') return 'The server took too long to respond. Please try again.';
  return error?.message || 'Something went wrong. Please try again.';
}

export function pageContent(data) {
  if (Array.isArray(data)) return data;
  return data?.content || [];
}
