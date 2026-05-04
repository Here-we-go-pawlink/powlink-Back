import apiClient from '@/services/apiClient';
import { clearTokens } from '@/services/auth';

export const getMe = () => apiClient.get('/api/auth/me').then((r) => r.data);

export const updateProfile = (data) =>
  apiClient.patch('/api/users/profile', data).then((r) => r.data);

export const uploadProfileImage = (file) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('directory', 'profile');
  return apiClient.post('/api/images/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }).then((r) => r.data.url);
};

export const logout = async () => {
  try {
    await apiClient.post('/api/auth/logout');
  } finally {
    clearTokens();
    window.location.href = '/login';
  }
};
