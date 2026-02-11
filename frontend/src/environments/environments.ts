// For local development (npm start) – point to API gateway:
export const environment = {
  production: false,
  apiUrl: 'https://localhost:8080', // Gateway with HTTPS
};

// For Docker deployment (docker-compose), use empty apiUrl so nginx proxies:
// export const environment = {
//   production: false,
//   apiUrl: '',
// };
