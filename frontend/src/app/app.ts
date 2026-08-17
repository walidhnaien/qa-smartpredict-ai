import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';

import {
  getFullName,
  getUsername,
  isLoggedIn,
  login,
  logout
} from './core/auth/keycloak.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {

  username = getUsername();
  fullName = getFullName();
  connected = isLoggedIn();

  loginUser(): void {
    login();
  }

  logoutUser(): void {
    logout();
  }
}