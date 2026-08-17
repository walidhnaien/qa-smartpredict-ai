import Keycloak from 'keycloak-js';

export const keycloak = new Keycloak({
  url: 'http://localhost:8082',
  realm: 'qa-smartpredict',
  clientId: 'qa-smartpredict-ui'
});

export function initKeycloak(): Promise<boolean> {
  return keycloak.init({
    onLoad: 'login-required',
    checkLoginIframe: false
  });
}

export function login(): Promise<void> {
  return keycloak.login({
    redirectUri: 'http://localhost:4200'
  });
}

export function logout(): Promise<void> {
  return keycloak.logout({
    redirectUri: 'http://localhost:4200'
  });
}

export function isLoggedIn(): boolean {
  return !!keycloak.authenticated;
}

export function getUsername(): string {
  return keycloak.tokenParsed?.['preferred_username'] || '';
}

export function getFullName(): string {
  return keycloak.tokenParsed?.['name'] || getUsername();
}

export function getToken(): string | undefined {
  return keycloak.token;
}